package com.unciv.logic.scripting

import com.badlogic.gdx.files.FileHandle
import yairm210.purity.annotations.Readonly
import com.unciv.logic.civilization.AlertType
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.PopupAlert
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unique.Countables
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.utils.Log
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.DebugLib
import org.luaj.vm2.lib.jse.JsePlatform

fun luaFunction(block: (Varargs) -> LuaValue): LuaFunction {
    return object : LuaFunction() {
        override fun call(): LuaValue = block(LuaValue.NONE)
        override fun call(arg: LuaValue): LuaValue = block(arg)
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue = block(LuaValue.varargsOf(arg1, arg2))
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue =
            block(LuaValue.varargsOf(arg1, arg2, arg3))

        /**
         * CRITICAL: luaj's [LuaClosure] uses [LuaValue.invoke] for any call whose argument list
         * contains a function call expression (e.g. `ctx.store.set("k", tostring(1))`), while plain
         * literal arguments use the [call] overloads above. The base [LuaFunction] does not override
         * invoke, so it falls through to [LuaValue.callmt] which looks up the `__call` metamethod -
         * absent for custom LuaFunction subclasses (luaj only installs it for its own LibFunctions via
         * the debug library) - throwing "attempt to call function" at runtime.
         * Overriding invoke here fixes every `foo(bar())` style call in mod scripts.
         */
        override fun invoke(varargs: Varargs): Varargs = block(varargs)
    }
}

object LuaScriptManager {

    private val luaFunctionRefRegex = Regex("^[a-zA-Z_]\\w*(:[a-zA-Z_]\\w*)?$")
    private val countableRegex = Regex("\\[[^]]+\\]")

    /**
     * Maximum number of Lua bytecode instructions a single script load (top-level execution)
     * or a single function call may execute. Guards against accidental or malicious infinite
     * loops (e.g. `while true do end`) that would otherwise hang the game's main thread forever.
     * luaj's interpreter executes on the order of a few million instructions per second, so this
     * budget limits one call to roughly a second of CPU time - far more than any sane mod logic.
     */
    private const val INSTRUCTION_BUDGET = 10_000_000L

    /**
     * Instruction-counting debug library installed on every sandboxed globals.
     * The luaj VM calls [onInstruction] for every executed bytecode instruction, which lets us
     * interrupt runaway loops. We deliberately override all three VM callbacks ([onCall],
     * [onInstruction], [onReturn]) without touching the superclass' internal `globals` field,
     * so no `debug.*` library is exposed to the mod (the [DebugLib] superclass would otherwise
     * register `debug` into the globals table, which the sandbox explicitly forbids).
     */
    private class InstructionBudgetDebugLib : DebugLib() {
        private var instructionCount = 0L
        private var budget = 0L

        fun reset(budget: Long) {
            instructionCount = 0L
            this.budget = budget
        }

        override fun onCall(closure: org.luaj.vm2.LuaClosure, varargs: Varargs, stack: Array<LuaValue>) {
            checkBudget()
        }

        /** BaseLib.pcall invokes this single-argument overload; the default reads this.globals (null here). */
        override fun onCall(function: LuaFunction) {
            checkBudget()
        }

        override fun onInstruction(pc: Int, v: Varargs, top: Int) {
            checkBudget()
        }

        override fun onReturn() {
            checkBudget()
        }

        /**
         * The luaj VM calls [DebugLib.traceback] from `LuaClosure.errorHook` whenever a script
         * throws, and the base implementation reads `this.globals` - which we never initialize
         * (initializing it via the superclass `call` would register the `debug.*` library into
         * the sandbox). Returning an empty traceback keeps the original error message intact.
         */
        override fun traceback(level: Int): String = ""

        private fun checkBudget() {
            if (++instructionCount > budget)
                throw LuaError("Lua execution budget exceeded - possible infinite loop")
        }
    }

    /** Per-mod instruction budgets, reset before every top-level load and every function call. */
    private val modInstructionBudgets = java.util.concurrent.ConcurrentHashMap<String, InstructionBudgetDebugLib>()

    /** Globals per mod for sandbox isolation */
    private val modGlobals = java.util.concurrent.ConcurrentHashMap<String, Globals>()

    // Cache for getKnownFunctions, invalidated by clear/clearMod
    private var cachedKnownFunctions: Set<String>? = null
    private var cachedKnownFunctionsRuleset: Ruleset? = null

    // Dedup set to prevent showing the same Lua error popup repeatedly in one session
    private val shownLuaErrors = HashSet<String>()

    fun clear() {
        modGlobals.clear()
        modInstructionBudgets.clear()
        cachedKnownFunctions = null
        cachedKnownFunctionsRuleset = null
        shownLuaErrors.clear()
    }

    fun clearMod(modName: String) {
        modGlobals.remove(modName)
        modInstructionBudgets.remove(modName)
        cachedKnownFunctions = null
        cachedKnownFunctionsRuleset = null
        shownLuaErrors.clear()
    }

    fun isModLoaded(modName: String) = modGlobals.containsKey(modName)

    fun getKnownFunctions(ruleset: Ruleset): Set<String> {
        if (cachedKnownFunctions != null && cachedKnownFunctionsRuleset === ruleset)
            return cachedKnownFunctions!!

        val functions = HashSet<String>()
        for ((modName, g) in modGlobals) {
            // When mods is populated (combined ruleset), only consider functions from mods in the ruleset.
            // When mods is empty (standalone mod loaded via loadSingleRuleset), consider all loaded functions.
            if (ruleset.mods.isNotEmpty() && modName !in ruleset.mods) continue
            for (key in g.keys()) {
                val v = g.get(key)
                if (v !is org.luaj.vm2.LuaClosure) continue
                functions.add(key.tojstring())
            }
        }
        cachedKnownFunctions = functions
        cachedKnownFunctionsRuleset = ruleset
        return functions
    }

    fun loadScripts(folderHandle: FileHandle, modName: String, ruleset: Ruleset): List<String> {
        val scriptsDir = folderHandle.child("scripts")
        if (!scriptsDir.exists() || !scriptsDir.isDirectory) {
            Log.debug("Lua: no scripts/ directory found for mod $modName")
            return emptyList()
        }

        val globals = createSandboxedGlobals(modName, ruleset)
        modGlobals[modName] = globals

        val budget = modInstructionBudgets[modName] ?: InstructionBudgetDebugLib().also {
            modInstructionBudgets[modName] = it
            globals.debuglib = it
        }

        val loaded = ArrayList<String>()

        // Phase 1: compile & execute all .lua files to define functions in globals
        for (file in scriptsDir.list()) {
            if (file.extension() != "lua") continue
            try {
                val source = file.readString(Charsets.UTF_8.name())
                val chunk = globals.load(source, file.name())
                budget.reset(INSTRUCTION_BUDGET)
                chunk.call() // Execute chunk so function definitions are registered
                loaded += file.nameWithoutExtension()
                Log.debug("Lua: loaded script ${file.name()} for mod $modName")
            } catch (ex: LuaError) {
                Log.error("Lua syntax error in ${file.name()}: ${ex.message}")
                val lineNum = extractLineNumber(ex.message)
                val msg = if (lineNum != null)
                    "Lua script '${file.name()}' has a syntax error at line $lineNum: ${ex.message}"
                else
                    "Lua script '${file.name()}' has a syntax error: ${ex.message}"
                ruleset.luaErrors.add(LuaScriptError(modName, file.name(), LuaScriptErrorSeverity.ERROR, msg, lineNum))
            } catch (ex: Exception) {
                Log.error("Failed to load Lua script ${file.name()}: ${ex.message}")
                ruleset.luaErrors.add(LuaScriptError(
                    modName, file.name(), LuaScriptErrorSeverity.ERROR,
                    "Failed to load Lua script '${file.name()}': ${ex.message}"
                ))
            }
        }

        if (loaded.isNotEmpty()) {
            ruleset.luaErrors.add(LuaScriptError(
                modName, null, LuaScriptErrorSeverity.INFO,
                "Loaded ${loaded.size} Lua script(s) for mod '$modName': ${loaded.joinToString(", ")}"
            ))
        }

        Log.debug("Lua: loaded ${loaded.size} scripts for mod $modName: $loaded")

        return loaded
    }

    fun getFunction(modName: String, functionName: String): Pair<String, LuaFunction>? {
        if (modName.isNotEmpty()) {
            val globals = modGlobals[modName]
            if (globals != null) {
                val func = globals.get(functionName)
                if (func != LuaValue.NIL && func is LuaFunction)
                    return modName to func
            }
        }
        // Fallback: search all mod globals
        for ((name, g) in modGlobals) {
            val func = g.get(functionName)
            if (func != LuaValue.NIL && func is LuaFunction)
                return name to func
        }
        Log.debug("Lua: function '$functionName' not found in any loaded mod (mod: '$modName')")
        return null
    }

    fun callFunction(
        func: LuaFunction,
        ctxTable: LuaValue,
        civInfo: Civilization? = null,
        functionName: String = "",
        onSuccess: (Boolean) -> Unit,
        modName: String = ""
    ) {
        // Reset the mod's instruction budget so a previous runaway script can't poison later calls
        modInstructionBudgets[modName]?.reset(INSTRUCTION_BUDGET)
        try {
            val result = func.call(ctxTable)
            val success = result.toboolean(1)
            onSuccess(success)
        } catch (ex: LuaError) {
            Log.error("Lua runtime error: ${ex.message}", ex)
            reportLuaError(civInfo, functionName, ex.message ?: "Unknown Lua runtime error", modName)
            onSuccess(false)
        } catch (ex: Exception) {
            Log.error("Unexpected Lua error: ${ex.message}", ex)
            reportLuaError(civInfo, functionName, ex.message ?: "Unknown error", modName)
            onSuccess(false)
        }
    }

    private fun reportLuaError(civInfo: Civilization?, functionName: String, message: String, modName: String = "") {
        val lineNum = extractLineNumber(message)
        val displayMessage = if (functionName.isNotEmpty())
            if (lineNum != null)
                "Function '$functionName' error at line $lineNum:|$message"
            else
                "Function '$functionName' error:|$message"
        else
            message

        // Record the error in the ruleset's error list so mod authors can see it in the
        // in-game mod checker - regardless of whether the triggering civ is human (AI-turn
        // errors previously only went to the log and were invisible to mod authors).
        val ruleset = civInfo?.gameInfo?.ruleset
        if (ruleset != null) {
            ruleset.luaErrors.add(LuaScriptError(
                modName, null, LuaScriptErrorSeverity.WARNING,
                "Runtime Lua error: $displayMessage", lineNum
            ))
        }

        if (civInfo == null || !civInfo.isHuman()) return
        val errorKey = if (functionName.isNotEmpty()) functionName else message
        if (!shownLuaErrors.add(errorKey)) return // Already shown this error in this session
        civInfo.popupAlerts.add(PopupAlert(AlertType.LuaError, displayMessage))
    }

    fun resolveCountablesInString(raw: String, gameContext: GameContext): String {
        if (!raw.contains('[')) return raw
        return countableRegex.replace(raw) { match ->
            val expression = match.value
            val result = Countables.getCountableAmount(expression, gameContext)
            result?.toString() ?: expression
        }
    }

    fun parseLuaRef(luaRef: String): Pair<String, String> {
        val parts = luaRef.split(":", limit = 2)
        return if (parts.size == 2) parts[0] to parts[1]
        else "" to parts[0]
    }

    @Readonly fun isValidFunctionRef(ref: String): Boolean = luaFunctionRefRegex.matches(ref)

    private fun extractLineNumber(message: String?): Int? {
        if (message == null) return null
        val regex = Regex(""":(\d+):""")
        return regex.find(message)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun createSandboxedGlobals(modName: String, ruleset: Ruleset): Globals {
        val globals = JsePlatform.standardGlobals()

        // Remove dangerous standard libraries and functions
        for (dangerous in listOf(
            "os", "io", "luajava",
            "dofile", "loadfile", "load", "loadstring",
            "require", "collectgarbage", "module",
            "rawget", "rawset", "rawequal", "rawlen",
            "setmetatable", "getmetatable", "newproxy",
            "debug", "coroutine",
            // The package library must go entirely: its `package.loaded` table keeps full
            // references to `io`, `os`, `luajava` and `coroutine` even after the globals above
            // are set to nil, giving mod scripts complete sandbox escape (arbitrary file
            // read/write, process execution and Java reflection via luajava).
            "package"
        )) {
            globals.set(dangerous, LuaValue.NIL)
        }

        // Remove string.dump for sandbox safety (prevents bytecode exfiltration)
        val stringLib = globals.get("string")
        if (stringLib is org.luaj.vm2.LuaTable) stringLib.set("dump", LuaValue.NIL)

        // Redirect print to game log
        globals.set("print", luaFunction { args ->
            val items = (1..args.narg()).map { args.arg(it).tojstring() }
            Log.debug("Lua[$modName]: ${items.joinToString("\t")}")
            LuaValue.NIL
        })

        return globals
    }
}

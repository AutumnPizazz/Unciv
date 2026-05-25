package com.unciv.logic.scripting

import com.badlogic.gdx.files.FileHandle
import yairm210.purity.annotations.Readonly
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unique.Countables
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.utils.Log
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.jse.JsePlatform

fun luaFunction(block: (Varargs) -> LuaValue): LuaFunction {
    return object : LuaFunction() {
        override fun call(): LuaValue = block(LuaValue.NONE)
        override fun call(arg: LuaValue): LuaValue = block(arg)
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue = block(LuaValue.varargsOf(arg1, arg2))
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue =
            block(LuaValue.varargsOf(arg1, arg2, arg3))
    }
}

object LuaScriptManager {

    private val luaFunctionRefRegex = Regex("^[a-zA-Z_]\\w*(:[a-zA-Z_]\\w*)?$")
    private val countableRegex = Regex("\\[[^]]+\\]")

    /** Globals per mod for sandbox isolation */
    private val modGlobals = java.util.concurrent.ConcurrentHashMap<String, Globals>()

    fun clear() {
        modGlobals.clear()
    }

    fun clearMod(modName: String) {
        modGlobals.remove(modName)
    }

    fun isModLoaded(modName: String) = modGlobals.containsKey(modName)

    fun getKnownFunctions(ruleset: Ruleset): Set<String> {
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

        val loaded = ArrayList<String>()

        // Phase 1: compile & execute all .lua files to define functions in globals
        for (file in scriptsDir.list()) {
            if (file.extension() != "lua") continue
            try {
                val source = file.readString(Charsets.UTF_8.name())
                val chunk = globals.load(source, file.name())
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

        // Phase 2: if init.lua was loaded, it auto-ran as part of chunk.call() above
        // so its top-level code already executed

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
        onSuccess: (Boolean) -> Unit
    ) {
        try {
            val result = func.call(ctxTable)
            val success = result.toboolean(1)
            onSuccess(success)
        } catch (ex: LuaError) {
            Log.error("Lua runtime error: ${ex.message}", ex)
            onSuccess(false)
        } catch (ex: Exception) {
            Log.error("Unexpected Lua error: ${ex.message}", ex)
            onSuccess(false)
        }
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
            "debug", "coroutine"
        )) {
            globals.set(dangerous, LuaValue.NIL)
        }

        // Redirect print to game log
        globals.set("print", luaFunction { args ->
            val items = (1..args.narg()).map { args.arg(it).tojstring() }
            Log.debug("Lua[$modName]: ${items.joinToString("\t")}")
            LuaValue.NIL
        })

        return globals
    }
}

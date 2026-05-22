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
        override fun call(arg: LuaValue): LuaValue = block(LuaValue.varargsOf(arg, LuaValue.NONE))
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue =
            block(LuaValue.varargsOf(arg1, LuaValue.varargsOf(arg2, LuaValue.NONE)))
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue =
            block(LuaValue.varargsOf(arg1, LuaValue.varargsOf(arg2, LuaValue.varargsOf(arg3, LuaValue.NONE))))
    }
}

object LuaScriptManager {

    private val luaFunctionRefRegex = Regex("^[a-zA-Z_]\\w*(:[a-zA-Z_]\\w*)?$")
    private val countableRegex = Regex("\\[[^]]+\\]")

    /** Globals per mod for sandbox isolation */
    private val modGlobals = HashMap<String, Globals>()

    fun getKnownFunctions(ruleset: Ruleset): Set<String> {
        val functions = HashSet<String>()
        for (modName in ruleset.mods) {
            val g = modGlobals[modName] ?: continue
            // Enumerate non-nil globals that are functions
            for (key in g.keys()) {
                val v = g.get(key)
                if (v is LuaFunction && v !is org.luaj.vm2.LuaClosure) continue
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
            } catch (ex: Exception) {
                Log.error("Failed to load Lua script ${file.name()}: ${ex.message}")
            }
        }

        Log.debug("Lua: loaded ${loaded.size} scripts for mod $modName: $loaded")

        // Phase 2: if init.lua was loaded, it auto-ran as part of chunk.call() above
        // so its top-level code already executed

        return loaded
    }

    fun getFunction(modName: String, functionName: String): LuaFunction? {
        val globals = modGlobals[modName]
        if (globals != null) {
            val func = globals.get(functionName)
            if (func != LuaValue.NIL && func is LuaFunction)
                return func
        }
        // Fallback: search all mod globals
        for ((_, g) in modGlobals) {
            val func = g.get(functionName)
            if (func != LuaValue.NIL && func is LuaFunction)
                return func
        }
        Log.debug("Lua: function '$functionName' not found in any loaded mod")
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
            Log.error("Lua runtime error: ${ex.message}")
            onSuccess(false)
        } catch (ex: Exception) {
            Log.error("Unexpected Lua error: ${ex.message}")
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

    fun parseLuaRef(luaRef: String, modName: String): Pair<String, String> {
        val parts = luaRef.split(":", limit = 2)
        return if (parts.size == 2) parts[0] to parts[1]
        else modName to parts[0]
    }

    @Readonly fun isValidFunctionRef(ref: String): Boolean = luaFunctionRefRegex.matches(ref)

    private fun createSandboxedGlobals(modName: String, ruleset: Ruleset): Globals {
        val globals = JsePlatform.standardGlobals()

        // Remove dangerous standard libraries and functions
        for (dangerous in listOf(
            "os", "io", "luajava",
            "dofile", "loadfile", "load", "loadstring",
            "require", "collectgarbage", "module",
            "rawget", "rawset", "rawequal", "rawlen",
            "setmetatable", "getmetatable", "newproxy",
            "debug"
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

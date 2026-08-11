package com.unciv.logic.scripting

import com.badlogic.gdx.files.FileHandle

/**
 * Static checks for mod Lua scripts that can run without a live game, used by the CLI mod
 * checker (desktop `mod-ci`). Complements the runtime validator: [LuaScriptManager] reports
 * syntax errors and missing function references, this checker catches API spelling mistakes
 * such as `ctx.civ.addGoldd(...)` that only surface at runtime.
 */
object LuaModStaticChecker {

    /** Pattern for direct API access: ctx.<owner>.<method> - catches typos in method names. */
    private val apiCallRegex = Regex("""ctx\.(civ|city|unit|tile|game|store|parameter)\s*\.\s*([A-Za-z_]\w*)""")

    /**
     * Scans every `.lua` file under [scriptsDir] for direct calls to unknown
     * `ctx.<owner>.<method>` APIs. Returns one [LuaScriptError] (WARNING severity) per unknown
     * name, so mod authors see them in the same error list as other Lua issues.
     */
    fun checkApiUsage(scriptsDir: FileHandle): List<LuaScriptError> {
        if (!scriptsDir.exists() || !scriptsDir.isDirectory) return emptyList()

        val errors = ArrayList<LuaScriptError>()
        for (file in scriptsDir.list().sortedBy { it.name() }) {
            if (file.extension() != "lua") continue
            val lines = try {
                file.readString(Charsets.UTF_8.name()).lines()
            } catch (ex: Exception) {
                errors.add(LuaScriptError(
                    "", file.name(), LuaScriptErrorSeverity.ERROR,
                    "Failed to read Lua script '${file.name()}': ${ex.message}"
                ))
                continue
            }
            for ((index, rawLine) in lines.withIndex()) {
                val line = stripComment(rawLine)
                if (line.isBlank()) continue
                for (match in apiCallRegex.findAll(line)) {
                    val owner = match.groupValues[1]
                    val method = match.groupValues[2]
                    val known = when (owner) {
                        "parameter" -> true // ctx.parameter is a plain string field
                        else -> LuaAPI.apiCatalog[owner]?.contains(method) == true
                    }
                    if (!known) {
                        val hint = if (owner == "parameter") "ctx.parameter is a value, not a table"
                        else "unknown API - did you mean one of: ${suggestSimilar(method, LuaAPI.apiCatalog[owner].orEmpty())}"
                        errors.add(LuaScriptError(
                            "", file.name(), LuaScriptErrorSeverity.WARNING,
                            "Lua script '${file.name()}' line ${index + 1}: ctx.$owner.$method - $hint",
                            index + 1
                        ))
                    }
                }
            }
        }
        return errors
    }

    /** Removes a `--` comment (but not the one inside a long string - good enough for a lint). */
    private fun stripComment(line: String): String {
        val commentStart = line.indexOf("--")
        return if (commentStart >= 0) line.substring(0, commentStart) else line
    }

    /** Cheap suggestion: known names sharing a 3+ char prefix, for friendlier error messages. */
    private fun suggestSimilar(method: String, known: Set<String>): String {
        val candidates = known
            .filter { it.length > 3 && method.length > 3 &&
                (it.startsWith(method.take(3)) || method.startsWith(it.take(3))) }
            .sortedBy { it.length }
            .take(3)
        return if (candidates.isEmpty()) "no close match in the Lua API" else candidates.joinToString(", ")
    }
}

package com.unciv.logic.scripting

/** Severity of a Lua-related issue, mapped to [com.unciv.models.ruleset.validation.RulesetErrorSeverity] during validation. */
enum class LuaScriptErrorSeverity {
    /** Informational only — e.g. successfully loaded scripts count. Maps to OK (green). */
    INFO,
    /** Potential issue that does not block play — e.g. unknown function reference in standalone mod check. Maps to Warning (yellow). */
    WARNING,
    /** Definite error that should block game start — e.g. syntax error, missing function in combined ruleset. Maps to Error (red). */
    ERROR
}

data class LuaScriptError(
    val modName: String,
    val scriptName: String?,
    val severity: LuaScriptErrorSeverity,
    val message: String,
    val lineNumber: Int? = null
)

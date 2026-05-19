package com.unciv.models.ruleset

/**
 * Merge control data attached to a game object.
 *
 * The [action] field determines how the object is merged into the target ruleset.
 * The [if_]/[then_]/[else_] fields are for control blocks and are handled at the
 * JsonValue level during [resolveConditionals] — they are not deserialized into
 * this class directly.
 *
 * JSON field mapping:
 * - "action" → [action]
 * - "if"     → [if_] (handled at JsonValue level, not deserialized here)
 * - "then"   → [then_] (handled at JsonValue level, not deserialized here)
 * - "else"   → [else_] (handled at JsonValue level, not deserialized here)
 */
class MergeAction {
    /** Operation type: null (replace), "TRY_INJECT", "CREATE_OR_REPLACE", "REMOVE", "REMOVE_FIELD" */
    var action: String? = null
}

/**
 * Condition for conditional merge operations.
 *
 * All fields are nullable — a non-null field represents an active condition atom.
 * Combinators ([not_], [and_], [or_]) combine sub-conditions.
 *
 * JSON field mapping note: The JSON key "not" maps to [not_], "and" maps to [and_],
 * "or" maps to [or_]. These are handled at JsonValue level during [resolveConditionals]
 * and are not deserialized here.
 */
class MergeCondition {
    // Environment conditions
    var mod_loaded: String? = null
    var mod_author: String? = null
    var base_ruleset: String? = null
    var game_version: String? = null

    // Object conditions
    var object_exists: String? = null
    var object_count: CountCheck? = null
    var object_has_unique: String? = null
    var any_object_has_unique: String? = null
    var object_has_field: FieldCheck? = null
    var object_field_equals: FieldCheck? = null
    var object_field_contains: FieldCheck? = null
    var object_field_compare: CompareCheck? = null

    // Combinators (handled at JsonValue level)
    var not_: MergeCondition? = null
    var and_: List<MergeCondition>? = null
    var or_: List<MergeCondition>? = null
}

class FieldCheck {
    var object_: String = ""
    var field: String = ""
    var equals: Any? = null
    var value: Any? = null
}

class CountCheck {
    var type: String = ""
    var greater_than: Double? = null
    var greater_than_or_equal: Double? = null
    var less_than: Double? = null
    var less_than_or_equal: Double? = null
    var equal: Double? = null
}

class CompareCheck {
    var object_: String = ""
    var field: String = ""
    var greater_than: Double? = null
    var greater_than_or_equal: Double? = null
    var less_than: Double? = null
    var less_than_or_equal: Double? = null
    var equal: Double? = null
}

/** Context for evaluating merge conditions. */
data class MergeContext(
    val loadedMods: Set<String>,
    val baseRuleset: String,
    val currentRuleset: Ruleset
) {
    companion object {
        val EMPTY = MergeContext(emptySet(), "", Ruleset())
    }
}

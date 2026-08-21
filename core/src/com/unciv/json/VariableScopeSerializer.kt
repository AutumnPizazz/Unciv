package com.unciv.json

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.Json.Serializer
import com.badlogic.gdx.utils.JsonValue
import com.unciv.models.ruleset.VariableScope

/** Keeps the mod-facing scope spelling lowercase while accepting legacy/capitalized input. */
class VariableScopeSerializer : Serializer<VariableScope> {
    override fun write(json: Json, scope: VariableScope, knownType: Class<*>?) {
        json.writeValue(scope.name.lowercase())
    }

    override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): VariableScope {
        val value = jsonData.asString()
        return VariableScope.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown variable scope '$value'")
    }
}

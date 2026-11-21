package com.unciv.logic.multiplayer

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.json.json

data class RealtimeTurnActionRequest(
    val turn: Int = 0,
    val clientSequence: Long = 0,
    val conflictKeys: ArrayList<String> = ArrayList(),
    val operations: ArrayList<SimultaneousTurnOperation> = ArrayList()
)

data class RealtimeTurnReadyRequest(
    val turn: Int = 0,
    val clientSequence: Long = 0
)

data class RealtimeTurnActionEntry(
    val turn: Int = 0,
    val sequence: Long = 0,
    val playerId: String = "",
    val clientSequence: Long = 0,
    val kind: String = "",
    val conflictKeys: ArrayList<String> = ArrayList(),
    val operations: ArrayList<SimultaneousTurnOperation> = ArrayList(),
    val createdAt: Long = 0
)

data class RealtimeTurnActionFeed(
    val turn: Int = 0,
    val after: Long = 0,
    val latestSequence: Long = 0,
    val actions: ArrayList<RealtimeTurnActionEntry> = ArrayList(),
    val readyPlayerIds: ArrayList<String> = ArrayList(),
    val timedOutPlayerIds: ArrayList<String> = ArrayList(),
    val allPlayersReady: Boolean = false
)

object RealtimeTurnActions {
    fun requestFor(operation: SimultaneousTurnOperation) = RealtimeTurnActionRequest(
        turn = operation.turn,
        clientSequence = operation.sequence,
        conflictKeys = ArrayList(conflictKeys(listOf(operation))),
        operations = arrayListOf(operation)
    )

    fun conflictKeys(operations: List<SimultaneousTurnOperation>): List<String> {
        val keys = sortedSetOf<String>()
        for (operation in operations) keys += conflictKeysFor(operation)
        return keys.ifEmpty { sortedSetOf("global") }.toList()
    }

    private fun conflictKeysFor(operation: SimultaneousTurnOperation): Set<String> {
        return try {
            val payload = JsonReader().parse(operation.payload)
            when {
                SimultaneousTurnOperations.isStateOperationType(operation.type) -> stateConflictKeys(payload)
                operation.type == "unit.move" -> unitKeys(payload) + tileKeys(payload, "fromX", "fromY", "toX", "toY")
                operation.type == "unit.swap" -> unitKeys(payload) + tileKeys(payload, "fromX", "fromY", "toX", "toY")
                operation.type == "unit.attack" -> attackKeys(payload)
                operation.type == "unit.action" -> unitKeys(payload)
                else -> setOf("global")
            }
        } catch (_: Exception) {
            setOf("global")
        }
    }

    private fun stateConflictKeys(payload: JsonValue): Set<String> {
        val result = json().fromJson(SimultaneousTurnGameStateResult::class.java, payload.toJson(JsonWriter.OutputType.json))
        val keys = sortedSetOf<String>()
        for (civ in result.civilizations) {
            // The explored bounds are independent of diplomacy and other civilization state.
            if ("exploredRegion" in civ.changedFields) keys += "exploration:${civ.key}"
            // Proximity is recalculated from city locations after replay. Founding a city
            // must not claim the governments of all the other civilizations on the map.
            if (civ.changedFields.isEmpty() || civ.changedFields.any {
                it != "exploredRegion" && it != SimultaneousTurnOperations.proximityField
            })
                keys += "civ:${civ.key}"
        }
        for (tile in result.tiles) {
            // Learning about a tile does not reserve the tile or the units already on it.
            if (SimultaneousTurnOperations.isExplorationOnly(tile)) continue
            keys += "tile:${tile.key}"
            addUnitKeys(tile.before, keys)
            addUnitKeys(tile.after, keys)
        }
        for (religion in result.religions) keys += "religion:${religion.key}"
        for (change in result.luaStoreChanges) keys += "lua:${change.modName}:${change.key}"
        if (result.globalBefore != result.globalAfter) {
            val before = JsonReader().parse(result.globalBefore)
            val after = JsonReader().parse(result.globalAfter)
            for (field in listOf("lastUnitId", "diplomaticVictoryVotesCast", "unitNamesTaken", "variables")) {
                if (before.get(field)?.toJson(JsonWriter.OutputType.json) != after.get(field)?.toJson(JsonWriter.OutputType.json))
                    keys += "global:$field"
            }
        }
        return keys
    }

    private fun addUnitKeys(tileJSON: String?, keys: MutableSet<String>) {
        if (tileJSON.isNullOrEmpty()) return
        val tile = JsonReader().parse(tileJSON)
        for (field in listOf("militaryUnit", "civilianUnit"))
            addUnitKey(tile.get(field), keys)
        val airUnits = tile.get("airUnits") ?: return
        for (unit in airUnits) addUnitKey(unit, keys)
    }

    private fun addUnitKey(unit: JsonValue?, keys: MutableSet<String>) {
        if (unit == null || unit.isNull) return
        val owner = unit.getString("owner", "")
        val id = unit.getInt("id", Int.MIN_VALUE)
        if (owner.isNotEmpty() && id != Int.MIN_VALUE) keys += "unit:$owner:$id"
    }

    private fun unitKeys(payload: JsonValue): Set<String> = buildSet {
        addUnitKey(payload, "owner", "unitId", this)
    }

    private fun attackKeys(payload: JsonValue): Set<String> = buildSet {
        addUnitKey(payload, "attackerOwner", "attackerId", this)
        addUnitKey(payload, "targetOwner", "targetId", this)
        payload.getString("targetOwner", "").takeIf { it.isNotEmpty() }?.let { add("civ:$it") }
        payload.getInt("targetX", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }?.let { x ->
            add("tile:$x,${payload.getInt("targetY", 0)}")
        }
    }

    private fun addUnitKey(payload: JsonValue, ownerField: String, unitField: String, keys: MutableSet<String>) {
        val owner = payload.getString(ownerField, "")
        val unitId = payload.getInt(unitField, Int.MIN_VALUE)
        if (owner.isEmpty() || unitId == Int.MIN_VALUE) return
        val key = "unit:$owner:$unitId"
        keys.add(key)
    }

    private fun tileKeys(payload: JsonValue, fromX: String, fromY: String, toX: String, toY: String): Set<String> = buildSet {
        addTile(payload, fromX, fromY, this)
        addTile(payload, toX, toY, this)
    }

    private fun addTile(payload: JsonValue, xField: String, yField: String, keys: MutableSet<String>) {
        val x = payload.getInt(xField, Int.MIN_VALUE)
        val y = payload.getInt(yField, Int.MIN_VALUE)
        if (x != Int.MIN_VALUE && y != Int.MIN_VALUE) keys += "tile:$x,$y"
    }
}

package com.unciv.logic.multiplayer

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.Constants
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.PopupAlert
import com.unciv.logic.multiplayer.SimultaneousTurnOperations.exploredByField
import com.unciv.logic.multiplayer.SimultaneousTurnOperations.proximityField
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.models.Religion
import com.unciv.utils.Log
import java.io.StringWriter

/**
 * Why settlement could not apply a recorded operation. [sentence] is a translation key, so the
 * player who lost the action reads the reason in their own language - the reason must not be
 * pre-translated, because the notification is stored in the save and shared between clients.
 */
enum class SimultaneousTurnFailureReason(val sentence: String) {
    UnitGone("The unit is no longer there"),
    WorldChanged("The world had already changed when this was settled"),
    Unreadable("The server could not read this action")
}

/** One operation settlement could not apply, and why it was refused. */
data class SimultaneousTurnFailedOperation(
    val operation: SimultaneousTurnOperation,
    val reason: SimultaneousTurnFailureReason
)

/** Applies recorded results to the turn-start game without re-running random combat. */
object SimultaneousTurnReplay {
    private data class UnitIdKey(val owner: String, val id: Int)
    private const val popupAlertsField = "popupAlerts"
    private val civilizationFieldsMergedOnReplay = setOf(popupAlertsField, proximityField)
    private val tileFieldsMergedOnReplay = setOf(exploredByField)

    fun replay(gameInfo: GameInfo, operations: List<SimultaneousTurnOperation>): List<SimultaneousTurnOperation> =
        replayOrExplain(gameInfo, operations).map { it.operation }

    /**
     * Applies what it can, and reports every operation it could not apply together with the reason.
     * Knowing which action was lost is not enough to act on it (plan 7.5): a move refused because the
     * unit is gone is a different problem from one refused because the world moved on.
     */
    fun replayOrExplain(
        gameInfo: GameInfo,
        operations: List<SimultaneousTurnOperation>
    ): List<SimultaneousTurnFailedOperation> {
        val failed = ArrayList<SimultaneousTurnFailedOperation>()
        val unitIdRemappings = HashMap<UnitIdKey, Int>()
        val maxRecordedUnitId = operations.maxOfOrNull { operation ->
            if (!SimultaneousTurnOperations.isStateOperationType(operation.type)) return@maxOfOrNull 0
            try {
                val result = json().fromJson(SimultaneousTurnGameStateResult::class.java, operation.payload)
                json().fromJson(SimultaneousTurnGlobalState::class.java, result.globalAfter).lastUnitId
            } catch (_: Exception) {
                0
            }
        } ?: 0
        if (maxRecordedUnitId > gameInfo.getLastUnitIdForSimultaneousTurns())
            gameInfo.setLastUnitIdForSimultaneousTurns(maxRecordedUnitId)
        for (operation in operations) {
            val reason = applyOrExplain(gameInfo, operation, unitIdRemappings) ?: continue
            failed.add(SimultaneousTurnFailedOperation(operation, reason))
        }
        return failed
    }

    fun apply(gameInfo: GameInfo, operation: SimultaneousTurnOperation): Boolean =
        applyOrExplain(gameInfo, operation) == null

    /** @return null when the operation was applied, otherwise the reason it was refused. */
    fun applyOrExplain(
        gameInfo: GameInfo,
        operation: SimultaneousTurnOperation
    ): SimultaneousTurnFailureReason? = applyOrExplain(gameInfo, operation, HashMap())

    private fun applyOrExplain(
        gameInfo: GameInfo,
        operation: SimultaneousTurnOperation,
        unitIdRemappings: MutableMap<UnitIdKey, Int>
    ): SimultaneousTurnFailureReason? = try {
        when (operation.type) {
            "unit.move" -> applyMove(gameInfo, json().fromJson(
                SimultaneousTurnMoveResult::class.java, operation.payload
            ), unitIdRemappings)
            "unit.attack" -> applyAttack(gameInfo, json().fromJson(
                SimultaneousTurnAttackResult::class.java, operation.payload
            ), unitIdRemappings)
            "unit.action" -> applyUnitAction(gameInfo, json().fromJson(
                SimultaneousTurnUnitActionResult::class.java, operation.payload
            ), unitIdRemappings)
            "unit.swap" -> applySwap(gameInfo, json().fromJson(
                SimultaneousTurnSwapResult::class.java, operation.payload
            ), unitIdRemappings)
            "game.state" -> applyGameState(gameInfo, json().fromJson(
                SimultaneousTurnGameStateResult::class.java, operation.payload
            ), unitIdRemappings)
            "done" -> null
            else -> if (SimultaneousTurnOperations.isStateOperationType(operation.type)) applyGameState(
                gameInfo, json().fromJson(SimultaneousTurnGameStateResult::class.java, operation.payload),
                unitIdRemappings
            ) else SimultaneousTurnFailureReason.Unreadable
        }
    } catch (e: Exception) {
        // Swallowing this silently once hid every unit operation being dropped at settlement
        // (a missing no-arg constructor made deserialization throw), so always leave a trace.
        Log.debug("Simultaneous-turn operation %s / %s failed to replay: %s", operation.type, operation.sequence, e)
        SimultaneousTurnFailureReason.Unreadable
    }

    private fun findUnit(gameInfo: GameInfo, owner: String, id: Int): MapUnit? =
        gameInfo.getCivilizationOrNull(owner)?.units?.getUnitById(id)

    private fun remappedUnitId(owner: String, id: Int, remappings: Map<UnitIdKey, Int>) =
        remappings[UnitIdKey(owner, id)] ?: id

    private fun applyMove(
        gameInfo: GameInfo,
        result: SimultaneousTurnMoveResult,
        remappings: Map<UnitIdKey, Int>
    ): SimultaneousTurnFailureReason? {
        val unit = findUnit(gameInfo, result.owner, remappedUnitId(result.owner, result.unitId, remappings))
            ?: return SimultaneousTurnFailureReason.UnitGone
        if (unit.isDestroyed) return SimultaneousTurnFailureReason.UnitGone
        // WorldMapHolder records a whole-component snapshot *and* this result for the same click, so
        // by the time this runs the snapshot may already have placed the unit on its destination.
        // All that is left to apply then is the unit's remaining hp and movement - rejecting the op
        // because the unit had "left" its recorded origin silently dropped every such move.
        val currentPosition = unit.currentTile.position
        if (currentPosition.x != result.toX || currentPosition.y != result.toY) {
            if (currentPosition.x != result.fromX || currentPosition.y != result.fromY)
                return SimultaneousTurnFailureReason.WorldChanged
            unit.movement.applyRecordedMoveToTile(gameInfo.tileMap[result.toX, result.toY])
        }
        unit.health = result.hp
        unit.currentMovement = result.movement
        return null
    }

    private fun applySwap(
        gameInfo: GameInfo,
        result: SimultaneousTurnSwapResult,
        remappings: Map<UnitIdKey, Int>
    ): SimultaneousTurnFailureReason? {
        val unit = findUnit(gameInfo, result.owner, remappedUnitId(result.owner, result.unitId, remappings))
            ?: return SimultaneousTurnFailureReason.UnitGone
        if (unit.isDestroyed) return SimultaneousTurnFailureReason.UnitGone
        val currentPosition = unit.currentTile.position
        if (currentPosition.x != result.toX || currentPosition.y != result.toY) {
            if (currentPosition.x != result.fromX || currentPosition.y != result.fromY)
                return SimultaneousTurnFailureReason.WorldChanged
            val destination = gameInfo.tileMap[result.toX, result.toY]
            if (unit.isEscorting()) {
                // A swap recorded before the recorder captured a state snapshot carries no escort data,
                // so the pair has to be re-derived from the movement rules. Swaps recorded by current
                // clients are already applied by their snapshot and never reach this branch.
                unit.movement.swapMoveToTile(destination, keepEscorting = true)
            } else {
                val otherUnit = (if (unit.isCivilian()) destination.civilianUnit else destination.militaryUnit)
                    ?: return SimultaneousTurnFailureReason.WorldChanged
                unit.movement.applyRecordedSwapTo(otherUnit)
            }
        }
        unit.health = result.health
        unit.currentMovement = result.movement
        return null
    }
    private fun applyUnitAction(
        gameInfo: GameInfo,
        result: SimultaneousTurnUnitActionResult,
        remappings: Map<UnitIdKey, Int>
    ): SimultaneousTurnFailureReason? {
        val unit = findUnit(gameInfo, result.owner, remappedUnitId(result.owner, result.unitId, remappings))
            ?: return SimultaneousTurnFailureReason.UnitGone
        if (unit.isDestroyed) return SimultaneousTurnFailureReason.UnitGone
        unit.action = result.action
        unit.automated = result.automated
        unit.due = result.due
        unit.health = result.health
        unit.currentMovement = result.movement
        if (result.escorting) unit.startEscorting() else unit.stopEscorting()
        return null
    }
    private fun applyGameState(
        gameInfo: GameInfo,
        result: SimultaneousTurnGameStateResult,
        unitIdRemappings: MutableMap<UnitIdKey, Int>
    ): SimultaneousTurnFailureReason? {
        val globalBefore = json().fromJson(
            SimultaneousTurnGlobalState::class.java, result.globalBefore
        )
        val globalAfter = json().fromJson(
            SimultaneousTurnGlobalState::class.java, result.globalAfter
        )
        val currentGlobal = SimultaneousTurnOperations.currentGlobalState(gameInfo)
        if (!globalStateCompatible(currentGlobal, globalBefore, globalAfter))
            return SimultaneousTurnFailureReason.WorldChanged
        for (change in result.luaStoreChanges) {
            if (gameInfo.modLuaStorage[change.modName]?.get(change.key) != change.before)
                return SimultaneousTurnFailureReason.WorldChanged
        }

        for (snapshot in result.civilizations) {
            val current = gameInfo.getCivilizationOrNull(snapshot.key)
                ?: return SimultaneousTurnFailureReason.WorldChanged
            val currentJson = componentJson(current, snapshot)
            if (!componentCompatible(currentJson, snapshot, civilizationFieldsMergedOnReplay))
                return SimultaneousTurnFailureReason.WorldChanged
        }
        for (snapshot in result.tiles) {
            if (SimultaneousTurnOperations.isExplorationOnly(snapshot)) continue
            val (x, y) = snapshot.key.split(',').map { it.toInt() }
            val currentJson = componentJson(gameInfo.tileMap[x, y], snapshot)
            if (!componentCompatible(currentJson, snapshot, tileFieldsMergedOnReplay, isTile = true))
                return SimultaneousTurnFailureReason.WorldChanged
        }
        for (snapshot in result.religions) {
            val current = gameInfo.religions[snapshot.key]
            val currentJson = if (current == null) null else componentJson(current, snapshot)
            if (!componentCompatible(currentJson, snapshot)) return SimultaneousTurnFailureReason.WorldChanged
        }

        for (snapshot in result.civilizations) {
            if (snapshot.after == null) return SimultaneousTurnFailureReason.WorldChanged
            val current = gameInfo.civilizations.firstOrNull { it.civName == snapshot.key }
                ?: return SimultaneousTurnFailureReason.WorldChanged
            applyCivilizationSnapshot(current, snapshot)
        }
        for (snapshot in result.tiles) {
            if (snapshot.after == null) return SimultaneousTurnFailureReason.WorldChanged
            val (x, y) = snapshot.key.split(',').map { it.toInt() }
            val tile = gameInfo.tileMap[x, y]
            val after = JsonReader().parse(snapshot.after)
            val afterExploration = after.remove(exploredByField)
            if (snapshot.changedFields.isEmpty() || exploredByField in snapshot.changedFields) {
                val beforeExploration = snapshot.before?.let { JsonReader().parse(it).get(exploredByField) }
                tile.applyRecordedExploration(exploredCivilizations(beforeExploration), exploredCivilizations(afterExploration))
            }
            if (SimultaneousTurnOperations.isExplorationOnly(snapshot)) continue
            remapUnitIdsInTileSnapshot(gameInfo, after, unitIdRemappings)
            // readFields only assigns fields present in the JSON, and Gdx does not serialize null
            // fields. A unit that left this tile would therefore stay behind as a stale duplicate
            // (and setTransients below would register it again), and e.g. a destroyed improvement
            // would resurrect. Clear omitted nullable fields only when applying a full snapshot or
            // when a field-level snapshot explicitly changed that field.
            fun shouldClear(field: String) =
                after.get(field) == null && (snapshot.changedFields.isEmpty() || field in snapshot.changedFields)
            if (shouldClear("militaryUnit")) tile.militaryUnit = null
            if (shouldClear("civilianUnit")) tile.civilianUnit = null
            if (shouldClear("airUnits")) tile.airUnits = ArrayList()
            if (shouldClear("naturalWonder")) tile.naturalWonder = null
            if (shouldClear("improvement")) tile.improvement = null
            if (shouldClear("improvementCreatedByCreatesOneImprovement"))
                tile.improvementCreatedByCreatesOneImprovement = null
            if (shouldClear("resource")) tile.tileResource = null
            json().readFields(tile, after)
        }
        for (snapshot in result.religions) {
            if (snapshot.after == null) gameInfo.religions.remove(snapshot.key)
            else {
                val current = gameInfo.religions[snapshot.key]
                if (current == null)
                    gameInfo.religions[snapshot.key] = json().fromJson(Religion::class.java, snapshot.after)
                else readComponentFields(current, snapshot)
            }
        }
        // Only write back the fields this operation actually changed. Writing all of them
        // unconditionally would roll back fields that an earlier operation in the same settlement
        // pass had already changed (they are unchanged from this operation's point of view).
        if (globalBefore.diplomaticVictoryVotesCast != globalAfter.diplomaticVictoryVotesCast) {
            gameInfo.diplomaticVictoryVotesCast.clear()
            gameInfo.diplomaticVictoryVotesCast.putAll(globalAfter.diplomaticVictoryVotesCast)
        }
        if (globalBefore.unitNamesTaken != globalAfter.unitNamesTaken) {
            gameInfo.unitNamesTaken.clear()
            gameInfo.unitNamesTaken.addAll(globalAfter.unitNamesTaken)
        }
        if (globalBefore.variables != globalAfter.variables) {
            gameInfo.variables.clear()
            gameInfo.variables.putAll(globalAfter.variables)
        }
        if (globalBefore.lastUnitId != globalAfter.lastUnitId)
            gameInfo.setLastUnitIdForSimultaneousTurns(
                maxOf(gameInfo.getLastUnitIdForSimultaneousTurns(), globalAfter.lastUnitId)
            )
        for (change in result.luaStoreChanges) {
            if (change.after != null) {
                gameInfo.modLuaStorage.getOrPut(change.modName) { HashMap() }[change.key] = change.after
            } else {
                val store = gameInfo.modLuaStorage[change.modName] ?: continue
                store.remove(change.key)
                if (store.isEmpty()) gameInfo.modLuaStorage.remove(change.modName)
            }
        }
        // A tile snapshot replaces the MapUnit instances stored in Tile.*Unit, while each civ's unit
        // list still references the pre-replay objects. setTransients() rebuilds those lists from the
        // map, but UnitManager.addUnit() appends - so discard the stale lists first, otherwise every
        // replayed unit change leaves a duplicate behind and later lookups hit the wrong instance.
        for (civ in gameInfo.civilizations) civ.units.clearUnits()
        gameInfo.setTransients()
        for (snapshot in result.civilizations) updateRecordedProximities(gameInfo, snapshot)
        return null
    }

    private fun exploredCivilizations(value: JsonValue?): Set<String> =
        if (value == null || value.isNull) emptySet() else value.mapTo(HashSet()) { it.asString() }

    /**
     * A full component snapshot is compatible when the current JSON equals its recorded before or
     * after value. A field-level snapshot only guards the fields it carries, so an unrelated field
     * that another player's operation already moved no longer rejects the whole component. Every
     * carried field must still hold either its before or its after value, which keeps a real conflict
     * (a third value) rejected - and makes writing all of them back safe.
     */
    private fun componentCompatible(
        currentJson: String?,
        snapshot: SimultaneousTurnComponentSnapshot,
        ignoredFields: Set<String> = emptySet(),
        isTile: Boolean = false
    ): Boolean {
        if (snapshot.changedFields.isEmpty() && ignoredFields.isEmpty())
            return currentJson == snapshot.before || currentJson == snapshot.after
        if (currentJson == null) return false
        val current = JsonReader().parse(currentJson)
        val before = snapshot.before?.let { JsonReader().parse(it) }
        val after = snapshot.after?.let { JsonReader().parse(it) }
        if (isTile) {
            // Cycling units can update this flag locally after a move was recorded. A tile
            // precondition must check the unit's game state, not whether it was cycled past.
            for (tile in listOfNotNull(current, before, after))
                for (unit in tileUnits(tile)) unit.remove("due")
        }
        if (snapshot.changedFields.isEmpty()) {
            for (field in ignoredFields) {
                current.remove(field)
                before?.remove(field)
                after?.remove(field)
            }
            return jsonValueEquals(current, before) || jsonValueEquals(current, after)
        }
        return snapshot.changedFields.all { field ->
            if (field in ignoredFields) return@all true
            val value = current.get(field)
            jsonValueEquals(value, before?.get(field)) || jsonValueEquals(value, after?.get(field))
        }
    }

    private fun applyCivilizationSnapshot(civ: Civilization, snapshot: SimultaneousTurnComponentSnapshot) {
        val after = JsonReader().parse(snapshot.after)
        after.remove(proximityField)
        val afterAlerts = after.remove(popupAlertsField)
        if (snapshot.changedFields.isEmpty() || popupAlertsField in snapshot.changedFields) {
            val beforeAlerts = snapshot.before?.let { JsonReader().parse(it).get(popupAlertsField) }
            mergePopupAlerts(civ.popupAlerts, beforeAlerts, afterAlerts)
        }
        json().readFields(civ, after)
    }

    private fun updateRecordedProximities(gameInfo: GameInfo, snapshot: SimultaneousTurnComponentSnapshot) {
        if (snapshot.changedFields.isNotEmpty() && proximityField !in snapshot.changedFields) return
        val before = snapshot.before?.let { JsonReader().parse(it).get(proximityField) }
        val after = snapshot.after?.let { JsonReader().parse(it).get(proximityField) }
        if (jsonValueEquals(before, after)) return
        val civ = gameInfo.getCivilization(snapshot.key)
        val peers = before?.map { it.name }.orEmpty().toSet() + after?.map { it.name }.orEmpty()
        // Another player may have founded a city since this result was recorded. Use the
        // city locations on the combined board instead of copying stale proximity rankings.
        for (peerId in peers) {
            if (jsonValueEquals(before?.get(peerId), after?.get(peerId))) continue
            val peer = gameInfo.getCivilizationOrNull(peerId) ?: continue
            civ.updateProximity(peer)
        }
    }

    /** Each player dismisses popups locally. Transfer the operation's edits without restoring old popups. */
    private fun mergePopupAlerts(current: ArrayList<PopupAlert>, before: JsonValue?, after: JsonValue?) {
        val serializer = json()
        val beforeAlerts = before?.map { serializer.readValue(PopupAlert::class.java, it) }.orEmpty()
        val afterAlerts = after?.map { serializer.readValue(PopupAlert::class.java, it) }.orEmpty()
        fun PopupAlert.key() = type to value
        val beforeCounts = beforeAlerts.groupingBy { it.key() }.eachCount()
        val afterCounts = afterAlerts.groupingBy { it.key() }.eachCount()
        val currentCounts = current.groupingBy { it.key() }.eachCount().toMutableMap()
        val iterator = current.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next().key()
            val count = currentCounts.getValue(key)
            val afterCount = afterCounts[key] ?: 0
            if ((beforeCounts[key] ?: 0) > afterCount && count > afterCount) {
                iterator.remove()
                currentCounts[key] = count - 1
            }
        }
        // Keep the recorded order of new alerts, skipping both pre-existing entries and additions
        // that are already present. If a player dismissed duplicates, add only the new copies.
        val toSkip = beforeCounts.toMutableMap()
        for ((key, count) in currentCounts) toSkip[key] = maxOf(count, toSkip[key] ?: 0)
        for (alert in afterAlerts) {
            val key = alert.key()
            val skip = toSkip[key] ?: 0
            if (skip > 0) toSkip[key] = skip - 1
            else current.add(alert)
        }
    }

    /**
     * Compares two fields of a partially-serialized component. An absent field and a JSON `null` are
     * the same thing here, because libgdx omits fields that still hold their prototype value (an empty
     * collection or map) - so "the field was not in the document" is the ordinary shape of "it was
     * empty", not a distinguishable value.
     */
    private fun jsonValueEquals(first: JsonValue?, second: JsonValue?): Boolean {
        val firstValue = comparableJsonValue(first)
        val secondValue = comparableJsonValue(second)
        if (firstValue == null || secondValue == null) return firstValue == null && secondValue == null
        if (firstValue.type() != secondValue.type()) return false
        if (firstValue.isObject) {
            if (firstValue.size != secondValue.size) return false
            val byName = secondValue.associateBy { it.name }
            return firstValue.all { it.name in byName && jsonValueEquals(it, byName[it.name]) }
        }
        if (firstValue.isArray) {
            if (firstValue.size != secondValue.size) return false
            return firstValue.zip(secondValue).all { (a, b) -> jsonValueEquals(a, b) }
        }
        return firstValue.toJson(JsonWriter.OutputType.json) == secondValue.toJson(JsonWriter.OutputType.json)
    }

    private fun comparableJsonValue(value: JsonValue?): JsonValue? {
        if (value == null || value.isNull) return null
        // The last city's removal or a save reload can leave an immutable empty List, while
        // Civilization.clone() uses an ArrayList. LibGDX writes type metadata only for the former.
        if (value.isObject && value.size == 2 && value.getString("class", "") == "kotlin.collections.EmptyList") {
            val items = value.get("items")
            if (items != null && items.isArray && items.size == 0) return items
        }
        return value
    }

    /**
     * A global-state operation may only be applied while the fields it actually changes still hold
     * their before-value. Fields it does not touch may already have been changed by another
     * player's operation replayed earlier in this settlement pass, so they must not invalidate this
     * one -- the previous "current != before && current != after" check rejected the second of two
     * same-turn global operations, and WorldScreen's `check(failedOperations.isEmpty())` then
     * aborted the turn forever.
     */
    private fun globalStateCompatible(
        current: SimultaneousTurnGlobalState,
        before: SimultaneousTurnGlobalState,
        after: SimultaneousTurnGlobalState
    ): Boolean {
        if (after.lastUnitId < before.lastUnitId || current.lastUnitId < before.lastUnitId) return false
        if (before.diplomaticVictoryVotesCast != after.diplomaticVictoryVotesCast &&
            current.diplomaticVictoryVotesCast != before.diplomaticVictoryVotesCast) return false
        if (before.unitNamesTaken != after.unitNamesTaken && current.unitNamesTaken != before.unitNamesTaken) return false
        if (before.variables != after.variables && current.variables != before.variables) return false
        return true
    }

    private fun applyAttack(
        gameInfo: GameInfo,
        result: SimultaneousTurnAttackResult,
        remappings: Map<UnitIdKey, Int>
    ): SimultaneousTurnFailureReason? {
        val attackerId = remappedUnitId(result.attackerOwner, result.attackerId, remappings)
        val attacker = if (result.attackerOwner.isEmpty()) findAnyUnit(gameInfo, attackerId)
            else findUnit(gameInfo, result.attackerOwner, attackerId)
        val target = findUnit(
            gameInfo, result.targetOwner, remappedUnitId(result.targetOwner, result.targetId, remappings)
        )
        if (attacker == null || target == null) return SimultaneousTurnFailureReason.UnitGone
        if (attacker.isDestroyed || target.isDestroyed) return SimultaneousTurnFailureReason.UnitGone
        if (target.currentTile.position.x != result.targetX || target.currentTile.position.y != result.targetY)
            return SimultaneousTurnFailureReason.WorldChanged

        attacker.health = result.attackerHp
        if (result.targetHp <= 0) target.destroy()
        else target.health = result.targetHp
        return null
    }

    private fun findAnyUnit(gameInfo: GameInfo, id: Int): MapUnit? =
        gameInfo.civilizations.asSequence()
            .mapNotNull { it.units.getUnitById(id) }
            .firstOrNull()

    private fun componentJson(component: Any, snapshot: SimultaneousTurnComponentSnapshot): String =
        if (snapshot.changedFields.isEmpty()) json().toJson(component)
        else {
            val current = JsonReader().parse(json().toJson(component))
            for (field in snapshot.changedFields) {
                if (current.get(field) != null) continue
                val exactValue = serializeField(component, field) ?: continue
                current.addChild(field, JsonValue(exactValue))
            }
            current.toJson(JsonWriter.OutputType.json)
        }

    private fun serializeField(component: Any, field: String): JsonValue? {
        val writer = StringWriter()
        val fieldJson = json()
        fieldJson.setWriter(writer)
        fieldJson.writeObjectStart()
        fieldJson.writeField(component, field)
        fieldJson.writeObjectEnd()
        return JsonReader().parse(writer.toString()).get(field)
    }

    private fun readComponentFields(target: Any, snapshot: SimultaneousTurnComponentSnapshot) {
        val after = snapshot.after ?: return
        json().readFields(target, JsonReader().parse(after))
    }

    private fun tileUnits(tile: JsonValue): Sequence<JsonValue> = sequence {
        for (field in listOf("militaryUnit", "civilianUnit")) {
            val unit = tile.get(field)
            if (unit != null && unit.isObject) yield(unit)
        }
        val airUnits = tile.get("airUnits")
        if (airUnits != null && airUnits.isArray)
            for (unit in airUnits) yield(unit)
    }

    private fun remapUnitIdsInTileSnapshot(
        gameInfo: GameInfo,
        tile: JsonValue,
        remappings: MutableMap<UnitIdKey, Int>
    ) {
        fun remap(unit: JsonValue) {
            val owner = unit.getString("owner", "")
            if (owner.isEmpty()) return
            val originalId = unit.getInt("id", Constants.NO_ID)
            if (originalId == Constants.NO_ID) return
            val key = UnitIdKey(owner, originalId)
            val mapped = remappings[key]
            val existingUnit = findUnit(gameInfo, owner, mapped ?: originalId)
            if (existingUnit != null) {
                // The unit list still describes the board before this snapshot, even when the
                // snapshot moves a unit. Keep the local flag; explicit unit.action can change it.
                unit.remove("due")
                if (!existingUnit.due) unit.addChild("due", JsonValue(false))
            }
            if (mapped != null) {
                unit.remove("id")
                unit.addChild("id", JsonValue(mapped.toLong()))
                return
            }
            val existing = findAnyUnit(gameInfo, originalId)
            if (existing == null || existing.owner == owner) return
            // All clients start at the same allocator value; reserve the recorded range first so
            // remapping one player's first new unit cannot steal another of their later IDs.
            val remappedId = gameInfo.getNextUnitId()
            remappings[key] = remappedId
            unit.remove("id")
            unit.addChild("id", JsonValue(remappedId.toLong()))
        }

        for (unit in tileUnits(tile)) remap(unit)
    }
}

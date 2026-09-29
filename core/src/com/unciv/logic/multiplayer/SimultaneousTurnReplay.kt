package com.unciv.logic.multiplayer

import com.badlogic.gdx.utils.JsonReader
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.models.Religion
import com.unciv.utils.Log

/** Applies recorded results to the turn-start game without re-running random combat. */
object SimultaneousTurnReplay {
    fun replay(gameInfo: GameInfo, operations: List<SimultaneousTurnOperation>): List<SimultaneousTurnOperation> {
        val failed = ArrayList<SimultaneousTurnOperation>()
        for (operation in operations) {
            if (!apply(gameInfo, operation)) failed.add(operation)
        }
        return failed
    }

    fun apply(gameInfo: GameInfo, operation: SimultaneousTurnOperation): Boolean = try {
        when (operation.type) {
            "unit.move" -> applyMove(gameInfo, json().fromJson(
                SimultaneousTurnMoveResult::class.java, operation.payload
            ))
            "unit.attack" -> applyAttack(gameInfo, json().fromJson(
                SimultaneousTurnAttackResult::class.java, operation.payload
            ))
            "unit.action" -> applyUnitAction(gameInfo, json().fromJson(
                SimultaneousTurnUnitActionResult::class.java, operation.payload
            ))
            "unit.swap" -> applySwap(gameInfo, json().fromJson(
                SimultaneousTurnSwapResult::class.java, operation.payload
            ))
            "game.state" -> applyGameState(gameInfo, json().fromJson(
                SimultaneousTurnGameStateResult::class.java, operation.payload
            ))
            "done" -> true
            else -> false
        }
    } catch (e: Exception) {
        // Swallowing this silently once hid every unit operation being dropped at settlement
        // (a missing no-arg constructor made deserialization throw), so always leave a trace.
        Log.debug("Simultaneous-turn operation %s / %s failed to replay: %s", operation.type, operation.sequence, e)
        false
    }

    private fun findUnit(gameInfo: GameInfo, owner: String, id: Int): MapUnit? =
        gameInfo.getCivilizationOrNull(owner)?.units?.getUnitById(id)

    private fun applyMove(gameInfo: GameInfo, result: SimultaneousTurnMoveResult): Boolean {
        val unit = findUnit(gameInfo, result.owner, result.unitId) ?: return false
        if (unit.isDestroyed) return false
        // WorldMapHolder records a whole-component snapshot *and* this result for the same click, so
        // by the time this runs the snapshot may already have placed the unit on its destination.
        // All that is left to apply then is the unit's remaining hp and movement - rejecting the op
        // because the unit had "left" its recorded origin silently dropped every such move.
        val currentPosition = unit.currentTile.position
        if (currentPosition.x != result.toX || currentPosition.y != result.toY) {
            if (currentPosition.x != result.fromX || currentPosition.y != result.fromY) return false
            unit.movement.applyRecordedMoveToTile(gameInfo.tileMap[result.toX, result.toY])
        }
        unit.health = result.hp
        unit.currentMovement = result.movement
        return true
    }

    private fun applySwap(gameInfo: GameInfo, result: SimultaneousTurnSwapResult): Boolean {
        val unit = findUnit(gameInfo, result.owner, result.unitId) ?: return false
        if (unit.isDestroyed) return false
        val currentPosition = unit.currentTile.position
        if (currentPosition.x != result.toX || currentPosition.y != result.toY) {
            if (currentPosition.x != result.fromX || currentPosition.y != result.fromY) return false
            val destination = gameInfo.tileMap[result.toX, result.toY]
            if (unit.isEscorting()) {
                // An escort pair swaps as a pair; that path still re-runs the movement rules.
                unit.movement.swapMoveToTile(destination, keepEscorting = true)
            } else {
                val otherUnit = (if (unit.isCivilian()) destination.civilianUnit else destination.militaryUnit)
                    ?: return false
                unit.movement.applyRecordedSwapTo(otherUnit)
            }
        }
        unit.health = result.health
        unit.currentMovement = result.movement
        return true
    }
    private fun applyUnitAction(gameInfo: GameInfo, result: SimultaneousTurnUnitActionResult): Boolean {
        val unit = findUnit(gameInfo, result.owner, result.unitId) ?: return false
        if (unit.isDestroyed) return false
        unit.action = result.action
        unit.automated = result.automated
        unit.due = result.due
        unit.health = result.health
        unit.currentMovement = result.movement
        if (result.escorting) unit.startEscorting() else unit.stopEscorting()
        return true
    }
    private fun applyGameState(gameInfo: GameInfo, result: SimultaneousTurnGameStateResult): Boolean {
        val globalBefore = json().fromJson(
            SimultaneousTurnGlobalState::class.java, result.globalBefore
        )
        val globalAfter = json().fromJson(
            SimultaneousTurnGlobalState::class.java, result.globalAfter
        )
        val currentGlobal = SimultaneousTurnOperations.currentGlobalState(gameInfo)
        if (!globalStateCompatible(currentGlobal, globalBefore, globalAfter)) return false

        for (snapshot in result.civilizations) {
            val current = gameInfo.getCivilizationOrNull(snapshot.key) ?: return false
            val currentJson = json().toJson(current)
            if (currentJson != snapshot.before && currentJson != snapshot.after) return false
        }
        for (snapshot in result.tiles) {
            val (x, y) = snapshot.key.split(',').map { it.toInt() }
            val currentJson = json().toJson(gameInfo.tileMap[x, y])
            if (currentJson != snapshot.before && currentJson != snapshot.after) return false
        }
        for (snapshot in result.religions) {
            val current = gameInfo.religions[snapshot.key]
            val currentJson = if (current == null) null else json().toJson(current)
            if (currentJson != snapshot.before && currentJson != snapshot.after) return false
        }

        for (snapshot in result.civilizations) {
            if (snapshot.after == null) return false
            val current = gameInfo.civilizations.firstOrNull { it.civName == snapshot.key } ?: return false
            json().readFields(current, JsonReader().parse(snapshot.after))
        }
        for (snapshot in result.tiles) {
            if (snapshot.after == null) return false
            val (x, y) = snapshot.key.split(',').map { it.toInt() }
            val tile = gameInfo.tileMap[x, y]
            // readFields only assigns fields present in the JSON, and Gdx does not serialize null
            // fields. A unit that left this tile would therefore stay behind as a stale duplicate
            // (and setTransients below would register it again), and e.g. a destroyed improvement
            // would resurrect. Clear every nullable serialized field first.
            tile.militaryUnit = null
            tile.civilianUnit = null
            tile.airUnits = ArrayList()
            tile.naturalWonder = null
            tile.improvement = null
            tile.improvementCreatedByCreatesOneImprovement = null
            tile.tileResource = null
            json().readFields(tile, JsonReader().parse(snapshot.after))
        }
        for (snapshot in result.religions) {
            if (snapshot.after == null) gameInfo.religions.remove(snapshot.key)
            else {
                val current = gameInfo.religions[snapshot.key]
                if (current == null)
                    gameInfo.religions[snapshot.key] = json().fromJson(Religion::class.java, snapshot.after)
                else json().readFields(current, JsonReader().parse(snapshot.after))
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
            gameInfo.setLastUnitIdForSimultaneousTurns(globalAfter.lastUnitId)
        // A tile snapshot replaces the MapUnit instances stored in Tile.*Unit, while each civ's unit
        // list still references the pre-replay objects. setTransients() rebuilds those lists from the
        // map, but UnitManager.addUnit() appends - so discard the stale lists first, otherwise every
        // replayed unit change leaves a duplicate behind and later lookups hit the wrong instance.
        for (civ in gameInfo.civilizations) civ.units.clearUnits()
        gameInfo.setTransients()
        return true
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
        if (before.lastUnitId != after.lastUnitId && current.lastUnitId != before.lastUnitId) return false
        if (before.diplomaticVictoryVotesCast != after.diplomaticVictoryVotesCast &&
            current.diplomaticVictoryVotesCast != before.diplomaticVictoryVotesCast) return false
        if (before.unitNamesTaken != after.unitNamesTaken && current.unitNamesTaken != before.unitNamesTaken) return false
        if (before.variables != after.variables && current.variables != before.variables) return false
        return true
    }

    private fun applyAttack(gameInfo: GameInfo, result: SimultaneousTurnAttackResult): Boolean {
        val attacker = findAnyUnit(gameInfo, result.attackerId) ?: return false
        val target = findUnit(gameInfo, result.targetOwner, result.targetId) ?: return false
        if (attacker.isDestroyed || target.isDestroyed
            || target.currentTile.position.x != result.targetX
            || target.currentTile.position.y != result.targetY
        ) return false

        attacker.health = result.attackerHp
        if (result.targetHp <= 0) target.destroy()
        else target.health = result.targetHp
        return true
    }

    private fun findAnyUnit(gameInfo: GameInfo, id: Int): MapUnit? =
        gameInfo.civilizations.asSequence()
            .mapNotNull { it.units.getUnitById(id) }
            .firstOrNull()
}

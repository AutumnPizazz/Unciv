package com.unciv.logic.multiplayer

import com.badlogic.gdx.utils.JsonReader
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.models.Religion

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
    } catch (_: Exception) {
        false
    }

    private fun findUnit(gameInfo: GameInfo, owner: String, id: Int): MapUnit? =
        gameInfo.getCivilizationOrNull(owner)?.units?.getUnitById(id)

    private fun applyMove(gameInfo: GameInfo, result: SimultaneousTurnMoveResult): Boolean {
        val unit = findUnit(gameInfo, result.owner, result.unitId) ?: return false
        if (unit.isDestroyed || unit.currentTile.position.x != result.fromX || unit.currentTile.position.y != result.fromY)
            return false
        val destination = gameInfo.tileMap[result.toX, result.toY]
        unit.movement.moveToTile(destination)
        if (unit.isDestroyed || unit.currentTile.position.x != result.toX || unit.currentTile.position.y != result.toY)
            return false
        unit.health = result.hp
        unit.currentMovement = result.movement
        return true
    }

    private fun applySwap(gameInfo: GameInfo, result: SimultaneousTurnSwapResult): Boolean {
        val unit = findUnit(gameInfo, result.owner, result.unitId) ?: return false
        if (unit.isDestroyed || unit.currentTile.position.x != result.fromX || unit.currentTile.position.y != result.fromY)
            return false
        val destination = gameInfo.tileMap[result.toX, result.toY]
        unit.movement.swapMoveToTile(destination, keepEscorting = true)
        if (unit.isDestroyed || unit.currentTile.position.x != result.toX || unit.currentTile.position.y != result.toY)
            return false
        unit.health = result.health
        unit.currentMovement = result.movement
        return true
    }
    private fun applyUnitAction(gameInfo: GameInfo, result: SimultaneousTurnUnitActionResult): Boolean {
        val unit = findUnit(gameInfo, result.owner, result.unitId) ?: return false
        if (unit.isDestroyed) return false
        unit.action = result.action
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
        if (currentGlobal != globalBefore && currentGlobal != globalAfter) return false

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
            json().readFields(gameInfo.tileMap[x, y], JsonReader().parse(snapshot.after))
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
        gameInfo.diplomaticVictoryVotesCast.clear()
        gameInfo.diplomaticVictoryVotesCast.putAll(globalAfter.diplomaticVictoryVotesCast)
        gameInfo.unitNamesTaken.clear()
        gameInfo.unitNamesTaken.addAll(globalAfter.unitNamesTaken)
        gameInfo.variables.clear()
        gameInfo.variables.putAll(globalAfter.variables)
        gameInfo.setLastUnitIdForSimultaneousTurns(globalAfter.lastUnitId)
        gameInfo.setTransients()
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

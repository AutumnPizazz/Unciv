package com.unciv.logic.multiplayer

import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.map.mapunit.MapUnit

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

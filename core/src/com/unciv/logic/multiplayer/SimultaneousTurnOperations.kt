package com.unciv.logic.multiplayer

import com.unciv.json.json
import com.unciv.logic.multiplayer.storage.MultiplayerFileNotFoundException
import com.unciv.logic.multiplayer.storage.MultiplayerServer

/** A client-produced operation for a simultaneous multiplayer turn. */
data class SimultaneousTurnOperation(
    val turn: Int = 0,
    val playerId: String = "",
    val sequence: Long = 0,
    val type: String = "",
    val payload: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class SimultaneousTurnMoveResult(
    val unitId: Int,
    val owner: String,
    val fromX: Int,
    val fromY: Int,
    val toX: Int,
    val toY: Int,
    val hp: Int,
    val movement: Float
)

data class SimultaneousTurnAttackResult(
    val attackerId: Int,
    val targetId: Int,
    val targetOwner: String,
    val attackerHp: Int,
    val targetHp: Int,
    val targetX: Int,
    val targetY: Int
)


data class SimultaneousTurnUnitActionResult(
    val unitId: Int,
    val owner: String,
    val action: String?,
    val due: Boolean,
    val health: Int,
    val movement: Float
)

object SimultaneousTurnOperations {
    private fun fileName(gameId: String) = "${gameId}.ops"

    fun merge(existing: List<SimultaneousTurnOperation>, incoming: List<SimultaneousTurnOperation>): List<SimultaneousTurnOperation> =
        (existing + incoming)
            .distinctBy { Triple(it.turn, it.playerId, it.sequence) }
            .sortedWith(compareBy<SimultaneousTurnOperation> { it.turn }.thenBy { it.playerId }.thenBy { it.sequence })

    fun encode(operations: List<SimultaneousTurnOperation>): String = json().toJson(operations)

    fun decode(data: String): List<SimultaneousTurnOperation> =
        json().fromJson(Array<SimultaneousTurnOperation>::class.java, data).toList()

    suspend fun upload(
        server: MultiplayerServer,
        gameId: String,
        operations: List<SimultaneousTurnOperation>
    ) {
        if (operations.isEmpty()) return
        val old = try {
            download(server, gameId)
        } catch (_: MultiplayerFileNotFoundException) {
            emptyList()
        }
        server.fileStorage().saveFileData(fileName(gameId), encode(merge(old, operations)))
    }

    suspend fun download(server: MultiplayerServer, gameId: String): List<SimultaneousTurnOperation> =
        decode(server.fileStorage().loadFileData(fileName(gameId)))
}

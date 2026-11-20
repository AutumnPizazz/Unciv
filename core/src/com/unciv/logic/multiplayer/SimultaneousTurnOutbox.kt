package com.unciv.logic.multiplayer

import com.badlogic.gdx.files.FileHandle
import java.io.IOException
import java.util.UUID

/** Stores local action batches until they can be checked against the server log. */
class SimultaneousTurnOutbox(
    gameDirectory: FileHandle,
    private val turn: Int,
    turnStart: Long,
    private val playerId: String
) {
    private val playerDirectory = gameDirectory.child(UUID.nameUUIDFromBytes(playerId.toByteArray(Charsets.UTF_8)).toString())
    private val turnDirectory = playerDirectory.child("${turn}_$turnStart")

    fun record(operations: List<SimultaneousTurnOperation>) {
        if (operations.isEmpty()) return
        require(operations.all { it.turn == turn && it.playerId == playerId && it.type != "done" })
        val name = UUID.randomUUID().toString()
        val staged = turnDirectory.child("$name.tmp")
        staged.writeString(SimultaneousTurnOperations.encode(operations), false, Charsets.UTF_8.name())
        if (!staged.file().renameTo(turnDirectory.child("$name.json").file()))
            throw IOException("Could not commit a simultaneous-turn action batch")
    }

    /** Only operates on the player and round of the downloaded turn-start archive. */
    suspend fun recover(
        serverOperations: List<SimultaneousTurnOperation>,
        serverReady: Boolean = false,
        upload: suspend (List<SimultaneousTurnOperation>) -> Unit
    ): Recovery {
        val server = serverOperations.filter { it.turn == turn && it.playerId == playerId }
        val submitted = serverReady || server.any { it.type == "done" }
        val files = turnDirectory.list(".json")
        val batches = files.map { SimultaneousTurnOperations.decode(it.readString(Charsets.UTF_8.name())) }
        val local = batches.flatten()
        check(local.all { it.turn == turn && it.playerId == playerId && it.type != "done" })
        val serverBySequence = server.associateBy { it.sequence }
        val missing = local.filter { it.sequence !in serverBySequence }
        val hasConflicts = local.any {
            val persisted = serverBySequence[it.sequence]
            persisted != null && (persisted.type != it.type || persisted.payload != it.payload)
        }
        // A completed submission may already be settling on another client. Do not extend it.
        if (submitted) return Recovery(server, hasConflicts || missing.isNotEmpty(), missing)

        val operations = SimultaneousTurnOperations.merge(server, missing)
        for (batch in batches.sortedBy { it.minOfOrNull { operation -> operation.sequence } ?: Long.MAX_VALUE }) {
            val pending = batch.filter { it.sequence !in serverBySequence }
            if (pending.isNotEmpty()) upload(pending)
        }
        return Recovery(operations, hasConflicts)
    }

    fun discardOlderTurns() {
        for (directory in playerDirectory.list()) {
            val recordedTurn = directory.name().substringBefore('_').toIntOrNull() ?: continue
            if (recordedTurn < turn) directory.deleteDirectory()
        }
    }

    fun discardRejectedOperations(sequences: Set<Long>) {
        if (sequences.isEmpty()) return
        for (file in turnDirectory.list(".json")) {
            val operations = SimultaneousTurnOperations.decode(file.readString(Charsets.UTF_8.name()))
            if (operations.none { it.sequence in sequences }) continue
            val remaining = operations.filterNot { it.sequence in sequences }
            if (remaining.isEmpty()) {
                file.delete()
                continue
            }
            val staged = turnDirectory.child("${file.nameWithoutExtension()}.tmp")
            staged.writeString(SimultaneousTurnOperations.encode(remaining), false, Charsets.UTF_8.name())
            if (!staged.file().renameTo(file.file())) {
                staged.delete()
                throw IOException("Could not update a rejected simultaneous-turn action batch")
            }
        }
    }

    data class Recovery(
        val operations: List<SimultaneousTurnOperation>,
        val hasConflicts: Boolean,
        val rejectedOperations: List<SimultaneousTurnOperation> = emptyList()
    )
}

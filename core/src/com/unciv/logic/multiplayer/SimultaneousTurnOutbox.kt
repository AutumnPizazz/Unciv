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
    private val discardMarker = turnDirectory.child("discard-from")

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
        upload: suspend (SimultaneousTurnOperation) -> Unit
    ): Recovery {
        val interruptedDiscard = discardMarker.exists()
        if (interruptedDiscard) finishDiscard(discardMarker.readString(Charsets.UTF_8.name()).toLong())
        val server = serverOperations.filter { it.turn == turn && it.playerId == playerId }
        val submitted = serverReady || server.any { it.type == "done" }
        val local = turnDirectory.list(".json").flatMap { readOperations(it) }.sortedBy { it.sequence }
        check(local.all { it.turn == turn && it.playerId == playerId && it.type != "done" })
        val serverBySequence = server.associateBy { it.sequence }
        val pending = local.filter {
            val persisted = serverBySequence[it.sequence]
            persisted == null || !persisted.sameAction(it)
        }

        val uploaded = ArrayList<SimultaneousTurnOperation>()
        for (operation in pending) {
            // A submitted player cannot extend this turn. A reused sequence also invalidates the local suffix.
            if (submitted || operation.sequence in serverBySequence) {
                discardFrom(operation.sequence)
                return Recovery(SimultaneousTurnOperations.merge(server, uploaded), hasConflicts = true)
            }
            try {
                upload(operation)
                uploaded += operation
            } catch (ex: SimultaneousTurnOperationRejectedException) {
                discardFrom(operation.sequence)
                return Recovery(SimultaneousTurnOperations.merge(server, uploaded), hasConflicts = true)
            }
        }
        return Recovery(SimultaneousTurnOperations.merge(server, uploaded), hasConflicts = interruptedDiscard)
    }

    fun discardOlderTurns() {
        for (directory in playerDirectory.list()) {
            val recordedTurn = directory.name().substringBefore('_').toIntOrNull() ?: continue
            if (recordedTurn < turn) directory.deleteDirectory()
        }
    }

    /** Keeps the rejected sequence on disk until every dependent journal entry has been removed. */
    fun discardFrom(sequence: Long) {
        val staged = turnDirectory.child("discard-from.tmp")
        staged.writeString(sequence.toString(), false, Charsets.UTF_8.name())
        if (!staged.file().renameTo(discardMarker.file()))
            throw IOException("Could not persist a rejected simultaneous-turn sequence")
        finishDiscard(sequence)
    }

    private fun finishDiscard(sequence: Long) {
        for (file in turnDirectory.list(".json")) {
            val operations = readOperations(file)
            val remaining = operations.filter { it.sequence < sequence }
            if (remaining.size == operations.size) continue
            if (remaining.isEmpty()) {
                if (!file.delete()) throw IOException("Could not remove a rejected simultaneous-turn action batch")
                continue
            }
            val staged = turnDirectory.child("${file.nameWithoutExtension()}.tmp")
            staged.writeString(SimultaneousTurnOperations.encode(remaining), false, Charsets.UTF_8.name())
            if (!staged.file().renameTo(file.file())) {
                staged.delete()
                throw IOException("Could not update a rejected simultaneous-turn action batch")
            }
        }
        if (!discardMarker.delete()) throw IOException("Could not clear a rejected simultaneous-turn sequence")
    }

    private fun readOperations(file: FileHandle) =
        SimultaneousTurnOperations.decode(file.readString(Charsets.UTF_8.name()))

    data class Recovery(
        val operations: List<SimultaneousTurnOperation>,
        val hasConflicts: Boolean
    )
}

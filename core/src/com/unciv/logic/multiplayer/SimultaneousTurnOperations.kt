package com.unciv.logic.multiplayer

import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.multiplayer.storage.MultiplayerFileNotFoundException
import com.unciv.logic.multiplayer.storage.MultiplayerServer
import com.unciv.models.UnitActionType
import com.unciv.utils.Log

/** A client-produced operation for a simultaneous multiplayer turn. */
data class SimultaneousTurnOperation(
    val turn: Int = 0,
    val playerId: String = "",
    val sequence: Long = 0,
    val type: String = "",
    val payload: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

// Note: every parameter needs a default value so Kotlin emits a no-arg constructor -
// libgdx Json cannot instantiate these classes otherwise, which made settlement silently
// drop every recorded unit operation (see the round-trip tests in SimultaneousTurnOperationsTest).
data class SimultaneousTurnMoveResult(
    val unitId: Int = 0,
    val owner: String = "",
    val fromX: Int = 0,
    val fromY: Int = 0,
    val toX: Int = 0,
    val toY: Int = 0,
    val hp: Int = 0,
    val movement: Float = 0f
)

data class SimultaneousTurnAttackResult(
    val attackerId: Int = 0,
    val targetId: Int = 0,
    val targetOwner: String = "",
    val attackerHp: Int = 0,
    val targetHp: Int = 0,
    val targetX: Int = 0,
    val targetY: Int = 0
)


data class SimultaneousTurnUnitActionResult(
    val unitId: Int = 0,
    val owner: String = "",
    val action: String? = null,
    val due: Boolean = false,
    val health: Int = 0,
    val movement: Float = 0f,
    val escorting: Boolean = false,
    val automated: Boolean = false
)

data class SimultaneousTurnSwapResult(
    val unitId: Int = 0,
    val owner: String = "",
    val fromX: Int = 0,
    val fromY: Int = 0,
    val toX: Int = 0,
    val toY: Int = 0,
    val health: Int = 0,
    val movement: Float = 0f
)

data class SimultaneousTurnComponentSnapshot(
    val key: String = "",
    val before: String? = null,
    val after: String? = null
)

data class SimultaneousTurnGlobalState(
    val lastUnitId: Int = 0,
    val diplomaticVictoryVotesCast: Map<String, String?> = HashMap(),
    val unitNamesTaken: List<String> = ArrayList(),
    val variables: Map<String, Int> = HashMap()
)

data class SimultaneousTurnGameStateResult(
    val action: String = "",
    val globalBefore: String = "",
    val globalAfter: String = "",
    val civilizations: List<SimultaneousTurnComponentSnapshot> = ArrayList(),
    val tiles: List<SimultaneousTurnComponentSnapshot> = ArrayList(),
    val religions: List<SimultaneousTurnComponentSnapshot> = ArrayList()
)

object SimultaneousTurnOperations {
    private val snapshottedUnitActions = setOf(
        UnitActionType.FoundCity,
        UnitActionType.ConstructImprovement,
        UnitActionType.Repair,
        UnitActionType.CreateImprovement,
        UnitActionType.Pillage,
        UnitActionType.HurryResearch,
        UnitActionType.HurryPolicy,
        UnitActionType.HurryWonder,
        UnitActionType.HurryBuilding,
        UnitActionType.ConductTradeMission,
        UnitActionType.FoundReligion,
        UnitActionType.TriggerUnique,
        UnitActionType.SpreadReligion,
        UnitActionType.RemoveHeresy,
        UnitActionType.EnhanceReligion,
        UnitActionType.AddInCapital,
        UnitActionType.Promote,
        UnitActionType.GiftUnit,
        // These actions either run deep inside other code (automation), finish asynchronously behind
        // a confirmation popup (disband), or are performed outside the unit-action recording helpers;
        // a full state diff is the reliable way to replay their real state changes.
        UnitActionType.Automate,
        UnitActionType.Explore,
        UnitActionType.Upgrade,
        UnitActionType.Transform,
        UnitActionType.ConnectRoad,
        UnitActionType.DisbandUnit
    )

    fun requiresGameStateSnapshot(type: UnitActionType) = type in snapshottedUnitActions

    fun captureGameStateChange(
        action: UnitActionType,
        before: GameInfo,
        after: GameInfo
    ): SimultaneousTurnGameStateResult? {
        val civilizations = ArrayList<SimultaneousTurnComponentSnapshot>()
        for (afterCiv in after.civilizations) {
            val beforeCiv = before.getCivilizationOrNull(afterCiv.civName)
            val beforeJson = if (beforeCiv == null) null else json().toJson(beforeCiv)
            val afterJson = json().toJson(afterCiv)
            if (beforeJson != afterJson)
                civilizations.add(SimultaneousTurnComponentSnapshot(afterCiv.civName, beforeJson, afterJson))
        }

        val tiles = ArrayList<SimultaneousTurnComponentSnapshot>()
        // Look tiles up by position instead of indexing before.tileMap[x, y]: "before" is normally a
        // raw GameInfo.clone() (see WorldScreen.beginSimultaneousGameStateSnapshot), and TileMap.clone()
        // deliberately leaves the transient tileMatrix empty, so indexing would crash for every snapshot.
        val beforeTiles = before.tileMap.tileList.associateBy { it.position }
        for (afterTile in after.tileMap.tileList) {
            val beforeTile = beforeTiles[afterTile.position]
            val beforeJson = if (beforeTile == null) null else json().toJson(beforeTile)
            val afterJson = json().toJson(afterTile)
            if (beforeJson != afterJson) {
                val key = "${afterTile.position.x},${afterTile.position.y}"
                tiles.add(SimultaneousTurnComponentSnapshot(key, beforeJson, afterJson))
            }
        }

        val religions = ArrayList<SimultaneousTurnComponentSnapshot>()
        for (name in before.religions.keys + after.religions.keys) {
            val beforeReligion = before.religions[name]
            val afterReligion = after.religions[name]
            val beforeJson = if (beforeReligion == null) null else json().toJson(beforeReligion)
            val afterJson = if (afterReligion == null) null else json().toJson(afterReligion)
            if (beforeJson != afterJson)
                religions.add(SimultaneousTurnComponentSnapshot(name, beforeJson, afterJson))
        }

        val beforeGlobal = captureGlobalState(before)
        val afterGlobal = captureGlobalState(after)
        if (beforeGlobal == afterGlobal && civilizations.isEmpty() && tiles.isEmpty() && religions.isEmpty()) return null
        return SimultaneousTurnGameStateResult(
            action.name,
            json().toJson(beforeGlobal),
            json().toJson(afterGlobal),
            civilizations,
            tiles,
            religions
        )
    }

    /**
     * The settlement host rebuilds a simultaneous turn by replaying the recorded operations on the
     * turn-start save. A state change made through a code path that failed to record an operation
     * would be silently lost. This replays [operations] on a clone of [turnStart] and diffs the
     * result against [current], so the caller can record any difference as a final catch-all
     * operation whose "before" matches the state the other operations will have produced.
     */
    fun diffUnrecordedState(
        turnStart: GameInfo,
        current: GameInfo,
        operations: List<SimultaneousTurnOperation>
    ): SimultaneousTurnGameStateResult? {
        // Both sides must be normalized the same way *after* all mutations: setTransients() runs
        // updateViewableTiles() -> updateLastSeenImprovements(), so normalizing only one side (or
        // normalizing before replay) reports a phantom unrecorded change every turn. A raw clone()
        // also leaves the cloned tiles bound to the original tile map (Tile.clone keeps the old
        // tileMap reference; only setTransients() rebinds it) and keeps each civ's unit list pointing
        // at pre-replay units, so replaying into it would update the wrong tile objects.
        val expected = normalizedClone(turnStart)
        SimultaneousTurnReplay.replay(expected, operations)
        return captureGameStateChange(
            UnitActionType.TriggerUnique, normalizedClone(expected), normalizedClone(current)
        )
    }

    /** A deep clone whose transient state (tile map bindings, civ unit lists) is coherent. */
    private fun normalizedClone(source: GameInfo): GameInfo {
        val clone = source.clone()
        for (civilization in clone.civilizations) civilization.units.clearUnits()
        clone.setTransients()
        return clone
    }

    /**
     * Splits a broad state diff into one operation per changed component. [applyGameState] rejects
     * an operation as a whole when any component no longer matches its recorded "before"/"after"
     * (e.g. two players editing the same tile in one simultaneous turn), so bundling many components
     * into a single operation - as the reconciliation catch-all would - would also discard the
     * unrelated, compatible changes. Independent operations keep the conflict local.
     */
    fun splitGameStateResult(result: SimultaneousTurnGameStateResult): List<SimultaneousTurnGameStateResult> {
        val parts = ArrayList<SimultaneousTurnGameStateResult>()
        // Always use ArrayList: listOf() / emptyList() return java.util.Collections singletons, and
        // libgdx Json writes their concrete class into the payload but cannot instantiate it again.
        val noGlobal = result.globalAfter
        if (result.globalBefore != result.globalAfter)
            parts.add(SimultaneousTurnGameStateResult(
                result.action, result.globalBefore, result.globalAfter, ArrayList(), ArrayList(), ArrayList()
            ))
        for (civilization in result.civilizations)
            parts.add(SimultaneousTurnGameStateResult(
                result.action, noGlobal, noGlobal, civilizations = arrayListOf(civilization)
            ))
        for (tile in result.tiles)
            parts.add(SimultaneousTurnGameStateResult(
                result.action, noGlobal, noGlobal, tiles = arrayListOf(tile)
            ))
        for (religion in result.religions)
            parts.add(SimultaneousTurnGameStateResult(
                result.action, noGlobal, noGlobal, religions = arrayListOf(religion)
            ))
        return parts
    }

    fun currentGlobalState(gameInfo: GameInfo) = SimultaneousTurnGlobalState(
        lastUnitId = gameInfo.getLastUnitIdForSimultaneousTurns(),
        diplomaticVictoryVotesCast = HashMap(gameInfo.diplomaticVictoryVotesCast),
        unitNamesTaken = ArrayList(gameInfo.unitNamesTaken),
        variables = HashMap(gameInfo.variables)
    )

    private fun captureGlobalState(gameInfo: GameInfo) = currentGlobalState(gameInfo)

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
        if (server.appendSimultaneousTurnOperations(gameId, encode(operations))) return
        // The server has no atomic append, so fall back to a whole-file read-modify-write. That is not
        // atomic: another client uploading at the same time can overwrite us. After each attempt,
        // verify our operations survived and merge again if they did not.
        val keys = operations.map { Triple(it.turn, it.playerId, it.sequence) }.toSet()
        repeat(3) {
            val old = loadLegacyOperations(server, gameId)
            server.fileStorage().saveFileData(fileName(gameId), encode(merge(old, operations)))
            val saved = loadLegacyOperations(server, gameId)
            if (saved.map { Triple(it.turn, it.playerId, it.sequence) }.containsAll(keys)) return
            Log.debug(
                "Simultaneous-turn legacy upload raced with another client (game %s), retrying",
                gameId
            )
        }
    }

    private suspend fun loadLegacyOperations(server: MultiplayerServer, gameId: String): List<SimultaneousTurnOperation> =
        try {
            decode(server.fileStorage().loadFileData(fileName(gameId)))
        } catch (_: MultiplayerFileNotFoundException) {
            emptyList()
        }

    suspend fun download(server: MultiplayerServer, gameId: String): List<SimultaneousTurnOperation> {
        val atomicData = server.loadSimultaneousTurnOperations(gameId)
        if (atomicData != null) return decode(atomicData)
        return try {
            decode(server.fileStorage().loadFileData(fileName(gameId)))
        } catch (_: MultiplayerFileNotFoundException) {
            emptyList()
        }
    }
}

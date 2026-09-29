package com.unciv.logic.multiplayer

import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.multiplayer.storage.MultiplayerFileNotFoundException
import com.unciv.logic.multiplayer.storage.MultiplayerServer
import com.unciv.models.UnitActionType

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
    val escorting: Boolean = false
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
    val diplomaticVictoryVotesCast: Map<String, String?> = emptyMap(),
    val unitNamesTaken: List<String> = emptyList(),
    val variables: Map<String, Int> = emptyMap()
)

data class SimultaneousTurnGameStateResult(
    val action: String = "",
    val globalBefore: String = "",
    val globalAfter: String = "",
    val civilizations: List<SimultaneousTurnComponentSnapshot> = emptyList(),
    val tiles: List<SimultaneousTurnComponentSnapshot> = emptyList(),
    val religions: List<SimultaneousTurnComponentSnapshot> = emptyList()
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
        for (afterTile in after.tileMap.tileList) {
            val beforeTile = before.tileMap[afterTile.position]
            val beforeJson = json().toJson(beforeTile)
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
        val old = try {
            download(server, gameId)
        } catch (_: MultiplayerFileNotFoundException) {
            emptyList()
        }
        server.fileStorage().saveFileData(fileName(gameId), encode(merge(old, operations)))
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

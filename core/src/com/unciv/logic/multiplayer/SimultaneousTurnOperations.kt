package com.unciv.logic.multiplayer

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.NotificationCategory
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.multiplayer.storage.MultiplayerFileNotFoundException
import com.unciv.logic.multiplayer.storage.MultiplayerServer
import com.unciv.models.UnitActionType
import com.unciv.models.translations.tr
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
    val after: String? = null,
    /**
     * When non-empty, [before] and [after] carry only these top-level fields and replay must confine
     * both its conflict check and its write to them. Empty means "full before/after JSON", which is
     * what every operation recorded before this field existed looks like.
     */
    val changedFields: List<String> = ArrayList()
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

/**
 * Thrown when a client's operations for a turn did not reach the server.
 *
 * The upload used to return normally when both the atomic append and the whole-file fallback lost
 * the operations: the turn then settled without them and the player kept playing as if they had been
 * recorded. The message is a translation key - the caller shows it to the player.
 */
class SimultaneousTurnOperationUploadException :
    Exception("The server did not keep the actions of this turn")

object SimultaneousTurnOperations {
    private const val stateOperationPrefix = "state."
    private const val legacyStateOperationType = "game.state"

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

    /**
     * Actions a simultaneous turn refuses outright.
     *
     * v1 cannot carry these: they change objects shared between players (religions, city-state
     * diplomacy, the great-person and gifted-unit pools) or they need a confirmation the other clients
     * never see, and recording them halfway through a component snapshot corrupted unrelated state
     * instead of failing visibly. Refusing them up front is the only way to keep the promise that an
     * action either happens everywhere or nowhere -
     * see docs_plan/simultaneous-turns-v1-action-inventory.md, section 3.
     */
    private val unsupportedUnitActions = setOf(
        UnitActionType.ConductTradeMission,
        UnitActionType.AddInCapital,
        UnitActionType.GiftUnit,
        UnitActionType.FoundReligion,
        UnitActionType.EnhanceReligion,
        UnitActionType.SpreadReligion,
        UnitActionType.RemoveHeresy,
        UnitActionType.ConnectRoad,
        UnitActionType.Pillage,
        UnitActionType.Paradrop
    )

    fun isUnsupportedUnitAction(type: UnitActionType) = type in unsupportedUnitActions

    fun requiresGameStateSnapshot(type: UnitActionType) = type in snapshottedUnitActions

    /**
     * Operation type of a recorded state change: `state.` plus the action that caused it, so an
     * operation says what it was instead of hiding every state change behind one generic type.
     */
    fun stateOperationType(actionName: String) = stateOperationPrefix + actionName

    /**
     * True for a recorded state change: the explicit [stateOperationType] types as well as the plain
     * `game.state` that operations recorded before the action was part of the type still use.
     */
    fun isStateOperationType(type: String) =
        type == legacyStateOperationType || type.startsWith(stateOperationPrefix)

    /**
     * Player-facing name of an operation, used when telling a player which of their actions did not
     * apply. Every state change used to be reported as `game.state`, which told them nothing.
     */
    fun describeOperationType(type: String): String {
        if (!type.startsWith(stateOperationPrefix)) return type
        val actionName = type.removePrefix(stateOperationPrefix)
        return SimultaneousTurnStateAction.values().firstOrNull { it.name == actionName }?.value
            ?: UnitActionType.values().firstOrNull { it.name == actionName }?.value
            ?: type
    }

    /**
     * True when a simultaneous turn has waited longer than [timeoutMinutes] for a player whose last
     * recorded activity was [lastActivityMillis]. Settlement uses this to continue without a player
     * who quit instead of freezing the game for everyone else. A [timeoutMinutes] of 0 waits forever.
     */
    fun hasSimultaneousTurnTimedOut(
        lastActivityMillis: Long,
        waitStartedAtMillis: Long,
        nowMillis: Long,
        timeoutMinutes: Int
    ): Boolean {
        if (timeoutMinutes <= 0) return false
        return nowMillis - maxOf(lastActivityMillis, waitStartedAtMillis) >= timeoutMinutes * 60_000L
    }

    /**
     * Player-visible description of the operations that settlement could not apply, or null when
     * there are none. A rejected operation used to end up in the debug log only: the turn advanced
     * as if it had been applied and the player whose action was lost was never told.
     * Knowing which action was lost is not enough to act on it, so the reason travels with it.
     * See docs_plan/simultaneous-turns-v1-action-inventory.md.
     * This is shown on the settling client, so translating the reason here is safe.
     */
    fun describeFailedSimultaneousTurnOperations(
        gameInfo: GameInfo,
        failedOperations: List<SimultaneousTurnFailedOperation>
    ): String? {
        if (failedOperations.isEmpty()) return null
        return failedOperations.joinToString("\n") { failure ->
            val operation = failure.operation
            val civName = gameInfo.civilizations
                .firstOrNull { it.playerId == operation.playerId }?.civName
                ?: operation.playerId.ifEmpty { "?" }
            "$civName - ${describeOperationType(operation.type)} (${failure.reason.sentence.tr()})"
        }
    }

    /**
     * Tells every player whose operations settlement could not apply. Rejected operations used to
     * reach the settling player only (as a settlement report popup), so the player who actually
     * lost an action was never told. Notification texts are stored untranslated and translated on
     * the fly, so each client reads the message in its own language.
     * @return the playerIds that were notified, for logging and tests.
     */
    fun notifyPlayersOfFailedOperations(
        gameInfo: GameInfo,
        failedOperations: List<SimultaneousTurnFailedOperation>
    ): List<String> {
        if (failedOperations.isEmpty()) return emptyList()
        val notified = ArrayList<String>()
        for ((playerId, failures) in failedOperations.groupBy { it.operation.playerId }) {
            if (playerId.isEmpty()) continue
            val civ = gameInfo.civilizations.firstOrNull { it.playerId == playerId } ?: continue
            if (civ.playerType == PlayerType.AI) continue
            // One notification per action and reason: both are placeholder terms, so every client
            // translates them into its own language when the notification is shown.
            val problems = failures
                .map { describeOperationType(it.operation.type) to it.reason.sentence }
                .distinct()
            for ((action, reason) in problems)
                civ.addNotification(
                    "A simultaneous turn could not apply one of your actions: [$action] - [$reason]",
                    NotificationCategory.General
                )
            notified.add(playerId)
        }
        return notified
    }

    fun captureGameStateChange(
        action: UnitActionType,
        before: GameInfo,
        after: GameInfo
    ): SimultaneousTurnGameStateResult? = captureGameStateChange(action.name, before, after)

    fun captureGameStateChange(
        action: SimultaneousTurnStateAction,
        before: GameInfo,
        after: GameInfo
    ): SimultaneousTurnGameStateResult? = captureGameStateChange(action.name, before, after)

    private fun captureGameStateChange(
        action: String,
        before: GameInfo,
        after: GameInfo
    ): SimultaneousTurnGameStateResult? {
        val civilizations = ArrayList<SimultaneousTurnComponentSnapshot>()
        for (afterCiv in after.civilizations) {
            val beforeCiv = before.getCivilizationOrNull(afterCiv.civName)
            val beforeJson = if (beforeCiv == null) null else json().toJson(beforeCiv)
            val afterJson = json().toJson(afterCiv)
            if (beforeJson != afterJson)
                civilizations.add(componentSnapshot(afterCiv.civName, beforeJson, afterJson))
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
                religions.add(componentSnapshot(name, beforeJson, afterJson))
        }

        val beforeGlobal = captureGlobalState(before)
        val afterGlobal = captureGlobalState(after)
        if (beforeGlobal == afterGlobal && civilizations.isEmpty() && tiles.isEmpty() && religions.isEmpty()) return null
        return SimultaneousTurnGameStateResult(
            action,
            json().toJson(beforeGlobal),
            json().toJson(afterGlobal),
            civilizations,
            tiles,
            religions
        )
    }

    /**
     * A component snapshot that carries only the top-level fields that actually changed.
     *
     * A whole-object snapshot makes the operation proportional to the *component* instead of to the
     * change: picking up a goody hut shipped ~4 KB of civilization JSON to describe the 24 bytes of
     * gold and science it granted. A field-level snapshot also lets replay merge per field, so an
     * unrelated field another player already moved no longer rejects the whole component.
     *
     * Falls back to the full before/after JSON whenever a faithful partial cannot be built: a new
     * component (no before), a field that disappeared in `after` (libgdx Json cannot write an explicit
     * null back, so clearing still needs the whole-object path), or JSON that differs without any
     * top-level field differing.
     */
    private fun componentSnapshot(
        key: String,
        beforeJson: String?,
        afterJson: String?
    ): SimultaneousTurnComponentSnapshot {
        val full = SimultaneousTurnComponentSnapshot(key, beforeJson, afterJson)
        // A component that appeared or disappeared has nothing to diff field by field: the whole
        // object is the change. (A removed component is applied by deleting it, see applyGameState.)
        if (beforeJson == null || afterJson == null) return full
        val beforeValue = JsonReader().parse(beforeJson)
        val afterValue = JsonReader().parse(afterJson)
        val fieldNames = LinkedHashSet<String>()
        for (field in afterValue) fieldNames.add(field.name)
        for (field in beforeValue) fieldNames.add(field.name)

        val changedFields = ArrayList<String>()
        val beforePartial = StringBuilder("{")
        val afterPartial = StringBuilder("{")
        for (name in fieldNames) {
            val beforeField = beforeValue.get(name)
            val afterField = afterValue.get(name)
            val beforeFieldJson = beforeField?.toJson(JsonWriter.OutputType.json)
            val afterFieldJson = afterField?.toJson(JsonWriter.OutputType.json)
            if (beforeFieldJson == afterFieldJson) continue
            // A field that disappeared needs the whole-object path: libgdx cannot write an explicit
            // null back, so a partial cannot express the removal.
            if (afterField == null) return full
            changedFields.add(name)
            // A field the before-document never carried (libgdx omits fields that still hold their
            // prototype value, such as an empty map) has to stay absent here too. Replay compares the
            // parsed current value against JsonValue.get(), which is null for an absent field, so
            // writing a literal `null` would make the two never compare equal and reject the whole
            // operation - which is exactly the common "an empty map gained an entry" case.
            if (beforeField != null) {
                if (beforePartial.length > 1) beforePartial.append(',')
                beforePartial.append('"').append(name).append("\":").append(beforeFieldJson)
            }
            if (afterPartial.length > 1) afterPartial.append(',')
            afterPartial.append('"').append(name).append("\":").append(afterFieldJson)
        }
        if (changedFields.isEmpty()) return full
        beforePartial.append('}')
        afterPartial.append('}')
        return SimultaneousTurnComponentSnapshot(
            key, beforePartial.toString(), afterPartial.toString(), changedFields
        )
    }

    /**
     * The settlement host rebuilds a simultaneous turn by replaying the recorded operations on the
     * turn-start save. A state change made through a code path that failed to record an operation
     * would be silently lost. This replays [operations] on a clone of [turnStart] and diffs the
     * result against [current]. A non-null result therefore means some code path changed state
     * without recording an operation: the caller reports it (see [describeGameStateResult]) instead
     * of patching the difference silently, because a silent patch hides the missing recording call.
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
     * A developer-facing summary of an unrecorded state diff. Naming the components - and the fields
     * inside them - is what makes the report actionable: its whole point is to find the code path that
     * changed state without recording an operation.
     */
    fun describeGameStateResult(result: SimultaneousTurnGameStateResult): String {
        val parts = ArrayList<String>()
        describeComponents("civilizations", result.civilizations, parts)
        describeComponents("tiles", result.tiles, parts)
        describeComponents("religions", result.religions, parts)
        if (result.globalBefore != result.globalAfter) {
            val fields = changedFieldNames(result.globalBefore, result.globalAfter)
            parts.add(if (fields.isEmpty()) "global" else "global (${fields.joinToString(", ")})")
        }
        // captureGameStateChange returns null for an all-equal diff, so a reported result normally has
        // at least one part; stay readable even if one ever arrives empty.
        return if (parts.isEmpty()) "unspecified state" else parts.joinToString("; ")
    }

    private fun describeComponents(
        name: String,
        components: List<SimultaneousTurnComponentSnapshot>,
        into: MutableList<String>
    ) {
        if (components.isEmpty()) return
        into.add("$name: " + components.joinToString(", ") { component ->
            // Older operations carry no changedFields, so fall back to diffing their full snapshots.
            val fields = component.changedFields.ifEmpty {
                changedFieldNames(component.before, component.after)
            }
            if (fields.isEmpty()) component.key else "${component.key} (${fields.joinToString(", ")})"
        })
    }

    /** The top-level field names whose JSON differs between two serialized component snapshots. */
    private fun changedFieldNames(beforeJson: String?, afterJson: String?): List<String> {
        if (beforeJson.isNullOrEmpty() || afterJson.isNullOrEmpty()) return emptyList()
        val beforeValue = try { JsonReader().parse(beforeJson) } catch (_: Exception) { return emptyList() }
        val afterValue = try { JsonReader().parse(afterJson) } catch (_: Exception) { return emptyList() }
        val names = LinkedHashSet<String>()
        for (field in beforeValue) names.add(field.name)
        for (field in afterValue) names.add(field.name)
        return names.filter { name ->
            beforeValue.get(name)?.toJson(JsonWriter.OutputType.json) !=
                afterValue.get(name)?.toJson(JsonWriter.OutputType.json)
        }
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
        // Falling through here means the operations are not on the server. Returning normally would
        // report a successful upload, so the turn would settle without this player's actions and they
        // would never learn they were lost. Report it instead - the caller retries and tells the player.
        throw SimultaneousTurnOperationUploadException()
    }

    private suspend fun loadLegacyOperations(server: MultiplayerServer, gameId: String): List<SimultaneousTurnOperation> =
        try {
            decode(server.fileStorage().loadFileData(fileName(gameId)))
        } catch (_: MultiplayerFileNotFoundException) {
            emptyList()
        }

    suspend fun download(server: MultiplayerServer, gameId: String): List<SimultaneousTurnOperation> {
        // Both stores must be read, not just the atomic one. Upload falls back to the legacy
        // whole-file store when the atomic append is rejected - a late-game turn can exceed the
        // server's per-request limit (413) - and reading only the atomic store once it had any row
        // made those operations invisible to everyone, so settlement silently dropped them.
        val atomicData = server.loadSimultaneousTurnOperations(gameId)
        val atomic = if (atomicData == null) emptyList() else decode(atomicData)
        return merge(atomic, loadLegacyOperations(server, gameId))
    }
}

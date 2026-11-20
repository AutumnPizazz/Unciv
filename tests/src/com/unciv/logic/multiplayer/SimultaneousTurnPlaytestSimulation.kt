package com.unciv.logic.multiplayer

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Base64Coder
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.files.UncivFiles
import com.unciv.logic.map.HexCoord
import com.unciv.logic.multiplayer.storage.MultiplayerTurnActionConflictException
import com.unciv.logic.multiplayer.storage.MultiplayerServer
import com.unciv.logic.multiplayer.storage.MultiplayerRoomCodeException
import com.unciv.models.UnitActionType
import com.unciv.models.metadata.Player
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.io.File
import java.net.CookieManager
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Local, reproducible "two clients play a simultaneous-turn game" simulation.
 *
 * Two independent [GameInfo] clients (A and B) each own a save, upload actions and ready markers over
 * the real HTTP contract, then one settles the authoritative ordered feed and atomically uploads the
 * new save. Both clients then re-download and must see identical state.
 *
 * A direct test run writes to `playtest-sim/` by default; `playtest-sim/run.sh` redirects output to a
 * temporary `.local/playtest-sim.*` directory:
 *   playtest-sim/
 *     server/           embedded server dump, or Go server data and logs in real mode
 *     clients/clientA/  uploaded ops, downloaded saves and state dumps per turn
 *     clients/clientB/
 *     logs/report.md    human-readable transcript of the whole run
 *     logs/simulation.log
 *
 * Run it with `playtest-sim/run.sh` (embedded HTTP server by default; `real` mode requires an
 * authorized local `server-go/` copy), or directly:
 * `./gradlew :tests:test --tests "com.unciv.logic.multiplayer.SimultaneousTurnPlaytestSimulation"`.
 * Without `playtest-sim/config.properties` (or `-Dplaytest.sim.enabled=true`) the test is skipped so
 * it never slows down the normal suite.
 */
@RunWith(BaseTestRunner::class)
class SimultaneousTurnPlaytestSimulation {

    companion object {
        private const val PASSWORD = "playtest123"
        private const val PLAYER_A = "aaaaaaaa-1111-4111-8111-111111111111"
        private const val PLAYER_B = "bbbbbbbb-2222-4222-8222-222222222222"
        private const val GAME_ID = "abcdef01-2345-4678-89ab-cdef01234567"
        private const val EMBEDDED_ROOM_CODE = "LOCAL-PLAYTEST-ROOM"

        /** Same headless adjustment the multiplayer integration tests need. */
        @BeforeClass
        @JvmStatic
        fun mockGdxApp() {
            val app = Mockito.mock(Application::class.java)
            Mockito.doAnswer { invocation ->
                (invocation.getArgument(0) as Runnable).run()
                null
            }.`when`(app).postRunnable(Mockito.any())
            Gdx.app = app
        }
    }

    private val transcript = StringBuilder()
    private lateinit var artifactsDir: File
    private lateinit var clientsDir: File
    private lateinit var logsDir: File
    private var serverUrl = ""

    private lateinit var civAName: String
    private lateinit var civBName: String
    private var unitAId = 0
    private var unitBId = 0

    private class PlaytestClient(
        val label: String,
        val playerId: String,
        val dir: File,
        val server: MultiplayerServer
    ) {
        val operations = ArrayList<SimultaneousTurnOperation>()
        val rejectedOperations = ArrayList<SimultaneousTurnOperation>()
        private val uploader = SimultaneousTurnOperationUploadQueue(
            upload = { batch ->
                for (operation in batch) {
                    try {
                        val accepted = server.appendRealtimeTurnAction(GAME_ID, RealtimeTurnActions.requestFor(operation))
                        check(accepted != null) { "The server does not support real-time turn actions" }
                    } catch (ex: MultiplayerTurnActionConflictException) {
                        throw SimultaneousTurnOperationRejectedException(operation, ex)
                    }
                }
            },
            onRejected = { operation, _ -> rejectedOperations.add(operation) }
        )

        fun record(operation: SimultaneousTurnOperation) {
            operations.add(operation)
            uploader.enqueueBatch(listOf(operation))
        }

        suspend fun flushOperations() = uploader.flushPending()
    }

    @Test
    fun twoClientsPlayTurnsAndConverge() {
        artifactsDir = resolveArtifactsDir()
        val configFile = File(artifactsDir, "config.properties")
        val enabled = System.getProperty("playtest.sim.enabled") == "true" || configFile.exists()
        Assume.assumeTrue(
            "Playtest simulation is disabled - run playtest-sim/run.sh to enable it",
            enabled
        )

        clientsDir = File(artifactsDir, "clients").apply { mkdirs() }
        logsDir = File(artifactsDir, "logs").apply { mkdirs() }

        val configuredServer = readConfig(configFile, "server")
        val localServer = if (configuredServer.isNullOrBlank()) LocalPlaytestServer(File(artifactsDir, "server")) else null
        serverUrl = configuredServer?.takeIf { it.isNotBlank() } ?: localServer!!.url

        try {
            note("using server $serverUrl")
            runBlocking { play(if (localServer != null) EMBEDDED_ROOM_CODE else issueRoomCode()) }
        } finally {
            localServer?.dump()
            localServer?.stop()
            writeReport()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Simulation
    // ---------------------------------------------------------------------------------------------

    private suspend fun play(roomCode: String) {
        val a = createClient("clientA", PLAYER_A)
        val b = createClient("clientB", PLAYER_B)
        register(a)
        register(b)
        note("registered players ${a.playerId} / ${b.playerId}")

        val testGame = buildGame()
        // TestGame resets UncivGame.Current, so (re)bind the file API afterwards: the first time two
        // civs meet, DiplomacyFunctions -> GameSettings.addCompletedTutorialTask -> save() needs it.
        UncivGame.Current.files = UncivFiles(Gdx.files)
        val initial = testGame.gameInfo
        // NB: do NOT call setTransients() here. GameInfo.setTransients() rebuilds each civ's
        // UnitManager from the tile map by *appending*, so calling it on a game that already has
        // units registered duplicates every unit. TestGame's addUnit() already registered them.
        fun assertRoomCodeRejected(rejectedCode: String?) {
            assertThrows(MultiplayerRoomCodeException::class.java) {
                runBlocking { a.server.uploadGame(initial, withPreview = true, roomCode = rejectedCode) }
            }
        }
        assertRoomCodeRejected(null)
        assertRoomCodeRejected("INVALID")
        a.server.uploadGame(initial, withPreview = true, roomCode = roomCode)
        val otherGameId = UUID.randomUUID().toString()
        initial.gameId = otherGameId
        assertRoomCodeRejected(roomCode)
        initial.gameId = GAME_ID
        note("clientA uploaded the initial save (turns=${initial.turns}, ${civAName} vs ${civBName})")

        var turnStart = a.server.downloadGame(GAME_ID)
        // downloadGame -> gameInfoFromString() already ran setTransients() once on a fresh parse.
        note("both clients boot from turns=${turnStart.turns}")
        dumpClientState(a, turnStart, "turns-${turnStart.turns}-boot")
        dumpClientState(b, turnStart, "turns-${turnStart.turns}-boot")
        verifyConvergence(a, b, turnStart, "boot")

        // ---- Turn 1: independent moves + one game-state change each ----
        run {
            val gameA = cloneGame(turnStart)
            val gameB = cloneGame(turnStart)
            a.operations.clear()
            b.operations.clear()

            moveUnit(a, gameA, civAName, unitAId, -1, 1)
            changeState(a, gameA, UnitActionType.TriggerUnique) {
                it.getCivilization(civAName).variables["playA"] = 1
            }
            moveUnit(b, gameB, civBName, unitBId, 1, -1)
            changeState(b, gameB, UnitActionType.ConstructImprovement) {
                it.getCivilization(civAName).variables["contested"] = 1
            }
            changeState(b, gameB, UnitActionType.ConstructImprovement) {
                it.tileMap[-3, 1].improvement = "Farm"
            }
            submit(a, gameA)
            b.flushOperations()
            assertEquals("first accepted civilization claim rejects the later state change", listOf(1L), b.rejectedOperations.map { it.sequence })
            note("clientB rejected a conflicting state action and rebuilt from the accepted feed")
            val restoredB = cloneGame(turnStart)
            val acceptedByA = checkNotNull(b.server.downloadRealtimeTurnActions(GAME_ID, turnStart.turns, 0))
                .actions.filter { it.kind == "action" }.flatMap { it.operations }
            val restoreFailures = SimultaneousTurnReplay.replay(restoredB, acceptedByA)
            assertTrue("conflict recovery must restore from the server feed: $restoreFailures", restoreFailures.isEmpty())
            assertUnitAt(restoredB, civBName, unitBId, 1, -1, "accepted move survives conflict recovery")
            restoredB.nextSimultaneousOperationSequence = maxOf(
                restoredB.nextSimultaneousOperationSequence,
                (acceptedByA.filter { it.playerId == b.playerId }.maxOfOrNull { it.sequence } ?: -1L) + 1
            )
            b.operations.clear()
            changeState(b, restoredB, UnitActionType.ConstructImprovement) {
                it.tileMap[-3, 1].improvement = "Farm"
            }
            b.rejectedOperations.clear()
            submit(b, restoredB)

            val settled = settle(a, turnStart, previousSettler = b)
            assertUnitAt(settled, civAName, unitAId, -1, 1, "turn 1 A move")
            assertUnitAt(settled, civBName, unitBId, 1, -1, "turn 1 B move")
            assertEquals("turn 1 A state change", 1, settled.getCivilization(civAName).variables["playA"])
            assertEquals("turn 1 tile improvement", "Farm", settled.tileMap[-3, 1].improvement)
            recordTurn(1, a, b, settled, "both players moved and changed state independently")
            turnStart = settled
        }

        // ---- Turn 2: client A reloads mid-turn, which reseeds its operation sequence ----
        run {
            val gameA1 = cloneGame(turnStart)
            val gameB = cloneGame(turnStart)
            a.operations.clear()
            b.operations.clear()

            // A acts and uploads...
            changeState(a, gameA1, UnitActionType.TriggerUnique) {
                it.getCivilization(civAName).variables["playA"] = 2
            }
            a.flushOperations()
            val staleSequence = a.operations.maxOf { it.sequence }
            val persistedBeforeReload = a.server.downloadRealtimeTurnActions(GAME_ID, turnStart.turns, 0)!!
                .actions.filter { it.kind == "action" }.flatMap { it.operations }
                .filter { it.playerId == a.playerId }
            assertEquals(
                "A's actions must be durable before submitting done",
                a.operations.map { it.copy(createdAtMillis = 0) }.toSet(),
                persistedBeforeReload.map { it.copy(createdAtMillis = 0) }.toSet()
            )
            note("A uploaded ${a.operations.size} op(s) before reload (max sequence=$staleSequence)")

            changeState(a, gameA1, UnitActionType.TriggerUnique) {
                it.getCivilization(civAName).variables["pendingAcrossReload"] = 1
            }
            val journalDirectory = FileHandle(File(a.dir, "outbox"))
            val gameA2 = a.server.downloadGame(GAME_ID)
            SimultaneousTurnOutbox(journalDirectory, gameA1.turns, gameA1.currentTurnStartTime, a.playerId)
                .record(a.operations)
            val recovered = SimultaneousTurnOutbox(journalDirectory, gameA2.turns, gameA2.currentTurnStartTime, a.playerId)
                .recover(
                    a.server.downloadRealtimeTurnActions(GAME_ID, gameA2.turns, 0)!!
                        .actions.filter { it.kind == "action" }.flatMap { it.operations }
                ) { missing ->
                    assertEquals("Only the unacknowledged action needs upload", listOf(a.operations.last()), missing)
                    for (operation in missing)
                        a.server.appendRealtimeTurnAction(GAME_ID, RealtimeTurnActions.requestFor(operation))
                }
            val replayFailures = SimultaneousTurnReplay.replay(gameA2, recovered.operations)
            assertTrue("A's journaled actions must replay after reload: $replayFailures", replayFailures.isEmpty())
            assertEquals(1, gameA2.getCivilization(civAName).variables["pendingAcrossReload"])
            gameA2.nextSimultaneousOperationSequence = maxOf(
                gameA2.nextSimultaneousOperationSequence,
                recovered.operations.maxOf { it.sequence } + 1
            )
            a.operations.clear()
            a.operations.addAll(recovered.operations)
            moveUnit(a, gameA2, civAName, unitAId, -2, 2)
            assertTrue(
                "reseeded sequence must not collide with the pre-reload operations",
                a.operations.last().sequence > staleSequence
            )
            submit(a, gameA2)

            moveUnit(b, gameB, civBName, unitBId, 2, -2)
            setUnitAction(b, gameB, civBName, unitBId, UnitActionType.Explore.value, automated = true)
            submit(b, gameB)

            val settled = settle(b, turnStart)
            assertEquals(
                "A's pre-reload state change must survive the reload",
                2, settled.getCivilization(civAName).variables["playA"]
            )
            assertEquals(1, settled.getCivilization(civAName).variables["pendingAcrossReload"])
            assertUnitAt(settled, civAName, unitAId, -2, 2, "turn 2 A move (recorded after reload)")
            assertUnitAt(settled, civBName, unitBId, 2, -2, "turn 2 B move")
            val unitB = settled.getCivilization(civBName).units.getUnitById(unitBId)!!
            assertEquals("turn 2 B action", UnitActionType.Explore.value, unitB.action)
            assertTrue("turn 2 B automation flag", unitB.automated)
            recordTurn(2, a, b, settled, "client A reloaded mid-turn and re-seeded its operation counter")
            turnStart = settled
        }

        // ---- Turn 3: one more round, settled by the other client ----
        run {
            val gameA = cloneGame(turnStart)
            val gameB = cloneGame(turnStart)
            a.operations.clear()
            b.operations.clear()

            moveUnit(a, gameA, civAName, unitAId, -1, 1)
            changeState(a, gameA, UnitActionType.TriggerUnique) {
                it.getCivilization(civAName).variables["playA"] = 3
            }
            moveUnit(b, gameB, civBName, unitBId, 1, -1)
            changeState(b, gameB, UnitActionType.TriggerUnique) {
                it.getCivilization(civBName).variables["playB"] = 3
            }
            submit(a, gameA)
            submit(b, gameB)

            val settled = settle(a, turnStart)
            assertEquals("turn 3 A state change", 3, settled.getCivilization(civAName).variables["playA"])
            assertEquals("turn 3 B state change", 3, settled.getCivilization(civBName).variables["playB"])
            assertUnitAt(settled, civAName, unitAId, -1, 1, "turn 3 A move")
            assertUnitAt(settled, civBName, unitBId, 1, -1, "turn 3 B move")
            recordTurn(3, a, b, settled, "third turn settled by the other client")
            turnStart = settled
        }

        note("simulation finished cleanly at turns=${turnStart.turns}")
    }

    // ---------------------------------------------------------------------------------------------
    // Client-side operations, mirroring WorldScreen's recording semantics
    // ---------------------------------------------------------------------------------------------

    private fun buildGame(): TestGame {
        val testGame = TestGame()
        testGame.makeHexagonalMap(4)
        val playableNations = testGame.ruleset.nations.values.filter { it.isMajorCiv }
        val civA = testGame.addCiv(playableNations[0], isPlayer = true)
        val civB = testGame.addCiv(playableNations[1], isPlayer = true)
        testGame.addCity(civA, testGame.getTile(-2, 0))
        testGame.addCity(civB, testGame.getTile(2, 0))
        val unitA = testGame.addUnit("Warrior", civA, testGame.getTile(-2, 1))
        val unitB = testGame.addUnit("Warrior", civB, testGame.getTile(2, -1))

        val game = testGame.gameInfo
        civA.playerId = PLAYER_A
        civB.playerId = PLAYER_B
        game.gameId = GAME_ID
        game.gameParameters.isOnlineMultiplayer = true
        game.gameParameters.simultaneousTurns = true
        game.gameParameters.pollingIntervalSeconds = 1
        game.gameParameters.multiplayerServerUrl = serverUrl
        game.gameParameters.players.clear()
        game.gameParameters.players.add(Player(civA.civName, PlayerType.Human, PLAYER_A))
        game.gameParameters.players.add(Player(civB.civName, PlayerType.Human, PLAYER_B))

        civAName = civA.civName
        civBName = civB.civName
        unitAId = unitA.id
        unitBId = unitB.id
        return testGame
    }

    private fun moveUnit(client: PlaytestClient, game: GameInfo, civName: String, unitId: Int, toX: Int, toY: Int) {
        val civ = game.getCivilization(civName)
        val unit = civ.units.getUnitById(unitId)!!
        val from = unit.currentTile.position
        unit.movement.moveToTile(game.tileMap[toX, toY])
        client.record(
            SimultaneousTurnOperation(
                turn = game.turns,
                playerId = client.playerId,
                sequence = game.nextSimultaneousOperationSequence++,
                type = "unit.move",
                payload = json().toJson(
                    SimultaneousTurnMoveResult(
                        unit.id, civName, from.x, from.y, toX, toY, unit.health, unit.currentMovement
                    )
                )
            )
        )
    }

    private fun setUnitAction(
        client: PlaytestClient, game: GameInfo, civName: String, unitId: Int, action: String, automated: Boolean
    ) {
        val civ = game.getCivilization(civName)
        val unit = civ.units.getUnitById(unitId)!!
        unit.action = action
        unit.automated = automated
        client.record(
            SimultaneousTurnOperation(
                turn = game.turns,
                playerId = client.playerId,
                sequence = game.nextSimultaneousOperationSequence++,
                type = "unit.action",
                payload = json().toJson(
                    SimultaneousTurnUnitActionResult(
                        unit.id, civName, action, unit.due, unit.health, unit.currentMovement,
                        unit.isEscorting(), automated
                    )
                )
            )
        )
    }

    /** Mirrors `WorldScreen.recordSimultaneousGameStateChange`: keep each user action atomic. */
    private fun changeState(
        client: PlaytestClient, game: GameInfo, action: UnitActionType, mutate: (GameInfo) -> Unit
    ) {
        // Mirrors WorldScreen.beginSimultaneousGameStateSnapshot(): a plain clone(), NOT cloneGame().
        // cloneGame() would run setTransients() a second time; that extra pass is not idempotent
        // (e.g. it can drop a freshly established diplomacy entry), so before/after would diverge.
        val before = game.clone()
        mutate(game)
        val result = SimultaneousTurnOperations.captureGameStateChange(action, before, game) ?: return
        client.record(
            SimultaneousTurnOperation(
                turn = game.turns,
                playerId = client.playerId,
                sequence = game.nextSimultaneousOperationSequence++,
                type = SimultaneousTurnOperations.stateOperationType(action.name),
                payload = json().toJson(result)
            )
        )
    }

    private suspend fun submit(client: PlaytestClient, game: GameInfo) {
        client.flushOperations()
        val ready = client.server.markRealtimeTurnReady(GAME_ID, game.turns, game.nextSimultaneousOperationSequence++)
        check(ready != null) { "The server does not support real-time turn ready markers" }
        writeClientOps(client, "turn-${game.turns}-submitted", client.operations)
        note("${client.label} submitted ${client.operations.size} action(s) and is ready for turn ${game.turns}")
    }

    // ---------------------------------------------------------------------------------------------
    // Settlement, mirroring WorldScreen.finishSimultaneousTurn
    // ---------------------------------------------------------------------------------------------

    private suspend fun settle(settler: PlaytestClient, turnStart: GameInfo, previousSettler: PlaytestClient? = null): GameInfo {
        val feed = checkNotNull(settler.server.downloadRealtimeTurnActions(GAME_ID, turnStart.turns, 0))
        val donePlayers = (feed.readyPlayerIds + feed.timedOutPlayerIds).toSet()
        assertEquals(
            "every living human player must be ready before settlement",
            setOf(PLAYER_A, PLAYER_B), donePlayers
        )
        assertTrue("the server must report all players ready", feed.allPlayersReady)

        val previousLease = previousSettler?.server?.acquireSimultaneousTurnSettlementLock(GAME_ID, turnStart.turns)
        if (previousSettler != null) {
            checkNotNull(previousLease)
            assertEquals(null, settler.server.acquireSimultaneousTurnSettlementLock(GAME_ID, turnStart.turns))
            previousSettler.server.releaseSimultaneousTurnSettlementLock(GAME_ID, previousLease)
        }
        val lease = checkNotNull(settler.server.acquireSimultaneousTurnSettlementLock(GAME_ID, turnStart.turns))
        try {
            val base = settler.server.tryDownloadGame(GAME_ID)
            val settlementFeed = checkNotNull(settler.server.downloadRealtimeTurnActions(GAME_ID, base.turns, 0))
            assertTrue("settlement feed must remain ready", settlementFeed.allPlayersReady)
            val turnOps = settlementFeed.actions.filter { it.kind == "action" }.flatMap { it.operations }
            note("${settler.label} settles turn ${base.turns} from ${turnOps.size} operation(s)")
            note("op order: " + turnOps.joinToString { "${it.playerId.take(1)}/${it.sequence}/${it.type}" })

            val hardFailures = ArrayList<String>()
            for (op in turnOps) {
                if (SimultaneousTurnReplay.apply(base, op)) continue
                note("FAILED op ${op.playerId}/${op.sequence}/${op.type}")
                hardFailures.add("${op.playerId}/${op.sequence}/${op.type}")
            }
            assertTrue("operations failed to replay: $hardFailures", hardFailures.isEmpty())
            val unrecorded = SimultaneousTurnOperations.diffUnrecordedState(turnStart, base, turnOps)
            if (unrecorded == null) {
                note("unrecorded state after replay: none")
            } else {
                note("unrecorded state after replay: detected (the client-side fallback would record it)")
                if (unrecorded.globalBefore != unrecorded.globalAfter)
                    note("  global: ${fieldDiff(unrecorded.globalBefore, unrecorded.globalAfter)}")
                for (component in unrecorded.civilizations)
                    note("  civ ${component.key}: ${fieldDiff(component.before, component.after)}")
                for (component in unrecorded.tiles)
                    note("  tile ${component.key}: ${fieldDiff(component.before, component.after)}")
            }

            base.nextTurnPolling()
            if (previousSettler != null && previousLease != null) {
                assertEquals(false, previousSettler.server.commitSimultaneousTurn(base, previousLease))
                assertEquals(turnStart.turns, settler.server.tryDownloadGamePreview(GAME_ID).turns)
                assertEquals(turnStart.turns, settler.server.downloadGame(GAME_ID).turns)
            }
            assertTrue(settler.server.commitSimultaneousTurn(base, lease))
            assertEquals(base.turns, settler.server.tryDownloadGamePreview(GAME_ID).turns)
            assertEquals("retry must not commit twice", false, settler.server.commitSimultaneousTurn(base, lease))
            note("${settler.label} uploaded the settled save (now turns=${base.turns})")
            return base
        } finally {
            settler.server.releaseSimultaneousTurnSettlementLock(GAME_ID, lease)
        }
    }

    /** Top-level JSON fields whose serialized values differ between two components. */
    private fun fieldDiff(before: String?, after: String?): String {
        if (before == null || after == null) return "before present=${before != null}, after present=${after != null}"
        val beforeValue = JsonReader().parse(before)
        val afterValue = JsonReader().parse(after)
        val keys = LinkedHashSet<String>()
        for (field in beforeValue) keys.add(field.name)
        for (field in afterValue) keys.add(field.name)
        return keys.filter { beforeValue.get(it)?.toString() != afterValue.get(it)?.toString() }
            .joinToString(", ")
    }

    // ---------------------------------------------------------------------------------------------
    // Verification and artifacts
    // ---------------------------------------------------------------------------------------------

    private suspend fun recordTurn(
        turn: Int, a: PlaytestClient, b: PlaytestClient, settled: GameInfo, extraNote: String
    ) {
        dumpClientState(a, settled, "turn-$turn")
        dumpClientState(b, settled, "turn-$turn")
        verifyConvergence(a, b, settled, "turn $turn")
        if (extraNote.isNotEmpty()) note("turn $turn: $extraNote")
    }

    private suspend fun verifyConvergence(
        a: PlaytestClient, b: PlaytestClient, authoritative: GameInfo, label: String
    ) {
        val seenA = a.server.downloadGame(GAME_ID)
        val seenB = b.server.downloadGame(GAME_ID)
        val expected = signature(authoritative)
        assertEquals("clientA view mismatched at $label", expected, signature(seenA))
        assertEquals("clientB view mismatched at $label", expected, signature(seenB))
        note("$label: both clients agree on the authoritative state")
    }

    /**
     * Deep-clones [source] and rebuilds its transient unit bookkeeping.
     *
     * `GameInfo.clone()` copies each civ's `UnitManager.unitList` verbatim, then `setTransients()`
     * would append every tile's unit to that already-populated list a second time. Discarding the
     * copied list first (exactly as `SimultaneousTurnReplay` does) keeps the unit count stable.
     */
    private fun cloneGame(source: GameInfo): GameInfo {
        val clone = source.clone()
        for (civ in clone.civilizations) civ.units.clearUnits()
        clone.setTransients()
        return clone
    }

    /** Deterministic textual fingerprint of the parts of the game the simulation touches. */
    private fun signature(game: GameInfo): String {
        val sb = StringBuilder()
        sb.append("turns=${game.turns}\n")
        for (name in listOf(civAName, civBName)) {
            val civ = game.getCivilization(name)
            sb.append("civ=$name vars=${civ.variables.toSortedMap()}\n")
            for (unit in civ.units.getCivUnits().sortedBy { it.id }) {
                sb.append(
                    "  unit=${unit.id} ${unit.name} at=${unit.currentTile.position} " +
                        "action=${unit.action} automated=${unit.automated} hp=${unit.health} " +
                        "mv=${unit.currentMovement}\n"
                )
            }
        }
        for (pos in listOf(HexCoord(-3, 1), HexCoord(-2, 0), HexCoord(2, 0))) {
            sb.append("tile=$pos improvement=${game.tileMap[pos.x, pos.y].improvement}\n")
        }
        return sb.toString()
    }

    private fun dumpClientState(client: PlaytestClient, game: GameInfo, name: String) {
        File(client.dir, "$name.state.txt").writeText(signature(game))
        File(client.dir, "$name.sav").writeText(
            UncivFiles.gameInfoToString(game, forceZip = true, updateChecksum = true)
        )
    }

    private fun writeClientOps(client: PlaytestClient, name: String, ops: List<SimultaneousTurnOperation>) {
        File(client.dir, "$name.ops.json").writeText(SimultaneousTurnOperations.encode(ops))
    }

    private fun assertUnitAt(game: GameInfo, civName: String, unitId: Int, x: Int, y: Int, label: String) {
        val unit = game.getCivilization(civName).units.getUnitById(unitId)
        assertTrue("$label: unit $unitId is missing", unit != null)
        assertEquals("$label", HexCoord(x, y), unit!!.currentTile.position)
    }

    private fun note(message: String) {
        val line = "[${java.time.LocalTime.now().withNano(0)}] $message"
        transcript.appendLine(line)
        println("PLAYTEST $line")
    }

    private fun writeReport() {
        File(logsDir, "simulation.log").writeText(transcript.toString())
        val report = buildString {
            appendLine("# 本地双客户端同时回合试玩模拟报告")
            appendLine()
            appendLine("- 服务器：`$serverUrl`")
            appendLine("- 游戏 ID：`$GAME_ID`")
            appendLine("- 玩家：A=`$PLAYER_A`，B=`$PLAYER_B`")
            appendLine("- 文明：A=`$civAName`，B=`$civBName`")
            appendLine()
            appendLine("产物：`clients/clientA|B/`（每回合上传的操作、下载的存档与状态指纹）、`logs/simulation.log`。")
            appendLine()
            appendLine("## 过程")
            appendLine()
            appendLine("```")
            append(transcript)
            appendLine("```")
        }
        File(logsDir, "report.md").writeText(report)
    }

    // ---------------------------------------------------------------------------------------------
    // Client setup helpers
    // ---------------------------------------------------------------------------------------------

    private fun createClient(label: String, playerId: String): PlaytestClient {
        val header = "Basic " + Base64Coder.encodeString("$playerId:$PASSWORD")
        val dir = File(clientsDir, label).apply { mkdirs() }
        return PlaytestClient(label, playerId, dir, MultiplayerServer.forTesting(serverUrl, mapOf("Authorization" to header)))
    }

    private fun register(client: PlaytestClient) {
        val authenticated = client.server.fileStorage().authenticate(client.playerId, PASSWORD)
        check(authenticated) { "authentication/registration failed for ${client.label}" }
    }

    private fun issueRoomCode(): String {
        val server = URI(serverUrl)
        check(server.host == "127.0.0.1") { "Room-code integration requires a loopback playtest server" }
        val adminPassword = File(artifactsDir, "server/admin-password").readText().trim()
        val client = HttpClient.newBuilder().cookieHandler(CookieManager()).build()
        val loginBody = JsonValue(JsonValue.ValueType.`object`).apply {
            addChild("username", JsonValue("admin"))
            addChild("password", JsonValue(adminPassword))
        }
        val loginRequest = HttpRequest.newBuilder(server.resolve("/api/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginBody.toJson(JsonWriter.OutputType.json)))
            .build()
        assertEquals(200, client.send(loginRequest, HttpResponse.BodyHandlers.discarding()).statusCode())
        val issueRequest = HttpRequest.newBuilder(server.resolve("/api/simultaneous-turn-room-codes"))
            .POST(HttpRequest.BodyPublishers.noBody()).build()
        val response = client.send(issueRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(201, response.statusCode())
        return JsonReader().parse(response.body()).getString("code")
    }

    // ---------------------------------------------------------------------------------------------
    // Tiny in-process server used when no external server is configured
    // ---------------------------------------------------------------------------------------------

    private class LocalPlaytestServer(private val root: File) {
        private val httpServer: HttpServer
        private val filesDir = File(root, "files").apply { mkdirs() }
        private val actions = ConcurrentHashMap<String, MutableList<RealtimeTurnActionEntry>>()
        private val locks = ConcurrentHashMap<String, String>()
        private val saves = mutableMapOf<String, Pair<String, String>>()
        private var roomCodeUsed = false
        val url: String

        init {
            httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            httpServer.createContext("/isalive") { respond(it, 200, "ok") }
            httpServer.createContext("/auth") { respond(it, 200, "ok") }
            httpServer.createContext("/files/") { handleFile(it) }
            httpServer.createContext("/simultaneous-turn-actions/") { handleRealtimeActions(it) }
            httpServer.createContext("/simultaneous-turn-ready/") { handleReady(it) }
            httpServer.createContext("/simultaneous-turn-lock/") { handleLock(it) }
            httpServer.createContext("/simultaneous-turn-saves/") { handleSave(it) }
            httpServer.executor = Executors.newCachedThreadPool()
            httpServer.start()
            url = "http://127.0.0.1:${httpServer.address.port}"
        }

        fun stop() = httpServer.stop(0)

        fun dump() {
            for ((id, pair) in saves) {
                storageFile(id).writeText(pair.first)
                storageFile("${id}_Preview").writeText(pair.second)
            }
            for ((id, list) in actions) {
                File(root, "$id-actions.json").writeText(json().toJson(list))
            }
            File(root, "server-state.txt").writeText(
                "files=${filesDir.list()?.sorted()}\nactions=${actions.mapValues { it.value.size }}\nlocks=${locks.keys}\n"
            )
        }

        private fun respond(exchange: HttpExchange, status: Int, body: String) {
            try {
                val bytes = body.toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.write(bytes)
            } finally {
                exchange.close()
            }
        }

        private fun storageFile(name: String) = File(filesDir, name.replace('/', '_'))

        private fun handleFile(exchange: HttpExchange) {
            try {
                val name = exchange.requestURI.path.removePrefix("/files/")
                when (exchange.requestMethod) {
                    "PUT" -> {
                        storageFile(name).writeText(exchange.requestBody.readBytes().toString(Charsets.UTF_8))
                        exchange.sendResponseHeaders(200, -1)
                    }
                    "GET" -> {
                        val pair = synchronized(saves) { saves[name.removeSuffix("_Preview")] }
                        val saved = if (name.endsWith("_Preview")) pair?.second else pair?.first
                        val file = storageFile(name)
                        if (saved != null) {
                            respond(exchange, 200, saved)
                        } else if (!file.exists()) {
                            exchange.sendResponseHeaders(404, -1)
                        } else {
                            val bytes = file.readBytes()
                            exchange.sendResponseHeaders(200, bytes.size.toLong())
                            exchange.responseBody.write(bytes)
                        }
                    }
                    "DELETE" -> {
                        storageFile(name).delete()
                        exchange.sendResponseHeaders(200, -1)
                    }
                    else -> exchange.sendResponseHeaders(405, -1)
                }
            } finally {
                exchange.close()
            }
        }

        private fun handleRealtimeActions(exchange: HttpExchange) {
            try {
                val id = exchange.requestURI.path.removePrefix("/simultaneous-turn-actions/")
                when (exchange.requestMethod) {
                    "POST" -> {
                        val request = json().fromJson(
                            RealtimeTurnActionRequest::class.java,
                            exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                        )
                        val operation = request.operations.singleOrNull()
                        if (operation == null) {
                            exchange.sendResponseHeaders(400, -1)
                            return
                        }
                        if (authenticatedPlayerID(exchange) != operation.playerId) {
                            exchange.sendResponseHeaders(403, -1)
                            return
                        }
                        var response: RealtimeTurnActionEntry? = null
                        var conflict = false
                        synchronized(actions) {
                            val list = actions.getOrPut(id) { ArrayList() }
                            response = list.firstOrNull {
                                it.turn == request.turn && it.playerId == operation.playerId &&
                                    it.clientSequence == request.clientSequence
                            }
                            if (response == null && list.any {
                                    it.turn == request.turn && it.playerId == operation.playerId && it.kind == "ready"
                                }) {
                                conflict = true
                            }
                            if (response == null && !conflict && list.any { accepted ->
                                    accepted.turn == request.turn && accepted.kind == "action" &&
                                        accepted.playerId != operation.playerId &&
                                        accepted.conflictKeys.any { it in request.conflictKeys }
                                }) {
                                conflict = true
                            }
                            if (response == null && !conflict) {
                                response = RealtimeTurnActionEntry(
                                    turn = request.turn,
                                    sequence = (list.filter { it.turn == request.turn }.maxOfOrNull { it.sequence } ?: 0) + 1,
                                    playerId = operation.playerId,
                                    clientSequence = request.clientSequence,
                                    kind = "action",
                                    conflictKeys = request.conflictKeys,
                                    operations = request.operations,
                                    createdAt = System.currentTimeMillis()
                                ).also { list.add(it) }
                            }
                        }
                        if (conflict) respond(exchange, 409, "conflict")
                        else respond(exchange, 200, json().toJson(response))
                    }
                    "GET" -> {
                        val query = exchange.requestURI.rawQuery.orEmpty().split('&')
                            .mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }
                            .toMap()
                        val turn = query["turn"]?.toIntOrNull() ?: 0
                        val after = query["after"]?.toLongOrNull() ?: 0L
                        val feed = synchronized(actions) {
                            val turnActions = actions[id].orEmpty().filter { it.turn == turn }.sortedBy { it.sequence }
                            val readyIDs = turnActions.filter { it.kind == "ready" }.map { it.playerId }.distinct()
                            RealtimeTurnActionFeed(
                                turn = turn,
                                after = after,
                                latestSequence = turnActions.maxOfOrNull { it.sequence } ?: 0L,
                                actions = ArrayList(turnActions.filter { it.sequence > after }),
                                readyPlayerIds = ArrayList(readyIDs),
                                allPlayersReady = readyIDs.toSet() == setOf(PLAYER_A, PLAYER_B)
                            )
                        }
                        respond(exchange, 200, json().toJson(feed))
                    }
                    else -> exchange.sendResponseHeaders(405, -1)
                }
            } finally {
                exchange.close()
            }
        }

        private fun handleReady(exchange: HttpExchange) {
            try {
                check(exchange.requestMethod == "POST")
                val id = exchange.requestURI.path.removePrefix("/simultaneous-turn-ready/")
                val request = JsonReader().parse(exchange.requestBody.reader(Charsets.UTF_8))
                val turn = request.getInt("turn", 0)
                val clientSequence = request.getLong("clientSequence", 0L)
                val playerId = authenticatedPlayerID(exchange)
                if (playerId !in setOf(PLAYER_A, PLAYER_B)) {
                    exchange.sendResponseHeaders(403, -1)
                    return
                }
                var response: RealtimeTurnActionEntry? = null
                var conflict = false
                synchronized(actions) {
                    val list = actions.getOrPut(id) { ArrayList() }
                    response = list.firstOrNull {
                        it.turn == turn && it.playerId == playerId && it.clientSequence == clientSequence
                    }
                    if (response == null && list.any { it.turn == turn && it.playerId == playerId && it.kind == "ready" })
                        conflict = true
                    if (response == null && !conflict) {
                        response = RealtimeTurnActionEntry(
                            turn = turn,
                            sequence = (list.filter { it.turn == turn }.maxOfOrNull { it.sequence } ?: 0) + 1,
                            playerId = playerId,
                            clientSequence = clientSequence,
                            kind = "ready",
                            createdAt = System.currentTimeMillis()
                        ).also { list.add(it) }
                    }
                }
                if (conflict) respond(exchange, 409, "already ready")
                else respond(exchange, 200, json().toJson(response))
            } catch (ex: Exception) {
                ex.printStackTrace()
                respond(exchange, 500, ex.toString())
            } finally {
                exchange.close()
            }
        }

        private fun handleLock(exchange: HttpExchange) {
            try {
                val renew = exchange.requestURI.path.endsWith("/renew")
                val id = exchange.requestURI.path
                    .removePrefix("/simultaneous-turn-lock/")
                    .removeSuffix("/renew")
                val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                when (exchange.requestMethod) {
                    "POST" -> {
                        if (renew) {
                            if (locks[id] == body) exchange.sendResponseHeaders(204, -1)
                            else exchange.sendResponseHeaders(409, -1)
                        } else {
                            val turn = body.toInt()
                            if (!allPlayersReady(id, turn)) {
                                exchange.sendResponseHeaders(409, -1)
                                return
                            }
                            val token = UUID.randomUUID().toString()
                            val existing = locks.putIfAbsent(id, token)
                            if (existing == null) respond(exchange, 201, token)
                            else exchange.sendResponseHeaders(409, -1)
                        }
                    }
                    "DELETE" -> {
                        locks.remove(id, body)
                        exchange.sendResponseHeaders(204, -1)
                    }
                    else -> exchange.sendResponseHeaders(405, -1)
                }
            } finally {
                exchange.close()
            }
        }

        private fun allPlayersReady(id: String, turn: Int): Boolean = synchronized(actions) {
            actions[id].orEmpty().filter { it.turn == turn && it.kind == "ready" }
                .map { it.playerId }.toSet() == setOf(PLAYER_A, PLAYER_B)
        }

        private fun authenticatedPlayerID(exchange: HttpExchange): String {
            val header = exchange.requestHeaders.getFirst("Authorization") ?: return ""
            if (!header.startsWith("Basic ")) return ""
            return try {
                Base64Coder.decodeString(header.removePrefix("Basic ")).substringBefore(':')
            } catch (_: Exception) {
                ""
            }
        }

        private fun handleSave(exchange: HttpExchange) {
            try {
                check(exchange.requestMethod == "POST")
                val id = exchange.requestURI.path.removePrefix("/simultaneous-turn-saves/")
                val request = JsonReader().parse(exchange.requestBody.reader(Charsets.UTF_8))
                val lease = request.getString("lease")
                val content = request.getString("content") to request.getString("preview")
                val status = synchronized(saves) {
                    if (lease.isEmpty()) {
                        if (id in saves) return@synchronized 409
                        if (roomCodeUsed || request.getString("roomCode", "") != EMBEDDED_ROOM_CODE) return@synchronized 422
                        roomCodeUsed = true
                    } else if (!locks.remove(id, lease)) return@synchronized 409
                    saves[id] = content
                    204
                }
                exchange.sendResponseHeaders(status, -1)
            } finally {
                exchange.close()
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Config / path resolution
    // ---------------------------------------------------------------------------------------------

    private fun resolveArtifactsDir(): File {
        System.getProperty("playtest.sim.dir")?.takeIf { it.isNotBlank() }?.let { return File(it).absoluteFile }
        var dir = File("").absoluteFile
        while (dir.parentFile != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir, "playtest-sim").absoluteFile
    }

    private fun readConfig(file: File, key: String): String? {
        if (!file.exists()) return null
        return file.readLines()
            .firstOrNull { it.substringBefore('=').trim() == key }
            ?.substringAfter('=', "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}

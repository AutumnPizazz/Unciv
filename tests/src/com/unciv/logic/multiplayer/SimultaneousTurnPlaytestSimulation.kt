package com.unciv.logic.multiplayer

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Base64Coder
import com.badlogic.gdx.utils.JsonReader
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.files.UncivFiles
import com.unciv.logic.map.HexCoord
import com.unciv.logic.multiplayer.storage.MultiplayerServer
import com.unciv.models.UnitActionType
import com.unciv.models.metadata.Player
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Local, reproducible "two clients play a simultaneous-turn game" simulation.
 *
 * Two independent [GameInfo] clients (A and B) each own a save, act on their own clone, upload
 * their operations over the real HTTP contract, then one of them settles the turn exactly the way
 * `WorldScreen.finishSimultaneousTurn` does (download ops -> replay -> `nextTurnPolling` -> upload
 * the new save). Both clients then re-download and must see identical state.
 *
 * Everything the run produces is written under a single directory (default `playtest-sim/`), so it
 * can be inspected, reused, or deleted as one unit:
 *   playtest-sim/
 *     server/           in-process server dump (files/, operations/)
 *     clients/clientA/  uploaded ops, downloaded saves and state dumps per turn
 *     clients/clientB/
 *     logs/report.md    human-readable transcript of the whole run
 *     logs/simulation.log
 *
 * Run it with `playtest-sim/run.sh` (which also boots the real Node server), or directly:
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
            runBlocking { play() }
        } finally {
            localServer?.dump()
            localServer?.stop()
            writeReport()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Simulation
    // ---------------------------------------------------------------------------------------------

    private suspend fun play() {
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
        a.server.uploadGame(initial, withPreview = true)
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
                it.tileMap[-3, 1].improvement = "Farm"
            }
            submit(a, gameA)
            submit(b, gameB)

            val settled = settle(a, turnStart)
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
            a.server.uploadSimultaneousTurnOperations(GAME_ID, ArrayList(a.operations))
            val staleSequence = a.operations.maxOf { it.sequence }
            note("A uploaded ${a.operations.size} op(s) before reload (max sequence=$staleSequence)")

            // ...then reloads: its in-memory op log is gone and the counter is re-seeded from the wall
            // clock, exactly like WorldScreen does on load. This is the regression this turn guards.
            val gameA2 = cloneGame(turnStart)
            gameA2.nextSimultaneousOperationSequence =
                maxOf(gameA2.nextSimultaneousOperationSequence, System.currentTimeMillis())
            a.operations.clear()
            moveUnit(a, gameA2, civAName, unitAId, -2, 2)
            assertTrue(
                "reseeded sequence must not collide with the pre-reload operations",
                a.operations.all { it.sequence > staleSequence }
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
        client.operations.add(
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
        client.operations.add(
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

    /** Mirrors `WorldScreen.recordSimultaneousGameStateChange`: snapshot, mutate, split into components. */
    private fun changeState(
        client: PlaytestClient, game: GameInfo, action: UnitActionType, mutate: (GameInfo) -> Unit
    ) {
        // Mirrors WorldScreen.beginSimultaneousGameStateSnapshot(): a plain clone(), NOT cloneGame().
        // cloneGame() would run setTransients() a second time; that extra pass is not idempotent
        // (e.g. it can drop a freshly established diplomacy entry), so before/after would diverge.
        val before = game.clone()
        mutate(game)
        val result = SimultaneousTurnOperations.captureGameStateChange(action, before, game) ?: return
        for (part in SimultaneousTurnOperations.splitGameStateResult(result)) {
            client.operations.add(
                SimultaneousTurnOperation(
                    turn = game.turns,
                    playerId = client.playerId,
                    sequence = game.nextSimultaneousOperationSequence++,
                    type = "game.state",
                    payload = json().toJson(part)
                )
            )
        }
    }

    private suspend fun submit(client: PlaytestClient, game: GameInfo) {
        val done = SimultaneousTurnOperation(
            turn = game.turns,
            playerId = client.playerId,
            sequence = game.nextSimultaneousOperationSequence++,
            type = "done",
            payload = "{}"
        )
        val ops = ArrayList(client.operations)
        ops.add(done)
        client.server.uploadSimultaneousTurnOperations(GAME_ID, ops)
        writeClientOps(client, "turn-${game.turns}-submitted", ops)
        note("${client.label} submitted ${ops.size} op(s) for turn ${game.turns}")
    }

    // ---------------------------------------------------------------------------------------------
    // Settlement, mirroring WorldScreen.finishSimultaneousTurn
    // ---------------------------------------------------------------------------------------------

    private suspend fun settle(settler: PlaytestClient, turnStart: GameInfo): GameInfo {
        val allOps = settler.server.downloadSimultaneousTurnOperations(GAME_ID)
        val donePlayers = allOps
            .filter { it.turn == turnStart.turns && it.type == "done" }
            .map { it.playerId }
            .toSet()
        assertEquals(
            "every human player must submit 'done' before settlement",
            setOf(PLAYER_A, PLAYER_B), donePlayers
        )

        assertTrue(
            "settlement lock for turn ${turnStart.turns}",
            settler.server.acquireSimultaneousTurnSettlementLock(GAME_ID, turnStart.turns, settler.playerId)
        )
        try {
            val base = cloneGame(turnStart)
            val turnOps = allOps.filter { it.turn == base.turns && it.type != "done" }
            note("${settler.label} settles turn ${base.turns} from ${turnOps.size} operation(s)")
            note("op order: " + turnOps.joinToString { "${it.playerId.take(1)}/${it.sequence}/${it.type}" })

            val hardFailures = ArrayList<String>()
            for (op in turnOps) {
                if (SimultaneousTurnReplay.apply(base, op)) continue
                // A "game.state" operation is redundant when another player's replayed operation
                // already produced its "after" state. Production only debug-logs unapplied operations;
                // a component matching neither "before" nor "after" right now is a genuine conflict.
                if (op.type == "game.state" && isRedundantStateOperation(base, op)) {
                    note("superseded op ${op.playerId}/${op.sequence}/${op.type} (its effect was already applied)")
                } else {
                    note("FAILED op ${op.playerId}/${op.sequence}/${op.type}")
                    hardFailures.add("${op.playerId}/${op.sequence}/${op.type}")
                }
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
            settler.server.uploadGame(base, withPreview = true)
            note("${settler.label} uploaded the settled save (now turns=${base.turns})")
            return base
        } finally {
            settler.server.releaseSimultaneousTurnSettlementLock(GAME_ID, turnStart.turns, settler.playerId)
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

    /**
     * True when a rejected `game.state` operation is merely redundant: every component it carries
     * already equals the operation's "after" value on [base] (another player's replayed operation
     * produced the same effect), or still equals its "before" value. A component that matches neither
     * is a real conflict, so this returns false and the caller fails the simulation.
     */
    private fun isRedundantStateOperation(base: GameInfo, operation: SimultaneousTurnOperation): Boolean {
        val result = json().fromJson(SimultaneousTurnGameStateResult::class.java, operation.payload)
        if (result.religions.isNotEmpty()) return false
        var alreadyApplied = false
        for (component in result.civilizations) {
            val current = base.getCivilizationOrNull(component.key)
            val currentJson = if (current == null) "null" else json().toJson(current)
            when (currentJson) {
                component.after -> alreadyApplied = true
                component.before -> {}
                else -> {
                    note("  civ=${component.key} matches neither before nor after")
                    note("  BEFORE=${component.before}")
                    note("  AFTER=${component.after}")
                    note("  CURRENT=${currentJson}")
                    return false
                }
            }
        }
        for (component in result.tiles) {
            val (x, y) = component.key.split(',').map { it.toInt() }
            val currentJson = json().toJson(base.tileMap[x, y])
            when (currentJson) {
                component.after -> alreadyApplied = true
                component.before -> {}
                else -> return false
            }
        }
        return alreadyApplied
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
        return PlaytestClient(label, playerId, dir, MultiplayerServer(serverUrl, mapOf("Authorization" to header)))
    }

    private fun register(client: PlaytestClient) {
        val authenticated = client.server.fileStorage().authenticate(client.playerId, PASSWORD)
        check(authenticated) { "authentication/registration failed for ${client.label}" }
    }

    // ---------------------------------------------------------------------------------------------
    // Tiny in-process server used when no external server is configured
    // ---------------------------------------------------------------------------------------------

    private class LocalPlaytestServer(private val root: File) {
        private val httpServer: HttpServer
        private val filesDir = File(root, "files").apply { mkdirs() }
        private val opsDir = File(root, "operations").apply { mkdirs() }
        private val operations = ConcurrentHashMap<String, MutableList<SimultaneousTurnOperation>>()
        private val locks = ConcurrentHashMap<String, String>()
        val url: String

        init {
            httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            httpServer.createContext("/isalive") { respond(it, 200, "ok") }
            httpServer.createContext("/auth") { respond(it, 200, "ok") }
            httpServer.createContext("/files/") { handleFile(it) }
            httpServer.createContext("/simultaneous-turn-operations/") { handleOps(it) }
            httpServer.createContext("/simultaneous-turn-lock/") { handleLock(it) }
            httpServer.executor = Executors.newCachedThreadPool()
            httpServer.start()
            url = "http://127.0.0.1:${httpServer.address.port}"
        }

        fun stop() = httpServer.stop(0)

        fun dump() {
            for ((id, list) in operations) {
                File(opsDir, "$id.json").writeText(SimultaneousTurnOperations.encode(list))
            }
            File(root, "server-state.txt").writeText(
                "files=${filesDir.list()?.sorted()}\noperations=${operations.mapValues { it.value.size }}\nlocks=${locks.keys}\n"
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
                        val file = storageFile(name)
                        if (!file.exists()) {
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

        private fun handleOps(exchange: HttpExchange) {
            try {
                val id = exchange.requestURI.path.removePrefix("/simultaneous-turn-operations/")
                when (exchange.requestMethod) {
                    "POST" -> {
                        val incoming = SimultaneousTurnOperations.decode(
                            exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                        )
                        synchronized(operations) {
                            val list = operations.getOrPut(id) { ArrayList() }
                            val existing = list.map { Triple(it.turn, it.playerId, it.sequence) }.toSet()
                            list += incoming.filterNot { Triple(it.turn, it.playerId, it.sequence) in existing }
                        }
                        exchange.sendResponseHeaders(200, -1)
                    }
                    "GET" -> {
                        val list = synchronized(operations) { operations[id]?.toList() }
                        if (list.isNullOrEmpty()) {
                            exchange.sendResponseHeaders(404, -1)
                        } else {
                            val body = SimultaneousTurnOperations.encode(
                                list.sortedWith(compareBy({ it.turn }, { it.playerId }, { it.sequence }))
                            )
                            val bytes = body.toByteArray(Charsets.UTF_8)
                            exchange.sendResponseHeaders(200, bytes.size.toLong())
                            exchange.responseBody.write(bytes)
                        }
                    }
                    else -> exchange.sendResponseHeaders(405, -1)
                }
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
                            val existing = locks.putIfAbsent(id, body)
                            if (existing == null || existing == body) exchange.sendResponseHeaders(201, -1)
                            else exchange.sendResponseHeaders(409, -1)
                        }
                    }
                    "DELETE" -> {
                        if (locks[id] == body) locks.remove(id)
                        exchange.sendResponseHeaders(200, -1)
                    }
                    else -> exchange.sendResponseHeaders(405, -1)
                }
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

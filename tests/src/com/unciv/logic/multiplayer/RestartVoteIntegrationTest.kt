package com.unciv.logic.multiplayer

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.GameStarter
import com.unciv.logic.files.UncivFiles
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapSize
import com.unciv.logic.multiplayer.storage.MultiplayerServer
import com.unciv.models.metadata.GameParameters
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.metadata.Player
import com.unciv.models.ruleset.RulesetCache
import com.unciv.testing.BaseTestRunner
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

/**
 * Integration tests for the restart vote feature.
 *
 * A local HTTP server emulates the multiplayer file storage (PUT/GET/DELETE of
 * `/files/{name}` - the same protocol the UncivServer uses), while switching
 * `settings.multiplayer.userId` simulates two different clients acting on one
 * [Multiplayer] instance. Game generation uses a Tiny map so `GameStarter`
 * runs fast.
 */
@RunWith(BaseTestRunner::class)
class RestartVoteIntegrationTest {

    private lateinit var httpServer: HttpServer
    private val storedFiles = ConcurrentHashMap<String, String>()
    private lateinit var multiplayer: Multiplayer
    private lateinit var game: GameInfo
    private lateinit var serverUrl: String

    private val gameId get() = game.gameId
    private val playerA = "playerA"
    private val playerB = "playerB"
    private val playerC = "playerC"

    companion object {
        /**
         * The restart vote code routes [com.unciv.logic.event.EventBus] sends through the GL
         * dispatcher (which is required in production), and [com.unciv.utils.Dispatcher.GL] is a
         * process-wide lazy that calls `Gdx.app` on first use. A plain headless test run has no
         * Gdx.app, so we provide a mock that executes posted runnables synchronously - which also
         * avoids the render-thread deadlock a real headless main loop would cause here.
         */
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

    @Before
    fun setUp() = runVoteTest {
        httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        httpServer.createContext("/files/") { exchange -> handleFileRequest(exchange) }
        httpServer.executor = Executors.newCachedThreadPool()
        httpServer.start()
        serverUrl = "http://127.0.0.1:${httpServer.address.port}"

        RulesetCache.loadRulesets(noMods = true)
        UncivGame.Current = UncivGame()
        UncivGame.Current.files = UncivFiles(Gdx.files)
        UncivGame.Current.settings = com.unciv.models.metadata.GameSettings().apply {
            multiplayer.setServer(serverUrl)
            multiplayer.setUserId(playerA)
        }
        multiplayer = Multiplayer()
        multiplayer.multiplayerGameUpdater.cancel() // no background polling in tests

        game = createOnlineGame(listOf(playerA, playerB))
        UncivGame.Current.gameInfo = game
    }

    @After
    fun tearDown() = runVoteTest {
        httpServer.stop(0)
    }

    private fun createOnlineGame(humanPlayers: List<String>): GameInfo {
        val param = GameParameters().apply {
            isOnlineMultiplayer = true
            numberOfCityStates = 0
            restartVoteTurn = 10
            restartVoteTimeoutMinutes = 60
            players.clear()
            players.add(Player("Rome", com.unciv.logic.civilization.PlayerType.Human, humanPlayers[0]))
            players.add(Player("Greece", com.unciv.logic.civilization.PlayerType.Human, humanPlayers[1]))
            if (humanPlayers.size > 2)
                players.add(Player("Persia", com.unciv.logic.civilization.PlayerType.Human, humanPlayers[2]))
        }
        val mapParameters = MapParameters().apply {
            mapSize = MapSize.Tiny
            seed = 42L
        }
        return GameStarter.startNewGame(GameSetupInfo(param, mapParameters))
    }

    private fun handleFileRequest(exchange: HttpExchange) {
        try {
            val name = exchange.requestURI.path.removePrefix("/files/")
            when (exchange.requestMethod) {
                "PUT" -> {
                    val data = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                    storedFiles[name] = data
                    exchange.sendResponseHeaders(200, -1)
                }
                "GET" -> {
                    val data = storedFiles[name]
                    if (data == null) {
                        exchange.sendResponseHeaders(404, -1)
                    } else {
                        val bytes = data.toByteArray(Charsets.UTF_8)
                        exchange.sendResponseHeaders(200, bytes.size.toLong())
                        exchange.responseBody.write(bytes)
                    }
                }
                "DELETE" -> {
                    storedFiles.remove(name)
                    exchange.sendResponseHeaders(200, -1)
                }
            }
        } finally {
            exchange.close()
        }
    }

    private fun asPlayer(playerId: String) {
        UncivGame.Current.settings.multiplayer.setUserId(playerId)
    }

    private fun restartVoteFileName() = "${gameId}_restartvote"
    private fun restartMarkerFileName() = "${gameId}_restart"

    private fun storedRestartVote(): RestartVote =
        json().fromJson(RestartVote::class.java, storedFiles[restartVoteFileName()]!!)

    private fun storedRestartMarker(): RestartMarker =
        json().fromJson(RestartMarker::class.java, storedFiles[restartMarkerFileName()]!!)

    private fun writeRestartMarker(marker: RestartMarker) {
        multiplayer.multiplayerServer.fileStorage()
            .saveFileData(restartMarkerFileName(), json().toJson(marker))
    }

    /** Runs a test body with the coroutine scope the Multiplayer suspend APIs need. */
    private fun runVoteTest(block: suspend () -> Unit) = runBlocking { block() }

    //region Scenarios

    @Test
    fun fullVoteLifecycleRestartsGame() = runVoteTest {
        // A initiates - auto-votes yes
        val started = multiplayer.startRestartVote(gameId, 10, 60)
        assertNotNull(started)
        assertEquals(RestartVoteStatus.OPEN, started!!.status)
        assertEquals(playerA, started.initiatorPlayerId)
        assertTrue(storedRestartVote().votes.containsKey(playerA))
        // Initiating again returns the existing vote instead of creating a second one
        val again = multiplayer.startRestartVote(gameId, 10, 60)
        assertEquals(playerA, again!!.initiatorPlayerId)
        assertEquals(1, storedFiles.keys.count { it == restartVoteFileName() })

        // B sees the vote via polling, votes no
        asPlayer(playerB)
        UncivGame.Current.gameInfo = game // same game from B's perspective
        multiplayer.refreshRestartVote(gameId)
        assertNotNull(multiplayer.getCachedRestartVote(gameId))
        val afterB = multiplayer.castRestartVote(gameId, false)
        assertNotNull(afterB)
        // All (2) players voted -> settles immediately; 1 yes >= ceil(2/2)=1 -> passed
        assertEquals(RestartVoteStatus.SETTLED, afterB!!.status)
        assertEquals(true, afterB.result)

        // A executes the restart
        asPlayer(playerA)
        val newGameId = multiplayer.executeRestart(gameId)
        assertNotNull(newGameId)
        assertTrue(newGameId!!.isNotEmpty())
        assertTrue(newGameId != gameId)
        // Marker is DONE and points at the new game
        val marker = storedRestartMarker()
        assertEquals(RestartMarkerStatus.DONE, marker.status)
        assertEquals(newGameId, marker.newGameId)
        // The new game is uploaded and downloadable
        val newGame = multiplayer.multiplayerServer.tryDownloadGame(newGameId)
        assertEquals(newGameId, newGame.gameId)
        assertEquals(0, newGame.turns)
        // Same setup, same playerId mapping, online flag preserved
        assertEquals(game.gameParameters.restartVoteTurn, newGame.gameParameters.restartVoteTurn)
        assertEquals(game.gameParameters.restartVoteTimeoutMinutes, newGame.gameParameters.restartVoteTimeoutMinutes)
        assertTrue(newGame.gameParameters.isOnlineMultiplayer)
        val humanIds = newGame.civilizations.filter { it.isHuman() }.map { it.playerId }.toSet()
        assertEquals(setOf(playerA, playerB), humanIds)
        assertEquals(game.tileMap.mapParameters.seed, newGame.tileMap.mapParameters.seed)
    }

    @Test
    fun voteRejectedByExplicitMajorityNo() = runVoteTest {
        // Three-player game: A yes, B and C no -> 1 < ceil(3/2)=2 -> rejected
        game = createOnlineGame(listOf(playerA, playerB, playerC))
        UncivGame.Current.gameInfo = game
        multiplayer.startRestartVote(gameId, 10, 60)
        asPlayer(playerB)
        multiplayer.castRestartVote(gameId, false)
        asPlayer(playerC)
        val afterC = multiplayer.castRestartVote(gameId, false)
        assertEquals(RestartVoteStatus.SETTLED, afterC!!.status)
        assertEquals(false, afterC.result)

        // A cannot restart a rejected vote
        asPlayer(playerA)
        assertNull(multiplayer.executeRestart(gameId))
        assertFalse(storedFiles.containsKey(restartMarkerFileName()))
    }

    @Test
    fun timeoutSettlesWithDefaultAgree() = runVoteTest {
        // Vote with timeout 0: any refresh settles it, non-voters count as agreeing
        multiplayer.startRestartVote(gameId, 10, 0)
        asPlayer(playerB)
        multiplayer.refreshRestartVote(gameId)
        val cached = multiplayer.getCachedRestartVote(gameId)
        assertEquals(RestartVoteStatus.SETTLED, cached!!.status)
        assertTrue(cached.result!!) // A yes + B default = 2 >= 1
    }

    @Test
    fun nonVotingPlayerCanStillVoteAfterTimeoutVoteIsOpen() = runVoteTest {
        // A initiates; B votes late but before settling: the vote must not have settled yet,
        // so B's vote counts and the result is by explicit votes
        multiplayer.startRestartVote(gameId, 10, 60)
        asPlayer(playerB)
        val afterB = multiplayer.castRestartVote(gameId, true)
        assertEquals(RestartVoteStatus.SETTLED, afterB!!.status)
        assertTrue(afterB.result!!)
        assertEquals(setOf(playerA, playerB), afterB.votes.keys)
    }

    @Test
    fun voteCanBeChangedBeforeSettling() = runVoteTest {
        // Three players so that changing a vote happens while the vote is still open
        game = createOnlineGame(listOf(playerA, playerB, playerC))
        UncivGame.Current.gameInfo = game
        multiplayer.startRestartVote(gameId, 10, 60)
        asPlayer(playerB)
        multiplayer.castRestartVote(gameId, false)
        // B changes their mind - still allowed, C hasn't voted yet
        val changed = multiplayer.castRestartVote(gameId, true)
        assertEquals(RestartVoteStatus.OPEN, changed!!.status)
        assertEquals(true, changed.votes[playerB])
        asPlayer(playerC)
        val settled = multiplayer.castRestartVote(gameId, true)
        assertEquals(RestartVoteStatus.SETTLED, settled!!.status)
        assertTrue(settled.result!!) // A + B + C yes = 3 >= ceil(3/2) = 2
    }

    @Test
    fun openVoteDoesNotRestart() = runVoteTest {
        multiplayer.startRestartVote(gameId, 10, 60)
        assertNull(multiplayer.executeRestart(gameId))
        assertFalse(storedFiles.containsKey(restartMarkerFileName()))
    }

    @Test
    fun crashedExecutorIsTakenOverAfterTimeout() = runVoteTest {
        // Vote passes first
        multiplayer.startRestartVote(gameId, 10, 60)
        asPlayer(playerB)
        multiplayer.castRestartVote(gameId, true)
        // Another client (C) claimed the marker but crashed 3 minutes ago (> 2 min takeover timeout)
        val staleMarker = RestartMarker().apply {
            status = RestartMarkerStatus.RESTARTING
            executorPlayerId = playerC
            startedAtMillis = System.currentTimeMillis() - 3 * 60 * 1000L
        }
        writeRestartMarker(staleMarker)
        asPlayer(playerA)
        val newGameId = multiplayer.executeRestart(gameId)
        assertNotNull(newGameId)
        val marker = storedRestartMarker()
        assertEquals(RestartMarkerStatus.DONE, marker.status)
        assertEquals(playerA, marker.executorPlayerId)
        assertEquals(newGameId, marker.newGameId)
    }

    @Test
    fun activeExecutorIsNotInterrupted() = runVoteTest {
        multiplayer.startRestartVote(gameId, 10, 60)
        asPlayer(playerB)
        multiplayer.castRestartVote(gameId, true)
        val activeMarker = RestartMarker().apply {
            status = RestartMarkerStatus.RESTARTING
            executorPlayerId = playerC
            startedAtMillis = System.currentTimeMillis() - 10_000L // still within takeover timeout
        }
        writeRestartMarker(activeMarker)
        asPlayer(playerA)
        assertNull(multiplayer.executeRestart(gameId)) // someone else is on it
    }

    @Test
    fun doneMarkerReturnsNewGameIdWithoutRegenerating() = runVoteTest {
        multiplayer.startRestartVote(gameId, 10, 60)
        asPlayer(playerB)
        multiplayer.castRestartVote(gameId, true)
        val doneMarker = RestartMarker().apply {
            status = RestartMarkerStatus.DONE
            executorPlayerId = playerC
            startedAtMillis = System.currentTimeMillis()
            newGameId = "already-restarted-game-id"
        }
        writeRestartMarker(doneMarker)
        asPlayer(playerA)
        val newGameId = multiplayer.executeRestart(gameId)
        assertEquals("already-restarted-game-id", newGameId)
        // No new game was generated/uploaded
        assertFalse(storedFiles.containsKey("already-restarted-game-id"))
    }

    @Test
    fun restartVoteFileIsAccessibleToBothPlayers() = runVoteTest {
        // The vote file must be readable/writable by any player, not just the current-turn one
        multiplayer.startRestartVote(gameId, 10, 60)
        val raw = multiplayer.multiplayerServer.fileStorage()
            .loadFileData(restartVoteFileName())
        assertTrue(raw.isNotEmpty())
        asPlayer(playerB)
        multiplayer.castRestartVote(gameId, true)
        val finalVote = storedRestartVote()
        assertEquals(setOf(playerA, playerB), finalVote.votes.keys)
    }

    @Test
    fun missingVoteFileIsHandledGracefully() = runVoteTest {
        asPlayer(playerB)
        assertNull(multiplayer.fetchRestartVote(gameId))
        assertNull(multiplayer.fetchRestartMarker(gameId))
        assertNull(multiplayer.getCachedRestartVote(gameId))
        assertNull(multiplayer.castRestartVote(gameId, true))
        assertNull(multiplayer.executeRestart(gameId))
    }

    //endregion
}

package com.unciv.logic.multiplayer

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.unciv.logic.multiplayer.storage.MultiplayerServer
import com.unciv.testing.BaseTestRunner
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * End-to-end check of the simultaneous-turn HTTP contract.
 *
 * A local server implements the same routes as UncivServer / server-ts (`/files/{name}`,
 * `/simultaneous-turn-operations/{gameId}`, `/simultaneous-turn-lock/{gameId}[/renew]`), so this
 * exercises the Kotlin client's wire format - method, path, body, expected status code - which unit
 * tests against the in-memory model cannot catch.
 */
@RunWith(BaseTestRunner::class)
class SimultaneousTurnServerIntegrationTest {

    private lateinit var httpServer: HttpServer
    private lateinit var server: MultiplayerServer
    private val storedFiles = ConcurrentHashMap<String, String>()
    private val storedOperations = ConcurrentHashMap<String, MutableList<SimultaneousTurnOperation>>()
    private val locks = ConcurrentHashMap<String, String>()
    private val gameId = "simultaneous-e2e"
    private var atomicOpsSupported = true
    private var atomicAppendRejected = false
    /** Simulates a legacy whole-file write that answers 200 without keeping the data. */
    private var legacyOpsWritesDropped = false

    companion object {
        /** Same headless adjustment the other multiplayer integration test needs. */
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
    fun setUp() {
        httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        httpServer.createContext("/files/") { handleFileRequest(it) }
        httpServer.createContext("/simultaneous-turn-operations/") { handleOperationsRequest(it) }
        httpServer.createContext("/simultaneous-turn-lock/") { handleLockRequest(it) }
        httpServer.executor = Executors.newCachedThreadPool()
        httpServer.start()
        server = MultiplayerServer(
            "http://127.0.0.1:${httpServer.address.port}",
            mapOf("Authorization" to "test")
        )
    }

    @After
    fun tearDown() {
        httpServer.stop(0)
    }

    @Test
    fun operationsAppendAndDownloadThroughTheServerApi() {
        val a = SimultaneousTurnOperation(1, "playerA", 1, "unit.move", "{}")
        val b = SimultaneousTurnOperation(1, "playerB", 1, "unit.move", "{}")
        runBlocking {
            assertTrue(server.appendSimultaneousTurnOperations(gameId, SimultaneousTurnOperations.encode(listOf(a))))
            assertTrue(server.appendSimultaneousTurnOperations(gameId, SimultaneousTurnOperations.encode(listOf(b))))
            val downloaded = SimultaneousTurnOperations.decode(server.loadSimultaneousTurnOperations(gameId)!!)
            assertEquals(listOf("playerA", "playerB"), downloaded.map { it.playerId })
        }
    }

    @Test
    fun appendingTheSameOperationTwiceIsIdempotent() {
        val op = SimultaneousTurnOperation(1, "playerA", 1, "unit.move", "{}")
        runBlocking {
            assertTrue(server.appendSimultaneousTurnOperations(gameId, SimultaneousTurnOperations.encode(listOf(op))))
            assertTrue(server.appendSimultaneousTurnOperations(
                gameId, SimultaneousTurnOperations.encode(listOf(op.copy(payload = "{other}")))
            ))
            val downloaded = SimultaneousTurnOperations.decode(server.loadSimultaneousTurnOperations(gameId)!!)
            assertEquals(1, downloaded.size)
            // The server keeps the first operation it saw for a (turn, playerId, sequence) key.
            assertEquals("{}", downloaded.single().payload)
        }
    }

    @Test
    fun downloadFallsBackToTheLegacyOperationsFile() {
        // A fixed createdAtMillis, not the time-based default: libgdx's usePrototypes omits any field
        // equal to the no-arg prototype's value, so a real timestamp can silently round-trip to a new one.
        val op = SimultaneousTurnOperation(2, "playerA", 5, "unit.move", "{}", 123)
        runBlocking {
            server.fileStorage().saveFileData("$gameId.ops", SimultaneousTurnOperations.encode(listOf(op)))
            assertEquals(listOf(op), SimultaneousTurnOperations.download(server, gameId))
        }
    }

    @Test
    fun operationsRejectedByTheAtomicAppendAreStillSeenByDownload() {
        // Regression: a turn whose upload exceeds the server's per-request limit (413) falls back to
        // the legacy whole-file store. Download used to return the atomic store as soon as it had any
        // row, so those fallback operations were invisible to everyone and silently dropped.
        val atomicOp = SimultaneousTurnOperation(1, "playerA", 1, "unit.move", "{}", 123)
        val rejectedOp = SimultaneousTurnOperation(1, "playerB", 2, "unit.move", "{}", 456)
        runBlocking {
            assertTrue(server.appendSimultaneousTurnOperations(gameId, SimultaneousTurnOperations.encode(listOf(atomicOp))))
            atomicAppendRejected = true
            SimultaneousTurnOperations.upload(server, gameId, listOf(rejectedOp))
            assertEquals(
                setOf("playerA", "playerB"),
                SimultaneousTurnOperations.download(server, gameId).map { it.playerId }.toSet()
            )
        }
    }

    @Test
    fun anUploadLostByBothStoresIsReportedInsteadOfSilentlyDropped() {
        // Regression: after three failed whole-file attempts the upload returned normally. The caller
        // then believed the turn was submitted, so it settled without this player's actions and the
        // player kept playing as if they had been recorded.
        val op = SimultaneousTurnOperation(1, "playerA", 1, "unit.move", "{}", 123)
        atomicAppendRejected = true
        legacyOpsWritesDropped = true
        assertThrows(SimultaneousTurnOperationUploadException::class.java) {
            runBlocking { SimultaneousTurnOperations.upload(server, gameId, listOf(op)) }
        }
    }

    @Test
    fun legacyUploadFallsBackAndKeepsBothClientsOperations() {
        atomicOpsSupported = false
        val a = SimultaneousTurnOperation(1, "playerA", 1, "unit.move", "{}")
        val b = SimultaneousTurnOperation(1, "playerB", 1, "unit.move", "{}")
        runBlocking {
            SimultaneousTurnOperations.upload(server, gameId, listOf(a))
            SimultaneousTurnOperations.upload(server, gameId, listOf(b))
            val ops = SimultaneousTurnOperations.decode(storedFiles["$gameId.ops"]!!)
            assertEquals(setOf("playerA", "playerB"), ops.map { it.playerId }.toSet())
        }
    }

    @Test
    fun settlementLockIsExclusiveAndRenewableThroughTheServerApi() {
        runBlocking {
            assertTrue(server.acquireSimultaneousTurnSettlementLock(gameId, 1, "playerA"))
            assertFalse("a second player must not take the settlement lock", server.acquireSimultaneousTurnSettlementLock(gameId, 1, "playerB"))
            assertTrue(server.renewSimultaneousTurnSettlementLock(gameId, 1, "playerA"))
            assertFalse("a non-owner must not renew the settlement lock", server.renewSimultaneousTurnSettlementLock(gameId, 1, "playerB"))
            server.releaseSimultaneousTurnSettlementLock(gameId, 1, "playerA")
            assertTrue("the lock is free after release", server.acquireSimultaneousTurnSettlementLock(gameId, 1, "playerB"))
        }
    }

    /** The real server derives the owner from authentication, so each player needs its own credentials. */
    private fun playerServer(playerId: String) = MultiplayerServer(
        "http://127.0.0.1:${httpServer.address.port}",
        mapOf("Authorization" to playerId)
    )

    private fun handleFileRequest(exchange: HttpExchange) {
        try {
            val name = exchange.requestURI.path.removePrefix("/files/")
            when (exchange.requestMethod) {
                "PUT" -> {
                    val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                    // A dropped write still answers 200, so the client cannot tell from the response.
                    if (legacyOpsWritesDropped && name.endsWith(".ops")) storedFiles.remove(name)
                    else storedFiles[name] = body
                    exchange.sendResponseHeaders(200, -1)
                }
                "GET" -> {
                    val data = storedFiles[name]
                    if (data == null) exchange.sendResponseHeaders(404, -1)
                    else {
                        val bytes = data.toByteArray(Charsets.UTF_8)
                        exchange.sendResponseHeaders(200, bytes.size.toLong())
                        exchange.responseBody.write(bytes)
                    }
                }
                "DELETE" -> {
                    storedFiles.remove(name)
                    exchange.sendResponseHeaders(200, -1)
                }
                else -> exchange.sendResponseHeaders(405, -1)
            }
        } finally {
            exchange.close()
        }
    }

    private fun handleOperationsRequest(exchange: HttpExchange) {
        try {
            val id = exchange.requestURI.path.removePrefix("/simultaneous-turn-operations/")
            when (exchange.requestMethod) {
                "POST" -> {
                    if (!atomicOpsSupported) {
                        exchange.sendResponseHeaders(404, -1)
                        return
                    }
                    if (atomicAppendRejected) {
                        // Mirrors server-ts rejecting an over-limit payload with HTTP 413.
                        exchange.sendResponseHeaders(413, -1)
                        return
                    }
                    val incoming = SimultaneousTurnOperations.decode(
                        exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                    )
                    synchronized(storedOperations) {
                        val list = storedOperations.getOrPut(id) { ArrayList() }
                        val existing = list.map { Triple(it.turn, it.playerId, it.sequence) }.toSet()
                        list += incoming.filterNot { Triple(it.turn, it.playerId, it.sequence) in existing }
                    }
                    exchange.sendResponseHeaders(200, -1)
                }
                "GET" -> {
                    val list = synchronized(storedOperations) { storedOperations[id]?.toList() }
                    if (!atomicOpsSupported || list.isNullOrEmpty()) {
                        exchange.sendResponseHeaders(404, -1)
                        return
                    }
                    val body = SimultaneousTurnOperations.encode(
                        list.sortedWith(compareBy({ it.turn }, { it.playerId }, { it.sequence }))
                    )
                    val bytes = body.toByteArray(Charsets.UTF_8)
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.write(bytes)
                }
                else -> exchange.sendResponseHeaders(405, -1)
            }
        } finally {
            exchange.close()
        }
    }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }

    private fun handleLockRequest(exchange: HttpExchange) {
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

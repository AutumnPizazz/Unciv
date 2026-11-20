package com.unciv.logic.multiplayer.storage

import com.badlogic.gdx.Net
import com.badlogic.gdx.utils.Base64Coder
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.logic.UncivShowableException
import com.unciv.utils.debug
import java.io.InputStream

internal class UncivServerFileStorage(
    private val serverUrl: String,
    private val authHeader: Map<String, String>
) : FileStorage {
    private val timeout = 30000
    private val turnOperationsHeaders = authHeader + ("X-Unciv-Turn-Protocol" to "1")
    private val realtimeTurnHeaders = authHeader + ("X-Unciv-Turn-Protocol" to "2")

    override fun saveFileData(fileName: String, data: String) {
        SimpleHttp.sendRequest(Net.HttpMethods.PUT, fileUrl(fileName), content=data, timeout=timeout, header=authHeader) {
                success, result, code ->
            if (!success) {
                debug("Error from UncivServer during save: %s", result)
                when (code) {
                    401 -> throw MultiplayerAuthException(Exception(result))
                    409 -> if (fileName.endsWith("_restart")) throw FileStorageConflictException()
                        else throw serverError(code, result)
                    else -> throw serverError(code, result)
                }
            }
        }
    }

    override fun loadFileData(fileName: String): InputStream {
        try {
            return SimpleHttp.openStream(Net.HttpMethods.GET, fileUrl(fileName), "", timeout, authHeader).body
        } catch (ex: SimpleHttp.HttpStatusException) {
            debug("Error from UncivServer during load: %s", ex.message)
            when (ex.code) {
                404 -> throw MultiplayerFileNotFoundException(ex)
                400 -> {
                    val error = serverError(ex.code, ex.message)
                    // Older UncivSrv versions reject unsupported sidecar suffixes as invalid IDs.
                    if (error.message == "无效的游戏ID格式" &&
                        (fileName.endsWith("_restartvote") || fileName.endsWith("_restart")))
                        throw MultiplayerFileNotFoundException(ex)
                    throw error
                }
                else -> throw serverError(ex.code, ex.message.orEmpty())
            }
        }
    }

    override fun getFileMetaData(fileName: String): FileMetaData {
        TODO("Not yet implemented")
    }

    override fun appendSimultaneousTurnOperations(gameId: String, operations: String): Boolean {
        var appended = false
        SimpleHttp.sendRequest(
            Net.HttpMethods.POST,
            "$serverUrl/simultaneous-turn-operations/$gameId",
            content = operations,
            timeout = timeout,
            header = turnOperationsHeaders
        ) { success, _, code ->
            appended = success && (code == 200 || code == 204)
        }
        return appended
    }

    override fun loadSimultaneousTurnOperations(gameId: String): String? {
        var data: String? = null
        SimpleHttp.sendGetRequest("$serverUrl/simultaneous-turn-operations/$gameId", timeout, turnOperationsHeaders) { success, result, code ->
            if (success) data = result else if (code != 404) throw serverError(code, result)
        }
        return data
    }

    override fun appendRealtimeTurnAction(gameId: String, request: String): String {
        var response: String? = null
        SimpleHttp.sendRequest(
            Net.HttpMethods.POST,
            "$serverUrl/simultaneous-turn-actions/$gameId",
            content = request,
            timeout = timeout,
            header = realtimeTurnHeaders
        ) { success, result, code ->
            if (success && code == 200) response = result
            else if (code == 401) throw MultiplayerAuthException(Exception(result))
            else if (code == 409) throw MultiplayerTurnActionConflictException(result)
            else throw serverError(code, result)
        }
        return response ?: throw Exception("Real-time turn action response was empty")
    }

    override fun loadRealtimeTurnActions(gameId: String, turn: Int, after: Long): String {
        var response: String? = null
        SimpleHttp.sendGetRequest(
            "$serverUrl/simultaneous-turn-actions/$gameId?turn=$turn&after=$after",
            timeout,
            realtimeTurnHeaders
        ) { success, result, code ->
            if (success && code == 200) response = result
            else if (code == 401) throw MultiplayerAuthException(Exception(result))
            else if (code == 409) throw MultiplayerTurnActionConflictException(result)
            else throw serverError(code, result)
        }
        return response ?: throw Exception("Real-time turn action feed was empty")
    }

    override fun markRealtimeTurnReady(gameId: String, request: String): String {
        var response: String? = null
        SimpleHttp.sendRequest(
            Net.HttpMethods.POST,
            "$serverUrl/simultaneous-turn-ready/$gameId",
            content = request,
            timeout = timeout,
            header = realtimeTurnHeaders
        ) { success, result, code ->
            if (success && code == 200) response = result
            else if (code == 401) throw MultiplayerAuthException(Exception(result))
            else if (code == 409) throw MultiplayerTurnActionConflictException(result)
            else throw serverError(code, result)
        }
        return response ?: throw Exception("Real-time turn ready response was empty")
    }

    fun acquireSimultaneousTurnSettlementLock(gameId: String, turn: Int): String? {
        var lease: String? = null
        SimpleHttp.sendRequest(
            Net.HttpMethods.POST,
            "$serverUrl/simultaneous-turn-lock/$gameId",
            content = turn.toString(),
            timeout = timeout,
            header = realtimeTurnHeaders
        ) { success, result, code ->
            if (success && code == 201 && result.isNotBlank()) lease = result
            else if (code != 409) throw serverError(code, result)
        }
        return lease
    }

    fun renewSimultaneousTurnSettlementLock(gameId: String, lease: String): Boolean {
        var renewed = false
        SimpleHttp.sendRequest(
            Net.HttpMethods.POST,
            "$serverUrl/simultaneous-turn-lock/$gameId/renew",
            content = lease,
            timeout = timeout,
            header = realtimeTurnHeaders
        ) { success, _, code ->
            renewed = success && code == 204
        }
        return renewed
    }

    fun releaseSimultaneousTurnSettlementLock(gameId: String, lease: String) {
        SimpleHttp.sendRequest(
            Net.HttpMethods.DELETE,
            "$serverUrl/simultaneous-turn-lock/$gameId",
            content = lease,
            timeout = timeout,
            header = realtimeTurnHeaders
        ) { _, _, _ -> }
    }

    fun saveSimultaneousTurn(
        gameId: String, content: String, preview: String, lease: String?, roomCode: String?,
        restartFrom: String? = null, restartClaim: Long = 0,
    ): Boolean {
        val request = JsonValue(JsonValue.ValueType.`object`).apply {
            addChild("content", JsonValue(content))
            addChild("preview", JsonValue(preview))
            addChild("lease", JsonValue(lease.orEmpty()))
            if (roomCode != null) addChild("roomCode", JsonValue(roomCode))
            if (restartFrom != null) {
                addChild("restartFrom", JsonValue(restartFrom))
                addChild("restartClaim", JsonValue(restartClaim))
            }
        }
        var saved = false
        SimpleHttp.sendRequest(
            Net.HttpMethods.POST,
            "$serverUrl/simultaneous-turn-saves/$gameId",
            content = request.toJson(JsonWriter.OutputType.json),
            timeout = timeout,
            header = realtimeTurnHeaders
        ) { success, result, code ->
            if (success && code == 204) saved = true
            else if (code == 401) throw MultiplayerAuthException(Exception(result))
            else if (code == 422 && lease == null) throw MultiplayerRoomCodeException()
            else if (code != 409) throw serverError(code, result)
        }
        return saved
    }

    override fun deleteFile(fileName: String) {
        SimpleHttp.sendRequest(Net.HttpMethods.DELETE, fileUrl(fileName), content="", timeout=timeout, header=authHeader) {
                success, result, code ->
            if (!success) {
                when (code) {
                    404 -> throw MultiplayerFileNotFoundException(Exception(result))
                    else -> throw serverError(code, result)
                }
            }
        }
    }

    override fun authenticate(userId: String, password: String): Boolean {
        var authenticated = false
        val preEncodedAuthValue = "$userId:$password"
        val submittedAuthHeader = mapOf("Authorization" to "Basic ${Base64Coder.encodeString(preEncodedAuthValue)}")
        SimpleHttp.sendGetRequest("$serverUrl/auth", timeout=timeout, header=submittedAuthHeader) {
                success, result, code ->
            if (!success) {
                debug("Error from UncivServer during authentication: %s", result)
                when (code) {
                    401 -> throw MultiplayerAuthException(Exception(result))
                    else -> throw Exception(result)
                }
            } else {
                authenticated = true
            }
        }
        return authenticated
    }

    override fun checkAuthStatus(userId: String, password: String): AuthStatus {
        var authStatus = AuthStatus.UNKNOWN
        val preEncodedAuthValue = "$userId:$password"
        val submittedAuthHeader = mapOf("Authorization" to "Basic ${Base64Coder.encodeString(preEncodedAuthValue)}")
        SimpleHttp.sendGetRequest("$serverUrl/auth", timeout = timeout, header = submittedAuthHeader) { _, _, code ->
            authStatus = when (code) {
                200 -> AuthStatus.VERIFIED
                204 -> AuthStatus.UNREGISTERED
                401 -> AuthStatus.UNAUTHORIZED
                else -> AuthStatus.UNKNOWN
            }
        }
        return authStatus
    }

    override fun setPassword(newPassword: String): Boolean {
        var setSuccessful = false
        SimpleHttp.sendRequest(Net.HttpMethods.PUT, "$serverUrl/auth", content=newPassword, timeout=timeout, header=authHeader) {
                success, result, code ->
            if (!success) {
                debug("Error from UncivServer during password set: %s", result)
                when (code) {
                    401 -> throw MultiplayerAuthException(Exception(result))
                    else -> throw Exception(result)
                }
            } else {
                setSuccessful = true
            }
        }
        return setSuccessful
    }

    /**
     * 把服务端的错误响应转成可以直接展示给玩家的异常。
     *
     * 大多数接口的错误响应是 JSON（`{"type":"error","message":"…"}`），
     * 但提示性响应（例如对局被冷归档、正在从网盘恢复）是纯文本，两种都要能读。
     */
    private fun serverError(code: Int?, result: String): UncivShowableException {
        val detail = Exception("$code $result")
        val json = try {
            JsonReader().parse(result)
        } catch (_: Exception) {
            null
        }
        if (json == null || !json.isObject)
            return UncivShowableException(result, detail)
        val message = json.get("message")
        if (message == null || !message.isString)
            return UncivShowableException(result, detail)
        return UncivShowableException(message.asString(), detail)
    }

    private fun fileUrl(fileName: String) = "$serverUrl/files/$fileName"
}

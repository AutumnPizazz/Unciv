package com.unciv.logic.multiplayer.storage

import com.unciv.json.json
import com.unciv.utils.Log
import com.unciv.utils.debug
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.Timer
import kotlin.concurrent.timer


/** Downloads the optional music pack from its upstream host. */
object DropBox {
    private var remainingRateLimitSeconds = 0
    private var rateLimitTimer: Timer? = null

    private fun dropboxApi(url: String, data: String = "", contentType: String = "", dropboxApiArg: String = ""): InputStream? {

        if (remainingRateLimitSeconds > 0)
            throw FileStorageRateLimitReached(remainingRateLimitSeconds)

        // URL(string) is deprecated, URI.toUrl(string) API level 36:
        with(URI(url).toURL().openConnection() as HttpURLConnection) {
            requestMethod = "POST"  // default is GET

            @Suppress("SpellCheckingInspection")
            setRequestProperty("Authorization", "Bearer LTdBbopPUQ0AAAAAAAACxh4_Qd1eVMM7IBK3ULV3BgxzWZDMfhmgFbuUNF_rXQWb")

            if (dropboxApiArg != "") setRequestProperty("Dropbox-API-Arg", dropboxApiArg)
            if (contentType != "") setRequestProperty("Content-Type", contentType)

            doOutput = true

            try {
                if (data != "") {
                    val postData: ByteArray = data.toByteArray(Charsets.UTF_8)
                    val outputStream = DataOutputStream(outputStream)
                    outputStream.write(postData)
                    outputStream.flush()
                }

                return inputStream
            } catch (ex: Exception) {
                debug("Dropbox exception", ex)
                val reader = BufferedReader(InputStreamReader(errorStream, Charsets.UTF_8))
                val responseString = reader.readText()
                debug("Response: %s", responseString)

                val error = json().fromJson(ErrorResponse::class.java, responseString)
                // Throw Exceptions based on the HTTP response from dropbox
                when {
                    error.error_summary.startsWith("too_many_requests/") -> triggerRateLimit(error)
                    error.error_summary.startsWith("path/not_found/") -> throw MultiplayerFileNotFoundException(ex)
                    error.error_summary.startsWith("path/conflict/file") -> throw FileStorageConflictException()
                }

                return null
            } catch (error: Error) {
                Log.error("Dropbox error", error)
                debug("Error stream: %s", { BufferedReader(InputStreamReader(errorStream, Charsets.UTF_8)).readText() })
                return null
            }
        }
    }

    fun downloadFile(fileName: String): InputStream {
        val response = dropboxApi("https://content.dropboxapi.com/2/files/download",
                contentType = "text/plain", dropboxApiArg = "{\"path\":\"$fileName\"}")
        return response!!
    }

    /**
     * If the dropbox rate limit is reached for this bearer token we strictly have to wait for the
     * specified retry_after seconds before trying again. If non is supplied or can not be parsed
     * the default value of 5 minutes will be used.
     * Any attempt before the rate limit is dropped again will also contribute to the rate limit
     */
    private fun triggerRateLimit(response: ErrorResponse) {
        remainingRateLimitSeconds = response.error?.retry_after?.toIntOrNull() ?: 300

        rateLimitTimer = timer("RateLimitTimer", true, 0, 1000) {
            remainingRateLimitSeconds--
            if (remainingRateLimitSeconds == 0)
                rateLimitTimer?.cancel()
        }
        throw FileStorageRateLimitReached(remainingRateLimitSeconds)
    }

    @Suppress("PropertyName")
    private class ErrorResponse {
        var error_summary = ""
        var error: Details? = null

        class Details {
            var retry_after = ""
        }
    }
}

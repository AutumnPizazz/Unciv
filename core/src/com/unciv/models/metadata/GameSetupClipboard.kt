package com.unciv.models.metadata

import com.badlogic.gdx.utils.Base64Coder
import com.badlogic.gdx.utils.JsonReader
import com.unciv.json.json

/**
 * Encodes a [GameSetupInfo] to a clipboard-friendly base64 text and decodes it back.
 *
 * The exported text is a self-describing prefix plus the base64-encoded JSON of the setup
 * ("UncivGameSetup:1:<base64>"), so players can share or restore a complete new game
 * configuration (including online multiplayer player IDs).
 *
 * Note: base64 is an encoding, not encryption - the exported text contains all settings
 * in readable form, including online multiplayer player IDs.
 */
object GameSetupClipboard {
    private const val FORMAT_ID = "UncivGameSetup"
    private const val FORMAT_VERSION = 1

    /** Encode [setup] to clipboard text. */
    fun encode(setup: GameSetupInfo): String {
        val jsonText = json().toJson(setup)
        val base64 = String(Base64Coder.encode(jsonText.toByteArray(Charsets.UTF_8)))
        return "$FORMAT_ID:$FORMAT_VERSION:$base64"
    }

    /** Decode clipboard [text] to a [GameSetupInfo]. Throws [IllegalArgumentException] on invalid input. */
    fun decode(text: String): GameSetupInfo {
        val parts = text.split(':')
        require(parts.size == 3 && parts[0] == FORMAT_ID) { "Not a game setup export" }
        val version = parts[1].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid game setup export version")
        require(version == FORMAT_VERSION) { "Unsupported game setup export version: $version" }
        val jsonText = try {
            String(Base64Coder.decode(parts[2]), Charsets.UTF_8)
        } catch (ex: Exception) {
            throw IllegalArgumentException("Invalid base64 data", ex)
        }
        return try {
            json().fromJson(GameSetupInfo::class.java, jsonText)
                ?: throw IllegalArgumentException("Could not parse game setup JSON")
        } catch (ex: IllegalArgumentException) {
            throw ex
        } catch (ex: Exception) {
            throw IllegalArgumentException("Could not parse game setup JSON", ex)
        }
    }

    /**
     * Copy all serialized values from [source] into [target] **in place**, keeping the [target] instances.
     *
     * Needed because the New Game screen's option tables hold references to the original
     * [GameSetupInfo.gameParameters] and [GameSetupInfo.mapParameters] objects.
     */
    fun copyValuesInto(target: GameSetupInfo, source: GameSetupInfo) {
        val json = json()
        json.readFields(target.gameParameters, JsonReader().parse(json.toJson(source.gameParameters)))
        json.readFields(target.mapParameters, JsonReader().parse(json.toJson(source.mapParameters)))
    }
}

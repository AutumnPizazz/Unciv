package com.unciv.logic.multiplayer

import com.unciv.logic.GameInfoPreview
import com.unciv.logic.event.Event

interface HasMultiplayerGameName {
    val name: String
}

interface MultiplayerGameUpdateEnded : Event, HasMultiplayerGameName
interface MultiplayerGameUpdateSucceeded : Event, HasMultiplayerGameName {
    val preview: GameInfoPreview
}

/**
 * Gets sent when a game successfully updated
 */
class MultiplayerGameUpdated(
    override val name: String,
    override val preview: GameInfoPreview,
) : MultiplayerGameUpdateEnded, MultiplayerGameUpdateSucceeded

/**
 * Gets sent when a game errored while updating
 */
class MultiplayerGameUpdateFailed(
    override val name: String,
    val error: Throwable
) : MultiplayerGameUpdateEnded
/**
 * Gets sent when a game updated successfully, but nothing changed
 */
class MultiplayerGameUpdateUnchanged(
    override val name: String,
    override val preview: GameInfoPreview
) : MultiplayerGameUpdateEnded, MultiplayerGameUpdateSucceeded

/**
 * Gets sent when a game starts updating
 */
class MultiplayerGameUpdateStarted(
    override val name: String
) : Event, HasMultiplayerGameName

/**
 * Gets sent when a game's name got changed
 */
class MultiplayerGameNameChanged(
    override val name: String,
    val newName: String
) : Event, HasMultiplayerGameName

/** Fired when an online status response is received via WebSocket in polling multiplayer. */
class OnlineStatusUpdated(
    val gameId: String,
    val civName: String
) : Event

/** Fired when a restart vote's state changed (new vote cast, settled, removed...).
 *  The new state is available via [com.unciv.UncivGame.onlineMultiplayer]'s restart vote cache. */
class RestartVoteUpdated(
    val gameId: String
) : Event

/** Fired when a WebSocket chat signal announces a restart vote state change. */
class RestartVoteSignalReceived(
    val gameId: String
) : Event


class SimultaneousTurnOperationReceived(
    val gameId: String,
    val turn: Int,
    val playerId: String,
    val sequence: Long
) : Event

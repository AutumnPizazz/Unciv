# Polling multiplayer

> Author: AutumnPizazz
>
> Date: 2026-05-21

## Overview

Polling multiplayer is a new multiplayer mode that UncivCN adds on top of the original dynamic-turn system. Original multiplayer is strictly sequential: after the current player finishes their turn and clicks "next turn", the save is passed on only after AI resolution — the wait depends on how long the opponent takes. Polling multiplayer splits the same turn into fixed-length time slices and passes the save between players slice by slice, capping the wait from "as long as the opponent wants" to a few seconds.

It is a purely additive feature — without a polling interval selected, behavior is identical to the original, and old saves load normally.

## Enabling

When creating a new game, check **Online Multiplayer**; a **Polling interval** dropdown appears below it:

| Option | Meaning |
|------|------|
| Off | Polling disabled, original dynamic turns |
| 5s / 10s / 15s / 20s / 30s | Length of each player's action window per slice |

The host picks an interval and creates the game; the whole game then runs in polling rhythm.

## Game flow

### Rotation within a turn

When a turn starts, the current player gets an action window, during which units and cities can be operated normally. When the window ends:

- **Player clicks "I'm done"**: the player is marked as finished for this turn and the save is passed to the next unfinished player; that player is no longer polled this turn.
- **Timer expires**: the save is automatically passed to the next unfinished player, but the current player is **not** marked finished — when the rotation comes back they can keep playing.
- **Only one unfinished player left**: that player is no longer switched away from; the timer keeps resetting until they click "I'm done".

### Turn advancement

When all surviving human players have clicked "I'm done", the turn advances: each human player's turn is ended in order, all AI civilizations are processed, finish flags are reset, and a new rotation starts for all human players of the new turn.

### Countdown display

The top bar shows the seconds left in the current window next to the turn counter, color-coded by remaining time:

- Green: more than half remaining
- Gold: 25% to half remaining
- Coral: less than 25% remaining

It is hidden when it is not your turn or when not in polling mode.

## Technical implementation

### Save passing

Polling multiplayer reuses the original multiplayer upload/download infrastructure. At each window end, the current player uploads the modified full game state to the server; the next player detects the update via polling or WebSocket push and downloads it. Only one player holds the action rights at any time, so no merge conflicts need handling.

### WebSocket push

To reduce polling latency, clients automatically establish a WebSocket connection when entering polling multiplayer. After a player uploads the save, the server immediately pushes a `GameUpdated` message to all online clients in the room, triggering an immediate download of the latest state. This reuses the existing chat WebSocket endpoint; no extra configuration needed.

### New fields

| Field | Location | Description |
|------|------|------|
| `pollingIntervalSeconds` | `GameParameters` | Polling interval in seconds; 0 = disabled |
| `playersFinishedThisTurn` | `GameInfo` | Set of civ IDs that confirmed finished this turn |

### Server-side changes

The server `Response` type gains a `GameUpdated` message, broadcast to all WebSocket subscribers of the room after a successful file upload.

### Related files

Core logic spans the following source files:

| File | Role |
|------|------|
| `GameParameters.kt` | new `pollingIntervalSeconds` field |
| `GameInfo.kt` | new `playersFinishedThisTurn` set, `nextTurnPolling()` and helper queries |
| `NextTurnAction.kt` | new `FinishAction` enum value |
| `NextTurnButton.kt` | button text appends countdown in polling mode |
| `WorldScreen.kt` | new timer coroutine, `finishPollingTurn()` / `passPollingTurn()` |
| `WorldScreenTopBarResources.kt` | top-bar countdown display |
| `GameOptionsTable.kt` | polling interval selector on the game-creation screen |
| `Multiplayer.kt` | null-safety fix for `GameInfoPreview.isUsersTurn()` |
| `MultiplayerGamePreview.kt` | null-safety fix for `GameInfoPreview.getCurrentPlayerCiv()` |
| `ChatWebSocket.kt` | client-side `GameUpdated` reception and refresh trigger |
| `UncivServer.kt` | server-side WebSocket broadcast after upload |
| `GameSettings.kt` | default password set to 123456 |

## Notes

- All players in a polling game must configure the multiplayer password in game settings (default 123456), otherwise WebSocket authentication failures affect push delivery.
- Adjust the interval to the number of players: 5–10s for 2 players; 15s+ recommended for 4 or more.
- The server must run with the `-chat` option (on by default) to support WebSocket push.

# Multiplayer

UncivCN multiplayer uses a shared server to exchange game saves. You can use the default server or set a compatible server address in the multiplayer options. For self-hosting, see [Hosting a Multiplayer server](#hosting-a-multiplayer-server).

## How to play

0. Make sure you are all using the same multiplayer server (`Main Menu -> Options -> Multiplayer`)
0. Let all players send you their user ID (`Main Menu -> Multiplayer -> Copy user ID`).
   * (Optional) Add those user IDs to your friend list (`Main Menu -> Multiplayer -> Friends list`).
0. In `Main Menu -> Start new game`, check `Online multiplayer` on the left. On the right, add more human players and input the user IDs of the players you want to play with. Press `Start game!`.
0. The game ID will be automatically put in your clipboard. (If you lost it, you can get it again from `Main Menu -> Multiplayer -> Copy game ID`). Send this game ID to the other players.
0. The other players need to go to `Main Menu -> Multiplayer -> Add multiplayer game` and enter the game ID you just sent them. They can then join the game from this multiplayer screen.

When the host enables restart voting, a surviving player can start a vote from the in-game menu on the configured turn. The vote ends when all surviving players have voted or the deadline passes. Players who miss the deadline count as agreeing; a tie also passes. UncivSrv keeps the vote across reconnects and server restarts. A passed vote creates a new game with the same setup.

## Hosting a Multiplayer server

The Go multiplayer server (UncivSrv) is closed-source and maintained separately. UncivCN no longer distributes Go server Docker images or GitHub Release downloads.

For self-hosting, choose a compatible implementation below and follow its documentation. Support for UncivCN features such as simultaneous turns and restart votes varies by server. Before starting a game, all players should enter the same server URL under **Options → Multiplayer** and select **Check connection to server**.

## Third-party (unofficial) software for hosting your own Unciv server

* [https://github.com/Mape6/Unciv_server](https://github.com/Mape6/Unciv_server) (Python)
* [https://gitlab.com/azzurite/unciv-server](https://gitlab.com/azzurite/unciv-server) (NodeJS)
* [https://github.com/oynqr/rust_unciv_server](https://github.com/oynqr/rust_unciv_server) (Rust)
* [https://github.com/touhidurrr/UncivServer.xyz](https://github.com/touhidurrr/UncivServer.xyz) (TypeScript | Bun)

## Third-party (unofficial) publicly hosted Unciv servers

These servers are run by the community and **not** official servers. These servers may become (temporarily or permanently) unavailable and lose your game saves. They might also collect data like your IP, how often you play, or other data. Use these only if you accept these risks and trust the server owners.

* [`https://uncivserver.xyz/`](https://uncivserver.xyz/) - Run by [@touhidurrr](https://github.com/touhidurrr) on [their Discord](https://discord.gg/H9em4ws8XP) ([Source Code](https://github.com/touhidurrr/UncivServer.xyz/))

# Multiplayer

UncivCN online multiplayer connects to the closed-source Go server (UncivSrv) run by the community at `https://unciv.civgo.top:30123`. The address is fixed in the client. Games hosted on other servers do not transfer automatically.

## How to play

0. In `Main Menu -> Options -> Multiplayer`, select `Check connection`.
0. Let all players send you their user ID (`Main Menu -> Multiplayer -> Copy user ID`).
   * (Optional) Add those user IDs to your friend list (`Main Menu -> Multiplayer -> Friends list`).
0. In `Main Menu -> Start new game`, check `Online multiplayer` on the left. On the right, add more human players and input the user IDs of the players you want to play with. Press `Start game!`.
0. The game ID will be automatically put in your clipboard. (If you lost it, you can get it again from `Main Menu -> Multiplayer -> Copy game ID`). Send this game ID to the other players.
0. The other players need to go to `Main Menu -> Multiplayer -> Add multiplayer game` and enter the game ID you just sent them. They can then join the game from this multiplayer screen.

When the host enables restart voting, a surviving player can start a vote from the in-game menu on the configured turn. The vote ends when all surviving players have voted or the deadline passes. Players who miss the deadline count as agreeing; a tie also passes. UncivSrv keeps the vote across reconnects and server restarts. A passed vote creates a new game with the same setup.

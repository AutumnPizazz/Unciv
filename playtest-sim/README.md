# playtest-sim — 本地双客户端同时回合试玩模拟

这个目录是一次「两个真实客户端打同一局同时回合游戏」的可复现模拟。它不做嘴炮：
两个独立的 `GameInfo` 客户端（A / B）各自行动、各自上传操作，其中一个按
`WorldScreen.finishSimultaneousTurn` 的流程结算，然后双方重新下载并逐字节比对状态。

## 跑一次

```bash
./playtest-sim/run.sh              # 启动真实 server-ts，走真实 HTTP 契约（推荐）
./playtest-sim/run.sh embedded     # 用测试内置 HttpServer 快跑，不依赖 Node
```

`config.properties` 由脚本生成：有 `server=` 时测试连外部服务器，留空时测试自建
内置服务器。没有这个文件（也没有 `-Dplaytest.sim.enabled=true`）时，测试会被
JUnit `Assume` 跳过，因此不影响常规 `./gradlew :tests:test`。

## 产物（全部集中在本目录）

| 路径 | 内容 |
| --- | --- |
| `logs/report.md` | 人类可读的完整过程报告 |
| `logs/simulation.log` | 同样的逐行日志 |
| `clients/clientA/`、`clients/clientB/` | 每回合上传的 `*.ops.json`、下载的 `*.sav`、状态指纹 `*.state.txt` |
| `server/` | real 模式：server-ts 的 SQLite 与 `server.log`；embedded 模式：内置服务器的 `files/`、`operations/` 转储 |

清理或复用：

```bash
rm -rf playtest-sim/server playtest-sim/clients playtest-sim/logs   # 只清产物，保留脚本
```

## 模拟覆盖了什么

- **第 1 回合**：A、B 各自移动单位并做一处 state 改动（文明变量 / 地块改良），
  双方独立上传 → 结算 → 双方下载一致。
- **第 2 回合**：A 先行动上传，然后**中途重载**（丢本地操作日志、把操作序号
  重新播种为墙上时钟），再移动一次并提交。结算必须同时保住重载前后的两批操作
  ——这正是「重载后复用操作序号导致新操作被静默丢弃」那个回归。
- **第 3 回合**：换另一个客户端做结算者，再做一轮移动 + state 改动。

## 相关代码

- 测试主体：`tests/src/com/unciv/logic/multiplayer/SimultaneousTurnPlaytestSimulation.kt`
- 被复刻的客户端记录语义：`core/src/com/unciv/ui/screens/worldscreen/WorldScreen.kt`
- 操作捕获/拆分/合并：`core/src/com/unciv/logic/multiplayer/SimultaneousTurnOperations.kt`
- 回放：`core/src/com/unciv/logic/multiplayer/SimultaneousTurnReplay.kt`
- 服务端：`server-ts/`

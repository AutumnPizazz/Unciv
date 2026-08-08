# 轮询联机机制

> 作者：AutumnPizazz
> 
> 完稿日期：2026 年 5 月 21 日

## 概述

轮询联机是 UncivCN 在原有动态回合机制之上新增的一种联机方式。原版联机采用严格顺序制：当前玩家操作完毕并点击"下一回合"后，经 AI 结算方可移交至下一位玩家，等待时长取决于对手操作速度。轮询联机将同一回合拆分为若干个时间片，存档在玩家之间按时间片轮流传递，从而将等待时间的上限从"对手想多久就多久"压缩为固定的数秒。

该机制为纯增量功能——不选轮询间隔则行为与原版完全一致，旧存档亦可正常加载。

## 启用方式

创建新游戏时，勾选 **Online Multiplayer**，其下方即出现 **Polling interval** 下拉框：

| 选项 | 含义 |
|------|------|
| Off | 关闭轮询，使用原版动态回合 |
| 5s / 10s / 15s / 20s / 30s | 每轮每位玩家的操作窗口时长 |

房主选择间隔后创建游戏，整局即进入轮询节奏。

## 游戏流程

### 回合内轮转

游戏回合开始时，当前玩家获得一个操作窗口。窗口期内可正常操作单位和城市。窗口结束时：

- **玩家点击 "I'm done"**：标记该玩家本回合内不再需要操作，存档传给下一位未完成的玩家。该玩家本轮不再被轮询。
- **计时器到期**：存档自动传给下一位未完成的玩家，但不标记当前玩家完成。待轮转回来时仍可继续操作。
- **仅剩一位玩家未完成**：该玩家不再被切走，计时器反复重置，直至其主动点击 "I'm done"。

### 回合推进

当所有存活的人类玩家均已点击 "I'm done"，回合正式推进：依次结束各人类玩家回合、处理所有 AI 文明、重置完成标记、为新回合的所有人类玩家开启新一轮轮转。

### 倒计时显示

顶栏回合数右侧会显示当前窗口的剩余秒数，颜色随剩余时间变化：

- 绿色：剩余超过一半
- 金色：剩余 25% 至一半
- 珊瑚色：剩余不足 25%

非自己回合或不处于轮询模式时，该位置不显示。

## 技术实现

### 存档传递

轮询联机沿用原版联机的上传/下载基础设施。每次窗口结束时，当前玩家将修改后的完整游戏状态上传至服务器，下一位玩家通过轮询或 WebSocket 推送检测到更新后下载。同一时刻仅一位玩家持操作权，无需处理合并冲突。

### WebSocket 推送

为降低轮询延迟，客户端在进入轮询联机时自动建立 WebSocket 连接。玩家上传存档后，服务器即时推送 `GameUpdated` 消息至同房间所有在线客户端，触发对方立即下载最新状态。该机制复用现有的聊天 WebSocket 端点，无需额外配置。

### 新增字段

| 字段 | 位置 | 说明 |
|------|------|------|
| `pollingIntervalSeconds` | `GameParameters` | 轮询间隔秒数，0 表示关闭 |
| `playersFinishedThisTurn` | `GameInfo` | 本回合已确认完成的文明 ID 集合 |

### 服务端改动

服务端 `Response` 类型新增 `GameUpdated` 消息，文件上传成功后向该房间的所有 WebSocket 订阅者广播。

### 相关文件

核心逻辑涉及以下源文件：

| 文件 | 角色 |
|------|------|
| `GameParameters.kt` | 新增 `pollingIntervalSeconds` 字段 |
| `GameInfo.kt` | 新增 `playersFinishedThisTurn` 集合、`nextTurnPolling()` 方法及辅助查询 |
| `NextTurnAction.kt` | 新增 `FinishAction` 枚举值 |
| `NextTurnButton.kt` | 轮询模式下按钮文本追加倒计时 |
| `WorldScreen.kt` | 新增计时器协程、`finishPollingTurn()` / `passPollingTurn()` 方法 |
| `WorldScreenTopBarResources.kt` | 顶栏倒计时显示 |
| `GameOptionsTable.kt` | 房间创建界面的轮询间隔选择器 |
| `Multiplayer.kt` | `GameInfoPreview.isUsersTurn()` 空值安全修复 |
| `MultiplayerGamePreview.kt` | `GameInfoPreview.getCurrentPlayerCiv()` 空值安全修复 |
| `ChatWebSocket.kt` | 客户端 `GameUpdated` 消息接收与刷新触发 |
| `UncivServer.kt` | 服务端文件上传后 WebSocket 广播 |
| `GameSettings.kt` | 默认密码设为 123456 |

## 注意事项

- 参与轮询联机的所有玩家须在游戏设置中配置联机密码（默认 123456），否则 WebSocket 认证失败将影响推送功能。
- 轮询间隔建议根据玩家人数调整：2 人局 5~10 秒，4 人及以上建议 15 秒以上。
- 服务端须启用 `-chat` 选项（默认开启）以支持 WebSocket 推送。

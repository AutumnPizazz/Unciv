# Lua 脚本

从 4.20.8.2 版本开始，UncivCN 支持在模组中使用 Lua 脚本。通过新增的 `TriggerLuaFunction` unique，你可以在 JSON 中声明触发时机，将复杂逻辑交给 Lua 处理。Lua 与 JSON 规则系统**并行工作**——JSON 负责数据驱动的内容定义（新单位、新建筑、数值修正），Lua 负责过程性逻辑（复杂条件判断、动态效果、程序化事件链）。

## 快速上手

### 1. 目录结构

在模组根目录下创建 `scripts/` 文件夹，放入 `.lua` 文件：

```
MyMod/
├── jsons/
│   ├── ModOptions.json
│   └── Buildings.json
├── scripts/              ← 新增
│   └── myFunctions.lua
└── ...
```

### 2. 定义触发

在 JSON 中使用 `TriggerLuaFunction` unique。这个 unique 可以放在任何支持 `Triggerable` 类型的对象上，包括：

- **Building / Wonder** — 建造完毕时触发
- **Tech** — 研究完成时触发
- **Policy** — 政策采纳时触发
- **Era** — 进入新时代时触发
- **Event / EventChoice** — 事件发生时触发
- **Unit / Promotion** — 单位执行动作时触发
- **GlobalUniques** — 结合 TriggerCondition 全局触发

```json
// Buildings.json — 在 Palace 上测试
{
    "name": "Palace",
    "uniques": [
        "Indicates the capital city",
        "Trigger the function [myMod:onPalaceBuilt] with [[Gold] + 50]"
    ]
}
```

### 3. 编写 Lua 函数

```lua
-- scripts/myMod.lua
function onPalaceBuilt(ctx)
    local gold = tonumber(ctx.parameter) or 0
    local civ = ctx.civ

    ctx.log("Palace built! Civ: " .. civ.name)
    civ.addNotification("Lua script fired! Gold param: " .. tostring(gold))
    civ.addStat("Science", 100)

    return true  -- 返回 true 表示成功
end
```

> **⚠️ 最常见的错误：搞混参数来源**
>
> Lua 函数被调用时，引擎只传入**一个参数**——`ctx` 上下文表。无论你把第一个参数命名为什么，它永远都是 `ctx`。
>
> ```lua
> -- ❌ 错误写法（容易踩坑）
> function onTech(ctx, n)         -- n 永远是 nil！引擎只传了一个参数
>     local civ = game.getCurrentPlayer()  -- game 是 nil！它不是全局变量
>     civ.addGold(n)              -- n 是 nil，没有效果
> end
>
> -- ✅ 正确写法
> function onTech(ctx)
>     local n = tonumber(ctx.parameter)   -- 参数从 ctx.parameter 取
>     local civ = ctx.game.getCurrentPlayer()  -- game 是 ctx 的属性
>     civ.addGold(n)
> end
> ```
>
> 记住三条规则：
> 1. **函数第一个参数永远是 `ctx`**——引擎注入的上下文表
> 2. **`game`、`civ`、`city` 不是全局变量**——全部挂在 `ctx` 下面
> 3. **Unique 传入的参数从 `ctx.parameter` 取**——它是一个字符串，需要时用 `tonumber()` 转换

## Unique 语法

```
"Trigger the function [luaFunction] with [parameter]"
```

| 占位符 | 说明 | 示例 |
|--------|------|------|
| `[luaFunction]` | 函数引用，格式 `modName:functionName`。省略 mod 前缀时，系统搜索所有已加载模组中的同名函数（为可靠起见，推荐始终使用 `modName:` 前缀） | `myMod:onWarDeclared` |
| `[parameter]` | 自由文本，支持嵌入 Countable 表达式，在运行时自动求值 | `[Gold] * 3 + [Culture]` |

Countable 表达式在调用 Lua 之前被解析为字符串。例如当前金币为 500，`[Gold] + 50` 会被解析为 `"500 + 50"`。你可以在 Lua 中用 `tonumber(ctx.parameter)` 或自行解析。

### 生命周期钩子

`TriggerLuaFunction` 可以结合触发条件（TriggerCondition）实现按回合/阶段自动执行。将 unique 放在 `GlobalUniques.json` 中并加上触发条件即可：

```json
// GlobalUniques.json
"uniques": [
    "Trigger the function [myMod:onTurnStart] with [] <upon turn start>",
    "Trigger the function [myMod:onTurnEnd] with [] <upon turn end>"
]
```

支持的触发条件包括：`<upon turn start>`、`<upon turn end>`、`<upon discovering [techFilter] technology>`、`<upon conquering a city>`、`<upon founding a city>` 等。此外，另支持 `<upon completing a trade with [civFilter] Civilizations>`（任意被接受的交易完成时对双方触发，含 AI 自动接受）。其他战争/外交钩子：`<upon declaring war on [civFilter] Civilizations>`、`<upon being declared war on by [civFilter] Civilizations>`、`<upon entering a war with [civFilter] Civilizations>`、`<upon signing a peace treaty with [civFilter] Civilizations>`、`<upon losing a city>`。此外，`TriggerLuaFunction` 可以直接放在 `Building`、`Tech`、`Policy`、`Era`、`Event`/`EventChoice`、`Unit`、`Promotion` 等对象的 `uniques` 中，在这些对象的自然触发时机（建造完成、研究完成、政策采纳等）执行。

## ctx 上下文对象

Lua 函数接收一个 `ctx` 表，包含以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `ctx.parameter` | string | 解析后的参数值 |
| `ctx.civ` | table | 触发文明（总是存在） |
| `ctx.city` | table | 触发城市（可能为 nil） |
| `ctx.unit` | table | 触发单位（可能为 nil） |
| `ctx.tile` | table | 触发地块（可能为 nil） |
| `ctx.game` | table | 全局游戏对象 |
| `ctx.store` | table | 模组持久化存储，含 `get` / `set` 方法 |
| `ctx.log(msg)` | function | 输出调试日志到 Unciv 日志 |
| `ctx.count(expr)` | function | 运行时求值 Countable 表达式 |
| `ctx.evaluateConditional(condition)` | function | 求值一个 conditional 条件句（如 `"when at war"`），返回 boolean |
| `ctx.random()` | function | 确定性随机数，范围 [0, 1)——相同游戏状态下的相同调用序列产生相同结果（联机安全） |
| `ctx.randomInt(min, max)` | function | 确定性随机整数，范围 [min, max]（含两端） |

**调用约定**：API 函数使用 `.` 语法（不是 `:` 语法）：

```lua
-- ✅ 正确
civ.addGold(500)
-- ❌ 错误——":" 会额外传递 civ 自身作为第一个参数
civ:addGold(500)
```

## API 参考

每个上下文表的全部函数与属性列表由游戏代码自动生成、永不落后于实现——见 [Lua API 参考](Lua-API-Reference.md)。

以下章节深入讲解特定主题：

## unit.base — 单位模板

`unit.base` 子表为只读，保存单位模板（类型定义）：

```lua
unit.base.name                     -- 单位名（如 "Warrior"）
unit.base.strength                 -- 近战战斗力
unit.base.rangedStrength           -- 远程战斗力（0 表示非远程）
unit.base.cost                     -- 建造/购买花费
unit.base.movement                 -- 基础移动力
unit.base.range                    -- 基础射程
unit.base.unitType                 -- 单位类型（如 "Melee"）
unit.base.requiredResource         -- 所需资源（空字符串表示无）
unit.base.requiredTech             -- 所需科技（空字符串表示无）
unit.base.obsoleteTech             -- 废弃科技（空字符串表示无）
unit.base.upgradesTo               -- 升级目标单位名
unit.base.replaces                 -- 替代单位名
unit.base.uniqueTo                 -- 专属文明名
unit.base.promotions               -- 初始晋升名列表
```

## game.findTiles — 地块搜索

`findTiles` 接受一个 Lua 表作为搜索条件，返回匹配的地块表列表。支持的条件键：

| 条件键 | 类型 | 说明 |
|--------|------|------|
| `resource` | string | 资源名（如 `"Iron"`） |
| `terrain` | string | 基础地形名（如 `"Grassland"`） |
| `terrainFeature` | string | 地形特征名（如 `"Forest"`） |
| `improvement` | string | 改良设施名（如 `"Farm"`） |
| `owned` | boolean | 是否已被拥有 |
| `owner` | string | 拥有者文明名 |
| `isCoast` | boolean | 是否为海岸地块 |
| `isLand` | boolean | 是否为陆地 |
| `isWater` | boolean | 是否为水域 |
| `isHill` | boolean | 是否为丘陵 |
| `maxDistance` + `centerX` + `centerY` | number | 空间范围约束（三者必须同时提供） |
| `maxResults` | number | 结果数量上限（默认 500，不指定时自动截断） |

```lua
-- 全图所有铁矿
local ironTiles = game.findTiles({ resource = "Iron" })

-- 距离 (10,15) 5 格内、未归属的森林地块
local nearbyForest = game.findTiles({
    terrainFeature = "Forest",
    owned = false,
    maxDistance = 5,
    centerX = 10,
    centerY = 15
})

-- 所有沿海的已归属地块
local ownedCoastal = game.findTiles({ isCoast = true, owned = true })
```

## ctx.store — 持久化存储

`ctx.store` 提供了一个跨回合、跨存档的键值存储，数据按模组自动隔离。所有值以 String 形式存储，需要数字时用 `tonumber()` 转换。

```lua
-- 写入
ctx.store.set("invasionCount", "5")
ctx.store.set("lastWarTarget", "Greece")

-- 读取（第二个参数为默认值）
local count = tonumber(ctx.store.get("invasionCount", "0"))
local target = ctx.store.get("lastWarTarget", "")

-- 计数器模式
local wars = tonumber(ctx.store.get("totalWars", "0")) + 1
ctx.store.set("totalWars", tostring(wars))
```

存储数据保存在存档文件中，随游戏进度一起持久化。每个模组的存储空间独立，不会互相干扰。

## ctx.evaluateConditional — 条件求值

复用 Unciv 内置的 conditional 系统，判断一个条件句在当前上下文中是否成立。

```lua
-- 通用条件
if ctx.evaluateConditional("when at war") then
    -- 处于战争中
end

-- 配合城市上下文
if ctx.evaluateConditional("in coastal cities") then
    -- 城市在沿海
end

-- Countable 条件
if ctx.evaluateConditional("when number of [Cities] is greater than [5]") then
    -- 拥有超过 5 个城市
end
```

支持的条件类型覆盖游戏内置的全部 conditional 格式（70+ 种），包括战争状态、科技/政策完成、资源数量比较、地形判断等。条件在调用 `triggerUnique` 时给定的文明/城市/单位上下文中求值。

## Lua 条件（LuaConditional）

除了用 `ctx.evaluateConditional` 在运行时求值，你还可以把 Lua 函数**直接接入 unique 条件系统**。任何 unique 都可以使用条件：

```
<if [myMod:myCondition] returns true>
```

函数收到常规 `ctx` 表，返回 `true` 时 unique 生效：

```json
{
    "name": "富有的国王",
    "uniques": [
        "[+2 Gold] <if [myMod:isRich] returns true>"
    ]
}
```

```lua
-- scripts/myMod.lua
function isRich(ctx)
    return ctx.civ.getGold() > 1000
end
```

规则与注意事项：

1. **函数必须是纯查询**——条件会被非常频繁地求值（每次检查 unique 时），请保持函数廉价且无副作用。缺失的函数按 `false` 处理（不会崩溃），模组检查器会在加载时报告
2. **性能**：每次检查都有跨语言开销。热路径优先用内置条件，Lua 条件用于内置条件无法表达的逻辑
3. 与触发器组合时与其他条件行为一致：`"Trigger the function [myMod:onX] with [] <if [myMod:shouldX] returns true> <upon turn start>"` 只有触发条件和 Lua 条件同时满足才会触发
4. `[civFilter]` 参数的约定适用于所有条件：用 `[All]` 匹配所有文明（空 `[]` 不匹配任何文明）

## 完整示例

```lua
-- scripts/rewards.lua
function eraScalingGold(ctx)
    local civ = ctx.civ
    local era = civ.getEra()

    -- 时代到金币的映射
    local rewards = {
        ["Ancient era"] = 50,
        ["Classical era"] = 150,
        ["Medieval era"] = 300,
        ["Renaissance era"] = 600,
        ["Industrial era"] = 1200,
        ["Modern era"] = 2500,
        ["Atomic era"] = 5000,
        ["Information era"] = 10000
    }

    local amount = rewards[era] or 50
    civ.addGold(amount)
    civ.addNotification("Received " .. tostring(amount) .. " Gold for entering " .. era .. "!")
    ctx.log("Era reward: " .. tostring(amount) .. " Gold for era " .. era)

    -- 如果研究超过 10 个科技，额外给首都加人口
    if #civ.getTechsResearched() > 10 then
        local capital = civ.getCapital()
        if capital ~= nil then
            capital.addPopulation(1)
            ctx.log("Bonus: +1 population in capital")
        end
    end

    return true
end
```

对应的 JSON（放在 `ModOptions.json` 或 `Eras.json` 中）：

```json
"uniques": [
    "Trigger the function [myMod:eraScalingGold] with [Gold]"
]
```

## 编辑器设置（自动补全与类型提示）

游戏本身会校验你的脚本（见[检查你的模组](#检查你的模组)），而编写时想获得自动补全、悬停文档和即时拼写检查，可以接入 Lua 语言服务器：

1. **安装 Lua 语言服务器**：在 VSCode 中安装 **Lua**（作者 sumneko，即 LuaLS 语言服务器）
2. **安装 Unciv Lua API 扩展**（推荐）：从最新的 [UncivCN Release](https://github.com/AutumnPizazz/Unciv/releases) 下载 `unciv-lua-api.vsix`，通过 **扩展 → ⋯ → 从 VSIX 安装…** 安装，然后在命令面板（Ctrl+Shift+P）运行一次 **Unciv: Configure Lua API autocompletion**。此后每次启动编辑器它都会把 `lua-api.lua` / `lua-map-api.lua` 保持在 `~/.unciv/lua-api/` 最新版——再也不用检查更新。详见 [Unciv Lua API 扩展](Lua-API-Extension.md)

如果偏好完全手动配置，可在模组目录创建 `.luarc.json` 并把定义文件复制到旁边（如从仓库 `docs/Modders/` 获取）：

```json
{
    "runtime.version": "Lua 5.2",
    "workspace.library": [
        ".lua-api"
    ],
    "diagnostics.globals": ["ctx"]
}
```

> **模组检查器说明**：`.luarc.json` 是模组根目录唯一允许的 JSON 文件（检查器白名单豁免它，因为 LuaLS 只从 workspace 根目录读取）。**不要重命名它**——LuaLS 只认这个确切文件名，改成任何其他名字都会被检查器当作放错位置的规则集文件报告。

可选：在 `.vscode/extensions.json` 中推荐扩展，方便协作者自动安装语言服务器：

```json
{
    "recommendations": ["sumneko.lua"]
}
```

配置完成后即可获得：`ctx.` 自动补全（civ/city/unit/tile/game/store）、方法名补全与悬停文档（如 `ctx.civ.addGold(`）、以及 `ctx.civ.addGoldd(...)` 这类拼写错误的即时红色波浪线。地图脚本（`GenerateMap(ctx)`）同样受益于 `lua-map-api.lua`——两个文件都由扩展自动管理。

> **限制说明**：定义文件由生成器从 API 目录自动生成，参数/返回值标注是尽力而为——常见模式精确、其余为宽松的 `fun(...)`。拿不准时以游戏内模组检查器或 `mod-ci` 为准（它们才是权威），并在游戏中实测函数行为。

## Lua 地图脚本

模组现在可以**用 Lua 编写完整的地图生成器**。`scripts/` 文件夹中定义以下两个函数的模组会出现在地图类型选项中：

| 函数 | 用途 |
|------|------|
| `GetMapScriptInfo()` | 返回 `{ name = "...", description = "..." }`——显示在新建游戏界面 |
| `GenerateMap(ctx)` | 生成地图；成功返回 `true` |

在新建游戏界面选择**地图类型 → Lua Generated**，再选择脚本。引擎会创建一张指定大小的全海洋 `TileMap` 并调用 `GenerateMap(ctx)`，之后对每个地块按规则集做地形规范化。

地图脚本 `ctx` 的完整 API 参考（`ctx.params`、`ctx.map` 助手、生成期地块表）由游戏代码自动生成、永不落后于实现——见 [Lua 地图脚本 API 参考](Lua-Map-API-Reference.md)。

> **重要**：遍历地块请用 `map.getAllTiles()`，不要假设坐标从 0 开始——矩形地图以 (0,0) 为中心，`params.bounds.minX`/`minY` 为负值。
>
> `GenerateMap` 与 `GetMapScriptInfo` 是**保留函数名**：模组检查器会拒绝在游戏内 `TriggerLuaFunction` 中引用它们，因为它们只在生成期运行。

完整的可复制改名示例位于 `docs/Modders/examples/LuaMapScriptExample/`（见其 README）。

地图脚本的编辑器自动补全：把 LuaLS 语言服务器指向 `docs/Modders/lua-map-api.lua`（`.luarc.json` 配置与[编辑器设置](#编辑器设置自动补全与类型提示)相同，`workspace.library` 同时列出 `unciv-api.lua` 与 `lua-map-api.lua`）；定义文件由引擎代码自动生成，永不落后于实现。

## 注意事项

- **参数约定（极易出错）**：引擎只向 lua 函数传入一个参数 `ctx`。`ctx.parameter` 才是 unique 中 `[parameter]` 解析后的值，`ctx.game`/`ctx.civ` 是上下文对象的入口。不要把第一个形参当成业务参数、不要把 `game` 当成全局变量——这是实测中最常见的错误
- **函数名必须全局唯一**：同一模组内不要定义同名函数。跨模组调用使用 `modName:functionName` 格式。函数名须匹配 `[a-zA-Z_][a-zA-Z0-9_]*`（可加 `modName:` 前缀），不能含连字符等特殊字符
- **返回值**：函数应返回 `true`（成功）或 `false`（失败）。返回 `false` 时触发器认为无效，在 UI 中可能显示为禁用状态。注意 Lua 的真值语义：`return 0` 和 `return nil` 都算**失败**，`return ""` 或 `return 1` 才算成功
- **性能**：Lua 调用有跨语言开销，避免在高频触发的路径上使用（如每回合的大量单位遍历）。优先使用 JSON Unique 处理简单的数值修正
- **快照属性**：上下文表的*属性*（如 `city.name`、`unit.health`、`civ.name`）是建表时的快照。写入（如 `city.setName(...)`、`unit.setHealth(...)`）之后，请用查询方法（`civ.getCityNames()`、`unit.getDamage()` 等）确认结果——属性会保持旧值直到下次构建 ctx
- **死循环会被截断**：每次脚本加载和每次函数调用都有指令预算（约一秒钟 CPU 时间）。意外的 `while true do end` 会以“预算超限”错误中断，而不是卡死游戏
- **沙箱**：Lua 环境是受限的，`os.*`、`io.*`、`coroutine.*`、`require`、`debug.*`、`string.dump`、`package` 库（及其 `package.loaded` 表）、文件操作、元表操作等功能已被禁用，脚本访问它们会直接报错
- **持久化存储**：`ctx.store` 中的值以字符串形式存入存档文件。存储非字符串数据时，用 `tostring()` 写入、`tonumber()` 读取
- **路径查找开销**：`unit.findPathTo()` 采用 A* 多回合寻路，在大型地图上可能有明显耗时，避免在高频循环中调用
- **日志**：`ctx.log(msg)` 输出到 Unciv 的调试日志。配合开发者控制台使用以调试脚本

## 检查你的模组

Unciv 分几层检查你的 Lua 脚本：

1. **游戏内模组检查器 / 模组管理器**：模组加载时其 `scripts/*.lua` 会被编译，语法错误显示为红色错误，缺失的 `modName:functionName` 引用显示为黄色警告。打开 **选项 → 模组 → 模组检查** 查看完整报告。
2. **静态 API 拼写检查**：对未知 API 的直接调用（如 `ctx.civ.addGoldd(...)`）会被标记并给出建议（“你是否想用：addGold, addStat…”）。游戏内模组检查器和下面的命令行工具都会运行该检查。
3. **运行时错误**：函数运行中抛出的错误（参数类型错误、调用 nil 等）会向人类玩家弹出提示，并带上脚本名与行号（`Function 'x' error at line N`）。AI 回合中触发的错误不弹窗，而是记入模组检查器的错误列表，模组作者同样能看到；这些错误也会写入游戏日志。
4. **命令行（CI / 离线）**：在模组根目录运行桌面版 `Unciv mod-ci`（或 `java -jar Unciv.jar mod-ci`）。它无头加载模组，运行全部 JSON 校验**以及**全部 Lua 检查（语法、函数引用、API 拼写），有错误时退出码为 1——适合接入 CI 流水线。

> **小技巧**：写一个调用你所用 API 的小函数（`function test(ctx) ctx.civ.addGold(1) ... return true end`），再从 `GlobalUniques.json` 里用一个调试用 unique 触发它，即可在游戏内冒烟测试你的逻辑。

> **Lua 模组新手？** 从起步模板 `docs/Modders/examples/LuaStarterMod/` 开始——一个完整可用、复制改名的模组，内含每回合钩子、参数示例与 API 冒烟测试（见其 README）。

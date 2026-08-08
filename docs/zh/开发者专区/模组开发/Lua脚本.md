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

支持的触发条件包括：`<upon turn start>`、`<upon turn end>`、`<upon discovering [techFilter] technology>`、`<upon conquering a city>`、`<upon founding a city>` 等。此外，`TriggerLuaFunction` 可以直接放在 `Building`、`Tech`、`Policy`、`Era`、`Event`/`EventChoice`、`Unit`、`Promotion` 等对象的 `uniques` 中，在这些对象的自然触发时机（建造完成、研究完成、政策采纳等）执行。

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

**调用约定**：API 函数使用 `.` 语法（不是 `:` 语法）：

```lua
-- ✅ 正确
civ.addGold(500)
-- ❌ 错误——":" 会额外传递 civ 自身作为第一个参数
civ:addGold(500)
```

## API 参考

### civ — 文明

**属性（直接读取）**：

| 属性 | 类型 | 说明 |
|------|------|------|
| `id` | string | 文明唯一 ID |
| `name` | string | 文明名称 |
| `isHuman`, `isAI`, `isAlive`, `isMajorCiv`, `isCityState`, `isBarbarian`, `isSpectator` | boolean | 身份标志 |

**方法**：

```lua
-- 属性查询
civ.getStat("Science")             -- 返回属性储备值
civ.getStatYield("Production")     -- 返回每回合产出
civ.getGoldPerTurn()               -- 返回每回合金币净收入
civ.getResourceAmount("Iron")      -- 返回资源库存
civ.hasResource("Horses")          -- 是否有 ≥1
civ.getEraNumber()                 -- 0-based 时代序号

-- 科技
civ.isResearched("Agriculture")    -- 是否已研究
civ.canResearch("Philosophy")      -- 能否研究
civ.getResearchingTech()           -- 当前研究中的科技名
civ.getResearchProgress("Writing") -- 已有烧瓶数
civ.getTechsResearched()           -- 返回已研究科技名列表
civ.getAvailableTechs()            -- 可研究科技名列表
civ.grantTech("Agriculture")       -- 直接授予科技

-- 政策
civ.hasPolicy("Oligarchy")         -- 是否已采纳
civ.canAdoptPolicy()               -- 是否有可采纳的政策
civ.getAdoptedPolicies()           -- 已采纳政策名列表
civ.grantPolicy("Oligarchy")       -- 直接采纳政策

-- 外交
civ.isAtWarWith("Greece")          -- 是否交战
civ.hasOpenBordersWith("Greece")   -- 是否有开放边界
civ.isAlliedWith("City-State")     -- 是否为盟友（城邦）
civ.getDiplomaticStatus("Greece")  -- 外交状态字符串
civ.getInfluence("City-State")     -- 城邦影响力数值
civ.getKnownCivs()                 -- 已知文明名列表
civ.addInfluence("City-State", 15) -- 增加城邦影响力
civ.declareWarOn("Greece")         -- 宣战

-- 宗教
civ.hasReligion()                  -- 是否已创建宗教
civ.getReligionName()              -- 宗教名
civ.getFaith()                     -- 信仰值

-- 单位 & 城市
civ.getCities()                    -- 城市表列表
civ.getCity("Rome")               -- 按名称获取城市表
civ.getCapital()                   -- 首都城市表
civ.getUnits()                     -- 单位表列表
civ.getUnitsMatching("Melee")      -- 按 filter 筛选单位
civ.getUnitCount()                 -- 单位总数
civ.getSpyCount()                  -- 间谍数量

-- 黄金时代
civ.isGoldenAge()                  -- 是否在黄金时代
civ.getGoldenAgeTurnsRemaining()   -- 剩余回合

-- 写操作
civ.addGold(500)                   -- 增加金币
civ.addStat("Science", 100)        -- 增加属性
civ.addStats("+2 Gold, +3 Culture") -- 复合属性变化
civ.addResource("Iron", 5)         -- 增加战略资源
civ.consumeResource("Iron", 2)     -- 消耗战略资源
civ.triggerGoldenAge(10)           -- 进入 N 回合黄金时代
civ.triggerGoldenAge()             -- 默认长度黄金时代
civ.grantFreeGreatPerson()         -- 免费伟人
civ.setLeaderTitle("Emperor")      -- 修改领袖头衔
civ.addNotification("text")        -- 弹出游戏内通知
civ.addNotificationAt("text", x, y) -- 可点击跳转的通知
civ.addFreeTech()                  -- 免费科技点数
civ.addUnit("Warrior")             -- 生成单位
civ.addUnitAtCity("Warrior", "Rome") -- 在指定城市生成单位
civ.addUnitAtTile("Warrior", 10, 5)  -- 在指定坐标生成单位
civ.addRebelUnit("Barbarian Axeman") -- 生成叛军

-- unique 查询
civ.hasUnique("unique text")         -- 文明是否拥有此 unique（搜索范围：nation + 已研究科技 + 已采纳政策 + 当前时代）
```

### city — 城市

```lua
-- 属性
city.id, city.name                 -- ID 和名称
city.isCapital, city.isCoastal     -- 身份
city.isPuppet, city.isBeingRazed   -- 状态
city.isConnectedToCapital          -- 是否连接到首都
city.population, city.health       -- 人口和血量

-- 查询
city.getStatYield("Production")    -- 单项产出
city.getAllYields()                -- 所有产出表
city.hasBuilding("Library")        -- 是否有某建筑
city.getBuiltBuildings()           -- 已建成建筑名列表
city.getBuildingCount()            -- 建筑总数
city.getWonderCount()              -- 奇观数
city.getPosition()                 -- {x, y} 坐标表
city.getCenterTile()               -- 城市中心 tile 对象
city.getTiles()                    -- 拥有的地块坐标列表
city.getCurrentConstruction()      -- 当前建设中项目名
city.getConstructionQueue()        -- 建设队列
city.getMajorityReligion()         -- 多数宗教名
city.isHolyCity()                  -- 是否为圣城

-- 写操作
city.addPopulation(1)              -- 增加人口
city.addBuilding("Library")        -- 免费建造
city.removeBuilding("Library")     -- 移除建筑

-- 建造队列
city.setProduction("Library")      -- 将当前建造项目设为指定项目
city.addToQueue("Walls")           -- 追加到建造队列末尾
city.clearQueue()                  -- 清空整个建造队列

-- unique 查询
city.hasUnique("unique text")      -- 已建建筑中是否有此 unique
```

### unit — 单位

```lua
-- 属性
unit.id, unit.name, unit.instanceName
unit.isCivilian, unit.isMilitary, unit.isRanged
unit.isEmbarked, unit.isFortified, unit.isAutomated
unit.health

-- base 子表（单位模板属性，只读）
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

-- 查询
unit.getRange()                    -- 射程
unit.getMovement()                 -- 最大移动力
unit.getCurrentMovement()          -- 剩余移动力
unit.getXP()                       -- 经验值
unit.hasPromotion("Shock I")       -- 是否有晋升
unit.hasUnique("unique text")      -- 单位是否拥有此 unique（含 unit type + 晋升）
unit.getPromotions()               -- 晋升名列表
unit.getPromotionCount()           -- 晋升数量
unit.hasStatus("Fortification")    -- 是否有状态
unit.getStatusTurns("Fortification") -- 状态剩余回合
unit.getPosition()                 -- {x, y} 坐标表
unit.canMoveTo(x, y)               -- 能否移动到
unit.getOwner()                    -- 所属文明名
unit.isOwnedBy("Rome")             -- 是否属于某文明

-- 写操作
unit.healBy(25)                    -- 回复血量
unit.takeDamage(30)                -- 造成伤害
unit.addXP(10)                     -- 增加经验
unit.addPromotion("Shock I")       -- 添加晋升
unit.removePromotion("Shock I")    -- 移除晋升
unit.addMovement(2)                -- 增加移动力
unit.useMovement(1.5)              -- 消耗移动力
unit.upgrade()                     -- 免费升级
unit.destroy()                     -- 摧毁单位
unit.teleportTo(x, y)              -- 传送
unit.attackTile(x, y)              -- 攻击目标坐标上的单位/城市
                                   -- 返回 {attackerDamage=n, defenderDamage=m} 或 false（不可攻击）

-- 路径查找
unit.canReach(x, y)                -- 能否到达目标坐标
unit.findPathTo(x, y)              -- 返回路径坐标列表 {{x,y}, {x,y}, ...} 或 nil
```

### tile — 地块

```lua
-- 属性
tile.position                      -- {x, y} 坐标表
tile.baseTerrain                   -- 基础地形名
tile.isLand, tile.isWater          -- 地形类型
tile.resourceName, tile.resourceAmount
tile.improvementName

-- 查询
tile.getX(), tile.getY()           -- 坐标
tile.isCoast(), tile.isHill()      -- 地形特征
tile.hasTerrainFeature("Forest")   -- 是否有地形特征
tile.getTerrainFeatures()          -- 地形特征列表
tile.isImpassable()                -- 是否不可通行
tile.isRiver()                     -- 是否为河流
tile.hasResource()                 -- 是否有资源
tile.hasImprovement()              -- 是否有改良设施
tile.hasMilitaryUnit()             -- 是否有军事单位
tile.hasCivilianUnit()             -- 是否有平民单位
tile.getUnits()                    -- 地块上的单位表列表
tile.getYield()                    -- 返回地块产出表 {Food=2, Production=1, Gold=0, ...}
tile.isOwned()                     -- 是否被拥有
tile.getOwner()                    -- 拥有者文明名
tile.isOwnedBy("Rome")             -- 是否属于某文明
tile.isCityCenter()                -- 是否为城市中心
tile.getOwningCity()               -- 拥有城市名
tile.isExploredBy("Rome")          -- 某文明是否已探索

-- 邻居（复杂条件判断的关键）
tile.getNeighbors()                -- 相邻地块表列表
tile.getNeighborAt(0)              -- 指定方向的相邻地块
tile.getTilesInDistance(3)         -- 范围内所有地块

-- 写操作
tile.setTerrain("Plains")          -- 改变基础地形
tile.addTerrainFeature("Forest")   -- 添加地形特征
tile.removeTerrainFeature("Forest") -- 移除地形特征
tile.setImprovement("Farm")        -- 设置改良设施
tile.removeImprovement()           -- 移除改良设施
tile.removeResource()              -- 移除资源
tile.setResource("Iron", 6)        -- 设置资源
tile.setRoad()                     -- 修建道路
tile.setRailroad()                 -- 修建铁路
tile.removeRoad()                  -- 移除道路
tile.isPillaged()                  -- 是否已被劫掠
```

### game — 全局

```lua
-- 属性
game.turn                          -- 当前回合数
game.speed                         -- 游戏速度名
game.difficulty                    -- 难度名

-- 查询
game.getYear()                     -- 当前年份
game.getCurrentPlayer()            -- 当前玩家文明名
game.getCiv("Rome")               -- 按名称获取文明表
game.getCivById("uuid...")        -- 按 ID 获取文明表
game.getAllCivs()                  -- 所有文明表列表
game.getAliveMajorCivs()           -- 存活的 major 文明
game.getAliveCityStates()          -- 存活的城邦
game.getBarbarianCiv()             -- 蛮族文明

-- 地图
game.getTile(x, y)                 -- 获取地块表
game.getMapWidth(), game.getMapHeight()
game.isWrapped()                   -- 地图是否环绕
game.getTilesNear(x, y, radius)    -- 范围内地块
game.findTiles(criteria)           -- 按条件搜索全图地块，见下方说明

-- 规则集查询
game.getRulesetBuildings()         -- 所有建筑名列表
game.getRulesetUnits()             -- 所有单位名列表
game.getRulesetTechs()             -- 所有科技名列表
game.getRulesetPolicies()          -- 所有政策名列表
game.getRulesetEras()              -- 所有时代名列表
game.getRulesetPromotions()        -- 所有晋升名列表
game.doesBuildingExist("Name")     -- 规则集中是否存在
game.doesUnitExist("Name")         -- 规则集中是否存在

-- 写操作
game.addGlobalNotification("text") -- 向所有人类玩家发通知
game.revealEntireMap("Rome")       -- 对某文明揭示全地图
game.revealTilesAround("Rome", x, y, radius)
```

### game.findTiles — 地块搜索

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

### ctx.store — 持久化存储

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

### ctx.evaluateConditional — 条件求值

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

## 注意事项

- **参数约定（极易出错）**：引擎只向 lua 函数传入一个参数 `ctx`。`ctx.parameter` 才是 unique 中 `[parameter]` 解析后的值，`ctx.game`/`ctx.civ` 是上下文对象的入口。不要把第一个形参当成业务参数、不要把 `game` 当成全局变量——这是实测中最常见的错误
- **函数名必须全局唯一**：同一模组内不要定义同名函数。跨模组调用使用 `modName:functionName` 格式
- **返回值**：函数应返回 `true`（成功）或 `false`（失败）。返回 `false` 时触发器认为无效，在 UI 中可能显示为禁用状态
- **性能**：Lua 调用有跨语言开销，避免在高频触发的路径上使用（如每回合的大量单位遍历）。优先使用 JSON Unique 处理简单的数值修正
- **沙箱**：Lua 环境是受限的，`os.*`、`io.*`、`coroutine.*`、`require`、`debug.*`、`string.dump`、文件操作、元表操作等功能已被禁用
- **持久化存储**：`ctx.store` 中的值以字符串形式存入存档文件。存储非字符串数据时，用 `tostring()` 写入、`tonumber()` 读取
- **路径查找开销**：`unit.findPathTo()` 采用 A* 多回合寻路，在大型地图上可能有明显耗时，避免在高频循环中调用
- **日志**：`ctx.log(msg)` 输出到 Unciv 的调试日志。配合开发者控制台使用以调试脚本

<!-- 本文件由 LuaApiReferenceWriter / LuaMapApiReferenceWriter 自动生成，请勿手动编辑。 -->
<!-- Generated from the LuaApiDocs / LuaMapGenApiDocs tables - never edit by hand. -->
<!-- 重新生成 / Regenerate with: ./gradlew desktop:generateDocs -->

# Lua API 参考

本页面由游戏代码自动生成，列出 Lua 上下文表（`ctx`）的全部函数与属性。它永远不会与实现
脱节——新增 API 时，本页面与类型定义（`lua-api.lua`）会一起重新生成。

教程、触发方式与编辑器设置见 [Lua 脚本](Lua-Modding.md)。

> **关于签名**：类型采用 LuaLS 语言服务器（`lua-api.lua`）使用的 EmmyLua 风格；
> `|nil` 表示该值可能为空，`string[]` 表示字符串列表。

## civ — 文明

**属性**:

`id`, `name`, `isHuman`, `isAI`, `isAlive`, `isMajorCiv`, `isCityState`, `isBarbarian`, `isSpectator`

**查询方法**:

```lua
civ.getNation()                     -- 文明（nation）名
civ.getLeaderName()                 -- 领袖名
civ.getScore()                      -- 当前分数
civ.getForce()                      -- 军力排名值
civ.getGold()                       -- 当前金币
civ.getHappiness()                  -- 当前笑脸
civ.getStat(stat)                   -- 返回属性储备值
civ.getStatYield(stat)              -- 返回每回合产出
civ.getGoldPerTurn()                -- 每回合金币净收入
civ.getSciencePerTurn()             -- 每回合科研
civ.getCulturePerTurn()             -- 每回合文化
civ.getFoodPerTurn()                -- 每回合食物
civ.getProductionPerTurn()          -- 每回合产能
civ.getResourceAmount(resourceName) -- 返回资源库存
civ.hasResource(resourceName)       -- 是否有 ≥1
civ.getResourceStockpiles()         -- 资源库存表 {资源名 = 数量}
civ.getEra()                        -- 当前时代名
civ.getEraNumber()                  -- 0-based 时代序号
civ.isResearched(techName)          -- 是否已研究
civ.canResearch(techName)           -- 能否研究
civ.getResearchingTech()            -- 当前研究中的科技名
civ.getResearchProgress(techName)   -- 已有烧瓶数
civ.getTechCount()                  -- 已研究科技数量
civ.getTechsResearched()            -- 已研究科技名列表
civ.getAvailableTechs()             -- 可研究科技名列表
civ.getTechCost(techName)           -- 科技基础研究成本
civ.hasPolicy(policyName)           -- 是否已采纳
civ.canAdoptPolicy()                -- 是否有可采纳的政策
civ.getAdoptedPolicyCount()         -- 已采纳政策数量
civ.getAdoptedPolicies()            -- 已采纳政策名列表
civ.getAvailablePolicyBranches()    -- 全部政策分支名列表
civ.getCultureNeededForNextPolicy() -- 下一个政策所需文化
civ.isAtWarWith(civName)            -- 是否交战
civ.hasOpenBordersWith(civName)     -- 是否有开放边界
civ.isAlliedWith(civName)           -- 是否为盟友（城邦）
civ.getDiplomaticStatus(civName)    -- 外交状态字符串
civ.getDiplomaticStatuses()         -- 外交状态表 {文明名 = 状态名}
civ.getProximityTo(civName)         -- 邻近度字符串（None/Neighbors/Close/Far）
civ.hasEmbassyWith(civName)         -- 是否已互设大使馆
civ.getInfluence(civName)           -- 城邦影响力数值
civ.getKnownCivs()                  -- 已知文明名列表
civ.hasReligion()                   -- 是否已创建宗教
civ.getReligionName()               -- 宗教名
civ.getFaith()                      -- 信仰值
civ.getCities()                     -- 城市表列表
civ.getCity(cityName)               -- 按名称获取城市表
civ.getCapital()                    -- 首都城市表
civ.getCityCount()                  -- 城市数量
civ.getCityNames()                  -- 城市名列表
civ.getTotalPopulation()            -- 所有城市人口总和
civ.getWondersBuilt()               -- 已建造的奇观名列表
civ.getUnits()                      -- 单位表列表
civ.getUnitsMatching(filter)        -- 按 filter 筛选单位
civ.getUnitCount()                  -- 单位总数
civ.isGoldenAge()                   -- 是否在黄金时代
civ.getGoldenAgeTurnsRemaining()    -- 剩余回合
civ.getSpyCount()                   -- 间谍数量
civ.getSpies()                      -- 间谍详情表列表 {name, rank, action, location}
civ.getLeaderTitle()                -- 当前领袖头衔
civ.hasUnique(uniqueText)           -- 文明是否拥有此 unique（搜索范围：nation + 已研究科技 + 已采纳政策 + 当前时代）
```

**写入方法**:

```lua
civ.grantTech(techName)                   -- 直接授予科技
civ.discoverTech(techName)                -- 直接发现科技（同 grantTech）
civ.grantPolicy(policyName)               -- 直接采纳政策
civ.addInfluence(civName, amount)         -- 增加城邦影响力
civ.declareWarOn(civName)                 -- 宣战
civ.makePeaceWith(civName)                -- 签订和平（不在战时则为空操作）
civ.addSpy()                              -- 增加一名间谍
civ.addGold(amount)                       -- 增加金币
civ.setGold(amount)                       -- 将金币设为精确值
civ.addStat(stat, amount)                 -- 增加属性
civ.addStats(statsText)                   -- 复合属性变化
civ.addResource(resourceName, amount)     -- 增加战略资源
civ.consumeResource(resourceName, amount) -- 消耗战略资源
civ.triggerGoldenAge(turns)               -- 进入 N 回合黄金时代（省略参数为默认长度）
civ.grantFreeGreatPerson()                -- 免费伟人
civ.setLeaderTitle(title)                 -- 修改领袖头衔
civ.addNotification(text)                 -- 弹出游戏内通知
civ.addNotificationAt(text, x, y)         -- 可点击跳转的通知
civ.addFreeTech()                         -- 免费科技点数
civ.addUnit(unitName)                     -- 生成单位
civ.addUnitAtCity(unitName, cityName)     -- 在指定城市生成单位
civ.addUnitAtTile(unitName, x, y)         -- 在指定坐标生成单位
civ.addRebelUnit(unitName)                -- 生成叛军
```

## city — 城市

**属性**:

`id`, `name`, `isCapital`, `isCoastal`, `isPuppet`, `isBeingRazed`, `isConnectedToCapital`, `population`, `health`

**查询方法**:

```lua
city.getStatYield(stat)        -- 单项产出
city.getAllYields()            -- 所有产出表
city.getFood()                 -- 当前食物产出
city.getFoodSurplus()          -- 每回合净食物（负数为饥荒）
city.getFoodStorage()          -- 已存食物（用于人口增长）
city.getFoodNeeded()           -- 下一个人口所需食物
city.getProductionProgress()   -- 当前建造已投入产能
city.getProductionCost()       -- 当前建造总成本
city.getTurnsToCompletion()    -- 预计完成回合数
city.getGarrisonedUnit()       -- 驻军单位表（无则 nil）
city.getStrength()             -- 城市战斗力
city.getSpecialistCount()      -- 已分配专家数
city.getUnemployedCount()      -- 空闲（未分配）人口
city.getBuiltWonders()         -- 已建造奇观名列表
city.isInResistance()          -- 征服后是否处于抵抗
city.hasBuilding(buildingName) -- 是否有某建筑
city.getBuiltBuildings()       -- 已建成建筑名列表
city.getBuildingCount()        -- 建筑总数
city.getWonderCount()          -- 奇观数
city.getPosition()             -- {x, y} 坐标表
city.getCenterTile()           -- 城市中心 tile 对象
city.getTiles()                -- 拥有的地块坐标列表
city.getCurrentConstruction()  -- 当前建设中项目名
city.getConstructionQueue()    -- 建设队列
city.getMajorityReligion()     -- 多数宗教名
city.isHolyCity()              -- 是否为圣城
city.hasUnique(uniqueText)     -- 已建建筑中是否有此 unique
```

**写入方法**:

```lua
city.addPopulation(amount)        -- 增加人口
city.setPopulation(count)         -- 精确设置人口（最低 1）
city.addFood(amount)              -- 增加存粮
city.addProduction(amount)        -- 增加当前建造产能
city.addHealth(amount)            -- 治疗城市
city.setName(newName)             -- 城市改名
city.addBuilding(buildingName)    -- 免费建造
city.removeBuilding(buildingName) -- 移除建筑
city.sellBuilding(buildingName)   -- 出售建筑换取金币
city.setProduction(itemName)      -- 将当前建造项目设为指定项目
city.addToQueue(itemName)         -- 追加到建造队列末尾
city.clearQueue()                 -- 清空整个建造队列
```

## unit — 单位

**属性**:

`id`, `name`, `instanceName`, `isCivilian`, `isMilitary`, `isRanged`, `isEmbarked`, `isFortified`, `isAutomated`, `base`, `health`

**查询方法**:

```lua
unit.getRange()                  -- 射程
unit.getEraNumber()              -- 所属文明的时代序号（0-based）
unit.getMovement()               -- 最大移动力
unit.getCurrentMovement()        -- 剩余移动力
unit.getXP()                     -- 经验值
unit.getMaxHealth()              -- 最大生命值
unit.getDamage()                 -- 最大生命减当前生命
unit.getAttacksLeft()            -- 本回合剩余攻击次数
unit.getVisibilityRange()        -- 视野范围（格）
unit.getAction()                 -- 当前行动字符串（"Fortify"、"moveTo x,y" 等）
unit.canAttack()                 -- 本回合能否攻击
unit.canPillage()                -- 当前地块是否可掠夺
unit.isInEnemyTerritory()        -- 是否处于敌方领土
unit.isInFriendlyTerritory()     -- 是否处于己方领土
unit.isGreatPerson()             -- 是否为伟人
unit.getReligionDisplayName()    -- 单位宗教名（无则为 ""）
unit.hasPromotion(promotionName) -- 是否有晋升
unit.hasUnique(uniqueText)       -- 单位是否拥有此 unique（含 unit type + 晋升）
unit.getPromotions()             -- 晋升名列表
unit.getPromotionCount()         -- 晋升数量
unit.hasStatus(statusName)       -- 是否有状态
unit.getStatusTurns(statusName)  -- 状态剩余回合
unit.getPosition()               -- {x, y} 坐标表
unit.canMoveTo(x, y)             -- 能否移动到
unit.getOwner()                  -- 所属文明名
unit.isOwnedBy(civName)          -- 是否属于某文明
unit.findPathTo(x, y)            -- 返回路径坐标列表 {{x,y}, {x,y}, ...} 或 nil（A* 多回合寻路）
unit.canReach(x, y)              -- 能否到达目标坐标
```

**写入方法**:

```lua
unit.healBy(amount)                 -- 回复血量
unit.takeDamage(amount)             -- 造成伤害
unit.addXP(amount)                  -- 增加经验
unit.setXP(amount)                  -- 精确设置经验
unit.setHealth(amount)              -- 精确设置生命（自动限制范围）
unit.addPromotion(promotionName)    -- 添加晋升
unit.removePromotion(promotionName) -- 移除晋升
unit.addMovement(amount)            -- 增加移动力
unit.useMovement(amount)            -- 消耗移动力
unit.setStatus(statusName, turns)   -- 施加 N 回合的单位状态
unit.setAttacksLeft(count)          -- 设置剩余攻击次数
unit.fortify()                      -- 驻防（action = "Fortify"）
unit.moveByPath(path)               -- 沿 findPathTo() 的路径移动；返回实际步数
unit.upgrade()                      -- 免费升级
unit.destroy()                      -- 摧毁单位
unit.attackTile(x, y)               -- 攻击目标坐标上的单位/城市；返回 {attackerDamage=n, defenderDamage=m} 或 false（不可攻击）
unit.teleportTo(x, y)               -- 传送到指定坐标
```

## tile — 地块

**属性**:

`position`, `baseTerrain`, `isLand`, `isWater`, `isCoast`, `isHill`, `isMountain`, `resourceName`, `resourceAmount`, `improvementName`

**查询方法**:

```lua
tile.getX()                         -- X 坐标
tile.getY()                         -- Y 坐标
tile.hasTerrainFeature(featureName) -- 是否有地形特征
tile.getTerrainFeatures()           -- 地形特征列表
tile.isImpassable()                 -- 是否不可通行
tile.isRiver()                      -- 是否为河流
tile.isAdjacentToCoast()            -- 是否邻接海岸
tile.hasRoad()                      -- 是否有道路
tile.hasRailroad()                  -- 是否有铁路
tile.hasNaturalWonder()             -- 是否有自然奇观
tile.getNaturalWonder()             -- 自然奇观名（无则 ""）
tile.hasResource()                  -- 是否有资源
tile.hasImprovement()               -- 是否有改良设施
tile.isPillaged()                   -- 是否已被劫掠
tile.getYield()                     -- 地块产出表 {Food=2, Production=1, ...}
tile.isOwned()                      -- 是否被拥有
tile.getOwner()                     -- 拥有者文明名
tile.isOwnedBy(civName)             -- 是否属于某文明
tile.isFriendlyTerritory(civName)   -- 是否为该文明友方领土
tile.isEnemyTerritory(civName)      -- 是否为该文明敌方领土
tile.isCityCenter()                 -- 是否为城市中心
tile.getOwningCity()                -- 拥有城市名
tile.isExploredBy(civName)          -- 某文明是否已探索
tile.getDistanceTo(x, y)            -- 空中直线距离（格，越界返回 -1）
tile.isAdjacentTo(x, y)             -- 是否相邻
tile.hasMilitaryUnit()              -- 是否有军事单位
tile.hasCivilianUnit()              -- 是否有平民单位
tile.getUnits()                     -- 地块上的单位表列表
tile.getNeighbors()                 -- 相邻地块表列表
tile.getNeighborAt(direction)       -- 指定方向的相邻地块（0-5）
tile.getTilesInDistance(radius)     -- 范围内所有地块
```

**写入方法**:

```lua
tile.setExplored(civName, explored)    -- 设置/取消某文明对该地块的探索
tile.setTerrain(terrainName)           -- 改变基础地形
tile.addTerrainFeature(featureName)    -- 添加地形特征
tile.removeTerrainFeature(featureName) -- 移除地形特征
tile.setImprovement(improvementName)   -- 设置改良设施
tile.removeImprovement()               -- 移除改良设施
tile.removeResource()                  -- 移除资源
tile.setResource(resourceName, amount) -- 设置资源
tile.setRoad()                         -- 修建道路
tile.setRailroad()                     -- 修建铁路
tile.removeRoad()                      -- 移除道路
```

## game — 全局

**属性**:

`turn`, `speed`, `difficulty`

**查询方法**:

```lua
game.getYear()                             -- 当前年份
game.getCurrentPlayer()                    -- 当前玩家文明名
game.getCurrentPlayerCiv()                 -- 当前玩家文明表（无则 nil）
game.getCiv(civName)                       -- 按名称获取文明表
game.getCivById(id)                        -- 按 ID 获取文明表
game.getAllCivs()                          -- 所有文明表列表
game.getCivNames()                         -- 所有文明名列表
game.getHumanCivs()                        -- 人类文明列表
game.getAliveMajorCivs()                   -- 存活的 major 文明
game.getAliveCityStates()                  -- 存活的城邦
game.getBarbarianCiv()                     -- 蛮族文明
game.getTile(x, y)                         -- 获取地块表
game.findTiles(criteria)                   -- 按条件表搜索全图地块（见 findTiles 章节）
game.getMapWidth()                         -- 地图宽度
game.getMapHeight()                        -- 地图高度
game.getMapName()                          -- 地图名
game.getMapType()                          -- 地图类型
game.isWrapped()                           -- 地图是否环绕
game.getTilesNear(x, y, radius)            -- 范围内地块
game.getEraNames()                         -- 所有时代名
game.getVictoryTypes()                     -- 启用的胜利类型
game.getMods()                             -- 启用的模组列表
game.getBaseRuleset()                      -- 基础规则集
game.getRulesetBuildings()                 -- 所有建筑名列表
game.getRulesetUnits()                     -- 所有单位名列表
game.getRulesetTechs()                     -- 所有科技名列表
game.getRulesetPolicies()                  -- 所有政策名列表
game.getRulesetEras()                      -- 所有时代名列表
game.getRulesetPromotions()                -- 所有晋升名列表
game.getRulesetTerrains()                  -- 地形名列表
game.getRulesetResources()                 -- 资源名列表
game.getRulesetImprovements()              -- 改良名列表
game.getRulesetNations()                   -- 文明（nation）名列表
game.getRulesetReligions()                 -- 宗教名列表
game.getRulesetBeliefs()                   -- 信条名列表
game.getRulesetEvents()                    -- 事件名列表
game.getRulesetNaturalWonders()            -- 自然奇观名列表
game.getRulesetUnitTypes()                 -- 单位类型名列表
game.doesBuildingExist(buildingName)       -- 规则集中是否存在
game.doesUnitExist(unitName)               -- 规则集中是否存在
game.doesTechExist(techName)               -- 科技是否存在
game.doesPolicyExist(policyName)           -- 政策是否存在
game.doesEraExist(eraName)                 -- 时代是否存在
game.doesPromotionExist(promotionName)     -- 晋升是否存在
game.doesTerrainExist(terrainName)         -- 地形是否存在
game.doesResourceExist(resourceName)       -- 资源是否存在
game.doesImprovementExist(improvementName) -- 改良是否存在
game.doesNationExist(nationName)           -- 文明（nation）是否存在
game.doesBeliefExist(beliefName)           -- 信条是否存在
game.doesEventExist(eventName)             -- 事件是否存在
```

**写入方法**:

```lua
game.addGlobalNotification(text)              -- 向所有人类玩家发通知
game.revealEntireMap(civName)                 -- 对某文明揭示全地图
game.revealTilesAround(civName, x, y, radius) -- 对某文明揭示指定位置周边地块
```

## ctx — 上下文

**属性**:

`parameter`, `value`, `city`, `unit`, `tile`, `civ`, `game`, `otherCiv`, `attacker`, `defender`, `target`, `combatAction`, `store`

**查询方法**:

```lua
ctx.count(expr)                    -- 在运行时求值一个 Countable 表达式
ctx.evaluateConditional(condition) -- 求值一个条件句，返回布尔值
ctx.random()                       -- 确定性随机数 [0, 1) - 相同游戏状态下相同调用序列产生相同值
ctx.randomInt(min, max)            -- 确定性随机整数 [min, max]（含两端）
```

**写入方法**:

```lua
ctx.log(msg) -- 写入 Unciv 的调试日志
```

## ctx.store — 持久化存储

**查询方法**:

```lua
store.get(key, default) -- 读取值；不存在时返回默认值
```

**写入方法**:

```lua
store.set(key, value) -- 存储值（所有值以字符串形式保存）
```

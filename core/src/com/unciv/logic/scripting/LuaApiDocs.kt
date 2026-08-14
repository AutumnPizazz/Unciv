package com.unciv.logic.scripting

/** 单个 Lua API 的文档元数据（游戏内 ctx 与地图脚本 ctx 共用） */
data class LuaApiDocEntry(
    val name: String,
    /** EmmyLua 风格签名：方法 "fun(...)"，属性直接写类型 */
    val signature: String,
    /** 英文说明 */
    val desc: String,
    /** 中文说明 */
    val descZh: String,
    val category: LuaApiDocCategory
)

enum class LuaApiDocCategory { Property, Query, Write }

/**
 * 每个 Lua API 的文档元数据：签名、中英文说明、类别。
 *
 * 与 [LuaAPI.apiCatalog] 条目一一对应（由 LuaApiDocsTests 双向校验强制），
 * 是以下产物的唯一文案来源：
 *  - `docs/Modders/lua-api.lua`（EmmyLua 类型定义，[LuaApiDefinitionWriter]）
 *  - `docs/Modders/Lua-API-Reference.md` / `docs/zh/Modders/Lua-API-Reference.md`
 *    （面向模组作者的 API 参考文档，[LuaApiReferenceWriter]）
 *
 * 新增或修改 API 时：同步改这里与 [LuaAPI.apiCatalog]，然后运行
 * `./gradlew desktop:generateDocs` 重新生成产物。**严禁人工编辑生成产物。**
 *
 * 签名格式沿用 EmmyLua 风格：方法 `fun(参数: 类型): 返回类型`，属性直接写类型
 * （`string` / `boolean` / `number` / `UncivCiv` 等）。
 */
object LuaApiDocs {

    /** 属性条目 */
    private fun p(name: String, signature: String, desc: String, descZh: String) =
        LuaApiDocEntry(name, signature, desc, descZh, LuaApiDocCategory.Property)

    /** 查询方法条目 */
    private fun q(name: String, signature: String, desc: String, descZh: String) =
        LuaApiDocEntry(name, signature, desc, descZh, LuaApiDocCategory.Query)

    /** 写入方法条目 */
    private fun w(name: String, signature: String, desc: String, descZh: String) =
        LuaApiDocEntry(name, signature, desc, descZh, LuaApiDocCategory.Write)

    // region ctx

    private val ctxEntries = listOf(
        p("parameter", "string", "Resolved [parameter] value from the trigger unique", "触发 unique 中 [parameter] 解析后的值"),
        p("value", "number|nil", "Base strength (strength hook) or incoming damage (received-damage hook); nil elsewhere", "基础力（战斗力钩子）或受到的伤害（减伤钩子）；其余为 nil"),
        p("modifier", "number|nil", "Total modifier factor from terrain/flanking/etc. (strength hook); nil elsewhere", "地形/夹击等修正的总倍率（战斗力钩子）；其余为 nil"),
        p("attackerStrength", "number|nil", "Final attacker strength (damage hook); nil elsewhere", "攻击方最终战斗力（伤害钩子）；其余为 nil"),
        p("defenderStrength", "number|nil", "Final defender strength (damage hook); nil elsewhere", "防守方最终战斗力（伤害钩子）；其余为 nil"),
        p("modifiers", "table|nil", "Individual combat modifiers {name = percentage} (terrain/flanking/etc.); nil outside combat", "逐项战斗修正表 {名称 = 百分比}（地形/夹击等）；战斗之外为 nil"),
        p("randomnessFactor", "number|nil", "Randomness factor in [0,1) used by the engine damage formula", "引擎伤害公式所用的随机因子 [0,1)"),
        p("healthRatio", "number|nil", "Wounded-unit damage penalty ratio of the dealer", "造成方受伤单位的伤害惩罚系数"),
        p("damageToAttacker", "boolean|nil", "Whether this damage is counter-damage dealt to the attacker", "是否为对攻击方的反击伤害"),
        p("city", "UncivCity", "Triggering city (may be nil)", "触发城市（可能为 nil）"),
        p("unit", "UncivUnit", "Triggering unit (may be nil)", "触发单位（可能为 nil）"),
        p("tile", "UncivTile", "Triggering tile (may be nil)", "触发地块（可能为 nil）"),
        p("civ", "UncivCiv", "Triggering civilization (always present)", "触发文明（始终存在）"),
        p("game", "UncivGame", "Global game state", "全局游戏状态"),
        p("otherCiv", "UncivCiv|nil", "Opposing civilization - the other party in combat or diplomacy (defender's civ when attacking, the other side of a trade/war, etc.)", "对手文明——战斗或外交中的另一方（进攻时为防守方文明、交易/战争的对侧等）"),
        p("attacker", "UncivUnit|UncivCity|nil", "Attacking combatant in combat (unit or city table; nil outside combat or when the role is unknown)", "战斗中的进攻方（单位或城市表；非战斗或角色未知时为 nil）"),
        p("defender", "UncivUnit|UncivCity|nil", "Defending combatant in combat (unit or city table; nil outside combat or when the role is unknown)", "战斗中的防守方（单位或城市表；非战斗或角色未知时为 nil）"),
        p("target", "UncivUnit|UncivCity|nil", "The opponent combatant from this trigger's perspective (always the other side in combat)", "本触发视角下的对手（战斗中的另一方）"),
        p("combatAction", "string|nil", "Combat role of this trigger: \"Attack\", \"Defend\" or \"Intercept\" (nil outside combat)", "本触发的战斗角色：\"Attack\"、\"Defend\" 或 \"Intercept\"（非战斗为 nil）"),
        w("log", "fun(msg: string)", "Write debug output to Unciv's log", "写入 Unciv 的调试日志"),
        q("count", "fun(expr: string): string", "Evaluate a Countable expression at runtime", "在运行时求值一个 Countable 表达式"),
        q("evaluateConditional", "fun(condition: string): boolean", "Evaluate a conditional string, returns boolean", "求值一个条件句，返回布尔值"),
        p("store", "UncivStore", "Persistent per-mod key-value storage", "按模组隔离的持久化键值存储"),
        q("random", "fun(): number", "Deterministic random in [0, 1) - same call sequence on the same game state yields the same values", "确定性随机数 [0, 1) - 相同游戏状态下相同调用序列产生相同值"),
        q("randomInt", "fun(min: number, max: number): number", "Deterministic random integer in [min, max] (inclusive)", "确定性随机整数 [min, max]（含两端）"),
    )

    // endregion

    // region store

    private val storeEntries = listOf(
        q("get", "fun(key: string, default: string?): string", "Read a value; returns default when absent", "读取值；不存在时返回默认值"),
        w("set", "fun(key: string, value: string)", "Store a value (all values are stored as strings)", "存储值（所有值以字符串形式保存）"),
    )

    // endregion

    // region civ

    private val civEntries = listOf(
        p("id", "string", "Civilization unique ID", "文明唯一 ID"),
        p("name", "string", "Civilization name", "文明名称"),
        p("isHuman", "boolean", "Human player", "是否人类玩家"),
        p("isAI", "boolean", "AI player", "是否 AI"),
        p("isAlive", "boolean", "Civilization is alive", "是否存活"),
        p("isMajorCiv", "boolean", "Major civilization", "是否主要文明"),
        p("isCityState", "boolean", "City-state", "是否城邦"),
        p("isBarbarian", "boolean", "Barbarian civilization", "是否蛮族"),
        p("isSpectator", "boolean", "Spectator", "是否旁观者"),
        q("getNation", "fun(): string", "Nation name", "文明（nation）名"),
        q("getLeaderName", "fun(): string", "Leader name", "领袖名"),
        q("getScore", "fun(): number", "Current score", "当前分数"),
        q("getForce", "fun(): number", "Military might ranking value", "军力排名值"),
        q("getGold", "fun(): number", "Gold", "当前金币"),
        q("getHappiness", "fun(): number", "Happiness", "当前笑脸"),
        q("getStat", "fun(stat: string): number", "Stat reserve value", "返回属性储备值"),
        q("getStatYield", "fun(stat: string): number", "Per-turn yield", "返回每回合产出"),
        q("getGoldPerTurn", "fun(): number", "Net gold per turn", "每回合金币净收入"),
        q("getSciencePerTurn", "fun(): number", "Net science per turn", "每回合科研"),
        q("getCulturePerTurn", "fun(): number", "Net culture per turn", "每回合文化"),
        q("getFoodPerTurn", "fun(): number", "Net food per turn", "每回合食物"),
        q("getProductionPerTurn", "fun(): number", "Net production per turn", "每回合产能"),
        q("getResourceAmount", "fun(resourceName: string): number", "Stockpiled resource amount", "返回资源库存"),
        q("hasResource", "fun(resourceName: string): boolean", "Has at least 1", "是否有 ≥1"),
        q("getResourceStockpiles", "fun(): table", "Table {resourceName = amount}", "资源库存表 {资源名 = 数量}"),
        q("getEra", "fun(): string", "Era name", "当前时代名"),
        q("getEraNumber", "fun(): number", "0-based era index", "0-based 时代序号"),
        q("isResearched", "fun(techName: string): boolean", "Has researched", "是否已研究"),
        q("canResearch", "fun(techName: string): boolean", "Can research", "能否研究"),
        q("getResearchingTech", "fun(): string", "Currently researching tech name", "当前研究中的科技名"),
        q("getResearchProgress", "fun(techName: string): number", "Accumulated science", "已有烧瓶数"),
        q("getTechCount", "fun(): number", "Number of researched techs", "已研究科技数量"),
        q("getTechsResearched", "fun(): string[]", "List of researched tech names", "已研究科技名列表"),
        q("getAvailableTechs", "fun(): string[]", "List of available tech names", "可研究科技名列表"),
        q("getTechCost", "fun(techName: string): number", "Base research cost of a tech", "科技基础研究成本"),
        w("grantTech", "fun(techName: string)", "Instantly grant a tech", "直接授予科技"),
        w("discoverTech", "fun(techName: string)", "Instantly discover a tech (same as grantTech)", "直接发现科技（同 grantTech）"),
        q("hasPolicy", "fun(policyName: string): boolean", "Has adopted", "是否已采纳"),
        q("canAdoptPolicy", "fun(): boolean", "Can adopt any policy", "是否有可采纳的政策"),
        q("getAdoptedPolicyCount", "fun(): number", "Number of adopted policies", "已采纳政策数量"),
        q("getAdoptedPolicies", "fun(): string[]", "List of adopted policy names", "已采纳政策名列表"),
        q("getAvailablePolicyBranches", "fun(): string[]", "List of all policy branch names", "全部政策分支名列表"),
        w("grantPolicy", "fun(policyName: string)", "Instantly adopt a policy", "直接采纳政策"),
        q("getCultureNeededForNextPolicy", "fun(): number", "Culture needed for the next policy", "下一个政策所需文化"),
        q("isAtWarWith", "fun(civName: string): boolean", "At war", "是否交战"),
        q("hasOpenBordersWith", "fun(civName: string): boolean", "Open borders", "是否有开放边界"),
        q("isAlliedWith", "fun(civName: string): boolean", "Allied city-state", "是否为盟友（城邦）"),
        q("getDiplomaticStatus", "fun(civName: string): string", "Diplomatic status string", "外交状态字符串"),
        q("getDiplomaticStatuses", "fun(): table", "Table {civName = statusName} for all known civs", "外交状态表 {文明名 = 状态名}"),
        q("getProximityTo", "fun(civName: string): string", "Proximity string (None/Neighbors/Close/Far)", "邻近度字符串（None/Neighbors/Close/Far）"),
        q("hasEmbassyWith", "fun(civName: string): boolean", "Embassy established either way", "是否已互设大使馆"),
        q("getInfluence", "fun(civName: string): number", "Influence with city-state", "城邦影响力数值"),
        q("getKnownCivs", "fun(): string[]", "Known civilization names", "已知文明名列表"),
        w("addInfluence", "fun(civName: string, amount: number)", "Add influence", "增加城邦影响力"),
        w("declareWarOn", "fun(civName: string)", "Declare war", "宣战"),
        w("makePeaceWith", "fun(civName: string)", "Sign peace (no-op when not at war)", "签订和平（不在战时则为空操作）"),
        q("hasReligion", "fun(): boolean", "Founded a religion", "是否已创建宗教"),
        q("getReligionName", "fun(): string", "Religion name", "宗教名"),
        q("getFaith", "fun(): number", "Faith amount", "信仰值"),
        q("getCities", "fun(): UncivCity[]", "List of city tables", "城市表列表"),
        q("getCity", "fun(cityName: string): UncivCity|nil", "City table by name", "按名称获取城市表"),
        q("getCapital", "fun(): UncivCity|nil", "Capital city table", "首都城市表"),
        q("getCityCount", "fun(): number", "Number of cities", "城市数量"),
        q("getCityNames", "fun(): string[]", "List of city names", "城市名列表"),
        q("getTotalPopulation", "fun(): number", "Sum of all city populations", "所有城市人口总和"),
        q("getWondersBuilt", "fun(): string[]", "List of built wonder names", "已建造的奇观名列表"),
        q("getUnits", "fun(): UncivUnit[]", "List of unit tables", "单位表列表"),
        q("getUnitsMatching", "fun(filter: string): UncivUnit[]", "Filter units by type", "按 filter 筛选单位"),
        q("getUnitCount", "fun(): number", "Total unit count", "单位总数"),
        q("isGoldenAge", "fun(): boolean", "In a golden age", "是否在黄金时代"),
        q("getGoldenAgeTurnsRemaining", "fun(): number", "Turns remaining", "剩余回合"),
        q("getSpyCount", "fun(): number", "Spy count", "间谍数量"),
        q("getSpies", "fun(): table[]", "List of {name, rank, action, location} tables", "间谍详情表列表 {name, rank, action, location}"),
        w("addSpy", "fun()", "Add a spy", "增加一名间谍"),
        q("getLeaderTitle", "fun(): string", "Current leader title", "当前领袖头衔"),
        q("hasUnique", "fun(uniqueText: string): boolean", "Searches: nation + researched techs + adopted policies + current era", "文明是否拥有此 unique（搜索范围：nation + 已研究科技 + 已采纳政策 + 当前时代）"),
        w("addGold", "fun(amount: number)", "Add gold", "增加金币"),
        w("setGold", "fun(amount: number)", "Set gold to exactly this amount", "将金币设为精确值"),
        w("addStat", "fun(stat: string, amount: number)", "Add to a stat reserve", "增加属性"),
        w("addStats", "fun(statsText: string)", "Apply a stats text like \"+2 Gold, +3 Culture\"", "复合属性变化"),
        w("addResource", "fun(resourceName: string, amount: number)", "Add a strategic resource", "增加战略资源"),
        w("consumeResource", "fun(resourceName: string, amount: number)", "Consume a strategic resource", "消耗战略资源"),
        w("triggerGoldenAge", "fun(turns: number?)", "Start a golden age for N turns (omit for default length)", "进入 N 回合黄金时代（省略参数为默认长度）"),
        w("grantFreeGreatPerson", "fun()", "Grant a free great person", "免费伟人"),
        w("setLeaderTitle", "fun(title: string)", "Set the leader title", "修改领袖头衔"),
        w("addNotification", "fun(text: string)", "Popup an in-game notification", "弹出游戏内通知"),
        w("addNotificationAt", "fun(text: string, x: number, y: number)", "Notification that jumps to a map position", "可点击跳转的通知"),
        w("addFreeTech", "fun()", "Grant a free tech", "免费科技点数"),
        w("addUnit", "fun(unitName: string): boolean", "Spawn a unit for this civ", "生成单位"),
        w("addUnitAtCity", "fun(unitName: string, cityName: string): boolean", "Spawn a unit at a city", "在指定城市生成单位"),
        w("addUnitAtTile", "fun(unitName: string, x: number, y: number): boolean", "Spawn a unit at a tile", "在指定坐标生成单位"),
        w("addRebelUnit", "fun(unitName: string): boolean", "Spawn a rebel unit", "生成叛军"),
    )

    // endregion

    // region city

    private val cityEntries = listOf(
        p("id", "string", "City unique ID", "城市唯一 ID"),
        p("name", "string", "City name", "城市名称"),
        p("isCapital", "boolean", "Is the capital", "是否为首都"),
        p("isCoastal", "boolean", "Coastal city", "是否沿海"),
        p("isPuppet", "boolean", "Puppet city", "是否为傀儡城市"),
        p("isBeingRazed", "boolean", "City is being razed", "是否正在被焚毁"),
        p("isConnectedToCapital", "boolean", "Connected to the capital", "是否连接到首都"),
        p("population", "number", "City population", "人口"),
        p("health", "number", "City health", "城市血量"),
        q("getStatYield", "fun(stat: string): number", "Single stat yield", "单项产出"),
        q("getAllYields", "fun(): table", "All yields as a table", "所有产出表"),
        q("getFood", "fun(): number", "Current food yield", "当前食物产出"),
        q("getFoodSurplus", "fun(): number", "Net food per turn (negative when starving)", "每回合净食物（负数为饥荒）"),
        q("getFoodStorage", "fun(): number", "Stored food toward growth", "已存食物（用于人口增长）"),
        q("getFoodNeeded", "fun(): number", "Food needed for the next population", "下一个人口所需食物"),
        q("getProductionProgress", "fun(): number", "Production already invested in current construction", "当前建造已投入产能"),
        q("getProductionCost", "fun(): number", "Total production cost of current construction", "当前建造总成本"),
        q("getTurnsToCompletion", "fun(): number", "Estimated turns to finish current construction", "预计完成回合数"),
        q("getGarrisonedUnit", "fun(): UncivUnit|nil", "Garrison unit table (or nil)", "驻军单位表（无则 nil）"),
        q("getStrength", "fun(): number", "City combat strength", "城市战斗力"),
        q("getSpecialistCount", "fun(): number", "Assigned specialists", "已分配专家数"),
        q("getUnemployedCount", "fun(): number", "Free (unassigned) population", "空闲（未分配）人口"),
        q("getBuiltWonders", "fun(): string[]", "Built wonder names", "已建造奇观名列表"),
        q("isInResistance", "fun(): boolean", "City is in resistance after conquest", "征服后是否处于抵抗"),
        q("hasBuilding", "fun(buildingName: string): boolean", "Has building", "是否有某建筑"),
        q("getBuiltBuildings", "fun(): string[]", "List of built building names", "已建成建筑名列表"),
        q("getBuildingCount", "fun(): number", "Total building count", "建筑总数"),
        q("getWonderCount", "fun(): number", "Wonder count", "奇观数"),
        q("getPosition", "fun(): table", "{x, y} coordinate table", "{x, y} 坐标表"),
        q("getCenterTile", "fun(): UncivTile", "City center tile object", "城市中心 tile 对象"),
        q("getTiles", "fun(): table", "Owned tile coordinates", "拥有的地块坐标列表"),
        q("getCurrentConstruction", "fun(): string", "Currently producing item name", "当前建设中项目名"),
        q("getConstructionQueue", "fun(): string[]", "Construction queue", "建设队列"),
        q("getMajorityReligion", "fun(): string", "Majority religion name", "多数宗教名"),
        q("isHolyCity", "fun(): boolean", "Is a holy city", "是否为圣城"),
        q("hasUnique", "fun(uniqueText: string): boolean", "Check built buildings for this unique", "已建建筑中是否有此 unique"),
        w("addPopulation", "fun(amount: number)", "Add population", "增加人口"),
        w("setPopulation", "fun(count: number)", "Set population exactly (min 1)", "精确设置人口（最低 1）"),
        w("addFood", "fun(amount: number)", "Add stored food", "增加存粮"),
        w("addProduction", "fun(amount: number)", "Add production to current construction", "增加当前建造产能"),
        w("addHealth", "fun(amount: number)", "Heal the city", "治疗城市"),
        w("setName", "fun(newName: string)", "Rename the city", "城市改名"),
        w("addBuilding", "fun(buildingName: string)", "Free-build a building", "免费建造"),
        w("removeBuilding", "fun(buildingName: string)", "Remove a building", "移除建筑"),
        w("sellBuilding", "fun(buildingName: string)", "Sell a building for gold", "出售建筑换取金币"),
        w("setProduction", "fun(itemName: string)", "Set the current construction item", "将当前建造项目设为指定项目"),
        w("addToQueue", "fun(itemName: string)", "Append to the construction queue", "追加到建造队列末尾"),
        w("clearQueue", "fun()", "Clear the whole construction queue", "清空整个建造队列"),
    )

    // endregion

    // region unit

    private val unitEntries = listOf(
        p("id", "string", "Unit instance unique ID", "单位实例唯一 ID"),
        p("name", "string", "Unit name (type name)", "单位名（类型名）"),
        p("instanceName", "string", "Unit instance name", "单位实例名"),
        p("isCivilian", "boolean", "Civilian unit", "是否平民单位"),
        p("isMilitary", "boolean", "Military unit", "是否军事单位"),
        p("isRanged", "boolean", "Ranged unit", "是否远程单位"),
        p("isEmbarked", "boolean", "Unit is embarked", "是否已下海"),
        p("isFortified", "boolean", "Unit is fortified", "是否驻防"),
        p("isAutomated", "boolean", "Unit is automated", "是否自动化"),
        p("base", "table", "Read-only unit template (name, strength, cost, movement, range, unitType, requiredResource, requiredTech, obsoleteTech, upgradesTo, replaces, uniqueTo, promotions)", "单位模板（只读）：name, strength, cost, movement, range, unitType, requiredResource, requiredTech, obsoleteTech, upgradesTo, replaces, uniqueTo, promotions"),
        p("health", "number", "Unit health", "单位生命值"),
        q("getRange", "fun(): number", "Attack range", "射程"),
        q("getEraNumber", "fun(): number", "0-based era index of the unit's owner civilization", "所属文明的时代序号（0-based）"),
        q("getMovement", "fun(): number", "Maximum movement", "最大移动力"),
        q("getCurrentMovement", "fun(): number", "Remaining movement", "剩余移动力"),
        q("getXP", "fun(): number", "Experience points", "经验值"),
        q("getMaxHealth", "fun(): number", "Max HP", "最大生命值"),
        q("getDamage", "fun(): number", "Max health minus current health", "最大生命减当前生命"),
        q("getAttacksLeft", "fun(): number", "Attacks remaining this turn", "本回合剩余攻击次数"),
        q("getVisibilityRange", "fun(): number", "Sight range in tiles", "视野范围（格）"),
        q("getAction", "fun(): string", "Current action string (\"Fortify\", \"moveTo x,y\", ...)", "当前行动字符串（\"Fortify\"、\"moveTo x,y\" 等）"),
        q("canAttack", "fun(): boolean", "Can attack this turn", "本回合能否攻击"),
        q("canPillage", "fun(): boolean", "Current tile has something to pillage", "当前地块是否可掠夺"),
        q("isInEnemyTerritory", "fun(): boolean", "Standing in enemy territory", "是否处于敌方领土"),
        q("isInFriendlyTerritory", "fun(): boolean", "Standing in own territory", "是否处于己方领土"),
        q("isGreatPerson", "fun(): boolean", "Is a great person", "是否为伟人"),
        q("getReligionDisplayName", "fun(): string", "Religion of this unit (\"\" if none)", "单位宗教名（无则为 \"\"）"),
        q("hasPromotion", "fun(promotionName: string): boolean", "Has promotion", "是否有晋升"),
        q("hasUnique", "fun(uniqueText: string): boolean", "Searches unit type + promotion uniques", "单位是否拥有此 unique（含 unit type + 晋升）"),
        q("getPromotions", "fun(): string[]", "Promotion names", "晋升名列表"),
        q("getPromotionCount", "fun(): number", "Number of promotions", "晋升数量"),
        q("hasStatus", "fun(statusName: string): boolean", "Has status", "是否有状态"),
        q("getStatusTurns", "fun(statusName: string): number", "Status turns remaining", "状态剩余回合"),
        q("getPosition", "fun(): table", "{x, y} coordinate table", "{x, y} 坐标表"),
        q("canMoveTo", "fun(x: number, y: number): boolean", "Can move to tile", "能否移动到"),
        q("getOwner", "fun(): string", "Owner civilization name", "所属文明名"),
        q("isOwnedBy", "fun(civName: string): boolean", "Owned by civilization", "是否属于某文明"),
        w("healBy", "fun(amount: number)", "Heal", "回复血量"),
        w("takeDamage", "fun(amount: number)", "Take damage", "造成伤害"),
        w("addXP", "fun(amount: number)", "Add experience", "增加经验"),
        w("setXP", "fun(amount: number)", "Set XP exactly", "精确设置经验"),
        w("setHealth", "fun(amount: number)", "Set health exactly (clamped)", "精确设置生命（自动限制范围）"),
        w("addPromotion", "fun(promotionName: string)", "Add promotion", "添加晋升"),
        w("removePromotion", "fun(promotionName: string)", "Remove promotion", "移除晋升"),
        w("addMovement", "fun(amount: number)", "Add movement", "增加移动力"),
        w("useMovement", "fun(amount: number)", "Spend movement", "消耗移动力"),
        w("setStatus", "fun(statusName: string, turns: number)", "Apply a unit status for N turns", "施加 N 回合的单位状态"),
        w("setAttacksLeft", "fun(count: number)", "Set attacks remaining", "设置剩余攻击次数"),
        w("fortify", "fun()", "Fortify (action = \"Fortify\")", "驻防（action = \"Fortify\"）"),
        w("moveByPath", "fun(path: table): number", "Move along a findPathTo() path; returns steps taken", "沿 findPathTo() 的路径移动；返回实际步数"),
        w("upgrade", "fun()", "Upgrade for free", "免费升级"),
        w("destroy", "fun()", "Destroy the unit", "摧毁单位"),
        w("attackTile", "fun(x: number, y: number): table|false", "Attack a unit/city at a tile; returns {attackerDamage=n, defenderDamage=m} or false (not attackable)", "攻击目标坐标上的单位/城市；返回 {attackerDamage=n, defenderDamage=m} 或 false（不可攻击）"),
        w("teleportTo", "fun(x: number, y: number)", "Teleport to a tile", "传送到指定坐标"),
        q("findPathTo", "fun(x: number, y: number): table|nil", "Returns path {{x,y}, {x,y}, ...} or nil (uses A* multi-turn pathfinding)", "返回路径坐标列表 {{x,y}, {x,y}, ...} 或 nil（A* 多回合寻路）"),
        q("canReach", "fun(x: number, y: number): boolean", "Can reach target tile", "能否到达目标坐标"),
    )

    // endregion

    // region tile

    private val tileEntries = listOf(
        p("position", "table", "{x, y} coordinate table", "{x, y} 坐标表"),
        p("baseTerrain", "string", "Base terrain name", "基础地形名"),
        p("isLand", "boolean", "Land tile", "是否陆地"),
        p("isWater", "boolean", "Water tile", "是否水域"),
        p("isCoast", "boolean", "Coast tile", "是否沿海"),
        p("isHill", "boolean", "Hill tile", "是否丘陵"),
        p("isMountain", "boolean", "Mountain tile", "是否山脉"),
        p("resourceName", "string", "Resource name (\"\" if none)", "资源名（无则为 \"\"）"),
        p("resourceAmount", "number", "Resource amount", "资源数量"),
        p("improvementName", "string", "Improvement name (\"\" if none)", "改良设施名（无则为 \"\"）"),
        q("getX", "fun(): number", "X coordinate", "X 坐标"),
        q("getY", "fun(): number", "Y coordinate", "Y 坐标"),
        q("hasTerrainFeature", "fun(featureName: string): boolean", "Has terrain feature", "是否有地形特征"),
        q("getTerrainFeatures", "fun(): string[]", "Terrain feature names", "地形特征列表"),
        q("isImpassable", "fun(): boolean", "Impassable tile", "是否不可通行"),
        q("isRiver", "fun(): boolean", "Tile has a river", "是否为河流"),
        q("isAdjacentToCoast", "fun(): boolean", "Adjacent to coast", "是否邻接海岸"),
        q("hasRoad", "fun(): boolean", "Has a road", "是否有道路"),
        q("hasRailroad", "fun(): boolean", "Has a railroad", "是否有铁路"),
        q("hasNaturalWonder", "fun(): boolean", "Has a natural wonder", "是否有自然奇观"),
        q("getNaturalWonder", "fun(): string", "Natural wonder name (\"\" if none)", "自然奇观名（无则 \"\"）"),
        q("hasResource", "fun(): boolean", "Has a resource", "是否有资源"),
        q("hasImprovement", "fun(): boolean", "Has an improvement", "是否有改良设施"),
        q("isPillaged", "fun(): boolean", "Tile is pillaged", "是否已被劫掠"),
        q("getYield", "fun(): table", "Yield table {Food=2, Production=1, ...}", "地块产出表 {Food=2, Production=1, ...}"),
        q("isOwned", "fun(): boolean", "Owned by some civ", "是否被拥有"),
        q("getOwner", "fun(): string", "Owner civilization name", "拥有者文明名"),
        q("isOwnedBy", "fun(civName: string): boolean", "Owned by civilization", "是否属于某文明"),
        q("isFriendlyTerritory", "fun(civName: string): boolean", "Friendly territory for the civ", "是否为该文明友方领土"),
        q("isEnemyTerritory", "fun(civName: string): boolean", "Enemy territory for the civ", "是否为该文明敌方领土"),
        q("isCityCenter", "fun(): boolean", "City center tile", "是否为城市中心"),
        q("getOwningCity", "fun(): string", "Owning city name", "拥有城市名"),
        q("isExploredBy", "fun(civName: string): boolean", "Explored by the civ", "某文明是否已探索"),
        q("getDistanceTo", "fun(x: number, y: number): number", "Aerial distance in tiles (-1 if out of map)", "空中直线距离（格，越界返回 -1）"),
        q("isAdjacentTo", "fun(x: number, y: number): boolean", "Adjacent to tile", "是否相邻"),
        q("hasMilitaryUnit", "fun(): boolean", "Has a military unit", "是否有军事单位"),
        q("hasCivilianUnit", "fun(): boolean", "Has a civilian unit", "是否有平民单位"),
        q("getUnits", "fun(): UncivUnit[]", "Units on this tile", "地块上的单位表列表"),
        q("getNeighbors", "fun(): UncivTile[]", "Adjacent tiles", "相邻地块表列表"),
        q("getNeighborAt", "fun(direction: number): UncivTile|nil", "Tile in a direction (0-5)", "指定方向的相邻地块（0-5）"),
        q("getTilesInDistance", "fun(radius: number): UncivTile[]", "All tiles within range", "范围内所有地块"),
        w("setExplored", "fun(civName: string, explored: boolean)", "Set/clear exploration for a civ", "设置/取消某文明对该地块的探索"),
        w("setTerrain", "fun(terrainName: string)", "Change base terrain", "改变基础地形"),
        w("addTerrainFeature", "fun(featureName: string)", "Add terrain feature", "添加地形特征"),
        w("removeTerrainFeature", "fun(featureName: string)", "Remove terrain feature", "移除地形特征"),
        w("setImprovement", "fun(improvementName: string)", "Set improvement", "设置改良设施"),
        w("removeImprovement", "fun()", "Remove improvement", "移除改良设施"),
        w("removeResource", "fun()", "Remove resource", "移除资源"),
        w("setResource", "fun(resourceName: string, amount: number)", "Set resource", "设置资源"),
        w("setRoad", "fun()", "Build a road", "修建道路"),
        w("setRailroad", "fun()", "Build a railroad", "修建铁路"),
        w("removeRoad", "fun()", "Remove road/railroad", "移除道路"),
    )

    // endregion

    // region game

    private val gameEntries = listOf(
        p("turn", "number", "Current turn number", "当前回合数"),
        p("speed", "string", "Game speed name", "游戏速度名"),
        p("difficulty", "string", "Difficulty name", "难度名"),
        q("getYear", "fun(): number", "Current year", "当前年份"),
        q("getCurrentPlayer", "fun(): string", "Current player civilization name", "当前玩家文明名"),
        q("getCurrentPlayerCiv", "fun(): UncivCiv|nil", "Current player civ table (or nil)", "当前玩家文明表（无则 nil）"),
        q("getCiv", "fun(civName: string): UncivCiv|nil", "Civ table by name", "按名称获取文明表"),
        q("getCivById", "fun(id: string): UncivCiv|nil", "Civ table by ID", "按 ID 获取文明表"),
        q("getAllCivs", "fun(): UncivCiv[]", "All civ tables", "所有文明表列表"),
        q("getCivNames", "fun(): string[]", "All civilization names", "所有文明名列表"),
        q("getHumanCivs", "fun(): UncivCiv[]", "Human civilizations", "人类文明列表"),
        q("getAliveMajorCivs", "fun(): UncivCiv[]", "Alive major civilizations", "存活的 major 文明"),
        q("getAliveCityStates", "fun(): UncivCiv[]", "Alive city-states", "存活的城邦"),
        q("getBarbarianCiv", "fun(): UncivCiv", "Barbarian civilization", "蛮族文明"),
        q("getTile", "fun(x: number, y: number): UncivTile|nil", "Tile table at coordinates", "获取地块表"),
        q("findTiles", "fun(criteria: table): UncivTile[]", "Search the map by criteria table (see the findTiles section)", "按条件表搜索全图地块（见 findTiles 章节）"),
        q("getMapWidth", "fun(): number", "Map width", "地图宽度"),
        q("getMapHeight", "fun(): number", "Map height", "地图高度"),
        q("getMapName", "fun(): string", "Map name", "地图名"),
        q("getMapType", "fun(): string", "Map type", "地图类型"),
        q("isWrapped", "fun(): boolean", "Map wraps around", "地图是否环绕"),
        q("getTilesNear", "fun(x: number, y: number, radius: number): UncivTile[]", "Tiles within radius", "范围内地块"),
        q("getEraNames", "fun(): string[]", "All era names", "所有时代名"),
        q("getVictoryTypes", "fun(): string[]", "Enabled victory types", "启用的胜利类型"),
        q("getMods", "fun(): string[]", "Enabled mod names", "启用的模组列表"),
        q("getBaseRuleset", "fun(): string", "Base ruleset name", "基础规则集"),
        q("getRulesetBuildings", "fun(): string[]", "All building names", "所有建筑名列表"),
        q("getRulesetUnits", "fun(): string[]", "All unit names", "所有单位名列表"),
        q("getRulesetTechs", "fun(): string[]", "All tech names", "所有科技名列表"),
        q("getRulesetPolicies", "fun(): string[]", "All policy names", "所有政策名列表"),
        q("getRulesetEras", "fun(): string[]", "All era names", "所有时代名列表"),
        q("getRulesetPromotions", "fun(): string[]", "All promotion names", "所有晋升名列表"),
        q("getRulesetTerrains", "fun(): string[]", "Terrain names", "地形名列表"),
        q("getRulesetResources", "fun(): string[]", "Resource names", "资源名列表"),
        q("getRulesetImprovements", "fun(): string[]", "Improvement names", "改良名列表"),
        q("getRulesetNations", "fun(): string[]", "Nation names", "文明（nation）名列表"),
        q("getRulesetReligions", "fun(): string[]", "Religion names", "宗教名列表"),
        q("getRulesetBeliefs", "fun(): string[]", "Belief names", "信条名列表"),
        q("getRulesetEvents", "fun(): string[]", "Event names", "事件名列表"),
        q("getRulesetNaturalWonders", "fun(): string[]", "Natural wonder names", "自然奇观名列表"),
        q("getRulesetUnitTypes", "fun(): string[]", "Unit type names", "单位类型名列表"),
        q("doesBuildingExist", "fun(buildingName: string): boolean", "Building exists in the ruleset", "规则集中是否存在"),
        q("doesUnitExist", "fun(unitName: string): boolean", "Unit exists in the ruleset", "规则集中是否存在"),
        q("doesTechExist", "fun(techName: string): boolean", "Tech exists in the ruleset", "科技是否存在"),
        q("doesPolicyExist", "fun(policyName: string): boolean", "Policy exists in the ruleset", "政策是否存在"),
        q("doesEraExist", "fun(eraName: string): boolean", "Era exists in the ruleset", "时代是否存在"),
        q("doesPromotionExist", "fun(promotionName: string): boolean", "Promotion exists in the ruleset", "晋升是否存在"),
        q("doesTerrainExist", "fun(terrainName: string): boolean", "Terrain exists in the ruleset", "地形是否存在"),
        q("doesResourceExist", "fun(resourceName: string): boolean", "Resource exists in the ruleset", "资源是否存在"),
        q("doesImprovementExist", "fun(improvementName: string): boolean", "Improvement exists in the ruleset", "改良是否存在"),
        q("doesNationExist", "fun(nationName: string): boolean", "Nation exists in the ruleset", "文明（nation）是否存在"),
        q("doesBeliefExist", "fun(beliefName: string): boolean", "Belief exists in the ruleset", "信条是否存在"),
        q("doesEventExist", "fun(eventName: string): boolean", "Event exists in the ruleset", "事件是否存在"),
        w("addGlobalNotification", "fun(text: string)", "Notify all human players", "向所有人类玩家发通知"),
        w("revealEntireMap", "fun(civName: string)", "Reveal the whole map for a civ", "对某文明揭示全地图"),
        w("revealTilesAround", "fun(civName: string, x: number, y: number, radius: number)", "Reveal tiles around a position for a civ", "对某文明揭示指定位置周边地块"),
    )

    // endregion

    /**
     * owner → 条目列表（顺序即文档展示顺序）。
     * 条目名集合必须与 [LuaAPI.apiCatalog] 完全一致（测试强制）。
     */
    val entries: Map<String, List<LuaApiDocEntry>> = mapOf(
        "ctx" to ctxEntries,
        "store" to storeEntries,
        "civ" to civEntries,
        "city" to cityEntries,
        "unit" to unitEntries,
        "tile" to tileEntries,
        "game" to gameEntries,
    )

    /** 条目名按 owner 索引，供生成器与测试使用 */
    val entriesByName: Map<String, Map<String, LuaApiDocEntry>> = entries.mapValues { (_, list) ->
        list.associateBy { it.name }
    }
}

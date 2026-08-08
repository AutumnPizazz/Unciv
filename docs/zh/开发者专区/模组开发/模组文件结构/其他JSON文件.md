---
title: 其他 JSON 文件
---

# 其他 JSON 文件

## Difficulties.json

[链接到原始文件](https://github.com/yairm210/Unciv/tree/master/android/assets/jsons/Civ%20V%20-%20Gods%20&%20Kings/Difficulties.json)

此可选文件定义玩家在开始新游戏时可以选择的难度级别。

基础规则集模组可以省略文件或提供空列表，尽管至少需要一种难度，在这种情况下，它们继承[来自 Vanilla 规则集的那些](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Vanilla/Difficulties.json)。

每个难度级别具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| baseHappiness | Integer | 0 | |
| extraHappinessPerLuxury | Float | 0 | |
| researchCostModifier | Float | 1 | |
| unitCostModifier | Float | 1 | |
| unitSupplyBase | Integer | 5 | |
| unitSupplyPerCity | Integer | 2 | |
| buildingCostModifier | Float | 1 | |
| policyCostModifier | Float | 1 | |
| unhappinessModifier | Float | 1 | |
| barbarianBonus | Float | 0 | |
| barbarianSpawnDelay | Integer | 0 | |
| playerBonusStartingUnits | List of Strings | 空 | 也可以是 'Era Starting Unit'，映射到 Eras 文件的 `startingMilitaryUnit`。所有其他单位必须在 [Units.json](单位相关JSON文件.md#unitsjson) 中。仅适用于人类玩家文明 |
| aiDifficultyLevel | String | 空 | 从选择的难度对 AI 应用一些修饰符。诸如 unhappinessModifier 和 aiUnhappinessModifier 等修饰符会相乘堆叠。如果为空：默认为 "Chieftain" 或第一个可用的难度。 |
| aiCityGrowthModifier | Float | 1 | |
| aiUnitCostModifier | Float | 1 | |
| aiBuildingCostModifier | Float | 1 | |
| aiWonderCostModifier | Float | 1 | |
| aiBuildingMaintenanceModifier | Float | 1 | |
| aiUnitMaintenanceModifier | Float | 1 | |
| aiUnitSupplyModifier | Integer | 5 | |
| aiFreeTechs | List of Strings | 空 | 必须在 [Techs.json](文明相关JSON文件.md#techsjson) 中 |
| aiMajorCivBonusStartingUnits | List of Strings | 空 | 与 playerBonusStartingUnits 相同的规则，请参阅上文。仅适用于 AI 主要文明 |
| aiCityStateBonusStartingUnits | List of Strings | 空 | 与 playerBonusStartingUnits 相同的规则，请参阅上文。仅适用于城邦文明 |
| aiUnhappinessModifier | Float | 1 | |
| turnBarbariansCanEnterPlayerTiles | Integer | 0 | |
| clearBarbarianCampReward | Integer | 25 | |
| uniques | List of Strings | 空 | 选择此难度时将被视为 GlobalUniques 的一部分 |

## Eras.json

[链接到原始文件](https://github.com/yairm210/Unciv/tree/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/Eras.json)

此文件应包含你想要在模组中使用的所有时代。

每个时代可以具有以下属性：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| researchAgreementCost | Integer (≥0) | 300 | 当科技最先进的文明处于此时代时，研究协议的成本 |
| iconRGB | [List of 3× Integer](#rgb-颜色列表) | white | 此时代的科技图标在科技屏幕中应具有的 RGB 颜色 |
| startingSettlerCount | Integer (≥0) | 1 | 在此时代开始游戏时应生成的定居者单位数量（不建议将其设置为零 [^1]） |
| startingSettlerUnit | String | "Settler" | 应用于前一个字段的单位名称。必须在 [Units.json](单位相关JSON文件.md#unitsjson) 中，或者必须存在具有 "Founds a new city" unique 的单位 |
| startingWorkerCount | Integer (≥0) | 0 | 在此时代开始游戏时应生成的工人单位数量 |
| startingWorkerUnit | String | "Worker" | 应用于前一个字段的单位名称。如果 startingWorkerCount>0，则它必须存在于 [Units.json](单位相关JSON文件.md#unitsjson) 中，或者必须存在具有 "Can build [filter] improvements on tiles" unique 的单位 |
| startingMilitaryUnitCount | Integer (≥0) | 1 | 在此时代开始游戏时应生成的军事单位数量 |
| startingMilitaryUnit | String | "Warrior" | 应用于前一个字段的单位名称。必须在 [Units.json](单位相关JSON文件.md#unitsjson) 中 |
| startingGold | Integer (≥0) | 0 | 每个文明在此时代开始游戏时应收到的金币数量 |
| startingCulture | Integer (≥0) | 0 | 每个文明在此时代开始游戏时应收到的文化数量 |
| settlerPopulation | Integer (>0) | 1 | 在此时代开始游戏时定居时每个城市应有的人口数量 |
| settlerBuildings | List of Strings | 空 | 在此时代开始游戏时每当建立城市时应自动建造的建筑 |
| startingObsoleteWonders | List of Strings | 空 | 在此时代开始游戏时应无法建造的奇观（以及技术上建筑）。在基础游戏中用于移除所有早于 2 个时代的奇观 |
| baseUnitBuyCost | Integer | 200 | 用于独特能力 "Can be purchased with [stat] [cityFilter]" 的默认值 |
| embarkDefense | Integer | 3 | 此时代中登船单位的默认防御力 |
| startPercent | Integer | 0 | 开始时，在 [Speed.json](#speedsjson) 中指定的总回合数中跳过的百分比（\[0\]%-\[100\]） |
| citySound | String | "cityClassical" | 在此时代建立城市时使用的声音 |
| uniques | List of Strings | 空 | 进入此时代后应用这批uniques作为等价的全局uniques |

[^1]: 在模组中成功将 startingSettlerCount 设置为零（想法：征服或死亡）并不容易。某些玩家控制的设置需要至少一个定居者，通过任何来源（请参阅难度设置以了解其他可能的定居者来源），否则你将无法开始游戏：一旦城市挑战需要所有玩家都有一个，并且允许任何城邦需要那些城邦有一个。还会影响失败规则。

## Speeds.json

[链接到原始文件](https://github.com/yairm210/Unciv/tree/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/Speeds.json)

此可选文件应包含你想要在模组中使用的所有速度。

基础规则集模组可以省略文件或提供空列表，尽管至少需要一种游戏速度，在这种情况下，它们继承[来自 Vanilla 规则集的那些](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Vanilla/Speeds.json)。

每个速度可以具有以下属性：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | 速度的名称 |
| modifier | Float (≥0) | 1.0 | 整体游戏速度修饰符 |
| productionCostModifier | Float (≥0) | `modifier` 值 | 缩放单位和建筑的生产成本 |
| goldCostModifier | Float (≥0) | `modifier` 值 | 缩放金币成本 |
| scienceCostModifier | Float (≥0) | `modifier` 值 | 缩放科学成本 |
| cultureCostModifier | Float (≥0) | `modifier` 值 | 缩放文化成本 |
| faithCostModifier | Float (≥0) | `modifier` 值 | 缩放信仰成本 |
| improvementBuildLengthModifier | Float (≥0) | `modifier` 值 | 缩放工人建造地块改良设施所需的时间 |
| barbarianModifier | Float (≥0) | `modifier` 值 | 缩放野蛮人生成之间的时间 |
| goldGiftModifier | Float (≥0) | `modifier` 值 | 缩放向城邦赠送金币获得的影响力 |
| cityStateTributeScalingInterval | Float (≥0) | 6.5 | 玩家向城邦索要的金币数量增加 5 金币所需的回合数 |
| goldenAgeLengthModifier | Float (≥0) | `modifier` 值 | 缩放黄金时代的长度 |
| religiousPressureAdjacentCity | Integer (≥0) | 6 | 定义一个城市对附近城市施加的宗教压力 |
| peaceDealDuration | Integer (≥0) | 10 | 和平协议持续的回合数 |
| dealDuration | Integer (≥0) | 30 | 非和平协议（研究协议、开放边界等）持续的回合数 |
| startYear | Float | -4000 | 游戏的起始年份（负数为公元前） |
| turns | List | 必需 | 每回合的时间间隔列表，[请参阅下文](#每回合时间间隔) |
| uniques | List of Strings | 空 | 选择此速度时将被视为 GlobalUniques 的一部分 |

### 每回合时间间隔

"turns" 属性定义回合之间经过的年数。该属性由对象列表组成，每个对象具有 2 个必需属性："yearsPerTurn"（Float）和 "untilTurn"（Integer）

| 属性 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| yearsPerTurn | Integer | 必需 | 回合之间经过的年数 |
| untilTurn | Integer | 必需 | 此间隔的结束（如果是最后一个对象，请参阅下文） |

对于每一行，"yearsPerTurn" 应用于从 "untilTurn"-1 到 "untilTurn" 步骤。
列表中最后一个 "untilTurn" 在年份计算中被忽略，即如果游戏超过了该回合数，年份将继续按最后一个条目的 "yearsPerTurn" 增加。
但是，这在后期时代开始游戏时使用：Era.startPercent 相对于最后一个 "untilTurn"。

下面的代码是有效的 "turns" 定义的示例，它指定游戏的前 50 回合每回合持续 60 年，接下来的 30 回合（以及第 80 回合之后玩的任何回合）每回合持续 40 年。

```json
"turns": [
    {"yearsPerTurn": 60, "untilTurn":  50},
    {"yearsPerTurn": 40, "untilTurn":  80}
]
```

## Events.json

事件允许用户在触发器的选项之间进行选择以激活。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | 用于通过 "Triggers a [event] event" unique 进行触发 |
| text | String | None | 向用户显示的风味文字 |
| presentation | One of: "None", "Alert", "Floating" | Alert | "Alert" 表示常规弹出窗口，"None" 表示随机选择，"Floating" 用于教程风格的指示器 |
| civilopediaText | List | 可选 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |
| choices | List of EventChoices | | 用户可以选择触发其中一个可行的选择 |

你可以使用 text 和/或 civilopediaText，如果两者都存在，则两者都显示（但为什么呢？）

事件选择由以下组成：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| text | String | 必需 | 作为按钮显示给用户。应该是动作名称 - "Do X" |
| uniques | List of uniques to trigger or limit conditions | 必需 | 此选择在被选中时激活的触发器，以及 "Unavailable" / "Only available" uniques |
| keyShortcut | key to select (name) | none | 键名称请参阅 [Gdx.Input.Keys](https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/Input.java#L69) |
| civilopediaText | List | 可选 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

在这里，civilopediaText 显示在活动按钮之外，在 triggeredUniques 之前。

## ModOptions.json

<!-- [链接到原始文件](https://github.com/yairm210/Unciv/tree/master/android/assets/jsons/Civ%20V%20-%20Gods%20&%20Kings/ModOptions.json) -->

此文件略有不同：

- 在 Vanilla 规则集中不存在
- 完全是可选的，但在下载模组后会创建

请注意，此文件控制_声明性模组兼容性_（进行中）- 例如，有 [uniques](../Unique能力列表.md#modoptions-uniques) 表示你的模组应该只或从不作为'永久视觉模组'使用。
不兼容性过滤目前适用于扩展模组和基础模组之间，但请随意使用相同的 Unique 记录已知的扩展到扩展的不兼容性。敬请期待！

该文件可以具有以下属性，不包括 Unciv 自动设置的值：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| isBaseRuleset | Boolean | false | 如果为 true，则替换 vanilla 规则集 |
| uniques | List | 空 | 模组范围的特性，[请参阅此处](../Unique能力列表.md#modoptions-uniques) |
| techsToRemove | List | 空 | 要移除的[技术](文明相关JSON文件.md#techsjson)或[technologyFilter](../Unique参数详解.md#technologyfilter)列表（仅适用于 isBaseRuleset=false） |
| buildingsToRemove | List | 空 | 要移除的[建筑或奇观](文明相关JSON文件.md#buildingsjson)或[buildingFilter](../Unique参数详解.md#buildingfilter)列表（仅适用于 isBaseRuleset=false） |
| unitsToRemove | List | 空 | 要移除的[单位](单位相关JSON文件.md#unitsjson)或[unitFilter](../Unique参数详解.md#baseunitfilter)列表（仅适用于 isBaseRuleset=false） |
| nationsToRemove | List | 空 | 要移除的[国家](文明相关JSON文件.md#nationsjson)或[nationFilter](../Unique参数详解.md#nationfilter)列表（仅适用于 isBaseRuleset=false） |
| policyBranchesToRemove | List | 空 | 要移除的[政策分支](文明相关JSON文件.md#policiesjson)列表（仅适用于 isBaseRuleset=false） |
| policiesToRemove | List | 空 | 要移除的[政策](文明相关JSON文件.md#policiesjson)列表（仅适用于 isBaseRuleset=false） |
| beliefsToRemove | List | 空 | 要移除的[信仰](文明相关JSON文件.md#beliefsjson)列表（仅适用于 isBaseRuleset=false） |
| religionsToRemove | List | 空 | 要移除的[宗教](文明相关JSON文件.md#religionsjson)列表（仅适用于 isBaseRuleset=false） |
| constants | Object | 空 | 请参阅 [ModConstants](#modconstants) |
| tileset | String | 空 | 仅适用于基础规则集 |
| unitset | String | 空 | 仅适用于基础规则集 |

通常从 github 元数据自动设置的值是：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modUrl | String | | 从中下载模组的 github 页面，或者如果使用了免费托管的 zip 则为空 |
| defaultBranch | String | master | 仓库的默认分支 |

为了澄清：当你的模组通过 github 分发时，在模组仓库中包含这些没有任何影响。
但是，当模组在_没有_ github 仓库的情况下分发时，这些值可以_应该_由作者在分发的 `ModOptions.json` 中设置。

### ModConstants

存储在 ModOptions.constants 中，这是 Unciv 内部使用的常量集合。
这是唯一_逐字段_从模组合并的结构，而不是覆盖，因此你可以在一个模组中更改来自野蛮人的 XP，在另一个模组中更改城市距离。在冲突的情况下，无法保证哪个模组获胜，只保证忽略_默认_值。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| maxXPfromBarbarians | Int | 30 | [^A] |
| cityStrengthBase | Float | 8.0 | [^B] |
| cityStrengthPerPop | Float | 0.4 | [^B] |
| cityStrengthFromTechsMultiplier | Float | 5.5 | [^B] |
| cityStrengthFromTechsExponent | Float | 2.8 | [^B] |
| cityStrengthFromTechsFullMultiplier | Float | 1.0 | [^B] |
| cityStrengthFromGarrison | Float | 0.2 | [^B] |
| baseCityBombardRange | Int | 2 | [^S] |
| cityWorkRange | Int | 3 | [^T] |
| cityExpandRange | Int | 5 | [^U] |
| cityAirUnitCapacity | Int | 6 | [^W] |
| unitSupplyPerPopulation | Float | 0.5 | [^C] |
| minimalCityDistance | Int | 3 | [^D] |
| minimalCityDistanceOnDifferentContinents | Int | 2 | [^D] |
| unitUpgradeCost | Object | [请参阅下文](#unitupgradecost) | [^J] |
| naturalWonderCountMultiplier | Float | 0.124 | [^E] |
| naturalWonderCountAddedConstant | Float | 0.1 | [^E] |
| ancientRuinCountMultiplier | Float | 0.02 | [^F] |
| spawnIceBelowTemperature | Float | -0.8 | [^G] |
| maxLakeSize | Int | 10 | [^H] |
| riverCountMultiplier | Float | 0.01 | [^I] |
| minRiverLength | Int | 5 | [^I] |
| maxRiverLength | Int | 666 | [^I] |
| religionLimitBase | Int | 1 | [^K] |
| religionLimitMultiplier | Float | 0.5 | [^K] |
| pantheonBase | Int | 10 | [^L] |
| pantheonGrowth | Int | 5 | [^L] |
| workboatAutomationSearchMaxTiles | Int | 20 | [^M] |
| maxSpyRank | Int | 3 | [^N] |
| spyRankSkillPercentBonus | Float | 30 | [^O] |
| minimumWarDuration | Int | 10 | [^P] |
| baseTurnsUntilRevolt | Int | 4 | [^Q] |
| cityStateElectionTurns | Int | 15 | [^R] |
| maxImprovementTechErasForward | Int | None | [^S] |
| goldGiftMultiplier | Float | 1 | [^T] |
| goldGiftTradeMultiplier | Float | 0.8 | [^U] |
| goldGiftDegradationMultiplier | Float | 1.0 | [^V] |

图例：

- [^A]: <span>可以从与野蛮人的战斗中获得的最大经验值</span>
- [^B]: <span>城市力量公式：</span>
    力量 = baseStrength + strengthPerPop + strengthFromTiles +
    ((%techs \* multiplier) ^ exponent) \* fullMultiplier +
    (garrisonBonus \* garrisonUnitStrength \* garrisonUnitHealth/100) +
    defensiveBuildingStrength
    其中 %techs 是科技树中已完成的科技的百分比
    如果此规则集中不存在科技，则 %techs = 0.5（=50%）
- [^S]: <span>城市可以攻击的距离</span>
- [^T]: <span>城市中人口可以工作的地块的距离。注意：较高的值可能会导致性能问题并可能导致错误。cityWorkRange 可以大于 cityExpandRange。</span>
- [^U]: <span>城市可以扩展其边界的距离。注意：较高的值可能会导致性能问题并可能导致错误。</span>
- [^C]: <span>单位供应公式：</span>
    供应 = unitSupplyBase（difficulties.json）
    unitSupplyPerCity \* amountOfCities +（difficulties.json）
    unitSupplyPerPopulation \* amountOfPopulationInAllCities
    unitSupplyBase 和 unitSupplyPerCity 可以在 difficulties.json 中找到
    unitSupplyBase、unitSupplyPerCity 和 unitSupplyPerPopulation 也可以通过 uniques 增加
- [^D]: <span>任何两个城市之间必须的最小距离，不包括城市所在的地块</span>
    数字是两个城市之间的地块数，不包括城市所在的地块。
    例如，"C__C"，其中 "C" 是有城市的地块，"_" 是没有城市的地块，距离为 2。
    第一个常量适用于同一大陆上的城市，第二个适用于不同大陆上的城市。
- [^E]: NaturalWonderGenerator 使用这些来确定给定地图大小要生成的自然奇观数量。数量与地图半径线性缩放：#wonders = radius * naturalWonderCountMultiplier + naturalWonderCountAddedConstant。默认值实际上意味着 Tiny - 1，Small - 2，Medium - 3，Large - 4，Huge - 5，自定义半径 >=109 - 所有 G&K 奇观。
- [^F]: <span>`MapGenerator.spreadAncientRuins`：遗迹数量 = 合适地块数量 * 此值</span>
- [^G]: <span>`MapGenerator.spawnIce`：在 T < 此值的地方生成冰，其中 T 从温度极端、纬度和 perlin 噪声计算。</span>
- [^H]: <span>`MapGenerator.spawnLakesAndCoasts`：最多此地块数量的水体变成湖泊</span>
- [^I]: <span>`RiverGenerator`：河流频率和长度边界</span>
- [^J]: [UnitUpgradeCost](#unitupgradecost) 子结构。
- [^K]: 可发现的最大宗教数量 = religionLimitBase + floor(MajorCivCount * religionLimitMultiplier)
- [^L]: 万神殿的成本 = pantheonBase + CivsWithReligion * pantheonGrowth
- [^M]: 当 AI 决定是否建造工作船时，从城市中心搜索可改进地块的回合数
- [^N]: <span>任何间谍可以达到的最大等级</span>
- [^O]: <span>每个等级给予的技能加成</span>
- [^P]: <span>文明在谈判和平之前必须等待的回合数</span>
- [^Q]: <span>在生成叛乱之前的回合数</span>
- [^R]: <span>城邦选举之间的回合数</span>
- [^S]: 如果设置，改进选择器将静默跳过技术要求比你的当前时代 + 此值更先进的改进。例如：使用 0 时，贸易站在中世纪之前不会显示，使用 1 时，它们将在古典时代显示。
- [^T]: <span>单边贸易的金币价值乘数，作为礼物存储。</span>
- [^U]: 常规贸易的金币价值乘数，作为礼物存储。设置为 0 以禁用双边贸易中的金币赠送。
- [^U]: 修改 "GaveUsGifts" 外交修饰符耗尽的速度。较高的值使其耗尽得更快。通常，根据外交关系，默认值每回合减少约 2.5%。
- [^W]: <span>可以驻扎在城市中的空中单位数量，不包括携带/运输的空中单位。</span>

#### UnitUpgradeCost

这些值不会单独合并，只会合并整个子结构。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| base | Float | 10 | |
| perProduction | Float | 2 | |
| eraMultiplier | Float | 0 | |
| exponent | Float | 1 | |
| roundTo | Int | 5 | |

单位升级的金币成本公式是（向下舍入到 `roundTo` 的倍数）：
        (
            max((`base` + `perProduction` \* (new_unit_cost - old_unit_cost)), 0)
            \* (1 + eraNumber \* `eraMultiplier`) \* `civModifier`
        ) ^ `exponent`
其中 `civModifier` 是适用于 ["\[relativeAmount\]% Gold cost of upgrading"](../Unique能力列表.md#global-uniques) uniques 的乘法聚合。

## GlobalUniques.json

[链接到原始文件](https://github.com/yairm210/Unciv/tree/master/android/assets/jsons/GlobalUniques.json)

GlobalUniques 定义全局应用的 uniques。例如，Vanilla 规则集在此定义不满意的效果。

基础规则集模组可以省略文件，在这种情况下，它们继承[来自 Vanilla 规则集的那些](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Vanilla/GlobalUniques.json)。
或者，它们可以提供带有空对象（`{}`）的文件，意味着该模组没有全局 Uniques。

它具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | "GlobalUniques" | name 字段未使用，但仍必须设置（规则集验证器可能会显示它）。 |
| uniques | List of Strings | 空 | 全局应用的[独特能力](../Unique能力列表.md)列表 |
| unitUniques | List of Strings | 空 | 应用于每个单位的[独特能力](../Unique能力列表.md)列表 |

当扩展规则集定义 GlobalUniques 时，所有 uniques 都会合并。目前，无法更改/移除基础模组设置的 uniques。

## Tutorials.json

[链接到原始文件](https://github.com/yairm210/Unciv/tree/master/android/assets/jsons/Tutorials.json)

**注意基础规则集模组可以通过添加名称等于模组名称的 "Tutorial" 在此处定义"欢迎页面"！**
作为一般规则的例外，基础规则集模组中的此文件不会_替换_默认值，而是像扩展模组一样添加到其中。
此外，即使原始文件在 vanilla json 之上找到一级，通常也将它放在 `<mod>/jsons/` 下。

每个教程具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | 条目名称 |
| category | String | 可选 | 此教程将显示的类别。"Tutorials" 是默认值。 |
| civilopediaText | List | 可选 | 请参阅 [civilopediaText 章节](#civilopedia-text) |
| steps | List of Strings | 可选 | 纯文本 |

如果条目同时包含 `steps` 和 `civilopediaText` 属性，则首先显示 `civilopediaText`。
显示为 Popup 的教程可以显示外部图像（不是纹理图集的一部分），如果 ExtraImages 中（直接位于资源或模组文件夹下）有同名的图像。
这是搜索到的，意味着定义教程的模组无关紧要，模组可以覆盖内置的 ExtraImages，区分大小写取决于操作系统。

## VictoryTypes.json

[链接到原始文件](https://github.com/yairm210/Unciv/tree/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/VictoryTypes.json)

这些文件包含此模组提供的胜利，以及要实现胜利必须达到的里程碑。
文件的大部分包含在胜利屏幕上向用户显示的字符串，其余部分是获胜的要求。

基础规则集模组可以省略文件或提供空列表，尽管至少需要一种胜利类型，在这种情况下，它们继承[来自 Vanilla 规则集的那些](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Vanilla/VictoryTypes.json)。

每个胜利具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | 胜利的名称 |
| victoryScreenHeader | String | none | 显示在胜利屏幕中"我们的状态"中的胜利页脚中 |
| victoryString | String | none | 当你用此胜利赢得游戏时，显示在胜利屏幕的页脚中 |
| defeatString | String | none | 当其他人用此胜利赢得游戏时，显示在胜利屏幕的页脚中 |
| hiddenInVictoryScreen | Boolean | false | 此胜利的进度是否在胜利屏幕中隐藏 |
| requiredSpaceshipParts | List of Strings | 空 | 必须添加到首都以实现相应里程碑的太空船部件 |
| Milestones | List of Strings | 必需 | 必须完成以获胜的里程碑列表，[请参阅下文](#里程碑) |
| civilopediaText | List | 可选 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

### 里程碑

目前支持以下里程碑：

| 里程碑 | 要求 |
|--------|------|
| Build [building] | 在任何城市建造建筑 [building] |
| Anyone should build [building] | 所有人必须建造建筑 [building] 才能使所有玩家拥有此里程碑 |
| Add all [comment] in capital | 将此胜利的 `requiredSpaceshipParts` 字段中的所有单位添加到首都 |
| Destroy all players | 你必须是唯一剩下城市的主要文明 |
| Capture all capitals | 捕获游戏中所有主要文明的原始首都 |
| Complete [amount] Policy branches | 完全完成至少 [amount] 个政策分支 |
| Win diplomatic vote | 在游戏中的任何时候赢得外交投票（联合国）。你以后可能会失去，但仍保留此里程碑 |
| Become the world religion | 使你的宗教成为所有主要文明中大多数城市的主流宗教 |
| Have highest score after max turns | 基本上是时间胜利。启用"最大回合"滑块并在达到该数量时计算分数 |
| Have more [countable] than each player's [countable] | 使你给定的 `countable` 多于每个其他文明的 `countable` 以实现此胜利。这对于模拟类似于美丽新世界中的文化胜利很有用。 |

## Civilopedia 文本

在 json 中定义并列在文明百科中的任何"东西"都可以提供额外的文本，特别是针对文明百科的。当自动生成的显示不足时，这可以用来更好地解释特殊考虑，或者用于"风味"、背景故事等。此类文本可以格式化并链接到其他文明百科条目，在限制范围内。

格式示例：

```json
"civilopediaText": [
    { "text": "古代遗迹在探索时提供一次性的随机奖励" },
    { "separator": true },
    {
        "text": "这一行是红色的，并链接到侦察兵，包括图标",
        "link": "Unit/Scout",
        "color": "red"
    },
    {
        "text": "带有金色星号的粗大标题",
        "header": 1,
        "starred": true,
        "color": "#ffeb7f"
    },
],
```

属性列表 - 注意并非所有组合都有效：

| 属性 | 类型 | 说明 |
|------|------|------|
| `text` | String | 要显示的文本 |
| `link` | String | 创建链接和图标，格式：Category/Name 或 _外部_ 链接（'http://'、'https://'、'mailto:'） |
| `icon` | String | 显示图标而不链接，格式：Category/Name |
| `extraImage` | String | 显示图像而不是文本。可以是在纹理集中找到的路径，或者是 ExtraImages 文件夹中的 png 或 jpg 的名称 |
| `imageSize` | Float | [extraImage] 的世界单位大小，较小的坐标在保持纵横比的情况下计算。可用宽度 |
| `header` | Integer | 标题级别。1 表示双倍文本大小，并从那里递减 |
| `size` | Integer | 文本大小，是 18。使用 `size` 或 `header` 但不要同时使用 |
| `indent` | Integer | 缩进级别。0 表示文本将跟随图标，1 与所有图标的右侧对齐，每进一步是 30 单位 |
| `padding` | Float | 行之间的垂直填充，5 单位 |
| `color` | String | 设置文本颜色，接受名称或 6/3 位 Web 颜色（例如 #FFA040） |
| `separator` | Boolean | 渲染分隔线而不是文本。只能与 `color` 和 `size`（线宽，默认 2）结合使用 |
| `starred` | Boolean | 用星号图标装饰文本 - 如果设置，它接收 `color` 而不是文本 |
| `centered` | Boolean | 居中该行（并关闭自动换行）。对于 `extraImage`，启用裁剪到内容以平衡透明边框 |
| `iconCrossed` | Boolean | 在 `icon` 或 `link` 图像上绘制红色 X |

来自 json 的行将"包围"自动生成的行，使得后者刚好插入在第一个带有链接的 json 行上方（如果有）。如果没有 json 行有链接，它们将插入在自动标题和自动信息之间。然而，此方法将来可能会更改。

注意：`text` 现在也支持内联颜色标记。插入 `«color»` 开始为文本着色，`«»` 停止。`color` 可以是名称或 6/8 位十六进制符号，如 `#ffa040`（与 `color` 属性符号不同仅在于不允许 3 位代码，但允许 alpha 通道）。
实际上，`«»` 标记在翻译_之后_被替换为 `[]`，然后传递到 [gdx 标记语言](https://libgdx.com/wiki/graphics/2d/fonts/color-markup-language)。

注意：在模组中使用 ExtraImages 文件夹直到版本 4.11.5 才能正常工作。

## RGB 颜色列表

某些对象可以指定具有自己的独特颜色。颜色由以下顺序的 3× Integer 列表定义：红色、绿色、蓝色。颜色范围从 \[0, 0, 0\]（黑色）到 \[255, 255, 255\]（白色）。

注意：某些对象的默认值是 [gdx 颜色类](https://javadoc.io/doc/com.badlogicgames.gdx/gdx/latest/com/badlogic/gdx/graphics/Color.html)。常量的值如下：

| 名称  | 值             |
|-------|-----------------|
| gold  | [225, 215, 0]   |
| white | [255, 255, 255] |
| black | [0, 0, 0]       |
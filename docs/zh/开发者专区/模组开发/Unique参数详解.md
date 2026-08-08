---
title: Unique 参数详解
---

# Unique 参数详解

本页面包含 uniques 中使用的所有不同参数的概述以及可以填充到它们中的值。
下面描述了每个不同的参数类型，包括它们的所有可能值。
这些分为两类：

- 描述：例如，"任何单位的名称"
- `像这样的文本`。最后一个必须完全相同使用

请注意，所有这些都区分大小写！

## 通用过滤器规则

除了 `populationFilter` 和 `resourceFilter` 之外的所有过滤器都接受格式为 `{A} {B} {C}` 等的多个值，意味着"对象必须匹配所有这些过滤器"

> 示例：`[{Military} {Water}] units`，`[{Wounded} {Armor}] units` 等。

`[` 和第一个 `{` 之间，或最后一个 `}` 和结束 `]` 之间不允许有空格或其他文本。但是，`} {` 中的空格是强制性的。

所有过滤器都接受 `non-[filter]` 作为可能的值

> 示例：`[non-[Wounded]] units`

这些可以通过嵌套组合，除了"ALL"过滤器不能包含另一个"ALL"过滤器，即使中间有 NON 过滤器。

> 示例：`[{non-[Wounded]} {Armor}] units` 意味着单位是类型 Armor 并且满生命值。
> 示例：`[non-[{Wounded} {Armor}]] units` 意味着单位既不是受伤的也不是 Armor 单位。

`[{non-[{Wounded} {Armor}]} {Embarked}] units` 将失败，因为游戏将同时处理两个"} {"并看到 `non-[{Wounded` 和 `Armor}]`，两者都无效。

Civilopedia 中复杂过滤器的显示可能会变得不可读。如果是这样，考虑隐藏该 unique 并使用 `Comment []` unique 单独提供更好的措辞。

## civFilter

允许过滤特定的文明。

允许的值：

- `Human player`
- `AI player`
- `Friendly`
- `Hostile`
- `Open Borders`
- `Known` - 意识到相关文明的文明。Known 包括自己。
- [nationFilter](#nationfilter)

## nationFilter

允许过滤特定的文明。由 [ModOptions.nationsToRemove](模组文件结构/其他JSON文件.md#modoptionsjson)使用。

允许的值：

- `All`，`all`
- `City-States`，`City-State`
- `Major`
- 文明名称
- 文明拥有的 unique（逐字，没有占位符）

## baseUnitFilter

单位过滤器可以分为两部分：`baseUnitFilter`s 和 `mapUnitFilter`s。
前者根据单位在 json 中出现的样子进行测试。
这意味着它没有拥有它的文明或它所站的地块，只是它的基本属性。
后者根据单位在地图上出现的样子进行测试，包括它的文明、地块、生命值和所有其他属性。

允许的值：

- 单位名称
- 单位类型 - 例如 Melee、Ranged、WaterSubmarine 等
- `Land`、`Water`、`Air`
- `land units`、`water units`、`air units`
- `non-air` 用于非空中非导弹单位
- `Military`、`military units`
- `Civilian`、`civilian units`
- `All`、`all`
- `Melee`
- `Ranged`
- `Nuclear Weapon`
- `Great Person`
- `Embarked`
- 匹配该单位所需的 [technologyfilter](#technologyfilter) - 例如 `Modern Era`
- 单位拥有的任何精确 unique
- 单位类型拥有的任何精确 unique
- 上述任何组合（仅在所有匹配时才匹配）。格式是 `{filter1} {filter2}` 并且可以匹配任意数量的过滤器。例如：`[{Modern era} {Land}]` units

## mapUnitFilter

这指示放置在地图上的单位。与 `baseUnitFilter` 比较。

允许的值：

- 任何匹配的 [baseUnitFilter](#baseunitfilter)
- 任何匹配拥有者的 [civFilter](#civfilter)
- 单位拥有的任何 unique - 还包括 [baseUnitFilter](#baseunitfilter) 未捕获的 uniques，例如晋升
- 任何晋升名称，或晋升拥有的精确 unique
- `Wounded`
- `Embarked`
- `City-State`
- `Barbarians`、`Barbarian`
- `Non-City`
- 同样，上述任何组合也是允许的，例如 `[{Wounded} {Water}]` units。

你可以在游戏中使用控制台通过 `unit checkfilter <filter>` 命令检查这一点

## buildingFilter

允许仅对特定建筑激活 unique。

允许的值：

- `All`、`all`
- `Buildings`、`Building`
- `Wonder`、`Wonders`
- `National Wonder`、`National`
- `World Wonder`、`World` -- 所有不是国家奇观的奇观
- 建筑名称
- 它替换的建筑的名称（因此，例如图书馆的 uniques 也将适用于造纸厂）
- 匹配该建筑所需的 [technologyfilter](#technologyfilter) - 例如 Modern Era
- 建筑拥有的精确 unique（例如：`spaceship part`）
- `Culture`、`Gold` 等，如果建筑对于该统计是 `stat-related`。Stat-related 建筑定义为以下之一：
    - 直接提供该统计（例如 +1 Culture）
    - 为该统计提供百分比加成（例如 +10% Production）
    - 作为资源的奖励提供该统计（例如每小麦 +1 Food）
    - 为一定数量的人口提供该统计（例如每 2 人口 +1 Science [cityFilter]）
- 上述任何组合（仅在所有匹配时才匹配）。格式是 `{filter1} {filter2}` 最多任意数量的过滤器。例如 `[{Ancient era} {Food}]` buildings。

## cityFilter

cityFilters 允许我们选择受此 unique 影响的城市范围：

- `in this city`
- `in all cities`、`All`、`all` - 通常适用于相关文明拥有的所有城市
- `in your cities`、`Your`
- `in all coastal cities`、`Coastal`
- `in capital`、`Capital`
- `in all non-occupied cities`、`Non-occupied` - 所有不是傀儡且没有因最近被征服而产生额外不快乐的城市
- `in all cities with a world wonder`
- `in all cities connected to capital`
- `in all cities with a garrison`、`Garrisoned`
- `in non-enemy foreign cities` - 在除你之外的其他文明拥有的、你没有与之交战的所有城市中
- `in enemy cities`、`Enemy`
- `in foreign cities`、`Foreign`
- `in annexed cities`、`Annexed`
- `in puppeted cities`、`Puppeted`
- `in resisting cities`、`Resisting`
- `in cities being razed`、`Razing`
- `in holy cities`、`Holy`
- `in City-State cities`
- `in cities following this religion` - 应仅用于宗教的万神殿/信徒 uniques
- `in cities following our religion`
- `in all cities in which the majority religion is a major religion`
- `in all cities in which the majority religion is an enhanced religion`
- [civFilter]

你可以在游戏中使用控制台通过 `city checkfilter <filter>` 命令检查这一点

## eraFilter

用于过滤特定时代。

允许的值：

- 时代名称（例如：`Modern era`）
- `any era`
- `Starting Era` - 游戏开始的时代
- `pre-[era]` - 出现在给定时代之前的任何时代（例如：`pre-[Modern era]`）
- `post-[era]` - 出现在给定时代之后的任何时代（例如：`post-[Ancient era]`）

## improvementFilter

用于过滤特定的改良设施。

允许的值：

- 改良设施名称
- 改良设施拥有的精确 unique（例如：`spaceship improvement`）
- `Improvement`
- `All`、`all`
- `Great Improvement`、`Great`
- `All Road` - 用于道路和铁路

## populationFilter

确定城市人口一部分的过滤器。

允许的值：

- `Population`
- `Specialists`
- `Unemployed`
- `Followers of the Majority Religion` 或 `Followers of this Religion`，两者仅当该宗教是城市中的多数宗教时才适用
- 专家名称

## religionFilter

用于过滤特定宗教

- `any`
- `major`
- `enhanced`
- `your`
- `foreign`
- `enemy`
- 宗教符号的名称
- 信仰的名称
- 宗教拥有的信仰的 unique

## policyFilter

允许的值：

- `All`、`all`
- `[policyBranchName] branch`
- 政策的名称
- 政策拥有的 unique（逐字，没有占位符）

## combatantFilter

允许的值：

- [mapUnitFilter](#mapunitfilter)，用于单位战斗者
- `City`、`All` 或 [civFilter](#civfilter)，用于城市战斗者

由于 mapUnitFilter 包含 civFilter，这意味着 civFilter 可以应用于单位和城市的 combatantFilter。

## regionType

用于在游戏开始时将世界划分为区域，每个区域放置单个玩家。
允许的值是 `Hybrid` 和任何具有以下两个 uniques 之一的地形的名称：

- `A Region is formed with at least [amount]% [simpleTerrain] tiles, with priority [amount]`
- `A Region is formed with at least [amount]% [simpleTerrain] tiles and [simpleTerrain] tiles, with priority [amount]`

## simpleTerrain

由 NaturalWonderGenerator 用于放置自然奇观

允许的值：

- `Land`
- `Water`
- `Elevated`
- 任何地形的名称

## stats

这指示由特定统计组成的文本，稍微复杂一些。

每个统计由几个统计变化组成，每个变化形式为 `+{amount} {stat}`，
其中'stat'是七个主要统计之一
（例如 `Production`、`Food`、`Gold`、`Science`、`Culture`、`Happiness` 和 `Faith`）。
例如：`+1 Science`。

这些可以用 ", " 串在一起，例如：`+2 Production, +3 Food`。

## resourceFilter

允许的值：

- 资源名称
- `any`
- `All`、`all`
- 资源类型：`Strategic`、`Luxury`、`Bonus`
- 改良时提供的统计（例如 `Food`）

## stockpiledResource

这指示对应于自定义库存资源的文本。

这些是全局文明资源，作用类似于主要的文明范围的资源，如 `Gold` 和 `Faith`。
你可以生成它们并消耗它们。如果你库存中没有足够的数量，消耗它们的操作将被阻止。

要使用，你需要首先定义一个具有"Stockpilled" Unique 的 TileResources。然后你可以在其他 Uniques 中引用它们。

## Stockpile

可以添加到文明的东西

允许的值：

- 库存资源（见上文）
- 统计名称 - 用于全局统计，不是城市统计
- `Stored Food`（用于城市）
- `Golden Age points`

## technologyFilter

目前仅针对 [ModOptions.techsToRemove](模组文件结构/其他JSON文件.md#modoptionsjson)实现。

允许的值：

- `All`、`all`
- 匹配科技时代的 [eraFilter](#erafilter)
- 科技的名称
- 科技拥有的 unique（逐字，没有占位符）

## terrainFilter

这指示单个地块上的地形。

允许的值：

- 过滤器命名特定的 json 属性（按名称）：
    - 基础地形
    - 地形特征
    - 基础地形 uniques
    - 地形特征 uniques
    - 资源
    - 自然奇观
    - 匹配地块拥有者的 [nationFilter](#nationfilter)
- 或者过滤器是选择派生测试的常量字符串：
    - `All`、`all`
    - `Terrain`
    - `Water`、`Land`
    - `Coastal`（至少一个直接邻居是海岸）
    - `River`（在所有'地块上的河流'上下文中，意味着'至少一侧与河流相邻'）
    - `Open terrain`、`Rough terrain`（注意所有没有 rough unique 的地形都算作开放）
    - `Friendly Land`、`Friendly` - 属于你的土地，或对你有开放边界的其他文明
    - `Foreign Land` - 任何不是友好土地的土地
    - `Enemy Land`、`Enemy` - 任何属于你与之交战的文明的土地
    - `your` - 属于你的土地
    - `Unowned` - 不被任何文明拥有的土地
    - `Water resource`、`Strategic resource`、`Luxury resource`、`Bonus resource`、`resource`
    - `Natural Wonder`（与上面相反，后者意味着按名称测试特定的自然奇观，这测试任何自然奇观）
    - `Featureless`
    - `Fresh Water`
    - `non-fresh water`
    - `Impassible`

请注意所有这些都区分大小写。

注意：资源过滤器取决于在过滤器运行的上下文中是否已知查看文明。水和特定测试需要查看文明，并且如果资源需要科技才能可见，则该科技必须由查看文明研究。其他资源类别测试只有在资源不需要任何科技的情况下才能在没有已知查看文明的情况下成功。所以 - 测试你的模组！

例如，unique "[stats] from [tileFilter] tiles [cityFilter]" 可以匹配几种情况：

## tileFilter

允许的值：

- 该地块的 [terrainFilter](#terrainfilter)
- 该地块的 [improvementFilter](#improvementfilter)
- 拥有该地块的文明的 [civFilter](#civfilter)
- `Improvement` 或 `improved` 用于有任何改良设施的地块
- `unimproved` 用于没有改良设施的地块
- `pillaged` 用于被掠夺的地块
- `worked` 用于被城市工作的地块

你可以在游戏中使用控制台通过 `tile checkfilter <filter>` 命令检查这一点

## terrainQuality

用于指示在将世界划分为区域时，应该将地形用于什么用途，每个区域在游戏开始时放置单个玩家。

允许的值：

- `Undesirable`
- `Food`
- `Desirable`
- `Production`

## countable

指示*可以计数的东西*，用于比较和乘法 uniques

允许的值：

- 整数常量 - 任何正整数或负整数
    - 示例：`Only available <when number of [123] is more than [0]>`
- `turns` - 已进行的回合数
    - 示例：`Only available <when number of [turns] is more than [0]>`
    - 无论游戏速度或开始时代如何，始终从零开始
- `year` - 当前年份
    - 示例：`Only available <when number of [year] is more than [0]>`
    - 取决于游戏速度或开始时代，公元前年份为负数
- `Cities` - 相关文明拥有的城市数量
    - 示例：`Only available <when number of [Cities] is more than [0]>`
- `Units` - 相关文明拥有的单位数量
    - 示例：`Only available <when number of [Units] is more than [0]>`
- 统计名称（`Production`、`Food`、`Gold`、`Science`、`Culture`、`Happiness` 或 `Faith`）
    - 示例：`Only available <when number of [Science] is more than [0]>`
    - 获取统计*储备*，而不是每回合的数量（可以是城市统计或文明统计，取决于 unique 在何处使用）
- 统计/资源每回合
    - 示例：`Only available <when number of [[Culture] Per Turn] is more than [0]>`
    - 获取文明每回合获得的统计或资源数量
- `Completed Policy branches`
    - 示例：`Only available <when number of [Completed Policy branches] is more than [0]>`
- `[cityFilter] Cities`
    - 示例：`Only available <when number of [[in all cities] Cities] is more than [0]>`
- `[mapUnitFilter] Units`
    - 示例：`Only available <when number of [[Wounded] Units] is more than [0]>`
- `Carried [mapUnitFilter] units` - 该单位携带的单位数量
    - 示例：`Only available <when number of [Carried [Air] units] is more than [0]>`
    - 仅计算匹配过滤器的运输单位。用于'when number of'条件。
- `[buildingFilter] Buildings`
    - 示例：`Only available <when number of [[Culture] Buildings] is more than [0]>`
- `[buildingFilter] Buildings by [civFilter] Civilizations`
    - 示例：`Only available <when number of [[Culture] Buildings by [City-States] Civilizations] is more than [0]>`
- `[cityFilter] Cities of [civFilter] Civilizations`
    - 示例：`Only available <when number of [[in all cities] Cities of [City-States] Civilizations] is more than [0]>`
- `Adopted [policyFilter] Policies`
    - 示例：`Only available <when number of [Adopted [Oligarchy] Policies] is more than [0]>`
- `Adopted [policyFilter] Policies by [civFilter] Civilizations`
    - 示例：`Only available <when number of [Adopted [Oligarchy] Policies by [City-States] Civilizations] is more than [0]>`
- `Researched [techFilter] Technologies`
    - 示例：`Only available <when number of [Researched [Agriculture] Technologies] is more than [0]>`
    - 计算相关文明研究的匹配科技
    - 可重复科技，如未来科技，只计算一次
- `Remaining [civFilter] Civilizations`
    - 示例：`Only available <when number of [Remaining [City-States] Civilizations] is more than [0]>`
- `Owned [tileFilter] Tiles`
    - 示例：`Only available <when number of [Owned [Farm] Tiles] is more than [0]>`
- `[tileFilter] Tiles`
    - 示例：`Only available <when number of [[Desert] Tiles] is more than [0]>`
- 资源名称 - 来自 [TileResources.json](模组文件结构/地图相关JSON文件.md#tileresourcesjson)
    - 示例：`Only available <when number of [Iron] is more than [0]>`
    - 可以是城市统计或文明统计，取决于 unique 在何处使用
    - 例如：如果 unique 放置在建筑上，则检索的资源将是城市的。如果放置在政策上，它们将是文明的。
    - 这可能产生影响，例如本地资源，这些资源是按城市计算的。
- `[resourceFilter] resource of [civFilter] Civilizations`
    - 示例：`Only available <when number of [[Strategic] resource of [City-States] Civilizations] is more than [0]>`
- `Era number` - 当前玩家所处的时代编号
    - 示例：`Only available <when number of [Era number] is more than [0]>`
    - Eras.json 中时代的从零开始的索引。
- `Speed modifier for [stat]` - 特定统计的游戏速度修改器，作为百分比
    - 示例：`Only available <when number of [Speed modifier for [Culture]] is more than [0]>`
    - 从玩家选择的 Speeds.json 条目中选择适当的字段。
    - 它返回乘以 100 的值。
    - Food 和 Happiness 返回通用的 `modifier` 字段。
    - 其他字段如 `goldGiftModifier` 或 `barbarianModifier` 无法通过此 Countable 访问。
- 评估表达式！
    - 示例：`Only available <when number of [[Iron] + 2] is more than [0]>`
    - 表达式支持任意数学运算，并且可以包含其他 countables，当用方括号包围时。
    - 例如，由于 `Cities` 是一个 countable，并且 `[Melee] units` 是一个 countable，你可以有类似这样的东西：`([[Melee] units] + 1) / [Cities]`（空格是可选的，但有助于可读性）
    - 由于在翻译时，括号被移除，表达式将显示为 `(Melee units + 1) / Cities`
    - 2 个值之间支持的操作是：+、-、*、/、%、^
    - 1 个值上支持的操作是：-（否定）、√（平方根）、abs（绝对值 - 将负数变为正数）、sqrt（平方根）、floor（向下取整）、ceil（向上取整）
    - 支持的函数：
    -  - `max(expression,expression,...)`
    -  - `min(expression,expression,...)`
---
title: 单位相关 JSON 文件
---

# 单位相关 JSON 文件

## Units.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/Units.json)

此文件应包含你想要在模组中使用的所有单位列表，包括军事和民用单位。

每个单位具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| unitType | String | 必需 | 单位的类型。必须在 [UnitTypes.json](#unittypesjson) 中 |
| cost | Integer | -1 | 建造此单位所需的生产力。所需的生产力始终为正数 |
| movement | Integer | 0 | 单位拥有的移动点数 |
| strength | Integer | 0 | 单位的近战攻击和防御力量。如果此值和 rangedStrength 被省略或为 0，则单位为民用 |
| rangedStrength | Integer | 0 | 单位的远程攻击和防御力量。如果省略，则单位不能进行远程攻击。如果使用，则必须设置 strength。 |
| religiousStrength | Integer | 0 | 单位的宗教攻击和防御力量 |
| range | Integer | 2 | 可以进行远程攻击的范围 |
| interceptRange | Integer | 0 | 在此范围内攻击的空中单位将被拦截 |
| requiredTech | String | none | 建造此单位所需的技术。必须在 [Techs.json](文明相关JSON文件.md#techsjson) 中 |
| obsoleteTech | String | none | 研究此技术后，将无法建造该单位。必须在 [Techs.json](文明相关JSON文件.md#techsjson) 中 |
| requiredResource | String | none | 建造此单位时消耗的资源。必须在 [TileResources.json](地图相关JSON文件.md#tileresourcesjson) 中 |
| upgradesTo | String | none | 当此单位可用时可以升级到的单位。必须在 [Units.json](#unitsjson) 中 |
| replaces | String | none | 如果此单位是某个国家独有的，则这是它替换的单位。必须在 [Units.json](#unitsjson) 中 |
| uniqueTo | String | none | 此单位独有的国家。必须在 [Nations.json](文明相关JSON文件.md#nationsjson) 中 |
| hurryCostModifier | Integer | 0 | 如果此单位用金币购买，其价格增加此百分比 |
| promotions | List of Strings | 空 | 单位在建造时自动获得的所有晋升列表。每个晋升必须在 [UnitPromotions.json](#unitpromotionsjson) 中 |
| uniques | List of Strings | 空 | 此单位具有的[独特能力](../Unique能力列表.md)列表 |
| replacementTextForUniques | String | none | 如果提供，则显示此内容而不是 uniques 列表。可用于更好的格式化。 |
| attackSound | String | none | 单位攻击时播放的声音，如果还没有以单位名称命名的声音。对于可能的值，请参阅 [声音](../图像和音频资源.md#声音) |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

## UnitPromotions.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/UnitPromotions.json)

此文件列出了可用的单位晋升。

每个晋升必须有一个图标，除了以 " I"、" II"、" III" 结尾的进程（不允许 IV V VI）通过查找没有这些后缀的图标并添加星号来渲染。

请记住，晋升可以用 XP "购买"，但也可以由单位类型、建筑、奇观等授予。它们在单位升级时保留，因此国家独特单位可以在升级时继承的特殊属性应该在晋升中，而不是单位 json 中的 uniques/stats（例如：投石兵撤退）。

每个晋升具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | 请参阅上面的 "I, II, III" 进程 |
| prerequisites | List of Strings | 空 | 先决晋升 |
| column | Integer | 可选 | 确定在晋升选择器屏幕上的放置顺序。名称是历史性的，这些坐标不再直接控制放置。没有坐标的晋升确保最后放置。（...） |
| row | Integer | 可选 | …在没有坐标的基础模组中，没有先决条件的晋升按字母顺序排序并从上到下放置，屏幕的其余部分将逻辑地构建依赖关系。如果你的模组有"Heal instantly"，建议使用 row=0 将其放在顶部 |
| unitTypes | List of Strings | 空 | 此晋升适用的单位类型，如 [UnitTypes.json](#unittypesjson) 中所指定 |
| uniques | List of Strings | 空 | 此晋升授予单位的[独特能力](../Unique能力列表.md)列表 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |
| innerColor | List | 空 | *图标*的颜色 |
| outerColor | List | 空 | *背景*的颜色 |

## UnitTypes.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/UnitTypes.json)

此可选文件用于定义新的单位类型。这些名称可以在 unitFilters 中使用，这些类型确定单位移动的域：在陆地上、在水上或通过空中。
对于基础规则集模组，如果省略文件或包含空列表，则[来自 Vanilla 规则集的所有类型](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Vanilla/UnitTypes.json)会自动添加：
Civilian、Melee、Ranged、Scout、Mounted、Armor、Siege、WaterCivilian、WaterMelee、WaterRanged、WaterSubmarine、WaterAircraftCarrier、Fighter、Bomber、AtomicBomber 和 Missile。

每个单位类型具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| movementType | Enum | 必需 | 单位移动的域。允许的值："Water"、"Land"、"Air" |
| uniques | List of String | none | 此晋升授予此类型单位的[独特能力](../Unique能力列表.md)列表 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

## UnitNameGroups.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/UnitNameGroups.json)

提供可以应用于单位的名称列表。一旦采用名称，就不会再次应用。

每个单位名称组具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | 单位名称组的唯一名称（例如：Scientist） |
| names | List of String | 必需 | 可以应用于组的名称列表（例如：Alan Turing） |
| uniques | List of String | none | 在创建时应用于单位的[可触发独特能力](../Unique能力列表.md#triggerable-uniques)列表 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |
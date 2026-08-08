---
title: 文明相关 JSON 文件
---

# 文明相关 JSON 文件

## Beliefs.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/Beliefs.json)

此文件包含你的模组中可以为宗教选择的信仰。

每个信仰具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| type | Enum | 必需 | 信仰类型。值必须是 Pantheon、Founder、Follower 或 Enhancer |
| uniques | List of Strings | 空 | 此信仰为其追随城市添加的[独特能力](../Unique能力列表.md)列表 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

## Buildings.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/Buildings.json)

此文件包含你想要在模组中使用的所有建筑和奇观。

每个建筑具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| cost | Integer | -1 | 建造建筑所需的生产力。如果为 -1，则使用 `requiredTech` 的 `buildingCost` [列](#列结构) |
| [`<stats>`](地图相关JSON文件.md#一般统计) | Float | 0 | 建筑产生的每回合收益 |
| maintenance | Integer | 0 | 建筑的维护费用 |
| isWonder | Boolean | false | 此建筑是否为全球奇观 |
| isNationalWonder | Boolean | false | 此建筑是否为国家奇观 |
| requiredBuilding | String | none | 在建造此建筑之前必须建造的建筑。必须在 [Buildings.json](#buildingsjson) 中 |
| requiredTech | String | none | 在建造此建筑之前必须研究的科技。必须在 [Techs.json](#techsjson) 中 |
| requiredResource | String | none | 建造此建筑时消耗的资源。必须在 [TileResources.json](地图相关JSON文件.md#tileresourcesjson) 中 |
| requiredNearbyImprovedResources | List of Strings | 空 | 仅当此列表中的任何资源位于此城市的边界内并已改进时，才能建造此建筑。每个资源必须在 [TileResources.json](地图相关JSON文件.md#tileresourcesjson) 中 |
| replaces | String | none | 应被此建筑替换的建筑的名称。必须在 [Buildings.json](#buildingsjson) 中 |
| uniqueTo | String | none | 如果提供，则只有具有此名称的文明可以建造此建筑。必须在 [Nations.json](#nationsjson) 中 |
| cityStrength | Integer | 0 | 建造此建筑的城市获得的战斗力加成 |
| cityHealth | Integer | 0 | 建造此建筑的城市获得的生命值加成 |
| hurryCostModifier | Integer | 0 | 当使用金币或信仰购买此建筑时，价格增加此百分比 |
| quote | String | none | 如果此建筑是（国家）奇观，则此字符串将显示在完成弹窗上 |
| uniques | List of Strings | 空 | 此建筑具有的[独特能力](../Unique能力列表.md)列表 |
| replacementTextForUniques | String | none | 如果提供，则显示此字符串而不是所有 uniques |
| percentStatBonus | Object | none | 建筑提供的统计数据的百分比加成。格式与[专业统计数据](地图相关JSON文件.md#专业统计数据)相同（数字为百分比。例如，`[30]` 表示统计数据加成 30%） |
| greatPersonPoints | Object | none | 此建筑每回合产生的伟人点数。有效键是单位名称（Great Scientist、Warrior 等），有效值是整数 |
| specialistSlots | Object | none | 此建筑提供的专家槽位。有效键是专家名称（在 [Specialists.json](#specialistsjson) 中定义），有效值是整数，为此专家提供的槽位数 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

## Nations.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/Nations.json)

此文件包含所有国家和城邦，包括野蛮人和观察者。

每个国家具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| leaderName | String | none | 仅城邦可以省略！如果你想要领袖头像，图像文件名必须完全匹配，包括大小写 |
| style | String | none | 附加到像素单位图像名称的修饰符 |
| cityStateType | String | none | 区分主要文明和城邦（必须在 [CityStateTypes.json](#citystatetypesjson) 中） |
| startBias | List of strings | 空 | 以下的一个或多个：[terrainFilter](../Unique参数详解.md#terrainfilter) 或 "Avoid [terrainFilter]"。[^S] |
| preferredVictoryType | String | Neutral | 主要文明将追求的胜利类型（无需在 [VictoryTypes.json](其他JSON文件.md#victorytypesjson) 中指定） |
| personality | String | none | 在 [Personalities.json](#personalitiesjson) 中指定的个性的名称 |
| favoredReligion | String | none | 主要文明在创立宗教时（如果可用）将选择的宗教。必须在 [Religions.json](#religionsjson) 中 |
| startIntroPart1 | String | none | 游戏开始时向玩家显示的介绍性文字...[^V] |
| startIntroPart2 | String | none | ...第二段。___不要___使用"TBD"！！！留空以跳过该警报。 |
| declaringWar | String | none | 另一个问候，支持语音挂钩 [^V] |
| attacked | String | none | 另一个问候，支持语音挂钩 [^V] |
| defeated | String | none | 另一个问候，支持语音挂钩 [^V] |
| denounced | String | none | 另一个问候，支持语音挂钩 [^V] |
| introduction | String | none | 另一个问候，支持语音挂钩 [^V] |
| neutralHello | String | none | 另一个问候，支持语音挂钩 [^V] |
| hateHello | String | none | 另一个问候，支持语音挂钩 [^V] |
| tradeRequest | String | none | 另一个问候，支持语音挂钩 [^V] |
| declaringFriendship | String | none | 另一个问候，支持语音挂钩 [^V] |
| innerColor | [List of 3× Integer](其他JSON文件.md#rgb-颜色列表) | black | 国家图标外环的 RGB 颜色 |
| outerColor | [List of 3× Integer](其他JSON文件.md#rgb-颜色列表) | 必需 | 国家图标内圈的 RGB 颜色 |
| uniqueName | String | none | 此国家特殊特征的装饰性名称 |
| uniqueText | String | none | "uniques"的替换文本。如果为空，则单独列出 uniques |
| uniques | List | 空 | 此文明具有的[独特能力](../Unique能力列表.md)列表 |
| cities | List | 空 | 按顺序用于新建立城市的城市名称。主要文明和城邦必需 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

[^S]: "Coast"偏好（_除非_与"Avoid"结合）被转换为对沿海地块、湖泊旁地块、河流地块或近河地块的复杂测试，并且这些文明首先被处理。在这种情况下，将忽略其他 startBias 条目。
其他正面（没有"Avoid"）的 startBias 接下来被处理。多个正面偏好被视为相等，但不会获得"回退"。
单个正面 startBias 如果没有（或不再有）具有该主要类型的区域，则可以获得"回退"区域：任何具有尽可能多的指定地形的剩余区域都可以。
多个"Avoid"条目被视为相等（并降低成功的机会 - 如果没有区域避免_所有_指定类型，该文明会获得一个随机区域）。
当组合偏好地形与"Avoid"时，后者优先，并且在选择非要避免类型的区域时，偏好地形只有次要权重。
这些注释__仅__在生成的地图上玩游戏时有效，从地图编辑器加载的地图没有"区域"，并且 startBias 处理方式不同（但你可以期望单条目 startBias 效果最好）。

[^V]: 请参阅 [提供领袖声音](../图像和音频资源.md#提供领袖声音)

## Personalities.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/Personalities.json)

此文件包含计算机玩家的所有个性。

每个个性具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| preferredVictoryType | String | Neutral | 主要文明将追求的胜利类型（无需在 [VictoryTypes.json](其他JSON文件.md#victorytypesjson) 中指定） |
| [`<stats>`](地图相关JSON文件.md#一般统计), [`<behaviors>`](文明相关JSON文件.md#个性行为) | Float | 5 | 计算机玩家对统计数据的关注程度。通常范围从 0（不关注）到 10（双倍关注） |
| priorities | Object | none | 每个政策分支的优先级 [^B] |
| uniques | List | 空 | 此个性具有的[独特能力](../Unique能力列表.md)列表 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

[^B]: 类似于 [政策优先级](#分支优先级) "priorities"对象定义主要文明的 AI 对政策分支的优先级。AI 为其首选胜利类型选择编号最高的政策分支。如果两个或更多候选分支具有相同的优先级，AI 在候选分支中随机选择一个。

该对象使用政策分支名称和整数将政策分支映射到主要文明的优先级值。任何未列出的分支的默认值为 0

下面的代码是有效的"priorities"定义的示例。

```
"priorities": {
    "Tradition": 30,
    "Liberty": 20,
    "Honor": 10
}
```

### 个性行为

个性行为**尚未完全实现**，其名称可能会更改。在它们准备好之前使用它们可能会使模组无法游玩。***以下是 WIP - 阅读风险自负！***

有 7 种定义行为会影响 AI 文明的行为。较高的值意味着它们的行为更像该属性。

- Military：决定文明对建造军队的重视程度，但不一定是使用军队。较高的值意味着更关注军事，较低的值意味着它可能更和平。
- Agressive：决定文明在战争期间如何使用其单位以及它们优先考虑哪些建筑。较高的值意味着文明更具攻击性，较低的值意味着它更具防御性。
- War：决定文明宣战的可能性。0 表示文明根本不会宣战。
- Commerce：决定文明对贸易、开放边界和解放城邦的开放程度。较高的值意味着即使与不喜欢的文明也有更高的贸易频率。
- Diplomacy：决定文明宣布友谊、防御条约、和平条约或其他外交行动的可能性。
- Loyal：决定文明对持久联盟的重视程度，加入战争意愿，以及鄙视其他不可靠文明的程度。
- Expansion：决定文明对新城市建立或征服的关注程度。较低的值意味着它们可能更关注文化。


## CityStateTypes.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/CityStateTypes.json)

此可选文件用于定义城邦的类型。这些类型决定了主要文明在通过影响力与城邦结交或结盟时获得的利益。
对于基础规则集模组，如果省略文件或包含空列表，则[来自 Vanilla 规则集的所有类型](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Vanilla/CityStateTypes.json)会自动添加：
Cultured、Maritime、Mercantile、Militaristic、Religious。

每个城邦类型具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| friendBonusUniques | List of Strings | 空 | 当与此城邦结交时授予主要文明的[独特能力](../Unique能力列表.md)列表 |
| allyBonusUniques | List of Strings | 空 | 当与城邦结盟时授予主要文明的[独特能力](../Unique能力列表.md)列表 |
| color | [List of 3× Integer](其他JSON文件.md#rgb-颜色列表) | [255, 255, 255] | 文明百科中文字的 RGB 颜色 |

## Policies.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/Policies.json)

此文件包含所有可以用文化"购买"的社会政策。

它们组织在'分支'中，每个分支有一个'开启者'、一个或多个'成员'政策和一个'完成者'。因此，此文件使用两个级别进行组织 - 分支和成员政策。

'开启者'的属性在分支级别定义，而'完成者'是成员级别的一个条目，_必须_命名为 `分支名称 + " Complete"`，区分大小写。例如，政策分支"Tradition"的完成者将具有名称"Tradition Complete"。

### 分支结构

每个政策分支具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| era | String | 必需 | 解锁时代，如 [Eras.json](其他JSON文件.md#erasjson) 中定义 |
| priorities | Object | none | 每个胜利类型的优先级，[请参阅此处](#分支优先级) |
| uniques | List | 空 | 采用此政策分支时授予的[独特能力](../Unique能力列表.md)列表 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |
| policies | List | 空 | [成员政策](#成员政策结构)和[分支'完成者'](#分支完成者结构)列表 - 注意 {} 和 [] 的嵌套 |

#### 成员政策结构

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| row | Integer | 必需 | UI 中的放置，每个单位大约是图标大小的一半 |
| column | Integer | 必需 | UI 中的放置，每个单位大约是图标大小的一半 |
| requires | List | 空 | 先决政策名称列表 |
| uniques | List | 空 | 采用此政策成员时授予的[独特能力](../Unique能力列表.md)列表 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

#### 分支完成者结构

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| uniques | List | 空 | 采用分支中所有政策成员时授予的[独特能力](../Unique能力列表.md)列表 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

### 分支优先级

"priorities"对象定义主要文明的 AI 对政策分支的优先级。AI 选择此处列出的首选胜利类型的最高总和与个性中列出的编号之和最高的政策分支。如果两个或更多候选分支具有相同的优先级，AI 在候选分支中随机选择一个。

该对象使用字符串和整数将胜利类型映射到主要文明的优先级值。如果未指定首选胜利类型，则默认优先级值设置为 0。

下面的代码是有效的"priorities"定义的示例。

```json
"priorities": {
"Neutral": 0,
"Cultural": 10,
"Diplomatic": 0,
"Domination": 0,
"Scientific": 10
}
```

## Quests.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/Quests.json)

此文件包含城邦可能给予主要文明的任务。

每个任务具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | 定义任务标准，[请参阅下文](#任务名称) |
| description | String | 必需 | 向玩家显示的任务描述。可以根据 `name` 添加额外信息，[请参阅下文](#任务名称) |
| type | Enum | Individual | Individual 或 Global |
| influence | Float | 40 | 任务完成时获得的影响力奖励 |
| duration | Integer | 0 | 完成任务的最大回合数。如果为 0，则没有回合限制 |
| minimumCivs | Integer | 1 | 开始任务所需的最少文明数。仅对 type = Global 有意义 |
| weightForCityStateType | Object | none | 对每个[城邦类型](#citystatetypesjson)或城邦个性（Friendly、Neutral、Hostile、Irrational）的此任务的相对权重乘数，[请参阅下文](#任务权重) |

### 任务名称

任务的名称定义任务的标准。如果它们在预定义枚举中未定义，则它们不会有行为。在描述中，任务描述中的方括号 `[]` 被额外信息替换（`Invest` 除外）。预定义任务名称列表如下：

| 名称 | 标准 | 额外信息 |
|------|------|----------|
| Route | 使用道路或铁路将城邦连接到主要文明的首都 | |
| Clear Barbarian Camp | 摧毁目标野蛮人营地 | |
| Construct Wonder | 建造目标奇观 | 目标 `wonder` |
| Connect Resource | 将目标资源连接到主要文明的贸易网络 | 目标 `tileResource` |
| Acquire Great Person | 获得目标伟人 | 目标 `greatPerson` |
| Conquer City State | 击败目标城邦 | 目标 `cityState` |
| Find Player | 遇见目标主要文明 | 目标 `civName` |
| Find Natural Wonder | 发现目标自然奇观 | 目标 `naturalWonder` |
| Give Gold | 向城邦捐赠金币（金额不重要） | 城邦的 `civName` "bully" |
| Pledge to Protect | 承诺保护城邦 | 城邦的 `civName` "bully" |
| Contest Culture | 在持续时间内成为文化增长最高的主要文明 | 主要文明的 `cultureGrowth` |
| Contest Faith | 在持续时间内成为信仰增长最高的主要文明 | 主要文明的 `faithGrowth` |
| Contest Technology | 在持续时间内研究科技最多的主要文明 | 主要文明的 `techsResearched` |
| Invest | 捐赠金币根据提供的价值产生额外影响力 | __重要__：方括号中的值是额外影响力的百分比。例如，\[50\] 表示 50% |
| Bully City State | 向目标城邦索取贡品 | 目标 `city state` |
| Denounce Civilization | 谴责"霸凌"城邦的主要文明 | 城邦的 `civName` "bully" |
| Spread Religion | 将主要文明的宗教传播到城邦 | 主要文明的 `religionName` |

### 任务权重

"weightForCityStateType"对象确定任务的权重乘数。当城邦发起任务时，初始权重为 1，并根据[城邦类型](#citystatetypesjson)和个性（Friendly、Neutral、Hostile、Irrational）乘以值。然后 AI 根据最终加权值随机选择一个任务。

该对象使用字符串到浮点数将城邦类型和个性映射到城邦的权重乘数。如果未找到首选胜利类型，则默认乘数为 1。

下面的代码是有效的"weightForCityStateType"定义的示例。在这种情况下，友好的军事主义城邦选择此任务的可能性是权重为 1 的任务的 0.4（0.2 × 2）倍。

```json
"weightForCityStateType": {
"Hostile": 2,
"Friendly": 0.2,
"Militaristic": 2
}
```

## Religions.json

[链接到原始文件](https://github.com/yairm210/Unciv/tree/master/android/assets/jsons/Civ%20V%20-%20Gods%20&%20Kings/Religions.json)

这只是一个字符串列表，指定所有预定义的宗教名称。必须存在相应的图标，仅此而已。毕竟，它们只是[信仰](#beliefsjson)的容器。

## Specialists.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/Specialists.json)

此文件应包含公民可以分配的所有可能专家的列表。

每个专家具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| [`<stats>`](地图相关JSON文件.md#一般统计) | Float | 0 | 专家产生的每回合收益 |
| color | [List of 3× Integer](其他JSON文件.md#rgb-颜色列表) | 必需 | 此专家图像的颜色 |
| greatPersonPoints | Object | none | 此专家每回合产生的伟人点数。有效键是单位名称（Great Scientist、Warrior 等），有效值是整数 |

## Techs.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Gods%20%26%20Kings/Techs.json)

此文件包含所有可以用科学研究的科技。它组织为外部'列'列表，其中每列包含一个或多个科技。

### 列结构

每个科技列具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| columnNumber | Integer | 必需 | 科技树中的水平放置 |
| era | String | 必需 | 确定研究此列中任何科技后达到的时代。必须在 [Eras.json](其他JSON文件.md#erasjson) 中 |
| techCost | Integer | 0 | 此列中科技的默认成本 |
| buildingCost | Integer | 必需 | 需要此科技的建筑的默认成本 |
| wonderCost | Integer | 必需 | 需要此科技的奇观的默认成本 |
| techs | List | 必需 | [科技](#科技结构)列表 - 注意 {} 和 [] 的嵌套 |

#### 科技结构

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| row | Integer | 0 | 科技树中的垂直放置，每列必须唯一 |
| cost | Integer | [列 techCost](#列结构) | 研究此科技所需的科学数量 |
| prerequisites | List of Strings | 空 | 此科技的先决科技名称列表。仅需要直接先决科技 |
| quote | String | none | 玩家研究此科技时呈现的一个好故事 |
| uniques | List of Strings | 空 | 此科技授予的[独特能力](../Unique能力列表.md)列表 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |
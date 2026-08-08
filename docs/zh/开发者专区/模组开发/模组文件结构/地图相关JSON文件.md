---
title: 地图相关 JSON 文件
---

# 地图相关 JSON 文件

## Terrains.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Vanilla/Terrains.json)

此文件包含可以出现在地图上的基础地形、地形特征和自然奇观。

每个地形条目具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | [^A] |
| type | Enum | 必需 | Land、Water、TerrainFeature、NaturalWonder [^B] |
| occursOn | List of Strings | none | 仅适用于地形特征和自然奇观：可以放置在其上的基础地形 |
| turnsInto | String | none | 仅适用于自然奇观：可选的强制性基础地形 [^C] |
| weight | Integer | 10 | 仅适用于自然奇观：被地图生成器选中的_相对_权重 |
| [`<stats>`](#一般统计) | Float | 0 | 地块的每回合收益或加成收益 |
| overrideStats | Boolean | false | 如果为 true，则特征的收益替换底层地形的任何收益，而不是添加到它 |
| unbuildable | Boolean | false | 如果为 true，则无法在此处建造任何东西 - 甚至无法建造资源改良设施 |
| impassable | Boolean | false | 没有单位可以进入，除非它具有特殊的独特能力 |
| movementCost | Integer | 1 | 基础移动成本 |
| defenceBonus | Float | 0 | 在此被攻击单位的战斗加成 |
| RGB | [List of 3× Integer](其他JSON文件.md#rgb-颜色列表) | Gold | 'Default'地形集显示的 RGB 颜色 |
| uniques | List of Strings | 空 | 此地形具有的[独特能力](../Unique能力列表.md)列表 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

[^A]: 某些名称具有特殊含义。`Grassland` 在某些情况下用作回退 - 例如，文明百科更喜欢在其上显示 TerrainFeature，除非 `occursOn` 不为空且不包含它。
      `River` 被硬编码用于查找 [Stats](../Unique能力列表.md#global-uniques) 独特能力来确定实际河流提供的加成（记住，河流存在于边缘而不是地形上）。
      River 应该始终是 TerrainFeature 并且具有与 vanilla 规则集中的那个相同的 uniques - 如果你更改它，请期待惊喜。
[^B]: 基础规则集模组始终期望至少提供一个 Land 地形和至少一个 Water 地形。我们不支持仅 Land 或仅 Water 的模组，即使它们可能是可能的。
[^C]: 如果设置，则在放置自然奇观后，基础地形更改为此，并且地形特征被清除。否则，地形特征减少为仅存在于 occursOn 中的那些。

## TileImprovements.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Vanilla/TileImprovements.json)

此文件列出了具有相应独特能力的单位可以在地图地块上建造或创建的改良设施。

请注意，改良设施有两种视觉表示 - 图标和地形集中的像素图形。省略图标会导致非常丑陋的用户界面，而省略地形集图形只会错过一个_可选_的可视化。如果你为 FantasyHex 提供像素图形，请注意分层系统和地形集 json 中的 ruleVariants。如果单个图形具有大量透明度，则单个图形可能就足够了，因为它将绘制在所有其他地形元素之上。

每个改良设施具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | [^A] |
| terrainsCanBeBuiltOn | List of Strings | 空 | 可以建造此改良设施的地形 [^B]。可移除的地形特征需要在建造改良设施之前移除 [^C]。必须在 [Terrains.json](#terrainsjson) 中 |
| techRequired | String | none | 建造此改良设施所需的技术名称 |
| replaces | String | none | 应被此改良设施替换的改良设施的名称。必须在 [TileImprovements.json](#tileimprovementsjson) 中 |
| uniqueTo | String | none | 此改良设施唯一的国家的名称 |
| [`<stats>`](#统计数据) | Integer | 0 | 地块的每回合加成收益 |
| turnsToBuild | Integer | -1 | 工人建造此改良设施花费的回合数。如果为 -1，则改良设施不可建造 [^D]。如果为 0，则改良设施始终在一个回合内建造 |
| uniques | List of Strings | 空 | 此改良设施具有的[独特能力](../Unique能力列表.md)列表 |
| shortcutKey | String | none | 键盘绑定。目前，只允许单个字符（没有功能键或 Ctrl 组合） |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

[^A]: 特殊改良设施：Road、Railroad、Remove \*、Cancel improvement order、City ruins、City center、Barbarian encampment - 这些名称具有硬编码的特殊含义。
[^B]: 具有空 `terrainsCanBeBuiltOn` 列表和正 `turnsToBuild` 值的改良设施只能建造在具有 `improvedBy` 或包含相应改良设施的 `improvement` 的[资源](#tileresourcesjson)上。
[^C]: 如果地形特征在 `terrainsCanBeBuiltOn` 中命名_或_使用了独特能力 `Does not need removal of [tileFilter]`（例如，资源允许的 Camp），则地形特征的移除是可选的。
[^D]: 它们仍然可以使用 UnitAction 独特能力 `Can instantly construct a [improvementFilter] improvement` 创建。

## TileResources.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Vanilla/TileResources.json)

此文件列出了地图地块可以具有的资源。

请注意，预定义的 `resourceType` 枚举不能在 json 中更改。

还要注意，资源有两种视觉表示 - 图标和地形集中的像素图形。省略图标会导致非常丑陋的用户界面，而省略地形集图形会错过地图上的可视化。如果你为 FantasyHex 提供像素图形，请注意分层系统和地形集 json 中的 ruleVariants。如果单个图形具有大量透明度，则单个图形可能就足够了，因为它将绘制在地形和特征之上但在改良设施之下 - 如果单个改良设施图形存在的话。

每个资源具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | |
| resourceType | Enum | Bonus | Bonus、Luxury 或 Strategic |
| terrainsCanBeFoundOn | List of Strings | 空 | 可以找到此资源的地形。必须在 [Terrains.json](#terrainsjson) 中 |
| [`<stats>`](#统计数据) | Integer | 0 | 地块的每回合加成收益 |
| improvementStats | Object | none | 改良时的额外收益，请参阅[专业统计数据](地图相关JSON文件.md#专业统计数据) |
| revealedBy | String | none | 查看、工作和改进此资源所需的技术名称 |
| improvedBy | List of strings | 空 | 获得此资源所需的改良设施。必须在 [TileImprovements.json](#tileimprovementsjson) 中 |
| improvement | String | none | 获得此资源所需的改良设施。必须在 [TileImprovements.json](#tileimprovementsjson) 中（由于 `improvedBy` 而冗余） |
| unique | List of Strings | 空 | 此资源具有的[独特能力](../Unique能力列表.md)列表 |
| civilopediaText | List | 空 | 请参阅 [civilopediaText 章节](其他JSON文件.md#civilopedia-text) |

## Ruins.json

[链接到原始文件](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/Civ%20V%20-%20Vanilla/Ruins.json)

此可选文件包含古代遗迹可能给予的奖励。

基础规则集模组可以省略文件，在这种情况下，它们从 Vanilla 规则集继承它们。但是，它们可以提供带有空列表（`[]`）的文件以避免这种情况。在这种情况下，不应该有具有 `Provides a random bonus when entered` Unique 的改良设施。相反，如果有这样的改良设施，模组检查器会将空的 Ruins 文件标记为错误。

文件中的每个对象代表你可以从遗迹获得的单个奖励。它具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | 必需 | 遗迹的名称。从不向用户显示，但它们必须区分开 |
| notification | String | 必需 | 选择此奖励时添加到用户的通知。如果省略，则显示空通知。某些通知可能具有参数，请参阅下表。 |
| weight | Integer (≥0) | 1 | 下次选择此奖励的_相对_权重 [^E] |
| uniques | List of Strings | 空 | 进入遗迹时将触发的[独特能力](../Unique能力列表.md)列表。如果添加了超过 1 个 unique，由于错误（可能已过时），通知将显示多次 |
| excludedDifficulties | List of Strings | 空 | 可能_不会_授予此奖励的所有难度列表 |

[^E]: <span>选择奖励的确切算法如下：</span>

- 创建所有可能奖励的列表。每个奖励在列表中的频率对应于其权重，权重为 1 的奖励将出现一次，权重为 2 的奖励将出现两次，依此类推。
- 随机排列此列表
- 尝试从列表顶部开始给予奖励。如果在此上下文中任何奖励的 uniques 有效，则给予它并停止尝试更多奖励。

### 通知

遗迹可以给予的一些奖励将具有在 JSON 中编写时不确定的结果，因此为此创建一个好的通知是不可能的。一个例子是 "Gain [50]-[100] [Gold]" unique，它将给予随机数量的金币。出于这个原因，我们允许某些通知具有参数，其中将填充值，例如 "You found [goldAmount] gold in the ruins!"。所有具有此属性的 unique 都可以在下面找到。

| Unique | 参数 |
|--------|------|
| `Free [] found in the ruins` | 单位的名称将填充到通知中，包括该国家的独特单位 |
| `[] population in a random city` | 人口添加到的城市的名称将填充到通知中 |
| `Gain []-[] []` | 获得的统计数据的确切数量将填充到通知中 |
| `[] free random reasearchable Tech(s) from the []` | 通知必须具有等于以这种方式授予的科技数量的占位符。这些免费科技中的每一个名称都将填充到通知中 |
| `Gain enough Faith for a Pantheon` | 获得的信仰数量填充到通知中 |
| `Gain enough Faith for []% of a Great Prophet` | 获得的信仰数量填充到通知中 |

### 特定的 uniques

可以添加几个独特能力到古代遗迹效果中以修改何时可以赚取它们。这些是：

- `Only available after [amount] turns`
- `Only available <when religion is enabled>`
- `Hidden after a great prophet has been earned`

## [地形集特定 json](../自定义地形集.md)

[链接到原始 FantasyHex](https://github.com/yairm210/Unciv/blob/master/android/assets/jsons/TileSets/FantasyHex.json)

模组可以定义新的地形集或添加到现有的地形集，即 FantasyHex。每个地形集有一个 json 文件，命名为与地形集相同，并放置在相对于其他 json 文件的名为 "TileSets" 的子文件夹中。这称为 TileSetConfig，具有以下结构：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| [useColorAsBaseTerrain](../自定义地形集.md#usecolorasbaseterrain) | Boolean | false | |
| [useSummaryImages](../自定义地形集.md#usesummaryimages) | Boolean | false | |
| [unexploredTileColor](../自定义地形集.md#unexploredtilecolor) | Color | Dark Gray | `{"r":0.25,"g":0.25,"b":0.25,"a":1}` |
| [fogOfWarColor](../自定义地形集.md#fogofwarcolor) | Color | Black | `{"r":0,"g":0,"b":0,"a":1}` |
| [fallbackTileSet](../自定义地形集.md#fallbacktileset) | String | "FantasyHex" | null 以禁用 |
| [tileScale](../自定义地形集.md#tilescale) | Float | 1.0 | 所有地块的比例。可用于增加或减少每个地块的大小 |
| [tileScales](../自定义地形集.md#tilescales) | Object | empty | 被 "Minimal" 地形集使用，将其所有地块（基础地形除外）缩小。覆盖指定地形的 `tileScale` 值 |
| [ruleVariants](../自定义地形集.md#rulevariants) | Object | empty | [请参阅此处](#分层图像) |

### 分层图像

ruleVariants 控制为地块分层图像时的替换，它们看起来像这样的列表：

```json
"ruleVariants": {
    "Grassland+Forest": ["Grassland", "GrasslandForest"],
    "Plains+Forest": ["Plains", "PlainsForest"],
    "Plains+Jungle": ["Plains", "PlainsJungle"],
    // . . .
}
```

每行意味着"如果地块内容是这个...然后组合以下 png 图像"。关键部分遵循特定顺序并且必须完全匹配，意味着 "Plains+Forest" 对 "Plains+Forest+Deer" 无效，当它匹配时，除了道路和单位之外不进行其他图像分层（我想 - _WIP_）。

当为同一地形集组合 TileSetConfig 时，对于前三个属性，最后一个模组获胜，而 ruleVariants 被合并，意味着只有具有相同键的条目才会覆盖较早的条目。（TODO）

## 统计数据

地形、特征、资源和改良设施可以列出收益统计数据。统计数据可以是以下之一：

- production
- food
- gold
- science
- culture
- happiness
- faith

### 一般统计

如果对象携带一般统计数据，它包含上述统计数据的任何组合（或没有），每个映射到相应的数字 [^1]。例如：

```json
"gold": 2,
"improvement": "Quarry",
```

### 专业统计数据

对于专业统计数据，它们可能作为命名字段中的子对象出现。子对象包含上述统计数据的任何组合（或没有），每个映射到相应的数字 [^1]。例如：

```json
"improvement": "Quarry",
"improvementStats": { "gold": 1, "production": 1 },
```

[^1]: 值通常是整数，尽管底层代码支持浮点数。但是，效果测试不足，因此 -到目前为止- 使用分数统计数据不受支持。继续在模组中彻底测试它并通过反馈提供帮助 😁。
# 区域（Regions）

## 概念

在生成随机地图时（仅限随机地图；不包括预置地图），地图会被划分为与主要文明数量相等的多个区域。每个区域根据其主流地形分类，无法分类的称为"混合"（hybrid）区域。
区域类型与文明分配时的起始偏好（start bias）相对应。
区域类型还决定起始位置以及该区域会出现哪些奢侈品。

<details>
    <summary>示例</summary>
    <img src="https://user-images.githubusercontent.com/63475501/140308518-ad5a2f50-d5f1-4467-a296-3a67f6d0b007.png" alt="区域示例" />
</details>

## 如何在模组中定义区域行为

游戏在没有额外 json 定义的情况下也能工作，但如果你希望区域系统在为你的模组生成地图时良好运作，就需要定义这些相关的 unique。

### Terrains.json

"Always Fertility [amount] for Map Generation"（地图生成时始终为 [amount] 肥沃度）、"[amount] to Fertility for Map Generation"（地图生成时肥沃度 +[amount]）——决定一种地形在公平划分土地方面的优劣。数值是任意的，但应反映各地形的相对价值。

"A Region is formed with at least [amount]% [simpleTerrain] tiles, with priority [amount]"（至少含 [amount]% [simpleTerrain] 地块的区域形成，优先级 [amount]）、
"A Region is formed with at least [amount]% [simpleTerrain] tiles and [simpleTerrain] tiles, with priority [amount]"（至少含 [amount]% [simpleTerrain] 地块和 [simpleTerrain] 地块的区域形成，优先级 [amount]）——决定一个区域何时被归类为例如"沙漠"区域的规则。地形按优先级升序评估，所以基础规则集中冻土地带优先被检查。
"A Region can not contain more [simpleTerrain] tiles than [simpleTerrain] tiles"（区域中的 [simpleTerrain] 地块不能多于 [simpleTerrain] 地块）——对上面"双地形之和"标准的实用补充，适用于两种地形本身都是地形类型的情况。所以基础规则集中，丛林与森林之和足够大时区域可被归类为丛林，但前提是丛林要多于森林。
"Base Terrain on this tile is not counted for Region determination"（此地块的基础地形不计入区域判定）——用于不可移除或以其他方式主导地块的地形特征。基础规则集中用于丘陵。
不满足任何标准的区域被归类为"混合"。

"Considered [terrainQuality] when determining start locations"（确定起始位置时视为 [terrainQuality]）——其中 "terrainQuality" 是 "Food"（食物）、"Production"（产能）、"Desirable"（理想）、"Undesirable"（不理想）之一。通常与 "`<in [regionType] Regions>`"（位于 [regionType] 区域）或 "`<in all except [regionType] Regions>`"（位于除 [regionType] 外的所有区域）配合使用，决定确定起始位置时什么地形有吸引力。注意：如果某地形没有这些定义，游戏会用该地形的基础数值来推断质量；但只要有任何定义，游戏就会假定它们是完整的。

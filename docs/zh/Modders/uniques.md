---
title: Unique 能力列表
---

<!-- 本文件由 desktop/src/com/unciv/app/desktop/UniqueDocsWriter.kt 自动生成，请勿手动编辑 -->

# Unique 能力列表

> 本列表由游戏代码自动生成，随版本保持最新。
> Uniques 概述可以在[这里](../Developers/Uniques.md)找到。
> 简单的 Unique 参数见文末参数表，复杂的参数在 [Unique 参数类型](Unique-parameters.md) 中说明。

## Triggerable uniques（触发型词条）
::: note

具有即时、一次性效果的词条。可添加到科技（研究时触发）、政策（采用时触发）、时代（到达时触发）、建筑（建造时触发）；或为其添加触发条件（TriggerCondition），使其成为在特定事件时激活的全局词条。也可添加到单位，赋予其将效果作为行动触发的能力（可用 UnitActionModifier / UnitTriggerCondition 条件修饰）。
:::

::: details Gain a free [buildingName] [cityFilter]
免费建筑不能自我移除——这会导致尝试添加建筑的无限循环

示例："Gain a free [Library] [in all cities]"

适用范围：Triggerable，Global

:::
::: details Remove [buildingFilter] [cityFilter]
示例："Remove [Culture] [in all cities]"

适用范围：Triggerable，Global

:::
::: details Sell [buildingFilter] buildings [cityFilter]
示例："Sell [Culture] buildings [in all cities]"

适用范围：Triggerable，Global

:::
::: details Free [unit] appears
示例："Free [Musketman] appears"

适用范围：Triggerable

:::
::: details [positiveAmount] free [unit] units appear
示例："[3] free [Musketman] units appear"

适用范围：Triggerable

:::
::: details A [unit] rebels
示例："A [Musketman] rebels"

适用范围：Triggerable

:::
::: details [positiveAmount] [unit]s rebel
示例："[3] [Musketman]s rebel"

适用范围：Triggerable

:::
::: details Free Social Policy
适用范围：Triggerable

:::
::: details [positiveAmount] Free Social Policies
示例："[3] Free Social Policies"

适用范围：Triggerable

:::
::: details Empire enters golden age
适用范围：Triggerable

:::
::: details Empire enters a [positiveAmount]-turn Golden Age
示例："Empire enters a [3]-turn Golden Age"

适用范围：Triggerable

:::
::: details Free Great Person
适用范围：Triggerable

:::
::: details [amount] population [cityFilter]
示例："[3] population [in all cities]"

适用范围：Triggerable

:::
::: details [amount] population in a random city
示例："[3] population in a random city"

适用范围：Triggerable

:::
::: details Discover [tech]
示例："Discover [Agriculture]"

适用范围：Triggerable

:::
::: details Adopt [policy/belief]
示例："Adopt [Oligarchy]"

适用范围：Triggerable

:::
::: details Remove [policyFilter]
示例："Remove [Oligarchy]"

适用范围：Triggerable

:::
::: details Remove [policyFilter] and refund [amount]% of its cost
示例："Remove [Oligarchy] and refund [3]% of its cost"

适用范围：Triggerable

:::
::: details Free Technology
适用范围：Triggerable

:::
::: details [positiveAmount] Free Technologies
示例："[3] Free Technologies"

适用范围：Triggerable

:::
::: details [positiveAmount] free random researchable Tech(s) from the [eraFilter]
示例："[3] free random researchable Tech(s) from the [Ancient era]"

适用范围：Triggerable

:::
::: details Reveals the entire map
适用范围：Triggerable

:::
::: details Gain a free [beliefType] belief
示例："Gain a free [Follower] belief"

适用范围：Triggerable

:::
::: details Triggers voting for the Diplomatic Victory
适用范围：Triggerable

:::
::: details Instantly consumes [positiveAmount] [stockpiledResource]
示例："Instantly consumes [3] [Mana]"

适用范围：Triggerable

:::
::: details Instantly provides [positiveAmount] [stockpiledResource]
示例："Instantly provides [3] [Mana]"

适用范围：Triggerable

:::
::: details Set [stockpile] to [countable]
示例："Set [Mana] to [1000]"

此词条的效果可被 &lt;(modified by game speed)&gt;

适用范围：Triggerable

:::
::: details Instantly gain [amount] [stockpile]
示例："Instantly gain [3] [Mana]"

此词条的效果可被 &lt;(modified by game speed)&gt;

适用范围：Triggerable

:::
::: details Gain [amount] [civWideStat]
示例："Gain [3] [Gold]"

此词条的效果可被 &lt;(modified by game speed)&gt;

适用范围：Triggerable

:::
::: details Gain [amount]-[amount2] [civWideStat]
示例："Gain [3]-[3] [Gold]"

此词条的效果可被 &lt;(modified by game speed)&gt;

适用范围：Triggerable

:::
::: details Gain enough Faith for a Pantheon
适用范围：Triggerable

:::
::: details Gain enough Faith for [positiveAmount]% of a Great Prophet
示例："Gain enough Faith for [3]% of a Great Prophet"

适用范围：Triggerable

:::
::: details Research [relativeAmount]% of [tech]
示例："Research [+20]% of [Agriculture]"

适用范围：Triggerable

:::
::: details Gain control over [tileFilter] tiles in a [nonNegativeAmount]-tile radius
示例："Gain control over [Farm] tiles in a [3]-tile radius"

适用范围：Triggerable

:::
::: details Gain control over [positiveAmount] tiles [cityFilter]
示例："Gain control over [3] tiles [in all cities]"

适用范围：Triggerable

:::
::: details Reveal up to [positiveAmount/'all'] [tileFilter] within a [positiveAmount] tile radius
示例："Reveal up to [3] [Farm] within a [3] tile radius"

适用范围：Triggerable

:::
::: details Triggers the following global alert: [comment]
支持用于政策和科技。

对其他目标，生成的通知可能读起来不顺畅，且很可能不支持翻译。原因：你的 [comment] 会得到自动生成的引言，其他触发器通常通知_你_，而非_他人_，这一差异目前通过文本映射处理。

条件在拥有该 unique 的文明上下文中求值，而非通知接收者。

示例："Triggers the following global alert: [comment]"

适用范围：Triggerable

:::
::: details Promotes all spies [positiveAmount] time(s)
示例："Promotes all spies [3] time(s)"

适用范围：Triggerable

:::
::: details Gain an extra spy
适用范围：Triggerable

:::
::: details Turn this tile into a [terrainName] tile
示例："Turn this tile into a [Forest] tile"

适用范围：Triggerable

:::
::: details Add [resource] to this tile
示例："Add [Iron] to this tile"

适用范围：Triggerable

:::
::: details Remove [resourceFilter] resources from this tile
示例："Remove [Strategic] resources from this tile"

适用范围：Triggerable

:::
::: details Remove [improvementFilter] improvements from this tile
示例："Remove [All Road] improvements from this tile"

适用范围：Triggerable

:::
::: details [mapUnitFilter] units gain the [promotion] promotion
仅适用于对该单位类型有效的晋升——或未指定单位类型的晋升。

示例："[Wounded] units gain the [Shock I] promotion"

适用范围：Triggerable

:::
::: details Provides the cheapest [stat] building in your first [positiveAmount] cities for free
示例："Provides the cheapest [Culture] building in your first [3] cities for free"

适用范围：Triggerable

:::
::: details Provides a [buildingName] in your first [positiveAmount] cities for free
示例："Provides a [Library] in your first [3] cities for free"

适用范围：Triggerable

:::
::: details Triggers a [event] event
示例："Triggers a [Inspiration] event"

适用范围：Triggerable

:::
::: details Trigger the function [luaFunction] with [comment]
示例："Trigger the function [myMod:myFunction] with [comment]"

适用范围：Triggerable

:::
::: details Mark tutorial [comment] complete
示例："Mark tutorial [comment] complete"

此词条不支持条件。

此词条自动对用户隐藏。

适用范围：Triggerable

:::
::: details Play [comment] sound
可用声音列表见 [图像和音频](/zh/Modders/Images-and-Audio#声音)。

示例："Play [comment] sound"

此词条自动对用户隐藏。

适用范围：Triggerable

:::
::: details Get the leader title of [leaderTitle]
示例："Get the leader title of [Sovereign [leaderName] the Great]"

此词条自动对用户隐藏。

适用范围：Triggerable

:::
::: details Choose a music track for [param], [param2], [param3]
参数不校验，不匹配现有曲目或标志的字符串会被忽略。

参见[情境音乐](/zh/Modders/Images-and-Audio#上下文敏感的音乐-概述)

第一个参数是曲目名前缀，例如文明名或 "this civ"。

第二个参数是零个或多个后缀的逗号分隔列表，用于指定 "mood"（氛围），如 Peace、War、Ambient 等。第一个匹配的曲目胜出。

第三个参数是零个或多个标志的列表：PrefixMustMatch, SuffixMustMatch, SlowFade, PlaySingle, PlayDefaultFile.

示例："Choose a music track for [Unknown], [Unknown], [Unknown]"

此词条自动对用户隐藏。

适用范围：Triggerable

:::
::: details Suppress warning [validationWarning]
Allows suppressing specific validation warnings. Errors, deprecation warnings, or warnings about untyped and non-filtering uniques should be heeded, not suppressed, and are therefore not accepted. Note that this can be used in ModOptions, in the uniques a warning is about, or as modifier on the unique triggering a warning - but you still need to be specific. Even in the modifier case you will need to specify a sufficiently selective portion of the warning text as parameter.

示例："Suppress warning [Tinman is supposed to automatically upgrade at tech Clockwork, and therefore Servos for its upgrade Mecha may not yet be researched! -or- *is supposed to automatically upgrade*]"

此词条不支持条件。

此词条自动对用户隐藏。

适用范围：Triggerable，Terrain，Speed，ModOptions，MetaModifier

:::
## UnitTriggerable uniques（单位触发型词条）
::: note

对单位产生即时、一次性效果的词条。可添加到单位（单位、单位类型或晋升），赋予其将效果作为行动触发的能力（可用 UnitActionModifier / UnitTriggerCondition 条件修饰）。
:::

::: details [unitTriggerTarget] heals [positiveAmount] HP
示例："[This Unit] heals [3] HP"

适用范围：UnitTriggerable

:::
::: details [unitTriggerTarget] takes [positiveAmount] damage
示例："[This Unit] takes [3] damage"

适用范围：UnitTriggerable

:::
::: details [unitTriggerTarget] gains [amount] XP
示例："[This Unit] gains [3] XP"

适用范围：UnitTriggerable

:::
::: details [unitTriggerTarget] upgrades for free
示例："[This Unit] upgrades for free"

适用范围：UnitTriggerable

:::
::: details [unitTriggerTarget] upgrades for free including special upgrades
示例："[This Unit] upgrades for free including special upgrades"

适用范围：UnitTriggerable

:::
::: details [unitTriggerTarget] gains the [promotion] promotion
示例："[This Unit] gains the [Shock I] promotion"

适用范围：UnitTriggerable

:::
::: details [unitTriggerTarget] loses the [promotion] promotion
示例："[This Unit] loses the [Shock I] promotion"

适用范围：UnitTriggerable

:::
::: details [unitTriggerTarget] gains [positiveAmount] movement
示例："[This Unit] gains [3] movement"

适用范围：UnitTriggerable

:::
::: details [unitTriggerTarget] loses [positiveAmount] movement
示例："[This Unit] loses [3] movement"

适用范围：UnitTriggerable

:::
::: details [unitTriggerTarget] gains the [promotion] status for [positiveAmount] turn(s)
状态是临时晋升。它们不叠加，重新应用特定状态会取最大值——在 1 回合状态上重新应用 3 回合状态会变成 3，但反过来则无效。状态的剩余回合数在*回合开始时*减少，所以持续 1 回合的加成在其他文明的回合中仍然生效。

示例："[This Unit] gains the [Shock I] status for [3] turn(s)"

适用范围：UnitTriggerable

:::
::: details [unitTriggerTarget] loses the [promotion] status
示例："[This Unit] loses the [Shock I] status"

适用范围：UnitTriggerable

:::
::: details [unitTriggerTarget] is destroyed
示例："[This Unit] is destroyed"

适用范围：UnitTriggerable

:::
::: details [unitTriggerTarget] gets a name from the [unitNameGroup] group
示例："[This Unit] gets a name from the [Scientist] group"

适用范围：UnitTriggerable

:::
## Global uniques（全球词条）
::: note

全局生效的词条。文明从国家词条、已到达的时代、已研究的科技、已采用的政策、已建造的建筑、宗教「创始人」词条、拥有的资源以及规则集全局词条中获得这些能力。
:::

::: details [stats]
示例："[+1 Gold, +2 Production]"

适用范围：Global，Terrain，Improvement

:::
::: details [stats] [cityFilter]
示例："[+1 Gold, +2 Production] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [stats] from every specialist [cityFilter]
示例："[+1 Gold, +2 Production] from every specialist [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [stats] per [positiveAmount] population [cityFilter]
示例："[+1 Gold, +2 Production] per [3] population [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [stats] per [positiveAmount] social policies adopted
仅对全文明范围的产出生效

示例："[+1 Gold, +2 Production] per [3] social policies adopted"

适用范围：Global

:::
::: details [stats] per every [positiveAmount] [civWideStat]
示例："[+1 Gold, +2 Production] per every [3] [Gold]"

适用范围：Global

:::
::: details [stats] in cities on [terrainFilter] tiles
示例："[+1 Gold, +2 Production] in cities on [Fresh Water] tiles"

适用范围：Global，FollowerBelief

:::
::: details [stats] from all [buildingFilter] buildings
示例："[+1 Gold, +2 Production] from all [Culture] buildings"

适用范围：Global，FollowerBelief

:::
::: details [stats] from [tileFilter] tiles [cityFilter]
示例："[+1 Gold, +2 Production] from [Farm] tiles [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [stats] from [tileFilter] tiles without [tileFilter2] [cityFilter]
示例："[+1 Gold, +2 Production] from [Farm] tiles without [Farm] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [stats] from every [tileFilter/specialist/buildingFilter]
示例："[+1 Gold, +2 Production] from every [Farm]"

适用范围：Global，FollowerBelief

:::
::: details [stats] from each Trade Route
示例："[+1 Gold, +2 Production] from each Trade Route"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% [stat]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% [Culture]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% [stat] [cityFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% [Culture] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% [stat] from every [tileFilter/buildingFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% [Culture] from every [Farm]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Yield from every [tileFilter/buildingFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% Yield from every [Farm]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% [stat] from City-States
示例："[+20]% [Culture] from City-States"

适用范围：Global

:::
::: details [relativeAmount]% [stat] from Trade Routes
示例："[+20]% [Culture] from Trade Routes"

适用范围：Global

:::
::: details Nullifies [stat] [cityFilter]
示例："Nullifies [Culture] [in all cities]"

适用范围：Global

:::
::: details Nullifies Growth [cityFilter]
示例："Nullifies Growth [in all cities]"

适用范围：Global

:::
::: details [relativeAmount]% Production when constructing [buildingFilter] buildings [cityFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% Production when constructing [Culture] buildings [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Production when constructing [baseUnitFilter] units [cityFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% Production when constructing [Melee] units [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Production when constructing [buildingFilter] wonders [cityFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% Production when constructing [Culture] wonders [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Production towards any buildings that already exist in the Capital
示例："[+20]% Production towards any buildings that already exist in the Capital"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Yield from pillaging tiles
示例："[+20]% Yield from pillaging tiles"

适用范围：Global，Unit

:::
::: details [relativeAmount]% Health from pillaging tiles
示例："[+20]% Health from pillaging tiles"

适用范围：Global，Unit

:::
::: details Military Units gifted from City-States start with [positiveAmount] XP
示例："Military Units gifted from City-States start with [3] XP"

适用范围：Global

:::
::: details Militaristic City-States grant units [positiveAmount] times as fast when you are at war with a common nation
示例："Militaristic City-States grant units [3] times as fast when you are at war with a common nation"

适用范围：Global

:::
::: details Gifts of Gold to City-States generate [relativeAmount]% more Influence
示例："Gifts of Gold to City-States generate [+20]% more Influence"

适用范围：Global

:::
::: details Can spend Gold to annex or puppet a City-State that has been your Ally for [nonNegativeAmount] turns
示例："Can spend Gold to annex or puppet a City-State that has been your Ally for [3] turns"

适用范围：Global

:::
::: details City-State territory always counts as friendly territory
适用范围：Global

:::
::: details Allied City-States will occasionally gift Great People
适用范围：Global

:::
::: details [relativeAmount]% City-State Influence degradation
示例："[+20]% City-State Influence degradation"

适用范围：Global

:::
::: details Resting point for Influence with City-States is increased by [amount]
示例："Resting point for Influence with City-States is increased by [3]"

适用范围：Global

:::
::: details Allied City-States provide [stat] equal to [relativeAmount]% of what they produce for themselves
示例："Allied City-States provide [Culture] equal to [+20]% of what they produce for themselves"

适用范围：Global

:::
::: details [relativeAmount]% resources gifted by City-States
示例："[+20]% resources gifted by City-States"

适用范围：Global

:::
::: details [relativeAmount]% Happiness from luxury resources gifted by City-States
示例："[+20]% Happiness from luxury resources gifted by City-States"

适用范围：Global

:::
::: details City-State Influence recovers at twice the normal rate
适用范围：Global

:::
::: details [relativeAmount]% growth [cityFilter]
示例："[+20]% growth [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [amount]% Food is carried over after population increases [cityFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[3]% Food is carried over after population increases [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Food consumption by [populationFilter] [cityFilter]
示例："[+20]% Food consumption by [Followers of this Religion] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% unhappiness from the number of cities
示例："[+20]% unhappiness from the number of cities"

适用范围：Global

:::
::: details [relativeAmount]% Unhappiness from [populationFilter] [cityFilter]
示例："[+20]% Unhappiness from [Followers of this Religion] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [amount] Happiness from each type of luxury resource
示例："[3] Happiness from each type of luxury resource"

适用范围：Global

:::
::: details Retain [relativeAmount]% of the happiness from a luxury after the last copy has been traded away
示例："Retain [+20]% of the happiness from a luxury after the last copy has been traded away"

适用范围：Global

:::
::: details [relativeAmount]% of excess happiness converted to [stat]
示例："[+20]% of excess happiness converted to [Culture]"

适用范围：Global

:::
::: details Cannot build [baseUnitFilter] units
示例："Cannot build [Melee] units"

适用范围：Global

:::
::: details Enables construction of Spaceship parts
适用范围：Global

:::
::: details May buy [baseUnitFilter] units for [nonNegativeAmount] [stat] [cityFilter] at an increasing price ([amount])
示例："May buy [Melee] units for [3] [Culture] [in all cities] at an increasing price ([3])"

适用范围：Global，FollowerBelief

:::
::: details May buy [buildingFilter] buildings for [nonNegativeAmount] [stat] [cityFilter] at an increasing price ([amount])
示例："May buy [Culture] buildings for [3] [Culture] [in all cities] at an increasing price ([3])"

适用范围：Global，FollowerBelief

:::
::: details May buy [baseUnitFilter] units for [nonNegativeAmount] [stat] [cityFilter]
示例："May buy [Melee] units for [3] [Culture] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details May buy [buildingFilter] buildings for [nonNegativeAmount] [stat] [cityFilter]
示例："May buy [Culture] buildings for [3] [Culture] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details May buy [baseUnitFilter] units with [stat] [cityFilter]
示例："May buy [Melee] units with [Culture] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details May buy [buildingFilter] buildings with [stat] [cityFilter]
示例："May buy [Culture] buildings with [Culture] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details May buy [baseUnitFilter] units with [stat] for [nonNegativeAmount] times their normal Production cost
示例："May buy [Melee] units with [Culture] for [3] times their normal Production cost"

适用范围：Global，FollowerBelief

:::
::: details May buy [buildingFilter] buildings with [stat] for [nonNegativeAmount] times their normal Production cost
示例："May buy [Culture] buildings with [Culture] for [3] times their normal Production cost"

适用范围：Global，FollowerBelief

:::
::: details [stat] cost of purchasing items in cities [relativeAmount]%
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[Culture] cost of purchasing items in cities [+20]%"

适用范围：Global，FollowerBelief

:::
::: details [stat] cost of purchasing [buildingFilter] buildings [relativeAmount]%
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[Culture] cost of purchasing [Culture] buildings [+20]%"

适用范围：Global，FollowerBelief

:::
::: details [stat] cost of purchasing [baseUnitFilter] units [relativeAmount]%
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[Culture] cost of purchasing [Melee] units [+20]%"

适用范围：Global，FollowerBelief

:::
::: details Enables conversion of city production to [stat]
示例："Enables conversion of city production to [Culture]"

适用范围：Global

:::
::: details Production to [stat] conversion in cities changed by [relativeAmount]%
示例："Production to [Culture] conversion in cities changed by [+20]%"

适用范围：Global

:::
::: details Improves movement speed on roads
适用范围：Global

:::
::: details Roads connect tiles across rivers
适用范围：Global

:::
::: details [relativeAmount]% maintenance on road & railroads
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% maintenance on road & railroads"

适用范围：Global

:::
::: details No Maintenance costs for improvements in [tileFilter] tiles
示例："No Maintenance costs for improvements in [Farm] tiles"

适用范围：Global

:::
::: details [relativeAmount]% construction time for [improvementFilter] improvements
示例："[+20]% construction time for [All Road] improvements"

适用范围：Global，Unit

:::
::: details Can build [improvementFilter] improvements at a [relativeAmount]% rate
示例："Can build [All Road] improvements at a [+20]% rate"

适用范围：Global，Unit

:::
::: details Gain a free [buildingName] [cityFilter]
免费建筑不能自我移除——这会导致尝试添加建筑的无限循环

示例："Gain a free [Library] [in all cities]"

适用范围：Triggerable，Global

:::
::: details [relativeAmount]% maintenance cost for [buildingFilter] buildings [cityFilter]
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% maintenance cost for [Culture] buildings [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details Remove [buildingFilter] [cityFilter]
示例："Remove [Culture] [in all cities]"

适用范围：Triggerable，Global

:::
::: details Sell [buildingFilter] buildings [cityFilter]
示例："Sell [Culture] buildings [in all cities]"

适用范围：Triggerable，Global

:::
::: details [relativeAmount]% Culture cost of natural border growth [cityFilter]
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Culture cost of natural border growth [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Gold cost of acquiring tiles [cityFilter]
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Gold cost of acquiring tiles [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details Each city founded increases culture cost of policies [relativeAmount]% less than normal
示例："Each city founded increases culture cost of policies [+20]% less than normal"

适用范围：Global

:::
::: details [relativeAmount]% Culture cost of adopting new Policies
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Culture cost of adopting new Policies"

适用范围：Global

:::
::: details Each city founded increases Science cost of Technologies [relativeAmount]% less than normal
示例："Each city founded increases Science cost of Technologies [+20]% less than normal"

适用范围：Global

:::
::: details [relativeAmount]% Science cost of researching new Technologies
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Science cost of researching new Technologies"

适用范围：Global

:::
::: details [stats] for every known Natural Wonder
示例："[+1 Gold, +2 Production] for every known Natural Wonder"

适用范围：Global

:::
::: details [stats] for discovering a Natural Wonder (bonus enhanced to [stats2] if first to discover it)
示例："[+1 Gold, +2 Production] for discovering a Natural Wonder (bonus enhanced to [+1 Gold, +2 Production] if first to discover it)"

适用范围：Global

:::
::: details [relativeAmount]% Great Person generation [cityFilter]
示例："[+20]% Great Person generation [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Gold from Great Merchant trade missions
示例："[+20]% Gold from Great Merchant trade missions"

适用范围：Global，Unit

:::
::: details Great General provides double combat bonus
适用范围：Global，Unit

:::
::: details Receive a free Great Person at the end of every [comment] (every 394 years), after researching [tech]. Each bonus person can only be chosen once.
示例："Receive a free Great Person at the end of every [comment] (every 394 years), after researching [Agriculture]. Each bonus person can only be chosen once."

适用范围：Global

:::
::: details Once The Long Count activates, the year on the world screen displays as the traditional Mayan Long Count.
适用范围：Global

:::
::: details [amount] Unit Supply
示例："[3] Unit Supply"

适用范围：Global

:::
::: details [amount] Unit Supply per [positiveAmount] population [cityFilter]
示例："[3] Unit Supply per [3] population [in all cities]"

适用范围：Global

:::
::: details [amount] Unit Supply per city
示例："[3] Unit Supply per city"

适用范围：Global

:::
::: details [amount] units cost no maintenance
示例："[3] units cost no maintenance"

适用范围：Global

:::
::: details Units in cities cost no Maintenance
适用范围：Global

:::
::: details Enables embarkation for land units
由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Global

:::
::: details Enables [mapUnitFilter] units to enter ocean tiles
示例："Enables [Wounded] units to enter ocean tiles"

适用范围：Global

:::
::: details Land units may cross [terrainName] tiles after the first [baseUnitFilter] is earned
示例："Land units may cross [Forest] tiles after the first [Melee] is earned"

由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Global

:::
::: details Enemy [mapUnitFilter] units must spend [positiveAmount] extra movement points when inside your territory
示例："Enemy [Wounded] units must spend [3] extra movement points when inside your territory"

由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Global

:::
::: details New [baseUnitFilter] units start with [amount] XP [cityFilter]
示例："New [Melee] units start with [3] XP [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details All newly-trained [baseUnitFilter] units [cityFilter] receive the [promotion] promotion
示例："All newly-trained [Melee] units [in all cities] receive the [Shock I] promotion"

适用范围：Global，FollowerBelief

:::
::: details [mapUnitFilter] Units adjacent to this city heal [amount] HP per turn when healing
示例："[Wounded] Units adjacent to this city heal [3] HP per turn when healing"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% XP required for promotions
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% XP required for promotions"

适用范围：Global

:::
::: details [relativeAmount]% City Strength from defensive buildings
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% City Strength from defensive buildings"

适用范围：Global

:::
::: details [relativeAmount]% Strength for cities
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% Strength for cities"

适用范围：Global，FollowerBelief

:::
::: details Provides [amount] [resource]
示例："Provides [3] [Iron]"

适用范围：Global，FollowerBelief，Improvement

:::
::: details [relativeAmount]% [resourceFilter] resource production
示例："[+20]% [Strategic] resource production"

适用范围：Global

:::
::: details Enables establishment of embassies
适用范围：Global

:::
::: details Requires establishing embassies to conduct advanced diplomacy
适用范围：Global

:::
::: details Enables Open Borders agreements
适用范围：Global

:::
::: details Enables Research agreements
适用范围：Global

:::
::: details Science gained from research agreements [relativeAmount]%
示例："Science gained from research agreements [+20]%"

适用范围：Global

:::
::: details Enables Defensive Pacts
适用范围：Global

:::
::: details When declaring friendship, both parties gain a [relativeAmount]% boost to great person generation
示例："When declaring friendship, both parties gain a [+20]% boost to great person generation"

适用范围：Global

:::
::: details Influence of all other civilizations with all city-states degrades [relativeAmount]% faster
示例："Influence of all other civilizations with all city-states degrades [+20]% faster"

适用范围：Global

:::
::: details Gain [amount] Influence with a [baseUnitFilter] gift to a City-State
示例："Gain [3] Influence with a [Melee] gift to a City-State"

适用范围：Global

:::
::: details Resting point for Influence with City-States following this religion [amount]
示例："Resting point for Influence with City-States following this religion [3]"

适用范围：Global

:::
::: details Notified of new Barbarian encampments
适用范围：Global

:::
::: details Receive [relativeAmount]% Gold from Barbarian encampments and pillaging Cities
示例："Receive [+20]% Gold from Barbarian encampments and pillaging Cities"

适用范围：Global

:::
::: details When conquering an encampment, earn [amount] Gold and recruit a Barbarian unit
示例："When conquering an encampment, earn [3] Gold and recruit a Barbarian unit"

适用范围：Global

:::
::: details When defeating a [mapUnitFilter] unit, earn [amount] Gold and recruit it
示例："When defeating a [Wounded] unit, earn [3] Gold and recruit it"

适用范围：Global

:::
::: details May choose [amount] additional [beliefType] beliefs when [foundingOrEnhancing] a religion
示例："May choose [3] additional [Follower] beliefs when [founding] a religion"

适用范围：Global

:::
::: details May choose [amount] additional belief(s) of any type when [foundingOrEnhancing] a religion
示例："May choose [3] additional belief(s) of any type when [founding] a religion"

适用范围：Global

:::
::: details [stats] when a city adopts this religion for the first time
示例："[+1 Gold, +2 Production] when a city adopts this religion for the first time"

此词条的效果可被 &lt;(modified by game speed)&gt;

适用范围：Global

:::
::: details [relativeAmount]% Natural religion spread [cityFilter]
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Natural religion spread [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details Religion naturally spreads to cities [amount] tiles away
示例："Religion naturally spreads to cities [3] tiles away"

适用范围：Global，FollowerBelief

:::
::: details May not generate great prophet equivalents naturally
适用范围：Global

:::
::: details [relativeAmount]% Faith cost of generating Great Prophet equivalents
示例："[+20]% Faith cost of generating Great Prophet equivalents"

适用范围：Global

:::
::: details [relativeAmount]% spy effectiveness [cityFilter]
示例："[+20]% spy effectiveness [in all cities]"

适用范围：Global

:::
::: details [relativeAmount]% enemy spy effectiveness [cityFilter]
示例："[+20]% enemy spy effectiveness [in all cities]"

适用范围：Global

:::
::: details New spies start with [amount] level(s)
示例："New spies start with [3] level(s)"

适用范围：Global

:::
::: details Spies in [cityFilter] cities act as though they have [relativeAmount] levels for [spyAction]
间谍在匹配城市执行指定行动时的临时有效等级变化（[relativeAmount] 加到等级上，如 +1）。不会永久提升间谍等级。按加法叠加，上限为 maxSpyRank。

示例："Spies in [in all cities] cities act as though they have [+20] levels for [Counter-intelligence]"

适用范围：Global

:::
::: details Triggers victory
适用范围：Global

:::
::: details Triggers a Cultural Victory upon completion
适用范围：Global

:::
::: details May buy items in puppet cities
适用范围：Global

:::
::: details May not annex cities
适用范围：Global

:::
::: details "Borrows" city names from other civilizations in the game
适用范围：Global

:::
::: details Cities are razed [amount] times as fast
示例："Cities are razed [3] times as fast"

适用范围：Global

:::
::: details Receive a tech boost when scientific buildings/wonders are built in capital
适用范围：Global

:::
::: details [relativeAmount]% Golden Age length
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Golden Age length"

适用范围：Global

:::
::: details Population loss from nuclear attacks [relativeAmount]% [cityFilter]
示例："Population loss from nuclear attacks [+20]% [in all cities]"

适用范围：Global

:::
::: details Damage to garrison from nuclear attacks [relativeAmount]% [cityFilter]
示例："Damage to garrison from nuclear attacks [+20]% [in all cities]"

适用范围：Global

:::
::: details Rebel units may spawn
适用范围：Global

:::
::: details Cannot build [buildingFilter] buildings
示例："Cannot build [Culture] buildings"

适用范围：Global

:::
::: details [relativeAmount]% Strength
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% Strength"

适用范围：Global，Unit

:::
::: details [relativeAmount] Strength
示例："[+20] Strength"

适用范围：Global，Unit

:::
::: details [relativeAmount] Max HP
允许负值倒扣；结果不大于 0 时重置为 1。只有无条件或带「for [mapUnitFilter] units」修饰的 unique 才生效：其余条件会被忽略以保证最大生命恒定。

示例："[+20] Max HP"

适用范围：Global，Unit

:::
::: details [relativeAmount]% Strength decreasing with distance from the capital
示例："[+20]% Strength decreasing with distance from the capital"

适用范围：Global，Unit

:::
::: details [relativeAmount]% to Flank Attack bonuses
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% to Flank Attack bonuses"

适用范围：Global，Unit

:::
::: details [amount] additional attacks per turn
示例："[3] additional attacks per turn"

适用范围：Global，Unit

:::
::: details [amount] Movement
示例："[3] Movement"

适用范围：Global，Unit

:::
::: details [amount] Sight
示例："[3] Sight"

适用范围：Global，Unit，Terrain，Improvement

:::
::: details [amount] Range
示例："[3] Range"

适用范围：Global，Unit

:::
::: details [relativeAmount] Air Interception Range
示例："[+20] Air Interception Range"

适用范围：Global，Unit

:::
::: details [amount] HP when healing
示例："[3] HP when healing"

适用范围：Global，Unit

:::
::: details [relativeAmount]% Spread Religion Strength
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Spread Religion Strength"

适用范围：Global，Unit

:::
::: details When spreading religion to a city, gain [amount] times the amount of followers of other religions as [stat]
示例："When spreading religion to a city, gain [3] times the amount of followers of other religions as [Culture]"

适用范围：Global，Unit

:::
::: details Ranged attacks may be performed over obstacles
适用范围：Global，Unit

:::
::: details No defensive terrain bonus
适用范围：Global，Unit

:::
::: details No defensive terrain penalty
适用范围：Global，Unit

:::
::: details No damage penalty for wounded units
适用范围：Global，Unit

:::
::: details Unable to capture cities
适用范围：Global，Unit

:::
::: details Unable to pillage tiles
适用范围：Global，Unit

:::
::: details No movement cost to pillage
适用范围：Global，Unit

:::
::: details May heal outside of friendly territory
适用范围：Global，Unit

:::
::: details All healing effects doubled
适用范围：Global，Unit

:::
::: details Heals [amount] damage if it kills a unit
示例："Heals [3] damage if it kills a unit"

适用范围：Global，Unit

:::
::: details Can only heal by pillaging
适用范围：Global，Unit

:::
::: details [relativeAmount]% maintenance costs
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% maintenance costs"

适用范围：Global，Unit

:::
::: details [relativeAmount]% Gold cost of upgrading
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Gold cost of upgrading"

适用范围：Global，Unit

:::
::: details Earn [amount]% of the damage done to [combatantFilter] units as [stockpile]
示例："Earn [3]% of the damage done to [City] units as [Mana]"

适用范围：Global，Unit

:::
::: details Upon capturing a city, receive [amount] times its [stat] production as [stockpile] immediately
示例："Upon capturing a city, receive [3] times its [Culture] production as [Mana] immediately"

适用范围：Global，Unit

:::
::: details Earn [amount]% of killed [mapUnitFilter] unit's [costOrStrength] as [stockpile]
示例："Earn [3]% of killed [Wounded] unit's [Cost] as [Mana]"

适用范围：Global，Unit

:::
::: details [amount] XP gained from combat
示例："[3] XP gained from combat"

适用范围：Global，Unit

:::
::: details [relativeAmount]% XP gained from combat
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% XP gained from combat"

适用范围：Global，Unit

:::
::: details [greatPerson] is earned [relativeAmount]% faster
示例："[Great General] is earned [+20]% faster"

适用范围：Global，Unit

:::
::: details [nonNegativeAmount] Movement point cost to disembark
示例："[3] Movement point cost to disembark"

由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Global，Unit

:::
::: details [nonNegativeAmount] Movement point cost to embark
示例："[3] Movement point cost to embark"

由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Global，Unit

:::
## Nation uniques（国家词条）
::: details Starts with [tech]
示例："Starts with [Agriculture]"

适用范围：Nation

:::
::: details Starts with [policy] adopted
示例："Starts with [Oligarchy] adopted"

适用范围：Nation

:::
::: details Start bias [terrainFilter]
与 Nation 的 startBias 字段条目效果相同。与 startBias 字段合并；对城邦而言，还与其 CityStateType 上匹配的 unique 合并。条件仅在生成地图/放置起始位置时对 GameInfo 求值（没有 Civilization——它可能只被部分初始化）。不要使用需要地块、城市或单位的条件。

示例："Start bias [Fresh Water]"

适用范围：Nation，CityState

:::
::: details All units move through Forest and Jungle Tiles in friendly territory as if they have roads. These tiles can be used to establish City Connections upon researching the Wheel.
由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Nation

:::
::: details Units ignore terrain costs when moving into any tile with Hills
由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Nation

:::
::: details Excluded from map editor
此词条自动对用户隐藏。

适用范围：Nation，Terrain，Improvement，Resource

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Will not be chosen for new games
适用范围：Nation

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## Personality uniques（个性词条）
::: details Will not build [baseUnitFilter/buildingFilter]
示例："Will not build [Melee]"

适用范围：Personality

:::
::: details [relativeAmount]% weight to [baseUnitFilter/buildingFilter] for AI decisions
示例："[+20]% weight to [Melee] for AI decisions"

此词条自动对用户隐藏。

适用范围：Personality

:::
## Era uniques（时代词条）
::: details Starting in this era disables religion
适用范围：Era

:::
::: details Every major Civilization gains a spy once a civilization enters this era
适用范围：Era

:::
## Tech uniques（科技词条）
::: details Starting tech
适用范围：Tech

:::
::: details Can be continually researched
适用范围：Tech

:::
::: details Only available
用于与条件配合，如 "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。也会阻止升级（Upgrade）和转换（Transform）行动。另见 CanOnlyBeBuiltWhen

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Unavailable
用于与条件配合，如 "Unavailable &lt;after generating a Great Prophet&gt;"。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Cannot be hurried
适用范围：Tech，Building

:::
::: details [relativeAmount]% weight to this choice for AI decisions
示例："[+20]% weight to this choice for AI decisions"

此词条自动对用户隐藏。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Promotion，EventChoice

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## Policy uniques（政策词条）
::: details Only available
用于与条件配合，如 "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。也会阻止升级（Upgrade）和转换（Transform）行动。另见 CanOnlyBeBuiltWhen

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Unavailable
用于与条件配合，如 "Unavailable &lt;after generating a Great Prophet&gt;"。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details [relativeAmount]% weight to this choice for AI decisions
示例："[+20]% weight to this choice for AI decisions"

此词条自动对用户隐藏。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Promotion，EventChoice

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## FounderBelief uniques（创始人信仰词条）
::: note

创始人及增强类信条的词条，作用于该宗教的创始人
:::

::: details [stats] for each global city following this religion
示例："[+1 Gold, +2 Production] for each global city following this religion"

适用范围：FounderBelief

:::
::: details [stats] from every [positiveAmount] global followers [cityFilter]
示例："[+1 Gold, +2 Production] from every [3] global followers [in all cities]"

适用范围：FounderBelief

:::
::: details [relativeAmount]% [stat] from every follower, up to [relativeAmount2]%
示例："[+20]% [Culture] from every follower, up to [+20]%"

适用范围：FounderBelief，FollowerBelief

:::
::: details Only available
用于与条件配合，如 "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。也会阻止升级（Upgrade）和转换（Transform）行动。另见 CanOnlyBeBuiltWhen

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Unavailable
用于与条件配合，如 "Unavailable &lt;after generating a Great Prophet&gt;"。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details [relativeAmount]% weight to this choice for AI decisions
示例："[+20]% weight to this choice for AI decisions"

此词条自动对用户隐藏。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Promotion，EventChoice

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## FollowerBelief uniques（追随者信仰词条）
::: note

万神殿与追随者类信条的词条，作用于该宗教为主导宗教的每座城市
:::

::: details [stats] [cityFilter]
示例："[+1 Gold, +2 Production] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [stats] from every specialist [cityFilter]
示例："[+1 Gold, +2 Production] from every specialist [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [stats] per [positiveAmount] population [cityFilter]
示例："[+1 Gold, +2 Production] per [3] population [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [stats] in cities on [terrainFilter] tiles
示例："[+1 Gold, +2 Production] in cities on [Fresh Water] tiles"

适用范围：Global，FollowerBelief

:::
::: details [stats] from all [buildingFilter] buildings
示例："[+1 Gold, +2 Production] from all [Culture] buildings"

适用范围：Global，FollowerBelief

:::
::: details [stats] from [tileFilter] tiles [cityFilter]
示例："[+1 Gold, +2 Production] from [Farm] tiles [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [stats] from [tileFilter] tiles without [tileFilter2] [cityFilter]
示例："[+1 Gold, +2 Production] from [Farm] tiles without [Farm] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [stats] from every [tileFilter/specialist/buildingFilter]
示例："[+1 Gold, +2 Production] from every [Farm]"

适用范围：Global，FollowerBelief

:::
::: details [stats] from each Trade Route
示例："[+1 Gold, +2 Production] from each Trade Route"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% [stat]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% [Culture]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% [stat] [cityFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% [Culture] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% [stat] from every [tileFilter/buildingFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% [Culture] from every [Farm]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Yield from every [tileFilter/buildingFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% Yield from every [Farm]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% [stat] from every follower, up to [relativeAmount2]%
示例："[+20]% [Culture] from every follower, up to [+20]%"

适用范围：FounderBelief，FollowerBelief

:::
::: details [relativeAmount]% Production when constructing [buildingFilter] buildings [cityFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% Production when constructing [Culture] buildings [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Production when constructing [baseUnitFilter] units [cityFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% Production when constructing [Melee] units [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Production when constructing [buildingFilter] wonders [cityFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% Production when constructing [Culture] wonders [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Production towards any buildings that already exist in the Capital
示例："[+20]% Production towards any buildings that already exist in the Capital"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% growth [cityFilter]
示例："[+20]% growth [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [amount]% Food is carried over after population increases [cityFilter]
多个加成按加法叠加：+50% + +50% = +100%

示例："[3]% Food is carried over after population increases [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Food consumption by [populationFilter] [cityFilter]
示例："[+20]% Food consumption by [Followers of this Religion] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Unhappiness from [populationFilter] [cityFilter]
示例："[+20]% Unhappiness from [Followers of this Religion] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details May buy [baseUnitFilter] units for [nonNegativeAmount] [stat] [cityFilter] at an increasing price ([amount])
示例："May buy [Melee] units for [3] [Culture] [in all cities] at an increasing price ([3])"

适用范围：Global，FollowerBelief

:::
::: details May buy [buildingFilter] buildings for [nonNegativeAmount] [stat] [cityFilter] at an increasing price ([amount])
示例："May buy [Culture] buildings for [3] [Culture] [in all cities] at an increasing price ([3])"

适用范围：Global，FollowerBelief

:::
::: details May buy [baseUnitFilter] units for [nonNegativeAmount] [stat] [cityFilter]
示例："May buy [Melee] units for [3] [Culture] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details May buy [buildingFilter] buildings for [nonNegativeAmount] [stat] [cityFilter]
示例："May buy [Culture] buildings for [3] [Culture] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details May buy [baseUnitFilter] units with [stat] [cityFilter]
示例："May buy [Melee] units with [Culture] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details May buy [buildingFilter] buildings with [stat] [cityFilter]
示例："May buy [Culture] buildings with [Culture] [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details May buy [baseUnitFilter] units with [stat] for [nonNegativeAmount] times their normal Production cost
示例："May buy [Melee] units with [Culture] for [3] times their normal Production cost"

适用范围：Global，FollowerBelief

:::
::: details May buy [buildingFilter] buildings with [stat] for [nonNegativeAmount] times their normal Production cost
示例："May buy [Culture] buildings with [Culture] for [3] times their normal Production cost"

适用范围：Global，FollowerBelief

:::
::: details [stat] cost of purchasing items in cities [relativeAmount]%
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[Culture] cost of purchasing items in cities [+20]%"

适用范围：Global，FollowerBelief

:::
::: details [stat] cost of purchasing [buildingFilter] buildings [relativeAmount]%
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[Culture] cost of purchasing [Culture] buildings [+20]%"

适用范围：Global，FollowerBelief

:::
::: details [stat] cost of purchasing [baseUnitFilter] units [relativeAmount]%
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[Culture] cost of purchasing [Melee] units [+20]%"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% maintenance cost for [buildingFilter] buildings [cityFilter]
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% maintenance cost for [Culture] buildings [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Culture cost of natural border growth [cityFilter]
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Culture cost of natural border growth [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Gold cost of acquiring tiles [cityFilter]
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Gold cost of acquiring tiles [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Great Person generation [cityFilter]
示例："[+20]% Great Person generation [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details New [baseUnitFilter] units start with [amount] XP [cityFilter]
示例："New [Melee] units start with [3] XP [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details All newly-trained [baseUnitFilter] units [cityFilter] receive the [promotion] promotion
示例："All newly-trained [Melee] units [in all cities] receive the [Shock I] promotion"

适用范围：Global，FollowerBelief

:::
::: details [mapUnitFilter] Units adjacent to this city heal [amount] HP per turn when healing
示例："[Wounded] Units adjacent to this city heal [3] HP per turn when healing"

适用范围：Global，FollowerBelief

:::
::: details [relativeAmount]% Strength for cities
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% Strength for cities"

适用范围：Global，FollowerBelief

:::
::: details Provides [amount] [resource]
示例："Provides [3] [Iron]"

适用范围：Global，FollowerBelief，Improvement

:::
::: details [relativeAmount]% Natural religion spread [cityFilter]
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Natural religion spread [in all cities]"

适用范围：Global，FollowerBelief

:::
::: details Religion naturally spreads to cities [amount] tiles away
示例："Religion naturally spreads to cities [3] tiles away"

适用范围：Global，FollowerBelief

:::
::: details Only available
用于与条件配合，如 "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。也会阻止升级（Upgrade）和转换（Transform）行动。另见 CanOnlyBeBuiltWhen

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Unavailable
用于与条件配合，如 "Unavailable &lt;after generating a Great Prophet&gt;"。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Earn [amount]% of [mapUnitFilter] unit's [costOrStrength] as [stockpile] when killed within 4 tiles of a city following this religion
示例："Earn [3]% of [Wounded] unit's [Cost] as [Mana] when killed within 4 tiles of a city following this religion"

适用范围：FollowerBelief

:::
::: details [relativeAmount]% weight to this choice for AI decisions
示例："[+20]% weight to this choice for AI decisions"

此词条自动对用户隐藏。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Promotion，EventChoice

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## Building uniques（建筑词条）
::: details [positiveAmount]% of [stat] from every [improvementFilter/buildingFilter] in the city added to [resource]
示例："[3]% of [Culture] from every [All Road] in the city added to [Iron]"

适用范围：Building

:::
::: details Consumes [amount] [resource]
示例："Consumes [3] [Iron]"

适用范围：Building，Unit，Improvement

:::
::: details Costs [amount] [stockpiledResource]
These resources are removed *when work begins* on the construction. Do not confuse with "costs [amount] [stockpiledResource]" (lowercase 'c'), the Unit Action Modifier.

示例："Costs [3] [Mana]"

此词条的效果可被 &lt;(modified by game speed)&gt;

适用范围：Building，Unit，Improvement

:::
::: details Unbuildable
阻止被建造（可能由条件决定）。但仍会出现在菜单中，且可通过金币或信仰等其他方式购买

适用范围：Building，Unit，Improvement

:::
::: details Cannot be purchased
适用范围：Building，Unit

:::
::: details Can be purchased with [stat] [cityFilter]
示例："Can be purchased with [Culture] [in all cities]"

适用范围：Building，Unit

:::
::: details Can be purchased for [amount] [stat] [cityFilter]
示例："Can be purchased for [3] [Culture] [in all cities]"

适用范围：Building，Unit

:::
::: details Limited to [amount] per Civilization
示例："Limited to [3] per Civilization"

适用范围：Building，Unit

:::
::: details Only available
用于与条件配合，如 "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。也会阻止升级（Upgrade）和转换（Transform）行动。另见 CanOnlyBeBuiltWhen

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Unavailable
用于与条件配合，如 "Unavailable &lt;after generating a Great Prophet&gt;"。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Excess Food converted to Production when under construction
适用范围：Building，Unit

:::
::: details Requires at least [amount] population
示例："Requires at least [3] population"

适用范围：Building，Unit

:::
::: details Triggers a global alert upon build start
适用范围：Building，Unit

:::
::: details Triggers a global alert upon completion
适用范围：Building，Unit

:::
::: details Cost increases by [amount] per owned city
示例："Cost increases by [3] per owned city"

适用范围：Building，Unit

:::
::: details Cost increases by [amount] when built
示例："Cost increases by [3] when built"

适用范围：Building，Unit

:::
::: details [amount]% production cost
用于与条件配合，动态调整建造费用。$MULTIPLICATIVE_BONUS_EXPLANATION_ZH

示例："[3]% production cost"

适用范围：Building，Unit

:::
::: details Can only be built
用于与条件配合，如 "Can only be built &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。**不会**阻止升级（Upgrade）和转换（Transform）行动。另见 OnlyAvailable。

适用范围：Building，Unit

:::
::: details Must have an owned [tileFilter] within [amount] tiles
示例："Must have an owned [Farm] within [3] tiles"

适用范围：Building

:::
::: details Enables nuclear weapon
适用范围：Building

:::
::: details Must be on [tileFilter]
示例："Must be on [Farm]"

适用范围：Building

:::
::: details Must not be on [tileFilter]
示例："Must not be on [Farm]"

适用范围：Building

:::
::: details Must be next to [tileFilter]
示例："Must be next to [Farm]"

适用范围：Building，Improvement

:::
::: details Must not be next to [tileFilter]
示例："Must not be next to [Farm]"

适用范围：Building

:::
::: details Unsellable
适用范围：Building

:::
::: details Obsolete with [tech]
示例："Obsolete with [Agriculture]"

适用范围：Building，Improvement，Resource

:::
::: details Indicates the capital city
适用范围：Building

:::
::: details Moves to new capital when capital changes
适用范围：Building

:::
::: details Provides 1 extra copy of each improved luxury resource near this City
适用范围：Building

:::
::: details Destroyed when the city is captured
适用范围：Building

:::
::: details Never destroyed when the city is captured
适用范围：Building

:::
::: details [relativeAmount]% Gold given to enemy if city is captured
示例："[+20]% Gold given to enemy if city is captured"

适用范围：Building

:::
::: details Removes extra unhappiness from annexed cities
适用范围：Building

:::
::: details Connects trade routes over water
适用范围：Building

:::
::: details Automatically built in all cities where it is buildable
适用范围：Building

:::
::: details Creates a [improvementName] improvement on a specific tile
When choosing to construct this building, the player must select a tile where the improvement can be built. Upon building completion, the tile will gain this improvement. Limited to one per building.

示例："Creates a [Trading Post] improvement on a specific tile"

此词条不支持条件。

适用范围：Building

:::
::: details Hidden from city screen
建造后，此建筑从城市详情界面隐藏。所有产出照常生效。

此词条自动对用户隐藏。

适用范围：Building

:::
::: details Can be built [amount] times in each city
允许此建筑在同一城市重复建造。使用 -1 表示不限次数。

示例："Can be built [3] times in each city"

适用范围：Building

:::
::: details Can carry [amount] extra [mapUnitFilter] units
对建筑，支持用 `Air` 作为 `mapUnitFilter` 以增加城市空中单位容量。

示例："Can carry [3] extra [Wounded] units"

适用范围：Building，Unit

:::
::: details Spaceship part
适用范围：Building，Unit

:::
::: details Cannot be hurried
适用范围：Tech，Building

:::
::: details [relativeAmount]% weight to this choice for AI decisions
示例："[+20]% weight to this choice for AI decisions"

此词条自动对用户隐藏。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Promotion，EventChoice

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Shown while unbuilable
此词条自动对用户隐藏。

适用范围：Building，Unit

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## UnitAction uniques（单位行动词条）
::: note

影响单位行动的词条，可用 UnitActionModifiers 修饰
:::

::: details Founds a new city
适用范围：UnitAction

:::
::: details Founds a new puppet city
适用范围：UnitAction

:::
::: details Can instantly construct a [improvementFilter] improvement
示例："Can instantly construct a [All Road] improvement"

适用范围：UnitAction

:::
::: details Can Spread Religion
适用范围：UnitAction

:::
::: details Can remove other religions from cities
适用范围：UnitAction

:::
::: details May found a religion
适用范围：UnitAction

:::
::: details May enhance a religion
适用范围：UnitAction

:::
::: details Can transform to [unit]
默认消耗全部移动力

示例："Can transform to [Musketman]"

适用范围：UnitAction

:::
## Unit uniques（单位词条）
::: note

可添加到单位、单位类型或晋升的词条
:::

::: details [relativeAmount]% Yield from pillaging tiles
示例："[+20]% Yield from pillaging tiles"

适用范围：Global，Unit

:::
::: details [relativeAmount]% Health from pillaging tiles
示例："[+20]% Health from pillaging tiles"

适用范围：Global，Unit

:::
::: details [relativeAmount]% construction time for [improvementFilter] improvements
示例："[+20]% construction time for [All Road] improvements"

适用范围：Global，Unit

:::
::: details Can build [improvementFilter] improvements at a [relativeAmount]% rate
示例："Can build [All Road] improvements at a [+20]% rate"

适用范围：Global，Unit

:::
::: details [relativeAmount]% Gold from Great Merchant trade missions
示例："[+20]% Gold from Great Merchant trade missions"

适用范围：Global，Unit

:::
::: details Great General provides double combat bonus
适用范围：Global，Unit

:::
::: details Consumes [amount] [resource]
示例："Consumes [3] [Iron]"

适用范围：Building，Unit，Improvement

:::
::: details Costs [amount] [stockpiledResource]
These resources are removed *when work begins* on the construction. Do not confuse with "costs [amount] [stockpiledResource]" (lowercase 'c'), the Unit Action Modifier.

示例："Costs [3] [Mana]"

此词条的效果可被 &lt;(modified by game speed)&gt;

适用范围：Building，Unit，Improvement

:::
::: details Unbuildable
阻止被建造（可能由条件决定）。但仍会出现在菜单中，且可通过金币或信仰等其他方式购买

适用范围：Building，Unit，Improvement

:::
::: details Cannot be purchased
适用范围：Building，Unit

:::
::: details Can be purchased with [stat] [cityFilter]
示例："Can be purchased with [Culture] [in all cities]"

适用范围：Building，Unit

:::
::: details Can be purchased for [amount] [stat] [cityFilter]
示例："Can be purchased for [3] [Culture] [in all cities]"

适用范围：Building，Unit

:::
::: details Limited to [amount] per Civilization
示例："Limited to [3] per Civilization"

适用范围：Building，Unit

:::
::: details Only available
用于与条件配合，如 "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。也会阻止升级（Upgrade）和转换（Transform）行动。另见 CanOnlyBeBuiltWhen

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Unavailable
用于与条件配合，如 "Unavailable &lt;after generating a Great Prophet&gt;"。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Excess Food converted to Production when under construction
适用范围：Building，Unit

:::
::: details Requires at least [amount] population
示例："Requires at least [3] population"

适用范围：Building，Unit

:::
::: details Triggers a global alert upon build start
适用范围：Building，Unit

:::
::: details Triggers a global alert upon completion
适用范围：Building，Unit

:::
::: details Cost increases by [amount] per owned city
示例："Cost increases by [3] per owned city"

适用范围：Building，Unit

:::
::: details Cost increases by [amount] when built
示例："Cost increases by [3] when built"

适用范围：Building，Unit

:::
::: details [amount]% production cost
用于与条件配合，动态调整建造费用。$MULTIPLICATIVE_BONUS_EXPLANATION_ZH

示例："[3]% production cost"

适用范围：Building，Unit

:::
::: details Can only be built
用于与条件配合，如 "Can only be built &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。**不会**阻止升级（Upgrade）和转换（Transform）行动。另见 OnlyAvailable。

适用范围：Building，Unit

:::
::: details May create improvements on water resources
适用范围：Unit

:::
::: details Can build [improvementFilter/terrainFilter] improvements on tiles
示例："Can build [All Road] improvements on tiles"

适用范围：Unit

:::
::: details Can be added to [comment] in the Capital
示例："Can be added to [comment] in the Capital"

适用范围：Unit

:::
::: details Prevents spreading of religion to the city it is next to
适用范围：Unit

:::
::: details Removes other religions when spreading religion
适用范围：Unit

:::
::: details May Paradrop to [tileFilter] tiles up to [positiveAmount] tiles away
示例："May Paradrop to [Farm] tiles up to [3] tiles away"

适用范围：Unit

:::
::: details Can perform Air Sweep
适用范围：Unit

:::
::: details Can speed up construction of a building
适用范围：Unit

:::
::: details Can speed up the construction of a wonder
适用范围：Unit

:::
::: details Can hurry technology research
适用范围：Unit

:::
::: details Can generate a large amount of culture
适用范围：Unit

:::
::: details Can undertake a trade mission with City-State, giving a large sum of gold and [amount] Influence
示例："Can undertake a trade mission with City-State, giving a large sum of gold and [3] Influence"

适用范围：Unit

:::
::: details Automation is a primary action
此词条自动对用户隐藏。

适用范围：Unit

:::
::: details [relativeAmount]% Strength
多个加成按加法叠加：+50% + +50% = +100%

示例："[+20]% Strength"

适用范围：Global，Unit

:::
::: details [relativeAmount] Strength
示例："[+20] Strength"

适用范围：Global，Unit

:::
::: details [relativeAmount] Max HP
允许负值倒扣；结果不大于 0 时重置为 1。只有无条件或带「for [mapUnitFilter] units」修饰的 unique 才生效：其余条件会被忽略以保证最大生命恒定。

示例："[+20] Max HP"

适用范围：Global，Unit

:::
::: details [relativeAmount]% Strength decreasing with distance from the capital
示例："[+20]% Strength decreasing with distance from the capital"

适用范围：Global，Unit

:::
::: details [relativeAmount]% to Flank Attack bonuses
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% to Flank Attack bonuses"

适用范围：Global，Unit

:::
::: details [relativeAmount]% Strength for enemy [mapUnitFilter] units in adjacent [tileFilter] tiles
示例："[+20]% Strength for enemy [Wounded] units in adjacent [Farm] tiles"

适用范围：Unit

:::
::: details [relativeAmount]% Strength bonus for [mapUnitFilter] units within [amount] tiles
示例："[+20]% Strength bonus for [Wounded] units within [3] tiles"

适用范围：Unit

:::
::: details [amount] additional attacks per turn
示例："[3] additional attacks per turn"

适用范围：Global，Unit

:::
::: details [amount] Movement
示例："[3] Movement"

适用范围：Global，Unit

:::
::: details [amount] Sight
示例："[3] Sight"

适用范围：Global，Unit，Terrain，Improvement

:::
::: details [amount] Range
示例："[3] Range"

适用范围：Global，Unit

:::
::: details [relativeAmount] Air Interception Range
示例："[+20] Air Interception Range"

适用范围：Global，Unit

:::
::: details [amount] HP when healing
示例："[3] HP when healing"

适用范围：Global，Unit

:::
::: details Before engaging in combat performs an extra ranged attack with [amount]% of melee combat strength
示例："Before engaging in combat performs an extra ranged attack with [3]% of melee combat strength"

适用范围：Unit

:::
::: details [relativeAmount]% Spread Religion Strength
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Spread Religion Strength"

适用范围：Global，Unit

:::
::: details When spreading religion to a city, gain [amount] times the amount of followers of other religions as [stat]
示例："When spreading religion to a city, gain [3] times the amount of followers of other religions as [Culture]"

适用范围：Global，Unit

:::
::: details Can only attack [combatantFilter] units
示例："Can only attack [City] units"

适用范围：Unit

:::
::: details Can only attack [tileFilter] tiles
示例："Can only attack [Farm] tiles"

适用范围：Unit

:::
::: details Cannot attack
适用范围：Unit

:::
::: details Must set up to ranged attack
适用范围：Unit

:::
::: details Self-destructs when attacking
适用范围：Unit

:::
::: details Eliminates combat penalty for attacking across a coast
适用范围：Unit

:::
::: details May attack when embarked
适用范围：Unit

:::
::: details Eliminates combat penalty for attacking over a river
适用范围：Unit

:::
::: details Blast radius [amount]
示例："Blast radius [3]"

适用范围：Unit

:::
::: details Ranged attacks may be performed over obstacles
适用范围：Global，Unit

:::
::: details Nuclear weapon of Strength [amount]
示例："Nuclear weapon of Strength [3]"

适用范围：Unit

:::
::: details Attacks also target [mapUnitFilter] units within [positiveAmount] tiles
对半径内匹配过滤器的所有单位发动攻击（包括未被过滤掉的盟军或己方单位），伤害均等。状态效果和命中能力作用于所有受影响单位。

如果同时存在本效果和递减范围攻击，则只使用递减范围攻击。

示例："Attacks also target [Wounded] units within [3] tiles"

适用范围：Unit

:::
::: details Attacks also target [mapUnitFilter] units within [positiveAmount] tiles, with damage decreasing by distance
对半径内匹配过滤器的所有单位发动攻击，伤害随与主目标的距离递减。状态效果和命中能力生效。

如果同时存在本效果和均等范围攻击，则只使用本效果；也影响反击伤害和自身范围攻击的伤害。

伤害公式：伤害 = (1 - (距离 / 半径)) * 基础伤害

示例："Attacks also target [Wounded] units within [3] tiles, with damage decreasing by distance"

适用范围：Unit

:::
::: details Takes [relativeAmount]% damage from own area attacks
此单位在范围内时承受自身范围攻击的伤害，100 = 100% 伤害。

示例："Takes [+20]% damage from own area attacks"

适用范围：Unit

:::
::: details Takes [relativeAmount]% counter damage from each unit hit by its area attacks
仅对近战单位生效，100 = 100% 伤害，负值可用但按正值处理。

示例："Takes [+20]% counter damage from each unit hit by its area attacks"

适用范围：Unit

:::
::: details No defensive terrain bonus
适用范围：Global，Unit

:::
::: details No defensive terrain penalty
适用范围：Global，Unit

:::
::: details No damage penalty for wounded units
适用范围：Global，Unit

:::
::: details Uncapturable
适用范围：Unit

:::
::: details Withdraws before melee combat
适用范围：Unit

:::
::: details Unable to capture cities
适用范围：Global，Unit

:::
::: details Unable to pillage tiles
适用范围：Global，Unit

:::
::: details Destroys [cityFilter] cities instead of capturing
此单位将摧毁 [cityFilter] 城市而不是占领它们，也允许非近战单位摧毁城市。首都（包括城邦）对此效果免疫。

示例："Destroys [in all cities] cities instead of capturing"

适用范围：Unit

:::
::: details No movement cost to pillage
适用范围：Global，Unit

:::
::: details Can move after attacking
适用范围：Unit

:::
::: details Transfer Movement to [mapUnitFilter]
示例："Transfer Movement to [Wounded]"

适用范围：Unit

:::
::: details Can move immediately once bought
适用范围：Unit

:::
::: details May heal outside of friendly territory
适用范围：Global，Unit

:::
::: details All healing effects doubled
适用范围：Global，Unit

:::
::: details Heals [amount] damage if it kills a unit
示例："Heals [3] damage if it kills a unit"

适用范围：Global，Unit

:::
::: details Can only heal by pillaging
适用范围：Global，Unit

:::
::: details Unit will heal every turn, even if it performs an action
适用范围：Unit

:::
::: details All adjacent units heal [amount] HP when healing
示例："All adjacent units heal [3] HP when healing"

适用范围：Unit

:::
::: details No Sight
适用范围：Unit

:::
::: details Can see over obstacles
适用范围：Unit

:::
::: details Can carry [amount] [mapUnitFilter] units
示例："Can carry [3] [Wounded] units"

适用范围：Unit

:::
::: details Can carry [amount] extra [mapUnitFilter] units
对建筑，支持用 `Air` 作为 `mapUnitFilter` 以增加城市空中单位容量。

示例："Can carry [3] extra [Wounded] units"

适用范围：Building，Unit

:::
::: details Cannot be carried by [mapUnitFilter] units
示例："Cannot be carried by [Wounded] units"

适用范围：Unit

:::
::: details [relativeAmount]% chance to intercept air attacks
示例："[+20]% chance to intercept air attacks"

适用范围：Unit

:::
::: details Damage taken from interception reduced by [relativeAmount]%
示例："Damage taken from interception reduced by [+20]%"

适用范围：Unit

:::
::: details [relativeAmount]% Damage when intercepting
示例："[+20]% Damage when intercepting"

适用范围：Unit

:::
::: details [amount] extra interceptions may be made per turn
示例："[3] extra interceptions may be made per turn"

适用范围：Unit

:::
::: details Cannot be intercepted
适用范围：Unit

:::
::: details Cannot intercept [mapUnitFilter] units
示例："Cannot intercept [Wounded] units"

适用范围：Unit

:::
::: details [relativeAmount]% Strength when performing Air Sweep
示例："[+20]% Strength when performing Air Sweep"

适用范围：Unit

:::
::: details [relativeAmount]% maintenance costs
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% maintenance costs"

适用范围：Global，Unit

:::
::: details [relativeAmount]% Gold cost of upgrading
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% Gold cost of upgrading"

适用范围：Global，Unit

:::
::: details Earn [amount]% of the damage done to [combatantFilter] units as [stockpile]
示例："Earn [3]% of the damage done to [City] units as [Mana]"

适用范围：Global，Unit

:::
::: details Upon capturing a city, receive [amount] times its [stat] production as [stockpile] immediately
示例："Upon capturing a city, receive [3] times its [Culture] production as [Mana] immediately"

适用范围：Global，Unit

:::
::: details Earn [amount]% of killed [mapUnitFilter] unit's [costOrStrength] as [stockpile]
示例："Earn [3]% of killed [Wounded] unit's [Cost] as [Mana]"

适用范围：Global，Unit

:::
::: details May capture killed [mapUnitFilter] units
示例："May capture killed [Wounded] units"

适用范围：Unit

:::
::: details [amount] XP gained from combat
示例："[3] XP gained from combat"

适用范围：Global，Unit

:::
::: details [relativeAmount]% XP gained from combat
多个加成按乘法叠加：+50% + +50% = x1.5 * x1.5 = +125%

示例："[+20]% XP gained from combat"

适用范围：Global，Unit

:::
::: details Can be earned through combat
适用范围：Unit

:::
::: details [greatPerson] is earned [relativeAmount]% faster
示例："[Great General] is earned [+20]% faster"

适用范围：Global，Unit

:::
::: details Invisible to others
适用范围：Unit

:::
::: details Invisible to non-adjacent units
适用范围：Unit

:::
::: details Can see invisible [mapUnitFilter] units
示例："Can see invisible [Wounded] units"

适用范围：Unit

:::
::: details May upgrade to [unit] through ruins-like effects
示例："May upgrade to [Musketman] through ruins-like effects"

适用范围：Unit

:::
::: details Can upgrade to [unit]
示例："Can upgrade to [Musketman]"

适用范围：Unit

:::
::: details Destroys tile improvements when attacking
适用范围：Unit

:::
::: details Cannot move
由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Unit

:::
::: details Double movement in [terrainFilter]
示例："Double movement in [Fresh Water]"

由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Unit

:::
::: details All tiles cost 1 movement
由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Unit

:::
::: details May travel on Water tiles without embarking
由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Unit

:::
::: details Can pass through impassable tiles
由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Unit

:::
::: details Ignores terrain cost
由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Unit

:::
::: details Ignores Zone of Control
由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Unit

:::
::: details Rough terrain penalty
由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Unit

:::
::: details Can enter ice tiles
由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Unit

:::
::: details Cannot embark
适用范围：Unit

:::
::: details Cannot enter ocean tiles
适用范围：Unit

:::
::: details May enter foreign tiles without open borders
由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Unit

:::
::: details May enter foreign tiles without open borders, but loses [amount] religious strength each turn it ends there
示例："May enter foreign tiles without open borders, but loses [3] religious strength each turn it ends there"

由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Unit

:::
::: details [nonNegativeAmount] Movement point cost to disembark
示例："[3] Movement point cost to disembark"

由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Global，Unit

:::
::: details [nonNegativeAmount] Movement point cost to embark
示例："[3] Movement point cost to embark"

由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

适用范围：Global，Unit

:::
::: details Never appears as a Barbarian unit
此词条自动对用户隐藏。

适用范围：Unit

:::
::: details Religious Unit
适用范围：Unit

:::
::: details Spaceship part
适用范围：Building，Unit

:::
::: details Takes your religion over the one in their birth city
适用范围：Unit

:::
::: details Great Person - [comment]
示例："Great Person - [comment]"

适用范围：Unit

:::
::: details Is part of Great Person group [comment]
同一组的伟人在获得时会使彼此的成本增加。获得一个后，同组的其他伟人将需要更多伟人点（GPP）。

示例："Is part of Great Person group [comment]"

适用范围：Unit

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Shown while unbuilable
此词条自动对用户隐藏。

适用范围：Building，Unit

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## UnitType uniques（单位类别词条）
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## Promotion uniques（晋升项词条）
::: details Only available
用于与条件配合，如 "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。也会阻止升级（Upgrade）和转换（Transform）行动。另见 CanOnlyBeBuiltWhen

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Unavailable
用于与条件配合，如 "Unavailable &lt;after generating a Great Prophet&gt;"。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Not shown on world screen
此词条自动对用户隐藏。

适用范围：Promotion，Resource

:::
::: details Doing so will consume this opportunity to choose a Promotion
适用范围：Promotion

:::
::: details This Promotion is free
适用范围：Promotion

:::
::: details [relativeAmount]% weight to this choice for AI decisions
示例："[+20]% weight to this choice for AI decisions"

此词条自动对用户隐藏。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Promotion，EventChoice

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## Terrain uniques（地形修正词条）
::: details [stats]
示例："[+1 Gold, +2 Production]"

适用范围：Global，Terrain，Improvement

:::
::: details [amount] Sight
示例："[3] Sight"

适用范围：Global，Unit，Terrain，Improvement

:::
::: details Must be adjacent to [amount] [simpleTerrain] tiles
示例："Must be adjacent to [3] [Elevated] tiles"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Must be adjacent to [amount] to [amount2] [simpleTerrain] tiles
示例："Must be adjacent to [3] to [3] [Elevated] tiles"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Must not be on [amount] largest landmasses
示例："Must not be on [3] largest landmasses"

此词条自动对用户隐藏。

适用范围：Terrain，Resource

:::
::: details Must be on [amount] largest landmasses
示例："Must be on [3] largest landmasses"

此词条自动对用户隐藏。

适用范围：Terrain，Resource

:::
::: details Occurs on latitudes from [amount] to [amount2] percent of distance equator to pole
示例："Occurs on latitudes from [3] to [3] percent of distance equator to pole"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Occurs in groups of [amount] to [amount2] tiles
示例："Occurs in groups of [3] to [3] tiles"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Neighboring tiles will convert to [baseTerrain/terrainFeature]
支持只需要 Tile 作为上下文的条件（如 `<with [n]% chance>`），并按每个相邻地块应用。

如果你的模组重命名了海岸或湖泊，请不要将其作为参数使用本效果，因为防止瑕疵的代码将无法工作。

示例："Neighboring tiles will convert to [Grassland]"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Grants [stats] to the first civilization to discover it
示例："Grants [+1 Gold, +2 Production] to the first civilization to discover it"

适用范围：Terrain

:::
::: details Units ending their turn on this terrain take [amount] damage
示例："Units ending their turn on this terrain take [3] damage"

由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

此词条不支持条件。

适用范围：Terrain

:::
::: details Grants [promotion] ([comment]) to adjacent [mapUnitFilter] units for the rest of the game
示例："Grants [Shock I] ([comment]) to adjacent [Wounded] units for the rest of the game"

适用范围：Terrain

:::
::: details [amount] Strength for cities built on this terrain
示例："[3] Strength for cities built on this terrain"

适用范围：Terrain

:::
::: details Provides a one-time bonus of [stats] to the closest city when cut down
示例："Provides a one-time bonus of [+1 Gold, +2 Production] to the closest city when cut down"

此词条的效果可被 &lt;(modified by game speed)&gt;

此词条的效果可被 &lt;(modified by game progress up to [relativeAmount]%)&gt;

适用范围：Terrain

:::
::: details Vegetation
适用范围：Terrain，Improvement

:::
::: details Tile provides yield without assigned population
适用范围：Terrain，Improvement

:::
::: details Nullifies all other stats this tile provides
适用范围：Terrain

:::
::: details Only [improvementFilter] improvements may be built on this tile
示例："Only [All Road] improvements may be built on this tile"

适用范围：Terrain

:::
::: details Blocks line-of-sight from tiles at same elevation
适用范围：Terrain

:::
::: details Has an elevation of [amount] for visibility calculations
示例："Has an elevation of [3] for visibility calculations"

适用范围：Terrain

:::
::: details Always Fertility [amount] for Map Generation
示例："Always Fertility [3] for Map Generation"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details [amount] to Fertility for Map Generation
示例："[3] to Fertility for Map Generation"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details A Region is formed with at least [amount]% [simpleTerrain] tiles, with priority [amount2]
示例："A Region is formed with at least [3]% [Elevated] tiles, with priority [3]"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details A Region is formed with at least [amount]% [simpleTerrain] tiles and [simpleTerrain2] tiles, with priority [amount2]
示例："A Region is formed with at least [3]% [Elevated] tiles and [Elevated] tiles, with priority [3]"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details A Region can not contain more [simpleTerrain] tiles than [simpleTerrain2] tiles
示例："A Region can not contain more [Elevated] tiles than [Elevated] tiles"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Base Terrain on this tile is not counted for Region determination
此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Starts in regions of this type receive an extra [resource]
示例："Starts in regions of this type receive an extra [Iron]"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Never receives any resources
此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Becomes [terrainName] when adjacent to [terrainFilter]
示例："Becomes [Forest] when adjacent to [Fresh Water]"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Considered [terrainQuality] when determining start locations
示例："Considered [Undesirable] when determining start locations"

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Doesn't generate naturally
此词条自动对用户隐藏。

适用范围：Terrain，Resource

:::
::: details Occurs at temperature between [fraction] and [fraction2] and humidity between [fraction3] and [fraction4]
示例："Occurs at temperature between [0.5] and [0.5] and humidity between [0.5] and [0.5]"

此词条自动对用户隐藏。

适用范围：Terrain，Resource

:::
::: details Occurs in chains at high elevations
此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Occurs in groups around high elevations
此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Every [amount] tiles with this terrain will receive a major deposit of a strategic resource.
示例："Every [3] tiles with this terrain will receive a major deposit of a strategic resource."

此词条自动对用户隐藏。

适用范围：Terrain

:::
::: details Rare feature
适用范围：Terrain

:::
::: details [amount]% Chance to be destroyed by nukes
示例："[3]% Chance to be destroyed by nukes"

适用范围：Terrain

:::
::: details Fresh water
适用范围：Terrain

:::
::: details Rough terrain
适用范围：Terrain

:::
::: details Coastal Water
Marks water tiles as Coast - all other water tiles count as Ocean. These distinctions are relevant e.g. for map generator or the ability to navigate here.

Note that terrain filters do not recognize this distinction, filtering for "Coast" or "Ocean" will only look for a terrain of that name.

Also note that for compatibility reasons, terrains named "Coast" are assuned to have this Unique even if it's missing. This may be removed in a future version.

A tile marked this way marks adjacent land tiles as "Coastal", so they fulfill the terrain filter, and cities built there can build ships, Harbor, etc.

适用范围：Terrain

:::
::: details Excluded from map editor
此词条自动对用户隐藏。

适用范围：Nation，Terrain，Improvement，Resource

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Suppress warning [validationWarning]
Allows suppressing specific validation warnings. Errors, deprecation warnings, or warnings about untyped and non-filtering uniques should be heeded, not suppressed, and are therefore not accepted. Note that this can be used in ModOptions, in the uniques a warning is about, or as modifier on the unique triggering a warning - but you still need to be specific. Even in the modifier case you will need to specify a sufficiently selective portion of the warning text as parameter.

示例："Suppress warning [Tinman is supposed to automatically upgrade at tech Clockwork, and therefore Servos for its upgrade Mecha may not yet be researched! -or- *is supposed to automatically upgrade*]"

此词条不支持条件。

此词条自动对用户隐藏。

适用范围：Triggerable，Terrain，Speed，ModOptions，MetaModifier

:::
## Improvement uniques（地块改良词条）
::: details [stats]
示例："[+1 Gold, +2 Production]"

适用范围：Global，Terrain，Improvement

:::
::: details Consumes [amount] [resource]
示例："Consumes [3] [Iron]"

适用范围：Building，Unit，Improvement

:::
::: details Provides [amount] [resource]
示例："Provides [3] [Iron]"

适用范围：Global，FollowerBelief，Improvement

:::
::: details Costs [amount] [stockpiledResource]
These resources are removed *when work begins* on the construction. Do not confuse with "costs [amount] [stockpiledResource]" (lowercase 'c'), the Unit Action Modifier.

示例："Costs [3] [Mana]"

此词条的效果可被 &lt;(modified by game speed)&gt;

适用范围：Building，Unit，Improvement

:::
::: details Unbuildable
阻止被建造（可能由条件决定）。但仍会出现在菜单中，且可通过金币或信仰等其他方式购买

适用范围：Building，Unit，Improvement

:::
::: details Only available
用于与条件配合，如 "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。也会阻止升级（Upgrade）和转换（Transform）行动。另见 CanOnlyBeBuiltWhen

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Unavailable
用于与条件配合，如 "Unavailable &lt;after generating a Great Prophet&gt;"。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Must be next to [tileFilter]
示例："Must be next to [Farm]"

适用范围：Building，Improvement

:::
::: details Obsolete with [tech]
示例："Obsolete with [Agriculture]"

适用范围：Building，Improvement，Resource

:::
::: details [amount] Sight
示例："[3] Sight"

适用范围：Global，Unit，Terrain，Improvement

:::
::: details Vegetation
适用范围：Terrain，Improvement

:::
::: details Tile provides yield without assigned population
适用范围：Terrain，Improvement

:::
::: details Excluded from map editor
此词条自动对用户隐藏。

适用范围：Nation，Terrain，Improvement，Resource

:::
::: details Can also be built on tiles adjacent to fresh water
适用范围：Improvement

:::
::: details [stats] from [tileFilter] tiles
示例："[+1 Gold, +2 Production] from [Farm] tiles"

适用范围：Improvement

:::
::: details [stats] for each adjacent [tileFilter]
示例："[+1 Gold, +2 Production] for each adjacent [Farm]"

适用范围：Improvement

:::
::: details Ensures a minimum tile yield of [stats]
示例："Ensures a minimum tile yield of [+1 Gold, +2 Production]"

适用范围：Improvement

:::
::: details Can be built outside your borders
适用范围：Improvement

:::
::: details Can be built just outside your borders
适用范围：Improvement

:::
::: details Can only be built on [tileFilter] tiles
示例："Can only be built on [Farm] tiles"

适用范围：Improvement

:::
::: details Cannot be built on [tileFilter] tiles
示例："Cannot be built on [Farm] tiles"

适用范围：Improvement

:::
::: details Can only be built to improve a resource
适用范围：Improvement

:::
::: details Does not need removal of [terrainFeature]
示例："Does not need removal of [Hill]"

适用范围：Improvement

:::
::: details Removes removable features when built
适用范围：Improvement

:::
::: details Gives a defensive bonus of [relativeAmount]%
不接受基于单位的条件

示例："Gives a defensive bonus of [+20]%"

适用范围：Improvement

:::
::: details Costs [amount] [stat] per turn when in your territory
示例："Costs [3] [Culture] per turn when in your territory"

适用范围：Improvement

:::
::: details Costs [amount] [stat] per turn
示例："Costs [3] [Culture] per turn"

适用范围：Improvement

:::
::: details Adjacent enemy units ending their turn take [amount] damage
示例："Adjacent enemy units ending their turn take [3] damage"

适用范围：Improvement

:::
::: details Great Improvement
适用范围：Improvement

:::
::: details Provides a random bonus when entered
适用范围：Improvement

:::
::: details Marks a barbarian camp
当有多种野蛮人营地改良设施可用时，每个新营地随机选择一种。

此词条自动对用户隐藏。

适用范围：Improvement

:::
::: details Unpillagable
适用范围：Improvement

:::
::: details Pillaging this improvement yields approximately [stats]
示例："Pillaging this improvement yields approximately [+1 Gold, +2 Production]"

此词条的效果可被 &lt;(modified by game speed)&gt;

此词条的效果可被 &lt;(modified by game progress up to [relativeAmount]%)&gt;

适用范围：Improvement

:::
::: details Pillaging this improvement yields [stats]
示例："Pillaging this improvement yields [+1 Gold, +2 Production]"

此词条的效果可被 &lt;(modified by game speed)&gt;

此词条的效果可被 &lt;(modified by game progress up to [relativeAmount]%)&gt;

适用范围：Improvement

:::
::: details Destroyed when pillaged
适用范围：Improvement

:::
::: details Irremovable
适用范围：Improvement

:::
::: details Will not be replaced by automated units
适用范围：Improvement

:::
::: details Improves [resourceFilter] resource in this tile
作为资源 improvedBy 字段的替代方案提供。加载游戏时，结果会在资源定义内缓存，且不依赖地形、城市、文明、单位或时间信息。因此，大多数条件不会生效，只有**不**依赖游戏状态的条件可以。

示例："Improves [Strategic] resource in this tile"

此词条不支持条件。

适用范围：Improvement

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## Resource uniques（资源词条）
::: details Obsolete with [tech]
示例："Obsolete with [Agriculture]"

适用范围：Building，Improvement，Resource

:::
::: details Must not be on [amount] largest landmasses
示例："Must not be on [3] largest landmasses"

此词条自动对用户隐藏。

适用范围：Terrain，Resource

:::
::: details Must be on [amount] largest landmasses
示例："Must be on [3] largest landmasses"

此词条自动对用户隐藏。

适用范围：Terrain，Resource

:::
::: details Doesn't generate naturally
此词条自动对用户隐藏。

适用范围：Terrain，Resource

:::
::: details Occurs at temperature between [fraction] and [fraction2] and humidity between [fraction3] and [fraction4]
示例："Occurs at temperature between [0.5] and [0.5] and humidity between [0.5] and [0.5]"

此词条自动对用户隐藏。

适用范围：Terrain，Resource

:::
::: details Excluded from map editor
此词条自动对用户隐藏。

适用范围：Nation，Terrain，Improvement，Resource

:::
::: details Deposits in [tileFilter] tiles always provide [amount] resources
示例："Deposits in [Farm] tiles always provide [3] resources"

适用范围：Resource

:::
::: details Can only be created by Mercantile City-States
适用范围：Resource

:::
::: details Stockpiled
此资源每回合累积，而不是在某一时刻有一组生产者和消费者。当前库存量可通过触发型 unique 影响。

适用范围：Resource

:::
::: details City-level resource
此资源按城市级别计算，而非按文明级别

适用范围：Resource

:::
::: details Cannot be traded
适用范围：Resource

:::
::: details Not shown on world screen
此词条自动对用户隐藏。

适用范围：Promotion，Resource

:::
::: details Generated with weight [amount]
选择此资源的概率为（此资源权重）/（所有合格资源权重之和）。没有 unique 的资源权重为 `1`

示例："Generated with weight [3]"

此词条自动对用户隐藏。

适用范围：Resource

:::
::: details Minor deposits generated with weight [amount]
选择此资源的概率为（此资源权重）/（所有合格资源权重之和）。没有 unique 的资源不会作为小型矿藏生成。

示例："Minor deposits generated with weight [3]"

此词条自动对用户隐藏。

适用范围：Resource

:::
::: details Generated near City States with weight [amount]
选择此资源的概率为（此资源权重）/（所有合格资源权重之和）。只能分配给奢侈品，没有 unique 的资源权重为 `1`

示例："Generated near City States with weight [3]"

此词条自动对用户隐藏。

适用范围：Resource

:::
::: details Special placement during map generation
此词条自动对用户隐藏。

适用范围：Resource

:::
::: details Generated on every [amount] tiles
示例："Generated on every [3] tiles"

此词条自动对用户隐藏。

适用范围：Resource

:::
::: details Guaranteed with Strategic Balance resource option
适用范围：Resource

:::
::: details AI will sell at [amount] Gold
示例："AI will sell at [3] Gold"

此词条自动对用户隐藏。

适用范围：Resource

:::
::: details AI will buy at [amount] Gold
示例："AI will buy at [3] Gold"

此词条自动对用户隐藏。

适用范围：Resource

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## Ruins uniques（遗迹词条）
::: details Only available
用于与条件配合，如 "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。也会阻止升级（Upgrade）和转换（Transform）行动。另见 CanOnlyBeBuiltWhen

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Unavailable
用于与条件配合，如 "Unavailable &lt;after generating a Great Prophet&gt;"。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Free [unit] found in the ruins
示例："Free [Musketman] found in the ruins"

适用范围：Ruins

:::
::: details From a randomly chosen tile [positiveAmount] tiles away from the ruins, reveal tiles up to [positiveAmount2] tiles away with [positiveAmount3]% chance
示例："From a randomly chosen tile [3] tiles away from the ruins, reveal tiles up to [3] tiles away with [3]% chance"

适用范围：Ruins

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## Speed uniques（游戏速度词条）
::: note

速度词条将作为所选游戏速度的 GlobalUniques 的一部分
:::

::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Suppress warning [validationWarning]
Allows suppressing specific validation warnings. Errors, deprecation warnings, or warnings about untyped and non-filtering uniques should be heeded, not suppressed, and are therefore not accepted. Note that this can be used in ModOptions, in the uniques a warning is about, or as modifier on the unique triggering a warning - but you still need to be specific. Even in the modifier case you will need to specify a sufficiently selective portion of the warning text as parameter.

示例："Suppress warning [Tinman is supposed to automatically upgrade at tech Clockwork, and therefore Servos for its upgrade Mecha may not yet be researched! -or- *is supposed to automatically upgrade*]"

此词条不支持条件。

此词条自动对用户隐藏。

适用范围：Triggerable，Terrain，Speed，ModOptions，MetaModifier

:::
## Difficulty uniques（难度词条）
::: note

难度词条将作为所选游戏难度的 GlobalUniques 的一部分
:::

::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## CityState uniques（城邦词条）
::: details Provides military units every ≈[positiveAmount] turns
示例："Provides military units every ≈[3] turns"

适用范围：CityState

:::
::: details Provides a unique luxury
适用范围：CityState

:::
::: details Start bias [terrainFilter]
与 Nation 的 startBias 字段条目效果相同。与 startBias 字段合并；对城邦而言，还与其 CityStateType 上匹配的 unique 合并。条件仅在生成地图/放置起始位置时对 GameInfo 求值（没有 Civilization——它可能只被部分初始化）。不要使用需要地块、城市或单位的条件。

示例："Start bias [Fresh Water]"

适用范围：Nation，CityState

:::
## ModOptions uniques（模组选项词条）
::: details Diplomatic relationships cannot change
此词条不支持条件。

适用范围：ModOptions

:::
::: details Can convert gold to science with sliders
此词条不支持条件。

适用范围：ModOptions

:::
::: details Allow City States to spawn with additional units
此词条不支持条件。

适用范围：ModOptions

:::
::: details Can trade civilization introductions for [positiveAmount] Gold
示例："Can trade civilization introductions for [3] Gold"

此词条不支持条件。

适用范围：ModOptions

:::
::: details Disable religion
此词条不支持条件。

适用范围：ModOptions

:::
::: details Can only start games from the starting era
在这种情况下，'starting era'（开始时代）指整个规则集中定义的第一个时代。

此词条不支持条件。

适用范围：ModOptions

:::
::: details Allow raze capital
此词条不支持条件。

适用范围：ModOptions

:::
::: details Allow raze holy city
此词条不支持条件。

适用范围：ModOptions

:::
::: details Allow cities to claim tiles
此词条不支持条件。

适用范围：ModOptions

:::
::: details City-states search for first city location
By default, city-state settlers with no cities yet found on their current tile when valid (predetermined map-gen / editor start). With this unique they use the same nearby-site search as major civs.

此词条不支持条件。

适用范围：ModOptions

:::
::: details Suppress warning [validationWarning]
Allows suppressing specific validation warnings. Errors, deprecation warnings, or warnings about untyped and non-filtering uniques should be heeded, not suppressed, and are therefore not accepted. Note that this can be used in ModOptions, in the uniques a warning is about, or as modifier on the unique triggering a warning - but you still need to be specific. Even in the modifier case you will need to specify a sufficiently selective portion of the warning text as parameter.

示例："Suppress warning [Tinman is supposed to automatically upgrade at tech Clockwork, and therefore Servos for its upgrade Mecha may not yet be researched! -or- *is supposed to automatically upgrade*]"

此词条不支持条件。

此词条自动对用户隐藏。

适用范围：Triggerable，Terrain，Speed，ModOptions，MetaModifier

:::
::: details Mod is incompatible with [modFilter]
指定你的模组与另一个模组不兼容。始终对称处理，且不能被声明为不兼容的模组覆盖。

示例："Mod is incompatible with [DeCiv Redux]"

此词条不支持条件。

适用范围：ModOptions

:::
::: details Mod requires [modFilter]
指定你的扩展模组仅在匹配过滤器的其他模组激活时可用。

此 unique 的多个副本不能用于指定替代项，它们按 'and' 逻辑工作。如果你需要替代项而通配符无法很好过滤，请提交 issue。

示例："Mod requires [DeCiv Redux]"

此词条不支持条件。

适用范围：ModOptions

:::
::: details Should only be used as permanent audiovisual mod
此词条不支持条件。

适用范围：ModOptions

:::
::: details Can be used as permanent audiovisual mod
此词条不支持条件。

适用范围：ModOptions

:::
::: details Cannot be used as permanent audiovisual mod
此词条不支持条件。

适用范围：ModOptions

:::
::: details Mod preselects map [comment]
仅对包含多张地图的模组有意义。当此模组在新建游戏界面的自定义地图模组下拉框中被选中时，命名地图将在地图下拉框中被选中。同时禁用按最近修改排序的选择。不区分大小写。

示例："Mod preselects map [comment]"

此词条不支持条件。

适用范围：ModOptions

:::
## Event uniques（事件词条）
::: details Only available
用于与条件配合，如 "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。也会阻止升级（Upgrade）和转换（Transform）行动。另见 CanOnlyBeBuiltWhen

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Unavailable
用于与条件配合，如 "Unavailable &lt;after generating a Great Prophet&gt;"。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
## EventChoice uniques（事件选择词条）
::: details Only available
用于与条件配合，如 "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;"。只有满足**全部**条件时才允许建造。也会阻止升级（Upgrade）和转换（Transform）行动。另见 CanOnlyBeBuiltWhen

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details Unavailable
用于与条件配合，如 "Unavailable &lt;after generating a Great Prophet&gt;"。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，Promotion，Improvement，Ruins，Event，EventChoice

:::
::: details [relativeAmount]% weight to this choice for AI decisions
示例："[+20]% weight to this choice for AI decisions"

此词条自动对用户隐藏。

适用范围：Tech，Policy，FounderBelief，FollowerBelief，Building，Promotion，EventChoice

:::
::: details Will not be displayed in Civilopedia
支持只需要 Game 作为上下文的条件。

大多数条件至少需要一个 Civilization，因此**不**会生效。

注意：从主菜单运行文明百科时，条件将被忽略。

此词条自动对用户隐藏。

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
::: details Comment [comment]
允许在 unique 列表中显示任意文本。只有 '[]' 方括号内的文本会显示，其余部分用于让规则集校验识别意图。

示例："Comment [comment]"

适用范围：Nation，Tech，Policy，FounderBelief，FollowerBelief，Building，Unit，UnitType，Promotion，Terrain，Improvement，Resource，Ruins，Speed，Difficulty，EventChoice

:::
## Conditional uniques（有前提的词条）
::: note

可添加到其他词条的修饰符，用于限制其生效时机
:::

::: details &lt;every [positiveAmount] turns&gt;
示例："every [3] turns"

适用范围：Conditional

:::
::: details &lt;before turn number [nonNegativeAmount]&gt;
示例："before turn number [3]"

适用范围：Conditional

:::
::: details &lt;after turn number [nonNegativeAmount]&gt;
示例："after turn number [3]"

适用范围：Conditional

:::
::: details &lt;on [speed] game speed&gt;
示例："on [Quick] game speed"

适用范围：Conditional

:::
::: details &lt;on [difficulty] difficulty&gt;
示例："on [Prince] difficulty"

适用范围：Conditional

:::
::: details &lt;on [difficulty] difficulty or higher&gt;
示例："on [Prince] difficulty or higher"

适用范围：Conditional

:::
::: details &lt;on [difficulty] difficulty or lower&gt;
示例："on [Prince] difficulty or lower"

适用范围：Conditional

:::
::: details &lt;when [victoryType] Victory is enabled&gt;
示例："when [Domination] Victory is enabled"

适用范围：Conditional

:::
::: details &lt;when [victoryType] Victory is disabled&gt;
示例："when [Domination] Victory is disabled"

适用范围：Conditional

:::
::: details &lt;when religion is enabled&gt;
适用范围：Conditional

:::
::: details &lt;when religion is disabled&gt;
适用范围：Conditional

:::
::: details &lt;when espionage is enabled&gt;
适用范围：Conditional

:::
::: details &lt;when espionage is disabled&gt;
适用范围：Conditional

:::
::: details &lt;when nuclear weapons are enabled&gt;
适用范围：Conditional

:::
::: details &lt;when nuclear weapons are disabled&gt;
适用范围：Conditional

:::
::: details &lt;with [nonNegativeAmount]% chance&gt;
示例："with [3]% chance"

适用范围：Conditional

:::
::: details &lt;if tutorials are enabled&gt;
此词条自动对用户隐藏。

适用范围：Conditional

:::
::: details &lt;if tutorial [comment] is completed&gt;
示例："if tutorial [comment] is completed"

此词条自动对用户隐藏。

适用范围：Conditional

:::
::: details &lt;for [civFilter] Civilizations&gt;
示例："for [City-States] Civilizations"

适用范围：Conditional

:::
::: details &lt;when at war&gt;
适用范围：Conditional

:::
::: details &lt;when not at war&gt;
适用范围：Conditional

:::
::: details &lt;during a Golden Age&gt;
适用范围：Conditional

:::
::: details &lt;when not in a Golden Age&gt;
适用范围：Conditional

:::
::: details &lt;during We Love The King Day&gt;
适用范围：Conditional

:::
::: details &lt;while the empire is happy&gt;
适用范围：Conditional

:::
::: details &lt;during the [era]&gt;
示例："during the [Ancient era]"

适用范围：Conditional

:::
::: details &lt;before the [era]&gt;
示例："before the [Ancient era]"

适用范围：Conditional

:::
::: details &lt;starting from the [era]&gt;
示例："starting from the [Ancient era]"

适用范围：Conditional

:::
::: details &lt;if starting in the [era]&gt;
示例："if starting in the [Ancient era]"

适用范围：Conditional

:::
::: details &lt;if no other Civilization has researched this&gt;
适用范围：Conditional

:::
::: details &lt;after discovering [techFilter]&gt;
示例："after discovering [Agriculture]"

适用范围：Conditional

:::
::: details &lt;before discovering [techFilter]&gt;
示例："before discovering [Agriculture]"

适用范围：Conditional

:::
::: details &lt;while researching [techFilter]&gt;
当科技正在被积极研究时（即研究点被投入的科技）满足此条件

示例："while researching [Agriculture]"

适用范围：Conditional

:::
::: details &lt;if no other Civilization has adopted this&gt;
适用范围：Conditional

:::
::: details &lt;if no Civilization has adopted [policy/belief]&gt;
示例："if no Civilization has adopted [Oligarchy]"

适用范围：Conditional

:::
::: details &lt;after adopting [policy/belief]&gt;
示例："after adopting [Oligarchy]"

适用范围：Conditional

:::
::: details &lt;before adopting [policy/belief]&gt;
示例："before adopting [Oligarchy]"

适用范围：Conditional

:::
::: details &lt;before founding a Pantheon&gt;
适用范围：Conditional

:::
::: details &lt;after founding a Pantheon&gt;
适用范围：Conditional

:::
::: details &lt;before founding a religion&gt;
适用范围：Conditional

:::
::: details &lt;after founding a religion&gt;
适用范围：Conditional

:::
::: details &lt;before enhancing a religion&gt;
适用范围：Conditional

:::
::: details &lt;after enhancing a religion&gt;
适用范围：Conditional

:::
::: details &lt;after generating a Great Prophet&gt;
适用范围：Conditional

:::
::: details &lt;if [buildingFilter] is constructed&gt;
示例："if [Culture] is constructed"

适用范围：Conditional

:::
::: details &lt;if [buildingFilter] is not constructed&gt;
示例："if [Culture] is not constructed"

适用范围：Conditional

:::
::: details &lt;if [buildingFilter] is constructed in all [cityFilter] cities&gt;
示例："if [Culture] is constructed in all [in all cities] cities"

适用范围：Conditional

:::
::: details &lt;if [buildingFilter] is constructed in at least [positiveAmount] of [cityFilter] cities&gt;
示例："if [Culture] is constructed in at least [3] of [in all cities] cities"

适用范围：Conditional

:::
::: details &lt;if [buildingFilter] is constructed by anybody&gt;
示例："if [Culture] is constructed by anybody"

适用范围：Conditional

:::
::: details &lt;if [buildingFilter] is not constructed by anybody&gt;
示例："if [Culture] is not constructed by anybody"

适用范围：Conditional

:::
::: details &lt;with [resource]&gt;
示例："with [Iron]"

适用范围：Conditional

:::
::: details &lt;without [resource]&gt;
示例："without [Iron]"

适用范围：Conditional

:::
::: details &lt;when above [amount] [stat/resource]&gt;
Stats 指累积产出，而非每回合产出。因此不支持笑脸——请使用 'when above [amount] Happiness'

示例："when above [3] [Culture]"

此词条的效果可被 &lt;(modified by game speed)&gt;

适用范围：Conditional

:::
::: details &lt;when below [amount] [stat/resource]&gt;
Stats 指累积产出，而非每回合产出。因此不支持笑脸——请使用 'when below [amount] Happiness'

示例："when below [3] [Culture]"

此词条的效果可被 &lt;(modified by game speed)&gt;

适用范围：Conditional

:::
::: details &lt;when between [amount] and [amount2] [stat/resource]&gt;
Stats 指累积产出，而非每回合产出。因此不支持笑脸。'Between'（之间）是包含边界的——所以 'between 1 and 5' 包含 1 和 5。

示例："when between [3] and [3] [Culture]"

此词条的效果可被 &lt;(modified by game speed)&gt;

适用范围：Conditional

:::
::: details &lt;in this city&gt;
适用范围：Conditional

:::
::: details &lt;in [cityFilter] cities&gt;
示例："in [in all cities] cities"

适用范围：Conditional

:::
::: details &lt;in cities connected to the capital&gt;
适用范围：Conditional

:::
::: details &lt;in cities with a [religionFilter] religion&gt;
示例："in cities with a [major] religion"

适用范围：Conditional

:::
::: details &lt;in cities not following a [religionFilter] religion&gt;
示例："in cities not following a [major] religion"

适用范围：Conditional

:::
::: details &lt;in cities with a major religion&gt;
适用范围：Conditional

:::
::: details &lt;in cities with an enhanced religion&gt;
适用范围：Conditional

:::
::: details &lt;in cities following our religion&gt;
适用范围：Conditional

:::
::: details &lt;in cities with a [buildingFilter]&gt;
示例："in cities with a [Culture]"

适用范围：Conditional

:::
::: details &lt;in cities without a [buildingFilter]&gt;
示例："in cities without a [Culture]"

适用范围：Conditional

:::
::: details &lt;in cities with at least [positiveAmount] [populationFilter]&gt;
示例："in cities with at least [3] [Followers of this Religion]"

适用范围：Conditional

:::
::: details &lt;in cities with [nonNegativeAmount] [populationFilter]&gt;
示例："in cities with [3] [Followers of this Religion]"

适用范围：Conditional

:::
::: details &lt;in cities with between [amount] and [amount2] [populationFilter]&gt;
'Between'（之间）是包含边界的——所以 'between 1 and 5' 包含 1 和 5。

示例："in cities with between [3] and [3] [Followers of this Religion]"

适用范围：Conditional

:::
::: details &lt;in cities with less than [amount] [populationFilter]&gt;
示例："in cities with less than [3] [Followers of this Religion]"

适用范围：Conditional

:::
::: details &lt;with a garrison&gt;
适用范围：Conditional

:::
::: details &lt;for [mapUnitFilter] units&gt;
示例："for [Wounded] units"

适用范围：Conditional

:::
::: details &lt;when [mapUnitFilter]&gt;
示例："when [Wounded]"

适用范围：Conditional

:::
::: details &lt;for units with [promotion]&gt;
也适用于带有临时状态的单位

示例："for units with [Shock I]"

适用范围：Conditional

:::
::: details &lt;for units without [promotion]&gt;
也适用于带有临时状态的单位

示例："for units without [Shock I]"

适用范围：Conditional

:::
::: details &lt;vs cities&gt;
适用范围：Conditional

:::
::: details &lt;vs [mapUnitFilter] units&gt;
示例："vs [Wounded] units"

适用范围：Conditional

:::
::: details &lt;vs [combatantFilter]&gt;
示例："vs [City]"

适用范围：Conditional

:::
::: details &lt;when fighting units from a Civilization with more Cities than you&gt;
适用范围：Conditional

:::
::: details &lt;when attacking&gt;
适用范围：Conditional

:::
::: details &lt;when defending&gt;
适用范围：Conditional

:::
::: details &lt;when fighting in [tileFilter] tiles&gt;
示例："when fighting in [Farm] tiles"

适用范围：Conditional

:::
::: details &lt;on foreign continents&gt;
适用范围：Conditional

:::
::: details &lt;when adjacent to a [mapUnitFilter] unit&gt;
示例："when adjacent to a [Wounded] unit"

适用范围：Conditional

:::
::: details &lt;when above [positiveAmount] HP&gt;
示例："when above [3] HP"

适用范围：Conditional

:::
::: details &lt;when below [positiveAmount] HP&gt;
示例："when below [3] HP"

适用范围：Conditional

:::
::: details &lt;when below [positiveAmount] movement&gt;
示例："when below [3] movement"

适用范围：Conditional

:::
::: details &lt;when above [nonNegativeAmount] movement&gt;
示例："when above [3] movement"

适用范围：Conditional

:::
::: details &lt;if it hasn't used other actions yet&gt;
适用范围：Conditional

:::
::: details &lt;when stacked with a [mapUnitFilter] unit&gt;
示例："when stacked with a [Wounded] unit"

适用范围：Conditional

:::
::: details &lt;when not stacked with a [mapUnitFilter] unit&gt;
示例："when not stacked with a [Wounded] unit"

适用范围：Conditional

:::
::: details &lt;with [nonNegativeAmount] to [nonNegativeAmount2] neighboring [tileFilter] tiles&gt;
示例："with [3] to [3] neighboring [Farm] tiles"

适用范围：Conditional

:::
::: details &lt;in [tileFilter] tiles&gt;
示例："in [Farm] tiles"

适用范围：Conditional

:::
::: details &lt;in tiles without [tileFilter]&gt;
示例："in tiles without [Farm]"

适用范围：Conditional

:::
::: details &lt;within [positiveAmount] tiles of a [tileFilter]&gt;
示例："within [3] tiles of a [Farm]"

适用范围：Conditional

:::
::: details &lt;in tiles adjacent to [tileFilter] tiles&gt;
示例："in tiles adjacent to [Farm] tiles"

适用范围：Conditional

:::
::: details &lt;in tiles not adjacent to [tileFilter] tiles&gt;
示例："in tiles not adjacent to [Farm] tiles"

适用范围：Conditional

:::
::: details &lt;on water maps&gt;
适用范围：Conditional

:::
::: details &lt;in [regionType] Regions&gt;
示例："in [Hybrid] Regions"

适用范围：Conditional

:::
::: details &lt;in all except [regionType] Regions&gt;
示例："in all except [Hybrid] Regions"

适用范围：Conditional

:::
::: details &lt;when number of [countable] is equal to [countable2]&gt;
示例："when number of [1000] is equal to [1000]"

适用范围：Conditional

:::
::: details &lt;when number of [countable] is different than [countable2]&gt;
示例："when number of [1000] is different than [1000]"

适用范围：Conditional

:::
::: details &lt;when number of [countable] is more than [countable2]&gt;
示例："when number of [1000] is more than [1000]"

适用范围：Conditional

:::
::: details &lt;when number of [countable] is less than [countable2]&gt;
示例："when number of [1000] is less than [1000]"

适用范围：Conditional

:::
::: details &lt;when number of [countable] is between [countable2] and [countable3]&gt;
'Between'（之间）是包含边界的——所以 'between 1 and 5' 包含 1 和 5。

示例："when number of [1000] is between [1000] and [1000]"

适用范围：Conditional

:::
::: details &lt;when carried by [mapUnitFilter] units&gt;
示例："when carried by [Wounded] units"

适用范围：Conditional

:::
::: details &lt;if [modFilter] is enabled&gt;
示例："if [DeCiv Redux] is enabled"

适用范围：Conditional

:::
::: details &lt;if [modFilter] is not enabled&gt;
示例："if [DeCiv Redux] is not enabled"

适用范围：Conditional

:::
::: details &lt;if [luaFunction] returns true&gt;
将已加载模组中的 Lua 函数作为条件求值；函数收到常规 ctx 表（civ/city/unit/tile/game/parameter…），返回 true 时条件成立。函数应为纯查询——条件会被频繁求值，请保持廉价且无副作用。缺失函数由模组检查器报告。

示例："if [myMod:myFunction] returns true"

适用范围：Conditional

:::
## TriggerCondition uniques（触发条件词条）
::: note

可添加到触发型词条的特殊条件，使它们在特定行动时激活。
:::

::: details &lt;upon discovering [techFilter] technology&gt;
示例："upon discovering [Agriculture] technology"

适用范围：TriggerCondition

:::
::: details &lt;upon entering the [era]&gt;
示例："upon entering the [Ancient era]"

适用范围：TriggerCondition

:::
::: details &lt;upon entering a new era&gt;
适用范围：TriggerCondition

:::
::: details &lt;upon adopting [policy/belief]&gt;
示例："upon adopting [Oligarchy]"

适用范围：TriggerCondition

:::
::: details &lt;upon declaring war on [civFilter] Civilizations&gt;
示例："upon declaring war on [City-States] Civilizations"

适用范围：TriggerCondition

:::
::: details &lt;upon being declared war on by [civFilter] Civilizations&gt;
示例："upon being declared war on by [City-States] Civilizations"

适用范围：TriggerCondition

:::
::: details &lt;upon entering a war with [civFilter] Civilizations&gt;
示例："upon entering a war with [City-States] Civilizations"

适用范围：TriggerCondition

:::
::: details &lt;upon signing a peace treaty with [civFilter] Civilizations&gt;
示例："upon signing a peace treaty with [City-States] Civilizations"

适用范围：TriggerCondition

:::
::: details &lt;upon completing a trade with [civFilter] Civilizations&gt;
当本文明与匹配的文明完成一笔交易（任意一方接受）时触发。注意：所有被接受的交易都会触发，包括 AI 自动接受与征服时签订的开放边界协议。

示例："upon completing a trade with [City-States] Civilizations"

适用范围：TriggerCondition

:::
::: details &lt;upon declaring friendship&gt;
适用范围：TriggerCondition

:::
::: details &lt;upon declaring a defensive pact&gt;
适用范围：TriggerCondition

:::
::: details &lt;upon entering a Golden Age&gt;
适用范围：TriggerCondition

:::
::: details &lt;upon ending a Golden Age&gt;
适用范围：TriggerCondition

:::
::: details &lt;upon conquering a city&gt;
适用范围：TriggerCondition，UnitTriggerCondition

:::
::: details &lt;upon losing a city&gt;
适用范围：TriggerCondition

:::
::: details &lt;upon founding a city&gt;
适用范围：TriggerCondition

:::
::: details &lt;upon building a [improvementFilter] improvement&gt;
示例："upon building a [All Road] improvement"

适用范围：TriggerCondition，UnitTriggerCondition

:::
::: details &lt;upon discovering a Natural Wonder&gt;
适用范围：TriggerCondition

:::
::: details &lt;upon constructing [buildingFilter]&gt;
示例："upon constructing [Culture]"

适用范围：TriggerCondition

:::
::: details &lt;upon constructing [buildingFilter] [cityFilter]&gt;
示例："upon constructing [Culture] [in all cities]"

适用范围：TriggerCondition

:::
::: details &lt;upon gaining a [baseUnitFilter] unit&gt;
示例："upon gaining a [Melee] unit"

适用范围：TriggerCondition

:::
::: details &lt;upon losing a [mapUnitFilter] unit&gt;
示例："upon losing a [Wounded] unit"

适用范围：TriggerCondition

:::
::: details &lt;upon turn end&gt;
适用范围：TriggerCondition，UnitTriggerCondition

:::
::: details &lt;upon turn start&gt;
适用范围：TriggerCondition，UnitTriggerCondition

:::
::: details &lt;upon founding a Pantheon&gt;
适用范围：TriggerCondition

:::
::: details &lt;upon founding a Religion&gt;
适用范围：TriggerCondition

:::
::: details &lt;upon enhancing a Religion&gt;
适用范围：TriggerCondition

:::
::: details &lt;upon expending a [mapUnitFilter] unit&gt;
示例："upon expending a [Wounded] unit"

适用范围：TriggerCondition

:::
## UnitTriggerCondition uniques（单位触发条件词条）
::: note

可添加到单位触发型词条的特殊条件，使它们在特定行动时激活。
:::

::: details &lt;upon conquering a city&gt;
适用范围：TriggerCondition，UnitTriggerCondition

:::
::: details &lt;upon building a [improvementFilter] improvement&gt;
示例："upon building a [All Road] improvement"

适用范围：TriggerCondition，UnitTriggerCondition

:::
::: details &lt;upon turn end&gt;
适用范围：TriggerCondition，UnitTriggerCondition

:::
::: details &lt;upon turn start&gt;
适用范围：TriggerCondition，UnitTriggerCondition

:::
::: details &lt;upon entering combat&gt;
适用范围：UnitTriggerCondition

:::
::: details &lt;upon damaging a [mapUnitFilter] unit&gt;
将第一个参数设为 'Target Unit' 即可对受损单位应用触发效果

示例："upon damaging a [Wounded] unit"

适用范围：UnitTriggerCondition

:::
::: details &lt;upon defeating a [mapUnitFilter] unit&gt;
示例："upon defeating a [Wounded] unit"

适用范围：UnitTriggerCondition

:::
::: details &lt;upon capturing a [mapUnitFilter] unit&gt;
示例："upon capturing a [Wounded] unit"

适用范围：UnitTriggerCondition

:::
::: details &lt;upon being captured&gt;
适用范围：UnitTriggerCondition

:::
::: details &lt;upon intercepting a [mapUnitFilter] unit&gt;
示例："upon intercepting a [Wounded] unit"

适用范围：UnitTriggerCondition

:::
::: details &lt;upon being intercepted&gt;
适用范围：UnitTriggerCondition

:::
::: details &lt;upon being defeated&gt;
适用范围：UnitTriggerCondition

:::
::: details &lt;upon being promoted&gt;
适用范围：UnitTriggerCondition

:::
::: details &lt;upon gaining the [promotion] promotion&gt;
示例："upon gaining the [Shock I] promotion"

适用范围：UnitTriggerCondition

:::
::: details &lt;upon losing the [promotion] promotion&gt;
示例："upon losing the [Shock I] promotion"

适用范围：UnitTriggerCondition

:::
::: details &lt;upon gaining the [promotion] status&gt;
示例："upon gaining the [Shock I] status"

适用范围：UnitTriggerCondition

:::
::: details &lt;upon losing the [promotion] status&gt;
示例："upon losing the [Shock I] status"

适用范围：UnitTriggerCondition

:::
::: details &lt;upon losing at least [positiveAmount] HP in a single attack&gt;
示例："upon losing at least [3] HP in a single attack"

适用范围：UnitTriggerCondition

:::
::: details &lt;upon ending a turn in a [tileFilter] tile&gt;
示例："upon ending a turn in a [Farm] tile"

适用范围：UnitTriggerCondition

:::
::: details &lt;upon discovering a [tileFilter] tile&gt;
示例："upon discovering a [Farm] tile"

适用范围：UnitTriggerCondition

:::
::: details &lt;upon entering a [tileFilter] tile&gt;
示例："upon entering a [Farm] tile"

适用范围：UnitTriggerCondition

:::
## UnitActionModifier uniques（单位行为修饰词条）
::: note

可作为条件添加到单位行动词条的修饰符
:::

::: details &lt;by consuming this unit&gt;
适用范围：UnitActionModifier

:::
::: details &lt;for [amount] movement&gt;
执行时最多消耗 [amount] 移动力

示例："for [3] movement"

适用范围：UnitActionModifier

:::
::: details &lt;for all movement&gt;
执行时消耗全部移动力

适用范围：UnitActionModifier

:::
::: details &lt;requires [nonNegativeAmount] movement&gt;
执行需要 [nonNegativeAmount] 移动力。单位的移动力向上取整

示例："requires [3] movement"

适用范围：UnitActionModifier

:::
::: details &lt;costs [stats] stats&gt;
正整数将从你的库存中扣除。食物和产能将从最近城市的当前库存中移除

示例："costs [+1 Gold, +2 Production] stats"

适用范围：UnitActionModifier

:::
::: details &lt;costs [amount] [stockpiledResource]&gt;
正整数将从你的库存中扣除。不要与改良设施、建筑和单位上的 "Costs [amount] [stockpiledResource]"（大写 'C'）混淆。

示例："costs [3] [Mana]"

适用范围：UnitActionModifier

:::
::: details &lt;removing the [promotion] promotion/status&gt;
从单位移除该晋升/状态——这不是代价，即使没有该晋升/状态，单位也能激活此行动。如需限制，请使用 &lt;with the [promotion] promotion&gt; 条件

示例："removing the [Shock I] promotion/status"

适用范围：UnitActionModifier

:::
::: details &lt;once&gt;
适用范围：UnitActionModifier

:::
::: details &lt;[positiveAmount] times&gt;
示例："[3] times"

适用范围：UnitActionModifier

:::
::: details &lt;[nonNegativeAmount] additional time(s)&gt;
示例："[3] additional time(s)"

适用范围：UnitActionModifier

:::
::: details &lt;after which this unit is consumed&gt;
适用范围：UnitActionModifier

:::
::: details &lt;with [amount] priority&gt;
此行动的使用频率，值越高表示越常用，且应放在更靠前的页面。100 表示非常频繁，50 表示比较频繁，小于 25 表示多回合移动时按一次。如果按钮像是添加到首都、晋升之类，&gt; 100 也是可能的——我们需要告知玩家采取行动是一个选项。

示例："with [3] priority"

此词条自动对用户隐藏。

适用范围：UnitActionModifier，MetaModifier

:::
## MetaModifier uniques（元修饰词条）
::: note

可添加到其他词条的修饰符，改变用户体验而非行为
:::

::: details &lt;for [nonNegativeAmount] turns&gt;
将此 unique 变为触发器，作为*全局* unique 激活一定回合数

示例："for [3] turns"

适用范围：MetaModifier

:::
::: details &lt;with [amount] priority&gt;
此行动的使用频率，值越高表示越常用，且应放在更靠前的页面。100 表示非常频繁，50 表示比较频繁，小于 25 表示多回合移动时按一次。如果按钮像是添加到首都、晋升之类，&gt; 100 也是可能的——我们需要告知玩家采取行动是一个选项。

示例："with [3] priority"

此词条自动对用户隐藏。

适用范围：UnitActionModifier，MetaModifier

:::
::: details &lt;hidden from users&gt;
适用范围：MetaModifier

:::
::: details &lt;for every [countable]&gt;
仅适用于正数

示例："for every [1000]"

适用范围：MetaModifier

:::
::: details &lt;for every adjacent [tileFilter]&gt;
仅适用于正数

示例："for every adjacent [Farm]"

适用范围：MetaModifier

:::
::: details &lt;for every [positiveAmount] [countable]&gt;
仅适用于正数

示例："for every [3] [1000]"

适用范围：MetaModifier

:::
::: details &lt;(modified by game speed)&gt;
只能应用于特定 unique，具体见每个 unique 的详细说明

适用范围：MetaModifier

:::
::: details &lt;(modified by game progress up to [relativeAmount]%)&gt;
只能应用于特定 unique，具体见每个 unique 的详细说明

示例："(modified by game progress up to [+20]%)"

适用范围：MetaModifier

:::
::: details &lt;Civilopedia link [pediaLink]&gt;
允许 unique 在文明百科中正常列出时链接到任意文明百科页面。这会覆盖对 unique 参数中对象的自动链接。

示例："Civilopedia link [Units/Settler]"

此词条自动对用户隐藏。

适用范围：MetaModifier

:::
::: details &lt;Suppress warning [validationWarning]&gt;
Allows suppressing specific validation warnings. Errors, deprecation warnings, or warnings about untyped and non-filtering uniques should be heeded, not suppressed, and are therefore not accepted. Note that this can be used in ModOptions, in the uniques a warning is about, or as modifier on the unique triggering a warning - but you still need to be specific. Even in the modifier case you will need to specify a sufficiently selective portion of the warning text as parameter.

示例："Suppress warning [Tinman is supposed to automatically upgrade at tech Clockwork, and therefore Servos for its upgrade Mecha may not yet be researched! -or- *is supposed to automatically upgrade*]"

此词条不支持条件。

此词条自动对用户隐藏。

适用范围：Triggerable，Terrain，Speed，ModOptions，MetaModifier

:::

## Unique 参数类型

| 参数 | 说明 |
|---|---|
| `amount` | This indicates a whole number, possibly with a + or - sign, such as `2`, `+13`, or `-3`. Also accepts Countable expressions. |
| `baseTerrain` | The name of any terrain that is a base terrain according to the json file. |
| `belief` | The name of any belief. |
| `beliefType` | 'Pantheon', 'Follower', 'Founder' or 'Enhancer' |
| `buildingName` | The name of any building. |
| `civWideStat` | All the following stats have civ-wide fields: `Gold`, `Science`, `Culture`, `Faith`. |
| `combatantFilter` | This indicates a combatant, which can either be a unit or a city (when bombarding). Must either be `City` or a `mapUnitFilter`. |
| `costOrStrength` | `Cost` or `Strength`. |
| `countable` | This indicates a number or a numeric variable.They can be tested in the developer console with `civ checkcountable` - for example, `civ checkcountable "[Iron]+2"`. |
| `difficulty` | The name of any difficulty. |
| `era` | The name of any era. |
| `eraFilter` | The name of an era, `any era`, `Starting Era`, `pre-[era]`, `post-[era]`. |
| `event` | The name of any event. |
| `foundingOrEnhancing` | Prophet Action Filters. |
| `fraction` | Indicates a fractional number, which can be negative. Also accepts Countable expressions. |
| `improvementName` | The name of any improvement excluding 'Cancel improvement order' |
| `leaderTitle` | Leader Title |
| `luaFunction` | A Lua function reference in the form [modName:]functionName. |
| `modFilter` | 模组名，区分大小写，或首尾为星号的简单通配符过滤器，不区分大小写。<br>注意必须使用 Unciv 显示的模组名，而不是仓库名。<br>存在影响连字符及首尾空格的转换，请务必不要混淆。 |
| `nonNegativeAmount` | This indicates a non-negative whole number, larger than or equal to zero, a '+' sign is optional. Also accepts Countable expressions. |
| `pediaLink` | Unique Specials. |
| `policy` | The name of any policy. |
| `policyFilter` | The name of any policy, a filtering Unique, any branch (matching only the branch itself), a branch name with " Completed" appended (matches if the branch is completed), or a policy branch as `[branchName] branch` (matching all policies in that branch). |
| `positiveAmount` | This indicates a positive whole number, larger than zero, a '+' sign is optional. Also accepts Countable expressions. |
| `promotion` | The name of any promotion. |
| `relativeAmount` | This indicates a number, usually with a + or - sign, such as `+25` (this kind of parameter is often followed by '%' which is nevertheless not part of the value). Also accepts Countable expressions. |
| `resource` | The name of any resource. |
| `resourceFilter` | A resource name, type, 'all', or a Stat listed in the resource's improvementStats. |
| `specialist` | The name of any specialist. |
| `speed` | The name of any speed. |
| `spyAction` | A spy action display name, e.g. `Counter-intelligence`, `Stealing Tech`. |
| `stat` | This is one of the 7 major stats in the game - `Gold`, `Science`, `Production`, `Food`, `Happiness`, `Culture` and `Faith`. Note that the stat names need to be capitalized! |
| `stats` | For example: `+2 Production, +3 Food`. Note that the stat names need to be capitalized! |
| `stockpile` | The name of any stockpiled resource. |
| `stockpiledResource` | The name of any stockpiled resource. |
| `tech` | The name of any tech. |
| `terrainFeature` | The name of any terrain that is a terrain feature according to the json file. |
| `tileFilter` | Tile Filters |
| `unitNameGroup` | The name of a unit name group found in UnitNameGroups.json, or one of their unique tags. |
| `unitTriggerTarget` | `This Unit` or `Target Unit`. |
| `unitType` | Unit Type Filters. |
| `validationWarning` | Mod-check warning |
| `victoryType` | The name of any victory type: 'Cultural', 'Diplomatic', 'Domination', 'Scientific', 'Time' or one of your mod's VictoryTypes.json names. |
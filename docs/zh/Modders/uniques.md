---
title: Unique 能力列表
---

<!-- 本文件由 desktop/src/com/unciv/app/desktop/UniqueDocsWriter.kt 自动生成，请勿手动编辑 -->

# Unique 能力列表

> 本列表由游戏代码自动生成，随版本保持最新。
> Uniques 概述可以在[这里](../Developers/Uniques.md)找到。
> 简单的 Unique 参数通过悬浮提示说明，复杂的参数在 [Unique 参数类型](Unique-parameters.md) 中说明。

## 触发型词条
::: note

    具有即时、一次性效果的词条。可添加到科技（研究时触发）、政策（采用时触发）、时代（到达时触发）、建筑（建造时触发）；或为其添加触发条件（TriggerCondition），使其成为在特定事件时激活的全局词条。也可添加到单位，赋予其将效果作为行动触发的能力（可用 UnitActionModifier / UnitTriggerCondition 条件修饰）。
:::

::: details Gain a free [buildingName] [cityFilter]
	Free buildings CANNOT be self-removing - this leads to an endless loop of trying to add the building

	示例："Gain a free [Library] [in all cities]"

	适用范围：触发型，全球

:::
::: details 移除[cityFilter]城市的[buildingFilter]
	示例："移除[Culture]城市的[in all cities]"

	适用范围：触发型，全球

:::
::: details 拆毁[cityFilter]的[buildingFilter](返还少量金钱)
	示例："拆毁[Culture]的[in all cities](返还少量金钱)"

	适用范围：触发型，全球

:::
::: details 免费的[unit]出现
	示例："免费的[Musketman]出现"

	适用范围：触发型

:::
::: details [positiveAmount]个免费的[unit]出现
	示例："[3]个免费的[Musketman]出现"

	适用范围：触发型

:::
::: details 一个[unit]反叛
	示例："一个[Musketman]反叛"

	适用范围：触发型

:::
::: details [positiveAmount][unit]反叛
	示例："[3][Musketman]反叛"

	适用范围：触发型

:::
::: details 推行1项免费社会政策
	适用范围：触发型

:::
::: details 免费获得[positiveAmount]个社会政策
	示例："免费获得[3]个社会政策"

	适用范围：触发型

:::
::: details 帝国进入黄金时代
	适用范围：触发型

:::
::: details  帝国进入[positiveAmount]回合的黄金时代 
	示例：" 帝国进入[3]回合的黄金时代 "

	适用范围：触发型

:::
::: details 免费的伟人
	适用范围：触发型

:::
::: details [cityFilter][amount]人口
	示例："[3][in all cities]人口"

	适用范围：触发型

:::
::: details 在一个随机城市中增加[amount]人口
	示例："在一个随机城市中增加[3]人口"

	适用范围：触发型

:::
::: details 发现[tech]
	示例："发现[Agriculture]"

	适用范围：触发型

:::
::: details 立刻推行[policy/belief]
	示例："立刻推行[Oligarchy]"

	适用范围：触发型

:::
::: details 取消推行[policyFilter]
	示例："取消推行[Oligarchy]"

	适用范围：触发型

:::
::: details 取消推行[policyFilter]并返还[amount]%的文化花费
	示例："取消推行[Oligarchy]并返还[3]%的文化花费"

	适用范围：触发型

:::
::: details 获得1项免费科技
	适用范围：触发型

:::
::: details 获得[positiveAmount]项免费科技
	示例："获得[3]项免费科技"

	适用范围：触发型

:::
::: details 免费获得[eraFilter]的[positiveAmount]个可研究科技
	示例："免费获得[3]的[Ancient era]个可研究科技"

	适用范围：触发型

:::
::: details 揭示全图
	适用范围：触发型

:::
::: details 获得免费的[beliefType]信仰
	示例："获得免费的[Follower]信仰"

	适用范围：触发型

:::
::: details 发起外交胜利投票
	适用范围：触发型

:::
::: details 立即消耗[positiveAmount][stockpiledResource]
	示例："立即消耗[3][Mana]"

	适用范围：触发型

:::
::: details 立即提供[positiveAmount][stockpiledResource]
	示例："立即提供[3][Mana]"

	适用范围：触发型

:::
::: details 设定[stockpile]为[countable]
	示例："设定[Mana]为[1000]"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	适用范围：触发型

:::
::: details  立即获得 [amount] [stockpile]
	示例：" 立即获得 [3] [Mana]"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	适用范围：触发型

:::
::: details 获得[amount][stat]
	示例："获得[3][Culture]"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	适用范围：触发型

:::
::: details 获得[amount]-[amount2][stat]
	示例："获得[3]-[3][Culture]"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	适用范围：触发型

:::
::: details 为万神殿获得足够的信仰
	适用范围：触发型

:::
::: details 获得足够一个大先知的[positiveAmount]%的信仰
	示例："获得足够一个大先知的[3]%的信仰"

	适用范围：触发型

:::
::: details 获得[tech][relativeAmount]%的进度
	示例："获得[+20][Agriculture]%的进度"

	适用范围：触发型

:::
::: details 获得以[tileFilter]为中心[nonNegativeAmount]格半径的地块
	示例："获得以[Farm]为中心[3]格半径的地块"

	适用范围：触发型

:::
::: details 获得对[cityFilter][positiveAmount]格领土的控制权
	示例："获得对[3][in all cities]格领土的控制权"

	适用范围：触发型

:::
::: details 在[positiveAmount]格半径内最多显示[positiveAmount/'all'][tileFilter]
	示例："在[3]格半径内最多显示[Farm][3]"

	适用范围：触发型

:::
::: details 触发下列全局警报：[comment]
	Supported on Policies and Technologies.

	For other targets, the generated Notification may not read nicely, and will likely not support translation. Reason: Your [comment] gets a generated introduction, other triggers usually notify _you_, not _others_, and that difference is currently handled by mapping text.

	Conditionals evaluate in the context of the civilization having the Unique, not the recipients of the alerts.

	示例："触发下列全局警报：[comment]"

	适用范围：触发型

:::
::: details 令所有间谍升[positiveAmount]级
	示例："令所有间谍升[3]级"

	适用范围：触发型

:::
::: details 获得1个间谍
	适用范围：触发型

:::
::: details 将此地块转变为[terrainName]
	示例："将此地块转变为[Forest]"

	适用范围：触发型

:::
::: details Add [resource] to this tile
	示例："Add [Iron] to this tile"

	适用范围：触发型

:::
::: details 从该地块移除[resourceFilter]资源
	示例："从该地块移除[Strategic]资源"

	适用范围：触发型

:::
::: details 从该地块移除[improvementFilter]地块改良
	示例："从该地块移除[All Road]地块改良"

	适用范围：触发型

:::
::: details [mapUnitFilter] units gain the [promotion] promotion
	Works only with promotions that are valid for the unit's type - or for promotions that do not specify any.

	示例："[Wounded] units gain the [Shock I] promotion"

	适用范围：触发型

:::
::: details 前[positiveAmount]座城市免费获得最便宜的[stat]建筑
	示例："前[Culture]座城市免费获得最便宜的[3]建筑"

	适用范围：触发型

:::
::: details 前[positiveAmount]座城市免费获得一座[buildingName]
	示例："前[Library]座城市免费获得一座[3]"

	适用范围：触发型

:::
::: details 触发事件：[event]
	示例："触发事件：[Inspiration]"

	适用范围：触发型

:::
::: details Trigger the function [luaFunction] with [comment]
	示例："Trigger the function [myMod:myFunction] with [comment]"

	适用范围：触发型

:::
::: details Mark tutorial [comment] complete
	示例："Mark tutorial [comment] complete"

	此词条不支持条件。

	此词条自动对用户隐藏。

	适用范围：触发型

:::
::: details Play [comment] sound
	See [Images and Audio](Images-and-Audio.md#sounds) for a list of available sounds.

	示例："Play [comment] sound"

	此词条自动对用户隐藏。

	适用范围：触发型

:::
::: details Get the leader title of [leaderTitle]
	示例："Get the leader title of [Sovereign [leaderName] the Great]"

	此词条自动对用户隐藏。

	适用范围：触发型

:::
::: details Choose a music track for [param], [param2], [param3]
	Parameters are unchecked, strings not matching existing tracks or flags are ignored.

	See [Context-sensitive music](Images-and-Audio.md#context-sensitive-music-overview)

	The first parameter is the track name prefix, e.g. a Civilization name or "this civ".

	The second parameter is a list of zero or more suffixes, comma-separated, used to specify a "mood", like Peace, War, Ambient etc. First track that matches wins.

	The third parameter is a list of zero or more flags: PrefixMustMatch, SuffixMustMatch, SlowFade, PlaySingle, PlayDefaultFile.

	示例："Choose a music track for [Unknown], [Unknown], [Unknown]"

	此词条自动对用户隐藏。

	适用范围：触发型

:::
::: details Suppress warning [validationWarning]
	Allows suppressing specific validation warnings. Errors, deprecation warnings, or warnings about untyped and non-filtering uniques should be heeded, not suppressed, and are therefore not accepted. Note that this can be used in ModOptions, in the uniques a warning is about, or as modifier on the unique triggering a warning - but you still need to be specific. Even in the modifier case you will need to specify a sufficiently selective portion of the warning text as parameter.

	示例："Suppress warning [Tinman is supposed to automatically upgrade at tech Clockwork, and therefore Servos for its upgrade Mecha may not yet be researched! -or- *is supposed to automatically upgrade*]"

	此词条不支持条件。

	此词条自动对用户隐藏。

	适用范围：触发型，地形修正，游戏速度，模组选项，元修饰

:::
## 单位触发型词条
::: note

    对单位产生即时、一次性效果的词条。可添加到单位（单位、单位类型或晋升），赋予其将效果作为行动触发的能力（可用 UnitActionModifier / UnitTriggerCondition 条件修饰）。
:::

::: details [unitTriggerTarget]恢复[positiveAmount]生命值
	示例："[This Unit]恢复[3]生命值"

	适用范围：单位触发型

:::
::: details [unitTriggerTarget]受到[positiveAmount]伤害
	示例："[This Unit]受到[3]伤害"

	适用范围：单位触发型

:::
::: details [unitTriggerTarget]获得[amount]经验值
	示例："[This Unit]获得[3]经验值"

	适用范围：单位触发型

:::
::: details [unitTriggerTarget]免费升级
	示例："[This Unit]免费升级"

	适用范围：单位触发型

:::
::: details [unitTriggerTarget]免费升级(含特色单位升级)
	示例："[This Unit]免费升级(含特色单位升级)"

	适用范围：单位触发型

:::
::: details [unitTriggerTarget]获得[promotion]晋升项
	示例："[This Unit]获得[Shock I]晋升项"

	适用范围：单位触发型

:::
::: details [unitTriggerTarget]失去[promotion]晋升项
	示例："[This Unit]失去[Shock I]晋升项"

	适用范围：单位触发型

:::
::: details [unitTriggerTarget]获得[positiveAmount]移动力
	示例："[This Unit]获得[3]移动力"

	适用范围：单位触发型

:::
::: details [unitTriggerTarget]失去[positiveAmount]移动力
	示例："[This Unit]失去[3]移动力"

	适用范围：单位触发型

:::
::: details [unitTriggerTarget]获得[promotion]临时晋升项(持续[positiveAmount]回合)
	Statuses are temporary promotions. They do not stack, and reapplying a specific status take the highest number - so reapplying a 3-turn on a 1-turn makes it 3, but doing the opposite will have no effect. Turns left on the status decrease at the *start of turn*, so bonuses applied for 1 turn are stll applied during other civ's turns.

	示例："[This Unit]获得[Shock I]临时晋升项(持续[3]回合)"

	适用范围：单位触发型

:::
::: details [unitTriggerTarget]失去[promotion]临时晋升项
	示例："[This Unit]失去[Shock I]临时晋升项"

	适用范围：单位触发型

:::
::: details [unitTriggerTarget]被消灭
	示例："[This Unit]被消灭"

	适用范围：单位触发型

:::
::: details [unitTriggerTarget]从[unitNameGroup]名称库中获得一个名字
	示例："[This Unit]从[Scientist]名称库中获得一个名字"

	适用范围：单位触发型

:::
## 全球词条
::: note

    全局生效的词条。文明从国家词条、已到达的时代、已研究的科技、已采用的政策、已建造的建筑、宗教「创始人」词条、拥有的资源以及规则集全局词条中获得这些能力。
:::

::: details [stats]
	示例："[+1 Gold, +2 Production]"

	适用范围：全球，地形修正，地块改良

:::
::: details [cityFilter][stats]
	示例："[+1 Gold, +2 Production][in all cities]"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]中的每个专业人员[stats]
	示例："[+1 Gold, +2 Production]中的每个专业人员[in all cities]"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]每[positiveAmount]人口[stats]
	示例："[+1 Gold, +2 Production]每[3]人口[in all cities]"

	适用范围：全球，追随者信仰

:::
::: details 每推行[positiveAmount]个政策[stats]
	Only works for civ-wide stats

	示例："每推行[+1 Gold, +2 Production]个政策[3]"

	适用范围：全球

:::
::: details  每[positiveAmount][civWideStat][stats]
	示例：" 每[+1 Gold, +2 Production][3][Gold]"

	适用范围：全球

:::
::: details 坐落于[terrainFilter]的城市[stats]
	示例："坐落于[+1 Gold, +2 Production]的城市[Fresh Water]"

	适用范围：全球，追随者信仰

:::
::: details 所有[buildingFilter]建筑[stats]
	示例："所有[+1 Gold, +2 Production]建筑[Culture]"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]的[tileFilter]地块[stats]
	示例："[+1 Gold, +2 Production]的[Farm]地块[in all cities]"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]除[tileFilter2]地块之外的[tileFilter]的地块[stats]
	示例："[+1 Gold, +2 Production]除[Farm]地块之外的[Farm]的地块[in all cities]"

	适用范围：全球，追随者信仰

:::
::: details [stats] from every [tileFilter/specialist/buildingFilter]
	示例："[+1 Gold, +2 Production] from every [Farm]"

	适用范围：全球，追随者信仰

:::
::: details 每条贸易路线 [stats]
	示例："每条贸易路线 [+1 Gold, +2 Production]"

	适用范围：全球，追随者信仰

:::
::: details [relativeAmount]% [stat]
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20]% [Culture]"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter][relativeAmount]%[stat]
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20][Culture]%[in all cities]"

	适用范围：全球，追随者信仰

:::
::: details [relativeAmount]% [stat] 来自每个 [tileFilter/buildingFilter]
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20]% [Culture] 来自每个 [Farm]"

	适用范围：全球，追随者信仰

:::
::: details [relativeAmount]%来自每个[tileFilter/buildingFilter]的产出
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20]%来自每个[Farm]的产出"

	适用范围：全球，追随者信仰

:::
::: details 来自城邦的[stat][relativeAmount]%
	示例："来自城邦的[+20][Culture]%"

	适用范围：全球

:::
::: details 来自贸易路线的[stat][relativeAmount]%
	示例："来自贸易路线的[+20][Culture]%"

	适用范围：全球

:::
::: details 消除[cityFilter]的[stat]
	示例："消除[Culture]的[in all cities]"

	适用范围：全球

:::
::: details 停止[cityFilter]中的人口增长
	示例："停止[in all cities]中的人口增长"

	适用范围：全球

:::
::: details [cityFilter] 建造[buildingFilter]建筑 时 [relativeAmount]%产能
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20] 建造[Culture]建筑 时 [in all cities]%产能"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter] 建造[baseUnitFilter]单位 时 [relativeAmount]%产能
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20] 建造[Melee]单位 时 [in all cities]%产能"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter] 建造[buildingFilter]奇观 时 [relativeAmount]%产能
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20] 建造[Culture]奇观 时 [in all cities]%产能"

	适用范围：全球，追随者信仰

:::
::: details 当所有在首都已建成的建筑在其他城市建造时 [relativeAmount]% 产能
	示例："当所有在首都已建成的建筑在其他城市建造时 [+20]% 产能"

	适用范围：全球，追随者信仰

:::
::: details 掠夺地块的收益[relativeAmount]%
	示例："掠夺地块的收益[+20]%"

	适用范围：全球，单位

:::
::: details 掠夺地块的血量恢复[relativeAmount]%
	示例："掠夺地块的血量恢复[+20]%"

	适用范围：全球，单位

:::
::: details 城邦赠送的军事单位起始拥有[positiveAmount]经验
	示例："城邦赠送的军事单位起始拥有[3]经验"

	适用范围：全球

:::
::: details 与共同的敌人交战时，军事型城邦提供单位的频率为平时的[positiveAmount]倍。
	示例："与共同的敌人交战时，军事型城邦提供单位的频率为平时的[3]倍。"

	适用范围：全球

:::
::: details 赠与城邦金钱提升的影响力+[relativeAmount]%
	示例："赠与城邦金钱提升的影响力+[+20]%"

	适用范围：全球

:::
::: details 可以花费金钱来吞并或傀儡一个已经连续[nonNegativeAmount]回合成为你盟友的城邦
	示例："可以花费金钱来吞并或傀儡一个已经连续[3]回合成为你盟友的城邦"

	适用范围：全球

:::
::: details 城邦领土始终视为友好领土
	适用范围：全球

:::
::: details 附属城邦随机赠送伟人
	适用范围：全球

:::
::: details 对城邦影响减少成度增加[relativeAmount]% 
	示例："对城邦影响减少成度增加[+20]% "

	适用范围：全球

:::
::: details 对所有城邦的影响力的基准值[amount]
	示例："对所有城邦的影响力的基准值[3]"

	适用范围：全球

:::
::: details 附属城邦提供自身[stat]的[relativeAmount]%
	示例："附属城邦提供自身[Culture]的[+20]%"

	适用范围：全球

:::
::: details [relativeAmount]%城邦赠送的资源
	示例："[+20]%城邦赠送的资源"

	适用范围：全球

:::
::: details [relativeAmount]% 城邦赠送的奢侈资源带来的快乐
	示例："[+20]% 城邦赠送的奢侈资源带来的快乐"

	适用范围：全球

:::
::: details 对城邦的影响力恢复速度是正常水平的两倍
	适用范围：全球

:::
::: details [cityFilter][relativeAmount]%人口增长
	示例："[+20][in all cities]%人口增长"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]在人口增长后人口增长所需食物减少[amount]%
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[3]在人口增长后人口增长所需食物减少[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]中[populationFilter]消耗的食物[relativeAmount]%
	示例："[+20]中[Followers of this Religion]消耗的食物[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details 城市数量造成的不快乐[relativeAmount]%
	示例："城市数量造成的不快乐[+20]%"

	适用范围：全球

:::
::: details [cityFilter][populationFilter]造成的不快乐[relativeAmount]%
	示例："[+20][Followers of this Religion]造成的不快乐[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details [amount]幸福度来源于每一种奢侈品资源
	示例："[3]幸福度来源于每一种奢侈品资源"

	适用范围：全球

:::
::: details 每种奢侈资源即使全部交易出去仍可保留 [relativeAmount]% 快乐奖励
	示例："每种奢侈资源即使全部交易出去仍可保留 [+20]% 快乐奖励"

	适用范围：全球

:::
::: details [relativeAmount]%富余的快乐转化为[stat]
	示例："[+20]%富余的快乐转化为[Culture]"

	适用范围：全球

:::
::: details 无法建造[baseUnitFilter]单位
	示例："无法建造[Melee]单位"

	适用范围：全球

:::
::: details 允许建造太空飞船的部件
	适用范围：全球

:::
::: details [cityFilter]可以以不断上涨的价格([nonNegativeAmount])用[amount][stat]购买[baseUnitFilter]单位
	示例："[Melee]可以以不断上涨的价格([3])用[Culture][in all cities]购买[3]单位"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]可以以不断上涨的价格([nonNegativeAmount])用[amount][stat]购买[buildingFilter]建筑
	示例："[Culture]可以以不断上涨的价格([3])用[Culture][in all cities]购买[3]建筑"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]可以用[nonNegativeAmount][stat]购买[baseUnitFilter]单位
	示例："[Melee]可以用[3][Culture]购买[in all cities]单位"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]中可以用[nonNegativeAmount][stat]购买[buildingFilter]建筑
	示例："[Culture]中可以用[3][Culture]购买[in all cities]建筑"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]可以用[stat]购买[baseUnitFilter]单位
	示例："[Melee]可以用[Culture]购买[in all cities]单位"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]可以用[stat]购买[buildingFilter]建筑
	示例："[Culture]可以用[Culture]购买[in all cities]建筑"

	适用范围：全球，追随者信仰

:::
::: details 可以用[stat]购买[baseUnitFilter]单位，价格为其正常生产成本的[nonNegativeAmount]倍
	示例："可以用[Melee]购买[Culture]单位，价格为其正常生产成本的[3]倍"

	适用范围：全球，追随者信仰

:::
::: details 可以用[nonNegativeAmount]倍于其正常的生产花费的[stat]购买[buildingFilter]建筑
	示例："可以用[Culture]倍于其正常的生产花费的[Culture]购买[3]建筑"

	适用范围：全球，追随者信仰

:::
::: details 在城市建造/组建单位时的[stat]花费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："在城市建造/组建单位时的[Culture]花费[+20]%"

	适用范围：全球，追随者信仰

:::
::: details 购买[buildingFilter]建筑的[stat]花费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："购买[Culture]建筑的[Culture]花费[+20]%"

	适用范围：全球，追随者信仰

:::
::: details [baseUnitFilter]单位的[stat]花费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："[Culture]单位的[Melee]花费[+20]%"

	适用范围：全球，追随者信仰

:::
::: details 允许在城市中将产能转化成[stat]
	示例："允许在城市中将产能转化成[Culture]"

	适用范围：全球

:::
::: details 城市中向[stat]的产能转化[relativeAmount]%
	示例："城市中向[Culture]的产能转化[+20]%"

	适用范围：全球

:::
::: details 提高道路上的移动速度
	适用范围：全球

:::
::: details 道路/铁路可跨河建造
	适用范围：全球

:::
::: details 道路/铁路维护费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："道路/铁路维护费[+20]%"

	适用范围：全球

:::
::: details 在[tileFilter]地块的改良无维修费用
	示例："在[Farm]地块的改良无维修费用"

	适用范围：全球

:::
::: details 对 [improvementFilter] 地块改良 [relativeAmount]% 的建造时间
	示例："对 [+20] 地块改良 [All Road]% 的建造时间"

	适用范围：全球，单位

:::
::: details 可以以[relativeAmount]%的速度建造[improvementFilter]地块改进改良。
	示例："可以以[All Road]%的速度建造[+20]地块改进改良。"

	适用范围：全球，单位

:::
::: details Gain a free [buildingName] [cityFilter]
	Free buildings CANNOT be self-removing - this leads to an endless loop of trying to add the building

	示例："Gain a free [Library] [in all cities]"

	适用范围：触发型，全球

:::
::: details [cityFilter]中[buildingFilter]建筑维护费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："[+20]中[Culture]建筑维护费[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details 移除[cityFilter]城市的[buildingFilter]
	示例："移除[Culture]城市的[in all cities]"

	适用范围：触发型，全球

:::
::: details 拆毁[cityFilter]的[buildingFilter](返还少量金钱)
	示例："拆毁[Culture]的[in all cities](返还少量金钱)"

	适用范围：触发型，全球

:::
::: details [cityFilter]自然扩张文化需求[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："[+20]自然扩张文化需求[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]购买地块金钱需求[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："[+20]购买地块金钱需求[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details 建立新城市引起的政策文化费用增幅-[relativeAmount]%
	示例："建立新城市引起的政策文化费用增幅-[+20]%"

	适用范围：全球

:::
::: details 推行新政策的文化花费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："推行新政策的文化花费[+20]%"

	适用范围：全球

:::
::: details 建立新城市引起的科学研究费用增幅-[relativeAmount]%
	示例："建立新城市引起的科学研究费用增幅-[+20]%"

	适用范围：全球

:::
::: details 研究新科技的科技花费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："研究新科技的科技花费[+20]%"

	适用范围：全球

:::
::: details 每发现一个自然奇观就[stats]
	示例："每发现一个自然奇观就[+1 Gold, +2 Production]"

	适用范围：全球

:::
::: details 每发现一个自然奇观就[stats]，若为首个发现则[stats2]
	示例："每发现一个自然奇观就[+1 Gold, +2 Production]，若为首个发现则[+1 Gold, +2 Production]"

	适用范围：全球

:::
::: details [cityFilter]伟人点数生成速度[relativeAmount]%
	示例："[+20]伟人点数生成速度[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details [relativeAmount]%来源于大商人贸易任务的金钱
	示例："[+20]%来源于大商人贸易任务的金钱"

	适用范围：全球，单位

:::
::: details 大军事家提供双倍战斗力加成
	适用范围：全球，单位

:::
::: details 研究[tech]后，每[comment]结束时(每394年)得到一个免费的伟人，每种伟人只能选择一次。
	示例："研究[comment]后，每[Agriculture]结束时(每394年)得到一个免费的伟人，每种伟人只能选择一次。"

	适用范围：全球

:::
::: details 一旦长历法激活，世界屏幕上的年份将显示为传统的玛雅长计数。
	适用范围：全球

:::
::: details [amount]单位补给
	示例："[3]单位补给"

	适用范围：全球

:::
::: details [cityFilter]毎[positiveAmount]人口[amount]单位补给
	示例："[3]毎[3]人口[in all cities]单位补给"

	适用范围：全球

:::
::: details 每座城市[amount]单位补给
	示例："每座城市[3]单位补给"

	适用范围：全球

:::
::: details 免除[amount]个单位的维护费
	示例："免除[3]个单位的维护费"

	适用范围：全球

:::
::: details 免除市中心驻军的维护费
	适用范围：全球

:::
::: details 陆军单位拥有船运能力
	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：全球

:::
::: details 允许[mapUnitFilter]单位进入海洋
	示例："允许[Wounded]单位进入海洋"

	适用范围：全球

:::
::: details 获得首个[baseUnitFilter]以后，陆地单位能穿过[terrainName]地块
	示例："获得首个[Forest]以后，陆地单位能穿过[Melee]地块"

	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：全球

:::
::: details 敌方的[mapUnitFilter]必须在你的土地上花费额外的[positiveAmount]点行动力
	示例："敌方的[Wounded]必须在你的土地上花费额外的[3]点行动力"

	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：全球

:::
::: details [cityFilter]新训练的[baseUnitFilter]单位初始拥有[amount]XP
	示例："[Melee]新训练的[3]单位初始拥有[in all cities]XP"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]新训练的[baseUnitFilter]单位初始拥有[promotion]晋升
	示例："[Melee]新训练的[in all cities]单位初始拥有[Shock I]晋升"

	适用范围：全球，追随者信仰

:::
::: details 毗邻城市中心的[mapUnitFilter]单位自愈时额外[amount]HP
	示例："毗邻城市中心的[Wounded]单位自愈时额外[3]HP"

	适用范围：全球，追随者信仰

:::
::: details 晋升所需XP[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："晋升所需XP[+20]%"

	适用范围：全球

:::
::: details 建筑提供的防御力[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："建筑提供的防御力[+20]%"

	适用范围：全球

:::
::: details 城市战斗力[relativeAmount]%
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："城市战斗力[+20]%"

	适用范围：全球，追随者信仰

:::
::: details 获得[amount]单位[resource]
	示例："获得[3]单位[Iron]"

	适用范围：全球，追随者信仰，地块改良

:::
::: details [resourceFilter]资源产量[relativeAmount]% 
	示例："[+20]资源产量[Strategic]% "

	适用范围：全球

:::
::: details 启用大使馆功能
	适用范围：全球

:::
::: details 需先建立大使馆才能开展高级外交
	适用范围：全球

:::
::: details 允许开放边境
	适用范围：全球

:::
::: details 允许签订科研协定
	适用范围：全球

:::
::: details 从科研协定中获得的科技值[relativeAmount]%
	示例："从科研协定中获得的科技值[+20]%"

	适用范围：全球

:::
::: details 允许签订共同防御条约
	适用范围：全球

:::
::: details 当宣布友谊宣言时，双方都获得[relativeAmount]%的伟人产生速率。
	示例："当宣布友谊宣言时，双方都获得[+20]%的伟人产生速率。"

	适用范围：全球

:::
::: details 其他文明对城邦的影响力下降速度+[relativeAmount]%
	示例："其他文明对城邦的影响力下降速度+[+20]%"

	适用范围：全球

:::
::: details 向城邦送予[baseUnitFilter]礼物时[amount]影响力
	示例："向城邦送予[3]礼物时[Melee]影响力"

	适用范围：全球

:::
::: details 对信奉该宗教的城邦影响力点数[amount]
	示例："对信奉该宗教的城邦影响力点数[3]"

	适用范围：全球

:::
::: details 标记新蛮族营地的位置
	适用范围：全球

:::
::: details 从掠夺城市和摧毁蛮族营地中获得[relativeAmount]%金钱
	示例："从掠夺城市和摧毁蛮族营地中获得[+20]%金钱"

	适用范围：全球

:::
::: details 摧毁蛮族营地时俘获一个蛮族并获得[amount]金钱
	示例："摧毁蛮族营地时俘获一个蛮族并获得[3]金钱"

	适用范围：全球

:::
::: details 击败[mapUnitFilter]单位时获得[amount]金钱并将其俘获
	示例："击败[Wounded]单位时获得[3]金钱并将其俘获"

	适用范围：全球

:::
::: details 当[foundingOrEnhancing]一个宗教时，可以额外选择[amount]个[beliefType]信条
	示例："当[3]一个宗教时，可以额外选择[Follower]个[founding]信条"

	适用范围：全球

:::
::: details 当[foundingOrEnhancing]一个宗教时，可以选择[amount]个任意类型的额外信条
	示例："当[3]一个宗教时，可以选择[founding]个任意类型的额外信条"

	适用范围：全球

:::
::: details 城市首次接受宗教的时间[stats]
	示例："城市首次接受宗教的时间[+1 Gold, +2 Production]"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	适用范围：全球

:::
::: details [cityFilter]宗教自然传播[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："[+20]宗教自然传播[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details 宗教自然传播距离扩大[amount]地块
	示例："宗教自然传播距离扩大[3]地块"

	适用范围：全球，追随者信仰

:::
::: details 不能自然产生大先知
	适用范围：全球

:::
::: details 获得大先知必须的信仰[relativeAmount]%
	示例："获得大先知必须的信仰[+20]%"

	适用范围：全球

:::
::: details [cityFilter]的我方间谍工作效率[relativeAmount]%
	示例："[+20]的我方间谍工作效率[in all cities]%"

	适用范围：全球

:::
::: details  [cityFilter]的敌方间谍工作效率[relativeAmount]%
	示例：" [+20]的敌方间谍工作效率[in all cities]%"

	适用范围：全球

:::
::: details 新间谍等级提高为[amount]级
	示例："新间谍等级提高为[3]级"

	适用范围：全球

:::
::: details Spies in [cityFilter] cities act as though they have [relativeAmount] levels for [spyAction]
	Temporary effective rank change ([relativeAmount] added to rank, e.g. +1) for spies doing the given action in a matching city. Does not permanently level the spy. Stacks additively, capped by maxSpyRank.

	示例："Spies in [in all cities] cities act as though they have [+20] levels for [Counter-intelligence]"

	适用范围：全球

:::
::: details 触发胜利
	适用范围：全球

:::
::: details 完成后触发文化胜利
	适用范围：全球

:::
::: details 可以在傀儡城市中购买建筑和单位
	适用范围：全球

:::
::: details 可能不会吞并城市
	适用范围：全球

:::
::: details 从其他文明处借用城市名
	适用范围：全球

:::
::: details [amount] 倍城市拆除速度
	示例："[3] 倍城市拆除速度"

	适用范围：全球

:::
::: details 每当在首都建成科研建筑 / 奇观时立刻获得一次科研点数奖励
	适用范围：全球

:::
::: details 黄金时代长度[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："黄金时代长度[+20]%"

	适用范围：全球

:::
::: details [cityFilter]核武器造成的人口损失[relativeAmount]%
	示例："[+20]核武器造成的人口损失[in all cities]%"

	适用范围：全球

:::
::: details [cityFilter]核武器造成的防御损失[relativeAmount]%
	示例："[+20]核武器造成的防御损失[in all cities]%"

	适用范围：全球

:::
::: details 可能会出现反叛单位
	适用范围：全球

:::
::: details 不可建造[buildingFilter]建筑
	示例："不可建造[Culture]建筑"

	适用范围：全球

:::
::: details 战斗力[relativeAmount]%
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："战斗力[+20]%"

	适用范围：全球，单位

:::
::: details 战斗力[relativeAmount]
	示例："战斗力[+20]"

	适用范围：全球，单位

:::
::: details 战斗力加成随与首都的距离减小而增大,最高为[relativeAmount]%
	示例："战斗力加成随与首都的距离减小而增大,最高为[+20]%"

	适用范围：全球，单位

:::
::: details 侧翼攻击加成[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："侧翼攻击加成[+20]%"

	适用范围：全球，单位

:::
::: details 在每回合可以额外攻击[amount]次
	示例："在每回合可以额外攻击[3]次"

	适用范围：全球，单位

:::
::: details 移动力[amount]
	示例："移动力[3]"

	适用范围：全球，单位

:::
::: details 视野[amount]
	示例："视野[3]"

	适用范围：全球，单位，地形修正，地块改良

:::
::: details 射程[amount]
	示例："射程[3]"

	适用范围：全球，单位

:::
::: details 航空器拦截范围[relativeAmount]
	示例："航空器拦截范围[+20]"

	适用范围：全球，单位

:::
::: details 恢复时额外恢复[amount]点生命值
	示例："恢复时额外恢复[3]点生命值"

	适用范围：全球，单位

:::
::: details 宗教传播力量[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："宗教传播力量[+20]%"

	适用范围：全球，单位

:::
::: details 将新城传教成功时获得此城其他宗教教徒数量[amount]倍的[stat]
	示例："将新城传教成功时获得此城其他宗教教徒数量[3]倍的[Culture]"

	适用范围：全球，单位

:::
::: details 远程攻击能越过障碍
	适用范围：全球，单位

:::
::: details 不受正面防御地形的影响
	适用范围：全球，单位

:::
::: details 不受负面防御地形的影响
	适用范围：全球，单位

:::
::: details 受伤单位免受伤害惩罚
	适用范围：全球，单位

:::
::: details 不能攻陷城市
	适用范围：全球，单位

:::
::: details 不能劫掠地块
	适用范围：全球，单位

:::
::: details 劫掠不消耗移动力
	适用范围：全球，单位

:::
::: details 可以在友好领土之外自愈
	适用范围：全球，单位

:::
::: details 所有生命回复效果翻倍
	适用范围：全球，单位

:::
::: details 消灭敌方单位时恢复[amount]生命值
	示例："消灭敌方单位时恢复[3]生命值"

	适用范围：全球，单位

:::
::: details 只能通过劫掠恢复生命值
	适用范围：全球，单位

:::
::: details 维护费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："维护费[+20]%"

	适用范围：全球，单位

:::
::: details 升级单位的金钱花费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："升级单位的金钱花费[+20]%"

	适用范围：全球，单位

:::
::: details 对[combatantFilter]单位造成时获得伤害[amount]%的[stockpile]
	示例："对[3]单位造成时获得伤害[City]%的[Mana]"

	适用范围：全球，单位

:::
::: details 攻陷城市时获得它[stat]产出[amount]倍的[stockpile]
	示例："攻陷城市时获得它[3]产出[Culture]倍的[Mana]"

	适用范围：全球，单位

:::
::: details 击杀敌方[mapUnitFilter]单位时获得[stockpile](≈已击杀单位的[costOrStrength]×[amount]%)
	示例："击杀敌方[3]单位时获得[Wounded](≈已击杀单位的[Cost]×[Mana]%)"

	适用范围：全球，单位

:::
::: details 从每次战斗中额外获得[amount]XP
	示例："从每次战斗中额外获得[3]XP"

	适用范围：全球，单位

:::
::: details 从战斗中获得的经验[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："从战斗中获得的经验[+20]%"

	适用范围：全球，单位

:::
::: details [greatPerson]的招募速率[relativeAmount]%
	示例："[Great General]的招募速率[+20]%"

	适用范围：全球，单位

:::
::: details 登陆时消耗[nonNegativeAmount]移动力
	示例："登陆时消耗[3]移动力"

	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：全球，单位

:::
::: details 下水时消耗[nonNegativeAmount]行动力
	示例："下水时消耗[3]行动力"

	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：全球，单位

:::
## 国家词条
::: details 初始即拥有科技：[tech]
	示例："初始即拥有科技：[Agriculture]"

	适用范围：国家

:::
::: details 游戏开始时就推行[policy]
	示例："游戏开始时就推行[Oligarchy]"

	适用范围：国家

:::
::: details Start bias [terrainFilter]
	Same effect as a Nation startBias field entry. Merged with the startBias field and, for city-states, with matching uniques on their CityStateType. Conditionals run against GameInfo only during map generation / start placement (no Civilization — it may be only partially initialized). Do not use conditionals that require tiles, cities, or units.

	示例："Start bias [Fresh Water]"

	适用范围：国家，城邦

:::
::: details 单位在己方森林和丛林地块上移动时视同在道路上移动，此类地块在研究轮子科技后可建立城市连接
	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：国家

:::
::: details 进行移动力损耗计算时忽略丘陵的影响
	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：国家

:::
::: details Excluded from map editor
	此词条自动对用户隐藏。

	适用范围：国家，地形修正，地块改良，资源

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details 不能在新游戏中选择
	适用范围：国家

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 个性词条
::: details 禁止建造[baseUnitFilter/buildingFilter]
	示例："禁止建造[Melee]"

	适用范围：个性

:::
::: details [relativeAmount]% weight to [baseUnitFilter/buildingFilter] for AI decisions
	示例："[+20]% weight to [Melee] for AI decisions"

	此词条自动对用户隐藏。

	适用范围：个性

:::
## 时代词条
::: details 在此时代开始游戏将会禁用宗教
	适用范围：时代

:::
::: details 当第一个文明进入此时代后，每个文明(城邦除外)都会获得一个间谍
	适用范围：时代

:::
## 科技词条
::: details 初始科技
	适用范围：科技

:::
::: details 可以持续研究
	适用范围：科技

:::
::: details 可用
	Meant to be used together with conditionals, like "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also block Upgrade and Transform actions. See also CanOnlyBeBuiltWhen

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 不可用
	Meant to be used together with conditionals, like "Unavailable &lt;after generating a Great Prophet&gt;".

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 不可加速建造
	适用范围：科技，建筑

:::
::: details [relativeAmount]% weight to this choice for AI decisions
	示例："[+20]% weight to this choice for AI decisions"

	此词条自动对用户隐藏。

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，晋升项，事件选择

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 政策词条
::: details 可用
	Meant to be used together with conditionals, like "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also block Upgrade and Transform actions. See also CanOnlyBeBuiltWhen

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 不可用
	Meant to be used together with conditionals, like "Unavailable &lt;after generating a Great Prophet&gt;".

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details [relativeAmount]% weight to this choice for AI decisions
	示例："[+20]% weight to this choice for AI decisions"

	此词条自动对用户隐藏。

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，晋升项，事件选择

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 创始人信仰词条
::: note

    创始人及增强类信条的词条，作用于该宗教的创始人
:::

::: details 所有信教的城市[stats]
	示例："所有信教的城市[+1 Gold, +2 Production]"

	适用范围：创始人信仰

:::
::: details [cityFilter]中每拥有[positiveAmount]单位全球教徒每回合[stats]
	示例："[+1 Gold, +2 Production]中每拥有[3]单位全球教徒每回合[in all cities]"

	适用范围：创始人信仰

:::
::: details 每个信徒都[relativeAmount]% [stat] ，但是最多到 [relativeAmount2]%
	示例："每个信徒都[+20]% [Culture] ，但是最多到 [+20]%"

	适用范围：创始人信仰，追随者信仰

:::
::: details 可用
	Meant to be used together with conditionals, like "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also block Upgrade and Transform actions. See also CanOnlyBeBuiltWhen

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 不可用
	Meant to be used together with conditionals, like "Unavailable &lt;after generating a Great Prophet&gt;".

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details [relativeAmount]% weight to this choice for AI decisions
	示例："[+20]% weight to this choice for AI decisions"

	此词条自动对用户隐藏。

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，晋升项，事件选择

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 追随者信仰词条
::: note

    万神殿与追随者类信条的词条，作用于该宗教为主导宗教的每座城市
:::

::: details [cityFilter][stats]
	示例："[+1 Gold, +2 Production][in all cities]"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]中的每个专业人员[stats]
	示例："[+1 Gold, +2 Production]中的每个专业人员[in all cities]"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]每[positiveAmount]人口[stats]
	示例："[+1 Gold, +2 Production]每[3]人口[in all cities]"

	适用范围：全球，追随者信仰

:::
::: details 坐落于[terrainFilter]的城市[stats]
	示例："坐落于[+1 Gold, +2 Production]的城市[Fresh Water]"

	适用范围：全球，追随者信仰

:::
::: details 所有[buildingFilter]建筑[stats]
	示例："所有[+1 Gold, +2 Production]建筑[Culture]"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]的[tileFilter]地块[stats]
	示例："[+1 Gold, +2 Production]的[Farm]地块[in all cities]"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]除[tileFilter2]地块之外的[tileFilter]的地块[stats]
	示例："[+1 Gold, +2 Production]除[Farm]地块之外的[Farm]的地块[in all cities]"

	适用范围：全球，追随者信仰

:::
::: details [stats] from every [tileFilter/specialist/buildingFilter]
	示例："[+1 Gold, +2 Production] from every [Farm]"

	适用范围：全球，追随者信仰

:::
::: details 每条贸易路线 [stats]
	示例："每条贸易路线 [+1 Gold, +2 Production]"

	适用范围：全球，追随者信仰

:::
::: details [relativeAmount]% [stat]
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20]% [Culture]"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter][relativeAmount]%[stat]
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20][Culture]%[in all cities]"

	适用范围：全球，追随者信仰

:::
::: details [relativeAmount]% [stat] 来自每个 [tileFilter/buildingFilter]
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20]% [Culture] 来自每个 [Farm]"

	适用范围：全球，追随者信仰

:::
::: details [relativeAmount]%来自每个[tileFilter/buildingFilter]的产出
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20]%来自每个[Farm]的产出"

	适用范围：全球，追随者信仰

:::
::: details 每个信徒都[relativeAmount]% [stat] ，但是最多到 [relativeAmount2]%
	示例："每个信徒都[+20]% [Culture] ，但是最多到 [+20]%"

	适用范围：创始人信仰，追随者信仰

:::
::: details [cityFilter] 建造[buildingFilter]建筑 时 [relativeAmount]%产能
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20] 建造[Culture]建筑 时 [in all cities]%产能"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter] 建造[baseUnitFilter]单位 时 [relativeAmount]%产能
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20] 建造[Melee]单位 时 [in all cities]%产能"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter] 建造[buildingFilter]奇观 时 [relativeAmount]%产能
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[+20] 建造[Culture]奇观 时 [in all cities]%产能"

	适用范围：全球，追随者信仰

:::
::: details 当所有在首都已建成的建筑在其他城市建造时 [relativeAmount]% 产能
	示例："当所有在首都已建成的建筑在其他城市建造时 [+20]% 产能"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter][relativeAmount]%人口增长
	示例："[+20][in all cities]%人口增长"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]在人口增长后人口增长所需食物减少[amount]%
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："[3]在人口增长后人口增长所需食物减少[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]中[populationFilter]消耗的食物[relativeAmount]%
	示例："[+20]中[Followers of this Religion]消耗的食物[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter][populationFilter]造成的不快乐[relativeAmount]%
	示例："[+20][Followers of this Religion]造成的不快乐[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]可以以不断上涨的价格([nonNegativeAmount])用[amount][stat]购买[baseUnitFilter]单位
	示例："[Melee]可以以不断上涨的价格([3])用[Culture][in all cities]购买[3]单位"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]可以以不断上涨的价格([nonNegativeAmount])用[amount][stat]购买[buildingFilter]建筑
	示例："[Culture]可以以不断上涨的价格([3])用[Culture][in all cities]购买[3]建筑"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]可以用[nonNegativeAmount][stat]购买[baseUnitFilter]单位
	示例："[Melee]可以用[3][Culture]购买[in all cities]单位"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]中可以用[nonNegativeAmount][stat]购买[buildingFilter]建筑
	示例："[Culture]中可以用[3][Culture]购买[in all cities]建筑"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]可以用[stat]购买[baseUnitFilter]单位
	示例："[Melee]可以用[Culture]购买[in all cities]单位"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]可以用[stat]购买[buildingFilter]建筑
	示例："[Culture]可以用[Culture]购买[in all cities]建筑"

	适用范围：全球，追随者信仰

:::
::: details 可以用[stat]购买[baseUnitFilter]单位，价格为其正常生产成本的[nonNegativeAmount]倍
	示例："可以用[Melee]购买[Culture]单位，价格为其正常生产成本的[3]倍"

	适用范围：全球，追随者信仰

:::
::: details 可以用[nonNegativeAmount]倍于其正常的生产花费的[stat]购买[buildingFilter]建筑
	示例："可以用[Culture]倍于其正常的生产花费的[Culture]购买[3]建筑"

	适用范围：全球，追随者信仰

:::
::: details 在城市建造/组建单位时的[stat]花费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："在城市建造/组建单位时的[Culture]花费[+20]%"

	适用范围：全球，追随者信仰

:::
::: details 购买[buildingFilter]建筑的[stat]花费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："购买[Culture]建筑的[Culture]花费[+20]%"

	适用范围：全球，追随者信仰

:::
::: details [baseUnitFilter]单位的[stat]花费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："[Culture]单位的[Melee]花费[+20]%"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]中[buildingFilter]建筑维护费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："[+20]中[Culture]建筑维护费[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]自然扩张文化需求[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："[+20]自然扩张文化需求[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]购买地块金钱需求[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："[+20]购买地块金钱需求[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]伟人点数生成速度[relativeAmount]%
	示例："[+20]伟人点数生成速度[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]新训练的[baseUnitFilter]单位初始拥有[amount]XP
	示例："[Melee]新训练的[3]单位初始拥有[in all cities]XP"

	适用范围：全球，追随者信仰

:::
::: details [cityFilter]新训练的[baseUnitFilter]单位初始拥有[promotion]晋升
	示例："[Melee]新训练的[in all cities]单位初始拥有[Shock I]晋升"

	适用范围：全球，追随者信仰

:::
::: details 毗邻城市中心的[mapUnitFilter]单位自愈时额外[amount]HP
	示例："毗邻城市中心的[Wounded]单位自愈时额外[3]HP"

	适用范围：全球，追随者信仰

:::
::: details 城市战斗力[relativeAmount]%
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："城市战斗力[+20]%"

	适用范围：全球，追随者信仰

:::
::: details 获得[amount]单位[resource]
	示例："获得[3]单位[Iron]"

	适用范围：全球，追随者信仰，地块改良

:::
::: details [cityFilter]宗教自然传播[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："[+20]宗教自然传播[in all cities]%"

	适用范围：全球，追随者信仰

:::
::: details 宗教自然传播距离扩大[amount]地块
	示例："宗教自然传播距离扩大[3]地块"

	适用范围：全球，追随者信仰

:::
::: details 可用
	Meant to be used together with conditionals, like "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also block Upgrade and Transform actions. See also CanOnlyBeBuiltWhen

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 不可用
	Meant to be used together with conditionals, like "Unavailable &lt;after generating a Great Prophet&gt;".

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 当[mapUnitFilter]单位中的[costOrStrength]在信仰该宗教的城市中4格范围内被击杀时获得其[stockpile]的[amount]%
	示例："当[3]单位中的[Wounded]在信仰该宗教的城市中4格范围内被击杀时获得其[Cost]的[Mana]%"

	适用范围：追随者信仰

:::
::: details [relativeAmount]% weight to this choice for AI decisions
	示例："[+20]% weight to this choice for AI decisions"

	此词条自动对用户隐藏。

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，晋升项，事件选择

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 建筑词条
::: details 此城市中来自每个 [improvementFilter/buildingFilter] 中 [positiveAmount]% 的 [stat] 增添到 [resource] 中
	示例："此城市中来自每个 [3] 中 [Culture]% 的 [All Road] 增添到 [Iron] 中"

	适用范围：建筑

:::
::: details 消耗[amount]单位[resource]
	示例："消耗[3]单位[Iron]"

	适用范围：建筑，单位，地块改良

:::
::: details 消耗[amount][stockpiledResource]
	These resources are removed *when work begins* on the construction. Do not confuse with "costs [amount] [stockpiledResource]" (lowercase 'c'), the Unit Action Modifier.

	示例："消耗[3][Mana]"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	适用范围：建筑，单位，地块改良

:::
::: details 不可建造或训练
	Blocks from being built, possibly by conditional. However it can still appear in the menu and be bought with other means such as Gold or Faith

	适用范围：建筑，单位，地块改良

:::
::: details 不可购买获得
	适用范围：建筑，单位

:::
::: details [cityFilter]可以使用[stat]购买
	示例："[Culture]可以使用[in all cities]购买"

	适用范围：建筑，单位

:::
::: details [cityFilter]可以用[amount][stat]购买
	示例："[3]可以用[Culture][in all cities]购买"

	适用范围：建筑，单位

:::
::: details 每个文明限于[amount]个
	示例："每个文明限于[3]个"

	适用范围：建筑，单位

:::
::: details 可用
	Meant to be used together with conditionals, like "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also block Upgrade and Transform actions. See also CanOnlyBeBuiltWhen

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 不可用
	Meant to be used together with conditionals, like "Unavailable &lt;after generating a Great Prophet&gt;".

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 组建过程中富余的食物将转化为产能
	适用范围：建筑，单位

:::
::: details 至少需要[amount]人口
	示例："至少需要[3]人口"

	适用范围：建筑，单位

:::
::: details 在建造开始时触发全局警报
	适用范围：建筑，单位

:::
::: details 完成后触发全局警报
	适用范围：建筑，单位

:::
::: details 每座我方城市使建造时花费增加[amount]
	示例："每座我方城市使建造时花费增加[3]"

	适用范围：建筑，单位

:::
::: details 每次重复建造使再次建造时花费增加[amount]
	示例："每次重复建造使再次建造时花费增加[3]"

	适用范围：建筑，单位

:::
::: details 造价[amount]%
	Intended to be used with conditionals to dynamically alter construction costs. Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："造价[3]%"

	适用范围：建筑，单位

:::
::: details 只能建造
	Meant to be used together with conditionals, like "Can only be built &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also NOT block Upgrade and Transform actions. See also OnlyAvailable.

	适用范围：建筑，单位

:::
::: details (城市)[amount]格内的己方地块必须有[tileFilter]
	示例："(城市)[Farm]格内的己方地块必须有[3]"

	适用范围：建筑

:::
::: details 允许建造核武器
	适用范围：建筑

:::
::: details Must be on [tileFilter]
	示例："Must be on [Farm]"

	适用范围：建筑

:::
::: details (城市)不能位于[tileFilter]
	示例："(城市)不能位于[Farm]"

	适用范围：建筑

:::
::: details Must be next to [tileFilter]
	示例："Must be next to [Farm]"

	适用范围：建筑，地块改良

:::
::: details (城市)不能建立在[tileFilter]旁边
	示例："(城市)不能建立在[Farm]旁边"

	适用范围：建筑

:::
::: details 不可售卖
	适用范围：建筑

:::
::: details Obsolete with [tech]
	示例："Obsolete with [Agriculture]"

	适用范围：建筑，地块改良，资源

:::
::: details 标记此城市为首都
	适用范围：建筑

:::
::: details 首都更迭时迁都
	适用范围：建筑

:::
::: details 此城开发的奢侈资源数量+1
	适用范围：建筑

:::
::: details 城市沦陷时摧毁
	适用范围：建筑

:::
::: details 城市沦陷时不会摧毁
	适用范围：建筑

:::
::: details 该城市被敌人占领时，敌人额外获得[relativeAmount]%金钱。
	示例："该城市被敌人占领时，敌人额外获得[+20]%金钱。"

	适用范围：建筑

:::
::: details 消除吞并城市带来的额外不满
	适用范围：建筑

:::
::: details 通过海路建立贸易路线
	适用范围：建筑

:::
::: details 在所有可能的城市中自动建造
	适用范围：建筑

:::
::: details 可以在一个特定的地块上建造[improvementName]
	When choosing to construct this building, the player must select a tile where the improvement can be built. Upon building completion, the tile will gain this improvement. Limited to one per building.

	示例："可以在一个特定的地块上建造[Trading Post]"

	此词条不支持条件。

	适用范围：建筑

:::
::: details Hidden from city screen
	This building is hidden from the city details screen after construction. All stats continue to apply normally.

	此词条自动对用户隐藏。

	适用范围：建筑

:::
::: details 可在同一城市建造 [amount] 次
	Allows this building to be constructed multiple times in the same city. Using -1 allows unlimited times.

	示例："可在同一城市建造 [3] 次"

	适用范围：建筑

:::
::: details 可装载[amount]个额外的[mapUnitFilter]单位
	For buildings, supports using `Air` for `mapUnitFilter` to increase city air unit capacity.

	示例："可装载[3]个额外的[Wounded]单位"

	适用范围：建筑，单位

:::
::: details 太空飞船部件
	适用范围：建筑，单位

:::
::: details 不可加速建造
	适用范围：科技，建筑

:::
::: details [relativeAmount]% weight to this choice for AI decisions
	示例："[+20]% weight to this choice for AI decisions"

	此词条自动对用户隐藏。

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，晋升项，事件选择

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details Shown while unbuilable
	此词条自动对用户隐藏。

	适用范围：建筑，单位

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 单位行动词条
::: note

    影响单位行动的词条，可用 UnitActionModifiers 修饰
:::

::: details 建立新城市
	适用范围：单位行动

:::
::: details 建立新傀儡城市
	适用范围：单位行动

:::
::: details 可立即建造[improvementFilter]地块改良
	示例："可立即建造[All Road]地块改良"

	适用范围：单位行动

:::
::: details 可以传播宗教
	适用范围：单位行动

:::
::: details 可以驱除城市异教
	适用范围：单位行动

:::
::: details 可创建一个宗教
	适用范围：单位行动

:::
::: details 可增强一个宗教
	适用范围：单位行动

:::
::: details 可以转换为[unit]
	By default consumes all movement

	示例："可以转换为[Musketman]"

	适用范围：单位行动

:::
## 单位词条
::: note

    可添加到单位、单位类型或晋升的词条
:::

::: details 掠夺地块的收益[relativeAmount]%
	示例："掠夺地块的收益[+20]%"

	适用范围：全球，单位

:::
::: details 掠夺地块的血量恢复[relativeAmount]%
	示例："掠夺地块的血量恢复[+20]%"

	适用范围：全球，单位

:::
::: details 对 [improvementFilter] 地块改良 [relativeAmount]% 的建造时间
	示例："对 [+20] 地块改良 [All Road]% 的建造时间"

	适用范围：全球，单位

:::
::: details 可以以[relativeAmount]%的速度建造[improvementFilter]地块改进改良。
	示例："可以以[All Road]%的速度建造[+20]地块改进改良。"

	适用范围：全球，单位

:::
::: details [relativeAmount]%来源于大商人贸易任务的金钱
	示例："[+20]%来源于大商人贸易任务的金钱"

	适用范围：全球，单位

:::
::: details 大军事家提供双倍战斗力加成
	适用范围：全球，单位

:::
::: details 消耗[amount]单位[resource]
	示例："消耗[3]单位[Iron]"

	适用范围：建筑，单位，地块改良

:::
::: details 消耗[amount][stockpiledResource]
	These resources are removed *when work begins* on the construction. Do not confuse with "costs [amount] [stockpiledResource]" (lowercase 'c'), the Unit Action Modifier.

	示例："消耗[3][Mana]"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	适用范围：建筑，单位，地块改良

:::
::: details 不可建造或训练
	Blocks from being built, possibly by conditional. However it can still appear in the menu and be bought with other means such as Gold or Faith

	适用范围：建筑，单位，地块改良

:::
::: details 不可购买获得
	适用范围：建筑，单位

:::
::: details [cityFilter]可以使用[stat]购买
	示例："[Culture]可以使用[in all cities]购买"

	适用范围：建筑，单位

:::
::: details [cityFilter]可以用[amount][stat]购买
	示例："[3]可以用[Culture][in all cities]购买"

	适用范围：建筑，单位

:::
::: details 每个文明限于[amount]个
	示例："每个文明限于[3]个"

	适用范围：建筑，单位

:::
::: details 可用
	Meant to be used together with conditionals, like "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also block Upgrade and Transform actions. See also CanOnlyBeBuiltWhen

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 不可用
	Meant to be used together with conditionals, like "Unavailable &lt;after generating a Great Prophet&gt;".

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 组建过程中富余的食物将转化为产能
	适用范围：建筑，单位

:::
::: details 至少需要[amount]人口
	示例："至少需要[3]人口"

	适用范围：建筑，单位

:::
::: details 在建造开始时触发全局警报
	适用范围：建筑，单位

:::
::: details 完成后触发全局警报
	适用范围：建筑，单位

:::
::: details 每座我方城市使建造时花费增加[amount]
	示例："每座我方城市使建造时花费增加[3]"

	适用范围：建筑，单位

:::
::: details 每次重复建造使再次建造时花费增加[amount]
	示例："每次重复建造使再次建造时花费增加[3]"

	适用范围：建筑，单位

:::
::: details 造价[amount]%
	Intended to be used with conditionals to dynamically alter construction costs. Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："造价[3]%"

	适用范围：建筑，单位

:::
::: details 只能建造
	Meant to be used together with conditionals, like "Can only be built &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also NOT block Upgrade and Transform actions. See also OnlyAvailable.

	适用范围：建筑，单位

:::
::: details 可在水上资源建造改良
	适用范围：单位

:::
::: details 可以建造地块改良：[improvementFilter/terrainFilter]
	示例："可以建造地块改良：[All Road]"

	适用范围：单位

:::
::: details 可以被加入在首都的[comment]
	示例："可以被加入在首都的[comment]"

	适用范围：单位

:::
::: details 阻止紧邻城市的宗教传播
	适用范围：单位

:::
::: details  传播宗教时消除异端
	适用范围：单位

:::
::: details 可向[tileFilter]区域空投，最远[positiveAmount]格
	示例："可向[Farm]区域空投，最远[3]格"

	适用范围：单位

:::
::: details 可以进行空中扫荡
	适用范围：单位

:::
::: details 可以加速建筑物的建造
	适用范围：单位

:::
::: details 可以加速奇观的建造
	适用范围：单位

:::
::: details 可以加速科技研究
	适用范围：单位

:::
::: details 可产生大量文化
	适用范围：单位

:::
::: details 可以拓展与城邦的贸易，获得大笔金钱和[amount]影响力
	示例："可以拓展与城邦的贸易，获得大笔金钱和[3]影响力"

	适用范围：单位

:::
::: details Automation is a primary action
	此词条自动对用户隐藏。

	适用范围：单位

:::
::: details 战斗力[relativeAmount]%
	Multiple bonuses stack additively: +50% + +50% = +100%

	示例："战斗力[+20]%"

	适用范围：全球，单位

:::
::: details 战斗力[relativeAmount]
	示例："战斗力[+20]"

	适用范围：全球，单位

:::
::: details 战斗力加成随与首都的距离减小而增大,最高为[relativeAmount]%
	示例："战斗力加成随与首都的距离减小而增大,最高为[+20]%"

	适用范围：全球，单位

:::
::: details 侧翼攻击加成[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："侧翼攻击加成[+20]%"

	适用范围：全球，单位

:::
::: details 位于相邻的[tileFilter]地块中的敌方[mapUnitFilter]单位[relativeAmount]%战斗力
	示例："位于相邻的[+20]地块中的敌方[Wounded]单位[Farm]%战斗力"

	适用范围：单位

:::
::: details 在[amount]格内[mapUnitFilter]单位[relativeAmount]%战斗力
	示例："在[+20]格内[Wounded]单位[3]%战斗力"

	适用范围：单位

:::
::: details 在每回合可以额外攻击[amount]次
	示例："在每回合可以额外攻击[3]次"

	适用范围：全球，单位

:::
::: details 移动力[amount]
	示例："移动力[3]"

	适用范围：全球，单位

:::
::: details 视野[amount]
	示例："视野[3]"

	适用范围：全球，单位，地形修正，地块改良

:::
::: details 射程[amount]
	示例："射程[3]"

	适用范围：全球，单位

:::
::: details 航空器拦截范围[relativeAmount]
	示例："航空器拦截范围[+20]"

	适用范围：全球，单位

:::
::: details 恢复时额外恢复[amount]点生命值
	示例："恢复时额外恢复[3]点生命值"

	适用范围：全球，单位

:::
::: details Before engaging in combat performs an extra ranged attack with [amount]% of melee combat strength
	示例："Before engaging in combat performs an extra ranged attack with [3]% of melee combat strength"

	适用范围：单位

:::
::: details 宗教传播力量[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："宗教传播力量[+20]%"

	适用范围：全球，单位

:::
::: details 将新城传教成功时获得此城其他宗教教徒数量[amount]倍的[stat]
	示例："将新城传教成功时获得此城其他宗教教徒数量[3]倍的[Culture]"

	适用范围：全球，单位

:::
::: details 只能攻击[combatantFilter]单位
	示例："只能攻击[City]单位"

	适用范围：单位

:::
::: details 只能攻击[tileFilter]地块
	示例："只能攻击[Farm]地块"

	适用范围：单位

:::
::: details 不可攻击
	适用范围：单位

:::
::: details 必须架设才能远程攻击
	适用范围：单位

:::
::: details 攻击时自毁
	适用范围：单位

:::
::: details 消除跨海攻击的地形影响
	适用范围：单位

:::
::: details 陆军单位可以在船载时攻击
	适用范围：单位

:::
::: details 无视跨河攻击时的战斗力减益
	适用范围：单位

:::
::: details 爆炸半径：[amount]
	示例："爆炸半径：[3]"

	适用范围：单位

:::
::: details 远程攻击能越过障碍
	适用范围：全球，单位

:::
::: details 核武器威力：[amount]
	示例："核武器威力：[3]"

	适用范围：单位

:::
::: details Attacks also target [mapUnitFilter] units within [positiveAmount] tiles
	Performs an attack against every unit that matches the filter inside the radius including allied units or own units if not filtered out, dealing equal damage. Status effects and on-hit abilities apply to all affected units.

	If both this and decreasing area attacks are present, only decreasing area attacks will be used.

	示例："Attacks also target [Wounded] units within [3] tiles"

	适用范围：单位

:::
::: details Attacks also target [mapUnitFilter] units within [positiveAmount] tiles, with damage decreasing by distance
	Performs an attack against every unit that matches the filter inside the radius with the damage decreasing with distance from the main target. Status effects and on-hit abilities apply.

	If both this and equal area attacks are present, only this will be used, also affects counter damage and damage from own area attacks.

	Damage formula: Damage = (1 - (distance / radius)) * baseDamage

	示例："Attacks also target [Wounded] units within [3] tiles, with damage decreasing by distance"

	适用范围：单位

:::
::: details Takes [relativeAmount]% damage from own area attacks
	This unit takes damage from its own area attacks when it is in range, 100 = 100% damage.

	示例："Takes [+20]% damage from own area attacks"

	适用范围：单位

:::
::: details Takes [relativeAmount]% counter damage from each unit hit by its area attacks
	Only works for melee units, 100 = 100% damage, negative values work but are taken as positive.

	示例："Takes [+20]% counter damage from each unit hit by its area attacks"

	适用范围：单位

:::
::: details 不受正面防御地形的影响
	适用范围：全球，单位

:::
::: details 不受负面防御地形的影响
	适用范围：全球，单位

:::
::: details 受伤单位免受伤害惩罚
	适用范围：全球，单位

:::
::: details 不可捕获
	适用范围：单位

:::
::: details 在近战前撤退
	适用范围：单位

:::
::: details 不能攻陷城市
	适用范围：全球，单位

:::
::: details 不能劫掠地块
	适用范围：全球，单位

:::
::: details [cityFilter]类型城市无法被占领，只能被摧毁 
	The unit will destroy [cityFilter] cities instead of capturing them, also allows non-melee units to destroy cities.Capital cities (including city states) are immune to this effect.

	示例："[in all cities]类型城市无法被占领，只能被摧毁 "

	适用范围：单位

:::
::: details 劫掠不消耗移动力
	适用范围：全球，单位

:::
::: details 攻击后可移动
	适用范围：单位

:::
::: details 将移动力转移至[mapUnitFilter]单位
	示例："将移动力转移至[Wounded]单位"

	适用范围：单位

:::
::: details 购买后无需等待一回合而可立即行动
	适用范围：单位

:::
::: details 可以在友好领土之外自愈
	适用范围：全球，单位

:::
::: details 所有生命回复效果翻倍
	适用范围：全球，单位

:::
::: details 消灭敌方单位时恢复[amount]生命值
	示例："消灭敌方单位时恢复[3]生命值"

	适用范围：全球，单位

:::
::: details 只能通过劫掠恢复生命值
	适用范围：全球，单位

:::
::: details 回合结束时自动恢复一定生命值
	适用范围：单位

:::
::: details 所有相邻单位在恢复时额外[amount]生命值
	示例："所有相邻单位在恢复时额外[3]生命值"

	适用范围：单位

:::
::: details 无视野
	适用范围：单位

:::
::: details 视野可以跨过障碍
	适用范围：单位

:::
::: details 可装载[amount]个[mapUnitFilter]单位
	示例："可装载[3]个[Wounded]单位"

	适用范围：单位

:::
::: details 可装载[amount]个额外的[mapUnitFilter]单位
	For buildings, supports using `Air` for `mapUnitFilter` to increase city air unit capacity.

	示例："可装载[3]个额外的[Wounded]单位"

	适用范围：建筑，单位

:::
::: details [mapUnitFilter]单位无法装载
	示例："[Wounded]单位无法装载"

	适用范围：单位

:::
::: details [relativeAmount]%几率拦截来袭的敌军飞机
	示例："[+20]%几率拦截来袭的敌军飞机"

	适用范围：单位

:::
::: details 遭受拦截时的损伤减少[relativeAmount]%
	示例："遭受拦截时的损伤减少[+20]%"

	适用范围：单位

:::
::: details 拦截敌方飞机时伤害[relativeAmount]%
	示例："拦截敌方飞机时伤害[+20]%"

	适用范围：单位

:::
::: details 每回合拦截次数额外[amount]
	示例："每回合拦截次数额外[3]"

	适用范围：单位

:::
::: details 不可拦截
	适用范围：单位

:::
::: details 无法拦截[mapUnitFilter]单位！
	示例："无法拦截[Wounded]单位！"

	适用范围：单位

:::
::: details 进行空中扫荡时力度[relativeAmount]%
	示例："进行空中扫荡时力度[+20]%"

	适用范围：单位

:::
::: details 维护费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："维护费[+20]%"

	适用范围：全球，单位

:::
::: details 升级单位的金钱花费[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："升级单位的金钱花费[+20]%"

	适用范围：全球，单位

:::
::: details 对[combatantFilter]单位造成时获得伤害[amount]%的[stockpile]
	示例："对[3]单位造成时获得伤害[City]%的[Mana]"

	适用范围：全球，单位

:::
::: details 攻陷城市时获得它[stat]产出[amount]倍的[stockpile]
	示例："攻陷城市时获得它[3]产出[Culture]倍的[Mana]"

	适用范围：全球，单位

:::
::: details 击杀敌方[mapUnitFilter]单位时获得[stockpile](≈已击杀单位的[costOrStrength]×[amount]%)
	示例："击杀敌方[3]单位时获得[Wounded](≈已击杀单位的[Cost]×[Mana]%)"

	适用范围：全球，单位

:::
::: details 能俘虏敌方被打败的[mapUnitFilter]单位
	示例："能俘虏敌方被打败的[Wounded]单位"

	适用范围：单位

:::
::: details 从每次战斗中额外获得[amount]XP
	示例："从每次战斗中额外获得[3]XP"

	适用范围：全球，单位

:::
::: details 从战斗中获得的经验[relativeAmount]%
	Multiple bonuses stack multiplicatively: +50% + +50% = x1.5 * x1.5 = +125%

	示例："从战斗中获得的经验[+20]%"

	适用范围：全球，单位

:::
::: details 可通过战斗获得
	适用范围：单位

:::
::: details [greatPerson]的招募速率[relativeAmount]%
	示例："[Great General]的招募速率[+20]%"

	适用范围：全球，单位

:::
::: details 对其他单位隐形
	适用范围：单位

:::
::: details 对非相邻单位隐身
	适用范围：单位

:::
::: details 能发现隐形的[mapUnitFilter]单位
	示例："能发现隐形的[Wounded]单位"

	适用范围：单位

:::
::: details 可在废墟中升级为[unit]
	示例："可在废墟中升级为[Musketman]"

	适用范围：单位

:::
::: details 可升级为[unit]
	示例："可升级为[Musketman]"

	适用范围：单位

:::
::: details 攻击时摧毁地块改良
	适用范围：单位

:::
::: details 不可移动
	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：单位

:::
::: details 在[terrainFilter]中拥有双倍移动力
	示例："在[Fresh Water]中拥有双倍移动力"

	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：单位

:::
::: details 进行移动力损耗计算时忽略所有影响
	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：单位

:::
::: details 下水时不处于船运状态
	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：单位

:::
::: details 可以穿过无法通行的地块
	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：单位

:::
::: details 进行移动力损耗计算时忽略负面影响
	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：单位

:::
::: details 忽视地块控制权
	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：单位

:::
::: details 进行移动力损耗计算时强调崎岖地形的影响
	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：单位

:::
::: details 可进入冰区
	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：单位

:::
::: details 不能船运
	适用范围：单位

:::
::: details 不能进入海洋地块
	适用范围：单位

:::
::: details 可进入外国未开放边界的领土
	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：单位

:::
::: details 可进入外国未开放边界的领土，在外国领土结束回合时失去[amount]宗教力量
	示例："可进入外国未开放边界的领土，在外国领土结束回合时失去[3]宗教力量"

	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：单位

:::
::: details 登陆时消耗[nonNegativeAmount]移动力
	示例："登陆时消耗[3]移动力"

	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：全球，单位

:::
::: details 下水时消耗[nonNegativeAmount]行动力
	示例："下水时消耗[3]行动力"

	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	适用范围：全球，单位

:::
::: details Never appears as a Barbarian unit
	此词条自动对用户隐藏。

	适用范围：单位

:::
::: details 宗教单位
	适用范围：单位

:::
::: details 太空飞船部件
	适用范围：建筑，单位

:::
::: details 使其他文明的城市信仰你的宗教，甚至消灭其他宗教
	适用范围：单位

:::
::: details [comment]类伟人
	示例："[comment]类伟人"

	适用范围：单位

:::
::: details 是[comment]类伟人的一部分
	Great people in the same group increase teach other's costs when gained. Gaining one will make all others in the same group cost more GPP.

	示例："是[comment]类伟人的一部分"

	适用范围：单位

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details Shown while unbuilable
	此词条自动对用户隐藏。

	适用范围：建筑，单位

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 单位类别词条
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 晋升项词条
::: details 可用
	Meant to be used together with conditionals, like "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also block Upgrade and Transform actions. See also CanOnlyBeBuiltWhen

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 不可用
	Meant to be used together with conditionals, like "Unavailable &lt;after generating a Great Prophet&gt;".

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details Not shown on world screen
	此词条自动对用户隐藏。

	适用范围：晋升项，资源

:::
::: details 这样做将消耗掉这次晋升机会
	适用范围：晋升项

:::
::: details 免费晋升
	适用范围：晋升项

:::
::: details [relativeAmount]% weight to this choice for AI decisions
	示例："[+20]% weight to this choice for AI decisions"

	此词条自动对用户隐藏。

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，晋升项，事件选择

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 地形修正词条
::: details [stats]
	示例："[+1 Gold, +2 Production]"

	适用范围：全球，地形修正，地块改良

:::
::: details 视野[amount]
	示例："视野[3]"

	适用范围：全球，单位，地形修正，地块改良

:::
::: details Must be adjacent to [amount] [simpleTerrain] tiles
	示例："Must be adjacent to [3] [Elevated] tiles"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details Must be adjacent to [amount] to [amount2] [simpleTerrain] tiles
	示例："Must be adjacent to [3] to [3] [Elevated] tiles"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details Must not be on [amount] largest landmasses
	示例："Must not be on [3] largest landmasses"

	此词条自动对用户隐藏。

	适用范围：地形修正，资源

:::
::: details Must be on [amount] largest landmasses
	示例："Must be on [3] largest landmasses"

	此词条自动对用户隐藏。

	适用范围：地形修正，资源

:::
::: details Occurs on latitudes from [amount] to [amount2] percent of distance equator to pole
	示例："Occurs on latitudes from [3] to [3] percent of distance equator to pole"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details Occurs in groups of [amount] to [amount2] tiles
	示例："Occurs in groups of [3] to [3] tiles"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details Neighboring tiles will convert to [baseTerrain/terrainFeature]
	Supports conditionals that need only a Tile as context and nothing else, like `<with [n]% chance>`, and applies them per neighbor.

	If your mod renames Coast or Lakes, do not use this with one of these as parameter, as the code preventing artifacts won't work.

	示例："Neighboring tiles will convert to [Grassland]"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details 赠与首个发现的文明[stats]
	示例："赠与首个发现的文明[+1 Gold, +2 Production]"

	适用范围：地形修正

:::
::: details 单位在此地形结束回合时将受到[amount]伤害
	示例："单位在此地形结束回合时将受到[3]伤害"

	由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。

	此词条不支持条件。

	适用范围：地形修正

:::
::: details 向相邻的[mapUnitFilter]单位授予[promotion]([comment])
	示例："向相邻的[Shock I]单位授予[comment]([Wounded])"

	适用范围：地形修正

:::
::: details 在此地形上建立的城市[amount]战斗力
	示例："在此地形上建立的城市[3]战斗力"

	适用范围：地形修正

:::
::: details 在移除后一次性为最近的城市产出[stats]
	示例："在移除后一次性为最近的城市产出[+1 Gold, +2 Production]"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	此词条的效果可被 &lt;(受游戏进程影响，且至多到[relativeAmount]%)&gt;

	适用范围：地形修正

:::
::: details 植被
	适用范围：地形修正，地块改良

:::
::: details 地块提供产量无需指定人口
	适用范围：地形修正，地块改良

:::
::: details 取消此地块的所有产出加成
	适用范围：地形修正

:::
::: details 只有[improvementFilter]能被建造在此地块上
	示例："只有[All Road]能被建造在此地块上"

	适用范围：地形修正

:::
::: details 可挡住来自同海拔地块的视线
	适用范围：地形修正

:::
::: details 计算单位的视野范围时按高程[amount]计算
	示例："计算单位的视野范围时按高程[3]计算"

	适用范围：地形修正

:::
::: details Always Fertility [amount] for Map Generation
	示例："Always Fertility [3] for Map Generation"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details [amount] to Fertility for Map Generation
	示例："[3] to Fertility for Map Generation"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details A Region is formed with at least [amount]% [simpleTerrain] tiles, with priority [amount2]
	示例："A Region is formed with at least [3]% [Elevated] tiles, with priority [3]"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details A Region is formed with at least [amount]% [simpleTerrain] tiles and [simpleTerrain2] tiles, with priority [amount2]
	示例："A Region is formed with at least [3]% [Elevated] tiles and [Elevated] tiles, with priority [3]"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details A Region can not contain more [simpleTerrain] tiles than [simpleTerrain2] tiles
	示例："A Region can not contain more [Elevated] tiles than [Elevated] tiles"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details Base Terrain on this tile is not counted for Region determination
	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details Starts in regions of this type receive an extra [resource]
	示例："Starts in regions of this type receive an extra [Iron]"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details Never receives any resources
	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details Becomes [terrainName] when adjacent to [terrainFilter]
	示例："Becomes [Forest] when adjacent to [Fresh Water]"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details Considered [terrainQuality] when determining start locations
	示例："Considered [Undesirable] when determining start locations"

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details Doesn't generate naturally
	此词条自动对用户隐藏。

	适用范围：地形修正，资源

:::
::: details Occurs at temperature between [fraction] and [fraction2] and humidity between [fraction3] and [fraction4]
	示例："Occurs at temperature between [0.5] and [0.5] and humidity between [0.5] and [0.5]"

	此词条自动对用户隐藏。

	适用范围：地形修正，资源

:::
::: details Occurs in chains at high elevations
	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details Occurs in groups around high elevations
	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details Every [amount] tiles with this terrain will receive a major deposit of a strategic resource.
	示例："Every [3] tiles with this terrain will receive a major deposit of a strategic resource."

	此词条自动对用户隐藏。

	适用范围：地形修正

:::
::: details 稀有地貌
	适用范围：地形修正

:::
::: details 被核武器攻击时有[amount]%概率毁灭
	示例："被核武器攻击时有[3]%概率毁灭"

	适用范围：地形修正

:::
::: details 淡水
	适用范围：地形修正

:::
::: details 崎岖地形
	适用范围：地形修正

:::
::: details 沿海
	Marks water tiles as Coast - all other water tiles count as Ocean. These distinctions are relevant e.g. for map generator or the ability to navigate here.

	Note that terrain filters do not recognize this distinction, filtering for "Coast" or "Ocean" will only look for a terrain of that name.

	Also note that for compatibility reasons, terrains named "Coast" are assuned to have this Unique even if it's missing. This may be removed in a future version.

	A tile marked this way marks adjacent land tiles as "Coastal", so they fulfill the terrain filter, and cities built there can build ships, Harbor, etc.

	适用范围：地形修正

:::
::: details Excluded from map editor
	此词条自动对用户隐藏。

	适用范围：国家，地形修正，地块改良，资源

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details Suppress warning [validationWarning]
	Allows suppressing specific validation warnings. Errors, deprecation warnings, or warnings about untyped and non-filtering uniques should be heeded, not suppressed, and are therefore not accepted. Note that this can be used in ModOptions, in the uniques a warning is about, or as modifier on the unique triggering a warning - but you still need to be specific. Even in the modifier case you will need to specify a sufficiently selective portion of the warning text as parameter.

	示例："Suppress warning [Tinman is supposed to automatically upgrade at tech Clockwork, and therefore Servos for its upgrade Mecha may not yet be researched! -or- *is supposed to automatically upgrade*]"

	此词条不支持条件。

	此词条自动对用户隐藏。

	适用范围：触发型，地形修正，游戏速度，模组选项，元修饰

:::
## 地块改良词条
::: details [stats]
	示例："[+1 Gold, +2 Production]"

	适用范围：全球，地形修正，地块改良

:::
::: details 消耗[amount]单位[resource]
	示例："消耗[3]单位[Iron]"

	适用范围：建筑，单位，地块改良

:::
::: details 获得[amount]单位[resource]
	示例："获得[3]单位[Iron]"

	适用范围：全球，追随者信仰，地块改良

:::
::: details 消耗[amount][stockpiledResource]
	These resources are removed *when work begins* on the construction. Do not confuse with "costs [amount] [stockpiledResource]" (lowercase 'c'), the Unit Action Modifier.

	示例："消耗[3][Mana]"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	适用范围：建筑，单位，地块改良

:::
::: details 不可建造或训练
	Blocks from being built, possibly by conditional. However it can still appear in the menu and be bought with other means such as Gold or Faith

	适用范围：建筑，单位，地块改良

:::
::: details 可用
	Meant to be used together with conditionals, like "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also block Upgrade and Transform actions. See also CanOnlyBeBuiltWhen

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 不可用
	Meant to be used together with conditionals, like "Unavailable &lt;after generating a Great Prophet&gt;".

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details Must be next to [tileFilter]
	示例："Must be next to [Farm]"

	适用范围：建筑，地块改良

:::
::: details Obsolete with [tech]
	示例："Obsolete with [Agriculture]"

	适用范围：建筑，地块改良，资源

:::
::: details 视野[amount]
	示例："视野[3]"

	适用范围：全球，单位，地形修正，地块改良

:::
::: details 植被
	适用范围：地形修正，地块改良

:::
::: details 地块提供产量无需指定人口
	适用范围：地形修正，地块改良

:::
::: details Excluded from map editor
	此词条自动对用户隐藏。

	适用范围：国家，地形修正，地块改良，资源

:::
::: details 也能建造在毗邻淡水的地块
	适用范围：地块改良

:::
::: details 使[tileFilter]地块[stats]
	示例："使[+1 Gold, +2 Production]地块[Farm]"

	适用范围：地块改良

:::
::: details [stats] for each adjacent [tileFilter]
	示例："[+1 Gold, +2 Production] for each adjacent [Farm]"

	适用范围：地块改良

:::
::: details 保底产出[stats]
	示例："保底产出[+1 Gold, +2 Production]"

	适用范围：地块改良

:::
::: details 可在你的国土外建造
	适用范围：地块改良

:::
::: details 可在与己方地块相邻的非己方地块上建造
	适用范围：地块改良

:::
::: details 只能建在[tileFilter]地块上
	示例："只能建在[Farm]地块上"

	适用范围：地块改良

:::
::: details 无法建在[tileFilter]地块上
	示例："无法建在[Farm]地块上"

	适用范围：地块改良

:::
::: details 只能在可以获得资源的情况下建造
	适用范围：地块改良

:::
::: details Does not need removal of [terrainFeature]
	示例："Does not need removal of [Hill]"

	适用范围：地块改良

:::
::: details 在建造时移除可移除地貌
	适用范围：地块改良

:::
::: details 提供[relativeAmount]%防御力加成
	Does not accept unit-based conditionals

	示例："提供[+20]%防御力加成"

	适用范围：地块改良

:::
::: details 建在你的领土内时每回合扣除[amount][stat] 
	示例："建在你的领土内时每回合扣除[3][Culture] "

	适用范围：地块改良

:::
::: details 每回合扣除[amount][stat] 
	示例："每回合扣除[3][Culture] "

	适用范围：地块改良

:::
::: details 相邻的敌方单位结束回合时受到[amount]伤害
	示例："相邻的敌方单位结束回合时受到[3]伤害"

	适用范围：地块改良

:::
::: details 伟人改良
	适用范围：地块改良

:::
::: details 有单位进入则提供随机奖励
	适用范围：地块改良

:::
::: details Marks a barbarian camp
	When several barbarian camp improvements are available, each new camp chooses one randomly.

	此词条自动对用户隐藏。

	适用范围：地块改良

:::
::: details 不可劫掠
	适用范围：地块改良

:::
::: details 劫掠该地块改良可获得约[stats]
	示例："劫掠该地块改良可获得约[+1 Gold, +2 Production]"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	此词条的效果可被 &lt;(受游戏进程影响，且至多到[relativeAmount]%)&gt;

	适用范围：地块改良

:::
::: details 劫掠该地块改良可获得[stats]
	示例："劫掠该地块改良可获得[+1 Gold, +2 Production]"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	此词条的效果可被 &lt;(受游戏进程影响，且至多到[relativeAmount]%)&gt;

	适用范围：地块改良

:::
::: details 劫掠后被摧毁
	适用范围：地块改良

:::
::: details 不可劫掠且不可摧毁
	适用范围：地块改良

:::
::: details 将不会自动交换单位
	适用范围：地块改良

:::
::: details 改良此地块的[resourceFilter]资源
	This is offered as an alternative to the improvedBy field of a resource. The result will be cached within the resource definition when loading a game, without knowledge about terrain, cities, civs, units or time. Therefore, most conditionals will not work, only those **not** dependent on game state.

	示例："改良此地块的[Strategic]资源"

	此词条不支持条件。

	适用范围：地块改良

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 资源词条
::: details Obsolete with [tech]
	示例："Obsolete with [Agriculture]"

	适用范围：建筑，地块改良，资源

:::
::: details Must not be on [amount] largest landmasses
	示例："Must not be on [3] largest landmasses"

	此词条自动对用户隐藏。

	适用范围：地形修正，资源

:::
::: details Must be on [amount] largest landmasses
	示例："Must be on [3] largest landmasses"

	此词条自动对用户隐藏。

	适用范围：地形修正，资源

:::
::: details Doesn't generate naturally
	此词条自动对用户隐藏。

	适用范围：地形修正，资源

:::
::: details Occurs at temperature between [fraction] and [fraction2] and humidity between [fraction3] and [fraction4]
	示例："Occurs at temperature between [0.5] and [0.5] and humidity between [0.5] and [0.5]"

	此词条自动对用户隐藏。

	适用范围：地形修正，资源

:::
::: details Excluded from map editor
	此词条自动对用户隐藏。

	适用范围：国家，地形修正，地块改良，资源

:::
::: details [tileFilter]地块始终提供[amount]资源
	示例："[Farm]地块始终提供[3]资源"

	适用范围：资源

:::
::: details 只能由商业城邦产出
	适用范围：资源

:::
::: details 具有可堆叠性
	This resource is accumulated each turn, rather than having a set of producers and consumers at a given moment.The current stockpiled amount can be affected with trigger uniques.

	适用范围：资源

:::
::: details 城市级别资源
	This resource is calculated on a per-city level rather than a per-civ level

	适用范围：资源

:::
::: details 无法交易
	适用范围：资源

:::
::: details Not shown on world screen
	此词条自动对用户隐藏。

	适用范围：晋升项，资源

:::
::: details Generated with weight [amount]
	The probability for this resource to be chosen is (this resource weight) / (sum weight of all eligible resources). Resources without a unique are given weight `1`

	示例："Generated with weight [3]"

	此词条自动对用户隐藏。

	适用范围：资源

:::
::: details Minor deposits generated with weight [amount]
	The probability for this resource to be chosen is (this resource weight) / (sum weight of all eligible resources). Resources without a unique are not generated as minor deposits.

	示例："Minor deposits generated with weight [3]"

	此词条自动对用户隐藏。

	适用范围：资源

:::
::: details Generated near City States with weight [amount]
	The probability for this resource to be chosen is (this resource weight) / (sum weight of all eligible resources). Only assignable to luxuries, resources without a unique are given weight `1`

	示例："Generated near City States with weight [3]"

	此词条自动对用户隐藏。

	适用范围：资源

:::
::: details Special placement during map generation
	此词条自动对用户隐藏。

	适用范围：资源

:::
::: details Generated on every [amount] tiles
	示例："Generated on every [3] tiles"

	此词条自动对用户隐藏。

	适用范围：资源

:::
::: details 开启战略平衡选项时一定出现在出生点附近
	适用范围：资源

:::
::: details AI will sell at [amount] Gold
	示例："AI will sell at [3] Gold"

	此词条自动对用户隐藏。

	适用范围：资源

:::
::: details AI will buy at [amount] Gold
	示例："AI will buy at [3] Gold"

	此词条自动对用户隐藏。

	适用范围：资源

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 遗迹词条
::: details 可用
	Meant to be used together with conditionals, like "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also block Upgrade and Transform actions. See also CanOnlyBeBuiltWhen

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 不可用
	Meant to be used together with conditionals, like "Unavailable &lt;after generating a Great Prophet&gt;".

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 在遗迹中发现[unit]
	示例："在遗迹中发现[Musketman]"

	适用范围：遗迹

:::
::: details 从距离一个废墟[positiveAmount]格外随机选择一个的地块处，以[positiveAmount3]%的可能性揭示至多[positiveAmount2]格地块
	示例："从距离一个废墟[3]格外随机选择一个的地块处，以[3]%的可能性揭示至多[3]格地块"

	适用范围：遗迹

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 游戏速度词条
::: note

    速度词条将作为所选游戏速度的 GlobalUniques 的一部分
:::

::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details Suppress warning [validationWarning]
	Allows suppressing specific validation warnings. Errors, deprecation warnings, or warnings about untyped and non-filtering uniques should be heeded, not suppressed, and are therefore not accepted. Note that this can be used in ModOptions, in the uniques a warning is about, or as modifier on the unique triggering a warning - but you still need to be specific. Even in the modifier case you will need to specify a sufficiently selective portion of the warning text as parameter.

	示例："Suppress warning [Tinman is supposed to automatically upgrade at tech Clockwork, and therefore Servos for its upgrade Mecha may not yet be researched! -or- *is supposed to automatically upgrade*]"

	此词条不支持条件。

	此词条自动对用户隐藏。

	适用范围：触发型，地形修正，游戏速度，模组选项，元修饰

:::
## 难度词条
::: note

    难度词条将作为所选游戏难度的 GlobalUniques 的一部分
:::

::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 城邦词条
::: details 每经过约[positiveAmount]回合提供一个军事单位
	示例："每经过约[3]回合提供一个军事单位"

	适用范围：城邦

:::
::: details 提供一种独特的奢侈资源
	适用范围：城邦

:::
::: details Start bias [terrainFilter]
	Same effect as a Nation startBias field entry. Merged with the startBias field and, for city-states, with matching uniques on their CityStateType. Conditionals run against GameInfo only during map generation / start placement (no Civilization — it may be only partially initialized). Do not use conditionals that require tiles, cities, or units.

	示例："Start bias [Fresh Water]"

	适用范围：国家，城邦

:::
## 模组选项词条
::: details 暂不能改变外交关系
	此词条不支持条件。

	适用范围：模组选项

:::
::: details 可通过滑块将金币转换为科学值
	此词条不支持条件。

	适用范围：模组选项

:::
::: details 允许开局时城邦带有额外单位
	此词条不支持条件。

	适用范围：模组选项

:::
::: details 可以用[positiveAmount]金币认识第三方文明
	示例："可以用[3]金币认识第三方文明"

	此词条不支持条件。

	适用范围：模组选项

:::
::: details 禁用宗教
	此词条不支持条件。

	适用范围：模组选项

:::
::: details 只能从起始时代开始游戏
	In this case, 'starting era' means the first defined Era in the entire ruleset.

	此词条不支持条件。

	适用范围：模组选项

:::
::: details 允许摧毁首都
	此词条不支持条件。

	适用范围：模组选项

:::
::: details 允许摧毁圣城
	此词条不支持条件。

	适用范围：模组选项

:::
::: details Allow cities to claim tiles
	此词条不支持条件。

	适用范围：模组选项

:::
::: details City-states search for first city location
	By default, city-state settlers with no cities yet found on their current tile when valid (predetermined map-gen / editor start). With this unique they use the same nearby-site search as major civs.

	此词条不支持条件。

	适用范围：模组选项

:::
::: details Suppress warning [validationWarning]
	Allows suppressing specific validation warnings. Errors, deprecation warnings, or warnings about untyped and non-filtering uniques should be heeded, not suppressed, and are therefore not accepted. Note that this can be used in ModOptions, in the uniques a warning is about, or as modifier on the unique triggering a warning - but you still need to be specific. Even in the modifier case you will need to specify a sufficiently selective portion of the warning text as parameter.

	示例："Suppress warning [Tinman is supposed to automatically upgrade at tech Clockwork, and therefore Servos for its upgrade Mecha may not yet be researched! -or- *is supposed to automatically upgrade*]"

	此词条不支持条件。

	此词条自动对用户隐藏。

	适用范围：触发型，地形修正，游戏速度，模组选项，元修饰

:::
::: details Mod与[modFilter]不兼容
	Specifies that your Mod is incompatible with another. Always treated symmetrically, and cannot be overridden by the Mod you are declaring as incompatible.

	示例："Mod与[DeCiv Redux]不兼容"

	此词条不支持条件。

	适用范围：模组选项

:::
::: details Mod 需要 [modFilter]
	Specifies that your Extension Mod is only available if any other Mod matching the filter is active.

	Multiple copies of this Unique cannot be used to specify alternatives, they work as 'and' logic. If you need alternates and wildcards can't filter them well enough, please open an issue.

	示例："Mod 需要 [DeCiv Redux]"

	此词条不支持条件。

	适用范围：模组选项

:::
::: details 只能用作永久视听Mod
	此词条不支持条件。

	适用范围：模组选项

:::
::: details 可作为永久视听Mod
	此词条不支持条件。

	适用范围：模组选项

:::
::: details 不能用作永久视听Mod
	此词条不支持条件。

	适用范围：模组选项

:::
::: details 模组预选地图：[comment]
	Only meaningful for Mods containing several maps. When this mod is selected on the new game screen's custom maps mod dropdown, the named map will be selected on the map dropdown. Also disables selection by recently modified. Case insensitive.

	示例："模组预选地图：[comment]"

	此词条不支持条件。

	适用范围：模组选项

:::
## 事件词条
::: details 可用
	Meant to be used together with conditionals, like "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also block Upgrade and Transform actions. See also CanOnlyBeBuiltWhen

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 不可用
	Meant to be used together with conditionals, like "Unavailable &lt;after generating a Great Prophet&gt;".

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
## 事件选择词条
::: details 可用
	Meant to be used together with conditionals, like "Only available &lt;after adopting [policy]&gt; &lt;while the empire is happy&gt;". Only allows Building when ALL conditionals are met. Will also block Upgrade and Transform actions. See also CanOnlyBeBuiltWhen

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details 不可用
	Meant to be used together with conditionals, like "Unavailable &lt;after generating a Great Prophet&gt;".

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，单位，晋升项，地块改良，遗迹，事件，事件选择

:::
::: details [relativeAmount]% weight to this choice for AI decisions
	示例："[+20]% weight to this choice for AI decisions"

	此词条自动对用户隐藏。

	适用范围：科技，政策，创始人信仰，追随者信仰，建筑，晋升项，事件选择

:::
::: details Will not be displayed in Civilopedia
	Supports conditionals that need only a Game as context and nothing else.

	Most conditionals require at least a Civilization and will **not** work.

	Note that when Civilopedia runs from main menu, conditionals will be ignored.

	此词条自动对用户隐藏。

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
::: details [comment]
	Allows displaying arbitrary text in a Unique listing. Only the text within the '[]' brackets will be displayed, the rest serves to allow Ruleset validation to recognize the intent.

	示例："[comment]"

	适用范围：国家，科技，政策，创始人信仰，追随者信仰，建筑，单位，单位类别，晋升项，地形修正，地块改良，资源，遗迹，游戏速度，难度，事件选择

:::
## 有前提的词条
::: note

    可添加到其他词条的修饰符，用于限制其生效时机
:::

::: details &lt;每[positiveAmount]回合&gt;
	示例："&lt;每[3]回合&gt;"

	适用范围：有前提的

:::
::: details &lt;[nonNegativeAmount]回合之前&gt;
	示例："&lt;[3]回合之前&gt;"

	适用范围：有前提的

:::
::: details &lt;[nonNegativeAmount]回合之后&gt;
	示例："&lt;[3]回合之后&gt;"

	适用范围：有前提的

:::
::: details &lt;以[speed]速度游玩时&gt;
	示例："&lt;以[Quick]速度游玩时&gt;"

	适用范围：有前提的

:::
::: details &lt;在[difficulty]难度下&gt;
	示例："&lt;在[Prince]难度下&gt;"

	适用范围：有前提的

:::
::: details &lt;在[difficulty]或更高的难度下&gt;
	示例："&lt;在[Prince]或更高的难度下&gt;"

	适用范围：有前提的

:::
::: details &lt;在[difficulty]或更低的难度下&gt;
	示例："&lt;在[Prince]或更低的难度下&gt;"

	适用范围：有前提的

:::
::: details &lt;若[victoryType]胜利方式可用&gt;
	示例："&lt;若[Domination]胜利方式可用&gt;"

	适用范围：有前提的

:::
::: details &lt;若[victoryType]胜利方式禁用&gt;
	示例："&lt;若[Domination]胜利方式禁用&gt;"

	适用范围：有前提的

:::
::: details &lt;若启用宗教&gt;
	适用范围：有前提的

:::
::: details &lt;若禁用宗教&gt;
	适用范围：有前提的

:::
::: details &lt;若启用间谍&gt;
	适用范围：有前提的

:::
::: details &lt;若禁用间谍&gt;
	适用范围：有前提的

:::
::: details &lt;若启用核武器&gt;
	适用范围：有前提的

:::
::: details &lt;若禁用核武器&gt;
	适用范围：有前提的

:::
::: details &lt;有[nonNegativeAmount]%概率&gt;
	示例："&lt;有[3]%概率&gt;"

	适用范围：有前提的

:::
::: details &lt;if tutorials are enabled&gt;
	此词条自动对用户隐藏。

	适用范围：有前提的

:::
::: details &lt;if tutorial [comment] is completed&gt;
	示例："&lt;if tutorial [comment] is completed&gt;"

	此词条自动对用户隐藏。

	适用范围：有前提的

:::
::: details &lt;令[civFilter]&gt;
	示例："&lt;令[City-States]&gt;"

	适用范围：有前提的

:::
::: details &lt;交战时&gt;
	适用范围：有前提的

:::
::: details &lt;和平时&gt;
	适用范围：有前提的

:::
::: details &lt;黄金时代期间&gt;
	适用范围：有前提的

:::
::: details &lt;非黄金时代期间&gt;
	适用范围：有前提的

:::
::: details &lt;“我们爱戴领袖日”期间&gt;
	适用范围：有前提的

:::
::: details &lt;处于快乐时&gt;
	适用范围：有前提的

:::
::: details &lt;处于[era]时&gt;
	示例："&lt;处于[Ancient era]时&gt;"

	适用范围：有前提的

:::
::: details &lt;在[era]前&gt;
	示例："&lt;在[Ancient era]前&gt;"

	适用范围：有前提的

:::
::: details &lt;从[era]开始&gt;
	示例："&lt;从[Ancient era]开始&gt;"

	适用范围：有前提的

:::
::: details &lt;若以[era]开始&gt;
	示例："&lt;若以[Ancient era]开始&gt;"

	适用范围：有前提的

:::
::: details &lt;若其他文明尚未研究这个科技&gt;
	适用范围：有前提的

:::
::: details &lt;发现[techFilter]后&gt;
	示例："&lt;发现[Agriculture]后&gt;"

	适用范围：有前提的

:::
::: details &lt;发现[techFilter]前&gt;
	示例："&lt;发现[Agriculture]前&gt;"

	适用范围：有前提的

:::
::: details &lt;研究[techFilter]期间&gt;
	This condition is fulfilled while the technology is actively being researched (it is the one research points are added to)

	示例："&lt;研究[Agriculture]期间&gt;"

	适用范围：有前提的

:::
::: details &lt;若没有其他文明推行此政策/信条&gt;
	适用范围：有前提的

:::
::: details &lt;若还没有文明推行此[policy/belief]&gt;
	示例："&lt;若还没有文明推行此[Oligarchy]&gt;"

	适用范围：有前提的

:::
::: details &lt;在推行[policy/belief]后&gt;
	示例："&lt;在推行[Oligarchy]后&gt;"

	适用范围：有前提的

:::
::: details &lt;在推行[policy/belief]前&gt;
	示例："&lt;在推行[Oligarchy]前&gt;"

	适用范围：有前提的

:::
::: details &lt;在建立万神殿之后&gt;
	适用范围：有前提的

:::
::: details &lt;在建立万神殿之前&gt;
	适用范围：有前提的

:::
::: details &lt;在创立宗教之前&gt;
	适用范围：有前提的

:::
::: details &lt;在创立宗教之后&gt;
	适用范围：有前提的

:::
::: details &lt;在加强宗教信仰之前&gt;
	适用范围：有前提的

:::
::: details &lt;在加强宗教信仰之后&gt;
	适用范围：有前提的

:::
::: details &lt;在出现大先知后&gt;
	适用范围：有前提的

:::
::: details &lt;如果已建造[buildingFilter]&gt;
	示例："&lt;如果已建造[Culture]&gt;"

	适用范围：有前提的

:::
::: details &lt;如果未建造[buildingFilter]&gt;
	示例："&lt;如果未建造[Culture]&gt;"

	适用范围：有前提的

:::
::: details &lt;如果在所有[cityFilter]城市已建造[buildingFilter]&gt;
	示例："&lt;如果在所有[Culture]城市已建造[in all cities]&gt;"

	适用范围：有前提的

:::
::: details &lt;如果在至少[positiveAmount]个[cityFilter]城市已建造[buildingFilter]&gt;
	示例："&lt;如果在至少[Culture]个[3]城市已建造[in all cities]&gt;"

	适用范围：有前提的

:::
::: details &lt;如果任何人建造了[buildingFilter]&gt;
	示例："&lt;如果任何人建造了[Culture]&gt;"

	适用范围：有前提的

:::
::: details &lt;如果还没有人建造过[buildingFilter]&gt;
	示例："&lt;如果还没有人建造过[Culture]&gt;"

	适用范围：有前提的

:::
::: details &lt;拥有[resource]的&gt;
	示例："&lt;拥有[Iron]的&gt;"

	适用范围：有前提的

:::
::: details &lt;未拥有[resource]的&gt;
	示例："&lt;未拥有[Iron]的&gt;"

	适用范围：有前提的

:::
::: details &lt;当[stat/resource]超过[amount]时&gt;
	Stats refers to the accumulated stat, not stat-per-turn. Therefore, does not support Happiness - for that use 'when above [amount] Happiness'

	示例："&lt;当[3]超过[Culture]时&gt;"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	适用范围：有前提的

:::
::: details &lt;当[stat/resource]低于[amount]时&gt;
	Stats refers to the accumulated stat, not stat-per-turn. Therefore, does not support Happiness - for that use 'when below [amount] Happiness'

	示例："&lt;当[3]低于[Culture]时&gt;"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	适用范围：有前提的

:::
::: details &lt;当[stat/resource]处在[amount]至[amount2]内时&gt;
	Stats refers to the accumulated stat, not stat-per-turn. Therefore, does not support Happiness. 'Between' is inclusive - so 'between 1 and 5' includes 1 and 5.

	示例："&lt;当[3]处在[3]至[Culture]内时&gt;"

	此词条的效果可被 &lt;(受游戏速度影响)&gt;

	适用范围：有前提的

:::
::: details &lt;在这个城市中&gt;
	适用范围：有前提的

:::
::: details &lt;在[cityFilter]城市中&gt;
	示例："&lt;在[in all cities]城市中&gt;"

	适用范围：有前提的

:::
::: details &lt;在连接至首都的城市中&gt;
	适用范围：有前提的

:::
::: details &lt;在信奉[religionFilter]宗教的城市中&gt;
	示例："&lt;在信奉[major]宗教的城市中&gt;"

	适用范围：有前提的

:::
::: details &lt;在不信奉[religionFilter]宗教的城市中&gt;
	示例："&lt;在不信奉[major]宗教的城市中&gt;"

	适用范围：有前提的

:::
::: details &lt;在存在主要宗教的城市中&gt;
	适用范围：有前提的

:::
::: details &lt;在存在强化信仰后的宗教的城市中&gt;
	适用范围：有前提的

:::
::: details &lt;在信仰我们宗教城市中&gt;
	适用范围：有前提的

:::
::: details &lt;在建有[buildingFilter]的城市中&gt;
	示例："&lt;在建有[Culture]的城市中&gt;"

	适用范围：有前提的

:::
::: details &lt;在未建造[buildingFilter]的城市中&gt;
	示例："&lt;在未建造[Culture]的城市中&gt;"

	适用范围：有前提的

:::
::: details &lt;在至少有[positiveAmount][populationFilter]的城市中&gt;
	示例："&lt;在至少有[3][Followers of this Religion]的城市中&gt;"

	适用范围：有前提的

:::
::: details &lt;在有[nonNegativeAmount][populationFilter]的城市中&gt;
	示例："&lt;在有[3][Followers of this Religion]的城市中&gt;"

	适用范围：有前提的

:::
::: details &lt;在[amount]到[amount2][populationFilter]的城市中&gt;
	'Between' is inclusive - so 'between 1 and 5' includes 1 and 5.

	示例："&lt;在[3]到[3][Followers of this Religion]的城市中&gt;"

	适用范围：有前提的

:::
::: details &lt;在少于[amount][populationFilter]的城市中&gt;
	示例："&lt;在少于[3][Followers of this Religion]的城市中&gt;"

	适用范围：有前提的

:::
::: details &lt;如果城市有军队驻扎&gt;
	适用范围：有前提的

:::
::: details &lt;令[mapUnitFilter]单位&gt;
	示例："&lt;令[Wounded]单位&gt;"

	适用范围：有前提的

:::
::: details &lt;当处于[mapUnitFilter]状态&gt;
	示例："&lt;当处于[Wounded]状态&gt;"

	适用范围：有前提的

:::
::: details &lt;令拥有[promotion]的单位&gt;
	Also applies to units with temporary status

	示例："&lt;令拥有[Shock I]的单位&gt;"

	适用范围：有前提的

:::
::: details &lt;令没有[promotion]的单位&gt;
	Also applies to units with temporary status

	示例："&lt;令没有[Shock I]的单位&gt;"

	适用范围：有前提的

:::
::: details &lt;对战城市&gt;
	适用范围：有前提的

:::
::: details &lt;对战[mapUnitFilter]单位&gt;
	示例："&lt;对战[Wounded]单位&gt;"

	适用范围：有前提的

:::
::: details &lt;vs [combatantFilter]&gt;
	示例："&lt;vs [City]&gt;"

	适用范围：有前提的

:::
::: details &lt;当与拥有较你更多城市的文明作战时&gt;
	适用范围：有前提的

:::
::: details &lt;攻击时&gt;
	适用范围：有前提的

:::
::: details &lt;防御时&gt;
	适用范围：有前提的

:::
::: details &lt;在[tileFilter]地块上作战时&gt;
	示例："&lt;在[Farm]地块上作战时&gt;"

	适用范围：有前提的

:::
::: details &lt;在异国大陆上时&gt;
	适用范围：有前提的

:::
::: details &lt;当和[mapUnitFilter]单位相邻时&gt;
	示例："&lt;当和[Wounded]单位相邻时&gt;"

	适用范围：有前提的

:::
::: details &lt;当超过[positiveAmount]生命值时&gt;
	示例："&lt;当超过[3]生命值时&gt;"

	适用范围：有前提的

:::
::: details &lt;当低于[positiveAmount]生命值时&gt;
	示例："&lt;当低于[3]生命值时&gt;"

	适用范围：有前提的

:::
::: details &lt;当低于[positiveAmount]移动力时&gt;
	示例："&lt;当低于[3]移动力时&gt;"

	适用范围：有前提的

:::
::: details &lt;当高于[nonNegativeAmount]移动力时&gt;
	示例："&lt;当高于[3]移动力时&gt;"

	适用范围：有前提的

:::
::: details &lt;当未进行执行其他行动时&gt;
	适用范围：有前提的

:::
::: details &lt;当与[mapUnitFilter]单位叠加时 &gt;
	示例："&lt;当与[Wounded]单位叠加时 &gt;"

	适用范围：有前提的

:::
::: details &lt;当没有与[mapUnitFilter]单位叠加时 &gt;
	示例："&lt;当没有与[Wounded]单位叠加时 &gt;"

	适用范围：有前提的

:::
::: details &lt;当与[nonNegativeAmount2]到[nonNegativeAmount]个[tileFilter]地块相邻时&gt;
	示例："&lt;当与[3]到[3]个[Farm]地块相邻时&gt;"

	适用范围：有前提的

:::
::: details &lt;在[tileFilter]地块上&gt;
	示例："&lt;在[Farm]地块上&gt;"

	适用范围：有前提的

:::
::: details &lt;在非[tileFilter]地块上&gt;
	示例："&lt;在非[Farm]地块上&gt;"

	适用范围：有前提的

:::
::: details &lt;在[tileFilter][positiveAmount]格范围内&gt;
	示例："&lt;在[3][Farm]格范围内&gt;"

	适用范围：有前提的

:::
::: details &lt;在与[tileFilter]相邻的地块&gt;
	示例："&lt;在与[Farm]相邻的地块&gt;"

	适用范围：有前提的

:::
::: details &lt;在与[tileFilter]不相邻的地块&gt;
	示例："&lt;在与[Farm]不相邻的地块&gt;"

	适用范围：有前提的

:::
::: details &lt;海洋地图&gt;
	适用范围：有前提的

:::
::: details &lt;[regionType]宗教中&gt;
	示例："&lt;[Hybrid]宗教中&gt;"

	适用范围：有前提的

:::
::: details &lt;除[regionType]之外的的所有宗教中&gt;
	示例："&lt;除[Hybrid]之外的的所有宗教中&gt;"

	适用范围：有前提的

:::
::: details &lt;[countable]等于[countable2]时&gt;
	示例："&lt;[1000]等于[1000]时&gt;"

	适用范围：有前提的

:::
::: details &lt;[countable]不等于[countable2]时&gt;
	示例："&lt;[1000]不等于[1000]时&gt;"

	适用范围：有前提的

:::
::: details &lt;[countable]大于[countable2]时&gt;
	示例："&lt;[1000]大于[1000]时&gt;"

	适用范围：有前提的

:::
::: details &lt;[countable]小于[countable2]时&gt;
	示例："&lt;[1000]小于[1000]时&gt;"

	适用范围：有前提的

:::
::: details &lt;[countable]大于[countable2]且小于[countable3]时&gt;
	'Between' is inclusive - so 'between 1 and 5' includes 1 and 5.

	示例："&lt;[1000]大于[1000]且小于[1000]时&gt;"

	适用范围：有前提的

:::
::: details &lt;当被[mapUnitFilter]单位搭载时 &gt;
	示例："&lt;当被[Wounded]单位搭载时 &gt;"

	适用范围：有前提的

:::
::: details &lt;如果模组[modFilter]已启用&gt;
	示例："&lt;如果模组[DeCiv Redux]已启用&gt;"

	适用范围：有前提的

:::
::: details &lt;如果模组[modFilter]未启用&gt;
	示例："&lt;如果模组[DeCiv Redux]未启用&gt;"

	适用范围：有前提的

:::
## 触发条件词条
::: note

    可添加到触发型词条的特殊条件，使它们在特定行动时激活。
:::

::: details &lt;一旦研究[techFilter]&gt;
	示例："&lt;一旦研究[Agriculture]&gt;"

	适用范围：触发条件

:::
::: details &lt;一旦进入[era]&gt;
	示例："&lt;一旦进入[Ancient era]&gt;"

	适用范围：触发条件

:::
::: details &lt;一旦进入新时代&gt;
	适用范围：触发条件

:::
::: details &lt;一旦推行[policy/belief]&gt;
	示例："&lt;一旦推行[Oligarchy]&gt;"

	适用范围：触发条件

:::
::: details &lt;一旦和[civFilter]文明宣战时&gt;
	示例："&lt;一旦和[City-States]文明宣战时&gt;"

	适用范围：触发条件

:::
::: details &lt;一旦被[civFilter]文明宣战时&gt;
	示例："&lt;一旦被[City-States]文明宣战时&gt;"

	适用范围：触发条件

:::
::: details &lt;一旦和[civFilter]文明交战时&gt;
	示例："&lt;一旦和[City-States]文明交战时&gt;"

	适用范围：触发条件

:::
::: details &lt;一旦与[civFilter]文明达成停战协议时&gt;
	示例："&lt;一旦与[City-States]文明达成停战协议时&gt;"

	适用范围：触发条件

:::
::: details &lt;一旦宣布友谊宣言&gt;
	适用范围：触发条件

:::
::: details &lt;一旦宣布共同防御条约&gt;
	适用范围：触发条件

:::
::: details &lt;一旦进入黄金时代&gt;
	适用范围：触发条件

:::
::: details &lt;一旦结束黄金时代&gt;
	适用范围：触发条件

:::
::: details &lt;一旦占领城市&gt;
	适用范围：触发条件，单位触发条件

:::
::: details &lt;一旦失去城市&gt;
	适用范围：触发条件

:::
::: details &lt;一旦建立城市&gt;
	适用范围：触发条件

:::
::: details &lt;一旦修建[improvementFilter]&gt;
	示例："&lt;一旦修建[All Road]&gt;"

	适用范围：触发条件，单位触发条件

:::
::: details &lt;一旦发现自然奇观&gt;
	适用范围：触发条件

:::
::: details &lt;一旦建造[buildingFilter]&gt;
	示例："&lt;一旦建造[Culture]&gt;"

	适用范围：触发条件

:::
::: details &lt;一旦[cityFilter]建造[buildingFilter]&gt;
	示例："&lt;一旦[Culture]建造[in all cities]&gt;"

	适用范围：触发条件

:::
::: details &lt;一旦获得[baseUnitFilter]单位&gt;
	示例："&lt;一旦获得[Melee]单位&gt;"

	适用范围：触发条件

:::
::: details &lt;一旦失去[mapUnitFilter]单位时&gt;
	示例："&lt;一旦失去[Wounded]单位时&gt;"

	适用范围：触发条件

:::
::: details &lt;一旦回合结束&gt;
	适用范围：触发条件，单位触发条件

:::
::: details &lt;一旦回合开始&gt;
	适用范围：触发条件，单位触发条件

:::
::: details &lt;一旦创立万神殿&gt;
	适用范围：触发条件

:::
::: details &lt;一旦创立宗教&gt;
	适用范围：触发条件

:::
::: details &lt;一旦强化宗教&gt;
	适用范围：触发条件

:::
::: details &lt;一旦消耗[mapUnitFilter]单位&gt;
	示例："&lt;一旦消耗[Wounded]单位&gt;"

	适用范围：触发条件

:::
## 单位触发条件词条
::: note

    可添加到单位触发型词条的特殊条件，使它们在特定行动时激活。
:::

::: details &lt;一旦占领城市&gt;
	适用范围：触发条件，单位触发条件

:::
::: details &lt;一旦修建[improvementFilter]&gt;
	示例："&lt;一旦修建[All Road]&gt;"

	适用范围：触发条件，单位触发条件

:::
::: details &lt;一旦回合结束&gt;
	适用范围：触发条件，单位触发条件

:::
::: details &lt;一旦回合开始&gt;
	适用范围：触发条件，单位触发条件

:::
::: details &lt;每次战斗&gt;
	适用范围：单位触发条件

:::
::: details &lt;一旦攻击[mapUnitFilter]单位&gt;
	Can apply triggers to to damaged unit by setting the first parameter to 'Target Unit'

	示例："&lt;一旦攻击[Wounded]单位&gt;"

	适用范围：单位触发条件

:::
::: details &lt;一旦击败[mapUnitFilter]单位&gt;
	示例："&lt;一旦击败[Wounded]单位&gt;"

	适用范围：单位触发条件

:::
::: details &lt;一旦死亡&gt;
	适用范围：单位触发条件

:::
::: details &lt;一旦晋升&gt;
	适用范围：单位触发条件

:::
::: details &lt;一旦获得[promotion]&gt;
	示例："&lt;一旦获得[Shock I]&gt;"

	适用范围：单位触发条件

:::
::: details &lt;一旦失去[promotion]&gt;
	示例："&lt;一旦失去[Shock I]&gt;"

	适用范围：单位触发条件

:::
::: details &lt;一旦获得[promotion]&gt;
	示例："&lt;一旦获得[Shock I]&gt;"

	适用范围：单位触发条件

:::
::: details &lt;一旦失去[promotion]&gt;
	示例："&lt;一旦失去[Shock I]&gt;"

	适用范围：单位触发条件

:::
::: details &lt;一旦在单次战斗中丢失至少[positiveAmount]HP&gt;
	示例："&lt;一旦在单次战斗中丢失至少[3]HP&gt;"

	适用范围：单位触发条件

:::
::: details &lt;一旦在[tileFilter]地块上结束回合&gt;
	示例："&lt;一旦在[Farm]地块上结束回合&gt;"

	适用范围：单位触发条件

:::
::: details &lt;一旦发现[tileFilter]地块&gt;
	示例："&lt;一旦发现[Farm]地块&gt;"

	适用范围：单位触发条件

:::
::: details &lt;一旦踏入[tileFilter]&gt;
	示例："&lt;一旦踏入[Farm]&gt;"

	适用范围：单位触发条件

:::
## 单位行为修饰词条
::: note

    可作为条件添加到单位行动词条的修饰符
:::

::: details &lt;通过消耗此单位&gt;
	适用范围：单位行为修饰

:::
::: details &lt;消耗[amount]移动力&gt;
	Will consume up to [amount] of Movement to execute

	示例："&lt;消耗[3]移动力&gt;"

	适用范围：单位行为修饰

:::
::: details &lt;耗尽移动力&gt;
	Will consume all Movement to execute

	适用范围：单位行为修饰

:::
::: details &lt;消耗[nonNegativeAmount]移动力&gt;
	Requires [nonNegativeAmount] of Movement to execute. Unit's Movement is rounded up

	示例："&lt;消耗[3]移动力&gt;"

	适用范围：单位行为修饰

:::
::: details &lt;花费[stats]&gt;
	A positive Integer value will be subtracted from your stock. Food and Production will be removed from Closest City's current stock

	示例："&lt;花费[+1 Gold, +2 Production]&gt;"

	适用范围：单位行为修饰

:::
::: details &lt;消耗[amount]单位[stockpiledResource]&gt;
	A positive Integer value will be subtracted from your stock. Do not confuse with "Costs [amount] [stockpiledResource]" (uppercase 'C') for Improvements, Buildings, and Units.

	示例："&lt;消耗[3]单位[Mana]&gt;"

	适用范围：单位行为修饰

:::
::: details &lt;移除[promotion]&gt;
	Removes the promotion/status from the unit - this is not a cost, units will be able to activate the action even without the promotion/status. To limit, use &lt;with the [promotion] promotion&gt; conditional

	示例："&lt;移除[Shock I]&gt;"

	适用范围：单位行为修饰

:::
::: details &lt;一次&gt;
	适用范围：单位行为修饰

:::
::: details &lt;[positiveAmount]次&gt;
	示例："&lt;[3]次&gt;"

	适用范围：单位行为修饰

:::
::: details &lt;额外[nonNegativeAmount]次&gt;
	示例："&lt;额外[3]次&gt;"

	适用范围：单位行为修饰

:::
::: details &lt;在单位被消耗后&gt;
	适用范围：单位行为修饰

:::
::: details &lt;with [amount] priority&gt;
	How often this action is used, a higher value means more often and that it should be on an earlier page. 100 is very frequent, 50 is somewhat frequent, less than 25 is press one time for multi-turn movement. A Rare case is &gt; 100 if a button is something like add in capital, promote or something, we need to inform the player that taking the action is an option.

	示例："&lt;with [3] priority&gt;"

	此词条自动对用户隐藏。

	适用范围：单位行为修饰，元修饰

:::
## 元修饰词条
::: note

    可添加到其他词条的修饰符，改变用户体验而非行为
:::

::: details &lt;持续[nonNegativeAmount]回合&gt;
	Turns this unique into a trigger, activating this unique as a *global* unique for a number of turns

	示例："&lt;持续[3]回合&gt;"

	适用范围：元修饰

:::
::: details &lt;with [amount] priority&gt;
	How often this action is used, a higher value means more often and that it should be on an earlier page. 100 is very frequent, 50 is somewhat frequent, less than 25 is press one time for multi-turn movement. A Rare case is &gt; 100 if a button is something like add in capital, promote or something, we need to inform the player that taking the action is an option.

	示例："&lt;with [3] priority&gt;"

	此词条自动对用户隐藏。

	适用范围：单位行为修饰，元修饰

:::
::: details &lt;对玩家隐藏&gt;
	适用范围：元修饰

:::
::: details &lt;每有1个[countable]&gt;
	Works for positive numbers only

	示例："&lt;每有1个[1000]&gt;"

	适用范围：元修饰

:::
::: details &lt;每个相邻[tileFilter]&gt;
	Works for positive numbers only

	示例："&lt;每个相邻[Farm]&gt;"

	适用范围：元修饰

:::
::: details &lt;每有[positiveAmount]个[countable]&gt;
	Works for positive numbers only

	示例："&lt;每有[3]个[1000]&gt;"

	适用范围：元修饰

:::
::: details &lt;(受游戏速度影响)&gt;
	Can only be applied to certain uniques, see details of each unique for specifics

	适用范围：元修饰

:::
::: details &lt;(受游戏进程影响，且至多到[relativeAmount]%)&gt;
	Can only be applied to certain uniques, see details of each unique for specifics

	示例："&lt;(受游戏进程影响，且至多到[+20]%)&gt;"

	适用范围：元修饰

:::
::: details &lt;Civilopedia link [pediaLink]&gt;
	Allows linking a unique to any Civilopedia page when it is listed in Civilopedia normally. This overrides automatic links to objects in the unique's parameters.

	示例："&lt;Civilopedia link [Units/Settler]&gt;"

	此词条自动对用户隐藏。

	适用范围：元修饰

:::
::: details &lt;Suppress warning [validationWarning]&gt;
	Allows suppressing specific validation warnings. Errors, deprecation warnings, or warnings about untyped and non-filtering uniques should be heeded, not suppressed, and are therefore not accepted. Note that this can be used in ModOptions, in the uniques a warning is about, or as modifier on the unique triggering a warning - but you still need to be specific. Even in the modifier case you will need to specify a sufficiently selective portion of the warning text as parameter.

	示例："&lt;Suppress warning [Tinman is supposed to automatically upgrade at tech Clockwork, and therefore Servos for its upgrade Mecha may not yet be researched! -or- *is supposed to automatically upgrade*]&gt;"

	此词条不支持条件。

	此词条自动对用户隐藏。

	适用范围：触发型，地形修正，游戏速度，模组选项，元修饰

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
| `foundingOrEnhancing` | `founding` or `enhancing`. |
| `fraction` | Indicates a fractional number, which can be negative. Also accepts Countable expressions. |
| `improvementName` | The name of any improvement excluding 'Cancel improvement order' |
| `leaderTitle` | Provides a leader title that includes the leader's name in parameters. |
| `luaFunction` | A Lua function reference in the form [modName:]functionName. |
| `modFilter` | A Mod name, case-sensitive _or_ a simple wildcard filter beginning and ending in an Asterisk, case-insensitive.
Note that this must use the Mod name as Unciv displays it, not the Repository name.
There is a conversion affecting dashes and leading/trailing blanks. Please make sure not to get confused. |
| `nonNegativeAmount` | This indicates a non-negative whole number, larger than or equal to zero, a '+' sign is optional. Also accepts Countable expressions. |
| `pediaLink` | A Civilopedia link in the form category/entry. |
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
| `tileFilter` | Anything that can be used either in an improvementFilter or in a terrainFilter can be used here, plus 'unimproved' |
| `unitNameGroup` | The name of a unit name group found in UnitNameGroups.json, or one of their unique tags. |
| `unitTriggerTarget` | `This Unit` or `Target Unit`. |
| `unitType` | Can be 'Land', 'Water', 'Air', any unit type, a filtering Unique on a unit type, or a multi-filter of these. |
| `validationWarning` | Suppresses one specific Ruleset validation warning. This can specify the full text verbatim including correct upper/lower case, or it can be a wildcard case-insensitive simple pattern starting and ending in an asterisk ('*'). If the suppression unique is used within an object or as modifier (not ModOptions), the wildcard symbols can be omitted, as selectivity is better due to the limited scope. |
| `victoryType` | The name of any victory type: 'Cultural', 'Diplomatic', 'Domination', 'Scientific', 'Time' or one of your mod's VictoryTypes.json names. |
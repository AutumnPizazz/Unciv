---
title: Unciv 原版更新日志
---

## Unciv 原版更新日志

本文为 [Unciv 官方更新日志](https://github.com/yairm210/Unciv/blob/master/changelog.md) 的中文翻译。仅收录近期版本，完整历史请参见英文原文。

---

## 4.21.0
- 文明百科中显示领袖个性特征 — By SomeTroglodyte
- 重制胜利界面 — By cy-elec
- Modding：允许为「数值或以上」类 uniques 传入参数 0 — By SeventhM
- 大量小修复 — By Angais（LLM 辅助）
- 修复 `allICivilopediaText` 中 `EventChoice` 条目重复的问题 — By xplon

## 4.20.19
- 修复城市界面音频问题（待验证）
- 同一风格被多个文明共享时，文明图标与风格背景不再冲突
- 大量小修复 — By Angais（LLM 辅助）

## 4.20.18
- 条件性工人 uniques 在禁用后不再引发崩溃
- 改良设施快捷键冲突时不再触发两次
- 文明专属图像优先于风格图像
- 修复 A\* 寻路中「无移动力的单位将已占据地块视为可通行」的 Bug
- 南侧边缘的城市边界与城市按钮不再消失
- CPU 性能优化
- By unciv-loof：
  - 地图编辑器中新增镜像类型选项
  - 调整地图类型排列顺序

## 4.20.17
- CPU 性能优化
- 修复：在地图编辑器中移除蛮族营地时避免崩溃
- 调整 Boreal（北方针叶林）地图类型的湖泊与海岸线生成 — By unciv-loof
- 控制台：允许设置战略资源储量 — By SomeTroglodyte

## 4.20.16
- By unciv-loof：
  - 根据移动力上限限制 `AdditionalAttacks` 带来的战斗力加成
  - 修复德国文明独特能力
  - 修复边境军事威慑力计算
- 使「蛮族营地」改良设施可被 Mod 自定义 — By SomeTroglodyte
- By BobbyCobby：
  - 更清晰的 Mod 更新图标
- 修复切换规则集时地图预览崩溃 — By Angais

## 4.20.15
- 消耗库存资源的改良设施改为开始建造时即扣除资源（与建筑和单位一致）
- 减少大地图的内存占用
- 亲王（Prince）难度的 AI 不再享有单位造价减免（与玩家 1:1 平等）
- 要求 `CreatesOneImprovement` 的目标地块必须归属于建造城市 — By superdusto
- 资源总览标签页改进 — By SomeTroglodyte
- Modding：修复 Personality uniques 的验证问题 — By mvanhorn

## 4.20.14
- 修复「下一回合按钮黑屏」问题
- 右键菜单指示器不再常驻显示
- 缩放时城市按钮正确居中于地块
- 城市产出图标不再与人口图标重叠
- By SomeTroglodyte：
  - 手势触发时长可在「选项 → 高级」中自定义
  - 修复 unique 构建器中「选择音乐曲目」触发项
  - 修复城邦始终显示「友好」个性的问题
- 防止自动化清理 `CreatesOneImprovement` 标记残留 — By superdusto

## 4.20.13
- 大幅降低大地图的内存占用
- Modding：显示 JSON 文件错误的具体位置
- By SomeTroglodyte：
  - 右键菜单视觉指示器
  - Modding：修复「空名称」问题
  - 修复开疆扩土摧毁蛮族营地后遗留过期任务的问题
  - 城市购买地块的右键菜单，支持一次购买多个地块
- 避免镜头聚焦于隐藏奇观位置 — By xplon
- 验证晋升时尊重替代前置条件 — By superdusto

## 4.20.12
- By xplon：
  - 翻译间谍名称时隐藏图标
  - 提示可翻译规则集名称冲突
- By SomeTroglodyte：
  - 改进右键菜单指示器大小
  - 控制台新增 `civ add` 和 `civ remove` 命令
- 高级游戏设置中新增缺失的换行 — By unciv-loof

## 4.20.11
- 修复伟人建造伟人改良设施时出现重复按钮的问题
- 修复 Android 自定义文件目录
- 允许 Mod 常量验证器中值为 0 — By unciv-loof
- By SomeTroglodyte：
  - 修复通知中统计符号的黑色描边
  - 修复 `TriggerUponLosingUnit`（单位阵亡触发）
  - 修复 Android 屏幕键盘显示/隐藏问题
  - 轻微 CPU 性能优化
- 为沼泽地块增加「植被」标签 — By EmperorPinguin

## 4.20.10
- By SomeTroglodyte：
  - 免费政策通知点击后直接打开政策界面
  - 新增「选择音乐曲目」可触发 unique
  - 优化剧本启动，减少意外情况
- Boreal（北方针叶林）地图类型 — By unciv-loof

## 4.20.9
- AI 在游戏后期不再停止建造城市
- 间谍管理界面新增国家与首都指示器
- 工人单位尽量分散作业 — By Ambeco
- By SomeTroglodyte：
  - 正确显示改良设施维护费的修正值
  - 修复注释嵌套 typed uniques 的翻译生成
- By unciv-loof：
  - 阻止 AI 谴责已灭亡的文明
  - 多人游戏元数据预览中不再显示（Unknown）文明名
- 修复「受过教育的精英」政策不赠送伟人的问题 — By AutumnPizazz

## 4.20.8
- 程序化地图的无缝世界包裹生成 — By Romelium
- By SomeTroglodyte：
  - 改善小屏幕下的「战报表」
  - CPU 性能优化
- RAM 性能优化 — By Ambeco

## 4.20.7

- 大幅地图的渲染性能优化
- 修复军事存在检查的 Bug — by unciv-loof
- 新增 "cannot build xx buildings" unique — by chenxing61

## 4.20.6

By SeventhM:
- 允许在全局/政策/时代 unique 中启用大使馆，不再仅限于科技
- 修复派遣大使馆的冗余条件要求
- 新增已工作地块/人口的计数条件
- 修复大先知花费异常计算的边界情况

- 改进语言处理，优化首次运行语言选择器的体验 — By SomeTroglodyte
- CPU 性能优化 — By Ambeco

## 4.20.5

- **模组开发**：正确处理规则集禁用 unique 时的过滤 unique
- **模组开发**：检测 AI 可能陷入死循环的事件链

By SomeTroglodyte:
- 改进文明百科中与大使馆相关的外交概念说明
- 修复地图编辑器崩溃

- 新增"单位阵亡时触发" unique — By PLynx01
- 处理所有屏幕方向的显示裁切，修复旋转问题 — By Gatien-L

## 4.20.4

- **模组开发**：当 unique 在当前规则集中被禁用时，忽略与该规则集相关的错误

By SomeTroglodyte:
- 宗教/间谍系统被禁用时隐藏相关需求
- 修复在间谍界面将间谍移动到未知文明城市时的空指针异常

By unciv-loof:
- 补充城市抵抗状态的缺失表格行

By Ambeco:
- Unciv 可直接接收游戏邀请或好友添加链接
- 防止更多类型的随机结果被 SL 利用

- 新增选项：禁止工人移除植被 — By Emandac

## 4.20.3

- 从旧存档开始新游戏时，自定义地图将被保留
- 被摧毁城市中的间谍正确撤离
- 观察者可以再次查看建筑列表

By unciv-loof:
- 违背不再刺探承诺前弹出确认框
- ConstructImprovementInstantly 现在会清除地块改良队列

- 修复城市中单位选择的 Bug — By ValeraShimchuck

By Ambeco:
- AI：攻击前最小化移动
- PathingMap 现在支持 BFS，将移动消耗纳入考虑

## 4.20.2

By unciv-loof:
- 恢复显示设置中禁用单位模型渲染的选项
- 关闭新游戏界面警告后不再强制重置模组选择
- 点击勒索和征服城邦任务时，地图视角居中到目标首都
- 宣战弹窗中不再显示空消息
- AI 可同时谴责多个文明

- 允许通过触发器获得的信仰使用可触发的 unique — by SeventhM
- 海洋浮冰应再次生成 — by Ambeco

## 4.20.1

- 大幅降低内存消耗
- 地块进入文明领土时移除野蛮人营地
- 对第三方的和平/宣战声明不再被视为贸易"赠礼"
- 修复城市购买时的罕见崩溃
- 具有"迁都时迁移至新首都"属性的建筑在城市被占领时不再被摧毁
- 桌面可执行文件：最大 JVM 内存实际提升至 4GB
- 通知按顺序在 UI 中显示

By unciv-loof:
- 谴责弹窗支持可模组化的消息和音效
- 第 0 回合自动存档

## 4.20.0

By unciv-loof:
- 修复大使馆贸易逻辑 Bug
- 其他文明回应要求后弹出通知

By SeventhM:
- 新增当前单位移动力条件判断
- 修复禁用的 unique 在意想不到的地方被忽略的问题

- 工人自动化性能的理论性改进 — By Ambeco
- 修复多人游戏预览描述中的翻译 — By evanofficial（新贡献者！）
- 新增"签署和平条约时触发" unique — By PLynx01

## 4.19.19

- 删除包含地块集的模组后重置地块集设置
- AI 忽略已灭亡文明的要求/宣友

By Ambeco:
- AStar 寻路不再将河流视为多回合障碍
- 平面六角地图不再崩溃

- Bug 修复：不要在已有平民单位的城市中尝试购买传教士 — By EmperorPinguin

## 4.19.18

- 忽略免费晋升的晋升路径成本
- 允许在获得经验值之前获得免费晋升

By Ambeco:
- 模组可以有多个海洋和海岸地形
- AStar 移动经过与 Classic 相同的格子

- 新增 showDemographics 选项 — By ICanSeeForever
- 强制解散单位考虑退款能力和晋升 — By unciv-loof
- DenounceWillingness 人格特质 — by unciv-loof
- AI：只有在能创立自己宗教时才因传教而生气 — By EmperorPinguin

## 4.19.17

- 新游戏界面切换规则集不再允许零胜利类型
- 用户无法通过 "-50" 按钮提供负数金币

By unciv-loof:
- 通知显示谁跳过或退出了玩家
- 新获取的模组条目尊重当前过滤器
- 宣战 + 宣友 UI 改进

By Ambeco:
- 修复备用寻路 Bug
- 单位行动和单位触发器具有可模组化的优先级
- 消除了大多数硬编码地形

## 4.19.16

- 默认最大内存从 1GB 提升至 4GB——2026 年了，让玩家尽情发挥吧
- 间谍城市视野限制功能 — By ICanSeeForever
- 淡水自然奇观不再显示为湖泊 — By Ambeco
- 根据游戏速度调整要求和"我们爱国王日"持续时间 — By unciv-loof

## 4.19.15

- 更明确地警告大地图可能导致内存崩溃
- 规则集验证中捕获带有 occursOn 的基础地形
- 移除含有改良设施创建建筑但无城市地块的改良设施时不再崩溃
- 城市可以攻击已揭示的隐身单位

By Ambeco:
- 添加备用寻路算法
- 丛林不再生成在丘陵上
- 地图生成时忽略稀有要素放置普通地块

- 为可计数表达式添加 max()、min() 函数 — By AutumnPizazz

## 4.19.14

- 在地图上为当前提供的资源添加星标图标
- 统一下 N 次晋升与单次晋升的经验值成本
- AI 自动谴责 — By unciv-loof

By AutumnPizazz:
- 新增游戏选项隐藏胜利画面统计数据
- 将 'Set [stockpile] to [amount]' 改为 'Set [stockpile] to [countable]'

- 地图生成支持多种山脉、丘陵、冰层、雪地、湖泊和植被 — By Ambeco

## 4.19.13

- 清除被复活文明的修正器和倒计时
- 傀儡城市正确移除"由建筑标记为待改良"的标记

模组开发：
- 特定金币购买费用 unique 始终覆盖默认费用
- 放置在单位上的全局 unique 可接受单位触发条件

By unciv-loof:
- 边境扩张逻辑优化：
  - 考虑工作范围边缘的邻近加成资源
  - 不考虑未探索地块
  - 略微优先选择有争议的地块

## 4.19.12

- 被覆盖政策分支中的政策不再"残留"影响 UI
- 在资源概览中添加已库存战略资源
- 新增 "Set [stockpile] to [amount]" 触发型 unique
- 未经用户允许不自动复制模组列表下载错误到剪贴板
- 加载缺少模组的游戏时先下载所有可用模组，再通知问题
- 查找文明等效单位时，若替代单位在规则集中不存在，不再崩溃

## 4.19.11

- "单位可晋升"通知显示正确的单位名称
- 修复第三方文明的宣友外交变化
- 对累积型外交修正器添加限制，特别是针对受保护城邦的负面修正器
- 城邦任务采用"一致性随机"
- 修复伟人不创建改良设施的问题
- 当单位无法接近目标时派遣不再导致单位图片消失
- "Must not be on [amount] largest landmasses" 兼容资源 — By chenxing61

## 4.19.10

- 新增 `[cityFilter] Cities of [civFilter] Civilizations` 可计数条件 — By RobLoach
- 人口食物消耗 unique — By PLynx01
- 修复胜利画面图表对观察者的可见性和布局 — By SomeTroglodyte
- 在线游戏可自定义游戏时长 — By AubertJocelyn（新贡献者！）

---

::: tip 更多版本
以上仅收录近期版本（4.19.10 起）。完整历史版本请参见 [Unciv 官方更新日志](https://github.com/yairm210/Unciv/blob/master/changelog.md)。
:::

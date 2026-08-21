---
title: UncivCN 更新日志
---

# UncivCN 更新日志

版本号规则：上游版本 + CN 子版本号（`.1`、`.2`、`.3`…，同一上游版本可发多个 CN 子版本，如 4.20.8.1 → 4.20.8.4；跟进新上游后从 `.1` 重新开始，如 4.21.5 → 4.21.5.1）。

::: tip 上游更新日志
上游（原版）版本记录见 [Unciv 官方更新日志](https://github.com/yairm210/Unciv/blob/master/changelog.md)（GitHub）。
:::

## 未发布

- 模组支持：单位级变量现可通过条件、触发、每回合产出、Lua 与单位 UI 读写——经验系统本身也已迁移到 `Experience` 单位变量（旧存档自动迁移），模组可通过同一套通道读取与修改单位经验——详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)

- 模组支持：Variables.json 变量新增第四档作用域 `unit`——模组可为每个单位实例附加整数计数器（如魔法值、护盾值、怒气值），可通过条件与 Lua 读取——详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)

- 模组支持：Variables.json 变量现支持城市级、文明级和全局级作用域、上下限、每回合产出、百分比加成、购买成本、生产转换、分作用域 unique、Lua 读写与作用域 UI；现有文明级变量需显式声明 `scope`，详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json) 与 [uniques](/zh/Modders/uniques)

- 修复：分作用域 Variables.json 变量现会在游戏状态复制后保留数值，遵守文明与作用域限制、城市筛选器和上下限，按整数原值扣除购买成本，并避免 AI/属性解析崩溃。

- 模组支持：变量（Variables.json）现可用于所有 countable 位置（如 `when number of [X] is more than [Y]`）与 `Set [X] to [countable]` 设定触发器，更多用资源伪装的计数器可以迁移到变量——详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)；CoeHarMod 为参考实现

- 合并上游 7 个提交：View 重构继续（#15280）——视图相等性比较统一、外文明视图访问收紧（getCiv()）、viewingCiv 私有化（观战者伪装成玩家时行为一致）；修复仅有被掠夺道路时的修复功能异常、打开文明百科时的 ANR；CN 特色功能（地图钉/单位备注可见性、多人聊天在线状态、重开投票）改用等价新 API

- 文档站：结构重组——英文区 CoeHarMod 更新日志移至 [/Community/Mods/CoeHarMod/changelog/](/Community/Mods/CoeHarMod/changelog/)（更新计划在 [/Community/Mods/CoeHarMod/changelog/roadmap](/Community/Mods/CoeHarMod/changelog/roadmap)），英文区社区图片改用英文文件名，中文页面不再重复存储图片、直接引用英文版图片

- 文档站：移除上游更新日志页，上游版本记录请通过本页顶部链接查看 [Unciv 官方更新日志](https://github.com/yairm210/Unciv/blob/master/changelog.md)

## 4.21.10.2（build 1257）

- 更新：游戏内更新检查与安装包下载改为按玩家地区分流——首次启动弹窗询问所在地区（可在「选项 - 高级 - 玩家地区」修改）：选择「中国大陆」的玩家走 CN 官方下载服务器（与联机服务器同域，由 server-ts 安装包托管提供），即使无法访问 github.com 也能稳定完成自动更新，服务器只保留最新版本（旧版自动清理）且带限速与并发保护，发布后由服务器自己经镜像（gh-proxy）从 GitHub 拉取安装包；其他玩家仍走其在选项中设置的下载源。模组下载不受影响，详见 [server-ts/README.md](https://github.com/AutumnPizazz/Unciv/blob/UncivCN/server-ts/README.md)

- 合并上游 4.21.10-patch1/patch2（9 个提交）：修复观战视角选择外国城市/单位时的崩溃、城市在点击瞬间被夷平的崩溃、同时触发城市界面两个箭头按钮的崩溃；大量单位可晋升时通知合并为一条；计时器在应用暂停时报告

## 4.21.10.1（build 1256）

- 模组支持：新增模组全局变量（Variables.json）——每个文明独立的整数计数器（如厌战度），支持 `when above/below/between` 条件、`Instantly provides/consumes/gain` 触发与 Lua（`civ.getVariable/setVariable/addVariable`、`game.getRulesetVariables/doesVariableExist`）；可在变量级及 ModOptions（`variableMenuThreshold`、`alwaysDisplayVariableCount`）配置顶栏与资源概览的显示，详见 [Variables.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)
- 模组支持：条件类 `[amount]`（如 `when above [amount] ...`、血量/移动力/人口阈值）现支持 Countable 表达式（如 `[Cities]`），详见 [Unique parameters](/zh/Modders/Unique-parameters#countable)

- 合并上游 4.21.10（46 个提交）：View 重构继续（地块/单位视图层、观战修复）；大量单位可晋升时通知合并为一条；军团不能再修复；改进设施选择器不再隐藏可研发的改进；胜利界面文明列表保持滚动位置；图表改用实时数据；全球政治概览隐藏观战者；单机被击败玩家获得全图视野；WLTKD 庆祝期间需求改写修复；不再因资源出售太空船部件；控制台可傀儡城市；多项 Android 与崩溃修复；Kotlin 升级到 2.4.10
- 修复：地图钉（地块备注）预览图不再提前显示科技未揭示的战略资源（如远古时代看不到石油）
- 地图生成旋转对称重构：对称改为在生成时参与而非事后打补丁——修复环形地图 3/6 折对称失效、镜像大陆不一致、资源/奇观复制异常；生成性能无退化，详见 [分支特色](/zh/UncivCN/Features)

## 4.21.8.3（build 1255）

- 联机：新游戏选项「禁止读档」——退出重进无法再回到回合开始重新操作（重新进入时从你最新的回合状态继续）
- 模组支持：新增 ModOptions unique「Production overflow applies immediately to the next construction」——无上限的产能溢出当回合立即作用于下一个项目，靠即时溢出完成的单位可立即移动；未启用时原版机制不变，详见 [uniques](/zh/Modders/uniques)

## 4.21.8.2（build 1254）

- 修复：地图钉（地块备注）预览图不再提前显示科技未揭示的资源（如远古时代看不到石油）
- 模组支持：文明6式单位维护费（ModOptions 加 unique 启用）——按单位固定维护费、扁平金币减免、不随游戏进度膨胀，旧体系不变；维护费与最大血量现显示在文明百科中，详见 [Units.json](/zh/Modders/Mod-file-structure/4-Unit-related-JSON-files) 与 [uniques](/zh/Modders/uniques)
- 模组支持：`[amount]` 类参数全面支持 Countable 表达式（半径、数量、免费单位数、治疗/伤害/经验、回合条件），详见 [Unique parameters](/zh/Modders/Unique-parameters)
- 模组支持：新增触发/条件/反向 unique（战斗/劫掠/焚毁触发、驻防/登船条件、失去地块/间谍/黄金时代、隐藏已探索地块），详见 [uniques](/zh/Modders/uniques)
- 模组/Lua：Lua 可修改地块产出、接管战斗力与伤害公式，并响应战斗/俘获事件（含完整攻防上下文），详见 [Lua Modding](/zh/Modders/Lua-Modding)
- 模组支持：ModOptions.json 新增版本字段（`recommendedGameVersion`、依赖的 `recommendedVersion`），游戏版本不符时给出非阻塞警告，详见 [ModOptions.json](/zh/Modders/Mod-file-structure/5-Miscellaneous-JSON-files)
- 模组支持：修复 REMOVE_FIELD 合并对 float 字段的崩溃，详见 [MergeActions](/zh/Modders/Mod-file-structure/6-MergeActions)

## 4.21.8.1（build 1253）

- 合并上游 4.21.8（17 个提交）：View 重构继续、新增排名类型「已探索地块」（可选）、观战位玩家上限、修复崩溃界面 OOM 与多处 ANR、直布罗陀巨岩附近水域河流
- 难度：国王及以上难度的 AI 不满修正改为 100/90/85/75（原 90/85/75/60）
- 模组支持：单位血量上限可模组化——Units.json 新增 `maxHP` 字段 + unique「[relativeAmount] Max HP」，残血减伤按血量百分比缩放，详见 [Units.json](/zh/Modders/Mod-file-structure/4-Unit-related-JSON-files)
- 主菜单右下角按钮：Discord 改为 QQ 群入口，新增百度贴吧按钮，GitHub 改指 CN 分支仓库；关于页链接更新
- 文档站链接按客户端语言自适应（中文客户端跳 /zh/ 区）
- 游戏内 wiki 链接改为 CN 文档站
- 全项目代码清理与审查修复（删除死代码、修复城市/过滤器等 bug）

## 4.21.7.2（build 1252）

- Android：更新弹窗游戏内直接下载 APK（带进度），一键调起系统安装，自动引导「安装未知来源应用」授权
- Android：下载断点救援，「安装/重新下载」双按钮，复制一份到系统下载文件夹，自动清理旧安装包
- 主菜单「下载最新版本」改为列出当前平台安装包并直接下载所选包

## 4.21.7.1（build 1251）

- 合并上游 4.21.7 / 4.21.7-patch1 / patch2（41 个提交）：AI 战争逻辑修复、隐形轰炸机闪避、MP 平均回合用时、迷你地图 ANR 修复、Gradle 9.4.1 + LibGDX 1.14.2
- Lua API：新增 `unit.getEraNumber()`、`civ.discoverTech()`，详见 [Lua API 参考](/zh/Modders/Lua-API-Reference)
- 模组：CoeHarMod 改为独立仓库的 git 子模块，规则经 Lua 精简

## 4.21.6.6（build 1250）

- 主菜单自动更新检测（镜像源回退）；「模组下载源」更名为「下载源」
- 修复自定义 Lua API 函数参数含函数调用表达式时的运行时崩溃
- Lua API 大扩展（约 100 个新方法）、`ConditionalLuaCheck` unique、`TriggerUponTradeMade` 钩子，详见 [Lua Modding](/zh/Modders/Lua-Modding)
- Lua 地图脚本：模组可在 `scripts/` 提供地图生成器，新增「Lua Generated」地图类型
- Lua 沙箱加固；模组检查器覆盖地图脚本 API

## 4.21.6.5（build 1249）

- Lua 错误弹窗带行号；堵住 `package.loaded` 沙箱逃逸；指令预算阻止死循环卡死
- 新增：联机重开投票（房主开启、全员投票、超时默认同意、自动重开）

## 4.21.6.4（build 1248）

- 新增「模组下载源」设置，支持国内镜像加速
- 开图界面：支持命名槽位保存/读取配置；剪贴板按钮移到右下角；「重置为默认」改为内置「默认配置」

## 4.21.6.3（build 1247）

- 修复 Docker 构建复制旧名 `Unciv.jar`；修复 Release 缺少 MSI 安装包

## 4.21.6.2（build 1246）

- 补齐 CN 自研功能缺失的简体中文翻译（47 个缺失 + 10 个空词条）

## 4.21.6.1（build 1245）

- 笔记改为按 gameId 存储，不再跨存档泄漏
- 地图钉/单位钉：长按/Alt+点击编辑、备注气泡、崩溃修复、中文词条补齐
- 修复开图界面自定义尺寸重复行；内置规则集与上游对齐
- 合并上游 4.21.6（CPU 性能、AI 工人相邻加成、多人上传失败提示）

## 4.21.5.3（build 1244）

- 修复 CI 测试失败（翻译模板缺空格、测试模组纳入版本库）；部署通知清理

## 4.21.5.2（build 1243）

- 官方 VitePress 文档站上线（中文全文搜索、中英一键切换）
- unique 说明文档中文化
- APK 签名本地化，产物统一命名 UncivCN

## 4.21.5.1（build 1242）

- 合并上游 4.21.5（AI 金币/战争逻辑修复、CPU 性能优化）
- 撤销自带 UCCC 模组；新增开图设置剪贴板导出/导入；人口锁地按钮调整；修复统计显示级联 bug

## 4.21.0.2（2026.8.1）

- 修复 UCCC 模组部分 bug

## 4.21.0.1（2026.8.1）

- 跟进上游近期更新（对称/镜像地图、统计面板）；补全中文本地化
- 联机默认服务器改为 `sp.unciv.cn:30123`；新增人口自动锁定按钮；自带 UCCC 模组
- 联机不再支持读档改变随机数

## 4.20.17.2（2026.7.12）

- 新增单位钉/地图钉；取消旋转对称地图半径奇偶限制；存档版本隔离；训练移民不再饿死市民

## 4.20.17.1（2026.7.4）

- 跟进上游一个多月的更新

## 4.20.8.4（2026.5.26）

- 模组检查器可筛查 Lua 错误；轮询联机显示在线状态

## 4.20.8.3（2026.5.23）

- 增强 Lua 系统；event 可挂载 Lua

## 4.20.8.2（2026.5.22）

- 模组支持 [Lua 脚本](/zh/Modders/Lua-Modding)
- 修复「众神与国王」精英教育不送伟人；修复 TRY_INJECT 造价覆盖 bug
- Amount 参数兼容 Countables

## 4.20.8.1（2026.5.21）

- 新增 [轮询联机功能](./Polling-multiplayer)

## 4.20.7.4（2026.5.19）

- 中心对称地图资源数量对称分布

## 4.20.7.3（2026.5.19）

- 扩展模组 JSON 系统，见 [MergeAction 教程](/zh/Modders/Mod-file-structure/6-MergeActions)

## 4.20.7.2

**发布日期**：2026.5.19

- 安卓端安装包不再自带 CoeHarMod 模组
- 新增三种镜像地图模式（两瓣/三瓣/六瓣中心对称）

## 4.20.6.3

**发布日期**：2026.5.17

- 新增模组特性：

| 英文原文 | 中文释义 |
| --- | --- |
| `Hidden from city screen` | 不再显示在城市面板中 |
| `Can be built [amount] times in each city` | 可以在单个城市中重复建造[amount]次 |

## 4.20.6.1

**发布日期**：2026.5.16

- 同步两个月来上游更新
- 随机结果可变性收束为设置开关

## 4.19.16.2

**发布日期**：2026.3.2

- 远古遗迹随机结果可通过读档重载改变

## 4.19.16.1

**发布日期**：2026.3.1

- 城邦任务随机结果可通过读档重载改变

## 4.19.15-cn2

**发布日期**：2026.2.28

- 新增范围攻击类模组特性（多目标、伤害随距离递减、自身/反击伤害）
- 统计面板数据导出 CSV

## 4.19.15

**发布日期**：2026.2.26

- 安卓端内置 [CoeHarMod/和合共生](https://github.com/AutumnPizazz/CoeHarMod)
- 相邻城市可交换地块（需在 ModOptions.json 声明 "Allow cities to claim tiles"）

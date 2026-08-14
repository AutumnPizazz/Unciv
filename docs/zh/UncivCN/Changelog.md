---
title: UncivCN 更新日志
---

# UncivCN 更新日志

版本号规则：上游版本 + CN 子版本号（`.1`、`.2`、`.3`…，同一上游版本可发多个 CN 子版本，如 4.20.8.1 → 4.20.8.4；跟进新上游后从 `.1` 重新开始，如 4.21.5 → 4.21.5.1）。

## 未发布

- 模组/Lua：Lua 现可接管战斗力与伤害公式——新增 unique「Combat strength is modified by [luaFunction]」（收到基础力 + 修正倍率）、「Combat damage dealt is modified by [luaFunction]」（收到攻防双方战斗力）、「Combat damage received is modified by [luaFunction]」（收到所受伤害）；返回 nil 则回退到引擎公式
- 模组/Lua：战斗触发的 Lua 现在能看到对手——`ctx.otherCiv`（外交与战斗）、`ctx.attacker`/`ctx.defender`/`ctx.target` 与 `ctx.combatAction` 已对 `TriggerLuaFunction`/`ConditionalLuaCheck` 开放
- 模组/Lua：新增单位触发 unique：俘获单位时 / 被俘获时 / 拦截单位时 / 被拦截时
- 文档：Units.json 字段参考补充 `maxHP` 字段说明（默认 100，可通过 `[relativeAmount] Max HP` unique 调整）；杂项 JSON 文档中的城市力量公式改为按驻军单位生命百分比计算，不再假设血量上限为 100
- CI：精简分支推送触发的工作流——Docker 镜像发布不再随每次提交构建推送（保留每日定时、发版 tag、PR 构建验证与手动触发），冲突标记不再随分支推送重扫（PR 仍触发）；纯文档改动跳过代码测试工作流（Build and test / Detekt），由文档工作流的 generateDocs 兜底编译

## 4.21.8.1（build 1253）

- 合并上游 4.21.8（17 个提交）：View 重构继续（#15280）——战争迷雾视图统一（`getGameViewConsideringForOfWar`）、帝国总览各页签接入 View、贸易界面分两步 View 化、城市界面迁移 `TileView`；新增非原版排名类型「已探索地块」（`Show additional stat types` 新游戏选项开启）与观战位玩家上限（Max players with spectator）；修复崩溃界面 OOM、科技选择器与通知总览 ANR、直布罗陀巨岩附近水域河流、开发控制台资源过滤可见性；文档重组（`Simulations` 迁至 Developers 并更名 `Testing AI changes`、Regions 并入地图 JSON 文档、预告片音频致谢并入 Credits）
- 合并冲突解决：版本号按 CN 惯例升至 4.21.8.1（build 1253）；简中翻译新增「显示额外统计类型」「已探索地块」；中英文档随上游重组同步（`Other/Regions`、`Other/Simulations` 与 `Credits_trailer` 独立页删除，VitePress 侧边栏更新）；`lua-api.lua`/`lua-map-api.lua` 经 `generateDocs` 重新生成（版本头同步）
- 难度：国王及以上难度的 AI 不满修正改为 100/90/85/75（原 90/85/75/60），以实机数值为准
- 模组支持：单位血量上限可模组化——Units.json 新增 `maxHP` 字段（默认 100）+ 新 unique `[relativeAmount] Max HP`；残血减伤按血量百分比缩放，AI/UI 阈值全部相对化，Lua 的 `unit.getMaxHealth()` 返回动态上限
- 主菜单右下角按钮：Discord 改为 QQ 群入口（qm.qq.com），新增百度贴吧按钮，GitHub 按钮改指 CN 分支仓库；关于页仓库/更新日志/README 链接改指 UncivCN 分支（含版本锚点修正）
- 文档站链接按客户端语言自适应：简体/繁体中文客户端跳中文区（/zh/），其余语言跳英文区
- CI：detekt / Docker 发布工作流改在 UncivCN 分支触发（此前绑定上游 master 分支从未运行），Release 判定只认 4 段版本号；移除上游 uncivbot 自动发版机器人（CN 为手动发版）
- 游戏内 wiki 链接改为 CN 文档站（club.unciv.cn），加载失败提示邮箱改为 hurxwork@qq.com
- 代码清理与审查修复：删除全项目审查发现的死代码与注释代码，修复 `ConditionalBuildingBuiltAll` 城市过滤失效、多段 `{A} {B}`/`non-[X]` 过滤恒错、`LongPriorityQueue.remove` 误删队首、`stateBasedRandom` 无头测试崩溃等问题
- CI：修复 detekt 静态检查失败——更新配置中的弃用属性（移除 `OptionalWhenBraces`，`ForbiddenComment` 的 `values`/`customMessage` 改为 `comments`），并补齐 `UnitPresenter.kt` 文件末尾缺失的换行

## 4.21.7.2（build 1252）

- Android：更新弹窗游戏内直接下载 APK（带进度），一键调起系统安装（FileProvider），自动引导「安装未知来源应用」授权
- Android：下载断点救援（.part + 原子改名），「安装/重新下载」双按钮，复制一份到系统下载文件夹，自动清理旧安装包
- 主菜单「下载最新版本」改为列出当前平台安装包并直接下载所选包（不再跳转浏览器发布页）

## 4.21.7.1（build 1251）

- 合并上游 4.21.7 / 4.21.7-patch1 / patch2（41 个提交）：View 重构持续推进（#15280）、AI 战争逻辑修复、隐形轰炸机闪避、MP 平均回合用时、迷你地图 ANR 修复、Gradle 9.4.1 + LibGDX 1.14.2 + target SDK 36；CN 地图钉/单位钉与人口锁地适配新 View API
- CI：修复 `unciv-lua-api` vsix 打包
- 模组工具：Lua API 定义随发版自动部署文档站，`unciv-lua-api` VSCode 扩展自动同步接入 LuaLS
- 文档：Lua API 与地图脚本参考改为 Kotlin 数据表驱动生成；工具链不再依赖 Python
- Lua API：新增 `unit.getEraNumber()`、`civ.discoverTech()`
- 模组：CoeHarMod 改为 git 子模块（独立仓库），规则经 Lua 精简

## 4.21.6.6（build 1250）

- 主菜单自动更新检测（镜像源回退）；「模组下载源」更名为「下载源」
- 修复自定义 Lua API 函数参数含函数调用表达式时的运行时崩溃
- Lua API 大扩展（约 100 个新方法）、`ConditionalLuaCheck` unique、`TriggerUponTradeMade` 钩子
- Lua 地图脚本：模组可在 `scripts/` 提供地图生成器，新增「Lua Generated」地图类型
- Lua 沙箱加固；模组检查器覆盖地图脚本 API

## 4.21.6.5（build 1249）

- 文档：Lua 章节全面重写、EmmyLua 类型定义 + `LuaStarterMod` 模板、mod-ci 命令行
- Lua 错误弹窗带行号；堵住 `package.loaded` 沙箱逃逸；指令预算阻止死循环卡死
- 新增：联机重开投票（房主开启、全员投票、超时默认同意、自动重开）
- CI：Release 标题显式使用纯版本号

## 4.21.6.4（build 1248）

- CI：Release 说明自动写入中英双语更新日志
- 新增「模组下载源」设置，支持国内镜像加速（gh-proxy/ghfast/ghproxy/自定义前缀）
- 开图界面：支持命名槽位保存/读取配置；剪贴板按钮移到右下角；「重置为默认」改为内置「默认配置」
- 文档：Coding-standards 新增发版检查清单与踩坑经验

## 4.21.6.3（build 1247）

- 修复 Docker 构建复制旧名 `Unciv.jar`；修复 Release 缺少 MSI 安装包

## 4.21.6.2（build 1246）

- 补齐 CN 自研功能缺失的简体中文翻译（47 个缺失 + 10 个空词条）

## 4.21.6.1（build 1245）

- 笔记改为按 gameId 存储，不再跨存档泄漏
- 地图钉/单位钉：长按/Alt+点击编辑、备注气泡、崩溃修复、中文词条补齐
- ModOptions.json 新增 `modVersion` / `gameVersionRange` / `modDependencies`
- 修复开图界面自定义尺寸重复行；内置规则集与上游对齐
- 合并上游 4.21.6（CPU 性能、AI 工人相邻加成、多人上传失败提示）
- 文档站链接修复（约 250 处锚点）；游戏内版本号构建时自动同步

## 4.21.5.3（build 1244）

- 修复 CI 测试失败（翻译模板缺空格、测试模组纳入版本库）
- Deploy 不再推送 Discord 通知

## 4.21.5.2（build 1243）

- 官方 VitePress 文档站上线（中文全文搜索、中英一键切换）
- `docDescriptionZh`：unique 说明文档中文化
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

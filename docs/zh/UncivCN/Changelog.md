---
title: UncivCN 更新日志
---

# UncivCN 更新日志

版本号规则：上游版本 + CN 子版本号（`.1`、`.2`、`.3`…，同一上游版本可发多个 CN 子版本，如 4.20.8.1 → 4.20.8.4；跟进新上游后从 `.1` 重新开始，如 4.21.5 → 4.21.5.1）。

详细历史记录见 [UncivCN 更新日志（社区归档）](/zh/UncivCN/Changelog)。

## 未发布（Unreleased）

- 合并上游 4.21.7 / 4.21.7-patch1 / patch2（41 个提交）：View 重构持续推进（#15280）——Trade API 视图、城市内政可见性统一（`ForeignCityView.tryGetCityView`）、`WorldMapHolder`/`WorldTileGroup`/`TileLayerUnitFlag` 迁移到 `TileView`、修复未探索地块不可点击；AI 不再为金钱接受自杀式战争（禁止“kamikaze wars”），更多 AI bug 修复；隐形轰炸机获得闪避能力且拦截不再造成负伤害；MP 预览显示玩家平均回合用时（完整回合后才显示）；航母载荷在空降/变形时保留；迷你地图仅在尺寸变化时于拖动中重建（修复 ANR）；Gradle 8.11 → 9.4.1（含 AGP/Kotlin 升级）、LibGDX 1.14.2、Android target SDK 36
- 合并冲突解决：版本号按 CN 惯例升至 4.21.7.1（build 1251）；保留 CN 特色——地图钉/单位钉（`editNoteAt` 的 Alt+点击与长按、注释气泡、`updateTileNoteLabel`）适配新的 `ForeignMapUnitView`/`TileView` API，城市人口锁地与上游 tileView 点击路径融合；`removeMissingModReferences` 随上游移出 `TileGroup` 至地图编辑器；Gradle 9.4.1 走腾讯云镜像；翻译文件无冲突合并

- 模组作者工具：Lua API 定义实现全自动分发——生成的 `lua-api.lua` / `lua-map-api.lua` 每次发版部署到文档站，新增 `unciv-lua-api` VSCode 扩展（云端薄拉取器，vsix 随每个 Release 附带）在每次编辑器启动时同步到 `~/.unciv/lua-api/` 并用一键命令接入 LuaLS；模组作者无需再人工检查更新。生成文件头部带 `-- Unciv version:` 版本标记，新旧一目了然

- 文档：Lua **地图脚本** API 文档也改为由 Kotlin 数据表（`LuaMapGenApiDocs`）驱动生成——`lua-map-api.lua`（原为手工维护）与新增的 `Lua-Map-API-Reference.md`（中英两版）由 `./gradlew desktop:generateDocs` 一并重新生成；`Lua-Modding.md` 教程的地图脚本章节改为链接到生成的参考页；类型定义生成循环已与游戏内 API 生成器共用（新增同步测试，退役基于正则的旧定义测试）

- 文档：Lua API 参考改为像 unique 文档一样由 Kotlin 数据表（`LuaApiDocs`）驱动生成——`lua-api.lua` 与新增的 `Lua-API-Reference.md`（中英两版）在新增 API 时由 `./gradlew desktop:generateDocs` 一并重新生成，面向模组作者的文档不再可能与实现漂移（新增同步测试）；教程页 `Lua-Modding.md`（中英）改为链接到生成的参考页；`UniqueDocsWriter` 与 Lua 文档生成器共用新的 `DocsWriter` 基类
- 工具链：文档工具链不再依赖 Python——CI 链接检查（`check-docs-links.mjs`）与本地预览服务器（`preview-server.mjs`）改为 Node 脚本，一次性迁移脚本与上游 `mkdocs.yml`/mkdocs workflow 已删除（只需 JDK + Node）
- Lua API：新增 `unit.getEraNumber()`（单位需求科技的最小时代序号，与 `{Era.X}` 单位过滤器语义一致）与 `civ.discoverTech()`（等价 `Discover [tech]` unique，绕过可研究性检查直接解锁，幂等）；已同步 EmmyLua 类型定义
- 模组：CoeHarMod 活用 Lua 系统减负——将军/海军统帅光环由每伟人 9 条时代分段 unique 合并为 1 条 Lua 条件；AI 修正（60 条市政 Discover + 11 条资源/政策槽 Provides）收敛为 2 个回合钩子；AI 市政改为按时代增量解锁（ctx.store 记录进度）；清理注释掉的旧规则定义约 1900 行

- 模组：CoeHarMod 改为以 git 子模块（submodule）方式内置，指向独立仓库 AutumnPizazz/CoeHarMod——以子模块引用替换仓库内副本（当前 v3.3.9+），内置版本随模组仓库更新

## v4.21.6.6（build 1250）
- 游戏更新：新增自动更新检测——进入主菜单后在后台查询 CN 分支仓库（AutumnPizazz/Unciv）的最新 GitHub Release（与模组下载一样走当前下载源）；发现新版本时主菜单底部版本号旁显示“发现新版本”提示，点击版本号弹出新版本信息与下载按钮；因网络问题检测失败时弹窗询问一键切换镜像下载源（与模组列表失败时的交互一致）
- 设置：将“模组下载源”更名为“下载源”（现在也用于更新检测）；下载源下拉框改为显示源名称（如“GitHub（官方）”）而非原始枚举名；修复“自定义下载前缀”一行把选项表撑宽的问题——标签移入左列，输入框与确定按钮放入右列并限制宽度
- 修复影响所有 Lua 模组脚本的运行时崩溃：自定义 Lua API 函数（`luaFunction`）现在重写 luaj 的 `invoke()`，参数列表包含函数调用表达式（如 `ctx.store.set("k", tostring(1))`、`ctx.log(tostring(x))`、`civ.addNotification(makeText())`）的调用不再报 "attempt to call function"——此前只有字面量参数可用；已补回归测试
- 修复 `testMOD` 示例脚本 `hello.lua` 使用不存在的 API 字段（`civ.gold` / `civ.cityCount` / `civ.era`，会静默得到 nil）——已改为 `civ.getGold()` / `civ.getCityCount()` / `civ.getEra()`
- Lua API 大扩展（各上下文表新增约 100 个方法）：civ 新增身份/排名（getNation/getLeaderName/getScore/getForce）、每回合产出（getSciencePerTurn/getCulturePerTurn/getFoodPerTurn/getProductionPerTurn）、科技成本、下个政策文化成本、外交关系表（getDiplomaticStatuses/getProximityTo/hasEmbassyWith/makePeaceWith）、城市列表（getCityNames/getTotalPopulation/getWondersBuilt）、资源库存、间谍详情与 addSpy、setGold；city 新增食物/增长（getFood/getFoodSurplus/getFoodStorage/getFoodNeeded）、生产信息（getProductionProgress/getProductionCost/getTurnsToCompletion）、驻军/城防/专家/失业/抵抗/奇观及写入（setPopulation/addFood/addProduction/addHealth/setName/sellBuilding）；unit 新增战斗信息（getMaxHealth/getDamage/getAttacksLeft/getVisibilityRange/getAction/canAttack/canPillage）、领土判定（isInEnemyTerritory/isInFriendlyTerritory）、伟人/宗教查询及写入（setHealth/setXP/setStatus/setAttacksLeft/fortify/moveByPath）；tile 新增道路（hasRoad/hasRailroad）、自然奇观、领土判定、距离/相邻查询与 setExplored；game 新增文明列表（getCivNames/getHumanCivs/getCurrentPlayerCiv）、地图信息（getMapName/getMapType）、规则集列表（地形/资源/改良/文明/宗教/信条/事件/自然奇观/单位类型、时代名、胜利类型、模组列表、基础规则集）与 10 个存在性检查；ctx 新增确定性随机（random/randomInt，联机安全）。所有新 API 已同步静态 API 目录表、EmmyLua 类型定义与 CLI 静态检查器，并由扩展的 testMOD 套件（testNewApi.lua）覆盖——注意上下文表属性（如 city.name、unit.health）是建表时的快照，写入后请用查询方法确认
- Lua 条件系统：新增 `ConditionalLuaCheck` unique——`<if [myMod:myFunction] returns true>` 把已加载模组中的 Lua 函数作为 unique 条件求值（函数收到常规 ctx 表，返回 true 即条件成立）；缺失函数按 false 处理并由模组检查器报告（游戏内与 mod-ci 均校验）
- 新增生命周期钩子：`TriggerUponTradeMade`——`<upon completing a trade with [civFilter] Civilizations>` 在任意被接受的交易完成时触发（双方都会触发，包括 AI 自动接受的交易）；与 TriggerLuaFunction 组合即可获得交易完成的 Lua 钩子
- Lua 地图脚本：模组现在可以在 `scripts/` 中提供地图生成器（`GetMapScriptInfo` 返回 `{name, description}` 元数据 + `GenerateMap(ctx)` 生成函数）；新建游戏界面新增“Lua Generated”地图类型并列出发现的脚本；生成器获得独立的沙箱 ctx（`params`/`seed`/`perlin`/`random`/`randomInt`/`log`/`map`，含地块访问、大陆分配、起始位置、气候/山脉/海岸/河流/冰雪生成、洪泛填充、地形规范化与起始地块平衡助手）；`params.bounds` 暴露实际（可能为负的）地块坐标范围；`GenerateMap`/`GetMapScriptInfo` 为保留函数，模组检查器禁止在游戏内 TriggerLuaFunction 中使用；新增示例模组 `docs/Modders/examples/LuaMapScriptExample/`、77 个 API 测试与一个端到端示例测试；同时修复 Perlin 噪声函数同样的 luaj invoke() 缺陷（4 参数调用）
- 上线前 Lua 加固：`city.sellBuilding` 对未知建筑不再抛异常（存在性守卫）；循环 Lua 条件（A 通过 `ctx.evaluateConditional` 求值 B、B 求值 A）加入深度限制，超限按 false 处理而非撑爆 JVM 栈；Lua Generated 模式不再渲染错误的 Kotlin 生成示例地图，导入含 Lua 地图脚本的配置后地图类型自动恢复为 Lua Generated；CLI 静态检查器（mod-ci）新增地图脚本 API 覆盖（`ctx.map`/`ctx.params`/`ctx.size`/`ctx.bounds`，tile API 按两套上下文取并集检查），静态 catalog 与运行时注册的一致性由测试保障；新增地图脚本 EmmyLua 类型定义（`docs/Modders/lua-map-api.lua`，防漂移测试守护）
- 翻译：为新增的 Lua UI 字符串与 unique 补齐简体中文词条（模板占位符同步）——`Lua Generated`（Lua 脚本生成）、`Scripted`（脚本生成）、`Map Script:`（地图脚本：）、`if [luaFunction] returns true`（如果 [luaFunction] 返回 true）、`upon completing a trade with [civFilter] Civilizations`（一旦与[civFilter]文明完成交易时）

## v4.21.6.5（build 1249）
- 文档：将 Lua 大改全面反映到文档站——Coding-standards 生成器清单新增 `LuaApiDefinitionWriter`/`lua-api.lua`；Features 页 Lua 章节重写（触发方式、安全沙箱、错误报告、工具链）；Differences 对比表更新 Lua 检查与工具链行；模组总览页新增 Lua 脚本入口（指向教程与起步模板，中英同步）
- Lua 模组编辑器支持：新增自动生成的 EmmyLua 类型定义（`docs/Modders/lua-api.lua`，由 `LuaApiDefinitionWriter` 从 API 目录表生成，命令 `./gradlew desktop:generateDocs`，与实现的一致性由测试保障）——模组作者把 LuaLS 语言服务器指向该文件即可获得自动补全、悬停文档与即时拼写检查；常见 API 的参数/返回值标注精确，其余宽松
- Lua 模组起步模板：新增可复制改名的模板模组 `docs/Modders/examples/LuaStarterMod/`（每回合钩子、参数示例、API 冒烟测试），并在 Lua-Modding 文档（中英）新增“编辑器设置”小节（LuaLS 扩展、`.luarc.json` 配置与模板说明）；测试持续保障模板能通过模组作者会跑的检查
- 文档：Lua-Modding 页面中英重新对齐——英文版补齐“最常见错误”提醒、`game.findTiles` 条件表与缺失的 civ API（getAdoptedPolicyCount / getAvailablePolicyBranches / getLeaderTitle / getTechCount）；两版共同补充返回值真值陷阱（`return 0`/`nil` 算失败）、函数名规则、沙箱加固与死循环预算说明，以及新增“检查你的模组”小节（游戏内模组检查器与 `mod-ci` 命令行）
- 移除 `testMapScript` 示例模组——其使用的 Lua 地图生成 API（`ctx.map`、`ctx.perlin`、`GenerateMap`）在引擎中从未实现，示例属于失效死代码
- Lua 模组健壮性与工具链：运行时错误弹窗现在带脚本行号；AI 回合触发的错误会记入游戏内模组检查器而非只写日志；Lua API 的浮点参数（addInfluence / addMovement / useMovement）增加 NaN/无穷值防护；新增静态 API 拼写检查（如 `ctx.civ.addGoldd(...)` 会报错并给建议），游戏内模组检查器与 `mod-ci` 命令行均会运行，底层 API 目录表与运行时注册的一致性由测试保障
- Lua 模组安全加固：堵住沙箱逃逸——全局名置 nil 后 `package.loaded` 表仍保留完整的 `io`/`os`/`luajava` 库引用，模组脚本可借此读写任意文件、执行系统命令与 Java 反射（现已彻底移除 package 库）；每次脚本加载与函数调用新增指令预算，`while true do end` 之类的死循环会被报错中断，不再卡死游戏
- 联机多人：新增重开投票功能——开局配置新增“重开投票回合”与“重开投票超时”选项；从该回合起任意玩家可发起重开投票，所有玩家（包括离线者，上线后仍可投票）可投赞成/反对，全员投完或超时后结算（超时未投者默认同意），除非超过半数明确反对否则通过；通过后按原配置以新 gameId 重开，所有客户端自动跳转到新局；投票进行期间未投票的玩家无法结束回合
- CI：GitHub Release 标题改为显式使用纯版本号（如 4.21.6.4），不再使用上传动作自动填充的冗长名称
## v4.21.6.4（build 1248）

- CI：修复 GitHub Release 说明文本为空——现在按发布 tag 从 CN 中英文更新日志（docs/UncivCN + docs/zh/UncivCN）提取对应版本小节，中英双语写入 Release 说明（此前 CN 四段式版本号在上游 changelog.md 中不存在，导致正文为空）
- 模组管理：新增“模组下载源”设置（选项 → 高级），国内等无法稳定访问 GitHub 的玩家可将模组列表、预览图与下载全部切到公共 GitHub 加速镜像（gh-proxy.com / ghfast.top / ghproxy.net，或任意自定义前缀）；官方源加载模组列表失败时，游戏会弹窗询问一键切换镜像源并自动重试；新增“国内玩家模组安装指南”文档页
- 移除新建游戏页"重置为默认"按钮：读取配置列表新增内置且不可删除的"默认配置"条目，可恢复完全相同的默认选项（保留原确认弹窗）
- 新建游戏页新增"保存当前配置/读取已保存配置"按钮（位于剪贴板按钮右侧）：配置以与剪贴板导出相同的格式存入 SaveFiles/GameSetup 下的命名槽位，读取弹窗可选择或删除槽位；窄屏时按钮独占一行，位于剪贴板行与开始游戏行之间
- 新建游戏页的剪贴板复制/粘贴按钮从左上角（游戏选项列顶部）移到右下角"开始游戏"旁：宽屏与开始游戏同一行、位于其左侧；窄屏单独一行放在开始游戏上方，避免撑破底部栏
- 文档：Coding-standards 新增发版检查清单与踩坑经验（tag 推送、文档站本地验证、禁止裸尖括号、Maven Central 403、Unciv.jar 旧名残留、continue-on-error 掩盖失败、CN 词条翻译完整性）

## v4.21.6.3（build 1247）

- 修复 Docker 构建：Dockerfile 复制的 `Unciv.jar` 在 CN 分支实际产物名为 `UncivCN.jar`（改名后镜像构建一直失败）
- 修复 MSI 发布：`wix build` 默认按源文件名输出 `unciv.msi`，而上传路径是 `UncivCN.msi`；`build-msi` job 带 `continue-on-error: true`，历次 Release 从未包含 MSI——现在显式指定输出文件名

## v4.21.6.2（build 1246）

- 修复 CN 自研功能词条缺失简体中文翻译（47 个缺失 + 10 个空翻译）：剪贴板复制/粘贴游戏设置、轮询联机（轮询间隔、刷新、在线玩家、回合计时）、镜像地图对称模式、自动锁定、地图钉笔记、读档随机数变动、Countables、寒带/螺旋地图类型等——此前显示为英文
## v4.21.6.1（build 1245）

- 修复笔记跨存档泄漏：笔记改为按 gameId 存储（"notes_{gameId}"）而非存档文件名——同一局游戏的手动存档/自动存档/另存为共享笔记，不同游戏严格隔离（此前共享的 "Autosave" 名称会导致笔记在不同游戏的自动存档间串档，且未保存的新游戏笔记会丢失）；旧 "{存档名}_notes" 文件首次加载时自动迁移，删档时两种命名都会清理，存档列表过滤新命名
- 地图钉/单位钉交互重构：手机长按、桌面 Alt+点击即可编辑备注，无需任何开关（普通点击永不被劫持，旧开关保留为备选）；备注气泡截断为 8 字符，点击气泡弹出全文查看（支持编辑/删除）；地块笔记开关改用星标图标，两个开关桌面悬停有文字提示
- 修复地图钉/单位钉窗口本地化：补齐笔记相关 UI 词条的中文翻译（"为地块添加笔记"、"为[unitName]添加笔记"、"添加笔记"、"笔记"、"显示单位/地块笔记"）——此前显示为英文
- 修复打开地图钉编辑弹窗时崩溃：地块材质包预览使用的 TileMapView 按地块数分配数组，真实地块（非零索引）会越界（现按最大索引扩容，并新增回归测试）
- 地图钉编辑弹窗：标题去掉地块坐标，左侧占位图标改为选中地块的材质包实时预览（地形/资源/改良/河流贴图）
- 内置基础规则集（Vanilla / Gods & Kings）与上游对齐：移除 G&K ModOptions 中 CN 独有的 "Allow cities to claim tiles"（tile-claim 改为模组通过 ModOptions 自行声明启用）与 CN 独有的大军事家 8 回合黄金时代 unique（上游 #13308 已移除）
- 修复开图界面：世界大小设为自定义后反复切换对称性会堆积重复的半径/宽度/高度输入行（六边形/矩形尺寸表重建时未清空）
- 新特性：`ModOptions.json` 模组版本要求——`modVersion`（n.n.n，不填默认 0.0.1）、`gameVersionRange`（min~max，不填默认全版本可用）、`modDependencies`（精确或范围版本要求）；不满足时在模组管理器、新建游戏模组选择与模组检查器中显示警告，不阻止使用
- 修复文档站死链接：中文 UncivCN 页面 14 处 `](` 损坏链接，以及 Modders 文档（中英，含 `UniqueType.kt` / `Countables.kt` / `UniqueDocsWriter` / `MergeActionDocsWriter` 生成器）约 250 处标题锚点错误（VitePress slug 格式）；已用全新 VitePress 构建产物逐链接验证
- 合并上游 4.21.6：CPU 性能优化（城市基线只算一次，部分存档下一回合提速约 20%）、AI 工人考虑未来的相邻加成、多人上传失败视觉提示、聊天昵称显示文明颜色、OneTimeGainStat 参数改为 `[civWideStat]` 并对非文明级产出给出 modding 警告、测试运行器重构（详见上游 [changelog.md](https://github.com/AutumnPizazz/Unciv/blob/UncivCN/changelog.md)）
- 修复自定义域名下文档站无样式：站点产物部署到 `Unciv/` 子目录并加根跳转页
- 修复导航栏 logo 路径重复 `/Unciv` 前缀
- 游戏内版本号改为构建时自动从 `BuildConfig.kt` 同步到 `UncivGame.kt`（此前手动发版容易漏同步，导致游戏内显示的版本号滞后）
- `AGENTS.md` 瘦身为纯行为规则；构建命令、项目结构、游戏状态模型、资源路径等开发参考移入 Coding-standards，该文档升级为工程手册（中英同步）
- 新增合并上游规范：自行解决与母仓库 master 的合并冲突并完整接入新特性；双方实现方式冲突时先向用户提问定夺

## v4.21.5.3（build 1244）

- 修复 CI 测试失败：13 行翻译模板缺末尾空格（TranslationTests 挂掉）；测试模组 testMOD / testMapScript 此前被 .gitignore 忽略，导致 CI 上 Lua 脚本与合并动作测试大量失败，现已纳入版本库并补充 testMOD/jsons/Buildings.json
- Deploy 工作流不再向 Discord 推送发布通知（GitHub Release 上传保留）

## v4.21.5.2（build 1243）

- 官方文档站上线：全新 VitePress 文档站（替换 mkdocs），支持中文全文搜索、中英文一键切换、unique 列表一键复制
  - 英文区完整复用上游文档；中文区为完整翻译镜像（Modders / Developers / Translating / Other 等全部页面）
  - 新增 UncivCN 专区（分支介绍 / 新特性 / 更新日志 / 差异对照 / 代码规范 / 轮询联机）与社区内容区（原版攻略 / 模组专区 / 源码分析 / 上游更新日志），均提供中英双语
- unique 说明文档中文化：新增 docDescriptionZh 机制，模组制作者可在文档站查看中文 unique 说明
- 构建与发布流程适配：APK 签名本地化（zipalign + apksigner V3）、产物命名 UncivCN；新增签名回归 CI

## v4.21.5.1（build 1242）


- 合并上游 4.21.5（AI 金币/战争逻辑修复、CPU 性能优化、城市邦开局优化等，详见上游 [changelog.md](https://github.com/AutumnPizazz/Unciv/blob/UncivCN/changelog.md)）
- 撤销自带 UCCC 模组改动，游戏本体不再打包任何模组
- 新增新建游戏设置导出/导入剪贴板功能
- 调整人口锁地按钮逻辑
- 修复统计显示级联 bug，与上游 master 对齐

## v4.21.0.2（2026.8.1）

- 修复 UCCC 模组部分 bug

## v4.21.0.1（2026.8.1）

- 跟进 Unciv 原版近期更新（含对称/镜像地图、统计面板等上游合入功能）
- 由于与上游对胜利面板均作类似功能改动，回退「禁用文明积分面板时同步禁用实验性统计面板」功能，以上游为准
- 补全缺失的中文本地化
- 联机默认服务器改为 `http://sp.unciv.cn:30123`
- 新增人口自动锁定快捷按钮（城市屏幕右下角「自动锁定」，永久切换不锁/锁人口）
- 自带 UCCC 模组
- 随机数可通过读档改变功能联机时不再可用

## v4.20.17.2（2026.7.12）

- 新增单位钉、地图钉功能（详见 [新特性](./Features#单位钉-地图钉-4-20-17-2)）
- 取消旋转对称地图对半径的奇偶限制
- 禁用文明积分面板时同步禁用实验性统计面板
- 实现游戏版本兼容性隔离（旧版无法加载新版存档）
- 训练移民等食物→产能单位时避免市民饿死

## v4.20.17.1（2026.7.4）

- 跟进 Unciv 原版一个多月的更新

## v4.20.8.4（2026.5.26）

- 原生模组检查器可筛查模组的 Lua 错误
- 轮询联机功能可查看其他玩家的在线状态

## v4.20.8.3（2026.5.23）

- 增强 Lua 系统
- event 可挂载 Lua

## v4.20.8.2（2026.5.22）

- 模组支持 [Lua 脚本](/zh/Modders/Lua-Modding)
- 修复「众神与国王」精英教育政策不送伟人的 bug
- Amount 参数兼容 Countables，新增多个 Countables 参数类型
- 修复 TRY_INJECT 单位时将造价覆盖为 0 的 bug

## v4.20.8.1（2026.5.21）

- 重磅更新！新增 [轮询联机功能](./Polling-multiplayer)

## v4.20.7.4（2026.5.19）

- 中心对称地图现在让资源数量也对称分布

## v4.20.7.3（2026.5.19）

- 扩展模组 JSON 系统，详见 [MergeAction 教程](/zh/Modders/Mod-file-structure/6-MergeActions)

---

## v4.20.7.2

**发布日期**：2026.5.19

- 安卓端安装包不再自带模组 CoeHarMod/和合共生
- 新增三种镜像地图模式，按两瓣/三瓣/六瓣中心对称分布


## v4.20.6.3

**发布日期**：2026.5.17

- 新增模组特性：

| 英文原文                                       | 中文释义                  |
| ------------------------------------------ | --------------------- |
| `Hidden from city screen`                  | 不再显示在城市面板中            |
| `Can be built [amount] times in each city` | 可以在单个城市中重复建造[amount]次 |


## v4.20.6.1

**发布日期**：2026.5.16

- 同步两个月来 Unciv 原版的更新内容
- 将前几个版本对随机性结果可变性的改动收束到设置页面，可选择是否启用


## v4.19.16.2

**发布日期**：2026.3.2

- 远古遗迹产出随机性结果可以通过读档重载改变


## v4.19.16.1

**发布日期**：2026.3.1

- 城邦任务随机性结果可以通过读档重载改变


## v4.19.15-cn2

**发布日期**：2026.2.28

- 新增模组特性：

| 英文原文                                                                                                          | 中文释义                                                       |
| ------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------- |
| `Attacks also target [mapUnitFilter] units within [positiveAmount] tiles`                                     | 攻击也会同时攻击 [positiveAmount] 格内满足 [mapUnitFilter] 的单位         |
| `Attacks also target [mapUnitFilter] units within [positiveAmount] tiles, with damage decreasing by distance` | 攻击也会同时攻击 [positiveAmount] 格内满足 [mapUnitFilter] 的单位，伤害随距离递减 |
| `Takes [relativeAmount]% damage from own area attacks`                                                        | 受到自身范围攻击时，仅承受 [relativeAmount]% 的伤害                        |
| `Takes [relativeAmount]% counter damage from each unit hit by its area attacks`                               | 其范围攻击每击中一个单位，自身就会承受该单位 [relativeAmount]% 的反击伤害             |

- 添加统计面板数据导出为 csv 表格的功能


## v4.19.15

**发布日期**：2026.2.26

- 为安卓端安装包加入内置模组 [CoeHarMod/和合共生](https://github.com/AutumnPizazz/CoeHarMod)
- 允许相邻城市切换地块，需要在模组的 ModOptions.json 文件中声明 uniques "Allow cities to claim tiles"



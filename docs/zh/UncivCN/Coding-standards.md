---
title: 代码规范
---

# 代码规范（UncivCN 分支）

> **本文件是 UncivCN 分支的工程手册：开发参考（构建 / 项目结构 / 状态模型 / 资源）+ 工程规范，全部条目均来自实际踩坑教训。**
> 修改文档、文档生成器、unique 定义或翻译相关代码前，**必须先通读本文**。

## 一、构建与运行

```bash
./gradlew desktop:run                                # 运行桌面版（主要开发流程）
./gradlew :tests:test                                # 运行全部测试
./gradlew :tests:test --tests "com.unciv.testing.BasicTests"   # 单个测试类
./gradlew desktop:dist                               # 构建 JAR → desktop/build/libs/Unciv.jar
./gradlew server:run                                 # 运行服务器
./gradlew server:dist                                # → server/build/libs/UncivServer.jar
java -jar detekt-cli.jar --parallel --report html:detekt/reports.html \
  --config .github/workflows/detekt_config/detekt-warnings.yml    # Detekt 检查（警告）
```

技术栈：Kotlin 2.1.21 · Gradle 8.11.1 · LibGDX 1.14.0 · Ktor 3.2.3 · Kotlinx Serialization · purity-plugin · Detekt

从零上手（环境准备、IDE 配置）见 [Building-Locally](../Developers/Building-Locally)。

## 二、项目结构

| 模块 | 职责 |
|---|---|
| `core/` | 99% 的游戏代码：逻辑、模型、UI、序列化、规则集。纯 Kotlin，不依赖平台 |
| `desktop/` | 桌面启动器与平台功能（JNA 通知、Discord RPC） |
| `android/` | Android 启动器；`android/assets/` 为各平台共享资源（图片、JSON、翻译） |
| `server/` | 多人服务器（Ktor + WebSocket），独立打包 |
| `tests/` | JUnit 4 + Mockito 单元测试 |

关键包（`core/src/com/unciv/`）：

| 包 | 用途 |
|---|---|
| `models/ruleset/` | 规则集类：Nation、Building、BaseUnit、Technology、Policy 等 |
| `logic/civilization/` | CivilizationInfo 及各管理器（科技/政策/外交） |
| `logic/city/` | CityInfo、城市建造/产出/人口 |
| `logic/map/` | TileMap、TileInfo、寻路（BFS、AStar） |
| `logic/map/mapunit/` | 地图上的单位实例 |
| `logic/battle/` | 战斗结算 |
| `logic/automation/` | AI 与自动化逻辑（工人自动化、下一回合流程） |
| `logic/trade/` | 贸易路线与交易 |
| `ui/screens/wordlscreen/` | 主游戏界面（大部分游玩时间所在） |
| `ui/screens/cityscreen/` | 城市管理界面 |
| `ui/screens/pickerscreens/` | 科技/政策/晋升选择界面 |
| `ui/popups/` | 模态弹窗 |
| `json/` | JSON 序列化配置、UncivJson |

更详细的类关系见 [Project-structure-and-major-classes](../Developers/Project-structure-and-major-classes)。

## 三、游戏状态与回合流程

`GameInfo` 是序列化根节点：内含 `List<CivilizationInfo>`（玩家，各有 `List<CityInfo>`）、`TileMap`（`List<TileInfo>`，可含 `MapUnit`）、`RuleSet`（**不序列化**，从 `android/assets/jsons/` 加载）。每个状态对象持有 `@Transient` 父引用，树可双向遍历。

每回合**先克隆 GameInfo 再在新副本上处理**——保证 UI 线程安全与多人联机确定性（整包收发状态）。

序列化细节见 [Saved-games-and-transients](../Developers/Saved-games-and-transients)。

## 四、文档站维护（VitePress vs 上游 mkdocs）

UncivCN 分支的文档站（`docs-vitepress/`）使用 VitePress（弃 mkdocs：不支持中文搜索）；
上游仍用 mkdocs。两者语法不兼容，维护时务必注意以下**踩坑记录**：

| 项目 | 上游 mkdocs | UncivCN VitePress |
|---|---|---|
| 容器（admonition） | `!!! note` + 内容 **4 空格缩进** | `::: note` + 内容**禁止缩进** |
| 提示块类型 | admonition 全类型 | 内置 tip/info/warning/danger/details；note 需自定义注册 |
| 缩写悬浮提示 | `*[param]: 说明` 自动渲染 abbr | 不识别，改用参数说明表 |
| 文档生成器输出 | mkdocs 格式（缩进容器） | VitePress 格式（无缩进容器） |

### 写作铁律（全部为实际踩过的坑）

1. **容器内容禁止缩进**：VitePress 容器内 4 空格/tab 缩进会被 markdown 当作代码块
   渲染为 `<pre>`，pre 不自动换行，大段落向右撑破屏幕。生成器输出、手工文档均同。
2. **JSON 字面量不翻译**：unique 文本、参数名、Countables 文本/示例必须保留英文原文——
   游戏按文本逐字匹配 `UniqueType.uniqueTypeMap` 才能生效，翻译后模组制作者照抄即失效。
3. **同名参数编号化**：unique 标题中重复参数按 `[amount] [amount]` → `[amount]-[amount2]`
   编号显示（与翻译模板 `getTranslatable()` 一致），读者可区分；示例行仍用原始文本
   （可直接复制进模组 JSON）。
4. **自动生成文档只许改生成器源码**：改完运行 `./gradlew desktop:generateDocs`，
   严禁人工编辑产物。
5. **参数表多行说明**：docDescription 含换行时会撑破 markdown 表格，生成器已将其
   替换为 `<br>`，手工维护表格时同样处理。

## 五、自动生成文档清单

| 产物 | 生成器 | 维护方式 |
|---|---|---|
| `docs/Modders/uniques.md` | `UniqueDocsWriter.write()` | **整体自动**，改生成器源码 |
| `docs/zh/Modders/uniques.md` | `UniqueDocsWriter.writeChinese()` | **整体自动**，改生成器源码 |
| `docs/Modders/Unique-parameters.md` | `UniqueDocsWriter.writeCountables()` | marker 区间自动，其余人工 |
| `docs/zh/Modders/Unique-parameters.md` | `UniqueDocsWriter.writeCountables()` | marker 区间自动（中文翻译内置在生成器），其余人工 |
| `docs/Modders/Mod-file-structure/6-MergeActions.md` | `MergeActionDocsWriter` | **整体自动** |
| `docs/Modders/Creating-a-UI-skin.md` | `UiElementDocsWriter` | marker 区间自动，其余人工 |
| `docs/zh/Modders/Creating-a-UI-skin.md` | `UiElementDocsWriter` | marker 区间自动，其余人工 |
| `docs/Modders/lua-api.lua` | `LuaApiDefinitionWriter` | **整体自动**，签名与文案来自 `LuaApiDocs` |
| `docs/Modders/Lua-API-Reference.md` | `LuaModdingDocsWriter.write()` | **整体自动**，内容来自 `LuaApiDocs` |
| `docs/zh/Modders/Lua-API-Reference.md` | `LuaModdingDocsWriter.writeChinese()` | **整体自动**，内容来自 `LuaApiDocs` |
| `docs/Modders/lua-map-api.lua` | `LuaMapApiDefinitionWriter` | **整体自动**，内容来自 `LuaMapGenApiDocs` |
| `docs/Modders/Lua-Map-API-Reference.md` | `LuaMapModdingDocsWriter.write()` | **整体自动**，内容来自 `LuaMapGenApiDocs` |
| `docs/zh/Modders/Lua-Map-API-Reference.md` | `LuaMapModdingDocsWriter.writeChinese()` | **整体自动**，内容来自 `LuaMapGenApiDocs` |
| 其余 `docs/`、`docs/zh/` 页面 | — | 人工维护 |

**原则**：自动生成的文档只通过「改生成器源码 + 运行 `./gradlew desktop:generateDocs`」维护，
**严禁人工编辑产物**。生成器内置的中文翻译（`docsSentence` / `countablesTranslate`）
同样随源码维护。

**新增 Lua API 的流程**：在 `LuaAPI.apiCatalog` 与 `LuaApiDocs`（签名、中英说明、类别）中
各登记一条，再运行 `./gradlew desktop:generateDocs` —— `lua-api.lua` 与两份
`Lua-API-Reference.md` 会一起重新生成；地图脚本 API 同理走
`LuaMapGenAPI.mapGenApiCatalog` / `LuaMapGenApiDocs`（`lua-map-api.lua` + 两份
`Lua-Map-API-Reference.md`）；`LuaApiDocsTests` / `LuaMapGenApiDocsTests` 会在漂移时
让构建失败。Lua 文档生成器与 `UniqueDocsWriter` 共用 `DocsWriter` 基类
（纯 `generate()` + 写盘分离）。

**仓库已无 Python 依赖**：CN 分支的文档工具链只需 JDK + Node（VitePress + `docs-vitepress/scripts/`
下的 Node 脚本，如 CI 链接检查 `check-docs-links.mjs`）；上游的 `mkdocs.yml` 与 mkdocs
workflow 在本分支已删除——合并上游时若重新出现需再次处理。

**Lua 定义的模组作者分发**：每次发版把 `lua-api.lua` / `lua-map-api.lua` 部署到文档站
（`docs.yml` → VitePress `buildEnd` 复制进产物，URL `https://club.unciv.cn/Unciv/Modders/…`）；
`unciv-lua-api` VSCode 扩展（薄拉取器，源码在 `unciv-lua-api/`，零 npm 依赖，
vsix 随每个 GitHub Release 附带）每次编辑器启动从云端拉取到 `~/.unciv/lua-api/`
并用一键配置命令接入 LuaLS。模组作者从此无需人工检查更新。生成文件头部带
`-- Unciv version:` 标记（来自 `UncivGame.VERSION`），新旧版本一目了然。

本地预览：双击 `docs-vitepress/build.bat`（构建 / 打开现有 / 重建重启三选一，
服务器空闲 5 分钟自动退出）。

## 六、unique 与翻译规范

1. **词条来源**：所有用户可见字符串必须可翻译，词条来源有三：
   - JSON 资源（`android/assets/jsons/`）— 由 `TranslationFileWriter` 自动收集
   - Unique 系统 — `UniqueType` / `UniqueParameterType` 自动生成词条
   - 手动模板 — Kotlin 中的 UI 字符串手动加入 `android/assets/jsons/translations/template.properties`
2. **占位符规则**：
   - `[]`：标签与内容都翻译（如 `[amount] gold`）；**禁止空 `[]`**，用有意义的标签
     （`[amount]`、`[city]` 等），一句话多个占位符必须用不同标签
   - `{}`：内容翻译，周围文字原样
   - 可翻译文本中不能出现 `[]`、`{}`、`<>`，改用 `()`
   - 测试 `allTranslationsHaveNoExtraPlaceholders` 与 `allTranslationsHaveCorrectPlaceholders`
     校验占位符一致性
3. **docDescriptionZh 与 docDescription 同处定义**：新增或修改 `UniqueType` /
   `UniqueParameterType` 的 `docDescription` 时，**必须同时**写 `docDescriptionZh`
   （中文说明），文档生成器自动读取；缺中文时回退英文。禁止在生成器里维护
   大翻译映射表。
4. **JSON 字面量不翻译**（见写作铁律 2）：unique 文本、参数名、Countables 的
   文本/示例是模组 JSON 字面量，保持英文原文；只翻译说明性文字。
5. **翻译接口分流**：
   - 游戏内显示字符串 → 翻译模板（`template.properties` / `Simplified_Chinese.properties`）
   - 纯文档字符串（`docDescription`、UniqueTarget 文档说明、Countables 说明）→
     源码字段（`docDescriptionZh`）或生成器内置翻译（`docsSentence` / `countablesTranslate`）
6. **唯一标识一致性**：翻译 key 用 `getTranslatable()`（重复占位符编号化），
   文档标题展示编号化形式，但示例永远用原始文本。

## 七、分支规范

- **版本号** = 上游版本 + CN 子版本号（如上游 4.21.5 → UncivCN 4.21.5.1；同一上游版本可发 `.1`/`.2`/`.3`…，如 4.20.8.1 → 4.20.8.4；跟进新上游后子版本号从 `.1` 重新开始），
  定义在 `buildSrc/src/main/kotlin/BuildConfig.kt`
- **增量实现**：新功能不修改上游既有行为，关闭相关选项后与上游一致
- **上游合并**：定期把母仓库（master）的新代码合并进 UncivCN 分支，合并冲突自行解决，
  确保母仓库新特性完整接入（不因冲突丢弃）；若母仓库与 CN 分支对同一功能采取了不同实现方式，
  先向用户提问决定采纳哪一方，再继续合并
- **文档镜像**：`docs/zh/` 与英文区一一对应（同一路径即对应翻译）；
  中文独有内容只放 `docs/zh/UncivCN/`
- **中文一等公民**：新 UI 字符串必须进翻译模板；新 unique 说明必须同时提供中文

## 八、更新日志与发版流程

- **微小更新也记日志**：任何非发版改动（功能 / 修复 / CI / 文档站）合入分支时，
  必须在同一提交里同步在中英两份更新日志
  （`docs/UncivCN/Changelog.md` 与 `docs/zh/UncivCN/Changelog.md`）
  顶部的「未发布（Unreleased）」小节各加一行；条目只积累，发版前不删除。
- **发版整合**：发版时把「未发布」小节整体移入新版本条目
  （`## X.Y.Z.N（build NNNN）`），措辞保持不变，然后清空「未发布」小节。
- **更新日志写作规范**：条目一行一条、短句要点（学习上游 `changelog.md` 风格），
  不写长篇解释、不留空话；需要署名时按 `- By 作者` 格式。
- **字数上限（30 字）**：每条日志正文不超过 30 字——中文按汉字数、英文按单词数
  计，标点与文档链接不计入。超长条目一律拆分：一条重要改动可拆成多条小点
  （每条独立一行、各自 ≤30 字），禁止用一条超长日志概括。
- **面向玩家、不写实现细节**：条目描述玩家能感知的变化（功能、修复、行为变更），
  禁止出现类名/函数名/内部机制/重构与测试细节；纯测试与行为不变的重构
  可不记（至多一句「已补充测试加固」）。落笔前自查：这条改动玩家能感知到什么？
  感知不到就不记，能感知就按玩家语言表达。
- **略写底层改动与优化**：底层架构调整、性能优化、重构、上游代码合并等玩家
  不可直接感知的改动，压缩成一条短条目（如「合并上游修复」「性能优化」）
  一句带过，不展开细节；玩家无感的纯内部改动可不记。
- **模组条目**：模组/Lua 功能面向模组作者，允许记录，但只写一句「新增了
  什么能力」（如「新增 XX 类 uniques」「Lua 现可接管 XX」），并附对应文档
  链接（`[uniques](/zh/Modders/uniques)`、`[Lua Modding](/zh/Modders/Lua-Modding)` 等）
  指向细节；字段名/函数名/机制细节一律留给文档，不写进更新日志；同样遵守
  30 字上限（链接不计），能力较多时拆成多条。
- **发版提交**：提交信息只写版本号（如 `4.21.7.2`）。
- **Release 标题**：Deploy 工作流按 tag 名设置（`name: github.ref_name`），
  因此 tag 必须为纯 4 段版本号，release 标题即纯版本号，不带其他字符。
- **版本号提升**：只改 `buildSrc/src/main/kotlin/BuildConfig.kt` 的
  `appVersion` 与 `appCodeNumber`（+1）；每次构建 core 前 `syncGameVersion`
  任务会自动把版本号镜像到 `UncivGame.kt` 的
  `AUTOMATICALLY GENERATED VERSION DATA` 区域（游戏内显示版本号），
  禁止手工改该区域。
- **发版标签**：MSI 安装包版本号取自 git tag（`github.ref_name`，
  4 段式如 `4.21.5.3`）；每次发版必须推送与版本号一致的 tag 触发 Deploy 工作流。
- **上游更新日志**：文档站不再镜像上游 `changelog.md`；上游官方版本记录通过
  CN 更新日志页（`docs/UncivCN/Changelog.md` / `docs/zh/UncivCN/Changelog.md`）
  顶部的链接查看。

### 子仓库模组（CoeHarMod）更新日志

- CoeHarMod 是 Unciv 的 submodule（`android/assets/mods/CoeHarMod`，独立仓库
  AutumnPizazz/CoeHarMod），**实际工作分支是 `workspace`**（`main` 是 GitHub Action
  按标签同步核心文件生成的发布分支）；模组版本号在 `jsons/ModOptions.json` 的
  `modVersion`，适配的游戏版本在 `recommendedGameVersion`
- **每次改动模组（功能/修复/CI/文档）时，必须在同一批改动中同步更新文档站**
  模组板块更新日志 `docs/Community/Mods/CoeHarMod/changelog/index.md` 与
  `docs/zh/Community/Mods/CoeHarMod/更新日志/index.md`（中英两份）：顶部
  「未发布 / Unreleased」小节各加一条（EN 用英文、ZH 用中文）
- 条目风格：一行一条、短句要点；同一版本内按类别用 emoji 分组小节
  （`### ⚖️ 快乐系统优化`、`### 🔧 开发工具` 等；EN 版章节标题用英文，如
  `### ⚖️ Maintenance system rework`），参照现有日志；条目同样面向模组玩家，
  只写用户可见变化，不写实现细节，**每条不超过 30 字（中文按汉字、英文按
  单词计，链接与标点不计），重要改动拆成多条小点**。
- 发版整合：更新顶部 `::: tip 最新版本 / Latest version` 提示块
  （`**vX.Y.Z** - 适配 Unciv A.B.C.D`，EN 版为 `- for A.B.C.D`），把「未发布」
  小节整体移入 `## vX.Y.Z - 适配 A.B.C.D` 版本条目（EN 版为 `## vX.Y.Z - for A.B.C.D`，
  措辞不变），然后清空未发布小节
- 提交流程：模组改动先在 CoeHarMod 子模块（workspace 分支）目录内提交，
  再在主仓库提交更新 submodule 指针；更新日志与模组改动在主仓库同一提交中完成
- 模组依赖主仓库的 Modding 能力（unique/字段/常量），合并上游后如模组受影响，
  需同步跟进模组版本并在更新日志记录

### 发版检查清单与踩坑经验

发版前逐项核对：
- [ ] 版本号：`buildSrc/src/main/kotlin/BuildConfig.kt`（`appVersion` + `appCodeNumber`+1），
      `syncGameVersion` 自动同步 `UncivGame.kt`，构建后确认 `VERSION = Version("x.y.z.n", NNNN)`
- [ ] 更新日志：Unreleased 整体移入新版本条目（中英两份），措辞不变
- [ ] 发版提交：`git commit -m "4.21.7.2"`（仅版本号）；tag 与提交同名推送
- [ ] 本地验证：`./gradlew tests:test` **与** `cd docs-vitepress && npm run docs:build`
      （后者漏跑会导致 CI 的 docs 构建在发版后才发现问题）
- [ ] 推送：分支与 **tag 分开推送**（`git push origin <分支>` + `git push origin <tag>`）；
      Deploy 工作流由 tag 触发，只推分支不会发版

发版后核对 GitHub Release 资产完整性：`UncivCN-<版本>.Apk`、`.jar`、
`Windows64.zip`、`Linux64.zip`、`UncivServer-<版本>.jar`、`.msi`。

踩坑经验：
- **md 文档禁止裸尖括号**：`<gameId>` 之类会被 VitePress/Vue 编译器当作未闭合 HTML
  标签（`Element is missing end tag`）导致 docs 构建失败；用 `{gameId}` 或 `&lt;...&gt;`
- **Maven Central 403**（`Could not GET ... 403 Forbidden`）：GitHub runner 偶发
  速率限制，重跑失败 job 即可，不必改代码
- **appName 改名后遗症**：搜索硬编码旧名（如 Dockerfile 的 `Unciv.jar`、
  `Unciv-Linux64.zip`）——CN 分支产物统一为 `UncivCN.*`
- **`continue-on-error` 的 job 会掩盖失败**：如 `build-msi`（wix 默认按源文件名输出
  `unciv.msi` 而上传路径是 `UncivCN.msi`）——发版后必须核对 Release 资产
- **CN 自研功能的新词条必须同步补 `Simplified_Chinese.properties` 翻译**：
  template 加词条不补中文，中文玩家界面即显示英文（可考虑后续加 CI 检查）

## 九、Mod 与资源

- Mod 位于 `android/assets/mods/`，通过 JSON 扩展规则集
- 图集 `Icons.atlas`、`NationIcons.atlas` 等及音效位于 `android/assets/`（`sounds/` 子目录）
- 规则集 JSON 位于 `android/assets/jsons/`
- IDE 中将 `android/assets/SaveFiles/` 与 `android/assets/mods/` 标记为 Excluded

## 十、其他注意事项

- 版本号定义在 `buildSrc/src/main/kotlin/BuildConfig.kt`，语义化版本，变更记录在 `changelog.md`
- Android 构建需 `local.properties`（`sdk.dir`）或 `ANDROID_HOME` 环境变量
- Android Studio 需将 Kotlin 连续缩进设为 4 空格
- 核心代码必须兼容所有平台；游戏逻辑主要在主线程执行，异步操作需谨慎
- 修改规则集相关代码时考虑 Mod 兼容性

## 相关文档

- [与上游差异（游戏内容对照）](./Differences)
- [UncivCN 更新日志](./Changelog)

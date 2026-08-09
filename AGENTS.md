# Unciv 项目指南

开源 Kotlin 版《文明5》复刻，基于 LibGDX，支持 Android / Desktop / Server。

## 构建与运行

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

## 项目结构

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

## 游戏状态与回合流程

`GameInfo` 是序列化根节点：内含 `List<CivilizationInfo>`（玩家，各有 `List<CityInfo>`）、`TileMap`（`List<TileInfo>`，可含 `MapUnit`）、`RuleSet`（**不序列化**，从 `android/assets/jsons/` 加载）。每个状态对象持有 `@Transient` 父引用，树可双向遍历。

每回合**先克隆 GameInfo 再在新副本上处理**——保证 UI 线程安全与多人联机确定性（整包收发状态）。

## 设计原则

1. **Crashing Early**：游戏状态非法立即崩溃，不要静默继续——bug 暴露得越早越好
2. **无过早抽象**：不要为单一实现建接口，有第二个具体需求再抽象；`if(x != null)` 优先于 `.let{}` 和 `?:`
3. **Modding Philosophy**：一切通过 Unique 系统参数化——最小化对象数量、最大化组合；条件做成可应用于所有 Unique 的 Conditionals；用单位行动修饰符而非特例属性
4. **AI Playing to Win**：AI 像真实玩家一样求胜，不会因"原则"拒绝交易，会在你军事薄弱时进攻
5. **序列化**：所有状态变更必须正确处理序列化；序列化类实现 `IsPartOfGameInfoSerialization`；不需序列化的属性标 `@Transient`

## 代码风格

- `for(item in list)`，不用 `list.forEach{}`（调试体验更好）
- `if(x != null)`，不用 `.let{}` / `?:`（对 Java/C# 背景贡献者更易读）
- Kotlin 连续缩进 **4 空格**
- 提交前删除非翻译文件的尾随空格（`template.properties` 中尾随空格有意义，不能删）
- 注解：`@Readonly`（无副作用）、`@Cache`（缓存方法）、`@VisibleForTesting`（测试辅助方法）

## 翻译系统

所有用户可见字符串必须可翻译，来源有三：
1. **JSON 资源**（`android/assets/jsons/`）— 由 `TranslationFileWriter` 自动收集
2. **Unique 系统** — `UniqueType` / `UniqueParameterType` 自动生成词条
3. **手动模板** — Kotlin 中的 UI 字符串手动加入 `android/assets/jsons/translations/template.properties`

占位符规则：
- `[]`：标签与内容都翻译（如 `[amount] gold`）；**禁止空 `[]`**，用有意义的标签（`[amount]`、`[city]` 等），一句话多个占位符必须用不同标签
- `{}`：内容翻译，周围文字原样
- 可翻译文本中不能出现 `[]`、`{}`、`<>`，改用 `()`
- 测试 `allTranslationsHaveNoExtraPlaceholders` 与 `allTranslationsHaveCorrectPlaceholders` 校验占位符一致性

## 文档站维护

VitePress 文档站（`docs-vitepress/`），**中英完全双向对应**（文件/目录同名，新页面须同时补中英两版）：`docs/` ↔ `docs/zh/` 翻译镜像；独有内容分 `docs/{,zh/}UncivCN/`（分支内容）与 `docs/{,zh/}Community/`（社区内容）。

**改动文档/生成器/unique 定义/翻译前，先读 `docs/zh/UncivCN/Coding-standards.md`（工程规范+全部踩坑）。**

- 生成器产物（uniques.md / Unique-parameters.md / 6-MergeActions.md / Creating-a-UI-skin.md 的 marker 区）**只改生成器源码再 `./gradlew desktop:generateDocs`，严禁人工编辑**；生成器清单见 Coding-standards 第 2 节
- 翻译：**JSON 字面量不翻译**（unique 文本/参数名/Countables 必须英文原文）；`docDescription` 同处写 `docDescriptionZh`，禁止生成器内大映射表
- 更新日志：任何改动合入时同步在中英 `docs/{,zh/}UncivCN/Changelog.md` 顶部的「未发布」小节各加一行，发版时整合为新版本条目（详见 Coding-standards 第 5 节）；上游日志翻译在 `docs/{,zh/}Community/Upstream-changelog.md`
- VitePress 坑：容器内容**禁止缩进**（=代码块撑破屏幕）；`::: note` 需自定义注册；localSearch `provider` 必须在顶层 themeConfig；locale key 用 `root`/`zh` 不带斜杠；srcDir 在工程外需 vue alias + buildEnd 复制 public；bat 必须 CRLF
- 预览：双击 `docs-vitepress/build.bat`（构建/打开/重建三选一，空闲 5 分钟自动退出）

## Mod 与资源

- Mod 位于 `android/assets/mods/`，通过 JSON 扩展规则集
- 图集 `Icons.atlas`、`NationIcons.atlas` 等及音效位于 `android/assets/`（`sounds/` 子目录）
- 规则集 JSON 位于 `android/assets/jsons/`
- IDE 中将 `android/assets/SaveFiles/` 与 `android/assets/mods/` 标记为 Excluded

## 其他注意事项

- 对于 git 提交：用户授权提交时（如明确说“提交”），允许自行参照历史提交风格组织提交标题与正文（subject + body）并执行提交，无需再询问提交信息；一个提交一个主题，提交粒度参照历史提交拆分
- 版本号定义在 `buildSrc/src/main/kotlin/BuildConfig.kt`，语义化版本，变更记录在 `changelog.md`
- Android 构建需 `local.properties`（`sdk.dir`）或 `ANDROID_HOME` 环境变量
- Android Studio 需将 Kotlin 连续缩进设为 4 空格
- 核心代码必须兼容所有平台；游戏逻辑主要在主线程执行，异步操作需谨慎
- 修改规则集相关代码时考虑 Mod 兼容性

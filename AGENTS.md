# Unciv 项目指南

开源 Kotlin 版《文明5》复刻，基于 LibGDX，支持 Android / Desktop / Server。
构建命令、项目结构、游戏状态模型、资源路径等开发参考见 `docs/zh/UncivCN/Coding-standards.md`
第一至三、九节（中英两版同步；改动文档/生成器/unique/翻译前先通读全文）。

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

## 翻译

- 所有用户可见字符串必须可翻译；词条来源、`[]`/`{}` 占位符规则与校验测试见 Coding-standards 第六节
- 改 unique 定义或翻译相关代码前，先读 Coding-standards 第六节

## 文档站

- 中英完全双向对应（`docs/` ↔ `docs/zh/`），新页面须同时补两版；独有内容按性质放 `UncivCN/`（分支内容）或 `Community/`（社区内容）
- 生成器产物只改生成器源码再 `./gradlew desktop:generateDocs`，严禁人工编辑（清单见 Coding-standards 第五节）
- Lua API 文档（`lua-api.lua` + 中英 `Lua-API-Reference.md`）由 `LuaApiDocs` 数据表驱动：新增 Lua API 时同步 `LuaAPI.apiCatalog` 与 `LuaApiDocs`（签名 + 中英说明）后重新生成，`LuaApiDocsTests` 强制一致
- 本分支文档工具链只需 JDK + Node，**无 Python 依赖**（上游 mkdocs 流程已弃用，合入上游时注意）
- VitePress 踩坑与预览方式见 Coding-standards 第四节

## 合并上游

用户明确要求时，将上游仓库（master）的新代码合并进本仓库（UncivCN 分支），自行解决合并冲突，确保母仓库新特性完整接入

解决合并冲突原则：
1.版本号以本仓库为准
2.上游仓库改动服务器时需要评估对server-ts的同步改动
3.上游仓库改动架构时，需要评估本仓库新功能是否需要跟随架构改动
4.上游仓库与本仓库对类似功能实现方式不同时，向用户提问决定采纳哪一方的实现
5.上游仓库改动本仓库已删除或改动的代码时，需要评估上游仓库意图，找到本仓库的代替实现方法，作出更高层面的适配

## 子仓库模组（CoeHarMod）

- CoeHarMod 是 `android/assets/mods/CoeHarMod` 的 git submodule（独立仓库 AutumnPizazz/CoeHarMod），**实际工作分支是 `workspace`**（`main` 由 GitHub Action 按标签同步核心文件生成）；模组改动在子模块目录内提交，再在主仓库提交更新 submodule 指针
- **每次改动 CoeHarMod（功能/修复/CI/文档）时，必须在同一批改动中同步更新文档站模组板块更新日志 `docs/Community/Mods/CoeHarMod/changelog/index.md`（英文）与 `docs/zh/Community/Mods/CoeHarMod/更新日志/index.md`（中文）**：在顶部「未发布 / Unreleased」小节各加一条；发版时整合进版本条目并更新 `::: tip 最新版本 / Latest version` 提示块（详见 Coding-standards 第八节）；条目面向模组玩家，只写用户可见变化，不写实现细节，**每条不超过 30 字（中文按汉字、英文按单词计，链接与标点不计），重要改动拆成多条小点**

## 其他

- 任何改动合入时，同步在中英 Changelog（`docs/{,zh/}UncivCN/Changelog.md`）顶部「未发布」小节各加一行（详见 Coding-standards 第八节）；**条目面向玩家**：只写玩家可感知的行为变化，禁止类名/函数名/重构与测试细节，纯测试与行为不变的重构可不记；**每条不超过 30 字（中文按汉字、英文按单词计，链接与标点不计），重要改动拆成多条小点、每条独立达标；底层改动与优化一律略写**；模组/Lua 条目一句带过并附对应文档链接指路细节
- git操作：自行参照历史提交风格组织提交标题与正文并执行，无需再询问；一个提交一个主题，粒度参照历史提交拆分；在没有用户明确命令时不得执行提交之外的破坏性git操作
- 核心代码必须兼容所有平台；游戏逻辑主要在主线程执行，异步操作需谨慎
- 修改规则集相关代码时考虑 Mod 兼容性

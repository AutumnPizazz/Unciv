# Unciv 项目指南

开源 Kotlin 版《文明5》复刻，基于 LibGDX，支持 Android / Desktop，客户端可连接联机服务。
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
- 文档写作宁删勿堆：重复表述、读者无感的内容直接删掉，不怕丢（git 可复原），也减轻后续提示词压力

## 合并上游

用户明确要求时，将上游仓库（master）的新代码合并进本仓库（UncivCN 分支），自行解决合并冲突，确保母仓库新特性完整接入

解决合并冲突原则：
1.版本号以本仓库为准
2.UncivCN 在线联机固定使用社区运营的闭源 Go 服务（UncivSrv）。服务端独立维护，本仓库不跟踪、构建或分发其源码、镜像和 Release 产物。合并上游时保持客户端协议兼容，不恢复 Kotlin 服务端。
3.上游仓库改动架构时，需要评估本仓库新功能是否需要跟随架构改动
4.上游仓库与本仓库对类似功能实现方式不同时，向用户提问决定采纳哪一方的实现
5.上游仓库改动本仓库已删除或改动的代码时，需要评估上游仓库意图，找到本仓库的代替实现方法，作出更高层面的适配

- 在主仓库运行 `playtest-sim/run.sh` 验证同步回合。真实 Go 接口及重开投票联调仅在有授权的本地私有副本时运行 `playtest-sim/run.sh real`。
- 新建和存量同步回合均使用原子结算协议，不回退到分次上传存档和预览。普通轮流及轮询保留原协议。
- 同步回合的操作须在修改前快照、修改后记录，提前返回也须记录已生效的变更。结算只重放服务端已接收的操作，出现拒绝时，须先恢复状态再提交就绪。
- 同步回合重放按差量合并弹窗和地块探索状态。仅揭开迷雾不占用地块或单位，探索范围与外交状态分开检查冲突。
- 新建同步回合房间（含投票重开生成的房间）由服务端原子兑换管理员签发的 24 小时一次性授权码，既有房间继续使用原有结算流程。
- Web 保持 `-Pweb` 按需构建验证，CN 文档站不部署游戏。

## 子仓库模组（CoeHarMod）

- CoeHarMod 是 `android/assets/mods/CoeHarMod` 的 git submodule（独立仓库 AutumnPizazz/CoeHarMod），**实际工作分支是 `workspace`**（`main` 由 GitHub Action 按标签同步核心文件生成）；模组改动在子模块目录内提交，再在主仓库提交更新 submodule 指针
- CoeHarMod 有玩家可感知的变化或版本兼容性变化时，同步更新 `docs/Community/Mods/CoeHarMod/changelog/index.md` 与 `docs/zh/Community/Mods/CoeHarMod/更新日志/index.md` 的「未发布」小节。发版时整合进版本条目（详见 Coding-standards 第八节）

## 其他

- 更新日志页只放版本、分组和变更条目。玩家或模组作者能感知的变化同步写入中英 Changelog（`docs/{,zh/}UncivCN/Changelog.md`）的「未发布」小节，每条 ≤30 字（链接不计）。实现细节和内部流程留在开发文档，模组与 Lua 条目可链接到对应参考文档（详见 Coding-standards 第八节）
- git操作：自行参照历史提交风格组织提交标题与正文并执行，无需再询问；一个提交一个主题，粒度参照历史提交拆分；在没有用户明确命令时不得执行提交之外的破坏性git操作
- 核心代码必须兼容所有平台；游戏逻辑主要在主线程执行，异步操作需谨慎
- 修改规则集相关代码时考虑 Mod 兼容性

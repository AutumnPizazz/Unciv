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
- VitePress 踩坑与预览方式见 Coding-standards 第四节

## 合并上游

- 定期将母仓库（master）的新代码合并进 UncivCN 分支，自行解决合并冲突，确保母仓库新特性完整接入
- 母仓库与 CN 分支对类似功能实现方式不同时，先向用户提问决定采纳哪一方的实现，再继续

## 其他

- 任何改动合入时，同步在中英 Changelog（`docs/{,zh/}UncivCN/Changelog.md`）顶部「未发布」小节各加一行（详见 Coding-standards 第八节）
- git操作：自行参照历史提交风格组织提交标题与正文并执行，无需再询问；一个提交一个主题，粒度参照历史提交拆分；在没有用户明确命令时不得执行提交之外的破坏性git操作
- 核心代码必须兼容所有平台；游戏逻辑主要在主线程执行，异步操作需谨慎
- 修改规则集相关代码时考虑 Mod 兼容性

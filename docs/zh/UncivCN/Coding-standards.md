---
title: 代码规范
---

# 代码规范（UncivCN 分支）

> **本文件是 UncivCN 分支的工程规范汇总，全部条目均来自实际踩坑教训。**
> 修改文档、文档生成器、unique 定义或翻译相关代码前，**必须先通读本文**。

## 一、文档站维护（VitePress vs 上游 mkdocs）

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

## 二、自动生成文档清单

| 产物 | 生成器 | 维护方式 |
|---|---|---|
| `docs/Modders/uniques.md` | `UniqueDocsWriter.write()` | **整体自动**，改生成器源码 |
| `docs/zh/Modders/uniques.md` | `UniqueDocsWriter.writeChinese()` | **整体自动**，改生成器源码 |
| `docs/Modders/Unique-parameters.md` | `UniqueDocsWriter.writeCountables()` | marker 区间自动，其余人工 |
| `docs/zh/Modders/Unique-parameters.md` | `UniqueDocsWriter.writeCountables()` | marker 区间自动（中文翻译内置在生成器），其余人工 |
| `docs/Modders/Mod-file-structure/6-MergeActions.md` | `MergeActionDocsWriter` | **整体自动** |
| `docs/Modders/Creating-a-UI-skin.md` | `UiElementDocsWriter` | marker 区间自动，其余人工 |
| `docs/zh/Modders/Creating-a-UI-skin.md` | `UiElementDocsWriter` | marker 区间自动，其余人工 |
| 其余 `docs/`、`docs/zh/` 页面 | — | 人工维护 |

**原则**：自动生成的文档只通过「改生成器源码 + 运行 `./gradlew desktop:generateDocs`」维护，
**严禁人工编辑产物**。生成器内置的中文翻译（`docsSentence` / `countablesTranslate`）
同样随源码维护。

本地预览：双击 `docs-vitepress/build.bat`（构建 / 打开现有 / 重建重启三选一，
服务器空闲 5 分钟自动退出）。

## 三、unique 与翻译规范

1. **docDescriptionZh 与 docDescription 同处定义**：新增或修改 `UniqueType` /
   `UniqueParameterType` 的 `docDescription` 时，**必须同时**写 `docDescriptionZh`
   （中文说明），文档生成器自动读取；缺中文时回退英文。禁止在生成器里维护
   大翻译映射表。
2. **JSON 字面量不翻译**（见写作铁律 2）：unique 文本、参数名、Countables 的
   文本/示例是模组 JSON 字面量，保持英文原文；只翻译说明性文字。
3. **翻译接口分流**：
   - 游戏内显示字符串 → 翻译模板（`template.properties` / `Simplified_Chinese.properties`）
   - 纯文档字符串（`docDescription`、UniqueTarget 文档说明、Countables 说明）→
     源码字段（`docDescriptionZh`）或生成器内置翻译（`docsSentence` / `countablesTranslate`）
4. **唯一标识一致性**：翻译 key 用 `getTranslatable()`（重复占位符编号化），
   文档标题展示编号化形式，但示例永远用原始文本。

## 四、分支规范

- **版本号** = 上游版本 + CN 子版本号（如上游 4.21.5 → UncivCN 4.21.5.1；同一上游版本可发 `.1`/`.2`/`.3`…，如 4.20.8.1 → 4.20.8.4；跟进新上游后子版本号从 `.1` 重新开始），
  定义在 `buildSrc/src/main/kotlin/BuildConfig.kt`
- **增量实现**：新功能不修改上游既有行为，关闭相关选项后与上游一致
- **文档镜像**：`docs/zh/` 与英文区一一对应（同一路径即对应翻译）；
  中文独有内容只放 `docs/zh/UncivCN/`
- **中文一等公民**：新 UI 字符串必须进翻译模板；新 unique 说明必须同时提供中文

## 相关文档

- [与上游差异（游戏内容对照）./Differences)
- [UncivCN 更新日志./Changelog)

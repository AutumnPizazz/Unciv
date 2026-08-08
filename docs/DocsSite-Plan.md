# UncivCN 文档站建设计划（VitePress 方案）

> **状态：阶段 1+2 已实施（2026-08-08），阶段 3 待后续**
>
> 已完成：VitePress 工程（docs-vitepress/）+ 英文区（上游 docs 原样）+ i18n 中英切换 + UncivCN 专区 +
> 社区 41 页移植 + kt 生成器 VitePress 格式/中文输出 + CI 部署（.github/workflows/docs.yml）。
> 待办（阶段 3）：更新日志自动生成器、搜索优化（pagefind）、Modders 文档逐步翻译。
>
> 目标：在游戏仓库内建 VitePress 文档站，复用上游 md 文档与 Kotlin 文档生成器遗产，
> 提供中英文切换，并开辟 UncivCN 分支新改动专区。
>
> 本文档是实施蓝图，所有决策与细节均已调研确认，可直接按此执行。

---

## 0. 背景与需求

- 上游 Unciv 有成熟文档遗产：`docs/` 目录（89 个文件）+ 3 个 Kotlin 文档生成器
  （UniqueDocsWriter / UiElementDocsWriter / MergeActionDocsWriter），mkdocs 构建链
- 用户另有社区文档仓库 `unciv-chinese-community`（VitePress 站，47 页中文内容，
  含原版攻略、CoeHarMod 专区、开发者专区、更新日志）
- 需求：
  1. 复用上游 md 文档和网页
  2. 添加中文支持，玩家可随意切换中英文
  3. 开辟新页面介绍 UncivCN 分支新改动
  4. 利用 kt 代码自动化生成文档并构建网页

## 1. 技术路线决策

| 候选 | 结论 | 原因 |
|---|---|---|
| mkdocs（上游在用） | ❌ 放弃 | **不支持中文搜索**（用户明确要求） |
| **VitePress**（社区仓库技术路线） | ✅ 采用 | 与社区仓库一致；内置 localSearch（minisearch）支持中文；mathjax 公式；`: ::` 容器与社区文档语法兼容；可 i18n 中英切换 |

## 2. 总体架构

```
docs/（上游 89 个 md，英文）            ← 完全复用，作为英文区
docs/zh/（中文区，新增）               ← UncivCN 专区 + 移植的社区中文内容
desktop DocsWriter（kt 自动生成）      ← 复用 + 扩展中文输出
docs-vitepress/（VitePress 工程）      ← config.ts（i18n/搜索/主题）+ package.json
.github/workflows/docs.yml            ← CI：kt 生成 → vitepress build → gh-pages
```

## 3. 复用上游遗产（3 件）

1. **`docs/` 89 个 md**：直接作 VitePress 英文区。frontmatter 兼容；
   mkdocs 特有 `!!! note` / `??? example` admonition 需脚本转 `::: note` 格式
   （约 20 处，见 6.2）
2. **3 个 kt 生成器**：天然继承——CN 分支运行 DesktopLauncher 时会自动重写
   `docs/Modders/uniques.md`、`Unique-parameters.md` 等，内容自动包含 CN 独有
   unique（AllowTileClaim 等）。生成器源码：`desktop/src/com/unciv/app/desktop/`
   - UniqueDocsWriter.kt（uniques.md + Unique-parameters.md，marker 插入）
   - UiElementDocsWriter.kt（SkinStrings 等 UI 元素文档）
   - MergeActionDocsWriter.kt（MergeAction 文档，CN 分支有历史提交）
   - 触发点：DesktopLauncher.main 中 `if (!isRunFromJAR) { ...DocsWriter().write() }`
3. **文档网页构建链**：上游 mkdocs workflow 不沿用，仅借鉴结构
   （`.github/workflows/mkdocs.yml`：checkout → 构建 → peaceiris/actions-gh-pages 推 gh-pages 分支）

## 4. 中文支持（i18n 语言切换）

- VitePress 官方 i18n：`/` 英文、`/zh/` 中文，右上角语言切换器
- 中文区内容来源（按优先级）：
  1. **UncivCN 专区**（全新手写）
  2. **移植社区仓库原创内容**：原版攻略 5 页、CoeHarMod 专区 4 页、开发者专区 27 页、
     更新日志 5 页（共 41 页；社区 `: ::` 容器语法与 VitePress 完全兼容，几乎零转换；
     frontmatter 保留；`<Badge>` 组件需转纯文本，仅 2 个文件）
  3. **kt 生成器中文输出**：扩展 UniqueDocsWriter 输出中文版 uniques.md——
     利用现有 unique 翻译模板（template.properties 已有全部 unique 中文翻译）
- 英文区 = 上游 docs/ 原样；中文区逐步扩充，导航只列已存在页面（避免 404）

## 5. UncivCN 新改动专区（核心新页）

`docs/zh/UncivCN/`：
- `index.md` —— 分支介绍（与上游关系、定位、构建方式）
- `新特性.md` —— 逐项：功能说明 + 启用方式（Lua 模组系统、镜像/环形地图、单位钉、
  随机数读档可变性、tile-claim、人口锁地按钮、游戏设置导出/导入剪贴板、
  轮询联机、默认服务器、设置隔离等）
- `更新日志.md` —— CN 版本记录（4.21.0.1 → 4.21.5.1 及以后）
- `与上游差异.md` —— 功能对照表

**kt 自动化扩展**：新增 UncivCNDocsWriter（或 CI 步骤），从 git 版本提交历史自动生成
`更新日志.md` 草稿——复用 `docs/CommunitySync-Design.md` 3.1 节的版本定位思路
（first-parent 遍历 BuildConfig.kt），用 Kotlin 实现；发版时文档自动更新

## 6. 关键技术细节

### 6.1 VitePress 工程（docs-vitepress/）

- 复用社区仓库配置经验（`.vitepress/config.ts`）：
  - `markdown-it-mathjax3`（公式）、`markdown-it-task-lists`（任务列表）
  - 主题：可复用社区 custom.css 换色；logo 用 `docs/assets/Icon.png`
  - 搜索：VitePress 默认 localSearch（中文按字索引，可用）；后续可换 pagefind 增强分词
- 版本：vitepress ^1.5.0（与社区仓库一致）
- 路径规划：vitepress srcDir 指向仓库根 `docs/`？——注意 mkdocs 的 `!!!` 语法转换、
  i18n 目录结构（`docs/zh/` 作为中文 locale 目录）需在 config.ts 的 locales 中配置

### 6.2 mkdocs admonition 转换（脚本，一次性）

- 上游 docs/ 中 `!!! note` / `??? example "..."` / `??? note ""` 等 → `::: note` / `::: example` 格式
- 已确认上游用法样例：
  - `!!! note`（Making-a-new-Civilization.md 等）
  - `??? example "Gain a free [buildingName] [cityFilter]"`（uniques.md 大量，由 kt 生成器输出——**优先改生成器**而非转换产物）
- 注意：uniques.md / Unique-parameters.md 是 kt 生成器产物，转换逻辑应放进生成器
  （或生成器直接输出 VitePress 兼容格式）

### 6.3 kt 文档生成器运行方式

- 现状：DocsWriter 在 DesktopLauncher.main 里 `isRunFromJAR=false` 时执行（启动游戏太重）
- 计划：新增轻量 main（如 `DocsGenerationMain`），只跑三个 DocsWriter + 更新日志生成器，
  供 CI 单独调用（`./gradlew desktop:run --args="--generate-docs"` 或独立 main class）
- 生成器写路径是相对 assets 目录的 `../../docs/...`，CI 中工作目录需正确设置

### 6.4 构建与部署（.github/workflows/docs.yml）

```
on: push branches: [UncivCN] + workflow_dispatch
1. checkout（fetch-depth 0，供更新日志生成器读 git 历史）
2. 跑 kt 文档生成（gradle）
3. setup-node + npm ci（docs-vitepress/）
4. vitepress build → site/
5. peaceiris/actions-gh-pages 推 gh-pages 分支
→ 仓库 Settings → Pages：部署源选 gh-pages 分支（用户手动操作一次）
→ 网址：autumnpizazz.github.io/Unciv（与上游 yairm210.github.io/Unciv 同构）
```

注意：上游 workflow 用 `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true`（Node24 弃用提示），可参考

## 7. 与社区仓库的关系

- 社区仓库原创内容移植进 `docs/zh/` 后，社区仓库**停更留档**（保留历史，旧链接可访问）
- 移植清单：原版专区 5 页、模组专区 4 页、开发者专区 27 页、更新日志 5 页 = 41 页
- 社区仓库路径：`C:\Users\27162\Documents\GitHub\unciv-chinese-community`（本地）
  云端 `github.com/AutumnPizazz/unciv-chinese-community`

## 8. 工作量估算

| 任务 | 工作量 |
|---|---|
| VitePress 工程 + i18n + 搜索配置 | 0.5 天 |
| mkdocs admonition 转换（20 处，含生成器侧） | 2 小时 |
| UncivCN 专区页面（手写中文） | 0.5–1 天 |
| 社区内容移植（41 页，脚本+校对） | 0.5–1 天 |
| kt 生成器中文输出 + 更新日志生成器 | 0.5–1 天 |
| CI workflow + 部署验证 | 2–3 小时 |

## 9. 分阶段实施

- **阶段 1（最小可用）**：VitePress 工程 + 英文区（上游 docs 原样）+ UncivCN 专区中文页
  + i18n 切换 + CI 部署 → 上线
- **阶段 2**：移植社区仓库内容（41 页）、kt 生成器中文输出（uniques 中文版）
- **阶段 3**：更新日志自动生成、搜索优化（pagefind 分词）、Modders 文档逐步翻译

## 10. 待确认决策（实施前问用户）

1. 部署位置：游戏仓库 Pages（`autumnpizazz.github.io/Unciv`）——需用户启用 Pages 并选 gh-pages 分支 ✅ 已确认
2. 阶段 1 先上线还是全部做完再上 ✅ 已确认：阶段 1+2 一次实施
3. 社区仓库内容全部移植还是先移植开发者专区 ✅ 已确认：脚本全量移植
4. kt 生成器扩展范围（中文 uniques 输出、更新日志生成器）是否都做 ✅ 已确认：中文输出做，更新日志生成器留阶段 3

## 10.1 实施记录（2026-08-08）

已交付：

- `docs-vitepress/`：VitePress 工程（vitepress ^1.5+，base `/Unciv/`，srcDir=../docs，i18n root/zh）
  - 关键坑：locale key 必须用 `root` + 目录名（`/` 会被当正则永远匹配，导致 locale 失效）；
    srcDir 在 docs-vitepress 之外时需 `vite.resolve.alias.vue` 指回本工程 node_modules
- 英文区：上游 docs/ 原样复用（admonition 已转 `:::`，5 处裸 HTML 标签已包反引号）
- 中文区 `docs/zh/`：社区 40 页全量移植（链接加 /zh 前缀、Badge 转文本）+ UncivCN 专区 4 页
- kt 生成器：UniqueDocsWriter 输出 VitePress 容器格式 + 中文版 Unique能力列表.md（复用
  Simplified_Chinese.properties 翻译 + 内置文档句翻译）；MergeActionDocsWriter 同步转换；
  DesktopLauncher 与新增轻量入口 `DocsGenerationMain`（`./gradlew desktop:generateDocs`）
- CI：`.github/workflows/docs.yml`（push UncivCN/master + workflow_dispatch → gradle 生成 →
  npm ci/build → 链接检查 → gh-pages）
- 工具脚本：`docs-vitepress/scripts/`（转换/移植/链接检查）

遗留：

- 用户需在仓库 Settings → Pages 选择 gh-pages 分支部署源（一次手动操作）
- 英文独有页面（Credits、Developers/* 等）在中文区的语言切换会 404（VitePress 固有行为，
  翻译完成后自然消失）
- 社区版 Unique能力列表.md 的 tooltip（span title）结构未保留，由生成器版替代（信息等量，
  无悬浮提示）
- 阶段 3：更新日志自动生成、pagefind、Modders 文档逐步翻译

## 10.2 中文区重构与全量翻译（2026-08-09）

按用户要求，中文区抛弃社区仓库的目录结构，改为**英文文档的翻译镜像**：

- 新结构：`docs/zh/{Modders, Developers, Translating, Other}/` 与英文一一对应（同名文件），
  语言切换同一路径即对应翻译；社区独有内容保留在 `UncivCN/`、`原版专区/`、`模组专区/`、`更新日志/`
- 19 个已有社区翻译迁入镜像位置（restructure_zh.py + fix_zh_links.py + fix_zh_anchors.py），
  内部链接全部重写为 /zh/ 绝对路径，顺带修复社区原有死链
- **全量补齐翻译**（16 个缺失文档）：Developers 6 个 + Other 6 个 + 顶层 4 个（Credits 由
  translate_credits.py 生成：翻译标题/说明，保留名单）——中英 36 个文档现在一一对应
- 生成器：中文 uniques 输出路径改为 `docs/zh/Modders/uniques.md`；mkdocs 缩写语法
  `*[param]: desc` 改为标准参数表（VitePress 不识别缩写）
- config.ts 中文导航重写为镜像结构；构建验证 0 死链、0 渲染残留（DocsSite-Plan 除外）

## 11. 相关参考

- 联动设计文档（含版本定位算法、跨仓库教训）：`docs/CommunitySync-Design.md`
- 上游 mkdocs workflow：`.github/workflows/mkdocs.yml`（已删除？见 git 历史）
- 上游 mkdocs 配置：`mkdocs.yml`（若已随上游合入仍在仓库根）
- kt 生成器源码：`desktop/src/com/unciv/app/desktop/UniqueDocsWriter.kt` 等
- 社区仓库 VitePress 配置：`unciv-chinese-community/.vitepress/config.ts`

# UncivCN ↔ 中文社区仓库联动（设计文档）

> **状态：已暂停实施（2026-08-08）**
>
> 曾实现一版 CI 自动联动（workflow + Python 脚本），实测可跑通全链路
> （游戏仓库打 tag → 自动生成内容 → 社区仓库 PR），但产出的更新日志草稿
> 依赖大量人工整理，自动化质量不达预期。**相关代码已撤销**，本文件保留
> 完整思路、架构、踩坑记录与复原步骤，供以后重新实施时参考。

---

## 1. 背景与目标

两个仓库的关系：

| | 游戏仓库 | 社区仓库 |
|---|---|---|
| 本地路径 | `C:\...\GitHub\Unciv` | `C:\...\GitHub\unciv-chinese-community` |
| 云端 | `github.com/AutumnPizazz/Unciv` | `github.com/AutumnPizazz/unciv-chinese-community` |
| 内容 | UncivCN 游戏代码、内置模组、`docs/Modders/` 文档 | VitePress 中文文档站（push main 自动部署 Pages） |

社区仓库是游戏仓库的**下游消费方**，共 4 条联动线索：

1. **更新日志**：社区 `更新日志/UncivCN/index.md` 记录 CN 版本（人工撰写）；
   游戏发版后需要新增条目（如 4.21.0.2 → 4.21.5.1）
2. **开发者文档**：游戏 `docs/Modders/`（uniques.md、Unique-parameters.md、Lua-Modding.md、
   MergeAction 等）↔ 社区 `开发者专区/模组开发/`（社区为人工翻译/重写，非直接复制）
3. **翻译状态**：游戏 `android/assets/jsons/translations/Simplified_Chinese.properties`
   的完成度（新增 key、未翻译数量）
4. **CoeHarMod 模组**：游戏内置 CoeHarMod（另有独立仓库 `AutumnPizazz/CoeHarMod`），
   社区 `模组专区/CoeHarMod/` 记录其版本与更新日志

---

## 2. 目标架构（已实现并验证可跑通）

```
[游戏仓库] 打 tag（如 4.21.5.1）或手动触发 workflow_dispatch
        │
        ▼
GitHub Actions: community-sync.yml（游戏仓库内）
        │  1. 检出游戏仓库（fetch-depth: 0，含 tags）
        │  2. 用 PAT 检出社区仓库到 ../community-repo
        │  3. 运行 sync.py：
        │     ├─ 更新日志草稿   → 社区 更新日志/UncivCN/草稿-v<版本>.md
        │     ├─ 文档同步报告   → 社区 同步报告/文档同步报告-v<版本>.md
        │     ├─ 翻译状态报告   → 社区 同步报告/翻译状态报告-v<版本>.md
        │     └─ 模组同步报告   → 社区 同步报告/模组同步报告-v<版本>.md
        │  4. push 到社区仓库 sync/<版本> 分支
        │  5. 创建 PR（gh CLI，GH_TOKEN=PAT）
        ▼
[社区仓库] PR → 人工审查整理（草稿进 index.md、按报告核对文档）→ 合并
        ▼
GitHub Pages 自动部署（社区仓库既有 deploy.yml）
```

关键设计决策：

- **游戏仓库驱动**：tag 触发在游戏仓库内，社区仓库无需任何感知机制；
  游戏仓库无法直接写社区仓库 main，故走 **sync/ 分支 + PR**，人工把关
- **内容生成与人工整理分离**：CI 只产出草稿与报告，不直接改社区正式文档
  （更新日志条目、文档翻译需人工撰写，机器只能给素材）
- **更新日志只列 CN 主干**：`git log --first-parent` 取 CN 自己的提交线；
  merge 提交引入的上游提交自动归并摘要（按 View 重构/性能优化/bug 修复/其他 计数），
  避免罗列全部上游提交
- **无 PAT 降级**：secret 缺失时 workflow 仍运行并生成内容（只读 clone），跳过推送

---

## 3. 脚本功能规格（sync.py）

纯 Python 标准库 + git 命令行，本地与 CI 通用（Windows/Linux 均可）。

### 3.1 版本定位（关键）

CN 发版不打 tag、版本号写在 `buildSrc/src/main/kotlin/BuildConfig.kt`（appVersion）
与 `core/src/com/unciv/UncivGame.kt`（VERSION）。定位方法：

```
git log --first-parent --format=%H -- buildSrc/src/main/kotlin/BuildConfig.kt
→ 逐提交读取 BuildConfig.kt 的 appVersion：
  - 最近一个 == 当前版本 的提交 = intro_commit（版本引入点）
  - 再往前最近一个 != 当前版本 的提交 = prev_commit（上一版本状态）
→ 更新日志范围 = prev_commit..intro_commit
```

注意：`git log -S` 无法匹配 merge 提交中的版本号变更（默认忽略 merge），
必须用上述 first-parent 遍历方式。

### 3.2 更新日志草稿

- CN 主干提交按关键字分类：合并上游 / 新功能 / 修复 / 性能优化 / 翻译 / 其他
- merge 提交：`git log <sha>^1..<sha>^2` 取上游提交 → summarize_upstream()
  按 "View implementation"、"perf"、"fix" 等前缀归并计数
- 版本标记类提交（"Version rollout"、"4.21.x"）跳过
- 输出格式对齐社区 index.md 惯例：`## vX.Y.Z <Badge type="tip" text="最新" />`、
  日期 `YYYY.M.D`、末尾附完整提交链接列表

### 3.3 文档同步报告

- 维护映射表：游戏 `docs/Modders/<file>` ↔ 社区 `开发者专区/模组开发/<file>`
  （13 对，如 uniques.md ↔ Unique能力列表.md、Lua-Modding.md ↔ Lua脚本.md）
- 用 `git log -1 --format=%ci -- <path>` 取两侧最后修改时间，游戏侧更新则标
  "⚠️ 需核对"（社区文档是翻译/重写，只能提示，不能自动覆盖）

### 3.4 翻译状态报告

- 统计 template.properties 与 Simplified_Chinese.properties 的 key 数
- `git diff --numstat <prev> HEAD -- template.properties` 展示本版本模板变更

### 3.5 CoeHarMod 模组报告

- 游戏内置 CoeHarMod 无版本号字段（modOptions.json 无 modVersion），
  只能报告其最后修改提交时间 + 社区记录的版本号，提示人工核对

---

## 4. CI workflow 规格（community-sync.yml）

- 触发：`push: tags: ['*.*.*', 'v*.*.*']` + `workflow_dispatch`
  （注意：`4.21.5.1` 是 4 段，glob 模式必须能匹配）
- job 级 `env: COMMUNITY_SYNC_TOKEN: ${{ secrets.COMMUNITY_SYNC_TOKEN }}`
  （step 级 if 无法引用该 step 自身的 env，必须提升到 job 级才能做
  `if: env.COMMUNITY_SYNC_TOKEN != ''` 判断）
- checkout 社区仓库：`git clone https://x-access-token:${PAT}@github.com/...`
- 提交：`git checkout -b sync/<版本>` → add 草稿+报告 → commit → push → 
  `gh pr create --body-file`（PR body 多行文本必须写文件再传，不能内联在 YAML 中）
- 同名 PR 已存在时跳过创建（`gh pr list --head` 判断）

---

## 5. 踩坑记录（复原必读）

1. **GitHub Actions secrets 无法用 fine-grained PAT 通过 API 创建**
   （"Resource not accessible by personal access token"）——必须网页手动：
   `https://github.com/AutumnPizazz/Unciv/settings/secrets/actions`
2. **fine-grained PAT 的仓库授权范围**：CI 要写社区仓库，token 必须
   Repository access 包含 `unciv-chinese-community`，Permissions 含
   `Contents: Read and write` + `Pull requests: Read and write`；
   否则 push 时报 `Permission denied ... 403`（读正常、写被拒，难排查）
3. **YAML 陷阱**：`on:` 在 YAML 1.1 是布尔别名（GitHub 用 1.2 无碍，本地校验工具会误报）；
   多行 shell 字符串必须用块标量 `|`；PR body 用 heredoc + `--body-file`
4. **/scripts 目录被游戏仓库 .gitignore 忽略**（内含本地 client2），
   脚本必须放 `.github/` 下才会入库
5. **template.properties 有 `" " = `（空格 key）**，统计行数时不能先 strip 整行，
   否则空格 key 行被破坏导致计数为 0
6. **本地 git 凭据**：修改/重建 token 后 Windows 凭据管理器中的旧凭据失效，
   push 报 403——需在凭据管理器删除 `git:https://github.com` 条目重新认证
7. **workflow_dispatch 需要 token 对游戏仓库有 Actions: write**；
   用删 tag 重打的方式触发则只需 push 权限（CI 验证时曾用此法）
8. **版本定位**：`git log -S` 不匹配 merge 提交，必须 first-parent 遍历（见 3.1）

---

## 6. 现有资产（撤销后仍保留，可复用）

| 资产 | 位置 | 说明 |
|---|---|---|
| 社区仓库 PR #3 | `unciv-chinese-community`（云端） | 4.21.5.1 的草稿+3 份报告，可手动整理合并 |
| 本地生成产物 | 社区仓库工作区 `更新日志/UncivCN/草稿-v4.21.5.1.md`、`同步报告/` | 同上，本地副本 |
| secret | 游戏仓库 `COMMUNITY_SYNC_TOKEN` | 未删除，复原时直接可用 |
| 版本 tag | 游戏仓库 `4.21.5.1` | 发版标记，保留 |
| 本设计文档 | `docs/CommunitySync-Design.md` | 本文档 |

---

## 7. 复原步骤（重新实施时）

1. 恢复两个文件（可从 git 历史 `14d070633` 检出）：
   ```bash
   git checkout 14d070633^ -- .github/workflows/community-sync.yml .github/community-sync/
   ```
   （或按 3、4 节重新编写）
2. 确认 secret `COMMUNITY_SYNC_TOKEN` 存在且 PAT 权限含社区仓库
   （Contents/PR 均 Read and write）
3. 本地跑通：`python .github/community-sync/sync.py`（默认输出到
   `../unciv-chinese-community`，可用 `--community-repo` 指定）
4. 推送到游戏仓库后打 tag 或手动触发，验证 PR 生成
5. 已知待改进项（见 8）实施后再考虑自动化程度提升

---

## 8. 已知不足（本次撤销的原因与改进方向）

1. **更新日志质量**：自动生成的草稿是提交列表分类汇总，与社区现有
   人工撰写的可读条目（如 4.21.0.2 的"修复UCCC模组部分bug"）差距大，
   仍需人工逐条撰写。改进方向：维护"重要变更"人工映射表（版本→要点），
   CI 只负责占位与提醒
2. **文档/翻译/模组报告粒度粗**：只提示"需核对"，不能自动 diff 内容。
   改进方向：对可机器对比的部分（如 uniques 列表）做结构化 diff
3. **CoeHarMod 无版本号**：内置模组无法自动取版本，需模组侧增加版本字段
   （如 modOptions.json 加 modVersion）才能全自动
4. **PR 流程繁琐**：每个版本一个 PR，人工整理+删除草稿。改进方向：
   考虑直接生成可合并的正式更新日志（质量达标后）

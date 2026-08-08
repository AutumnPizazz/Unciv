# UncivCN ↔ 中文社区仓库联动

自动同步 UncivCN（本仓库）发版信息到 [unciv-chinese-community](https://github.com/AutumnPizazz/unciv-chinese-community)（VitePress 中文社区文档站）。

## 联动架构

```
[游戏仓库 Unciv] 打 tag（如 4.21.5.1）或手动触发
        │
        ▼
GitHub Actions: community-sync.yml
        │  1. 读取 BuildConfig.kt 版本号，定位上一版本提交（git first-parent）
        │  2. 运行 .github/community-sync/sync.py
        │     ├─ 更新日志草稿   → 更新日志/UncivCN/草稿-v<版本>.md
        │     ├─ 文档同步报告   → 同步报告/文档同步报告-v<版本>.md
        │     ├─ 翻译状态报告   → 同步报告/翻译状态报告-v<版本>.md
        │     └─ 模组同步报告   → 同步报告/模组同步报告-v<版本>.md
        ▼
[社区仓库] sync/<版本> 分支 + Pull Request
        │
        ▼
人工审查 PR：整理草稿进 index.md、按报告核对文档 → 合并
        │
        ▼
GitHub Pages 自动部署（社区仓库既有 deploy.yml）
```

## CI 配置（一次性）

1. 在 GitHub 创建 Personal Access Token：
   - 访问 https://github.com/settings/tokens?type=beta （Fine-grained）或
     https://github.com/settings/tokens （classic，勾选 `repo`）
   - 权限（Fine-grained）：**Contents: Read/Write**、**Pull requests: Read/Write**，
     仓库范围选择 `AutumnPizazz/unciv-chinese-community`
2. 在本仓库 Settings → Secrets and variables → Actions → New repository secret：
   - Name：`COMMUNITY_SYNC_TOKEN`
   - Value：粘贴上面创建的 token
3. 之后每次发版（打 tag 或手动触发 workflow），CI 会自动生成内容并向社区仓库发起 PR。

> 未配置 token 时，workflow 仍会运行并生成内容（只读克隆社区仓库），但跳过推送与 PR 步骤——日志中可见生成结果。

## 本地使用

```bash
# 在游戏仓库根目录，生成全部内容（默认输出到 ../unciv-chinese-community）
python .github/community-sync/sync.py

# 指定路径 / 版本 / 上一版本
python .github/community-sync/sync.py \
  --game-repo "C:/.../Unciv" \
  --community-repo "C:/.../unciv-chinese-community" \
  --version 4.21.5.1 \
  --prev 10e3b8ad

# 只生成某一项
python .github/community-sync/sync.py changelog
python .github/community-sync/sync.py docs-report translation mod-report
```

## 发版 → 社区同步的标准流程

1. 游戏仓库发版：更新版本号（BuildConfig.kt + UncivGame.kt）并打 tag（如 `4.21.5.1`）
2. CI 自动：生成 4 份内容 → 社区仓库 `sync/4.21.5.1` 分支 → 创建 PR
3. 人工在社区仓库：
   - 将 `更新日志/UncivCN/草稿-v4.21.5.1.md` 的内容**整理**进 `更新日志/UncivCN/index.md`
     （把 `<Badge type="tip" text="最新" />` 移到新条目，删除旧条目上的 badge）
   - 按 `同步报告/文档同步报告-*.md` 核对开发者专区文档（社区文档是人工翻译/重写，需人工判断）
   - 按 `同步报告/翻译状态报告-*.md` 决定是否补翻译
   - 按 `同步报告/模组同步报告-*.md` 核对 CoeHarMod 模组专区
   - 删除草稿/报告文件（或移入归档），合并 PR
4. 社区仓库 push main 自动触发 GitHub Pages 部署

## 脚本说明（.github/community-sync/sync.py）

- 纯 Python 标准库 + git 命令行，本地与 CI 通用（Windows/Linux 均可）
- 版本定位：沿 `--first-parent` 遍历 BuildConfig.kt 历史，找最近的版本号变更提交，
  以「上一版本提交..当前版本提交」为更新日志范围
- 更新日志主列表只含 CN 主干提交（merge 提交引入的上游提交自动归并摘要：
  View 重构 / 性能优化 / bug 修复 / 其他改动 各计提交数）

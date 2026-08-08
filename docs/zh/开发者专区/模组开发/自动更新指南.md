---
title: 自动更新指南
---

# 自动更新指南

Unciv 包含内置的图像打包和自动更新 uniques 的功能。

你可以通过向你的仓库添加 GitHub Actions 工作流来自动化这些更新。

- 在你的 Github 页面转到 "actions" 标签
- 为此仓库推荐 > Simple workflow > "Configure"
- 将[此文件](https://github.com/yairm210/Unciv-IV-mod/blob/master/.github/workflows/autoupdate.yml)（"copy raw file"）的文本复制到你仓库中的新文件
- 在顶部将文件名更改为 "autoupdate.yml"
- "Commit changes"（绿色按钮，右上角） > "Commit changes"

在每次提交时，以及每天一次，它将：

- 尝试压缩 PNG 图像（这是无损的，像素数据保持不变，但文件占用更少空间）
- 尝试自动更新已弃用的 uniques

如果有更改，这将创建一个 PR 到你的仓库 - [这里有一个示例](https://github.com/yairm210/Unciv-IV-mod/pull/31) - 你可以选择接受

如果你看到自动更新不是 100% - 在这种情况下与我联系，我们会解决它 🙂

## 错误

如果你收到 `remote: Permission to <...>.git denied to github-actions[bot].`
你需要：

- 打开 Settings > Actions > General
- 在 "Workflow permissions" 下选择 "Read and write permissions"、"Allow GitHub Actions to create and approve pull requests"

---

### ⚠️ 中文社区特别说明

原版 Unciv 项目使用 GitHub Actions 实现模组自动更新。

**Unciv 中文社区采用人工审核更新机制**：
- 模组作者通过 QQ 群或 GitHub Issue 提交更新
- 社区维护者进行人工审核
- 审核通过后由社区统一发布

这样可以确保：
- 翻译质量
- 模组兼容性
- 中文用户的使用体验
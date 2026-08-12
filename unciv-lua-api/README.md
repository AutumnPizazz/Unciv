# Unciv Lua API (VSCode extension)

Unciv 模组开发专用的 Lua API 定义**云端薄拉取器**：每次 VSCode 启动自动从 UncivCN 文档站
拉取最新 `lua-api.lua` / `lua-map-api.lua` 到 `~/.unciv/lua-api/`，配合 LuaLS 获得
永不过期的补全。零 npm 依赖。

> **模组作者请查看使用指南**：<https://club.unciv.cn/Unciv/Modders/Lua-API-Extension>
> （或仓库 `docs/Modders/Lua-API-Extension.md`，中英双语，包含安装步骤、原理与 FAQ）

## 开发者信息

- 源码：UncivCN 仓库 `unciv-lua-api/` 目录（本目录）
- 打包：`npm run package`（即 `npx @vscode/vsce package`），产物 `unciv-lua-api.vsix`
  由发版 workflow（`.github/workflows/buildAndDeploy.yml`）自动构建并附到 GitHub Release
- 定义文件单一来源：`docs/Modders/lua-api.lua` / `lua-map-api.lua`（生成器产物，
  见仓库 Coding-standards 第五节），VitePress 构建时自动复制进文档站产物
- 拉取源：文档站 `https://club.unciv.cn/Unciv/Modders/` 优先，GitHub raw 备用，
  失败自动切换；离线保留本地旧版
- 更新判断：定义文件头部 `-- Unciv version: x.y.z (build n)` 标记，内容变化才写盘

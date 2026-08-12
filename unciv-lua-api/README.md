# Unciv Lua API (VSCode 扩展)

Unciv 模组开发专用：**Lua API 定义自动更新器**。每次 VSCode 启动自动从云端拉取最新
`lua-api.lua` / `lua-map-api.lua` 并写入 `~/.unciv/lua-api/`，配合 LuaLS 语言服务器
（VSCode 扩展 "Lua" by sumneko）获得永不过期的代码补全、悬停说明与拼写检查。

> 核心设计：**数据与逻辑解耦**。本扩展是薄拉取器，不内置定义文件——定义由游戏代码
> 生成后部署在文档站（每次发版自动更新），扩展每次激活实时拉取。所以 API 如何演变，
> 本扩展都无需更新；模组作者装一次即可永远拿到最新版。

## 安装

1. 从 UncivCN 的 [GitHub Release](https://github.com/AutumnPizazz/Unciv/releases) 下载
   `unciv-lua-api.vsix`（跟随游戏版本发布）；
2. 在 VSCode 中执行 **Extensions → ⋯ → Install from VSIX…** 选择该文件；
3. 打开任意模组工作区，运行命令面板（Ctrl+Shift+P）→ **Unciv: Configure Lua API
   autocompletion** —— 一键把 `~/.unciv/lua-api` 加入 LuaLS `workspace.library`
   （用户级设置，所有模组生效，只需一次）；
4. 开始写 Lua：`ctx.` 自动补全、悬停说明、`ctx.civ.addGoldd(...)` 拼写错误即时标红。

无需下载游戏源码、无需运行游戏、无需手动复制任何文件。

## 工作方式

- **自动同步**：VSCode 启动及打开 `.lua` 文件时，后台从云端（文档站
  `club.unciv.cn`，失败自动切换 GitHub raw）拉取最新定义，内容有变化才写盘，
  状态栏短暂提示 "Unciv Lua API 已更新"；
- **版本可辨**：定义文件头部带 `-- Unciv version: 4.21.6.6 (build 1250)`，
  打开文件即可确认版本；本地旧版在拉取失败时保留，不打断工作；
- **手动刷新**：命令面板 → **Unciv: Update Lua API definitions now**。

## 命令

| 命令 | 作用 |
|------|------|
| Unciv: Configure Lua API autocompletion | 把 `~/.unciv/lua-api` 加入 LuaLS workspace.library（用户级，幂等） |
| Unciv: Update Lua API definitions now | 立即从云端拉取最新定义并落盘 |

## 常见问题

**为什么不用市场自动更新？** 本扩展逻辑极少变化（薄拉取器），定义数据走云端实时拉取，
因此不依赖扩展市场推送也能永远最新；后续如需上 Open VSX / VSCode Marketplace，
CI 加一步发布即可，扩展本体无需改动。

**离线能用吗？** 能。拉取失败时保留上次成功同步的定义（附版本头可辨新旧），
并保留 LuaLS 补全能力；联网后下次启动自动恢复。

**想自己托管？** 在 VSCode 用户设置里修改 `uncivLuaApi.sources`（尚未开放配置时，
直接编辑扩展源码中的 `SOURCES` 数组即可）。

## 开发者

- 源码位于 UncivCN 仓库 `unciv-lua-api/` 目录；零 npm 依赖（原生 `node:https`）。
- 打包：`npm run package`（即 `npx @vscode/vsce package`），产物 `unciv-lua-api.vsix`。
- 定义文件单一来源：`docs/Modders/lua-api.lua` / `lua-map-api.lua`（生成器产物，
  见仓库 Coding-standards 第五节），文档站 CI 部署后即被本扩展拉取。

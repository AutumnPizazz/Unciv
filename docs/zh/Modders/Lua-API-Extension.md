# Unciv Lua API 扩展（VSCode）

**一次安装，补全永不过期。** `unciv-lua-api` VSCode 扩展是一个云端薄拉取器：每次启动 VSCode（或打开 `.lua` 文件）都会从 UncivCN 文档站拉取最新的 `lua-api.lua` / `lua-map-api.lua` 并同步到 `~/.unciv/lua-api/`。配合 LuaLS 语言服务器，`ctx.…` 的自动补全、悬停说明与拼写检查**永不落后**——无需手动下载、无需复制文件、无需检查更新。

## 为什么需要它

Lua API 定义文件（`lua-api.lua` / `lua-map-api.lua`，由游戏代码生成）会随每次发版变化。手工把定义复制进模组意味着：

- 忘记检查新版本 → 补全过期、提示错误；
- 每个模组都要一份副本，需反复重新同步；
- 新 API（如 `civ.discoverTech()`）在你想起更新之前永远不补全。

扩展通过**数据与逻辑解耦**解决这个问题：定义文件放在云端（每次发版自动部署），扩展只负责拉取。扩展本体几乎永远不需要更新。

## 安装

1. **安装 Lua 语言服务器**：在 VSCode 中安装 **Lua** 扩展（作者 sumneko，即 LuaLS 语言服务器）
2. **安装 Unciv Lua API 扩展**：从最新的 [UncivCN Release](https://github.com/AutumnPizazz/Unciv/releases) 下载 `unciv-lua-api.vsix`（每个 Release 都附带，约 6 KB），然后在 VSCode 中执行 **扩展 → ⋯ → 从 VSIX 安装…** 选择该文件
3. **一键配置**：打开命令面板（Ctrl+Shift+P），运行 **Unciv: Configure Lua API autocompletion**。它会将 `~/.unciv/lua-api`（绝对路径）加入你的用户级 `Lua.workspace.library` 设置——对**本机所有模组工作区**生效，只需做这一次

完成。打开任意模组目录开始写：`ctx.` 自动补全，悬停 `ctx.civ.addGold(` 显示签名，`ctx.civ.addGoldd(...)` 这类拼写错误即时红色波浪线。地图脚本（`GenerateMap(ctx)`）同样受益于 `lua-map-api.lua`——两个文件都由扩展自动管理。

## 工作原理

```
游戏代码 → 生成 lua-api.lua / lua-map-api.lua（头部带 -- Unciv version: 标记）
    ↓ 每次发版自动部署
文档站：https://club.unciv.cn/Unciv/Modders/lua-api.lua
    ↓ 每次 VSCode 启动 / 打开 .lua 文件时拉取（备用源 GitHub raw 自动切换）
扩展 → ~/.unciv/lua-api/
    ↓ 一键配置
LuaLS workspace.library（用户级设置）→ 所有模组自动补全
```

- **更新判断**：生成文件头部带 `-- Unciv version: 4.21.6.6 (build 1250)` 标记，新旧版本一目了然；扩展仅在内容真正变化时写盘
- **离线可用**：云端不可达时保留上次同步的副本（带版本头），补全照常工作，联网后下次启动自动追平
- **手动刷新**：命令面板 → **Unciv: Update Lua API definitions now**

## 手动配置（备选）

不想用扩展的话，把定义文件从仓库 `docs/Modders/`（或[文档站](https://club.unciv.cn/Unciv/Modders/lua-api.lua)）复制进模组，并创建 `.luarc.json`：

```json
{
    "runtime.version": "Lua 5.2",
    "workspace.library": [
        ".lua-api"
    ],
    "diagnostics.globals": ["ctx"]
}
```

> 模组检查器已白名单豁免模组根目录的 `.luarc.json`——请保持这个确切文件名（LuaLS 只从 workspace 根目录读取 `.luarc.json`；改名会破坏自动补全，检查器也会把它当作放错位置的文件报告）。

记得每次游戏更新后重新复制——这正是扩展帮你消灭的麻烦。

## 常见问题

**需要游戏或源码吗？** 不需要。只需 VSCode + Lua 语言服务器 + 本扩展。

**为什么不在 VSCode 市场里？** 扩展刻意做成薄拉取器——定义文件实时来自云端，不依赖市场推送也能保持最新。将来如需上架（Open VSX / VSCode Marketplace），CI 加一步发布即可，扩展本体无需改动。

**用 VSCodium / Cursor 等其他编辑器？** 扩展走 VSCode 扩展 API；其他编辑器可手动把 LuaLS 的 `workspace.library` 指向 `~/.unciv/lua-api`（先在 VSCode 里跑一次扩展，或手动同步文件）。

**定义文件从哪来？** 由游戏代码生成（`LuaApiDocs` / `LuaMapGenApiDocs` 数据表，见[代码规范](../UncivCN/Coding-standards.md)）——与[Lua API 参考](Lua-API-Reference.md)同源，网站上看到的文档和编辑器里的定义永远不会不一致。

## 相关页面

- [Lua 脚本](Lua-Modding.md) —— 教程、触发方式与生命周期钩子
- [Lua API 参考](Lua-API-Reference.md) —— 上下文表的全部函数与属性
- [Lua 地图脚本 API 参考](Lua-Map-API-Reference.md) —— 地图脚本专用的生成期 API

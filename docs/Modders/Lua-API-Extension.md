# Unciv Lua API Extension (VSCode)

**One-time install, forever-fresh autocompletion.** The `unciv-lua-api` VSCode extension is a thin cloud puller: every time you start VSCode (or open a `.lua` file) it fetches the latest `lua-api.lua` / `lua-map-api.lua` from the UncivCN docs site and syncs them into `~/.unciv/lua-api/`. Combined with the LuaLS language server you get autocompletion, hover docs and typo checking for `ctx.…` that **never goes stale** — no manual downloads, no copying files, no checking for updates.

## Why you want this

The Lua API definitions (`lua-api.lua` / `lua-map-api.lua`, generated from the game code) change with every game release. Copying them into your mod by hand means:

- you forget to check for new releases → stale autocompletion, wrong hints;
- every mod needs its own copy that must be re-synced;
- new APIs (e.g. `civ.discoverTech()`) simply don't autocomplete until you remember to update.

The extension solves this by separating **data from logic**: the definitions live in the cloud (deployed automatically on every release) and the extension just pulls them. The extension itself almost never needs updating.

## Install

1. **Install the Lua language server**: in VSCode, install the **Lua** extension by sumneko (the LuaLS language server).
2. **Install the Unciv Lua API extension**: download `unciv-lua-api.vsix` from the latest [UncivCN release](https://github.com/AutumnPizazz/Unciv/releases) (it's attached to every release, ~6 KB), then in VSCode run **Extensions → ⋯ → Install from VSIX…** and pick the file.
3. **Configure once**: open the command palette (Ctrl+Shift+P) and run **Unciv: Configure Lua API autocompletion**. It adds `~/.unciv/lua-api` (absolute path) to your user-level `Lua.workspace.library` setting — this applies to **every mod workspace** on your machine, you never do this again.

Done. Open any mod folder and start writing: `ctx.` autocompletes, hovering `ctx.civ.addGold(` shows its signature, typos like `ctx.civ.addGoldd(...)` get red squiggles immediately. Map scripts (`GenerateMap(ctx)`) get the same treatment from `lua-map-api.lua` — both files are managed automatically.

## How it works

```
Game code → generates lua-api.lua / lua-map-api.lua (with a -- Unciv version: header)
    ↓ deployed automatically on every release
docs site: https://club.unciv.cn/Unciv/Modders/lua-api.lua
    ↓ pulled on every VSCode start / on opening a .lua file (fallback: GitHub raw)
extension → ~/.unciv/lua-api/
    ↓ one-click configure
LuaLS workspace.library (user-level setting) → autocompletion everywhere
```

- **Update check**: the generated files carry a `-- Unciv version: 4.21.6.6 (build 1250)` header, so old and new copies are instantly distinguishable; the extension only writes when the content actually changed.
- **Offline**: if the cloud is unreachable, the last synced copy is kept (with its version header) and your autocompletion keeps working; it catches up automatically next time.
- **Manual refresh**: command palette → **Unciv: Update Lua API definitions now**.

## Manual setup (alternative)

If you prefer not to use the extension, copy the definitions from the repo's `docs/Modders/` (or from [the docs site](https://club.unciv.cn/Unciv/Modders/lua-api.lua)) into your mod and create a `.luarc.json`:

```json
{
    "runtime.version": "Lua 5.2",
    "workspace.library": [
        ".lua-api"
    ],
    "diagnostics.globals": ["ctx"]
}
```

> The mod checker whitelists `.luarc.json` in the mod root - keep this exact file name (LuaLS only reads `.luarc.json` from the workspace root; renaming it breaks autocompletion and the checker reports it as misplaced).

Remember to re-copy the files after each game update — this is exactly the chore the extension eliminates.

## FAQ

**Does this require the game or the source code?** No. Only VSCode + the Lua language server + this extension.

**Why isn't it on the VSCode marketplace?** The extension is deliberately a thin puller — the definitions come from the cloud in real time, so it doesn't depend on marketplace auto-updates to stay fresh. If it ever needs a marketplace listing (Open VSX / VSCode Marketplace), publishing is a one-step CI addition; the extension itself doesn't change.

**I use VSCodium / Cursor / another LuaLS-capable editor?** The extension targets VSCode's extension API; on other editors you can still get the same result by pointing LuaLS's `workspace.library` at `~/.unciv/lua-api` (after running the extension once in VSCode, or syncing the files by hand).

**Where do the definitions come from?** They are generated from the game code (`LuaApiDocs` / `LuaMapGenApiDocs` tables, see [Coding Standards](../UncivCN/Coding-standards.md)) — the same source as [Lua API Reference](Lua-API-Reference.md), so the docs you read online and the definitions in your editor can never disagree.

## See also

- [Lua Mod Scripts](Lua-Modding.md) — tutorials, triggers and lifecycle hooks
- [Lua API Reference](Lua-API-Reference.md) — every function and property of the context tables
- [Lua Map-Script API Reference](Lua-Map-API-Reference.md) — the generation-only API for map scripts

# LuaStarterMod — 模组 Lua 起步模板

一个完整可用的 Lua 模组模板：复制目录、改名字、就能跑。包含每回合触发的入口函数、参数传递示例、API 冒烟自检函数。

## 目录结构

```
LuaStarterMod/
├── ModOptions.json          ← 模组元信息（名称/作者/版本）
├── jsons/
│   └── GlobalUniques.json   ← 用 TriggerLuaFunction 声明触发时机
└── scripts/
    ├── main.lua             ← onTurnStart / onTurnEnd 入口函数
    └── api_test.lua         ← selfCheck 冒烟测试（可选）
```

## 快速开始

1. 把 `LuaStarterMod` 目录复制到游戏的 `mods/` 文件夹（桌面版位于游戏根目录，Android 在 `Android/data/com.unciv.app/files/mods/`），改成你自己的模组名
2. 编辑 `ModOptions.json`：改 `name`、`author`；`modVersion` 用 `n.n.n` 格式
3. 编辑 `jsons/GlobalUniques.json`：把 `LuaStarterMod` 前缀全部换成你的模组名
4. 新建游戏 → 勾选你的模组 → 开始游戏
5. 每回合开始会收到 "+1 Gold" 通知——模组已生效

## 测试你的脚本

- **冒烟测试**：取消 `GlobalUniques.json` 中 `selfCheck` 触发行的注释（`<before turn number [1]>` 表示第 1 回合前触发），开局第一回合后查看日志（开发者控制台或日志文件）中的 `test API:` 输出
- **mod-ci 检查**：在模组目录运行 `java -jar Unciv.jar mod-ci`（或 `Unciv mod-ci`），自动检查 JSON 校验 + Lua 语法 + 函数引用 + API 拼写
- **编辑器补全**：参见 Lua-Modding.md 的「编辑器设置」——把 `lua-api.lua` 类型定义配进 LuaLS 后，写 `ctx.` 就有自动补全

## 触发时机速查（GlobalUniques.json）

| 触发条件 | 示例 |
|---|---|
| 每回合开始/结束 | `<upon turn start>` / `<upon turn end>` |
| 研究某科技 | `<upon discovering [Writing] technology>` |
| 建城/攻城 | `<upon founding a city>` / `<upon conquering a city>` |
| 第 N 回合前 | `<before turn number [10]>` |

完整语法见文档 Lua-Modding.md。

---

# LuaStarterMod — Lua mod starter template

A complete, working Lua mod template: copy the folder, rename it, and it runs. Includes per-turn entry functions, a parameter-passing example and an API smoke-test function.

## Quick start

1. Copy the `LuaStarterMod` folder into the game's `mods/` folder, rename it to your mod
2. Edit `ModOptions.json`: set `name` / `author`; `modVersion` uses `n.n.n`
3. Edit `jsons/GlobalUniques.json`: replace every `LuaStarterMod` prefix with your mod name
4. New game → enable your mod → start
5. You'll get a "+1 Gold" notification every turn start — the mod is live

## Testing

- **Smoke test**: uncomment the `selfCheck` trigger in `GlobalUniques.json` (`<before turn number [1]>` fires before turn 1); check the `test API:` lines in the log
- **mod-ci**: run `java -jar Unciv.jar mod-ci` (or `Unciv mod-ci`) in the mod folder — checks JSON validation + Lua syntax + function references + API spelling
- **Editor support**: see "Editor setup" in Lua-Modding.md — point LuaLS at `lua-api.lua` for autocompletion

## Trigger quick reference (GlobalUniques.json)

| Trigger | Example |
|---|---|
| Turn start / end | `<upon turn start>` / `<upon turn end>` |
| Tech discovered | `<upon discovering [Writing] technology>` |
| City founded / conquered | `<upon founding a city>` / `<upon conquering a city>` |
| Before turn N | `<before turn number [10]>` |

Full syntax: see Lua-Modding.md.

---
title: 类型检查
---

# 类型检查

错误难免发生。字段命名错误、忘记添加的内容，甚至不知道存在的东西。计算机可以自动处理很多这些问题，因此我们可以让它们工作以确保我们的 json 文件正确，方法是使用 *JSON Schemas*。

JSON Schema 文件还直接在你的编辑器中启用自动补全、警告和一些智能感知功能。

## Android Studio

1. 双击空格，搜索 "json schema mappings"，进入
2. 点击小 '+'（顶部，'language & frameworks' 文本下）
3. 将 URL 设置为 `https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Buildings.schema.json`
4. 点击 'Schema version' 文本下的 '+'，添加 'File pattern'，将模式设置为 `*/Buildings.json`

Android Studio 现在将识别所有 Buildings.json 文件属于该 schema，并会警告不一致之处。

## VSCode 或类似编辑器

在 VSCode 中启用 JSON schemas 有两种方式：通过安装 [VSCode Unciv 扩展](https://github.com/robloach/vscode-unciv) 或手动添加。使用扩展更容易。

### 扩展

[VSCode Unciv 扩展](https://github.com/robloach/vscode-unciv) 启用 JSON Schemas，设置正确的文件关联，以及一些其他功能。

1. 打开 VSCode
2. 按 *CTRL+P* 打开命令面板
3. 编写以下内容，然后按 enter：
   ```
   ext install robloach.unciv
   ```
4. 恭喜，你现在安装了 Unciv JSON Schema 文件，具有自动补全、智能感知和文件检查功能。

### 手动

如果你不想使用 VSCode 扩展，也可以手动启用 schemas...

1. 按 *CTRL+SHIFT+P* 打开命令面板
2. 搜索 "Open User Settings (JSON)"，进入
3. 将以下内容复制到你的设置中：
```json
"json.schemas": [
    {
        "fileMatch": ["jsons/Beliefs.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Beliefs.schema.json"
    },
    {
        "fileMatch": ["jsons/Buildings.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Buildings.schema.json"
    },
    {
        "fileMatch": ["jsons/CityStateTypes.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/CityStateTypes.schema.json"
    },
    {
        "fileMatch": ["jsons/Difficulties.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Difficulties.schema.json"
    },
    {
        "fileMatch": ["jsons/Eras.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Eras.schema.json"
    },
    {
        "fileMatch": ["jsons/Units.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Units.schema.json"
    },
    {
        "fileMatch": ["jsons/Nations.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Nations.schema.json"
    },
    {
        "fileMatch": ["jsons/Personalities.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Personalities.schema.json"
    },
    {
        "fileMatch": ["jsons/Policies.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Policies.schema.json"
    },
    {
        "fileMatch": ["jsons/Quests.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Quests.schema.json"
    },
    {
        "fileMatch": ["jsons/Religions.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Religions.schema.json"
    },
    {
        "fileMatch": ["jsons/Ruins.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Ruins.schema.json"
    },
    {
        "fileMatch": ["jsons/Specialists.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Specialists.schema.json"
    },
    {
        "fileMatch": ["jsons/Speeds.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Speeds.schema.json"
    },
    {
        "fileMatch": ["jsons/TileImprovements.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/TileImprovements.schema.json"
    },
    {
        "fileMatch": ["jsons/TileSets/*.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/TileSetConfig.schema.json"
    },
    {
        "fileMatch": ["jsons/Techs.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Techs.schema.json"
    },
    {
        "fileMatch": ["jsons/UnitTypes.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Mododers/schemas/UnitTypes.schema.json"
    },
    {
        "fileMatch": ["jsons/UnitPromotions.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/UnitPromotions.schema.json"
    },
    {
        "fileMatch": ["jsons/TileResources.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/TileResources.schema.json"
    },
    {
        "fileMatch": ["jsons/Events.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Events.schema.json"
    },
    {
        "fileMatch": ["jsons/GlobalUniques.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/GlobalUniques.schema.json"
    },
    {
        "fileMatch": ["jsons/ModOptions.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/ModOptions.schema.json"
    },
    {
        "fileMatch": ["jsons/Terrains.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Terrains.schema.json"
    },
    {
        "fileMatch": ["jsons/Tutorials.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Tutorials.schema.json"
    },
    {
        "fileMatch": ["jsons/UnitNameGroups.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/UnitNameGroups.schema.json"
    },
    {
        "fileMatch": ["jsons/VictoryTypes.json"],
        "url": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/VictoryTypes.schema.json"
    }
]
```

## 在线工具

如果你不使用这些编辑器中的任何一个，你可以使用[此工具](https://www.jsonschemavalidator.net)在线检查你的文件。但是，它无法处理 VSCode 和 Android Studio 处理的缺失逗号，因此你可能需要让你的 json 符合规范才能使用它。

你要验证的 schema 是：
```json
{
	"$ref": "https://raw.githubusercontent.com/yairm210/Unciv/master/docs/Modders/schemas/Buildings.schema.json"
}
```
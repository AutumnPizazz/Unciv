# Unciv 模组 JSON MergeAction 教程

## 为什么需要 MergeAction？

旧版本中，如果你想给 Shrine 加一行 unique，必须把整个建筑完整重写：

```json
// 只想给 Shrine 加一行，却被迫抄写全部字段
{
    "name": "Shrine",
    "faith": 1,
    "cost": 40,
    "maintenance": 1,
    "requiredTech": "Pottery",
    "uniques": [
        "Only available <when religion is enabled>",
        "[+1 Happiness]"   // ← 唯一想加的一行
    ]
}
```

**问题**：原版更新 Shrine 后，你的 mod 会覆盖掉原版新值。多个 mod 无法叠加修改同一个对象。遗漏字段会意外删除属性。

MergeAction 让你**只写要改的字段**。

---

## 快速入门：TRY_INJECT

最常见的需求——给原版对象加个 unique 或改个数值：

```json
{
    "name": "Shrine",
    "_mergeAction": { "action": "TRY_INJECT" },
    "faith": 2,
    "uniques": ["[+1 Happiness]"]
}
```

效果：
- `faith` 改为 2（原版 1）
- `uniques` 追加 `"[+1 Happiness]"`（原版 uniques 保留）
- 未写的字段（`cost`、`maintenance`、`requiredTech`）保持原版不变

**规则**：普通字段（数字、文字）非默认值则覆盖，数组字段（uniques、promotions 等）追加到末尾。

---

## 五种操作类型

### 1. 不写 action（默认）—— 完整覆盖

保持旧版行为，适合全新内容或刻意完全替换：

```json
{
    "name": "MyNewBuilding",
    "cost": 100,
    "uniques": ["[+5 Science]"]
}
```

### 2. TRY_INJECT —— 字段级合并

最常用。只写要改的字段，目标不存在时静默跳过（不会意外创建对象）：

```json
{
    "name": "Warrior",
    "_mergeAction": { "action": "TRY_INJECT" },
    "strength": 10,
    "promotions": ["Shock I"]
}
```

### 3. CREATE_OR_REPLACE —— 无论如何以我为准

不管目标是否存在，最终结果以此定义为准：

```json
{
    "name": "Grand Temple",
    "_mergeAction": { "action": "CREATE_OR_REPLACE" },
    "faith": 8,
    "culture": 3,
    "isNationalWonder": true,
    "cost": 120
}
```

### 4. REMOVE —— 删除对象

替代旧版 `ModOptions` 中的 `unitsToRemove` 等。操作和数据声明在同一文件内，更直观：

```json
{ "name": "Scout", "_mergeAction": { "action": "REMOVE" } }
```

`ModOptions.*ToRemove` 仍然有效，两者可同时使用。

### 5. REMOVE_FIELD —— 移除特定字段

重置某个属性，或从数组中删除特定元素：

```json
{
    "name": "Swordsman",
    "_mergeAction": { "action": "REMOVE_FIELD" },
    "requiredResource": null,
    "promotions": ["Shock I"]
}
```

效果：Swordsman 不再需要铁资源，Shock I 晋升被移除。

**通配符**：星号 `*` 可出现在数组元素末尾表示前缀匹配：

```json
"promotions": ["Shock*"]
// 删除所有以 Shock 开头的晋升：Shock I、Shock II、Shock III
```

---

## 条件分支

### 对象级条件

条件不满足时，该对象被跳过（静默忽略）：

```json
{
    "name": "Bazooka",
    "_mergeAction": {
        "action": "TRY_INJECT",
        "if": { "object_exists": "Unit:Bazooka" }
    },
    "cost": 300
}
// 只有 Bazooka 存在时，才调整其 cost
```

带条件的 REMOVE：

```json
{
    "name": "Warrior",
    "_mergeAction": {
        "action": "REMOVE",
        "if": { "mod_loaded": "ModX" }
    }
}
// 只有 ModX 激活时，才删除 Warrior
```

### 控制块（then / else）

当你需要一组操作统一走不同分支时使用。控制块没有游戏数据字段，只包含 `_mergeAction`：

```json
{
    "_mergeAction": {
        "if": { "base_ruleset": "Civ V - Gods & Kings" },
        "then": [
            { "name": "Spearman", "_mergeAction": { "action": "TRY_INJECT" }, "strength": 12 },
            { "name": "Pikeman",  "_mergeAction": { "action": "TRY_INJECT" }, "strength": 17 }
        ],
        "else": [
            { "name": "Spearman", "_mergeAction": { "action": "TRY_INJECT" }, "strength": 11 }
        ]
    }
}
```

**注意**：如果你的 JSON 文件只包含控制块（不含普通对象），需要在控制块上添加一个占位 `name`，否则模组管理器会拒绝加载。这个 `name` 仅用于通过验证，不会产生实际游戏对象：

```json
{
    "name": "我的调整",
    "_mergeAction": {
        "if": { "mod_loaded": "SomeMod" },
        "then": [ /* ... */ ]
    }
}
```

`else` 是可选的——很多场景只需要"条件成立才执行"：

```json
{
    "name": "G&K调整",
    "_mergeAction": {
        "if": { "base_ruleset": "Civ V - Gods & Kings" },
        "then": [
            { "name": "Shrine", "_mergeAction": { "action": "TRY_INJECT" }, "faith": 3 }
        ]
    }
}
```

---

## 嵌套条件

控制块的 `then`/`else` 内可以再放控制块，实现多级分支：

```json
{
    "name": "复杂调整",
    "_mergeAction": {
        "if": { "mod_loaded": "BigMod" },
        "then": [
            { "name": "Warrior", "strength": 15 },

            {
                "name": "子条件",
                "_mergeAction": {
                    "if": { "base_ruleset": "Civ V - Gods & Kings" },
                    "then": [
                        { "name": "Shrine", "_mergeAction": { "action": "TRY_INJECT" }, "faith": 4 }
                    ],
                    "else": [
                        { "name": "Shrine", "_mergeAction": { "action": "TRY_INJECT" }, "faith": 3 }
                    ]
                }
            }
        ],
        "else": [
            { "name": "Warrior", "strength": 10 }
        ]
    }
}
```

等效逻辑：
```
如果 BigMod 激活：
    Warrior.strength = 15
    如果 基础规则集是 G&K：
        Shrine.faith += 4
    否则：
        Shrine.faith += 3
否则：
    Warrior.strength = 10
```

---

## 完整条件参考

### 环境条件

| 条件 | JSON 写法 | 说明 |
|------|-----------|------|
| 模组已加载 | `{ "mod_loaded": "模组名称" }` | 指定模组是否在激活列表中 |
| 同名作者 | `{ "mod_author": "作者名" }` | 是否有该作者的模组激活 |
| 基础规则集 | `{ "base_ruleset": "Civ V - Gods & Kings" }` | 首个 isBaseRuleset 模组的名称是否匹配 |
| 游戏版本 | `{ "game_version": ">=4.20.0" }` | 当前 Unciv 版本比较。运算符：`>=` `>` `<=` `<` `==` |

### 对象条件

| 条件 | JSON 写法 | 说明 |
|------|-----------|------|
| 对象存在 | `{ "object_exists": "Unit:Warrior" }` | "类型:名称" 格式 |
| 对象计数 | `{ "object_count": { "type": "Building", "greater_than_or_equal": 200 } }` | 某类型对象总数比较 |
| 对象含 unique | `{ "object_has_unique": "Unit:Swordsman:Shock I" }` | "类型:名称:Unique文本" |
| 全局含 unique | `{ "any_object_has_unique": "[+1 Happiness]" }` | 扫描整个规则集的 uniques |
| 字段已设置 | `{ "object_has_field": { "object": "Unit:Swordsman", "field": "replaces" } }` | 字段是否为非默认值 |
| 字段精确匹配 | `{ "object_field_equals": { "object": "Unit:Warrior", "field": "strength", "equals": 8 } }` | 字段值完全相等 |
| 字段包含匹配 | `{ "object_field_contains": { "object": "Unit:Warrior", "field": "promotions", "value": "Shock I" } }` | 数组/字符串包含 |
| 字段数值比较 | `{ "object_field_compare": { "object": "Unit:Warrior", "field": "strength", "greater_than_or_equal": 10 } }` | 运算符：`greater_than` `greater_than_or_equal` `less_than` `less_than_or_equal` `equal` |

### 组合器

| 条件 | JSON 写法 | 说明 |
|------|-----------|------|
| 取反 | `{ "not": { ... } }` | 子条件取反 |
| 与 | `{ "and": [{ ... }, { ... }] }` | 所有子条件为 true |
| 或 | `{ "or": [{ ... }, { ... }] }` | 任一子条件为 true |

复合条件示例：

```json
"if": {
    "and": [
        { "mod_loaded": "ModA" },
        { "mod_loaded": "ModB" },
        { "not": { "mod_loaded": "ModC" } }
    ]
}
// ModA 和 ModB 都激活，且 ModC 未激活
```

---

## 同名对象多指令

同一个名字可以出现在多条指令中，按 JSON 数组顺序依次执行：

```json
[
    { "name": "Warrior", "_mergeAction": { "action": "TRY_INJECT" }, "promotions": ["Shock I"] },
    { "name": "Warrior", "_mergeAction": { "action": "TRY_INJECT" }, "strength": 10 }
]
// 结果：promotions 追加 Shock I，strength 改为 10
```

先删再建：

```json
[
    { "name": "Warrior", "_mergeAction": { "action": "REMOVE" } },
    { "name": "Warrior", "strength": 12, "cost": 30 }
]
// 删掉原版 Warrior，新建一个 strength=12, cost=30 的 Warrior
```

---

## 实用示例

### 示例 1：新建单位 + 调整原版 + 条件删除

```json
[
    { "name": "Elite Guard", "unitType": "Sword", "movement": 2, "strength": 18, "cost": 100 },

    { "name": "Scout", "_mergeAction": { "action": "REMOVE" } },

    {
        "name": "Warrior",
        "_mergeAction": { "action": "TRY_INJECT" },
        "strength": 10,
        "promotions": ["Shock I"]
    },

    {
        "name": "Swordsman",
        "_mergeAction": { "action": "REMOVE_FIELD" },
        "requiredResource": null
    },

    {
        "name": "Bazooka",
        "_mergeAction": {
            "action": "TRY_INJECT",
            "if": { "object_exists": "Unit:Bazooka" }
        },
        "cost": 300
    }
]
```

### 示例 2：根据基础规则集选择不同方案

```json
{
    "name": "G&K调整",
    "_mergeAction": {
        "if": { "base_ruleset": "Civ V - Gods & Kings" },
        "then": [
            { "name": "Spearman", "_mergeAction": { "action": "TRY_INJECT" }, "strength": 12 },
            { "name": "Pikeman",  "_mergeAction": { "action": "TRY_INJECT" }, "strength": 17 }
        ],
        "else": [
            { "name": "Spearman", "_mergeAction": { "action": "TRY_INJECT" }, "strength": 11 }
        ]
    }
}
```

### 示例 3：给 Palace 加效果（不影响原有"标识首都"功能）

```json
{
    "name": "Palace",
    "_mergeAction": { "action": "TRY_INJECT" },
    "uniques": ["[+1 Happiness]"],
    "faith": 2
}
```

---

## 与旧版写法的对比

| 场景 | 旧版（完整覆盖） | 新版（MergeAction） |
|------|------------------|---------------------|
| 给 Shrine 加一行 unique | 重写整个建筑（~8 行） | 只写 `uniques`（~3 行） |
| 多模组叠加修改同一对象 | 不可能（后者覆盖前者） | TRY_INJECT 追加 |
| 删除对象 | 在 ModOptions 中列名 | 在对应 JSON 文件中用 REMOVE |
| 条件性修改 | 无法实现 | `if` 条件 + 控制块 |
| 原版更新后 | 模组覆盖新值，造成 bug | 字段级合并，原版新字段保留 |
| 依赖特定模组激活 | 无法实现 | `mod_loaded` 条件 |

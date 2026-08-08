# Merge Actions (`_mergeAction`)

**Merge Actions** let you modify existing ruleset objects without replacing them entirely.
Instead of copying every field from the original to change just one value, you write only the fields you want to change.

They are specified via a `"_mergeAction"` field on any ruleset object (Buildings, Units, Nations, etc.).

## Why this exists

Before Merge Actions, mods used *full-object replacement*:

```json
// To add one unique to Shrine, you had to copy EVERY field:
{
    "name": "Shrine",
    "faith": 1, "cost": 40, "maintenance": 1,
    "requiredTech": "Pottery",
    "uniques": [
        "Only available <when religion is enabled>",
        "[+1 Happiness]"   // ← the only new line
    ]
}
```

Problems: if the base ruleset updates Shrine, your mod overwrites the new values.
Multiple mods can't stack changes on the same object. Missing fields accidentally delete properties.

With Merge Actions, you write only the diff:

```json
{
    "name": "Shrine",
    "_mergeAction": { "action": "TRY_INJECT" },
    "faith": 2,
    "uniques": ["[+1 Happiness]"]
}
```

## Operation Types

The `action` field determines how the object is merged. If omitted, the object replaces the target entirely (backward-compatible behaviour).

### TRY_INJECT — field-level merge

The most common action. Only write the fields you want to change.
If the target object doesn't exist, the operation is silently skipped.

| Field type | Behaviour | Example |
|------------|-----------|---------|
| Scalar (String, Int, Float, Boolean) | Overwrites if the new value is non-default | `"strength": 10` overwrites, unwritten fields keep original values |
| Collection (uniques, promotions, etc.) | Appends to the end (duplicates are skipped) | `"[+1 Happiness]"` is added to existing uniques if not already present |
| Nested objects | Shallow-replaces the entire field | The whole sub-object is replaced |

```json
{
    "name": "Warrior",
    "_mergeAction": { "action": "TRY_INJECT" },
    "strength": 10,
    "promotions": ["Shock I"]
}
// Result: strength becomes 10, Shock I is added to promotions,
// all other fields remain as in the original Warrior.
```

**Multiple TRY_INJECT instructions** on the same object are applied in JSON array order:

```json
[
    { "name": "Warrior", "_mergeAction": { "action": "TRY_INJECT" }, "promotions": ["Shock I"] },
    { "name": "Warrior", "_mergeAction": { "action": "TRY_INJECT" }, "strength": 10 }
]
// Result: promotions gets Shock I, then strength becomes 10.
```

### CREATE_OR_REPLACE — always use this definition

Whether the target exists or not, the final result is exactly this object.

```json
{
    "name": "Grand Temple",
    "_mergeAction": { "action": "CREATE_OR_REPLACE" },
    "faith": 8, "culture": 3,
    "isNationalWonder": true, "cost": 120
}
```

### REMOVE — delete an object

Semantically replaces the `*ToRemove` lists in [ModOptions.json](5-Miscellaneous-JSON-files.md#modoptionsjson).
The operation is declared in the same file as the data, keeping related concerns together.

```json
{ "name": "Scout", "_mergeAction": { "action": "REMOVE" } }
```

`ModOptions.*ToRemove` is still supported and can be used alongside REMOVE.

### REMOVE_FIELD — reset fields or remove array elements

Resets a scalar field to its default value, or removes matching elements from a collection field.

```json
{
    "name": "Swordsman",
    "_mergeAction": { "action": "REMOVE_FIELD" },
    "requiredResource": null,
    "promotions": ["Shock I"]
}
// Result: Swordsman no longer needs a resource, Shock I is removed from promotions.
```

For collection fields, a `*` wildcard at the end of a string performs prefix matching:

```json
"promotions": ["Shock*"]
// Removes all promotions starting with "Shock": Shock I, Shock II, Shock III
```

Note: a `*` only appearing at the end of a string element (and not elsewhere) is interpreted as a prefix wildcard.

## Conditions

Each merge operation can be guarded by an `"if"` condition. When the condition evaluates to false, the object is silently skipped.

### Object-level conditions

```json
{
    "name": "Bazooka",
    "_mergeAction": {
        "action": "TRY_INJECT",
        "if": { "object_exists": "Unit:Bazooka" }
    },
    "cost": 300
}
// Only adjusts Bazooka's cost if it exists in the ruleset.
```

```json
{
    "name": "Warrior",
    "_mergeAction": {
        "action": "REMOVE",
        "if": { "mod_loaded": "ModX" }
    }
}
// Only removes Warrior when ModX is active.
```

### Control blocks (then / else)

When you need a group of operations to follow the same branch, use a control block.
A control block is a JSON object containing only `_mergeAction` with `"then"` and optionally `"else"` arrays.

```json
{
    "name": "G&K Adjustments",
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

::: note
    If a JSON file contains **only** control blocks (no regular objects), the outermost control block must include a `"name"` field as a placeholder, otherwise the mod manager will reject the file. This name is purely for validation and does not create a game object.
:::

The `"else"` branch is optional — many scenarios only need conditional execution:

```json
{
    "name": "G&K Only",
    "_mergeAction": {
        "if": { "base_ruleset": "Civ V - Gods & Kings" },
        "then": [
            { "name": "Shrine", "_mergeAction": { "action": "TRY_INJECT" }, "faith": 3 }
        ]
    }
}
// When not on G&K, nothing happens.
```

### Nested conditions

Control blocks can be nested inside `then`/`else` branches for multi-level branching:

```json
{
    "name": "Complex Adjustments",
    "_mergeAction": {
        "if": { "mod_loaded": "BigMod" },
        "then": [
            { "name": "Warrior", "strength": 15 },
            {
                "name": "Nested",
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

Equivalent logic: if BigMod is active, set Warrior.strength to 15 and then (if base ruleset is G&K, add 4 faith to Shrine; otherwise add 3); if BigMod is not active, set Warrior.strength to 10.

## Condition Reference

### Environment conditions

| Condition | JSON | Evaluates to true when |
|-----------|------|------------------------|
| Mod loaded | `{ "mod_loaded": "ModName" }` | The named mod is in the active mod list |
| Mod author | `{ "mod_author": "AuthorName" }` | Any mod by that author is active |
| Base ruleset | `{ "base_ruleset": "Civ V - Gods & Kings" }` | The first `isBaseRuleset` mod matches this name |
| Game version | `{ "game_version": ">=4.20.0" }` | Current Unciv version satisfies the comparison. Operators: `>=`, `>`, `<=`, `<`, `==` (no operator means `==`) |

### Object conditions

All object conditions use the format `"Type:Name"` (e.g., `"Unit:Warrior"`, `"Building:Palace"`).

| Condition | JSON | Evaluates to true when |
|-----------|------|------------------------|
| Object exists | `{ "object_exists": "Unit:Bazooka" }` | The named object is present in the ruleset |
| Object count | `{ "object_count": { "type": "Building", "greater_than_or_equal": 200 } }` | Count of objects of the given type satisfies the comparison |
| Object has unique | `{ "object_has_unique": "Unit:Swordsman:Shock I" }` | The named object's uniques list contains the exact text |
| Any object has unique | `{ "any_object_has_unique": "[+1 Happiness]" }` | Any object in the entire ruleset has this unique text |
| Field is set | `{ "object_has_field": { "object": "Unit:Swordsman", "field": "replaces" } }` | The named field has a non-default value |
| Field equals | `{ "object_field_equals": { "object": "Unit:Warrior", "field": "strength", "equals": 8 } }` | The field's value exactly matches |
| Field contains | `{ "object_field_contains": { "object": "Unit:Warrior", "field": "promotions", "value": "Shock I" } }` | The collection/string field contains the element |
| Field compare | `{ "object_field_compare": { "object": "Unit:Warrior", "field": "strength", "greater_than_or_equal": 10 } }` | The numeric field satisfies the comparison |

Count and field comparison operators: `greater_than`, `greater_than_or_equal`, `less_than`, `less_than_or_equal`, `equal` (each optional).

### Combinators

| Combinator | JSON | Effect |
|------------|------|--------|
| NOT | `{ "not": { ... } }` | Inverts the sub-condition |
| AND | `{ "and": [{ ... }, { ... }] }` | All sub-conditions must be true |
| OR | `{ "or": [{ ... }, { ... }] }` | At least one sub-condition must be true |

Example:

```json
"if": {
    "and": [
        { "mod_loaded": "ModA" },
        { "mod_loaded": "ModB" },
        { "not": { "mod_loaded": "ModC" } }
    ]
}
// ModA AND ModB active, AND ModC NOT active.
```

## Complete example

A balance mod's `Units.json` demonstrating several features together:

```json
[
    { "name": "Elite Guard", "unitType": "Sword", "movement": 2, "strength": 18, "cost": 100 },

    { "name": "Scout", "_mergeAction": { "action": "REMOVE" } },

    {
        "name": "Warrior",
        "_mergeAction": { "action": "TRY_INJECT" },
        "strength": 10,
        "uniques": ["[+5]% Strength when adjacent to a friendly unit"]
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
    },

    {
        "name": "RuleSet Branch",
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
]
```


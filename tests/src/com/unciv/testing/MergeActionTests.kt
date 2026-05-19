package com.unciv.testing

import com.badlogic.gdx.utils.JsonReader
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.ruleset.Building
import com.unciv.models.ruleset.MergeAction
import com.unciv.models.ruleset.MergeContext
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.models.ruleset.unit.BaseUnit
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GdxTestRunner::class)
class MergeActionTests {

    private lateinit var context: MergeContext

    @Before
    fun setup() {
        context = MergeContext(
            loadedMods = setOf("testMod", "modA"),
            baseRuleset = "Civ V - Vanilla",
            currentRuleset = Ruleset().apply {
                mods.add("Civ V - Vanilla")
            }
        )
    }

    // ===== resolveConditionals tests =====

    @Test
    fun `resolveConditionals expands then branch`() {
        val json = JsonReader().parse("""
        [
            {
                "_mergeAction": {
                    "if": { "mod_loaded": "testMod" },
                    "then": [
                        { "name": "Warrior", "strength": 15 },
                        { "name": "Archer", "range": 3 }
                    ],
                    "else": [
                        { "name": "Warrior", "strength": 10 }
                    ]
                }
            }
        ]
        """.trimIndent())

        val ruleset = Ruleset()
        val result = ruleset.resolveConditionals(json, context)

        Assert.assertEquals("Should have 2 objects from then branch", 2, result.size)
        val names = result.map { it.getString("name") }
        Assert.assertTrue("Should contain Warrior", names.contains("Warrior"))
        Assert.assertTrue("Should contain Archer", names.contains("Archer"))
    }

    @Test
    fun `resolveConditionals expands else branch`() {
        val json = JsonReader().parse("""
        [
            {
                "_mergeAction": {
                    "if": { "mod_loaded": "nonExistentMod" },
                    "then": [
                        { "name": "Warrior", "strength": 15 },
                        { "name": "Archer", "range": 3 }
                    ],
                    "else": [
                        { "name": "Warrior", "strength": 10 }
                    ]
                }
            }
        ]
        """.trimIndent())

        val ruleset = Ruleset()
        val result = ruleset.resolveConditionals(json, context)

        Assert.assertEquals("Should have 1 object from else branch", 1, result.size)
        Assert.assertEquals("Should be Warrior", "Warrior", result[0].getString("name"))
        Assert.assertEquals("Strength should be 10", 10.0, result[0].getDouble("strength"), 0.01)
    }

    @Test
    fun `resolveConditionals skips object with false condition`() {
        val json = JsonReader().parse("""
        [
            { "name": "AlwaysHere", "cost": 100 },
            {
                "name": "ConditionalBuilding",
                "_mergeAction": {
                    "action": "TRY_INJECT",
                    "if": { "mod_loaded": "nonExistentMod" }
                },
                "cost": 200
            },
            { "name": "AlsoHere", "cost": 300 }
        ]
        """.trimIndent())

        val ruleset = Ruleset()
        val result = ruleset.resolveConditionals(json, context)

        Assert.assertEquals("Should have 2 objects (conditional one skipped)", 2, result.size)
        val names = result.map { it.getString("name") }
        Assert.assertTrue("Should contain AlwaysHere", names.contains("AlwaysHere"))
        Assert.assertTrue("Should contain AlsoHere", names.contains("AlsoHere"))
        Assert.assertFalse("Should NOT contain ConditionalBuilding", names.contains("ConditionalBuilding"))
    }

    @Test
    fun `resolveConditionals handles nested control blocks`() {
        val json = JsonReader().parse("""
        [
            {
                "_mergeAction": {
                    "if": { "mod_loaded": "testMod" },
                    "then": [
                        { "name": "Warrior", "strength": 15 },
                        {
                            "_mergeAction": {
                                "if": { "base_ruleset": "Civ V - Vanilla" },
                                "then": [
                                    { "name": "Shrine", "faith": 4 }
                                ],
                                "else": [
                                    { "name": "Shrine", "faith": 3 }
                                ]
                            }
                        }
                    ]
                }
            }
        ]
        """.trimIndent())

        val ruleset = Ruleset()
        val result = ruleset.resolveConditionals(json, context)

        Assert.assertEquals("Should have 2 objects", 2, result.size)
        val names = result.map { it.getString("name") }
        Assert.assertTrue("Should contain Warrior", names.contains("Warrior"))
        Assert.assertTrue("Should contain Shrine", names.contains("Shrine"))

        val shrine = result.find { it.getString("name") == "Shrine" }!!
        Assert.assertEquals("Shrine faith should be 4 (vanilla branch)", 4.0, shrine.getDouble("faith"), 0.01)
    }

    // ===== evaluateCondition tests =====

    @Test
    fun `condition mod_loaded`() {
        val ruleset = Ruleset()
        val trueCond = JsonReader().parse("""{ "mod_loaded": "testMod" }""")
        val falseCond = JsonReader().parse("""{ "mod_loaded": "nonExistentMod" }""")

        Assert.assertTrue(ruleset.evaluateCondition(trueCond, context))
        Assert.assertFalse(ruleset.evaluateCondition(falseCond, context))
    }

    @Test
    fun `condition base_ruleset`() {
        val ruleset = Ruleset()
        val match = JsonReader().parse("""{ "base_ruleset": "Civ V - Vanilla" }""")
        val noMatch = JsonReader().parse("""{ "base_ruleset": "Civ V - Gods & Kings" }""")

        Assert.assertTrue(ruleset.evaluateCondition(match, context))
        Assert.assertFalse(ruleset.evaluateCondition(noMatch, context))
    }

    @Test
    fun `condition and combinator`() {
        val ruleset = Ruleset()
        val bothTrue = JsonReader().parse("""{ "and": [{ "mod_loaded": "testMod" }, { "base_ruleset": "Civ V - Vanilla" }] }""")
        val oneFalse = JsonReader().parse("""{ "and": [{ "mod_loaded": "testMod" }, { "mod_loaded": "nonExistentMod" }] }""")

        Assert.assertTrue(ruleset.evaluateCondition(bothTrue, context))
        Assert.assertFalse(ruleset.evaluateCondition(oneFalse, context))
    }

    @Test
    fun `condition or combinator`() {
        val ruleset = Ruleset()
        val oneTrue = JsonReader().parse("""{ "or": [{ "mod_loaded": "testMod" }, { "mod_loaded": "nonExistentMod" }] }""")
        val allFalse = JsonReader().parse("""{ "or": [{ "mod_loaded": "modX" }, { "mod_loaded": "modY" }] }""")

        Assert.assertTrue(ruleset.evaluateCondition(oneTrue, context))
        Assert.assertFalse(ruleset.evaluateCondition(allFalse, context))
    }

    @Test
    fun `condition not combinator`() {
        val ruleset = Ruleset()
        val notFalse = JsonReader().parse("""{ "not": { "mod_loaded": "nonExistentMod" } }""")
        val notTrue = JsonReader().parse("""{ "not": { "mod_loaded": "testMod" } }""")

        Assert.assertTrue(ruleset.evaluateCondition(notFalse, context))
        Assert.assertFalse(ruleset.evaluateCondition(notTrue, context))
    }

    @Test
    fun `condition object_exists`() {
        val ruleset = Ruleset().apply {
            units["Warrior"] = BaseUnit().apply { name = "Warrior" }
        }
        val ctx = context.copy(currentRuleset = ruleset)

        val exists = JsonReader().parse("""{ "object_exists": "Unit:Warrior" }""")
        val notExists = JsonReader().parse("""{ "object_exists": "Unit:Bazooka" }""")

        Assert.assertTrue(ruleset.evaluateCondition(exists, ctx))
        Assert.assertFalse(ruleset.evaluateCondition(notExists, ctx))
    }

    @Test
    fun `condition object_count`() {
        val ruleset = Ruleset().apply {
            buildings["B1"] = Building().apply { name = "B1" }
            buildings["B2"] = Building().apply { name = "B2" }
            buildings["B3"] = Building().apply { name = "B3" }
        }
        val ctx = context.copy(currentRuleset = ruleset)

        val trueCond = JsonReader().parse("""{ "object_count": { "type": "Building", "greater_than_or_equal": 3 } }""")
        val falseCond = JsonReader().parse("""{ "object_count": { "type": "Building", "greater_than": 5 } }""")

        Assert.assertTrue(ruleset.evaluateCondition(trueCond, ctx))
        Assert.assertFalse(ruleset.evaluateCondition(falseCond, ctx))
    }

    // ===== processObjects tests =====

    @Test
    fun `processObjects TRY_INJECT appends arrays and overwrites scalars`() {
        val target = linkedMapOf(
            "Warrior" to BaseUnit().apply {
                name = "Warrior"; strength = 8
                uniques = arrayListOf("[+10]% Strength in [Grassland]")
            }
        )
        val source = listOf(
            BaseUnit().apply {
                name = "Warrior"
                _mergeAction = MergeAction().apply { action = "TRY_INJECT" }
                strength = 10
                uniques = arrayListOf("[+1] Movement")
            }
        )

        val ruleset = Ruleset()
        ruleset.processObjects(target, source, ruleset)

        Assert.assertEquals("Strength should be overwritten", 10, target["Warrior"]!!.strength)
        Assert.assertEquals("Should have 2 uniques", 2, target["Warrior"]!!.uniques.size)
        Assert.assertTrue("Should contain original unique",
            target["Warrior"]!!.uniques.contains("[+10]% Strength in [Grassland]"))
        Assert.assertTrue("Should contain appended unique",
            target["Warrior"]!!.uniques.contains("[+1] Movement"))
    }

    @Test
    fun `processObjects TRY_INJECT skips missing target`() {
        val target = linkedMapOf<String, BaseUnit>()
        val source = listOf(
            BaseUnit().apply {
                name = "Warrior"
                _mergeAction = MergeAction().apply { action = "TRY_INJECT" }
                strength = 10
            }
        )

        val ruleset = Ruleset()
        ruleset.processObjects(target, source, ruleset)

        Assert.assertFalse("Warrior should NOT be created by TRY_INJECT", target.containsKey("Warrior"))
    }

    @Test
    fun `processObjects CREATE_OR_REPLACE always puts`() {
        val target = linkedMapOf(
            "Warrior" to BaseUnit().apply { name = "Warrior"; strength = 8 }
        )
        val source = listOf(
            BaseUnit().apply {
                name = "Warrior"
                _mergeAction = MergeAction().apply { action = "CREATE_OR_REPLACE" }
                strength = 15
            }
        )

        val ruleset = Ruleset()
        ruleset.processObjects(target, source, ruleset)

        Assert.assertEquals("Strength should be replaced", 15, target["Warrior"]!!.strength)
    }

    @Test
    fun `processObjects REMOVE deletes object`() {
        val target = linkedMapOf(
            "Warrior" to BaseUnit().apply { name = "Warrior"; strength = 8 }
        )
        val source = listOf(
            BaseUnit().apply {
                name = "Warrior"
                _mergeAction = MergeAction().apply { action = "REMOVE" }
            }
        )

        val ruleset = Ruleset()
        ruleset.processObjects(target, source, ruleset)

        Assert.assertFalse("Warrior should be removed", target.containsKey("Warrior"))
    }

    @Test
    fun `processObjects REMOVE_FIELD resets fields`() {
        val target = linkedMapOf(
            "Swordsman" to BaseUnit().apply {
                name = "Swordsman"; strength = 14
                requiredResource = "Iron"
                promotions = hashSetOf("Shock I", "Drill I")
            }
        )
        val source = listOf(
            BaseUnit().apply {
                name = "Swordsman"
                _mergeAction = MergeAction().apply { action = "REMOVE_FIELD" }
                requiredResource = "Iron"
                promotions = hashSetOf("Shock I")
            }
        )

        val ruleset = Ruleset()
        ruleset.processObjects(target, source, ruleset)

        Assert.assertEquals("requiredResource reset", "", target["Swordsman"]!!.requiredResource)
        Assert.assertEquals("1 promotion left", 1, target["Swordsman"]!!.promotions.size)
        Assert.assertEquals("Remaining: Drill I", "Drill I", target["Swordsman"]!!.promotions.first())
        Assert.assertEquals("Strength unchanged", 14, target["Swordsman"]!!.strength)
    }

    @Test
    fun `processObjects null action is backward compat`() {
        val target = linkedMapOf(
            "Warrior" to BaseUnit().apply { name = "Warrior"; strength = 8; cost = 40 }
        )
        val source = listOf(
            BaseUnit().apply { name = "Warrior"; strength = 10 }
        )

        val ruleset = Ruleset()
        ruleset.processObjects(target, source, ruleset)

        Assert.assertEquals("Strength updated", 10, target["Warrior"]!!.strength)
        Assert.assertEquals("Cost set to default -1 (entire object replaced)", -1, target["Warrior"]!!.cost)
    }

    @Test
    fun `processObjects clears mergeAction after processing`() {
        val target = linkedMapOf<String, BaseUnit>()
        val source = listOf(
            BaseUnit().apply {
                name = "NewUnit"
                _mergeAction = MergeAction().apply { action = "CREATE_OR_REPLACE" }
                strength = 12
            }
        )

        val ruleset = Ruleset()
        ruleset.processObjects(target, source, ruleset)

        Assert.assertNull("_mergeAction should be cleared", target["NewUnit"]!!._mergeAction)
    }

    @Test
    fun `sequential same-name TRY_INJECT in order`() {
        val target = linkedMapOf(
            "Warrior" to BaseUnit().apply {
                name = "Warrior"; strength = 8
                promotions = hashSetOf("Accuracy I")
            }
        )
        val source = listOf(
            BaseUnit().apply {
                name = "Warrior"
                _mergeAction = MergeAction().apply { action = "TRY_INJECT" }
                promotions = hashSetOf("Shock I")
            },
            BaseUnit().apply {
                name = "Warrior"
                _mergeAction = MergeAction().apply { action = "TRY_INJECT" }
                strength = 10
            },
            BaseUnit().apply {
                name = "Warrior"
                _mergeAction = MergeAction().apply { action = "TRY_INJECT" }
                promotions = hashSetOf("Drill I")
            }
        )

        val ruleset = Ruleset()
        ruleset.processObjects(target, source, ruleset)

        Assert.assertEquals("Strength is 10", 10, target["Warrior"]!!.strength)
        Assert.assertEquals("3 promotions", 3, target["Warrior"]!!.promotions.size)
        Assert.assertTrue("Accuracy I", target["Warrior"]!!.promotions.contains("Accuracy I"))
        Assert.assertTrue("Shock I", target["Warrior"]!!.promotions.contains("Shock I"))
        Assert.assertTrue("Drill I", target["Warrior"]!!.promotions.contains("Drill I"))
    }

    // ===== mergeFields test =====

    @Test
    fun `mergeFields appends uniques and overwrites scalars`() {
        val ruleset = Ruleset()
        val target = Building().apply {
            name = "Shrine"; faith = 1f; cost = 40; maintenance = 1
            requiredTech = "Pottery"
            uniques = arrayListOf("Only available <when religion is enabled>")
        }
        val source = Building().apply {
            name = "Shrine"; faith = 2f
            uniques = arrayListOf("[+1 Happiness]")
        }

        with(ruleset) { target.mergeFields(source) }

        Assert.assertEquals("faith overwritten", 2f, target.faith)
        Assert.assertEquals("cost preserved", 40, target.cost)
        Assert.assertEquals("maintenance preserved", 1, target.maintenance)
        Assert.assertEquals("requiredTech preserved", "Pottery", target.requiredTech)
        Assert.assertEquals("2 uniques", 2, target.uniques.size)
        Assert.assertTrue("original unique", target.uniques.contains("Only available <when religion is enabled>"))
        Assert.assertTrue("new unique", target.uniques.contains("[+1 Happiness]"))
    }

    // ===== removeFields wildcard test =====

    @Test
    fun `removeFields wildcard`() {
        val ruleset = Ruleset()
        val target = BaseUnit().apply {
            name = "Swordsman"
            promotions = hashSetOf("Shock I", "Shock II", "Shock III", "Drill I", "Cover I")
        }
        val source = BaseUnit().apply {
            name = "Swordsman"
            promotions = hashSetOf("Shock*")
        }

        with(ruleset) { target.removeFields(source) }

        Assert.assertEquals("2 promotions left", 2, target.promotions.size)
        Assert.assertTrue("Drill I", target.promotions.contains("Drill I"))
        Assert.assertTrue("Cover I", target.promotions.contains("Cover I"))
        Assert.assertFalse("Shock I gone", target.promotions.contains("Shock I"))
        Assert.assertFalse("Shock II gone", target.promotions.contains("Shock II"))
        Assert.assertFalse("Shock III gone", target.promotions.contains("Shock III"))
    }

    // ===== Full add() integration tests =====

    @Test
    fun `full add with raw JSON and merge actions`() {
        val base = Ruleset().apply {
            name = "TestBase"
            modOptions.isBaseRuleset = true
            mods.add("TestBase")

            buildings["Shrine"] = Building().apply {
                name = "Shrine"; faith = 1f; cost = 40; maintenance = 1
                requiredTech = "Pottery"
                uniques = arrayListOf("Only available <when religion is enabled>")
            }
            units["Warrior"] = BaseUnit().apply {
                name = "Warrior"; strength = 8; cost = 40; movement = 2
            }
            units["Scout"] = BaseUnit().apply {
                name = "Scout"; strength = 4; cost = 25; movement = 2
            }
        }

        val mod = Ruleset().apply { name = "TestMod"; mods.add("TestMod") }

        // Simulate Units.json with _mergeAction objects
        mod.rawJsonArrays["Units.json"] = JsonReader().parse("""
        [
            { "name": "Warrior", "_mergeAction": { "action": "TRY_INJECT" }, "strength": 10 },
            { "name": "Scout", "_mergeAction": { "action": "REMOVE" } },
            { "name": "Archer", "strength": 5, "cost": 40, "movement": 2, "range": 2,
              "uniques": ["May upgrade to [Composite Bowman]"] }
        ]
        """.trimIndent())

        mod.rawJsonArrays["Buildings.json"] = JsonReader().parse("""
        [
            { "name": "Shrine", "_mergeAction": { "action": "TRY_INJECT" }, "faith": 2 }
        ]
        """.trimIndent())

        // Pre-populate maps (as load() would do, preserving sourceMap for non-raw-json fallback)
        mod.units["Warrior"] = BaseUnit().apply {
            name = "Warrior"; _mergeAction = MergeAction().apply { action = "TRY_INJECT" }; strength = 10
        }
        mod.units["Scout"] = BaseUnit().apply {
            name = "Scout"; _mergeAction = MergeAction().apply { action = "REMOVE" }
        }
        mod.units["Archer"] = BaseUnit().apply {
            name = "Archer"; strength = 5; cost = 40; movement = 2; range = 2
            uniques = arrayListOf("May upgrade to [Composite Bowman]")
        }
        mod.buildings["Shrine"] = Building().apply {
            name = "Shrine"; _mergeAction = MergeAction().apply { action = "TRY_INJECT" }; faith = 2f
        }

        base.add(mod)

        // Verify units
        Assert.assertTrue("Archer created", base.units.containsKey("Archer"))
        Assert.assertEquals("Archer strength", 5, base.units["Archer"]!!.strength)
        Assert.assertFalse("Scout removed", base.units.containsKey("Scout"))
        Assert.assertTrue("Warrior exists", base.units.containsKey("Warrior"))
        Assert.assertEquals("Warrior strength injected", 10, base.units["Warrior"]!!.strength)

        // Verify buildings
        Assert.assertTrue("Shrine exists", base.buildings.containsKey("Shrine"))
        Assert.assertEquals("Shrine faith injected", 2f, base.buildings["Shrine"]!!.faith)
        Assert.assertEquals("Shrine cost preserved", 40, base.buildings["Shrine"]!!.cost)
        Assert.assertEquals("Shrine maintenance preserved", 1, base.buildings["Shrine"]!!.maintenance)
    }

    @Test
    fun `add with conditional control block`() {
        val base = Ruleset().apply {
            name = "TestBase"
            modOptions.isBaseRuleset = true
            mods.add("TestBase")
            units["Warrior"] = BaseUnit().apply { name = "Warrior"; strength = 8; cost = 40 }
        }

        val mod = Ruleset().apply { name = "SomeMod"; mods.add("SomeMod") }

        // Control block: "SomeMod" IS loaded → then branch chosen
        mod.rawJsonArrays["Units.json"] = JsonReader().parse("""
        [
            {
                "_mergeAction": {
                    "if": { "mod_loaded": "SomeMod" },
                    "then": [
                        { "name": "Warrior", "strength": 15 },
                        { "name": "Archer", "strength": 5, "cost": 40, "movement": 2, "range": 2 }
                    ],
                    "else": [
                        { "name": "Warrior", "strength": 10 }
                    ]
                }
            }
        ]
        """.trimIndent())

        mod.units["Warrior"] = BaseUnit().apply { name = "Warrior"; strength = 15 }
        mod.units["Archer"] = BaseUnit().apply { name = "Archer"; strength = 5; cost = 40; movement = 2; range = 2 }

        base.add(mod)

        // "SomeMod" is in loadedMods → then branch taken
        Assert.assertTrue("Archer created from then branch", base.units.containsKey("Archer"))
        Assert.assertEquals("Warrior strength from then branch", 15, base.units["Warrior"]!!.strength)
        Assert.assertEquals("Archer strength", 5, base.units["Archer"]!!.strength)
    }

    @Test
    fun `control block with failing condition does NOT affect existing objects`() {
        val base = Ruleset().apply {
            name = "TestBase"
            modOptions.isBaseRuleset = true
            mods.add("TestBase")

            buildings["Palace"] = Building().apply {
                name = "Palace"
                isNationalWonder = true
                cost = 0
                production = 3f
                science = 3f
                gold = 3f
                culture = 1f
                cityStrength = 2.5
                uniques = arrayListOf("Indicates the capital city")
            }
        }

        // Simulates the user's testMOD: control block with failing condition
        val mod = Ruleset().apply { name = "testMOD"; mods.add("testMOD") }
        mod.rawJsonArrays["Buildings.json"] = JsonReader().parse("""
        [
            {
                "name": "测试",
                "_mergeAction": {
                    "if": { "mod_loaded": "Civ V - Gods & Kings" },
                    "then": [
                        {
                            "name": "Palace",
                            "_mergeAction": { "action": "TRY_INJECT" },
                            "uniques": [ "[+1 Happiness]" ]
                        }
                    ]
                }
            }
        ]
        """.trimIndent())

        // Pre-populate (as load() would)
        mod.buildings["测试"] = Building().apply { name = "测试" }

        base.add(mod)

        // Palace must be preserved exactly as it was
        Assert.assertTrue("Palace must exist", base.buildings.containsKey("Palace"))
        val palace = base.buildings["Palace"]!!
        Assert.assertTrue("isNationalWonder preserved", palace.isNationalWonder)
        Assert.assertEquals("cost preserved", 0, palace.cost)
        Assert.assertEquals("production preserved", 3f, palace.production)
        Assert.assertEquals("science preserved", 3f, palace.science)
        Assert.assertEquals("gold preserved", 3f, palace.gold)
        Assert.assertEquals("culture preserved", 1f, palace.culture)
        Assert.assertEquals("cityStrength preserved", 2.5, palace.cityStrength, 0.01)
        Assert.assertTrue("IndicatesCapital unique preserved",
            palace.uniques.contains("Indicates the capital city"))

        // The control block container "测试" should NOT be added (it's a control block, not a building)
        Assert.assertFalse("Control block container should not become a building",
            base.buildings.containsKey("测试"))
    }

    @Test
    fun `user scenario - control block with GNK condition injects Palace uniques without losing IndicatesCapital`() {
        // Exact reproduction of the user's testMOD when loaded with G&K base ruleset.
        // The control block's if: { "mod_loaded": "Civ V - Gods & Kings" } should be TRUE.
        val base = Ruleset().apply {
            name = "TestBase"
            modOptions.isBaseRuleset = true
            mods.add("TestBase")
            mods.add("Civ V - Gods & Kings")  // Simulates G&K being loaded

            buildings["Palace"] = Building().apply {
                name = "Palace"; isNationalWonder = true; cost = 0
                production = 3f; science = 3f; gold = 3f; culture = 1f
                cityStrength = 2.5
                uniques = arrayListOf("Indicates the capital city")
            }
        }

        val mod = Ruleset().apply { name = "testMOD"; mods.add("testMOD") }
        // Exact user JSON
        mod.rawJsonArrays["Buildings.json"] = JsonReader().parse("""
        [
            {
                "name":"测试",
                "_mergeAction": {
                    "if": { "mod_loaded": "Civ V - Gods & Kings" },
                    "then": [
                        {
                            "name": "Palace",
                            "_mergeAction": { "action": "TRY_INJECT" },
                            "uniques":["[+1 Happiness]"]
                        }
                    ]
                }
            }
        ]
        """.trimIndent())

        mod.buildings["测试"] = Building().apply { name = "测试" }

        base.add(mod)

        // Palace must have BOTH original and injected uniques
        val palace = base.buildings["Palace"]!!
        Assert.assertTrue("IndicatesCapital MUST still exist",
            palace.uniques.contains("Indicates the capital city"))
        Assert.assertTrue("Injected unique MUST exist",
            palace.uniques.contains("[+1 Happiness]"))
        Assert.assertEquals("Should have exactly 2 uniques", 2, palace.uniques.size)

        // Critical fields untouched
        Assert.assertTrue("isNationalWonder", palace.isNationalWonder)
        Assert.assertEquals("cost", 0, palace.cost)
    }

    @Test
    fun `actual file-based load and add preserves IndicatesCapital`() {
        // Write actual JSON files to temp directory and use Ruleset.load() + add()
        // This is the closest simulation of the real game flow.
        val tmpDir = com.badlogic.gdx.Gdx.files.local("tmp_merge_test")
        tmpDir.mkdirs()

        try {
            // Write test mod's Buildings.json
            val jsonsDir = tmpDir.child("jsons")
            jsonsDir.mkdirs()
            jsonsDir.child("Buildings.json").writeString("""
            [
                {
                    "name":"测试",
                    "_mergeAction": {
                        "if": { "mod_loaded": "MyBaseRuleset" },
                        "then": [
                            {
                                "name": "Palace",
                                "_mergeAction": { "action": "TRY_INJECT" },
                                "uniques":["[+1 Happiness]"]
                            }
                        ]
                    }
                }
            ]
            """.trimIndent(), false)

            // Also write a minimal ModOptions.json so load() doesn't fail
            jsonsDir.child("ModOptions.json").writeString("""
            { "isBaseRuleset": false, "author": "test", "lastUpdated": "", "modUrl": "", "modSize": 0 }
            """.trimIndent(), false)

            // Load the mod ruleset from disk (load() expects the jsons/ subfolder)
            val mod = Ruleset().apply { name = "testMOD" }
            mod.load(jsonsDir)

            // Verify raw JSON was stored (crucial for merge action processing)
            Assert.assertNotNull("raw JSON must be stored for Buildings.json",
                mod.rawJsonArrays["Buildings.json"])
            Assert.assertEquals("Mod should have 1 building (the control block container '测试')",
                1, mod.buildings.size)

            // Create a base ruleset with Palace
            val base = Ruleset().apply {
                name = "MyBaseRuleset"
                modOptions.isBaseRuleset = true
                mods.add("MyBaseRuleset")

                buildings["Palace"] = Building().apply {
                    name = "Palace"; isNationalWonder = true; cost = 0
                    production = 3f; science = 3f; gold = 3f; culture = 1f
                    cityStrength = 2.5
                    uniques = arrayListOf("Indicates the capital city")
                }
            }

            // Combine
            base.add(mod)

            // Verify
            val palace = base.buildings["Palace"]
            Assert.assertNotNull("Palace must exist", palace)
            Assert.assertTrue("IndicatesCapital MUST be preserved after file load→add",
                palace!!.uniques.contains("Indicates the capital city"))
            Assert.assertTrue("Injected unique MUST be present",
                palace.uniques.contains("[+1 Happiness]"))
        } finally {
            tmpDir.deleteDirectory()
        }
    }

    @Test
    fun `real GNK ruleset plus testMOD preserves Palace IndicatesCapital`() {
        // Load the ACTUAL G&K built-in ruleset and the actual testMOD from disk.
        if (RulesetCache.isEmpty())
            RulesetCache.loadRulesets(noMods = true)

        val gnk = RulesetCache[BaseRuleset.Civ_V_GnK.fullName]!!.clone()
        Assert.assertTrue("G&K must have Palace before merge",
            gnk.buildings.containsKey("Palace"))
        val palaceBefore = gnk.buildings["Palace"]!!
        Assert.assertTrue("G&K Palace must have IndicatesCapital before merge",
            palaceBefore.uniques.contains("Indicates the capital city"))
        val uniquesBefore = ArrayList(palaceBefore.uniques)

        // Load testMOD from actual file — try several paths
        var testModDir = com.badlogic.gdx.Gdx.files.internal("mods/testMOD")
        if (!testModDir.isDirectory)
            testModDir = com.badlogic.gdx.Gdx.files.absolute(
                System.getProperty("user.dir") + "/android/assets/mods/testMOD")
        if (!testModDir.isDirectory) {
            println("Skipping test: testMOD directory not found at ${testModDir.path()}")
            return
        }

        val modJsonsDir = testModDir.child("jsons")
        if (!modJsonsDir.isDirectory) {
            println("Skipping test: testMOD/jsons not found")
            return
        }

        val mod = Ruleset().apply { name = "testMOD" }
        mod.load(modJsonsDir)

        // Verify the mod actually loaded and has raw JSON stored
        Assert.assertNotNull("testMOD must have raw JSON for Buildings.json",
            mod.rawJsonArrays["Buildings.json"])
        Assert.assertTrue("testMOD must have buildings loaded",
            mod.buildings.isNotEmpty())

        // Combine G&K + testMOD
        gnk.add(mod)

        // Check Palace — ALL critical properties must survive
        val palace = gnk.buildings["Palace"]!!
        Assert.assertTrue("isNationalWonder must stay true", palace.isNationalWonder)
        Assert.assertEquals("cost must stay 0", 0, palace.cost)
        Assert.assertTrue("IndicatesCapital MUST survive the merge. Before: $uniquesBefore, After: ${palace.uniques}",
            palace.uniques.contains("Indicates the capital city"))
    }

    @Test
    fun `JSON round-trip preserves Boolean and Int fields correctly`() {
        // This tests a potential issue: when raw JSON is present in add(),
        // buildings go through toJson()→fromJson() round-trip. We need to verify
        // that Boolean (isNationalWonder) and Int (cost) fields survive correctly.
        val ruleset = Ruleset().apply { name = "test" }
        val jsonArray = JsonReader().parse("""
        [
            { "name": "Palace", "isNationalWonder": true, "cost": 0 },
            { "name": "Barracks", "cost": 50, "requiredTech": "Bronze Working" }
        ]
        """.trimIndent())

        val result = ruleset.deserializeResolvedList<Building>(jsonArray.toList(), Array<Building>::class.java)

        Assert.assertEquals("2 buildings", 2, result.size)
        val palace = result.find { it.name == "Palace" }!!
        Assert.assertTrue("Palace isNationalWonder MUST be true",
            palace.isNationalWonder)
        Assert.assertEquals("Palace cost MUST be 0", 0, palace.cost)

        val barracks = result.find { it.name == "Barracks" }!!
        Assert.assertEquals("Barracks cost MUST be 50", 50, barracks.cost)
        Assert.assertEquals("Barracks requiredTech", "Bronze Working", barracks.requiredTech)
    }

    @Test
    fun `TRY_INJECT on Palace preserves isNationalWonder and IndicatesCapital`() {
        val base = Ruleset().apply {
            name = "TestBase"
            modOptions.isBaseRuleset = true
            mods.add("TestBase")

            buildings["Palace"] = Building().apply {
                name = "Palace"
                isNationalWonder = true
                cost = 0
                production = 3f
                science = 3f
                gold = 3f
                culture = 1f
                cityStrength = 2.5
                uniques = arrayListOf("Indicates the capital city")
            }
        }

        val mod = Ruleset().apply { name = "TestMod"; mods.add("TestMod") }
        mod.rawJsonArrays["Buildings.json"] = JsonReader().parse("""
        [
            {
                "name": "Palace",
                "_mergeAction": { "action": "TRY_INJECT" },
                "faith": 2,
                "uniques": [ "[+1 Happiness]" ]
            }
        ]
        """.trimIndent())

        mod.buildings["Palace"] = Building().apply {
            name = "Palace"
            _mergeAction = com.unciv.models.ruleset.MergeAction().apply { action = "TRY_INJECT" }
            faith = 2f
            uniques = arrayListOf("[+1 Happiness]")
        }

        base.add(mod)

        val palace = base.buildings["Palace"]!!

        // Critical fields preserved
        Assert.assertTrue("isNationalWonder must remain true", palace.isNationalWonder)
        Assert.assertTrue("IndicatesCapital must remain",
            palace.uniques.contains("Indicates the capital city"))

        // Injected fields added
        Assert.assertEquals("faith injected", 2f, palace.faith, 0.001f)
        Assert.assertTrue("new unique appended",
            palace.uniques.contains("[+1 Happiness]"))

        // Other fields untouched
        Assert.assertEquals("cost preserved", 0, palace.cost)
        Assert.assertEquals("production preserved", 3f, palace.production)
    }
}

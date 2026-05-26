package com.unciv.logic.scripting

import com.unciv.logic.battle.Battle
import com.unciv.logic.battle.MapUnitCombatant
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.NotificationCategory
import com.unciv.logic.civilization.diplomacy.DiplomaticStatus
import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.unique.Conditionals
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.stats.Stat
import com.unciv.models.stats.Stats
import com.unciv.ui.screens.victoryscreen.RankingType
import com.unciv.ui.screens.worldscreen.unit.actions.UnitActionsUpgrade
import com.unciv.models.UpgradeUnitAction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

object LuaAPI {

    private fun LuaValue.safeToInt(): Int {
        val d = this.todouble()
        if (d.isNaN() || d.isInfinite()) {
            com.unciv.utils.Log.error("Lua: argument is NaN/Infinity, using 0")
            return 0
        }
        if (d < Int.MIN_VALUE.toDouble() || d > Int.MAX_VALUE.toDouble()) {
            com.unciv.utils.Log.error("Lua: argument $d overflows Int range, clamping")
            return if (d < 0) Int.MIN_VALUE else Int.MAX_VALUE
        }
        return d.toInt()
    }

    fun buildContext(
        civInfo: Civilization,
        city: City?,
        unit: MapUnit?,
        tile: Tile?,
        resolvedParam: String,
        gameContext: GameContext,
        modName: String = ""
    ): LuaValue {
        val ctx = LuaValue.tableOf()
        ctx.set("parameter", LuaValue.valueOf(resolvedParam))
        if (city != null) ctx.set("city", buildCityTable(city))
        if (unit != null) ctx.set("unit", buildUnitTable(unit))
        if (tile != null) ctx.set("tile", buildTileTable(tile, civInfo))
        ctx.set("civ", buildCivTable(civInfo))
        ctx.set("game", buildGameTable(civInfo))

        ctx.set("log", luaFunction { args ->
            com.unciv.utils.Log.debug("Lua: ${args.arg(1).tojstring()}")
            LuaValue.NIL
        })
        ctx.set("count", luaFunction { args ->
            val expr = args.arg(1).tojstring()
            val result = LuaScriptManager.resolveCountablesInString(expr, gameContext)
            LuaValue.valueOf(result)
        })
        ctx.set("evaluateConditional", luaFunction { args ->
            val conditionalText = args.arg(1).tojstring()
            val conditional = Unique(conditionalText)
            val applies = Conditionals.conditionalApplies(null, conditional, gameContext)
            LuaValue.valueOf(applies)
        })

        if (modName.isNotEmpty()) {
            val storage = civInfo.gameInfo.modLuaStorage.getOrPut(modName) { HashMap() }
            val store = LuaValue.tableOf()
            store.set("get", luaFunction { args ->
                val key = args.arg(1).tojstring()
                val defaultValue = if (args.narg() > 1) args.arg(2).tojstring() else ""
                LuaValue.valueOf(storage[key] ?: defaultValue)
            })
            store.set("set", luaFunction { args ->
                val key = args.arg(1).tojstring()
                val value = args.arg(2).tojstring()
                storage[key] = value
                LuaValue.NIL
            })
            ctx.set("store", store)
        }

        return ctx
    }

    // region civ
    private fun buildCivTable(civ: Civilization): LuaValue {
        val t = LuaValue.tableOf()

        // Identity
        t.set("id", LuaValue.valueOf(civ.civID))
        t.set("name", LuaValue.valueOf(civ.civName))
        t.set("isHuman", LuaValue.valueOf(civ.isHuman()))
        t.set("isAI", LuaValue.valueOf(civ.isAI()))
        t.set("isAlive", LuaValue.valueOf(civ.isAlive()))
        t.set("isMajorCiv", LuaValue.valueOf(civ.isMajorCiv()))
        t.set("isCityState", LuaValue.valueOf(civ.isCityState))
        t.set("isBarbarian", LuaValue.valueOf(civ.isBarbarian))
        t.set("isSpectator", LuaValue.valueOf(civ.isSpectator()))

        // Stats
        t.set("getGold", luaFunction { LuaValue.valueOf(civ.gold) })
        t.set("getHappiness", luaFunction { LuaValue.valueOf(civ.getHappiness()) })
        t.set("getStat", luaFunction { args ->
            val stat = Stat.safeValueOf(args.arg(1).tojstring())
            LuaValue.valueOf(if (stat != null) civ.getStatReserve(stat) else 0)
        })
        t.set("getStatYield", luaFunction { args ->
            val stat = Stat.safeValueOf(args.arg(1).tojstring())
            LuaValue.valueOf(
                if (stat != null) (civ.stats.statsForNextTurn[stat] ?: 0f).toDouble()
                else 0.0
            )
        })
        t.set("getGoldPerTurn", luaFunction {
            LuaValue.valueOf((civ.stats.statsForNextTurn[Stat.Gold] ?: 0f).toDouble())
        })

        // Resources
        t.set("getResourceAmount", luaFunction { args ->
            LuaValue.valueOf(civ.getResourceAmount(args.arg(1).tojstring()))
        })
        t.set("hasResource", luaFunction { args ->
            LuaValue.valueOf(civ.getResourceAmount(args.arg(1).tojstring()) > 0)
        })

        // Era
        t.set("getEra", luaFunction { LuaValue.valueOf(civ.getEra().name) })
        t.set("getEraNumber", luaFunction { LuaValue.valueOf(civ.getEra().eraNumber) })

        // Tech
        t.set("isResearched", luaFunction { args ->
            LuaValue.valueOf(civ.tech.isResearched(args.arg(1).tojstring()))
        })
        t.set("canResearch", luaFunction { args ->
            LuaValue.valueOf(civ.tech.canBeResearched(args.arg(1).tojstring()))
        })
        t.set("getResearchingTech", luaFunction {
            LuaValue.valueOf(civ.tech.currentTechnologyName() ?: "")
        })
        t.set("getResearchProgress", luaFunction { args ->
            val name = args.arg(1).tojstring()
            LuaValue.valueOf(civ.tech.techsInProgress[name] ?: 0)
        })
        t.set("getTechCount", luaFunction {
            LuaValue.valueOf(civ.tech.researchedTechnologies.size)
        })
        t.set("getTechsResearched", luaFunction {
            val arr = LuaTable()
            civ.tech.researchedTechnologies.forEachIndexed { i, tech ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(tech.name))
            }
            arr
        })
        t.set("getAvailableTechs", luaFunction {
            val arr = LuaTable()
            val available = civ.gameInfo.ruleset.technologies.keys
                .filter { civ.tech.canBeResearched(it) }
            available.forEachIndexed { i, tech ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(tech))
            }
            arr
        })
        t.set("grantTech", luaFunction { args ->
            val name = args.arg(1).tojstring()
            if (civ.tech.canBeResearched(name)) civ.tech.addTechnology(name)
            LuaValue.NIL
        })

        // Policies
        t.set("hasPolicy", luaFunction { args ->
            LuaValue.valueOf(civ.policies.isAdopted(args.arg(1).tojstring()))
        })
        t.set("canAdoptPolicy", luaFunction {
            LuaValue.valueOf(civ.policies.canAdoptPolicy())
        })
        t.set("getAdoptedPolicyCount", luaFunction {
            LuaValue.valueOf(civ.policies.getAdoptedPolicies().size)
        })
        t.set("getAdoptedPolicies", luaFunction {
            val arr = LuaTable()
            civ.policies.getAdoptedPolicies().forEachIndexed { i, policyName ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(policyName))
            }
            arr
        })
        t.set("getAvailablePolicyBranches", luaFunction {
            val arr = LuaTable()
            civ.gameInfo.ruleset.policyBranches.keys.forEachIndexed { i, branchName ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(branchName))
            }
            arr
        })
        t.set("grantPolicy", luaFunction { args ->
            val name = args.arg(1).tojstring()
            val policy = civ.gameInfo.ruleset.policies[name]
            if (policy != null && !civ.policies.isAdopted(name))
                civ.policies.adopt(policy)
            LuaValue.NIL
        })

        // Diplomacy
        t.set("isAtWarWith", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val otherCiv = civ.gameInfo.getCivilizationOrNull(other)
            LuaValue.valueOf(otherCiv != null && civ.isAtWarWith(otherCiv))
        })
        t.set("hasOpenBordersWith", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val dm = civ.diplomacy.values.firstOrNull { it.otherCivName == other }
            LuaValue.valueOf(dm?.hasOpenBorders == true)
        })
        t.set("isAlliedWith", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val dm = civ.diplomacy.values.firstOrNull { it.otherCivName == other }
            LuaValue.valueOf(dm != null && dm.diplomaticStatus == DiplomaticStatus.Peace
                && dm.otherCiv.isCityState)
        })
        t.set("getDiplomaticStatus", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val dm = civ.diplomacy.values.firstOrNull { it.otherCivName == other }
            LuaValue.valueOf(dm?.diplomaticStatus?.name ?: "Neutral")
        })
        t.set("getInfluence", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val dm = civ.diplomacy.values.firstOrNull { it.otherCivName == other }
            LuaValue.valueOf(dm?.getInfluence()?.toInt() ?: 0)
        })
        t.set("getKnownCivs", luaFunction {
            val arr = LuaTable()
            civ.getKnownCivs().forEachIndexed { i, c ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(c.civName))
            }
            arr
        })
        t.set("addInfluence", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val amount = args.arg(2).tofloat()
            val dm = civ.diplomacy.values.firstOrNull { it.otherCivName == other }
            dm?.addInfluence(amount)
            LuaValue.NIL
        })
        t.set("declareWarOn", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val otherCiv = civ.gameInfo.getCivilizationOrNull(other)
            if (otherCiv != null && !civ.isAtWarWith(otherCiv))
                civ.getDiplomacyManagerOrMeet(otherCiv).declareWar()
            LuaValue.NIL
        })

        // Religion
        t.set("hasReligion", luaFunction {
            LuaValue.valueOf(civ.religionManager.religion != null)
        })
        t.set("getReligionName", luaFunction {
            LuaValue.valueOf(civ.religionManager.religion?.getReligionDisplayName() ?: "")
        })
        t.set("getFaith", luaFunction {
            LuaValue.valueOf(civ.getStatReserve(Stat.Faith))
        })

        // Cities
        t.set("getCities", luaFunction {
            val arr = LuaTable()
            civ.cities.forEachIndexed { i, city ->
                arr.set(LuaValue.valueOf(i + 1), buildCityTable(city))
            }
            arr
        })
        t.set("getCity", luaFunction { args ->
            val name = args.arg(1).tojstring()
            val city = civ.cities.firstOrNull { it.name == name }
            if (city != null) buildCityTable(city) else LuaValue.NIL
        })
        t.set("getCapital", luaFunction {
            val capital = civ.getCapital()
            if (capital != null) buildCityTable(capital) else LuaValue.NIL
        })
        t.set("getCityCount", luaFunction { LuaValue.valueOf(civ.cities.size) })

        // Units
        t.set("getUnits", luaFunction {
            val arr = LuaTable()
            civ.units.getCivUnits().forEachIndexed { i, unit ->
                arr.set(LuaValue.valueOf(i + 1), buildUnitTable(unit))
            }
            arr
        })
        t.set("getUnitsMatching", luaFunction { args ->
            val filter = args.arg(1).tojstring()
            val arr = LuaTable()
            civ.units.getCivUnits().filter { it.matchesFilter(filter) }
                .forEachIndexed { i, unit ->
                    arr.set(LuaValue.valueOf(i + 1), buildUnitTable(unit))
                }
            arr
        })
        t.set("getUnitCount", luaFunction {
            LuaValue.valueOf(civ.units.getCivUnits().count())
        })

        // Golden Age
        t.set("isGoldenAge", luaFunction {
            LuaValue.valueOf(civ.goldenAges.isGoldenAge())
        })
        t.set("getGoldenAgeTurnsRemaining", luaFunction {
            LuaValue.valueOf(civ.goldenAges.turnsLeftForCurrentGoldenAge)
        })

        // Misc
        t.set("getSpyCount", luaFunction {
            LuaValue.valueOf(civ.espionageManager.spyList.size)
        })
        t.set("getLeaderTitle", luaFunction {
            LuaValue.valueOf(civ.leaderTitle.ifEmpty { null } ?: "")
        })
        t.set("hasUnique", luaFunction { args ->
            val text = args.arg(1).tojstring()
            val ruleset = civ.gameInfo.ruleset
            LuaValue.valueOf(
                civ.nation.uniqueObjects.any { it.text == text }
                || civ.policies.getAdoptedPolicies().any { ruleset.policies[it]?.uniqueObjects?.any { u -> u.text == text } == true }
                || civ.tech.researchedTechnologies.any { it.uniqueObjects.any { u -> u.text == text } }
                || civ.getEra().uniqueObjects.any { it.text == text }
            )
        })

        // Write operations
        t.set("addGold", luaFunction { args ->
            civ.addGold(args.arg(1).safeToInt())
            LuaValue.NIL
        })
        t.set("addStat", luaFunction { args ->
            val stat = Stat.safeValueOf(args.arg(1).tojstring())
            val amount = args.arg(2).safeToInt()
            if (stat != null) civ.addStat(stat, amount)
            LuaValue.NIL
        })
        t.set("addStats", luaFunction { args ->
            val stats = Stats.parse(args.arg(1).tojstring())
            if (!stats.isEmpty()) civ.addStats(stats)
            LuaValue.NIL
        })
        t.set("addResource", luaFunction { args ->
            val resource = civ.gameInfo.ruleset.tileResources[args.arg(1).tojstring()]
            val amount = args.arg(2).safeToInt()
            if (resource != null) civ.gainStockpiledResource(resource, amount)
            LuaValue.NIL
        })
        t.set("consumeResource", luaFunction { args ->
            val resource = civ.gameInfo.ruleset.tileResources[args.arg(1).tojstring()]
            val amount = args.arg(2).safeToInt()
            if (resource != null) civ.gainStockpiledResource(resource, -amount)
            LuaValue.NIL
        })
        t.set("triggerGoldenAge", luaFunction { args ->
            val arg1 = args.arg(1)
            if (arg1.isnil()) civ.goldenAges.enterGoldenAge()
            else civ.goldenAges.enterGoldenAge(arg1.safeToInt())
            LuaValue.NIL
        })
        t.set("grantFreeGreatPerson", luaFunction {
            civ.greatPeople.freeGreatPeople++
            LuaValue.NIL
        })
        t.set("setLeaderTitle", luaFunction { args ->
            civ.leaderTitle = args.arg(1).tojstring()
            LuaValue.NIL
        })
        t.set("addNotification", luaFunction { args ->
            civ.addNotification(args.arg(1).tojstring(), NotificationCategory.General)
            LuaValue.NIL
        })
        t.set("addNotificationAt", luaFunction { args ->
            civ.addNotification(args.arg(1).tojstring(), HexCoord(args.arg(2).safeToInt(), args.arg(3).safeToInt()), NotificationCategory.General)
            LuaValue.NIL
        })
        t.set("addFreeTech", luaFunction {
            civ.tech.freeTechs++
            LuaValue.NIL
        })

        // Unit creation
        t.set("addUnit", luaFunction { args ->
            val unitName = args.arg(1).tojstring()
            val baseUnit = civ.gameInfo.ruleset.units[unitName] ?: return@luaFunction LuaValue.FALSE
            val placedUnit = civ.units.addUnit(civ.getEquivalentUnit(baseUnit), civ.getCapital())
            LuaValue.valueOf(placedUnit != null)
        })
        t.set("addUnitAtCity", luaFunction { args ->
            val unitName = args.arg(1).tojstring()
            val cityName = args.arg(2).tojstring()
            val city = civ.cities.firstOrNull { it.name == cityName }
                ?: return@luaFunction LuaValue.FALSE
            val baseUnit = civ.gameInfo.ruleset.units[unitName]
                ?: return@luaFunction LuaValue.FALSE
            val placedUnit = civ.units.addUnit(civ.getEquivalentUnit(baseUnit), city)
            LuaValue.valueOf(placedUnit != null)
        })
        t.set("addUnitAtTile", luaFunction { args ->
            val unitName = args.arg(1).tojstring()
            val x = args.arg(2).safeToInt()
            val y = args.arg(3).safeToInt()
            val baseUnit = civ.gameInfo.ruleset.units[unitName]
                ?: return@luaFunction LuaValue.FALSE
            val placedUnit = civ.units.placeUnitNearTile(HexCoord(x, y), civ.getEquivalentUnit(baseUnit))
            LuaValue.valueOf(placedUnit != null)
        })
        t.set("addRebelUnit", luaFunction { args ->
            val unitName = args.arg(1).tojstring()
            val baseUnit = civ.gameInfo.ruleset.units[unitName]
                ?: return@luaFunction LuaValue.FALSE
            val barbarians = civ.gameInfo.getBarbarianCivilization()
            val placeNear = civ.cities.randomOrNull()?.getCenterTile()
                ?: civ.units.getCivUnits().firstOrNull()?.currentTile
                ?: return@luaFunction LuaValue.FALSE
            val placed = civ.gameInfo.tileMap.placeUnitNearTile(placeNear.position, baseUnit, barbarians)
            LuaValue.valueOf(placed != null)
        })

        return t
    }
    // endregion

    // region city
    private fun buildCityTable(city: City): LuaValue {
        val t = LuaValue.tableOf()

        t.set("id", LuaValue.valueOf(city.id))
        t.set("name", LuaValue.valueOf(city.name))
        t.set("isCapital", LuaValue.valueOf(city.isCapital()))
        t.set("isCoastal", LuaValue.valueOf(
            city.getCenterTile().neighbors.any { it.isWater }
        ))
        t.set("isPuppet", LuaValue.valueOf(city.isPuppet))
        t.set("isBeingRazed", LuaValue.valueOf(city.isBeingRazed))
        t.set("isConnectedToCapital", LuaValue.valueOf(city.isConnectedToCapital()))

        t.set("population", LuaValue.valueOf(city.population.population))
        t.set("health", LuaValue.valueOf(city.health))

        t.set("getStatYield", luaFunction { args ->
            val stat = Stat.safeValueOf(args.arg(1).tojstring())
            LuaValue.valueOf(
                if (stat != null) (city.cityStats.currentCityStats[stat] ?: 0f).toDouble()
                else 0.0
            )
        })
        t.set("getAllYields", luaFunction {
            val yields = LuaValue.tableOf()
            for (stat in Stat.entries) {
                val v = city.cityStats.currentCityStats[stat]
                if (v != null && v != 0f) yields.set(stat.name, LuaValue.valueOf(v.toDouble()))
            }
            yields
        })

        t.set("hasBuilding", luaFunction { args ->
            LuaValue.valueOf(city.cityConstructions.containsBuildingOrEquivalent(args.arg(1).tojstring()))
        })
        t.set("getBuiltBuildings", luaFunction {
            val arr = LuaTable()
            city.cityConstructions.getBuiltBuildings().forEachIndexed { i, b ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(b.name))
            }
            arr
        })
        t.set("getBuildingCount", luaFunction {
            LuaValue.valueOf(city.cityConstructions.getBuiltBuildings().count())
        })
        t.set("getWonderCount", luaFunction {
            LuaValue.valueOf(city.cityConstructions.getBuiltBuildings().count { it.isAnyWonder() })
        })

        t.set("getPosition", luaFunction {
            val pos = LuaValue.tableOf()
            pos.set("x", LuaValue.valueOf(city.location.x))
            pos.set("y", LuaValue.valueOf(city.location.y))
            pos
        })
        t.set("getCenterTile", luaFunction { buildTileTable(city.getCenterTile(), city.civ) })
        t.set("getTiles", luaFunction {
            val arr = LuaTable()
            city.tiles.forEachIndexed { i, coord ->
                val pt = LuaValue.tableOf()
                pt.set("x", LuaValue.valueOf(coord.x))
                pt.set("y", LuaValue.valueOf(coord.y))
                arr.set(LuaValue.valueOf(i + 1), pt)
            }
            arr
        })

        t.set("getCurrentConstruction", luaFunction {
            LuaValue.valueOf(city.cityConstructions.currentConstructionName())
        })
        t.set("getConstructionQueue", luaFunction {
            val arr = LuaTable()
            city.cityConstructions.constructionQueue.forEachIndexed { i, c ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(c))
            }
            arr
        })

        t.set("getMajorityReligion", luaFunction {
            LuaValue.valueOf(city.religion.getMajorityReligionName() ?: "")
        })
        t.set("isHolyCity", luaFunction {
            LuaValue.valueOf(city.isHolyCity())
        })
        t.set("hasUnique", luaFunction { args ->
            val text = args.arg(1).tojstring()
            LuaValue.valueOf(city.cityConstructions.getBuiltBuildings().any { b -> b.uniqueObjects.any { it.text == text } })
        })

        // Write
        t.set("addPopulation", luaFunction { args ->
            city.population.addPopulation(args.arg(1).safeToInt())
            LuaValue.NIL
        })
        t.set("addBuilding", luaFunction { args ->
            val building = city.civ.getEquivalentBuilding(args.arg(1).tojstring())
            if (!city.cityConstructions.containsBuildingOrEquivalent(building.name))
                city.cityConstructions.completeConstruction(building)
            LuaValue.NIL
        })
        t.set("removeBuilding", luaFunction { args ->
            val building = city.cityConstructions.getBuiltBuildings()
                .firstOrNull { it.name == args.arg(1).tojstring() }
            if (building != null) city.cityConstructions.removeBuilding(building)
            LuaValue.NIL
        })

        // Production queue
        t.set("setProduction", luaFunction { args ->
            city.cityConstructions.setCurrentConstruction(args.arg(1).tojstring())
            LuaValue.NIL
        })
        t.set("addToQueue", luaFunction { args ->
            city.cityConstructions.addToQueue(args.arg(1).tojstring())
            LuaValue.NIL
        })
        t.set("clearQueue", luaFunction {
            city.cityConstructions.removeAll()
            LuaValue.NIL
        })

        return t
    }
    // endregion

    // region unit
    private fun buildUnitTable(unit: MapUnit): LuaValue {
        val t = LuaValue.tableOf()

        t.set("id", LuaValue.valueOf(unit.id))
        t.set("name", LuaValue.valueOf(unit.name))
        t.set("instanceName", LuaValue.valueOf(unit.instanceName ?: ""))
        t.set("isCivilian", LuaValue.valueOf(unit.baseUnit.isCivilian()))
        t.set("isMilitary", LuaValue.valueOf(unit.baseUnit.isMilitary))
        t.set("isRanged", LuaValue.valueOf(unit.baseUnit.isRanged()))
        t.set("isEmbarked", LuaValue.valueOf(unit.isEmbarked()))
        t.set("isFortified", LuaValue.valueOf(unit.isFortified()))
        t.set("isAutomated", LuaValue.valueOf(unit.isAutomated()))

        val baseTable = LuaValue.tableOf()
        baseTable.set("name", LuaValue.valueOf(unit.baseUnit.name))
        baseTable.set("cost", LuaValue.valueOf(unit.baseUnit.cost))
        baseTable.set("movement", LuaValue.valueOf(unit.baseUnit.movement))
        baseTable.set("strength", LuaValue.valueOf(unit.baseUnit.strength))
        baseTable.set("rangedStrength", LuaValue.valueOf(unit.baseUnit.rangedStrength))
        baseTable.set("range", LuaValue.valueOf(unit.baseUnit.range))
        baseTable.set("unitType", LuaValue.valueOf(unit.baseUnit.unitType))
        baseTable.set("requiredResource", LuaValue.valueOf(unit.baseUnit.requiredResource ?: ""))
        baseTable.set("requiredTech", LuaValue.valueOf(unit.baseUnit.requiredTech ?: ""))
        baseTable.set("obsoleteTech", LuaValue.valueOf(unit.baseUnit.obsoleteTech ?: ""))
        baseTable.set("upgradesTo", LuaValue.valueOf(unit.baseUnit.upgradesTo ?: ""))
        baseTable.set("replaces", LuaValue.valueOf(unit.baseUnit.replaces ?: ""))
        baseTable.set("uniqueTo", LuaValue.valueOf(unit.baseUnit.uniqueTo ?: ""))
        val basePromos = LuaTable()
        unit.baseUnit.promotions.forEachIndexed { i, p ->
            basePromos.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(p))
        }
        baseTable.set("promotions", basePromos)
        t.set("base", baseTable)

        t.set("health", LuaValue.valueOf(unit.health))
        t.set("getRange", luaFunction { LuaValue.valueOf(unit.getRange()) })
        t.set("getMovement", luaFunction { LuaValue.valueOf(unit.getMaxMovement().toDouble()) })
        t.set("getCurrentMovement", luaFunction {
            LuaValue.valueOf(unit.currentMovement.toDouble())
        })
        t.set("getXP", luaFunction { LuaValue.valueOf(unit.promotions.XP) })

        t.set("hasPromotion", luaFunction { args ->
            LuaValue.valueOf(unit.promotions.promotions.contains(args.arg(1).tojstring()))
        })
        t.set("hasUnique", luaFunction { args ->
            val text = args.arg(1).tojstring()
            LuaValue.valueOf(unit.baseUnit.rulesetUniqueObjects.any { it.text == text }
                || unit.promotions.promotions.any { promoName ->
                    unit.civ.gameInfo.ruleset.unitPromotions[promoName]?.uniqueObjects?.any { it.text == text } == true
                })
        })
        t.set("getPromotions", luaFunction {
            val arr = LuaTable()
            unit.promotions.promotions.forEachIndexed { i, p ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(p))
            }
            arr
        })
        t.set("getPromotionCount", luaFunction {
            LuaValue.valueOf(unit.promotions.promotions.size)
        })

        t.set("hasStatus", luaFunction { args ->
            LuaValue.valueOf(unit.hasStatus(args.arg(1).tojstring()))
        })
        t.set("getStatusTurns", luaFunction { args ->
            val status = unit.getStatus(args.arg(1).tojstring())
            LuaValue.valueOf(status?.turnsLeft ?: 0)
        })

        t.set("getPosition", luaFunction {
            val pos = LuaValue.tableOf()
            pos.set("x", LuaValue.valueOf(unit.currentTile.position.x))
            pos.set("y", LuaValue.valueOf(unit.currentTile.position.y))
            pos
        })
        t.set("canMoveTo", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val tile = unit.civ.gameInfo.tileMap[HexCoord(x, y)]
            LuaValue.valueOf(tile != null && unit.movement.canMoveTo(tile))
        })
        t.set("getOwner", luaFunction {
            LuaValue.valueOf(unit.civ.civName)
        })
        t.set("isOwnedBy", luaFunction { args ->
            LuaValue.valueOf(unit.civ.civName == args.arg(1).tojstring())
        })

        // Write
        t.set("healBy", luaFunction { args ->
            unit.healBy(args.arg(1).safeToInt())
            LuaValue.NIL
        })
        t.set("takeDamage", luaFunction { args ->
            unit.takeDamage(args.arg(1).safeToInt())
            LuaValue.NIL
        })
        t.set("addXP", luaFunction { args ->
            unit.promotions.XP += args.arg(1).safeToInt()
            LuaValue.NIL
        })
        t.set("addPromotion", luaFunction { args ->
            unit.promotions.addPromotion(args.arg(1).tojstring(), true)
            LuaValue.NIL
        })
        t.set("removePromotion", luaFunction { args ->
            unit.promotions.removePromotion(args.arg(1).tojstring())
            LuaValue.NIL
        })
        t.set("addMovement", luaFunction { args ->
            unit.useMovementPoints(-args.arg(1).tofloat())
            LuaValue.NIL
        })
        t.set("useMovement", luaFunction { args ->
            unit.useMovementPoints(args.arg(1).tofloat())
            LuaValue.NIL
        })
        t.set("upgrade", luaFunction {
            val upgradeAction = UnitActionsUpgrade.getFreeUpgradeAction(unit)
            if (upgradeAction.any()) {
                upgradeAction.minBy { (it as UpgradeUnitAction).unitToUpgradeTo.cost }.action?.invoke()
            }
            LuaValue.NIL
        })
        t.set("destroy", luaFunction {
            unit.destroy()
            LuaValue.NIL
        })
        t.set("attackTile", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val targetTile = unit.civ.gameInfo.tileMap[HexCoord(x, y)]
                ?: return@luaFunction LuaValue.FALSE
            val defender = Battle.getMapCombatantOfTile(targetTile)
                ?: return@luaFunction LuaValue.FALSE
            val attacker = MapUnitCombatant(unit)
            val result = Battle.attack(attacker, defender)
            val resultTable = LuaValue.tableOf()
            resultTable.set("attackerDamage", LuaValue.valueOf(result.attackerDealt))
            resultTable.set("defenderDamage", LuaValue.valueOf(result.defenderDealt))
            resultTable
        })
        t.set("teleportTo", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val target = unit.civ.gameInfo.tileMap[HexCoord(x, y)]
            if (target != null) unit.movement.moveToTile(target)
            LuaValue.NIL
        })

        // Pathfinding
        t.set("findPathTo", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val target = unit.civ.gameInfo.tileMap[HexCoord(x, y)] ?: return@luaFunction LuaValue.NIL
            val path = unit.movement.getShortestPath(target)
            val arr = LuaTable()
            for (i in path.indices) {
                val pt = LuaValue.tableOf()
                pt.set("x", LuaValue.valueOf(path[i].position.x))
                pt.set("y", LuaValue.valueOf(path[i].position.y))
                arr.set(LuaValue.valueOf(i + 1), pt)
            }
            arr
        })
        t.set("canReach", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val target = unit.civ.gameInfo.tileMap[HexCoord(x, y)] ?: return@luaFunction LuaValue.FALSE
            LuaValue.valueOf(unit.movement.canReach(target))
        })

        return t
    }
    // endregion

    // region tile
    private fun buildTileTable(tile: Tile, civInfo: Civilization): LuaValue {
        val t = LuaValue.tableOf()

        val pos = LuaValue.tableOf()
        pos.set("x", LuaValue.valueOf(tile.position.x))
        pos.set("y", LuaValue.valueOf(tile.position.y))
        t.set("position", pos)
        t.set("getX", luaFunction { LuaValue.valueOf(tile.position.x) })
        t.set("getY", luaFunction { LuaValue.valueOf(tile.position.y) })

        t.set("baseTerrain", LuaValue.valueOf(tile.baseTerrain))
        t.set("isLand", LuaValue.valueOf(tile.isLand))
        t.set("isWater", LuaValue.valueOf(tile.isWater))
        t.set("isCoast", LuaValue.valueOf(tile.baseTerrain == "Coast"))
        t.set("isHill", LuaValue.valueOf(tile.isHill()))
        t.set("isMountain", LuaValue.valueOf(tile.isImpassible()))
        t.set("hasTerrainFeature", luaFunction { args ->
            LuaValue.valueOf(tile.terrainFeatures.contains(args.arg(1).tojstring()))
        })
        t.set("getTerrainFeatures", luaFunction {
            val arr = LuaTable()
            tile.terrainFeatures.sorted().forEachIndexed { i, f ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(f))
            }
            arr
        })
        t.set("isImpassable", luaFunction { LuaValue.valueOf(tile.isImpassible()) })
        t.set("isRiver", luaFunction { LuaValue.valueOf(tile.neighbors.any { tile.isConnectedByRiver(it) }) })

        t.set("hasResource", luaFunction { LuaValue.valueOf(tile.tileResource != null) })
        t.set("resourceName", LuaValue.valueOf(tile.tileResource?.name ?: ""))
        t.set("resourceAmount", LuaValue.valueOf(tile.resourceAmount))
        t.set("hasImprovement", luaFunction { LuaValue.valueOf(tile.tileImprovement != null) })
        t.set("improvementName", LuaValue.valueOf(tile.improvement ?: ""))
        t.set("isPillaged", luaFunction { LuaValue.valueOf(tile.isPillaged()) })

        t.set("getYield", luaFunction {
            val result = LuaValue.tableOf()
            val stats = tile.stats.getTileStats(civInfo)
            for (stat in Stat.entries) {
                val v = stats[stat]
                if (v != 0f) result.set(stat.name, LuaValue.valueOf(v.toDouble()))
            }
            result
        })

        t.set("isOwned", luaFunction { LuaValue.valueOf(tile.getOwner() != null) })
        t.set("getOwner", luaFunction { LuaValue.valueOf(tile.getOwner()?.civName ?: "") })
        t.set("isOwnedBy", luaFunction { args ->
            LuaValue.valueOf(tile.getOwner()?.civName == args.arg(1).tojstring())
        })
        t.set("isCityCenter", luaFunction { LuaValue.valueOf(tile.isCityCenter()) })
        t.set("getOwningCity", luaFunction {
            LuaValue.valueOf(tile.owningCity?.name ?: "")
        })

        t.set("isExploredBy", luaFunction { args ->
            val other = civInfo.gameInfo.getCivilizationOrNull(args.arg(1).tojstring())
            LuaValue.valueOf(other != null && tile.isExplored(other))
        })

        t.set("hasMilitaryUnit", luaFunction { LuaValue.valueOf(tile.militaryUnit != null) })
        t.set("hasCivilianUnit", luaFunction { LuaValue.valueOf(tile.civilianUnit != null) })
        t.set("getUnits", luaFunction {
            val arr = LuaTable()
            tile.getUnits().forEachIndexed { i, u -> arr.set(LuaValue.valueOf(i + 1), buildUnitTable(u)) }
            arr
        })

        t.set("getNeighbors", luaFunction {
            val arr = LuaTable()
            tile.neighbors.forEachIndexed { i, n -> arr.set(LuaValue.valueOf(i + 1), buildTileTable(n, civInfo)) }
            arr
        })
        t.set("getNeighborAt", luaFunction { args ->
            val dir = args.arg(1).safeToInt()
            val neighbors = tile.neighbors.toList()
            if (dir in neighbors.indices) buildTileTable(neighbors[dir], civInfo) else LuaValue.NIL
        })
        t.set("getTilesInDistance", luaFunction { args ->
            val radius = args.arg(1).safeToInt()
            val arr = LuaTable()
            tile.getTilesInDistance(radius).forEachIndexed { i, t2 -> arr.set(LuaValue.valueOf(i + 1), buildTileTable(t2, civInfo)) }
            arr
        })

        // Write
        t.set("setTerrain", luaFunction { args ->
            val terrain = civInfo.gameInfo.ruleset.terrains[args.arg(1).tojstring()]
            if (terrain != null) tile.setBaseTerrain(terrain)
            LuaValue.NIL
        })
        t.set("addTerrainFeature", luaFunction { args ->
            tile.addTerrainFeature(args.arg(1).tojstring())
            LuaValue.NIL
        })
        t.set("removeTerrainFeature", luaFunction { args ->
            tile.removeTerrainFeature(args.arg(1).tojstring())
            LuaValue.NIL
        })
        t.set("setImprovement", luaFunction { args ->
            val improvement = civInfo.gameInfo.ruleset.tileImprovements[args.arg(1).tojstring()]
            if (improvement != null) tile.setImprovement(improvement)
            LuaValue.NIL
        })
        t.set("removeImprovement", luaFunction {
            tile.removeImprovement()
            LuaValue.NIL
        })
        t.set("removeResource", luaFunction {
            tile.tileResource = null
            tile.resourceAmount = 0
            LuaValue.NIL
        })
        t.set("setResource", luaFunction { args ->
            val resource = civInfo.gameInfo.ruleset.tileResources[args.arg(1).tojstring()]
            val amount = args.arg(2).safeToInt()
            tile.tileResource = resource
            tile.resourceAmount = amount
            LuaValue.NIL
        })
        t.set("setRoad", luaFunction {
            val roadStatus = com.unciv.logic.map.tile.RoadStatus.Road
            tile.setRoadStatus(roadStatus, civInfo)
            LuaValue.NIL
        })
        t.set("setRailroad", luaFunction {
            val roadStatus = com.unciv.logic.map.tile.RoadStatus.Railroad
            tile.setRoadStatus(roadStatus, civInfo)
            LuaValue.NIL
        })
        t.set("removeRoad", luaFunction {
            tile.removeRoad()
            LuaValue.NIL
        })

        return t
    }
    // endregion

    // region game
    private fun buildGameTable(civInfo: Civilization): LuaValue {
        val gameInfo = civInfo.gameInfo
        val t = LuaValue.tableOf()

        t.set("turn", LuaValue.valueOf(gameInfo.turns))
        t.set("getYear", luaFunction { LuaValue.valueOf(gameInfo.getYear(0)) })
        t.set("speed", LuaValue.valueOf(gameInfo.speed.name))
        t.set("difficulty", LuaValue.valueOf(gameInfo.difficulty))
        t.set("getCurrentPlayer", luaFunction {
            try {
                LuaValue.valueOf(gameInfo.currentPlayerCiv.civName)
            } catch (e: Exception) {
                LuaValue.NIL
            }
        })

        // Civ queries
        t.set("getCiv", luaFunction { args ->
            val name = args.arg(1).tojstring()
            val civ = gameInfo.civilizations.firstOrNull { it.civName == name }
            if (civ != null) buildCivTable(civ) else LuaValue.NIL
        })
        t.set("getCivById", luaFunction { args ->
            val id = args.arg(1).tojstring()
            val civ = gameInfo.getCivilizationOrNull(id)
            if (civ != null) buildCivTable(civ) else LuaValue.NIL
        })
        t.set("getAllCivs", luaFunction {
            val arr = LuaTable()
            gameInfo.civilizations.forEachIndexed { i, c -> arr.set(LuaValue.valueOf(i + 1), buildCivTable(c)) }
            arr
        })
        t.set("getAliveMajorCivs", luaFunction {
            val arr = LuaTable()
            gameInfo.getAliveMajorCivs().forEachIndexed { i, c -> arr.set(LuaValue.valueOf(i + 1), buildCivTable(c)) }
            arr
        })
        t.set("getAliveCityStates", luaFunction {
            val arr = LuaTable()
            gameInfo.getAliveCityStates().forEachIndexed { i, c -> arr.set(LuaValue.valueOf(i + 1), buildCivTable(c)) }
            arr
        })
        t.set("getBarbarianCiv", luaFunction {
            buildCivTable(gameInfo.getBarbarianCivilization())
        })

        // Map
        val ruleset = gameInfo.ruleset
        t.set("getTile", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val tile = gameInfo.tileMap[HexCoord(x, y)]
            if (tile != null) buildTileTable(tile, civInfo) else LuaValue.NIL
        })
        t.set("findTiles", luaFunction { args ->
            val criteria = args.arg(1).checktable()
            val maxResults = criteria.get("maxResults")
            val limit = if (maxResults.isnil()) 500 else maxResults.safeToInt().coerceAtLeast(1)
            val tiles = gameInfo.tileMap.values.asSequence()
            val filtered = filterTilesByCriteria(tiles, criteria, civInfo).take(limit)
            val arr = LuaTable()
            var idx = 1
            for (tile in filtered) {
                arr.set(LuaValue.valueOf(idx), buildTileTable(tile, civInfo))
                idx++
            }
            arr
        })
        t.set("getMapWidth", luaFunction {
            LuaValue.valueOf(gameInfo.tileMap.mapParameters.mapSize.width)
        })
        t.set("getMapHeight", luaFunction {
            LuaValue.valueOf(gameInfo.tileMap.mapParameters.mapSize.height)
        })
        t.set("isWrapped", luaFunction {
            LuaValue.valueOf(gameInfo.tileMap.mapParameters.worldWrap)
        })
        t.set("getTilesNear", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val radius = args.arg(3).safeToInt()
            val center = gameInfo.tileMap[HexCoord(x, y)] ?: return@luaFunction LuaValue.NIL
            val arr = LuaTable()
            center.getTilesInDistance(radius).forEachIndexed { i, t2 -> arr.set(LuaValue.valueOf(i + 1), buildTileTable(t2, civInfo)) }
            arr
        })

        // Ruleset queries
        t.set("getRulesetBuildings", luaFunction {
            val arr = LuaTable()
            ruleset.buildings.keys.forEachIndexed { i, k -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(k)) }
            arr
        })
        t.set("getRulesetUnits", luaFunction {
            val arr = LuaTable()
            ruleset.units.keys.forEachIndexed { i, k -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(k)) }
            arr
        })
        t.set("getRulesetTechs", luaFunction {
            val arr = LuaTable()
            ruleset.technologies.keys.forEachIndexed { i, k -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(k)) }
            arr
        })
        t.set("getRulesetPolicies", luaFunction {
            val arr = LuaTable()
            ruleset.policies.keys.forEachIndexed { i, k -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(k)) }
            arr
        })
        t.set("getRulesetEras", luaFunction {
            val arr = LuaTable()
            ruleset.eras.keys.forEachIndexed { i, k -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(k)) }
            arr
        })
        t.set("getRulesetPromotions", luaFunction {
            val arr = LuaTable()
            ruleset.unitPromotions.keys.forEachIndexed { i, k -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(k)) }
            arr
        })
        t.set("doesBuildingExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.buildings.containsKey(args.arg(1).tojstring()))
        })
        t.set("doesUnitExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.units.containsKey(args.arg(1).tojstring()))
        })

        // Write
        t.set("addGlobalNotification", luaFunction { args ->
            val text = args.arg(1).tojstring()
            for (c in gameInfo.civilizations) {
                if (c.isHuman() && c.isAlive())
                    c.addNotification(text, NotificationCategory.General)
            }
            LuaValue.NIL
        })
        t.set("revealEntireMap", luaFunction { args ->
            val name = args.arg(1).tojstring()
            val civ = gameInfo.civilizations.firstOrNull { it.civName == name }
            if (civ != null) gameInfo.tileMap.values.forEach { it.setExplored(civ, true) }
            LuaValue.NIL
        })
        t.set("revealTilesAround", luaFunction { args ->
            val name = args.arg(1).tojstring()
            val x = args.arg(2).safeToInt()
            val y = args.arg(3).safeToInt()
            val radius = args.arg(4).safeToInt()
            val civ = gameInfo.civilizations.firstOrNull { it.civName == name }
                ?: return@luaFunction LuaValue.NIL
            val center = gameInfo.tileMap[HexCoord(x, y)] ?: return@luaFunction LuaValue.NIL
            center.getTilesInDistance(radius).forEach { it.setExplored(civ, true) }
            LuaValue.NIL
        })

        return t
    }
    // endregion

    // region helpers
    private fun filterTilesByCriteria(
        tiles: Sequence<Tile>,
        criteria: LuaValue,
        civInfo: Civilization
    ): Sequence<Tile> {
        var result = tiles

        val resourceName = (criteria.get("resource").takeIf { !it.isnil() })?.tojstring()
        if (resourceName != null)
            result = result.filter { it.tileResource?.name == resourceName }

        val terrainName = (criteria.get("terrain").takeIf { !it.isnil() })?.tojstring()
        if (terrainName != null)
            result = result.filter { it.baseTerrain == terrainName }

        val terrainFeature = (criteria.get("terrainFeature").takeIf { !it.isnil() })?.tojstring()
        if (terrainFeature != null)
            result = result.filter { it.terrainFeatures.contains(terrainFeature) }

        val improvement = (criteria.get("improvement").takeIf { !it.isnil() })?.tojstring()
        if (improvement != null)
            result = result.filter { it.improvement == improvement }

        val owned = criteria.get("owned")
        if (!owned.isnil())
            result = result.filter { owned.toboolean() == (it.getOwner() != null) }

        val ownerName = (criteria.get("owner").takeIf { !it.isnil() })?.tojstring()
        if (ownerName != null)
            result = result.filter { it.getOwner()?.civName == ownerName }

        val isCoast = criteria.get("isCoast")
        if (!isCoast.isnil())
            result = result.filter { isCoast.toboolean() == (it.baseTerrain == "Coast") }

        val isLand = criteria.get("isLand")
        if (!isLand.isnil())
            result = result.filter { isLand.toboolean() == it.isLand }

        val isWater = criteria.get("isWater")
        if (!isWater.isnil())
            result = result.filter { isWater.toboolean() == it.isWater }

        val isHill = criteria.get("isHill")
        if (!isHill.isnil())
            result = result.filter { isHill.toboolean() == it.isHill() }

        val maxDistance = criteria.get("maxDistance")
        if (!maxDistance.isnil()) {
            val centerX = criteria.get("centerX").safeToInt()
            val centerY = criteria.get("centerY").safeToInt()
            val origin = HexCoord(centerX, centerY)
            val tilesInRange = civInfo.gameInfo.tileMap.getTilesInDistance(origin, maxDistance.safeToInt()).toHashSet()
            result = result.filter { it in tilesInRange }
        }

        return result
    }
    // endregion
}

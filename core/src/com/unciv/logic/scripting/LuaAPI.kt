package com.unciv.logic.scripting

import com.unciv.Constants
import com.unciv.logic.battle.Battle
import com.unciv.logic.battle.MapUnitCombatant
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.NotificationCategory
import com.unciv.logic.civilization.diplomacy.DiplomaticStatus
import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.RoadStatus
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.IConstruction
import com.unciv.models.ruleset.INonPerpetualConstruction
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

    /**
     * Static catalog of every API name exposed per context table - the source of
     * truth for the CLI mod checker (desktop mod-ci / --check-mod), which has no
     * running game to trigger the runtime registration below.
     * Kept in sync with the runtime registration by [LuaSecurityTests].
     */
    val apiCatalog: Map<String, Set<String>> = mapOf(
        "ctx" to setOf("parameter", "city", "unit", "tile", "civ", "game", "log", "count", "evaluateConditional", "store", "random", "randomInt"),
        "store" to setOf("get", "set"),
        "civ" to setOf("id", "name", "isHuman", "isAI", "isAlive", "isMajorCiv", "isCityState", "isBarbarian", "isSpectator", "getNation", "getLeaderName", "getScore", "getForce", "getGold", "getHappiness", "getStat", "getStatYield", "getGoldPerTurn", "getSciencePerTurn", "getCulturePerTurn", "getFoodPerTurn", "getProductionPerTurn", "getResourceAmount", "hasResource", "getResourceStockpiles", "getEra", "getEraNumber", "isResearched", "canResearch", "getResearchingTech", "getResearchProgress", "getTechCount", "getTechsResearched", "getAvailableTechs", "getTechCost", "grantTech", "discoverTech", "hasPolicy", "canAdoptPolicy", "getAdoptedPolicyCount", "getAdoptedPolicies", "getAvailablePolicyBranches", "grantPolicy", "getCultureNeededForNextPolicy", "isAtWarWith", "hasOpenBordersWith", "isAlliedWith", "getDiplomaticStatus", "getDiplomaticStatuses", "getProximityTo", "hasEmbassyWith", "getInfluence", "getKnownCivs", "addInfluence", "declareWarOn", "makePeaceWith", "hasReligion", "getReligionName", "getFaith", "getCities", "getCity", "getCapital", "getCityCount", "getCityNames", "getTotalPopulation", "getWondersBuilt", "getUnits", "getUnitsMatching", "getUnitCount", "isGoldenAge", "getGoldenAgeTurnsRemaining", "getSpyCount", "getSpies", "addSpy", "getLeaderTitle", "hasUnique", "addGold", "setGold", "addStat", "addStats", "addResource", "consumeResource", "triggerGoldenAge", "grantFreeGreatPerson", "setLeaderTitle", "addNotification", "addNotificationAt", "addFreeTech", "addUnit", "addUnitAtCity", "addUnitAtTile", "addRebelUnit"),
        "city" to setOf("id", "name", "isCapital", "isCoastal", "isPuppet", "isBeingRazed", "isConnectedToCapital", "population", "health", "getStatYield", "getAllYields", "getFood", "getFoodSurplus", "getFoodStorage", "getFoodNeeded", "getProductionProgress", "getProductionCost", "getTurnsToCompletion", "getGarrisonedUnit", "getStrength", "getSpecialistCount", "getUnemployedCount", "getBuiltWonders", "isInResistance", "hasBuilding", "getBuiltBuildings", "getBuildingCount", "getWonderCount", "getPosition", "getCenterTile", "getTiles", "getCurrentConstruction", "getConstructionQueue", "getMajorityReligion", "isHolyCity", "hasUnique", "addPopulation", "setPopulation", "addFood", "addProduction", "addHealth", "setName", "addBuilding", "removeBuilding", "sellBuilding", "setProduction", "addToQueue", "clearQueue"),
        "unit" to setOf("id", "name", "instanceName", "isCivilian", "isMilitary", "isRanged", "isEmbarked", "isFortified", "isAutomated", "base", "health", "getRange", "getEraNumber", "getMovement", "getCurrentMovement", "getXP", "getMaxHealth", "getDamage", "getAttacksLeft", "getVisibilityRange", "getAction", "canAttack", "canPillage", "isInEnemyTerritory", "isInFriendlyTerritory", "isGreatPerson", "getReligionDisplayName", "hasPromotion", "hasUnique", "getPromotions", "getPromotionCount", "hasStatus", "getStatusTurns", "getPosition", "canMoveTo", "getOwner", "isOwnedBy", "healBy", "takeDamage", "addXP", "setXP", "setHealth", "addPromotion", "removePromotion", "addMovement", "useMovement", "setStatus", "setAttacksLeft", "fortify", "moveByPath", "upgrade", "destroy", "attackTile", "teleportTo", "findPathTo", "canReach"),
        "tile" to setOf("position", "getX", "getY", "baseTerrain", "isLand", "isWater", "isCoast", "isHill", "isMountain", "hasTerrainFeature", "getTerrainFeatures", "isImpassable", "isRiver", "isAdjacentToCoast", "hasRoad", "hasRailroad", "hasNaturalWonder", "getNaturalWonder", "hasResource", "resourceName", "resourceAmount", "hasImprovement", "improvementName", "isPillaged", "getYield", "isOwned", "getOwner", "isOwnedBy", "isFriendlyTerritory", "isEnemyTerritory", "isCityCenter", "getOwningCity", "isExploredBy", "getDistanceTo", "isAdjacentTo", "hasMilitaryUnit", "hasCivilianUnit", "getUnits", "getNeighbors", "getNeighborAt", "getTilesInDistance", "setExplored", "setTerrain", "addTerrainFeature", "removeTerrainFeature", "setImprovement", "removeImprovement", "removeResource", "setResource", "setRoad", "setRailroad", "removeRoad"),
        "game" to setOf("turn", "getYear", "speed", "difficulty", "getCurrentPlayer", "getCurrentPlayerCiv", "getCiv", "getCivById", "getAllCivs", "getCivNames", "getHumanCivs", "getAliveMajorCivs", "getAliveCityStates", "getBarbarianCiv", "getTile", "findTiles", "getMapWidth", "getMapHeight", "getMapName", "getMapType", "isWrapped", "getTilesNear", "getEraNames", "getVictoryTypes", "getMods", "getBaseRuleset", "getRulesetBuildings", "getRulesetUnits", "getRulesetTechs", "getRulesetPolicies", "getRulesetEras", "getRulesetPromotions", "getRulesetTerrains", "getRulesetResources", "getRulesetImprovements", "getRulesetNations", "getRulesetReligions", "getRulesetBeliefs", "getRulesetEvents", "getRulesetNaturalWonders", "getRulesetUnitTypes", "doesBuildingExist", "doesUnitExist", "doesTechExist", "doesPolicyExist", "doesEraExist", "doesPromotionExist", "doesTerrainExist", "doesResourceExist", "doesImprovementExist", "doesNationExist", "doesBeliefExist", "doesEventExist", "addGlobalNotification", "revealEntireMap", "revealTilesAround"),
    )


    /**
     * API methods exposed per context table (civ/city/unit/tile/game/ctx), registered when the
     * tables are built at runtime. Consumed by the CLI mod checker (server --check-mod) to
     * statically detect typos in mod Lua scripts, e.g. `ctx.civ.addGoldd(...)`.
     */
    val knownApiMethods = java.util.concurrent.ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * Counter feeding the seed of [ctx.random]/[ctx.randomInt]. Combined with the game's
     * state-based RNG this makes Lua randomness deterministic across online-multiplayer
     * clients (same call sequence on the same game state yields the same numbers), while
     * still producing fresh values on every call.
     */
    private val luaRandomCounter = java.util.concurrent.atomic.AtomicLong()

    private fun LuaTable.registerApi(owner: String, name: String, value: LuaValue): LuaTable {
        if (name !in apiCatalog[owner].orEmpty())
            com.unciv.utils.Log.error("LuaAPI: '$name' (owner '$owner') is missing from apiCatalog - the CLI mod checker will not know it")
        knownApiMethods.getOrPut(owner) { java.util.concurrent.ConcurrentHashMap.newKeySet() }.add(name)
        this.set(name, value)
        return this
    }

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

    private fun LuaValue.safeToFloat(): Float {
        val f = this.tofloat()
        if (f.isNaN() || f.isInfinite()) {
            com.unciv.utils.Log.error("Lua: argument is NaN/Infinity, using 0")
            return 0f
        }
        return f
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
        ctx.registerApi("ctx", "parameter", LuaValue.valueOf(resolvedParam))
        if (city != null) ctx.registerApi("ctx", "city", buildCityTable(city))
        if (unit != null) ctx.registerApi("ctx", "unit", buildUnitTable(unit))
        if (tile != null) ctx.registerApi("ctx", "tile", buildTileTable(tile, civInfo))
        ctx.registerApi("ctx", "civ", buildCivTable(civInfo))
        ctx.registerApi("ctx", "game", buildGameTable(civInfo))

        ctx.registerApi("ctx", "log", luaFunction { args ->
            com.unciv.utils.Log.debug("Lua: ${args.arg(1).tojstring()}")
            LuaValue.NIL
        })
        ctx.registerApi("ctx", "count", luaFunction { args ->
            val expr = args.arg(1).tojstring()
            val result = LuaScriptManager.resolveCountablesInString(expr, gameContext)
            LuaValue.valueOf(result)
        })
        ctx.registerApi("ctx", "evaluateConditional", luaFunction { args ->
            val conditionalText = args.arg(1).tojstring()
            val conditional = Unique(conditionalText)
            val applies = Conditionals.conditionalApplies(null, conditional, gameContext)
            LuaValue.valueOf(applies)
        })

        // Deterministic random helpers (seeded from game state, so online-multiplayer safe)
        ctx.registerApi("ctx", "random", luaFunction {
            val rng = gameContext.stateBasedRandom("LuaAPI.random", (luaRandomCounter.incrementAndGet() and 0x7FFFFFFF).toInt())
            LuaValue.valueOf(rng.nextDouble())
        })
        ctx.registerApi("ctx", "randomInt", luaFunction { args ->
            val min = args.arg(1).safeToInt()
            val max = args.arg(2).safeToInt()
            if (max < min) return@luaFunction LuaValue.valueOf(min)
            val rng = gameContext.stateBasedRandom("LuaAPI.randomInt", (luaRandomCounter.incrementAndGet() and 0x7FFFFFFF).toInt())
            LuaValue.valueOf(rng.nextInt(max - min + 1) + min)
        })

        if (modName.isNotEmpty()) {
            val storage = civInfo.gameInfo.modLuaStorage.getOrPut(modName) { HashMap() }
            val store = LuaValue.tableOf()
            store.registerApi("store", "get", luaFunction { args ->
                val key = args.arg(1).tojstring()
                val defaultValue = if (args.narg() > 1) args.arg(2).tojstring() else ""
                LuaValue.valueOf(storage[key] ?: defaultValue)
            })
            store.registerApi("store", "set", luaFunction { args ->
                val key = args.arg(1).tojstring()
                val value = args.arg(2).tojstring()
                storage[key] = value
                LuaValue.NIL
            })
            ctx.registerApi("ctx", "store", store)
        }

        return ctx
    }

    // region civ
    private fun buildCivTable(civ: Civilization): LuaValue {
        val t = LuaValue.tableOf()

        // Identity
        t.registerApi("civ", "id", LuaValue.valueOf(civ.civID))
        t.registerApi("civ", "name", LuaValue.valueOf(civ.civName))
        t.registerApi("civ", "isHuman", LuaValue.valueOf(civ.isHuman()))
        t.registerApi("civ", "isAI", LuaValue.valueOf(civ.isAI()))
        t.registerApi("civ", "isAlive", LuaValue.valueOf(civ.isAlive()))
        t.registerApi("civ", "isMajorCiv", LuaValue.valueOf(civ.isMajorCiv()))
        t.registerApi("civ", "isCityState", LuaValue.valueOf(civ.isCityState))
        t.registerApi("civ", "isBarbarian", LuaValue.valueOf(civ.isBarbarian))
        t.registerApi("civ", "isSpectator", LuaValue.valueOf(civ.isSpectator()))
        t.registerApi("civ", "getNation", luaFunction { LuaValue.valueOf(civ.nation.name) })
        t.registerApi("civ", "getLeaderName", luaFunction { LuaValue.valueOf(civ.nation.leaderName) })
        t.registerApi("civ", "getScore", luaFunction {
            LuaValue.valueOf(civ.getStatForRanking(RankingType.Score))
        })
        t.registerApi("civ", "getForce", luaFunction {
            LuaValue.valueOf(civ.getStatForRanking(RankingType.Force))
        })

        // Stats
        t.registerApi("civ", "getGold", luaFunction { LuaValue.valueOf(civ.gold) })
        t.registerApi("civ", "getHappiness", luaFunction { LuaValue.valueOf(civ.getHappiness()) })
        t.registerApi("civ", "getStat", luaFunction { args ->
            val stat = Stat.safeValueOf(args.arg(1).tojstring())
            LuaValue.valueOf(if (stat != null) civ.getStatReserve(stat) else 0)
        })
        t.registerApi("civ", "getStatYield", luaFunction { args ->
            val stat = Stat.safeValueOf(args.arg(1).tojstring())
            LuaValue.valueOf(
                if (stat != null) (civ.stats.statsForNextTurn[stat] ?: 0f).toDouble()
                else 0.0
            )
        })
        t.registerApi("civ", "getGoldPerTurn", luaFunction {
            LuaValue.valueOf((civ.stats.statsForNextTurn[Stat.Gold] ?: 0f).toDouble())
        })
        t.registerApi("civ", "getSciencePerTurn", luaFunction {
            LuaValue.valueOf((civ.stats.statsForNextTurn[Stat.Science] ?: 0f).toDouble())
        })
        t.registerApi("civ", "getCulturePerTurn", luaFunction {
            LuaValue.valueOf((civ.stats.statsForNextTurn[Stat.Culture] ?: 0f).toDouble())
        })
        t.registerApi("civ", "getFoodPerTurn", luaFunction {
            LuaValue.valueOf((civ.stats.statsForNextTurn[Stat.Food] ?: 0f).toDouble())
        })
        t.registerApi("civ", "getProductionPerTurn", luaFunction {
            LuaValue.valueOf((civ.stats.statsForNextTurn[Stat.Production] ?: 0f).toDouble())
        })

        // Resources
        t.registerApi("civ", "getResourceAmount", luaFunction { args ->
            LuaValue.valueOf(civ.getResourceAmount(args.arg(1).tojstring()))
        })
        t.registerApi("civ", "hasResource", luaFunction { args ->
            LuaValue.valueOf(civ.getResourceAmount(args.arg(1).tojstring()) > 0)
        })
        t.registerApi("civ", "getResourceStockpiles", luaFunction {
            val stockpiles = LuaValue.tableOf()
            for ((name, amount) in civ.resourceStockpiles)
                stockpiles.set(name, LuaValue.valueOf(amount))
            stockpiles
        })

        // Era
        t.registerApi("civ", "getEra", luaFunction { LuaValue.valueOf(civ.getEra().name) })
        t.registerApi("civ", "getEraNumber", luaFunction { LuaValue.valueOf(civ.getEra().eraNumber) })

        // Tech
        t.registerApi("civ", "isResearched", luaFunction { args ->
            LuaValue.valueOf(civ.tech.isResearched(args.arg(1).tojstring()))
        })
        t.registerApi("civ", "canResearch", luaFunction { args ->
            LuaValue.valueOf(civ.tech.canBeResearched(args.arg(1).tojstring()))
        })
        t.registerApi("civ", "getResearchingTech", luaFunction {
            LuaValue.valueOf(civ.tech.currentTechnologyName() ?: "")
        })
        t.registerApi("civ", "getResearchProgress", luaFunction { args ->
            val name = args.arg(1).tojstring()
            LuaValue.valueOf(civ.tech.techsInProgress[name] ?: 0)
        })
        t.registerApi("civ", "getTechCount", luaFunction {
            LuaValue.valueOf(civ.tech.researchedTechnologies.size)
        })
        t.registerApi("civ", "getTechsResearched", luaFunction {
            val arr = LuaTable()
            civ.tech.researchedTechnologies.forEachIndexed { i, tech ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(tech.name))
            }
            arr
        })
        t.registerApi("civ", "getAvailableTechs", luaFunction {
            val arr = LuaTable()
            val available = civ.gameInfo.ruleset.technologies.keys
                .filter { civ.tech.canBeResearched(it) }
            available.forEachIndexed { i, tech ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(tech))
            }
            arr
        })
        t.registerApi("civ", "getTechCost", luaFunction { args ->
            val tech = civ.gameInfo.ruleset.technologies[args.arg(1).tojstring()]
            LuaValue.valueOf(tech?.cost ?: 0)
        })
        t.registerApi("civ", "grantTech", luaFunction { args ->
            val name = args.arg(1).tojstring()
            if (civ.tech.canBeResearched(name)) civ.tech.addTechnology(name)
            LuaValue.NIL
        })
        t.registerApi("civ", "discoverTech", luaFunction { args ->
            // 等价于 "Discover [tech]" unique：绕过可研究性检查直接解锁（幂等）
            val name = args.arg(1).tojstring()
            if (civ.gameInfo.ruleset.technologies.containsKey(name)) civ.tech.addTechnology(name)
            LuaValue.NIL
        })

        // Policies
        t.registerApi("civ", "hasPolicy", luaFunction { args ->
            LuaValue.valueOf(civ.policies.isAdopted(args.arg(1).tojstring()))
        })
        t.registerApi("civ", "canAdoptPolicy", luaFunction {
            LuaValue.valueOf(civ.policies.canAdoptPolicy())
        })
        t.registerApi("civ", "getAdoptedPolicyCount", luaFunction {
            LuaValue.valueOf(civ.policies.getAdoptedPolicies().size)
        })
        t.registerApi("civ", "getAdoptedPolicies", luaFunction {
            val arr = LuaTable()
            civ.policies.getAdoptedPolicies().forEachIndexed { i, policyName ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(policyName))
            }
            arr
        })
        t.registerApi("civ", "getAvailablePolicyBranches", luaFunction {
            val arr = LuaTable()
            civ.gameInfo.ruleset.policyBranches.keys.forEachIndexed { i, branchName ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(branchName))
            }
            arr
        })
        t.registerApi("civ", "grantPolicy", luaFunction { args ->
            val name = args.arg(1).tojstring()
            val policy = civ.gameInfo.ruleset.policies[name]
            if (policy != null && !civ.policies.isAdopted(name))
                civ.policies.adopt(policy)
            LuaValue.NIL
        })
        t.registerApi("civ", "getCultureNeededForNextPolicy", luaFunction {
            LuaValue.valueOf(civ.policies.getCultureNeededForNextPolicy())
        })

        // Diplomacy
        t.registerApi("civ", "isAtWarWith", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val otherCiv = civ.gameInfo.getCivilizationOrNull(other)
            LuaValue.valueOf(otherCiv != null && civ.isAtWarWith(otherCiv))
        })
        t.registerApi("civ", "hasOpenBordersWith", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val dm = civ.diplomacy.values.firstOrNull { it.otherCivName == other }
            LuaValue.valueOf(dm?.hasOpenBorders == true)
        })
        t.registerApi("civ", "isAlliedWith", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val dm = civ.diplomacy.values.firstOrNull { it.otherCivName == other }
            LuaValue.valueOf(dm != null && dm.diplomaticStatus == DiplomaticStatus.Peace
                && dm.otherCiv.isCityState)
        })
        t.registerApi("civ", "getDiplomaticStatus", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val dm = civ.diplomacy.values.firstOrNull { it.otherCivName == other }
            LuaValue.valueOf(dm?.diplomaticStatus?.name ?: "Neutral")
        })
        t.registerApi("civ", "getDiplomaticStatuses", luaFunction {
            val statuses = LuaValue.tableOf()
            for (dm in civ.diplomacy.values)
                statuses.set(dm.otherCivName, LuaValue.valueOf(dm.diplomaticStatus.name))
            statuses
        })
        t.registerApi("civ", "getProximityTo", luaFunction { args ->
            val other = civ.gameInfo.getCivilizationOrNull(args.arg(1).tojstring())
            LuaValue.valueOf(if (other != null) civ.getProximity(other).name else "")
        })
        t.registerApi("civ", "hasEmbassyWith", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val dm = civ.diplomacy.values.firstOrNull { it.otherCivName == other }
            val hasEmbassy = dm?.trades?.any { trade ->
                trade.ourOffers.any { it.name == Constants.acceptEmbassy && it.duration > 0 }
                    || trade.theirOffers.any { it.name == Constants.acceptEmbassy && it.duration > 0 }
            } == true
            LuaValue.valueOf(hasEmbassy)
        })
        t.registerApi("civ", "getInfluence", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val dm = civ.diplomacy.values.firstOrNull { it.otherCivName == other }
            LuaValue.valueOf(dm?.getInfluence()?.toInt() ?: 0)
        })
        t.registerApi("civ", "getKnownCivs", luaFunction {
            val arr = LuaTable()
            civ.getKnownCivs().forEachIndexed { i, c ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(c.civName))
            }
            arr
        })
        t.registerApi("civ", "addInfluence", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val amount = args.arg(2).safeToFloat()
            val dm = civ.diplomacy.values.firstOrNull { it.otherCivName == other }
            dm?.addInfluence(amount)
            LuaValue.NIL
        })
        t.registerApi("civ", "declareWarOn", luaFunction { args ->
            val other = args.arg(1).tojstring()
            val otherCiv = civ.gameInfo.getCivilizationOrNull(other)
            if (otherCiv != null && !civ.isAtWarWith(otherCiv))
                civ.getDiplomacyManagerOrMeet(otherCiv).declareWar()
            LuaValue.NIL
        })
        t.registerApi("civ", "makePeaceWith", luaFunction { args ->
            val dm = civ.diplomacy.values.firstOrNull { it.otherCivName == args.arg(1).tojstring() }
            if (dm != null && dm.diplomaticStatus == DiplomaticStatus.War)
                dm.makePeace()
            LuaValue.NIL
        })

        // Religion
        t.registerApi("civ", "hasReligion", luaFunction {
            LuaValue.valueOf(civ.religionManager.religion != null)
        })
        t.registerApi("civ", "getReligionName", luaFunction {
            LuaValue.valueOf(civ.religionManager.religion?.getReligionDisplayName() ?: "")
        })
        t.registerApi("civ", "getFaith", luaFunction {
            LuaValue.valueOf(civ.getStatReserve(Stat.Faith))
        })

        // Cities
        t.registerApi("civ", "getCities", luaFunction {
            val arr = LuaTable()
            civ.cities.forEachIndexed { i, city ->
                arr.set(LuaValue.valueOf(i + 1), buildCityTable(city))
            }
            arr
        })
        t.registerApi("civ", "getCity", luaFunction { args ->
            val name = args.arg(1).tojstring()
            val city = civ.cities.firstOrNull { it.name == name }
            if (city != null) buildCityTable(city) else LuaValue.NIL
        })
        t.registerApi("civ", "getCapital", luaFunction {
            val capital = civ.getCapital()
            if (capital != null) buildCityTable(capital) else LuaValue.NIL
        })
        t.registerApi("civ", "getCityCount", luaFunction { LuaValue.valueOf(civ.cities.size) })
        t.registerApi("civ", "getCityNames", luaFunction {
            val arr = LuaTable()
            civ.cities.forEachIndexed { i, c -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(c.name)) }
            arr
        })
        t.registerApi("civ", "getTotalPopulation", luaFunction {
            LuaValue.valueOf(civ.cities.sumOf { it.population.population })
        })
        t.registerApi("civ", "getWondersBuilt", luaFunction {
            val arr = LuaTable()
            civ.cities.flatMap { it.cityConstructions.getBuiltBuildings() }
                .filter { it.isAnyWonder() }
                .forEachIndexed { i, b -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(b.name)) }
            arr
        })

        // Units
        t.registerApi("civ", "getUnits", luaFunction {
            val arr = LuaTable()
            civ.units.getCivUnits().forEachIndexed { i, unit ->
                arr.set(LuaValue.valueOf(i + 1), buildUnitTable(unit))
            }
            arr
        })
        t.registerApi("civ", "getUnitsMatching", luaFunction { args ->
            val filter = args.arg(1).tojstring()
            val arr = LuaTable()
            civ.units.getCivUnits().filter { it.matchesFilter(filter) }
                .forEachIndexed { i, unit ->
                    arr.set(LuaValue.valueOf(i + 1), buildUnitTable(unit))
                }
            arr
        })
        t.registerApi("civ", "getUnitCount", luaFunction {
            LuaValue.valueOf(civ.units.getCivUnits().count())
        })

        // Golden Age
        t.registerApi("civ", "isGoldenAge", luaFunction {
            LuaValue.valueOf(civ.goldenAges.isGoldenAge())
        })
        t.registerApi("civ", "getGoldenAgeTurnsRemaining", luaFunction {
            LuaValue.valueOf(civ.goldenAges.turnsLeftForCurrentGoldenAge)
        })

        // Misc
        t.registerApi("civ", "getSpyCount", luaFunction {
            LuaValue.valueOf(civ.espionageManager.spyList.size)
        })
        t.registerApi("civ", "getSpies", luaFunction {
            val arr = LuaTable()
            civ.espionageManager.spyList.forEachIndexed { i, spy ->
                val s = LuaValue.tableOf()
                s.set("name", LuaValue.valueOf(spy.name))
                s.set("rank", LuaValue.valueOf(spy.rank))
                s.set("action", LuaValue.valueOf(spy.action.name))
                s.set("location", LuaValue.valueOf(spy.getLocationName()))
                arr.set(LuaValue.valueOf(i + 1), s)
            }
            arr
        })
        t.registerApi("civ", "addSpy", luaFunction {
            civ.espionageManager.addSpy()
            LuaValue.NIL
        })
        t.registerApi("civ", "getLeaderTitle", luaFunction {
            LuaValue.valueOf(civ.leaderTitle.ifEmpty { null } ?: "")
        })
        t.registerApi("civ", "hasUnique", luaFunction { args ->
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
        t.registerApi("civ", "addGold", luaFunction { args ->
            civ.addGold(args.arg(1).safeToInt())
            LuaValue.NIL
        })
        t.registerApi("civ", "setGold", luaFunction { args ->
            val target = args.arg(1).safeToInt().coerceAtLeast(0)
            civ.addGold(target - civ.gold)
            LuaValue.NIL
        })
        t.registerApi("civ", "addStat", luaFunction { args ->
            val stat = Stat.safeValueOf(args.arg(1).tojstring())
            val amount = args.arg(2).safeToInt()
            if (stat != null) civ.addStat(stat, amount)
            LuaValue.NIL
        })
        t.registerApi("civ", "addStats", luaFunction { args ->
            val stats = Stats.parse(args.arg(1).tojstring())
            if (!stats.isEmpty()) civ.addStats(stats)
            LuaValue.NIL
        })
        t.registerApi("civ", "addResource", luaFunction { args ->
            val resource = civ.gameInfo.ruleset.tileResources[args.arg(1).tojstring()]
            val amount = args.arg(2).safeToInt()
            if (resource != null) civ.gainStockpiledResource(resource, amount)
            LuaValue.NIL
        })
        t.registerApi("civ", "consumeResource", luaFunction { args ->
            val resource = civ.gameInfo.ruleset.tileResources[args.arg(1).tojstring()]
            val amount = args.arg(2).safeToInt()
            if (resource != null) civ.gainStockpiledResource(resource, -amount)
            LuaValue.NIL
        })
        t.registerApi("civ", "triggerGoldenAge", luaFunction { args ->
            val arg1 = args.arg(1)
            if (arg1.isnil()) civ.goldenAges.enterGoldenAge()
            else civ.goldenAges.enterGoldenAge(arg1.safeToInt())
            LuaValue.NIL
        })
        t.registerApi("civ", "grantFreeGreatPerson", luaFunction {
            civ.greatPeople.freeGreatPeople++
            LuaValue.NIL
        })
        t.registerApi("civ", "setLeaderTitle", luaFunction { args ->
            civ.leaderTitle = args.arg(1).tojstring()
            LuaValue.NIL
        })
        t.registerApi("civ", "addNotification", luaFunction { args ->
            civ.addNotification(args.arg(1).tojstring(), NotificationCategory.General)
            LuaValue.NIL
        })
        t.registerApi("civ", "addNotificationAt", luaFunction { args ->
            civ.addNotification(args.arg(1).tojstring(), HexCoord(args.arg(2).safeToInt(), args.arg(3).safeToInt()), NotificationCategory.General)
            LuaValue.NIL
        })
        t.registerApi("civ", "addFreeTech", luaFunction {
            civ.tech.freeTechs++
            LuaValue.NIL
        })

        // Unit creation
        t.registerApi("civ", "addUnit", luaFunction { args ->
            val unitName = args.arg(1).tojstring()
            val baseUnit = civ.gameInfo.ruleset.units[unitName] ?: return@luaFunction LuaValue.FALSE
            val placedUnit = civ.units.addUnit(civ.getEquivalentUnit(baseUnit), civ.getCapital())
            LuaValue.valueOf(placedUnit != null)
        })
        t.registerApi("civ", "addUnitAtCity", luaFunction { args ->
            val unitName = args.arg(1).tojstring()
            val cityName = args.arg(2).tojstring()
            val city = civ.cities.firstOrNull { it.name == cityName }
                ?: return@luaFunction LuaValue.FALSE
            val baseUnit = civ.gameInfo.ruleset.units[unitName]
                ?: return@luaFunction LuaValue.FALSE
            val placedUnit = civ.units.addUnit(civ.getEquivalentUnit(baseUnit), city)
            LuaValue.valueOf(placedUnit != null)
        })
        t.registerApi("civ", "addUnitAtTile", luaFunction { args ->
            val unitName = args.arg(1).tojstring()
            val x = args.arg(2).safeToInt()
            val y = args.arg(3).safeToInt()
            val baseUnit = civ.gameInfo.ruleset.units[unitName]
                ?: return@luaFunction LuaValue.FALSE
            val placedUnit = civ.units.placeUnitNearTile(HexCoord(x, y), civ.getEquivalentUnit(baseUnit))
            LuaValue.valueOf(placedUnit != null)
        })
        t.registerApi("civ", "addRebelUnit", luaFunction { args ->
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

        t.registerApi("city", "id", LuaValue.valueOf(city.id))
        t.registerApi("city", "name", LuaValue.valueOf(city.name))
        t.registerApi("city", "isCapital", LuaValue.valueOf(city.isCapital()))
        t.registerApi("city", "isCoastal", LuaValue.valueOf(
            city.getCenterTile().neighbors.any { it.isWater }
        ))
        t.registerApi("city", "isPuppet", LuaValue.valueOf(city.isPuppet))
        t.registerApi("city", "isBeingRazed", LuaValue.valueOf(city.isBeingRazed))
        t.registerApi("city", "isConnectedToCapital", LuaValue.valueOf(city.isConnectedToCapital()))

        t.registerApi("city", "population", LuaValue.valueOf(city.population.population))
        t.registerApi("city", "health", LuaValue.valueOf(city.health))

        t.registerApi("city", "getStatYield", luaFunction { args ->
            val stat = Stat.safeValueOf(args.arg(1).tojstring())
            LuaValue.valueOf(
                if (stat != null) (city.cityStats.currentCityStats[stat] ?: 0f).toDouble()
                else 0.0
            )
        })
        t.registerApi("city", "getAllYields", luaFunction {
            val yields = LuaValue.tableOf()
            for (stat in Stat.entries) {
                val v = city.cityStats.currentCityStats[stat]
                if (v != null && v != 0f) yields.set(stat.name, LuaValue.valueOf(v.toDouble()))
            }
            yields
        })

        // Food & growth
        t.registerApi("city", "getFood", luaFunction {
            LuaValue.valueOf((city.cityStats.currentCityStats[Stat.Food] ?: 0f).toDouble())
        })
        t.registerApi("city", "getFoodSurplus", luaFunction {
            LuaValue.valueOf(city.foodForNextTurn())
        })
        t.registerApi("city", "getFoodStorage", luaFunction {
            LuaValue.valueOf(city.population.foodStored)
        })
        t.registerApi("city", "getFoodNeeded", luaFunction {
            LuaValue.valueOf(city.population.getFoodToNextPopulation())
        })

        // Production
        t.registerApi("city", "getProductionProgress", luaFunction {
            LuaValue.valueOf(city.cityConstructions.getWorkDone(city.cityConstructions.currentConstructionName()))
        })
        t.registerApi("city", "getProductionCost", luaFunction {
            val construction = city.cityConstructions.getCurrentConstruction()
            val cost = (construction as? INonPerpetualConstruction)?.getProductionCost(city.civ, city)
            LuaValue.valueOf(cost ?: 0)
        })
        t.registerApi("city", "getTurnsToCompletion", luaFunction {
            LuaValue.valueOf(city.cityConstructions.turnsToConstruction(city.cityConstructions.currentConstructionName()))
        })

        // Military
        t.registerApi("city", "getGarrisonedUnit", luaFunction {
            val garrison = city.getGarrison()
            if (garrison != null) buildUnitTable(garrison) else LuaValue.NIL
        })
        t.registerApi("city", "getStrength", luaFunction {
            LuaValue.valueOf(city.getStrength().toDouble())
        })

        // Population
        t.registerApi("city", "getSpecialistCount", luaFunction {
            LuaValue.valueOf(city.population.getNumberOfSpecialists())
        })
        t.registerApi("city", "getUnemployedCount", luaFunction {
            LuaValue.valueOf(city.population.getFreePopulation())
        })
        t.registerApi("city", "isInResistance", luaFunction {
            LuaValue.valueOf(city.isInResistance())
        })

        t.registerApi("city", "hasBuilding", luaFunction { args ->
            LuaValue.valueOf(city.cityConstructions.containsBuildingOrEquivalent(args.arg(1).tojstring()))
        })
        t.registerApi("city", "getBuiltBuildings", luaFunction {
            val arr = LuaTable()
            city.cityConstructions.getBuiltBuildings().forEachIndexed { i, b ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(b.name))
            }
            arr
        })
        t.registerApi("city", "getBuildingCount", luaFunction {
            LuaValue.valueOf(city.cityConstructions.getBuiltBuildings().count())
        })
        t.registerApi("city", "getWonderCount", luaFunction {
            LuaValue.valueOf(city.cityConstructions.getBuiltBuildings().count { it.isAnyWonder() })
        })
        t.registerApi("city", "getBuiltWonders", luaFunction {
            val arr = LuaTable()
            city.cityConstructions.getBuiltBuildings().filter { it.isAnyWonder() }
                .forEachIndexed { i, b -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(b.name)) }
            arr
        })

        t.registerApi("city", "getPosition", luaFunction {
            val pos = LuaValue.tableOf()
            pos.set("x", LuaValue.valueOf(city.location.x))
            pos.set("y", LuaValue.valueOf(city.location.y))
            pos
        })
        t.registerApi("city", "getCenterTile", luaFunction { buildTileTable(city.getCenterTile(), city.civ) })
        t.registerApi("city", "getTiles", luaFunction {
            val arr = LuaTable()
            city.tiles.forEachIndexed { i, coord ->
                val pt = LuaValue.tableOf()
                pt.set("x", LuaValue.valueOf(coord.x))
                pt.set("y", LuaValue.valueOf(coord.y))
                arr.set(LuaValue.valueOf(i + 1), pt)
            }
            arr
        })

        t.registerApi("city", "getCurrentConstruction", luaFunction {
            LuaValue.valueOf(city.cityConstructions.currentConstructionName())
        })
        t.registerApi("city", "getConstructionQueue", luaFunction {
            val arr = LuaTable()
            city.cityConstructions.constructionQueue.forEachIndexed { i, c ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(c))
            }
            arr
        })

        t.registerApi("city", "getMajorityReligion", luaFunction {
            LuaValue.valueOf(city.religion.getMajorityReligionName() ?: "")
        })
        t.registerApi("city", "isHolyCity", luaFunction {
            LuaValue.valueOf(city.isHolyCity())
        })
        t.registerApi("city", "hasUnique", luaFunction { args ->
            val text = args.arg(1).tojstring()
            LuaValue.valueOf(city.cityConstructions.getBuiltBuildings().any { b -> b.uniqueObjects.any { it.text == text } })
        })

        // Write
        t.registerApi("city", "addPopulation", luaFunction { args ->
            city.population.addPopulation(args.arg(1).safeToInt())
            LuaValue.NIL
        })
        t.registerApi("city", "setPopulation", luaFunction { args ->
            city.population.setPopulation(args.arg(1).safeToInt().coerceAtLeast(1))
            LuaValue.NIL
        })
        t.registerApi("city", "addFood", luaFunction { args ->
            city.addStat(Stat.Food, args.arg(1).safeToInt())
            LuaValue.NIL
        })
        t.registerApi("city", "addProduction", luaFunction { args ->
            city.addStat(Stat.Production, args.arg(1).safeToInt())
            LuaValue.NIL
        })
        t.registerApi("city", "addHealth", luaFunction { args ->
            city.health = (city.health + args.arg(1).safeToInt()).coerceAtLeast(0)
            LuaValue.NIL
        })
        t.registerApi("city", "setName", luaFunction { args ->
            city.name = args.arg(1).tojstring()
            LuaValue.NIL
        })
        t.registerApi("city", "addBuilding", luaFunction { args ->
            val building = city.civ.getEquivalentBuilding(args.arg(1).tojstring())
            if (!city.cityConstructions.containsBuildingOrEquivalent(building.name))
                city.cityConstructions.completeConstruction(building)
            LuaValue.NIL
        })
        t.registerApi("city", "removeBuilding", luaFunction { args ->
            val building = city.cityConstructions.getBuiltBuildings()
                .firstOrNull { it.name == args.arg(1).tojstring() }
            if (building != null) city.cityConstructions.removeBuilding(building)
            LuaValue.NIL
        })
        t.registerApi("city", "sellBuilding", luaFunction { args ->
            val building = city.civ.gameInfo.ruleset.buildings[args.arg(1).tojstring()]
            if (building != null && city.cityConstructions.containsBuildingOrEquivalent(building.name))
                city.sellBuilding(building)
            LuaValue.NIL
        })

        // Production queue
        t.registerApi("city", "setProduction", luaFunction { args ->
            city.cityConstructions.setCurrentConstruction(args.arg(1).tojstring())
            LuaValue.NIL
        })
        t.registerApi("city", "addToQueue", luaFunction { args ->
            city.cityConstructions.addToQueue(args.arg(1).tojstring())
            LuaValue.NIL
        })
        t.registerApi("city", "clearQueue", luaFunction {
            city.cityConstructions.removeAll()
            LuaValue.NIL
        })

        return t
    }
    // endregion

    // region unit
    private fun buildUnitTable(unit: MapUnit): LuaValue {
        val t = LuaValue.tableOf()

        t.registerApi("unit", "id", LuaValue.valueOf(unit.id))
        t.registerApi("unit", "name", LuaValue.valueOf(unit.name))
        t.registerApi("unit", "instanceName", LuaValue.valueOf(unit.instanceName ?: ""))
        t.registerApi("unit", "isCivilian", LuaValue.valueOf(unit.baseUnit.isCivilian()))
        t.registerApi("unit", "isMilitary", LuaValue.valueOf(unit.baseUnit.isMilitary))
        t.registerApi("unit", "isRanged", LuaValue.valueOf(unit.baseUnit.isRanged()))
        t.registerApi("unit", "isEmbarked", LuaValue.valueOf(unit.isEmbarked()))
        t.registerApi("unit", "isFortified", LuaValue.valueOf(unit.isFortified()))
        t.registerApi("unit", "isAutomated", LuaValue.valueOf(unit.isAutomated()))

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
        t.registerApi("unit", "base", baseTable)

        t.registerApi("unit", "health", LuaValue.valueOf(unit.health))
        t.registerApi("unit", "getMaxHealth", luaFunction {
            LuaValue.valueOf(MapUnitCombatant(unit).getMaxHealth())
        })
        t.registerApi("unit", "getDamage", luaFunction {
            LuaValue.valueOf(MapUnitCombatant(unit).getMaxHealth() - unit.health)
        })
        t.registerApi("unit", "getAttacksLeft", luaFunction {
            LuaValue.valueOf(unit.maxAttacksPerTurn() - unit.attacksThisTurn)
        })
        t.registerApi("unit", "getVisibilityRange", luaFunction {
            LuaValue.valueOf(unit.getVisibilityRange())
        })
        t.registerApi("unit", "getAction", luaFunction {
            LuaValue.valueOf(unit.action ?: "")
        })
        t.registerApi("unit", "canAttack", luaFunction { LuaValue.valueOf(unit.canAttack()) })
        t.registerApi("unit", "canPillage", luaFunction {
            LuaValue.valueOf(unit.currentTile.canPillageTile())
        })
        t.registerApi("unit", "isInEnemyTerritory", luaFunction {
            LuaValue.valueOf(unit.currentTile.isEnemyTerritory(unit.civ))
        })
        t.registerApi("unit", "isInFriendlyTerritory", luaFunction {
            LuaValue.valueOf(unit.currentTile.isFriendlyTerritory(unit.civ))
        })
        t.registerApi("unit", "isGreatPerson", luaFunction {
            LuaValue.valueOf(unit.isGreatPerson())
        })
        t.registerApi("unit", "getReligionDisplayName", luaFunction {
            LuaValue.valueOf(unit.getReligionDisplayName() ?: "")
        })
        t.registerApi("unit", "getRange", luaFunction { LuaValue.valueOf(unit.getRange()) })
        t.registerApi("unit", "getMovement", luaFunction { LuaValue.valueOf(unit.getMaxMovement().toDouble()) })
        t.registerApi("unit", "getCurrentMovement", luaFunction {
            LuaValue.valueOf(unit.currentMovement.toDouble())
        })
        t.registerApi("unit", "getXP", luaFunction { LuaValue.valueOf(unit.promotions.XP) })

        t.registerApi("unit", "hasPromotion", luaFunction { args ->
            LuaValue.valueOf(unit.promotions.promotions.contains(args.arg(1).tojstring()))
        })
        t.registerApi("unit", "hasUnique", luaFunction { args ->
            val text = args.arg(1).tojstring()
            LuaValue.valueOf(unit.baseUnit.rulesetUniqueObjects.any { it.text == text }
                || unit.promotions.promotions.any { promoName ->
                    unit.civ.gameInfo.ruleset.unitPromotions[promoName]?.uniqueObjects?.any { it.text == text } == true
                })
        })
        t.registerApi("unit", "getPromotions", luaFunction {
            val arr = LuaTable()
            unit.promotions.promotions.forEachIndexed { i, p ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(p))
            }
            arr
        })
        t.registerApi("unit", "getEraNumber", luaFunction {
            // 单位时代 = 其需求科技所属时代的最小序号（与 `{Era.X}` 单位过滤器的"任一需求科技匹配"语义一致）
            val ruleset = unit.civ.gameInfo.ruleset
            val eraNumbers = unit.baseUnit.requiredTechs()
                .mapNotNull { tech -> ruleset.technologies[tech]?.column?.era }
                .mapNotNull { eraName -> ruleset.eras[eraName]?.eraNumber }
            LuaValue.valueOf(eraNumbers.minOrNull() ?: -1)
        })
        t.registerApi("unit", "getPromotionCount", luaFunction {
            LuaValue.valueOf(unit.promotions.promotions.size)
        })

        t.registerApi("unit", "hasStatus", luaFunction { args ->
            LuaValue.valueOf(unit.hasStatus(args.arg(1).tojstring()))
        })
        t.registerApi("unit", "getStatusTurns", luaFunction { args ->
            val status = unit.getStatus(args.arg(1).tojstring())
            LuaValue.valueOf(status?.turnsLeft ?: 0)
        })

        t.registerApi("unit", "getPosition", luaFunction {
            val pos = LuaValue.tableOf()
            pos.set("x", LuaValue.valueOf(unit.currentTile.position.x))
            pos.set("y", LuaValue.valueOf(unit.currentTile.position.y))
            pos
        })
        t.registerApi("unit", "canMoveTo", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val tile = unit.civ.gameInfo.tileMap[HexCoord(x, y)]
            LuaValue.valueOf(tile != null && unit.movement.canMoveTo(tile))
        })
        t.registerApi("unit", "getOwner", luaFunction {
            LuaValue.valueOf(unit.civ.civName)
        })
        t.registerApi("unit", "isOwnedBy", luaFunction { args ->
            LuaValue.valueOf(unit.civ.civName == args.arg(1).tojstring())
        })

        // Write
        t.registerApi("unit", "healBy", luaFunction { args ->
            unit.healBy(args.arg(1).safeToInt())
            LuaValue.NIL
        })
        t.registerApi("unit", "takeDamage", luaFunction { args ->
            unit.takeDamage(args.arg(1).safeToInt())
            LuaValue.NIL
        })
        t.registerApi("unit", "setHealth", luaFunction { args ->
            val maxHealth = MapUnitCombatant(unit).getMaxHealth()
            unit.health = args.arg(1).safeToInt().coerceIn(0, maxHealth)
            LuaValue.NIL
        })
        t.registerApi("unit", "addXP", luaFunction { args ->
            unit.promotions.XP += args.arg(1).safeToInt()
            LuaValue.NIL
        })
        t.registerApi("unit", "setXP", luaFunction { args ->
            unit.promotions.XP = args.arg(1).safeToInt().coerceAtLeast(0)
            LuaValue.NIL
        })
        t.registerApi("unit", "addPromotion", luaFunction { args ->
            unit.promotions.addPromotion(args.arg(1).tojstring(), true)
            LuaValue.NIL
        })
        t.registerApi("unit", "removePromotion", luaFunction { args ->
            unit.promotions.removePromotion(args.arg(1).tojstring())
            LuaValue.NIL
        })
        t.registerApi("unit", "addMovement", luaFunction { args ->
            unit.useMovementPoints(-args.arg(1).safeToFloat())
            LuaValue.NIL
        })
        t.registerApi("unit", "useMovement", luaFunction { args ->
            unit.useMovementPoints(args.arg(1).safeToFloat())
            LuaValue.NIL
        })
        t.registerApi("unit", "setStatus", luaFunction { args ->
            unit.setStatus(args.arg(1).tojstring(), args.arg(2).safeToInt())
            LuaValue.NIL
        })
        t.registerApi("unit", "setAttacksLeft", luaFunction { args ->
            val n = args.arg(1).safeToInt().coerceIn(0, unit.maxAttacksPerTurn())
            unit.attacksThisTurn = unit.maxAttacksPerTurn() - n
            LuaValue.NIL
        })
        t.registerApi("unit", "fortify", luaFunction {
            unit.fortify()
            LuaValue.NIL
        })
        t.registerApi("unit", "moveByPath", luaFunction { args ->
            val path = args.arg(1).checktable()
            var steps = 0
            var i = 1
            while (true) {
                val node = path.get(LuaValue.valueOf(i))
                if (node.isnil()) break
                val x = node.get("x").safeToInt()
                val y = node.get("y").safeToInt()
                val target = unit.civ.gameInfo.tileMap[HexCoord(x, y)] ?: break
                if (unit.currentTile == target) {
                    i++
                    continue
                }
                if (!unit.movement.canMoveTo(target)) break
                unit.movement.moveToTile(target)
                steps++
                i++
            }
            LuaValue.valueOf(steps)
        })
        t.registerApi("unit", "upgrade", luaFunction {
            val upgradeAction = UnitActionsUpgrade.getFreeUpgradeAction(unit)
            if (upgradeAction.any()) {
                upgradeAction.minBy { (it as UpgradeUnitAction).unitToUpgradeTo.cost }.action?.invoke()
            }
            LuaValue.NIL
        })
        t.registerApi("unit", "destroy", luaFunction {
            unit.destroy()
            LuaValue.NIL
        })
        t.registerApi("unit", "attackTile", luaFunction { args ->
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
        t.registerApi("unit", "teleportTo", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val target = unit.civ.gameInfo.tileMap[HexCoord(x, y)]
            if (target != null) unit.movement.moveToTile(target)
            LuaValue.NIL
        })

        // Pathfinding
        t.registerApi("unit", "findPathTo", luaFunction { args ->
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
        t.registerApi("unit", "canReach", luaFunction { args ->
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
        t.registerApi("tile", "position", pos)
        t.registerApi("tile", "getX", luaFunction { LuaValue.valueOf(tile.position.x) })
        t.registerApi("tile", "getY", luaFunction { LuaValue.valueOf(tile.position.y) })

        t.registerApi("tile", "baseTerrain", LuaValue.valueOf(tile.baseTerrain))
        t.registerApi("tile", "isLand", LuaValue.valueOf(tile.isLand))
        t.registerApi("tile", "isWater", LuaValue.valueOf(tile.isWater))
        t.registerApi("tile", "isCoast", LuaValue.valueOf(tile.baseTerrain == "Coast"))
        t.registerApi("tile", "isHill", LuaValue.valueOf(tile.isHill()))
        t.registerApi("tile", "isMountain", LuaValue.valueOf(tile.isImpassible()))
        t.registerApi("tile", "hasTerrainFeature", luaFunction { args ->
            LuaValue.valueOf(tile.terrainFeatures.contains(args.arg(1).tojstring()))
        })
        t.registerApi("tile", "getTerrainFeatures", luaFunction {
            val arr = LuaTable()
            tile.terrainFeatures.sorted().forEachIndexed { i, f ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(f))
            }
            arr
        })
        t.registerApi("tile", "isImpassable", luaFunction { LuaValue.valueOf(tile.isImpassible()) })
        t.registerApi("tile", "isRiver", luaFunction { LuaValue.valueOf(tile.neighbors.any { tile.isConnectedByRiver(it) }) })
        t.registerApi("tile", "isAdjacentToCoast", luaFunction { LuaValue.valueOf(tile.isAdjacentToCoast()) })
        t.registerApi("tile", "hasRoad", luaFunction { LuaValue.valueOf(tile.roadStatus == RoadStatus.Road) })
        t.registerApi("tile", "hasRailroad", luaFunction { LuaValue.valueOf(tile.roadStatus == RoadStatus.Railroad) })
        t.registerApi("tile", "hasNaturalWonder", luaFunction { LuaValue.valueOf(tile.naturalWonder != null) })
        t.registerApi("tile", "getNaturalWonder", luaFunction { LuaValue.valueOf(tile.naturalWonder ?: "") })

        t.registerApi("tile", "hasResource", luaFunction { LuaValue.valueOf(tile.tileResource != null) })
        t.registerApi("tile", "resourceName", LuaValue.valueOf(tile.tileResource?.name ?: ""))
        t.registerApi("tile", "resourceAmount", LuaValue.valueOf(tile.resourceAmount))
        t.registerApi("tile", "hasImprovement", luaFunction { LuaValue.valueOf(tile.tileImprovement != null) })
        t.registerApi("tile", "improvementName", LuaValue.valueOf(tile.improvement ?: ""))
        t.registerApi("tile", "isPillaged", luaFunction { LuaValue.valueOf(tile.isPillaged()) })

        t.registerApi("tile", "getYield", luaFunction {
            val result = LuaValue.tableOf()
            val stats = tile.stats.getTileStats(civInfo)
            for (stat in Stat.entries) {
                val v = stats[stat]
                if (v != 0f) result.set(stat.name, LuaValue.valueOf(v.toDouble()))
            }
            result
        })

        t.registerApi("tile", "isOwned", luaFunction { LuaValue.valueOf(tile.getOwner() != null) })
        t.registerApi("tile", "getOwner", luaFunction { LuaValue.valueOf(tile.getOwner()?.civName ?: "") })
        t.registerApi("tile", "isOwnedBy", luaFunction { args ->
            LuaValue.valueOf(tile.getOwner()?.civName == args.arg(1).tojstring())
        })
        t.registerApi("tile", "isFriendlyTerritory", luaFunction { args ->
            val other = civInfo.gameInfo.getCivilizationOrNull(args.arg(1).tojstring())
            LuaValue.valueOf(other != null && tile.isFriendlyTerritory(other))
        })
        t.registerApi("tile", "isEnemyTerritory", luaFunction { args ->
            val other = civInfo.gameInfo.getCivilizationOrNull(args.arg(1).tojstring())
            LuaValue.valueOf(other != null && tile.isEnemyTerritory(other))
        })
        t.registerApi("tile", "getDistanceTo", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val other = civInfo.gameInfo.tileMap[HexCoord(x, y)]
            LuaValue.valueOf(if (other != null) tile.aerialDistanceTo(other) else -1)
        })
        t.registerApi("tile", "isAdjacentTo", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            LuaValue.valueOf(tile.neighbors.any { it.position == HexCoord(x, y) })
        })
        t.registerApi("tile", "isCityCenter", luaFunction { LuaValue.valueOf(tile.isCityCenter()) })
        t.registerApi("tile", "getOwningCity", luaFunction {
            LuaValue.valueOf(tile.owningCity?.name ?: "")
        })

        t.registerApi("tile", "isExploredBy", luaFunction { args ->
            val other = civInfo.gameInfo.getCivilizationOrNull(args.arg(1).tojstring())
            LuaValue.valueOf(other != null && tile.isExplored(other))
        })
        t.registerApi("tile", "setExplored", luaFunction { args ->
            val other = civInfo.gameInfo.getCivilizationOrNull(args.arg(1).tojstring())
            if (other != null) tile.setExplored(other, args.arg(2).toboolean())
            LuaValue.NIL
        })

        t.registerApi("tile", "hasMilitaryUnit", luaFunction { LuaValue.valueOf(tile.militaryUnit != null) })
        t.registerApi("tile", "hasCivilianUnit", luaFunction { LuaValue.valueOf(tile.civilianUnit != null) })
        t.registerApi("tile", "getUnits", luaFunction {
            val arr = LuaTable()
            tile.getUnits().forEachIndexed { i, u -> arr.set(LuaValue.valueOf(i + 1), buildUnitTable(u)) }
            arr
        })

        t.registerApi("tile", "getNeighbors", luaFunction {
            val arr = LuaTable()
            tile.neighbors.forEachIndexed { i, n -> arr.set(LuaValue.valueOf(i + 1), buildTileTable(n, civInfo)) }
            arr
        })
        t.registerApi("tile", "getNeighborAt", luaFunction { args ->
            val dir = args.arg(1).safeToInt()
            val neighbors = tile.neighbors.toList()
            if (dir in neighbors.indices) buildTileTable(neighbors[dir], civInfo) else LuaValue.NIL
        })
        t.registerApi("tile", "getTilesInDistance", luaFunction { args ->
            val radius = args.arg(1).safeToInt()
            val arr = LuaTable()
            tile.getTilesInDistance(radius).forEachIndexed { i, t2 -> arr.set(LuaValue.valueOf(i + 1), buildTileTable(t2, civInfo)) }
            arr
        })

        // Write
        t.registerApi("tile", "setTerrain", luaFunction { args ->
            val terrain = civInfo.gameInfo.ruleset.terrains[args.arg(1).tojstring()]
            if (terrain != null) tile.setBaseTerrain(terrain)
            LuaValue.NIL
        })
        t.registerApi("tile", "addTerrainFeature", luaFunction { args ->
            tile.addTerrainFeature(args.arg(1).tojstring())
            LuaValue.NIL
        })
        t.registerApi("tile", "removeTerrainFeature", luaFunction { args ->
            tile.removeTerrainFeature(args.arg(1).tojstring())
            LuaValue.NIL
        })
        t.registerApi("tile", "setImprovement", luaFunction { args ->
            val improvement = civInfo.gameInfo.ruleset.tileImprovements[args.arg(1).tojstring()]
            if (improvement != null) tile.setImprovement(improvement)
            LuaValue.NIL
        })
        t.registerApi("tile", "removeImprovement", luaFunction {
            tile.removeImprovement()
            LuaValue.NIL
        })
        t.registerApi("tile", "removeResource", luaFunction {
            tile.tileResource = null
            tile.resourceAmount = 0
            LuaValue.NIL
        })
        t.registerApi("tile", "setResource", luaFunction { args ->
            val resource = civInfo.gameInfo.ruleset.tileResources[args.arg(1).tojstring()]
            val amount = args.arg(2).safeToInt()
            tile.tileResource = resource
            tile.resourceAmount = amount
            LuaValue.NIL
        })
        t.registerApi("tile", "setRoad", luaFunction {
            val roadStatus = com.unciv.logic.map.tile.RoadStatus.Road
            tile.setRoadStatus(roadStatus, civInfo)
            LuaValue.NIL
        })
        t.registerApi("tile", "setRailroad", luaFunction {
            val roadStatus = com.unciv.logic.map.tile.RoadStatus.Railroad
            tile.setRoadStatus(roadStatus, civInfo)
            LuaValue.NIL
        })
        t.registerApi("tile", "removeRoad", luaFunction {
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

        t.registerApi("game", "turn", LuaValue.valueOf(gameInfo.turns))
        t.registerApi("game", "getYear", luaFunction { LuaValue.valueOf(gameInfo.getYear(0)) })
        t.registerApi("game", "speed", LuaValue.valueOf(gameInfo.speed.name))
        t.registerApi("game", "difficulty", LuaValue.valueOf(gameInfo.difficulty))
        t.registerApi("game", "getCurrentPlayer", luaFunction {
            try {
                LuaValue.valueOf(gameInfo.currentPlayerCiv.civName)
            } catch (e: Exception) {
                LuaValue.NIL
            }
        })
        t.registerApi("game", "getCurrentPlayerCiv", luaFunction {
            try {
                buildCivTable(gameInfo.currentPlayerCiv)
            } catch (e: Exception) {
                LuaValue.NIL
            }
        })

        // Civ queries
        t.registerApi("game", "getCiv", luaFunction { args ->
            val name = args.arg(1).tojstring()
            val civ = gameInfo.civilizations.firstOrNull { it.civName == name }
            if (civ != null) buildCivTable(civ) else LuaValue.NIL
        })
        t.registerApi("game", "getCivById", luaFunction { args ->
            val id = args.arg(1).tojstring()
            val civ = gameInfo.getCivilizationOrNull(id)
            if (civ != null) buildCivTable(civ) else LuaValue.NIL
        })
        t.registerApi("game", "getAllCivs", luaFunction {
            val arr = LuaTable()
            gameInfo.civilizations.forEachIndexed { i, c -> arr.set(LuaValue.valueOf(i + 1), buildCivTable(c)) }
            arr
        })
        t.registerApi("game", "getCivNames", luaFunction {
            val arr = LuaTable()
            gameInfo.civilizations.forEachIndexed { i, c -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(c.civName)) }
            arr
        })
        t.registerApi("game", "getHumanCivs", luaFunction {
            val arr = LuaTable()
            gameInfo.civilizations.filter { it.isHuman() }
                .forEachIndexed { i, c -> arr.set(LuaValue.valueOf(i + 1), buildCivTable(c)) }
            arr
        })
        t.registerApi("game", "getAliveMajorCivs", luaFunction {
            val arr = LuaTable()
            gameInfo.getAliveMajorCivs().forEachIndexed { i, c -> arr.set(LuaValue.valueOf(i + 1), buildCivTable(c)) }
            arr
        })
        t.registerApi("game", "getAliveCityStates", luaFunction {
            val arr = LuaTable()
            gameInfo.getAliveCityStates().forEachIndexed { i, c -> arr.set(LuaValue.valueOf(i + 1), buildCivTable(c)) }
            arr
        })
        t.registerApi("game", "getBarbarianCiv", luaFunction {
            buildCivTable(gameInfo.getBarbarianCivilization())
        })

        // Map
        val ruleset = gameInfo.ruleset
        t.registerApi("game", "getTile", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val tile = gameInfo.tileMap[HexCoord(x, y)]
            if (tile != null) buildTileTable(tile, civInfo) else LuaValue.NIL
        })
        t.registerApi("game", "findTiles", luaFunction { args ->
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
        t.registerApi("game", "getMapWidth", luaFunction {
            LuaValue.valueOf(gameInfo.tileMap.mapParameters.mapSize.width)
        })
        t.registerApi("game", "getMapHeight", luaFunction {
            LuaValue.valueOf(gameInfo.tileMap.mapParameters.mapSize.height)
        })
        t.registerApi("game", "getMapName", luaFunction {
            LuaValue.valueOf(gameInfo.tileMap.mapParameters.name)
        })
        t.registerApi("game", "getMapType", luaFunction {
            LuaValue.valueOf(gameInfo.tileMap.mapParameters.type)
        })
        t.registerApi("game", "isWrapped", luaFunction {
            LuaValue.valueOf(gameInfo.tileMap.mapParameters.worldWrap)
        })
        t.registerApi("game", "getTilesNear", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val radius = args.arg(3).safeToInt()
            val center = gameInfo.tileMap[HexCoord(x, y)] ?: return@luaFunction LuaValue.NIL
            val arr = LuaTable()
            center.getTilesInDistance(radius).forEachIndexed { i, t2 -> arr.set(LuaValue.valueOf(i + 1), buildTileTable(t2, civInfo)) }
            arr
        })

        // Ruleset queries
        t.registerApi("game", "getRulesetBuildings", luaFunction {
            val arr = LuaTable()
            ruleset.buildings.keys.forEachIndexed { i, k -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(k)) }
            arr
        })
        t.registerApi("game", "getRulesetUnits", luaFunction {
            val arr = LuaTable()
            ruleset.units.keys.forEachIndexed { i, k -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(k)) }
            arr
        })
        t.registerApi("game", "getRulesetTechs", luaFunction {
            val arr = LuaTable()
            ruleset.technologies.keys.forEachIndexed { i, k -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(k)) }
            arr
        })
        t.registerApi("game", "getRulesetPolicies", luaFunction {
            val arr = LuaTable()
            ruleset.policies.keys.forEachIndexed { i, k -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(k)) }
            arr
        })
        t.registerApi("game", "getRulesetEras", luaFunction {
            val arr = LuaTable()
            ruleset.eras.keys.forEachIndexed { i, k -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(k)) }
            arr
        })
        t.registerApi("game", "getRulesetPromotions", luaFunction {
            val arr = LuaTable()
            ruleset.unitPromotions.keys.forEachIndexed { i, k -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(k)) }
            arr
        })
        t.registerApi("game", "getRulesetTerrains", luaFunction {
            stringList(ruleset.terrains.keys)
        })
        t.registerApi("game", "getRulesetResources", luaFunction {
            stringList(ruleset.tileResources.keys)
        })
        t.registerApi("game", "getRulesetImprovements", luaFunction {
            stringList(ruleset.tileImprovements.keys)
        })
        t.registerApi("game", "getRulesetNations", luaFunction {
            stringList(ruleset.nations.keys)
        })
        t.registerApi("game", "getRulesetReligions", luaFunction {
            stringList(ruleset.religions)
        })
        t.registerApi("game", "getRulesetBeliefs", luaFunction {
            stringList(ruleset.beliefs.keys)
        })
        t.registerApi("game", "getRulesetEvents", luaFunction {
            stringList(ruleset.events.keys)
        })
        t.registerApi("game", "getRulesetNaturalWonders", luaFunction {
            stringList(gameInfo.tileMap.naturalWonders)
        })
        t.registerApi("game", "getRulesetUnitTypes", luaFunction {
            stringList(ruleset.unitTypes.keys)
        })
        t.registerApi("game", "getEraNames", luaFunction {
            stringList(ruleset.eras.keys)
        })
        t.registerApi("game", "getVictoryTypes", luaFunction {
            stringList(gameInfo.gameParameters.victoryTypes)
        })
        t.registerApi("game", "getMods", luaFunction {
            stringList(gameInfo.gameParameters.mods)
        })
        t.registerApi("game", "getBaseRuleset", luaFunction {
            LuaValue.valueOf(gameInfo.gameParameters.baseRuleset)
        })
        t.registerApi("game", "doesBuildingExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.buildings.containsKey(args.arg(1).tojstring()))
        })
        t.registerApi("game", "doesUnitExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.units.containsKey(args.arg(1).tojstring()))
        })
        t.registerApi("game", "doesTechExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.technologies.containsKey(args.arg(1).tojstring()))
        })
        t.registerApi("game", "doesPolicyExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.policies.containsKey(args.arg(1).tojstring()))
        })
        t.registerApi("game", "doesEraExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.eras.containsKey(args.arg(1).tojstring()))
        })
        t.registerApi("game", "doesPromotionExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.unitPromotions.containsKey(args.arg(1).tojstring()))
        })
        t.registerApi("game", "doesTerrainExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.terrains.containsKey(args.arg(1).tojstring()))
        })
        t.registerApi("game", "doesResourceExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.tileResources.containsKey(args.arg(1).tojstring()))
        })
        t.registerApi("game", "doesImprovementExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.tileImprovements.containsKey(args.arg(1).tojstring()))
        })
        t.registerApi("game", "doesNationExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.nations.containsKey(args.arg(1).tojstring()))
        })
        t.registerApi("game", "doesBeliefExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.beliefs.containsKey(args.arg(1).tojstring()))
        })
        t.registerApi("game", "doesEventExist", luaFunction { args ->
            LuaValue.valueOf(ruleset.events.containsKey(args.arg(1).tojstring()))
        })

        // Write
        t.registerApi("game", "addGlobalNotification", luaFunction { args ->
            val text = args.arg(1).tojstring()
            for (c in gameInfo.civilizations) {
                if (c.isHuman() && c.isAlive())
                    c.addNotification(text, NotificationCategory.General)
            }
            LuaValue.NIL
        })
        t.registerApi("game", "revealEntireMap", luaFunction { args ->
            val name = args.arg(1).tojstring()
            val civ = gameInfo.civilizations.firstOrNull { it.civName == name }
            if (civ != null) gameInfo.tileMap.values.forEach { it.setExplored(civ, true) }
            LuaValue.NIL
        })
        t.registerApi("game", "revealTilesAround", luaFunction { args ->
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
    /** Builds a 1-based Lua array table from a collection of strings. */
    private fun stringList(values: Collection<String>): LuaValue {
        val arr = LuaTable()
        values.forEachIndexed { i, v -> arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(v)) }
        return arr
    }

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

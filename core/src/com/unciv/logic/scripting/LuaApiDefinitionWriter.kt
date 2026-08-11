package com.unciv.logic.scripting

import java.io.File

/**
 * Generates an EmmyLua-compatible type definition file for the mod Lua API, consumed by
 * the LuaLS language server (VSCode extension "Lua" by sumneko) so mod authors get
 * autocompletion, hover docs and immediate method-name checking while writing scripts.
 *
 * The file is generated from [LuaAPI.apiCatalog]; every API name in the catalog appears in
 * the output (verified by tests), so the definitions can never drift from the implementation.
 * Parameter/return signatures are a curated best-effort: precise for the common patterns,
 * `fun(...)` for the rest. Run via `./gradlew desktop:generateDocs` - never edit the
 * generated file by hand.
 */
class LuaApiDefinitionWriter {

    companion object {
        /** Output path relative to the assets dir (the generateDocs task's working dir). */
        private const val outputFileName = "../../docs/Modders/lua-api.lua"

        /** Precise signatures per context table, for APIs whose shape can't be inferred from the name. */
        private val explicitSignatures: Map<String, Map<String, String>> = mapOf(
            "ctx" to mapOf(
                "parameter" to "string",
                "civ" to "UncivCiv",
                "city" to "UncivCity",
                "unit" to "UncivUnit",
                "tile" to "UncivTile",
                "game" to "UncivGame",
                "store" to "UncivStore",
                "log" to "fun(msg: string)",
                "count" to "fun(expr: string): string",
                "evaluateConditional" to "fun(condition: string): boolean",
                "random" to "fun(): number",
                "randomInt" to "fun(min: number, max: number): number"
            ),
            "store" to mapOf(
                "get" to "fun(key: string, default: string?): string",
                "set" to "fun(key: string, value: string)"
            ),
            "civ" to mapOf(
                "id" to "string",
                "name" to "string",
                "isHuman" to "boolean",
                "isAI" to "boolean",
                "isAlive" to "boolean",
                "isMajorCiv" to "boolean",
                "isCityState" to "boolean",
                "isBarbarian" to "boolean",
                "isSpectator" to "boolean",

                "getNation" to "fun(): string",
                "getLeaderName" to "fun(): string",
                "getScore" to "fun(): number",
                "getForce" to "fun(): number",
                "getGold" to "fun(): number",
                "getHappiness" to "fun(): number",
                "getStat" to "fun(stat: string): number",
                "getStatYield" to "fun(stat: string): number",
                "getGoldPerTurn" to "fun(): number",

                "getSciencePerTurn" to "fun(): number",
                "getCulturePerTurn" to "fun(): number",
                "getFoodPerTurn" to "fun(): number",
                "getProductionPerTurn" to "fun(): number",
                "getResourceAmount" to "fun(resourceName: string): number",
                "hasResource" to "fun(resourceName: string): boolean",

                "getResourceStockpiles" to "fun(): table",
                "getEra" to "fun(): string",
                "getEraNumber" to "fun(): number",
                "isResearched" to "fun(techName: string): boolean",
                "canResearch" to "fun(techName: string): boolean",
                "getResearchingTech" to "fun(): string",
                "getResearchProgress" to "fun(techName: string): number",
                "getTechCount" to "fun(): number",
                "getTechsResearched" to "fun(): string[]",
                "getAvailableTechs" to "fun(): string[]",

                "getTechCost" to "fun(techName: string): number",
                "grantTech" to "fun(techName: string)",
                "discoverTech" to "fun(techName: string)",
                "hasPolicy" to "fun(policyName: string): boolean",
                "canAdoptPolicy" to "fun(): boolean",
                "getAdoptedPolicyCount" to "fun(): number",
                "getAdoptedPolicies" to "fun(): string[]",
                "getAvailablePolicyBranches" to "fun(): string[]",
                "grantPolicy" to "fun(policyName: string)",

                "getCultureNeededForNextPolicy" to "fun(): number",
                "isAtWarWith" to "fun(civName: string): boolean",
                "hasOpenBordersWith" to "fun(civName: string): boolean",
                "isAlliedWith" to "fun(civName: string): boolean",
                "getDiplomaticStatus" to "fun(civName: string): string",

                "getDiplomaticStatuses" to "fun(): table",
                "getProximityTo" to "fun(civName: string): string",
                "hasEmbassyWith" to "fun(civName: string): boolean",
                "getInfluence" to "fun(civName: string): number",
                "getKnownCivs" to "fun(): string[]",
                "addInfluence" to "fun(civName: string, amount: number)",
                "declareWarOn" to "fun(civName: string)",

                "makePeaceWith" to "fun(civName: string)",
                "hasReligion" to "fun(): boolean",
                "getReligionName" to "fun(): string",
                "getFaith" to "fun(): number",
                "getCities" to "fun(): UncivCity[]",
                "getCity" to "fun(cityName: string): UncivCity|nil",
                "getCapital" to "fun(): UncivCity|nil",
                "getCityCount" to "fun(): number",
                "getCityNames" to "fun(): string[]",
                "getTotalPopulation" to "fun(): number",
                "getWondersBuilt" to "fun(): string[]",
                "getUnits" to "fun(): UncivUnit[]",
                "getUnitsMatching" to "fun(filter: string): UncivUnit[]",
                "getUnitCount" to "fun(): number",
                "isGoldenAge" to "fun(): boolean",
                "getGoldenAgeTurnsRemaining" to "fun(): number",
                "getSpyCount" to "fun(): number",

                "getSpies" to "fun(): table[]",
                "addSpy" to "fun()",
                "getLeaderTitle" to "fun(): string",
                "hasUnique" to "fun(uniqueText: string): boolean",
                "addGold" to "fun(amount: number)",

                "setGold" to "fun(amount: number)",
                "addStat" to "fun(stat: string, amount: number)",
                "addStats" to "fun(statsText: string)",
                "addResource" to "fun(resourceName: string, amount: number)",
                "consumeResource" to "fun(resourceName: string, amount: number)",
                "triggerGoldenAge" to "fun(turns: number?)",
                "grantFreeGreatPerson" to "fun()",
                "setLeaderTitle" to "fun(title: string)",
                "addNotification" to "fun(text: string)",
                "addNotificationAt" to "fun(text: string, x: number, y: number)",
                "addFreeTech" to "fun()",
                "addUnit" to "fun(unitName: string): boolean",
                "addUnitAtCity" to "fun(unitName: string, cityName: string): boolean",
                "addUnitAtTile" to "fun(unitName: string, x: number, y: number): boolean",
                "addRebelUnit" to "fun(unitName: string): boolean"
            ),
            "city" to mapOf(
                "id" to "string",
                "name" to "string",
                "isCapital" to "boolean",
                "isCoastal" to "boolean",
                "isPuppet" to "boolean",
                "isBeingRazed" to "boolean",
                "isConnectedToCapital" to "boolean",
                "population" to "number",
                "health" to "number",
                "getStatYield" to "fun(stat: string): number",
                "getAllYields" to "fun(): table",

                "getFood" to "fun(): number",
                "getFoodSurplus" to "fun(): number",
                "getFoodStorage" to "fun(): number",
                "getFoodNeeded" to "fun(): number",
                "getProductionProgress" to "fun(): number",
                "getProductionCost" to "fun(): number",
                "getTurnsToCompletion" to "fun(): number",
                "getGarrisonedUnit" to "fun(): UncivUnit|nil",
                "getStrength" to "fun(): number",
                "getSpecialistCount" to "fun(): number",
                "getUnemployedCount" to "fun(): number",
                "getBuiltWonders" to "fun(): string[]",
                "isInResistance" to "fun(): boolean",
                "hasBuilding" to "fun(buildingName: string): boolean",
                "getBuiltBuildings" to "fun(): string[]",
                "getBuildingCount" to "fun(): number",
                "getWonderCount" to "fun(): number",
                "getPosition" to "fun(): table",
                "getCenterTile" to "fun(): UncivTile",
                "getTiles" to "fun(): table",
                "getCurrentConstruction" to "fun(): string",
                "getConstructionQueue" to "fun(): string[]",
                "getMajorityReligion" to "fun(): string",
                "isHolyCity" to "fun(): boolean",
                "hasUnique" to "fun(uniqueText: string): boolean",
                "addPopulation" to "fun(amount: number)",

                "setPopulation" to "fun(count: number)",
                "addFood" to "fun(amount: number)",
                "addProduction" to "fun(amount: number)",
                "addHealth" to "fun(amount: number)",
                "setName" to "fun(newName: string)",
                "addBuilding" to "fun(buildingName: string)",
                "removeBuilding" to "fun(buildingName: string)",

                "sellBuilding" to "fun(buildingName: string)",
                "setProduction" to "fun(itemName: string)",
                "addToQueue" to "fun(itemName: string)",
                "clearQueue" to "fun()"
            ),
            "unit" to mapOf(
                "id" to "string",
                "name" to "string",
                "instanceName" to "string",
                "isCivilian" to "boolean",
                "isMilitary" to "boolean",
                "isRanged" to "boolean",
                "isEmbarked" to "boolean",
                "isFortified" to "boolean",
                "isAutomated" to "boolean",
                "base" to "table",
                "health" to "number",
                "getRange" to "fun(): number",
                "getEraNumber" to "fun(): number",
                "getMovement" to "fun(): number",
                "getCurrentMovement" to "fun(): number",
                "getXP" to "fun(): number",

                "getMaxHealth" to "fun(): number",
                "getDamage" to "fun(): number",
                "getAttacksLeft" to "fun(): number",
                "getVisibilityRange" to "fun(): number",
                "getAction" to "fun(): string",
                "canAttack" to "fun(): boolean",
                "canPillage" to "fun(): boolean",
                "isInEnemyTerritory" to "fun(): boolean",
                "isInFriendlyTerritory" to "fun(): boolean",
                "isGreatPerson" to "fun(): boolean",
                "getReligionDisplayName" to "fun(): string",
                "hasPromotion" to "fun(promotionName: string): boolean",
                "hasUnique" to "fun(uniqueText: string): boolean",
                "getPromotions" to "fun(): string[]",
                "getPromotionCount" to "fun(): number",
                "hasStatus" to "fun(statusName: string): boolean",
                "getStatusTurns" to "fun(statusName: string): number",
                "getPosition" to "fun(): table",
                "canMoveTo" to "fun(x: number, y: number): boolean",
                "getOwner" to "fun(): string",
                "isOwnedBy" to "fun(civName: string): boolean",

                "isFriendlyTerritory" to "fun(civName: string): boolean",
                "isEnemyTerritory" to "fun(civName: string): boolean",
                "healBy" to "fun(amount: number)",
                "takeDamage" to "fun(amount: number)",
                "addXP" to "fun(amount: number)",

                "setXP" to "fun(amount: number)",
                "setHealth" to "fun(amount: number)",
                "addPromotion" to "fun(promotionName: string)",
                "removePromotion" to "fun(promotionName: string)",
                "addMovement" to "fun(amount: number)",
                "useMovement" to "fun(amount: number)",

                "setStatus" to "fun(statusName: string, turns: number)",
                "setAttacksLeft" to "fun(count: number)",
                "fortify" to "fun()",
                "moveByPath" to "fun(path: table): number",
                "upgrade" to "fun()",
                "destroy" to "fun()",
                "attackTile" to "fun(x: number, y: number): table|false",
                "teleportTo" to "fun(x: number, y: number)",
                "findPathTo" to "fun(x: number, y: number): table|nil",
                "canReach" to "fun(x: number, y: number): boolean"
            ),
            "tile" to mapOf(
                "position" to "table",
                "baseTerrain" to "string",
                "isLand" to "boolean",
                "isWater" to "boolean",
                "isCoast" to "boolean",
                "isHill" to "boolean",
                "isMountain" to "boolean",
                "resourceName" to "string",
                "resourceAmount" to "number",
                "improvementName" to "string",
                "getX" to "fun(): number",
                "getY" to "fun(): number",
                "hasTerrainFeature" to "fun(featureName: string): boolean",
                "getTerrainFeatures" to "fun(): string[]",
                "isImpassable" to "fun(): boolean",
                "isRiver" to "fun(): boolean",

                "isAdjacentToCoast" to "fun(): boolean",
                "hasRoad" to "fun(): boolean",
                "hasRailroad" to "fun(): boolean",
                "hasNaturalWonder" to "fun(): boolean",
                "getNaturalWonder" to "fun(): string",
                "hasResource" to "fun(): boolean",
                "hasImprovement" to "fun(): boolean",
                "isPillaged" to "fun(): boolean",
                "getYield" to "fun(): table",
                "isOwned" to "fun(): boolean",
                "getOwner" to "fun(): string",
                "isOwnedBy" to "fun(civName: string): boolean",
                "isCityCenter" to "fun(): boolean",
                "getOwningCity" to "fun(): string",
                "isExploredBy" to "fun(civName: string): boolean",

                "getDistanceTo" to "fun(x: number, y: number): number",
                "isAdjacentTo" to "fun(x: number, y: number): boolean",
                "hasMilitaryUnit" to "fun(): boolean",
                "hasCivilianUnit" to "fun(): boolean",
                "getUnits" to "fun(): UncivUnit[]",
                "getNeighbors" to "fun(): UncivTile[]",
                "getNeighborAt" to "fun(direction: number): UncivTile|nil",
                "getTilesInDistance" to "fun(radius: number): UncivTile[]",

                "setExplored" to "fun(civName: string, explored: boolean)",
                "setTerrain" to "fun(terrainName: string)",
                "addTerrainFeature" to "fun(featureName: string)",
                "removeTerrainFeature" to "fun(featureName: string)",
                "setImprovement" to "fun(improvementName: string)",
                "removeImprovement" to "fun()",
                "removeResource" to "fun()",
                "setResource" to "fun(resourceName: string, amount: number)",
                "setRoad" to "fun()",
                "setRailroad" to "fun()",
                "removeRoad" to "fun()"
            ),
            "game" to mapOf(
                "turn" to "number",
                "speed" to "string",
                "difficulty" to "string",
                "getYear" to "fun(): number",
                "getCurrentPlayer" to "fun(): string",

                "getCurrentPlayerCiv" to "fun(): UncivCiv|nil",
                "getCiv" to "fun(civName: string): UncivCiv|nil",
                "getCivById" to "fun(id: string): UncivCiv|nil",
                "getAllCivs" to "fun(): UncivCiv[]",

                "getCivNames" to "fun(): string[]",
                "getHumanCivs" to "fun(): UncivCiv[]",
                "getAliveMajorCivs" to "fun(): UncivCiv[]",
                "getAliveCityStates" to "fun(): UncivCiv[]",
                "getBarbarianCiv" to "fun(): UncivCiv",
                "getTile" to "fun(x: number, y: number): UncivTile|nil",
                "findTiles" to "fun(criteria: table): UncivTile[]",
                "getMapWidth" to "fun(): number",
                "getMapHeight" to "fun(): number",

                "getMapName" to "fun(): string",
                "getMapType" to "fun(): string",
                "isWrapped" to "fun(): boolean",
                "getTilesNear" to "fun(x: number, y: number, radius: number): UncivTile[]",

                "getEraNames" to "fun(): string[]",
                "getVictoryTypes" to "fun(): string[]",
                "getMods" to "fun(): string[]",
                "getBaseRuleset" to "fun(): string",
                "getRulesetBuildings" to "fun(): string[]",
                "getRulesetUnits" to "fun(): string[]",
                "getRulesetTechs" to "fun(): string[]",
                "getRulesetPolicies" to "fun(): string[]",
                "getRulesetEras" to "fun(): string[]",
                "getRulesetPromotions" to "fun(): string[]",

                "getRulesetTerrains" to "fun(): string[]",
                "getRulesetResources" to "fun(): string[]",
                "getRulesetImprovements" to "fun(): string[]",
                "getRulesetNations" to "fun(): string[]",
                "getRulesetReligions" to "fun(): string[]",
                "getRulesetBeliefs" to "fun(): string[]",
                "getRulesetEvents" to "fun(): string[]",
                "getRulesetNaturalWonders" to "fun(): string[]",
                "getRulesetUnitTypes" to "fun(): string[]",
                "doesBuildingExist" to "fun(buildingName: string): boolean",
                "doesUnitExist" to "fun(unitName: string): boolean",

                "doesTechExist" to "fun(techName: string): boolean",
                "doesPolicyExist" to "fun(policyName: string): boolean",
                "doesEraExist" to "fun(eraName: string): boolean",
                "doesPromotionExist" to "fun(promotionName: string): boolean",
                "doesTerrainExist" to "fun(terrainName: string): boolean",
                "doesResourceExist" to "fun(resourceName: string): boolean",
                "doesImprovementExist" to "fun(improvementName: string): boolean",
                "doesNationExist" to "fun(nationName: string): boolean",
                "doesBeliefExist" to "fun(beliefName: string): boolean",
                "doesEventExist" to "fun(eventName: string): boolean",
                "addGlobalNotification" to "fun(text: string)",
                "revealEntireMap" to "fun(civName: string)",
                "revealTilesAround" to "fun(civName: string, x: number, y: number, radius: number)"
            )
        )

        /** Context table -> generated class name. */
        private val classNameByOwner = mapOf(
            "ctx" to "UncivCtx",
            "civ" to "UncivCiv",
            "city" to "UncivCity",
            "unit" to "UncivUnit",
            "tile" to "UncivTile",
            "game" to "UncivGame",
            "store" to "UncivStore"
        )

        private val ownerOrder = listOf("civ", "city", "unit", "tile", "game", "store", "ctx")

        private fun signatureFor(owner: String, method: String): String {
            explicitSignatures[owner]?.get(method)?.let { return it }
            // Fallback: methods without a curated signature accept anything; plain fields are unknown-typed
            return if (looksLikeMethod(method)) "fun(...)" else "any"
        }

        private fun looksLikeMethod(method: String): Boolean =
            method.startsWith("get") || method.startsWith("is") || method.startsWith("has") ||
                method.startsWith("can") || method.startsWith("add") || method.startsWith("set") ||
                method.startsWith("remove") || method.startsWith("clear") || method.startsWith("trigger") ||
                method.startsWith("declare") || method.startsWith("grant") || method.startsWith("consume") ||
                method.startsWith("use") || method.startsWith("heal") || method.startsWith("take") ||
                method.startsWith("teleport") || method.startsWith("attack") || method.startsWith("find") ||
                method.startsWith("upgrade") || method.startsWith("destroy") || method.startsWith("reveal") ||
                method.startsWith("does") || method.startsWith("evaluate") || method.startsWith("count") ||
                method.startsWith("log")
    }

    /** Pure generation - the tests call this; the docs task writes the result to disk. */
    fun generate(): String {
        val out = StringBuilder()
        out.appendLine("---@meta")
        out.appendLine()
        out.appendLine("-- Unciv Lua 模组 API 类型定义（自动生成，请勿手工编辑）")
        out.appendLine("-- Generated by LuaApiDefinitionWriter - never edit by hand.")
        out.appendLine("-- Regenerate with: ./gradlew desktop:generateDocs")
        out.appendLine("--")
        out.appendLine("-- 用法 / Usage: install the LuaLS language server (VSCode extension \"Lua\" by sumneko),")
        out.appendLine("-- then point workspace.library at this file in .luarc.json - you get autocompletion,")
        out.appendLine("-- hover docs and immediate checking of ctx.civ.addGoldd(...)-style typos.")
        out.appendLine("-- Note: signatures are best-effort; the runtime game (mod checker / mod-ci) is authoritative.")
        out.appendLine()

        for (owner in ownerOrder) {
            val names = LuaAPI.apiCatalog[owner] ?: continue
            out.appendLine("---@class ${classNameByOwner[owner]}")
            for (name in names) {
                out.appendLine("---@field $name ${signatureFor(owner, name)}")
            }
            out.appendLine()
        }
        return out.toString().trimEnd() + "\n"
    }

    /** Writes the generated definitions into the docs folder (generateDocs task, working dir = assets). */
    fun write() {
        File(outputFileName).writeText(generate())
    }
}

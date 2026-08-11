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
                "evaluateConditional" to "fun(condition: string): boolean"
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
                "getGold" to "fun(): number",
                "getHappiness" to "fun(): number",
                "getStat" to "fun(stat: string): number",
                "getStatYield" to "fun(stat: string): number",
                "getGoldPerTurn" to "fun(): number",
                "getResourceAmount" to "fun(resourceName: string): number",
                "hasResource" to "fun(resourceName: string): boolean",
                "getEra" to "fun(): string",
                "getEraNumber" to "fun(): number",
                "isResearched" to "fun(techName: string): boolean",
                "canResearch" to "fun(techName: string): boolean",
                "getResearchingTech" to "fun(): string",
                "getResearchProgress" to "fun(techName: string): number",
                "getTechCount" to "fun(): number",
                "getTechsResearched" to "fun(): string[]",
                "getAvailableTechs" to "fun(): string[]",
                "grantTech" to "fun(techName: string)",
                "hasPolicy" to "fun(policyName: string): boolean",
                "canAdoptPolicy" to "fun(): boolean",
                "getAdoptedPolicyCount" to "fun(): number",
                "getAdoptedPolicies" to "fun(): string[]",
                "getAvailablePolicyBranches" to "fun(): string[]",
                "grantPolicy" to "fun(policyName: string)",
                "isAtWarWith" to "fun(civName: string): boolean",
                "hasOpenBordersWith" to "fun(civName: string): boolean",
                "isAlliedWith" to "fun(civName: string): boolean",
                "getDiplomaticStatus" to "fun(civName: string): string",
                "getInfluence" to "fun(civName: string): number",
                "getKnownCivs" to "fun(): string[]",
                "addInfluence" to "fun(civName: string, amount: number)",
                "declareWarOn" to "fun(civName: string)",
                "hasReligion" to "fun(): boolean",
                "getReligionName" to "fun(): string",
                "getFaith" to "fun(): number",
                "getCities" to "fun(): UncivCity[]",
                "getCity" to "fun(cityName: string): UncivCity|nil",
                "getCapital" to "fun(): UncivCity|nil",
                "getCityCount" to "fun(): number",
                "getUnits" to "fun(): UncivUnit[]",
                "getUnitsMatching" to "fun(filter: string): UncivUnit[]",
                "getUnitCount" to "fun(): number",
                "isGoldenAge" to "fun(): boolean",
                "getGoldenAgeTurnsRemaining" to "fun(): number",
                "getSpyCount" to "fun(): number",
                "getLeaderTitle" to "fun(): string",
                "hasUnique" to "fun(uniqueText: string): boolean",
                "addGold" to "fun(amount: number)",
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
                "addBuilding" to "fun(buildingName: string)",
                "removeBuilding" to "fun(buildingName: string)",
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
                "getMovement" to "fun(): number",
                "getCurrentMovement" to "fun(): number",
                "getXP" to "fun(): number",
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
                "healBy" to "fun(amount: number)",
                "takeDamage" to "fun(amount: number)",
                "addXP" to "fun(amount: number)",
                "addPromotion" to "fun(promotionName: string)",
                "removePromotion" to "fun(promotionName: string)",
                "addMovement" to "fun(amount: number)",
                "useMovement" to "fun(amount: number)",
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
                "hasMilitaryUnit" to "fun(): boolean",
                "hasCivilianUnit" to "fun(): boolean",
                "getUnits" to "fun(): UncivUnit[]",
                "getNeighbors" to "fun(): UncivTile[]",
                "getNeighborAt" to "fun(direction: number): UncivTile|nil",
                "getTilesInDistance" to "fun(radius: number): UncivTile[]",
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
                "getCiv" to "fun(civName: string): UncivCiv|nil",
                "getCivById" to "fun(id: string): UncivCiv|nil",
                "getAllCivs" to "fun(): UncivCiv[]",
                "getAliveMajorCivs" to "fun(): UncivCiv[]",
                "getAliveCityStates" to "fun(): UncivCiv[]",
                "getBarbarianCiv" to "fun(): UncivCiv",
                "getTile" to "fun(x: number, y: number): UncivTile|nil",
                "findTiles" to "fun(criteria: table): UncivTile[]",
                "getMapWidth" to "fun(): number",
                "getMapHeight" to "fun(): number",
                "isWrapped" to "fun(): boolean",
                "getTilesNear" to "fun(x: number, y: number, radius: number): UncivTile[]",
                "getRulesetBuildings" to "fun(): string[]",
                "getRulesetUnits" to "fun(): string[]",
                "getRulesetTechs" to "fun(): string[]",
                "getRulesetPolicies" to "fun(): string[]",
                "getRulesetEras" to "fun(): string[]",
                "getRulesetPromotions" to "fun(): string[]",
                "doesBuildingExist" to "fun(buildingName: string): boolean",
                "doesUnitExist" to "fun(unitName: string): boolean",
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
        return out.toString()
    }

    /** Writes the generated definitions into the docs folder (generateDocs task, working dir = assets). */
    fun write() {
        File(outputFileName).writeText(generate())
    }
}

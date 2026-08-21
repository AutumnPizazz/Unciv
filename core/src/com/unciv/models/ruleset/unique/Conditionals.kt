package com.unciv.models.ruleset.unique

import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.logic.automation.Timers.Companion.timeThis
import com.unciv.logic.battle.CombatAction
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.managers.ReligionState
import com.unciv.logic.scripting.LuaAPI
import com.unciv.logic.scripting.LuaScriptManager
import com.unciv.models.ruleset.validation.ModCompatibility
import com.unciv.models.stats.Stat
import com.unciv.utils.hashOf
import yairm210.purity.annotations.Readonly
import kotlin.random.Random

object Conditionals {

    /**
     * Evaluates a Lua function from a loaded mod as a condition. The function receives the usual
     * ctx table and must return true for the condition to apply. Runs mod code inside a [@Readonly]
     * context - authors must keep the function a side-effect-free pure query, since conditions are
     * evaluated very often (and each call carries Lua interop overhead). Missing functions simply
     * evaluate to false; the mod checker reports them at load time.
     */
    /**
     * Max nesting depth for Lua conditions. A Lua condition function may call
     * [LuaAPI]'s `ctx.evaluateConditional` (or trigger other uniques) which can in turn evaluate
     * another Lua condition - a mod author writing a circular reference (`if [a]` -> calls
     * `if [b]` -> calls `if [a]`) would otherwise recurse until the JVM throws
     * StackOverflowError, which [LuaScriptManager.callFunction] (catching Exception only) cannot
     * contain. Exceeding the depth simply evaluates to false.
     */
    private val luaConditionDepth = ThreadLocal.withInitial { 0 }

    @Readonly @Suppress("purity") // running mod-provided code is inherently effectful - documented above
    private fun checkLuaCondition(conditional: Unique, state: GameContext): Boolean {
        val depth = luaConditionDepth.get()
        if (depth > 8) return false
        luaConditionDepth.set(depth + 1)
        try {
            val civInfo = state.relevantCiv ?: return false
            val (modName, functionName) = LuaScriptManager.parseLuaRef(conditional.params[0])
            val (foundMod, luaFunc) = LuaScriptManager.getFunction(modName, functionName)
                ?: return false
            val ctx = LuaAPI.buildContext(
                civInfo, state.relevantCity, state.relevantUnit, state.relevantTile,
                "", state, foundMod
            )
            var result = false
            LuaScriptManager.callFunction(luaFunc, ctx, civInfo, functionName,
                onSuccess = { result = it }, modName = foundMod)
            return result
        } finally {
            luaConditionDepth.set(depth)
        }
    }

    @Readonly @Suppress("purity") // hashcode... requires a think
    private fun getStateBasedRandom(state: GameContext, unique: Unique?): Float {
        val isOnlineMultiplayer = state.gameInfo?.gameParameters?.isOnlineMultiplayer == true
        val random = if (UncivGame.Current.settings.isRandomVarianceEnabled(isOnlineMultiplayer))
            Random
        else {
            val seed = hashOf(state.gameInfo?.turns?.hashCode() ?: 0,
                unique?.hashCode() ?: 0,
                state.hashCode())
            Random(seed)
        }
        return random.nextFloat()
    }

    @Readonly
    fun conditionalApplies(
        unique: Unique?,
        conditional: Unique,
        state: GameContext
    ): Boolean = timeThis("Conditionals.conditionalApplies") {

        if (conditional.isOtherModifierType)
            return true // not a filtering condition, includes e.g. ModifierHiddenFromUsers

        /** Helper to simplify conditional tests requiring gameInfo */
        @Readonly
        fun checkOnGameInfo(@Readonly predicate: (GameInfo.() -> Boolean)): Boolean {
            if (state.gameInfo == null) return false
            return state.gameInfo.predicate()
        }

        /** Helper to simplify conditional tests requiring a Civilization */
        @Readonly
        fun checkOnCiv(@Readonly predicate: (Civilization.() -> Boolean)): Boolean {
            if (state.relevantCiv == null) return false
            return state.relevantCiv!!.predicate()
        }

        /** Helper to simplify conditional tests requiring a City */
        @Readonly
        fun checkOnCity(@Readonly predicate: (City.() -> Boolean)): Boolean {
            if (state.relevantCity == null) return false
            return state.relevantCity!!.predicate()
        }

        /** Helper to simplify the "compare civ's current era with named era" conditions */
        @Readonly
        fun compareEra(eraParam: String, @Readonly compare: (civEra: Int, paramEra: Int) -> Boolean): Boolean {
            if (state.gameInfo == null) return false
            val era = state.gameInfo.ruleset.eras[eraParam] ?: return false
            return compare(state.relevantCiv!!.getEraNumber(), era.eraNumber)
        }

        /** Helper for ConditionalWhenAboveAmountStatResource and its below counterpart */
        @Readonly
        fun checkResourceOrStatAmount(
            resourceOrStatName: String,
            lowerLimit: Float,
            upperLimit: Float,
            modifyByGameSpeed: Boolean = false,
            @Readonly compare: (current: Int, lowerLimit: Float, upperLimit: Float) -> Boolean
        ): Boolean {
            if (state.gameInfo == null) return false
            var gameSpeedModifier = if (modifyByGameSpeed) state.gameInfo.speed.modifier else 1f

            if (state.gameInfo.ruleset.tileResources.containsKey(resourceOrStatName))
                return compare(state.getResourceAmount(resourceOrStatName), lowerLimit * gameSpeedModifier, upperLimit * gameSpeedModifier)
            if (state.gameInfo.ruleset.variables.containsKey(resourceOrStatName))
                return compare(state.getVariableAmount(resourceOrStatName), lowerLimit * gameSpeedModifier, upperLimit * gameSpeedModifier)
            val stat = Stat.safeValueOf(resourceOrStatName)
                ?: return false
            val statReserve = state.getStatAmount(stat)

            gameSpeedModifier = if (modifyByGameSpeed) state.gameInfo.speed.statCostModifiers[stat]!! else 1f
            return compare(statReserve, lowerLimit * gameSpeedModifier, upperLimit * gameSpeedModifier)
        }

        @Readonly
        fun compareCountables(
            first: String,
            second: String,
            @Readonly compare: (first: Int, second: Int) -> Boolean): Boolean {

            val firstNumber = Countables.getCountableAmount(first, state)
            val secondNumber = Countables.getCountableAmount(second, state)

            return if (firstNumber != null && secondNumber != null)
                compare(firstNumber, secondNumber)
            else
                false
        }

        @Readonly
        fun compareCountables(first: String, second: String, third: String,
                              @Readonly compare: (first: Int, second: Int, third: Int) -> Boolean): Boolean {

            val firstNumber = Countables.getCountableAmount(first, state)
            val secondNumber = Countables.getCountableAmount(second, state)
            val thirdNumber = Countables.getCountableAmount(third, state)

            return if (firstNumber != null && secondNumber != null && thirdNumber != null)
                compare(firstNumber, secondNumber, thirdNumber)
            else
                false
        }

        return when (conditional.type) {
            UniqueType.ConditionalChance -> {
                val chance = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                getStateBasedRandom(state, unique) < chance.toFloat() / 100f
            }
            UniqueType.ConditionalEveryTurns -> checkOnGameInfo { turns % (Countables.getCountableAmount(conditional.params[0], state) ?: return@checkOnGameInfo false) == 0 }
            UniqueType.ConditionalBeforeTurns -> checkOnGameInfo { turns < (Countables.getCountableAmount(conditional.params[0], state) ?: return@checkOnGameInfo false) }
            UniqueType.ConditionalAfterTurns -> checkOnGameInfo { turns >= (Countables.getCountableAmount(conditional.params[0], state) ?: return@checkOnGameInfo false) }
            UniqueType.ConditionalTutorialsEnabled -> UncivGame.Current.settings.showTutorials
            UniqueType.ConditionalTutorialCompleted -> conditional.params[0] in UncivGame.Current.settings.tutorialTasksCompleted

            UniqueType.ConditionalLuaCheck -> checkLuaCondition(conditional, state)

            UniqueType.ConditionalCivFilter -> checkOnCiv { matchesFilter(conditional.params[0], state) }
            UniqueType.ConditionalWar -> checkOnCiv { isAtWar() }
            UniqueType.ConditionalNotWar -> checkOnCiv { !isAtWar() }
            UniqueType.ConditionalWithResource -> state.getResourceAmount(conditional.params[0]) > 0
            UniqueType.ConditionalWithoutResource -> state.getResourceAmount(conditional.params[0]) <= 0

            UniqueType.ConditionalWhenAboveAmountStatResource -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                checkResourceOrStatAmount(conditional.params[1], amount.toFloat(), Float.MAX_VALUE, unique?.isModifiedByGameSpeed() == true)
                    { current, lowerLimit, _ -> current > lowerLimit }
            }
            UniqueType.ConditionalWhenBelowAmountStatResource -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                checkResourceOrStatAmount(conditional.params[1], Float.MIN_VALUE, amount.toFloat(), unique?.isModifiedByGameSpeed() == true)
                    { current, _, upperLimit -> current < upperLimit }
            }
            UniqueType.ConditionalWhenBetweenStatResource -> {
                val lowerAmount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                val upperAmount = Countables.getCountableAmount(conditional.params[1], state) ?: return false
                checkResourceOrStatAmount(conditional.params[2], lowerAmount.toFloat(), upperAmount.toFloat(), unique?.isModifiedByGameSpeed() == true)
                    { current, lowerLimit, upperLimit -> current >= lowerLimit && current <= upperLimit }
            }

            // City-scope variable conditionals: judged on the city matching the [cityFilter]
            UniqueType.ConditionalWhenAboveCityVariable -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                checkOnCity {
                    matchesFilter(conditional.params[2]) && getVariable(conditional.params[1]) > amount
                }
            }
            UniqueType.ConditionalWhenBelowCityVariable -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                checkOnCity {
                    matchesFilter(conditional.params[2]) && getVariable(conditional.params[1]) < amount
                }
            }
            UniqueType.ConditionalWhenBetweenCityVariable -> {
                val lowerAmount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                val upperAmount = Countables.getCountableAmount(conditional.params[1], state) ?: return false
                checkOnCity {
                    matchesFilter(conditional.params[3]) &&
                        getVariable(conditional.params[2]) >= lowerAmount && getVariable(conditional.params[2]) <= upperAmount
                }
            }

            // Global-scope variable conditionals: judged against the game-wide value
            UniqueType.ConditionalWhenAboveGlobalVariable -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                checkOnGameInfo { getVariable(conditional.params[1]) > amount }
            }
            UniqueType.ConditionalWhenBelowGlobalVariable -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                checkOnGameInfo { getVariable(conditional.params[1]) < amount }
            }
            UniqueType.ConditionalWhenBetweenGlobalVariable -> {
                val lowerAmount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                val upperAmount = Countables.getCountableAmount(conditional.params[1], state) ?: return false
                checkOnGameInfo {
                    getVariable(conditional.params[2]) >= lowerAmount && getVariable(conditional.params[2]) <= upperAmount
                }
            }

            UniqueType.ConditionalHappy -> checkOnCiv { stats.happiness >= 0 }
            UniqueType.ConditionalGoldenAge -> checkOnCiv { goldenAges.isGoldenAge() }
            UniqueType.ConditionalNotGoldenAge -> checkOnCiv { !goldenAges.isGoldenAge() }

            UniqueType.ConditionalBeforeEra -> compareEra(conditional.params[0]) { current, param -> current < param }
            UniqueType.ConditionalStartingFromEra -> compareEra(conditional.params[0]) { current, param -> current >= param }
            UniqueType.ConditionalDuringEra -> compareEra(conditional.params[0]) { current, param -> current == param }
            UniqueType.ConditionalIfStartingInEra -> checkOnGameInfo { gameParameters.startingEra == conditional.params[0] }
            UniqueType.ConditionalSpeed -> checkOnGameInfo { gameParameters.speed == conditional.params[0] }
            UniqueType.ConditionalDifficulty -> checkOnGameInfo { gameParameters.difficulty == conditional.params[0] }
            UniqueType.ConditionalDifficultyOrHigher -> checkOnGameInfo {
                val difficulty = conditional.params[0]
                if (difficulty in ruleset.difficulties) {
                    val difficulties = ruleset.difficulties.keys.toList()
                    difficulties.indexOf(getDifficulty().name) >= difficulties.indexOf(difficulty)
                } else false
            }
            UniqueType.ConditionalDifficultyOrLower -> checkOnGameInfo {
                val difficulty = conditional.params[0]
                if (difficulty in ruleset.difficulties) {
                    val difficulties = ruleset.difficulties.keys.toList()
                    difficulties.indexOf(getDifficulty().name) <= difficulties.indexOf(difficulty)
                } else false
            }
            UniqueType.ConditionalVictoryEnabled -> checkOnGameInfo { gameParameters.victoryTypes.contains(conditional.params[0]) }
            UniqueType.ConditionalVictoryDisabled -> checkOnGameInfo { !gameParameters.victoryTypes.contains(conditional.params[0]) }
            UniqueType.ConditionalReligionEnabled -> checkOnGameInfo { isReligionEnabled() }
            UniqueType.ConditionalReligionDisabled -> checkOnGameInfo { !isReligionEnabled() }
            UniqueType.ConditionalEspionageEnabled -> checkOnGameInfo { isEspionageEnabled() }
            UniqueType.ConditionalEspionageDisabled -> checkOnGameInfo { !isEspionageEnabled() }
            UniqueType.ConditionalNuclearWeaponsEnabled -> checkOnGameInfo { gameParameters.nuclearWeaponsEnabled }
            UniqueType.ConditionalNuclearWeaponsDisabled -> checkOnGameInfo { !gameParameters.nuclearWeaponsEnabled }
            UniqueType.ConditionalTech -> checkOnCiv {
                val filter = conditional.params[0]
                if (filter in gameInfo.ruleset.technologies) tech.isResearched(conditional.params[0]) // fast common case
                else tech.researchedTechnologies.any { it.matchesFilter(filter) }
            }
            UniqueType.ConditionalNoTech -> checkOnCiv {
                val filter = conditional.params[0]
                if (filter in gameInfo.ruleset.technologies) !tech.isResearched(conditional.params[0]) // fast common case
                else tech.researchedTechnologies.none { it.matchesFilter(filter) }
            }
            UniqueType.ConditionalWhileResearching -> checkOnCiv { tech.currentTechnology()?.matchesFilter(conditional.params[0]) == true }
            UniqueType.ConditionalNoCivAdopted -> checkOnGameInfo {
                civilizations.none {
                    it.isMajorCiv() &&
                    it.isAlive() &&
                    (it.policies.isAdopted(conditional.params[0]) || it.religionManager.religion?.hasBelief(conditional.params[0]) == true)
                }
            }
            UniqueType.ConditionalAfterPolicyOrBelief ->
                checkOnCiv { policies.isAdopted(conditional.params[0]) || religionManager.religion?.hasBelief(conditional.params[0]) == true }
            UniqueType.ConditionalBeforePolicyOrBelief ->
                checkOnCiv { !policies.isAdopted(conditional.params[0]) && religionManager.religion?.hasBelief(conditional.params[0]) != true }
            UniqueType.ConditionalBeforePantheon ->
                checkOnCiv { religionManager.religionState == ReligionState.None }
            UniqueType.ConditionalAfterPantheon ->
                checkOnCiv { religionManager.religionState != ReligionState.None }
            UniqueType.ConditionalBeforeReligion ->
                checkOnCiv { religionManager.religionState < ReligionState.Religion }
            UniqueType.ConditionalAfterReligion ->
                checkOnCiv { religionManager.religionState >= ReligionState.Religion }
            UniqueType.ConditionalBeforeEnhancingReligion ->
                checkOnCiv { religionManager.religionState < ReligionState.EnhancedReligion }
            UniqueType.ConditionalAfterEnhancingReligion ->
                checkOnCiv { religionManager.religionState >= ReligionState.EnhancedReligion }
            UniqueType.ConditionalAfterGeneratingGreatProphet ->
                checkOnCiv { religionManager.greatProphetsEarned() > 0 }

            UniqueType.ConditionalBuildingBuilt ->
                checkOnCiv { cities.any { it.cityConstructions.containsBuildingOrEquivalent(conditional.params[0]) } }
            UniqueType.ConditionalBuildingNotBuilt ->
                checkOnCiv { cities.none { it.cityConstructions.containsBuildingOrEquivalent(conditional.params[0]) } }
            UniqueType.ConditionalBuildingBuiltAll ->
                checkOnCiv { cities.filter { it.matchesFilter(conditional.params[1]) }.all {
                  it.cityConstructions.containsBuildingOrEquivalent(conditional.params[0]) } }
            UniqueType.ConditionalBuildingBuiltAmount -> {
                val amount = Countables.getCountableAmount(conditional.params[1], state) ?: return false
                checkOnCiv { cities.count { it.cityConstructions.containsBuildingOrEquivalent(conditional.params[0])
                    && it.matchesFilter(conditional.params[2]) } >= amount }
            }
            UniqueType.ConditionalBuildingBuiltByAnybody ->
                checkOnGameInfo { getCities().any { it.cityConstructions.containsBuildingOrEquivalent(conditional.params[0]) } }
            UniqueType.ConditionalBuildingNotBuiltByAnybody ->
                !checkOnGameInfo { getCities().any { it.cityConstructions.containsBuildingOrEquivalent(conditional.params[0]) } }

            // Filtered via city.getMatchingUniques
            UniqueType.ConditionalInThisCity -> state.relevantCity != null
            UniqueType.ConditionalCityFilter -> checkOnCity { matchesFilter(conditional.params[0], state.relevantCiv) }
            UniqueType.ConditionalCityConnected -> checkOnCity { isConnectedToCapital() }
            UniqueType.ConditionalCityReligion -> checkOnCity {
                religion.getMajorityReligion()
                    ?.matchesFilter(conditional.params[0], state, state.relevantCiv) == true
            }
            UniqueType.ConditionalCityNotReligion -> checkOnCity {
                religion.getMajorityReligion()
                    ?.matchesFilter(conditional.params[0], state, state.relevantCiv) != true
            }
            UniqueType.ConditionalCityMajorReligion -> checkOnCity {
                religion.getMajorityReligion()?.isMajorReligion() == true }
            UniqueType.ConditionalCityEnhancedReligion -> checkOnCity {
                religion.getMajorityReligion()?.isEnhancedReligion() == true }
            UniqueType.ConditionalCityThisReligion -> checkOnCity {
                religion.getMajorityReligion() == state.relevantCiv?.religionManager?.religion }
            UniqueType.ConditionalWLTKD -> checkOnCity { isWeLoveTheKingDayActive() }
            UniqueType.ConditionalCityWithBuilding ->
                checkOnCity { cityConstructions.containsBuildingOrEquivalent(conditional.params[0]) }
            UniqueType.ConditionalCityWithoutBuilding ->
                checkOnCity { !cityConstructions.containsBuildingOrEquivalent(conditional.params[0]) }
            UniqueType.ConditionalPopulationFilter -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                checkOnCity { population.getPopulationFilterAmount(conditional.params[1]) >= amount }
            }
            UniqueType.ConditionalExactPopulationFilter -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                checkOnCity { population.getPopulationFilterAmount(conditional.params[1]) == amount }
            }
            UniqueType.ConditionalBetweenPopulationFilter -> {
                val lowerAmount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                val upperAmount = Countables.getCountableAmount(conditional.params[1], state) ?: return false
                checkOnCity { population.getPopulationFilterAmount(conditional.params[2]) in lowerAmount..upperAmount }
            }
            UniqueType.ConditionalBelowPopulationFilter -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                checkOnCity { population.getPopulationFilterAmount(conditional.params[1]) < amount }
            }
            UniqueType.ConditionalWhenGarrisoned ->
                checkOnCity { getCenterTile().militaryUnit?.canGarrison() == true }
            UniqueType.ConditionalCityBeingRazed ->
                checkOnCity { isBeingRazed }

            UniqueType.ConditionalVsCity -> state.theirCombatant?.matchesFilter("City", false) == true
            UniqueType.ConditionalVsUnits,  UniqueType.ConditionalVsCombatant -> state.theirCombatant?.matchesFilter(conditional.params[0]) == true
            UniqueType.ConditionalOurUnit, UniqueType.ConditionalOurUnitOnUnit ->
                state.relevantUnit?.matchesFilter(conditional.params[0]) == true
            UniqueType.ConditionalUnitWithPromotion -> state.relevantUnit != null &&
                    (state.relevantUnit!!.promotions.promotions.contains(conditional.params[0])
                    || state.relevantUnit!!.hasStatus(conditional.params[0]) )
            UniqueType.ConditionalUnitWithoutPromotion -> state.relevantUnit != null &&
                    !(state.relevantUnit!!.promotions.promotions.contains(conditional.params[0])
                            || state.relevantUnit!!.hasStatus(conditional.params[0]) )
            UniqueType.ConditionalUnitFortified -> state.relevantUnit?.isFortified() == true
            UniqueType.ConditionalUnitEmbarked -> state.relevantUnit?.isEmbarked() == true
            UniqueType.ConditionalAttacking -> state.combatAction == CombatAction.Attack
            UniqueType.ConditionalDefending -> state.combatAction == CombatAction.Defend
            UniqueType.ConditionalAboveHP -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                state.relevantUnit != null && state.relevantUnit!!.health > amount
                    || state.ourCombatant != null && state.ourCombatant.getHealth() > amount
            }
            UniqueType.ConditionalBelowHP -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                state.relevantUnit != null && state.relevantUnit!!.health < amount
                    || state.ourCombatant != null && state.ourCombatant.getHealth() < amount
            }
            UniqueType.ConditionalAboveMovement -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                state.relevantUnit != null && state.relevantUnit!!.currentMovement > amount
            }
            UniqueType.ConditionalBelowMovement -> {
                val amount = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                state.relevantUnit != null && state.relevantUnit!!.currentMovement < amount
            }
            UniqueType.ConditionalHasNotUsedOtherActions ->
                state.unit == null || // So we get the action as a valid action in BaseUnit.hasUnique()
                    state.unit.abilityToTimesUsed.isEmpty()
            UniqueType.ConditionalStackedWithUnit -> state.relevantUnit != null &&
                    state.relevantUnit!!.getTile().getUnits().any { it != state.relevantUnit && it.matchesFilter(conditional.params[0]) }
            UniqueType.ConditionalNotStackedWithUnit -> state.relevantUnit == null ||
                    !state.relevantUnit!!.getTile().getUnits().any { it != state.relevantUnit && it.matchesFilter(conditional.params[0]) }

            UniqueType.ConditionalInTiles ->
                state.relevantTile?.matchesFilter(conditional.params[0], state.relevantCiv) == true
            UniqueType.ConditionalInTilesNot ->
                state.relevantTile?.matchesFilter(conditional.params[0], state.relevantCiv) == false
            UniqueType.ConditionalAdjacentTo -> state.relevantTile?.isAdjacentTo(conditional.params[0], state.relevantCiv) == true
            UniqueType.ConditionalNotAdjacentTo -> state.relevantTile?.isAdjacentTo(conditional.params[0], state.relevantCiv) == false
            UniqueType.ConditionalFightingInTiles ->
                state.attackedTile?.matchesFilter(conditional.params[0], state.relevantCiv) == true
            UniqueType.ConditionalNearTiles -> {
                val distance = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                state.relevantTile != null && state.relevantTile!!.getTilesInDistance(distance).any {
                    it.matchesFilter(conditional.params[1], state.relevantCiv)
                }
            }

            UniqueType.ConditionalVsLargerCiv -> {
                val yourCities = state.relevantCiv?.cities?.size ?: 1
                val theirCities = state.theirCombatant?.getCivInfo()?.cities?.size ?: 0
                yourCities < theirCities
            }
            UniqueType.ConditionalForeignContinent -> checkOnCiv {
                state.relevantTile != null && (
                    cities.isEmpty() || getCapital() == null
                        || getCapital()!!.getCenterTile().getContinent() != state.relevantTile!!.getContinent()
                    )
            }
            UniqueType.ConditionalAdjacentUnit ->
                state.relevantCiv != null &&
                        state.relevantUnit != null &&
                        state.relevantTile!!.neighbors.any {
                        it.getUnits().any {
                            it != state.relevantUnit &&
                                it.civ == state.relevantCiv &&
                                it.matchesFilter(conditional.params[0])
                        }
                    }

            UniqueType.ConditionalNeighborTiles -> {
                val minNeighbors = Countables.getCountableAmount(conditional.params[0], state) ?: return false
                val maxNeighbors = Countables.getCountableAmount(conditional.params[1], state) ?: return false
                state.relevantTile != null
                    && state.relevantTile!!.neighbors.count {
                        it.matchesFilter(conditional.params[2], state.relevantCiv)
                    } in minNeighbors..maxNeighbors
            }

            UniqueType.ConditionalOnWaterMaps -> state.region?.continentID == -1
            UniqueType.ConditionalInRegionOfType -> state.region?.type == conditional.params[0]
            UniqueType.ConditionalInRegionExceptOfType -> state.region?.type != conditional.params[0]

            UniqueType.ConditionalFirstCivToResearch ->
                unique != null
                    && unique.sourceObjectType == UniqueTarget.Tech
                    && checkOnGameInfo { civilizations.none {
                        it != state.relevantCiv && it.isMajorCiv()
                            && it.tech.isResearched(unique.sourceObjectName!!) // guarded by the sourceObjectType check
                    } }

            UniqueType.ConditionalFirstCivToAdopt ->
                unique != null
                    && unique.sourceObjectType == UniqueTarget.Policy
                    && checkOnGameInfo { civilizations.none {
                        it != state.relevantCiv && it.isMajorCiv()
                            && it.policies.isAdopted(unique.sourceObjectName!!) // guarded by the sourceObjectType check
                    } }

            UniqueType.ConditionalCountableEqualTo ->
                compareCountables(conditional.params[0], conditional.params[1]) {
                    first, second -> first == second
                }

            UniqueType.ConditionalCountableDifferentThan ->
                compareCountables(conditional.params[0], conditional.params[1]) {
                        first, second -> first != second
                }

            UniqueType.ConditionalCountableMoreThan ->
                compareCountables(conditional.params[0], conditional.params[1]) {
                        first, second -> first > second
                }

            UniqueType.ConditionalCountableLessThan ->
                compareCountables(conditional.params[0], conditional.params[1]) {
                        first, second -> first < second
                }

            UniqueType.ConditionalCountableBetween ->
                compareCountables(conditional.params[0], conditional.params[1], conditional.params[2]) {
                    first, second, third ->
                    first in second..third
                }

            UniqueType.ConditionalWhenCarriedBy -> {
                // Check if the unit is currently transported and being carried by matching filter
                if (state.relevantUnit == null || !state.relevantUnit!!.isTransported) false
                else {
                    val carrier = state.relevantUnit!!.getTile().militaryUnit
                    // Only true if: 1) carrier exists, 2) carrier is NOT the unit itself, 3) carrier matches filter
                    carrier != null && carrier != state.relevantUnit &&
                    carrier.matchesFilter(conditional.params[0]) == true
                }
            }

            UniqueType.ConditionalModEnabled -> checkOnGameInfo {
                val filter = conditional.params[0]
                (gameParameters.mods.asSequence() + gameParameters.baseRuleset).any { ModCompatibility.modNameFilter(it, filter) }
            }
            UniqueType.ConditionalModNotEnabled -> checkOnGameInfo {
                val filter = conditional.params[0]
                (gameParameters.mods.asSequence() + gameParameters.baseRuleset).none { ModCompatibility.modNameFilter(it, filter) }
            }

            else -> false
        }
    }
}

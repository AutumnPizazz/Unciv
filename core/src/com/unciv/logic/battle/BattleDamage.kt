package com.unciv.logic.battle

import com.unciv.UncivGame
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.scripting.LuaAPI
import com.unciv.logic.scripting.LuaScriptManager
import com.unciv.models.Counter
import com.unciv.models.ruleset.GlobalUniques
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueTarget
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.translations.tr
import com.unciv.ui.components.extensions.toPercent
import yairm210.purity.annotations.LocalState
import yairm210.purity.annotations.Pure
import yairm210.purity.annotations.Readonly
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import org.luaj.vm2.LuaValue

object BattleDamage {

    @Readonly
    private fun getModifierStringFromUnique(unique: Unique): String {
        val source = when (unique.sourceObjectType) {
            UniqueTarget.Unit -> "Unit ability"
            UniqueTarget.Nation -> "National ability"
            UniqueTarget.Global -> GlobalUniques.getUniqueSourceDescription(unique)
            else -> "[${unique.sourceObjectName}] ([${unique.getSourceNameForUser()}])"
        }.tr()
        if (unique.modifiers.isEmpty()) return source

        val conditionalsText = unique.modifiers.joinToString { it.text.tr() }
        return "$source - $conditionalsText"
    }

    @Readonly
    private fun getGeneralModifiers(combatant: ICombatant, enemy: ICombatant, combatAction: CombatAction, tileToAttackFrom: Tile): Counter<String> {
        val modifiers = Counter<String>()

        val conditionalState = getGameContext(combatAction, combatant, enemy)
        val civInfo = combatant.getCivInfo()

        if (combatant is MapUnitCombatant) {

            val unitUniqueModifiers = getUnitUniqueModifiers(combatant, enemy, conditionalState, tileToAttackFrom)
            modifiers.add(unitUniqueModifiers)

            val civResources = civInfo.getCivResourcesByName()
            for (resource in combatant.unit.getResourceRequirementsPerTurn().keys)
                if (civResources[resource]!! < 0 && !civInfo.isBarbarian)
                    modifiers["Missing resource"] = BattleConstants.MISSING_RESOURCES_MALUS

            val (greatGeneralName, greatGeneralBonus) = GreatGeneralImplementation.getGreatGeneralBonus(combatant, enemy, combatAction)
            if (greatGeneralBonus != 0)
                modifiers[greatGeneralName] = greatGeneralBonus

        } else if (combatant is CityCombatant) {
            for (unique in combatant.city.getMatchingUniques(UniqueType.StrengthForCities, conditionalState)) {
                modifiers.add(getModifierStringFromUnique(unique), unique.params[0].toInt())
            }
        }

        if (enemy.getCivInfo().isBarbarian) {
            modifiers["Difficulty"] =
                (civInfo.gameInfo.getDifficulty().barbarianBonus * 100).toInt()
        }

        return modifiers
    }

    @Readonly
    private fun getGameContext(
        combatAction: CombatAction,
        combatant: ICombatant,
        enemy: ICombatant,
    ): GameContext {
        val attackedTile =
            if (combatAction == CombatAction.Attack) enemy.getTile()
            else combatant.getTile()

        val conditionalState = GameContext(
            combatant.getCivInfo(),
            city = (combatant as? CityCombatant)?.city,
            ourCombatant = combatant,
            theirCombatant = enemy,
            attackedTile = attackedTile,
            combatAction = combatAction
        )
        return conditionalState
    }

    @Readonly
    private fun getUnitUniqueModifiers(combatant: MapUnitCombatant, enemy: ICombatant, conditionalState: GameContext,
                                       tileToAttackFrom: Tile): Counter<String> {
        val civInfo = combatant.getCivInfo()
        val modifiers = Counter<String>()

        for (unique in combatant.getMatchingUniques(UniqueType.Strength, conditionalState, true)) {
            modifiers.add(getModifierStringFromUnique(unique), unique.params[0].toInt())
        }

        // e.g., Mehal Sefari https://civilization.fandom.com/wiki/Mehal_Sefari_(Civ5)
        for (unique in combatant.getMatchingUniques(
            UniqueType.StrengthNearCapital, conditionalState, true
        )) {
            if (civInfo.cities.isEmpty() || civInfo.getCapital() == null) break
            val distance =
                combatant.getTile().aerialDistanceTo(civInfo.getCapital()!!.getCenterTile())
            // https://steamcommunity.com/sharedfiles/filedetails/?id=326411722#464287
            val effect = unique.params[0].toInt() - 3 * distance
            if (effect > 0)
                modifiers.add(getModifierStringFromUnique(unique), effect)
        }

        //https://www.carlsguides.com/strategy/civilization5/war/combatbonuses.php
        var adjacentUnits = combatant.getTile().neighbors.flatMap { it.getUnits() }
        if (enemy.getTile() !in combatant.getTile().neighbors && tileToAttackFrom in combatant.getTile().neighbors
            && enemy is MapUnitCombatant
        )
            adjacentUnits += sequenceOf(enemy.unit)

        // e.g., Maori Warrior - https://civilization.fandom.com/wiki/Maori_Warrior_(Civ5)
        val strengthMalus = adjacentUnits.filter { it.civ.isAtWarWith(combatant.getCivInfo()) }
            .flatMap { it.getMatchingUniques(UniqueType.StrengthForAdjacentEnemies) }
            .filter { combatant.matchesFilter(it.params[1]) && combatant.getTile().matchesFilter(it.params[2]) }
            .maxByOrNull { it.params[0] }
        if (strengthMalus != null) {
            modifiers.add("Adjacent enemy units", strengthMalus.params[0].toInt())
        }
        return modifiers
    }

    @Readonly
    fun getAttackModifiers(
        attacker: ICombatant,
        defender: ICombatant, tileToAttackFrom: Tile
    ): Counter<String> {
        @LocalState val modifiers = getGeneralModifiers(attacker, defender, CombatAction.Attack, tileToAttackFrom)

        if (attacker is MapUnitCombatant) {

            val terrainAttackModifiers = getTerrainAttackModifiers(attacker, defender, tileToAttackFrom)
            modifiers.add(terrainAttackModifiers)

            // Air unit attacking with Air Sweep
            if (attacker.unit.isPreparingAirSweep())
                modifiers.add(getAirSweepAttackModifiers(attacker))

            if (attacker.isMelee()) {
                val numberOfOtherAttackersSurroundingDefender = defender.getTile().neighbors.count {
                    it.militaryUnit != null && it.militaryUnit != attacker.unit
                            && it.militaryUnit!!.civ == attacker.getCivInfo()
                            && MapUnitCombatant(it.militaryUnit!!).isMelee()
                }
                if (numberOfOtherAttackersSurroundingDefender > 0) {
                    var flankingBonus = BattleConstants.BASE_FLANKING_BONUS

                    // e.g., Discipline policy - https://civilization.fandom.com/wiki/Discipline_(Civ5)
                    for (unique in attacker.unit.getMatchingUniques(UniqueType.FlankAttackBonus, checkCivInfoUniques = true,
                            gameContext = getGameContext(CombatAction.Attack, attacker, defender)))
                        flankingBonus *= unique.params[0].toPercent()
                    modifiers["Flanking"] =
                        (flankingBonus * numberOfOtherAttackersSurroundingDefender).toInt()
                }
            }

        }

        return modifiers
    }

    @Readonly
    private fun getTerrainAttackModifiers(attacker: MapUnitCombatant, defender: ICombatant, tileToAttackFrom: Tile): Counter<String> {
        val modifiers = Counter<String>()
        if (attacker.unit.isEmbarked() && defender.getTile().isLand
            && !attacker.unit.hasUnique(UniqueType.AttackAcrossCoast)
        )
            modifiers["Landing"] = BattleConstants.LANDING_MALUS

        // Land Melee Unit attacking to Water
        if (attacker.unit.type.isLandUnit() && !attacker.getTile().isWater && attacker.isMelee() && defender.getTile().isWater
            && !attacker.unit.hasUnique(UniqueType.AttackAcrossCoast)
        )
            modifiers["Boarding"] = BattleConstants.BOARDING_MALUS

        // Melee Unit on water attacking to Land (not City) unit
        if (!attacker.unit.type.isAirUnit() && attacker.isMelee() && attacker.getTile().isWater && !defender.getTile().isWater
            && !attacker.unit.hasUnique(UniqueType.AttackAcrossCoast) && !defender.isCity()
        )
            modifiers["Landing"] = BattleConstants.LANDING_MALUS

        if (isMeleeAttackingAcrossRiverWithNoBridge(attacker, tileToAttackFrom, defender))
            modifiers["Across river"] = BattleConstants.ATTACKING_ACROSS_RIVER_MALUS
        return modifiers
    }

    @Readonly
    private fun isMeleeAttackingAcrossRiverWithNoBridge(attacker: MapUnitCombatant, tileToAttackFrom: Tile, defender: ICombatant) = (
        attacker.isMelee()
            &&
            (tileToAttackFrom.aerialDistanceTo(defender.getTile()) == 1
                && tileToAttackFrom.isConnectedByRiver(defender.getTile())
                && !attacker.unit.hasUnique(UniqueType.AttackAcrossRiver))
            &&
            (!tileToAttackFrom.hasConnection(attacker.getCivInfo()) // meaning, the tiles are not road-connected for this civ
                || !defender.getTile().hasConnection(attacker.getCivInfo())
                || !attacker.getCivInfo().tech.roadsConnectAcrossRivers)
        )

    @Readonly
    fun getAirSweepAttackModifiers(
        attacker: ICombatant
    ): Counter<String> {
        val modifiers = Counter<String>()

        if (attacker is MapUnitCombatant) {
            for (unique in attacker.unit.getMatchingUniques(UniqueType.StrengthWhenAirsweep)) {
                modifiers.add(getModifierStringFromUnique(unique), unique.params[0].toInt())
            }
        }

        return modifiers
    }

    @Readonly
    fun getDefenceModifiers(attacker: ICombatant, defender: ICombatant, tileToAttackFrom: Tile): Counter<String> {
        @LocalState val modifiers = getGeneralModifiers(defender, attacker, CombatAction.Defend, tileToAttackFrom)
        val tile = defender.getTile()

        if (defender is MapUnitCombatant && !defender.unit.isEmbarked()) { // Embarked units get no terrain defensive bonuses

            val tileDefenceBonus = tile.getDefensiveBonus(unit = defender.unit)
            if (!defender.unit.hasUnique(UniqueType.NoDefensiveTerrainBonus, checkCivInfoUniques = true) && tileDefenceBonus > 0
                || !defender.unit.hasUnique(UniqueType.NoDefensiveTerrainPenalty, checkCivInfoUniques = true) && tileDefenceBonus < 0
            )
                modifiers["Tile"] = (tileDefenceBonus * 100).toInt()


            if (defender.unit.isFortified() || defender.unit.isGuarding())
                modifiers["Fortification"] = BattleConstants.FORTIFICATION_BONUS * defender.unit.getFortificationTurns()
        }

        return modifiers
    }

    @Readonly
    private fun modifiersToFinalBonus(modifiers: Counter<String>): Float {
        var finalModifier = 1f
        for (modifierValue in modifiers.values) finalModifier += modifierValue / 100f
        return finalModifier
    }

    @Readonly
    private fun getHealthDependantDamageRatio(combatant: ICombatant): Float {
        return if (combatant !is MapUnitCombatant
            || combatant.unit.hasUnique(UniqueType.NoDamagePenaltyWoundedUnits, checkCivInfoUniques = true)
        ) 1f
        else {
            // Each 3% of missing health reduces damage dealt by 1%
            val maxHealth = combatant.getMaxHealth()
            val missingHealthPercent = (maxHealth - combatant.getHealth()) * 100f / maxHealth
            1 - missingHealthPercent / BattleConstants.DAMAGE_REDUCTION_WOUNDED_UNIT_RATIO_PERCENTAGE
        }
    }


    /**
     * Includes attack modifiers
     */
    @Readonly
    fun getAttackingStrength(
        attacker: ICombatant,
        defender: ICombatant,
        tileToAttackFrom: Tile
    ): Float {
        val attackModifiers = getAttackModifiers(attacker, defender, tileToAttackFrom)
        val attackModifier = modifiersToFinalBonus(attackModifiers)
        val base = attacker.getAttackingStrength(defender).toFloat()
        val modified = applyLuaStrengthFormula(attacker, defender, CombatAction.Attack, base, attackModifier, attackModifiers)
        return max(1f, modified)
    }


    /**
     * Includes defence modifiers
     */
    @Readonly
    fun getDefendingStrength(attacker: ICombatant, defender: ICombatant, tileToAttackFrom: Tile): Float {
        val defenceModifiers = getDefenceModifiers(attacker, defender, tileToAttackFrom)
        val defenceModifier = modifiersToFinalBonus(defenceModifiers)
        val base = defender.getDefendingStrength(attacker).toFloat()
        val modified = applyLuaStrengthFormula(defender, attacker, CombatAction.Defend, base, defenceModifier, defenceModifiers)
        return max(1f, modified)
    }

    // region Lua combat formula hooks

    /** Guards against re-entrant combat hooks (Lua → attackTile → combat → Lua). */
    private val luaCombatHookDepth = ThreadLocal.withInitial { 0 }
    private const val MAX_LUA_COMBAT_HOOK_DEPTH = 8

    @Readonly
    private fun getCombatLuaUniques(combatant: ICombatant, uniqueType: UniqueType, gameContext: GameContext): Sequence<Unique> = when (combatant) {
        is MapUnitCombatant -> combatant.getMatchingUniques(uniqueType, gameContext, checkCivUniques = true)
        is CityCombatant -> combatant.city.getMatchingUniques(uniqueType, gameContext)
        else -> emptySequence()
    }

    @Readonly @Suppress("purity") // building a Lua table for mod code - luaj's tableOf/set are not @Readonly
    private fun modifiersToLuaTable(modifiers: Counter<String>): LuaValue {
        val table = LuaValue.tableOf()
        for ((name, value) in modifiers)
            table.set(name, LuaValue.valueOf(value))
        return table
    }

    private fun buildLuaCombatContext(
        owner: ICombatant,
        gameContext: GameContext,
        value: Double?,
        modifier: Double?,
        attackerStrength: Double?,
        defenderStrength: Double?,
        modifiers: LuaValue?,
        randomnessFactor: Double?,
        healthRatio: Double?,
        damageToAttacker: Boolean?,
        modName: String
    ): LuaValue {
        val city = (owner as? CityCombatant)?.city
        val unit = (owner as? MapUnitCombatant)?.unit
        return LuaAPI.buildContext(owner.getCivInfo(), city, unit, owner.getTile(), "", gameContext, modName,
            value = value, modifier = modifier, attackerStrength = attackerStrength, defenderStrength = defenderStrength,
            modifiers = modifiers, randomnessFactor = randomnessFactor, healthRatio = healthRatio, damageToAttacker = damageToAttacker)
    }

    /**
     * Invokes the owner's first matching [uniqueType] Lua function with the raw combat inputs and
     * returns its numeric result, or null when no matching unique exists / it returns nil / it throws.
     * Lua receives the raw parameters (strengths, individual modifiers, randomness, health ratio,
     * direction) so it owns the whole formula.
     */
    @Readonly @Suppress("purity") // running mod-provided code is inherently effectful
    private fun invokeLuaCombatHook(
        owner: ICombatant,
        enemy: ICombatant,
        combatAction: CombatAction,
        uniqueType: UniqueType,
        value: Double?,
        modifier: Double?,
        attackerStrength: Double?,
        defenderStrength: Double?,
        modifiers: LuaValue?,
        randomnessFactor: Double?,
        healthRatio: Double?,
        damageToAttacker: Boolean?
    ): Double? {
        val gameContext = getGameContext(combatAction, owner, enemy)
        val unique = getCombatLuaUniques(owner, uniqueType, gameContext).firstOrNull() ?: return null
        val (modName, functionName) = LuaScriptManager.parseLuaRef(unique.params[0])
        val (foundMod, func) = LuaScriptManager.getFunction(modName, functionName) ?: return null

        val depth = luaCombatHookDepth.get()
        if (depth >= MAX_LUA_COMBAT_HOOK_DEPTH) return null
        luaCombatHookDepth.set(depth + 1)
        try {
            val ctx = buildLuaCombatContext(owner, gameContext, value, modifier, attackerStrength, defenderStrength,
                modifiers, randomnessFactor, healthRatio, damageToAttacker, foundMod)
            var result: Double? = null
            LuaScriptManager.callFunctionForValue(func, ctx, owner.getCivInfo(), functionName, foundMod) { result = it }
            return result
        } finally {
            luaCombatHookDepth.set(depth)
        }
    }

    /** Strength formula: Lua receives base strength + modifier factor + individual modifiers; falls back to base * modifier. */
    @Readonly
    private fun applyLuaStrengthFormula(owner: ICombatant, enemy: ICombatant, combatAction: CombatAction, base: Float, modifier: Float, modifiers: Counter<String>): Float {
        val result = invokeLuaCombatHook(owner, enemy, combatAction, UniqueType.LuaModifyCombatStrength,
            value = base.toDouble(), modifier = modifier.toDouble(), attackerStrength = null, defenderStrength = null,
            modifiers = modifiersToLuaTable(modifiers), randomnessFactor = null, healthRatio = null, damageToAttacker = null)
        return result?.toFloat() ?: (base * modifier)
    }

    /** Damage formula: Lua receives the strengths + randomness + health ratio + direction; falls back to the engine formula. */
    @Readonly
    private fun applyLuaDamageFormula(owner: ICombatant, enemy: ICombatant, combatAction: CombatAction,
                                      attackerStrength: Float, defenderStrength: Float, damageToAttacker: Boolean, randomnessFactor: Float, healthRatio: Float): Float {
        val result = invokeLuaCombatHook(owner, enemy, combatAction, UniqueType.LuaModifyCombatDamage,
            value = null, modifier = null, attackerStrength = attackerStrength.toDouble(), defenderStrength = defenderStrength.toDouble(),
            modifiers = null, randomnessFactor = randomnessFactor.toDouble(), healthRatio = healthRatio.toDouble(), damageToAttacker = damageToAttacker)
        if (result != null) return result.toFloat()
        return damageModifier(attackerStrength / defenderStrength, damageToAttacker, randomnessFactor) * healthRatio
    }

    /** Defensive reduction: Lua receives the incoming damage + full combat inputs; falls back to leaving it unchanged. */
    @Readonly
    private fun applyLuaDamageReceived(owner: ICombatant, enemy: ICombatant, combatAction: CombatAction,
                                       damage: Float, attackerStrength: Float, defenderStrength: Float, randomnessFactor: Float, healthRatio: Float, damageToAttacker: Boolean): Float {
        val result = invokeLuaCombatHook(owner, enemy, combatAction, UniqueType.LuaModifyCombatDamageReceived,
            value = damage.toDouble(), modifier = null, attackerStrength = attackerStrength.toDouble(), defenderStrength = defenderStrength.toDouble(),
            modifiers = null, randomnessFactor = randomnessFactor.toDouble(), healthRatio = healthRatio.toDouble(), damageToAttacker = damageToAttacker)
        return result?.toFloat() ?: damage
    }

    // endregion
    
    @Readonly
    fun getRandomness(combatant: ICombatant): Float {
        val gameInfo = combatant.getCivInfo().gameInfo
        val allowRandomVariance = UncivGame.Current.settings
            .isRandomVarianceEnabled(gameInfo.gameParameters.isOnlineMultiplayer)
        val random = if (allowRandomVariance)
            Random
        else
            Random(gameInfo.turns
                * combatant.getTile().position.toVector2().hashCode().toLong())
        return random.nextFloat()
    }

    @Readonly
    fun calculateDamageToAttacker(
        attacker: ICombatant,
        defender: ICombatant,
        tileToAttackFrom: Tile = defender.getTile(),
        /** Between 0 and 1. */
        randomnessFactor: Float = getRandomness(attacker)
    ): Int {
        if (attacker.isRanged() && !attacker.isAirUnit()) return 0
        if (defender.isCivilian()) return 0
        val attackerStrength = getAttackingStrength(attacker, defender, tileToAttackFrom)
        val defenderStrength = getDefendingStrength(attacker, defender, tileToAttackFrom)
        // Counter-damage formula is owned by the defender; the attacker may then reduce what it receives.
        val healthRatio = getHealthDependantDamageRatio(defender)
        val raw = applyLuaDamageFormula(defender, attacker, CombatAction.Defend, attackerStrength, defenderStrength, damageToAttacker = true, randomnessFactor, healthRatio)
        val afterReceiver = applyLuaDamageReceived(attacker, defender, CombatAction.Attack, raw, attackerStrength, defenderStrength, randomnessFactor, healthRatio, damageToAttacker = true)
        return afterReceiver.roundToInt().coerceAtLeast(0)
    }

    @Readonly
    fun calculateDamageToDefender(
        attacker: ICombatant,
        defender: ICombatant,
        tileToAttackFrom: Tile = defender.getTile(),
        /** Between 0 and 1.  Defaults to turn and location-based random to avoid save scumming */
        randomnessFactor: Float = getRandomness(defender)
        ,
    ): Int {
        if (defender.isCivilian()) return BattleConstants.DAMAGE_TO_CIVILIAN_UNIT
        val attackerStrength = getAttackingStrength(attacker, defender, tileToAttackFrom)
        val defenderStrength = getDefendingStrength(attacker, defender, tileToAttackFrom)
        // Damage formula is owned by the attacker; the defender may then reduce what it receives.
        val healthRatio = getHealthDependantDamageRatio(attacker)
        val raw = applyLuaDamageFormula(attacker, defender, CombatAction.Attack, attackerStrength, defenderStrength, damageToAttacker = false, randomnessFactor, healthRatio)
        val afterReceiver = applyLuaDamageReceived(defender, attacker, CombatAction.Defend, raw, attackerStrength, defenderStrength, randomnessFactor, healthRatio, damageToAttacker = false)
        return afterReceiver.roundToInt().coerceAtLeast(0)
    }

    @Pure
    private fun damageModifier(
        attackerToDefenderRatio: Float,
        damageToAttacker: Boolean,
        /** Between 0 and 1. */
        randomnessFactor: Float,
    ): Float {
        // https://forums.civfanatics.com/threads/getting-the-combat-damage-math.646582/#post-15468029
        val strongerToWeakerRatio =
            attackerToDefenderRatio.pow(if (attackerToDefenderRatio < 1) -1 else 1)
        var ratioModifier = (((strongerToWeakerRatio + 3) / 4).pow(4) + 1) / 2
        if (damageToAttacker && attackerToDefenderRatio > 1 || !damageToAttacker && attackerToDefenderRatio < 1) // damage ratio from the weaker party is inverted
            ratioModifier = ratioModifier.pow(-1)
        val randomCenteredAround30 = 24 + 12 * randomnessFactor
        return randomCenteredAround30 * ratioModifier
    }
}
enum class CombatAction {
    Attack,
    Defend,
    Intercept,
}

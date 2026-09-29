package com.unciv.logic.multiplayer

/**
 * The intent behind a state change that is not one of the explicitly recorded unit actions
 * (`unit.move`, `unit.attack`, ...).
 *
 * Every one of these used to record itself under the generic `UnitActionType.TriggerUnique` label,
 * which is what let a whole class of actions "look recorded" without saying what they were: a state
 * change that failed to apply could only be reported as `game.state`, and there was no way to tell
 * two of them apart, let alone refuse one of them.
 *
 * [value] is the player-facing name, used when telling a player which of their actions did not apply.
 */
enum class SimultaneousTurnStateAction(val value: String) {
    /** Side effects of a move whose unit result is recorded separately as `unit.move`. */
    Move("Move"),
    /** Side effects of an attack whose result is recorded separately as `unit.attack`. */
    Combat("Combat"),
    /** Side effects of a swap whose unit result is recorded separately as `unit.swap`. */
    Swap("Swap units"),
    Trade("Trade"),
    Diplomacy("Diplomacy"),
    Espionage("Espionage"),
    WorldCongressVote("World Congress vote"),
    Event("Event"),
    GreatPerson("Great Person"),
    Pantheon("Pantheon"),
    Policy("Policy"),
    Technology("Technology"),
    /** A choice made in an alert popup, whose effect depends on the alert. */
    Alert("Alert")
}

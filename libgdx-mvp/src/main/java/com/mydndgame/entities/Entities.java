package com.mydndgame.entities;

import java.util.function.Supplier;

/** Mirrors {@code src/entities.py} factories and reset. */
public final class Entities {

    private Entities() {}

    public static Combatant makePlayer() {
        return new Combatant("Player", 40, 40, 12, 0, 0, 0, 0, "player", null);
    }

    public static Combatant makeGoblin() {
        return new Combatant("Goblin", 25, 25, 10, 0, 0, 0, 0, "goblin", null);
    }

    public static Combatant makeBeast() {
        return new Combatant("Beast", 16, 16, 7, 0, 0, 0, 0, "beast", null);
    }

    public static Combatant makeHobgoblin() {
        return new Combatant("Hobgoblin", 48, 48, 16, 0, 0, 0, 0, "hobgoblin", 3);
    }

    public static Supplier<Combatant> goblinFactory() {
        return Entities::makeGoblin;
    }

    public static Supplier<Combatant> beastFactory() {
        return Entities::makeBeast;
    }

    public static Supplier<Combatant> hobgoblinFactory() {
        return Entities::makeHobgoblin;
    }

    public static void resetBetweenEncounters(Combatant c) {
        c.resetShield();
        c.attackEnhancement = 0;
        c.defenseEnhancement = 0;
        c.hitEnhancement = 0;
    }
}

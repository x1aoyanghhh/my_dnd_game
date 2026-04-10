package com.mydndgame.dice;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Matches Python {@code src/dice.py} {@code resolve_attack_roll}:
 * nat 1 miss, nat 20 hit+crit, else d20 + hitBonus >= AC.
 */
public final class AttackDice {

    private AttackDice() {}

    public static int rollD20() {
        return ThreadLocalRandom.current().nextInt(1, 21);
    }

    public enum Outcome {
        CRITICAL_FAIL,
        CRITICAL_HIT,
        HIT,
        MISS
    }

    public static Outcome resolve(int roll, int hitBonus, int targetAc) {
        if (roll == 1) {
            return Outcome.CRITICAL_FAIL;
        }
        if (roll == 20) {
            return Outcome.CRITICAL_HIT;
        }
        if (roll + hitBonus >= targetAc) {
            return Outcome.HIT;
        }
        return Outcome.MISS;
    }
}

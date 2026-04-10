package com.mydndgame.entities;

/** Mirrors {@code src/entities.py} Combatant. */
public class Combatant {

    public final String name;
    public final int maxHp;
    public int hp;
    public final int ac;
    public int shield;
    public int attackEnhancement;
    public int defenseEnhancement;
    public int hitEnhancement;
    public String monsterTag;
    /** If set, every N-th enemy turn (1-based), gain +1 attack enhancement before acting. */
    public Integer buffAttackEveryNEnemyTurns;

    public Combatant(
        String name,
        int maxHp,
        int hp,
        int ac,
        int shield,
        int attackEnhancement,
        int defenseEnhancement,
        int hitEnhancement,
        String monsterTag,
        Integer buffAttackEveryNEnemyTurns
    ) {
        this.name = name;
        this.maxHp = maxHp;
        this.hp = Math.min(hp, maxHp);
        this.ac = ac;
        this.shield = shield;
        this.attackEnhancement = attackEnhancement;
        this.defenseEnhancement = defenseEnhancement;
        this.hitEnhancement = hitEnhancement;
        this.monsterTag = monsterTag;
        this.buffAttackEveryNEnemyTurns = buffAttackEveryNEnemyTurns;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void addShield(int amount) {
        if (amount > 0) {
            shield += amount;
        }
    }

    public void resetShield() {
        shield = 0;
    }

    public int takeDamage(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int remaining = amount;
        if (shield > 0) {
            int absorbed = Math.min(shield, remaining);
            shield -= absorbed;
            remaining -= absorbed;
        }
        int lost = 0;
        if (remaining > 0) {
            lost = Math.min(remaining, hp);
            hp -= lost;
        }
        return lost;
    }
}

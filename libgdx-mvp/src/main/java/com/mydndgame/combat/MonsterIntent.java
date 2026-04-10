package com.mydndgame.combat;

/** Mirrors Python {@code MonsterIntent}. */
public final class MonsterIntent {

    public final MonsterIntentType intentType;
    public final int value;
    public final int attackHitCount;

    public MonsterIntent(MonsterIntentType intentType, int value, int attackHitCount) {
        this.intentType = intentType;
        this.value = value;
        this.attackHitCount = attackHitCount;
    }

    public MonsterIntent(MonsterIntentType intentType, int value) {
        this(intentType, value, 1);
    }

    public String describe() {
        MonsterIntentType t = intentType;
        if (t == MonsterIntentType.ATTACK) {
            int h = attackHitCount;
            if (h <= 1) {
                return "Attack (base " + value + " per hit; on hit deals base + attack enhancement)";
            }
            return "Attack " + value + " × " + h + " hits (separate rolls; enhancement applies per hit)";
        }
        if (t == MonsterIntentType.DEFEND) {
            return "Defend (gain " + value + " shield + defense enhancement)";
        }
        if (t == MonsterIntentType.BUFF_ATTACK_UP) {
            return "Buff: +" + value + " attack enhancement";
        }
        if (t == MonsterIntentType.BUFF_DEFENSE_UP) {
            return "Buff: +" + value + " defense enhancement";
        }
        return "Buff: +" + value + " hit enhancement";
    }
}

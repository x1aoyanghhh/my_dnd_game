package com.mydndgame.game;

public enum RunPhase {
    /** Overworld 3×3 map; click a neighbor to travel. */
    WORLD_MAP,
    /** Offer attack potion before combat (only if potions > 0). */
    POTION_PROMPT,
    COMBAT,
    SHOP,
    /** Event / chest placeholders (Python prints "not implemented yet"). */
    NODE_STUB,
    RUN_VICTORY,
    GAME_OVER
}

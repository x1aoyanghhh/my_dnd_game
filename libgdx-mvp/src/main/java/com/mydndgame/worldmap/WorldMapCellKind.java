package com.mydndgame.worldmap;

public enum WorldMapCellKind {
    /** Random enemy from pool on enter. */
    COMBAT,
    SHOP,
    /** Random outcome: combat / gold / stub / mini-shop. */
    EVENT
}

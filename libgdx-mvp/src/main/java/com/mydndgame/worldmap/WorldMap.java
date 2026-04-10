package com.mydndgame.worldmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 3×3 grid A–I plus START; edges match the sketch (columns A–D–G, B–E–H, C–F–I; START to A,B,C).
 */
public final class WorldMap {

    private final EnumMap<WorldMapCellId, WorldMapCellKind> kinds = new EnumMap<>(WorldMapCellId.class);
    private final Set<WorldMapCellId> cleared = EnumSet.noneOf(WorldMapCellId.class);
    private WorldMapCellId position = WorldMapCellId.START;

    public WorldMap() {
        resetExampleLayout();
    }

    /**
     * ABC = combat, DEF = shop, GHI = event (per your example).
     */
    public void resetExampleLayout() {
        kinds.clear();
        cleared.clear();
        position = WorldMapCellId.START;
        putRowCombat(WorldMapCellId.A, WorldMapCellId.B, WorldMapCellId.C);
        putRowShop(WorldMapCellId.D, WorldMapCellId.E, WorldMapCellId.F);
        putRowEvent(WorldMapCellId.G, WorldMapCellId.H, WorldMapCellId.I);
    }

    private void putRowCombat(WorldMapCellId a, WorldMapCellId b, WorldMapCellId c) {
        kinds.put(a, WorldMapCellKind.COMBAT);
        kinds.put(b, WorldMapCellKind.COMBAT);
        kinds.put(c, WorldMapCellKind.COMBAT);
    }

    private void putRowShop(WorldMapCellId a, WorldMapCellId b, WorldMapCellId c) {
        kinds.put(a, WorldMapCellKind.SHOP);
        kinds.put(b, WorldMapCellKind.SHOP);
        kinds.put(c, WorldMapCellKind.SHOP);
    }

    private void putRowEvent(WorldMapCellId a, WorldMapCellId b, WorldMapCellId c) {
        kinds.put(a, WorldMapCellKind.EVENT);
        kinds.put(b, WorldMapCellKind.EVENT);
        kinds.put(c, WorldMapCellKind.EVENT);
    }

    public WorldMapCellId getPosition() {
        return position;
    }

    public void setPosition(WorldMapCellId id) {
        this.position = id;
    }

    public WorldMapCellKind getKind(WorldMapCellId id) {
        return kinds.get(id);
    }

    public boolean isMainCell(WorldMapCellId id) {
        return id != WorldMapCellId.START && kinds.containsKey(id);
    }

    public boolean isCleared(WorldMapCellId id) {
        return cleared.contains(id);
    }

    public void markCleared(WorldMapCellId id) {
        if (id != WorldMapCellId.START) {
            cleared.add(id);
        }
    }

    /** True when A–I are all cleared. */
    public boolean isRunWon() {
        for (WorldMapCellId id : WorldMapCellId.values()) {
            if (id == WorldMapCellId.START) {
                continue;
            }
            if (!cleared.contains(id)) {
                return false;
            }
        }
        return true;
    }

    public static List<WorldMapCellId> neighbors(WorldMapCellId id) {
        switch (id) {
            case START:
                return list(WorldMapCellId.A, WorldMapCellId.B, WorldMapCellId.C);
            case A:
                return list(WorldMapCellId.START, WorldMapCellId.D);
            case B:
                return list(WorldMapCellId.START, WorldMapCellId.E);
            case C:
                return list(WorldMapCellId.START, WorldMapCellId.F);
            case D:
                return list(WorldMapCellId.A, WorldMapCellId.G);
            case E:
                return list(WorldMapCellId.B, WorldMapCellId.H);
            case F:
                return list(WorldMapCellId.C, WorldMapCellId.I);
            case G:
                return Collections.singletonList(WorldMapCellId.D);
            case H:
                return Collections.singletonList(WorldMapCellId.E);
            case I:
                return Collections.singletonList(WorldMapCellId.F);
            default:
                return Collections.emptyList();
        }
    }

    public boolean canTravelTo(WorldMapCellId from, WorldMapCellId to) {
        return neighbors(from).contains(to);
    }

    /** Reachable in one step from current position (for UI). */
    public List<WorldMapCellId> reachableFromCurrent() {
        return new ArrayList<>(neighbors(position));
    }

    private static List<WorldMapCellId> list(WorldMapCellId a, WorldMapCellId b, WorldMapCellId c) {
        List<WorldMapCellId> out = new ArrayList<>(3);
        out.add(a);
        out.add(b);
        out.add(c);
        return out;
    }

    private static List<WorldMapCellId> list(WorldMapCellId a, WorldMapCellId b) {
        List<WorldMapCellId> out = new ArrayList<>(2);
        out.add(a);
        out.add(b);
        return out;
    }
}

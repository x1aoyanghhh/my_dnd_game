package com.mydndgame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.mydndgame.cards.Card;
import com.mydndgame.combat.Combat;
import com.mydndgame.entities.Combatant;
import com.mydndgame.game.GameRunState;
import com.mydndgame.game.RunPhase;
import com.mydndgame.shop.Shop;
import com.mydndgame.shop.ShopListing;
import com.mydndgame.worldmap.WorldMap;
import com.mydndgame.worldmap.WorldMapCellId;
import com.mydndgame.worldmap.WorldMapCellKind;

import java.util.ArrayList;
import java.util.List;

public class MvpGame extends ApplicationAdapter {

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private BitmapFont fontSmall;
    private GameRunState game;

    private static final float PAD = 10f;
    private static final float CARD_H = 72f;
    private static final float CARD_GAP = 6f;
    private static final float MAX_CARD_W = 118f;
    private static final int LOG_LINES = 26;

    /** Combat arena: avatars + HP bars (set in {@link #layoutCombatArena}). */
    private static final float AVATAR_RADIUS = 30f;
    private static final float HP_BAR_W = 160f;
    private static final float HP_BAR_H = 18f;
    private static final float AVATAR_BAR_GAP = 12f;
    private static final float HP_BORDER = 2f;
    private static final int CIRCLE_SEGMENTS = 48;

    private float arenaPlayerCx;
    private float arenaPlayerCy;
    private float arenaEnemyCx;
    private float arenaEnemyCy;
    private Rectangle hpBarPlayerOutline;
    private Rectangle hpBarEnemyOutline;

    private final List<String> log = new ArrayList<>();
    private final List<Rectangle> cardRects = new ArrayList<>();
    private final Matrix4 screenOrtho = new Matrix4();

    private Rectangle endTurnBtn;
    private Rectangle shopLeaveBtn;
    private final List<Rectangle> shopItemRects = new ArrayList<>();
    private Rectangle potionYesBtn;
    private Rectangle potionNoBtn;
    private Rectangle stubContinueBtn;
    private Rectangle newRunBtn;

    private static final float MAP_NODE_R = 34f;
    private final List<MapClickZone> mapClickZones = new ArrayList<>();
    private float mapYA;
    private float mapYD;
    private float mapYG;
    private float mapYStart;
    private float mapCxL;
    private float mapCxM;
    private float mapCxR;

    private static final class MapClickZone {
        final WorldMapCellId id;
        final float cx;
        final float cy;

        MapClickZone(WorldMapCellId id, float cx, float cy) {
            this.id = id;
            this.cx = cx;
            this.cy = cy;
        }

        boolean contains(float x, float y) {
            float dx = x - cx;
            float dy = y - cy;
            return dx * dx + dy * dy <= MAP_NODE_R * MAP_NODE_R;
        }
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.05f);
        fontSmall = new BitmapFont();
        fontSmall.setColor(new Color(0.8f, 0.82f, 0.85f, 1f));
        fontSmall.getData().setScale(0.9f);

        game = new GameRunState();
        game.startRun();
        clearLog();
        logPhaseHelp();

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                float yFlip = Gdx.graphics.getHeight() - screenY;
                handleTap(screenX, yFlip);
                return true;
            }
        });
    }

    private void clearLog() {
        log.clear();
    }

    private void logPhaseHelp() {
        appendLog("Map: 3x3 A-I. Sword icon = combat, $ = shop, ? = event. Clear all to win.");
        appendLog("Click a connected node to travel. Gold starts at 20.");
        appendLog("Combat: nat 1 miss, nat 20 crit ×2. End Turn ends your turn.");
    }

    private void appendLog(String line) {
        if (line == null || line.isEmpty()) {
            return;
        }
        log.add(line);
        while (log.size() > LOG_LINES) {
            log.remove(0);
        }
    }

    private void handleTap(float x, float y) {
        RunPhase ph = game.phase;
        if (ph == RunPhase.RUN_VICTORY || ph == RunPhase.GAME_OVER) {
            if (newRunBtn != null && newRunBtn.contains(x, y)) {
                clearLog();
                game.startRun();
                logPhaseHelp();
                appendLog("New run started.");
            }
            return;
        }
        if (ph == RunPhase.POTION_PROMPT) {
            if (potionYesBtn != null && potionYesBtn.contains(x, y)) {
                game.applyMapPotionChoice(true);
                appendLog("+1 attack enhancement this combat (potion).");
            } else if (potionNoBtn != null && potionNoBtn.contains(x, y)) {
                game.applyMapPotionChoice(false);
            }
            return;
        }
        if (ph == RunPhase.WORLD_MAP) {
            for (MapClickZone z : mapClickZones) {
                if (z.contains(x, y)) {
                    String msg = game.tryTravelTo(z.id);
                    if (msg != null && !msg.isEmpty()) {
                        appendLog(msg);
                    }
                    return;
                }
            }
            return;
        }
        if (ph == RunPhase.SHOP) {
            if (shopLeaveBtn != null && shopLeaveBtn.contains(x, y)) {
                game.leaveShop();
                return;
            }
            List<ShopListing> listings = Shop.shopListings();
            for (int i = 0; i < shopItemRects.size() && i < listings.size(); i++) {
                if (shopItemRects.get(i).contains(x, y)) {
                    String msg = game.tryBuy(listings.get(i));
                    if (!msg.isEmpty()) {
                        appendLog(msg);
                    }
                    return;
                }
            }
            return;
        }
        if (ph == RunPhase.NODE_STUB) {
            if (stubContinueBtn != null && stubContinueBtn.contains(x, y)) {
                game.continueStubNode();
                return;
            }
            return;
        }
        if (ph != RunPhase.COMBAT || game.combat == null) {
            return;
        }
        Combat c = game.combat;
        if (c.isCombatOver()) {
            return;
        }
        for (int i = 0; i < cardRects.size(); i++) {
            if (cardRects.get(i).contains(x, y) && c.canPlay(i)) {
                for (String line : game.playCard(i)) {
                    appendLog(line);
                }
                if (game.phase == RunPhase.GAME_OVER) {
                    appendLog("You are defeated.");
                }
                return;
            }
        }
        if (endTurnBtn != null && endTurnBtn.contains(x, y)) {
            for (String line : game.endPlayerTurn()) {
                appendLog(line);
            }
            if (game.phase == RunPhase.GAME_OVER) {
                appendLog("You are defeated.");
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        screenOrtho.setToOrtho2D(0f, 0f, width, height);
        batch.setProjectionMatrix(screenOrtho);
        shapes.setProjectionMatrix(screenOrtho);
    }

    @Override
    public void render() {
        drainQueuedMessages();

        Gdx.gl.glClearColor(0.1f, 0.11f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float yTop = h - PAD;

        layoutUi(w, h);

        if (game.phase == RunPhase.WORLD_MAP) {
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(0.42f, 0.45f, 0.5f, 0.95f);
            drawMapEdges();
            shapes.end();
        }

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (game.phase == RunPhase.WORLD_MAP) {
            drawMapNodesFilled();
        }
        if (game.phase == RunPhase.COMBAT && game.combat != null && !game.combat.isCombatOver()) {
            drawCombatAvatarsAndHealth();
        }
        drawPanelBackgrounds();
        shapes.end();

        if (game.phase == RunPhase.WORLD_MAP) {
            shapes.begin(ShapeRenderer.ShapeType.Line);
            drawMapNodeOutlinesAndSwords();
            shapes.end();
        }

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.92f, 0.9f, 0.82f, 1f);
        if (hpBarPlayerOutline != null) {
            shapes.rect(hpBarPlayerOutline.x, hpBarPlayerOutline.y, hpBarPlayerOutline.width, hpBarPlayerOutline.height);
        }
        if (hpBarEnemyOutline != null) {
            shapes.rect(hpBarEnemyOutline.x, hpBarEnemyOutline.y, hpBarEnemyOutline.width, hpBarEnemyOutline.height);
        }
        shapes.setColor(0.85f, 0.88f, 0.9f, 0.5f);
        for (Rectangle r : cardRects) {
            shapes.rect(r.x, r.y, r.width, r.height);
        }
        shapes.end();

        batch.begin();
        RunPhase ph = game.phase;
        font.setColor(Color.WHITE);
        font.draw(batch, bannerLine(), PAD, yTop);
        yTop -= 22f;

        if (ph == RunPhase.COMBAT && game.combat != null) {
            Combat co = game.combat;
            Combatant p = game.player;
            Combatant e = co.enemy;
            fontSmall.setColor(0.75f, 0.9f, 1f, 1f);
            fontSmall.draw(batch, "Intent: " + co.monsterIntent.describe(), PAD, yTop);
            yTop -= 18f;
            fontSmall.setColor(Color.WHITE);
            fontSmall.draw(
                batch,
                p.name + "  AC " + p.ac + "  shield " + p.shield
                    + "  |  AP " + co.actionPoints + "/" + co.maxActionPoints
                    + "  |  ATK+" + p.attackEnhancement + " DEF+" + p.defenseEnhancement + " HIT+" + p.hitEnhancement,
                PAD,
                yTop
            );
            yTop -= 18f;
            fontSmall.draw(
                batch,
                e.name + "  AC " + e.ac + "  shield " + e.shield
                    + "  |  ATK+" + e.attackEnhancement + " DEF+" + e.defenseEnhancement + " HIT+" + e.hitEnhancement,
                PAD,
                yTop
            );
            yTop -= 22f;

            if (hpBarPlayerOutline != null && hpBarEnemyOutline != null) {
                fontSmall.setColor(0.7f, 0.95f, 0.75f, 1f);
                String ps = p.hp + "/" + p.maxHp;
                fontSmall.draw(batch, ps, arenaPlayerCx - 22f, hpBarPlayerOutline.y - 4f);
                fontSmall.setColor(1f, 0.65f, 0.6f, 1f);
                String es = e.hp + "/" + e.maxHp;
                fontSmall.draw(batch, es, arenaEnemyCx - 22f, hpBarEnemyOutline.y - 4f);
                fontSmall.setColor(Color.WHITE);
            }
        } else {
            fontSmall.setColor(0.85f, 0.85f, 0.88f, 1f);
            fontSmall.draw(batch, "Gold: " + game.gold + "  |  Potions: " + game.attackPotions, PAD, yTop);
            yTop -= 20f;
            if (ph == RunPhase.SHOP) {
                fontSmall.draw(batch, "Wandering merchant or map shop", PAD, yTop);
                yTop -= 20f;
            }
        }

        if (ph == RunPhase.WORLD_MAP) {
            drawMapLabels(batch);
        }

        float logTop = Math.min(yTop, h - PAD - 120f);
        fontSmall.setColor(0.72f, 0.74f, 0.78f, 1f);
        for (String line : log) {
            fontSmall.draw(batch, line, PAD + 4f, logTop);
            logTop -= 16f;
        }

        drawPhaseControls(batch, w, h, ph);
        batch.end();
    }

    private void drainQueuedMessages() {
        if (game.pendingBannerLine != null) {
            appendLog(game.pendingBannerLine);
            game.pendingBannerLine = null;
        }
        if (game.pendingVictoryLine != null) {
            appendLog(game.pendingVictoryLine);
            game.pendingVictoryLine = null;
        }
        if (game.pendingLootLine != null) {
            appendLog(game.pendingLootLine);
            game.pendingLootLine = null;
        }
    }

    private String bannerLine() {
        WorldMapCellId pos = game.worldMap.getPosition();
        switch (game.phase) {
            case WORLD_MAP:
                return "Map — You are at " + pos.name() + "  |  Gold " + game.gold;
            case POTION_PROMPT:
                return "Potion — at " + pos.name() + "  |  Gold " + game.gold;
            case COMBAT:
                return "Combat — at " + pos.name() + "  |  Gold " + game.gold;
            case SHOP:
                return "Shop — at " + pos.name() + "  |  Gold " + game.gold;
            case NODE_STUB:
                return "Event — " + game.stubNodeTitle;
            case RUN_VICTORY:
                return "Run complete!";
            case GAME_OVER:
                return "Game over";
            default:
                return "—";
        }
    }

    private void layoutUi(float w, float h) {
        cardRects.clear();
        shopItemRects.clear();
        hpBarPlayerOutline = null;
        hpBarEnemyOutline = null;
        endTurnBtn = null;
        shopLeaveBtn = null;
        potionYesBtn = null;
        potionNoBtn = null;
        stubContinueBtn = null;
        newRunBtn = null;

        float btnH = 36f;
        float btnW = 140f;

        mapClickZones.clear();

        RunPhase ph = game.phase;
        if (ph == RunPhase.RUN_VICTORY || ph == RunPhase.GAME_OVER) {
            newRunBtn = new Rectangle(w / 2f - 80f, h / 2f - 20f, 160f, 40f);
            return;
        }
        if (ph == RunPhase.WORLD_MAP) {
            layoutMapGeometry(w, h);
            return;
        }
        if (ph == RunPhase.POTION_PROMPT) {
            potionYesBtn = new Rectangle(w / 2f - btnW - 8f, PAD + 20f, btnW, btnH);
            potionNoBtn = new Rectangle(w / 2f + 8f, PAD + 20f, btnW, btnH);
            return;
        }
        if (ph == RunPhase.SHOP) {
            float y = PAD + 50f;
            for (int i = 0; i < 3; i++) {
                shopItemRects.add(new Rectangle(PAD, y, Math.min(420f, w - 2 * PAD), btnH + 8f));
                y += btnH + 16f;
            }
            shopLeaveBtn = new Rectangle(PAD, PAD + 8f, 100f, btnH);
            return;
        }
        if (ph == RunPhase.NODE_STUB) {
            stubContinueBtn = new Rectangle(PAD, h / 2f - 30f, 200f, 44f);
            return;
        }
        if (ph == RunPhase.COMBAT && game.combat != null && !game.combat.isCombatOver()) {
            List<Card> hand = game.combat.getHand();
            int n = hand.size();
            float rowY = PAD + 8f;
            float avail = w - 2f * PAD - 150f;
            float cw = n <= 0 ? MAX_CARD_W : Math.min(MAX_CARD_W, (avail - (n - 1) * CARD_GAP) / n);
            float startX = PAD;
            for (int i = 0; i < n; i++) {
                cardRects.add(new Rectangle(startX + i * (cw + CARD_GAP), rowY, cw, CARD_H));
            }
            endTurnBtn = new Rectangle(w - PAD - 130f, rowY + 18f, 120f, btnH);
            layoutCombatArena(w, h, rowY + CARD_H + 16f);
        }
    }

    private void layoutMapGeometry(float w, float h) {
        mapCxL = w * 0.22f;
        mapCxM = w * 0.5f;
        mapCxR = w * 0.78f;
        mapYA = 125f;
        float rowStep = (h - 210f) / 3.2f;
        if (rowStep < 64f) {
            rowStep = 64f;
        }
        mapYD = mapYA + rowStep;
        mapYG = mapYD + rowStep;
        mapYStart = mapYA - 58f;

        mapClickZones.add(new MapClickZone(WorldMapCellId.START, mapCxM, mapYStart));
        mapClickZones.add(new MapClickZone(WorldMapCellId.A, mapCxL, mapYA));
        mapClickZones.add(new MapClickZone(WorldMapCellId.B, mapCxM, mapYA));
        mapClickZones.add(new MapClickZone(WorldMapCellId.C, mapCxR, mapYA));
        mapClickZones.add(new MapClickZone(WorldMapCellId.D, mapCxL, mapYD));
        mapClickZones.add(new MapClickZone(WorldMapCellId.E, mapCxM, mapYD));
        mapClickZones.add(new MapClickZone(WorldMapCellId.F, mapCxR, mapYD));
        mapClickZones.add(new MapClickZone(WorldMapCellId.G, mapCxL, mapYG));
        mapClickZones.add(new MapClickZone(WorldMapCellId.H, mapCxM, mapYG));
        mapClickZones.add(new MapClickZone(WorldMapCellId.I, mapCxR, mapYG));
    }

    private void drawMapEdges() {
        mapLine(mapCxM, mapYStart, mapCxL, mapYA);
        mapLine(mapCxM, mapYStart, mapCxM, mapYA);
        mapLine(mapCxM, mapYStart, mapCxR, mapYA);
        mapLine(mapCxL, mapYA, mapCxL, mapYD);
        mapLine(mapCxM, mapYA, mapCxM, mapYD);
        mapLine(mapCxR, mapYA, mapCxR, mapYD);
        mapLine(mapCxL, mapYD, mapCxL, mapYG);
        mapLine(mapCxM, mapYD, mapCxM, mapYG);
        mapLine(mapCxR, mapYD, mapCxR, mapYG);
    }

    private void mapLine(float x1, float y1, float x2, float y2) {
        shapes.line(x1, y1, x2, y2);
    }

    private void drawMapNodesFilled() {
        WorldMap wm = game.worldMap;
        WorldMapCellId here = wm.getPosition();
        List<WorldMapCellId> reach = wm.reachableFromCurrent();

        drawOneMapNode(WorldMapCellId.START, mapCxM, mapYStart, here, reach, null);

        drawCellNode(WorldMapCellId.A, mapCxL, mapYA, here, reach, wm);
        drawCellNode(WorldMapCellId.B, mapCxM, mapYA, here, reach, wm);
        drawCellNode(WorldMapCellId.C, mapCxR, mapYA, here, reach, wm);
        drawCellNode(WorldMapCellId.D, mapCxL, mapYD, here, reach, wm);
        drawCellNode(WorldMapCellId.E, mapCxM, mapYD, here, reach, wm);
        drawCellNode(WorldMapCellId.F, mapCxR, mapYD, here, reach, wm);
        drawCellNode(WorldMapCellId.G, mapCxL, mapYG, here, reach, wm);
        drawCellNode(WorldMapCellId.H, mapCxM, mapYG, here, reach, wm);
        drawCellNode(WorldMapCellId.I, mapCxR, mapYG, here, reach, wm);
    }

    private void drawCellNode(
        WorldMapCellId id,
        float cx,
        float cy,
        WorldMapCellId here,
        List<WorldMapCellId> reach,
        WorldMap wm
    ) {
        WorldMapCellKind k = wm.getKind(id);
        drawOneMapNode(id, cx, cy, here, reach, k);
    }

    private void drawOneMapNode(
        WorldMapCellId id,
        float cx,
        float cy,
        WorldMapCellId here,
        List<WorldMapCellId> reach,
        WorldMapCellKind kind
    ) {
        boolean cleared = game.worldMap.isCleared(id);
        boolean current = id == here;
        boolean canStep = reach.contains(id);

        float r = MAP_NODE_R;
        if (id == WorldMapCellId.START) {
            if (current) {
                shapes.setColor(0.38f, 0.44f, 0.65f, 1f);
            } else {
                shapes.setColor(0.25f, 0.28f, 0.38f, 1f);
            }
            shapes.circle(cx, cy, r, CIRCLE_SEGMENTS);
            return;
        }

        float rr;
        float gg;
        float bb;
        if (cleared) {
            rr = 0.22f;
            gg = 0.22f;
            bb = 0.24f;
        } else if (kind == WorldMapCellKind.COMBAT) {
            rr = 0.4f;
            gg = 0.22f;
            bb = 0.24f;
        } else if (kind == WorldMapCellKind.SHOP) {
            rr = 0.2f;
            gg = 0.36f;
            bb = 0.28f;
        } else {
            rr = 0.34f;
            gg = 0.24f;
            bb = 0.4f;
        }
        if (current) {
            rr = 0.42f;
            gg = 0.52f;
            bb = 0.88f;
        } else if (canStep && !cleared) {
            rr = Math.min(1f, rr * 1.15f);
            gg = Math.min(1f, gg * 1.15f);
            bb = Math.min(1f, bb * 1.12f);
        }
        shapes.setColor(rr, gg, bb, 1f);
        shapes.circle(cx, cy, r, CIRCLE_SEGMENTS);
    }

    private void drawMapNodeOutlinesAndSwords() {
        WorldMap wm = game.worldMap;
        WorldMapCellId here = wm.getPosition();
        List<WorldMapCellId> reach = wm.reachableFromCurrent();

        shapes.setColor(0.88f, 0.9f, 0.92f, 0.85f);
        outlineNode(mapCxM, mapYStart, here == WorldMapCellId.START, reach.contains(WorldMapCellId.START));

        outlineCell(WorldMapCellId.A, mapCxL, mapYA, here, reach, wm);
        outlineCell(WorldMapCellId.B, mapCxM, mapYA, here, reach, wm);
        outlineCell(WorldMapCellId.C, mapCxR, mapYA, here, reach, wm);
        outlineCell(WorldMapCellId.D, mapCxL, mapYD, here, reach, wm);
        outlineCell(WorldMapCellId.E, mapCxM, mapYD, here, reach, wm);
        outlineCell(WorldMapCellId.F, mapCxR, mapYD, here, reach, wm);
        outlineCell(WorldMapCellId.G, mapCxL, mapYG, here, reach, wm);
        outlineCell(WorldMapCellId.H, mapCxM, mapYG, here, reach, wm);
        outlineCell(WorldMapCellId.I, mapCxR, mapYG, here, reach, wm);

        shapes.setColor(0.95f, 0.92f, 0.75f, 1f);
        drawSwordIfCombat(WorldMapCellId.A, mapCxL, mapYA, wm);
        drawSwordIfCombat(WorldMapCellId.B, mapCxM, mapYA, wm);
        drawSwordIfCombat(WorldMapCellId.C, mapCxR, mapYA, wm);
        drawSwordIfCombat(WorldMapCellId.D, mapCxL, mapYD, wm);
        drawSwordIfCombat(WorldMapCellId.E, mapCxM, mapYD, wm);
        drawSwordIfCombat(WorldMapCellId.F, mapCxR, mapYD, wm);
        drawSwordIfCombat(WorldMapCellId.G, mapCxL, mapYG, wm);
        drawSwordIfCombat(WorldMapCellId.H, mapCxM, mapYG, wm);
        drawSwordIfCombat(WorldMapCellId.I, mapCxR, mapYG, wm);
    }

    private void outlineNode(float cx, float cy, boolean current, boolean neighbor) {
        if (current) {
            shapes.setColor(0.35f, 0.75f, 1f, 1f);
        } else if (neighbor) {
            shapes.setColor(0.45f, 0.85f, 0.55f, 1f);
        } else {
            shapes.setColor(0.55f, 0.58f, 0.62f, 0.9f);
        }
        shapes.circle(cx, cy, MAP_NODE_R + 2f, CIRCLE_SEGMENTS);
    }

    private void outlineCell(
        WorldMapCellId id,
        float cx,
        float cy,
        WorldMapCellId here,
        List<WorldMapCellId> reach,
        WorldMap wm
    ) {
        boolean current = id == here;
        boolean neighbor = reach.contains(id);
        if (wm.isCleared(id)) {
            shapes.setColor(0.4f, 0.4f, 0.42f, 0.7f);
        } else if (current) {
            shapes.setColor(0.35f, 0.75f, 1f, 1f);
        } else if (neighbor) {
            shapes.setColor(0.4f, 0.88f, 0.5f, 1f);
        } else {
            shapes.setColor(0.55f, 0.58f, 0.62f, 0.85f);
        }
        shapes.circle(cx, cy, MAP_NODE_R + 2f, CIRCLE_SEGMENTS);
    }

    private void drawSwordIfCombat(WorldMapCellId id, float cx, float cy, WorldMap wm) {
        if (wm.isCleared(id) || wm.getKind(id) != WorldMapCellKind.COMBAT) {
            return;
        }
        float s = 14f;
        shapes.line(cx - 1.5f, cy + s, cx - 1.5f, cy - s);
        shapes.line(cx - s * 0.55f, cy + 2f, cx + s * 0.75f, cy + 2f);
        shapes.line(cx - 3f, cy - s + 4f, cx + 2f, cy - s);
    }

    private void drawMapLabels(SpriteBatch batch) {
        fontSmall.setColor(0.9f, 0.92f, 0.95f, 1f);
        fontSmall.draw(batch, "起始点", mapCxM - 28f, mapYStart - MAP_NODE_R - 8f);

        labelCell(batch, WorldMapCellId.A, "A", mapCxL, mapYA);
        labelCell(batch, WorldMapCellId.B, "B", mapCxM, mapYA);
        labelCell(batch, WorldMapCellId.C, "C", mapCxR, mapYA);
        labelCell(batch, WorldMapCellId.D, "D", mapCxL, mapYD);
        labelCell(batch, WorldMapCellId.E, "E", mapCxM, mapYD);
        labelCell(batch, WorldMapCellId.F, "F", mapCxR, mapYD);
        labelCell(batch, WorldMapCellId.G, "G", mapCxL, mapYG);
        labelCell(batch, WorldMapCellId.H, "H", mapCxM, mapYG);
        labelCell(batch, WorldMapCellId.I, "I", mapCxR, mapYG);

        fontSmall.setColor(0.65f, 0.68f, 0.72f, 1f);
        fontSmall.draw(batch, "Lines: sword=combat, $=shop, ?=event. Green ring = one step away.", PAD, 88f);
        fontSmall.setColor(Color.WHITE);
    }

    private void labelCell(SpriteBatch batch, WorldMapCellId id, String letter, float cx, float cy) {
        WorldMap wm = game.worldMap;
        WorldMapCellKind k = wm.getKind(id);
        boolean cleared = wm.isCleared(id);

        if (!cleared && k == WorldMapCellKind.SHOP) {
            fontSmall.setColor(0.45f, 0.95f, 0.55f, 1f);
            float prev = fontSmall.getData().scaleX;
            fontSmall.getData().setScale(1.45f);
            fontSmall.draw(batch, "$", cx - 6f, cy + 10f);
            fontSmall.getData().setScale(prev);
        } else if (!cleared && k == WorldMapCellKind.EVENT) {
            fontSmall.setColor(0.98f, 0.88f, 0.4f, 1f);
            float prev = fontSmall.getData().scaleX;
            fontSmall.getData().setScale(1.55f);
            fontSmall.draw(batch, "?", cx - 5f, cy + 10f);
            fontSmall.getData().setScale(prev);
        }

        if (cleared) {
            fontSmall.setColor(0.48f, 0.5f, 0.53f, 1f);
        } else {
            fontSmall.setColor(0.92f, 0.93f, 0.95f, 1f);
        }
        fontSmall.draw(batch, letter, cx - 5f, cy - MAP_NODE_R - 6f);
        fontSmall.setColor(Color.WHITE);
    }

    /**
     * Vertical band between header text and hand row: player left, enemy right, both vertically centered.
     */
    private void layoutCombatArena(float w, float h, float handAreaTop) {
        float headerBottom = h - PAD - 22f - 18f - 18f - 18f - 8f;
        float mid = (handAreaTop + headerBottom) * 0.5f;
        arenaPlayerCx = w * 0.22f;
        arenaEnemyCx = w * 0.78f;
        arenaPlayerCy = mid;
        arenaEnemyCy = mid;

        float barBottomP = arenaPlayerCy - AVATAR_RADIUS - AVATAR_BAR_GAP - HP_BAR_H;
        float barBottomE = arenaEnemyCy - AVATAR_RADIUS - AVATAR_BAR_GAP - HP_BAR_H;
        float barLeftP = arenaPlayerCx - HP_BAR_W * 0.5f;
        float barLeftE = arenaEnemyCx - HP_BAR_W * 0.5f;
        hpBarPlayerOutline = new Rectangle(barLeftP, barBottomP, HP_BAR_W, HP_BAR_H);
        hpBarEnemyOutline = new Rectangle(barLeftE, barBottomE, HP_BAR_W, HP_BAR_H);
    }

    private void drawCombatAvatarsAndHealth() {
        Combatant p = game.player;
        Combatant e = game.combat.enemy;

        shapes.setColor(0.22f, 0.52f, 0.95f, 1f);
        shapes.circle(arenaPlayerCx, arenaPlayerCy, AVATAR_RADIUS, CIRCLE_SEGMENTS);

        shapes.setColor(0.92f, 0.28f, 0.22f, 1f);
        shapes.circle(arenaEnemyCx, arenaEnemyCy, AVATAR_RADIUS, CIRCLE_SEGMENTS);

        float hpP = p.maxHp <= 0 ? 0f : Math.min(1f, (float) p.hp / (float) p.maxHp);
        float hpE = e.maxHp <= 0 ? 0f : Math.min(1f, (float) e.hp / (float) e.maxHp);

        drawBorderedHealthBar(hpBarPlayerOutline, hpP, 0.2f, 0.78f, 0.35f);
        drawBorderedHealthBar(hpBarEnemyOutline, hpE, 0.9f, 0.22f, 0.2f);
    }

    /**
     * Outer outline drawn separately in Line pass; inner track dark; fill grows from the left by HP ratio.
     */
    private void drawBorderedHealthBar(Rectangle outer, float hpRatio, float fillR, float fillG, float fillB) {
        if (outer == null) {
            return;
        }
        float innerX = outer.x + HP_BORDER;
        float innerY = outer.y + HP_BORDER;
        float innerW = outer.width - 2f * HP_BORDER;
        float innerH = outer.height - 2f * HP_BORDER;
        shapes.setColor(0.12f, 0.12f, 0.14f, 1f);
        shapes.rect(innerX, innerY, innerW, innerH);
        float fillW = Math.max(0f, innerW * hpRatio);
        shapes.setColor(fillR, fillG, fillB, 1f);
        shapes.rect(innerX, innerY, fillW, innerH);
    }

    private void drawPanelBackgrounds() {
        shapes.setColor(0.18f, 0.2f, 0.24f, 1f);
        for (int i = 0; i < cardRects.size(); i++) {
            Rectangle r = cardRects.get(i);
            boolean can = game.combat != null && i < game.combat.getHand().size() && game.combat.canPlay(i);
            if (can) {
                shapes.setColor(0.22f, 0.38f, 0.3f, 1f);
            } else {
                shapes.setColor(0.28f, 0.28f, 0.3f, 1f);
            }
            shapes.rect(r.x, r.y, r.width, r.height);
        }
        if (endTurnBtn != null) {
            shapes.setColor(0.35f, 0.28f, 0.2f, 1f);
            shapes.rect(endTurnBtn.x, endTurnBtn.y, endTurnBtn.width, endTurnBtn.height);
        }
        if (shopLeaveBtn != null) {
            shapes.setColor(0.3f, 0.25f, 0.35f, 1f);
            shapes.rect(shopLeaveBtn.x, shopLeaveBtn.y, shopLeaveBtn.width, shopLeaveBtn.height);
        }
        for (Rectangle r : shopItemRects) {
            shapes.setColor(0.22f, 0.32f, 0.4f, 1f);
            shapes.rect(r.x, r.y, r.width, r.height);
        }
        if (potionYesBtn != null) {
            shapes.setColor(0.2f, 0.35f, 0.28f, 1f);
            shapes.rect(potionYesBtn.x, potionYesBtn.y, potionYesBtn.width, potionYesBtn.height);
        }
        if (potionNoBtn != null) {
            shapes.setColor(0.35f, 0.3f, 0.25f, 1f);
            shapes.rect(potionNoBtn.x, potionNoBtn.y, potionNoBtn.width, potionNoBtn.height);
        }
        if (stubContinueBtn != null) {
            shapes.setColor(0.25f, 0.28f, 0.38f, 1f);
            shapes.rect(stubContinueBtn.x, stubContinueBtn.y, stubContinueBtn.width, stubContinueBtn.height);
        }
        if (newRunBtn != null) {
            shapes.setColor(0.3f, 0.35f, 0.45f, 1f);
            shapes.rect(newRunBtn.x, newRunBtn.y, newRunBtn.width, newRunBtn.height);
        }
    }

    private void drawPhaseControls(SpriteBatch batch, float w, float h, RunPhase ph) {
        if (ph == RunPhase.RUN_VICTORY || ph == RunPhase.GAME_OVER) {
            font.setColor(1f, 0.95f, 0.75f, 1f);
            String msg = ph == RunPhase.RUN_VICTORY ? "You cleared the map!" : "You were defeated.";
            font.draw(batch, msg, w / 2f - 120f, h / 2f + 40f);
            font.setColor(Color.WHITE);
            if (newRunBtn != null) {
                font.draw(batch, "New Run", newRunBtn.x + 40f, newRunBtn.y + 26f);
            }
            return;
        }
        if (ph == RunPhase.POTION_PROMPT) {
            font.draw(batch, "Drink Attack Potion? (+1 attack dmg this combat)", PAD, h / 2f + 40f);
            if (potionYesBtn != null) {
                font.draw(batch, "Yes", potionYesBtn.x + 50f, potionYesBtn.y + 24f);
            }
            if (potionNoBtn != null) {
                font.draw(batch, "No", potionNoBtn.x + 55f, potionNoBtn.y + 24f);
            }
            return;
        }
        if (ph == RunPhase.SHOP) {
            List<ShopListing> listings = Shop.shopListings();
            for (int i = 0; i < listings.size() && i < shopItemRects.size(); i++) {
                ShopListing s = listings.get(i);
                Rectangle r = shopItemRects.get(i);
                font.draw(batch, "[" + s.key + "] " + s.label + " — " + s.price + "g", r.x + 8f, r.y + r.height - 10f);
            }
            if (shopLeaveBtn != null) {
                fontSmall.setColor(Color.WHITE);
                fontSmall.draw(batch, "Leave", shopLeaveBtn.x + 28f, shopLeaveBtn.y + 24f);
            }
            return;
        }
        if (ph == RunPhase.NODE_STUB) {
            font.draw(batch, "Something odd happens… (random event stub)", PAD, h / 2f + 20f);
            if (stubContinueBtn != null) {
                font.draw(batch, "Continue", stubContinueBtn.x + 50f, stubContinueBtn.y + 28f);
            }
            return;
        }
        if (ph == RunPhase.COMBAT && game.combat != null && !game.combat.isCombatOver()) {
            List<Card> hand = game.combat.getHand();
            for (int i = 0; i < hand.size() && i < cardRects.size(); i++) {
                Card c = hand.get(i);
                Rectangle r = cardRects.get(i);
                font.draw(batch, trim(c.name, 14), r.x + 5f, r.y + r.height - 14f);
                fontSmall.setColor(0.85f, 0.9f, 1f, 1f);
                fontSmall.draw(batch, describeCard(c, game), r.x + 5f, r.y + r.height - 32f);
                fontSmall.setColor(Color.WHITE);
            }
            if (endTurnBtn != null) {
                font.draw(batch, "End Turn", endTurnBtn.x + 22f, endTurnBtn.y + 24f);
            }
        }
    }

    private static String trim(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }

    private static String describeCard(Card c, GameRunState g) {
        Combatant p = g.player;
        switch (c.cardType) {
            case ATTACK:
                return (c.damage + p.attackEnhancement) + "×" + c.hitCount + " · " + c.energyCost + "AP";
            case DEFENSE:
                return "+" + (c.shield + p.defenseEnhancement) + " shield · " + c.energyCost + "AP";
            case DRAW:
                return "Draw " + c.drawCards + " · 0AP";
            case BUFF_ATTACK_UP:
                return "ATK +" + c.buffAttack + " · " + c.energyCost + "AP";
            case BUFF_DEFENSE_UP:
                return "DEF +" + c.buffDefense + " · " + c.energyCost + "AP";
            case BUFF_HIT_UP:
                return "HIT +" + c.buffHit + " · " + c.energyCost + "AP";
            default:
                return "";
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
        fontSmall.dispose();
    }
}

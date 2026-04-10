package com.mydndgame.game;

import com.mydndgame.cards.Card;
import com.mydndgame.cards.Cards;
import com.mydndgame.combat.Combat;
import com.mydndgame.entities.Combatant;
import com.mydndgame.entities.Entities;
import com.mydndgame.shop.Shop;
import com.mydndgame.shop.ShopListing;
import com.mydndgame.worldmap.WorldMap;
import com.mydndgame.worldmap.WorldMapCellId;
import com.mydndgame.worldmap.WorldMapCellKind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** Map-based run: 3×3 grid + START; combat / shop / random events. */
public class GameRunState {

    public final Combatant player;
    public List<Card> deck;
    public int gold;
    public int attackPotions;
    public final WorldMap worldMap = new WorldMap();
    public Combat combat;
    public RunPhase phase = RunPhase.WORLD_MAP;
    public int lastLoot;
    public String stubNodeTitle = "";
    public String pendingBannerLine;
    public String pendingVictoryLine;
    public String pendingLootLine;
    /** Potion prompt before map combat (not shop). */
    public boolean mapAwaitingPotionForCombat;

    private static final List<Supplier<Combatant>> MAP_ENEMY_POOL = Arrays.asList(
        Entities::makeGoblin,
        Entities::makeBeast,
        Entities::makeHobgoblin
    );

    public GameRunState() {
        this.player = Entities.makePlayer();
        this.deck = Cards.starterDeck();
        this.gold = 20;
        this.attackPotions = 0;
    }

    public void startRun() {
        gold = 20;
        attackPotions = 0;
        deck = Cards.starterDeck();
        player.hp = player.maxHp;
        Entities.resetBetweenEncounters(player);
        combat = null;
        mapAwaitingPotionForCombat = false;
        pendingBannerLine = null;
        pendingVictoryLine = null;
        pendingLootLine = null;
        worldMap.resetExampleLayout();
        phase = RunPhase.WORLD_MAP;
    }

    public Supplier<Combatant> randomMapEnemy() {
        return MAP_ENEMY_POOL.get(ThreadLocalRandom.current().nextInt(MAP_ENEMY_POOL.size()));
    }

    private String battleBanner(WorldMapCellId cell) {
        return "======== Battle at " + cell.name() + " ========";
    }

    public void beginMapCombat(boolean drinkPotion, Supplier<Combatant> enemyFactory, String banner) {
        Combatant enemy = enemyFactory.get();
        Entities.resetBetweenEncounters(player);
        if (drinkPotion && attackPotions > 0) {
            player.attackEnhancement += 1;
            attackPotions--;
        }
        combat = new Combat(player, enemy, deck);
        combat.startPlayerTurn();
        phase = RunPhase.COMBAT;
        pendingBannerLine = banner;
    }

    /**
     * Move to {@code target} if adjacent; triggers cell content unless already cleared or START.
     * @return user-facing message (empty if OK / handled silently)
     */
    public String tryTravelTo(WorldMapCellId target) {
        if (phase != RunPhase.WORLD_MAP) {
            return "";
        }
        WorldMapCellId from = worldMap.getPosition();
        if (!worldMap.canTravelTo(from, target)) {
            return "You can't go there from here.";
        }
        worldMap.setPosition(target);
        if (target == WorldMapCellId.START) {
            return "You are at the starting crossroads.";
        }
        if (worldMap.isCleared(target)) {
            return "Already explored — nothing new here.";
        }
        WorldMapCellKind kind = worldMap.getKind(target);
        if (kind == null) {
            return "";
        }
        switch (kind) {
            case COMBAT:
                if (attackPotions > 0) {
                    mapAwaitingPotionForCombat = true;
                    phase = RunPhase.POTION_PROMPT;
                } else {
                    beginMapCombat(false, randomMapEnemy(), battleBanner(target));
                }
                break;
            case SHOP:
                phase = RunPhase.SHOP;
                pendingBannerLine = "======== Shop (" + target.name() + ") ========";
                combat = null;
                break;
            case EVENT:
                resolveMapEvent(target);
                break;
            default:
                break;
        }
        return "";
    }

    private void resolveMapEvent(WorldMapCellId cell) {
        double r = ThreadLocalRandom.current().nextDouble();
        if (r < 0.38) {
            if (attackPotions > 0) {
                mapAwaitingPotionForCombat = true;
                phase = RunPhase.POTION_PROMPT;
            } else {
                beginMapCombat(false, randomMapEnemy(), "======== Event: ambush at " + cell.name() + " ========");
            }
        } else if (r < 0.58) {
            phase = RunPhase.NODE_STUB;
            stubNodeTitle = "Event at " + cell.name();
            pendingBannerLine = "======== Strange encounter ========";
            combat = null;
        } else if (r < 0.78) {
            int g = 5 + ThreadLocalRandom.current().nextInt(8);
            gold += g;
            pendingLootLine = "You find " + g + " gold! (total " + gold + ").";
            worldMap.markCleared(cell);
            phase = RunPhase.WORLD_MAP;
            checkRunWonAfterClear();
        } else {
            phase = RunPhase.SHOP;
            pendingBannerLine = "======== Wandering merchant (" + cell.name() + ") ========";
            combat = null;
        }
    }

    /** After potion prompt for map combat. */
    public void applyMapPotionChoice(boolean drink) {
        if (!mapAwaitingPotionForCombat) {
            return;
        }
        mapAwaitingPotionForCombat = false;
        WorldMapCellId cell = worldMap.getPosition();
        beginMapCombat(drink, randomMapEnemy(), battleBanner(cell));
    }

    public List<String> playCard(int handIndex) {
        if (phase != RunPhase.COMBAT || combat == null) {
            return Collections.emptyList();
        }
        List<String> lines = combat.playCard(handIndex);
        if (combat != null && combat.isCombatOver()) {
            if (!player.isAlive()) {
                phase = RunPhase.GAME_OVER;
                combat = null;
            } else {
                String ename = combat.enemy.name;
                lastLoot = ThreadLocalRandom.current().nextInt(1, 4);
                gold += lastLoot;
                pendingLootLine = ename + " defeated! +" + lastLoot + " gold (total " + gold + ").";
                deck = combat.exportDeckForNextCombat();
                combat = null;
                worldMap.markCleared(worldMap.getPosition());
                checkRunWonAfterClear();
                if (phase != RunPhase.GAME_OVER && phase != RunPhase.RUN_VICTORY) {
                    phase = RunPhase.WORLD_MAP;
                }
            }
        }
        return lines;
    }

    public List<String> endPlayerTurn() {
        if (phase != RunPhase.COMBAT || combat == null) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>(combat.enemyTurn());
        if (combat != null && combat.isCombatOver()) {
            if (!player.isAlive()) {
                phase = RunPhase.GAME_OVER;
                combat = null;
            }
        } else if (combat != null) {
            combat.startPlayerTurn();
        }
        return lines;
    }

    public void leaveShop() {
        if (phase != RunPhase.SHOP) {
            return;
        }
        worldMap.markCleared(worldMap.getPosition());
        phase = RunPhase.WORLD_MAP;
        checkRunWonAfterClear();
    }

    public String tryBuy(ShopListing listing) {
        if (phase != RunPhase.SHOP) {
            return "";
        }
        if (gold < listing.price) {
            return "Not enough gold.";
        }
        gold -= listing.price;
        if ("card".equals(listing.kind)) {
            Card c = Shop.cardForListing(listing);
            if (c != null) {
                deck.add(c);
                return "Sold: " + c.name + " added to your deck.";
            }
        } else if ("potion".equals(listing.kind)) {
            attackPotions += 1;
            return "Sold: Attack Potion added to your bag.";
        }
        return "Unknown item.";
    }

    public void continueStubNode() {
        if (phase != RunPhase.NODE_STUB) {
            return;
        }
        worldMap.markCleared(worldMap.getPosition());
        phase = RunPhase.WORLD_MAP;
        checkRunWonAfterClear();
    }

    private void checkRunWonAfterClear() {
        if (worldMap.isRunWon()) {
            phase = RunPhase.RUN_VICTORY;
            pendingVictoryLine = "Victory! You cleared every location on the map.";
        }
    }

    public boolean isRunComplete() {
        return phase == RunPhase.RUN_VICTORY || phase == RunPhase.GAME_OVER;
    }
}

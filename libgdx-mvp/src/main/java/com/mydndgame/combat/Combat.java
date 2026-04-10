package com.mydndgame.combat;

import com.mydndgame.cards.Card;
import com.mydndgame.cards.CardType;
import com.mydndgame.cards.Cards;
import com.mydndgame.dice.AttackDice;
import com.mydndgame.entities.Combatant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Mirrors {@code src/combat.py} Combat. */
public class Combat {

    public final Combatant player;
    public final Combatant enemy;
    private final List<Card> masterDeck;
    private final List<Card> drawPile = new ArrayList<>();
    private final List<Card> hand = new ArrayList<>();
    private final List<Card> discardPile = new ArrayList<>();

    public int actionPoints;
    public final int maxActionPoints = 3;
    public final int cardsPerTurn = 5;
    public final int maxHandSize = 10;
    public MonsterIntent monsterIntent;
    private int enemyTurnCount;

    public Combat(Combatant player, Combatant enemy, List<Card> deck) {
        this.player = player;
        this.enemy = enemy;
        this.masterDeck = Cards.copyDeck(deck);
        this.enemyTurnCount = 0;
        shuffleDrawPile();
        this.monsterIntent = rollMonsterIntent();
    }

    private void shuffleDrawPile() {
        drawPile.clear();
        for (Card c : masterDeck) {
            drawPile.add(c.copy());
        }
        Collections.shuffle(drawPile, ThreadLocalRandom.current());
    }

    private boolean drawOne() {
        if (hand.size() >= maxHandSize) {
            return false;
        }
        if (drawPile.isEmpty()) {
            if (!discardPile.isEmpty()) {
                drawPile.addAll(discardPile);
                discardPile.clear();
                Collections.shuffle(drawPile, ThreadLocalRandom.current());
            } else {
                return false;
            }
        }
        if (drawPile.isEmpty()) {
            return false;
        }
        hand.add(drawPile.remove(drawPile.size() - 1));
        return true;
    }

    private int drawUpTo(int n) {
        int drawn = 0;
        for (int i = 0; i < n; i++) {
            if (!drawOne()) {
                break;
            }
            drawn++;
        }
        return drawn;
    }

    public void startPlayerTurn() {
        player.resetShield();
        actionPoints = maxActionPoints;
        discardHand();
        for (int i = 0; i < cardsPerTurn; i++) {
            if (!drawOne()) {
                break;
            }
        }
    }

    public void discardHand() {
        discardPile.addAll(hand);
        hand.clear();
    }

    public boolean canPlay(int handIndex) {
        if (handIndex < 0 || handIndex >= hand.size()) {
            return false;
        }
        return actionPoints >= hand.get(handIndex).energyCost;
    }

    public List<Card> getHand() {
        return hand;
    }

    public List<String> playCard(int handIndex) {
        List<String> lines = new ArrayList<>();
        if (!canPlay(handIndex)) {
            lines.add("Cannot play: not enough action points or invalid index.");
            return lines;
        }

        Card card = hand.remove(handIndex);
        actionPoints -= card.energyCost;
        discardPile.add(card);

        if (card.cardType == CardType.DRAW) {
            int want = card.drawCards;
            int got = drawUpTo(want);
            lines.add(
                "You play [" + card.name + "] (0 AP): drew " + got + " card(s) "
                    + "(requested " + want + ", hand " + hand.size() + "/" + maxHandSize + ")."
            );
            return lines;
        }

        if (card.cardType == CardType.DEFENSE) {
            int gained = card.shield + player.defenseEnhancement;
            player.addShield(gained);
            lines.add(
                "You play [" + card.name + "] and gain " + gained + " shield "
                    + "(base " + card.shield + " + defense enhancement " + player.defenseEnhancement + ")."
            );
            return lines;
        }

        if (card.cardType == CardType.ATTACK) {
            int perHit = card.damage + player.attackEnhancement;
            int bonus = player.hitEnhancement;
            int totalHits = Math.max(1, card.hitCount);
            lines.add(
                "You play [" + card.name + "] (" + totalHits + " hit(s), " + perHit + " damage per hit if not crit):"
            );
            for (int i = 0; i < totalHits; i++) {
                int hi = i + 1;
                int atkRoll = AttackDice.rollD20();
                AttackDice.Outcome o = AttackDice.resolve(atkRoll, bonus, enemy.ac);
                lines.add(
                    "  Hit " + hi + "/" + totalHits + ": d20 = " + atkRoll
                        + (bonus != 0 ? " + hit enhancement " + bonus + " = " + (atkRoll + bonus) : "")
                        + " vs " + enemy.name + " AC " + enemy.ac + "."
                );
                if (atkRoll == 1) {
                    lines.add("    Critical fail! Miss.");
                    continue;
                }
                if (atkRoll == 20) {
                    int dmg = perHit * 2;
                    enemy.takeDamage(dmg);
                    lines.add(
                        "    Critical hit! " + dmg + " damage (" + perHit + " × 2). " + enemy.name + " HP: "
                            + enemy.hp + "/" + enemy.maxHp + "."
                    );
                    continue;
                }
                if (o == AttackDice.Outcome.HIT) {
                    enemy.takeDamage(perHit);
                    lines.add(
                        "    Hit! " + perHit + " damage. " + enemy.name + " HP: "
                            + enemy.hp + "/" + enemy.maxHp + "."
                    );
                } else {
                    lines.add("    Miss.");
                }
                if (!enemy.isAlive()) {
                    break;
                }
            }
            return lines;
        }

        if (card.cardType == CardType.BUFF_ATTACK_UP) {
            player.attackEnhancement += card.buffAttack;
            lines.add(
                "You play [" + card.name + "]: attack enhancement +" + card.buffAttack
                    + " (total attack enhancement " + player.attackEnhancement + ")."
            );
            return lines;
        }

        if (card.cardType == CardType.BUFF_DEFENSE_UP) {
            player.defenseEnhancement += card.buffDefense;
            lines.add(
                "You play [" + card.name + "]: defense enhancement +" + card.buffDefense
                    + " (total defense enhancement " + player.defenseEnhancement + ")."
            );
            return lines;
        }

        if (card.cardType == CardType.BUFF_HIT_UP) {
            player.hitEnhancement += card.buffHit;
            lines.add(
                "You play [" + card.name + "]: hit enhancement +" + card.buffHit
                    + " (total hit enhancement " + player.hitEnhancement + ")."
            );
            return lines;
        }

        lines.add("Unknown card type.");
        return lines;
    }

    public List<Card> exportDeckForNextCombat() {
        List<Card> merged = new ArrayList<>();
        for (Card c : drawPile) {
            merged.add(c.copy());
        }
        for (Card c : hand) {
            merged.add(c.copy());
        }
        for (Card c : discardPile) {
            merged.add(c.copy());
        }
        return merged;
    }

    public List<String> enemyTurn() {
        List<String> lines = new ArrayList<>();
        if (!enemy.isAlive()) {
            return lines;
        }
        enemy.resetShield();

        enemyTurnCount++;
        Combatant e = enemy;
        Combatant p = player;

        if (e.buffAttackEveryNEnemyTurns != null) {
            int n = e.buffAttackEveryNEnemyTurns;
            if (enemyTurnCount % n == 0) {
                e.attackEnhancement += 1;
                lines.add(
                    e.name + " discipline: +1 attack enhancement (every " + n
                        + " enemy turns). Total attack enhancement " + e.attackEnhancement + "."
                );
            }
        }

        MonsterIntent intent = monsterIntent;

        if (intent.intentType == MonsterIntentType.DEFEND) {
            int gained = intent.value + e.defenseEnhancement;
            e.addShield(gained);
            lines.add(
                e.name + " defends and gains " + gained + " shield (base " + intent.value
                    + " + defense enhancement " + e.defenseEnhancement + ")."
            );
        } else if (intent.intentType == MonsterIntentType.BUFF_ATTACK_UP) {
            e.attackEnhancement += intent.value;
            lines.add(
                e.name + " powers up: attack enhancement +" + intent.value
                    + " (total " + e.attackEnhancement + ")."
            );
        } else if (intent.intentType == MonsterIntentType.BUFF_DEFENSE_UP) {
            e.defenseEnhancement += intent.value;
            lines.add(
                e.name + " powers up: defense enhancement +" + intent.value
                    + " (total " + e.defenseEnhancement + ")."
            );
        } else if (intent.intentType == MonsterIntentType.BUFF_HIT_UP) {
            e.hitEnhancement += intent.value;
            lines.add(
                e.name + " powers up: hit enhancement +" + intent.value
                    + " (total " + e.hitEnhancement + ")."
            );
        } else {
            int perHit = intent.value + e.attackEnhancement;
            int totalHits = Math.max(1, intent.attackHitCount);
            lines.add(e.name + " attacks (" + totalHits + " hit(s), " + perHit + " damage per hit if not crit):");
            for (int i = 0; i < totalHits; i++) {
                int hi = i + 1;
                int atkRoll = AttackDice.rollD20();
                AttackDice.Outcome o = AttackDice.resolve(atkRoll, e.hitEnhancement, p.ac);
                lines.add(
                    "  Hit " + hi + "/" + totalHits + ": d20 = " + atkRoll
                        + (e.hitEnhancement != 0
                            ? " + hit enhancement " + e.hitEnhancement + " = " + (atkRoll + e.hitEnhancement)
                            : "")
                        + " vs your AC " + p.ac + "."
                );
                if (atkRoll == 1) {
                    lines.add("    " + e.name + " critical fail! Miss.");
                    continue;
                }
                if (atkRoll == 20) {
                    int dmg = perHit * 2;
                    p.takeDamage(dmg);
                    lines.add(
                        "    Critical hit! You take " + dmg + " damage (" + perHit + " × 2). Your HP: "
                            + p.hp + "/" + p.maxHp + ", shield: " + p.shield + "."
                    );
                } else if (o == AttackDice.Outcome.HIT) {
                    p.takeDamage(perHit);
                    lines.add(
                        "    Hit! You take " + perHit + " damage. Your HP: "
                            + p.hp + "/" + p.maxHp + ", shield: " + p.shield + "."
                    );
                } else {
                    lines.add("    " + e.name + "'s strike misses.");
                }
                if (!p.isAlive()) {
                    break;
                }
            }
        }

        monsterIntent = rollMonsterIntent();
        return lines;
    }

    public boolean isCombatOver() {
        return !player.isAlive() || !enemy.isAlive();
    }

    private MonsterIntent rollMonsterIntent() {
        String tag = enemy.monsterTag;
        double roll = ThreadLocalRandom.current().nextDouble();

        if ("beast".equals(tag)) {
            if (roll < 0.28) {
                return new MonsterIntent(MonsterIntentType.ATTACK, 8, 1);
            }
            if (roll < 0.46) {
                return new MonsterIntent(MonsterIntentType.ATTACK, 4, 2);
            }
            if (roll < 0.58) {
                return new MonsterIntent(MonsterIntentType.DEFEND, 4);
            }
            if (roll < 0.70) {
                return new MonsterIntent(MonsterIntentType.BUFF_ATTACK_UP, 1);
            }
            if (roll < 0.82) {
                return new MonsterIntent(MonsterIntentType.BUFF_DEFENSE_UP, 1);
            }
            return new MonsterIntent(MonsterIntentType.BUFF_HIT_UP, 1);
        }

        if ("hobgoblin".equals(tag)) {
            if (roll < 0.12) {
                return new MonsterIntent(MonsterIntentType.ATTACK, 3, 1);
            }
            if (roll < 0.28) {
                return new MonsterIntent(MonsterIntentType.ATTACK, 2, 2);
            }
            if (roll < 0.46) {
                return new MonsterIntent(MonsterIntentType.DEFEND, 8);
            }
            if (roll < 0.61) {
                return new MonsterIntent(MonsterIntentType.BUFF_ATTACK_UP, 1);
            }
            if (roll < 0.76) {
                return new MonsterIntent(MonsterIntentType.BUFF_DEFENSE_UP, 1);
            }
            return new MonsterIntent(MonsterIntentType.BUFF_HIT_UP, 1);
        }

        // goblin
        if (roll < 0.18) {
            return new MonsterIntent(MonsterIntentType.ATTACK, 5, 1);
        }
        if (roll < 0.32) {
            return new MonsterIntent(MonsterIntentType.ATTACK, 3, 2);
        }
        if (roll < 0.50) {
            return new MonsterIntent(MonsterIntentType.DEFEND, 5);
        }
        if (roll < 0.68) {
            return new MonsterIntent(MonsterIntentType.BUFF_ATTACK_UP, 1);
        }
        if (roll < 0.84) {
            return new MonsterIntent(MonsterIntentType.BUFF_DEFENSE_UP, 1);
        }
        return new MonsterIntent(MonsterIntentType.BUFF_HIT_UP, 1);
    }
}

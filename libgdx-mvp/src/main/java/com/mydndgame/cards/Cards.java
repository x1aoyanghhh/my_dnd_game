package com.mydndgame.cards;

import java.util.ArrayList;
import java.util.List;

/** Mirrors {@code src/cards.py} factories and starter deck. */
public final class Cards {

    private Cards() {}

    public static Card attackCard() {
        return new Card("Attack", CardType.ATTACK, 1, 5, 1, 0, 0, 0, 0, 0);
    }

    public static Card twinSlashCard() {
        return new Card("Twin Slash", CardType.ATTACK, 1, 3, 2, 0, 0, 0, 0, 0);
    }

    public static Card readjustCard() {
        return new Card("Readjust", CardType.DRAW, 0, 0, 1, 0, 0, 0, 0, 2);
    }

    public static Card defenseCard() {
        return new Card("Defense", CardType.DEFENSE, 1, 0, 1, 5, 0, 0, 0, 0);
    }

    public static Card buffAttackUp(int amount) {
        return new Card("Atk Up", CardType.BUFF_ATTACK_UP, 1, 0, 1, 0, amount, 0, 0, 0);
    }

    public static Card buffDefenseUp(int amount) {
        return new Card("Def Up", CardType.BUFF_DEFENSE_UP, 1, 0, 1, 0, 0, amount, 0, 0);
    }

    public static Card buffHitUp(int amount) {
        return new Card("Hit Up", CardType.BUFF_HIT_UP, 1, 0, 1, 0, 0, 0, amount, 0);
    }

    public static List<Card> starterDeck() {
        List<Card> d = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            d.add(attackCard());
        }
        for (int i = 0; i < 4; i++) {
            d.add(defenseCard());
        }
        d.add(buffAttackUp(1));
        d.add(buffDefenseUp(1));
        d.add(buffHitUp(20));
        return d;
    }

    public static List<Card> copyDeck(List<Card> deck) {
        List<Card> out = new ArrayList<>(deck.size());
        for (Card c : deck) {
            out.add(c.copy());
        }
        return out;
    }
}

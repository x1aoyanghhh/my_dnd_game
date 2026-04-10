package com.mydndgame.cards;

/**
 * Immutable card; use {@link #copy()} when cloning into piles (matches Python deepcopy).
 */
public final class Card {

    public final String name;
    public final CardType cardType;
    public final int energyCost;
    public final int damage;
    public final int hitCount;
    public final int shield;
    public final int buffAttack;
    public final int buffDefense;
    public final int buffHit;
    public final int drawCards;

    public Card(
        String name,
        CardType cardType,
        int energyCost,
        int damage,
        int hitCount,
        int shield,
        int buffAttack,
        int buffDefense,
        int buffHit,
        int drawCards
    ) {
        this.name = name;
        this.cardType = cardType;
        this.energyCost = energyCost;
        this.damage = damage;
        this.hitCount = hitCount;
        this.shield = shield;
        this.buffAttack = buffAttack;
        this.buffDefense = buffDefense;
        this.buffHit = buffHit;
        this.drawCards = drawCards;
    }

    public Card copy() {
        return new Card(
            name, cardType, energyCost, damage, hitCount, shield,
            buffAttack, buffDefense, buffHit, drawCards
        );
    }
}

package com.mydndgame.shop;

import com.mydndgame.cards.Card;
import com.mydndgame.cards.Cards;

import java.util.Arrays;
import java.util.List;

/** Mirrors {@code src/shop.py}. */
public final class Shop {

    private Shop() {}

    public static List<ShopListing> shopListings() {
        return Arrays.asList(
            new ShopListing("1", "Skill book: Twin Slash", 10, "card"),
            new ShopListing("2", "Skill book: Readjust", 8, "card"),
            new ShopListing("3", "Attack Potion (+1 attack enhancement for one combat)", 5, "potion")
        );
    }

    public static Card cardForListing(ShopListing listing) {
        if (!"card".equals(listing.kind)) {
            return null;
        }
        if ("1".equals(listing.key)) {
            return Cards.twinSlashCard();
        }
        if ("2".equals(listing.key)) {
            return Cards.readjustCard();
        }
        return null;
    }
}

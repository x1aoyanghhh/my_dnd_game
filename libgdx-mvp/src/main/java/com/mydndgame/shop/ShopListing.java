package com.mydndgame.shop;

public final class ShopListing {

    public final String key;
    public final String label;
    public final int price;
    /** "card" or "potion" */
    public final String kind;

    public ShopListing(String key, String label, int price, String kind) {
        this.key = key;
        this.label = label;
        this.price = price;
        this.kind = kind;
    }
}

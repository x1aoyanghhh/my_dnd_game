"""In-run shop: spend gold on skill books (cards) and potions."""

from __future__ import annotations

from dataclasses import dataclass

from .cards import Card, readjust_card, twin_slash_card


@dataclass(frozen=True)
class ShopListing:
    key: str
    label: str
    price: int
    kind: str  # "card" | "potion"


def shop_listings() -> list[ShopListing]:
    return [
        ShopListing("1", "Skill book: Twin Slash", 10, "card"),
        ShopListing("2", "Skill book: Readjust", 8, "card"),
        ShopListing("3", "Attack Potion (+1 attack enhancement for one combat)", 5, "potion"),
    ]


def _card_for_listing(listing: ShopListing) -> Card | None:
    if listing.kind != "card":
        return None
    if listing.key == "1":
        return twin_slash_card()
    if listing.key == "2":
        return readjust_card()
    return None


def open_shop(gold: int, deck: list[Card], attack_potions: int) -> tuple[int, list[Card], int]:
    """
    Text shop loop. Mutates deck in place; returns (gold, deck, attack_potions).
    """
    listings = shop_listings()
    while True:
        print("\n======== SHOP ========")
        print(f"Gold: {gold}")
        print(f"Attack potions in bag: {attack_potions}")
        for s in listings:
            print(f"  [{s.key}] {s.label} — {s.price}g")
        print("  [0] Leave shop")
        try:
            raw = input("Buy (1–3) or leave (0): ").strip()
        except EOFError:
            print()
            return gold, deck, attack_potions
        if raw == "0":
            return gold, deck, attack_potions
        chosen = next((s for s in listings if s.key == raw), None)
        if not chosen:
            print("Invalid choice.")
            continue
        if gold < chosen.price:
            print("Not enough gold.")
            continue
        gold -= chosen.price
        if chosen.kind == "card":
            card = _card_for_listing(chosen)
            if card is not None:
                deck.append(card)
                print(f"Sold: {card.name} added to your deck.")
        else:
            attack_potions += 1
            print("Sold: Attack Potion added to your bag.")

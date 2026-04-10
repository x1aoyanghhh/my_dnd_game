from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class CardType(Enum):
    ATTACK = "attack"
    DEFENSE = "defense"
    DRAW = "draw"
    BUFF_ATTACK_UP = "buff_attack_up"
    BUFF_DEFENSE_UP = "buff_defense_up"
    BUFF_HIT_UP = "buff_hit_up"


@dataclass(frozen=True)
class Card:
    name: str
    card_type: CardType
    energy_cost: int = 1
    # Per-hit damage for ATTACK (hit_count rolls separately). Enhancement adds to each hit.
    damage: int = 0
    hit_count: int = 1
    shield: int = 0
    buff_attack: int = 0
    buff_defense: int = 0
    buff_hit: int = 0
    draw_cards: int = 0


def attack_card() -> Card:
    return Card(name="Attack", card_type=CardType.ATTACK, damage=5, hit_count=1)


def twin_slash_card() -> Card:
    """Two hits of 3 base damage each (not in starter deck)."""
    return Card(name="Twin Slash", card_type=CardType.ATTACK, damage=3, hit_count=2, energy_cost=1)


def readjust_card() -> Card:
    """Draw up to N cards; 0 AP (not in starter deck)."""
    return Card(name="Readjust", card_type=CardType.DRAW, energy_cost=0, draw_cards=2)


def defense_card() -> Card:
    return Card(name="Defense", card_type=CardType.DEFENSE, shield=5)


def buff_attack_up(amount: int = 1) -> Card:
    return Card(name="Atk Up", card_type=CardType.BUFF_ATTACK_UP, buff_attack=amount)


def buff_defense_up(amount: int = 1) -> Card:
    return Card(name="Def Up", card_type=CardType.BUFF_DEFENSE_UP, buff_defense=amount)


def buff_hit_up(amount: int = 20) -> Card:
    return Card(name="Hit Up", card_type=CardType.BUFF_HIT_UP, buff_hit=amount)


def starter_deck() -> list[Card]:
    """Starter deck: 4 Attack, 4 Defense, plus one of each buff card (amounts from factories)."""
    return (
        [attack_card() for _ in range(4)]
        + [defense_card() for _ in range(4)]
        + [buff_attack_up(1), buff_defense_up(1), buff_hit_up(20)]
    )

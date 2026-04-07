from __future__ import annotations

import copy
import random
from dataclasses import dataclass, field
from enum import Enum

from .cards import Card, CardType, starter_deck
from .dice import resolve_attack_roll, roll_d20
from .entities import Combatant


class MonsterIntentType(Enum):
    ATTACK = "attack"
    DEFEND = "defend"
    BUFF_ATTACK_UP = "buff_attack_up"
    BUFF_DEFENSE_UP = "buff_defense_up"
    BUFF_HIT_UP = "buff_hit_up"


@dataclass(frozen=True)
class MonsterIntent:
    intent_type: MonsterIntentType
    value: int

    def describe(self) -> str:
        t = self.intent_type
        if t == MonsterIntentType.ATTACK:
            return f"Attack (base {self.value}; on hit deals base + attack enhancement)"
        if t == MonsterIntentType.DEFEND:
            return f"Defend (gain {self.value} shield + defense enhancement)"
        if t == MonsterIntentType.BUFF_ATTACK_UP:
            return f"Buff: +{self.value} attack enhancement"
        if t == MonsterIntentType.BUFF_DEFENSE_UP:
            return f"Buff: +{self.value} defense enhancement"
        return f"Buff: +{self.value} hit enhancement"


@dataclass
class Combat:
    """Minimal combat loop: player plays cards, then the enemy acts."""

    player: Combatant
    enemy: Combatant
    deck: list[Card] = field(default_factory=list)
    draw_pile: list[Card] = field(default_factory=list)
    hand: list[Card] = field(default_factory=list)
    discard_pile: list[Card] = field(default_factory=list)
    action_points: int = 0
    max_action_points: int = 3
    cards_per_turn: int = 5
    monster_intent: MonsterIntent = field(
        default_factory=lambda: MonsterIntent(MonsterIntentType.ATTACK, 5)
    )

    def __post_init__(self) -> None:
        if not self.deck:
            self.deck = starter_deck()
        self._shuffle_draw_pile()
        self.monster_intent = self._roll_monster_intent()

    def _roll_monster_intent(self) -> MonsterIntent:
        # Weighted mix: direct actions + self-buffs.
        roll = random.random()
        if roll < 0.22:
            return MonsterIntent(MonsterIntentType.ATTACK, 5)
        if roll < 0.40:
            return MonsterIntent(MonsterIntentType.DEFEND, 5)
        if roll < 0.58:
            return MonsterIntent(MonsterIntentType.BUFF_ATTACK_UP, 1)
        if roll < 0.76:
            return MonsterIntent(MonsterIntentType.BUFF_DEFENSE_UP, 1)
        return MonsterIntent(MonsterIntentType.BUFF_HIT_UP, 1)

    def _shuffle_draw_pile(self) -> None:
        self.draw_pile = copy.deepcopy(self.deck)
        random.shuffle(self.draw_pile)

    def _draw_one(self) -> None:
        if not self.draw_pile:
            if self.discard_pile:
                self.draw_pile = self.discard_pile
                self.discard_pile = []
                random.shuffle(self.draw_pile)
            else:
                return
        if self.draw_pile:
            self.hand.append(self.draw_pile.pop())

    def start_player_turn(self) -> None:
        # Shields expire at the start of each owner's turn.
        self.player.reset_shield()
        self.action_points = self.max_action_points
        self.discard_hand()
        for _ in range(self.cards_per_turn):
            self._draw_one()

    def discard_hand(self) -> None:
        self.discard_pile.extend(self.hand)
        self.hand.clear()

    def can_play(self, hand_index: int) -> bool:
        if hand_index < 0 or hand_index >= len(self.hand):
            return False
        return self.action_points >= self.hand[hand_index].energy_cost

    def play_card(self, hand_index: int) -> list[str]:
        """Play a card; return log lines for the CLI."""
        lines: list[str] = []
        if not self.can_play(hand_index):
            lines.append("Cannot play: not enough action points or invalid index.")
            return lines

        card = self.hand.pop(hand_index)
        self.action_points -= card.energy_cost
        self.discard_pile.append(card)

        if card.card_type == CardType.DEFENSE:
            gained = card.shield + self.player.defense_enhancement
            self.player.add_shield(gained)
            lines.append(
                f"You play [{card.name}] and gain {gained} shield "
                f"(base {card.shield} + defense enhancement {self.player.defense_enhancement})."
            )
            return lines

        if card.card_type == CardType.ATTACK:
            base_damage = card.damage + self.player.attack_enhancement
            bonus = self.player.hit_enhancement
            atk_roll = roll_d20()
            hit, crit = resolve_attack_roll(atk_roll, bonus, self.enemy.ac)

            lines.append(
                f"You play [{card.name}], attack roll: d20 = {atk_roll}"
                + (f" + hit enhancement {bonus} = {atk_roll + bonus}" if bonus else "")
                + f" vs {self.enemy.name} AC {self.enemy.ac}."
            )
            if atk_roll == 1:
                lines.append("Critical fail! Miss.")
                return lines
            if atk_roll == 20:
                dmg = base_damage * 2
                self.enemy.take_damage(dmg)
                lines.append(
                    f"Critical hit! {dmg} damage ({base_damage} × 2). "
                    f"{self.enemy.name} HP: {self.enemy.hp}."
                )
                return lines
            if hit:
                self.enemy.take_damage(base_damage)
                lines.append(f"Hit! {base_damage} damage. {self.enemy.name} HP: {self.enemy.hp}.")
            else:
                lines.append("Miss.")
            return lines

        if card.card_type == CardType.BUFF_ATTACK_UP:
            self.player.attack_enhancement += card.buff_attack
            lines.append(
                f"You play [{card.name}]: attack enhancement +{card.buff_attack} "
                f"(total attack enhancement {self.player.attack_enhancement})."
            )
            return lines

        if card.card_type == CardType.BUFF_DEFENSE_UP:
            self.player.defense_enhancement += card.buff_defense
            lines.append(
                f"You play [{card.name}]: defense enhancement +{card.buff_defense} "
                f"(total defense enhancement {self.player.defense_enhancement})."
            )
            return lines

        if card.card_type == CardType.BUFF_HIT_UP:
            self.player.hit_enhancement += card.buff_hit
            lines.append(
                f"You play [{card.name}]: hit enhancement +{card.buff_hit} "
                f"(total hit enhancement {self.player.hit_enhancement})."
            )
            return lines

        lines.append("Unknown card type.")
        return lines

    def enemy_turn(self) -> list[str]:
        """Enemy acts according to intent, then rolls a new intent."""
        lines: list[str] = []
        if not self.enemy.is_alive():
            return lines
        self.enemy.reset_shield()

        intent = self.monster_intent
        e = self.enemy
        p = self.player

        if intent.intent_type == MonsterIntentType.DEFEND:
            gained = intent.value + e.defense_enhancement
            e.add_shield(gained)
            lines.append(
                f"{e.name} defends and gains {gained} shield "
                f"(base {intent.value} + defense enhancement {e.defense_enhancement})."
            )
        elif intent.intent_type in (
            MonsterIntentType.BUFF_ATTACK_UP,
            MonsterIntentType.BUFF_DEFENSE_UP,
            MonsterIntentType.BUFF_HIT_UP,
        ):
            if intent.intent_type == MonsterIntentType.BUFF_ATTACK_UP:
                e.attack_enhancement += intent.value
                lines.append(
                    f"{e.name} powers up: attack enhancement +{intent.value} "
                    f"(total {e.attack_enhancement})."
                )
            elif intent.intent_type == MonsterIntentType.BUFF_DEFENSE_UP:
                e.defense_enhancement += intent.value
                lines.append(
                    f"{e.name} powers up: defense enhancement +{intent.value} "
                    f"(total {e.defense_enhancement})."
                )
            else:
                e.hit_enhancement += intent.value
                lines.append(
                    f"{e.name} powers up: hit enhancement +{intent.value} "
                    f"(total {e.hit_enhancement})."
                )
        else:
            # Attack
            base_damage = intent.value + e.attack_enhancement
            atk_roll = roll_d20()
            hit, _ = resolve_attack_roll(atk_roll, e.hit_enhancement, p.ac)
            lines.append(
                f"{e.name} attacks: d20 = {atk_roll}"
                + (f" + hit enhancement {e.hit_enhancement} = {atk_roll + e.hit_enhancement}" if e.hit_enhancement else "")
                + f" vs your AC {p.ac}."
            )
            if atk_roll == 1:
                lines.append(f"{e.name} critical fail! Miss.")
            elif atk_roll == 20:
                dmg = base_damage * 2
                p.take_damage(dmg)
                lines.append(
                    f"Critical hit! You take {dmg} damage ({base_damage} × 2). "
                    f"Your HP: {p.hp}, shield: {p.shield}."
                )
            elif hit:
                p.take_damage(base_damage)
                lines.append(
                    f"Hit! You take {base_damage} damage. Your HP: {p.hp}, shield: {p.shield}."
                )
            else:
                lines.append(f"{e.name}'s attack misses.")

        self.monster_intent = self._roll_monster_intent()
        return lines

    def is_combat_over(self) -> bool:
        return not self.player.is_alive() or not self.enemy.is_alive()

from __future__ import annotations

import copy
import random
from dataclasses import dataclass, field
from enum import Enum

from .cards import Card, CardType, starter_deck
from .console import style_hit, style_hp_ratio, style_miss
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
    # For ATTACK only: number of separate d20 rolls (enhancement applies to each hit's damage).
    attack_hit_count: int = 1

    def describe(self) -> str:
        t = self.intent_type
        if t == MonsterIntentType.ATTACK:
            h = self.attack_hit_count
            if h <= 1:
                return (
                    f"Attack (base {self.value} per hit; on hit deals base + attack enhancement)"
                )
            return (
                f"Attack {self.value} × {h} hits (separate rolls; enhancement applies per hit)"
            )
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
    max_hand_size: int = 10
    monster_intent: MonsterIntent = field(
        default_factory=lambda: MonsterIntent(MonsterIntentType.ATTACK, 5)
    )
    _enemy_turn_count: int = field(default=0, repr=False)

    def __post_init__(self) -> None:
        if not self.deck:
            self.deck = starter_deck()
        self._enemy_turn_count = 0
        self._shuffle_draw_pile()
        self.monster_intent = self._roll_monster_intent()

    def _roll_monster_intent(self) -> MonsterIntent:
        """Intent pools depend on monster_tag (goblin / beast / hobgoblin)."""
        tag = self.enemy.monster_tag
        roll = random.random()

        if tag == "beast":
            if roll < 0.28:
                return MonsterIntent(MonsterIntentType.ATTACK, 8, 1)
            if roll < 0.46:
                return MonsterIntent(MonsterIntentType.ATTACK, 4, 2)
            if roll < 0.58:
                return MonsterIntent(MonsterIntentType.DEFEND, 4)
            if roll < 0.70:
                return MonsterIntent(MonsterIntentType.BUFF_ATTACK_UP, 1)
            if roll < 0.82:
                return MonsterIntent(MonsterIntentType.BUFF_DEFENSE_UP, 1)
            return MonsterIntent(MonsterIntentType.BUFF_HIT_UP, 1)

        if tag == "hobgoblin":
            if roll < 0.12:
                return MonsterIntent(MonsterIntentType.ATTACK, 3, 1)
            if roll < 0.28:
                return MonsterIntent(MonsterIntentType.ATTACK, 2, 2)
            if roll < 0.46:
                return MonsterIntent(MonsterIntentType.DEFEND, 8)
            if roll < 0.61:
                return MonsterIntent(MonsterIntentType.BUFF_ATTACK_UP, 1)
            if roll < 0.76:
                return MonsterIntent(MonsterIntentType.BUFF_DEFENSE_UP, 1)
            return MonsterIntent(MonsterIntentType.BUFF_HIT_UP, 1)

        # goblin
        if roll < 0.18:
            return MonsterIntent(MonsterIntentType.ATTACK, 5, 1)
        if roll < 0.32:
            return MonsterIntent(MonsterIntentType.ATTACK, 3, 2)
        if roll < 0.50:
            return MonsterIntent(MonsterIntentType.DEFEND, 5)
        if roll < 0.68:
            return MonsterIntent(MonsterIntentType.BUFF_ATTACK_UP, 1)
        if roll < 0.84:
            return MonsterIntent(MonsterIntentType.BUFF_DEFENSE_UP, 1)
        return MonsterIntent(MonsterIntentType.BUFF_HIT_UP, 1)

    def _shuffle_draw_pile(self) -> None:
        self.draw_pile = copy.deepcopy(self.deck)
        random.shuffle(self.draw_pile)

    def _draw_one(self) -> bool:
        """Draw one card if hand not full and a card is available. Returns whether a card was drawn."""
        if len(self.hand) >= self.max_hand_size:
            return False
        if not self.draw_pile:
            if self.discard_pile:
                self.draw_pile = self.discard_pile
                self.discard_pile = []
                random.shuffle(self.draw_pile)
            else:
                return False
        if self.draw_pile:
            self.hand.append(self.draw_pile.pop())
            return True
        return False

    def _draw_up_to(self, n: int) -> int:
        """Draw up to n cards, respecting hand limit and pile exhaustion."""
        drawn = 0
        for _ in range(n):
            if not self._draw_one():
                break
            drawn += 1
        return drawn

    def start_player_turn(self) -> None:
        self.player.reset_shield()
        self.action_points = self.max_action_points
        self.discard_hand()
        for _ in range(self.cards_per_turn):
            if not self._draw_one():
                break

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

        if card.card_type == CardType.DRAW:
            want = card.draw_cards
            got = self._draw_up_to(want)
            lines.append(
                f"You play [{card.name}] (0 AP): drew {got} card(s) "
                f"(requested {want}, hand {len(self.hand)}/{self.max_hand_size})."
            )
            return lines

        if card.card_type == CardType.DEFENSE:
            gained = card.shield + self.player.defense_enhancement
            self.player.add_shield(gained)
            lines.append(
                f"You play [{card.name}] and gain {gained} shield "
                f"(base {card.shield} + defense enhancement {self.player.defense_enhancement})."
            )
            return lines

        if card.card_type == CardType.ATTACK:
            per_hit = card.damage + self.player.attack_enhancement
            bonus = self.player.hit_enhancement
            total_hits = max(1, card.hit_count)
            prefix = f"You play [{card.name}] ({total_hits} hit(s), {per_hit} damage per hit if not crit):"
            lines.append(prefix)
            for i in range(total_hits):
                hi = i + 1
                atk_roll = roll_d20()
                hit, _ = resolve_attack_roll(atk_roll, bonus, self.enemy.ac)
                lines.append(
                    f"  Hit {hi}/{total_hits}: d20 = {atk_roll}"
                    + (f" + hit enhancement {bonus} = {atk_roll + bonus}" if bonus else "")
                    + f" vs {self.enemy.name} AC {self.enemy.ac}."
                )
                if atk_roll == 1:
                    lines.append(style_miss("    Critical fail! Miss."))
                    continue
                if atk_roll == 20:
                    dmg = per_hit * 2
                    self.enemy.take_damage(dmg)
                    lines.append(
                        style_hit("    Critical hit!")
                        + f" {dmg} damage ({per_hit} × 2). {self.enemy.name} HP: "
                        + style_hp_ratio(self.enemy.hp, self.enemy.max_hp)
                        + "."
                    )
                    continue
                if hit:
                    self.enemy.take_damage(per_hit)
                    lines.append(
                        style_hit("    Hit!")
                        + f" {per_hit} damage. {self.enemy.name} HP: "
                        + style_hp_ratio(self.enemy.hp, self.enemy.max_hp)
                        + "."
                    )
                else:
                    lines.append(style_miss("    Miss."))
                if not self.enemy.is_alive():
                    break
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

    def export_deck_for_next_combat(self) -> list[Card]:
        """Merge draw pile, hand, and discard into one list for the next encounter."""
        return copy.deepcopy(self.draw_pile + self.hand + self.discard_pile)

    def enemy_turn(self) -> list[str]:
        """Enemy acts according to intent, then rolls a new intent."""
        lines: list[str] = []
        if not self.enemy.is_alive():
            return lines
        self.enemy.reset_shield()

        self._enemy_turn_count += 1
        e = self.enemy
        p = self.player

        if e.buff_attack_every_n_enemy_turns:
            n = e.buff_attack_every_n_enemy_turns
            if self._enemy_turn_count % n == 0:
                e.attack_enhancement += 1
                lines.append(
                    f"{e.name} discipline: +1 attack enhancement "
                    f"(every {n} enemy turns). Total attack enhancement {e.attack_enhancement}."
                )

        intent = self.monster_intent

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
            per_hit = intent.value + e.attack_enhancement
            total_hits = max(1, intent.attack_hit_count)
            lines.append(
                f"{e.name} attacks ({total_hits} hit(s), {per_hit} damage per hit if not crit):"
            )
            for i in range(total_hits):
                hi = i + 1
                atk_roll = roll_d20()
                hit, _ = resolve_attack_roll(atk_roll, e.hit_enhancement, p.ac)
                lines.append(
                    f"  Hit {hi}/{total_hits}: d20 = {atk_roll}"
                    + (
                        f" + hit enhancement {e.hit_enhancement} = {atk_roll + e.hit_enhancement}"
                        if e.hit_enhancement
                        else ""
                    )
                    + f" vs your AC {p.ac}."
                )
                if atk_roll == 1:
                    lines.append(style_miss(f"    {e.name} critical fail! Miss."))
                    continue
                if atk_roll == 20:
                    dmg = per_hit * 2
                    p.take_damage(dmg)
                    lines.append(
                        style_hit("    Critical hit!")
                        + f" You take {dmg} damage ({per_hit} × 2). Your HP: "
                        + style_hp_ratio(p.hp, p.max_hp)
                        + f", shield: {p.shield}."
                    )
                elif hit:
                    p.take_damage(per_hit)
                    lines.append(
                        style_hit("    Hit!")
                        + f" You take {per_hit} damage. Your HP: "
                        + style_hp_ratio(p.hp, p.max_hp)
                        + f", shield: {p.shield}."
                    )
                else:
                    lines.append(style_miss(f"    {e.name}'s strike misses."))
                if not p.is_alive():
                    break

        self.monster_intent = self._roll_monster_intent()
        return lines

    def is_combat_over(self) -> bool:
        return not self.player.is_alive() or not self.enemy.is_alive()

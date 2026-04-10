"""
Minimal text-mode combat prototype — linear 3-encounter route.

Run: python main.py
"""

from __future__ import annotations

import copy
import random
import sys

from src.cards import CardType, starter_deck
from src.combat import Combat
from src.console import style_hp_ratio, try_enable_windows_ansi
from src.dice import hit_chance_with_hit_bonus
from src.entities import Combatant, make_player, reset_between_encounters
from src.route import NodeType, default_campaign_route
from src.shop import open_shop


def _attack_line(card, player, enemy) -> str:
    eff = card.damage + player.attack_enhancement
    chance = hit_chance_with_hit_bonus(enemy.ac, player.hit_enhancement)
    hits = max(1, card.hit_count)
    hit_part = f"per-hit hit {chance:.0%}"
    if hits > 1:
        core = f"{eff}×{hits} hits (each rolled separately) | {hit_part}"
    elif player.attack_enhancement:
        core = f"dmg {eff} (card {card.damage} + atk enh {player.attack_enhancement}) | hit {chance:.0%}"
    else:
        core = f"dmg {eff} | hit {chance:.0%}"
    return core


def _defense_line(card, player) -> str:
    eff = card.shield + player.defense_enhancement
    if player.defense_enhancement:
        return (
            f"shield +{eff} (card {card.shield} + def enh {player.defense_enhancement})"
        )
    return f"shield +{eff}"


def _buff_line(card) -> str:
    if card.card_type == CardType.BUFF_ATTACK_UP:
        return f"attack enhancement +{card.buff_attack}"
    if card.card_type == CardType.BUFF_DEFENSE_UP:
        return f"defense enhancement +{card.buff_defense}"
    return f"hit enhancement +{card.buff_hit}"


def _draw_line(card, battle: Combat) -> str:
    return (
        f"draw up to {card.draw_cards} (0 AP; hand cap {battle.max_hand_size}; "
        "uses draw pile → discard pile)"
    )


def _admin_kill_enemy(enemy: Combatant) -> None:
    """Debug: force current enemy to 0 HP (ignores shield and combat rules)."""
    enemy.shield = 0
    enemy.hp = 0


def _print_hand(battle: Combat, player, enemy) -> None:
    for i, c in enumerate(battle.hand, start=1):
        if c.card_type == CardType.ATTACK:
            extra = _attack_line(c, player, enemy)
        elif c.card_type == CardType.DEFENSE:
            extra = _defense_line(c, player)
        elif c.card_type == CardType.DRAW:
            extra = _draw_line(c, battle)
        else:
            extra = _buff_line(c)
        print(f"  [{i}] {c.name} (cost {c.energy_cost}) — {extra}")


def play_combat(battle: Combat, player, enemy) -> str:
    """
    Run one combat to completion.
    Returns 'win', 'lose', or 'quit'.
    """
    while not battle.is_combat_over():
        battle.start_player_turn()
        print(
            f"--- {player.name}'s turn (AP {battle.action_points}/{battle.max_action_points}) ---"
        )
        print(
            f"{enemy.name} intent: {battle.monster_intent.describe()} | "
            f"enemy buffs ATK+{enemy.attack_enhancement} DEF+{enemy.defense_enhancement} "
            f"HIT+{enemy.hit_enhancement}"
        )
        _print_hand(battle, player, enemy)

        # Allow 0-cost cards after AP is spent; empty hand can still skip or admin kill.
        while not battle.is_combat_over():
            try:
                if battle.hand:
                    raw = input(
                        "Play card (1–n), skip turn (skip/0/end), quit (q), admin kill (kill): "
                    ).strip().lower()
                else:
                    raw = input(
                        "No cards in hand — skip turn (skip/0), quit (q), admin kill (kill): "
                    ).strip().lower()
            except EOFError:
                print()
                return "quit"
            if raw == "q":
                print("Quit.")
                return "quit"
            if raw == "kill":
                _admin_kill_enemy(enemy)
                print("[admin] Enemy killed instantly.")
                break
            if raw in ("0", "skip", "end", "done"):
                break
            if not battle.hand:
                print("No cards to play. Use skip/0, kill (admin), or q.")
                continue
            try:
                idx = int(raw) - 1
            except ValueError:
                print(
                    "Unknown input. Use a card index, skip/0 to end turn, kill (admin), or q."
                )
                continue
            for line in battle.play_card(idx):
                print(line)
            if battle.is_combat_over():
                break
            print(
                f"AP: {battle.action_points} | {enemy.name} HP "
                f"{style_hp_ratio(enemy.hp, enemy.max_hp)} shield {enemy.shield} | "
                f"you HP {style_hp_ratio(player.hp, player.max_hp)} shield {player.shield}"
            )
            if battle.hand:
                for i, c in enumerate(battle.hand, start=1):
                    if c.card_type == CardType.ATTACK:
                        extra = _attack_line(c, player, enemy)
                    elif c.card_type == CardType.DEFENSE:
                        extra = _defense_line(c, player)
                    elif c.card_type == CardType.DRAW:
                        extra = _draw_line(c, battle)
                    else:
                        extra = _buff_line(c)
                    print(f"  [{i}] {c.name} — {extra}")

        if battle.is_combat_over():
            break

        print()
        for line in battle.enemy_turn():
            print(line)
        print()

    if not player.is_alive():
        return "lose"
    return "win"


def offer_attack_potion(player: Combatant, attack_potions: int) -> int:
    """
    After reset_between_encounters; drinking adds +1 attack enhancement for this combat only.
    Returns updated potion count.
    """
    if attack_potions <= 0:
        return attack_potions
    print(f"\nYou carry {attack_potions} Attack Potion(s).")
    try:
        raw = input("Drink one for +1 attack enhancement this combat? (y/n): ").strip().lower()
    except EOFError:
        print()
        return attack_potions
    if raw in ("y", "yes"):
        player.attack_enhancement += 1
        print("+1 attack enhancement for this fight.")
        return attack_potions - 1
    return attack_potions


def main() -> None:
    try_enable_windows_ansi()
    player = make_player(attack_enhancement=0, defense_enhancement=0, hit_enhancement=0)
    deck = copy.deepcopy(starter_deck())
    gold = 20
    attack_potions = 0

    route = default_campaign_route()

    print("=== Node-based route (HP & deck persist; enhancements reset each fight) ===")
    print(f"{player.name}: HP {style_hp_ratio(player.hp, player.max_hp)}, AC {player.ac}")
    print(
        "Enhancements — "
        f"ATK+{player.attack_enhancement} (damage), "
        f"DEF+{player.defense_enhancement} (shield from Defense), "
        f"HIT+{player.hit_enhancement} (after d20; nat 1 miss, nat 20 crit ×2)"
    )
    print(
        "Attack rules (you & enemy): nat 1 auto miss; nat 20 auto hit and crit (×2); "
        "otherwise d20 + hit enhancement vs target AC."
    )
    print("Hobgoblin: +1 attack enhancement every 3 enemy turns (before its intent).")
    print("Hand limit 10; drawing respects draw pile + discard pile recycle.")
    print("Route: data-driven nodes — combat → shop → combat → combat (see src/route.py).")
    print("Starting gold: 20. Each win drops 1–3 more.")
    print("Type kill during your turn (admin) to kill the enemy.\n")

    for node in route:
        if not player.is_alive():
            break

        if node.node_type == NodeType.COMBAT:
            if node.enemy_factory is None:
                print("Internal error: COMBAT node missing enemy_factory.")
                return
            enemy = node.enemy_factory()
            reset_between_encounters(player)
            attack_potions = offer_attack_potion(player, attack_potions)

            print(f"======== {node.label} ========")
            print(
                f"Gold: {gold} | {enemy.name}: HP {style_hp_ratio(enemy.hp, enemy.max_hp)}, "
                f"AC {enemy.ac}"
            )
            battle = Combat(player=player, enemy=enemy, deck=deck)
            result = play_combat(battle, player, enemy)
            if result == "quit":
                return
            if result == "lose":
                print(f"Defeat… {player.name} falls at {node.label}.")
                return
            print(f"{enemy.name} defeated!")
            loot = random.randint(1, 3)
            gold += loot
            print(f"Loot: +{loot} gold (total {gold}).")
            deck = battle.export_deck_for_next_combat()
            print(f"{player.name} HP: {style_hp_ratio(player.hp, player.max_hp)}\n")

        elif node.node_type == NodeType.SHOP:
            print(f"======== {node.label} ========")
            print(f"Gold: {gold}\n")
            gold, deck, attack_potions = open_shop(gold, deck, attack_potions)
            print()

        elif node.node_type == NodeType.EVENT:
            print(f"======== {node.label} ========")
            print("(Random events not implemented yet — skipping this node.)\n")

        elif node.node_type == NodeType.CHEST:
            print(f"======== {node.label} ========")
            print("(Chests not implemented yet — skipping this node.)\n")

    if player.is_alive():
        print("Victory! You cleared the route.")
    else:
        print(f"Defeat… {player.name} falls.")


if __name__ == "__main__":
    main()

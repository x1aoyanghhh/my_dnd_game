"""
Minimal text-mode combat prototype.

Run: python main.py
"""

from __future__ import annotations

import sys

from src.cards import CardType
from src.combat import Combat
from src.dice import hit_chance_with_hit_bonus
from src.entities import make_monster, make_player


def _attack_line(card, player, enemy) -> str:
    eff = card.damage + player.attack_enhancement
    chance = hit_chance_with_hit_bonus(enemy.ac, player.hit_enhancement)
    if player.attack_enhancement:
        return (
            f"dmg {eff} (card {card.damage} + atk enh {player.attack_enhancement}) | hit {chance:.0%}"
        )
    return f"dmg {eff} | hit {chance:.0%}"


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


def main() -> None:
    # Tune starting enhancements here (later: relics / meta upgrades).
    player = make_player(attack_enhancement=0, defense_enhancement=0, hit_enhancement=0)
    enemy = make_monster()
    battle = Combat(player=player, enemy=enemy)

    print("=== Minimal combat prototype ===")
    print(f"{player.name}: HP {player.hp}/{player.max_hp}, AC {player.ac}")
    print(f"{enemy.name}: HP {enemy.hp}/{enemy.max_hp}, AC {enemy.ac}")
    print(
        "Enhancements — "
        f"ATK+{player.attack_enhancement} (damage), "
        f"DEF+{player.defense_enhancement} (shield from Defense), "
        f"HIT+{player.hit_enhancement} (added after d20; nat 1 miss, nat 20 crit ×2)"
    )
    print(
        "Attack rules (you & enemy): nat 1 auto miss; nat 20 auto hit and crit (×2); "
        "otherwise d20 + hit enhancement vs target AC."
    )
    print("Input: card index 1..n, 0 to end turn, q to quit.\n")

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
        for i, c in enumerate(battle.hand, start=1):
            if c.card_type == CardType.ATTACK:
                extra = _attack_line(c, player, enemy)
            elif c.card_type == CardType.DEFENSE:
                extra = _defense_line(c, player)
            else:
                extra = _buff_line(c)
            print(f"  [{i}] {c.name} (cost {c.energy_cost}) — {extra}")

        while battle.action_points > 0 and battle.hand and not battle.is_combat_over():
            try:
                raw = input("Play card (index / 0 end / q): ").strip().lower()
            except EOFError:
                print()
                sys.exit(0)
            if raw == "q":
                print("Quit.")
                return
            if raw == "0":
                break
            try:
                idx = int(raw) - 1
            except ValueError:
                print("Enter a number.")
                continue
            for line in battle.play_card(idx):
                print(line)
            if battle.is_combat_over():
                break
            print(
                f"AP: {battle.action_points} | {enemy.name} HP {enemy.hp} shield {enemy.shield} | "
                f"you HP {player.hp} shield {player.shield}"
            )
            if battle.hand:
                for i, c in enumerate(battle.hand, start=1):
                    if c.card_type == CardType.ATTACK:
                        extra = _attack_line(c, player, enemy)
                    elif c.card_type == CardType.DEFENSE:
                        extra = _defense_line(c, player)
                    else:
                        extra = _buff_line(c)
                    print(f"  [{i}] {c.name} — {extra}")

        if battle.is_combat_over():
            break

        print()
        for line in battle.enemy_turn():
            print(line)
        print()

    if player.is_alive():
        print(f"Victory! {enemy.name} is defeated.")
    else:
        print(f"Defeat… {player.name} falls.")


if __name__ == "__main__":
    main()

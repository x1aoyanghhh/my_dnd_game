from __future__ import annotations

import random


def roll_d20() -> int:
    """Roll a single d20 (uniform 1–20)."""
    return random.randint(1, 20)


def attack_hits(roll: int, target_ac: int) -> bool:
    """Simple hit check: raw d20 roll >= target AC (no modifiers)."""
    return roll >= target_ac


def hit_chance(target_ac: int) -> float:
    """
    Hit probability without a hit bonus: P(d20 >= AC).

    Example: AC 10 -> rolls 10..20 succeed -> 11/20 = 0.55.
    """
    needed = target_ac
    if needed <= 1:
        return 1.0
    if needed > 20:
        return 0.0
    successful = 21 - needed
    return successful / 20.0


def hit_chance_with_hit_bonus(target_ac: int, hit_bonus: int) -> float:
    """
    Hit probability with a hit bonus stacked after the d20 (player/monster rules):
    - Natural 1: automatic miss (critical fail).
    - Natural 20: automatic hit (critical hit on attacks).
    - Otherwise: hit if (d20 + hit_bonus) >= AC.
    """
    # Natural 20 always hits.
    p = 1.0 / 20.0
    for r in range(2, 20):
        if r + hit_bonus >= target_ac:
            p += 1.0 / 20.0
    return p


def resolve_attack_roll(
    roll: int,
    hit_bonus: int,
    target_ac: int,
) -> tuple[bool, bool]:
    """
    Resolve an attack roll against AC.

    Returns (hit, crit). Crit is True only on a natural 20.
    """
    if roll == 1:
        return False, False
    if roll == 20:
        return True, True
    if roll + hit_bonus >= target_ac:
        return True, False
    return False, False


# Backwards-compatible alias (older name).
resolve_player_attack = resolve_attack_roll

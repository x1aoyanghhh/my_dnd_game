from __future__ import annotations

from dataclasses import dataclass


@dataclass
class Combatant:
    """A combat participant: HP, AC, temporary shield, and persistent enhancements."""

    name: str
    max_hp: int
    hp: int
    ac: int
    shield: int = 0
    # Persistent modifiers (relics / upgrades / buff cards). Monsters may use these too.
    attack_enhancement: int = 0  # Added to attack damage.
    defense_enhancement: int = 0  # Added to shield gained from defend effects.
    hit_enhancement: int = 0  # Added to d20 before comparing to AC (nat 1 / nat 20 override).

    def __post_init__(self) -> None:
        if self.hp > self.max_hp:
            self.hp = self.max_hp

    def is_alive(self) -> bool:
        return self.hp > 0

    def add_shield(self, amount: int) -> None:
        if amount > 0:
            self.shield += amount

    def reset_shield(self) -> None:
        self.shield = 0

    def take_damage(self, amount: int) -> int:
        """Apply damage after shield; returns HP actually lost."""
        if amount <= 0:
            return 0
        remaining = amount
        if self.shield > 0:
            absorbed = min(self.shield, remaining)
            self.shield -= absorbed
            remaining -= absorbed
        lost = 0
        if remaining > 0:
            lost = min(remaining, self.hp)
            self.hp -= lost
        return lost


def make_player(
    name: str = "Player",
    max_hp: int = 40,
    ac: int = 12,
    *,
    attack_enhancement: int = 0,
    defense_enhancement: int = 0,
    hit_enhancement: int = 0,
) -> Combatant:
    return Combatant(
        name=name,
        max_hp=max_hp,
        hp=max_hp,
        ac=ac,
        attack_enhancement=attack_enhancement,
        defense_enhancement=defense_enhancement,
        hit_enhancement=hit_enhancement,
    )


def make_monster(name: str = "Goblin", max_hp: int = 25, ac: int = 10) -> Combatant:
    return Combatant(name=name, max_hp=max_hp, hp=max_hp, ac=ac)

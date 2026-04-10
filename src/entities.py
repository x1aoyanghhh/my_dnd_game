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
    # Encounter AI / special rules (monsters only; ignored for player).
    monster_tag: str = "goblin"
    # If set, every N-th enemy turn (1-based count), gain +1 attack enhancement before acting.
    buff_attack_every_n_enemy_turns: int | None = None

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


def reset_between_encounters(combatant: Combatant) -> None:
    """
    Clear shield and all enhancement stacks when starting a new encounter.
    Used for the player between fights (each enemy is a fresh Combatant).
    """
    combatant.reset_shield()
    combatant.attack_enhancement = 0
    combatant.defense_enhancement = 0
    combatant.hit_enhancement = 0


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
        monster_tag="player",
    )


def make_goblin() -> Combatant:
    """Baseline early enemy."""
    return Combatant(
        name="Goblin",
        max_hp=25,
        hp=25,
        ac=10,
        monster_tag="goblin",
    )


def make_beast() -> Combatant:
    """High damage, low AC, low HP."""
    return Combatant(
        name="Beast",
        max_hp=16,
        hp=16,
        ac=7,
        monster_tag="beast",
    )


def make_hobgoblin() -> Combatant:
    """Tanky, low base hit damage; gains +1 attack enhancement every 3 enemy turns."""
    return Combatant(
        name="Hobgoblin",
        max_hp=48,
        hp=48,
        ac=16,
        monster_tag="hobgoblin",
        buff_attack_every_n_enemy_turns=3,
    )


def make_monster(name: str = "Goblin", max_hp: int = 25, ac: int = 10) -> Combatant:
    """Legacy helper; defaults to goblin-like stats."""
    return Combatant(name=name, max_hp=max_hp, hp=max_hp, ac=ac, monster_tag="goblin")

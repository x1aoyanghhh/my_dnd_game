"""
Data-driven run route: ordered nodes (combat, shop, future event/chest).

Phase A: linear campaign as a list of RouteNode; main loop dispatches by NodeType.
"""

from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass
from enum import Enum

from .entities import Combatant, make_beast, make_goblin, make_hobgoblin


class NodeType(Enum):
    COMBAT = "combat"
    SHOP = "shop"
    EVENT = "event"
    CHEST = "chest"


@dataclass(frozen=True)
class RouteNode:
    """One step on the run. COMBAT nodes must provide enemy_factory()."""

    node_type: NodeType
    label: str
    enemy_factory: Callable[[], Combatant] | None = None


def default_campaign_route() -> list[RouteNode]:
    """
    Current linear campaign: Goblin → Shop → Beast → Hobgoblin.

    EVENT and CHEST node types exist for future maps; omit them here until implemented.
    """
    return [
        RouteNode(NodeType.COMBAT, "1/3 — Goblin", make_goblin),
        RouteNode(NodeType.SHOP, "Shop"),
        RouteNode(NodeType.COMBAT, "2/3 — Beast", make_beast),
        RouteNode(NodeType.COMBAT, "3/3 — Hobgoblin", make_hobgoblin),
    ]

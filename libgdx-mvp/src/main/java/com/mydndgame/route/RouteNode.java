package com.mydndgame.route;

import com.mydndgame.entities.Combatant;

import java.util.function.Supplier;

public final class RouteNode {

    public final NodeType nodeType;
    public final String label;
    public final Supplier<Combatant> enemyFactory;

    public RouteNode(NodeType nodeType, String label, Supplier<Combatant> enemyFactory) {
        this.nodeType = nodeType;
        this.label = label;
        this.enemyFactory = enemyFactory;
    }
}

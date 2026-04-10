package com.mydndgame.route;

import com.mydndgame.entities.Entities;

import java.util.Arrays;
import java.util.List;

/** Mirrors {@code src/route.py} {@code default_campaign_route}. */
public final class RouteData {

    private RouteData() {}

    public static List<RouteNode> defaultCampaignRoute() {
        return Arrays.asList(
            new RouteNode(NodeType.COMBAT, "1/3 — Goblin", Entities.goblinFactory()),
            new RouteNode(NodeType.SHOP, "Shop", null),
            new RouteNode(NodeType.COMBAT, "2/3 — Beast", Entities.beastFactory()),
            new RouteNode(NodeType.COMBAT, "3/3 — Hobgoblin", Entities.hobgoblinFactory())
        );
    }
}

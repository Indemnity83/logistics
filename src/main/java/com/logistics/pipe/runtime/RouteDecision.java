package com.logistics.pipe.runtime;

import com.logistics.pipe.runtime.TravelingItem;
import net.minecraft.util.math.Direction;

import java.util.List;

public final class RouteDecision {
    public enum Type {
        PASS,
        DROP,
        DISCARD,
        REROUTE,
        SPLIT
    }

    private static final RouteDecision PASS = new RouteDecision(Type.PASS, null, List.of());
    private static final RouteDecision DROP = new RouteDecision(Type.DROP, null, List.of());
    private static final RouteDecision DISCARD = new RouteDecision(Type.DISCARD, null, List.of());

    private final Type type;
    private final Direction direction;
    private final List<TravelingItem> items;

    private RouteDecision(Type type, Direction direction, List<TravelingItem> items) {
        this.type = type;
        this.direction = direction;
        this.items = items;
    }

    public static RouteDecision pass() {
        return PASS;
    }

    public static RouteDecision drop() {
        return DROP;
    }

    public static RouteDecision discard() {
        return DISCARD;
    }

    public static RouteDecision reroute(Direction direction) {
        return new RouteDecision(Type.REROUTE, direction, List.of());
    }

    public static RouteDecision split(List<TravelingItem> items) {
        return new RouteDecision(Type.SPLIT, null, items);
    }

    public Type getType() {
        return type;
    }

    public Direction getDirection() {
        return direction;
    }

    public List<TravelingItem> getItems() {
        return items;
    }
}

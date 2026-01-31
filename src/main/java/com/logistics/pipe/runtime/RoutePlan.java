package com.logistics.pipe.runtime;

import java.util.List;
import net.minecraft.util.math.Direction;

public final class RoutePlan {
    public enum Type {
        PASS,
        DROP,
        DISCARD,
        REROUTE,
        SPLIT
    }

    private static final RoutePlan PASS = new RoutePlan(Type.PASS, List.of(), List.of());
    private static final RoutePlan DROP = new RoutePlan(Type.DROP, List.of(), List.of());
    private static final RoutePlan DISCARD = new RoutePlan(Type.DISCARD, List.of(), List.of());

    private final Type type;
    private final List<Direction> directions;
    private final List<TravelingItem> items;

    private RoutePlan(Type type, List<Direction> directions, List<TravelingItem> items) {
        this.type = type;
        this.directions = directions;
        this.items = items;
    }

    public static RoutePlan pass() {
        return PASS;
    }

    public static RoutePlan drop() {
        return DROP;
    }

    public static RoutePlan discard() {
        return DISCARD;
    }

    public static RoutePlan reroute(List<Direction> directions) {
        return new RoutePlan(Type.REROUTE, List.copyOf(directions), List.of());
    }

    public static RoutePlan reroute(Direction direction) {
        return new RoutePlan(Type.REROUTE, List.of(direction), List.of());
    }

    public static RoutePlan split(List<TravelingItem> items) {
        return new RoutePlan(Type.SPLIT, List.of(), items);
    }

    public Type getType() {
        return type;
    }

    public List<Direction> getDirections() {
        return directions;
    }

    public List<TravelingItem> getItems() {
        return items;
    }
}

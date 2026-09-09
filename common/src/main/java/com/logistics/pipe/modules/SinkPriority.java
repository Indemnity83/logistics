package com.logistics.pipe.modules;

/**
 * The sink priority ladder. Higher wins; ties are broken deterministically by
 * position (see {@code SinkResolver#findSink}).
 *
 * <p>Every sink registration in the mod draws its priority from here, so the ladder
 * can be read in one place rather than from literals scattered across registration
 * files.
 *
 * <p>Two entries deliberately share a rung today: {@link #ITEM_SINK} and
 * {@link #POLYMORPHIC_SINK} are both 7, so an Item Sink and a Polymorphic Sink that
 * both accept an item are separated only by the positional tiebreak. The same module
 * also sits on two rungs — {@link #ITEM_SINK} as a chassis module item, and
 * {@link #BASIC_LOGISTICS_PIPE_SINK} as the Basic Logistics Pipe block.
 */
public final class SinkPriority {

    /** Catch-all default route. Any filtered sink outbids it. */
    public static final int DEFAULT_ROUTE = 0;

    /** Enchantment Sink module — accepts enchanted items. */
    public static final int ENCHANTMENT_SINK = 3;

    /** Terminus module — filter-only sink with no default-route capability. */
    public static final int TERMINUS = 4;

    /** Mod-Based Item Sink module — accepts every item from one namespace. */
    public static final int MOD_SINK = 5;

    /** Basic Logistics Pipe block — a {@link SinkModule} carried by the pipe itself. */
    public static final int BASIC_LOGISTICS_PIPE_SINK = 5;

    /** Item Sink module — accepts the item types in its filter slots. */
    public static final int ITEM_SINK = 7;

    /** Polymorphic Sink module — accepts item types already present in the adjacent inventory. */
    public static final int POLYMORPHIC_SINK = 7;

    /** Passive Supplier module — tops up a stocked inventory from network traffic. */
    public static final int PASSIVE_SUPPLIER = 8;

    private SinkPriority() {}
}

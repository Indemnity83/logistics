package com.logistics.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.modules.*;

public final class PipeTypes {
    // -----------------
    // Tier 1 (Basic routing / passive movement)
    // -----------------

    // Early transport pipe - slow item movement.
    public static final Pipe STONE_TRANSPORT_PIPE =
            new Pipe(new TransportModule(LogisticsPipe.CONFIG.ITEM_MIN_SPEED, LogisticsPipe.CONFIG.DRAG_COEFFICIENT)) {};

    // Base transport pipe - simple item movement.
    // NOTE: No special connection restrictions; this is the default backbone pipe.
    public static final Pipe COPPER_TRANSPORT_PIPE = new Pipe(new WeatheringModule(), new PipeMarkingModule()) {};

    // Accelerator transport - accelerates items when powered by redstone.
    public static final Pipe GOLD_TRANSPORT = new Pipe(new BoostModule(LogisticsPipe.CONFIG.ACCELERATION_RATE)) {};

    // Item extractor - pulls items from an adjacent inventory (requires energy)
    public static final Pipe ITEM_EXTRACTOR =
            new Pipe(new ExtractionModule(), new BlockConnectionModule(() -> PipeTypes.ITEM_EXTRACTOR))
                    .withEnergy();

    // Item merger - combines multiple incoming streams into a single output.
    public static final Pipe ITEM_MERGER = new Pipe(new MergerModule()) {};

    // Item insertion pipe - decorative pipe that only connects to other pipes.
    public static final Pipe ITEM_PASSTHROUGH_PIPE = new Pipe(new PipeOnlyModule()) {};

    // -----------------
    // Tier 2 (Decision based routing / bulk movement)
    // -----------------

    // Item filter - routes items based on per-side filters.
    public static final Pipe ITEM_FILTER = new Pipe(new ItemFilterModule()) {};

    // Item void - deletes items at the center with particle effects.
    public static final Pipe ITEM_VOID = new Pipe(new VoidModule(), new PipeOnlyModule()) {};

    // -----------------
    // Tier 3 (Network Logistics)
    // -----------------

    // Basic Logistics Pipe - network sink with filtering and default route capability.
    public static final Pipe BASIC_LOGISTICS_PIPE = new Pipe(
            new NetworkRouterModule(),
            new SinkModule()) {};

    // Provider Logistics Pipe - scans adjacent inventories and fulfills network requests.
    public static final Pipe PROVIDER_LOGISTICS_PIPE = new Pipe(
            new NetworkRouterModule(),
            new ProviderModule())
            .withEnergy();

    // Requester Logistics Pipe - creates requests for items from the network.
    public static final Pipe REQUESTER_LOGISTICS_PIPE = new Pipe(
            new NetworkRouterModule(),
            new RequesterModule())
            .withEnergy();

    // Supplier Logistics Pipe - maintains inventory stock levels by requesting items from the network.
    public static final Pipe SUPPLIER_LOGISTICS_PIPE = new Pipe(
            new NetworkRouterModule(),
            new SupplierModule())
            .withEnergy();

    // -----------------
    // Special
    // -----------------

    // Item insertion - prefers inventories with space, otherwise routes to pipes.
    public static final Pipe ITEM_INSERTION = new Pipe(new InsertionModule()) {};

    private PipeTypes() {}
}

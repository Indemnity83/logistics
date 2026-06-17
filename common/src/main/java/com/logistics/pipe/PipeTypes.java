package com.logistics.pipe;

import com.logistics.LogisticsFluid;
import com.logistics.core.LogisticsConfig;
import com.logistics.pipe.fluid.FluidPipe;
import com.logistics.pipe.fluid.module.FluidExtractorModule;
import com.logistics.pipe.fluid.module.FluidMergerModule;
import com.logistics.pipe.fluid.module.FluidTransportModule;
import com.logistics.pipe.fluid.module.FluidVoidModule;
import com.logistics.pipe.modules.*;

public final class PipeTypes {
    // -----------------
    // Tier 1 (Basic routing / passive movement)
    // -----------------

    // Early transport pipe - slow item movement.
    public static final ItemPipe STONE_TRANSPORT_PIPE =
            new ItemPipe(new TransportModule(LogisticsConfig.get().pipe.minSpeed, LogisticsConfig.get().pipe.drag)) {};

    // Base transport pipe - simple item movement.
    // NOTE: No special connection restrictions; this is the default backbone pipe.
    public static final ItemPipe COPPER_TRANSPORT_PIPE = new ItemPipe(new WeatheringModule(), new PipeMarkingModule()) {};

    // Accelerator transport - accelerates items when powered by redstone.
    public static final ItemPipe GOLD_TRANSPORT = new ItemPipe(new BoostModule(LogisticsConfig.get().pipe.acceleration)) {};

    // Item extractor - pulls items from an adjacent inventory (requires energy)
    public static final ItemPipe ITEM_EXTRACTOR =
            new ItemPipe(new ExtractionModule(), new BlockConnectionModule(() -> PipeTypes.ITEM_EXTRACTOR))
                    .withEnergy();

    // Item merger - combines multiple incoming streams into a single output.
    public static final ItemPipe ITEM_MERGER = new ItemPipe(new MergerModule()) {};

    // Item insertion pipe - decorative pipe that only connects to other pipes.
    public static final ItemPipe ITEM_PASSTHROUGH_PIPE = new ItemPipe(new PipeOnlyModule()) {};

    // -----------------
    // Tier 2 (Decision based routing / bulk movement)
    // -----------------

    // Item filter - routes items based on per-side filters.
    public static final ItemPipe ITEM_FILTER = new ItemPipe(new ItemFilterModule()) {};

    // Item void - deletes items at the center with particle effects.
    public static final ItemPipe ITEM_VOID = new ItemPipe(new VoidModule(), new PipeOnlyModule()) {};

    // -----------------
    // Tier 3 (Network Logistics)
    // -----------------

    // Basic Logistics Pipe - network sink with filtering and default route capability.
    public static final ItemPipe BASIC_LOGISTICS_PIPE = new ItemPipe(
            new NetworkRouterModule(),
            new SinkModule(5)) {};

    // Provider Logistics Pipe - scans adjacent inventories and fulfills network requests.
    public static final ItemPipe PROVIDER_LOGISTICS_PIPE = new ItemPipe(
            new NetworkRouterModule(),
            new ProviderModule(8, 1))
            .withEnergy();

    // Requester Logistics Pipe - creates requests for items from the network.
    public static final ItemPipe REQUESTER_LOGISTICS_PIPE = new ItemPipe(
            new NetworkRouterModule(),
            new RequesterModule())
            .withEnergy();

    // Supplier Logistics Pipe - maintains inventory stock levels by requesting items from the network.
    public static final ItemPipe SUPPLIER_LOGISTICS_PIPE = new ItemPipe(
            new NetworkRouterModule(),
            new SupplierModule())
            .withEnergy();

    // Crafting Logistics Pipe - on-demand crafting via adjacent Autocrafter.
    public static final ItemPipe CRAFTING_LOGISTICS_PIPE = new ItemPipe(
            new CraftingModule(1, 1),
            new NetworkRouterModule())
            .withEnergy();

    // Process Logistics Pipe - machine-style transformation (furnace smelting, alloy machines, etc.)
    public static final ItemPipe PROCESS_LOGISTICS_PIPE = new ItemPipe(
            new ProcessModule(),
            new NetworkRouterModule())
            .withEnergy();

    // Satellite Logistics Pipe - named insertion endpoint for routing inputs to machine faces.
    public static final ItemPipe SATELLITE_LOGISTICS_PIPE = new ItemPipe(
            new SatelliteModule(),
            new NetworkRouterModule())
            .withEnergy();

    // -----------------
    // Tier 3 (Chassis)
    // -----------------

    // Chassis Logistics Pipe MkI - 1 module slot.
    public static final ChassisPipe CHASSIS_LOGISTICS_PIPE_MK1 =
            new ChassisPipe(1, new NetworkRouterModule());

    // Chassis Logistics Pipe MkII - 2 module slots.
    public static final ChassisPipe CHASSIS_LOGISTICS_PIPE_MK2 =
            new ChassisPipe(2, new NetworkRouterModule());

    // Chassis Logistics Pipe MkIII - 3 module slots.
    public static final ChassisPipe CHASSIS_LOGISTICS_PIPE_MK3 =
            new ChassisPipe(3, new NetworkRouterModule());

    // Chassis Logistics Pipe MkIV - 4 module slots.
    public static final ChassisPipe CHASSIS_LOGISTICS_PIPE_MK4 =
            new ChassisPipe(4, new NetworkRouterModule());

    // Chassis Logistics Pipe MkV - 8 module slots.
    public static final ChassisPipe CHASSIS_LOGISTICS_PIPE_MK5 =
            new ChassisPipe(8, new NetworkRouterModule());

    // -----------------
    // Special
    // -----------------

    // Item insertion - prefers inventories with space, otherwise routes to pipes.
    public static final ItemPipe ITEM_INSERTION = new ItemPipe(new InsertionModule()) {};

    // -----------------
    // Fluid pipes (cellular fluid transport; defined the same compositional way as item pipes)
    // -----------------

    // Copper fluid pipe - 2x rate; weathers like vanilla copper and accepts colored markings.
    public static final FluidPipe COPPER_FLUID_PIPE = new FluidPipe(
            new FluidTransportModule(2, true, false),
            new WeatheringModule(LogisticsFluid::model, "copper_fluid_pipe", false),
            new PipeMarkingModule(LogisticsFluid::model, "pipe_markings"));

    // Stone fluid pipe - base (1x) rate.
    public static final FluidPipe STONE_FLUID_PIPE = new FluidPipe(new FluidTransportModule(1, true, false));

    // Gold fluid pipe - 4x rate.
    public static final FluidPipe GOLD_FLUID_PIPE = new FluidPipe(new FluidTransportModule(4, true, false));

    // Insertion fluid pipe - fills adjacent tanks before spilling to pipes.
    public static final FluidPipe INSERTION_FLUID_PIPE = new FluidPipe(new FluidTransportModule(2, true, true));

    // Bypass fluid pipe - connects to pipes only, never to handlers.
    public static final FluidPipe BYPASS_FLUID_PIPE = new FluidPipe(new FluidTransportModule(2, false, false));

    // Merger fluid pipe - directional check valve with a wrench-set output face.
    public static final FluidPipe MERGER_FLUID_PIPE = new FluidPipe(
            new FluidTransportModule(3, true, false), new FluidMergerModule());

    // Fluid extractor - pulls fluid from an adjacent handler when powered.
    public static final FluidPipe FLUID_EXTRACTOR_PIPE = new FluidPipe(
            new FluidTransportModule(1, true, false), new FluidExtractorModule()).withEnergy();

    // Void fluid pipe - destroys fluid; connects to pipes only.
    public static final FluidPipe VOID_FLUID_PIPE = new FluidPipe(
            new FluidTransportModule(1, false, false), new FluidVoidModule());

    private PipeTypes() {}
}

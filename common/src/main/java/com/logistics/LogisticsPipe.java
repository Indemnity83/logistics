package com.logistics;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigEntries;
import com.indemnity83.configory.ConfigKey;
import com.logistics.api.LogisticsApi;
import com.logistics.core.LogisticsConfigMigrator;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.LogisticsCreativeTab;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.network.chat.Component;
import com.logistics.pipe.modules.*;
import com.logistics.pipe.PipeApi;
import com.logistics.pipe.PipeTypes;
import com.logistics.pipe.block.FluidPipeBlock;
import com.logistics.pipe.block.GlassTankBlock;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.PowerJunctionBlock;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.block.entity.PowerJunctionBlockEntity;
import com.logistics.pipe.item.FluidPipeBlockItem;
import com.logistics.pipe.item.GlassTankBlockItem;
import com.logistics.pipe.data.PipeDataComponents.WeatheringState;
import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import com.logistics.pipe.item.FluidPacketItem;
import com.logistics.pipe.item.MarkingFluidItem;
import com.logistics.pipe.item.ModularPipeBlockItem;
import com.logistics.pipe.item.ModuleItem;
import com.logistics.pipe.ui.ItemFilterScreenHandler;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;

import java.util.EnumMap;
import java.util.Map;

public final class LogisticsPipe extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsPipe INSTANCE = new LogisticsPipe();

    @Override
    protected String domain() {
        return "pipe";
    }

    public static ResourceId resource(String name) {
        return INSTANCE.domainResource(name);
    }

    public static ResourceId model(String name) {
        return INSTANCE.domainModelResource(name);
    }

    @Override
    public void registerConfig() {
        CONFIG.register();
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        BLOCK.register();
        ITEM.register();
        ENTITY.register();
        DATA.register();
        SCREEN.register();
        CREATIVE.register();
        ALIAS.register();

        LogisticsApi.Registry.transport(new PipeApi());
    }

    /**
     * Pipe tuning. Fluids are part of the pipe domain, so both files live here — item transport in
     * {@code config/logistics/pipes.json}, fluid pipe/pump in {@code config/logistics/fluids.json}, and
     * fluid logistics in {@code config/logistics/fluid_logistics.json}.
     */
    public static final class CONFIG extends ConfigEntries {
        private static final Config pipes = configFor(LogisticsConfigHost.MOD_ID, "pipes");
        private static final Config fluids = configFor(LogisticsConfigHost.MOD_ID, "fluids");
        private static final Config fluidLogistics = configFor(LogisticsConfigHost.MOD_ID, "fluid_logistics");

        private CONFIG() {}

        // ---- fluid logistics (fluid_logistics.json) ----

        public static final ConfigKey<Long> FLUID_PACKET_MAX_MB =
                fluidLogistics.defineLong("fluid_packet_max_mb", 5000L)
                        .min(1L)
                        // Bounds FluidProviderModule's MAX_PACKETS_PER_DISPATCH (64) * maxMb so that
                        // dispatch-cap multiplication can never overflow — an overflow there previously
                        // threw out of onFluidDispatch, which tickFluidDispatch treats as a failed
                        // dispatch and de-registers the provider entirely.
                        .max(Long.MAX_VALUE / 64)
                        .describe("Max mB carried by a single fluid packet (no minimum — a packet may be smaller)")
                        .register();

        public static final ConfigKey<Long> FLUID_ENDPOINT_RF_PER_PACKET =
                fluidLogistics.defineLong("fluid_endpoint_rf_per_packet", 10L)
                        .min(0L)
                        .describe("RF charged per packet at each endpoint")
                        .register();

        // ---- item transport (pipes.json) ----

        public static final ConfigKey<Float> PIPE_MAX_SPEED = pipes.defineFloat("max_speed", 0.16f)
                .finite()
                .greaterThan(0.0f)
                .minValueOf(() -> CONFIG.PIPE_MIN_SPEED)
                .describe("Item speed ceiling (blocks/tick)")
                .register();

        public static final ConfigKey<Float> PIPE_MIN_SPEED = pipes.defineFloat("min_speed", 0.02f)
                .finite()
                .greaterThan(0.0f)
                .maxValueOf(() -> CONFIG.PIPE_MAX_SPEED)
                .describe("Item speed floor (blocks/tick)")
                .register();

        public static final ConfigKey<Float> PIPE_ACCELERATION = pipes.defineFloat("acceleration", 1.0f / 200.0f)
                .finite()
                .min(0.0f)
                .describe("Speed gain per tick when accelerating")
                .register();

        public static final ConfigKey<Float> PIPE_DRAG = pipes.defineFloat("drag", 0.005f)
                .finite()
                .range(0.0f, 1.0f)
                .describe("Speed decay fraction per tick")
                .register();

        public static final ConfigKey<Float> PIPE_INJECT_SPEED = pipes.defineFloat("inject_speed", 0.2f)
                .finite()
                .greaterThan(0.0f)
                .describe("Item speed when injected by network routing (blocks/tick)")
                .register();

        // ---- fluid pipe + pump (fluids.json) ----

        public static final ConfigKey<Integer> FLUID_PIPE_BASE_TRANSFER_RATE =
                fluids.defineInt("pipe_base_transfer_rate", 10)
                        .min(1)
                        .describe("Base Fluid Pipe transfer rate (mB/tick), scaled per tier")
                        .register();

        public static final ConfigKey<Integer> FLUID_PIPE_BASE_CAPACITY = fluids.defineInt("pipe_base_capacity", 250)
                .min(1)
                .describe("Fluid Pipe buffer capacity (mB)")
                .register();

        public static final ConfigKey<Integer> FLUID_PIPE_EXTRACTOR_CAPACITY = fluids.defineInt(
                        "pipe_extractor_capacity", 1000)
                .min(1)
                .describe("Buffer capacity (mB) of the Fluid Extractor Pipe. Must hold one bucket to drain a "
                        + "cauldron, which only gives up whole levels.")
                .register();

        public static final ConfigKey<Boolean> FLUID_PIPE_WOODEN_REQUIRES_ENGINE =
                fluids.defineBoolean("pipe_wooden_requires_engine", true)
                        .describe("Fluid Extractor Pipe requires engine power")
                        .register();

        public static final ConfigKey<Boolean> FLUID_PIPE_ACTIVE_EXTRACTION =
                fluids.defineBoolean("pipe_active_extraction", true)
                        .describe("Debug: extractor pulling enabled")
                        .register();

        public static final ConfigKey<Integer> FLUID_PUMP_TANK_CAPACITY_MB =
                fluids.defineInt("pump_tank_capacity_mb", 16_000)
                        .min(1)
                        .describe("Fluid Pump tank capacity (mB)")
                        .register();

        public static final ConfigKey<Long> FLUID_PUMP_ENERGY_CAPACITY = fluids.defineLong("pump_energy_capacity", 1_000L)
                .min(1L)
                .describe("Fluid Pump energy buffer capacity (RF)")
                .register();

        public static final ConfigKey<Long> FLUID_PUMP_MAX_ENERGY_INPUT = fluids.defineLong("pump_max_energy_input", 150L)
                .min(1L)
                .describe("Fluid Pump max energy input (RF/t)")
                .register();

        public static final ConfigKey<Long> FLUID_PUMP_ENERGY_PER_SOURCE = fluids.defineLong("pump_energy_per_source", 100L)
                .min(0L)
                .describe("RF consumed per source block pumped")
                .register();

        public static final ConfigKey<Integer> FLUID_PUMP_PUSH_RATE_MB = fluids.defineInt("pump_push_rate_mb", 400)
                .min(1)
                .describe("Fluid Pump output rate (mB/tick)")
                .register();

        public static final ConfigKey<Integer> FLUID_PUMP_INTERVAL_TICKS = fluids.defineInt("pump_interval_ticks", 16)
                .min(1)
                .describe("Fluid Pump source pickup interval (ticks)")
                .register();

        // Capped at 46340 so the radius can be squared as an int downstream without overflowing.
        public static final ConfigKey<Integer> FLUID_PUMP_SEARCH_RADIUS = fluids.defineInt("pump_search_radius", 64)
                .range(1, 46_340)
                .describe("Fluid Pump connected source search radius")
                .register();

        public static final ConfigKey<Float> FLUID_PUMP_ARM_SPEED = fluids.defineFloat("pump_arm_speed", 0.01f)
                .finite()
                .greaterThan(0.0f)
                .describe("Fluid Pump arm movement speed (blocks/tick)")
                .register();

        public static final ConfigKey<Integer> FLUID_PUMP_INFINITE_SOURCE_THRESHOLD =
                fluids.defineInt("pump_infinite_source_threshold", 9)
                        .min(0)
                        .describe("Connected water sources at or above which the pump treats the body as infinite and"
                                + " pumps without consuming blocks (0 = always consume)")
                        .register();

        static void register() {
            pipes.registerSanitizeHook(() -> pipes.repairMinMax(PIPE_MIN_SPEED, PIPE_MAX_SPEED));

            LogisticsConfigMigrator.mapLegacy("pipe", "acceleration", PIPE_ACCELERATION);
            LogisticsConfigMigrator.mapLegacy("pipe", "drag", PIPE_DRAG);
            LogisticsConfigMigrator.mapLegacy("pipe", "injectSpeed", PIPE_INJECT_SPEED);
            LogisticsConfigMigrator.mapLegacyPair("pipe", "minSpeed", "maxSpeed", PIPE_MIN_SPEED, PIPE_MAX_SPEED);

            LogisticsConfigMigrator.mapLegacy("fluidPipe", "baseTransferRate", FLUID_PIPE_BASE_TRANSFER_RATE);
            LogisticsConfigMigrator.mapLegacy("fluidPipe", "baseCapacity", FLUID_PIPE_BASE_CAPACITY);
            LogisticsConfigMigrator.mapLegacy("fluidPipe", "woodenRequiresEngine", FLUID_PIPE_WOODEN_REQUIRES_ENGINE);
            LogisticsConfigMigrator.mapLegacy("fluidPipe", "activeExtraction", FLUID_PIPE_ACTIVE_EXTRACTION);
            LogisticsConfigMigrator.mapLegacy("fluidPump", "tankCapacityMb", FLUID_PUMP_TANK_CAPACITY_MB);
            LogisticsConfigMigrator.mapLegacy("fluidPump", "energyCapacity", FLUID_PUMP_ENERGY_CAPACITY);
            LogisticsConfigMigrator.mapLegacy("fluidPump", "maxEnergyInput", FLUID_PUMP_MAX_ENERGY_INPUT);
            LogisticsConfigMigrator.mapLegacy("fluidPump", "energyPerSource", FLUID_PUMP_ENERGY_PER_SOURCE);
            LogisticsConfigMigrator.mapLegacy("fluidPump", "pushRateMb", FLUID_PUMP_PUSH_RATE_MB);
            LogisticsConfigMigrator.mapLegacy("fluidPump", "pumpIntervalTicks", FLUID_PUMP_INTERVAL_TICKS);
            LogisticsConfigMigrator.mapLegacy("fluidPump", "searchRadius", FLUID_PUMP_SEARCH_RADIUS);
            LogisticsConfigMigrator.mapLegacy("fluidPump", "armSpeed", FLUID_PUMP_ARM_SPEED);
            LogisticsConfigMigrator.mapLegacy("fluidPump", "infiniteSourceThreshold", FLUID_PUMP_INFINITE_SOURCE_THRESHOLD);
        }
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static Block STONE_TRANSPORT_PIPE;
        public static Block ITEM_PASSTHROUGH_PIPE;
        public static Block COPPER_TRANSPORT_PIPE;
        public static Block ITEM_EXTRACTOR_PIPE;
        public static Block ITEM_MERGER_PIPE;
        public static Block GOLD_TRANSPORT_PIPE;
        public static Block ITEM_FILTER_PIPE;
        public static Block ITEM_INSERTION_PIPE;
        public static Block ITEM_VOID_PIPE;
        public static Block BASIC_LOGISTICS_PIPE;
        public static Block PROVIDER_LOGISTICS_PIPE;
        public static Block FLUID_PROVIDER_LOGISTICS_PIPE;
        public static Block REQUESTER_LOGISTICS_PIPE;
        public static Block SUPPLIER_LOGISTICS_PIPE;
        public static Block FLUID_SUPPLIER_LOGISTICS_PIPE;
        public static Block CRAFTING_LOGISTICS_PIPE;
        public static Block PROCESS_LOGISTICS_PIPE;
        public static Block SATELLITE_LOGISTICS_PIPE;
        public static Block CHASSIS_LOGISTICS_PIPE_MK1;
        public static Block CHASSIS_LOGISTICS_PIPE_MK2;
        public static Block CHASSIS_LOGISTICS_PIPE_MK3;
        public static Block CHASSIS_LOGISTICS_PIPE_MK4;
        public static Block CHASSIS_LOGISTICS_PIPE_MK5;
        public static Block POWER_JUNCTION;

        // Fluid transport
        public static Block COPPER_FLUID_PIPE;
        public static Block STONE_FLUID_PIPE;
        public static Block GOLD_FLUID_PIPE;
        public static Block INSERTION_FLUID_PIPE;
        public static Block MERGER_FLUID_PIPE;
        public static Block FLUID_EXTRACTOR_PIPE;
        public static Block VOID_FLUID_PIPE;
        public static Block BYPASS_FLUID_PIPE;
        public static Block GLASS_TANK;

        private static Block.Properties pipeProps(Block.Properties props) {
            return props.mapColor(MapColor.NONE)
                    .strength(0.3f)
                    .sound(SoundType.METAL)
                    .noOcclusion();
        }

        private static Block.Properties tankProps(Block.Properties props) {
            return props.mapColor(MapColor.NONE)
                    .strength(0.5f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isValidSpawn((s, w, p, t) -> false)
                    .isRedstoneConductor((s, w, p) -> false)
                    .isSuffocating((s, w, p) -> false)
                    .isViewBlocking((s, w, p, box) -> false);
        }

        static void register() {
            STONE_TRANSPORT_PIPE = INSTANCE.registerBlockWithItem("stone_transport_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.STONE_TRANSPORT_PIPE));
            ITEM_PASSTHROUGH_PIPE = INSTANCE.registerBlockWithItem("item_passthrough_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.ITEM_PASSTHROUGH_PIPE));
            COPPER_TRANSPORT_PIPE = INSTANCE.registerBlockWithItem("copper_transport_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.COPPER_TRANSPORT_PIPE),
                ModularPipeBlockItem::new);
            ITEM_EXTRACTOR_PIPE = INSTANCE.registerBlockWithItem("item_extractor_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.ITEM_EXTRACTOR));
            ITEM_MERGER_PIPE = INSTANCE.registerBlockWithItem("item_merger_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.ITEM_MERGER));
            GOLD_TRANSPORT_PIPE = INSTANCE.registerBlockWithItem("gold_transport_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.GOLD_TRANSPORT));
            ITEM_FILTER_PIPE = INSTANCE.registerBlockWithItem("item_filter_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.ITEM_FILTER));
            ITEM_INSERTION_PIPE = INSTANCE.registerBlockWithItem("item_insertion_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.ITEM_INSERTION));
            ITEM_VOID_PIPE = INSTANCE.registerBlockWithItem("item_void_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.ITEM_VOID));
            BASIC_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("basic_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.BASIC_LOGISTICS_PIPE));
            PROVIDER_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("provider_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.PROVIDER_LOGISTICS_PIPE));
            FLUID_PROVIDER_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("fluid_provider_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.FLUID_PROVIDER_LOGISTICS_PIPE));
            REQUESTER_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("requester_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.REQUESTER_LOGISTICS_PIPE));
            SUPPLIER_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("supplier_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.SUPPLIER_LOGISTICS_PIPE));
            FLUID_SUPPLIER_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("fluid_supplier_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.FLUID_SUPPLIER_LOGISTICS_PIPE));
            CRAFTING_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("crafting_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.CRAFTING_LOGISTICS_PIPE));
            PROCESS_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("process_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.PROCESS_LOGISTICS_PIPE));
            SATELLITE_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("satellite_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.SATELLITE_LOGISTICS_PIPE));
            CHASSIS_LOGISTICS_PIPE_MK1 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk1",
                props -> new PipeBlock(pipeProps(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK1));
            CHASSIS_LOGISTICS_PIPE_MK2 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk2",
                props -> new PipeBlock(pipeProps(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK2));
            CHASSIS_LOGISTICS_PIPE_MK3 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk3",
                props -> new PipeBlock(pipeProps(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK3));
            CHASSIS_LOGISTICS_PIPE_MK4 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk4",
                props -> new PipeBlock(pipeProps(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK4));
            CHASSIS_LOGISTICS_PIPE_MK5 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk5",
                props -> new PipeBlock(pipeProps(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK5));
            POWER_JUNCTION = INSTANCE.registerBlockWithItem("power_junction",
                props -> new PowerJunctionBlock(props.strength(3.0f).sound(SoundType.METAL)));

            // Fluid transport
            COPPER_FLUID_PIPE = INSTANCE.registerBlockWithItem("copper_fluid_pipe",
                props -> new FluidPipeBlock(pipeProps(props), PipeTypes.COPPER_FLUID_PIPE), FluidPipeBlockItem::new);
            STONE_FLUID_PIPE = INSTANCE.registerBlockWithItem("stone_fluid_pipe",
                props -> new FluidPipeBlock(pipeProps(props), PipeTypes.STONE_FLUID_PIPE), FluidPipeBlockItem::new);
            GOLD_FLUID_PIPE = INSTANCE.registerBlockWithItem("gold_fluid_pipe",
                props -> new FluidPipeBlock(pipeProps(props), PipeTypes.GOLD_FLUID_PIPE), FluidPipeBlockItem::new);
            INSERTION_FLUID_PIPE = INSTANCE.registerBlockWithItem("insertion_fluid_pipe",
                props -> new FluidPipeBlock(pipeProps(props), PipeTypes.INSERTION_FLUID_PIPE), FluidPipeBlockItem::new);
            MERGER_FLUID_PIPE = INSTANCE.registerBlockWithItem("merger_fluid_pipe",
                props -> new FluidPipeBlock(pipeProps(props), PipeTypes.MERGER_FLUID_PIPE), FluidPipeBlockItem::new);
            FLUID_EXTRACTOR_PIPE = INSTANCE.registerBlockWithItem("fluid_extractor_pipe",
                props -> new FluidPipeBlock(pipeProps(props), PipeTypes.FLUID_EXTRACTOR_PIPE), FluidPipeBlockItem::new);
            VOID_FLUID_PIPE = INSTANCE.registerBlockWithItem("void_fluid_pipe",
                props -> new FluidPipeBlock(pipeProps(props), PipeTypes.VOID_FLUID_PIPE), FluidPipeBlockItem::new);
            BYPASS_FLUID_PIPE = INSTANCE.registerBlockWithItem("bypass_fluid_pipe",
                props -> new FluidPipeBlock(pipeProps(props), PipeTypes.BYPASS_FLUID_PIPE), FluidPipeBlockItem::new);
            GLASS_TANK = INSTANCE.registerBlockWithItem("glass_tank",
                props -> new GlassTankBlock(tankProps(props)), GlassTankBlockItem::new);
        }
    }

    public static final class ITEM {
        private ITEM() {}

        public static Item BLANK_MODULE;
        public static Item ITEM_SINK_MODULE;
        public static Item POLYMORPHIC_SINK_MODULE;
        public static Item ENCHANTMENT_SINK_MODULE;
        public static Item MOD_ITEM_SINK_MODULE;
        public static Item PASSIVE_SUPPLIER_MODULE;
        public static Item ACTIVE_SUPPLIER_MODULE;
        public static Item PROVIDER_MODULE;
        public static Item PROVIDER_MODULE_MKII;
        public static Item FLUID_PROVIDER_MODULE;
        public static Item FLUID_SUPPLIER_MODULE;
        public static Item EXTRACTOR_MODULE;
        public static Item EXTRACTOR_MODULE_MKII;
        public static Item EXTRACTOR_MODULE_MKIII;
        public static Item CRAFTER_MODULE;
        public static Item CRAFTER_MODULE_MKII;
        public static Item CRAFTER_MODULE_MKIII;
        public static Item QUICKSORT_MODULE;
        public static Item TERMINUS_MODULE;

        // Hidden internal item — moves a fluid through the item network; never in a creative tab.
        public static Item FLUID_PACKET;

        public static Item WHITE_MARKING_FLUID;
        public static Item ORANGE_MARKING_FLUID;
        public static Item MAGENTA_MARKING_FLUID;
        public static Item LIGHT_BLUE_MARKING_FLUID;
        public static Item YELLOW_MARKING_FLUID;
        public static Item LIME_MARKING_FLUID;
        public static Item PINK_MARKING_FLUID;
        public static Item GRAY_MARKING_FLUID;
        public static Item LIGHT_GRAY_MARKING_FLUID;
        public static Item CYAN_MARKING_FLUID;
        public static Item PURPLE_MARKING_FLUID;
        public static Item BLUE_MARKING_FLUID;
        public static Item BROWN_MARKING_FLUID;
        public static Item GREEN_MARKING_FLUID;
        public static Item RED_MARKING_FLUID;
        public static Item BLACK_MARKING_FLUID;

        // Forward map used by CREATIVE and ALIAS for iteration.
        private static final Map<DyeColor, Item> MARKING_FLUIDS_BY_COLOR = new EnumMap<>(DyeColor.class);

        static void register() {
            BLANK_MODULE = INSTANCE.registerItem("blank_module", Item::new);
            ITEM_SINK_MODULE = INSTANCE.registerItem("item_sink_module",
                    props -> new ModuleItem(props, () -> new SinkModule(7)));
            POLYMORPHIC_SINK_MODULE = INSTANCE.registerItem("polymorphic_sink_module",
                    props -> new ModuleItem(props, () -> new PolymorphicSinkModule(7)));
            ENCHANTMENT_SINK_MODULE = INSTANCE.registerItem("enchantment_sink_module",
                    props -> new ModuleItem(props, () -> new EnchantmentSinkModule(3)));
            MOD_ITEM_SINK_MODULE = INSTANCE.registerItem("mod_item_sink_module",
                    props -> new ModuleItem(props, () -> new ModSinkModule(5)));
            PASSIVE_SUPPLIER_MODULE = INSTANCE.registerItem("passive_supplier_module",
                    props -> new ModuleItem(props, () -> new PassiveSupplierModule(8)));
            ACTIVE_SUPPLIER_MODULE = INSTANCE.registerItem("active_supplier_module",
                    props -> new ModuleItem(props, SupplierModule::new));
            PROVIDER_MODULE = INSTANCE.registerItem("provider_module",
                    props -> new ModuleItem(props, () -> new ProviderModule(8, 1)));
            PROVIDER_MODULE_MKII = INSTANCE.registerItem("provider_mkii_module",
                    props -> new ModuleItem(props, () -> new ProviderModule(64, 4)));
            FLUID_PROVIDER_MODULE = INSTANCE.registerItem("fluid_provider_module",
                    props -> new ModuleItem(props, FluidProviderModule::new));
            FLUID_SUPPLIER_MODULE = INSTANCE.registerItem("fluid_supplier_module",
                    props -> new ModuleItem(props, FluidSupplierModule::new));
            EXTRACTOR_MODULE = INSTANCE.registerItem("extractor_module",
                    props -> new ModuleItem(props, () -> new BasicExtractorModule(8, 80)));
            EXTRACTOR_MODULE_MKII = INSTANCE.registerItem("extractor_module_mkii",
                    props -> new ModuleItem(props, () -> new BasicExtractorModule(8, 20)));
            EXTRACTOR_MODULE_MKIII = INSTANCE.registerItem("extractor_module_mkiii",
                    props -> new ModuleItem(props, () -> new AdvancedExtractorModule(8, 20)));
            CRAFTER_MODULE = INSTANCE.registerItem("crafter_module",
                    props -> new ModuleItem(props, () -> new CraftingModule(1, 1)));
            CRAFTER_MODULE_MKII = INSTANCE.registerItem("crafter_mkii_module",
                    props -> new ModuleItem(props, () -> new CraftingModule(16, 1)));
            CRAFTER_MODULE_MKIII = INSTANCE.registerItem("crafter_mkiii_module",
                    props -> new ModuleItem(props, () -> new CraftingModule(64, 4)));
            QUICKSORT_MODULE = INSTANCE.registerItem("quicksort_module",
                    props -> new ModuleItem(props, QuickSortModule::new));
            TERMINUS_MODULE = INSTANCE.registerItem("terminus_module",
                    props -> new ModuleItem(props, () -> new TerminusModule(4)));

            FLUID_PACKET = INSTANCE.registerItem("fluid_packet", props -> new FluidPacketItem(props.stacksTo(1)));

            WHITE_MARKING_FLUID = markingFluid(DyeColor.WHITE);
            ORANGE_MARKING_FLUID = markingFluid(DyeColor.ORANGE);
            MAGENTA_MARKING_FLUID = markingFluid(DyeColor.MAGENTA);
            LIGHT_BLUE_MARKING_FLUID = markingFluid(DyeColor.LIGHT_BLUE);
            YELLOW_MARKING_FLUID = markingFluid(DyeColor.YELLOW);
            LIME_MARKING_FLUID = markingFluid(DyeColor.LIME);
            PINK_MARKING_FLUID = markingFluid(DyeColor.PINK);
            GRAY_MARKING_FLUID = markingFluid(DyeColor.GRAY);
            LIGHT_GRAY_MARKING_FLUID = markingFluid(DyeColor.LIGHT_GRAY);
            CYAN_MARKING_FLUID = markingFluid(DyeColor.CYAN);
            PURPLE_MARKING_FLUID = markingFluid(DyeColor.PURPLE);
            BLUE_MARKING_FLUID = markingFluid(DyeColor.BLUE);
            BROWN_MARKING_FLUID = markingFluid(DyeColor.BROWN);
            GREEN_MARKING_FLUID = markingFluid(DyeColor.GREEN);
            RED_MARKING_FLUID = markingFluid(DyeColor.RED);
            BLACK_MARKING_FLUID = markingFluid(DyeColor.BLACK);
        }

        private static Item markingFluid(DyeColor color) {
            Item item = INSTANCE.registerItem(color.getName() + "_marking_fluid",
                    props -> new MarkingFluidItem(props.stacksTo(1).durability(16), color));
            MARKING_FLUIDS_BY_COLOR.put(color, item);
            return item;
        }
    }

    public static final class ENTITY {
        public static BlockEntityType<PipeBlockEntity> PIPE_BLOCK_ENTITY;
        public static BlockEntityType<PowerJunctionBlockEntity> POWER_JUNCTION_BLOCK_ENTITY;
        public static BlockEntityType<FluidPipeBlockEntity> FLUID_PIPE_BLOCK_ENTITY;
        public static BlockEntityType<GlassTankBlockEntity> GLASS_TANK_BLOCK_ENTITY;

        private ENTITY() {}

        static void register() {
            POWER_JUNCTION_BLOCK_ENTITY = INSTANCE.registerBlockEntity("power_junction",
                PowerJunctionBlockEntity::new, BLOCK.POWER_JUNCTION);
            FLUID_PIPE_BLOCK_ENTITY = INSTANCE.registerBlockEntity("fluid_pipe",
                FluidPipeBlockEntity::new,
                BLOCK.COPPER_FLUID_PIPE, BLOCK.STONE_FLUID_PIPE, BLOCK.GOLD_FLUID_PIPE,
                BLOCK.INSERTION_FLUID_PIPE, BLOCK.MERGER_FLUID_PIPE, BLOCK.FLUID_EXTRACTOR_PIPE,
                BLOCK.VOID_FLUID_PIPE, BLOCK.BYPASS_FLUID_PIPE);
            GLASS_TANK_BLOCK_ENTITY = INSTANCE.registerBlockEntity("glass_tank",
                GlassTankBlockEntity::new, BLOCK.GLASS_TANK);
            PIPE_BLOCK_ENTITY = INSTANCE.registerBlockEntity("pipe",
                PipeBlockEntity::new,
                BLOCK.STONE_TRANSPORT_PIPE,
                BLOCK.ITEM_EXTRACTOR_PIPE,
                BLOCK.ITEM_MERGER_PIPE,
                BLOCK.GOLD_TRANSPORT_PIPE,
                BLOCK.ITEM_FILTER_PIPE,
                BLOCK.COPPER_TRANSPORT_PIPE,
                BLOCK.ITEM_PASSTHROUGH_PIPE,
                BLOCK.ITEM_INSERTION_PIPE,
                BLOCK.ITEM_VOID_PIPE,
                BLOCK.BASIC_LOGISTICS_PIPE,
                BLOCK.PROVIDER_LOGISTICS_PIPE,
                BLOCK.FLUID_PROVIDER_LOGISTICS_PIPE,
                BLOCK.REQUESTER_LOGISTICS_PIPE,
                BLOCK.SUPPLIER_LOGISTICS_PIPE,
                BLOCK.FLUID_SUPPLIER_LOGISTICS_PIPE,
                BLOCK.CRAFTING_LOGISTICS_PIPE,
                BLOCK.PROCESS_LOGISTICS_PIPE,
                BLOCK.SATELLITE_LOGISTICS_PIPE,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK1,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK2,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK3,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK4,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK5);
        }
    }

    public static final class DATA {
        public static DataComponentType<WeatheringState> WEATHERING_STATE;
        public static DataComponentType<FluidPacket> FLUID_PACKET;

        private DATA() {}

        static void register() {
            WEATHERING_STATE = Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    LogisticsPipe.resource("weathering_state").toIdentifier(),
                    DataComponentType.<WeatheringState>builder()
                            .persistent(WeatheringState.CODEC)
                            .build());
            FLUID_PACKET = Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    LogisticsPipe.resource("fluid_packet").toIdentifier(),
                    DataComponentType.<FluidPacket>builder()
                            .persistent(FluidPacket.CODEC)
                            .networkSynchronized(FluidPacket.STREAM_CODEC)
                            .build());
        }
    }

    public static final class SCREEN {
        public static MenuType<ItemFilterScreenHandler> ITEM_FILTER;
        public static MenuType<com.logistics.pipe.ui.RequesterScreenHandler> REQUESTER;
        public static MenuType<com.logistics.pipe.ui.SupplierScreenHandler> SUPPLIER;
        public static MenuType<com.logistics.pipe.ui.FluidSupplierScreenHandler> FLUID_SUPPLIER;
        public static MenuType<com.logistics.pipe.ui.ProviderScreenHandler> PROVIDER;
        public static MenuType<com.logistics.pipe.ui.SinkScreenHandler> SINK;
        public static MenuType<com.logistics.pipe.ui.CraftingScreenHandler> CRAFTING;
        public static MenuType<com.logistics.pipe.ui.ProcessScreenHandler> PROCESS;
        public static MenuType<com.logistics.pipe.ui.SatelliteScreenHandler> SATELLITE;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK1;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK2;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK3;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK4;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK5;
        public static MenuType<com.logistics.pipe.ui.AdvancedExtractorScreenHandler> ADVANCED_EXTRACTOR;
        public static MenuType<com.logistics.pipe.ui.ModSinkScreenHandler> MOD_SINK;

        private SCREEN() {}

        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> chassisMenuTypeFor(int slotCount) {
            return switch (slotCount) {
                case 1 -> CHASSIS_MK1;
                case 2 -> CHASSIS_MK2;
                case 3 -> CHASSIS_MK3;
                case 4 -> CHASSIS_MK4;
                case 8 -> CHASSIS_MK5;
                default -> throw new IllegalArgumentException("Invalid chassis slot count: " + slotCount);
            };
        }

        static void register() {
            ITEM_FILTER = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("item_filter").toIdentifier(),
                    new MenuType<>(ItemFilterScreenHandler::new, FeatureFlagSet.of()));
            REQUESTER = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("requester").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.RequesterScreenHandler::new, FeatureFlagSet.of()));
            SUPPLIER = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("supplier").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.SupplierScreenHandler::new, FeatureFlagSet.of()));
            FLUID_SUPPLIER = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("fluid_supplier").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.FluidSupplierScreenHandler::new, FeatureFlagSet.of()));
            PROVIDER = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("provider").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.ProviderScreenHandler::new, FeatureFlagSet.of()));
            SINK = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("sink").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.SinkScreenHandler::new, FeatureFlagSet.of()));
            CRAFTING = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("crafting").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.CraftingScreenHandler::new, FeatureFlagSet.of()));
            PROCESS = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("process").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.ProcessScreenHandler::new, FeatureFlagSet.of()));
            SATELLITE = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("satellite").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.SatelliteScreenHandler::new, FeatureFlagSet.of()));
            CHASSIS_MK1 = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("chassis_mk1").toIdentifier(),
                    new MenuType<>((syncId, inv) -> new com.logistics.pipe.ui.ChassisScreenHandler(syncId, inv, 1), FeatureFlagSet.of()));
            CHASSIS_MK2 = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("chassis_mk2").toIdentifier(),
                    new MenuType<>((syncId, inv) -> new com.logistics.pipe.ui.ChassisScreenHandler(syncId, inv, 2), FeatureFlagSet.of()));
            CHASSIS_MK3 = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("chassis_mk3").toIdentifier(),
                    new MenuType<>((syncId, inv) -> new com.logistics.pipe.ui.ChassisScreenHandler(syncId, inv, 3), FeatureFlagSet.of()));
            CHASSIS_MK4 = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("chassis_mk4").toIdentifier(),
                    new MenuType<>((syncId, inv) -> new com.logistics.pipe.ui.ChassisScreenHandler(syncId, inv, 4), FeatureFlagSet.of()));
            CHASSIS_MK5 = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("chassis_mk5").toIdentifier(),
                    new MenuType<>((syncId, inv) -> new com.logistics.pipe.ui.ChassisScreenHandler(syncId, inv, 8), FeatureFlagSet.of()));
            ADVANCED_EXTRACTOR = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("advanced_extractor").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.AdvancedExtractorScreenHandler::new, FeatureFlagSet.of()));
            MOD_SINK = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("mod_sink").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.ModSinkScreenHandler::new, FeatureFlagSet.of()));
        }
    }

    public static final class CREATIVE {
        public static final LogisticsCreativeTab TAB = LogisticsCreativeTab.create(
            LogisticsMod.modId("2_pipes"),
            Component.translatable("itemGroup.logistics.2_pipes"),
            () -> new ItemStack(BLOCK.COPPER_TRANSPORT_PIPE)
        );

        private CREATIVE() {}

        static void register() {
            // Marking fluids
            TAB.addAll(ITEM.MARKING_FLUIDS_BY_COLOR.values());

            // Item Transport
            TAB.add(BLOCK.STONE_TRANSPORT_PIPE);
            TAB.addWithVariants(BLOCK.COPPER_TRANSPORT_PIPE);
            TAB.add(BLOCK.GOLD_TRANSPORT_PIPE);
            TAB.add(BLOCK.ITEM_EXTRACTOR_PIPE);
            TAB.add(BLOCK.ITEM_MERGER_PIPE);
            TAB.add(BLOCK.ITEM_INSERTION_PIPE);
            TAB.add(BLOCK.ITEM_FILTER_PIPE);
            TAB.add(BLOCK.ITEM_PASSTHROUGH_PIPE);
            TAB.add(BLOCK.ITEM_VOID_PIPE);

            // Fluid Transport
            TAB.add(BLOCK.STONE_FLUID_PIPE);
            TAB.addWithVariants(BLOCK.COPPER_FLUID_PIPE);
            TAB.add(BLOCK.GOLD_FLUID_PIPE);
            TAB.add(BLOCK.FLUID_EXTRACTOR_PIPE);
            TAB.add(BLOCK.MERGER_FLUID_PIPE);
            TAB.add(BLOCK.INSERTION_FLUID_PIPE);
            TAB.add(BLOCK.BYPASS_FLUID_PIPE);
            TAB.add(BLOCK.VOID_FLUID_PIPE);
            TAB.add(BLOCK.GLASS_TANK);

            // Logistics Pipes
            TAB.add(BLOCK.BASIC_LOGISTICS_PIPE);
            TAB.add(BLOCK.PROVIDER_LOGISTICS_PIPE);
            TAB.add(BLOCK.FLUID_PROVIDER_LOGISTICS_PIPE);
            TAB.add(BLOCK.REQUESTER_LOGISTICS_PIPE);
            TAB.add(BLOCK.SUPPLIER_LOGISTICS_PIPE);
            TAB.add(BLOCK.FLUID_SUPPLIER_LOGISTICS_PIPE);
            TAB.add(BLOCK.CRAFTING_LOGISTICS_PIPE);
            TAB.add(BLOCK.PROCESS_LOGISTICS_PIPE);
            TAB.add(BLOCK.SATELLITE_LOGISTICS_PIPE);
            TAB.add(BLOCK.CHASSIS_LOGISTICS_PIPE_MK1);
            TAB.add(BLOCK.CHASSIS_LOGISTICS_PIPE_MK2);
            TAB.add(BLOCK.CHASSIS_LOGISTICS_PIPE_MK3);
            TAB.add(BLOCK.CHASSIS_LOGISTICS_PIPE_MK4);
            TAB.add(BLOCK.CHASSIS_LOGISTICS_PIPE_MK5);
            TAB.add(BLOCK.POWER_JUNCTION);

            // Logistics Modules
            TAB.add(ITEM.BLANK_MODULE);
            TAB.add(ITEM.ITEM_SINK_MODULE);
            TAB.add(ITEM.POLYMORPHIC_SINK_MODULE);
            TAB.add(ITEM.ENCHANTMENT_SINK_MODULE);
            TAB.add(ITEM.MOD_ITEM_SINK_MODULE);
            TAB.add(ITEM.PASSIVE_SUPPLIER_MODULE);
            TAB.add(ITEM.ACTIVE_SUPPLIER_MODULE);
            TAB.add(ITEM.PROVIDER_MODULE);
            TAB.add(ITEM.PROVIDER_MODULE_MKII);
            TAB.add(ITEM.FLUID_PROVIDER_MODULE);
            TAB.add(ITEM.FLUID_SUPPLIER_MODULE);
            TAB.add(ITEM.EXTRACTOR_MODULE);
            TAB.add(ITEM.EXTRACTOR_MODULE_MKII);
            TAB.add(ITEM.EXTRACTOR_MODULE_MKIII);
            TAB.add(ITEM.CRAFTER_MODULE);
            TAB.add(ITEM.CRAFTER_MODULE_MKII);
            TAB.add(ITEM.CRAFTER_MODULE_MKIII);
            TAB.add(ITEM.QUICKSORT_MODULE);
            TAB.add(ITEM.TERMINUS_MODULE);

            CreativeTabRegistrar.INSTANCE.registerTab(TAB);
        }
    }

    public static final class ALIAS {
        private ALIAS() {}

        static void register() {
            // Fluid pipes and the glass tank moved from the former fluid unit into the pipe domain.
            bridgeFluid("copper_fluid_pipe", BLOCK.COPPER_FLUID_PIPE);
            bridgeFluid("stone_fluid_pipe", BLOCK.STONE_FLUID_PIPE);
            bridgeFluid("gold_fluid_pipe", BLOCK.GOLD_FLUID_PIPE);
            bridgeFluid("insertion_fluid_pipe", BLOCK.INSERTION_FLUID_PIPE);
            bridgeFluid("merger_fluid_pipe", BLOCK.MERGER_FLUID_PIPE);
            bridgeFluid("fluid_extractor_pipe", BLOCK.FLUID_EXTRACTOR_PIPE);
            bridgeFluid("void_fluid_pipe", BLOCK.VOID_FLUID_PIPE);
            bridgeFluid("bypass_fluid_pipe", BLOCK.BYPASS_FLUID_PIPE);
            bridgeFluid("glass_tank", BLOCK.GLASS_TANK);
            INSTANCE.registerBlockEntityAlias("fluid/fluid_pipe", ENTITY.FLUID_PIPE_BLOCK_ENTITY);
            INSTANCE.registerBlockEntityAlias("fluid/glass_tank", ENTITY.GLASS_TANK_BLOCK_ENTITY);
        }

        private static void bridgeFluid(String name, Block block) {
            INSTANCE.registerBlockAlias("fluid/" + name, block);
            INSTANCE.registerItemAlias("fluid/" + name, block.asItem());
        }
    }

}

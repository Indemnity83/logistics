package com.logistics;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigEntries;
import com.indemnity83.configory.ConfigKey;
import com.logistics.core.LogisticsConfigMigrator;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.LogisticsCreativeTab;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.logistics.power.block.BatteryBlock;
import com.logistics.power.block.BatteryBlockItem;
import com.logistics.power.block.CreativeSinkBlock;
import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.power.block.entity.CreativeSinkBlockEntity;
import com.logistics.power.cable.CableBlock;
import com.logistics.power.cable.CableBlockEntity;
import com.logistics.power.cable.CableTier;
import com.logistics.power.engine.block.CreativeEngineBlock;
import com.logistics.power.engine.block.FuelEngineBlock;
import com.logistics.power.engine.block.StirlingEngineBlock;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.FuelEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.logistics.power.engine.ui.FuelEngineScreenHandler;
import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class LogisticsPower extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsPower INSTANCE = new LogisticsPower();

    @Override
    protected String domain() {
        return "power";
    }

    public static ResourceId resource(String name) {
        return INSTANCE.domainResource(name);
    }

    public static ResourceId model(String name) {
        return INSTANCE.domainModelResource(name);
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public void registerConfig() {
        CONFIG.register();
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        BLOCK.register();
        ENTITY.register();
        SCREEN.register();
        CREATIVE.register();
    }

    /**
     * Engine + power tuning — one file per unit under {@code config/logistics/engines/} and
     * {@code config/logistics/power/}. Every engine shares a {@code buffer_capacity} knob; defaults
     * match the historical hardcoded values, so this is a pure "make it configurable" surface.
     */
    public static final class CONFIG extends ConfigEntries {
        private static final Config stirling = configFor(LogisticsConfigHost.MOD_ID, "engines.stirling");
        private static final Config creative = configFor(LogisticsConfigHost.MOD_ID, "engines.creative");
        private static final Config fuel = configFor(LogisticsConfigHost.MOD_ID, "engines.fuel");
        private static final Config battery = configFor(LogisticsConfigHost.MOD_ID, "power.battery");
        private static final Config cables = configFor(LogisticsConfigHost.MOD_ID, "power.cables");

        private CONFIG() {}


        // Stirling engine
        public static final ConfigKey<Double> STIRLING_MIN_OUTPUT = stirling.defineDouble("min_output", 3.0)
                .min(0.0)
                .finite()
                .maxValueOf(() -> CONFIG.STIRLING_MAX_OUTPUT)
                .describe("Minimum RF/t output")
                .register();
        public static final ConfigKey<Double> STIRLING_MAX_OUTPUT = stirling.defineDouble("max_output", 10.0)
                .min(0.0)
                .finite()
                .minValueOf(() -> CONFIG.STIRLING_MIN_OUTPUT)
                .describe("Maximum RF/t output")
                .register();
        public static final ConfigKey<Long> STIRLING_BUFFER_CAPACITY = stirling.defineLong("buffer_capacity", 10_000L)
                .min(0L)
                .describe("Internal RF buffer capacity")
                .register();

        // Creative engine
        public static final ConfigKey<Long> CREATIVE_BUFFER_CAPACITY = creative.defineLong("buffer_capacity", 10_000L)
                .min(0L)
                .describe("Internal RF buffer capacity")
                .register();

        // Fuel engine
        public static final ConfigKey<Long> FUEL_MIN_OUTPUT = fuel.defineLong("min_output", 10L)
                .min(0L)
                .describe("Minimum RF/t generated while a fuel batch is burning")
                .register();
        public static final ConfigKey<Long> FUEL_MAX_OUTPUT = fuel.defineLong("max_output", 40L)
                .min(1L)
                .describe("Maximum RF/t generated")
                .register();
        public static final ConfigKey<Long> FUEL_BUFFER_CAPACITY = fuel.defineLong("buffer_capacity", 10_000L)
                .min(0L)
                .describe("Internal RF buffer capacity")
                .register();
        public static final ConfigKey<Double> FUEL_MAX_TEMPERATURE = fuel.defineDouble("max_temperature", 250.0)
                .min(1.0)
                .finite()
                .describe("Temperature at which the engine overheats and thermally shuts down")
                .register();
        public static final ConfigKey<Double> FUEL_WASTE_HEAT_PER_RF = fuel.defineDouble("waste_heat_per_rf", 0.25)
                .min(0.0)
                .finite()
                .describe("Heat added per RF of generation that cannot fit into the buffer")
                .register();
        public static final ConfigKey<Long> FUEL_TANK_CAPACITY = fuel.defineLong("fuel_tank_capacity_mb", 4_000L)
                .min(100L)
                .describe("Fuel tank capacity (mB)")
                .register();
        public static final ConfigKey<Long> COOLANT_TANK_CAPACITY = fuel.defineLong("coolant_tank_capacity_mb", 4_000L)
                .min(100L)
                .describe("Coolant tank capacity (mB)")
                .register();

        // Battery
        public static final ConfigKey<Long> BATTERY_CAPACITY = battery.defineLong("capacity", 100_000L)
                .min(0L)
                .describe("Total RF storage")
                .register();
        public static final ConfigKey<Long> BATTERY_MAX_IO = battery.defineLong("max_io", 1_000L)
                .min(0L)
                .describe("Max RF/t inserted or extracted per side")
                .register();
        public static final ConfigKey<Long> BATTERY_OUTPUT_PER_SIDE = battery.defineLong("output_per_side", 200L)
                .min(0L)
                .describe("Max RF/t actively pushed into each adjacent machine")
                .register();

        // Cables
        public static final ConfigKey<Long> CABLE_COPPER_TRANSFER = cables.defineLong("copper", 30L)
                .min(0L)
                .describe("Copper cable RF/t throughput")
                .register();
        public static final ConfigKey<Long> CABLE_GOLD_TRANSFER = cables.defineLong("gold", 60L)
                .min(0L)
                .describe("Gold cable RF/t throughput")
                .register();
        public static final ConfigKey<Long> CABLE_ENDER_TRANSFER = cables.defineLong("ender", 120L)
                .min(0L)
                .describe("Ender cable RF/t throughput")
                .register();

        static void register() {
            stirling.registerSanitizeHook(() -> stirling.repairMinMax(STIRLING_MIN_OUTPUT, STIRLING_MAX_OUTPUT));
            LogisticsConfigMigrator.mapLegacyPair(
                    "engine", "stirlingMinOutput", "stirlingMaxOutput", STIRLING_MIN_OUTPUT, STIRLING_MAX_OUTPUT);
        }
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static Block STIRLING_ENGINE;
        public static Block FUEL_ENGINE;
        public static Block CREATIVE_ENGINE;
        public static Block CREATIVE_SINK;
        public static Block BATTERY;
        public static Block COPPER_CABLE;
        public static Block GOLD_CABLE;
        public static Block ENDER_CABLE;

        static void register() {
            STIRLING_ENGINE = INSTANCE.registerBlockWithItem("stirling_engine",
                props -> new StirlingEngineBlock(props.strength(5.0f).sound(SoundType.COPPER).noOcclusion()));
            FUEL_ENGINE = INSTANCE.registerBlockWithItem("fuel_engine",
                props -> new FuelEngineBlock(props.strength(5.0f).sound(SoundType.COPPER).noOcclusion()));
            CREATIVE_ENGINE = INSTANCE.registerBlockWithItem("creative_engine",
                props -> new CreativeEngineBlock(props.strength(5.0f).sound(SoundType.STONE).noOcclusion()));
            CREATIVE_SINK = INSTANCE.registerBlockWithItem("creative_sink",
                props -> new CreativeSinkBlock(props.strength(5.0f).sound(SoundType.STONE)));
            BATTERY = INSTANCE.registerBlockWithItem("battery",
                props -> new BatteryBlock(props.strength(3.0f).sound(SoundType.METAL)),
                BatteryBlockItem::new);
            COPPER_CABLE = registerCable("copper_cable", CableTier.COPPER, SoundType.COPPER);
            GOLD_CABLE = registerCable("gold_cable", CableTier.GOLD, SoundType.METAL);
            ENDER_CABLE = registerCable("ender_cable", CableTier.ENDER, SoundType.AMETHYST);
        }

        private static Block registerCable(String name, CableTier tier, SoundType soundType) {
            return INSTANCE.registerBlockWithItem(name,
                    props -> new CableBlock(props.strength(1.5f).sound(soundType).noOcclusion().dynamicShape(), tier));
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        public static BlockEntityType<StirlingEngineBlockEntity> STIRLING_ENGINE_BLOCK_ENTITY;
        public static BlockEntityType<FuelEngineBlockEntity> FUEL_ENGINE_BLOCK_ENTITY;
        public static BlockEntityType<CreativeEngineBlockEntity> CREATIVE_ENGINE_BLOCK_ENTITY;
        public static BlockEntityType<CreativeSinkBlockEntity> CREATIVE_SINK_BLOCK_ENTITY;
        public static BlockEntityType<BatteryBlockEntity> BATTERY_BLOCK_ENTITY;
        public static BlockEntityType<CableBlockEntity> CABLE_BLOCK_ENTITY;

        static void register() {
            STIRLING_ENGINE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("stirling_engine", StirlingEngineBlockEntity::new, BLOCK.STIRLING_ENGINE);
            FUEL_ENGINE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("fuel_engine", FuelEngineBlockEntity::new, BLOCK.FUEL_ENGINE);
            CREATIVE_ENGINE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("creative_engine", CreativeEngineBlockEntity::new, BLOCK.CREATIVE_ENGINE);
            CREATIVE_SINK_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("creative_sink", CreativeSinkBlockEntity::new, BLOCK.CREATIVE_SINK);
            BATTERY_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("battery", BatteryBlockEntity::new, BLOCK.BATTERY);
            CABLE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("cable", CableBlockEntity::new,
                        BLOCK.COPPER_CABLE, BLOCK.GOLD_CABLE, BLOCK.ENDER_CABLE);
        }
    }

    public static final class SCREEN {
        private SCREEN() {}

        public static MenuType<StirlingEngineScreenHandler> STIRLING_ENGINE;
        public static MenuType<FuelEngineScreenHandler> FUEL_ENGINE;

        static void register() {
            STIRLING_ENGINE = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPower.resource("stirling_engine").toIdentifier(),
                    new MenuType<>(StirlingEngineScreenHandler::new, FeatureFlagSet.of()));
            FUEL_ENGINE = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPower.resource("fuel_engine").toIdentifier(),
                    new MenuType<>(FuelEngineScreenHandler::new, FeatureFlagSet.of()));
        }
    }

    public static final class CREATIVE {
        public static final LogisticsCreativeTab TAB = LogisticsCreativeTab.create(
            LogisticsMod.modId("3_power"),
            Component.translatable("itemGroup.logistics.3_power"),
            () -> new ItemStack(BLOCK.STIRLING_ENGINE)
        );

        private CREATIVE() {}

        static void register() {
            TAB.add(BLOCK.STIRLING_ENGINE);
            TAB.add(BLOCK.FUEL_ENGINE);
            TAB.add(BLOCK.CREATIVE_ENGINE);
            TAB.add(BLOCK.CREATIVE_SINK);
            TAB.add(BLOCK.BATTERY);
            TAB.add(BLOCK.COPPER_CABLE);
            TAB.add(BLOCK.GOLD_CABLE);
            TAB.add(BLOCK.ENDER_CABLE);
            CreativeTabRegistrar.INSTANCE.registerTab(TAB);
        }
    }
}

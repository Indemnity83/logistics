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
import com.logistics.power.engine.block.SteamEngineBlock;
import com.logistics.power.engine.block.StirlingEngineBlock;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.SteamEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.logistics.power.engine.ui.SteamEngineScreenHandler;
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
        private static final Config steam = configFor(LogisticsConfigHost.MOD_ID, "engines.steam");
        private static final Config creative = configFor(LogisticsConfigHost.MOD_ID, "engines.creative");
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

        // Steam engine — a thermal-mass boiler: fuel -> boiler heat -> steam -> pressure -> RF.
        public static final ConfigKey<Long> STEAM_MAX_OUTPUT = steam.defineLong("max_output", 40L)
                .min(1L)
                .describe("Maximum RF/t generated (flat at/above operating pressure)")
                .register();
        public static final ConfigKey<Long> STEAM_WATER_TANK_CAPACITY = steam.defineLong("water_tank_capacity_mb", 4_000L)
                .min(100L)
                .describe("Water tank capacity (mB)")
                .register();
        public static final ConfigKey<Long> STEAM_FIRING_RATE = steam.defineLong("firing_rate", 16L)
                .min(1L)
                .describe("Furnace burn ticks consumed per game tick while a committed reserve is firing")
                .register();
        public static final ConfigKey<Double> STEAM_MAX_PRESSURE = steam.defineDouble("max_pressure", 1000.0)
                .min(1.0)
                .finite()
                .describe("Safety pressure ceiling (clamp); the boiler never targets this")
                .register();
        public static final ConfigKey<Double> STEAM_OPERATING_PRESSURE = steam.defineDouble("operating_pressure", 400.0)
                .min(0.0)
                .finite()
                .describe("Pressure at/above which the engine outputs full RF/t")
                .register();
        public static final ConfigKey<Double> STEAM_TARGET_PRESSURE = steam.defineDouble("target_pressure", 800.0)
                .min(1.0)
                .finite()
                .describe("Pressure the boiler produces steam up to before pausing")
                .register();
        public static final ConfigKey<Double> STEAM_STEAM_RATE = steam.defineDouble("steam_rate", 12.0)
                .min(0.0)
                .finite()
                .describe("Maximum pressure produced per tick, at full steam quality (target heat)")
                .register();
        public static final ConfigKey<Double> STEAM_PRESSURE_PER_RF = steam.defineDouble("pressure_per_rf", 0.25)
                .min(0.0)
                .finite()
                .describe("Pressure consumed per RF generated")
                .register();
        public static final ConfigKey<Double> STEAM_WATER_CONVERSION = steam.defineDouble("water_conversion", 6.0)
                .min(0.0)
                .finite()
                .describe("Pressure produced per mB of water boiled")
                .register();
        public static final ConfigKey<Double> STEAM_CONDENSATION_RATE = steam.defineDouble("condensation_rate", 0.5)
                .min(0.0)
                .finite()
                .describe("Pressure lost per tick while the boiler is below the boiling point (steam condensing)")
                .register();
        public static final ConfigKey<Double> STEAM_MAX_BOILER_HEAT = steam.defineDouble("max_boiler_heat", 20_000.0)
                .min(1.0)
                .finite()
                .describe("Boiler thermal capacity (abstract heat units); heat past this is discarded")
                .register();
        public static final ConfigKey<Double> STEAM_BOILING_HEAT = steam.defineDouble("boiling_heat", 8_000.0)
                .min(0.0)
                .finite()
                .describe("Boiler heat at/above which steam begins to form")
                .register();
        public static final ConfigKey<Double> STEAM_REFUEL_HEAT = steam.defineDouble("refuel_heat", 14_000.0)
                .min(0.0)
                .finite()
                .describe("Commit the next fuel item once boiler heat falls to/below this")
                .register();
        public static final ConfigKey<Double> STEAM_TARGET_HEAT = steam.defineDouble("target_heat", 16_000.0)
                .min(1.0)
                .finite()
                .describe("Boiler heat at which steam reaches full quality (nominal operating temperature)")
                .register();
        public static final ConfigKey<Double> STEAM_HEAT_PER_BURN_TICK = steam.defineDouble("heat_per_burn_tick", 1.0)
                .min(0.0)
                .finite()
                .describe("Boiler heat added per furnace burn tick consumed")
                .register();
        public static final ConfigKey<Double> STEAM_PASSIVE_HEAT_LOSS = steam.defineDouble("passive_heat_loss", 0.1)
                .min(0.0)
                .finite()
                .describe("Boiler heat lost per tick in every state (keeps idle cheap, cooldown slow)")
                .register();
        public static final ConfigKey<Double> STEAM_LATENT_HEAT = steam.defineDouble("latent_heat", 0.5)
                .min(0.0)
                .finite()
                .describe("Boiler heat consumed per unit of pressure (steam) produced")
                .register();

        // Creative engine
        public static final ConfigKey<Long> CREATIVE_BUFFER_CAPACITY = creative.defineLong("buffer_capacity", 10_000L)
                .min(0L)
                .describe("Internal RF buffer capacity")
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
            steam.registerSanitizeHook(() -> {
                steam.repairMinMax(STEAM_OPERATING_PRESSURE, STEAM_TARGET_PRESSURE);
                steam.repairMinMax(STEAM_TARGET_PRESSURE, STEAM_MAX_PRESSURE);
                steam.repairMinMax(STEAM_BOILING_HEAT, STEAM_REFUEL_HEAT);
                steam.repairMinMax(STEAM_REFUEL_HEAT, STEAM_TARGET_HEAT);
                steam.repairMinMax(STEAM_TARGET_HEAT, STEAM_MAX_BOILER_HEAT);
            });
            LogisticsConfigMigrator.mapLegacyPair(
                    "engine", "stirlingMinOutput", "stirlingMaxOutput", STIRLING_MIN_OUTPUT, STIRLING_MAX_OUTPUT);
        }
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static Block STIRLING_ENGINE;
        public static Block STEAM_ENGINE;
        public static Block CREATIVE_ENGINE;
        public static Block CREATIVE_SINK;
        public static Block BATTERY;
        public static Block COPPER_CABLE;
        public static Block GOLD_CABLE;
        public static Block ENDER_CABLE;

        static void register() {
            STIRLING_ENGINE = INSTANCE.registerBlockWithItem("stirling_engine",
                props -> new StirlingEngineBlock(props.strength(5.0f).sound(SoundType.COPPER).noOcclusion()));
            STEAM_ENGINE = INSTANCE.registerBlockWithItem("steam_engine",
                props -> new SteamEngineBlock(props.strength(5.0f).sound(SoundType.COPPER).noOcclusion()));
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
        public static BlockEntityType<SteamEngineBlockEntity> STEAM_ENGINE_BLOCK_ENTITY;
        public static BlockEntityType<CreativeEngineBlockEntity> CREATIVE_ENGINE_BLOCK_ENTITY;
        public static BlockEntityType<CreativeSinkBlockEntity> CREATIVE_SINK_BLOCK_ENTITY;
        public static BlockEntityType<BatteryBlockEntity> BATTERY_BLOCK_ENTITY;
        public static BlockEntityType<CableBlockEntity> CABLE_BLOCK_ENTITY;

        static void register() {
            STIRLING_ENGINE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("stirling_engine", StirlingEngineBlockEntity::new, BLOCK.STIRLING_ENGINE);
            STEAM_ENGINE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("steam_engine", SteamEngineBlockEntity::new, BLOCK.STEAM_ENGINE);
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
        public static MenuType<SteamEngineScreenHandler> STEAM_ENGINE;

        static void register() {
            STIRLING_ENGINE = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPower.resource("stirling_engine").toIdentifier(),
                    new MenuType<>(StirlingEngineScreenHandler::new, FeatureFlagSet.of()));
            STEAM_ENGINE = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPower.resource("steam_engine").toIdentifier(),
                    new MenuType<>(SteamEngineScreenHandler::new, FeatureFlagSet.of()));
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
            TAB.add(BLOCK.STEAM_ENGINE);
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

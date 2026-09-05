package com.logistics;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigEntries;
import com.indemnity83.configory.ConfigKey;
import com.logistics.core.LogisticsConfigMigrator;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.crash.CrashReporting;
import com.logistics.core.item.WrenchItem;
import com.logistics.core.lib.platform.LogisticsCreativeTab;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.engine.block.RedstoneEngineBlock;
import com.logistics.core.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.core.marker.MarkerBlock;
import com.logistics.core.marker.MarkerBlockEntity;
import com.logistics.core.worldgen.BogPatchConfiguration;
import com.logistics.core.worldgen.BogPatchFeature;
import com.logistics.core.worldgen.OilSeepFeature;
import com.logistics.core.worldgen.OilSandsConfiguration;
import com.logistics.core.worldgen.OilSandsFeature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import com.logistics.core.lib.fluids.LogisticsLiquidBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;

public final class LogisticsCore extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsCore INSTANCE = new LogisticsCore();

    @Override
    protected String domain() {
        return "core";
    }

    public static ResourceId resource(String name) {
        return INSTANCE.domainResource(name);
    }

    public static ResourceId model(String name) {
        return INSTANCE.domainModelResource(name);
    }

    /**
     * A custom fluid produced by the Crucible. Registered per loader under
     * {@code logistics:core/<name>} (source) + {@code logistics:core/flowing_<name>}; rendered from its own
     * baked {@code still}/{@code flow} sprites (color + per-fluid alpha pre-applied in the texture), so the
     * {@code tint} stays {@code 0xFFFFFFFF} (untinted). {@code overlay} may be null.
     */
    public record FluidDef(
            String name, int tint, String still, String flow, String overlay, WorldFlow world,
            int luminance) {

        /** Flow tuning for a {@link #world()} fluid — a placeable {@code LiquidBlock} that spreads. */
        public record WorldFlow(int slopeFindDistance, int dropOff, int tickDelay) {}

        /** A fluid rendered from its baked textures under {@code block/core/fluid/}, untinted, dark. */
        public static FluidDef core(String name) {
            return core(name, 0);
        }

        /**
         * A tank/pipe/bucket fluid drawn from the vanilla water sprites, flat-tinted with {@code 0xRRGGBB}.
         * Full alpha is applied so NeoForge (which uses {@link #tint()} raw) renders it opaque.
         */
        public static FluidDef tinted(String name, int rgb) {
            int argb = 0xFF000000 | rgb;
            return new FluidDef(name, argb,
                "minecraft:block/water_still", "minecraft:block/water_flow", null, null, 0);
        }

        /** A {@link #core} fluid that emits {@code luminance} (0-15) when held in a pipe or tank. */
        public static FluidDef core(String name, int luminance) {
            String base = "logistics:block/core/fluid/" + name;
            return new FluidDef(name, 0xFFFFFFFF, base + "_still", base + "_flow", null, null, luminance);
        }

        /** A {@link #core} fluid that also has a world block, spreading with the given flow tuning. */
        public static FluidDef world(String name, int slopeFindDistance, int dropOff, int tickDelay) {
            String base = "logistics:block/core/fluid/" + name;
            return new FluidDef(name, 0xFFFFFFFF, base + "_still", base + "_flow", null,
                new WorldFlow(slopeFindDistance, dropOff, tickDelay), 0);
        }

        /** Whether this fluid can be placed in the world (has a {@code LiquidBlock}). */
        public boolean placeable() {
            return world != null;
        }
    }

    /** The custom fluids. Most are tank/pipe/bucket contents only; crude oil also flows in the world. */
    public static final List<FluidDef> CUSTOM_FLUIDS = List.of(
        FluidDef.core("liquid_redstone"),
        FluidDef.core("liquid_ender"),
        FluidDef.core("liquid_glowstone", 15),
        FluidDef.world("crude_oil", 2, 2, 15),
        FluidDef.core("liquid_biomass"),
        FluidDef.tinted("bio_fuel", 0xFFFC5C),
        FluidDef.tinted("fuel_oil", 0xFE8C01),
        FluidDef.tinted("seed_oil", 0xD9C74A));

    private static Map<Fluid, Integer> fluidLuminance;

    /**
     * Block light level (0-15) a pipe or tank should emit while holding {@code fluid}. Vanilla lava
     * glows at 15; a custom fluid glows at its {@link FluidDef#luminance()}; everything else is dark.
     */
    public static int fluidLuminance(Fluid fluid) {
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return 15;
        }
        if (fluidLuminance == null) {
            fluidLuminance = buildFluidLuminance();
        }
        return fluidLuminance.getOrDefault(fluid, 0);
    }

    /** Map each glowing custom fluid (source + flowing) to its luminance, resolved after registration. */
    private static Map<Fluid, Integer> buildFluidLuminance() {
        Map<Fluid, Integer> map = new IdentityHashMap<>();
        for (FluidDef def : CUSTOM_FLUIDS) {
            if (def.luminance() <= 0) {
                continue;
            }
            putIfRegistered(map, resource(def.name()), def.luminance());
            putIfRegistered(map, resource("flowing_" + def.name()), def.luminance());
        }
        return map;
    }

    private static void putIfRegistered(Map<Fluid, Integer> map, ResourceId id, int luminance) {
        Fluid fluid = BuiltInRegistries.FLUID.getValue(id.toIdentifier());
        if (fluid != Fluids.EMPTY) {
            map.put(fluid, luminance);
        }
    }

    @Override
    public int order() {
        return -100;
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
        BUCKET.register();
        ENTITY.register();
        WORLDGEN.register();
        CREATIVE.register();
        ALIAS.register();
    }

    /** Crash reporting toggles — {@code config/logistics/reporting.json}. Exposed under {@code /logistics config reporting}. */
    public static final class CONFIG extends ConfigEntries {
        private static final Config reporting = configFor(LogisticsConfigHost.MOD_ID, "reporting");
        private static final Config redstone = configFor(LogisticsConfigHost.MOD_ID, "engines.redstone");

        private CONFIG() {}

        // Redstone engine
        public static final ConfigKey<Long> REDSTONE_OUTPUT = redstone.defineLong("output", 10L)
                .min(0L)
                .describe("RF generated per generation interval")
                .register();
        public static final ConfigKey<Long> REDSTONE_GENERATION_INTERVAL =
                redstone.defineLong("generation_interval", 16L)
                        .min(1L)
                        .describe("Ticks between generation pulses when powered")
                        .register();
        public static final ConfigKey<Long> REDSTONE_BUFFER_CAPACITY = redstone.defineLong("buffer_capacity", 1_000L)
                .min(0L)
                .describe("Internal RF buffer capacity")
                .register();

        public static final ConfigKey<Boolean> CRASH_REPORTING_ENABLED = reporting.defineBoolean("enabled", false)
                .describe("Opt-in sanitized crash reporting via Sentry")
                .register();

        public static final ConfigKey<Boolean> CRASH_REPORTING_SHOW_NOTIFICATION =
                reporting.defineBoolean("show_notification", true)
                        .describe("Notify operators on join while crash reporting is active")
                        .register();

        public static final ConfigKey<String> CRASH_REPORTING_DSN_OVERRIDE =
                reporting.defineString("dsn_override", "")
                        .describe("Override the Sentry DSN (blank = built-in default)")
                        .register();

        static void register() {
            reporting.registerSanitizeHook(CrashReporting::reconcile);
            LogisticsConfigMigrator.mapLegacy("crashReporting", "enabled", CRASH_REPORTING_ENABLED);
            LogisticsConfigMigrator.mapLegacy("crashReporting", "notifyOperators", CRASH_REPORTING_SHOW_NOTIFICATION);
            LogisticsConfigMigrator.mapLegacy("crashReporting", "dsnOverride", CRASH_REPORTING_DSN_OVERRIDE);
            LogisticsConfigMigrator.mapLegacy("engine", "redstoneOutput", REDSTONE_OUTPUT);
        }
    }

    public static final class WORLDGEN {
        public static Feature<BogPatchConfiguration> BOG_PATCH;
        public static Feature<LakeFeature.Configuration> OIL_SEEP;
        public static Feature<OilSandsConfiguration> OIL_SANDS;

        private WORLDGEN() {}

        static void register() {
            BOG_PATCH = Registry.register(
                BuiltInRegistries.FEATURE,
                LogisticsMod.modId("bog_patch").toIdentifier(),
                new BogPatchFeature(BogPatchConfiguration.CODEC));
            OIL_SEEP = Registry.register(
                BuiltInRegistries.FEATURE,
                LogisticsMod.modId("oil_seep").toIdentifier(),
                new OilSeepFeature());
            OIL_SANDS = Registry.register(
                BuiltInRegistries.FEATURE,
                LogisticsMod.modId("oil_sands").toIdentifier(),
                new OilSandsFeature(OilSandsConfiguration.CODEC));
        }
    }

    /** A filled bucket per custom fluid ({@code logistics:core/<name>}), keyed by fluid name. */
    public static final class BUCKET {
        private static final java.util.Map<String, Item> BY_FLUID = new java.util.LinkedHashMap<>();

        private BUCKET() {}

        static void register() {
            for (FluidDef def : CUSTOM_FLUIDS) {
                if (def.placeable()) {
                    continue; // placeable fluids get a vanilla placing bucket, registered per loader
                }
                Item bucket = INSTANCE.registerItem(def.name(),
                    props -> new com.logistics.core.item.LogisticsBucketItem(def.name(), props.stacksTo(1)));
                BY_FLUID.put(def.name(), bucket);
            }
        }

        /** The filled-bucket item for a fluid name; placeable fluids resolve through the item registry. */
        public static Item forFluid(String name) {
            Item bucket = BY_FLUID.get(name);
            return bucket != null ? bucket : BuiltInRegistries.ITEM.getValue(resource(name).toIdentifier());
        }

        /** Fluid name → filled bucket item, for the tank-only fluids. */
        public static java.util.Map<String, Item> all() {
            return java.util.Collections.unmodifiableMap(BY_FLUID);
        }
    }

    /**
     * Registers a placeable fluid's {@code LiquidBlock} (water-like block properties). Called by each
     * loader's fluid registration once the source fluid exists, so ordering is never a concern.
     */
    public static LiquidBlock registerFluidBlock(String name, FlowingFluid source) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, resource(name).toIdentifier());
        LiquidBlock block = new LogisticsLiquidBlock(source, BlockBehaviour.Properties.of()
                .setId(key)
                .replaceable()
                .noCollision()
                .strength(100.0f)
                .pushReaction(PushReaction.POPPED)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    /** Registers a placeable fluid's vanilla {@link BucketItem} (places, picks up, and fills tanks). */
    public static BucketItem registerFluidBucket(String name, FlowingFluid source) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, resource(name).toIdentifier());
        BucketItem bucket = new BucketItem(source, new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .craftRemainder(Items.BUCKET));
        return Registry.register(BuiltInRegistries.ITEM, key, bucket);
    }

    public static final class BLOCK {
        public static Block TIN_ORE;
        public static Block DEEPSLATE_TIN_ORE;
        public static Block TIN_BLOCK;
        public static Block RAW_TIN_BLOCK;
        public static Block BRONZE_BLOCK;
        public static Block APATITE_ORE;
        public static Block APATITE_BLOCK;
        public static Block QUARTZ_CRYSTAL;
        public static Block MARKER;
        public static Block BOG_EARTH;
        public static Block OIL_SAND;
        public static Block OIL_RED_SAND;
        public static Block OIL_SHALE;
        public static Block REDSTONE_ENGINE;

        private BLOCK() {}

        static void register() {
            // Tin Ore and Storage Blocks
            TIN_ORE = INSTANCE.registerBlockWithItem("tin_ore",
                props -> new Block(props.strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
            DEEPSLATE_TIN_ORE = INSTANCE.registerBlockWithItem("deepslate_tin_ore",
                props -> new Block(props.strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops()));
            TIN_BLOCK = INSTANCE.registerBlockWithItem("tin_block",
                props -> new Block(props.strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
            RAW_TIN_BLOCK = INSTANCE.registerBlockWithItem("raw_tin_block",
                props -> new Block(props.strength(5.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

            // Bronze Storage Block
            BRONZE_BLOCK = INSTANCE.registerBlockWithItem("bronze_block",
                props -> new Block(props.strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

            // Apatite Ore and Storage Block
            APATITE_ORE = INSTANCE.registerBlockWithItem("apatite_ore",
                props -> new DropExperienceBlock(UniformInt.of(0, 2), props.strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
            APATITE_BLOCK = INSTANCE.registerBlockWithItem("apatite_block",
                props -> new Block(props.strength(5.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
            QUARTZ_CRYSTAL = INSTANCE.registerBlockWithItem("quartz_crystal",
                props -> new Block(props.strength(0.8f).sound(SoundType.GLASS).noOcclusion()));

            MARKER = INSTANCE.registerBlockWithItem("marker",
                props -> new MarkerBlock(props.strength(0.0f).sound(SoundType.WOOD).noCollision()));

            // Bog Earth — dirt-like soil that generates at swamp water edges; smelts into peat
            BOG_EARTH = INSTANCE.registerBlockWithItem("bog_earth",
                props -> new Block(props.strength(0.5f).sound(SoundType.MUD)));

            // Oil-bearing deposits — macerate into bitumen
            OIL_SAND = INSTANCE.registerBlockWithItem("oil_sand",
                props -> new Block(props.strength(0.5f).sound(SoundType.SAND)));
            OIL_RED_SAND = INSTANCE.registerBlockWithItem("oil_red_sand",
                props -> new Block(props.strength(0.5f).sound(SoundType.SAND)));
            OIL_SHALE = INSTANCE.registerBlockWithItem("oil_shale",
                props -> new Block(props.strength(0.6f).sound(SoundType.GRAVEL)));
            REDSTONE_ENGINE = INSTANCE.registerBlockWithItem("redstone_engine",
                props -> new RedstoneEngineBlock(
                    props.strength(2.0f).sound(SoundType.WOOD).noOcclusion().requiresCorrectToolForDrops()));
        }
    }

    public static final class ITEM {
        public static Item WRENCH;
        public static Item RAW_TIN;
        public static Item TIN_INGOT;
        public static Item TIN_NUGGET;
        public static Item BRONZE_INGOT;
        public static Item BRONZE_NUGGET;
        public static Item COPPER_NUGGET;
        public static Item APATITE;
        public static Item MACHINE_CORE;
        public static Item REDSTONE_RECEPTION_COIL;
        public static Item RUBBER;
        public static Item NATURAL_POLYMER;
        public static Item SYNTHETIC_POLYMER;

        public static Item WOODEN_GEAR;
        public static Item STONE_GEAR;
        public static Item COPPER_GEAR;
        public static Item IRON_GEAR;
        public static Item GOLD_GEAR;
        public static Item BRONZE_GEAR;
        public static Item DIAMOND_GEAR;
        public static Item NETHERITE_GEAR;

        // Macerator outputs — dusts
        public static Item APATITE_DUST;
        public static Item IRON_DUST;
        public static Item COPPER_DUST;
        public static Item TIN_DUST;
        public static Item BRONZE_DUST;
        public static Item GOLD_DUST;
        public static Item LAPIS_DUST;
        public static Item QUARTZ_DUST;
        public static Item COAL_DUST;
        public static Item AMETHYST_DUST;
        public static Item DIAMOND_DUST;
        public static Item EMERALD_DUST;
        public static Item NETHERITE_DUST;
        public static Item OBSIDIAN_DUST;
        public static Item ENDER_DUST;
        public static Item ECHO_DUST;
        public static Item PRISMARINE_DUST;
        public static Item SULFUR_DUST;
        public static Item QUICKSILVER;
        public static Item NITER;
        public static Item SILICON_MIX;
        public static Item SILICON_WAFER;
        public static Item FLOUR;
        public static Item SAWDUST;
        public static Item PEAT;
        public static Item PULPED_BIOMASS;
        public static Item SLAG;
        public static Item RICH_SLAG;
        public static Item BITUMEN;
        public static Item TAR;

        // Chips — logic components for pipe modules
        public static Item REDSTONE_CHIPSET;
        public static Item ECHO_CHIPSET;
        public static Item COPPER_CHIPSET;
        public static Item IRON_CHIPSET;
        public static Item GOLD_CHIPSET;
        public static Item DIAMOND_CHIPSET;
        public static Item NETHERITE_CHIPSET;

        // Valves — pipe chassis components
        public static Item TIN_VALVE;
        public static Item COPPER_VALVE;
        public static Item RUBBER_VALVE;
        public static Item BRONZE_VALVE;
        public static Item IRON_VALVE;
        public static Item GOLD_VALVE;
        public static Item LAPIS_VALVE;
        public static Item APATITE_VALVE;
        public static Item OBSIDIAN_VALVE;
        public static Item AMETHYST_VALVE;
        public static Item EMERALD_VALVE;
        public static Item BLAZING_VALVE;
        public static Item DIAMOND_VALVE;
        public static Item ECHO_VALVE;
        public static Item NETHERITE_VALVE;

        private ITEM() {}

        static void register() {
            WRENCH = INSTANCE.registerItem("wrench",
                props -> new WrenchItem(props.stacksTo(1)));

            // Tin Materials
            RAW_TIN = INSTANCE.registerItem("raw_tin", Item::new);
            TIN_INGOT = INSTANCE.registerItem("tin_ingot", Item::new);
            TIN_NUGGET = INSTANCE.registerItem("tin_nugget", Item::new);

            // Bronze Materials
            BRONZE_INGOT = INSTANCE.registerItem("bronze_ingot", Item::new);
            BRONZE_NUGGET = INSTANCE.registerItem("bronze_nugget", Item::new);
            COPPER_NUGGET = INSTANCE.registerItem("copper_nugget", Item::new);

            // Apatite
            APATITE = INSTANCE.registerItem("apatite", Item::new);

            // Components
            MACHINE_CORE = INSTANCE.registerItem("machine_core", Item::new);
            REDSTONE_RECEPTION_COIL = INSTANCE.registerItem("redstone_reception_coil", Item::new);
            RUBBER = INSTANCE.registerItem("rubber", Item::new);
            NATURAL_POLYMER = INSTANCE.registerItem("natural_polymer", Item::new);
            SYNTHETIC_POLYMER = INSTANCE.registerItem("synthetic_polymer", Item::new);

            // Gears
            WOODEN_GEAR = INSTANCE.registerItem("wooden_gear", Item::new);
            STONE_GEAR = INSTANCE.registerItem("stone_gear", Item::new);
            COPPER_GEAR = INSTANCE.registerItem("copper_gear", Item::new);
            IRON_GEAR = INSTANCE.registerItem("iron_gear", Item::new);
            GOLD_GEAR = INSTANCE.registerItem("gold_gear", Item::new);
            BRONZE_GEAR = INSTANCE.registerItem("bronze_gear", Item::new);
            DIAMOND_GEAR = INSTANCE.registerItem("diamond_gear", Item::new);
            NETHERITE_GEAR = INSTANCE.registerItem("netherite_gear", Item::new);

            // Powders and dusts
            APATITE_DUST = INSTANCE.registerItem("apatite_dust", Item::new);
            IRON_DUST = INSTANCE.registerItem("iron_dust", Item::new);
            COPPER_DUST = INSTANCE.registerItem("copper_dust", Item::new);
            TIN_DUST = INSTANCE.registerItem("tin_dust", Item::new);
            BRONZE_DUST = INSTANCE.registerItem("bronze_dust", Item::new);
            GOLD_DUST = INSTANCE.registerItem("gold_dust", Item::new);
            LAPIS_DUST = INSTANCE.registerItem("lapis_dust", Item::new);
            QUARTZ_DUST = INSTANCE.registerItem("quartz_dust", Item::new);
            COAL_DUST = INSTANCE.registerItem("coal_dust", Item::new);
            AMETHYST_DUST = INSTANCE.registerItem("amethyst_dust", Item::new);
            DIAMOND_DUST = INSTANCE.registerItem("diamond_dust", Item::new);
            EMERALD_DUST = INSTANCE.registerItem("emerald_dust", Item::new);
            NETHERITE_DUST = INSTANCE.registerItem("netherite_dust", Item::new);
            OBSIDIAN_DUST = INSTANCE.registerItem("obsidian_dust", Item::new);
            ENDER_DUST = INSTANCE.registerItem("ender_dust", Item::new);
            ECHO_DUST = INSTANCE.registerItem("echo_dust", Item::new);
            PRISMARINE_DUST = INSTANCE.registerItem("prismarine_dust", Item::new);
            SULFUR_DUST = INSTANCE.registerItem("sulfur_dust", Item::new);
            QUICKSILVER = INSTANCE.registerItem("quicksilver", Item::new);
            NITER = INSTANCE.registerItem("niter", Item::new);
            SILICON_MIX = INSTANCE.registerItem("silicon_mix", Item::new);
            SILICON_WAFER = INSTANCE.registerItem("silicon_wafer", Item::new);
            FLOUR = INSTANCE.registerItem("flour", Item::new);
            SAWDUST = INSTANCE.registerItem("sawdust", Item::new);
            PEAT = INSTANCE.registerItem("peat", Item::new);
            PULPED_BIOMASS = INSTANCE.registerItem("pulped_biomass", Item::new);
            SLAG = INSTANCE.registerItem("slag", Item::new);
            RICH_SLAG = INSTANCE.registerItem("rich_slag", Item::new);
            BITUMEN = INSTANCE.registerItem("bitumen", Item::new);
            TAR = INSTANCE.registerItem("tar", Item::new);

            // Chips
            REDSTONE_CHIPSET = INSTANCE.registerItem("redstone_chipset", Item::new);
            ECHO_CHIPSET = INSTANCE.registerItem("echo_chipset", Item::new);
            COPPER_CHIPSET = INSTANCE.registerItem("copper_chipset", Item::new);
            IRON_CHIPSET = INSTANCE.registerItem("iron_chipset", Item::new);
            GOLD_CHIPSET = INSTANCE.registerItem("gold_chipset", Item::new);
            DIAMOND_CHIPSET = INSTANCE.registerItem("diamond_chipset", Item::new);
            NETHERITE_CHIPSET = INSTANCE.registerItem("netherite_chipset", Item::new);

            // Valves
            TIN_VALVE = INSTANCE.registerItem("tin_valve", Item::new);
            COPPER_VALVE = INSTANCE.registerItem("copper_valve", Item::new);
            RUBBER_VALVE = INSTANCE.registerItem("rubber_valve", Item::new);
            BRONZE_VALVE = INSTANCE.registerItem("bronze_valve", Item::new);
            IRON_VALVE = INSTANCE.registerItem("iron_valve", Item::new);
            GOLD_VALVE = INSTANCE.registerItem("gold_valve", Item::new);
            LAPIS_VALVE = INSTANCE.registerItem("lapis_valve", Item::new);
            APATITE_VALVE = INSTANCE.registerItem("apatite_valve", Item::new);
            OBSIDIAN_VALVE = INSTANCE.registerItem("obsidian_valve", Item::new);
            AMETHYST_VALVE = INSTANCE.registerItem("amethyst_valve", Item::new);
            EMERALD_VALVE = INSTANCE.registerItem("emerald_valve", Item::new);
            BLAZING_VALVE = INSTANCE.registerItem("blazing_valve", Item::new);
            DIAMOND_VALVE = INSTANCE.registerItem("diamond_valve", Item::new);
            ECHO_VALVE = INSTANCE.registerItem("echo_valve", Item::new);
            NETHERITE_VALVE = INSTANCE.registerItem("netherite_valve", Item::new);
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        public static BlockEntityType<MarkerBlockEntity> MARKER_BLOCK_ENTITY;
        public static BlockEntityType<RedstoneEngineBlockEntity> REDSTONE_ENGINE_BLOCK_ENTITY;

        static void register() {
            MARKER_BLOCK_ENTITY = INSTANCE.registerBlockEntity("marker", MarkerBlockEntity::new, BLOCK.MARKER);
            REDSTONE_ENGINE_BLOCK_ENTITY = INSTANCE.registerBlockEntity(
                "redstone_engine", RedstoneEngineBlockEntity::new, BLOCK.REDSTONE_ENGINE);
        }
    }

    public static final class CREATIVE {
        // Tab ids carry an ordinal prefix: Fabric sorts creative tabs alphabetically by id, so the
        // prefix (not registration order) controls left-to-right placement. Order: resources, pipes,
        // power, automation.
        public static final LogisticsCreativeTab TAB = LogisticsCreativeTab.create(
            LogisticsMod.modId("1_resources"),
            Component.translatable("itemGroup.logistics.1_resources"),
            () -> new ItemStack(ITEM.WRENCH)
        );

        private CREATIVE() {}

        static void register() {
            // Tools
            TAB.add(ITEM.WRENCH);
            TAB.add(BLOCK.REDSTONE_ENGINE);

            // --- Misc & raw materials (kept contiguous) ---
            // Ores and storage blocks
            TAB.add(BLOCK.BOG_EARTH);
            TAB.add(BLOCK.TIN_ORE);
            TAB.add(BLOCK.DEEPSLATE_TIN_ORE);
            TAB.add(BLOCK.APATITE_ORE);
            TAB.add(BLOCK.OIL_SAND);
            TAB.add(BLOCK.OIL_RED_SAND);
            TAB.add(BLOCK.OIL_SHALE);
            TAB.add(BLOCK.RAW_TIN_BLOCK);

            // Metals
            TAB.add(ITEM.RAW_TIN);
            TAB.add(ITEM.APATITE);
            TAB.add(ITEM.TIN_INGOT);
            TAB.add(ITEM.BRONZE_INGOT);
            TAB.add(ITEM.TIN_NUGGET);
            TAB.add(ITEM.COPPER_NUGGET);
            TAB.add(ITEM.BRONZE_NUGGET);
            TAB.add(BLOCK.APATITE_BLOCK);
            TAB.add(BLOCK.TIN_BLOCK);
            TAB.add(BLOCK.BRONZE_BLOCK);

            // Components
            TAB.add(ITEM.MACHINE_CORE);
            TAB.add(ITEM.REDSTONE_RECEPTION_COIL);
            TAB.add(ITEM.SILICON_WAFER);
            TAB.add(BLOCK.MARKER);
            TAB.add(BLOCK.QUARTZ_CRYSTAL);

            // --- Component ladder: gears -> valves -> chipsets ---
            TAB.add(ITEM.WOODEN_GEAR);
            TAB.add(ITEM.STONE_GEAR);
            TAB.add(ITEM.COPPER_GEAR);
            TAB.add(ITEM.IRON_GEAR);
            TAB.add(ITEM.BRONZE_GEAR);
            TAB.add(ITEM.GOLD_GEAR);
            TAB.add(ITEM.DIAMOND_GEAR);
            TAB.add(ITEM.NETHERITE_GEAR);

            TAB.add(ITEM.TIN_VALVE);
            TAB.add(ITEM.COPPER_VALVE);
            TAB.add(ITEM.RUBBER_VALVE);
            TAB.add(ITEM.BRONZE_VALVE);
            TAB.add(ITEM.IRON_VALVE);
            TAB.add(ITEM.GOLD_VALVE);
            TAB.add(ITEM.LAPIS_VALVE);
            TAB.add(ITEM.APATITE_VALVE);
            TAB.add(ITEM.OBSIDIAN_VALVE);
            TAB.add(ITEM.AMETHYST_VALVE);
            TAB.add(ITEM.EMERALD_VALVE);
            TAB.add(ITEM.BLAZING_VALVE);
            TAB.add(ITEM.DIAMOND_VALVE);
            TAB.add(ITEM.ECHO_VALVE);
            TAB.add(ITEM.NETHERITE_VALVE);

            TAB.add(ITEM.REDSTONE_CHIPSET);
            TAB.add(ITEM.ECHO_CHIPSET);
            TAB.add(ITEM.COPPER_CHIPSET);
            TAB.add(ITEM.IRON_CHIPSET);
            TAB.add(ITEM.GOLD_CHIPSET);
            TAB.add(ITEM.DIAMOND_CHIPSET);
            TAB.add(ITEM.NETHERITE_CHIPSET);

            // Dusts and powders (macerator outputs)
            TAB.add(ITEM.APATITE_DUST);
            TAB.add(ITEM.IRON_DUST);
            TAB.add(ITEM.COPPER_DUST);
            TAB.add(ITEM.TIN_DUST);
            TAB.add(ITEM.BRONZE_DUST);
            TAB.add(ITEM.GOLD_DUST);
            TAB.add(ITEM.LAPIS_DUST);
            TAB.add(ITEM.QUARTZ_DUST);
            TAB.add(ITEM.COAL_DUST);
            TAB.add(ITEM.AMETHYST_DUST);
            TAB.add(ITEM.DIAMOND_DUST);
            TAB.add(ITEM.EMERALD_DUST);
            TAB.add(ITEM.NETHERITE_DUST);
            TAB.add(ITEM.OBSIDIAN_DUST);
            TAB.add(ITEM.ENDER_DUST);
            TAB.add(ITEM.ECHO_DUST);
            TAB.add(ITEM.PRISMARINE_DUST);
            TAB.add(ITEM.SULFUR_DUST);
            TAB.add(ITEM.SAWDUST);
            TAB.add(ITEM.PULPED_BIOMASS);
            TAB.add(ITEM.FLOUR);

            // Byproducts and intermediates
            TAB.add(ITEM.PEAT);
            TAB.add(ITEM.SILICON_MIX);
            TAB.add(ITEM.SLAG);
            TAB.add(ITEM.RICH_SLAG);
            TAB.add(ITEM.NITER);
            TAB.add(ITEM.QUICKSILVER);
            TAB.add(ITEM.BITUMEN);
            TAB.add(ITEM.TAR);
            TAB.add(ITEM.NATURAL_POLYMER);
            TAB.add(ITEM.SYNTHETIC_POLYMER);
            TAB.add(ITEM.RUBBER);

            // Fluid buckets
            BUCKET.all().values().forEach(TAB::add);
            // Placeable fluids' buckets register per loader; add them lazily so they resolve at populate time.
            TAB.add(out -> {
                for (FluidDef def : CUSTOM_FLUIDS) {
                    if (def.placeable()) {
                        Item bucket = BUCKET.forFluid(def.name());
                        if (bucket != null) {
                            out.accept(bucket);
                        }
                    }
                }
            });

            CreativeTabRegistrar.INSTANCE.registerTab(TAB);
        }
    }

    public static final class ALIAS {
        private ALIAS() {}

        static void register() {
            // Wood Pulp renamed to Sawdust; old recipes/worlds resolve to the new item.
            INSTANCE.registerItemAlias("core/wood_pulp", ITEM.SAWDUST);

            // Marker moved from the automation domain to core; bridge the previous-major (0.6.x) IDs.
            INSTANCE.registerBlockAlias("automation/marker", BLOCK.MARKER);
            INSTANCE.registerBlockEntityAlias("automation/marker", ENTITY.MARKER_BLOCK_ENTITY);
            INSTANCE.registerItemAlias("automation/marker", BLOCK.MARKER.asItem());

            // Chips renamed to "chipset"; bridge the previously-released chip IDs.
            INSTANCE.registerItemAlias("core/redstone_chip", ITEM.REDSTONE_CHIPSET);
            INSTANCE.registerItemAlias("core/echo_chip", ITEM.ECHO_CHIPSET);

            // Redstone Engine moved from the power domain to core.
            INSTANCE.registerBlockAlias("power/redstone_engine", BLOCK.REDSTONE_ENGINE);
            INSTANCE.registerBlockEntityAlias("power/redstone_engine", ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY);
            INSTANCE.registerItemAlias("power/redstone_engine", BLOCK.REDSTONE_ENGINE.asItem());

            // Rubber and polymers moved from the power domain to core.
            INSTANCE.registerItemAlias("power/rubber", ITEM.RUBBER);
            INSTANCE.registerItemAlias("power/natural_polymer", ITEM.NATURAL_POLYMER);
            INSTANCE.registerItemAlias("power/synthetic_polymer", ITEM.SYNTHETIC_POLYMER);

            // Older power-domain renames: Rubber Mix -> Natural Polymer, Rubber Chunk -> Rubber.
            INSTANCE.registerItemAlias("power/rubber_mix", ITEM.NATURAL_POLYMER);
            INSTANCE.registerItemAlias("power/rubber_chunk", ITEM.RUBBER);
        }
    }
}

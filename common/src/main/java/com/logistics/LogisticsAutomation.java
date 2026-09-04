package com.logistics;

import com.logistics.automation.alloysmelter.AlloySmelterBlock;
import com.logistics.automation.alloysmelter.AlloySmelterBlockEntity;
import com.logistics.automation.alloysmelter.AlloySmelterRecipe;
import com.logistics.automation.alloysmelter.AlloySmelterRecipeSerializer;
import com.logistics.automation.alloysmelter.AlloySmelterScreenHandler;
import com.logistics.automation.crucible.CrucibleBlock;
import com.logistics.automation.crucible.CrucibleBlockEntity;
import com.logistics.automation.crucible.CrucibleRecipe;
import com.logistics.automation.crucible.CrucibleRecipeSerializer;
import com.logistics.automation.crucible.CrucibleScreenHandler;
import com.logistics.automation.fabricator.FabricatorRecipe;
import com.logistics.automation.fabricator.FabricatorRecipeSerializer;
import com.logistics.automation.fabricator.SequentialFabricatorBlock;
import com.logistics.automation.fabricator.SequentialFabricatorBlockEntity;
import com.logistics.automation.fabricator.SequentialFabricatorScreenHandler;
import com.logistics.automation.kiln.KilnBlock;
import com.logistics.automation.kiln.KilnBlockEntity;
import com.logistics.automation.kiln.KilnScreenHandler;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryFrameBlock;
import com.logistics.automation.pump.FluidPumpBlock;
import com.logistics.automation.pump.FluidPumpBlockEntity;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.automation.macerator.MaceratorBlock;
import com.logistics.automation.macerator.MaceratorBlockEntity;
import com.logistics.automation.macerator.MaceratorRecipeSerializer;
import com.logistics.automation.macerator.MaceratorRecipeWrapper;
import com.logistics.automation.macerator.MaceratorScreenHandler;
import com.logistics.automation.refinery.RefineryBlock;
import com.logistics.automation.refinery.RefineryBlockEntity;
import com.logistics.automation.refinery.RefineryRecipe;
import com.logistics.automation.refinery.RefineryRecipeSerializer;
import com.logistics.automation.refinery.RefineryScreenHandler;
import com.logistics.automation.sawmill.SawmillBlock;
import com.logistics.automation.sawmill.SawmillBlockEntity;
import com.logistics.automation.sawmill.SawmillRecipe;
import com.logistics.automation.sawmill.SawmillRecipeSerializer;
import com.logistics.automation.sawmill.SawmillScreenHandler;
import com.logistics.automation.transposer.TransposerBlock;
import com.logistics.automation.transposer.TransposerBlockEntity;
import com.logistics.automation.transposer.TransposerRecipe;
import com.logistics.automation.transposer.TransposerRecipeSerializer;
import com.logistics.automation.transposer.TransposerScreenHandler;
import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigEntries;
import com.indemnity83.configory.ConfigKey;
import com.logistics.core.LogisticsConfigMigrator;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.LogisticsCreativeTab;
import com.logistics.core.lib.resource.ResourceId;
import java.util.Comparator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class LogisticsAutomation extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsAutomation INSTANCE = new LogisticsAutomation();

    @Override
    protected String domain() {
        return "automation";
    }

    public static ResourceId resource(String name) {
        return INSTANCE.domainResource(name);
    }

    public static ResourceId model(String name) {
        return INSTANCE.domainModelResource(name);
    }

    @Override
    public int order() {
        return 20;
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
        MENU.register();
        RECIPE.register();
        CREATIVE.register();
        ALIAS.register();
        TICKET_TYPE.register();
    }

    /**
     * Machine tuning — one file per machine under {@code config/logistics/machines/}. Every recipe machine
     * shares the same template: {@code energy_capacity}, {@code max_energy_input}, {@code energy_per_tick}.
     * Defaults match the historical hardcoded values, so this is a pure "make it configurable" surface.
     */
    public static final class CONFIG extends ConfigEntries {
        private static final Config macerator = configFor(LogisticsConfigHost.MOD_ID, "machines.macerator");
        private static final Config kiln = configFor(LogisticsConfigHost.MOD_ID, "machines.kiln");
        private static final Config sawmill = configFor(LogisticsConfigHost.MOD_ID, "machines.sawmill");
        private static final Config crucible = configFor(LogisticsConfigHost.MOD_ID, "machines.crucible");
        private static final Config alloySmelter = configFor(LogisticsConfigHost.MOD_ID, "machines.alloy_smelter");
        private static final Config quarry = configFor(LogisticsConfigHost.MOD_ID, "machines.quarry");
        private static final Config refinery = configFor(LogisticsConfigHost.MOD_ID, "machines.refinery");
        private static final Config fabricator = configFor(LogisticsConfigHost.MOD_ID, "machines.fabricator");
        private static final Config transposer = configFor(LogisticsConfigHost.MOD_ID, "machines.transposer");

        private CONFIG() {}

        // Macerator
        public static final ConfigKey<Long> MACERATOR_ENERGY_CAPACITY =
                macerator.defineLong("energy_capacity", 10_000L).min(0L).describe("Internal RF buffer capacity").register();
        public static final ConfigKey<Long> MACERATOR_MAX_ENERGY_INPUT =
                macerator.defineLong("max_energy_input", 128L).min(0L).describe("Max RF/t accepted from the network").register();
        public static final ConfigKey<Long> MACERATOR_ENERGY_PER_TICK =
                macerator.defineLong("energy_per_tick", 10L).min(1L).describe("RF/t spent processing (higher = faster)").register();

        // Kiln
        public static final ConfigKey<Long> KILN_ENERGY_CAPACITY =
                kiln.defineLong("energy_capacity", 10_000L).min(0L).describe("Internal RF buffer capacity").register();
        public static final ConfigKey<Long> KILN_MAX_ENERGY_INPUT =
                kiln.defineLong("max_energy_input", 128L).min(0L).describe("Max RF/t accepted from the network").register();
        public static final ConfigKey<Long> KILN_ENERGY_PER_TICK =
                kiln.defineLong("energy_per_tick", 20L).min(1L).describe("RF/t spent processing (higher = faster)").register();
        public static final ConfigKey<Long> KILN_RF_PER_COOK_TICK =
                kiln.defineLong("rf_per_cook_tick", 10L).min(1L).describe("RF per vanilla smelting cook-tick (recipe energy = cook time x this)").register();

        // Sawmill
        public static final ConfigKey<Long> SAWMILL_ENERGY_CAPACITY =
                sawmill.defineLong("energy_capacity", 10_000L).min(0L).describe("Internal RF buffer capacity").register();
        public static final ConfigKey<Long> SAWMILL_MAX_ENERGY_INPUT =
                sawmill.defineLong("max_energy_input", 128L).min(0L).describe("Max RF/t accepted from the network").register();
        public static final ConfigKey<Long> SAWMILL_ENERGY_PER_TICK =
                sawmill.defineLong("energy_per_tick", 20L).min(1L).describe("RF/t spent processing (higher = faster)").register();

        // Crucible
        public static final ConfigKey<Long> CRUCIBLE_ENERGY_CAPACITY =
                crucible.defineLong("energy_capacity", 40_000L).min(0L).describe("Internal RF buffer capacity").register();
        public static final ConfigKey<Long> CRUCIBLE_MAX_ENERGY_INPUT =
                crucible.defineLong("max_energy_input", 128L).min(0L).describe("Max RF/t accepted from the network").register();
        public static final ConfigKey<Long> CRUCIBLE_ENERGY_PER_TICK =
                crucible.defineLong("energy_per_tick", 40L).min(1L).describe("RF/t spent processing (higher = faster)").register();
        public static final ConfigKey<Long> CRUCIBLE_TANK_CAPACITY_MB =
                crucible.defineLong("tank_capacity_mb", 10_000L).min(1L).describe("Molten-metal tank capacity (mB)").register();

        // Alloy Smelter
        public static final ConfigKey<Long> ALLOY_SMELTER_ENERGY_CAPACITY =
                alloySmelter.defineLong("energy_capacity", 10_000L).min(0L).describe("Internal RF buffer capacity").register();
        public static final ConfigKey<Long> ALLOY_SMELTER_MAX_ENERGY_INPUT =
                alloySmelter.defineLong("max_energy_input", 128L).min(0L).describe("Max RF/t accepted from the network").register();
        public static final ConfigKey<Long> ALLOY_SMELTER_ENERGY_PER_TICK =
                alloySmelter.defineLong("energy_per_tick", 20L).min(1L).describe("RF/t spent processing (higher = faster)").register();

        // Refinery
        public static final ConfigKey<Long> REFINERY_ENERGY_CAPACITY =
                refinery.defineLong("energy_capacity", 20_000L).min(1L).describe("Internal RF buffer capacity").register();
        public static final ConfigKey<Long> REFINERY_MAX_ENERGY_INPUT =
                refinery.defineLong("max_energy_input", 128L).min(0L).describe("Max RF/t accepted from the network").register();
        public static final ConfigKey<Long> REFINERY_ENERGY_PER_TICK =
                refinery.defineLong("energy_per_tick", 20L).min(1L).describe("RF/t spent processing (higher = faster)").register();
        public static final ConfigKey<Long> REFINERY_INPUT_TANK_MB =
                refinery.defineLong("input_tank_mb", 4_000L).min(1L).describe("Input fluid tank capacity (mB)").register();
        public static final ConfigKey<Long> REFINERY_OUTPUT_TANK_MB =
                refinery.defineLong("output_tank_mb", 10_000L).min(1L).describe("Output fluid tank capacity (mB)").register();

        // Sequential Fabricator
        public static final ConfigKey<Long> FABRICATOR_ENERGY_CAPACITY =
                fabricator.defineLong("energy_capacity", 100_000L).min(1L).describe("Internal RF buffer capacity").register();
        public static final ConfigKey<Long> FABRICATOR_MAX_ENERGY_INPUT =
                fabricator.defineLong("max_energy_input", 128L).min(0L).describe("Max RF/t accepted from the network").register();
        public static final ConfigKey<Long> FABRICATOR_ENERGY_PER_TICK =
                fabricator.defineLong("energy_per_tick", 80L).min(1L).describe("RF/t spent processing (higher = faster)").register();

        // Transposer
        public static final ConfigKey<Long> TRANSPOSER_ENERGY_CAPACITY =
                transposer.defineLong("energy_capacity", 20_000L).min(1L).describe("Internal RF buffer capacity").register();
        public static final ConfigKey<Long> TRANSPOSER_MAX_ENERGY_INPUT =
                transposer.defineLong("max_energy_input", 128L).min(0L).describe("Max RF/t accepted from the network").register();
        public static final ConfigKey<Long> TRANSPOSER_ENERGY_PER_TICK =
                transposer.defineLong("energy_per_tick", 20L).min(1L).describe("RF/t spent processing (higher = faster)").register();
        public static final ConfigKey<Long> TRANSPOSER_TANK_CAPACITY_MB =
                transposer.defineLong("tank_capacity_mb", 16_000L).min(1_000L).describe("Internal fluid tank capacity (mB)").register();

        // Laser Quarry
        public static final ConfigKey<Integer> QUARRY_AREA = quarry.defineInt("area", 16)
                .min(3)
                .describe("Quarry mining area side length (NxN blocks)")
                .register();

        public static final ConfigKey<Long> QUARRY_ENERGY_PER_BLOCK = quarry.defineLong("energy_per_block", 60L)
                .min(0L)
                .describe("RF cost per block mined")
                .register();

        public static final ConfigKey<Double> QUARRY_ENERGY_MULTIPLIER =
                quarry.defineDouble("energy_multiplier", 1.0)
                        .finite()
                        .min(0.0)
                        .describe("Global energy cost multiplier")
                        .register();

        public static final ConfigKey<Float> QUARRY_ARM_SPEED = quarry.defineFloat("arm_speed", 0.1f)
                .finite()
                .min(0.0f)
                .describe("Arm movement speed (blocks/tick)")
                .register();

        public static final ConfigKey<Float> QUARRY_ARM_SPEED_SCALING =
                quarry.defineFloat("arm_speed_scaling", 2000f)
                        .finite()
                        .greaterThan(0.0f)
                        .describe("Energy-to-speed scaling constant")
                        .register();

        public static final ConfigKey<Long> QUARRY_ARM_ENERGY = quarry.defineLong("arm_energy", 20L)
                .min(0L)
                .describe("RF/tick cost for arm movement")
                .register();

        public static final ConfigKey<Float> QUARRY_RAIN_PENALTY = quarry.defineFloat("rain_penalty", 0.7f)
                .finite()
                .range(0.0f, 1.0f)
                .describe("Speed multiplier when raining (0.0-1.0)")
                .register();

        public static final ConfigKey<Integer> QUARRY_SCAN_RATE = quarry.defineInt("scan_rate", 256)
                .min(1)
                .max(65536)
                .describe("Max blocks scanned per tick when searching")
                .register();

        public static final ConfigKey<Boolean> QUARRY_LOAD_CHUNKS = quarry.defineBoolean("load_chunks", false)
                .describe("Keep the quarry and its work area chunk-loaded while running")
                .register();

        static void register() {
            LogisticsConfigMigrator.mapLegacy("quarry", "area", QUARRY_AREA);
            LogisticsConfigMigrator.mapLegacy("quarry", "energyPerBlock", QUARRY_ENERGY_PER_BLOCK);
            LogisticsConfigMigrator.mapLegacy("quarry", "energyMultiplier", QUARRY_ENERGY_MULTIPLIER);
            LogisticsConfigMigrator.mapLegacy("quarry", "armSpeed", QUARRY_ARM_SPEED);
            LogisticsConfigMigrator.mapLegacy("quarry", "armSpeedScaling", QUARRY_ARM_SPEED_SCALING);
            LogisticsConfigMigrator.mapLegacy("quarry", "armEnergy", QUARRY_ARM_ENERGY);
            LogisticsConfigMigrator.mapLegacy("quarry", "rainPenalty", QUARRY_RAIN_PENALTY);
            LogisticsConfigMigrator.mapLegacy("quarry", "scanRate", QUARRY_SCAN_RATE);
            LogisticsConfigMigrator.mapLegacy("quarry", "loadChunks", QUARRY_LOAD_CHUNKS);
        }
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static Block LASER_QUARRY;
        public static Block LASER_QUARRY_FRAME;
        public static Block KILN;
        public static Block MACERATOR;
        public static Block SAWMILL;
        public static Block ALLOY_SMELTER;
        public static Block CRUCIBLE;
        public static Block REFINERY;
        public static Block SEQUENTIAL_FABRICATOR;
        public static Block FLUID_PUMP;
        public static Block TRANSPOSER;

        static void register() {
            LASER_QUARRY = INSTANCE.registerBlockWithItem("laser_quarry",
                props -> new LaserQuarryBlock(props.strength(5.0f).sound(SoundType.STONE)));
            LASER_QUARRY_FRAME = INSTANCE.registerBlock("laser_quarry_frame",
                props -> new LaserQuarryFrameBlock(props.strength(2.0f).noOcclusion().noLootTable()));
            KILN = INSTANCE.registerBlockWithItem("kiln",
                props -> new KilnBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(KilnBlock.LIT) ? 13 : 0)));
            MACERATOR = INSTANCE.registerBlockWithItem("macerator",
                props -> new MaceratorBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MaceratorBlock.LIT) ? 13 : 0)));
            SAWMILL = INSTANCE.registerBlockWithItem("sawmill",
                props -> new SawmillBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(SawmillBlock.LIT) ? 13 : 0)));
            ALLOY_SMELTER = INSTANCE.registerBlockWithItem("alloy_smelter",
                props -> new AlloySmelterBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(AlloySmelterBlock.LIT) ? 13 : 0)));
            CRUCIBLE = INSTANCE.registerBlockWithItem("crucible",
                props -> new CrucibleBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(CrucibleBlock.LIT) ? 13 : 0)));
            REFINERY = INSTANCE.registerBlockWithItem("refinery",
                props -> new RefineryBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(RefineryBlock.LIT) ? 13 : 0)));
            SEQUENTIAL_FABRICATOR = INSTANCE.registerBlockWithItem("sequential_fabricator",
                props -> new SequentialFabricatorBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(SequentialFabricatorBlock.LIT) ? 13 : 0)));
            FLUID_PUMP = INSTANCE.registerBlockWithItem("fluid_pump",
                props -> new FluidPumpBlock(props.mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
            TRANSPOSER = INSTANCE.registerBlockWithItem("transposer",
                props -> new TransposerBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        public static BlockEntityType<LaserQuarryBlockEntity> LASER_QUARRY_BLOCK_ENTITY;
        public static BlockEntityType<KilnBlockEntity> KILN_BLOCK_ENTITY;
        public static BlockEntityType<MaceratorBlockEntity> MACERATOR_BLOCK_ENTITY;
        public static BlockEntityType<SawmillBlockEntity> SAWMILL_BLOCK_ENTITY;
        public static BlockEntityType<AlloySmelterBlockEntity> ALLOY_SMELTER_BLOCK_ENTITY;
        public static BlockEntityType<CrucibleBlockEntity> CRUCIBLE_BLOCK_ENTITY;
        public static BlockEntityType<RefineryBlockEntity> REFINERY_BLOCK_ENTITY;
        public static BlockEntityType<SequentialFabricatorBlockEntity> SEQUENTIAL_FABRICATOR_BLOCK_ENTITY;
        public static BlockEntityType<FluidPumpBlockEntity> FLUID_PUMP_BLOCK_ENTITY;
        public static BlockEntityType<TransposerBlockEntity> TRANSPOSER_BLOCK_ENTITY;

        static void register() {
            LASER_QUARRY_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("laser_quarry", LaserQuarryBlockEntity::new, BLOCK.LASER_QUARRY);
            KILN_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("kiln", KilnBlockEntity::new, BLOCK.KILN);
            MACERATOR_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("macerator", MaceratorBlockEntity::new, BLOCK.MACERATOR);
            SAWMILL_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("sawmill", SawmillBlockEntity::new, BLOCK.SAWMILL);
            ALLOY_SMELTER_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("alloy_smelter", AlloySmelterBlockEntity::new, BLOCK.ALLOY_SMELTER);
            CRUCIBLE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("crucible", CrucibleBlockEntity::new, BLOCK.CRUCIBLE);
            REFINERY_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("refinery", RefineryBlockEntity::new, BLOCK.REFINERY);
            SEQUENTIAL_FABRICATOR_BLOCK_ENTITY = INSTANCE.registerBlockEntity(
                "sequential_fabricator", SequentialFabricatorBlockEntity::new, BLOCK.SEQUENTIAL_FABRICATOR);
            FLUID_PUMP_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("fluid_pump", FluidPumpBlockEntity::new, BLOCK.FLUID_PUMP);
            TRANSPOSER_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("transposer", TransposerBlockEntity::new, BLOCK.TRANSPOSER);
        }
    }

    public static final class MENU {
        private MENU() {}

        public static MenuType<KilnScreenHandler> KILN;
        public static MenuType<MaceratorScreenHandler> MACERATOR;
        public static MenuType<SawmillScreenHandler> SAWMILL;
        public static MenuType<AlloySmelterScreenHandler> ALLOY_SMELTER;
        public static MenuType<CrucibleScreenHandler> CRUCIBLE;
        public static MenuType<RefineryScreenHandler> REFINERY;
        public static MenuType<SequentialFabricatorScreenHandler> SEQUENTIAL_FABRICATOR;
        public static MenuType<TransposerScreenHandler> TRANSPOSER;

        static void register() {
            KILN = INSTANCE.registerMenuType("kiln", KilnScreenHandler::new);
            MACERATOR = INSTANCE.registerMenuType("macerator", MaceratorScreenHandler::new);
            SAWMILL = INSTANCE.registerMenuType("sawmill", SawmillScreenHandler::new);
            ALLOY_SMELTER = INSTANCE.registerMenuType("alloy_smelter", AlloySmelterScreenHandler::new);
            CRUCIBLE = INSTANCE.registerMenuType("crucible", CrucibleScreenHandler::new);
            REFINERY = INSTANCE.registerMenuType("refinery", RefineryScreenHandler::new);
            SEQUENTIAL_FABRICATOR =
                INSTANCE.registerMenuType("sequential_fabricator", SequentialFabricatorScreenHandler::new);
            TRANSPOSER = INSTANCE.registerMenuType("transposer", TransposerScreenHandler::new);
        }
    }

    public static final class RECIPE {
        private RECIPE() {}

        public static RecipeType<MaceratorRecipeWrapper> MACERATOR_RECIPE_TYPE;
        public static RecipeSerializer<MaceratorRecipeWrapper> MACERATOR_RECIPE_SERIALIZER;
        public static RecipeType<SawmillRecipe> SAWMILL_RECIPE_TYPE;
        public static RecipeSerializer<SawmillRecipe> SAWMILL_RECIPE_SERIALIZER;
        public static RecipeType<AlloySmelterRecipe> ALLOY_SMELTER_RECIPE_TYPE;
        public static RecipeSerializer<AlloySmelterRecipe> ALLOY_SMELTER_RECIPE_SERIALIZER;
        public static RecipeType<CrucibleRecipe> CRUCIBLE_RECIPE_TYPE;
        public static RecipeSerializer<CrucibleRecipe> CRUCIBLE_RECIPE_SERIALIZER;
        public static RecipeType<RefineryRecipe> REFINERY_RECIPE_TYPE;
        public static RecipeSerializer<RefineryRecipe> REFINERY_RECIPE_SERIALIZER;
        public static RecipeType<FabricatorRecipe> FABRICATOR_RECIPE_TYPE;
        public static RecipeSerializer<FabricatorRecipe> FABRICATOR_RECIPE_SERIALIZER;
        public static RecipeType<TransposerRecipe> TRANSPOSER_RECIPE_TYPE;
        public static RecipeSerializer<TransposerRecipe> TRANSPOSER_RECIPE_SERIALIZER;

        static void register() {
            MACERATOR_RECIPE_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_TYPE,
                LogisticsMod.modId("macerator").toIdentifier(),
                new RecipeType<MaceratorRecipeWrapper>() {
                    @Override
                    public String toString() {
                        return "logistics:macerator";
                    }
                }
            );
            MACERATOR_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                LogisticsMod.modId("macerator").toIdentifier(),
                MaceratorRecipeSerializer.INSTANCE
            );
            SAWMILL_RECIPE_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_TYPE,
                LogisticsMod.modId("sawmill").toIdentifier(),
                new RecipeType<SawmillRecipe>() {
                    @Override
                    public String toString() {
                        return "logistics:sawmill";
                    }
                }
            );
            SAWMILL_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                LogisticsMod.modId("sawmill").toIdentifier(),
                SawmillRecipeSerializer.INSTANCE
            );
            ALLOY_SMELTER_RECIPE_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_TYPE,
                LogisticsMod.modId("alloy_smelter").toIdentifier(),
                new RecipeType<AlloySmelterRecipe>() {
                    @Override
                    public String toString() {
                        return "logistics:alloy_smelter";
                    }
                }
            );
            ALLOY_SMELTER_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                LogisticsMod.modId("alloy_smelter").toIdentifier(),
                AlloySmelterRecipeSerializer.INSTANCE
            );
            CRUCIBLE_RECIPE_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_TYPE,
                LogisticsMod.modId("crucible").toIdentifier(),
                new RecipeType<CrucibleRecipe>() {
                    @Override
                    public String toString() {
                        return "logistics:crucible";
                    }
                }
            );
            CRUCIBLE_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                LogisticsMod.modId("crucible").toIdentifier(),
                CrucibleRecipeSerializer.INSTANCE
            );
            REFINERY_RECIPE_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_TYPE,
                LogisticsMod.modId("refinery").toIdentifier(),
                new RecipeType<RefineryRecipe>() {
                    @Override
                    public String toString() {
                        return "logistics:refinery";
                    }
                }
            );
            REFINERY_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                LogisticsMod.modId("refinery").toIdentifier(),
                RefineryRecipeSerializer.INSTANCE
            );
            FABRICATOR_RECIPE_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_TYPE,
                LogisticsMod.modId("fabricator").toIdentifier(),
                new RecipeType<FabricatorRecipe>() {
                    @Override
                    public String toString() {
                        return "logistics:fabricator";
                    }
                }
            );
            FABRICATOR_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                LogisticsMod.modId("fabricator").toIdentifier(),
                FabricatorRecipeSerializer.INSTANCE
            );
            TRANSPOSER_RECIPE_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_TYPE,
                LogisticsMod.modId("transposer").toIdentifier(),
                new RecipeType<TransposerRecipe>() {
                    @Override
                    public String toString() {
                        return "logistics:transposer";
                    }
                }
            );
            TRANSPOSER_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                LogisticsMod.modId("transposer").toIdentifier(),
                TransposerRecipeSerializer.INSTANCE
            );
        }
    }

    public static final class CREATIVE {
        public static final LogisticsCreativeTab TAB = LogisticsCreativeTab.create(
            LogisticsMod.modId("4_automation"),
            Component.translatable("itemGroup.logistics.4_automation"),
            () -> new ItemStack(BLOCK.KILN)
        );

        private CREATIVE() {}

        static void register() {
            TAB.add(BLOCK.LASER_QUARRY);
            TAB.add(BLOCK.FLUID_PUMP);
            TAB.add(BLOCK.KILN);
            TAB.add(BLOCK.MACERATOR);
            TAB.add(BLOCK.SAWMILL);
            TAB.add(BLOCK.ALLOY_SMELTER);
            TAB.add(BLOCK.CRUCIBLE);
            TAB.add(BLOCK.REFINERY);
            TAB.add(BLOCK.SEQUENTIAL_FABRICATOR);
            TAB.add(BLOCK.TRANSPOSER);
            CreativeTabRegistrar.INSTANCE.registerTab(TAB);
        }
    }

    public static final class ALIAS {
        private ALIAS() {}

        static void register() {
            // Macerator moved from the core domain to automation; bridge the previous-major (0.6.x) IDs.
            INSTANCE.registerBlockAlias("core/macerator", BLOCK.MACERATOR);
            INSTANCE.registerBlockEntityAlias("core/macerator", ENTITY.MACERATOR_BLOCK_ENTITY);
            INSTANCE.registerItemAlias("core/macerator", BLOCK.MACERATOR.asItem());

            // Fluid Pump moved from the fluid unit into the automation domain.
            INSTANCE.registerBlockAlias("fluid/fluid_pump", BLOCK.FLUID_PUMP);
            INSTANCE.registerBlockEntityAlias("fluid/fluid_pump", ENTITY.FLUID_PUMP_BLOCK_ENTITY);
            INSTANCE.registerItemAlias("fluid/fluid_pump", BLOCK.FLUID_PUMP.asItem());
        }
    }

    public static final class TICKET_TYPE {
        private TICKET_TYPE() {}

        public static TicketType<ChunkPos> QUARRY;
        public static TicketType<ChunkPos> QUARRY_BOUNDARY;

        static void register() {
            QUARRY = TicketType.create("logistics:quarry", Comparator.comparingLong(ChunkPos::toLong), 20);
            QUARRY_BOUNDARY = TicketType.create("logistics:quarry_boundary", Comparator.comparingLong(ChunkPos::toLong), 20);
        }
    }
}

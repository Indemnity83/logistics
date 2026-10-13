package com.logistics;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigEntries;
import com.indemnity83.configory.ConfigKey;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.LogisticsCreativeTab;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.FarmMenu;
import com.logistics.forestry.farm.arboretum.ArboretumBlock;
import com.logistics.forestry.farm.crop.CropFarmBlock;
import com.logistics.forestry.humus.HumusBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class LogisticsForestry extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsForestry INSTANCE = new LogisticsForestry();

    @Override
    protected String domain() {
        return "forestry";
    }

    public static ResourceId resource(String name) {
        return INSTANCE.domainResource(name);
    }

    public static ResourceId model(String name) {
        return INSTANCE.domainModelResource(name);
    }

    @Override
    public int order() {
        return 30;
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
        MENU.register();
        RECIPE.register();
        CREATIVE.register();
        ALIAS.register();
    }

    /**
     * Farm soils ({@code humus.json}) and the shared farm machine ({@code farm.json}), both under
     * {@code config/logistics/forestry/}.
     */
    public static final class CONFIG extends ConfigEntries {
        private static final Config humus = configFor(LogisticsConfigHost.MOD_ID, "forestry.humus");
        private static final Config farm = configFor(LogisticsConfigHost.MOD_ID, "forestry.farm");

        private CONFIG() {}

        public static final ConfigKey<Double> HUMUS_GROWTH_CHANCE = humus.defineDouble("humus_growth_chance", 0.25)
                .finite()
                .range(0.0, 1.0)
                .describe("Chance per Humus random tick to give the sapling above an extra growth step (0.0-1.0)")
                .register();

        public static final ConfigKey<Double> HUMUS_DEGRADE_CHANCE = humus.defineDouble("humus_degrade_chance", 0.5)
                .finite()
                .range(0.0, 1.0)
                .describe("Chance that Humus turns to dirt when its boost grows a tree (0.0-1.0)")
                .register();

        public static final ConfigKey<Integer> FARM_WORK_INTERVAL_TICKS = farm.defineInt("work_interval_ticks", 10)
                .min(1)
                .describe("Ticks between plots; a farm works at most one plot per interval")
                .register();

        public static final ConfigKey<Long> FARM_ENERGY_CAPACITY = farm.defineLong("energy_capacity", 20_000L)
                .min(1L)
                .describe("Internal RF buffer capacity")
                .register();

        public static final ConfigKey<Long> FARM_MAX_ENERGY_INPUT = farm.defineLong("max_energy_input", 128L)
                .min(1L)
                .describe("Maximum RF accepted per tick")
                .register();

        public static final ConfigKey<Long> FARM_RF_HARVEST_PER_BLOCK = farm.defineLong("rf_harvest_per_block", 40L)
                .min(0L)
                .maxValueOf(() -> CONFIG.FARM_ENERGY_CAPACITY)
                .describe("RF per block harvested")
                .register();

        public static final ConfigKey<Long> FARM_RF_CLEAR_PER_BLOCK = farm.defineLong("rf_clear_per_block", 40L)
                .min(0L)
                .maxValueOf(() -> CONFIG.FARM_ENERGY_CAPACITY)
                .describe("RF per block or fluid cleared from above a plot")
                .register();

        public static final ConfigKey<Long> FARM_RF_PLANT = farm.defineLong("rf_plant", 100L)
                .min(0L)
                .maxValueOf(() -> CONFIG.FARM_ENERGY_CAPACITY)
                .describe("RF per germling planted")
                .register();

        public static final ConfigKey<Long> FARM_RF_TILL = farm.defineLong("rf_till", 60L)
                .min(0L)
                .maxValueOf(() -> CONFIG.FARM_ENERGY_CAPACITY)
                .describe("RF per block tilled")
                .register();

        public static final ConfigKey<Long> FARM_RF_SOIL = farm.defineLong("rf_soil", 60L)
                .min(0L)
                .maxValueOf(() -> CONFIG.FARM_ENERGY_CAPACITY)
                .describe("RF per soil block laid")
                .register();

        public static final ConfigKey<Long> FARM_RF_MOISTEN = farm.defineLong("rf_moisten", 20L)
                .min(0L)
                .maxValueOf(() -> CONFIG.FARM_ENERGY_CAPACITY)
                .describe("RF per planted farmland block kept moist; spends no water")
                .register();

        public static final ConfigKey<Integer> FARM_WATER_CAPACITY_MB = farm.defineInt("water_capacity_mb", 8_000)
                .min(1)
                .describe("Water tank capacity (mB) of farms that till with water")
                .register();

        public static final ConfigKey<Integer> FARM_WATER_PER_TILL_MB = farm.defineInt("water_per_till", 250)
                .min(0)
                .describe("Water (mB) per block tilled, which keeps it moist for the crop's whole life")
                .register();

        public static final ConfigKey<Integer> FARM_HARVEST_BLOCKS_PER_TICK =
                farm.defineInt("harvest_blocks_per_tick", 4)
                        .min(1)
                        .describe("Blocks broken per tick while harvesting, e.g. felling a tree")
                        .register();

        public static final ConfigKey<Integer> FARM_IDLE_BACKOFF_BASE_TICKS =
                farm.defineInt("idle_backoff_base_ticks", 20)
                        .min(1)
                        .describe("Pause after a full pass over the plot finds nothing to do")
                        .register();

        public static final ConfigKey<Integer> FARM_IDLE_BACKOFF_MAX_TICKS =
                farm.defineInt("idle_backoff_max_ticks", 200)
                        .min(1)
                        .minValueOf(() -> CONFIG.FARM_IDLE_BACKOFF_BASE_TICKS)
                        .describe("Longest pause between idle passes; the pause doubles per idle pass up to this")
                        .register();

        static void register() {}
    }

    public static final class TAG {
        private TAG() {}

        /** Items that count as fertilizer. */
        public static final TagKey<Item> FERTILIZERS =
                TagKey.create(Registries.ITEM, resource("fertilizers").toIdentifier());

        /** Items the Arboretum plants. */
        public static final TagKey<Item> ARBORETUM_GERMLINGS =
                TagKey.create(Registries.ITEM, resource("arboretum_germlings").toIdentifier());

        /** Items the Arboretum lays as soil. */
        public static final TagKey<Item> ARBORETUM_SOILS =
                TagKey.create(Registries.ITEM, resource("arboretum_soils").toIdentifier());

        /** Saplings that only grow as an aligned 2×2 group, e.g. dark oak. */
        public static final TagKey<Item> REQUIRES_2X2 =
                TagKey.create(Registries.ITEM, resource("requires_2x2").toIdentifier());

        /** Ground the Crop Farm tills into farmland. */
        public static final TagKey<Block> TILLABLE =
                TagKey.create(Registries.BLOCK, resource("tillable").toIdentifier());

        /** Seeds and crop items the Crop Farm plants. */
        public static final TagKey<Item> CROP_GERMLINGS =
                TagKey.create(Registries.ITEM, resource("crop_germlings").toIdentifier());

        /** Soil the Crop Farm lays where its plot has no ground. */
        public static final TagKey<Item> CROP_SOILS =
                TagKey.create(Registries.ITEM, resource("crop_soils").toIdentifier());
    }

    public static final class BLOCK {
        private BLOCK() {}

        /** Every farm block; {@link ENTITY#FARM} is valid for exactly these. */
        private static final List<Block> FARMS = new ArrayList<>();

        public static Block HUMUS;
        public static Block ARBORETUM;
        public static Block CROP_FARM;

        static void register() {
            HUMUS = INSTANCE.registerBlockWithItem("humus",
                props -> new HumusBlock(props.mapColor(MapColor.DIRT).strength(0.5f).sound(SoundType.ROOTED_DIRT)));
            ARBORETUM = registerFarm("arboretum",
                props -> new ArboretumBlock(props.mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(FarmMachineBlock.LIT) ? 13 : 0)));
            CROP_FARM = registerFarm("crop_farm",
                props -> new CropFarmBlock(props.mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(CropFarmBlock.LIT) ? 13 : 0)));
        }

        /**
         * Registers a farm block with its item and makes the shared {@link ENTITY#FARM} block entity
         * valid for it. Call from {@link #register()}, which runs before {@link ENTITY#register()}.
         */
        static Block registerFarm(String name, Function<BlockBehaviour.Properties, FarmMachineBlock> factory) {
            Block block = INSTANCE.registerBlockWithItem(name, factory::apply);
            FARMS.add(block);
            return block;
        }
    }

    public static final class ITEM {
        private ITEM() {}

        public static Item FERTILIZER;

        static void register() {
            FERTILIZER = INSTANCE.registerItem("fertilizer", Item::new);
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        /** The one block entity type every farm block shares. */
        public static BlockEntityType<FarmBlockEntity> FARM;

        static void register() {
            FARM = INSTANCE.registerBlockEntity("farm", FarmBlockEntity::new, BLOCK.FARMS.toArray(new Block[0]));
        }
    }

    public static final class MENU {
        private MENU() {}

        /** The one menu every farm block opens. */
        public static MenuType<FarmMenu> FARM;

        static void register() {
            FARM = INSTANCE.registerMenuType("farm", FarmMenu::new);
        }
    }

    public static final class RECIPE {
        private RECIPE() {}

        static void register() {}
    }

    public static final class CREATIVE {
        public static final LogisticsCreativeTab TAB = LogisticsCreativeTab.create(
            LogisticsMod.modId("5_forestry"),
            Component.translatable("itemGroup.logistics.5_forestry"),
            () -> new ItemStack(ITEM.FERTILIZER)
        );

        private CREATIVE() {}

        static void register() {
            TAB.add(ITEM.FERTILIZER);
            TAB.add(BLOCK.HUMUS);
            TAB.add(BLOCK.ARBORETUM);
            TAB.add(BLOCK.CROP_FARM);
            CreativeTabRegistrar.INSTANCE.registerTab(TAB);
        }
    }

    public static final class ALIAS {
        private ALIAS() {}

        static void register() {}
    }
}

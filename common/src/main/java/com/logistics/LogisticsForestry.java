package com.logistics;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigEntries;
import com.indemnity83.configory.ConfigKey;
import com.indemnity83.configory.ValidationResult;
import com.indemnity83.configory.builder.IntConfigBuilder;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.LogisticsCreativeTab;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.core.machine.tier.TieredBlockItem;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmCost;
import com.logistics.forestry.farm.FarmGeometry;
import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.FarmMenu;
import com.logistics.forestry.farm.FarmSettings;
import com.logistics.forestry.farm.FarmTierProfile;
import com.logistics.forestry.farm.arboretum.ArboretumBlock;
import com.logistics.forestry.farm.crop.CropFarmBlock;
import com.logistics.forestry.humus.HumusBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
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
import org.jetbrains.annotations.Nullable;

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

        /** One tier's farm profile keys, {@code tier.<id>.*}; see {@link FarmTierProfile}. */
        public record FarmTierKeys(
                ConfigKey<Integer> outerSize,
                ConfigKey<Integer> checksPerSecond,
                ConfigKey<Integer> actionsPerSecond,
                ConfigKey<Long> energyCapacity,
                ConfigKey<Long> maxEnergyInput,
                ConfigKey<Integer> waterCapacityMb) {}

        public static final FarmTierKeys FARM_TIER_BASIC = farmTierKeys(MachineTier.BASIC, null);
        public static final FarmTierKeys FARM_TIER_INDUSTRIAL =
                farmTierKeys(MachineTier.INDUSTRIAL, () -> CONFIG.FARM_TIER_BASIC.outerSize());
        public static final FarmTierKeys FARM_TIER_PRECISION =
                farmTierKeys(MachineTier.PRECISION, () -> CONFIG.FARM_TIER_INDUSTRIAL.outerSize());
        public static final FarmTierKeys FARM_TIER_INFERNAL =
                farmTierKeys(MachineTier.INFERNAL, () -> CONFIG.FARM_TIER_PRECISION.outerSize());

        public static FarmTierKeys farmTier(MachineTier tier) {
            return switch (tier) {
                case BASIC -> FARM_TIER_BASIC;
                case INDUSTRIAL -> FARM_TIER_INDUSTRIAL;
                case PRECISION -> FARM_TIER_PRECISION;
                case INFERNAL -> FARM_TIER_INFERNAL;
            };
        }

        private static FarmTierKeys farmTierKeys(
                MachineTier tier, @Nullable Supplier<ConfigKey<Integer>> previousOuter) {
            FarmTierProfile defaults = FarmTierProfile.defaults(tier);
            String prefix = "tier." + tier.id() + ".";
            IntConfigBuilder outer = farm.defineInt(prefix + "outer_size", defaults.outerSize())
                    .greaterThan(FarmGeometry.COURTYARD)
                    .validator((value, config) ->
                            value % 2 != 0 ? ValidationResult.ok() : ValidationResult.error("must be odd"))
                    .describe("Side of the square the farm works out to at this tier: odd, wider than the "
                            + FarmGeometry.COURTYARD + "x" + FarmGeometry.COURTYARD
                            + " courtyard, and at least the previous tier's");
            if (previousOuter != null) {
                outer.minValueOf(previousOuter);
            }
            return new FarmTierKeys(
                    outer.register(),
                    farm.defineInt(prefix + "checks_per_second", defaults.checksPerSecond())
                            .min(1)
                            .describe("Plots the farm inspects per second at this tier; inspecting costs no RF")
                            .register(),
                    farm.defineInt(prefix + "actions_per_second", defaults.actionsPerSecond())
                            .min(1)
                            .describe("World-changing actions (clear, harvest, till, soil, plant, moisten) per "
                                    + "second at this tier; each costs RF")
                            .register(),
                    farm.defineLong(prefix + "energy_capacity", defaults.energyCapacity())
                            .min(1L)
                            .describe("Internal RF buffer capacity at this tier")
                            .register(),
                    farm.defineLong(prefix + "max_energy_input", defaults.maxEnergyInput())
                            .min(1L)
                            .describe("Maximum RF accepted per tick at this tier")
                            .register(),
                    farm.defineInt(prefix + "water_capacity_mb", defaults.waterCapacityMb())
                            .min(1)
                            .describe("Water tank capacity (mB) at this tier, for farms that till with water")
                            .register());
        }

        private static final Map<FarmCost, ConfigKey<Long>> FARM_COSTS = farmCosts();

        /** The RF key of {@code cost}, {@code rf_*}. */
        public static ConfigKey<Long> farmCost(FarmCost cost) {
            return FARM_COSTS.get(cost);
        }

        private static Map<FarmCost, ConfigKey<Long>> farmCosts() {
            Map<FarmCost, ConfigKey<Long>> keys = new EnumMap<>(FarmCost.class);
            for (FarmCost cost : FarmCost.values()) {
                keys.put(cost, farm.defineLong(cost.configName(), cost.defaultRf())
                        .min(0L)
                        .maxValueOf(() -> CONFIG.FARM_TIER_BASIC.energyCapacity())
                        .describe(cost.description())
                        .register());
            }
            return Collections.unmodifiableMap(keys);
        }

        public static final ConfigKey<Integer> FARM_WATER_PER_TILL_MB = farm.defineInt(
                        "water_per_till_mb", FarmSettings.DEFAULT_WATER_PER_TILL_MB)
                .min(0)
                .describe("Water (mB) per block tilled in a temperate biome, which keeps it moist for the crop's "
                        + "whole life; hot or dry biomes cost up to twice this, rain halves it")
                .register();

        public static final ConfigKey<Double> FARM_HARVEST_SHARE = farm.defineDouble(
                        "harvest_share", FarmSettings.DEFAULT_HARVEST_SHARE)
                .finite()
                .range(0.0, FarmSettings.MAX_HARVEST_SHARE)
                .describe("Share of actions a multi-block harvest (a tree) takes while the farm also has plots "
                        + "to work (0.0-0.75); the rest always goes to planting, tilling and clearing")
                .register();

        public static final ConfigKey<Integer> FARM_READS_PER_CHECK = farm.defineInt(
                        "reads_per_check", FarmSettings.DEFAULT_READS_PER_CHECK)
                .min(16)
                .describe("Block reads one check buys; a plot inspection costs one check, and tree planning "
                        + "spends checks as it reads")
                .register();

        public static final ConfigKey<Integer> FARM_IDLE_BACKOFF_BASE_TICKS =
                farm.defineInt("idle_backoff_base_ticks", FarmSettings.DEFAULT_IDLE_BACKOFF_BASE_TICKS)
                        .min(1)
                        .describe("Pause after a full pass over the plot finds nothing to do")
                        .register();

        public static final ConfigKey<Integer> FARM_IDLE_BACKOFF_MAX_TICKS =
                farm.defineInt("idle_backoff_max_ticks", FarmSettings.DEFAULT_IDLE_BACKOFF_MAX_TICKS)
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
            Block block = INSTANCE.registerBlockWithItem(name, factory::apply, TieredBlockItem::new);
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

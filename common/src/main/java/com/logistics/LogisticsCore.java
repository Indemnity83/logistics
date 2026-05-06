package com.logistics;

import com.logistics.core.LogisticsConfig;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.item.ProbeItem;
import com.logistics.core.item.WrenchItem;
import com.logistics.core.macerator.MaceratorBlock;
import com.logistics.core.macerator.MaceratorBlockEntity;
import com.logistics.core.macerator.MaceratorRecipeDisplay;
import com.logistics.core.macerator.MaceratorRecipeManager;
import com.logistics.core.macerator.MaceratorRecipeSerializer;
import com.logistics.core.macerator.MaceratorRecipeWrapper;
import com.logistics.core.macerator.MaceratorScreenHandler;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        LogisticsConfig.load();

        BLOCK.register();
        ITEM.register();
        ENTITY.register();
        MENU.register();
        RECIPE.register();

        registerLegacyAliases();
        addCreativeTabEntries();
        MaceratorRecipeManager.register();
    }

    @Override
    public int order() {
        return -100;
    }

    public static final class BLOCK {
        public static Block TIN_ORE;
        public static Block DEEPSLATE_TIN_ORE;
        public static Block TIN_BLOCK;
        public static Block RAW_TIN_BLOCK;
        public static Block BRONZE_BLOCK;
        public static Block APATITE_ORE;
        public static Block APATITE_BLOCK;
        public static Block MACERATOR;
        public static Block QUARTZ_CRYSTAL;

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
            MACERATOR = INSTANCE.registerBlockWithItem("macerator",
                props -> new MaceratorBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MaceratorBlock.LIT) ? 13 : 0)));
            QUARTZ_CRYSTAL = INSTANCE.registerBlockWithItem("quartz_crystal",
                props -> new Block(props.strength(0.8f).sound(SoundType.GLASS).noOcclusion()));
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        public static BlockEntityType<MaceratorBlockEntity> MACERATOR_BLOCK_ENTITY;

        static void register() {
            MACERATOR_BLOCK_ENTITY = INSTANCE.registerBlockEntity("macerator", MaceratorBlockEntity::new, BLOCK.MACERATOR);
        }
    }

    public static final class MENU {
        private MENU() {}

        public static MenuType<MaceratorScreenHandler> MACERATOR;

        static void register() {
            MACERATOR = INSTANCE.registerMenuType("macerator", MaceratorScreenHandler::new);
        }
    }

    public static final class RECIPE {
        private RECIPE() {}

        public static RecipeType<MaceratorRecipeWrapper> MACERATOR_RECIPE_TYPE;
        public static RecipeSerializer<MaceratorRecipeWrapper> MACERATOR_RECIPE_SERIALIZER;
        public static RecipeBookCategory MACERATOR_CATEGORY;
        public static RecipeDisplay.Type<MaceratorRecipeDisplay> MACERATOR_DISPLAY_TYPE;

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
            MACERATOR_CATEGORY = Registry.register(
                BuiltInRegistries.RECIPE_BOOK_CATEGORY,
                LogisticsMod.modId("macerator").toIdentifier(),
                new RecipeBookCategory()
            );
            MACERATOR_DISPLAY_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_DISPLAY,
                LogisticsMod.modId("macerator").toIdentifier(),
                MaceratorRecipeDisplay.TYPE
            );
        }
    }

    public static final class ITEM {
        public static Item WRENCH;
        public static Item PROBE;
        public static Item RAW_TIN;
        public static Item TIN_INGOT;
        public static Item TIN_NUGGET;
        public static Item BRONZE_INGOT;
        public static Item BRONZE_NUGGET;
        public static Item APATITE;
        public static Item STURDY_CASING;
        public static Item MACHINE_CORE;

        public static Item WOODEN_GEAR;
        public static Item STONE_GEAR;
        public static Item COPPER_GEAR;
        public static Item TIN_GEAR;
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
        public static Item SILICON_MIX;
        public static Item SILICON_WAFER;
        public static Item FLOUR;
        public static Item WOOD_PULP;

        // Chips — logic components for pipe modules
        public static Item CARBON_CHIP;
        public static Item REDSTONE_CHIP;
        public static Item AMETHYST_CHIP;
        public static Item ECHO_CHIP;

        // Valves — pipe chassis components
        public static Item WOODEN_VALVE;
        public static Item COPPER_VALVE;
        public static Item BRONZE_VALVE;
        public static Item IRON_VALVE;
        public static Item GOLD_VALVE;
        public static Item DIAMOND_VALVE;
        public static Item OBSIDIAN_VALVE;
        public static Item BLAZING_VALVE;
        public static Item EMERALD_VALVE;
        public static Item APATITE_VALVE;
        public static Item LAPIS_VALVE;
        public static Item ENDER_VALVE;
        public static Item NETHERITE_VALVE;

        // Cores — intermediate components for valves and pipe logic
        public static Item WOODEN_CORE;
        public static Item COPPER_CORE;
        public static Item BRONZE_CORE;
        public static Item IRON_CORE;
        public static Item GOLD_CORE;
        public static Item LAPIS_CORE;
        public static Item APATITE_CORE;
        public static Item DIAMOND_CORE;
        public static Item EMERALD_CORE;
        public static Item BLAZING_CORE;
        public static Item NETHERITE_CORE;
        public static Item OBSIDIAN_CORE;
        public static Item ENDER_CORE;

        private ITEM() {}

        static void register() {
            WRENCH = INSTANCE.registerItem("wrench",
                props -> new WrenchItem(props.stacksTo(1)));
            PROBE = INSTANCE.registerItem("probe",
                props -> new ProbeItem(props.stacksTo(1)));

            // Tin Materials
            RAW_TIN = INSTANCE.registerItem("raw_tin", Item::new);
            TIN_INGOT = INSTANCE.registerItem("tin_ingot", Item::new);
            TIN_NUGGET = INSTANCE.registerItem("tin_nugget", Item::new);

            // Bronze Materials
            BRONZE_INGOT = INSTANCE.registerItem("bronze_ingot", Item::new);
            BRONZE_NUGGET = INSTANCE.registerItem("bronze_nugget", Item::new);

            // Apatite
            APATITE = INSTANCE.registerItem("apatite", Item::new);

            // Components
            STURDY_CASING = INSTANCE.registerItem("sturdy_casing", Item::new);
            MACHINE_CORE = INSTANCE.registerItem("machine_core", Item::new);

            // Gears
            WOODEN_GEAR = INSTANCE.registerItem("wooden_gear", Item::new);
            STONE_GEAR = INSTANCE.registerItem("stone_gear", Item::new);
            COPPER_GEAR = INSTANCE.registerItem("copper_gear", Item::new);
            TIN_GEAR = INSTANCE.registerItem("tin_gear", Item::new);
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
            SILICON_MIX = INSTANCE.registerItem("silicon_mix", Item::new);
            SILICON_WAFER = INSTANCE.registerItem("silicon_wafer", Item::new);
            FLOUR = INSTANCE.registerItem("flour", Item::new);
            WOOD_PULP = INSTANCE.registerItem("wood_pulp", Item::new);

            // Chips
            CARBON_CHIP = INSTANCE.registerItem("carbon_chip", Item::new);
            REDSTONE_CHIP = INSTANCE.registerItem("redstone_chip", Item::new);
            AMETHYST_CHIP = INSTANCE.registerItem("amethyst_chip", Item::new);
            ECHO_CHIP = INSTANCE.registerItem("echo_chip", Item::new);

            // Valves
            WOODEN_VALVE = INSTANCE.registerItem("wooden_valve", Item::new);
            COPPER_VALVE = INSTANCE.registerItem("copper_valve", Item::new);
            BRONZE_VALVE = INSTANCE.registerItem("bronze_valve", Item::new);
            IRON_VALVE = INSTANCE.registerItem("iron_valve", Item::new);
            GOLD_VALVE = INSTANCE.registerItem("gold_valve", Item::new);
            DIAMOND_VALVE = INSTANCE.registerItem("diamond_valve", Item::new);
            OBSIDIAN_VALVE = INSTANCE.registerItem("obsidian_valve", Item::new);
            BLAZING_VALVE = INSTANCE.registerItem("blazing_valve", Item::new);
            EMERALD_VALVE = INSTANCE.registerItem("emerald_valve", Item::new);
            APATITE_VALVE = INSTANCE.registerItem("apatite_valve", Item::new);
            LAPIS_VALVE = INSTANCE.registerItem("lapis_valve", Item::new);
            ENDER_VALVE = INSTANCE.registerItem("ender_valve", Item::new);
            NETHERITE_VALVE = INSTANCE.registerItem("netherite_valve", Item::new);

            // Cores
            WOODEN_CORE = INSTANCE.registerItem("wooden_core", Item::new);
            COPPER_CORE = INSTANCE.registerItem("copper_core", Item::new);
            BRONZE_CORE = INSTANCE.registerItem("bronze_core", Item::new);
            IRON_CORE = INSTANCE.registerItem("iron_core", Item::new);
            GOLD_CORE = INSTANCE.registerItem("gold_core", Item::new);
            LAPIS_CORE = INSTANCE.registerItem("lapis_core", Item::new);
            APATITE_CORE = INSTANCE.registerItem("apatite_core", Item::new);
            DIAMOND_CORE = INSTANCE.registerItem("diamond_core", Item::new);
            EMERALD_CORE = INSTANCE.registerItem("emerald_core", Item::new);
            BLAZING_CORE = INSTANCE.registerItem("blazing_core", Item::new);
            NETHERITE_CORE = INSTANCE.registerItem("netherite_core", Item::new);
            OBSIDIAN_CORE = INSTANCE.registerItem("obsidian_core", Item::new);
            ENDER_CORE = INSTANCE.registerItem("ender_core", Item::new);
        }
    }

    public static final class CREATIVE_TAB {
        private CREATIVE_TAB() {}
        private static final List<Consumer<CreativeModeTab.Output>> ENTRIES = new ArrayList<>();

        public static CreativeModeTab LOGISTICS_TRANSPORT;

        public static void populateEntries(CreativeModeTab.Output output) {
            for (Consumer<CreativeModeTab.Output> entry : ENTRIES) {
                entry.accept(output);
            }
        }

        public static void add(Consumer<CreativeModeTab.Output> entryBuilder) {
            ENTRIES.add(entryBuilder);
        }

        public static void addItem(ItemLike item) {
            add(entries -> entries.accept(item));
        }

        public static void addItems(ItemLike... items) {
            add(entries -> {
                for (ItemLike item : items) {
                    entries.accept(item);
                }
            });
        }
    }

    private static void addCreativeTabEntries() {
        CREATIVE_TAB.addItems(
                ITEM.WRENCH,
                ITEM.PROBE
        );
        CREATIVE_TAB.addItem(BLOCK.MACERATOR);
        CREATIVE_TAB.addItem(BLOCK.QUARTZ_CRYSTAL);
    }

    private void registerLegacyAliases() {
        // v0.2 => v0.3
        registerItemAlias("wrench", ITEM.WRENCH);
        registerItemAlias("wooden_gear", ITEM.WOODEN_GEAR);
        registerItemAlias("stone_gear", ITEM.STONE_GEAR);
        registerItemAlias("copper_gear", ITEM.COPPER_GEAR);
        registerItemAlias("iron_gear", ITEM.IRON_GEAR);
        registerItemAlias("gold_gear", ITEM.GOLD_GEAR);
        registerItemAlias("diamond_gear", ITEM.DIAMOND_GEAR);
        registerItemAlias("netherite_gear", ITEM.NETHERITE_GEAR);

    }
}

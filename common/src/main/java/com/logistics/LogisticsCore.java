package com.logistics;

import com.logistics.core.LogisticsConfig;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.item.WrenchItem;
import com.logistics.core.macerator.MaceratorBlock;
import com.logistics.core.macerator.MaceratorBlockEntity;
import com.logistics.core.macerator.MaceratorRecipeDisplay;
import com.logistics.core.macerator.MaceratorRecipeSerializer;
import com.logistics.core.macerator.MaceratorRecipeWrapper;
import com.logistics.core.macerator.MaceratorScreenHandler;
import com.logistics.core.lib.platform.LogisticsCreativeTab;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;

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
    public int order() {
        return -100;
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
        CREATIVE.register();
        ALIAS.register();
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

    public static final class ITEM {
        public static Item WRENCH;
        public static Item RAW_TIN;
        public static Item TIN_INGOT;
        public static Item TIN_NUGGET;
        public static Item BRONZE_INGOT;
        public static Item BRONZE_NUGGET;
        public static Item APATITE;
        public static Item STURDY_CASING;

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
        public static Item SAWDUST;

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
            SAWDUST = INSTANCE.registerItem("sawdust", Item::new);

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

    public static final class CREATIVE {
        public static final LogisticsCreativeTab TAB = LogisticsCreativeTab.create(
            LogisticsMod.modId("logistics_transport"),
            Component.translatable("itemGroup.logistics.logistics_transport"),
            () -> new ItemStack(ITEM.IRON_GEAR)
        );

        private CREATIVE() {}

        static void register() {
            TAB.add(ITEM.WRENCH);
            TAB.add(BLOCK.MACERATOR);
            TAB.add(BLOCK.QUARTZ_CRYSTAL);

            // Register the tab — populate() is lazy, so other domains can still add items after this
            CreativeTabRegistrar.INSTANCE.registerTab(TAB);

            // Add storage blocks to Building Blocks tab
            CreativeTabRegistrar.INSTANCE.modifyTab(CreativeModeTabs.BUILDING_BLOCKS, entries -> {
                entries.insertAfter(Items.COAL_BLOCK, BLOCK.APATITE_BLOCK);
                entries.insertBefore(Items.IRON_BLOCK, BLOCK.TIN_BLOCK);
                entries.insertAfter(Items.IRON_BLOCK, BLOCK.BRONZE_BLOCK);
            });

            // Add materials to Ingredients tab — all in one callback so each insertAfter/insertBefore
            // sees the items placed by earlier calls in the same invocation
            CreativeTabRegistrar.INSTANCE.modifyTab(CreativeModeTabs.INGREDIENTS, entries -> {
                // Raw materials
                entries.insertBefore(Items.RAW_IRON, ITEM.RAW_TIN);

                // Ingots
                entries.insertBefore(Items.IRON_INGOT, ITEM.TIN_INGOT);
                entries.insertAfter(ITEM.TIN_INGOT, ITEM.BRONZE_INGOT);

                // Nuggets
                entries.insertBefore(Items.IRON_NUGGET, ITEM.TIN_NUGGET);
                entries.insertAfter(ITEM.TIN_NUGGET, ITEM.BRONZE_NUGGET);

                // Apatite
                entries.insertBefore(Items.AMETHYST_SHARD, ITEM.APATITE);

                // Intermediate Crafting Items
                entries.insertBefore(Items.HEAVY_CORE, ITEM.STURDY_CASING);
                entries.insertAfter(ITEM.STURDY_CASING, ITEM.WOODEN_GEAR);
                entries.insertAfter(ITEM.WOODEN_GEAR, ITEM.STONE_GEAR);
                entries.insertAfter(ITEM.STONE_GEAR, ITEM.COPPER_GEAR);
                entries.insertAfter(ITEM.COPPER_GEAR, ITEM.TIN_GEAR);
                entries.insertAfter(ITEM.TIN_GEAR, ITEM.IRON_GEAR);
                entries.insertAfter(ITEM.IRON_GEAR, ITEM.BRONZE_GEAR);
                entries.insertAfter(ITEM.BRONZE_GEAR, ITEM.GOLD_GEAR);
                entries.insertAfter(ITEM.GOLD_GEAR, ITEM.DIAMOND_GEAR);
                entries.insertAfter(ITEM.DIAMOND_GEAR, ITEM.NETHERITE_GEAR);

                // Valves — after netherite gear
                Item[] valves = {
                    ITEM.WOODEN_VALVE,
                    ITEM.COPPER_VALVE, ITEM.BRONZE_VALVE,
                    ITEM.IRON_VALVE, ITEM.GOLD_VALVE, ITEM.DIAMOND_VALVE,
                    ITEM.OBSIDIAN_VALVE, ITEM.BLAZING_VALVE, ITEM.EMERALD_VALVE,
                    ITEM.APATITE_VALVE, ITEM.LAPIS_VALVE, ITEM.ENDER_VALVE,
                    ITEM.NETHERITE_VALVE
                };
                Item prev = ITEM.NETHERITE_GEAR;
                for (Item valve : valves) {
                    entries.insertAfter(prev, valve);
                    prev = valve;
                }

                // Dusts, chips, cores — after bronze_ingot (inserted above)
                Item[] intermediates = {
                    ITEM.APATITE_DUST,
                    ITEM.IRON_DUST, ITEM.COPPER_DUST, ITEM.TIN_DUST, ITEM.BRONZE_DUST,
                    ITEM.GOLD_DUST, ITEM.LAPIS_DUST, ITEM.QUARTZ_DUST, ITEM.COAL_DUST,
                    ITEM.AMETHYST_DUST, ITEM.DIAMOND_DUST, ITEM.EMERALD_DUST,
                    ITEM.NETHERITE_DUST, ITEM.OBSIDIAN_DUST, ITEM.ENDER_DUST,
                    ITEM.ECHO_DUST, ITEM.PRISMARINE_DUST,
                    ITEM.SILICON_MIX, ITEM.SILICON_WAFER, ITEM.FLOUR, ITEM.SAWDUST,
                    ITEM.CARBON_CHIP, ITEM.REDSTONE_CHIP, ITEM.AMETHYST_CHIP, ITEM.ECHO_CHIP,
                    ITEM.WOODEN_CORE,
                    ITEM.COPPER_CORE, ITEM.BRONZE_CORE,
                    ITEM.IRON_CORE, ITEM.GOLD_CORE, ITEM.LAPIS_CORE,
                    ITEM.APATITE_CORE, ITEM.DIAMOND_CORE, ITEM.EMERALD_CORE,
                    ITEM.BLAZING_CORE, ITEM.NETHERITE_CORE,
                    ITEM.OBSIDIAN_CORE, ITEM.ENDER_CORE
                };
                Item anchor = ITEM.BRONZE_INGOT;
                for (Item item : intermediates) {
                    entries.insertAfter(anchor, item);
                    anchor = item;
                }
            });

            // Add ore blocks to Natural Blocks tab
            CreativeTabRegistrar.INSTANCE.modifyTab(CreativeModeTabs.NATURAL_BLOCKS, entries -> {
                // Tin ores
                entries.insertAfter(Items.DEEPSLATE_COAL_ORE, BLOCK.TIN_ORE);
                entries.insertAfter(BLOCK.TIN_ORE, BLOCK.DEEPSLATE_TIN_ORE);

                // Apatite ore
                entries.insertBefore(Items.AMETHYST_BLOCK, BLOCK.APATITE_ORE);

                // Raw tin block
                entries.insertBefore(Items.RAW_IRON_BLOCK, BLOCK.RAW_TIN_BLOCK);
            });
        }
    }

    public static final class ALIAS {
        private ALIAS() {}

        static void register() {
            // v0.2 => v0.3
            INSTANCE.registerItemAlias("wrench", ITEM.WRENCH);
            INSTANCE.registerItemAlias("wooden_gear", ITEM.WOODEN_GEAR);
            INSTANCE.registerItemAlias("stone_gear", ITEM.STONE_GEAR);
            INSTANCE.registerItemAlias("copper_gear", ITEM.COPPER_GEAR);
            INSTANCE.registerItemAlias("iron_gear", ITEM.IRON_GEAR);
            INSTANCE.registerItemAlias("gold_gear", ITEM.GOLD_GEAR);
            INSTANCE.registerItemAlias("diamond_gear", ITEM.DIAMOND_GEAR);
            INSTANCE.registerItemAlias("netherite_gear", ITEM.NETHERITE_GEAR);

            // wood_pulp renamed to sawdust
            INSTANCE.registerItemAlias("core/wood_pulp", ITEM.SAWDUST);
        }
    }
}

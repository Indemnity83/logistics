package com.logistics;

import com.logistics.core.LogisticsCommands;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.item.ProbeItem;
import com.logistics.core.item.WrenchItem;
import com.logistics.core.macerator.MaceratorBlock;
import com.logistics.core.macerator.MaceratorBlockEntity;
import com.logistics.core.macerator.MaceratorRecipeManager;
import com.logistics.core.macerator.MaceratorRecipeSerializer;
import com.logistics.core.macerator.MaceratorRecipeWrapper;
import com.logistics.core.macerator.MaceratorScreenHandler;
import com.logistics.core.lib.block.lookup.EnergyStorageAccess;
import com.logistics.core.lib.block.lookup.FluidStorageAccess;
import com.logistics.core.lib.block.lookup.ItemStorageAccess;
import com.logistics.core.lib.block.lookup.PipeConnectionAccess;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.fluids.MoltenGlassFluid;
import com.logistics.core.loot.ChestLootModifier;
import com.logistics.core.network.NetworkTickHandler;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

        BLOCK.register();
        ITEM.register();
        FLUID.register();
        ENTITY.register();
        MENU.register();
        RECIPE.register();
        CREATIVE_TAB.register();

        registerStorageAccess();
        registerLegacyAliases();
        addCreativeTabEntries();
        addVanillaCreativeTabEntries();
        registerWorldgen();
        ChestLootModifier.register();
        NetworkTickHandler.register();
        LogisticsCommands.register();
        MaceratorRecipeManager.register();
    }

    private void registerStorageAccess() {
        ItemStorageAccess.register();
        FluidStorageAccess.register();
        EnergyStorageAccess.register();
        PipeConnectionAccess.register();
    }

    private void registerWorldgen() {
        // Tin ore worldgen: separate features for stone (rare) and deepslate (abundant)
        // Stone: ~7% of copper's effective rate (1 vein/chunk vs copper's 16)
        // Deepslate: ~80% of copper's rate (13 veins/chunk)
        // Use LogisticsMod.modId() to avoid core/ prefix
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.modId("tin_ore_stone").toIdentifier())
        );
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.modId("tin_ore_deepslate").toIdentifier())
        );

        // Apatite ore worldgen: large veins (up to 48 blocks) spawning above Y 60
        // 2 veins per chunk, Y 60-256 (uniform distribution)
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.modId("apatite_ore_stone").toIdentifier())
        );
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
                new MaceratorRecipeSerializer()
            );
        }
    }

    public static final class FLUID {
        public static net.minecraft.world.level.material.FlowingFluid MOLTEN_GLASS_FLOWING;
        public static net.minecraft.world.level.material.Fluid MOLTEN_GLASS_STILL;

        private FLUID() {}

        static void register() {
            // Register flowing variant first (required by still variant)
            MOLTEN_GLASS_FLOWING = Registry.register(
                BuiltInRegistries.FLUID,
                LogisticsMod.modId("molten_glass_flowing").toIdentifier(),
                new MoltenGlassFluid.Flowing()
            );

            // Register still variant
            MOLTEN_GLASS_STILL = Registry.register(
                BuiltInRegistries.FLUID,
                LogisticsMod.modId("molten_glass").toIdentifier(),
                new MoltenGlassFluid.Still()
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
        public static Item MACHINE_FRAME;

        public static Item WOODEN_GEAR;
        public static Item STONE_GEAR;
        public static Item COPPER_GEAR;
        public static Item TIN_GEAR;
        public static Item IRON_GEAR;
        public static Item GOLD_GEAR;
        public static Item BRONZE_GEAR;
        public static Item DIAMOND_GEAR;
        public static Item NETHERITE_GEAR;

        // Macerator outputs — powders and dusts
        public static Item APATITE_DUST;
        public static Item IRON_POWDER;
        public static Item COPPER_POWDER;
        public static Item TIN_POWDER;
        public static Item BRONZE_POWDER;
        public static Item GOLD_POWDER;
        public static Item LAPIS_DUST;
        public static Item QUARTZ_DUST;
        public static Item COAL_DUST;
        public static Item AMETHYST_DUST;
        public static Item DIAMOND_DUST;
        public static Item EMERALD_DUST;
        public static Item NETHERITE_POWDER;
        public static Item OBSIDIAN_DUST;
        public static Item ENDER_DUST;
        public static Item ECHO_DUST;
        public static Item PRISMARINE_DUST;
        public static Item SILICON_MIX;
        public static Item SILICON_WAFER;
        public static Item FLOUR;
        public static Item WOOD_PULP;

        // Chips — logic components for pipe modules
        public static Item CHIP_CARBON;
        public static Item CHIP_REDSTONE;
        public static Item CHIP_AMETHYST;
        public static Item CHIP_ECHO;

        // Valves — pipe chassis components
        public static Item VALVE_WOODEN;
        public static Item VALVE_COPPER;
        public static Item VALVE_BRONZE;
        public static Item VALVE_IRON;
        public static Item VALVE_GOLD;
        public static Item VALVE_DIAMOND;
        public static Item VALVE_OBSIDIAN;
        public static Item VALVE_BLAZING;
        public static Item VALVE_EMERALD;
        public static Item VALVE_APATITE;
        public static Item VALVE_LAPIS;
        public static Item VALVE_ENDER;
        public static Item VALVE_NETHERITE;

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
            MACHINE_FRAME = INSTANCE.registerItem("machine_frame", Item::new);

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
            IRON_POWDER = INSTANCE.registerItem("iron_powder", Item::new);
            COPPER_POWDER = INSTANCE.registerItem("copper_powder", Item::new);
            TIN_POWDER = INSTANCE.registerItem("tin_powder", Item::new);
            BRONZE_POWDER = INSTANCE.registerItem("bronze_powder", Item::new);
            GOLD_POWDER = INSTANCE.registerItem("gold_powder", Item::new);
            LAPIS_DUST = INSTANCE.registerItem("lapis_dust", Item::new);
            QUARTZ_DUST = INSTANCE.registerItem("quartz_dust", Item::new);
            COAL_DUST = INSTANCE.registerItem("coal_dust", Item::new);
            AMETHYST_DUST = INSTANCE.registerItem("amethyst_dust", Item::new);
            DIAMOND_DUST = INSTANCE.registerItem("diamond_dust", Item::new);
            EMERALD_DUST = INSTANCE.registerItem("emerald_dust", Item::new);
            NETHERITE_POWDER = INSTANCE.registerItem("netherite_powder", Item::new);
            OBSIDIAN_DUST = INSTANCE.registerItem("obsidian_dust", Item::new);
            ENDER_DUST = INSTANCE.registerItem("ender_dust", Item::new);
            ECHO_DUST = INSTANCE.registerItem("echo_dust", Item::new);
            PRISMARINE_DUST = INSTANCE.registerItem("prismarine_dust", Item::new);
            SILICON_MIX = INSTANCE.registerItem("silicon_mix", Item::new);
            SILICON_WAFER = INSTANCE.registerItem("silicon_wafer", Item::new);
            FLOUR = INSTANCE.registerItem("flour", Item::new);
            WOOD_PULP = INSTANCE.registerItem("wood_pulp", Item::new);

            // Chips
            CHIP_CARBON = INSTANCE.registerItem("chip_carbon", Item::new);
            CHIP_REDSTONE = INSTANCE.registerItem("chip_redstone", Item::new);
            CHIP_AMETHYST = INSTANCE.registerItem("chip_amethyst", Item::new);
            CHIP_ECHO = INSTANCE.registerItem("chip_echo", Item::new);

            // Valves
            VALVE_WOODEN = INSTANCE.registerItem("valve_wooden", Item::new);
            VALVE_COPPER = INSTANCE.registerItem("valve_copper", Item::new);
            VALVE_BRONZE = INSTANCE.registerItem("valve_bronze", Item::new);
            VALVE_IRON = INSTANCE.registerItem("valve_iron", Item::new);
            VALVE_GOLD = INSTANCE.registerItem("valve_gold", Item::new);
            VALVE_DIAMOND = INSTANCE.registerItem("valve_diamond", Item::new);
            VALVE_OBSIDIAN = INSTANCE.registerItem("valve_obsidian", Item::new);
            VALVE_BLAZING = INSTANCE.registerItem("valve_blazing", Item::new);
            VALVE_EMERALD = INSTANCE.registerItem("valve_emerald", Item::new);
            VALVE_APATITE = INSTANCE.registerItem("valve_apatite", Item::new);
            VALVE_LAPIS = INSTANCE.registerItem("valve_lapis", Item::new);
            VALVE_ENDER = INSTANCE.registerItem("valve_ender", Item::new);
            VALVE_NETHERITE = INSTANCE.registerItem("valve_netherite", Item::new);

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

        static void register() {
            LOGISTICS_TRANSPORT = Registry.register(
                    BuiltInRegistries.CREATIVE_MODE_TAB,
                    LogisticsMod.modId("logistics_transport").toIdentifier(),
                    FabricItemGroup.builder()
                            .title(Component.literal("Logistics"))
                            .icon(() -> new ItemStack(ITEM.IRON_GEAR))
                            .displayItems((params, entries) -> {
                                for (Consumer<CreativeModeTab.Output> entry : ENTRIES) {
                                    entry.accept(entries);
                                }
                            })
                            .build());
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
    }

    private static void addVanillaCreativeTabEntries() {
        // Add storage blocks to Building Blocks tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.addAfter(Items.COAL_BLOCK, BLOCK.APATITE_BLOCK);
            entries.addBefore(Items.IRON_BLOCK, BLOCK.TIN_BLOCK);
            entries.addAfter(Items.IRON_BLOCK, BLOCK.BRONZE_BLOCK);
        });

        // Add materials to Ingredients tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            // Raw materials
            entries.addBefore(Items.RAW_IRON, ITEM.RAW_TIN);

            // Ingots
            entries.addBefore(Items.IRON_INGOT, ITEM.TIN_INGOT);
            entries.addAfter(ITEM.TIN_INGOT, ITEM.BRONZE_INGOT);

            // Nuggets
            entries.addBefore(Items.IRON_NUGGET, ITEM.TIN_NUGGET);
            entries.addAfter(ITEM.TIN_NUGGET, ITEM.BRONZE_NUGGET);

            // Apatite
            entries.addBefore(Items.AMETHYST_SHARD, ITEM.APATITE);

            // Intermediate Crafting Items
            entries.addBefore(Items.HEAVY_CORE, ITEM.STURDY_CASING);
            entries.addAfter(ITEM.STURDY_CASING, ITEM.MACHINE_FRAME);
            entries.addAfter(ITEM.MACHINE_FRAME, ITEM.WOODEN_GEAR);
            entries.addAfter(ITEM.WOODEN_GEAR, ITEM.STONE_GEAR);
            entries.addAfter(ITEM.STONE_GEAR, ITEM.COPPER_GEAR);
            entries.addAfter(ITEM.COPPER_GEAR, ITEM.TIN_GEAR);
            entries.addAfter(ITEM.TIN_GEAR, ITEM.IRON_GEAR);
            entries.addAfter(ITEM.IRON_GEAR, ITEM.BRONZE_GEAR);
            entries.addAfter(ITEM.BRONZE_GEAR, ITEM.GOLD_GEAR);
            entries.addAfter(ITEM.GOLD_GEAR, ITEM.DIAMOND_GEAR);
            entries.addAfter(ITEM.DIAMOND_GEAR, ITEM.NETHERITE_GEAR);

            // Valves — after netherite gear
            Item[] valves = {
                ITEM.VALVE_WOODEN,
                ITEM.VALVE_COPPER, ITEM.VALVE_BRONZE,
                ITEM.VALVE_IRON, ITEM.VALVE_GOLD, ITEM.VALVE_DIAMOND,
                ITEM.VALVE_OBSIDIAN, ITEM.VALVE_BLAZING, ITEM.VALVE_EMERALD,
                ITEM.VALVE_APATITE, ITEM.VALVE_LAPIS, ITEM.VALVE_ENDER,
                ITEM.VALVE_NETHERITE
            };
            Item prev = ITEM.NETHERITE_GEAR;
            for (Item item : valves) {
                entries.addAfter(prev, item);
                prev = item;
            }
        });

        // Add dusts, chips, cores to Ingredients tab — after bronze_ingot
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            Item[] intermediates = {
                ITEM.APATITE_DUST,
                ITEM.IRON_POWDER, ITEM.COPPER_POWDER, ITEM.TIN_POWDER, ITEM.BRONZE_POWDER,
                ITEM.GOLD_POWDER, ITEM.LAPIS_DUST, ITEM.QUARTZ_DUST, ITEM.COAL_DUST,
                ITEM.AMETHYST_DUST, ITEM.DIAMOND_DUST, ITEM.EMERALD_DUST,
                ITEM.NETHERITE_POWDER, ITEM.OBSIDIAN_DUST, ITEM.ENDER_DUST,
                ITEM.ECHO_DUST, ITEM.PRISMARINE_DUST,
                ITEM.SILICON_MIX, ITEM.SILICON_WAFER, ITEM.FLOUR, ITEM.WOOD_PULP,
                ITEM.CHIP_CARBON, ITEM.CHIP_REDSTONE, ITEM.CHIP_AMETHYST, ITEM.CHIP_ECHO,
                ITEM.WOODEN_CORE,
                ITEM.COPPER_CORE, ITEM.BRONZE_CORE,
                ITEM.IRON_CORE, ITEM.GOLD_CORE, ITEM.LAPIS_CORE,
                ITEM.APATITE_CORE, ITEM.DIAMOND_CORE, ITEM.EMERALD_CORE,
                ITEM.BLAZING_CORE, ITEM.NETHERITE_CORE,
                ITEM.OBSIDIAN_CORE, ITEM.ENDER_CORE
            };
            Item anchor = ITEM.BRONZE_INGOT;
            for (Item item : intermediates) {
                entries.addAfter(anchor, item);
                anchor = item;
            }
        });

        // Add ore blocks to Natural Blocks tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
            // Tin ores
            entries.addAfter(Items.DEEPSLATE_COAL_ORE, BLOCK.TIN_ORE);
            entries.addAfter(BLOCK.TIN_ORE, BLOCK.DEEPSLATE_TIN_ORE);

            // Apatite ore
            entries.addBefore(Items.AMETHYST_BLOCK, BLOCK.APATITE_ORE);

            // Raw tin block
            entries.addBefore(Items.RAW_IRON_BLOCK, BLOCK.RAW_TIN_BLOCK);
        });
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

        // automation domain => core domain (items moved from automation to core)
        registerItemAlias("automation/apatite_powder", ITEM.APATITE_DUST);
        registerItemAlias("core/apatite_powder", ITEM.APATITE_DUST);
        registerItemAlias("automation/iron_powder", ITEM.IRON_POWDER);
        registerItemAlias("automation/copper_powder", ITEM.COPPER_POWDER);
        registerItemAlias("automation/tin_powder", ITEM.TIN_POWDER);
        registerItemAlias("automation/bronze_powder", ITEM.BRONZE_POWDER);
        registerItemAlias("automation/gold_powder", ITEM.GOLD_POWDER);
        registerItemAlias("automation/lapis_dust", ITEM.LAPIS_DUST);
        registerItemAlias("automation/quartz_dust", ITEM.QUARTZ_DUST);
        registerItemAlias("automation/coal_dust", ITEM.COAL_DUST);
        registerItemAlias("automation/amethyst_dust", ITEM.AMETHYST_DUST);
        registerItemAlias("automation/diamond_dust", ITEM.DIAMOND_DUST);
        registerItemAlias("automation/emerald_dust", ITEM.EMERALD_DUST);
        registerItemAlias("automation/netherite_dust", ITEM.NETHERITE_POWDER);
        registerItemAlias("core/netherite_dust", ITEM.NETHERITE_POWDER);
        registerItemAlias("automation/obsidian_dust", ITEM.OBSIDIAN_DUST);
        registerItemAlias("automation/ender_dust", ITEM.ENDER_DUST);
        registerItemAlias("automation/echo_dust", ITEM.ECHO_DUST);
        registerItemAlias("automation/prismarine_dust", ITEM.PRISMARINE_DUST);
        registerItemAlias("automation/silicon_mix", ITEM.SILICON_MIX);
        registerItemAlias("automation/silicon_wafer", ITEM.SILICON_WAFER);
        registerItemAlias("automation/flour", ITEM.FLOUR);
        registerItemAlias("automation/wood_pulp", ITEM.WOOD_PULP);
        registerItemAlias("automation/chip_coal", ITEM.CHIP_CARBON);
        registerItemAlias("core/chip_coal", ITEM.CHIP_CARBON);
        registerItemAlias("automation/chip_redstone", ITEM.CHIP_REDSTONE);
        registerItemAlias("automation/chip_amethyst", ITEM.CHIP_AMETHYST);
        registerItemAlias("automation/chip_echo", ITEM.CHIP_ECHO);
        registerItemAlias("automation/valve_wooden", ITEM.VALVE_WOODEN);
        registerItemAlias("automation/valve_copper", ITEM.VALVE_COPPER);
        registerItemAlias("automation/valve_bronze", ITEM.VALVE_BRONZE);
        registerItemAlias("automation/valve_iron", ITEM.VALVE_IRON);
        registerItemAlias("automation/valve_gold", ITEM.VALVE_GOLD);
        registerItemAlias("automation/valve_diamond", ITEM.VALVE_DIAMOND);
        registerItemAlias("automation/valve_obsidian", ITEM.VALVE_OBSIDIAN);
        registerItemAlias("automation/valve_blazing", ITEM.VALVE_BLAZING);
        registerItemAlias("automation/valve_emerald", ITEM.VALVE_EMERALD);
        registerItemAlias("automation/valve_apatite", ITEM.VALVE_APATITE);
        registerItemAlias("automation/valve_lapis", ITEM.VALVE_LAPIS);
        registerItemAlias("automation/valve_ender", ITEM.VALVE_ENDER);
        registerItemAlias("automation/valve_netherite", ITEM.VALVE_NETHERITE);
        registerItemAlias("automation/wooden_core", ITEM.WOODEN_CORE);
        registerItemAlias("automation/copper_core", ITEM.COPPER_CORE);
        registerItemAlias("automation/bronze_core", ITEM.BRONZE_CORE);
        registerItemAlias("automation/iron_core", ITEM.IRON_CORE);
        registerItemAlias("automation/gold_core", ITEM.GOLD_CORE);
        registerItemAlias("automation/lapis_core", ITEM.LAPIS_CORE);
        registerItemAlias("automation/apatite_core", ITEM.APATITE_CORE);
        registerItemAlias("automation/diamond_core", ITEM.DIAMOND_CORE);
        registerItemAlias("automation/emerald_core", ITEM.EMERALD_CORE);
        registerItemAlias("automation/blazing_core", ITEM.BLAZING_CORE);
        registerItemAlias("automation/netherite_core", ITEM.NETHERITE_CORE);
        registerItemAlias("automation/obsidian_core", ITEM.OBSIDIAN_CORE);
        registerItemAlias("automation/ender_core", ITEM.ENDER_CORE);

        // automation domain => core domain (macerator block moved)
        registerBlockAlias("automation/macerator", BLOCK.MACERATOR);
        registerBlockEntityAlias("automation/macerator", ENTITY.MACERATOR_BLOCK_ENTITY);
        registerItemAlias("automation/macerator", BLOCK.MACERATOR.asItem());
    }
}

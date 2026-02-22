package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.item.ProbeItem;
import com.logistics.core.item.WrenchItem;
import com.logistics.core.lib.block.lookup.EnergyStorageAccess;
import com.logistics.core.lib.block.lookup.FluidStorageAccess;
import com.logistics.core.lib.block.lookup.ItemStorageAccess;
import com.logistics.core.lib.block.lookup.PipeConnectionAccess;
import com.logistics.core.loot.ChestLootModifier;
import com.logistics.core.marker.MarkerBlock;
import com.logistics.core.marker.MarkerBlockEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class LogisticsCore extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsCore INSTANCE = new LogisticsCore();

    @Override
    protected String domain() {
        return "core";
    }

    public static Identifier identifier(String name) {
        return INSTANCE.getDomainIdentifier(name);
    }

    public static Identifier blockModelIdentifier(String name) {
        return INSTANCE.getBlockModelIdentifier(name);
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        BLOCK.register();
        ITEM.register();
        ENTITY.register();
        CREATIVE_TAB.register();

        registerStorageAccess();
        registerLegacyAliases();
        addCreativeTabEntries();
        addVanillaCreativeTabEntries();
        registerWorldgen();
        ChestLootModifier.register();
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
        // Use LogisticsMod.getIdentifier() to avoid core/ prefix
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.getIdentifier("tin_ore_stone"))
        );
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.getIdentifier("tin_ore_deepslate"))
        );

        // Apatite ore worldgen: large veins (up to 48 blocks) spawning above Y 60
        // 2 veins per chunk, Y 60-256 (uniform distribution)
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.getIdentifier("apatite_ore_stone"))
        );
    }

    @Override
    public int order() {
        return -100;
    }

    public static final class BLOCK {
        public static Block MARKER;
        public static Block TIN_ORE;
        public static Block DEEPSLATE_TIN_ORE;
        public static Block TIN_BLOCK;
        public static Block RAW_TIN_BLOCK;
        public static Block BRONZE_BLOCK;
        public static Block APATITE_ORE;
        public static Block APATITE_BLOCK;

        private BLOCK() {}

        static void register() {
            MARKER = INSTANCE.registerBlockWithItem("marker",
                props -> new MarkerBlock(props.strength(0.0f).sound(SoundType.WOOD).noCollision()));

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
        }
    }

    public static final class ENTITY {
        public static BlockEntityType<MarkerBlockEntity> MARKER_BLOCK_ENTITY;

        private ENTITY() {}

        static void register() {
            MARKER_BLOCK_ENTITY = INSTANCE.registerBlockEntity("marker", MarkerBlockEntity::new, BLOCK.MARKER);
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
        public static Item WOODEN_GEAR;
        public static Item STONE_GEAR;
        public static Item COPPER_GEAR;
        public static Item TIN_GEAR;
        public static Item IRON_GEAR;
        public static Item GOLD_GEAR;
        public static Item BRONZE_GEAR;
        public static Item DIAMOND_GEAR;
        public static Item NETHERITE_GEAR;

        private ITEM() {}

        static void register() {
            WRENCH = INSTANCE.registerItem("wrench",
                props -> new WrenchItem(props.stacksTo(1)));
            PROBE = INSTANCE.registerItem("probe",
                props -> new ProbeItem(props.stacksTo(1)));

            // Tin Materials
            RAW_TIN = INSTANCE.registerItem("raw_tin",
                props -> new Item(props));
            TIN_INGOT = INSTANCE.registerItem("tin_ingot",
                props -> new Item(props));
            TIN_NUGGET = INSTANCE.registerItem("tin_nugget",
                props -> new Item(props));

            // Bronze Materials
            BRONZE_INGOT = INSTANCE.registerItem("bronze_ingot",
                props -> new Item(props));
            BRONZE_NUGGET = INSTANCE.registerItem("bronze_nugget",
                props -> new Item(props));

            // Apatite
            APATITE = INSTANCE.registerItem("apatite",
                props -> new Item(props));

            // Components
            STURDY_CASING = INSTANCE.registerItem("sturdy_casing",
                props -> new Item(props));

            // Gears
            WOODEN_GEAR = INSTANCE.registerItem("wooden_gear",
                props -> new Item(props));
            STONE_GEAR = INSTANCE.registerItem("stone_gear",
                props -> new Item(props));
            COPPER_GEAR = INSTANCE.registerItem("copper_gear",
                props -> new Item(props));
            TIN_GEAR = INSTANCE.registerItem("tin_gear",
                props -> new Item(props));
            IRON_GEAR = INSTANCE.registerItem("iron_gear",
                props -> new Item(props));
            GOLD_GEAR = INSTANCE.registerItem("gold_gear",
                props -> new Item(props));
            BRONZE_GEAR = INSTANCE.registerItem("bronze_gear",
                props -> new Item(props));
            DIAMOND_GEAR = INSTANCE.registerItem("diamond_gear",
                props -> new Item(props));
            NETHERITE_GEAR = INSTANCE.registerItem("netherite_gear",
                props -> new Item(props));
        }
    }

    public static final class CREATIVE_TAB {
        private CREATIVE_TAB() {}
        private static final List<Consumer<CreativeModeTab.Output>> ENTRIES = new ArrayList<>();

        public static CreativeModeTab LOGISTICS_TRANSPORT;

        static void register() {
            LOGISTICS_TRANSPORT = Registry.register(
                    BuiltInRegistries.CREATIVE_MODE_TAB,
                    LogisticsMod.getIdentifier("logistics_transport"),
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
                ITEM.PROBE,
                BLOCK.MARKER
        );
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
            entries.addAfter(ITEM.STURDY_CASING, ITEM.WOODEN_GEAR);
            entries.addAfter(ITEM.WOODEN_GEAR, ITEM.STONE_GEAR);
            entries.addAfter(ITEM.STONE_GEAR, ITEM.COPPER_GEAR);
            entries.addAfter(ITEM.COPPER_GEAR, ITEM.TIN_GEAR);
            entries.addAfter(ITEM.TIN_GEAR, ITEM.IRON_GEAR);
            entries.addAfter(ITEM.IRON_GEAR, ITEM.BRONZE_GEAR);
            entries.addAfter(ITEM.BRONZE_GEAR, ITEM.GOLD_GEAR);
            entries.addAfter(ITEM.GOLD_GEAR, ITEM.DIAMOND_GEAR);
            entries.addAfter(ITEM.DIAMOND_GEAR, ITEM.NETHERITE_GEAR);
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
        registerBlockAlias("marker", BLOCK.MARKER);
        registerBlockEntityAlias("marker", ENTITY.MARKER_BLOCK_ENTITY);
        registerItemAlias("marker", BLOCK.MARKER.asItem());
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

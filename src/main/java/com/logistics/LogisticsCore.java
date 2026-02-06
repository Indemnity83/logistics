package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.item.ProbeItem;
import com.logistics.core.item.WrenchItem;
import com.logistics.core.marker.MarkerBlock;
import com.logistics.core.marker.MarkerBlockEntity;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
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

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        registerLegacyAliases();
        addCreativeTabEntries();
    }

    @Override
    public int order() {
        return -100;
    }

    public static final class BLOCK {
        public static final Block MARKER = INSTANCE.registerBlockWithItem("marker",
            props -> new MarkerBlock(props.strength(0.0f).sound(SoundType.WOOD).noCollision()));

        private BLOCK() {}

    }

    public static final class ENTITY {
        public static final BlockEntityType<MarkerBlockEntity> MARKER_BLOCK_ENTITY =
            INSTANCE.registerBlockEntity("marker", MarkerBlockEntity::new, BLOCK.MARKER);

        private ENTITY() {}

    }

    public static final class ITEM {
        public static final Item WRENCH = INSTANCE.registerItem("wrench",
            props -> new WrenchItem(props.stacksTo(1)));
        public static final Item PROBE = INSTANCE.registerItem("probe",
            props -> new ProbeItem(props.stacksTo(1)));
        public static final Item WOODEN_GEAR = INSTANCE.registerItem("wooden_gear",
            props -> new Item(props));
        public static final Item STONE_GEAR = INSTANCE.registerItem("stone_gear",
            props -> new Item(props));
        public static final Item COPPER_GEAR = INSTANCE.registerItem("copper_gear",
            props -> new Item(props));
        public static final Item IRON_GEAR = INSTANCE.registerItem("iron_gear",
            props -> new Item(props));
        public static final Item GOLD_GEAR = INSTANCE.registerItem("gold_gear",
            props -> new Item(props));
        public static final Item DIAMOND_GEAR = INSTANCE.registerItem("diamond_gear",
            props -> new Item(props));
        public static final Item NETHERITE_GEAR = INSTANCE.registerItem("netherite_gear",
            props -> new Item(props));

        private ITEM() {}
    }

    public static final class CREATIVE_TAB {
        private CREATIVE_TAB() {}
        private static final List<Consumer<CreativeModeTab.Output>> ENTRIES = new ArrayList<>();

        public static final CreativeModeTab LOGISTICS_TRANSPORT = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                LogisticsMod.getIdentifier("logistics_transport"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemgroup.logistics.transport"))
                        .icon(() -> new ItemStack(LogisticsCore.BLOCK.MARKER))
                        .displayItems((params, entries) -> {
                            for (Consumer<CreativeModeTab.Output> entry : ENTRIES) {
                                entry.accept(entries);
                            }
                        })
                        .build());

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
                ITEM.WOODEN_GEAR,
                ITEM.STONE_GEAR,
                ITEM.COPPER_GEAR,
                ITEM.IRON_GEAR,
                ITEM.GOLD_GEAR,
                ITEM.DIAMOND_GEAR,
                ITEM.NETHERITE_GEAR,
                BLOCK.MARKER
        );
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

package com.logistics;

import com.logistics.api.LogisticsApi;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeApi;
import com.logistics.pipe.PipeTypes;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.data.PipeDataComponents.WeatheringState;
import com.logistics.pipe.item.ModularPipeBlockItem;
import com.logistics.pipe.ui.ItemFilterScreenHandler;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LogisticsPipe extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsPipe INSTANCE = new LogisticsPipe();
    private static final Map<Item, DyeColor> MARKING_FLUID_ITEM_COLORS = new HashMap<>();
    private static Map<DyeColor, Item> MARKING_FLUID_ITEMS = Collections.emptyMap();

    @Override
    protected String domain() {
        return "pipe";
    }

    public static ResourceLocation identifier(String name) {
        return INSTANCE.getDomainResourceLocation(name);
    }

    public static ResourceLocation blockModelIdentifier(String name) {
        return INSTANCE.getBlockModelResourceLocation(name);
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        BLOCK.register();
        ENTITY.register();
        DATA.register();
        SCREEN.register();
        registerMarkingFluidItems();

        registerLegacyAliases();
        addCreativeTabEntries();

        LogisticsApi.Registry.transport(new PipeApi());
    }

    private static void addCreativeTabEntries() {
        // Marking fluids
        LogisticsCore.CREATIVE_TAB.add(entries -> {
            for (DyeColor color : DyeColor.values()) {
                entries.accept(getMarkingFluidItem(color));
            }
        });

        // Copper pipe variants (modular)
        LogisticsCore.CREATIVE_TAB.add(entries -> {
            if (BLOCK.COPPER_TRANSPORT_PIPE instanceof PipeBlock pipeBlock) {
                Pipe pipe = pipeBlock.getPipe();
                if (pipe != null) {
                    ItemStack baseStack = new ItemStack(BLOCK.COPPER_TRANSPORT_PIPE);
                    List<ItemStack> variants = new ArrayList<>();
                    pipe.appendCreativeMenuVariants(variants, baseStack);
                    variants.forEach(entries::accept);
                }
            }
        });

        // Pipes
        LogisticsCore.CREATIVE_TAB.addItems(
                BLOCK.STONE_TRANSPORT_PIPE,
                BLOCK.ITEM_PASSTHROUGH_PIPE,
                BLOCK.COPPER_TRANSPORT_PIPE,
                BLOCK.ITEM_EXTRACTOR_PIPE,
                BLOCK.ITEM_MERGER_PIPE,
                BLOCK.GOLD_TRANSPORT_PIPE,
                BLOCK.ITEM_FILTER_PIPE,
                BLOCK.ITEM_INSERTION_PIPE,
                BLOCK.ITEM_VOID_PIPE
        );
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static Block STONE_TRANSPORT_PIPE;
        public static Block ITEM_PASSTHROUGH_PIPE;
        public static Block COPPER_TRANSPORT_PIPE;
        public static Block ITEM_EXTRACTOR_PIPE;
        public static Block ITEM_MERGER_PIPE;
        public static Block GOLD_TRANSPORT_PIPE;
        public static Block ITEM_FILTER_PIPE;
        public static Block ITEM_INSERTION_PIPE;
        public static Block ITEM_VOID_PIPE;

        static void register() {
            STONE_TRANSPORT_PIPE = INSTANCE.registerBlockWithItem("stone_transport_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.STONE_TRANSPORT_PIPE));
            ITEM_PASSTHROUGH_PIPE = INSTANCE.registerBlockWithItem("item_passthrough_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.ITEM_PASSTHROUGH_PIPE));
            COPPER_TRANSPORT_PIPE = INSTANCE.registerBlockWithItem("copper_transport_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.COPPER_TRANSPORT_PIPE),
                ModularPipeBlockItem::new);
            ITEM_EXTRACTOR_PIPE = INSTANCE.registerBlockWithItem("item_extractor_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.ITEM_EXTRACTOR));
            ITEM_MERGER_PIPE = INSTANCE.registerBlockWithItem("item_merger_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.ITEM_MERGER));
            GOLD_TRANSPORT_PIPE = INSTANCE.registerBlockWithItem("gold_transport_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.GOLD_TRANSPORT));
            ITEM_FILTER_PIPE = INSTANCE.registerBlockWithItem("item_filter_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.ITEM_FILTER));
            ITEM_INSERTION_PIPE = INSTANCE.registerBlockWithItem("item_insertion_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.ITEM_INSERTION));
            ITEM_VOID_PIPE = INSTANCE.registerBlockWithItem("item_void_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.ITEM_VOID));
        }
    }

    public static final class ENTITY {
        public static BlockEntityType<PipeBlockEntity> PIPE_BLOCK_ENTITY;

        private ENTITY() {}

        static void register() {
            PIPE_BLOCK_ENTITY = INSTANCE.registerBlockEntity("pipe",
                PipeBlockEntity::new,
                BLOCK.STONE_TRANSPORT_PIPE,
                BLOCK.ITEM_EXTRACTOR_PIPE,
                BLOCK.ITEM_MERGER_PIPE,
                BLOCK.GOLD_TRANSPORT_PIPE,
                BLOCK.ITEM_FILTER_PIPE,
                BLOCK.COPPER_TRANSPORT_PIPE,
                BLOCK.ITEM_PASSTHROUGH_PIPE,
                BLOCK.ITEM_INSERTION_PIPE,
                BLOCK.ITEM_VOID_PIPE);
        }
    }

    public static final class DATA {
        public static DataComponentType<WeatheringState> WEATHERING_STATE;

        private DATA() {}

        static void register() {
            WEATHERING_STATE = Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    LogisticsPipe.identifier("weathering_state"),
                    DataComponentType.<WeatheringState>builder()
                            .persistent(WeatheringState.CODEC)
                            .build());
        }
    }

    public static final class CONFIG {
        // Constant speed added per tick when a pipe applies acceleration (e.g., powered boost pipes).
        // This is a linear delta, not a multiplier, so larger values ramp speed faster each tick.
        // 1/200 blocks per tick^2 means +0.005 blocks/tick after one tick of acceleration.
        public static final float ACCELERATION_RATE = 1.0f / 200.0f;

        // Fraction of the current speed removed per tick when not accelerating.
        // This creates a smooth exponential decay (speed -= speed * DRAG_COEFFICIENT).
        // Tuned so one fully-powered boost segment (starting at ITEM_MIN_SPEED with ACCELERATION_RATE)
        // keeps the item just above ITEM_MIN_SPEED after ~15 more unpowered segments.
        public static final float DRAG_COEFFICIENT = 0.005f;

        // Hard floor for item speed while traveling through pipes.
        // Items will never slow below this, even under drag, so movement doesn't stall.
        public static final float ITEM_MIN_SPEED = 0.02f;

        // Default ceiling for item speed while traveling through pipes.
        // Individual pipes can override this up or down via getMaxSpeed.
        public static final float PIPE_MAX_SPEED = 0.16f;

        private static final int MARKING_FLUID_USES = 16;

        private CONFIG() {}
    }

    public static final class SCREEN {
        public static MenuType<ItemFilterScreenHandler> ITEM_FILTER;

        private SCREEN() {}

        static void register() {
            ITEM_FILTER = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.identifier("item_filter"),
                    new MenuType<>(ItemFilterScreenHandler::new, FeatureFlagSet.of()));
        }
    }

    private static Block.Properties createPipeProperties(Block.Properties props) {
        return props.mapColor(MapColor.NONE)
                .strength(0.25f)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    private static void registerMarkingFluidItems() {
        Map<DyeColor, Item> items = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            String name = "marking_fluid_" + color.getName();
            Item item = INSTANCE.registerItem(name, props ->
                new Item(props.stacksTo(1).durability(CONFIG.MARKING_FLUID_USES)));
            items.put(color, item);
            MARKING_FLUID_ITEM_COLORS.put(item, color);
        }
        MARKING_FLUID_ITEMS = Collections.unmodifiableMap(items);
    }

    public static Item getMarkingFluidItem(DyeColor color) {
        return MARKING_FLUID_ITEMS.get(color);
    }

    public static @Nullable DyeColor getMarkingFluidColor(ItemStack stack) {
        return MARKING_FLUID_ITEM_COLORS.get(stack.getItem());
    }

    private void registerLegacyAliases() {
        // v0.2 => v0.3
        registerBlockEntityAlias("pipe", ENTITY.PIPE_BLOCK_ENTITY);

        for (DyeColor color : DyeColor.values()) {
            String name = "marking_fluid_" + color.getName();
            registerItemAlias(name, MARKING_FLUID_ITEMS.get(color));
        }

        ResourceLocation newMenuId = BuiltInRegistries.MENU.getKey(SCREEN.ITEM_FILTER);
        if (newMenuId != null) {
            BuiltInRegistries.MENU.addAlias(
                    LogisticsMod.getResourceLocation("item_filter"),
                    newMenuId);
        }

        ResourceLocation newDataId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(DATA.WEATHERING_STATE);
        if (newDataId != null) {
            BuiltInRegistries.DATA_COMPONENT_TYPE.addAlias(
                    LogisticsMod.getResourceLocation("weathering_state"),
                    newDataId);
        }
    }
}

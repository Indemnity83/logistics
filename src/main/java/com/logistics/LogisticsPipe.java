package com.logistics;

import com.logistics.api.LogisticsApi;
import com.logistics.core.DebugLog;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.modules.*;
import com.logistics.pipe.modules.Module;
import com.logistics.pipe.network.NetDbg;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeApi;
import com.logistics.pipe.PipeTypes;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.data.PipeDataComponents.WeatheringState;
import com.logistics.pipe.item.ModularPipeBlockItem;
import com.logistics.pipe.item.ModuleItem;
import com.logistics.pipe.ui.ItemFilterScreenHandler;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
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

    public static ResourceId resource(String name) {
        return INSTANCE.domainResource(name);
    }

    public static ResourceId model(String name) {
        return INSTANCE.domainModelResource(name);
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        DebugLog.register(NetDbg.DOMAIN);

        BLOCK.register();
        ENTITY.register();
        ITEM.register();
        DATA.register();
        SCREEN.register();
        registerMarkingFluidItems();
        registerNetworkPackets();

        registerLegacyAliases();
        addCreativeTabEntries();

        LogisticsApi.Registry.transport(new PipeApi());
    }

    private static void registerNetworkPackets() {
        com.logistics.pipe.network.RequestItemPacket.register();
        com.logistics.pipe.network.SyncRequesterInventoryPacket.register();
        com.logistics.pipe.network.OpenChassisSlotPacket.register();
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
                BLOCK.ITEM_VOID_PIPE,
                BLOCK.BASIC_LOGISTICS_PIPE,
                BLOCK.PROVIDER_LOGISTICS_PIPE,
                BLOCK.REQUESTER_LOGISTICS_PIPE,
                BLOCK.SUPPLIER_LOGISTICS_PIPE,
                BLOCK.CRAFTING_LOGISTICS_PIPE,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK1,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK2,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK3,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK4,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK5,
                ITEM.BLANK_MODULE,
                ITEM.ITEM_SINK_MODULE,
                ITEM.POLYMORPHIC_SINK_MODULE,
                ITEM.ENCHANTMENT_SINK_MODULE,
                ITEM.MOD_ITEM_SINK_MODULE,
                ITEM.PASSIVE_SUPPLIER_MODULE,
                ITEM.ACTIVE_SUPPLIER_MODULE,
                ITEM.PROVIDER_MODULE,
                ITEM.PROVIDER_MODULE_MKII,
                ITEM.EXTRACTOR_MODULE,
                ITEM.EXTRACTOR_MODULE_MKII,
                ITEM.EXTRACTOR_MODULE_MKIII,
                ITEM.ADVANCED_EXTRACTOR_MODULE,
                ITEM.ADVANCED_EXTRACTOR_MODULE_MKII,
                ITEM.ADVANCED_EXTRACTOR_MODULE_MKIII,
                ITEM.CRAFTER_MODULE,
                ITEM.CRAFTER_MODULE_MKII,
                ITEM.CRAFTER_MODULE_MKIII,
                ITEM.QUICKSORT_MODULE,
                ITEM.TERMINUS_MODULE
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
        public static Block BASIC_LOGISTICS_PIPE;
        public static Block PROVIDER_LOGISTICS_PIPE;
        public static Block REQUESTER_LOGISTICS_PIPE;
        public static Block SUPPLIER_LOGISTICS_PIPE;
        public static Block CRAFTING_LOGISTICS_PIPE;
        public static Block CHASSIS_LOGISTICS_PIPE_MK1;
        public static Block CHASSIS_LOGISTICS_PIPE_MK2;
        public static Block CHASSIS_LOGISTICS_PIPE_MK3;
        public static Block CHASSIS_LOGISTICS_PIPE_MK4;
        public static Block CHASSIS_LOGISTICS_PIPE_MK5;

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
            BASIC_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("basic_logistics_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.BASIC_LOGISTICS_PIPE));
            PROVIDER_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("provider_logistics_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.PROVIDER_LOGISTICS_PIPE));
            REQUESTER_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("requester_logistics_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.REQUESTER_LOGISTICS_PIPE));
            SUPPLIER_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("supplier_logistics_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.SUPPLIER_LOGISTICS_PIPE));
            CRAFTING_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("crafting_logistics_pipe",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.CRAFTING_LOGISTICS_PIPE));
            CHASSIS_LOGISTICS_PIPE_MK1 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk1",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK1));
            CHASSIS_LOGISTICS_PIPE_MK2 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk2",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK2));
            CHASSIS_LOGISTICS_PIPE_MK3 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk3",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK3));
            CHASSIS_LOGISTICS_PIPE_MK4 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk4",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK4));
            CHASSIS_LOGISTICS_PIPE_MK5 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk5",
                props -> new PipeBlock(createPipeProperties(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK5));
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
                BLOCK.ITEM_VOID_PIPE,
                BLOCK.BASIC_LOGISTICS_PIPE,
                BLOCK.PROVIDER_LOGISTICS_PIPE,
                BLOCK.REQUESTER_LOGISTICS_PIPE,
                BLOCK.SUPPLIER_LOGISTICS_PIPE,
                BLOCK.CRAFTING_LOGISTICS_PIPE,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK1,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK2,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK3,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK4,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK5);
        }
    }

    public static final class ITEM {
        private ITEM() {}

        public static Item BLANK_MODULE;
        public static Item ITEM_SINK_MODULE;
        public static Item POLYMORPHIC_SINK_MODULE;
        public static Item ENCHANTMENT_SINK_MODULE;
        public static Item MOD_ITEM_SINK_MODULE;
        public static Item PASSIVE_SUPPLIER_MODULE;
        public static Item ACTIVE_SUPPLIER_MODULE;
        public static Item PROVIDER_MODULE;
        public static Item PROVIDER_MODULE_MKII;
        public static Item EXTRACTOR_MODULE;
        public static Item EXTRACTOR_MODULE_MKII;
        public static Item EXTRACTOR_MODULE_MKIII;
        public static Item ADVANCED_EXTRACTOR_MODULE;
        public static Item ADVANCED_EXTRACTOR_MODULE_MKII;
        public static Item ADVANCED_EXTRACTOR_MODULE_MKIII;
        public static Item CRAFTER_MODULE;
        public static Item CRAFTER_MODULE_MKII;
        public static Item CRAFTER_MODULE_MKIII;
        public static Item QUICKSORT_MODULE;
        public static Item TERMINUS_MODULE;

        static void register() {
            BLANK_MODULE = INSTANCE.registerItem("blank_module", Item::new);
            ITEM_SINK_MODULE = INSTANCE.registerItem("item_sink_module",
                    props -> new ModuleItem(props, () -> new SinkModule(7)));
            POLYMORPHIC_SINK_MODULE = INSTANCE.registerItem("polymorphic_sink_module",
                    props -> new ModuleItem(props, () -> new PolymorphicSinkModule(7)));
            ENCHANTMENT_SINK_MODULE = INSTANCE.registerItem("enchantment_sink_module",
                    props -> new ModuleItem(props, () -> new EnchantmentSinkModule(3)));
            MOD_ITEM_SINK_MODULE = INSTANCE.registerItem("mod_item_sink_module",
                    props -> new ModuleItem(props, () -> new Module() {}));
            PASSIVE_SUPPLIER_MODULE = INSTANCE.registerItem("passive_supplier_module",
                    props -> new ModuleItem(props, () -> new PassiveSupplierModule(8)));
            ACTIVE_SUPPLIER_MODULE = INSTANCE.registerItem("active_supplier_module",
                    props -> new ModuleItem(props, SupplierModule::new));
            PROVIDER_MODULE = INSTANCE.registerItem("provider_module",
                    props -> new ModuleItem(props, () -> new ProviderModule(8, 1)));
            PROVIDER_MODULE_MKII = INSTANCE.registerItem("provider_mkii_module",
                    props -> new ModuleItem(props, () -> new ProviderModule(64, 4)));
            EXTRACTOR_MODULE = INSTANCE.registerItem("extractor_module",
                    props -> new ModuleItem(props, () -> new BasicExtractorModule(1, 100)));
            EXTRACTOR_MODULE_MKII = INSTANCE.registerItem("extractor_module_mkii",
                    props -> new ModuleItem(props, () -> new BasicExtractorModule(1, 20)));
            EXTRACTOR_MODULE_MKIII = INSTANCE.registerItem("extractor_module_mkiii",
                    props -> new ModuleItem(props, () -> new BasicExtractorModule(64, 1)));
            ADVANCED_EXTRACTOR_MODULE = INSTANCE.registerItem("advanced_extractor_module",
                    props -> new ModuleItem(props, () -> new AdvancedExtractorModule(1, 100)));
            ADVANCED_EXTRACTOR_MODULE_MKII = INSTANCE.registerItem("advanced_extractor_mkii_module",
                    props -> new ModuleItem(props, () -> new AdvancedExtractorModule(1, 20)));
            ADVANCED_EXTRACTOR_MODULE_MKIII = INSTANCE.registerItem("advanced_extractor_mkiii_module",
                    props -> new ModuleItem(props, () -> new AdvancedExtractorModule(64, 1)));
            CRAFTER_MODULE = INSTANCE.registerItem("crafter_module",
                    props -> new ModuleItem(props, () -> new CraftingModule(1, 1)));
            CRAFTER_MODULE_MKII = INSTANCE.registerItem("crafter_mkii_module",
                    props -> new ModuleItem(props, () -> new CraftingModule(16, 1)));
            CRAFTER_MODULE_MKIII = INSTANCE.registerItem("crafter_mkiii_module",
                    props -> new ModuleItem(props, () -> new CraftingModule(64, 4)));
            QUICKSORT_MODULE = INSTANCE.registerItem("quicksort_module",
                    props -> new ModuleItem(props, QuickSortModule::new));
            TERMINUS_MODULE = INSTANCE.registerItem("terminus_module",
                    props -> new ModuleItem(props, () -> new TerminusModule(4)));
        }
    }

    public static final class DATA {
        public static DataComponentType<WeatheringState> WEATHERING_STATE;

        private DATA() {}

        static void register() {
            WEATHERING_STATE = Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    LogisticsPipe.resource("weathering_state").toIdentifier(),
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

        // Initial speed for items injected into the network by provider and crafting pipes.
        // Matches LP's effective routed injection speed (PIPE_NORMAL_SPEED × 20 boost = 0.2f).
        public static final float ITEM_NETWORK_SPEED = 0.2f;

        // Timeout for pending requests in supplier/requester pipes (in ticks).
        // If items don't arrive within this time, the pending request is cleared and assumed lost.
        // 6000 ticks = 5 minutes. Increase if items take longer to travel through large networks.
        public static final int ORDER_TTL = 6000;

        private static final int MARKING_FLUID_USES = 16;

        private CONFIG() {}
    }

    public static final class SCREEN {
        public static MenuType<ItemFilterScreenHandler> ITEM_FILTER;
        public static MenuType<com.logistics.pipe.ui.RequesterScreenHandler> REQUESTER;
        public static MenuType<com.logistics.pipe.ui.SupplierScreenHandler> SUPPLIER;
        public static MenuType<com.logistics.pipe.ui.ProviderScreenHandler> PROVIDER;
        public static MenuType<com.logistics.pipe.ui.SinkScreenHandler> SINK;
        public static MenuType<com.logistics.pipe.ui.CraftingScreenHandler> CRAFTING;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK1;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK2;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK3;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK4;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK5;
        public static MenuType<com.logistics.pipe.ui.AdvancedExtractorScreenHandler> ADVANCED_EXTRACTOR;

        private SCREEN() {}

        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> chassisMenuTypeFor(int slotCount) {
            return switch (slotCount) {
                case 1 -> CHASSIS_MK1;
                case 2 -> CHASSIS_MK2;
                case 3 -> CHASSIS_MK3;
                case 4 -> CHASSIS_MK4;
                case 8 -> CHASSIS_MK5;
                default -> throw new IllegalArgumentException("Invalid chassis slot count: " + slotCount);
            };
        }

        static void register() {
            ITEM_FILTER = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("item_filter").toIdentifier(),
                    new MenuType<>(ItemFilterScreenHandler::new, FeatureFlagSet.of()));
            REQUESTER = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("requester").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.RequesterScreenHandler::new, FeatureFlagSet.of()));
            SUPPLIER = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("supplier").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.SupplierScreenHandler::new, FeatureFlagSet.of()));
            PROVIDER = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("provider").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.ProviderScreenHandler::new, FeatureFlagSet.of()));
            SINK = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("sink").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.SinkScreenHandler::new, FeatureFlagSet.of()));
            CRAFTING = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("crafting").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.CraftingScreenHandler::new, FeatureFlagSet.of()));
            CHASSIS_MK1 = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("chassis_mk1").toIdentifier(),
                    new MenuType<>((syncId, inv) -> new com.logistics.pipe.ui.ChassisScreenHandler(syncId, inv, 1), FeatureFlagSet.of()));
            CHASSIS_MK2 = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("chassis_mk2").toIdentifier(),
                    new MenuType<>((syncId, inv) -> new com.logistics.pipe.ui.ChassisScreenHandler(syncId, inv, 2), FeatureFlagSet.of()));
            CHASSIS_MK3 = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("chassis_mk3").toIdentifier(),
                    new MenuType<>((syncId, inv) -> new com.logistics.pipe.ui.ChassisScreenHandler(syncId, inv, 3), FeatureFlagSet.of()));
            CHASSIS_MK4 = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("chassis_mk4").toIdentifier(),
                    new MenuType<>((syncId, inv) -> new com.logistics.pipe.ui.ChassisScreenHandler(syncId, inv, 4), FeatureFlagSet.of()));
            CHASSIS_MK5 = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("chassis_mk5").toIdentifier(),
                    new MenuType<>((syncId, inv) -> new com.logistics.pipe.ui.ChassisScreenHandler(syncId, inv, 8), FeatureFlagSet.of()));
            ADVANCED_EXTRACTOR = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("advanced_extractor").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.AdvancedExtractorScreenHandler::new, FeatureFlagSet.of()));
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

        var newMenuId = BuiltInRegistries.MENU.getKey(SCREEN.ITEM_FILTER);
        if (newMenuId != null) {
            BuiltInRegistries.MENU.addAlias(
                    LogisticsMod.modId("item_filter").toIdentifier(),
                    newMenuId);
        }

        var newDataId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(DATA.WEATHERING_STATE);
        if (newDataId != null) {
            BuiltInRegistries.DATA_COMPONENT_TYPE.addAlias(
                    LogisticsMod.modId("weathering_state").toIdentifier(),
                    newDataId);
        }
    }
}

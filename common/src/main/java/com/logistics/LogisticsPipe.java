package com.logistics;

import com.logistics.api.LogisticsApi;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.platform.PlatformService;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.modules.*;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeApi;
import com.logistics.pipe.PipeTypes;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.data.PipeDataComponents.WeatheringState;
import com.logistics.pipe.item.MarkingFluidItem;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class LogisticsPipe extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsPipe INSTANCE = new LogisticsPipe();

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

        BLOCK.register();
        ITEM.register();
        ENTITY.register();
        DATA.register();
        SCREEN.register();
        CREATIVE.register();
        ALIAS.register();

        LogisticsApi.Registry.transport(new PipeApi());

        // Fluid is part of the pipe domain (so fluid pipes can compose pipe modules); register it here
        // rather than as an independent DomainBootstrap.
        LogisticsFluid.registerCommon();
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
        public static Block PROCESS_LOGISTICS_PIPE;
        public static Block SATELLITE_LOGISTICS_PIPE;
        public static Block CHASSIS_LOGISTICS_PIPE_MK1;
        public static Block CHASSIS_LOGISTICS_PIPE_MK2;
        public static Block CHASSIS_LOGISTICS_PIPE_MK3;
        public static Block CHASSIS_LOGISTICS_PIPE_MK4;
        public static Block CHASSIS_LOGISTICS_PIPE_MK5;

        private static Block.Properties pipeProps(Block.Properties props) {
            return props.mapColor(MapColor.NONE)
                    .strength(0.25f)
                    .sound(SoundType.METAL)
                    .noOcclusion();
        }

        static void register() {
            STONE_TRANSPORT_PIPE = INSTANCE.registerBlockWithItem("stone_transport_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.STONE_TRANSPORT_PIPE));
            ITEM_PASSTHROUGH_PIPE = INSTANCE.registerBlockWithItem("item_passthrough_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.ITEM_PASSTHROUGH_PIPE));
            COPPER_TRANSPORT_PIPE = INSTANCE.registerBlockWithItem("copper_transport_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.COPPER_TRANSPORT_PIPE),
                ModularPipeBlockItem::new);
            ITEM_EXTRACTOR_PIPE = INSTANCE.registerBlockWithItem("item_extractor_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.ITEM_EXTRACTOR));
            ITEM_MERGER_PIPE = INSTANCE.registerBlockWithItem("item_merger_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.ITEM_MERGER));
            GOLD_TRANSPORT_PIPE = INSTANCE.registerBlockWithItem("gold_transport_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.GOLD_TRANSPORT));
            ITEM_FILTER_PIPE = INSTANCE.registerBlockWithItem("item_filter_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.ITEM_FILTER));
            ITEM_INSERTION_PIPE = INSTANCE.registerBlockWithItem("item_insertion_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.ITEM_INSERTION));
            ITEM_VOID_PIPE = INSTANCE.registerBlockWithItem("item_void_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.ITEM_VOID));
            BASIC_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("basic_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.BASIC_LOGISTICS_PIPE));
            PROVIDER_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("provider_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.PROVIDER_LOGISTICS_PIPE));
            REQUESTER_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("requester_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.REQUESTER_LOGISTICS_PIPE));
            SUPPLIER_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("supplier_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.SUPPLIER_LOGISTICS_PIPE));
            CRAFTING_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("crafting_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.CRAFTING_LOGISTICS_PIPE));
            PROCESS_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("process_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.PROCESS_LOGISTICS_PIPE));
            SATELLITE_LOGISTICS_PIPE = INSTANCE.registerBlockWithItem("satellite_logistics_pipe",
                props -> new PipeBlock(pipeProps(props), PipeTypes.SATELLITE_LOGISTICS_PIPE));
            CHASSIS_LOGISTICS_PIPE_MK1 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk1",
                props -> new PipeBlock(pipeProps(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK1));
            CHASSIS_LOGISTICS_PIPE_MK2 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk2",
                props -> new PipeBlock(pipeProps(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK2));
            CHASSIS_LOGISTICS_PIPE_MK3 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk3",
                props -> new PipeBlock(pipeProps(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK3));
            CHASSIS_LOGISTICS_PIPE_MK4 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk4",
                props -> new PipeBlock(pipeProps(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK4));
            CHASSIS_LOGISTICS_PIPE_MK5 = INSTANCE.registerBlockWithItem("chassis_logistics_pipe_mk5",
                props -> new PipeBlock(pipeProps(props), PipeTypes.CHASSIS_LOGISTICS_PIPE_MK5));
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
        public static Item CRAFTER_MODULE;
        public static Item CRAFTER_MODULE_MKII;
        public static Item CRAFTER_MODULE_MKIII;
        public static Item QUICKSORT_MODULE;
        public static Item TERMINUS_MODULE;

        public static Item WHITE_MARKING_FLUID;
        public static Item ORANGE_MARKING_FLUID;
        public static Item MAGENTA_MARKING_FLUID;
        public static Item LIGHT_BLUE_MARKING_FLUID;
        public static Item YELLOW_MARKING_FLUID;
        public static Item LIME_MARKING_FLUID;
        public static Item PINK_MARKING_FLUID;
        public static Item GRAY_MARKING_FLUID;
        public static Item LIGHT_GRAY_MARKING_FLUID;
        public static Item CYAN_MARKING_FLUID;
        public static Item PURPLE_MARKING_FLUID;
        public static Item BLUE_MARKING_FLUID;
        public static Item BROWN_MARKING_FLUID;
        public static Item GREEN_MARKING_FLUID;
        public static Item RED_MARKING_FLUID;
        public static Item BLACK_MARKING_FLUID;

        // Forward map used by CREATIVE and ALIAS for iteration.
        private static final Map<DyeColor, Item> MARKING_FLUIDS_BY_COLOR = new EnumMap<>(DyeColor.class);

        static void register() {
            BLANK_MODULE = INSTANCE.registerItem("blank_module", Item::new);
            ITEM_SINK_MODULE = INSTANCE.registerItem("item_sink_module",
                    props -> new ModuleItem(props, () -> new SinkModule(7)));
            POLYMORPHIC_SINK_MODULE = INSTANCE.registerItem("polymorphic_sink_module",
                    props -> new ModuleItem(props, () -> new PolymorphicSinkModule(7)));
            ENCHANTMENT_SINK_MODULE = INSTANCE.registerItem("enchantment_sink_module",
                    props -> new ModuleItem(props, () -> new EnchantmentSinkModule(3)));
            MOD_ITEM_SINK_MODULE = INSTANCE.registerItem("mod_item_sink_module",
                    props -> new ModuleItem(props, () -> new ModSinkModule(5)));
            PASSIVE_SUPPLIER_MODULE = INSTANCE.registerItem("passive_supplier_module",
                    props -> new ModuleItem(props, () -> new PassiveSupplierModule(8)));
            ACTIVE_SUPPLIER_MODULE = INSTANCE.registerItem("active_supplier_module",
                    props -> new ModuleItem(props, SupplierModule::new));
            PROVIDER_MODULE = INSTANCE.registerItem("provider_module",
                    props -> new ModuleItem(props, () -> new ProviderModule(8, 1)));
            PROVIDER_MODULE_MKII = INSTANCE.registerItem("provider_mkii_module",
                    props -> new ModuleItem(props, () -> new ProviderModule(64, 4)));
            EXTRACTOR_MODULE = INSTANCE.registerItem("extractor_module",
                    props -> new ModuleItem(props, () -> new BasicExtractorModule(8, 80)));
            EXTRACTOR_MODULE_MKII = INSTANCE.registerItem("extractor_module_mkii",
                    props -> new ModuleItem(props, () -> new BasicExtractorModule(8, 20)));
            EXTRACTOR_MODULE_MKIII = INSTANCE.registerItem("extractor_module_mkiii",
                    props -> new ModuleItem(props, () -> new AdvancedExtractorModule(8, 20)));
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

            WHITE_MARKING_FLUID = markingFluid(DyeColor.WHITE);
            ORANGE_MARKING_FLUID = markingFluid(DyeColor.ORANGE);
            MAGENTA_MARKING_FLUID = markingFluid(DyeColor.MAGENTA);
            LIGHT_BLUE_MARKING_FLUID = markingFluid(DyeColor.LIGHT_BLUE);
            YELLOW_MARKING_FLUID = markingFluid(DyeColor.YELLOW);
            LIME_MARKING_FLUID = markingFluid(DyeColor.LIME);
            PINK_MARKING_FLUID = markingFluid(DyeColor.PINK);
            GRAY_MARKING_FLUID = markingFluid(DyeColor.GRAY);
            LIGHT_GRAY_MARKING_FLUID = markingFluid(DyeColor.LIGHT_GRAY);
            CYAN_MARKING_FLUID = markingFluid(DyeColor.CYAN);
            PURPLE_MARKING_FLUID = markingFluid(DyeColor.PURPLE);
            BLUE_MARKING_FLUID = markingFluid(DyeColor.BLUE);
            BROWN_MARKING_FLUID = markingFluid(DyeColor.BROWN);
            GREEN_MARKING_FLUID = markingFluid(DyeColor.GREEN);
            RED_MARKING_FLUID = markingFluid(DyeColor.RED);
            BLACK_MARKING_FLUID = markingFluid(DyeColor.BLACK);
        }

        private static Item markingFluid(DyeColor color) {
            Item item = INSTANCE.registerItem(color.getName() + "_marking_fluid",
                    props -> new MarkingFluidItem(props.stacksTo(1).durability(16), color));
            MARKING_FLUIDS_BY_COLOR.put(color, item);
            return item;
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
                BLOCK.PROCESS_LOGISTICS_PIPE,
                BLOCK.SATELLITE_LOGISTICS_PIPE,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK1,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK2,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK3,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK4,
                BLOCK.CHASSIS_LOGISTICS_PIPE_MK5);
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

    public static final class SCREEN {
        public static MenuType<ItemFilterScreenHandler> ITEM_FILTER;
        public static MenuType<com.logistics.pipe.ui.RequesterScreenHandler> REQUESTER;
        public static MenuType<com.logistics.pipe.ui.SupplierScreenHandler> SUPPLIER;
        public static MenuType<com.logistics.pipe.ui.ProviderScreenHandler> PROVIDER;
        public static MenuType<com.logistics.pipe.ui.SinkScreenHandler> SINK;
        public static MenuType<com.logistics.pipe.ui.CraftingScreenHandler> CRAFTING;
        public static MenuType<com.logistics.pipe.ui.ProcessScreenHandler> PROCESS;
        public static MenuType<com.logistics.pipe.ui.SatelliteScreenHandler> SATELLITE;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK1;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK2;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK3;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK4;
        public static MenuType<com.logistics.pipe.ui.ChassisScreenHandler> CHASSIS_MK5;
        public static MenuType<com.logistics.pipe.ui.AdvancedExtractorScreenHandler> ADVANCED_EXTRACTOR;
        public static MenuType<com.logistics.pipe.ui.ModSinkScreenHandler> MOD_SINK;

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
            PROCESS = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("process").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.ProcessScreenHandler::new, FeatureFlagSet.of()));
            SATELLITE = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("satellite").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.SatelliteScreenHandler::new, FeatureFlagSet.of()));
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
            MOD_SINK = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPipe.resource("mod_sink").toIdentifier(),
                    new MenuType<>(com.logistics.pipe.ui.ModSinkScreenHandler::new, FeatureFlagSet.of()));
        }
    }

    public static final class CREATIVE {
        private CREATIVE() {}

        static void register() {
            // Marking fluids
            LogisticsCore.CREATIVE.TAB.add(entries -> {
                for (Item fluid : ITEM.MARKING_FLUIDS_BY_COLOR.values()) {
                    entries.accept(fluid);
                }
            });

            // Copper pipe variants (modular)
            LogisticsCore.CREATIVE.TAB.add(entries -> {
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
            LogisticsCore.CREATIVE.TAB.add(BLOCK.STONE_TRANSPORT_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.ITEM_PASSTHROUGH_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.COPPER_TRANSPORT_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.ITEM_EXTRACTOR_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.ITEM_MERGER_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.GOLD_TRANSPORT_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.ITEM_FILTER_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.ITEM_INSERTION_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.ITEM_VOID_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.BASIC_LOGISTICS_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.PROVIDER_LOGISTICS_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.REQUESTER_LOGISTICS_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.SUPPLIER_LOGISTICS_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.CRAFTING_LOGISTICS_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.PROCESS_LOGISTICS_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.SATELLITE_LOGISTICS_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.CHASSIS_LOGISTICS_PIPE_MK1);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.CHASSIS_LOGISTICS_PIPE_MK2);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.CHASSIS_LOGISTICS_PIPE_MK3);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.CHASSIS_LOGISTICS_PIPE_MK4);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.CHASSIS_LOGISTICS_PIPE_MK5);
            LogisticsCore.CREATIVE.TAB.add(ITEM.BLANK_MODULE);
            LogisticsCore.CREATIVE.TAB.add(ITEM.ITEM_SINK_MODULE);
            LogisticsCore.CREATIVE.TAB.add(ITEM.POLYMORPHIC_SINK_MODULE);
            LogisticsCore.CREATIVE.TAB.add(ITEM.ENCHANTMENT_SINK_MODULE);
            LogisticsCore.CREATIVE.TAB.add(ITEM.MOD_ITEM_SINK_MODULE);
            LogisticsCore.CREATIVE.TAB.add(ITEM.PASSIVE_SUPPLIER_MODULE);
            LogisticsCore.CREATIVE.TAB.add(ITEM.ACTIVE_SUPPLIER_MODULE);
            LogisticsCore.CREATIVE.TAB.add(ITEM.PROVIDER_MODULE);
            LogisticsCore.CREATIVE.TAB.add(ITEM.PROVIDER_MODULE_MKII);
            LogisticsCore.CREATIVE.TAB.add(ITEM.EXTRACTOR_MODULE);
            LogisticsCore.CREATIVE.TAB.add(ITEM.EXTRACTOR_MODULE_MKII);
            LogisticsCore.CREATIVE.TAB.add(ITEM.EXTRACTOR_MODULE_MKIII);
            LogisticsCore.CREATIVE.TAB.add(ITEM.CRAFTER_MODULE);
            LogisticsCore.CREATIVE.TAB.add(ITEM.CRAFTER_MODULE_MKII);
            LogisticsCore.CREATIVE.TAB.add(ITEM.CRAFTER_MODULE_MKIII);
            LogisticsCore.CREATIVE.TAB.add(ITEM.QUICKSORT_MODULE);
            LogisticsCore.CREATIVE.TAB.add(ITEM.TERMINUS_MODULE);
        }
    }

    public static final class ALIAS {
        private ALIAS() {}

        static void register() {
            // v0.2 => v0.3
            INSTANCE.registerBlockEntityAlias("pipe", ENTITY.PIPE_BLOCK_ENTITY);

            for (Map.Entry<DyeColor, Item> entry : ITEM.MARKING_FLUIDS_BY_COLOR.entrySet()) {
                INSTANCE.registerItemAlias("marking_fluid_" + entry.getKey().getName(), entry.getValue());
            }

            // v0.4.0 => v0.4.1 (marking_fluid_<color> => <color>_marking_fluid)
            for (Map.Entry<DyeColor, Item> entry : ITEM.MARKING_FLUIDS_BY_COLOR.entrySet()) {
                INSTANCE.registerItemAlias("pipe/marking_fluid_" + entry.getKey().getName(), entry.getValue());
            }

            PlatformService.INSTANCE.registerAlias(
                    BuiltInRegistries.MENU,
                    LogisticsMod.modId("item_filter"),
                    SCREEN.ITEM_FILTER);
            PlatformService.INSTANCE.registerAlias(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    LogisticsMod.modId("weathering_state"),
                    DATA.WEATHERING_STATE);
        }
    }

}

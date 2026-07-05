package com.logistics;

import com.logistics.core.LogisticsConfig;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.power.block.BatteryBlock;
import com.logistics.power.block.BatteryBlockItem;
import com.logistics.power.block.CreativeSinkBlock;
import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.power.block.entity.CreativeSinkBlockEntity;
import com.logistics.power.cable.CableBlock;
import com.logistics.power.cable.CableBlockEntity;
import com.logistics.power.cable.CableTier;
import com.logistics.power.engine.block.CreativeEngineBlock;
import com.logistics.power.engine.block.RedstoneEngineBlock;
import com.logistics.power.engine.block.StirlingEngineBlock;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class LogisticsPower extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsPower INSTANCE = new LogisticsPower();

    @Override
    protected String domain() {
        return "power";
    }

    public static ResourceId resource(String name) {
        return INSTANCE.domainResource(name);
    }

    public static ResourceId model(String name) {
        return INSTANCE.domainModelResource(name);
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public void registerConfig() {
        LogisticsConfig defaults = new LogisticsConfig();

        LogisticsConfig.reg("redstone_engine_output", "RF generated per 16-tick interval",
                () -> LogisticsConfig.get().engine.redstoneOutput,
                v -> LogisticsConfig.get().engine.redstoneOutput = v,
                Long::parseLong,
                v -> LogisticsConfig.requireMin(v, 0L, "must be greater than or equal to 0"),
                () -> defaults.engine.redstoneOutput);
        LogisticsConfig.regCrossField("stirling_engine_min_output", "Stirling engine minimum RF/t output",
                () -> LogisticsConfig.get().engine.stirlingMinOutput,
                v -> LogisticsConfig.get().engine.stirlingMinOutput = v,
                Double::parseDouble,
                v -> LogisticsConfig.requireFiniteMin(v, 0.0, "must be finite and greater than or equal to 0"),
                () -> defaults.engine.stirlingMinOutput,
                v -> LogisticsConfig.requireCondition(
                        v <= LogisticsConfig.get().engine.stirlingMaxOutput,
                        "must be less than or equal to stirling_engine_max_output"));
        LogisticsConfig.regCrossField("stirling_engine_max_output", "Stirling engine maximum RF/t output",
                () -> LogisticsConfig.get().engine.stirlingMaxOutput,
                v -> LogisticsConfig.get().engine.stirlingMaxOutput = v,
                Double::parseDouble,
                v -> LogisticsConfig.requireFiniteMin(v, 0.0, "must be finite and greater than or equal to 0"),
                () -> defaults.engine.stirlingMaxOutput,
                v -> LogisticsConfig.requireCondition(
                        v >= LogisticsConfig.get().engine.stirlingMinOutput,
                        "must be greater than or equal to stirling_engine_min_output"));
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        BLOCK.register();
        ITEM.register();
        ENTITY.register();
        SCREEN.register();
        CREATIVE.register();
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static Block REDSTONE_ENGINE;
        public static Block STIRLING_ENGINE;
        public static Block CREATIVE_ENGINE;
        public static Block CREATIVE_SINK;
        public static Block BATTERY;
        public static Block COPPER_CABLE;
        public static Block GOLD_CABLE;
        public static Block ENDER_CABLE;

        static void register() {
            REDSTONE_ENGINE = INSTANCE.registerBlockWithItem("redstone_engine",
                props -> new RedstoneEngineBlock(props.strength(5.0f).sound(SoundType.WOOD).noOcclusion()));
            STIRLING_ENGINE = INSTANCE.registerBlockWithItem("stirling_engine",
                props -> new StirlingEngineBlock(props.strength(5.0f).sound(SoundType.COPPER).noOcclusion()));
            CREATIVE_ENGINE = INSTANCE.registerBlockWithItem("creative_engine",
                props -> new CreativeEngineBlock(props.strength(5.0f).sound(SoundType.STONE).noOcclusion()));
            CREATIVE_SINK = INSTANCE.registerBlockWithItem("creative_sink",
                props -> new CreativeSinkBlock(props.strength(5.0f).sound(SoundType.STONE)));
            BATTERY = INSTANCE.registerBlockWithItem("battery",
                props -> new BatteryBlock(props.strength(3.0f).sound(SoundType.METAL)),
                BatteryBlockItem::new);
            COPPER_CABLE = registerCable("copper_cable", CableTier.COPPER, SoundType.COPPER);
            GOLD_CABLE = registerCable("gold_cable", CableTier.GOLD, SoundType.METAL);
            ENDER_CABLE = registerCable("ender_cable", CableTier.ENDER, SoundType.AMETHYST);
        }

        private static Block registerCable(String name, CableTier tier, SoundType soundType) {
            return INSTANCE.registerBlockWithItem(name,
                    props -> new CableBlock(props.strength(1.5f).sound(soundType).noOcclusion().dynamicShape(), tier));
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        public static BlockEntityType<RedstoneEngineBlockEntity> REDSTONE_ENGINE_BLOCK_ENTITY;
        public static BlockEntityType<StirlingEngineBlockEntity> STIRLING_ENGINE_BLOCK_ENTITY;
        public static BlockEntityType<CreativeEngineBlockEntity> CREATIVE_ENGINE_BLOCK_ENTITY;
        public static BlockEntityType<CreativeSinkBlockEntity> CREATIVE_SINK_BLOCK_ENTITY;
        public static BlockEntityType<BatteryBlockEntity> BATTERY_BLOCK_ENTITY;
        public static BlockEntityType<CableBlockEntity> CABLE_BLOCK_ENTITY;

        static void register() {
            REDSTONE_ENGINE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("redstone_engine", RedstoneEngineBlockEntity::new, BLOCK.REDSTONE_ENGINE);
            STIRLING_ENGINE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("stirling_engine", StirlingEngineBlockEntity::new, BLOCK.STIRLING_ENGINE);
            CREATIVE_ENGINE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("creative_engine", CreativeEngineBlockEntity::new, BLOCK.CREATIVE_ENGINE);
            CREATIVE_SINK_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("creative_sink", CreativeSinkBlockEntity::new, BLOCK.CREATIVE_SINK);
            BATTERY_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("battery", BatteryBlockEntity::new, BLOCK.BATTERY);
            CABLE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("cable", CableBlockEntity::new,
                        BLOCK.COPPER_CABLE, BLOCK.GOLD_CABLE, BLOCK.ENDER_CABLE);
        }
    }

    public static final class ITEM {
        private ITEM() {}

        public static Item RUBBER_CHUNK;
        public static Item RUBBER_MIX;

        static void register() {
            RUBBER_CHUNK = INSTANCE.registerItem("rubber_chunk", Item::new);
            RUBBER_MIX = INSTANCE.registerItem("rubber_mix", Item::new);
        }
    }

    public static final class SCREEN {
        private SCREEN() {}

        public static MenuType<StirlingEngineScreenHandler> STIRLING_ENGINE;

        static void register() {
            STIRLING_ENGINE = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPower.resource("stirling_engine").toIdentifier(),
                    new MenuType<>(StirlingEngineScreenHandler::new, FeatureFlagSet.of()));
        }
    }

    public static final class CREATIVE {
        private CREATIVE() {}

        static void register() {
            LogisticsCore.CREATIVE.TAB.add(ITEM.RUBBER_MIX);
            LogisticsCore.CREATIVE.TAB.add(ITEM.RUBBER_CHUNK);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.REDSTONE_ENGINE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.STIRLING_ENGINE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.CREATIVE_ENGINE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.CREATIVE_SINK);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.BATTERY);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.COPPER_CABLE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.GOLD_CABLE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.ENDER_CABLE);
        }
    }
}

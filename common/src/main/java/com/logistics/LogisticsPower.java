package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.power.block.CreativeSinkBlock;
import com.logistics.power.block.entity.CreativeSinkBlockEntity;
import com.logistics.power.engine.block.CreativeEngineBlock;
import com.logistics.power.engine.block.RedstoneEngineBlock;
import com.logistics.power.engine.block.StirlingEngineBlock;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
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
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        BLOCK.register();
        ENTITY.register();
        SCREEN.register();

        addCreativeTabEntries();
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static Block REDSTONE_ENGINE;
        public static Block STIRLING_ENGINE;
        public static Block CREATIVE_ENGINE;
        public static Block CREATIVE_SINK;

        static void register() {
            REDSTONE_ENGINE = INSTANCE.registerBlockWithItem("redstone_engine",
                props -> new RedstoneEngineBlock(props.strength(5.0f).sound(SoundType.WOOD).noOcclusion()));
            STIRLING_ENGINE = INSTANCE.registerBlockWithItem("stirling_engine",
                props -> new StirlingEngineBlock(props.strength(5.0f).sound(SoundType.COPPER).noOcclusion()));
            CREATIVE_ENGINE = INSTANCE.registerBlockWithItem("creative_engine",
                props -> new CreativeEngineBlock(props.strength(5.0f).sound(SoundType.STONE).noOcclusion()));
            CREATIVE_SINK = INSTANCE.registerBlockWithItem("creative_sink",
                props -> new CreativeSinkBlock(props.strength(5.0f).sound(SoundType.STONE)));
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        public static BlockEntityType<RedstoneEngineBlockEntity> REDSTONE_ENGINE_BLOCK_ENTITY;
        public static BlockEntityType<StirlingEngineBlockEntity> STIRLING_ENGINE_BLOCK_ENTITY;
        public static BlockEntityType<CreativeEngineBlockEntity> CREATIVE_ENGINE_BLOCK_ENTITY;
        public static BlockEntityType<CreativeSinkBlockEntity> CREATIVE_SINK_BLOCK_ENTITY;

        static void register() {
            REDSTONE_ENGINE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("redstone_engine", RedstoneEngineBlockEntity::new, BLOCK.REDSTONE_ENGINE);
            STIRLING_ENGINE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("stirling_engine", StirlingEngineBlockEntity::new, BLOCK.STIRLING_ENGINE);
            CREATIVE_ENGINE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("creative_engine", CreativeEngineBlockEntity::new, BLOCK.CREATIVE_ENGINE);
            CREATIVE_SINK_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("creative_sink", CreativeSinkBlockEntity::new, BLOCK.CREATIVE_SINK);
        }
    }

    public static final class SCREEN {
        private SCREEN() {}

        public static MenuType<StirlingEngineScreenHandler> STIRLING_ENGINE;

        static void register() {
            STIRLING_ENGINE = Registry.register(
                    BuiltInRegistries.MENU,
                    LogisticsPower.resource("stirling_engine").toIdentifier(),
                    new ExtendedScreenHandlerType<>(StirlingEngineScreenHandler::new, BlockPos.STREAM_CODEC));
        }
    }

    private static void addCreativeTabEntries() {
        LogisticsCore.LOGISTICS_TAB.add(
                BLOCK.REDSTONE_ENGINE,
                BLOCK.STIRLING_ENGINE,
                BLOCK.CREATIVE_ENGINE,
                BLOCK.CREATIVE_SINK
        );
    }
}

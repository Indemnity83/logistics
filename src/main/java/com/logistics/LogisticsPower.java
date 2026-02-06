package com.logistics;

import com.logistics.api.fabric.TREnergyStorageAdapter;
import com.logistics.core.bootstrap.DomainBootstrap;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import team.reborn.energy.api.EnergyStorage;

public final class LogisticsPower extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsPower INSTANCE = new LogisticsPower();

    @Override
    protected String domain() {
        return "power";
    }

    public static Identifier identifier(String name) {
        return INSTANCE.getDomainIdentifier(name);
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        registerEnergyApi();
        addCreativeTabEntries();
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static final Block REDSTONE_ENGINE = INSTANCE.registerBlockWithItem("redstone_engine",
            props -> new RedstoneEngineBlock(props.strength(5.0f).sound(SoundType.WOOD).noOcclusion()));
        public static final Block STIRLING_ENGINE = INSTANCE.registerBlockWithItem("stirling_engine",
            props -> new StirlingEngineBlock(props.strength(5.0f).sound(SoundType.COPPER).noOcclusion()));
        public static final Block CREATIVE_ENGINE = INSTANCE.registerBlockWithItem("creative_engine",
            props -> new CreativeEngineBlock(props.strength(5.0f).sound(SoundType.STONE).noOcclusion()));
        public static final Block CREATIVE_SINK = INSTANCE.registerBlockWithItem("creative_sink",
            props -> new CreativeSinkBlock(props.strength(5.0f).sound(SoundType.STONE)));
    }

    public static final class ENTITY {
        public static final BlockEntityType<RedstoneEngineBlockEntity> REDSTONE_ENGINE_BLOCK_ENTITY =
            INSTANCE.registerBlockEntity("redstone_engine", RedstoneEngineBlockEntity::new, BLOCK.REDSTONE_ENGINE);
        public static final BlockEntityType<StirlingEngineBlockEntity> STIRLING_ENGINE_BLOCK_ENTITY =
            INSTANCE.registerBlockEntity("stirling_engine", StirlingEngineBlockEntity::new, BLOCK.STIRLING_ENGINE);
        public static final BlockEntityType<CreativeEngineBlockEntity> CREATIVE_ENGINE_BLOCK_ENTITY =
            INSTANCE.registerBlockEntity("creative_engine", CreativeEngineBlockEntity::new, BLOCK.CREATIVE_ENGINE);
        public static final BlockEntityType<CreativeSinkBlockEntity> CREATIVE_SINK_BLOCK_ENTITY =
            INSTANCE.registerBlockEntity("creative_sink", CreativeSinkBlockEntity::new, BLOCK.CREATIVE_SINK);

        private ENTITY() {}
    }

    public static final class SCREEN {
        public static final MenuType<StirlingEngineScreenHandler> STIRLING_ENGINE = Registry.register(
                BuiltInRegistries.MENU,
                LogisticsPower.identifier("stirling_engine"),
                new ExtendedScreenHandlerType<>(StirlingEngineScreenHandler::new, BlockPos.STREAM_CODEC));

        private SCREEN() {}
    }

    private static void addCreativeTabEntries() {
        LogisticsCore.CREATIVE_TAB.addItems(
                BLOCK.REDSTONE_ENGINE,
                BLOCK.STIRLING_ENGINE,
                BLOCK.CREATIVE_ENGINE,
                BLOCK.CREATIVE_SINK
        );
    }

    private static void registerEnergyApi() {
        EnergyStorage.SIDED.registerForBlockEntity(
                (engine, direction) -> engine.isOutputDirection(direction) ? new TREnergyStorageAdapter(engine) : null,
                ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY);

        EnergyStorage.SIDED.registerForBlockEntity(
                (engine, direction) -> engine.isOutputDirection(direction) ? new TREnergyStorageAdapter(engine) : null,
                ENTITY.STIRLING_ENGINE_BLOCK_ENTITY);

        EnergyStorage.SIDED.registerForBlockEntity(
                (engine, direction) -> engine.isOutputDirection(direction) ? new TREnergyStorageAdapter(engine) : null,
                ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY);

        EnergyStorage.SIDED.registerForBlockEntity(
                (sink, direction) -> new TREnergyStorageAdapter(sink),
                ENTITY.CREATIVE_SINK_BLOCK_ENTITY);
    }
}

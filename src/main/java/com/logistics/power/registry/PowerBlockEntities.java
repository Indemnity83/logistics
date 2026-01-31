package com.logistics.power.registry;

import com.logistics.LogisticsMod;
import com.logistics.api.fabric.TREnergyStorageAdapter;
import com.logistics.power.block.entity.CreativeSinkBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import team.reborn.energy.api.EnergyStorage;

public final class PowerBlockEntities {
    private PowerBlockEntities() {}

    public static final BlockEntityType<RedstoneEngineBlockEntity> REDSTONE_ENGINE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(LogisticsMod.MOD_ID, "power/redstone_engine"),
            FabricBlockEntityTypeBuilder.create(RedstoneEngineBlockEntity::new, PowerBlocks.REDSTONE_ENGINE)
                    .build());

    public static final BlockEntityType<StirlingEngineBlockEntity> STIRLING_ENGINE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(LogisticsMod.MOD_ID, "power/stirling_engine"),
            FabricBlockEntityTypeBuilder.create(StirlingEngineBlockEntity::new, PowerBlocks.STIRLING_ENGINE)
                    .build());

    public static final BlockEntityType<CreativeEngineBlockEntity> CREATIVE_ENGINE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(LogisticsMod.MOD_ID, "power/creative_engine"),
            FabricBlockEntityTypeBuilder.create(CreativeEngineBlockEntity::new, PowerBlocks.CREATIVE_ENGINE)
                    .build());

    public static final BlockEntityType<CreativeSinkBlockEntity> CREATIVE_SINK_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(LogisticsMod.MOD_ID, "power/creative_sink"),
            FabricBlockEntityTypeBuilder.create(CreativeSinkBlockEntity::new, PowerBlocks.CREATIVE_SINK)
                    .build());

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering power block entities");

        // Register engines with Team Reborn Energy API for cross-mod compatibility
        // Energy is only exposed from the engine's output face
        EnergyStorage.SIDED.registerForBlockEntity(
                (engine, direction) -> {
                    if (engine.isOutputDirection(direction)) {
                        return new TREnergyStorageAdapter(engine);
                    }
                    return null;
                },
                REDSTONE_ENGINE_BLOCK_ENTITY);

        EnergyStorage.SIDED.registerForBlockEntity(
                (engine, direction) -> {
                    if (engine.isOutputDirection(direction)) {
                        return new TREnergyStorageAdapter(engine);
                    }
                    return null;
                },
                STIRLING_ENGINE_BLOCK_ENTITY);

        EnergyStorage.SIDED.registerForBlockEntity(
                (engine, direction) -> {
                    if (engine.isOutputDirection(direction)) {
                        return new TREnergyStorageAdapter(engine);
                    }
                    return null;
                },
                CREATIVE_ENGINE_BLOCK_ENTITY);

        // Creative Sink - accepts energy from all sides
        EnergyStorage.SIDED.registerForBlockEntity(
                (sink, direction) -> new TREnergyStorageAdapter(sink),
                CREATIVE_SINK_BLOCK_ENTITY);
    }
}

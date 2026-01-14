package com.logistics.block.entity;

import com.logistics.LogisticsMod;
import com.logistics.block.LogisticsBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class LogisticsBlockEntities {
    public static final BlockEntityType<PipeBlockEntity> PIPE_BLOCK_ENTITY = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        Identifier.of(LogisticsMod.MOD_ID, "pipe"),
        FabricBlockEntityTypeBuilder.create(PipeBlockEntity::new,
            LogisticsBlocks.STONE_TRANSPORT_PIPE,
            LogisticsBlocks.ITEM_EXTRACTOR_PIPE,
            LogisticsBlocks.ITEM_MERGER_PIPE,
            LogisticsBlocks.GOLD_TRANSPORT_PIPE,
            LogisticsBlocks.ITEM_FILTER_PIPE,
            LogisticsBlocks.COPPER_TRANSPORT_PIPE,
            LogisticsBlocks.ITEM_SENSOR_PIPE,
            LogisticsBlocks.ITEM_VOID_PIPE,
            LogisticsBlocks.ITEM_PASSTHROUGH_PIPE
        ).build()
    );

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering block entities");
    }
}

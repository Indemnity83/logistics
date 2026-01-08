package com.logistics.block.entity;

import com.logistics.LogisticsMod;
import com.logistics.block.LogisticsBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class LogisticsBlockEntities {
    public static final BlockEntityType<PipeBlockEntity> PIPE_BLOCK_ENTITY = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        Identifier.of(LogisticsMod.MOD_ID, "pipe"),
        BlockEntityType.Builder.create(PipeBlockEntity::new,
            LogisticsBlocks.COBBLESTONE_TRANSPORT_PIPE,
            LogisticsBlocks.STONE_TRANSPORT_PIPE,
            LogisticsBlocks.WOODEN_TRANSPORT_PIPE,
            LogisticsBlocks.IRON_TRANSPORT_PIPE,
            LogisticsBlocks.GOLD_TRANSPORT_PIPE,
            LogisticsBlocks.DIAMOND_TRANSPORT_PIPE,
            LogisticsBlocks.COPPER_TRANSPORT_PIPE,
            LogisticsBlocks.QUARTZ_TRANSPORT_PIPE,
            LogisticsBlocks.VOID_TRANSPORT_PIPE
        ).build()
    );

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering block entities");
    }
}

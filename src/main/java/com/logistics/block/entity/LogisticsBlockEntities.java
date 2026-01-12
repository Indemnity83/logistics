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
//            LogisticsBlocks.COBBLESTONE_TRANSPORT_PIPE,
            LogisticsBlocks.BASIC_SPLITTER_PIPE,
            LogisticsBlocks.BASIC_EXTRACTOR_PIPE,
            LogisticsBlocks.BASIC_MERGER_PIPE,
            LogisticsBlocks.GOLD_TRANSPORT_PIPE,
            LogisticsBlocks.SMART_SPLITTER_PIPE,
            LogisticsBlocks.COPPER_TRANSPORT_PIPE,
            LogisticsBlocks.COMPARATOR_PIPE,
            LogisticsBlocks.VOID_PIPE
        ).build()
    );

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering block entities");
    }
}

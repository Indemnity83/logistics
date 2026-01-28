package com.logistics.pipe.registry;

import com.logistics.LogisticsMod;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class PipeBlockEntities {
    private PipeBlockEntities() {}

    public static final BlockEntityType<PipeBlockEntity> PIPE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(LogisticsMod.MOD_ID, "pipe/pipe"),
            FabricBlockEntityTypeBuilder.create(
                            PipeBlockEntity::new,
                            PipeBlocks.STONE_TRANSPORT_PIPE,
                            PipeBlocks.ITEM_EXTRACTOR_PIPE,
                            PipeBlocks.ITEM_MERGER_PIPE,
                            PipeBlocks.GOLD_TRANSPORT_PIPE,
                            PipeBlocks.ITEM_FILTER_PIPE,
                            PipeBlocks.COPPER_TRANSPORT_PIPE,
                            PipeBlocks.ITEM_VOID_PIPE,
                            PipeBlocks.ITEM_PASSTHROUGH_PIPE,
                            PipeBlocks.ITEM_INSERTION_PIPE,
                            PipeBlocks.ITEM_VOID_PIPE)
                    .build());

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering pipe block entities");
        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        Registries.BLOCK_ENTITY_TYPE.addAlias(
                Identifier.of(LogisticsMod.MOD_ID, "pipe"), Registries.BLOCK_ENTITY_TYPE.getId(PIPE_BLOCK_ENTITY));
    }
}

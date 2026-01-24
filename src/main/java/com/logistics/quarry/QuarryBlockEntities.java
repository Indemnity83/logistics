package com.logistics.quarry;

import com.logistics.LogisticsMod;
import com.logistics.quarry.entity.QuarryBlockEntity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class QuarryBlockEntities {
    private QuarryBlockEntities() {}

    public static final BlockEntityType<QuarryBlockEntity> QUARRY_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(LogisticsMod.MOD_ID, "quarry"),
            FabricBlockEntityTypeBuilder.create(
                    QuarryBlockEntity::new,
                    QuarryBlocks.QUARRY
            ).build());

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering quarry block entities");
    }
}

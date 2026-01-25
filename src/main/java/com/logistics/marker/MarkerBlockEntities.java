package com.logistics.marker;

import com.logistics.LogisticsMod;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class MarkerBlockEntities {
    private MarkerBlockEntities() {}

    public static final BlockEntityType<MarkerBlockEntity> MARKER_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(LogisticsMod.MOD_ID, "marker"),
            FabricBlockEntityTypeBuilder.create(
                    MarkerBlockEntity::new,
                    MarkerBlocks.MARKER
            ).build());

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering marker block entities");
    }
}

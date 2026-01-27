package com.logistics.core.registry;

import com.logistics.LogisticsMod;
import com.logistics.core.marker.MarkerBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class CoreBlockEntities {
    private CoreBlockEntities() {}

    public static final BlockEntityType<MarkerBlockEntity> MARKER_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(LogisticsMod.MOD_ID, "marker"),
            FabricBlockEntityTypeBuilder.create(MarkerBlockEntity::new, CoreBlocks.MARKER)
                    .build());

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering core block entities");
    }
}

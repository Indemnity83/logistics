package com.logistics.core.registry;

import com.logistics.LogisticsMod;
import com.logistics.core.marker.MarkerBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public final class CoreBlockEntities {
    private CoreBlockEntities() {}

    public static final BlockEntityType<MarkerBlockEntity> MARKER_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "core/marker"),
            FabricBlockEntityTypeBuilder.create(MarkerBlockEntity::new, CoreBlocks.MARKER)
                    .build());

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering core block entities");
        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        BuiltInRegistries.BLOCK_ENTITY_TYPE.addAlias(
                Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "marker"), BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(MARKER_BLOCK_ENTITY));
    }
}

package com.logistics.automation.registry;

import com.logistics.LogisticsMod;
import com.logistics.automation.quarry.entity.QuarryBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class AutomationBlockEntities {
    private AutomationBlockEntities() {}

    public static final BlockEntityType<QuarryBlockEntity> QUARRY_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "automation/quarry"),
            FabricBlockEntityTypeBuilder.create(QuarryBlockEntity::new, AutomationBlocks.QUARRY)
                    .build());

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering automation block entities");
        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        BuiltInRegistries.BLOCK_ENTITY_TYPE.addAlias(
                Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "quarry"),
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(QUARRY_BLOCK_ENTITY));
    }
}

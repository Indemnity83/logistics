package com.logistics.automation.registry;

import com.logistics.LogisticsMod;
import com.logistics.automation.quarry.entity.QuarryBlockEntity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class AutomationBlockEntities {
    private AutomationBlockEntities() {}

    public static final BlockEntityType<QuarryBlockEntity> QUARRY_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(LogisticsMod.MOD_ID, "automation/quarry"),
            FabricBlockEntityTypeBuilder.create(QuarryBlockEntity::new, AutomationBlocks.QUARRY)
                    .build());

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering automation block entities");
        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        Registries.BLOCK_ENTITY_TYPE.addAlias(
                Identifier.of(LogisticsMod.MOD_ID, "quarry"), Registries.BLOCK_ENTITY_TYPE.getId(QUARRY_BLOCK_ENTITY));
    }
}

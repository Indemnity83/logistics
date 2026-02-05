package com.logistics.automation.registry;

import com.logistics.LogisticsMod;
import com.logistics.api.fabric.TREnergyStorageAdapter;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import team.reborn.energy.api.EnergyStorage;

public final class AutomationBlockEntities {
    private AutomationBlockEntities() {}

    public static final BlockEntityType<LaserQuarryBlockEntity> LASER_QUARRY_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "automation/laser_quarry"),
            FabricBlockEntityTypeBuilder.create(LaserQuarryBlockEntity::new, AutomationBlocks.LASER_QUARRY)
                    .build());

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering automation block entities");
        registerLegacyAliases();
        registerEnergyProviders();
    }

    private static void registerEnergyProviders() {
        // Register laser quarry as an energy consumer (accepts energy from any side)
        EnergyStorage.SIDED.registerForBlockEntity(
                (quarry, direction) -> new TREnergyStorageAdapter(quarry), LASER_QUARRY_BLOCK_ENTITY);
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        BuiltInRegistries.BLOCK_ENTITY_TYPE.addAlias(
                Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "quarry"),
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(LASER_QUARRY_BLOCK_ENTITY));
        // v0.3 => v0.4 (quarry renamed to laser_quarry)
        BuiltInRegistries.BLOCK_ENTITY_TYPE.addAlias(
                Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "automation/quarry"),
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(LASER_QUARRY_BLOCK_ENTITY));
    }
}

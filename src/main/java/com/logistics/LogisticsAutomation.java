package com.logistics;

import com.logistics.api.fabric.TREnergyStorageAdapter;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryFrameBlock;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.pipe.PipeConnectionRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import team.reborn.energy.api.EnergyStorage;

public final class LogisticsAutomation extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsAutomation INSTANCE = new LogisticsAutomation();

    @Override
    protected String domain() {
        return "automation";
    }

    public static Identifier identifier(String name) {
        return INSTANCE.getDomainIdentifier(name);
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        registerLegacyAliases();
        registerEnergyApi();
        addCreativeTabEntries();

        // Register pipe connectivity for quarry (only accepts connections from above)
        PipeConnectionRegistry.SIDED.registerForBlockEntity(
                (quarry, direction) -> direction == Direction.UP ? quarry : null,
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY);

        ServerWorldEvents.UNLOAD.register((server, world) -> LaserQuarryBlockEntity.clearActiveQuarries(world));
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static final Block LASER_QUARRY = INSTANCE.registerBlockWithItem("laser_quarry",
            props -> new LaserQuarryBlock(props.strength(5.0f).sound(SoundType.STONE)));
        public static final Block LASER_QUARRY_FRAME = INSTANCE.registerBlock("laser_quarry_frame",
            props -> new LaserQuarryFrameBlock(props.strength(-1.0f, 3600000.0f).noOcclusion().noLootTable().randomTicks()));
    }

    public static final class ENTITY {
        private ENTITY() {}

        public static final BlockEntityType<LaserQuarryBlockEntity> LASER_QUARRY_BLOCK_ENTITY =
            INSTANCE.registerBlockEntity("laser_quarry", LaserQuarryBlockEntity::new, BLOCK.LASER_QUARRY);
    }

    private static void registerEnergyApi() {
        EnergyStorage.SIDED.registerForBlockEntity(
            (quarry, direction) -> new TREnergyStorageAdapter(quarry),
            ENTITY.LASER_QUARRY_BLOCK_ENTITY);
    }

    private static void addCreativeTabEntries() {
        LogisticsCore.CREATIVE_TAB.addItem(BLOCK.LASER_QUARRY);
    }

    private void registerLegacyAliases() {
        // v0.2 => v0.3
        registerBlockAlias("quarry", BLOCK.LASER_QUARRY);
        registerItemAlias("quarry", BLOCK.LASER_QUARRY.asItem());
        registerBlockAlias("quarry_frame", BLOCK.LASER_QUARRY_FRAME);
        registerBlockEntityAlias("quarry", ENTITY.LASER_QUARRY_BLOCK_ENTITY);

        // v0.2 => v0.3 (quarry renamed to laser_quarry)
        registerBlockAlias("automation/quarry", BLOCK.LASER_QUARRY);
        registerItemAlias("automation/quarry", BLOCK.LASER_QUARRY.asItem());
        registerBlockAlias("automation/quarry_frame", BLOCK.LASER_QUARRY_FRAME);
        registerBlockEntityAlias("automation/quarry", ENTITY.LASER_QUARRY_BLOCK_ENTITY);
    }
}

package com.logistics;

import com.logistics.automation.kiln.KilnBlock;
import com.logistics.automation.kiln.KilnBlockEntity;
import com.logistics.automation.kiln.KilnScreenHandler;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryFrameBlock;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.pipe.PipeConnectionRegistry;
import com.logistics.automation.marker.MarkerBlock;
import com.logistics.automation.marker.MarkerBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class LogisticsAutomation extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsAutomation INSTANCE = new LogisticsAutomation();

    @Override
    protected String domain() {
        return "automation";
    }

    public static com.logistics.core.lib.resource.ResourceId resource(String name) {
        return INSTANCE.domainResource(name);
    }

    public static com.logistics.core.lib.resource.ResourceId model(String name) {
        return INSTANCE.domainModelResource(name);
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        BLOCK.register();
        ENTITY.register();
        MENU.register();

        registerLegacyAliases();
        addCreativeTabEntries();

        // Register pipe connectivity for quarry (only accepts connections from above)
        PipeConnectionRegistry.SIDED.registerForBlockEntity(
                (quarry, direction) -> direction == Direction.UP ? quarry : null,
                ENTITY.LASER_QUARRY_BLOCK_ENTITY);

        ServerWorldEvents.UNLOAD.register((server, world) -> LaserQuarryBlockEntity.clearActiveQuarries(world));
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static Block MARKER;
        public static Block LASER_QUARRY;
        public static Block LASER_QUARRY_FRAME;
        public static Block KILN;

        static void register() {
            MARKER = INSTANCE.registerBlockWithItem("marker",
                props -> new MarkerBlock(props.strength(0.0f).sound(SoundType.WOOD).noCollision()));
            LASER_QUARRY = INSTANCE.registerBlockWithItem("laser_quarry",
                props -> new LaserQuarryBlock(props.strength(5.0f).sound(SoundType.STONE)));
            LASER_QUARRY_FRAME = INSTANCE.registerBlock("laser_quarry_frame",
                props -> new LaserQuarryFrameBlock(props.strength(-1.0f, 3600000.0f).noOcclusion().noLootTable().randomTicks()));
            KILN = INSTANCE.registerBlockWithItem("kiln",
                props -> new KilnBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(KilnBlock.LIT) ? 13 : 0)));
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        public static BlockEntityType<MarkerBlockEntity> MARKER_BLOCK_ENTITY;
        public static BlockEntityType<LaserQuarryBlockEntity> LASER_QUARRY_BLOCK_ENTITY;
        public static BlockEntityType<KilnBlockEntity> KILN_BLOCK_ENTITY;

        static void register() {
            MARKER_BLOCK_ENTITY = INSTANCE.registerBlockEntity("marker", MarkerBlockEntity::new, LogisticsAutomation.BLOCK.MARKER);
            LASER_QUARRY_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("laser_quarry", LaserQuarryBlockEntity::new, BLOCK.LASER_QUARRY);
            KILN_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("kiln", KilnBlockEntity::new, BLOCK.KILN);
        }
    }

    public static final class MENU {
        private MENU() {}

        public static MenuType<KilnScreenHandler> KILN;

        static void register() {
            KILN = INSTANCE.registerMenuType("kiln", KilnScreenHandler::new);
        }
    }

    private static void addCreativeTabEntries() {
        LogisticsCore.CREATIVE_TAB.addItem(BLOCK.MARKER);
        LogisticsCore.CREATIVE_TAB.addItem(BLOCK.LASER_QUARRY);
        LogisticsCore.CREATIVE_TAB.addItem(BLOCK.KILN);
    }

    private void registerLegacyAliases() {
        // v0.2 => v0.3
        registerBlockAlias("marker", BLOCK.MARKER);
        registerBlockEntityAlias("marker", ENTITY.MARKER_BLOCK_ENTITY);
        registerItemAlias("marker", BLOCK.MARKER.asItem());
        // core domain => automation domain (marker moved)
        registerBlockAlias("core/marker", BLOCK.MARKER);
        registerBlockEntityAlias("core/marker", ENTITY.MARKER_BLOCK_ENTITY);
        registerItemAlias("core/marker", BLOCK.MARKER.asItem());
        registerBlockAlias("quarry", BLOCK.LASER_QUARRY);
        registerItemAlias("quarry", BLOCK.LASER_QUARRY.asItem());
        registerBlockAlias("quarry_frame", BLOCK.LASER_QUARRY_FRAME);
        registerBlockEntityAlias("quarry", ENTITY.LASER_QUARRY_BLOCK_ENTITY);

        // v0.2 => v0.3 (quarry renamed to laser_quarry)
        registerBlockAlias("automation/quarry", BLOCK.LASER_QUARRY);
        registerItemAlias("automation/quarry", BLOCK.LASER_QUARRY.asItem());
        registerBlockAlias("automation/quarry_frame", BLOCK.LASER_QUARRY_FRAME);
        registerBlockEntityAlias("automation/quarry", ENTITY.LASER_QUARRY_BLOCK_ENTITY);

        // core domain => automation domain (kiln moved)
        registerBlockAlias("core/kiln", BLOCK.KILN);
        registerBlockEntityAlias("core/kiln", ENTITY.KILN_BLOCK_ENTITY);
        registerItemAlias("core/kiln", BLOCK.KILN.asItem());

        // automation domain => core domain (quartz_crystal moved)
        registerBlockAlias("automation/quartz_crystal", LogisticsCore.BLOCK.QUARTZ_CRYSTAL);
        registerItemAlias("automation/quartz_crystal", LogisticsCore.BLOCK.QUARTZ_CRYSTAL.asItem());
    }
}

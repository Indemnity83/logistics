package com.logistics;

import com.logistics.automation.kiln.KilnBlock;
import com.logistics.automation.kiln.KilnBlockEntity;
import com.logistics.automation.kiln.KilnScreenHandler;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryFrameBlock;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.LogisticsCreativeTab;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.automation.marker.MarkerBlock;
import com.logistics.automation.marker.MarkerBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class LogisticsAutomation extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsAutomation INSTANCE = new LogisticsAutomation();

    @Override
    protected String domain() {
        return "automation";
    }

    public static ResourceId resource(String name) {
        return INSTANCE.domainResource(name);
    }

    public static ResourceId model(String name) {
        return INSTANCE.domainModelResource(name);
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        ITEM.register();
        BLOCK.register();
        ENTITY.register();
        MENU.register();
        CREATIVE.register();
        ALIAS.register();
        TICKET_TYPE.register();
    }

    public static final class ITEM {
        private ITEM() {}

        // Shared machine components — only ever used in machine recipes, so they live here.
        public static Item MACHINE_CORE;
        public static Item REDSTONE_RECEPTION_COIL;

        static void register() {
            MACHINE_CORE = INSTANCE.registerItem("machine_core", Item::new);
            REDSTONE_RECEPTION_COIL = INSTANCE.registerItem("redstone_reception_coil", Item::new);
        }
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

    public static final class CREATIVE {
        public static final LogisticsCreativeTab TAB = LogisticsCreativeTab.create(
            LogisticsMod.modId("automation"),
            Component.translatable("itemGroup.logistics.automation"),
            () -> new ItemStack(ITEM.MACHINE_CORE)
        );

        private CREATIVE() {}

        static void register() {
            // Machine components, then the machines themselves. (Macerator joins when its
            // domain move lands.)
            TAB.add(ITEM.MACHINE_CORE);
            TAB.add(ITEM.REDSTONE_RECEPTION_COIL);
            TAB.add(BLOCK.KILN);
            TAB.add(BLOCK.LASER_QUARRY);
            TAB.add(BLOCK.MARKER);
            CreativeTabRegistrar.INSTANCE.registerTab(TAB);
        }
    }

    public static final class ALIAS {
        private ALIAS() {}

        static void register() {
            // core domain => automation domain (machine frame moved)
            INSTANCE.registerItemAlias("core/machine_core", ITEM.MACHINE_CORE);

            // v0.2 => v0.3
            INSTANCE.registerBlockAlias("marker", BLOCK.MARKER);
            INSTANCE.registerBlockEntityAlias("marker", ENTITY.MARKER_BLOCK_ENTITY);
            INSTANCE.registerItemAlias("marker", BLOCK.MARKER.asItem());
            // core domain => automation domain (marker moved)
            INSTANCE.registerBlockAlias("core/marker", BLOCK.MARKER);
            INSTANCE.registerBlockEntityAlias("core/marker", ENTITY.MARKER_BLOCK_ENTITY);
            INSTANCE.registerItemAlias("core/marker", BLOCK.MARKER.asItem());
            INSTANCE.registerBlockAlias("quarry", BLOCK.LASER_QUARRY);
            INSTANCE.registerItemAlias("quarry", BLOCK.LASER_QUARRY.asItem());
            INSTANCE.registerBlockAlias("quarry_frame", BLOCK.LASER_QUARRY_FRAME);
            INSTANCE.registerBlockEntityAlias("quarry", ENTITY.LASER_QUARRY_BLOCK_ENTITY);

            // v0.2 => v0.3 (quarry renamed to laser_quarry)
            INSTANCE.registerBlockAlias("automation/quarry", BLOCK.LASER_QUARRY);
            INSTANCE.registerItemAlias("automation/quarry", BLOCK.LASER_QUARRY.asItem());
            INSTANCE.registerBlockAlias("automation/quarry_frame", BLOCK.LASER_QUARRY_FRAME);
            INSTANCE.registerBlockEntityAlias("automation/quarry", ENTITY.LASER_QUARRY_BLOCK_ENTITY);

            // core domain => automation domain (kiln moved)
            INSTANCE.registerBlockAlias("core/kiln", BLOCK.KILN);
            INSTANCE.registerBlockEntityAlias("core/kiln", ENTITY.KILN_BLOCK_ENTITY);
            INSTANCE.registerItemAlias("core/kiln", BLOCK.KILN.asItem());

            // automation domain => core domain (quartz_crystal moved)
            INSTANCE.registerBlockAlias("automation/quartz_crystal", LogisticsCore.BLOCK.QUARTZ_CRYSTAL);
            INSTANCE.registerItemAlias("automation/quartz_crystal", LogisticsCore.BLOCK.QUARTZ_CRYSTAL.asItem());
        }
    }

    public static final class TICKET_TYPE {
        private TICKET_TYPE() {}

        public static TicketType QUARRY;
        public static TicketType QUARRY_BOUNDARY;

        static void register() {
            QUARRY = Registry.register(
                    BuiltInRegistries.TICKET_TYPE,
                    LogisticsMod.modId("quarry").toIdentifier(),
                    new TicketType(20L, TicketType.FLAG_PERSIST | TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE)
            );

            QUARRY_BOUNDARY = Registry.register(
                    BuiltInRegistries.TICKET_TYPE,
                    LogisticsMod.modId("quarry_boundary").toIdentifier(),
                    new TicketType(20L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE)
            );
        }
    }
}

package com.logistics;

import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryFrameBlock;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.automation.macerator.MaceratorBlock;
import com.logistics.automation.macerator.MaceratorBlockEntity;
import com.logistics.automation.macerator.MaceratorRecipeManager;
import com.logistics.automation.macerator.MaceratorRecipeSerializer;
import com.logistics.automation.macerator.MaceratorRecipeWrapper;
import com.logistics.automation.macerator.MaceratorScreenHandler;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.pipe.PipeConnectionRegistry;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.automation.marker.MarkerBlock;
import com.logistics.automation.marker.MarkerBlockEntity;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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
        RECIPE.register();

        MaceratorRecipeManager.register();

        registerLegacyAliases();
        addCreativeTabEntries();

        // Register pipe connectivity for quarry (only accepts connections from above)
        PipeConnectionRegistry.SIDED.registerForBlockEntity(
                (quarry, direction) -> direction == Direction.UP ? quarry : null,
                ENTITY.LASER_QUARRY_BLOCK_ENTITY);

        ServerWorldEvents.UNLOAD.register((server, world) -> LaserQuarryBlockEntity.clearActiveQuarries(world));
    }

    public static final class ITEM {
        private ITEM() {}

        public static Item IRON_POWDER;
        public static Item COPPER_POWDER;
        public static Item TIN_POWDER;
        public static Item BRONZE_POWDER;
        public static Item GOLD_POWDER;
        public static Item LAPIS_DUST;
        public static Item QUARTZ_DUST;
        public static Item COAL_DUST;
        public static Item AMETHYST_DUST;
        public static Item DIAMOND_DUST;
        public static Item EMERALD_DUST;
        public static Item NETHERITE_DUST;
        public static Item OBSIDIAN_DUST;
        public static Item ENDER_DUST;
        public static Item ECHO_DUST;
        public static Item PRISMARINE_DUST;
        public static Item SILICON_MIX;
        public static Item SILICON_WAFER;
        public static Item FLOUR;

        public static Item VALVE_COPPER;
        public static Item VALVE_BRONZE;
        public static Item VALVE_IRON;
        public static Item VALVE_GOLD;
        public static Item VALVE_DIAMOND;
        public static Item VALVE_OBSIDIAN;
        public static Item VALVE_BLAZING;
        public static Item VALVE_EMERALD;
        public static Item VALVE_APATITE;
        public static Item VALVE_LAPIS;
        public static Item VALVE_ENDER;
        public static Item VALVE_NETHERITE;

        public static Item FILAMENT_COPPER;
        public static Item FILAMENT_BRONZE;
        public static Item FILAMENT_IRON;
        public static Item FILAMENT_GOLD;
        public static Item FILAMENT_LAPIS;
        public static Item FILAMENT_APATITE;
        public static Item FILAMENT_DIAMOND;
        public static Item FILAMENT_EMERALD;
        public static Item FILAMENT_BLAZING;
        public static Item FILAMENT_NETHERITE;
        public static Item FILAMENT_OBSIDIAN;
        public static Item FILAMENT_ENDER;

        static void register() {
            IRON_POWDER = INSTANCE.registerItem("iron_powder", Item::new);
            COPPER_POWDER = INSTANCE.registerItem("copper_powder", Item::new);
            TIN_POWDER = INSTANCE.registerItem("tin_powder", Item::new);
            BRONZE_POWDER = INSTANCE.registerItem("bronze_powder", Item::new);
            GOLD_POWDER = INSTANCE.registerItem("gold_powder", Item::new);
            LAPIS_DUST = INSTANCE.registerItem("lapis_dust", Item::new);
            QUARTZ_DUST = INSTANCE.registerItem("quartz_dust", Item::new);
            COAL_DUST = INSTANCE.registerItem("coal_dust", Item::new);
            AMETHYST_DUST = INSTANCE.registerItem("amethyst_dust", Item::new);
            DIAMOND_DUST = INSTANCE.registerItem("diamond_dust", Item::new);
            EMERALD_DUST = INSTANCE.registerItem("emerald_dust", Item::new);
            NETHERITE_DUST = INSTANCE.registerItem("netherite_dust", Item::new);
            OBSIDIAN_DUST = INSTANCE.registerItem("obsidian_dust", Item::new);
            ENDER_DUST = INSTANCE.registerItem("ender_dust", Item::new);
            ECHO_DUST = INSTANCE.registerItem("echo_dust", Item::new);
            PRISMARINE_DUST = INSTANCE.registerItem("prismarine_dust", Item::new);
            SILICON_MIX = INSTANCE.registerItem("silicon_mix", Item::new);
            SILICON_WAFER = INSTANCE.registerItem("silicon_wafer", Item::new);
            FLOUR = INSTANCE.registerItem("flour", Item::new);

            VALVE_COPPER = INSTANCE.registerItem("valve_copper", Item::new);
            VALVE_BRONZE = INSTANCE.registerItem("valve_bronze", Item::new);
            VALVE_IRON = INSTANCE.registerItem("valve_iron", Item::new);
            VALVE_GOLD = INSTANCE.registerItem("valve_gold", Item::new);
            VALVE_DIAMOND = INSTANCE.registerItem("valve_diamond", Item::new);
            VALVE_OBSIDIAN = INSTANCE.registerItem("valve_obsidian", Item::new);
            VALVE_BLAZING = INSTANCE.registerItem("valve_blazing", Item::new);
            VALVE_EMERALD = INSTANCE.registerItem("valve_emerald", Item::new);
            VALVE_APATITE = INSTANCE.registerItem("valve_apatite", Item::new);
            VALVE_LAPIS = INSTANCE.registerItem("valve_lapis", Item::new);
            VALVE_ENDER = INSTANCE.registerItem("valve_ender", Item::new);
            VALVE_NETHERITE = INSTANCE.registerItem("valve_netherite", Item::new);

            FILAMENT_COPPER = INSTANCE.registerItem("filament_copper", Item::new);
            FILAMENT_BRONZE = INSTANCE.registerItem("filament_bronze", Item::new);
            FILAMENT_IRON = INSTANCE.registerItem("filament_iron", Item::new);
            FILAMENT_GOLD = INSTANCE.registerItem("filament_gold", Item::new);
            FILAMENT_LAPIS = INSTANCE.registerItem("filament_lapis", Item::new);
            FILAMENT_APATITE = INSTANCE.registerItem("filament_apatite", Item::new);
            FILAMENT_DIAMOND = INSTANCE.registerItem("filament_diamond", Item::new);
            FILAMENT_EMERALD = INSTANCE.registerItem("filament_emerald", Item::new);
            FILAMENT_BLAZING = INSTANCE.registerItem("filament_blazing", Item::new);
            FILAMENT_NETHERITE = INSTANCE.registerItem("filament_netherite", Item::new);
            FILAMENT_OBSIDIAN = INSTANCE.registerItem("filament_obsidian", Item::new);
            FILAMENT_ENDER = INSTANCE.registerItem("filament_ender", Item::new);
        }
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static Block MARKER;
        public static Block LASER_QUARRY;
        public static Block LASER_QUARRY_FRAME;
        public static Block MACERATOR;
        public static Block QUARTZ_CRYSTAL;

        static void register() {
            MARKER = INSTANCE.registerBlockWithItem("marker",
                props -> new MarkerBlock(props.strength(0.0f).sound(SoundType.WOOD).noCollision()));
            LASER_QUARRY = INSTANCE.registerBlockWithItem("laser_quarry",
                props -> new LaserQuarryBlock(props.strength(5.0f).sound(SoundType.STONE)));
            LASER_QUARRY_FRAME = INSTANCE.registerBlock("laser_quarry_frame",
                props -> new LaserQuarryFrameBlock(props.strength(-1.0f, 3600000.0f).noOcclusion().noLootTable().randomTicks()));
            MACERATOR = INSTANCE.registerBlockWithItem("macerator",
                props -> new MaceratorBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MaceratorBlock.LIT) ? 13 : 0)));
            QUARTZ_CRYSTAL = INSTANCE.registerBlockWithItem("quartz_crystal",
                props -> new Block(props.strength(0.8f).sound(SoundType.GLASS).noOcclusion()));
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        public static BlockEntityType<MarkerBlockEntity> MARKER_BLOCK_ENTITY;
        public static BlockEntityType<LaserQuarryBlockEntity> LASER_QUARRY_BLOCK_ENTITY;
        public static BlockEntityType<MaceratorBlockEntity> MACERATOR_BLOCK_ENTITY;

        static void register() {
            MARKER_BLOCK_ENTITY = INSTANCE.registerBlockEntity("marker", MarkerBlockEntity::new, LogisticsAutomation.BLOCK.MARKER);
            LASER_QUARRY_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("laser_quarry", LaserQuarryBlockEntity::new, BLOCK.LASER_QUARRY);
            MACERATOR_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("macerator", MaceratorBlockEntity::new, BLOCK.MACERATOR);
        }
    }

    public static final class MENU {
        private MENU() {}

        public static MenuType<MaceratorScreenHandler> MACERATOR;

        static void register() {
            MACERATOR = INSTANCE.registerMenuType("macerator", MaceratorScreenHandler::new);
        }
    }

    public static final class RECIPE {
        private RECIPE() {}

        public static RecipeType<MaceratorRecipeWrapper> MACERATOR_RECIPE_TYPE;
        public static RecipeSerializer<MaceratorRecipeWrapper> MACERATOR_RECIPE_SERIALIZER;

        static void register() {
            MACERATOR_RECIPE_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_TYPE,
                LogisticsMod.modId("macerator").toIdentifier(),
                new RecipeType<MaceratorRecipeWrapper>() {
                    @Override
                    public String toString() {
                        return "logistics:macerator";
                    }
                }
            );
            MACERATOR_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                LogisticsMod.modId("macerator").toIdentifier(),
                new MaceratorRecipeSerializer()
            );
        }
    }

    private static void addCreativeTabEntries() {
        LogisticsCore.CREATIVE_TAB.addItem(BLOCK.MARKER);
        LogisticsCore.CREATIVE_TAB.addItem(BLOCK.LASER_QUARRY);
        LogisticsCore.CREATIVE_TAB.addItem(BLOCK.MACERATOR);
        LogisticsCore.CREATIVE_TAB.addItem(BLOCK.QUARTZ_CRYSTAL);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            List<Item> valves = List.of(
                ITEM.VALVE_COPPER, ITEM.VALVE_BRONZE,
                ITEM.VALVE_IRON, ITEM.VALVE_GOLD, ITEM.VALVE_DIAMOND,
                ITEM.VALVE_OBSIDIAN, ITEM.VALVE_BLAZING, ITEM.VALVE_EMERALD,
                ITEM.VALVE_APATITE, ITEM.VALVE_LAPIS, ITEM.VALVE_ENDER,
                ITEM.VALVE_NETHERITE);
            Item prev = LogisticsCore.ITEM.NETHERITE_GEAR;
            for (Item item : valves) {
                entries.addAfter(prev, item);
                prev = item;
            }
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            List<Item> dusts = List.of(
                ITEM.IRON_POWDER, ITEM.COPPER_POWDER, ITEM.TIN_POWDER, ITEM.BRONZE_POWDER,
                ITEM.GOLD_POWDER, ITEM.LAPIS_DUST, ITEM.QUARTZ_DUST, ITEM.COAL_DUST,
                ITEM.AMETHYST_DUST, ITEM.DIAMOND_DUST, ITEM.EMERALD_DUST,
                ITEM.NETHERITE_DUST, ITEM.OBSIDIAN_DUST, ITEM.ENDER_DUST,
                ITEM.ECHO_DUST, ITEM.PRISMARINE_DUST,
                ITEM.SILICON_MIX, ITEM.SILICON_WAFER, ITEM.FLOUR,
                ITEM.FILAMENT_COPPER, ITEM.FILAMENT_BRONZE,
                ITEM.FILAMENT_IRON, ITEM.FILAMENT_GOLD, ITEM.FILAMENT_LAPIS,
                ITEM.FILAMENT_APATITE, ITEM.FILAMENT_DIAMOND, ITEM.FILAMENT_EMERALD,
                ITEM.FILAMENT_BLAZING, ITEM.FILAMENT_NETHERITE,
                ITEM.FILAMENT_OBSIDIAN, ITEM.FILAMENT_ENDER);
            Item prev = LogisticsCore.ITEM.BRONZE_INGOT;
            for (Item item : dusts) {
                entries.addAfter(prev, item);
                prev = item;
            }
        });
    }

    private void registerLegacyAliases() {
        // core domain => automation domain (valves moved)
        registerItemAlias("core/valve_copper", ITEM.VALVE_COPPER);
        registerItemAlias("core/valve_bronze", ITEM.VALVE_BRONZE);
        registerItemAlias("core/valve_iron", ITEM.VALVE_IRON);
        registerItemAlias("core/valve_gold", ITEM.VALVE_GOLD);
        registerItemAlias("core/valve_diamond", ITEM.VALVE_DIAMOND);
        registerItemAlias("core/valve_obsidian", ITEM.VALVE_OBSIDIAN);
        registerItemAlias("core/valve_blazing", ITEM.VALVE_BLAZING);
        registerItemAlias("core/valve_emerald", ITEM.VALVE_EMERALD);
        registerItemAlias("core/valve_apatite", ITEM.VALVE_APATITE);
        registerItemAlias("core/valve_lapis", ITEM.VALVE_LAPIS);
        registerItemAlias("core/valve_ender", ITEM.VALVE_ENDER);
        registerItemAlias("core/valve_netherite", ITEM.VALVE_NETHERITE);

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
    }
}

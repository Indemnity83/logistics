package com.logistics;

import com.logistics.automation.alloysmelter.AlloySmelterBlock;
import com.logistics.automation.alloysmelter.AlloySmelterBlockEntity;
import com.logistics.automation.alloysmelter.AlloySmelterRecipe;
import com.logistics.automation.alloysmelter.AlloySmelterRecipeDisplay;
import com.logistics.automation.alloysmelter.AlloySmelterRecipeSerializer;
import com.logistics.automation.alloysmelter.AlloySmelterScreenHandler;
import com.logistics.automation.crucible.MagmaCrucibleBlock;
import com.logistics.automation.crucible.MagmaCrucibleBlockEntity;
import com.logistics.automation.crucible.MagmaCrucibleRecipe;
import com.logistics.automation.crucible.MagmaCrucibleRecipeDisplay;
import com.logistics.automation.crucible.MagmaCrucibleRecipeSerializer;
import com.logistics.automation.crucible.MagmaCrucibleScreenHandler;
import com.logistics.automation.kiln.KilnBlock;
import com.logistics.automation.kiln.KilnBlockEntity;
import com.logistics.automation.kiln.KilnScreenHandler;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryFrameBlock;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.automation.macerator.MaceratorBlock;
import com.logistics.automation.macerator.MaceratorBlockEntity;
import com.logistics.automation.macerator.MaceratorRecipeDisplay;
import com.logistics.automation.macerator.MaceratorRecipeSerializer;
import com.logistics.automation.macerator.MaceratorRecipeWrapper;
import com.logistics.automation.macerator.MaceratorScreenHandler;
import com.logistics.automation.sawmill.SawmillBlock;
import com.logistics.automation.sawmill.SawmillBlockEntity;
import com.logistics.automation.sawmill.SawmillRecipe;
import com.logistics.automation.sawmill.SawmillRecipeDisplay;
import com.logistics.automation.sawmill.SawmillRecipeSerializer;
import com.logistics.automation.sawmill.SawmillScreenHandler;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.LogisticsCreativeTab;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
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

        BLOCK.register();
        ENTITY.register();
        MENU.register();
        RECIPE.register();
        CREATIVE.register();
        ALIAS.register();
        TICKET_TYPE.register();
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static Block LASER_QUARRY;
        public static Block LASER_QUARRY_FRAME;
        public static Block KILN;
        public static Block MACERATOR;
        public static Block SAWMILL;
        public static Block ALLOY_SMELTER;
        public static Block MAGMA_CRUCIBLE;

        static void register() {
            LASER_QUARRY = INSTANCE.registerBlockWithItem("laser_quarry",
                props -> new LaserQuarryBlock(props.strength(5.0f).sound(SoundType.STONE)));
            LASER_QUARRY_FRAME = INSTANCE.registerBlock("laser_quarry_frame",
                props -> new LaserQuarryFrameBlock(props.strength(-1.0f, 3600000.0f).noOcclusion().noLootTable().randomTicks()));
            KILN = INSTANCE.registerBlockWithItem("kiln",
                props -> new KilnBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(KilnBlock.LIT) ? 13 : 0)));
            MACERATOR = INSTANCE.registerBlockWithItem("macerator",
                props -> new MaceratorBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MaceratorBlock.LIT) ? 13 : 0)));
            SAWMILL = INSTANCE.registerBlockWithItem("sawmill",
                props -> new SawmillBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(SawmillBlock.LIT) ? 13 : 0)));
            ALLOY_SMELTER = INSTANCE.registerBlockWithItem("alloy_smelter",
                props -> new AlloySmelterBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(AlloySmelterBlock.LIT) ? 13 : 0)));
            MAGMA_CRUCIBLE = INSTANCE.registerBlockWithItem("magma_crucible",
                props -> new MagmaCrucibleBlock(props.strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MagmaCrucibleBlock.LIT) ? 13 : 0)));
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        public static BlockEntityType<LaserQuarryBlockEntity> LASER_QUARRY_BLOCK_ENTITY;
        public static BlockEntityType<KilnBlockEntity> KILN_BLOCK_ENTITY;
        public static BlockEntityType<MaceratorBlockEntity> MACERATOR_BLOCK_ENTITY;
        public static BlockEntityType<SawmillBlockEntity> SAWMILL_BLOCK_ENTITY;
        public static BlockEntityType<AlloySmelterBlockEntity> ALLOY_SMELTER_BLOCK_ENTITY;
        public static BlockEntityType<MagmaCrucibleBlockEntity> MAGMA_CRUCIBLE_BLOCK_ENTITY;

        static void register() {
            LASER_QUARRY_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("laser_quarry", LaserQuarryBlockEntity::new, BLOCK.LASER_QUARRY);
            KILN_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("kiln", KilnBlockEntity::new, BLOCK.KILN);
            MACERATOR_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("macerator", MaceratorBlockEntity::new, BLOCK.MACERATOR);
            SAWMILL_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("sawmill", SawmillBlockEntity::new, BLOCK.SAWMILL);
            ALLOY_SMELTER_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("alloy_smelter", AlloySmelterBlockEntity::new, BLOCK.ALLOY_SMELTER);
            MAGMA_CRUCIBLE_BLOCK_ENTITY =
                INSTANCE.registerBlockEntity("magma_crucible", MagmaCrucibleBlockEntity::new, BLOCK.MAGMA_CRUCIBLE);
        }
    }

    public static final class MENU {
        private MENU() {}

        public static MenuType<KilnScreenHandler> KILN;
        public static MenuType<MaceratorScreenHandler> MACERATOR;
        public static MenuType<SawmillScreenHandler> SAWMILL;
        public static MenuType<AlloySmelterScreenHandler> ALLOY_SMELTER;
        public static MenuType<MagmaCrucibleScreenHandler> MAGMA_CRUCIBLE;

        static void register() {
            KILN = INSTANCE.registerMenuType("kiln", KilnScreenHandler::new);
            MACERATOR = INSTANCE.registerMenuType("macerator", MaceratorScreenHandler::new);
            SAWMILL = INSTANCE.registerMenuType("sawmill", SawmillScreenHandler::new);
            ALLOY_SMELTER = INSTANCE.registerMenuType("alloy_smelter", AlloySmelterScreenHandler::new);
            MAGMA_CRUCIBLE = INSTANCE.registerMenuType("magma_crucible", MagmaCrucibleScreenHandler::new);
        }
    }

    public static final class RECIPE {
        private RECIPE() {}

        public static RecipeType<MaceratorRecipeWrapper> MACERATOR_RECIPE_TYPE;
        public static RecipeSerializer<MaceratorRecipeWrapper> MACERATOR_RECIPE_SERIALIZER;
        public static RecipeBookCategory MACERATOR_CATEGORY;
        public static RecipeDisplay.Type<MaceratorRecipeDisplay> MACERATOR_DISPLAY_TYPE;
        public static RecipeType<SawmillRecipe> SAWMILL_RECIPE_TYPE;
        public static RecipeSerializer<SawmillRecipe> SAWMILL_RECIPE_SERIALIZER;
        public static RecipeBookCategory SAWMILL_CATEGORY;
        public static RecipeDisplay.Type<SawmillRecipeDisplay> SAWMILL_DISPLAY_TYPE;
        public static RecipeType<AlloySmelterRecipe> ALLOY_SMELTER_RECIPE_TYPE;
        public static RecipeSerializer<AlloySmelterRecipe> ALLOY_SMELTER_RECIPE_SERIALIZER;
        public static RecipeBookCategory ALLOY_SMELTER_CATEGORY;
        public static RecipeDisplay.Type<AlloySmelterRecipeDisplay> ALLOY_SMELTER_DISPLAY_TYPE;
        public static RecipeType<MagmaCrucibleRecipe> MAGMA_CRUCIBLE_RECIPE_TYPE;
        public static RecipeSerializer<MagmaCrucibleRecipe> MAGMA_CRUCIBLE_RECIPE_SERIALIZER;
        public static RecipeBookCategory MAGMA_CRUCIBLE_CATEGORY;
        public static RecipeDisplay.Type<MagmaCrucibleRecipeDisplay> MAGMA_CRUCIBLE_DISPLAY_TYPE;

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
                MaceratorRecipeSerializer.INSTANCE
            );
            MACERATOR_CATEGORY = Registry.register(
                BuiltInRegistries.RECIPE_BOOK_CATEGORY,
                LogisticsMod.modId("macerator").toIdentifier(),
                new RecipeBookCategory()
            );
            MACERATOR_DISPLAY_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_DISPLAY,
                LogisticsMod.modId("macerator").toIdentifier(),
                MaceratorRecipeDisplay.TYPE
            );
            SAWMILL_RECIPE_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_TYPE,
                LogisticsMod.modId("sawmill").toIdentifier(),
                new RecipeType<SawmillRecipe>() {
                    @Override
                    public String toString() {
                        return "logistics:sawmill";
                    }
                }
            );
            SAWMILL_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                LogisticsMod.modId("sawmill").toIdentifier(),
                SawmillRecipeSerializer.INSTANCE
            );
            SAWMILL_CATEGORY = Registry.register(
                BuiltInRegistries.RECIPE_BOOK_CATEGORY,
                LogisticsMod.modId("sawmill").toIdentifier(),
                new RecipeBookCategory()
            );
            SAWMILL_DISPLAY_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_DISPLAY,
                LogisticsMod.modId("sawmill").toIdentifier(),
                SawmillRecipeDisplay.TYPE
            );
            ALLOY_SMELTER_RECIPE_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_TYPE,
                LogisticsMod.modId("alloy_smelter").toIdentifier(),
                new RecipeType<AlloySmelterRecipe>() {
                    @Override
                    public String toString() {
                        return "logistics:alloy_smelter";
                    }
                }
            );
            ALLOY_SMELTER_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                LogisticsMod.modId("alloy_smelter").toIdentifier(),
                AlloySmelterRecipeSerializer.INSTANCE
            );
            ALLOY_SMELTER_CATEGORY = Registry.register(
                BuiltInRegistries.RECIPE_BOOK_CATEGORY,
                LogisticsMod.modId("alloy_smelter").toIdentifier(),
                new RecipeBookCategory()
            );
            ALLOY_SMELTER_DISPLAY_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_DISPLAY,
                LogisticsMod.modId("alloy_smelter").toIdentifier(),
                AlloySmelterRecipeDisplay.TYPE
            );
            MAGMA_CRUCIBLE_RECIPE_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_TYPE,
                LogisticsMod.modId("magma_crucible").toIdentifier(),
                new RecipeType<MagmaCrucibleRecipe>() {
                    @Override
                    public String toString() {
                        return "logistics:magma_crucible";
                    }
                }
            );
            MAGMA_CRUCIBLE_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                LogisticsMod.modId("magma_crucible").toIdentifier(),
                MagmaCrucibleRecipeSerializer.INSTANCE
            );
            MAGMA_CRUCIBLE_CATEGORY = Registry.register(
                BuiltInRegistries.RECIPE_BOOK_CATEGORY,
                LogisticsMod.modId("magma_crucible").toIdentifier(),
                new RecipeBookCategory()
            );
            MAGMA_CRUCIBLE_DISPLAY_TYPE = Registry.register(
                BuiltInRegistries.RECIPE_DISPLAY,
                LogisticsMod.modId("magma_crucible").toIdentifier(),
                MagmaCrucibleRecipeDisplay.TYPE
            );
        }
    }

    public static final class CREATIVE {
        public static final LogisticsCreativeTab TAB = LogisticsCreativeTab.create(
            LogisticsMod.modId("automation"),
            Component.translatable("itemGroup.logistics.automation"),
            () -> new ItemStack(LogisticsCore.ITEM.MACHINE_CORE)
        );

        private CREATIVE() {}

        static void register() {
            // Machine components, then the machines.
            TAB.add(LogisticsCore.ITEM.MACHINE_CORE);
            TAB.add(LogisticsCore.ITEM.REDSTONE_RECEPTION_COIL);
            TAB.add(BLOCK.LASER_QUARRY);
            TAB.add(BLOCK.KILN);
            TAB.add(BLOCK.MACERATOR);
            TAB.add(BLOCK.SAWMILL);
            TAB.add(BLOCK.ALLOY_SMELTER);
            TAB.add(BLOCK.MAGMA_CRUCIBLE);
            CreativeTabRegistrar.INSTANCE.registerTab(TAB);
        }
    }

    public static final class ALIAS {
        private ALIAS() {}

        static void register() {
            // Macerator moved from the core domain to automation; bridge the previous-major (0.6.x) IDs.
            INSTANCE.registerBlockAlias("core/macerator", BLOCK.MACERATOR);
            INSTANCE.registerBlockEntityAlias("core/macerator", ENTITY.MACERATOR_BLOCK_ENTITY);
            INSTANCE.registerItemAlias("core/macerator", BLOCK.MACERATOR.asItem());
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

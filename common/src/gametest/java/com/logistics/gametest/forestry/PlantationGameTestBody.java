package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmMachine;
import com.logistics.forestry.farm.FarmStatus;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Plantation GameTest bodies, compiled into both loaders' {@code gametest} source sets.
 * Fabric's {@code PlantationGameTest} delegates to these methods and NeoForge's
 * {@code PlantationGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>The rig is real blocks: the farm, a Creative Engine held on by a redstone block powering it
 * from the west, and stalks whose bases stand level with the farm, inside its Reach. Sugar cane grows
 * on sand beside a walled-in water source, bamboo on dirt, and cactus on sand with nothing beside it.
 * A hopper under the farm pulls its produce into a chest.
 */
public class PlantationGameTestBody {

    private static final BlockPos FARM = new BlockPos(4, 2, 4);
    private static final BlockPos ENGINE = FARM.west();
    private static final BlockPos ENGINE_SIGNAL = ENGINE.south();
    private static final BlockPos OUTPUT_HOPPER = FARM.below();
    private static final BlockPos CHEST = OUTPUT_HOPPER.below();
    /** Checks per second, so a pass over the Reach takes well under a second. */
    private static final int CHECKS_PER_SECOND = 400;

    /** Sugar cane's base, two blocks west of the farm, with its water two west of that. */
    private static final BlockPos CANE = FARM.west(2);
    private static final BlockPos WATER = CANE.below().west();
    /** Bamboo's base, two blocks east of the farm. */
    private static final BlockPos BAMBOO = FARM.east(2);
    /** Cactus's base, two blocks south of the farm, nothing solid beside it. */
    private static final BlockPos CACTUS = FARM.south(2);

    private static final int CANE_HEIGHT = 3;
    /** Tall enough to reach two blocks above the Reach, which ends one above the farm. */
    private static final int BAMBOO_HEIGHT = 5;
    private static final int CACTUS_HEIGHT = 3;

    /**
     * Grown sugar cane, bamboo and cactus are harvested down to their base, bamboo above the Reach
     * included, and the hopper pulls every block taken into the chest. Every base and its ground stay.
     */
    public static void testHarvestsStalksAndKeepsEveryBase(GameTestHelper context) {
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(OUTPUT_HOPPER, Blocks.HOPPER);
        plantCane(context, CANE_HEIGHT);
        plantBamboo(context, BAMBOO_HEIGHT);
        plantCactus(context, CACTUS_HEIGHT);
        FarmBlockEntity farm = farm(context);
        if (farm == null) {
            return;
        }

        context.succeedWhen(() -> {
            assertBase(context, CANE, Blocks.SUGAR_CANE, Blocks.SAND);
            assertBase(context, BAMBOO, Blocks.BAMBOO, Blocks.DIRT);
            assertBase(context, CACTUS, Blocks.CACTUS, Blocks.SAND);
            assertChestHolds(context, Items.SUGAR_CANE, CANE_HEIGHT - 1);
            assertChestHolds(context, Items.BAMBOO, BAMBOO_HEIGHT - 1);
            assertChestHolds(context, Items.CACTUS, CACTUS_HEIGHT - 1);
        });
    }

    /**
     * A farm whose inventory is full keeps harvesting: the sugar cane taken drops at its base, which
     * stays, and the farm reports Inventory full.
     */
    public static void testFullFarmSpillsAtTheBase(GameTestHelper context) {
        plantCane(context, CANE_HEIGHT);
        FarmBlockEntity farm = farm(context);
        if (farm == null) {
            return;
        }
        for (int slot = 0; slot < FarmMachine.SLOT_COUNT; slot++) {
            farm.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        Vec3 drop = Vec3.atCenterOf(context.absolutePos(CANE));
        int[] spilled = {0};

        context.onEachTick(() -> {
            AABB everywhere = new AABB(context.absolutePos(BlockPos.ZERO)).inflate(16.0);
            for (ItemEntity item : context.getLevel().getEntitiesOfClass(ItemEntity.class, everywhere)) {
                if (!item.getItem().is(Items.SUGAR_CANE)) {
                    continue;
                }
                // Loose items drift, so each is checked the tick it first appears, then removed.
                if (item.position().distanceTo(drop) > 1.0) {
                    throw new GameTestAssertPosException("The sugar cane should drop at its base, landed at "
                            + item.position() + ", " + item.position().distanceTo(drop) + " away", context.absolutePos(CANE), CANE, context.getTick());
                }
                spilled[0] += item.getItem().getCount();
                item.discard();
            }
        });
        context.succeedWhen(() -> {
            if (spilled[0] < CANE_HEIGHT - 1) {
                throw new GameTestAssertPosException("The full farm should drop all " + (CANE_HEIGHT - 1)
                        + " sugar cane at the base, " + spilled[0] + " so far", context.absolutePos(CANE), CANE, context.getTick());
            }
            assertBase(context, CANE, Blocks.SUGAR_CANE, Blocks.SAND);
            if (farm.status() != FarmStatus.INVENTORY_FULL) {
                throw new GameTestAssertPosException("The farm should report Inventory full, reports " + farm.status(), context.absolutePos(FARM), FARM, context.getTick());
            }
        });
    }

    /**
     * A harvested bamboo base is a Plot: once it is broken, the farm plants a bamboo shoot back from
     * its inventory.
     */
    public static void testReplantsAMissingBaseFromTheInventory(GameTestHelper context) {
        plantBamboo(context, 3);
        FarmBlockEntity farm = farm(context);
        if (farm == null) {
            return;
        }

        context.startSequence()
                .thenWaitUntil(() -> {
                    assertBase(context, BAMBOO, Blocks.BAMBOO, Blocks.DIRT);
                    if (count(farm, Items.BAMBOO) < 2) {
                        throw new GameTestAssertPosException("The farm should hold the harvested bamboo", context.absolutePos(FARM), FARM, context.getTick());
                    }
                })
                .thenExecute(() -> context.setBlock(BAMBOO, Blocks.AIR))
                .thenWaitUntil(() -> {
                    BlockState state = context.getBlockState(BAMBOO);
                    if (!state.is(Blocks.BAMBOO_SAPLING) && !state.is(Blocks.BAMBOO)) {
                        throw new GameTestAssertPosException("The Plot should be replanted with a bamboo shoot, holds "
                                + state + "; farm reports " + farm.status(), context.absolutePos(BAMBOO), BAMBOO, context.getTick());
                    }
                    if (count(farm, Items.BAMBOO) != 1) {
                        throw new GameTestAssertPosException("The replant should take one bamboo from the inventory", context.absolutePos(FARM), FARM, context.getTick());
                    }
                })
                .thenSucceed();
    }

    // ==================== Rig ====================

    /** Sugar cane {@code height} tall on sand, beside a water source walled in on every other side. */
    private static void plantCane(GameTestHelper context, int height) {
        BlockPos ground = CANE.below();
        context.setBlock(ground.below(), Blocks.STONE);
        context.setBlock(WATER.below(), Blocks.STONE);
        context.setBlock(WATER.west(), Blocks.STONE);
        context.setBlock(WATER.north(), Blocks.STONE);
        context.setBlock(WATER.south(), Blocks.STONE);
        context.setBlock(WATER, Blocks.WATER);
        context.setBlock(ground, Blocks.SAND);
        stack(context, CANE, Blocks.SUGAR_CANE.defaultBlockState(), height);
    }

    private static void plantBamboo(GameTestHelper context, int height) {
        context.setBlock(BAMBOO.below(), Blocks.DIRT);
        stack(context, BAMBOO, Blocks.BAMBOO.defaultBlockState(), height);
    }

    private static void plantCactus(GameTestHelper context, int height) {
        context.setBlock(CACTUS.below(2), Blocks.STONE);
        context.setBlock(CACTUS.below(), Blocks.SAND);
        stack(context, CACTUS, Blocks.CACTUS.defaultBlockState(), height);
    }

    private static void stack(GameTestHelper context, BlockPos base, BlockState stalk, int height) {
        for (int y = 0; y < height; y++) {
            context.setBlock(base.above(y), stalk);
        }
    }

    /**
     * Places the farm beside a running Creative Engine. Returns {@code null}, with the test failed,
     * when a block entity is missing.
     */
    @Nullable
    private static FarmBlockEntity farm(GameTestHelper context) {
        context.setBlock(FARM, LogisticsForestry.BLOCK.PLANTATION.defaultBlockState());
        FarmBlockEntity farm = (FarmBlockEntity) context.getBlockEntity(FARM);
        if (farm == null) {
            context.fail("Expected the farm block entity");
            return null;
        }
        farm.setRateOverride(CHECKS_PER_SECOND, 20);
        context.setBlock(ENGINE_SIGNAL, Blocks.REDSTONE_BLOCK);
        context.setBlock(ENGINE, LogisticsPower.BLOCK.CREATIVE_ENGINE.defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));
        CreativeEngineBlockEntity engine = (CreativeEngineBlockEntity) context.getBlockEntity(ENGINE);
        if (engine == null) {
            context.fail("Expected a creative engine at " + ENGINE);
            return null;
        }
        // 20 -> 40 -> 80 -> 160 RF/t, above the farm's input cap.
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        return farm;
    }

    /** Throws unless {@code base} still holds {@code stalk} on {@code ground}, with nothing of it left above. */
    private static void assertBase(GameTestHelper context, BlockPos base, Block stalk, Block ground) {
        if (!context.getBlockState(base).is(stalk)) {
            throw new GameTestAssertPosException("The base should stay " + stalk + ", holds "
                    + context.getBlockState(base), context.absolutePos(base), base, context.getTick());
        }
        if (!context.getBlockState(base.below()).is(ground)) {
            throw new GameTestAssertPosException("The ground should stay " + ground, context.absolutePos(base.below()), base.below(), context.getTick());
        }
        if (context.getBlockState(base.above()).is(stalk)) {
            throw new GameTestAssertPosException("Everything above the base should be harvested", context.absolutePos(base.above()), base.above(), context.getTick());
        }
    }

    private static void assertChestHolds(GameTestHelper context, Item item, int expected) {
        int pulled = count((ChestBlockEntity) context.getBlockEntity(CHEST), item);
        if (pulled < expected) {
            throw new GameTestAssertPosException("The hopper should pull " + expected + " " + item
                    + " into the chest, " + pulled + " so far", context.absolutePos(CHEST), CHEST, context.getTick());
        }
    }

    private static int count(@Nullable Container container, Item item) {
        if (container == null) {
            return 0;
        }
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}

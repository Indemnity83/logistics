package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmMachine;
import com.logistics.forestry.farm.FarmStatus;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Gourd Farm GameTest bodies, compiled into both loaders' {@code gametest} source sets.
 * Fabric's {@code GourdFarmGameTest} delegates to these methods and NeoForge's
 * {@code GourdFarmGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Every rig is built from real blocks: the farm, a Creative Engine held on by a redstone block
 * powering it from the west, and stems on farmland one level above the farm, inside its Reach, each
 * attached to its fruit on dirt. A hopper under the farm pulls its produce into a chest.
 */
public class GourdFarmGameTestBody {

    private static final BlockPos FARM = new BlockPos(3, 2, 3);
    private static final BlockPos ENGINE = FARM.west();
    private static final BlockPos ENGINE_SIGNAL = ENGINE.south();
    private static final BlockPos OUTPUT_HOPPER = FARM.below();
    private static final BlockPos CHEST = OUTPUT_HOPPER.below();
    /** Checks per second for the replant test, so a pass over the Reach takes well under a second. */
    private static final int FAST_CHECKS_PER_SECOND = 400;

    /** A stem, which way it points, and the fruit it grew there. */
    private record Vine(BlockPos stem, Direction facing, Block stemBlock, Block attached, Block fruit) {
        BlockPos fruitPos() {
            return stem.relative(facing);
        }
    }

    /** Two pumpkins and two melons, their stems one level above the farm and every fruit inside the structure. */
    private static final List<Vine> VINES = List.of(
            pumpkin(new BlockPos(1, 3, 2), Direction.WEST),
            pumpkin(new BlockPos(2, 3, 5), Direction.SOUTH),
            melon(new BlockPos(5, 3, 4), Direction.EAST),
            melon(new BlockPos(4, 3, 1), Direction.NORTH));

    private static Vine pumpkin(BlockPos stem, Direction facing) {
        return new Vine(stem, facing, Blocks.PUMPKIN_STEM, Blocks.ATTACHED_PUMPKIN_STEM, Blocks.PUMPKIN);
    }

    private static Vine melon(BlockPos stem, Direction facing) {
        return new Vine(stem, facing, Blocks.MELON_STEM, Blocks.ATTACHED_MELON_STEM, Blocks.MELON);
    }

    /** Lays out {@code vines}, places the farm and powers it; {@code null}, with the test failed, if a block entity is missing. */
    @Nullable
    private static FarmBlockEntity build(GameTestHelper context, List<Vine> vines) {
        for (Vine vine : vines) {
            context.setBlock(vine.stem().below(), Blocks.FARMLAND);
            context.setBlock(vine.fruitPos().below(), Blocks.DIRT);
            // The fruit first: a stem attached to anything else turns back into a plain stem.
            context.setBlock(vine.fruitPos(), vine.fruit());
            context.setBlock(vine.stem(), vine.attached().defaultBlockState().setValue(AttachedStemBlock.FACING, vine.facing()));
        }
        context.setBlock(FARM, LogisticsForestry.BLOCK.GOURD_FARM.defaultBlockState());
        FarmBlockEntity farm = context.getBlockEntity(FARM, FarmBlockEntity.class);
        if (farm == null) {
            context.fail("Expected the farm block entity");
            return null;
        }
        context.setBlock(ENGINE_SIGNAL, Blocks.REDSTONE_BLOCK);
        context.setBlock(ENGINE, LogisticsPower.BLOCK.CREATIVE_ENGINE.defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));
        CreativeEngineBlockEntity engine = context.getBlockEntity(ENGINE, CreativeEngineBlockEntity.class);
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

    /** Fails the tick any stem is anything but its own stem, attached or not. */
    private static void assertStemsStand(GameTestHelper context, List<Vine> vines) {
        for (Vine vine : vines) {
            BlockState state = context.getBlockState(vine.stem());
            if (!state.is(vine.stemBlock()) && !state.is(vine.attached())) {
                throw context.assertionException(vine.stem(), "The farm must never break a stem, found " + state);
            }
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

    /**
     * The farm harvests every pumpkin and melon its stems grew, with vanilla's loot, and never breaks
     * a stem; the hopper under the farm pulls the harvest out into the chest.
     */
    public static void testHarvestsFruitNeverStemsAndHopperPullsHarvest(GameTestHelper context) {
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(OUTPUT_HOPPER, Blocks.HOPPER);
        FarmBlockEntity farm = build(context, VINES);
        if (farm == null) {
            return;
        }

        context.onEachTick(() -> assertStemsStand(context, VINES));
        context.succeedWhen(() -> {
            Container chest = context.getBlockEntity(CHEST, ChestBlockEntity.class);
            int pumpkins = count(chest, Items.PUMPKIN);
            int slices = count(chest, Items.MELON_SLICE);
            // Two pumpkins; two melons of at least 3 slices each.
            if (pumpkins < 2 || slices < 6) {
                throw context.assertionException(CHEST, "The hopper should pull 2 pumpkins and at least 6 melon slices"
                        + " into the chest, has " + pumpkins + " and " + slices + "; farm reports " + farm.status());
            }
            if (count(farm, Items.PUMPKIN_SEEDS) + count(farm, Items.MELON_SEEDS) + count(chest, Items.PUMPKIN_SEEDS)
                    + count(chest, Items.MELON_SEEDS) > 0) {
                throw context.assertionException(FARM, "Harvesting fruit should never yield seeds");
            }
        });
    }

    /**
     * A farm whose inventory is full keeps harvesting: the pumpkin drops at its stem, the Plot, and
     * the stem stays.
     */
    public static void testFullFarmSpillsAtTheStem(GameTestHelper context) {
        Vine vine = VINES.get(0);
        context.setBlock(OUTPUT_HOPPER, Blocks.STONE);
        FarmBlockEntity farm = build(context, List.of(vine));
        if (farm == null) {
            return;
        }
        for (int slot = 0; slot < FarmMachine.SLOT_COUNT; slot++) {
            farm.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        Vec3 drop = Vec3.atCenterOf(context.absolutePos(vine.stem()));
        boolean[] spilled = {false};

        context.onEachTick(() -> {
            assertStemsStand(context, List.of(vine));
            if (spilled[0]) {
                return;
            }
            AABB everywhere = new AABB(context.absolutePos(BlockPos.ZERO)).inflate(16.0);
            for (ItemEntity item : context.getLevel().getEntitiesOfClass(ItemEntity.class, everywhere)) {
                if (!item.getItem().is(Items.PUMPKIN)) {
                    continue;
                }
                // Loose items drift, so they are checked the tick they first appear.
                if (item.position().distanceTo(drop) > 1.0) {
                    throw context.assertionException(vine.stem(), "The pumpkin should drop at its stem, landed at "
                            + item.position() + ", " + item.position().distanceTo(drop) + " away");
                }
                spilled[0] = true;
            }
        });
        context.succeedWhen(() -> {
            if (!spilled[0]) {
                throw context.assertionException(vine.stem(), "The full farm should drop the pumpkin at its stem");
            }
            if (context.getBlockState(vine.fruitPos()).is(Blocks.PUMPKIN)) {
                throw context.assertionException(vine.fruitPos(), "The pumpkin should be harvested");
            }
            if (farm.status() != FarmStatus.INVENTORY_FULL) {
                throw context.assertionException(FARM, "The farm should report Inventory full, reports " + farm.status());
            }
        });
    }

    /**
     * A stem broken by trampling, its farmland turned to dirt, leaves an Empty Plot: the farm re-tills
     * the dirt and replants the stem from the seeds in its inventory.
     */
    public static void testBrokenStemIsReTilledAndReplanted(GameTestHelper context) {
        context.setBlock(OUTPUT_HOPPER, Blocks.STONE);
        BlockPos stem = VINES.get(0).stem();
        context.setBlock(stem.below(), Blocks.FARMLAND);
        context.setBlock(stem, Blocks.PUMPKIN_STEM);
        FarmBlockEntity farm = build(context, List.of());
        if (farm == null) {
            return;
        }
        farm.setRateOverride(FAST_CHECKS_PER_SECOND, 20);

        context.startSequence()
                // A few passes over the Reach, which take the growing stem on as a Plot.
                .thenExecuteAfter(40, () -> {
                    context.setBlock(stem, Blocks.AIR);
                    context.setBlock(stem.below(), Blocks.DIRT);
                })
                .thenWaitUntil(() -> {
                    if (farm.status() != FarmStatus.WAITING_FOR_SEEDS) {
                        throw context.assertionException(FARM, "The farm should wait for seeds, reports " + farm.status());
                    }
                })
                .thenExecute(() -> farm.setItem(0, new ItemStack(Items.PUMPKIN_SEEDS, 4)))
                .thenWaitUntil(() -> {
                    BlockState replant = context.getBlockState(stem);
                    if (!replant.is(Blocks.PUMPKIN_STEM)) {
                        throw context.assertionException(stem, "The stem should be replanted, holds " + replant);
                    }
                    if (!context.getBlockState(stem.below()).is(Blocks.FARMLAND)) {
                        throw context.assertionException(stem.below(), "The dirt should be re-tilled into farmland");
                    }
                    if (count(farm, Items.PUMPKIN_SEEDS) != 3) {
                        throw context.assertionException(FARM, "The replant should take one seed");
                    }
                })
                .thenSucceed();
    }
}

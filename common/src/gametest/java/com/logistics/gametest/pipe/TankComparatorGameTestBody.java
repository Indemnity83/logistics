package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;

/**
 * Shared GameTest bodies for reading a glass tank's fill level with a comparator, compiled into both
 * loaders' {@code gametest} source sets (see {@code common/build.gradle}).
 *
 * <p>These read through {@code BlockState.getAnalogOutputSignal}, the same accessor vanilla redstone
 * uses, rather than calling the block's method directly — so a signature that no longer overrides
 * anything fails here instead of passing silently.
 */
public class TankComparatorGameTestBody {

    /** Reads the tank the way a comparator does. */
    private static int signal(GameTestHelper context, BlockPos pos) {
        BlockPos abs = context.absolutePos(pos);
        return context.getLevel()
                .getBlockState(abs)
                .getAnalogOutputSignal(context.getLevel(), abs, Direction.NORTH);
    }

    private static GlassTankBlockEntity tankAt(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPipe.BLOCK.GLASS_TANK);
        return context.getBlockEntity(pos, GlassTankBlockEntity.class);
    }

    /** An empty tank reads nothing. */
    public static void emptyTankReadsZero(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        tankAt(context, pos);

        int reading = signal(context, pos);
        if (reading != 0) {
            context.fail("An empty tank should read 0, read " + reading);
            return;
        }
        context.succeed();
    }

    /** A full tank reads the full 15. */
    public static void fullTankReadsFifteen(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        GlassTankBlockEntity tank = tankAt(context, pos);
        tank.tank().setContents(SimpleFluidKey.of(Fluids.WATER), tank.capacity());

        int reading = signal(context, pos);
        if (reading != 15) {
            context.fail("A full tank should read 15, read " + reading);
            return;
        }
        context.succeed();
    }

    /**
     * The reading is the whole column's fill, not the cell's. Fluid settles bottom-up, so one cell's
     * worth in a four-tall column fills the bottom cell completely — a per-cell reading would report
     * a full 15 for a column that is only a quarter full.
     */
    public static void columnFillIsReadNotCellFill(GameTestHelper context) {
        BlockPos bottom = new BlockPos(1, 1, 1);
        GlassTankBlockEntity tank = tankAt(context, bottom);
        long oneCell = tank.capacity();
        for (int i = 1; i < 4; i++) {
            tankAt(context, bottom.above(i));
        }

        long inserted = tank.column().insert(SimpleFluidKey.of(Fluids.WATER), oneCell, false);
        if (inserted != oneCell) {
            context.fail("Expected to insert one cell's worth, inserted " + inserted);
            return;
        }

        int reading = signal(context, bottom);
        // 15 * 1/4 == 3. A per-cell reading would be 15, because the bottom cell is full.
        if (reading != 3) {
            context.fail("A quarter-full four-tall column should read 3, read " + reading
                    + " (15 would mean the cell is being read instead of the column)");
            return;
        }
        context.succeed();
    }

    /** Every cell of a column answers alike, the way either half of a double chest does. */
    public static void everyCellInAColumnReadsTheSame(GameTestHelper context) {
        BlockPos bottom = new BlockPos(1, 1, 1);
        GlassTankBlockEntity tank = tankAt(context, bottom);
        for (int i = 1; i < 3; i++) {
            tankAt(context, bottom.above(i));
        }
        tank.column().insert(SimpleFluidKey.of(Fluids.WATER), tank.capacity(), false);

        int atBottom = signal(context, bottom);
        for (int i = 1; i < 3; i++) {
            int here = signal(context, bottom.above(i));
            if (here != atBottom) {
                context.fail("Cell " + i + " read " + here + " but the bottom read " + atBottom);
                return;
            }
        }
        if (atBottom == 0) {
            context.fail("The column holds fluid but reads 0");
            return;
        }
        context.succeed();
    }

    /** Pipes carry no comparator reading — the seam that once backed one is gone. */
    public static void pipesAdvertiseNoComparatorOutput(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(1, 1, 1);
        BlockPos chassisPos = new BlockPos(3, 1, 1);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(chassisPos, LogisticsPipe.BLOCK.CHASSIS_LOGISTICS_PIPE_MK1);

        for (BlockPos pos : new BlockPos[] {pipePos, chassisPos}) {
            BlockPos abs = context.absolutePos(pos);
            if (context.getLevel().getBlockState(abs).hasAnalogOutputSignal()) {
                context.fail("A pipe at " + pos + " should not advertise a comparator output");
                return;
            }
        }
        context.succeed();
    }
}

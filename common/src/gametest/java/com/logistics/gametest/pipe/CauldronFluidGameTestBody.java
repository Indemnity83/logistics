package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;

/**
 * Shared GameTest bodies for trading fluid with a cauldron, compiled into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}).
 *
 * <p>A cauldron is an all-or-nothing fluid handler: both loaders expose one through the ordinary block fluid
 * capability, but it only parts with (and only accepts) whole levels — a bucket for lava, a third of a bucket
 * for water. These tests drive real blocks end to end: a real Creative Engine powers a real Fluid Extractor
 * Pipe to drain a real cauldron into a real Glass Tank, and a real Insertion Fluid Pipe fills one back up.
 */
public class CauldronFluidGameTestBody {

    /** One bucket, the buffer the handler-boundary pipes carry so a whole cauldron level fits. */
    private static final long BUCKET_MB = 1000;

    /**
     * A powered extractor pipe drains a lava cauldron into a tank. Lava is the hard case: its slow spread rate
     * scales the per-tick extraction budget down to 1 mB, and a lava cauldron parts with a full 1000 mB or
     * nothing, so the rate-limited pull alone can never move any of it.
     */
    public static void extractorDrainsLavaCauldron(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(1, 2, 1);
        context.setBlock(pipePos.west(), Blocks.LAVA_CAULDRON);

        GlassTankBlockEntity tank = buildExtractorRig(context, pipePos);

        context.succeedWhen(() -> {
            if (!context.getBlockState(pipePos.west()).is(Blocks.CAULDRON)) {
                throw context.assertionException("Extractor should have emptied the lava cauldron");
            }
            if (tank.tank().getAmount() <= 0 || tank.tank().getFluidKey().getFluid() != Fluids.LAVA) {
                throw context.assertionException("Extractor should have moved the lava into the tank");
            }
        });
    }

    /**
     * The same rig against a full water cauldron, whose levels are 333⅓ mB rather than a whole bucket — so the
     * pipe must cope with a chunk size that is not a round number of millibuckets.
     */
    public static void extractorDrainsWaterCauldron(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(1, 2, 1);
        context.setBlock(
                pipePos.west(),
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));

        GlassTankBlockEntity tank = buildExtractorRig(context, pipePos);

        context.succeedWhen(() -> {
            if (!context.getBlockState(pipePos.west()).is(Blocks.CAULDRON)) {
                throw context.assertionException("Extractor should have emptied the water cauldron");
            }
            if (tank.tank().getAmount() <= 0 || tank.tank().getFluidKey().getFluid() != Fluids.WATER) {
                throw context.assertionException("Extractor should have moved the water into the tank");
            }
        });
    }

    /**
     * Builds an extractor rig around {@code pipePos}: a tank to the east to receive, a powered Creative Engine
     * to the north to drive it, and the pull face pointed west at whatever the caller placed there. Returns the
     * receiving tank, failing the test if any block entity is missing.
     */
    private static GlassTankBlockEntity buildExtractorRig(GameTestHelper context, BlockPos pipePos) {
        BlockPos enginePos = pipePos.north();
        context.setBlock(pipePos, LogisticsPipe.BLOCK.FLUID_EXTRACTOR_PIPE);
        context.setBlock(pipePos.east(), LogisticsPipe.BLOCK.GLASS_TANK);
        context.setBlock(enginePos.north(), Blocks.REDSTONE_BLOCK);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.SOUTH)
                .setValue(AbstractEngineBlock.POWERED, true));

        FluidPipeBlockEntity pipe = context.getBlockEntity(pipePos, FluidPipeBlockEntity.class);
        GlassTankBlockEntity tank = context.getBlockEntity(pipePos.east(), GlassTankBlockEntity.class);
        CreativeEngineBlockEntity engine = context.getBlockEntity(enginePos, CreativeEngineBlockEntity.class);
        if (pipe == null || tank == null || engine == null) {
            throw context.assertionException("Expected extractor pipe, tank, and engine block entities");
        }
        pipe.setFeatureDirection(Direction.WEST);
        return tank;
    }

}

package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluid;
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
                context.assertTrue(false, "Extractor should have emptied the lava cauldron");
            }
            if (tank.tank().getAmount() <= 0 || tank.tank().getFluidKey().getFluid() != Fluids.LAVA) {
                context.assertTrue(false, "Extractor should have moved the lava into the tank");
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
                context.assertTrue(false, "Extractor should have emptied the water cauldron");
            }
            if (tank.tank().getAmount() <= 0 || tank.tank().getFluidKey().getFluid() != Fluids.WATER) {
                context.assertTrue(false, "Extractor should have moved the water into the tank");
            }
        });
    }

    /** An insertion pipe carrying a bucket of water fills an empty cauldron to its top level. */
    public static void insertionPipeFillsCauldronWithWater(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(1, 2, 1);
        BlockPos cauldronPos = pipePos.west();
        context.setBlock(cauldronPos, Blocks.CAULDRON);

        chargeInsertionPipe(context, pipePos, Fluids.WATER);

        context.succeedWhen(() -> {
            if (!context.getBlockState(cauldronPos).is(Blocks.WATER_CAULDRON)) {
                context.assertTrue(false, "Insertion pipe should have filled the cauldron with water");
            }
            if (context.getBlockState(cauldronPos).getValue(LayeredCauldronBlock.LEVEL) != 3) {
                context.assertTrue(false, "A bucket of water should fill the cauldron to its top level");
            }
        });
    }

    /** An insertion pipe carrying a bucket of lava fills an empty cauldron. */
    public static void insertionPipeFillsCauldronWithLava(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(1, 2, 1);
        BlockPos cauldronPos = pipePos.west();
        context.setBlock(cauldronPos, Blocks.CAULDRON);

        chargeInsertionPipe(context, pipePos, Fluids.LAVA);

        context.succeedWhen(() -> {
            if (!context.getBlockState(cauldronPos).is(Blocks.LAVA_CAULDRON)) {
                context.assertTrue(false, "Insertion pipe should have filled the cauldron with lava");
            }
        });
    }

    /**
     * The realistic fill: a stocked Glass Tank feeding a powered Fluid Extractor Pipe, which feeds an Insertion
     * Fluid Pipe next to an empty cauldron — no hand-placed fluid anywhere. Fluid reaches the insertion pipe
     * only in rate-sized hops that never merge afterwards, so this is the case a pre-charged pipe cannot stand
     * in for: the pipe must pool its stalled arrivals before the cauldron will take any of them.
     */
    public static void pipeNetworkFillsCauldronWithWater(GameTestHelper context) {
        BlockPos cauldronPos = new BlockPos(0, 2, 2);
        BlockPos insertionPos = cauldronPos.east();
        BlockPos extractorPos = insertionPos.east();
        BlockPos tankPos = extractorPos.east();
        BlockPos enginePos = extractorPos.north();

        context.setBlock(cauldronPos, Blocks.CAULDRON);
        context.setBlock(insertionPos, LogisticsPipe.BLOCK.INSERTION_FLUID_PIPE);
        context.setBlock(extractorPos, LogisticsPipe.BLOCK.FLUID_EXTRACTOR_PIPE);
        context.setBlock(tankPos, LogisticsPipe.BLOCK.GLASS_TANK);
        context.setBlock(enginePos.north(), Blocks.REDSTONE_BLOCK);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.SOUTH)
                .setValue(AbstractEngineBlock.POWERED, true));

        GlassTankBlockEntity tank = (GlassTankBlockEntity) context.getBlockEntity(tankPos);
        FluidPipeBlockEntity extractor = (FluidPipeBlockEntity) context.getBlockEntity(extractorPos);
        if (tank == null || extractor == null) {
            context.assertTrue(false, "Expected tank and extractor pipe block entities");
        }
        tank.setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(BUCKET_MB * 4));
        extractor.setFeatureDirection(Direction.EAST);

        context.succeedWhen(() -> {
            if (!context.getBlockState(cauldronPos).is(Blocks.WATER_CAULDRON)) {
                context.assertTrue(false, "A pipe network feeding an insertion pipe should fill the cauldron");
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

        FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) context.getBlockEntity(pipePos);
        GlassTankBlockEntity tank = (GlassTankBlockEntity) context.getBlockEntity(pipePos.east());
        CreativeEngineBlockEntity engine = (CreativeEngineBlockEntity) context.getBlockEntity(enginePos);
        if (pipe == null || tank == null || engine == null) {
            context.assertTrue(false, "Expected extractor pipe, tank, and engine block entities");
        }
        pipe.setFeatureDirection(Direction.WEST);
        return tank;
    }

    /**
     * Places an insertion pipe at {@code pipePos} holding one bucket of {@code fluid}, entering from the east so
     * it heads west into whatever the caller placed there. Fails the test if the pipe cannot hold the bucket.
     */
    private static void chargeInsertionPipe(GameTestHelper context, BlockPos pipePos, Fluid fluid) {
        context.setBlock(pipePos, LogisticsPipe.BLOCK.INSERTION_FLUID_PIPE);
        FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) context.getBlockEntity(pipePos);
        if (pipe == null) {
            context.assertTrue(false, "Expected an insertion fluid pipe block entity");
        }
        long accepted = pipe.acceptFluid(SimpleFluidKey.of(fluid), BUCKET_MB, Direction.EAST);
        if (accepted < BUCKET_MB) {
            context.assertTrue(false, "Insertion pipe should buffer a whole bucket, took only " + accepted + " mB");
        }
    }
}

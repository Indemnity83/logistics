package com.logistics.automation.pump;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What the intake tube may descend through. A block a player built to seal a pocket off has to
 * stop the tube whether or not it happens to hold water.
 */
@DisplayName("FluidPumpComponent.blocksTube")
class FluidPumpTubeDescentTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("a waterlogged block stops the tube, like the solid block it is")
    void waterloggedSolidsStopTheTube() {
        for (BlockState state : waterlogged()) {
            assertThat(FluidPumpComponent.blocksTube(state))
                    .as("%s is a solid floor that happens to hold water", state.getBlock())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("a standalone fluid does not stop the tube")
    void fluidsDoNotStopTheTube() {
        assertThat(FluidPumpComponent.blocksTube(Blocks.WATER.defaultBlockState())).isFalse();
        assertThat(FluidPumpComponent.blocksTube(Blocks.LAVA.defaultBlockState())).isFalse();
    }

    @Test
    @DisplayName("a walk-through block does not stop the tube, even though it is solid")
    void walkThroughBlocksDoNotStopTheTube() {
        // Cobweb and bamboo sapling are the two blocks vanilla reports as solid but lets entities
        // walk through. 26.3 removed BlockState#blocksMotion, which drew that distinction, so a
        // plain isSolid() check would newly stop the tube at a cobweb -- common in flooded
        // mineshafts, and a silent change to where a pump can reach.
        assertThat(FluidPumpComponent.blocksTube(Blocks.COBWEB.defaultBlockState()))
                .as("a cobweb has never stopped the tube")
                .isFalse();
        assertThat(FluidPumpComponent.blocksTube(Blocks.BAMBOO_SAPLING.defaultBlockState()))
                .as("a bamboo sapling has never stopped the tube")
                .isFalse();
    }

    @Test
    @DisplayName("air does not stop the tube and stone does")
    void airPassesAndStoneBlocks() {
        assertThat(FluidPumpComponent.blocksTube(Blocks.AIR.defaultBlockState())).isFalse();
        assertThat(FluidPumpComponent.blocksTube(Blocks.STONE.defaultBlockState())).isTrue();
    }

    private static java.util.List<BlockState> waterlogged() {
        return java.util.stream.Stream.of(Blocks.OAK_SLAB, Blocks.OAK_STAIRS, Blocks.COBBLESTONE_WALL)
                .map(block -> block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true))
                .toList();
    }
}

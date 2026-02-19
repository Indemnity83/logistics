package com.logistics.core.lib.block.behavior;

import com.logistics.core.lib.heat.HeatStage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Provides heat-related block behavior utilities.
 * Includes blockstate synchronization and particle effects for heated blocks.
 */
public final class HeatBehavior {

    /** Block state property for heat stage. */
    public static final EnumProperty<HeatStage> STAGE = EnumProperty.create("stage", HeatStage.class);

    private HeatBehavior() {
        // Utility class
    }

    /**
     * Syncs the heat stage to the block state if changed.
     *
     * @param level the world
     * @param pos block position
     * @param currentStage the new heat stage
     * @param previousStage the previous heat stage
     */
    public static void syncStageToBlock(Level level, BlockPos pos, HeatStage currentStage, HeatStage previousStage) {
        if (level == null || currentStage == previousStage) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(STAGE)) {
            BlockState newState = state.setValue(STAGE, currentStage);
            level.setBlock(pos, newState, Block.UPDATE_ALL);
        }
    }

    /**
     * Spawns heat particles for HOT and OVERHEAT stages.
     * Should be called each tick for visual feedback.
     *
     * @param level the world
     * @param pos block position
     * @param stage current heat stage
     */
    public static void spawnHeatParticles(ServerLevel level, BlockPos pos, HeatStage stage) {
        if (level.getRandom().nextInt(4) != 0) {
            return; // Spawn particles 25% of the time
        }

        double x = pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.5;

        if (stage == HeatStage.OVERHEAT) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0, 0.05, 0, 0.01);
        } else if (stage == HeatStage.HOT) {
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0, 0.05, 0, 0.01);
        }
    }
}

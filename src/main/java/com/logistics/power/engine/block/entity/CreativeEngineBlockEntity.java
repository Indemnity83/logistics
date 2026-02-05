package com.logistics.power.engine.block.entity;

import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.power.engine.block.CreativeEngineBlock;
import com.logistics.power.registry.PowerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity for the Creative Engine.
 * A special engine for Creative Mode that generates configurable amounts of energy.
 */
public class CreativeEngineBlockEntity extends AbstractEngineBlockEntity {

    // ==================== Constants ====================

    /** Output levels that double with each wrench click. */
    public static final long[] OUTPUT_LEVELS = {20, 40, 80, 160, 320, 640, 1280};

    private static final long MAX_ENERGY = 10_000L;

    // ==================== State ====================

    private int outputLevelIndex = 0;

    // ==================== Constructor & Ticker ====================

    public CreativeEngineBlockEntity(BlockPos pos, BlockState state) {
        super(PowerBlockEntities.CREATIVE_ENGINE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, CreativeEngineBlockEntity entity) {
        entity.tickEngine(world, pos, state);
    }

    // ==================== Subclass Configuration ====================

    @Override
    protected long getEnergyBufferCapacity() {
        return MAX_ENERGY;
    }

    @Override
    protected long getOutputPower() {
        return OUTPUT_LEVELS[outputLevelIndex];
    }

    @Override
    public boolean canOverheat() {
        return false;
    }

    @Override
    protected Direction getOutputDirection() {
        return CreativeEngineBlock.getOutputDirection(getBlockState());
    }

    @Override
    protected boolean isRedstonePowered() {
        return getBlockState().getValue(CreativeEngineBlock.POWERED);
    }

    @Override
    protected boolean sendsEnergyContinuously() {
        return true;
    }

    @Override
    protected HeatStage computeStage() {
        return HeatStage.COLD;
    }

    @Override
    public float getPistonSpeed() {
        return 0.02F * (outputLevelIndex + 1);
    }

    // ==================== Lifecycle Hooks ====================

    /**
     * Creative engine generates infinite energy - buffer is always full when running.
     */
    @Override
    protected void produceEnergy() {
        if (!isRedstonePowered()) {
            return;
        }

        // Infinite energy generation - always fill buffer to max
        energy = getEnergyBufferCapacity();
    }

    // ==================== Output Level Control ====================

    /**
     * Cycles to the next output level (doubles the output rate).
     * Wraps around to minimum when maximum is exceeded.
     *
     * @return the new output rate in RF/t
     */
    public long cycleOutputLevel() {
        outputLevelIndex = (outputLevelIndex + 1) % OUTPUT_LEVELS.length;
        setChanged();

        // Sync to clients so renderer can update piston speed
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        return OUTPUT_LEVELS[outputLevelIndex];
    }

    /**
     * Gets the current output level index.
     */
    public int getOutputLevelIndex() {
        return outputLevelIndex;
    }

    /**
     * Gets the current output rate in RF/t.
     */
    public long getOutputRate() {
        return OUTPUT_LEVELS[outputLevelIndex];
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);

        CompoundTag creativeData = new CompoundTag();
        creativeData.putInt("outputLevelIndex", outputLevelIndex);
        view.store("CreativeData", CompoundTag.CODEC, creativeData);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);

        view.read("CreativeData", CompoundTag.CODEC).ifPresent(creativeData -> {
            outputLevelIndex = creativeData.getInt("outputLevelIndex").orElse(0);
            // Clamp to valid range
            if (outputLevelIndex < 0 || outputLevelIndex >= OUTPUT_LEVELS.length) {
                outputLevelIndex = 0;
            }
        });
    }
}

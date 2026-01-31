package com.logistics.power.engine.block.entity;

import com.logistics.power.engine.block.CreativeEngineBlock;
import com.logistics.power.registry.PowerBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

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

    public static void tick(World world, BlockPos pos, BlockState state, CreativeEngineBlockEntity entity) {
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
        return CreativeEngineBlock.getOutputDirection(getCachedState());
    }

    @Override
    protected boolean isRedstonePowered() {
        return getCachedState().get(CreativeEngineBlock.POWERED);
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
        markDirty();

        // Sync to clients so renderer can update piston speed
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
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
    protected void writeData(WriteView view) {
        super.writeData(view);

        NbtCompound creativeData = new NbtCompound();
        creativeData.putInt("outputLevelIndex", outputLevelIndex);
        view.put("CreativeData", NbtCompound.CODEC, creativeData);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        view.read("CreativeData", NbtCompound.CODEC).ifPresent(creativeData -> {
            outputLevelIndex = creativeData.getInt("outputLevelIndex").orElse(0);
            // Clamp to valid range
            if (outputLevelIndex < 0 || outputLevelIndex >= OUTPUT_LEVELS.length) {
                outputLevelIndex = 0;
            }
        });
    }
}

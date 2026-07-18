package com.logistics.power.engine.block.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.EngineBuilder;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.lib.power.component.EngineHeatComponent;
import com.logistics.power.engine.block.CreativeEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Creative Engine: generates infinite energy for testing. Fills its buffer every tick while
 * powered, never overheats (stage pinned to COLD), and its output rate — and piston speed — cycle
 * through doubling levels on a sneak-wrench.
 */
public class CreativeEngineBlockEntity extends EngineEntity {

    /** Output levels that double with each wrench click. */
    public static final long[] OUTPUT_LEVELS = CreativeOutputLevels.DEFAULT_LEVELS.clone();

    private final CreativeOutputLevels outputLevels = new CreativeOutputLevels();

    private EngineEnergyOutputComponent energy;
    private EngineHeatComponent heat;

    public CreativeEngineBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CreativeEngineBlockEntity entity) {
        EngineEntity.tick(level, pos, state, entity);
    }

    @Override
    protected void configure(EngineBuilder engine) {
        energy = engine.energyOutput("energy")
                .capacity(() -> LogisticsConfigHost.get(LogisticsPower.CONFIG.CREATIVE_BUFFER_CAPACITY))
                .build();
        heat = engine.heat("heat")
                .energy(energy)
                .canOverheat(false)
                .stageOverride(HeatStage.COLD)
                .running(this::isRunning)
                .build();
        engine.constantFill("burn")
                .energy(energy)
                .powered(this::isPowered)
                .levelIndex(() -> outputLevels.index(), value -> outputLevels.restore(value))
                .build();
        engine.pistonCycle("cycle")
                .energy(energy)
                .heat(heat)
                .powered(this::isPowered)
                .pistonSpeed(() -> outputLevels.pistonSpeed())
                .outputPower(() -> outputLevels.outputRate())
                .outputFace(() -> CreativeEngineBlock.getOutputDirection(getBlockState()))
                .sendsEnergyContinuously(true)
                .build();
    }

    /** Cycles to the next output level (doubles the output rate); syncs so the renderer updates. */
    public long cycleOutputLevel() {
        long outputRate = outputLevels.cycle();
        markDirtyAndSync();
        return outputRate;
    }

    public int getOutputLevelIndex() {
        return outputLevels.index();
    }

    public long getOutputRate() {
        return outputLevels.outputRate();
    }
}

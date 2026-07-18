package com.logistics.power.engine.block.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.EngineBuilder;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.lib.power.LowTierEnergySource;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.lib.power.component.EngineHeatComponent;
import com.logistics.core.lib.power.component.EnginePistonCycleComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.power.engine.block.RedstoneEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Redstone Engine: the simplest engine, converting a redstone signal into a trickle of energy
 * with no fuel. Composed of an output buffer, a non-overheating heat model, an interval generator,
 * and a piston cycle.
 */
public class RedstoneEngineBlockEntity extends EngineEntity implements LowTierEnergySource {

    private EngineEnergyOutputComponent energy;
    private EngineHeatComponent heat;
    private EnginePistonCycleComponent cycle;

    public RedstoneEngineBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RedstoneEngineBlockEntity entity) {
        EngineEntity.tick(level, pos, state, entity);
    }

    @Override
    protected void configure(EngineBuilder engine) {
        energy = engine.energyOutput("energy")
                .capacity(() -> LogisticsConfigHost.get(LogisticsPower.CONFIG.REDSTONE_BUFFER_CAPACITY))
                .build();
        heat = engine.heat("heat")
                .energy(energy)
                .canOverheat(false)
                .running(this::isRunning)
                .compression(() -> cycle.isCompression())
                .build();
        engine.intervalGen("burn")
                .energy(energy)
                .heat(heat)
                .powered(this::isPowered)
                .output(() -> LogisticsConfigHost.get(LogisticsPower.CONFIG.REDSTONE_OUTPUT))
                .interval(() -> LogisticsConfigHost.get(LogisticsPower.CONFIG.REDSTONE_GENERATION_INTERVAL))
                .runningGate(this::targetAcceptsLowTier)
                .build();
        cycle = engine.pistonCycle("cycle")
                .energy(energy)
                .heat(heat)
                .powered(this::isPowered)
                .pistonSpeed(heat::getPistonSpeed)
                .outputPower(() -> LogisticsConfigHost.get(LogisticsPower.CONFIG.REDSTONE_OUTPUT))
                .outputFace(() -> RedstoneEngineBlock.getOutputDirection(getBlockState()))
                .sendsEnergyContinuously(false)
                .build();
    }

    /** Redstone engines refuse to run unless the target accepts low-tier energy. */
    private boolean targetAcceptsLowTier(MachineContext ctx) {
        Level level = ctx.level();
        if (level == null) {
            return false;
        }
        Direction outputDir = RedstoneEngineBlock.getOutputDirection(getBlockState());
        BlockEntity target = level.getBlockEntity(ctx.pos().relative(outputDir));
        return RedstoneTargetGate.acceptsLowTierEnergy(target, outputDir);
    }
}

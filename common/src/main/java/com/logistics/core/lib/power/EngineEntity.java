package com.logistics.core.lib.power;

import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.lib.power.component.EnginePistonCycleComponent;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineComponentContainer;
import com.logistics.core.machine.MachineContext;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Component-hosted base for engine block entities — the source-side sibling of
 * {@code MachineEntity}. Holds a {@link MachineComponentContainer}, ticks it, dispatches save/load,
 * and answers the block/renderer-facing getters (heat, piston speed, running) by querying the
 * engine capability SPIs in {@link EngineComponent}.
 *
 * <p>Engines are energy <em>sources</em>: energy is exposed only on the output face and never
 * accepted, so this base intentionally omits the demand/sink routing {@code MachineEntity} has.
 * Item storage and menus are engine-specific (only the Stirling Engine has them), so those
 * interfaces are declared by the concrete engine rather than here.
 */
public abstract class EngineEntity extends BaseBlockEntity implements MachineContext, HasEnergyStorage {

    /** Client-side callback for cleanup when an engine is removed. Set by client bootstrap. */
    private static Consumer<BlockPos> onRemovedCallback;

    protected final MachineComponentContainer components = new MachineComponentContainer();
    private boolean loaded = false;

    protected EngineEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        configure(new EngineBuilder(components, this::setChanged));
    }

    /** Compose this engine's components via the fluent builder. Called once from the constructor. */
    protected abstract void configure(EngineBuilder engine);

    public static void tick(Level level, BlockPos pos, BlockState state, EngineEntity engine) {
        if (level.isClientSide()) {
            return;
        }
        if (!engine.loaded) {
            engine.components.onLoad(engine);
            engine.loaded = true;
        }
        engine.components.serverTick(engine);
    }

    // ==================== Energy source (output-face gated) ====================

    @Override
    @Nullable
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        if (side != null && side != AbstractEngineBlock.getOutputDirection(getBlockState())) {
            return null;
        }
        return components.find(MachineComponent.EnergyAccess.class).map(a -> a.energy(side)).orElse(null);
    }

    // ==================== Engine getters (block + renderer facing) ====================

    /** Running when powered, not overheated, and every running gate is satisfied. */
    public boolean isRunning() {
        if (!isPowered() || isOverheated()) {
            return false;
        }
        boolean[] running = {true};
        components.forEach(EngineComponent.RunningGate.class, gate -> {
            if (!gate.isRunning(this)) {
                running[0] = false;
            }
        });
        return running[0];
    }

    public boolean isPowered() {
        return getBlockState().getValue(AbstractEngineBlock.POWERED);
    }

    public float getPistonSpeed() {
        return components.find(EngineComponent.PistonState.class).map(EngineComponent.PistonState::pistonSpeed).orElse(0f);
    }

    public boolean isOverheated() {
        return heat().map(EngineComponent.HeatState::isOverheated).orElse(false);
    }

    public boolean canOverheat() {
        return heat().map(EngineComponent.HeatState::canOverheat).orElse(true);
    }

    public boolean resetOverheat() {
        return heat().map(h -> h.resetOverheat(this)).orElse(false);
    }

    public double getTemperature() {
        return heat().map(EngineComponent.HeatState::temperature).orElse(0.0);
    }

    public double getMaxTemperature() {
        return heat().map(EngineComponent.HeatState::maxTemperature).orElse(0.0);
    }

    public long getCurrentOutputPower() {
        return components.find(EnginePistonCycleComponent.class)
                .map(EnginePistonCycleComponent::outputPower)
                .orElse(0L);
    }

    public HeatStage getHeatStage() {
        return heat().map(EngineComponent.HeatState::stage).orElse(HeatStage.COLD);
    }

    public long getEnergy() {
        return components.find(EngineEnergyOutputComponent.class).map(EngineEnergyOutputComponent::getAmount).orElse(0L);
    }

    public long getMaxEnergy() {
        return components.find(EngineEnergyOutputComponent.class).map(EngineEnergyOutputComponent::getCapacity).orElse(0L);
    }

    public float getProgress() {
        return components.find(EnginePistonCycleComponent.class).map(EnginePistonCycleComponent::progress).orElse(0f);
    }

    private Optional<EngineComponent.HeatState> heat() {
        return components.find(EngineComponent.HeatState.class);
    }

    // The look-at (Jade) HUD is captured on this branch by EngineHudData/EngineServerDataProvider,
    // not the 26.x component-HUD-model path (MachineHudModel is unavailable on 1.21.1).

    // ==================== MachineContext ====================

    @Override
    public Level level() {
        return this.level;
    }

    @Override
    public BlockPos pos() {
        return this.worldPosition;
    }

    @Override
    public BlockState blockState() {
        return getBlockState();
    }

    @Override
    public HolderLookup.Provider registries() {
        return level != null
                ? level.registryAccess()
                : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Override
    public void sync() {
        markDirtyAndSync();
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    public void setBlockState(BlockState state, int flags) {
        if (level != null) {
            level.setBlock(worldPosition, state, flags);
        }
    }

    @Override
    @Nullable
    public RecipeManager recipeManager() {
        return level instanceof ServerLevel serverLevel ? serverLevel.getServer().getRecipeManager() : null;
    }

    @Override
    public RandomSource random() {
        return level != null ? level.getRandom() : RandomSource.create();
    }

    // ==================== Persistence ====================

    @Override
    protected void saveLogisticsData(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider registries) {
        components.save(tag, registries);
    }

    @Override
    protected void loadLogisticsData(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider registries) {
        components.load(tag, registries);
    }

    // ==================== Lifecycle ====================

    /** Sets a callback invoked when an engine is removed; used client-side to clean up render caches. */
    public static void setOnRemovedCallback(Consumer<BlockPos> callback) {
        onRemovedCallback = callback;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (onRemovedCallback != null && level != null && level.isClientSide()) {
            onRemovedCallback.accept(getBlockPos());
        }
    }
}

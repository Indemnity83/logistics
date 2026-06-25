package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.EnergyDemandProvider;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.upgrade.MachineModifiers;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Laser Quarry block entity. Holds the RF buffer (the capability surface) and the
 * {@link QuarryComponent} that owns all mining behavior, and serves as the component's
 * {@link MachineContext}. Most public methods are thin facades the renderer / HUD / frame block /
 * markers call.
 */
public class LaserQuarryBlockEntity extends BaseBlockEntity
        implements PipeConnection, HasEnergyStorage, EnergyDemandProvider, MachineContext {

    // Energy buffer — the capability surface; tracks received energy for demand + the HUD readout.
    private final EnergyStorageComponent energy = new EnergyStorageComponent(
            "energy",
            LogisticsConfig.get().quarry.energyCapacity(),
            LogisticsConfig.get().quarry.maxEnergyInput(),
            0,
            true,
            true,
            this::setChanged);

    // All mining behavior (bounds, arm, phase runner, energy policy, output, chunk loading).
    private final QuarryComponent quarry;

    public LaserQuarryBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, pos, state);
        this.quarry = new QuarryComponent("quarry", pos, energy, MachineModifiers.identity());
    }

    // ==================== HasEnergyStorage ====================

    @Override
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        return energy.energy(side);
    }

    // ==================== Tick ====================

    public static void tick(Level world, BlockPos pos, BlockState state, LaserQuarryBlockEntity entity) {
        if (world.isClientSide()) {
            return;
        }

        // Reset the energy-received counter (rolls the "last tick" value for the HUD).
        entity.energy.serverTick(entity);

        entity.quarry.serverTick(entity);
    }

    /**
     * Sync arm state to clients. Called on arm state transitions.
     *
     * <p>Does not call {@code setChanged()} — chunk dirty is managed separately, marked when energy
     * is consumed or mining advances.
     */
    void syncToClients() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    // ==================== MachineContext (host view for QuarryComponent) ====================

    @Override
    public Level level() {
        return this.level;
    }

    @Override
    public BlockPos pos() {
        return worldPosition;
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
        syncToClients();
    }

    @Override
    public void setBlockState(BlockState newState, int flags) {
        if (level != null) {
            level.setBlock(worldPosition, newState, flags);
        }
    }

    @Override
    @Nullable
    public RecipeManager recipeManager() {
        return null;
    }

    @Override
    public RandomSource random() {
        return level != null ? level.getRandom() : RandomSource.create();
    }

    // ==================== Marker / bounds API ====================

    public void setCustomBounds(int minX, int minZ, int maxX, int maxZ) {
        quarry.setCustomBounds(minX, minZ, maxX, maxZ);
        setChanged();
    }

    // ==================== Energy diagnostics ====================

    public double getEnergyLevel() {
        return (double) energy.amount() / LogisticsConfig.get().quarry.energyCapacity();
    }

    @Override
    public long networkDemandPerTick() {
        return energy.networkDemandPerTick();
    }

    public long getEnergyReceivedLastTick() {
        return energy.receivedLastTick();
    }

    // ==================== NBT ====================

    @Override
    protected void saveLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveLogisticsData(nbt, registries);

        energy.saveLegacy(nbt);
        quarry.save(nbt, registries);
    }

    @Override
    protected void loadLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLogisticsData(nbt, registries);

        if (nbt.contains("Energy")) {
            energy.loadLegacy(nbt, registries);
        } else {
            energy.setAmount(NbtCompat.getLong(nbt, "StoredEnergy", 0L));
        }

        quarry.load(nbt, registries);
    }

    // ==================== Lifecycle ====================

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);

        if (level != null && level.isClientSide()) {
            ClientRenderCacheHooks.clearQuarryInterpolationCache(pos);
        }

        if (level != null && !level.isClientSide()) {
            ActiveQuarryRegistry.unregister((ServerLevel) level, pos);
            BlockPos currentTarget = quarry.currentTarget();
            if (currentTarget != null) {
                ((ServerLevel) level).destroyBlockProgress(quarry.breakingEntityIdValue(), currentTarget, -1);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide()) {
            ClientRenderCacheHooks.clearQuarryInterpolationCache(worldPosition);
        }
    }

    // ==================== Public getters (renderer / frame block / HUD) ====================

    public QuarryPhase getCurrentPhase() {
        return quarry.phase();
    }

    public float getArmX() {
        return quarry.armX();
    }

    public float getArmY() {
        return quarry.armY();
    }

    public float getArmZ() {
        return quarry.armZ();
    }

    public QuarryArmState getArmState() {
        return quarry.armState();
    }

    public float getSyncedArmSpeed() {
        return quarry.syncedArmSpeed();
    }

    public boolean isArmInitialized() {
        return quarry.armInitialized();
    }

    public boolean isFinished() {
        return quarry.isFinished();
    }

    public boolean consumedEnergyThisTick() {
        return quarry.consumedEnergyThisTick();
    }

    public boolean hasCustomBounds() {
        return quarry.hasCustomBounds();
    }

    public int getCustomMinX() {
        return quarry.customMinX();
    }

    public int getCustomMinZ() {
        return quarry.customMinZ();
    }

    public int getCustomMaxX() {
        return quarry.customMaxX();
    }

    public int getCustomMaxZ() {
        return quarry.customMaxZ();
    }

    /** True if the quarry has been placed but hasn't started any clearing yet. */
    public boolean isFreshlyPlaced() {
        return quarry.isFreshlyPlaced();
    }

    public static List<BlockPos> getActiveQuarries(ServerLevel world) {
        return ActiveQuarryRegistry.getAll(world);
    }

    public static void clearActiveQuarries(ServerLevel world) {
        ActiveQuarryRegistry.clear(world);
    }

    // ==================== PipeConnection ====================

    /**
     * Quarry accepts pipe connections from above so pipes render arms to it.
     * Other directions report no connection.
     */
    @Override
    public PipeConnection.Type getConnectionType(Direction direction) {
        return direction == Direction.UP ? PipeConnection.Type.PIPE : PipeConnection.Type.NONE;
    }

    /** Quarry never accepts items from pipes — it only pushes them out. */
    @Override
    public boolean addItem(Direction from, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canAcceptFrom(Direction from, ItemStack stack) {
        return false;
    }
}

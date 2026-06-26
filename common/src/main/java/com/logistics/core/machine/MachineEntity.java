package com.logistics.core.machine;

import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.ProcessingMachine;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.block.capability.HasExperienceStorage;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.power.EnergyDemandProvider;
import com.logistics.core.lib.storage.ContainerItemStorage;
import com.logistics.core.lib.storage.IItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Boring infrastructure base for component-hosted machines.
 *
 * <p>Holds a {@link MachineComponentContainer}, ticks it, dispatches save/load, and routes the
 * loader-agnostic capability interfaces ({@link HasItemStorage}/{@link HasEnergyStorage}/
 * {@link HasFluidStorage}, {@link WorldlyContainer}, {@link EnergyDemandProvider},
 * {@link ProcessingMachine}) to whichever component provides them. Subclasses supply only their
 * component set in {@link #configure(MachineComponentContainer)} and read like configuration.
 *
 * <p>Persistence nests under the existing {@code LogisticsData} root as {@code LogisticsData/components}.
 */
public abstract class MachineEntity extends BaseBlockEntity
        implements MachineContext, HasItemStorage, HasEnergyStorage, HasFluidStorage,
                HasExperienceStorage, WorldlyContainer, MenuBehavior.HasMenu, EnergyDemandProvider, ProcessingMachine {

    private static final int[] NO_SLOTS = new int[0];

    protected final MachineComponentContainer components = new MachineComponentContainer();
    private boolean loaded = false;

    protected MachineEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        configure(new MachineBuilder(components, this::setChanged));
    }

    /** Compose this machine's components via the fluent builder. Called once from the constructor. */
    protected abstract void configure(MachineBuilder machine);

    public static void tick(Level level, BlockPos pos, BlockState state, MachineEntity machine) {
        if (level.isClientSide()) {
            return;
        }
        if (!machine.loaded) {
            machine.components.onLoad(machine);
            machine.loaded = true;
        }
        machine.components.serverTick(machine);
    }

    // ==================== Capability routing ====================

    @Override
    public IItemStorage itemStorage(@Nullable Direction side) {
        return sidedItems().map(items -> (IItemStorage) new ContainerItemStorage(this, side)).orElse(null);
    }

    @Override
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        return components.find(MachineComponent.EnergyAccess.class).map(a -> a.energy(side)).orElse(null);
    }

    @Override
    public IFluidStorage fluidStorage(@Nullable Direction side) {
        return components.find(MachineComponent.FluidAccess.class).map(a -> a.fluid(side)).orElse(null);
    }

    @Override
    public long networkDemandPerTick() {
        return components.find(MachineComponent.Demand.class).map(MachineComponent.Demand::networkDemandPerTick).orElse(0L);
    }

    @Override
    public int drainExperience(RandomSource random) {
        return components.find(MachineComponent.ExperienceStore.class).map(s -> s.drainExperience(random)).orElse(0);
    }

    @Override
    public float processProgress() {
        return components.find(MachineComponent.ProcessState.class).map(MachineComponent.ProcessState::progress).orElse(0f);
    }

    @Override
    public boolean isProcessing() {
        return components.find(MachineComponent.ProcessState.class).map(MachineComponent.ProcessState::isProcessing).orElse(false);
    }

    private java.util.Optional<MachineComponent.SidedItems> sidedItems() {
        return components.find(MachineComponent.SidedItems.class);
    }

    // ==================== WorldlyContainer delegation ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        return sidedItems().map(s -> s.slotsForFace(side)).orElse(NO_SLOTS);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return direction != null && sidedItems().map(s -> s.canPlace(slot, stack, direction)).orElse(false);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return sidedItems().map(s -> s.canTake(slot, stack, direction)).orElse(false);
    }

    @Override
    public int getContainerSize() {
        return sidedItems().map(s -> s.container().getContainerSize()).orElse(0);
    }

    @Override
    public boolean isEmpty() {
        return sidedItems().map(s -> s.container().isEmpty()).orElse(true);
    }

    @Override
    public ItemStack getItem(int slot) {
        return sidedItems().map(s -> s.container().getItem(slot)).orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return sidedItems().map(s -> s.container().removeItem(slot, amount)).orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return sidedItems().map(s -> s.container().removeItemNoUpdate(slot)).orElse(ItemStack.EMPTY);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        sidedItems().ifPresent(s -> s.container().setItem(slot, stack));
    }

    @Override
    public boolean stillValid(Player player) {
        return sidedItems().map(s -> s.container().stillValid(player)).orElse(true);
    }

    @Override
    public void clearContent() {
        sidedItems().ifPresent(s -> s.container().clearContent());
    }

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
    protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        components.save(tag, registries);
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        components.load(tag, registries);
    }
}

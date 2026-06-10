package com.logistics.core.macerator;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.ProcessingMachine;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.items.ItemInventoryComponent;
import com.logistics.core.lib.power.EnergyDemandProvider;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.storage.ContainerItemStorage;
import com.logistics.core.lib.storage.IItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.logistics.core.lib.energy.IEnergyStorage;

/**
 * Block entity for the Iron Macerator.
 * Grinds input items into dust using RF energy.
 *
 * <p>Inventory layout (2 slots):
 * <ul>
 *   <li>Slot 0: Input item</li>
 *   <li>Slot 1: Output dust</li>
 * </ul>
 *
 * <p>Sided access (like a furnace):
 * <ul>
 *   <li>TOP: Input slot only</li>
 *   <li>SIDES: Input slot only</li>
 *   <li>BOTTOM: Output slot only</li>
 * </ul>
 */
public class MaceratorBlockEntity extends BaseBlockEntity
    implements HasItemStorage, HasEnergyStorage, WorldlyContainer, MenuBehavior.HasMenu, EnergyDemandProvider,
        ProcessingMachine {

    static final int INPUT_SLOT = 0;
    static final int OUTPUT_SLOT = 1;
    private static final int TOTAL_SLOTS = 2;

    static final long ENERGY_CAPACITY = 10_000L;
    static final long MAX_ENERGY_INPUT = 128L;

    // Processing constants — tuned so 1 coal in a Stirling Engine (10 RF/t at full temp, 1600 ticks)
    // produces exactly 16,000 RF, enough to macerate 8 items (8 × 2,000 RF each).
    static final int ENERGY_PER_TICK = 10;

    private final ItemInventoryComponent inventory = new ItemInventoryComponent(TOTAL_SLOTS, this::setChanged);
    final EnergyComponent energy = new EnergyComponent(ENERGY_CAPACITY, MAX_ENERGY_INPUT, 0, this::setChanged);
    private long energyReceivedThisTick = 0;
    private final IEnergyStorage trackingStorage = new IEnergyStorage() {
        @Override
        public long insert(long maxAmount, boolean simulate) {
            long inserted = energy.insert(maxAmount, simulate);
            if (!simulate && inserted > 0) energyReceivedThisTick += inserted;
            return inserted;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            return energy.extract(maxAmount, simulate);
        }

        @Override
        public long getAmount() { return energy.getAmount(); }

        @Override
        public long getCapacity() { return energy.getCapacity(); }

        @Override
        public boolean canInsert() { return energy.canInsert(); }

        @Override
        public boolean canExtract() { return energy.canExtract(); }
    };

    @Nullable private RecipeHolder<MaceratorRecipeWrapper> activeRecipe = null;
    int processProgress = 0;

    // ContainerData indices for GUI sync
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_TOTAL_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    static final int DATA_COUNT = 3;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> processProgress;
                case DATA_TOTAL_TICKS -> activeRecipe != null ? activeRecipe.value().grindingTime() : 0;
                case DATA_ENERGY -> (int) Math.min(energy.getAmount(), Integer.MAX_VALUE);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> processProgress = value;
                case DATA_ENERGY -> energy.setAmount(Math.min(value, ENERGY_CAPACITY));
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MaceratorBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsCore.ENTITY.MACERATOR_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MaceratorBlockEntity entity) {
        if (level.isClientSide()) return;
        entity.energyReceivedThisTick = 0;
        if (entity.tickProcessing(level, state)) {
            entity.setChanged();
        }
    }

    private boolean tickProcessing(Level level, BlockState state) {
        // If no active recipe, try to find one
        if (activeRecipe == null) {
            activeRecipe = findMatchingRecipe(level);
            if (activeRecipe == null || !canStartProcessing(activeRecipe)) {
                setLit(level, state, false);
                return false;
            }
        }

        MaceratorRecipeWrapper recipe = activeRecipe.value();

        // Cancel if input no longer matches
        if (!recipe.matches(inventory.getItem(INPUT_SLOT))) {
            activeRecipe = null;
            processProgress = 0;
            setLit(level, state, false);
            return true;
        }

        MaceratorProcessingPlan.Result plan = MaceratorProcessingPlan.advance(
            processProgress,
            recipe.grindingTime(),
            energy.getAmount(),
            ENERGY_PER_TICK,
            canAcceptOutput(recipe.getResultItem()));

        if (!plan.consumedEnergy()) {
            setLit(level, state, false);
            return false;
        }

        energy.consume(ENERGY_PER_TICK);
        setLit(level, state, true);
        processProgress = plan.progress();

        if (plan.complete()) {
            completeProcessing(recipe);
        }

        return true;
    }

    @Nullable
    private RecipeHolder<MaceratorRecipeWrapper> findMatchingRecipe(Level level) {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (input.isEmpty()) return null;
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getRecipeManager()
            .getRecipeFor(LogisticsCore.RECIPE.MACERATOR_RECIPE_TYPE, new SingleRecipeInput(input), level)
            .orElse(null);
    }

    private boolean canStartProcessing(RecipeHolder<MaceratorRecipeWrapper> recipe) {
        return energy.getAmount() >= ENERGY_PER_TICK
            && canAcceptOutput(recipe.value().getResultItem());
    }

    private boolean canAcceptOutput(ItemStack result) {
        return MaceratorProcessingPlan.acceptsOutput(inventory.getItem(OUTPUT_SLOT), result);
    }

    private void completeProcessing(MaceratorRecipeWrapper recipe) {
        inventory.getItem(INPUT_SLOT).shrink(1);

        ItemStack result = recipe.getResultItem();
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setItem(OUTPUT_SLOT, result);
        } else {
            output.grow(result.getCount());
        }

        awardExperience(recipe.experience());

        activeRecipe = null;
        processProgress = 0;
    }

    private void awardExperience(float experience) {
        if (experience <= 0 || !(level instanceof ServerLevel serverLevel)) return;
        int xp = (int) experience;
        if (level.random.nextFloat() < (experience - xp)) xp++;
        if (xp > 0) ExperienceOrb.award(serverLevel, Vec3.atCenterOf(getBlockPos()), xp);
    }

    private void setLit(Level level, BlockState state, boolean lit) {
        boolean wasLit = state.getValue(MaceratorBlock.LIT);
        if (lit != wasLit) {
            level.setBlock(getBlockPos(), state.setValue(MaceratorBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    // ==================== Capability Implementations ====================

    @Override
    public IItemStorage itemStorage(@Nullable Direction side) {
        return new ContainerItemStorage(this, side);
    }

    @Override
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        return trackingStorage;
    }

    @Override
    public long networkDemandPerTick() {
        long storageRoom = Math.max(0, ENERGY_CAPACITY - energy.getAmount());
        long remainingInput = Math.max(0, MAX_ENERGY_INPUT - energyReceivedThisTick);
        return Math.min(remainingInput, storageRoom);
    }

    // ==================== ProcessingMachine ====================

    @Override
    public float processProgress() {
        if (activeRecipe == null) {
            return 0f;
        }
        int total = activeRecipe.value().grindingTime();
        return total > 0 ? Math.min(1f, (float) processProgress / total) : 0f;
    }

    @Override
    public boolean isProcessing() {
        return activeRecipe != null;
    }

    // ==================== WorldlyContainer (Sided Inventory) ====================

    private static final int[] SLOTS_INPUT = new int[]{INPUT_SLOT};
    private static final int[] SLOTS_OUTPUT = new int[]{OUTPUT_SLOT};

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_OUTPUT : SLOTS_INPUT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction != Direction.DOWN && slot == INPUT_SLOT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction == Direction.DOWN && slot == OUTPUT_SLOT;
    }

    // ==================== Container Delegation ====================

    @Override
    public int getContainerSize() { return inventory.getContainerSize(); }

    @Override
    public boolean isEmpty() { return inventory.isEmpty(); }

    @Override
    public ItemStack getItem(int slot) { return inventory.getItem(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) { return inventory.removeItem(slot, amount); }

    @Override
    public ItemStack removeItemNoUpdate(int slot) { return inventory.removeItemNoUpdate(slot); }

    @Override
    public void setItem(int slot, ItemStack stack) { inventory.setItem(slot, stack); }

    @Override
    public boolean stillValid(Player player) { return inventory.stillValid(player); }

    @Override
    public void clearContent() { inventory.clearContent(); }

    // ==================== MenuBehavior ====================

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.logistics.macerator");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new MaceratorScreenHandler(syncId, playerInventory, MaceratorBlockEntity.this, containerData);
            }
        };
    }

    // ==================== NBT ====================

    @Override
    protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        inventory.writeNbt(tag, "Inventory", registries);
        energy.writeNbt(tag, "Energy");
        tag.putInt("ProcessProgress", processProgress);
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        inventory.readNbt(tag, "Inventory", registries);
        energy.readNbt(tag, "Energy");
        processProgress = NbtCompat.getInt(tag, "ProcessProgress", 0);
        // activeRecipe is re-resolved on the next tick from the vanilla recipe manager
    }
}

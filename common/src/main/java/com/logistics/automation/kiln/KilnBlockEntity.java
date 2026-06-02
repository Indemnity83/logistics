package com.logistics.automation.kiln;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.items.ItemInventoryComponent;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.logistics.core.lib.energy.IEnergyStorage;

/**
 * Block entity for the Electric Kiln.
 * Smelts any vanilla smelting recipe using RF energy.
 *
 * <p>Inventory layout (2 slots):
 * <ul>
 *   <li>Slot 0: Input item</li>
 *   <li>Slot 1: Output</li>
 * </ul>
 *
 * <p>Sided access:
 * <ul>
 *   <li>TOP: Input slot</li>
 *   <li>SIDES: Input slot</li>
 *   <li>BOTTOM: Output slot</li>
 * </ul>
 */
public class KilnBlockEntity extends BaseBlockEntity
    implements HasItemStorage, HasEnergyStorage, WorldlyContainer, MenuBehavior.HasMenu {

    static final int INPUT_SLOT = 0;
    static final int OUTPUT_SLOT = 1;
    private static final int TOTAL_SLOTS = 2;

    static final long ENERGY_CAPACITY = 10_000L;
    static final long MAX_ENERGY_INPUT = 128L;

    static final int ENERGY_PER_TICK = 1;

    private final ItemInventoryComponent inventory = new ItemInventoryComponent(TOTAL_SLOTS, this::setChanged);
    final EnergyComponent energy = new EnergyComponent(ENERGY_CAPACITY, MAX_ENERGY_INPUT, 0, this::setChanged);

    @Nullable private RecipeHolder<SmeltingRecipe> activeRecipe = null;
    int processProgress = 0;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_TOTAL_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    static final int DATA_COUNT = 3;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> processProgress;
                case DATA_TOTAL_TICKS -> activeRecipe != null ? activeRecipe.value().cookingTime() : 0;
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

    public KilnBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.KILN_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, KilnBlockEntity entity) {
        if (level.isClientSide()) return;
        if (entity.tickProcessing(level, state)) {
            entity.setChanged();
        }
    }

    private boolean tickProcessing(Level level, BlockState state) {
        if (activeRecipe == null) {
            activeRecipe = findMatchingRecipe(level);
            if (activeRecipe == null || !canStartProcessing(activeRecipe, level)) {
                setLit(level, state, false);
                return false;
            }
        }

        ItemStack input = inventory.getItem(INPUT_SLOT);

        // Cancel if input no longer matches
        if (!activeRecipe.value().matches(new SingleRecipeInput(input), level)) {
            activeRecipe = null;
            processProgress = 0;
            setLit(level, state, false);
            return true;
        }

        ItemStack result = activeRecipe.value().assemble(new SingleRecipeInput(input), level.registryAccess());

        KilnProcessingPlan.Result plan = KilnProcessingPlan.advance(
                processProgress,
                activeRecipe.value().cookingTime(),
                energy.getAmount(),
                ENERGY_PER_TICK,
                canAcceptOutput(result));

        if (!plan.consumedEnergy()) {
            setLit(level, state, false);
            return false;
        }

        energy.consume(ENERGY_PER_TICK);
        setLit(level, state, true);
        processProgress = plan.progress();

        if (plan.complete()) {
            completeProcessing(result);
        }

        return true;
    }

    @Nullable
    private RecipeHolder<SmeltingRecipe> findMatchingRecipe(Level level) {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (input.isEmpty()) return null;
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getRecipeManager()
            .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level)
            .orElse(null);
    }

    private boolean canStartProcessing(RecipeHolder<SmeltingRecipe> recipe, Level level) {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        ItemStack result = recipe.value().assemble(new SingleRecipeInput(input), level.registryAccess());
        return energy.getAmount() >= ENERGY_PER_TICK && canAcceptOutput(result);
    }

    private boolean canAcceptOutput(ItemStack result) {
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        return KilnProcessingPlan.acceptsOutput(output, result);
    }

    private void completeProcessing(ItemStack result) {
        inventory.getItem(INPUT_SLOT).shrink(1);

        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setItem(OUTPUT_SLOT, result);
        } else {
            output.grow(result.getCount());
        }

        activeRecipe = null;
        processProgress = 0;
    }

    private void setLit(Level level, BlockState state, boolean lit) {
        boolean wasLit = state.getValue(KilnBlock.LIT);
        if (lit != wasLit) {
            level.setBlock(getBlockPos(), state.setValue(KilnBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    // ==================== Capability Implementations ====================

    @Override
    public IItemStorage itemStorage(@Nullable Direction side) {
        return new ContainerItemStorage(this, side);
    }

    @Override
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        return energy;
    }

    // ==================== WorldlyContainer (Sided Inventory) ====================

    private static final int[] SLOTS_INPUT = new int[]{INPUT_SLOT};
    private static final int[] SLOTS_OUTPUT = new int[]{OUTPUT_SLOT};
    private static final int[] SLOTS_EMPTY = new int[]{};

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return SLOTS_INPUT;
        if (side == Direction.DOWN) return SLOTS_OUTPUT;
        return SLOTS_EMPTY;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction == Direction.UP && slot == INPUT_SLOT && canSmelt(stack);
    }

    private boolean canSmelt(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(level instanceof ServerLevel serverLevel)) return true; // Allow during world load or client-side
        return serverLevel.getServer().getRecipeManager()
            .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
            .isPresent();
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
                return Component.translatable("container.logistics.kiln");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new KilnScreenHandler(syncId, playerInventory, KilnBlockEntity.this, containerData);
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
        // activeRecipe is re-resolved on the next tick
    }
}

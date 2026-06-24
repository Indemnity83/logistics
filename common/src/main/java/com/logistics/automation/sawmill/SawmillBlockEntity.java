package com.logistics.automation.sawmill;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.block.MachineBlockEntity;
import com.logistics.core.lib.block.ProcessingMachine;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.items.ItemInventoryComponent;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Sawmill.
 * Saws wood into a primary product plus a chance-based Wood Pulp byproduct, driven by
 * {@code logistics:sawmill} recipes. Each recipe defines the total RF cost; the machine drains it
 * at a fixed {@link #ENERGY_PER_TICK} RF/t.
 *
 * <p>Inventory: slot 0 = input; slot 1 = primary output; slot 2 = secondary (byproduct).
 * Top/sides feed the input; bottom pulls both outputs. A full output slot stalls processing
 * (pause-until-clear) so nothing is lost.
 */
public class SawmillBlockEntity extends MachineBlockEntity
    implements HasItemStorage, WorldlyContainer, MenuBehavior.HasMenu, ProcessingMachine {

    static final int INPUT_SLOT = 0;
    static final int PRIMARY_OUTPUT_SLOT = 1;
    static final int SECONDARY_OUTPUT_SLOT = 2;
    static final int TOTAL_SLOTS = 3;

    private static final int[] PRIMARY_SLOTS = {PRIMARY_OUTPUT_SLOT};
    private static final int[] SECONDARY_SLOTS = {SECONDARY_OUTPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = {PRIMARY_OUTPUT_SLOT, SECONDARY_OUTPUT_SLOT};

    static final long ENERGY_CAPACITY = 20_000L;
    static final long MAX_ENERGY_INPUT = 128L;
    /** Fixed sawing speed; recipes set the total RF cost, drained at this rate. */
    static final int ENERGY_PER_TICK = 20;

    private final ItemInventoryComponent inventory = new ItemInventoryComponent(TOTAL_SLOTS, this::setChanged);

    @Nullable private RecipeHolder<SawmillRecipe> activeRecipe = null;
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
                case DATA_TOTAL_TICKS -> activeRecipe != null ? totalTicks(activeRecipe.value()) : 0;
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

    public SawmillBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.SAWMILL_BLOCK_ENTITY, pos, state, () -> ENERGY_CAPACITY, () -> MAX_ENERGY_INPUT);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SawmillBlockEntity entity) {
        if (level.isClientSide()) return;
        entity.resetEnergyReceived();
        if (entity.tickProcessing(level, state)) {
            entity.setChanged();
        }
    }

    private boolean tickProcessing(Level level, BlockState state) {
        if (activeRecipe == null) {
            activeRecipe = findMatchingRecipe(level);
            if (activeRecipe == null || !canStartProcessing(activeRecipe.value())) {
                setLit(level, state, false);
                return false;
            }
        }

        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (!activeRecipe.value().matches(new SingleRecipeInput(input), level)) {
            activeRecipe = null;
            processProgress = 0;
            setLit(level, state, false);
            return true;
        }

        SawmillRecipe recipe = activeRecipe.value();
        boolean canRun = hasEnoughInput(recipe) && canAcceptOutput(recipe);
        SawmillProcessingPlan.Result plan = SawmillProcessingPlan.advance(
                processProgress, totalTicks(recipe), energy.getAmount(), ENERGY_PER_TICK, canRun);

        if (!plan.consumedEnergy()) {
            setLit(level, state, false);
            return false;
        }

        energy.consume(ENERGY_PER_TICK);
        setLit(level, state, true);
        processProgress = plan.progress();

        if (plan.complete()) {
            completeProcessing(recipe, level);
        }
        return true;
    }

    @Nullable
    private RecipeHolder<SawmillRecipe> findMatchingRecipe(Level level) {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (input.isEmpty()) return null;
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getRecipeManager()
                .getRecipeFor(LogisticsAutomation.RECIPE.SAWMILL_RECIPE_TYPE, new SingleRecipeInput(input), level)
                .orElse(null);
    }

    private boolean canStartProcessing(SawmillRecipe recipe) {
        return hasEnoughInput(recipe) && energy.getAmount() >= ENERGY_PER_TICK && canAcceptOutput(recipe);
    }

    private boolean hasEnoughInput(SawmillRecipe recipe) {
        return inventory.getItem(INPUT_SLOT).getCount() >= recipe.ingredientCount();
    }

    /** The primary product and the guaranteed byproduct must fit, or we stall (nothing lost). */
    private boolean canAcceptOutput(SawmillRecipe recipe) {
        if (!SawmillProcessingPlan.canInsert(inventory, PRIMARY_SLOTS, recipe.getResultItem())) {
            return false;
        }
        SawmillByproduct byproduct = recipe.byproduct();
        int guaranteed = byproduct.guaranteedCount();
        return guaranteed <= 0
                || SawmillProcessingPlan.canInsert(inventory, SECONDARY_SLOTS, byproduct.stack(guaranteed));
    }

    private void completeProcessing(SawmillRecipe recipe, Level level) {
        inventory.getItem(INPUT_SLOT).shrink(recipe.ingredientCount());
        SawmillProcessingPlan.insert(inventory, PRIMARY_SLOTS, recipe.getResultItem());

        int byproductCount = recipe.byproduct().roll(level.getRandom());
        if (byproductCount > 0) {
            SawmillProcessingPlan.insert(inventory, SECONDARY_SLOTS, recipe.byproduct().stack(byproductCount));
        }

        activeRecipe = null;
        processProgress = 0;
    }

    private static int totalTicks(SawmillRecipe recipe) {
        return Math.max(1, (recipe.energy() + ENERGY_PER_TICK - 1) / ENERGY_PER_TICK);
    }

    private void setLit(Level level, BlockState state, boolean lit) {
        if (state.getValue(SawmillBlock.LIT) != lit) {
            level.setBlock(getBlockPos(), state.setValue(SawmillBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    private boolean isSawable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(level instanceof ServerLevel serverLevel)) return true; // permissive during load / on the client
        return serverLevel.getServer().getRecipeManager()
                .getRecipeFor(LogisticsAutomation.RECIPE.SAWMILL_RECIPE_TYPE, new SingleRecipeInput(stack), level)
                .isPresent();
    }

    // ==================== Capability Implementations ====================

    @Override
    public IItemStorage itemStorage(@Nullable Direction side) {
        return new ContainerItemStorage(this, side);
    }

    // ==================== WorldlyContainer (Sided Inventory) ====================

    private static final int[] SLOTS_INPUT = {INPUT_SLOT};

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : SLOTS_INPUT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == INPUT_SLOT && direction != Direction.DOWN && isSawable(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction == Direction.DOWN && slot != INPUT_SLOT;
    }

    // ==================== ProcessingMachine ====================

    @Override
    public float processProgress() {
        if (activeRecipe == null) return 0f;
        int total = totalTicks(activeRecipe.value());
        return total > 0 ? Math.min(1f, (float) processProgress / total) : 0f;
    }

    @Override
    public boolean isProcessing() {
        return activeRecipe != null;
    }

    // ==================== Container Delegation ====================

    @Override
    public int getContainerSize() {
        return inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return inventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    // ==================== MenuBehavior ====================

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.logistics.sawmill");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new SawmillScreenHandler(syncId, playerInventory, SawmillBlockEntity.this, containerData);
            }
        };
    }

    // ==================== NBT ====================

    @Override
    protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveLogisticsData(tag, registries);
        inventory.writeNbt(tag, "Inventory", registries);
        tag.putInt("ProcessProgress", processProgress);
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadLogisticsData(tag, registries);
        inventory.readNbt(tag, "Inventory", registries);
        processProgress = NbtCompat.getInt(tag, "ProcessProgress", 0);
    }
}

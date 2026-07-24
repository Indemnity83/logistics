package com.logistics.power.engine.block.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.items.ItemInventoryComponent;
import com.logistics.core.lib.power.EngineBuilder;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.lib.power.EngineEnergyPusher;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.FluidSyncComponent;
import com.logistics.power.engine.block.ReactionEngineBlock;
import com.logistics.power.engine.reaction.ReactionEngineComponent;
import com.logistics.power.engine.reaction.ReactionEngineReactions;
import com.logistics.power.engine.ui.ReactionEngineScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jetbrains.annotations.Nullable;

/**
 * The Reaction Engine: consumes a liquid reactant + a solid catalyst in one committed reaction, then
 * generates a fixed RF/t for a fixed duration — pushed <b>directly to the network with no buffer</b>.
 *
 * <p><b>The Reaction Engine intentionally exposes no energy storage capability.</b> It never owns RF; it
 * only attempts to push freshly generated energy into the adjacent network. If the network cannot accept it
 * immediately, that energy ceases to exist. This is a gameplay guarantee, not an implementation shortcut —
 * do not add an internal buffer. (The {@code conduit} below is a throwaway transport adapter for
 * {@link EngineEnergyPusher}; it is never persisted, never exposed, and holds no energy across ticks.)
 */
public class ReactionEngineBlockEntity extends EngineEntity
        implements ContainerSingleItem.BlockContainerSingleItem,
                WorldlyContainer,
                HasItemStorage,
                HasFluidStorage,
                MenuBehavior.HasMenu {

    // Synced GUI data indices (the catalyst slot is a real Slot, not ContainerData).
    public static final int DATA_PROGRESS_REMAINING = 0;
    public static final int DATA_PROGRESS_TOTAL = 1;
    public static final int DATA_ATTEMPTED = 2;
    public static final int DATA_ACCEPTED = 3;
    public static final int DATA_REACTANT_ID = 4;
    public static final int DATA_REACTANT_AMOUNT = 5;
    public static final int DATA_REACTANT_CAPACITY = 6;
    public static final int DATA_POWERED = 7;
    public static final int DATA_REACTING = 8;
    public static final int DATA_COUNT = 9;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS_REMAINING -> reactionSim.remainingReactionTicks();
                case DATA_PROGRESS_TOTAL -> reactionSim.reactionDurationTicks();
                case DATA_ATTEMPTED -> (int) reactionSim.lastAttempted();
                case DATA_ACCEPTED -> (int) reactionSim.lastAccepted();
                case DATA_REACTANT_ID -> fluidId(reactantTank);
                case DATA_REACTANT_AMOUNT -> amountMb(reactantTank);
                case DATA_REACTANT_CAPACITY -> capacityMb(reactantTank);
                case DATA_POWERED -> isPowered() ? 1 : 0;
                case DATA_REACTING -> reactionSim.isReacting() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-side data source only; the client uses a SimpleContainerData populated by sync.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    // Assigned in configure() (runs from the super-constructor, before field initializers).
    private ItemInventoryComponent catalystInventory;
    private FluidStoreComponent reactantTank;
    private ReactionEngineComponent reactionSim;

    // Throwaway transport adapter for EngineEnergyPusher — never persisted, never exposed, never stores RF.
    private final EnergyComponent conduit = new EnergyComponent(Long.MAX_VALUE, 0, Long.MAX_VALUE, () -> {});

    public ReactionEngineBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.REACTION_ENGINE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ReactionEngineBlockEntity entity) {
        EngineEntity.tick(level, pos, state, entity);
        entity.updateStageTint(level, pos);
    }

    /**
     * Binary tint via the STAGE property (no heat model): HOT (red) while reacting, OVERHEAT (dark, reads as
     * "off") otherwise. The blockstate change re-renders the block so the color flips — twice per reaction,
     * so no oscillating chunk churn.
     */
    private void updateStageTint(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        BlockState current = level.getBlockState(pos);
        if (!current.hasProperty(HeatStage.STAGE)) {
            return;
        }
        HeatStage desired = reactionSim.isReacting() ? HeatStage.HOT : HeatStage.OVERHEAT;
        if (current.getValue(HeatStage.STAGE) != desired) {
            level.setBlock(pos, current.setValue(HeatStage.STAGE, desired), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void configure(EngineBuilder engine) {
        catalystInventory = new ItemInventoryComponent(1, this::setChanged);

        reactantTank = engine.fluids("reactantTank")
                .capacity(FluidUnits.mb(LogisticsConfigHost.get(LogisticsPower.CONFIG.REACTION_TANK_CAPACITY)))
                .build();
        engine.add(new FluidSyncComponent(reactantTank));
        // No energyOutput, no pistonCycle — bufferless. The sim pushes directly and drives the piston itself.
        // Reactant, reagent, energy, and duration all come from the matched datapack recipe (ignition looks
        // it up via ReactionEngineReactions against the level's RecipeManager).
        reactionSim = engine.add(new ReactionEngineComponent(
                "reactionSim", this::pushEnergy, reactantTank, catalystInventory,
                ReactionEngineReactions::find, this::isPowered, this::setChanged));
    }

    /** Push generated RF straight into the output-face network via a throwaway conduit; discard the rest. */
    private long pushEnergy(MachineContext ctx, long amount) {
        Level lvl = ctx.level();
        if (lvl == null || amount <= 0) {
            return 0;
        }
        conduit.setAmount(amount);
        long sent = EngineEnergyPusher.push(
                lvl, getBlockPos(), ReactionEngineBlock.getOutputDirection(getBlockState()), conduit, amount);
        conduit.setAmount(0); // never store — whatever the network didn't take is discarded
        return sent;
    }

    /** Running ≡ reacting: a committed reaction animates the piston even while unpowered. */
    @Override
    public boolean isRunning() {
        return reactionSim.isReacting();
    }

    // ==================== HasFluidStorage (reactant, insert-only, filtered) ====================

    @Override
    @Nullable
    public IFluidStorage fluidStorage(@Nullable Direction side) {
        return new IFluidStorage() {
            @Override
            public long insert(IFluidKey fluid, long maxAmount, boolean simulate) {
                return ReactionEngineReactions.isReactant(recipeManager(), fluid.getFluid())
                        ? reactantTank.tank().insert(fluid, maxAmount, simulate)
                        : 0;
            }

            @Override
            public long extract(IFluidKey fluid, long maxAmount, boolean simulate) {
                return 0; // the reaction consumes the reactant internally; not pipe-drainable
            }

            @Override
            public Iterable<IFluidView> contents() {
                return reactantTank.tank().contents();
            }
        };
    }

    // ==================== HasItemStorage (catalyst slot, hidden on the output face) ====================

    @Override
    @Nullable
    public IItemStorage itemStorage(@Nullable Direction side) {
        if (side != null && side == ReactionEngineBlock.getOutputDirection(getBlockState())) {
            return null;
        }
        return new IItemStorage() {
            @Override
            public long insert(IItemKey item, long maxAmount, boolean simulate) {
                if (!ReactionEngineReactions.isCatalyst(recipeManager(), item.toStack(1))) {
                    return 0;
                }
                ItemStack current = catalystInventory.getItem(0);
                ItemStack template = item.toStack(1);
                long clampedAmount = Math.min(maxAmount, template.getMaxStackSize());
                if (current.isEmpty()) {
                    if (!simulate) catalystInventory.setItem(0, item.toStack((int) clampedAmount));
                    return clampedAmount;
                } else if (ItemStack.isSameItemSameComponents(current, template)) {
                    long canFit = current.getMaxStackSize() - current.getCount();
                    long toInsert = Math.min(maxAmount, canFit);
                    if (toInsert > 0 && !simulate) {
                        current.grow((int) toInsert);
                        catalystInventory.setChanged();
                    }
                    return toInsert;
                }
                return 0;
            }

            @Override
            public long extract(IItemKey item, long maxAmount, boolean simulate) {
                ItemStack current = catalystInventory.getItem(0);
                if (current.isEmpty() || !item.matches(current)) return 0;
                long toExtract = Math.min(maxAmount, current.getCount());
                if (!simulate) catalystInventory.removeItem(0, (int) toExtract);
                return toExtract;
            }

            @Override
            public Iterable<IItemView> contents() {
                ItemStack stack = catalystInventory.getItem(0);
                if (stack.isEmpty()) return java.util.Collections.emptyList();
                IItemKey key = ItemStorageLookup.of(stack);
                IItemView view = new IItemView() {
                    @Override
                    public IItemKey resource() {
                        return key;
                    }

                    @Override
                    public long amount() {
                        return stack.getCount();
                    }
                };
                return java.util.Collections.singletonList(view);
            }
        };
    }

    // ==================== GUI/HUD accessors ====================

    public FluidStoreComponent reactantTank() {
        return reactantTank;
    }

    public ReactionEngineComponent simulation() {
        return reactionSim;
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    private static int fluidId(FluidStoreComponent store) {
        return store.tank().isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(store.tank().getFluidKey().getFluid());
    }

    private static int amountMb(FluidStoreComponent store) {
        return (int) Math.min(FluidUnits.toMillibuckets(store.tank().getAmount()), Integer.MAX_VALUE);
    }

    private static int capacityMb(FluidStoreComponent store) {
        return (int) Math.min(FluidUnits.toMillibuckets(store.tank().getCapacity()), Integer.MAX_VALUE);
    }

    // ==================== SingleStackInventory (catalyst slot) ====================

    public ItemStack getTheItem() {
        return catalystInventory.getItem(0);
    }

    public void setTheItem(ItemStack stack) {
        catalystInventory.setItem(0, stack);
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    public boolean isValid(int slot, ItemStack stack) {
        // Server-side filter only (hoppers/automation): with no RecipeManager (client), stay permissive so
        // the GUI slot isn't wrongly blocked — matching the Macerator/other item-input machines.
        var recipes = recipeManager();
        return recipes == null || ReactionEngineReactions.isCatalyst(recipes, stack);
    }

    @Override
    public boolean isEmpty() {
        return catalystInventory.getItem(0).isEmpty();
    }

    // ==================== WorldlyContainer (catalyst from every face except output) ====================

    private static final int[] SLOTS_FOR_INPUT = new int[]{0};
    private static final int[] SLOTS_FOR_OUTPUT = new int[]{};

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == ReactionEngineBlock.getOutputDirection(getBlockState()) ? SLOTS_FOR_OUTPUT : SLOTS_FOR_INPUT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (direction == ReactionEngineBlock.getOutputDirection(getBlockState())) {
            return false;
        }
        return isValid(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction != ReactionEngineBlock.getOutputDirection(getBlockState());
    }

    // ==================== Menu ====================

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("block.logistics.power.reaction_engine");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new ReactionEngineScreenHandler(
                        syncId, playerInventory, ReactionEngineBlockEntity.this, containerData);
            }
        };
    }

    // ==================== Persistence (engine components + the BE-local catalyst slot) ====================

    @Override
    protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveLogisticsData(tag, registries);
        catalystInventory.writeNbt(tag, "Inventory", registries);
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadLogisticsData(tag, registries);
        catalystInventory.readNbt(tag, "Inventory", registries);
    }
}

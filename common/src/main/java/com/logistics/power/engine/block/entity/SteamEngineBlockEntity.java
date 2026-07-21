package com.logistics.power.engine.block.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.items.ItemInventoryComponent;
import com.logistics.core.lib.power.EngineBuilder;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.lib.power.FuelHelper;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.lib.power.fuel.FuelSource;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.FluidSyncComponent;
import com.logistics.power.engine.block.SteamEngineBlock;
import com.logistics.power.engine.steam.SteamEngineComponent;
import com.logistics.power.engine.steam.SteamEngineProfile;
import com.logistics.power.engine.ui.SteamEngineScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jetbrains.annotations.Nullable;

/**
 * The Steam Engine: boils solid fuel + water into stored pressure, which the turbine draws down as RF.
 * It has a solid-fuel slot and a water tank; pressure is an internal reserve owned by the
 * {@link SteamEngineComponent} simulation. It has no heat model at all — structurally it cannot
 * overheat — so the piston cycle is wired with a constant {@code overheated = false} gate.
 */
public class SteamEngineBlockEntity extends EngineEntity
        implements HasItemStorage,
                HasFluidStorage,
                WorldlyContainer,
                MenuBehavior.HasMenu,
                ContainerSingleItem.BlockContainerSingleItem {

    // Synced GUI data indices.
    public static final int DATA_ENERGY = 0; // 0..10000 fill fraction
    public static final int DATA_PRESSURE = 1;
    public static final int DATA_MAX_PRESSURE = 2;
    public static final int DATA_OPERATING = 3;
    public static final int DATA_RELIGHT = 4;
    public static final int DATA_TARGET = 5;
    public static final int DATA_GENERATION = 6;
    public static final int DATA_WATER_ID = 7;
    public static final int DATA_WATER_AMOUNT = 8;
    public static final int DATA_WATER_CAPACITY = 9;
    public static final int DATA_COMMITTED_BURN = 10;
    public static final int DATA_TOTAL_FUEL = 11;
    public static final int DATA_BURN_FRACTION = 12; // committed-burn fraction, 0..1000
    public static final int DATA_STATUS = 13; // SteamEngineStatus ordinal
    public static final int DATA_FIREBOX = 14; // SteamFireboxState ordinal
    public static final int DATA_COUNT = 15;

    // Assigned in configure() — see the note there on why these can't be field initializers.
    private ItemInventoryComponent fuelInventory;
    private EngineEnergyOutputComponent energy;
    private FluidStoreComponent waterTank;
    private SteamEngineComponent steamSim;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY -> energyFraction();
                case DATA_PRESSURE -> (int) Math.round(steamSim.pressure());
                case DATA_MAX_PRESSURE -> (int) Math.round(cfg(LogisticsPower.CONFIG.STEAM_MAX_PRESSURE));
                case DATA_OPERATING -> (int) Math.round(cfg(LogisticsPower.CONFIG.STEAM_OPERATING_PRESSURE));
                case DATA_RELIGHT -> (int) Math.round(cfg(LogisticsPower.CONFIG.STEAM_RELIGHT_PRESSURE));
                case DATA_TARGET -> (int) Math.round(cfg(LogisticsPower.CONFIG.STEAM_TARGET_PRESSURE));
                case DATA_GENERATION -> (int) steamSim.lastGenerationRate();
                case DATA_WATER_ID -> fluidId(waterTank);
                case DATA_WATER_AMOUNT -> amountMb(waterTank);
                case DATA_WATER_CAPACITY -> capacityMb(waterTank);
                case DATA_COMMITTED_BURN -> steamSim.committedBurnTicks();
                case DATA_TOTAL_FUEL -> steamSim.totalFuelTicks();
                case DATA_BURN_FRACTION -> (int) Math.round(steamSim.burnFraction() * 1000);
                case DATA_STATUS -> steamSim.status().ordinal();
                case DATA_FIREBOX -> steamSim.fireboxState().ordinal();
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

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.STEAM_ENGINE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SteamEngineBlockEntity entity) {
        EngineEntity.tick(level, pos, state, entity);
    }

    // configure() runs from the EngineEntity super-constructor, before this subclass's field
    // initializers would run — so the fuel slot, tanks, and sim are created here (not as field
    // initializers) to be non-null when the components capture them.
    @Override
    protected void configure(EngineBuilder engine) {
        fuelInventory = new ItemInventoryComponent(1, this::setChanged);

        SteamEngineProfile profile = new SteamEngineProfile(
                cfg(LogisticsPower.CONFIG.STEAM_MAX_OUTPUT),
                cfg(LogisticsPower.CONFIG.STEAM_MAX_PRESSURE),
                cfg(LogisticsPower.CONFIG.STEAM_OPERATING_PRESSURE),
                cfg(LogisticsPower.CONFIG.STEAM_RELIGHT_PRESSURE),
                cfg(LogisticsPower.CONFIG.STEAM_TARGET_PRESSURE),
                cfg(LogisticsPower.CONFIG.STEAM_STEAM_PER_BURN_TICK),
                cfg(LogisticsPower.CONFIG.STEAM_PRESSURE_PER_RF),
                cfg(LogisticsPower.CONFIG.STEAM_WATER_CONVERSION),
                cfg(LogisticsPower.CONFIG.STEAM_COOLING_DECAY),
                (int) (long) cfg(LogisticsPower.CONFIG.STEAM_STOKED_BURN_INTERVAL));

        energy = engine.energyOutput("energy")
                .capacity(() -> cfg(LogisticsPower.CONFIG.STEAM_BUFFER_CAPACITY))
                .build();
        waterTank = engine.fluids("waterTank")
                .capacity(FluidUnits.mb(cfg(LogisticsPower.CONFIG.STEAM_WATER_TANK_CAPACITY)))
                .build();
        engine.add(new FluidSyncComponent(waterTank));
        steamSim = engine.add(new SteamEngineComponent(
                "steamSim", energy, waterTank, fuelSource(), profile,
                this::isPowered, this::setLit, this::setChanged));
        engine.pistonCycle("cycle")
                .energy(energy)
                .overheated(() -> false) // no heat model — the steam engine cannot overheat
                .powered(this::isPowered)
                .pistonSpeed(steamSim::pistonSpeed)
                .outputPower(() -> cfg(LogisticsPower.CONFIG.STEAM_MAX_OUTPUT))
                .outputFace(() -> SteamEngineBlock.getOutputDirection(getBlockState()))
                .sendsEnergyContinuously(true)
                .build();
    }

    private static <T> T cfg(com.indemnity83.configory.ConfigKey<T> key) {
        return LogisticsConfigHost.get(key);
    }

    /** Consumes one solid fuel item from the fuel slot and reports its burn duration. */
    private FuelSource fuelSource() {
        return ctx -> {
            Level level = ctx.level();
            if (level == null) {
                return 0;
            }
            ItemStack stack = fuelInventory.getItem(0);
            int burnTicks = FuelHelper.getBurnDuration(level, stack);
            if (burnTicks <= 0) {
                return 0;
            }
            if (stack.is(Items.LAVA_BUCKET)) {
                fuelInventory.setItem(0, new ItemStack(Items.BUCKET));
            } else {
                stack.shrink(1);
            }
            return burnTicks;
        };
    }

    private void setLit(MachineContext ctx, boolean lit) {
        Level level = ctx.level();
        if (level == null) {
            return;
        }
        BlockState state = ctx.blockState();
        if (state.hasProperty(SteamEngineBlock.LIT) && state.getValue(SteamEngineBlock.LIT) != lit) {
            level.setBlock(ctx.pos(), state.setValue(SteamEngineBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    // ==================== HasItemStorage (fuel slot, hidden on the output face) ====================

    @Override
    @Nullable
    public IItemStorage itemStorage(@Nullable Direction side) {
        if (side != null && side == SteamEngineBlock.getOutputDirection(getBlockState())) {
            return null;
        }
        return new IItemStorage() {
            @Override
            public long insert(IItemKey item, long maxAmount, boolean simulate) {
                if (level == null || !FuelHelper.isFuel(level, item.toStack(1))) {
                    return 0;
                }
                ItemStack current = fuelInventory.getItem(0);
                ItemStack template = item.toStack(1);
                long clampedAmount = Math.min(maxAmount, template.getMaxStackSize());
                if (current.isEmpty()) {
                    if (!simulate) fuelInventory.setItem(0, item.toStack((int) clampedAmount));
                    return clampedAmount;
                } else if (ItemStack.isSameItemSameComponents(current, template)) {
                    long canFit = current.getMaxStackSize() - current.getCount();
                    long toInsert = Math.min(maxAmount, canFit);
                    if (toInsert > 0 && !simulate) {
                        current.grow((int) toInsert);
                        fuelInventory.setChanged();
                    }
                    return toInsert;
                }
                return 0;
            }

            @Override
            public long extract(IItemKey item, long maxAmount, boolean simulate) {
                ItemStack current = fuelInventory.getItem(0);
                if (current.isEmpty() || !item.matches(current)) return 0;
                long toExtract = Math.min(maxAmount, current.getCount());
                if (!simulate) fuelInventory.removeItem(0, (int) toExtract);
                return toExtract;
            }

            @Override
            public Iterable<IItemView> contents() {
                ItemStack stack = fuelInventory.getItem(0);
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

    // ==================== HasFluidStorage (water-only input view) ====================

    @Override
    @Nullable
    public IFluidStorage fluidStorage(@Nullable Direction side) {
        return new SteamEngineFluidView(waterTank);
    }

    /** Insert-only view accepting water into the boiler tank; not pipe-drainable. */
    private record SteamEngineFluidView(FluidStoreComponent waterTank) implements IFluidStorage {
        @Override
        public long insert(IFluidKey fluid, long maxAmount, boolean simulate) {
            if (!fluid.getFluid().defaultFluidState().is(FluidTags.WATER)) {
                return 0;
            }
            return waterTank.tank().insert(fluid, maxAmount, simulate);
        }

        @Override
        public long extract(IFluidKey fluid, long maxAmount, boolean simulate) {
            return 0; // the boiler consumes water internally; not pipe-drainable
        }

        @Override
        public Iterable<IFluidView> contents() {
            return waterTank.tank().contents();
        }
    }

    // ==================== GUI/HUD accessors ====================

    public FluidStoreComponent waterTank() {
        return waterTank;
    }

    public SteamEngineComponent simulation() {
        return steamSim;
    }

    public long getEnergyStored() {
        return energy.getAmount();
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    private int energyFraction() {
        long capacity = energy.getCapacity();
        return capacity <= 0 ? 0 : (int) Math.min(10_000L, energy.getAmount() * 10_000L / capacity);
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

    // ==================== SingleStackInventory (fuel slot) ====================

    public ItemStack getTheItem() {
        return fuelInventory.getItem(0);
    }

    public void setTheItem(ItemStack stack) {
        fuelInventory.setItem(0, stack);
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    public boolean isValid(int slot, ItemStack stack) {
        if (level == null) {
            return true;
        }
        return FuelHelper.isFuel(level, stack);
    }

    @Override
    public boolean isEmpty() {
        return fuelInventory.getItem(0).isEmpty();
    }

    // ==================== WorldlyContainer (fuel from every face except output) ====================

    private static final int[] SLOTS_FOR_INPUT = new int[]{0};
    private static final int[] SLOTS_FOR_OUTPUT = new int[]{};

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == SteamEngineBlock.getOutputDirection(getBlockState()) ? SLOTS_FOR_OUTPUT : SLOTS_FOR_INPUT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (direction == SteamEngineBlock.getOutputDirection(getBlockState())) {
            return false;
        }
        return isValid(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction != SteamEngineBlock.getOutputDirection(getBlockState());
    }

    // ==================== Menu ====================

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("block.logistics.power.steam_engine");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new SteamEngineScreenHandler(
                        syncId, playerInventory, SteamEngineBlockEntity.this, containerData);
            }
        };
    }

    // ==================== Persistence (engine components + the BE-local fuel inventory) ====================

    @Override
    protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveLogisticsData(tag, registries);
        fuelInventory.writeNbt(tag, "Inventory", registries);
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadLogisticsData(tag, registries);
        fuelInventory.readNbt(tag, "Inventory", registries);
    }
}

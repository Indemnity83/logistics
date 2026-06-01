package com.logistics.power.engine.block.entity;

import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.items.ItemInventoryComponent;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.core.lib.power.FuelHelper;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.block.behavior.ProbeResult;
import com.logistics.power.engine.block.StirlingEngineBlock;
import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import com.logistics.LogisticsPower;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ItemStorageLookup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Stirling Engine.
 * A fuel-burning engine that converts solid fuel to energy.
 *
 * <p>Characteristics (aligned with BuildCraft):
 * <ul>
 *   <li>Max heat: 250°C (overheat at 100%)</li>
 *   <li>Buffer: 10,000 RF</li>
 *   <li>Generation: PID-controlled 3-10 RF/t to maintain target temperature</li>
 *   <li>Target temperature: 150°C (60% heat level)</li>
 *   <li>Heat model: tied to buffer level (empty=20°C, full=250°C)</li>
 *   <li>Energy decay: 10 RF/t when off</li>
 *   <li>Overheat: thermal shutdown, no explosion</li>
 * </ul>
 *
 * <p>The PID controller adjusts generation rate to maintain target temperature.
 * When energy is being consumed, temperature drops and generation increases.
 * When buffer fills up, temperature rises and generation decreases.
 */
public class StirlingEngineBlockEntity extends AbstractEngineBlockEntity
        implements ContainerSingleItem.BlockContainerSingleItem, WorldlyContainer, HasItemStorage, MenuBehavior.HasMenu {

    // ==================== Constants ====================

    private static final long MAX_ENERGY = 10_000L;

    // PID controller settings (tuned via run/pid_simulator.py)
    private static final double PID_KP = 0.2;
    private static final double PID_KI = 0.0002;
    private static final double PID_KD = 0.3;
    private static final double TARGET_TEMPERATURE = 150;

    // Output range defaults — actual range read from LogisticsConfig at runtime
    private static final double DEFAULT_MIN_GENERATION = 3.0;

    // Property delegate indices for GUI
    public static final int PROPERTY_BURN_TIME = 0;
    public static final int PROPERTY_FUEL_TIME = 1;
    public static final int PROPERTY_HEAT = 2;
    public static final int PROPERTY_ENERGY = 3;
    public static final int PROPERTY_GENERATION = 4;
    public static final int PROPERTY_COUNT = 5;

    // ==================== State ====================

    private final StirlingFuelState fuelState = new StirlingFuelState();
    private final StirlingGenerationPlanner generationPlanner = new StirlingGenerationPlanner(
            PID_KP, PID_KI, PID_KD, TARGET_TEMPERATURE, DEFAULT_MIN_GENERATION);

    // Inventory (single fuel slot)
    private final ItemInventoryComponent inventory = new ItemInventoryComponent(1, this::setChanged);

    // Property delegate for syncing data to GUI
    private final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_BURN_TIME -> fuelState.burnTime();
                case PROPERTY_FUEL_TIME -> fuelState.fuelTime();
                case PROPERTY_HEAT -> (int) getTemperature();
                case PROPERTY_ENERGY -> (int) (getEnergy() / 100);
                case PROPERTY_GENERATION -> (int) (generationPlanner.currentGeneration() * 100);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case PROPERTY_BURN_TIME -> fuelState.restore(value, fuelState.fuelTime());
                case PROPERTY_FUEL_TIME -> fuelState.restore(fuelState.burnTime(), value);
                case PROPERTY_HEAT -> {} // Read-only on client, computed from energy level
                case PROPERTY_ENERGY -> energyBuffer.setAmount(value * 100L);
                case PROPERTY_GENERATION -> generationPlanner.restore(
                        value / 100.0, generationPlanner.generationCarry(), generationPlanner.pidIntegral());
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return PROPERTY_COUNT;
        }
    };

    // ==================== Constructor & Ticker ====================

    public StirlingEngineBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY, pos, state);
    }

    // ==================== HasItemStorage ====================

    @Override
    public IItemStorage itemStorage(@Nullable Direction side) {
        // Don't expose inventory on the output face (facing direction)
        if (side != null && isOutputDirection(side)) {
            return null;
        }

        // Wrap the single fuel slot with fuel-validation logic
        return new IItemStorage() {
            @Override
            public long insert(IItemKey item, long maxAmount, boolean simulate) {
                // Reject non-fuel items (same validation as isValid() for GUI)
                if (level == null || !FuelHelper.isFuel(level, item.toStack(1))) {
                    return 0;
                }
                ItemStack current = inventory.getItem(0);
                ItemStack template = item.toStack(1);
                long clampedAmount = Math.min(maxAmount, template.getMaxStackSize());
                if (current.isEmpty()) {
                    if (!simulate) inventory.setItem(0, item.toStack((int) clampedAmount));
                    return clampedAmount;
                } else if (ItemStack.isSameItemSameComponents(current, template)) {
                    long canFit = current.getMaxStackSize() - current.getCount();
                    long toInsert = Math.min(maxAmount, canFit);
                    if (toInsert > 0 && !simulate) {
                        current.grow((int) toInsert);
                        inventory.setChanged();
                    }
                    return toInsert;
                }
                return 0;
            }

            @Override
            public long extract(IItemKey item, long maxAmount, boolean simulate) {
                ItemStack current = inventory.getItem(0);
                if (current.isEmpty() || !item.matches(current)) return 0;
                long toExtract = Math.min(maxAmount, current.getCount());
                if (!simulate) inventory.removeItem(0, (int) toExtract);
                return toExtract;
            }

            @Override
            public Iterable<IItemView> contents() {
                ItemStack stack = inventory.getItem(0);
                if (stack.isEmpty()) return java.util.Collections.emptyList();
                IItemKey key = ItemStorageLookup.of(stack);
                IItemView view = new IItemView() {
                    @Override public IItemKey resource() { return key; }
                    @Override public long amount() { return stack.getCount(); }
                };
                return java.util.Collections.singletonList(view);
            }
        };
    }

    // ==================== Ticker ====================

    public static void tick(Level world, BlockPos pos, BlockState state, StirlingEngineBlockEntity entity) {
        entity.tickEngine(world, pos, state);

        // Update LIT state based on burn time
        if (!world.isClientSide()) {
            BlockState currentState = world.getBlockState(pos);
            boolean wasLit = currentState.getValue(StirlingEngineBlock.LIT);
            boolean isLit = entity.fuelState.isBurning();
            if (isLit != wasLit) {
                world.setBlock(pos, currentState.setValue(StirlingEngineBlock.LIT, isLit), Block.UPDATE_CLIENTS);
            }
        }
    }

    // ==================== Subclass Configuration ====================

    @Override
    protected long getEnergyBufferCapacity() {
        return MAX_ENERGY;
    }

    @Override
    protected long getOutputPower() {
        return generationPlanner.outputPower(
                getTemperature(),
                getTemperatureFloor(),
                LogisticsConfig.get().engine.stirlingMinOutput,
                LogisticsConfig.get().engine.stirlingMaxOutput);
    }

    @Override
    protected Direction getOutputDirection() {
        return StirlingEngineBlock.getOutputDirection(getBlockState());
    }

    @Override
    protected boolean isRedstonePowered() {
        return getBlockState().getValue(StirlingEngineBlock.POWERED);
    }

    @Override
    protected boolean sendsEnergyContinuously() {
        return true;
    }

    // ==================== Lifecycle Hooks ====================

    /** Stirling engine is running when powered, not overheated, AND has fuel burning. */
    @Override
    public boolean isRunning() {
        return super.isRunning() && fuelState.isBurning();
    }

    @Override
    protected void produceEnergy() {
        if (!isRedstonePowered() || isOverheated()) {
            fuelState.extinguish();
            return;
        }

        if (fuelState.burn() || refuel()) {
            generateWithCarry();
        }
    }

    @Override
    protected void onShutdown() {
        generationPlanner.reset();
    }

    // ==================== Fuel & Generation ====================

    private boolean refuel() {
        if (level == null) {
            return false;
        }

        ItemStack fuel = inventory.getItem(0);
        int burnTicks = FuelHelper.getBurnDuration(level, fuel);
        if (burnTicks <= 0) {
            return false;
        }

        fuelState.ignite(burnTicks);

        if (fuel.is(Items.LAVA_BUCKET)) {
            inventory.setItem(0, new ItemStack(Items.BUCKET));
        } else {
            fuel.shrink(1);
        }
        setChanged();
        return true;
    }

    private void generateWithCarry() {
        long toAdd = generationPlanner.generate(
                getTemperature(),
                getEnergy(),
                getEnergyBufferCapacity(),
                LogisticsConfig.get().engine.stirlingMinOutput,
                LogisticsConfig.get().engine.stirlingMaxOutput);

        if (toAdd > 0) {
            addEnergy(toAdd);
        }
    }

    // ==================== Public API ====================

    public int getBurnTime() {
        return fuelState.burnTime();
    }

    public int getFuelTime() {
        return fuelState.fuelTime();
    }

    public double getCurrentGenerationRate() {
        return generationPlanner.currentGeneration();
    }

    public ContainerData getPropertyDelegate() {
        return propertyDelegate;
    }

    @Override
    protected void addProbeEntries(ProbeResult.Builder builder) {
        super.addProbeEntries(builder);

        // Generation rate (PID controlled)
        builder.entry("Generation", String.format("%.2f RF/t", generationPlanner.currentGeneration()), ChatFormatting.GREEN);

        // Fuel burn time
        if (fuelState.fuelTime() > 0) {
            builder.entry(
                    "Fuel",
                    String.format(
                            "%d / %d ticks (%.1f%%)",
                            fuelState.burnTime(),
                            fuelState.fuelTime(),
                            (fuelState.burnTime() / (float) fuelState.fuelTime()) * 100),
                    ChatFormatting.YELLOW);
        } else {
            builder.entry("Fuel", "None", ChatFormatting.GRAY);
        }
    }

    // ==================== SingleStackInventory Implementation ====================

    public ItemStack getTheItem() {
        return inventory.getItem(0);
    }

    public void setTheItem(ItemStack stack) {
        inventory.setItem(0, stack);
        // Note: setChanged() already called by inventory component
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    public boolean isValid(int slot, ItemStack stack) {
        if (level == null) {
            return true; // Allow insertion when world not loaded, validate on use
        }
        return FuelHelper.isFuel(level, stack);
    }

    @Override
    public boolean isEmpty() {
        return inventory.getItem(0).isEmpty();
    }

    // ==================== WorldlyContainer Implementation (Sided Inventory) ====================

    private static final int[] SLOTS_FOR_INPUT = new int[]{0};
    private static final int[] SLOTS_FOR_OUTPUT = new int[]{};

    @Override
    public int[] getSlotsForFace(Direction side) {
        Direction outputDir = getOutputDirection();
        // No slots accessible from output face
        if (side == outputDir) {
            return SLOTS_FOR_OUTPUT;
        }
        // Fuel slot accessible from all other faces
        return SLOTS_FOR_INPUT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        Direction outputDir = getOutputDirection();
        // Cannot insert items from output face
        if (direction == outputDir) {
            return false;
        }
        // Can insert fuel from other faces
        return isValid(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        Direction outputDir = getOutputDirection();
        // Cannot extract items from output face
        return direction != outputDir;
    }

    // ==================== MenuBehavior.HasMenu Implementation ====================

    @Override
    public net.minecraft.world.MenuProvider createMenuProvider() {
        return new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("block.logistics.power.stirling_engine");
            }

            @Nullable @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new StirlingEngineScreenHandler(syncId, playerInventory, StirlingEngineBlockEntity.this, propertyDelegate);
            }
        };
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void saveLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveLogisticsData(nbt, registries);

        // Save Stirling-specific data
        nbt.putInt("BurnTimeRemaining", fuelState.burnTime());
        nbt.putInt("TotalFuelTime", fuelState.fuelTime());
        nbt.putDouble("CurrentGeneration", generationPlanner.currentGeneration());
        nbt.putDouble("GenerationCarryover", generationPlanner.generationCarry());
        nbt.putDouble("PIDIntegral", generationPlanner.pidIntegral());

        // Save fuel inventory
        inventory.writeNbt(nbt, "Inventory", registries);
    }

    @Override
    protected void loadLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLogisticsData(nbt, registries);

        // Load Stirling-specific data
        fuelState.restore(
                NbtCompat.getInt(nbt, "BurnTimeRemaining", 0),
                NbtCompat.getInt(nbt, "TotalFuelTime", 0));
        generationPlanner.restore(
                NbtCompat.getDouble(nbt, "CurrentGeneration", DEFAULT_MIN_GENERATION),
                NbtCompat.getDouble(nbt, "GenerationCarryover", 0.0),
                NbtCompat.getDouble(nbt, "PIDIntegral", 0.0));

        // Load fuel inventory (try new key first, fall back to legacy)
        if (nbt.contains("Inventory")) {
            inventory.readNbt(nbt, "Inventory", registries);
        } else if (nbt.contains("FuelStack")) {
            // Legacy: single item stored with CODEC
            inventory.setItem(0, ItemStack.EMPTY);
            var ops = registries.createSerializationContext(NbtOps.INSTANCE);
            ItemStack.CODEC.parse(ops, nbt.get("FuelStack"))
                    .result()
                    .ifPresent(stack -> inventory.setItem(0, stack));
        }
    }

    @Override
    protected void loadLegacyData(net.minecraft.world.level.storage.ValueInput view) {
        super.loadLegacyData(view); // Loads engine data from "Engine" tag

        // Load Stirling-specific data from old "StirlingData" tag
        view.read("StirlingData", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(stirlingData -> {
            fuelState.restore(
                    NbtCompat.getInt(stirlingData, "burnTime", 0),
                    NbtCompat.getInt(stirlingData, "fuelTime", 0));
            generationPlanner.restore(
                    NbtCompat.getDouble(stirlingData, "currentGeneration", DEFAULT_MIN_GENERATION),
                    NbtCompat.getDouble(stirlingData, "generationCarry", 0.0),
                    NbtCompat.getDouble(stirlingData, "pidIntegral", 0.0));
        });

        // Load fuel from old "Fuel" tag at root level
        inventory.setItem(0, ItemStack.EMPTY);
        view.read("Fuel", ItemStack.CODEC).ifPresent(stack -> inventory.setItem(0, stack));
    }
}

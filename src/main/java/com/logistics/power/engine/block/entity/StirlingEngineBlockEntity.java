package com.logistics.power.engine.block.entity;

import com.logistics.core.lib.entity.HasItemStorage;
import com.logistics.core.lib.entity.HasMenu;
import com.logistics.core.lib.items.ItemInventoryComponent;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.core.lib.support.ProbeResult;
import com.logistics.power.engine.PIDController;
import com.logistics.power.engine.block.StirlingEngineBlock;
import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import com.logistics.LogisticsPower;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        implements ExtendedScreenHandlerFactory<BlockPos>, ContainerSingleItem.BlockContainerSingleItem, HasItemStorage, HasMenu {

    // ==================== Constants ====================

    private static final long MAX_ENERGY = 10_000L;

    // PID controller settings (tuned via run/pid_simulator.py)
    private static final double PID_KP = 0.2;
    private static final double PID_KI = 0.0002;
    private static final double PID_KD = 0.3;
    private static final double TARGET_TEMPERATURE = 150;

    // Output range in RF per tick
    private static final double MIN_GENERATION = 3.0;
    private static final double MAX_GENERATION = 10.0;

    // Property delegate indices for GUI
    public static final int PROPERTY_BURN_TIME = 0;
    public static final int PROPERTY_FUEL_TIME = 1;
    public static final int PROPERTY_HEAT = 2;
    public static final int PROPERTY_ENERGY = 3;
    public static final int PROPERTY_GENERATION = 4;
    public static final int PROPERTY_COUNT = 5;

    // ==================== State ====================

    // Fuel state
    private int burnTime = 0;
    private int fuelTime = 0;

    // PID controller for output regulation
    private final PIDController pidController = new PIDController(PID_KP, PID_KI, PID_KD);
    private double currentGeneration = MIN_GENERATION;
    private double generationCarry = 0.0;

    // Inventory (single fuel slot)
    private final ItemInventoryComponent inventory = new ItemInventoryComponent(1, this::setChanged);

    // Property delegate for syncing data to GUI
    private final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_BURN_TIME -> burnTime;
                case PROPERTY_FUEL_TIME -> fuelTime;
                case PROPERTY_HEAT -> (int) temperature;
                case PROPERTY_ENERGY -> (int) (getEnergy() / 100);
                case PROPERTY_GENERATION -> (int) (currentGeneration * 100);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case PROPERTY_BURN_TIME -> burnTime = value;
                case PROPERTY_FUEL_TIME -> fuelTime = value;
                case PROPERTY_HEAT -> temperature = value;
                case PROPERTY_ENERGY -> energyStorage.amount = value * 100L;
                case PROPERTY_GENERATION -> currentGeneration = value / 100.0;
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
    public Storage<ItemVariant> itemStorage() {
        return inventory.storage();
    }

    // ==================== Ticker ====================

    public static void tick(Level world, BlockPos pos, BlockState state, StirlingEngineBlockEntity entity) {
        entity.tickEngine(world, pos, state);

        // Update LIT state based on burn time
        if (!world.isClientSide()) {
            BlockState currentState = world.getBlockState(pos);
            boolean wasLit = currentState.getValue(StirlingEngineBlock.LIT);
            boolean isLit = entity.burnTime > 0;
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
        double tempRatio =
                Math.min(1.0, (temperature - getTemperatureFloor()) / (TARGET_TEMPERATURE - getTemperatureFloor()));
        double output = MIN_GENERATION + tempRatio * (MAX_GENERATION - MIN_GENERATION);
        return Math.round(output);
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
        return super.isRunning() && burnTime > 0;
    }

    @Override
    protected void produceEnergy() {
        if (!isRedstonePowered() || isOverheated()) {
            extinguish();
            return;
        }

        if (burn() || refuel()) {
            generateWithCarry();
        }
    }

    @Override
    protected void onShutdown() {
        pidController.reset();
        currentGeneration = MIN_GENERATION;
        generationCarry = 0.0;
    }

    // ==================== Fuel & Generation ====================

    private void extinguish() {
        burnTime = 0;
        fuelTime = 0;
    }

    private boolean burn() {
        if (burnTime > 0) {
            burnTime--;
        }
        return burnTime > 0;
    }

    private boolean refuel() {
        if (level == null) {
            return false;
        }

        ItemStack fuel = inventory.getItem(0);
        int burnTicks = level.fuelValues().burnDuration(fuel);
        if (burnTicks <= 0) {
            return false;
        }

        fuelTime = burnTicks;
        burnTime = fuelTime;

        if (fuel.is(Items.LAVA_BUCKET)) {
            inventory.setItem(0,new ItemStack(Items.BUCKET));
        } else {
            fuel.shrink(1);
        }
        setChanged();
        return true;
    }

    private void generateWithCarry() {
        currentGeneration = pidController.compute(TARGET_TEMPERATURE, temperature, MIN_GENERATION, MAX_GENERATION);
        generationCarry += currentGeneration;

        long whole = (long) Math.floor(generationCarry);
        if (whole <= 0) {
            return;
        }

        long space = getEnergyBufferCapacity() - getEnergy();
        long toAdd = Math.min(whole, space);

        if (toAdd > 0) {
            addEnergy(toAdd);
            generationCarry -= toAdd;
        }

        if (space <= 0) {
            generationCarry = 0.0;
        }
    }

    // ==================== Public API ====================

    public int getBurnTime() {
        return burnTime;
    }

    public int getFuelTime() {
        return fuelTime;
    }

    public double getCurrentGenerationRate() {
        return currentGeneration;
    }

    public ContainerData getPropertyDelegate() {
        return propertyDelegate;
    }

    @Override
    protected void addProbeEntries(ProbeResult.Builder builder) {
        super.addProbeEntries(builder);

        // Generation rate (PID controlled)
        builder.entry("Generation", String.format("%.2f RF/t", currentGeneration), ChatFormatting.GREEN);

        // Fuel burn time
        if (fuelTime > 0) {
            builder.entry(
                    "Fuel",
                    String.format("%d / %d ticks (%.1f%%)", burnTime, fuelTime, (burnTime / (float) fuelTime) * 100),
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
        return level.fuelValues().isFuel(stack);
    }

    @Override
    public boolean isEmpty() {
        return inventory.getItem(0).isEmpty();
    }

    // ==================== ExtendedScreenHandlerFactory Implementation ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.logistics.power.stirling_engine");
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return getBlockPos();
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new StirlingEngineScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    @Override
    public net.minecraft.world.MenuProvider createMenuProvider() {
        return this; // StirlingEngineBlockEntity already implements MenuProvider via ExtendedScreenHandlerFactory
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void saveLogisticsData(CompoundTag nbt) {
        super.saveLogisticsData(nbt);

        // Save Stirling-specific data
        nbt.putInt("BurnTimeRemaining", burnTime);
        nbt.putInt("TotalFuelTime", fuelTime);
        nbt.putDouble("CurrentGeneration", currentGeneration);
        nbt.putDouble("GenerationCarryover", generationCarry);
        nbt.putDouble("PIDIntegral", pidController.getIntegral());

        // Save fuel inventory
        inventory.writeNbt(nbt, "Inventory");
    }

    @Override
    protected void loadLogisticsData(CompoundTag nbt) {
        super.loadLogisticsData(nbt);

        // Load Stirling-specific data
        burnTime = NbtCompat.getInt(nbt, "BurnTimeRemaining", 0);
        fuelTime = NbtCompat.getInt(nbt, "TotalFuelTime", 0);
        currentGeneration = NbtCompat.getDouble(nbt, "CurrentGeneration", MIN_GENERATION);
        generationCarry = NbtCompat.getDouble(nbt, "GenerationCarryover", 0.0);
        double pidIntegral = NbtCompat.getDouble(nbt, "PIDIntegral", 0.0);
        pidController.setIntegral(pidIntegral);

        // Load fuel inventory (try new key first, fall back to legacy)
        if (nbt.contains("Inventory")) {
            inventory.readNbt(nbt, "Inventory");
        } else if (nbt.contains("FuelStack")) {
            // Legacy: single item stored with CODEC
            inventory.setItem(0,ItemStack.EMPTY);
            ItemStack.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, nbt.get("FuelStack"))
                    .result()
                    .ifPresent(stack -> inventory.setItem(0,stack));
        }
    }

    @Override
    protected void loadLegacyData(net.minecraft.world.level.storage.ValueInput view) {
        super.loadLegacyData(view); // Loads engine data from "Engine" tag

        // Load Stirling-specific data from old "StirlingData" tag
        view.read("StirlingData", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(stirlingData -> {
            burnTime = NbtCompat.getInt(stirlingData, "burnTime", 0);
            fuelTime = NbtCompat.getInt(stirlingData, "fuelTime", 0);
            currentGeneration = NbtCompat.getDouble(stirlingData, "currentGeneration", MIN_GENERATION);
            generationCarry = NbtCompat.getDouble(stirlingData, "generationCarry", 0.0);
            double pidIntegral = NbtCompat.getDouble(stirlingData, "pidIntegral", 0.0);
            pidController.setIntegral(pidIntegral);
        });

        // Load fuel from old "Fuel" tag at root level
        inventory.setItem(0,ItemStack.EMPTY);
        view.read("Fuel", ItemStack.CODEC).ifPresent(stack -> inventory.setItem(0,stack));
    }
}

package com.logistics.core.fabricator;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsMod;
import com.logistics.core.lib.BaseBlockEntity;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.items.ItemInventoryComponent;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.power.engine.PIDController;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Kiln.
 * Crafts valves using molten glass and configurable recipes provided by KilnRecipeManager.
 *
 * <p>Key features:
 * <ul>
 *   <li>Fuel-powered: Burns coal, wood, etc. like a furnace</li>
 *   <li>Glass melting: Glass/sand/panes → molten glass (requires heat)</li>
 *   <li>Valve crafting: Pattern-based recipes using molten glass in a 3x3 grid (data-driven)</li>
 *   <li>Energy-based heat system: Internal energy buffer maps to temperature via exponential curve</li>
 *   <li>PID-controlled fuel consumption: Variable burn rate maintains target temperature</li>
 * </ul>
 *
 * <p>Inventory layout (12 slots):
 * <ul>
 *   <li>Slot 0: Fuel input (coal, wood, lava buckets, etc.)</li>
 *   <li>Slot 1: Glass input (glass/sand/panes only)</li>
 *   <li>Slots 2-10: 3x3 crafting grid</li>
 *   <li>Slot 11: Output (valves)</li>
 * </ul>
 *
 * <p>Heat system:
 * <ul>
 *   <li>Internal energy (E) accumulates from fuel consumption</li>
 *   <li>Temperature derived via: T = TMAX * (1 - exp(-E / C))</li>
 *   <li>PID controller adjusts throttle (0-1) to maintain operating setpoint</li>
 *   <li>Burn rate: BASE * (throttle * fuelBurnCap) ticks/tick</li>
 *   <li>Energy decay: E_loss = E / TAU per tick (replaces passive cooling)</li>
 * </ul>
 */
public class KilnBlockEntity extends BaseBlockEntity
        implements HasItemStorage, HasFluidStorage, MenuBehavior.HasMenu {

    // ==================== Constants ====================

    private static final int FUEL_SLOT = 0;
    private static final int GLASS_INPUT_SLOT = 1;
    private static final int GRID_START_SLOT = 2;
    private static final int GRID_END_SLOT = 10;
    private static final int OUTPUT_SLOT = 11;
    private static final int TOTAL_SLOTS = 12;

    private static final long GLASS_TANK_CAPACITY = 2000L; // 2 buckets

    private static final int MELT_INTERVAL_TICKS = 40; // 2 seconds

    // Glass conversion rates (in millibuckets)
    private static final long GLASS_BLOCK_MB = 1000;
    private static final long GLASS_PANE_MB = 300;

    // ==================== Components ====================

    private final ItemInventoryComponent inventory = new ItemInventoryComponent(TOTAL_SLOTS, this::setChanged);
    private final FluidTankComponent glassTank;

    // ==================== State ====================

    // Energy-based heat system
    private double energyE = 0.0;              // Internal energy buffer
    private double temperature = 0.0;          // Derived from energy each tick
    private double fuelEnergyTicks = 0.0;      // Fractional fuel buffer
    private double activeFuelBurnCap = 0.0;    // Current fuel's burn cap (ticks/tick)
    private final PIDController pidController; // PID for throttle control
    private double currentThrottle = 0.0;      // Last computed throttle (for display/debug)

    // Melting and crafting progress
    private int glassMeltProgress = 0;
    private double meltEnergyDebtFrac = 0.0; // Fractional accumulator for distributed melt energy cost

    // Recipe state
    private Identifier activeRecipeId = null;
    private int annealProgressTicks = 0;
    private double craftEnergyDebtFrac = 0.0;

    // ==================== Constructor ====================

    public KilnBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsCore.ENTITY.KILN, pos, state);

        // Initialize fluid tank (molten glass only)
        this.glassTank = new FluidTankComponent(GLASS_TANK_CAPACITY, this::setChanged) {};

        // Initialize PID controller
        this.pidController = new PIDController(
            KilnConstants.PID_KP,
            KilnConstants.PID_KI,
            KilnConstants.PID_KD
        );
    }

    // ==================== Ticker ====================

    public static void tick(Level level, BlockPos pos, BlockState state, KilnBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }

        // Energy-based PID heat system tick order:
        // 1. Update temperature from energy
        entity.updateTemperature();

        // 2. PID-controlled fuel burn
        entity.tickFuelSystem();

        // 3. Apply energy decay (replaces passive cooling)
        entity.applyEnergyDecay();

        // 4. Glass melting (temp checks + energy work cost)
        entity.tickGlassMelting();

        // 5. Recipe crafting (temp checks + energy work cost)
        entity.tickCrafting();

        // 6. Sync to client
        entity.syncLitState();
        entity.setChanged();
    }

    // ==================== Helper Methods ====================

    /**
     * Updates temperature from current energy using exponential mapping.
     * T = TMAX * (1 - exp(-E / C))
     */
    private void updateTemperature() {
        temperature = KilnConstants.TMAX * (1.0 - Math.exp(-energyE / KilnConstants.THERMAL_MASS_C));
    }

    /**
     * Derives burn cap for a given fuel burn time.
     * cap = REFERENCE_RATE * (burnTime / REFERENCE_TIME) ^ EXPONENT
     * Clamped to [BURN_CAP_MIN, BURN_CAP_MAX]
     */
    private double deriveBurnCap(int burnTimeTicks) {
        double ratio = burnTimeTicks / KilnConstants.BURN_CAP_REFERENCE_ITEM_TIME;
        double cap = KilnConstants.BURN_CAP_REFERENCE_RATE * Math.pow(ratio, KilnConstants.BURN_CAP_EXPONENT);
        return Math.max(KilnConstants.BURN_CAP_MIN, Math.min(cap, KilnConstants.BURN_CAP_MAX));
    }

    /**
     * Computes throttle using PID controller with optional startup kick.
     * Startup kick provides aggressive warmup when below 80% of setpoint.
     */
    private double computeThrottle() {
        double pidThrottle = pidController.compute(
            KilnConstants.OPERATING_SETPOINT,
            temperature,
            0.0,
            1.0
        );

        // Startup kick for aggressive warmup from cold
        if (temperature < KilnConstants.OPERATING_SETPOINT * 0.8) {
            double tempRatio = temperature / KilnConstants.OPERATING_SETPOINT;
            double kick = Math.pow(1.0 - tempRatio, KilnConstants.KICK_EXPONENT);
            double kickAmount = kick * (1.0 - KilnConstants.MIN_THROTTLE_KICK);
            pidThrottle = Math.max(pidThrottle, KilnConstants.MIN_THROTTLE_KICK + kickAmount);
        }

        return Math.max(0.0, Math.min(1.0, pidThrottle));
    }

    /**
     * Computes energy decay (loss) for this tick.
     * loss = E / TAU_TICKS (exponential decay)
     */
    private double computeEnergyDecay() {
        return energyE / KilnConstants.ENERGY_LOSS_TAU_TICKS;
    }

    // ==================== Fuel System ====================

    /**
     * PID-controlled fuel system.
     * Loads fuel from slot when buffer empty, computes throttle, burns at variable rate.
     */
    private void tickFuelSystem() {
        // Load fuel if buffer empty and fuel available
        if (fuelEnergyTicks <= 0.0 && !inventory.getItem(FUEL_SLOT).isEmpty()) {
            loadFuel();
        }

        // No fuel available - reset PID and idle
        if (fuelEnergyTicks <= 0.0) {
            pidController.reset();
            currentThrottle = 0.0;
            return;
        }

        // Compute PID throttle
        currentThrottle = computeThrottle();

        // Variable burn rate based on throttle and fuel burn cap
        double burnRate = KilnConstants.BASE_BURN_RATE * (currentThrottle * activeFuelBurnCap);
        double fuelConsumed = Math.min(burnRate, fuelEnergyTicks);
        fuelEnergyTicks -= fuelConsumed;

        // Add energy from burned fuel
        energyE += fuelConsumed * KilnConstants.ENERGY_PER_FUEL_TICK;
    }

    /**
     * Loads fuel from the fuel slot into the energy buffer.
     * Derives burn cap from vanilla burn time and consumes the item.
     */
    private void loadFuel() {
        if (level == null) return;

        ItemStack fuel = inventory.getItem(FUEL_SLOT);
        int burnTicks = (int) level.fuelValues().burnDuration(fuel);
        if (burnTicks <= 0) return;

        // Derive burn cap from fuel type
        activeFuelBurnCap = deriveBurnCap(burnTicks);
        fuelEnergyTicks = burnTicks;

        // Consume fuel item
        if (fuel.is(Items.LAVA_BUCKET)) {
            inventory.setItem(FUEL_SLOT, new ItemStack(Items.BUCKET));
        } else {
            fuel.shrink(1);
        }

        setChanged();
    }

    // ==================== Energy Decay ====================

    /**
     * Applies energy decay (replaces passive cooling).
     * loss = E / TAU_TICKS per tick
     */
    private void applyEnergyDecay() {
        if (energyE > 0.0) {
            double loss = computeEnergyDecay();
            energyE = Math.max(0.0, energyE - loss);
        }
    }

    // ==================== Glass Melting ====================

    private void tickGlassMelting() {
        ItemStack glassStack = inventory.getItem(GLASS_INPUT_SLOT);
        if (glassStack.isEmpty()) {
            // Don't reset progress - pause
            return;
        }

        long mbYield = getGlassYield(glassStack);
        if (mbYield == 0) {
            return;
        }

        if (glassTank.amount + mbYield > GLASS_TANK_CAPACITY) {
            return;
        }

        // Check temperature requirement
        if (temperature < KilnConstants.GLASS_MELT_MIN_TEMP) {
            return; // Pause, don't reset
        }

        // Increment progress
        glassMeltProgress++;

        // Distributed energy consumption while melting
        meltEnergyDebtFrac += KilnConstants.MELT_ENERGY_COST_PER_TICK;
        double energyToConsume = Math.floor(meltEnergyDebtFrac);
        if (energyToConsume > 0.0) {
            energyE = Math.max(0.0, energyE - energyToConsume);
            meltEnergyDebtFrac -= energyToConsume;
        }

        // Complete melt operation
        if (glassMeltProgress >= MELT_INTERVAL_TICKS) {
            if (glassTank.amount + mbYield <= GLASS_TANK_CAPACITY) {
                // Apply any remaining fractional energy cost
                double remainingEnergy = Math.ceil(meltEnergyDebtFrac);
                energyE = Math.max(0.0, energyE - remainingEnergy);
                meltEnergyDebtFrac = 0.0;

                // Add molten glass
                glassTank.variant = FluidVariant.of(LogisticsCore.FLUID.MOLTEN_GLASS_STILL);
                glassTank.amount += mbYield;

                // Consume input
                glassStack.shrink(1);
            }

            // Reset progress interval
            glassMeltProgress -= MELT_INTERVAL_TICKS;
        }
    }

    private long getGlassYield(ItemStack stack) {
        if (stack.is(Items.GLASS) || stack.is(Items.SAND)) {
            return GLASS_BLOCK_MB;
        } else if (stack.is(Items.GLASS_PANE)) {
            return GLASS_PANE_MB;
        }
        return 0;
    }

    // ==================== Crafting ====================

    private void tickCrafting() {
        // Find recipe if none active
        if (activeRecipeId == null) {
            KilnRecipe recipe = findMatchingRecipe();
            if (recipe == null) {
                annealProgressTicks = 0;
                return;
            }

            // Check start conditions
            if (!canStartCraft(recipe)) {
                annealProgressTicks = 0;
                return;
            }

            activeRecipeId = recipe.getId();
        }

        // Get active recipe
        KilnRecipe recipe = getRecipeById(activeRecipeId);
        if (recipe == null) {
            activeRecipeId = null;
            annealProgressTicks = 0;
            return;
        }

        // Validate still matches
        if (!recipe.matches(inventory, GRID_START_SLOT)) {
            activeRecipeId = null;
            annealProgressTicks = 0;
            craftEnergyDebtFrac = 0.0;
            return;
        }

        // Check continue conditions
        if (!canContinueCraft(recipe)) {
            // Pause, don't reset
            return;
        }

        // Progress annealing
        annealProgressTicks++;

        // Distributed energy draw per tick
        double perTickEnergy = recipe.getEnergyPerTick();
        craftEnergyDebtFrac += perTickEnergy;
        double energyToConsume = Math.floor(craftEnergyDebtFrac);
        if (energyToConsume > 0.0) {
            energyE = Math.max(0.0, energyE - energyToConsume);
            craftEnergyDebtFrac -= energyToConsume;
        }

        // Complete craft
        if (annealProgressTicks >= recipe.getProcessTimeTicks()) {
            completeCraft(recipe);
        }
    }

    private KilnRecipe findMatchingRecipe() {
        return KilnRecipeManager.getAllRecipes().values().stream()
            .filter(recipe -> recipe.matches(inventory, GRID_START_SLOT))
            .findFirst()
            .orElse(null);
    }

    private KilnRecipe getRecipeById(Identifier recipeId) {
        return KilnRecipeManager.getRecipe(recipeId);
    }

    private boolean canStartCraft(KilnRecipe recipe) {
        return glassTank.amount >= recipe.getFluidAmountMb()
            && temperature >= KilnConstants.CRAFTING_MIN_TEMP
            && canAcceptOutput(recipe.getResultItem());
    }

    private boolean canContinueCraft(KilnRecipe recipe) {
        return temperature >= KilnConstants.CRAFTING_MIN_TEMP
            && canAcceptOutput(recipe.getResultItem());
    }

    private boolean canAcceptOutput(ItemStack result) {
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
            && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void completeCraft(KilnRecipe recipe) {
        if (glassTank.amount < recipe.getFluidAmountMb()) {
            // Insufficient glass - abort craft and reset state
            activeRecipeId = null;
            annealProgressTicks = 0;
            craftEnergyDebtFrac = 0.0;
            return;
        }

        // Consume inputs
        recipe.consumeIngredients(inventory, GRID_START_SLOT);

        // Consume molten glass
        glassTank.amount -= recipe.getFluidAmountMb();

        // Apply remaining energy cost
        double remainingEnergy = Math.ceil(craftEnergyDebtFrac);
        energyE = Math.max(0.0, energyE - remainingEnergy);
        craftEnergyDebtFrac = 0.0;

        // Insert result
        insertOutput(recipe.getResultItem());

        // Reset
        activeRecipeId = null;
        annealProgressTicks = 0;
    }

    private void insertOutput(ItemStack result) {
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setItem(OUTPUT_SLOT, result);
        } else {
            output.grow(result.getCount());
        }
    }

    private void syncLitState() {
        if (level == null) return;

        BlockState state = getBlockState();
        boolean wasLit = state.getValue(KilnBlock.LIT);
        boolean isLit = fuelEnergyTicks > 0.0;

        if (isLit != wasLit) {
            level.setBlock(getBlockPos(), state.setValue(KilnBlock.LIT, isLit), Block.UPDATE_ALL);
        }
    }

    // ==================== Capability Implementations ====================

    @Override
    public Storage<ItemVariant> itemStorage(@Nullable Direction side) {
        return inventory.storage();
    }

    @Override
    public Storage<FluidVariant> fluidStorage(@Nullable Direction side) {
        // Molten glass is internal-only - cannot be extracted or inserted from outside
        // (conceptually, molten glass wouldn't stay hot outside the kiln)
        return null;
    }

    // ==================== NBT Persistence ====================

    @Override
    protected void saveLogisticsData(CompoundTag tag) {
        inventory.writeNbt(tag, "Inventory");
        glassTank.writeNbt(tag, "GlassTank");

        // Energy-based heat system
        tag.putDouble("EnergyE", energyE);
        tag.putDouble("Temperature", temperature);
        tag.putDouble("FuelEnergyTicks", fuelEnergyTicks);
        tag.putDouble("ActiveFuelBurnCap", activeFuelBurnCap);
        tag.putDouble("PIDIntegral", pidController.getIntegral());
        tag.putDouble("CurrentThrottle", currentThrottle);

        // Progress state
        tag.putInt("GlassMeltProgress", glassMeltProgress);
        tag.putDouble("MeltEnergyDebtFrac", meltEnergyDebtFrac);
        tag.putInt("AnnealProgressTicks", annealProgressTicks);
        tag.putDouble("CraftEnergyDebtFrac", craftEnergyDebtFrac);

        if (activeRecipeId != null) {
            tag.putString("ActiveRecipeId", activeRecipeId.toString());
        }
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag) {
        inventory.readNbt(tag, "Inventory");
        glassTank.readNbt(tag, "GlassTank");

        // Load new energy-based system
        energyE = NbtCompat.getDouble(tag, "EnergyE", 0.0);
        temperature = NbtCompat.getDouble(tag, "Temperature", 0.0);
        fuelEnergyTicks = NbtCompat.getDouble(tag, "FuelEnergyTicks", 0.0);
        activeFuelBurnCap = NbtCompat.getDouble(tag, "ActiveFuelBurnCap", 0.0);
        pidController.setIntegral(NbtCompat.getDouble(tag, "PIDIntegral", 0.0));
        currentThrottle = NbtCompat.getDouble(tag, "CurrentThrottle", 0.0);

        glassMeltProgress = NbtCompat.getInt(tag, "GlassMeltProgress", 0);
        meltEnergyDebtFrac = NbtCompat.getDouble(tag, "MeltEnergyDebtFrac", 0.0);
        annealProgressTicks = NbtCompat.getInt(tag, "AnnealProgressTicks", 0);
        craftEnergyDebtFrac = NbtCompat.getDouble(tag, "CraftEnergyDebtFrac", 0.0);

        if (tag.contains("ActiveRecipeId")) {
            tag.getString("ActiveRecipeId").ifPresent(s -> activeRecipeId = LogisticsMod.parseIdentifier(s));
        }

        // Migration: convert old heat system to energy if present
        if (tag.contains("Heat") && !tag.contains("EnergyE")) {
            int oldHeat = NbtCompat.getInt(tag, "Heat", 0);
            if (oldHeat > 0) {
                // Inverse of T = TMAX * (1 - exp(-E / C))
                // E = -C * ln(1 - T / TMAX)
                double targetTemp = Math.min(oldHeat, KilnConstants.TMAX - 1);
                energyE = -KilnConstants.THERMAL_MASS_C * Math.log(1.0 - targetTemp / KilnConstants.TMAX);
                temperature = targetTemp;
            }
        }

        // Migrate old fuel buffer to new system
        if (tag.contains("BurnTicksRemaining") && !tag.contains("FuelEnergyTicks")) {
            int oldBurnTicks = NbtCompat.getInt(tag, "BurnTicksRemaining", 0);
            if (oldBurnTicks > 0) {
                fuelEnergyTicks = oldBurnTicks;
                // Use default burn cap if no migration info available
                activeFuelBurnCap = KilnConstants.BURN_CAP_REFERENCE_RATE;
            }
        }
    }

    // ==================== Getters ====================

    public double getTemperature() {
        return temperature;
    }

    public boolean isBurning() {
        return fuelEnergyTicks > 0.0;
    }

    public int getGlassMeltProgress() {
        return glassMeltProgress;
    }

    public int getAnnealProgressTicks() {
        return annealProgressTicks;
    }

    public FluidTankComponent getGlassTank() {
        return glassTank;
    }

    public Container getInventory() {
        return inventory;
    }

    public double getCurrentThrottle() {
        return currentThrottle;
    }

    public double getEnergyE() {
        return energyE;
    }

    public double getFuelEnergyTicks() {
        return fuelEnergyTicks;
    }

    // ==================== Menu / GUI ====================

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.logistics.kiln");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new KilnScreenHandler(syncId, playerInventory, inventory, createContainerData());
            }
        };
    }

    private ContainerData createContainerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (int) temperature; // Display temperature
                    case 1 -> (int) (glassTank.amount & 0xFFFFFFFF); // Glass tank low bits
                    case 2 -> (int) (glassTank.amount >> 32); // Glass tank high bits
                    case 3 -> glassMeltProgress;
                    case 4 -> {
                        // Calculate anneal progress as percentage (0-100)
                        KilnRecipe recipe = getRecipeById(activeRecipeId);
                        if (recipe == null || annealProgressTicks == 0) {
                            yield 0;
                        }
                        yield Math.min(100, (100 * annealProgressTicks) / recipe.getProcessTimeTicks());
                    }
                    case 5 -> (fuelEnergyTicks > 0.0) ? 1 : 0; // Burning flag
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> temperature = value;
                    case 1 -> glassTank.amount = (glassTank.amount & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                    case 2 -> glassTank.amount = (glassTank.amount & 0xFFFFFFFFL) | ((long) value << 32);
                    case 3 -> glassMeltProgress = value;
                    case 4 -> annealProgressTicks = value;
                    case 5 -> fuelEnergyTicks = (value > 0) ? 1.0 : 0.0;
                }
            }

            @Override
            public int getCount() {
                return 6;
            }
        };
    }
}

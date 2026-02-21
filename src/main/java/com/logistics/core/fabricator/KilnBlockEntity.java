package com.logistics.core.fabricator;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.BaseBlockEntity;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.ModFluids;
import com.logistics.core.lib.items.ItemInventoryComponent;
import com.logistics.core.lib.storage.NbtCompat;
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
 * Crafts electron tubes using molten glass and pattern matching in a 3x3 grid.
 *
 * <p>Key features:
 * <ul>
 *   <li>Fuel-powered: Burns coal, wood, etc. like a furnace</li>
 *   <li>Glass melting: Glass/sand/panes → molten glass (requires heat, 5 seconds)</li>
 *   <li>Tube crafting: 7 material + 2 redstone/ender eye + 1000mb glass → 4 tubes (10 seconds)</li>
 *   <li>Heat system: 20-10,000°C range, rises when burning fuel, decays when idle</li>
 * </ul>
 *
 * <p>Inventory layout (12 slots):
 * <ul>
 *   <li>Slot 0: Fuel input (coal, wood, lava buckets, etc.)</li>
 *   <li>Slot 1: Glass input (glass/sand/panes only)</li>
 *   <li>Slots 2-10: 3x3 crafting grid</li>
 *   <li>Slot 11: Output (electron tubes)</li>
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
    private static final int MELT_MIN_HEAT = 600;
    private static final int MELT_HEAT_COST = 1;

    private static final double DEFAULT_HEAT_PER_TICK = 2.0;

    // Glass conversion rates (in millibuckets)
    private static final long GLASS_BLOCK_MB = 1000;
    private static final long GLASS_PANE_MB = 300;

    // Heat buffer constants (kiln-specific integer heat system)
    private static final int MAX_HEAT = 2000;
    private static final int HEAT_COLD = 0;
    private static final int HEAT_WARMING = 200;
    private static final int HEAT_WORKING = 600;
    private static final int HEAT_HOT = 1000;
    private static final int HEAT_EXTREME = 1400;
    private static final int HEAT_OVERFIRE = 1700;

    // ==================== Components ====================

    private final ItemInventoryComponent inventory = new ItemInventoryComponent(TOTAL_SLOTS, this::setChanged);
    private final FluidTankComponent glassTank;

    // ==================== State ====================

    // Heat buffer (0-2000) - kiln-specific integer heat system
    private int heat = 0;
    private double heatFrac = 0.0; // Fractional accumulator for heat generation

    // Fuel state with pause mechanics
    private int burnTicksRemaining = 0;
    private int burnTicksTotal = 0;
    private double activeHeatPerTick = 0.0;
    private int activeFuelMaxHeat = 0;

    // Melting and crafting progress
    private int glassMeltProgress = 0;
    private double meltHeatDebtFrac = 0.0; // Fractional accumulator for distributed melt heat cost

    // Recipe state (will be updated for JSON recipes)
    private Identifier activeRecipeId = null;
    private int annealProgressTicks = 0;
    private double heatDebtFrac = 0.0;

    // ==================== Constructor ====================

    public KilnBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsCore.ENTITY.KILN, pos, state);

        // Initialize fluid tank (molten glass only)
        this.glassTank = new FluidTankComponent(GLASS_TANK_CAPACITY, this::setChanged) {};
    }

    // ==================== Ticker ====================

    public static void tick(Level level, BlockPos pos, BlockState state, KilnBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }

        // Per spec tick order:
        // 1. Passive cooling
        entity.tickPassiveCooling();

        // 2. Fuel burn tick (with pause mechanic)
        entity.tickFuelBurning();

        // 3. Melting
        entity.tickGlassMelting();

        // 4. Annealing crafting
        entity.tickCrafting();

        // 5. Sync to client
        entity.syncLitState();
        entity.setChanged();
    }

    // ==================== Fuel System ====================

    private void tickFuelBurning() {
        // Start new fuel if needed
        if (burnTicksRemaining <= 0 && needsHeat()) {
            refuel();
        }

        // Burn tick with pause mechanic
        if (burnTicksRemaining > 0) {
            // Compute effective max heat (for future upgrades)
            int effectiveMaxHeat = Math.min(MAX_HEAT, activeFuelMaxHeat); // + bonusFromUpgrades

            // Accumulate heat
            heatFrac += activeHeatPerTick;
            int deltaHeat = (int) Math.floor(heatFrac);

            if (deltaHeat > 0) {
                if (heat + deltaHeat <= effectiveMaxHeat) {
                    // Add heat and consume fuel
                    heat += deltaHeat;
                    heatFrac -= deltaHeat;
                    burnTicksRemaining--;
                } else {
                    // Fuel pauses - no heat added, no burn consumed
                    // Clamp accumulator to prevent large debt
                    heatFrac = Math.min(heatFrac, activeHeatPerTick * 2);
                }
            }
        }
    }

    private boolean needsHeat() {
        // Need heat if we're melting glass or have active recipe
        return !inventory.getItem(GLASS_INPUT_SLOT).isEmpty() || activeRecipeId != null;
    }

    private void refuel() {
        if (level == null) return;

        ItemStack fuel = inventory.getItem(FUEL_SLOT);
        int burnTicks = (int) level.fuelValues().burnDuration(fuel);
        if (burnTicks <= 0) return;

        // Derive fuel properties
        double heatPerTick = DEFAULT_HEAT_PER_TICK;
        int fuelMaxHeat = deriveFuelMaxHeat(burnTicks);

        // Override table (for future special fuels)
        // Currently empty

        // Consume fuel
        if (fuel.is(Items.LAVA_BUCKET)) {
            inventory.setItem(FUEL_SLOT, new ItemStack(Items.BUCKET));
        } else {
            fuel.shrink(1);
        }

        // Set burn state
        burnTicksRemaining = burnTicks;
        burnTicksTotal = burnTicks;
        activeHeatPerTick = heatPerTick;
        activeFuelMaxHeat = fuelMaxHeat;
        heatFrac = 0.0;

        setChanged();
    }

    private int deriveFuelMaxHeat(int burnTicks) {
        if (burnTicks < 200) return 700;
        if (burnTicks < 400) return 900;
        if (burnTicks < 800) return 1100;
        if (burnTicks < 1600) return 1200;
        if (burnTicks < 3200) return 1300;
        return 1400;
    }

    // ==================== Passive Cooling ====================

    private void tickPassiveCooling() {
        if (heat > 0) {
            int passiveLoss = Math.max(1, heat / 500);
            heat = Math.max(0, heat - passiveLoss);
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

        if (heat < MELT_MIN_HEAT) {
            return; // Pause, don't reset
        }

        if (glassTank.amount >= GLASS_TANK_CAPACITY) {
            return;
        }

        // Increment progress
        glassMeltProgress++;

        // Distributed heat consumption: 0.5 heat per tick while melting
        meltHeatDebtFrac += 0.5;
        int heatToConsume = (int) Math.floor(meltHeatDebtFrac);
        if (heatToConsume > 0) {
            heat = Math.max(0, heat - heatToConsume);
            meltHeatDebtFrac -= heatToConsume;
        }

        // Complete melt operation
        if (glassMeltProgress >= MELT_INTERVAL_TICKS) {
            if (heat >= MELT_MIN_HEAT && glassTank.amount + mbYield <= GLASS_TANK_CAPACITY) {
                // Apply any remaining fractional heat cost
                int remainingHeat = (int) Math.ceil(meltHeatDebtFrac);
                heat = Math.max(0, heat - remainingHeat);
                meltHeatDebtFrac = 0.0;

                // Add molten glass
                glassTank.variant = FluidVariant.of(ModFluids.MOLTEN_GLASS_STILL);
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
                // Debug: print grid contents
                if (level != null && !level.isClientSide() && level.getGameTime() % 20 == 0) {
                    System.out.println("No recipe found. Grid contents:");
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = inventory.getItem(GRID_START_SLOT + i);
                        System.out.println("  Slot " + i + ": " + (stack.isEmpty() ? "empty" : stack.getItem()));
                    }
                    System.out.println("Total recipes loaded: " + KilnRecipeManager.getAllRecipes().size());
                }
                annealProgressTicks = 0;
                return;
            }

            // Check start conditions
            if (!canStartCraft(recipe)) {
                // Debug: why can't we start?
                if (level != null && !level.isClientSide() && level.getGameTime() % 20 == 0) {
                    System.out.println("Found recipe " + recipe.getId() + " but can't start:");
                    System.out.println("  Glass: " + glassTank.amount + " / " + recipe.getMoltenCost());
                    System.out.println("  Heat: " + heat + " / " + recipe.getRequiredHeat());
                    System.out.println("  Can accept output: " + canAcceptOutput(recipe.getResultItem()));
                }
                annealProgressTicks = 0;
                return;
            }

            System.out.println("Starting recipe: " + recipe.getId());
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
            heatDebtFrac = 0.0;
            return;
        }

        // Check continue conditions
        if (!canContinueCraft(recipe)) {
            // Pause, don't reset
            return;
        }

        // Progress annealing
        annealProgressTicks++;

        // Distributed heat draw
        double perTickHeat = (double) recipe.getHeatCost() / recipe.getSoakTicks();
        heatDebtFrac += perTickHeat;
        int heatToConsume = (int) Math.floor(heatDebtFrac);
        if (heatToConsume > 0) {
            heat = Math.max(0, heat - heatToConsume);
            heatDebtFrac -= heatToConsume;
        }

        // Complete craft
        if (annealProgressTicks >= recipe.getSoakTicks()) {
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
        return glassTank.amount >= recipe.getMoltenCost()
            && heat >= recipe.getRequiredHeat()
            && canAcceptOutput(recipe.getResultItem());
    }

    private boolean canContinueCraft(KilnRecipe recipe) {
        return heat >= recipe.getRequiredHeat()
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
        // Consume inputs
        recipe.consumeIngredients(inventory, GRID_START_SLOT);

        // Consume molten glass
        glassTank.amount -= recipe.getMoltenCost();

        // Apply remaining heat cost
        int remainingHeat = (int) Math.ceil(heatDebtFrac);
        heat = Math.max(0, heat - remainingHeat);
        heatDebtFrac = 0.0;

        // Insert result
        insertOutput(recipe.getResultItem().copy());

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
        boolean isLit = burnTicksRemaining > 0;

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
        return glassTank.storage();
    }

    // ==================== NBT Persistence ====================

    @Override
    protected void saveLogisticsData(CompoundTag tag) {
        inventory.writeNbt(tag, "Inventory");
        glassTank.writeNbt(tag, "GlassTank");

        tag.putInt("Heat", heat);
        tag.putDouble("HeatFrac", heatFrac);
        tag.putInt("BurnTicksRemaining", burnTicksRemaining);
        tag.putInt("BurnTicksTotal", burnTicksTotal);
        tag.putDouble("ActiveHeatPerTick", activeHeatPerTick);
        tag.putInt("ActiveFuelMaxHeat", activeFuelMaxHeat);
        tag.putInt("GlassMeltProgress", glassMeltProgress);
        tag.putDouble("MeltHeatDebtFrac", meltHeatDebtFrac);
        tag.putInt("AnnealProgressTicks", annealProgressTicks);
        tag.putDouble("HeatDebtFrac", heatDebtFrac);

        if (activeRecipeId != null) {
            tag.putString("ActiveRecipeId", activeRecipeId.toString());
        }
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag) {
        inventory.readNbt(tag, "Inventory");
        glassTank.readNbt(tag, "GlassTank");

        heat = NbtCompat.getInt(tag, "Heat", 0);
        heatFrac = tag.getDouble("HeatFrac").orElse(0.0);
        burnTicksRemaining = NbtCompat.getInt(tag, "BurnTicksRemaining", 0);
        burnTicksTotal = NbtCompat.getInt(tag, "BurnTicksTotal", 0);
        activeHeatPerTick = tag.getDouble("ActiveHeatPerTick").orElse(0.0);
        activeFuelMaxHeat = NbtCompat.getInt(tag, "ActiveFuelMaxHeat", 0);
        glassMeltProgress = NbtCompat.getInt(tag, "GlassMeltProgress", 0);
        meltHeatDebtFrac = tag.getDouble("MeltHeatDebtFrac").orElse(0.0);
        annealProgressTicks = NbtCompat.getInt(tag, "AnnealProgressTicks", 0);
        heatDebtFrac = tag.getDouble("HeatDebtFrac").orElse(0.0);

        if (tag.contains("ActiveRecipeId")) {
            tag.getString("ActiveRecipeId").ifPresent(s -> activeRecipeId = Identifier.parse(s));
        }
    }

    // ==================== Getters ====================

    public int getHeat() {
        return heat;
    }

    public int getBurnTicksRemaining() {
        return burnTicksRemaining;
    }

    public int getBurnTicksTotal() {
        return burnTicksTotal;
    }

    public double getActiveHeatPerTick() {
        return activeHeatPerTick;
    }

    public int getActiveFuelMaxHeat() {
        return activeFuelMaxHeat;
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
                    case 0 -> heat;
                    case 1 -> burnTicksRemaining;
                    case 2 -> burnTicksTotal;
                    case 3 -> (int) (activeHeatPerTick * 10); // Encode as fixed-point
                    case 4 -> activeFuelMaxHeat;
                    case 5 -> (int) (glassTank.amount & 0xFFFFFFFF);
                    case 6 -> (int) (glassTank.amount >> 32);
                    case 7 -> glassMeltProgress;
                    case 8 -> annealProgressTicks;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> heat = value;
                    case 1 -> burnTicksRemaining = value;
                    case 2 -> burnTicksTotal = value;
                    case 3 -> activeHeatPerTick = value / 10.0;
                    case 4 -> activeFuelMaxHeat = value;
                    case 5 -> glassTank.amount = (glassTank.amount & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                    case 6 -> glassTank.amount = (glassTank.amount & 0xFFFFFFFFL) | ((long) value << 32);
                    case 7 -> glassMeltProgress = value;
                    case 8 -> annealProgressTicks = value;
                }
            }

            @Override
            public int getCount() {
                return 9;
            }
        };
    }
}

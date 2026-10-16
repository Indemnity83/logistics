package com.logistics.forestry.farm;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.MachineHudModel;
import com.logistics.core.machine.area.PlotArea;
import com.logistics.core.machine.area.PlotScanner;
import com.logistics.core.machine.area.RingArea;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.tier.MachineTier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The farm's work loop. Every work interval it takes the next plot of its {@link RingArea} from a
 * {@link PlotScanner}. While the plot's column holds anything that is not the farm's own growth, it
 * clears one block of it ({@link ColumnClearer}); once the column is clear, it asks the
 * {@link FarmLogic} what the plot needs and performs that one {@link PlotAction} if the farm can
 * afford it. Every per-plot question goes to the logic its {@link FarmPlan} assigns to that plot.
 *
 * <p>The ring surrounds the {@link FarmGeometry#COURTYARD}, the farm included, and reaches out to
 * the outer size of the farm's tier. The courtyard is never touched: every world write goes through
 * a {@link CourtyardGuard}, which refuses any in a courtyard column at any Y.
 *
 * <p>The plots are one block above the farm, so their soil, {@code plot.below()}, is level with the
 * farm block and crops grow on top of it. A column is cleared from the plot upward.
 *
 * <p>The farm-wide gates live here, not in the logic. A redstone signal pauses everything. Every op
 * costs RF and is skipped whole when the buffer cannot pay for it; tilling, on a farm with a tank,
 * also costs water, planting needs germlings and laying soil needs soil. A full output buffer stops every op that
 * breaks a block. Nothing touches an unloaded position, every write first asks claim protection,
 * and a block entity or an unbreakable block is never broken.
 *
 * <p>A harvest of several blocks (a tree) is broken a few blocks per tick until done, ahead of the
 * scan. Once it completes and the plot stands empty, the soil under the plot becomes the logic's
 * {@link FarmLogic#soilAfterHarvest}. Drops and the loose items swept from the ring afterwards go to
 * the output slots, and what does not fit waits in an overflow list.
 */
public final class FarmComponent implements MachineComponent, MachineComponent.HudContributor {

    /** Opens the world a farm works; {@code null} where there is none to work (client, unit tests). */
    @FunctionalInterface
    public interface WorldFactory {
        @Nullable
        FarmWorldAccess open(MachineContext ctx, @Nullable UUID owner);
    }

    /** The live server level. */
    public static final WorldFactory LEVEL =
            (ctx, owner) -> ctx.level() instanceof ServerLevel level ? new LevelFarmWorld(level, owner) : null;

    public static final String HUD_STATUS = "jade.logistics.machine.status";
    public static final String HUD_PLOT = "jade.logistics.farm.plot";
    public static final String HUD_PLOT_SIZE = "jade.logistics.farm.plot_size";

    /** Loose items are swept from the soil layer to this far above the plots. */
    private static final int SWEEP_HEIGHT = 32;

    private static final int MAX_MOISTURE = Collections.max(BlockStateProperties.MOISTURE.getPossibleValues());

    private record HarvestTarget(BlockPos pos, Block block) {}

    private final String id;
    // The block's own logic: the plan's default, never asked about a plot directly.
    private final FarmLogic logic;
    private final Container items;
    private final EnergyStorageComponent energy;
    @Nullable private final FluidStoreComponent water;
    private final Supplier<FarmSettings> settings;
    private final Supplier<MachineTier> tier;
    private final WorldFactory worlds;
    private final FarmOutput output;
    private final Map<FarmLogic, Inputs> inputs = new IdentityHashMap<>();
    private final FarmStatusTracker tracker = new FarmStatusTracker();
    private final ArrayDeque<HarvestTarget> harvest = new ArrayDeque<>();

    @Nullable private PlotScanner scanner;
    private int scannerBase;
    private int scannerMax;
    private int savedCursor;
    private int stepsInPass;
    private int cooldown;
    @Nullable private UUID owner;
    private int innerOverride;
    private int outerOverride;
    private int clearanceOverride;
    @Nullable private Function<RingArea, FarmPlan> planOverride;
    // Only the harvest's plot persists; the blocks are re-planned from it after a reload.
    @Nullable private BlockPos harvestBase;
    // Plots of the area the harvest has broken a block on, whose soil it restores once complete.
    private final Set<BlockPos> harvestedPlots = new LinkedHashSet<>();
    private boolean replanHarvest;
    private FarmStatus status = FarmStatus.IDLE;
    // What the last NO_POWER / NO_GERMLINGS / NO_SOIL was short of, so the status clears as soon as it is there.
    private long powerNeeded;
    private ItemStack germlingNeeded = ItemStack.EMPTY;
    private int germlingCountNeeded;
    private FarmLogic germlingLogic;
    private FarmLogic soilLogic;
    private boolean dirty;

    public FarmComponent(
            String id,
            FarmLogic logic,
            ItemStoreComponent items,
            EnergyStorageComponent energy,
            @Nullable FluidStoreComponent water,
            Supplier<FarmSettings> settings,
            Supplier<MachineTier> tier,
            WorldFactory worlds) {
        this.id = id;
        this.logic = logic;
        this.items = items.container();
        this.energy = energy;
        this.water = water;
        this.settings = settings;
        this.tier = tier;
        this.worlds = worlds;
        this.germlingLogic = logic;
        this.soilLogic = logic;
        this.output = new FarmOutput(this.items, FarmMachine.OUTPUT_START, FarmMachine.OUTPUT_END);
    }

    @Override
    public String id() {
        return id;
    }

    // ==================== State ====================

    public FarmStatus status() {
        return status;
    }

    /** Side of the square whose ring is worked: a 15 means the ring reaches 15×15. */
    public int areaSize() {
        return outerOverride > 0 ? outerOverride : settings.get().profile(tier.get()).outerSize();
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    /**
     * Test seam: works the ring between squares of {@code inner} and {@code outer} (both odd,
     * {@code inner >= 1}, {@code outer > inner}) instead of the courtyard and the tier's; 0 for both
     * restores them. The square of {@code inner} is then the one never touched.
     */
    public void setAreaOverride(int inner, int outer) {
        if (inner <= 0 && outer <= 0) {
            innerOverride = 0;
            outerOverride = 0;
            return;
        }
        new RingArea(BlockPos.ZERO, inner, outer); // validates
        innerOverride = inner;
        outerOverride = outer;
    }

    /** Test seam: keeps {@code height} blocks clear above each plot instead of the logic's; 0 or less restores it. */
    public void setClearanceHeightOverride(int height) {
        clearanceOverride = Math.max(0, height);
    }

    /** Test seam: assigns the ring's plots to logics through {@code plans} instead of the block's one logic; null restores it. */
    void setPlanOverride(@Nullable Function<RingArea, FarmPlan> plans) {
        planOverride = plans;
    }

    private int clearanceHeight(FarmLogic plotLogic) {
        return clearanceOverride > 0 ? clearanceOverride : plotLogic.clearanceHeight();
    }

    List<ItemStack> overflow() {
        return output.overflow();
    }

    boolean isHarvesting() {
        return !harvest.isEmpty();
    }

    // ==================== Tick ====================

    @Override
    public void onLoad(MachineContext ctx) {
        // Spread farms loaded together across the work interval.
        cooldown = Math.floorMod(ctx.pos().asLong(), settings.get().workIntervalTicks());
    }

    @Override
    public void serverTick(MachineContext ctx) {
        FarmWorldAccess opened = worlds.open(ctx, owner);
        if (opened == null) {
            return;
        }
        FarmSettings s = settings.get();
        if (output.drainOverflow()) {
            dirty = true;
        }
        RingArea area = area(ctx, s);
        FarmWorldAccess world = new CourtyardGuard(opened, area.keepOut());
        FarmPlan plan = plan(area);
        ensureScanner(area.plotCount(), s);

        boolean paused = world.hasNeighborSignal(ctx.pos());
        if (!paused) {
            work(world, s, area, plan);
        }
        updateStatus(ctx, s, area, paused);

        if (dirty) {
            dirty = false;
            ctx.markChanged();
        }
    }

    private void work(FarmWorldAccess world, FarmSettings s, PlotArea area, FarmPlan plan) {
        if (replanHarvest) {
            replanHarvest(world, area, plan);
        }
        if (!harvest.isEmpty()) {
            FarmStatus outcome = continueHarvest(world, s, area, plan);
            tracker.record(outcome);
            if (outcome == FarmStatus.WORKING) {
                return;
            }
            // A stalled harvest leaves the rest of the plot to be worked.
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        cooldown = s.workIntervalTicks() - 1;
        step(world, s, area, plan);
    }

    private void step(FarmWorldAccess world, FarmSettings s, PlotArea area, FarmPlan plan) {
        tracker.step();
        int index = scanner.next();
        stepsInPass++;
        dirty = true;

        BlockPos plot = area.plot(index);
        FarmStatus outcome = world.isLoaded(plot) ? workPlot(world, s, area, plan, index, plot) : FarmStatus.IDLE;
        tracker.record(outcome);
        if (outcome == FarmStatus.WORKING) {
            scanner.markWorked();
        }
        if (stepsInPass >= scanner.plotCount()) {
            stepsInPass = 0;
            cooldown += scanner.idleTicks();
        }
    }

    private void ensureScanner(int plotCount, FarmSettings s) {
        if (scanner == null || scannerBase != s.idleBackoffBaseTicks() || scannerMax != s.idleBackoffMaxTicks()) {
            int cursor = scanner != null ? scanner.cursor() : savedCursor;
            scannerBase = s.idleBackoffBaseTicks();
            scannerMax = s.idleBackoffMaxTicks();
            scanner = PlotScanner.restore(plotCount, cursor, scannerBase, scannerMax);
            stepsInPass = 0;
        } else if (scanner.plotCount() != plotCount) {
            scanner.resize(plotCount);
            stepsInPass = 0;
        }
    }

    /**
     * The ring of plots around the courtyard out to the tier's outer size, one block above the farm:
     * their soil is level with it.
     */
    private RingArea area(MachineContext ctx, FarmSettings s) {
        BlockPos origin = ctx.pos().above();
        return outerOverride > 0
                ? new RingArea(origin, innerOverride, outerOverride)
                : FarmGeometry.ring(origin, s.profile(tier.get()).outerSize());
    }

    private FarmPlan plan(RingArea area) {
        return planOverride != null ? planOverride.apply(area) : new SingleLogicPlan(logic, area);
    }

    // ==================== Ops ====================

    /** Clears one block from {@code plot}'s column, or, once it is clear, does what its logic asks. */
    private FarmStatus workPlot(
            FarmWorldAccess world, FarmSettings s, PlotArea area, FarmPlan plan, int index, BlockPos plot) {
        FarmLogic plotLogic = plan.logicFor(index);
        ColumnClearer.ColumnView column = pos -> {
            BlockState state = world.state(pos);
            return ColumnClearer.classify(state, world.destroySpeed(pos), plotLogic.isOwnGrowth(state));
        };
        return switch (ColumnClearer.next(column, plot, clearanceHeight(plotLogic))) {
            case ColumnClearer.Clearing.Clear clear -> {
                PlotAction action = plotLogic.decide(world, plan.regionFor(index), plot, inputsFor(plotLogic));
                yield execute(world, s, area, plan, plotLogic, plot, action);
            }
            case ColumnClearer.Clearing.Break breakBlock -> clearBlock(world, s, breakBlock.pos());
            case ColumnClearer.Clearing.Drain drain -> drain(world, s, drain.pos());
            case ColumnClearer.Clearing.Blocked blocked -> FarmStatus.BLOCKED;
        };
    }

    private FarmStatus clearBlock(FarmWorldAccess world, FarmSettings s, BlockPos pos) {
        if (output.isFull()) {
            return FarmStatus.OUTPUT_FULL;
        }
        if (energy.amount() < s.rfClearPerBlock()) {
            return noPower(s.rfClearPerBlock());
        }
        if (!world.canModify(pos)) {
            return FarmStatus.BLOCKED;
        }
        List<ItemStack> drops = world.breakBlock(pos);
        if (drops == null) {
            return FarmStatus.BLOCKED;
        }
        drops.forEach(output::accept);
        energy.consume(s.rfClearPerBlock());
        return FarmStatus.WORKING;
    }

    private FarmStatus drain(FarmWorldAccess world, FarmSettings s, BlockPos pos) {
        if (energy.amount() < s.rfClearPerBlock()) {
            return noPower(s.rfClearPerBlock());
        }
        if (!world.canModify(pos)) {
            return FarmStatus.BLOCKED;
        }
        world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        energy.consume(s.rfClearPerBlock());
        return FarmStatus.WORKING;
    }

    private FarmStatus execute(
            FarmWorldAccess world,
            FarmSettings s,
            PlotArea area,
            FarmPlan plan,
            FarmLogic plotLogic,
            BlockPos plot,
            PlotAction action) {
        return switch (action) {
            case PlotAction.Harvest harvestAction -> startHarvest(world, s, area, plan, plot, harvestAction.blocks());
            case PlotAction.PlaceSoil placeSoil -> placeSoil(world, s, plotLogic, placeSoil.pos());
            case PlotAction.Till till -> till(world, s, till.pos(), till.result());
            case PlotAction.Moisten moisten -> moisten(world, s, moisten.pos());
            case PlotAction.Plant plant -> plant(world, s, plotLogic, plant.positions(), plant.germling());
            case PlotAction.None none -> FarmStatus.IDLE;
            case PlotAction.Blocked blocked ->
                blocked.reason() == PlotAction.Reason.TOO_DARK ? FarmStatus.TOO_DARK : FarmStatus.BLOCKED;
        };
    }

    private FarmStatus startHarvest(
            FarmWorldAccess world, FarmSettings s, PlotArea area, FarmPlan plan, BlockPos plot, List<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return FarmStatus.IDLE;
        }
        // One harvest at a time; one still waiting means it stalled on output or power.
        if (!harvest.isEmpty() || output.isFull()) {
            return output.isFull() ? FarmStatus.OUTPUT_FULL : noPower(s.rfHarvestPerBlock());
        }
        if (energy.amount() < s.rfHarvestPerBlock()) {
            return noPower(s.rfHarvestPerBlock());
        }
        queueHarvest(world, blocks);
        if (harvest.isEmpty()) {
            return FarmStatus.IDLE;
        }
        harvestBase = plot.immutable();
        harvestedPlots.clear();
        return continueHarvest(world, s, area, plan);
    }

    private void queueHarvest(FarmWorld world, List<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            if (world.isLoaded(pos)) {
                BlockState state = world.state(pos);
                if (!state.isAir()) {
                    harvest.add(new HarvestTarget(pos, state.getBlock()));
                }
            }
        }
    }

    /** Breaks up to {@code harvestBlocksPerTick} blocks of the pending harvest. */
    private FarmStatus continueHarvest(FarmWorldAccess world, FarmSettings s, PlotArea area, FarmPlan plan) {
        long cost = s.rfHarvestPerBlock();
        int pending = harvest.size();
        int broken = 0;
        boolean refused = false;
        FarmStatus stall = null;
        while (broken < s.harvestBlocksPerTick() && !harvest.isEmpty()) {
            if (output.isFull()) {
                stall = FarmStatus.OUTPUT_FULL;
                break;
            }
            if (energy.amount() < cost) {
                stall = noPower(cost);
                break;
            }
            HarvestTarget target = harvest.poll();
            if (!world.isLoaded(target.pos()) || world.state(target.pos()).getBlock() != target.block()) {
                continue; // gone or replaced since it was planned
            }
            if (!world.canModify(target.pos())) {
                refused = true;
                continue;
            }
            List<ItemStack> drops = world.breakBlock(target.pos());
            if (drops == null) {
                refused = true;
                continue;
            }
            energy.consume(cost);
            drops.forEach(output::accept);
            broken++;
            if (harvestBase != null && target.pos().getY() == harvestBase.getY() && area.contains(target.pos())) {
                harvestedPlots.add(target.pos().immutable());
            }
        }
        if (harvest.size() != pending) {
            dirty = true;
        }
        if (harvest.isEmpty()) {
            completeHarvest(world, area, plan);
            for (ItemStack stack : world.collectItems(sweepBox(area), area::contains)) {
                output.accept(stack);
            }
        }
        if (broken > 0) {
            tracker.clear(FarmStatus.OUTPUT_FULL);
            return FarmStatus.WORKING;
        }
        if (stall != null) {
            return stall;
        }
        return refused ? FarmStatus.BLOCKED : FarmStatus.IDLE;
    }

    /** Restores the soil under the harvest's plot and every plot it broke a block on. */
    private void completeHarvest(FarmWorldAccess world, PlotArea area, FarmPlan plan) {
        if (harvestBase != null) {
            harvestedPlots.add(harvestBase);
        }
        for (BlockPos plot : harvestedPlots) {
            restoreSoil(world, area, plan, plot);
        }
        harvestedPlots.clear();
        harvestBase = null;
    }

    /**
     * Turns the soil under {@code plot}, emptied by a completed harvest, into its logic's
     * after-harvest soil. A position that is not a plot of the ring is left alone.
     */
    private void restoreSoil(FarmWorldAccess world, PlotArea area, FarmPlan plan, BlockPos plot) {
        int index = area.indexOf(plot);
        BlockPos soilPos = plot.below();
        if (index < 0 || !world.isLoaded(plot) || !world.isLoaded(soilPos) || !world.state(plot).isAir()) {
            return;
        }
        BlockState soil = world.state(soilPos);
        BlockState after = plan.logicFor(index).soilAfterHarvest(soil);
        if (after == null || after == soil || !world.canModify(soilPos)) {
            return;
        }
        world.setBlock(soilPos, after, Block.UPDATE_ALL);
    }

    private void replanHarvest(FarmWorldAccess world, PlotArea area, FarmPlan plan) {
        replanHarvest = false;
        BlockPos base = harvestBase;
        harvestBase = null;
        dirty = true;
        if (base == null || !harvest.isEmpty() || !world.isLoaded(base)) {
            return;
        }
        int index = area.indexOf(base);
        if (index >= 0) {
            FarmLogic baseLogic = plan.logicFor(index);
            if (baseLogic.decide(world, plan.regionFor(index), base, inputsFor(baseLogic))
                    instanceof PlotAction.Harvest replanned) {
                queueHarvest(world, replanned.blocks());
            }
        }
        harvestBase = base;
        if (harvest.isEmpty()) {
            // Nothing left standing: the harvest finished as the farm unloaded.
            completeHarvest(world, area, plan);
        }
    }

    private static AABB sweepBox(PlotArea area) {
        BlockPos min = area.min();
        BlockPos max = area.max();
        return new AABB(
                min.getX(), min.getY() - 1, min.getZ(), max.getX() + 1, max.getY() + SWEEP_HEIGHT + 1, max.getZ() + 1);
    }

    private FarmStatus placeSoil(FarmWorldAccess world, FarmSettings s, FarmLogic plotLogic, BlockPos pos) {
        if (!world.isLoaded(pos)) {
            return FarmStatus.IDLE;
        }
        if (energy.amount() < s.rfSoil()) {
            return noPower(s.rfSoil());
        }
        int slot = soilSlot(plotLogic);
        if (slot < 0) {
            soilLogic = plotLogic;
            return FarmStatus.NO_SOIL;
        }
        BlockState soil = ((BlockItem) items.getItem(slot).getItem()).getBlock().defaultBlockState();
        BlockState current = world.state(pos);
        if (current.is(soil.getBlock())) {
            return FarmStatus.IDLE;
        }
        if (!world.canModify(pos) || current.hasBlockEntity() || world.destroySpeed(pos) < 0) {
            return FarmStatus.BLOCKED;
        }
        // Air and fluids are simply overwritten; anything else is broken first.
        boolean overwrite = current.isAir() || current.getBlock() instanceof LiquidBlock;
        if (!overwrite) {
            if (output.isFull()) {
                return FarmStatus.OUTPUT_FULL;
            }
            List<ItemStack> drops = world.breakBlock(pos);
            if (drops == null) {
                return FarmStatus.BLOCKED;
            }
            drops.forEach(output::accept);
        }
        world.setBlock(pos, soil, Block.UPDATE_ALL);
        items.removeItem(slot, 1);
        energy.consume(s.rfSoil());
        tracker.clear(FarmStatus.NO_SOIL);
        return FarmStatus.WORKING;
    }

    private FarmStatus till(FarmWorldAccess world, FarmSettings s, BlockPos pos, BlockState result) {
        if (!world.isLoaded(pos) || world.state(pos) == result) {
            return FarmStatus.IDLE;
        }
        if (energy.amount() < s.rfTill()) {
            return noPower(s.rfTill());
        }
        long needed = water != null ? FluidUnits.mb(s.waterPerTillMb()) : 0;
        if (water != null && water.tank().getAmount() < needed) {
            return FarmStatus.NO_WATER;
        }
        if (!world.canModify(pos)) {
            return FarmStatus.BLOCKED;
        }
        world.setBlock(pos, result, Block.UPDATE_ALL);
        if (needed > 0) {
            FluidTankComponent tank = water.tank();
            tank.extract(tank.getFluidKey(), needed, false);
        }
        energy.consume(s.rfTill());
        tracker.clear(FarmStatus.NO_WATER);
        return FarmStatus.WORKING;
    }

    private FarmStatus moisten(FarmWorldAccess world, FarmSettings s, BlockPos pos) {
        if (!world.isLoaded(pos)) {
            return FarmStatus.IDLE;
        }
        BlockState state = world.state(pos);
        if (!state.hasProperty(BlockStateProperties.MOISTURE)
                || state.getValue(BlockStateProperties.MOISTURE) >= MAX_MOISTURE) {
            return FarmStatus.IDLE;
        }
        if (energy.amount() < s.rfMoisten()) {
            return noPower(s.rfMoisten());
        }
        if (!world.canModify(pos)) {
            return FarmStatus.BLOCKED;
        }
        // Moisture is a visual-only change, so neighbours are not updated.
        world.setBlock(pos, state.setValue(BlockStateProperties.MOISTURE, MAX_MOISTURE), Block.UPDATE_CLIENTS);
        energy.consume(s.rfMoisten());
        return FarmStatus.WORKING;
    }

    private FarmStatus plant(
            FarmWorldAccess world, FarmSettings s, FarmLogic plotLogic, List<BlockPos> positions, ItemStack germling) {
        if (positions.isEmpty()) {
            return FarmStatus.IDLE;
        }
        for (BlockPos pos : positions) {
            if (!world.isLoaded(pos)) {
                return FarmStatus.IDLE;
            }
        }
        long cost = s.rfPlant() * positions.size();
        if (energy.amount() < cost) {
            return noPower(cost);
        }
        if (germling.isEmpty() || inputsFor(plotLogic).germlingCount(germling) < positions.size()) {
            germlingNeeded = germling;
            germlingCountNeeded = positions.size();
            germlingLogic = plotLogic;
            return FarmStatus.NO_GERMLINGS;
        }
        List<BlockState> plants = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            BlockState plant = plotLogic.plantState(germling, pos, world);
            BlockState current = world.state(pos);
            if (plant == null
                    || !world.canModify(pos)
                    || !(current.isAir() || current.canBeReplaced())
                    || !world.canSurvive(plant, pos)) {
                return FarmStatus.BLOCKED;
            }
            plants.add(plant);
        }
        for (int i = 0; i < positions.size(); i++) {
            world.setBlock(positions.get(i), plants.get(i), Block.UPDATE_ALL);
        }
        takeGermlings(plotLogic, germling, positions.size());
        energy.consume(cost);
        tracker.clear(FarmStatus.NO_GERMLINGS);
        return FarmStatus.WORKING;
    }

    private FarmStatus noPower(long needed) {
        powerNeeded = needed;
        return FarmStatus.NO_POWER;
    }

    // ==================== Inventory ====================

    /** First soil slot holding a placeable block {@code plotLogic} accepts as soil, or -1. */
    private int soilSlot(FarmLogic plotLogic) {
        if (!plotLogic.laysSoil()) {
            return -1;
        }
        for (int slot = FarmMachine.SOIL_START; slot < FarmMachine.SOIL_END; slot++) {
            ItemStack stack = items.getItem(slot);
            if (stack.getItem() instanceof BlockItem && plotLogic.isSoil(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private void takeGermlings(FarmLogic plotLogic, ItemStack kind, int count) {
        int remaining = count;
        if (plotLogic.replantsFromOutput()) {
            remaining -= output.take(kind, remaining);
        }
        for (int slot = FarmMachine.GERMLING_START; slot < FarmMachine.GERMLING_END && remaining > 0; slot++) {
            ItemStack stack = items.getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, kind)) {
                int n = Math.min(remaining, stack.getCount());
                items.removeItem(slot, n);
                remaining -= n;
            }
        }
    }

    private Inputs inputsFor(FarmLogic plotLogic) {
        return inputs.computeIfAbsent(plotLogic, Inputs::new);
    }

    /** One logic's view of the inventory: only the germlings and soil that logic accepts. */
    private final class Inputs implements FarmInputs {

        private final FarmLogic logic;

        Inputs(FarmLogic logic) {
            this.logic = logic;
        }

        @Override
        public List<ItemStack> germlings() {
            List<ItemStack> kinds = new ArrayList<>();
            if (logic.replantsFromOutput()) {
                for (int slot = FarmMachine.OUTPUT_START; slot < FarmMachine.OUTPUT_END; slot++) {
                    ItemStack stack = items.getItem(slot);
                    if (!stack.isEmpty() && logic.isGermling(stack)) {
                        addKind(kinds, stack);
                    }
                }
            }
            for (int slot = FarmMachine.GERMLING_START; slot < FarmMachine.GERMLING_END; slot++) {
                ItemStack stack = items.getItem(slot);
                if (!stack.isEmpty() && logic.isGermling(stack)) {
                    addKind(kinds, stack);
                }
            }
            return kinds;
        }

        private void addKind(List<ItemStack> kinds, ItemStack stack) {
            if (stack.isEmpty()) {
                return;
            }
            for (ItemStack kind : kinds) {
                if (ItemStack.isSameItemSameComponents(kind, stack)) {
                    return;
                }
            }
            kinds.add(stack.copyWithCount(germlingCount(stack)));
        }

        @Override
        public int germlingCount(ItemStack kind) {
            if (kind.isEmpty() || !logic.isGermling(kind)) {
                return 0;
            }
            int total = logic.replantsFromOutput() ? output.count(kind) : 0;
            for (int slot = FarmMachine.GERMLING_START; slot < FarmMachine.GERMLING_END; slot++) {
                ItemStack stack = items.getItem(slot);
                if (ItemStack.isSameItemSameComponents(stack, kind)) {
                    total += stack.getCount();
                }
            }
            return total;
        }

        @Override
        public boolean hasSoil() {
            return soilSlot(logic) >= 0;
        }

        @Override
        public ItemStack soil() {
            int slot = soilSlot(logic);
            return slot >= 0 ? items.getItem(slot).copyWithCount(1) : ItemStack.EMPTY;
        }

        @Override
        public boolean hasWater() {
            return water != null && water.tank().getAmount() >= FluidUnits.mb(settings.get().waterPerTillMb());
        }
    }

    // ==================== Status ====================

    private void updateStatus(MachineContext ctx, FarmSettings s, PlotArea area, boolean paused) {
        status = paused ? FarmStatus.PAUSED : tracker.status(area.plotCount(), problem -> stillApplies(problem, s));
        BlockState state = ctx.blockState();
        boolean lit = status == FarmStatus.WORKING;
        if (state.hasProperty(FarmMachineBlock.LIT) && state.getValue(FarmMachineBlock.LIT) != lit) {
            ctx.setBlockState(state.setValue(FarmMachineBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    private boolean stillApplies(FarmStatus problem, FarmSettings s) {
        return switch (problem) {
            case NO_POWER -> energy.amount() < powerNeeded;
            case NO_WATER -> water == null || water.tank().getAmount() < FluidUnits.mb(s.waterPerTillMb());
            case NO_GERMLINGS ->
                germlingNeeded.isEmpty() || inputsFor(germlingLogic).germlingCount(germlingNeeded) < germlingCountNeeded;
            case NO_SOIL -> soilSlot(soilLogic) < 0;
            case OUTPUT_FULL -> output.isFull();
            default -> true;
        };
    }

    @Override
    public void contributeHud(MachineHudModel hud) {
        hud.line(HUD_STATUS, status.translationKey());
        hud.line(HUD_PLOT, HUD_PLOT_SIZE, Integer.toString(areaSize()));
    }

    // ==================== Persistence ====================

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Cursor", scanner != null ? scanner.cursor() : savedCursor);
        if (owner != null) {
            tag.putString("Owner", owner.toString());
        }
        if (harvestBase != null) {
            tag.putBoolean("Harvesting", true);
            tag.putLong("HarvestBase", harvestBase.asLong());
            tag.putLongArray(
                    "HarvestedPlots",
                    harvestedPlots.stream().mapToLong(BlockPos::asLong).toArray());
        }
        output.save(tag, "Overflow", registries);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        savedCursor = NbtCompat.getInt(tag, "Cursor", 0);
        scanner = null;
        owner = parseUuid(NbtCompat.getString(tag, "Owner", ""));
        harvest.clear();
        harvestBase = NbtCompat.getBoolean(tag, "Harvesting", false)
                ? BlockPos.of(NbtCompat.getLong(tag, "HarvestBase", 0L))
                : null;
        replanHarvest = harvestBase != null;
        harvestedPlots.clear();
        if (harvestBase != null) {
            for (long plot : NbtCompat.getLongArray(tag, "HarvestedPlots", new long[0])) {
                harvestedPlots.add(BlockPos.of(plot));
            }
        }
        output.load(tag, "Overflow", registries);
    }

    @Nullable
    private static UUID parseUuid(String value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

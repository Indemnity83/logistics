package com.logistics.forestry.farm;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.MachineHudModel;
import com.logistics.core.machine.area.PlotScanner;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.forestry.logic.LevelTreeView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A farm's work loop: it watches every position of its Reach, {@value #REACH} blocks out on every
 * side (corners included) and {@value #REACH_HEIGHT} above and below the farm, the farm's own
 * position left out, and harvests whatever its {@link TendLogic} finds ripe there. It plants only
 * on its Plots.
 *
 * <p>The scan visits one position per check, round-robin, bounded every tick by a
 * {@link WorkBudget}: its tier's checks and actions per second. A pass that finds nothing ripe backs
 * the scan off ({@link PlotScanner#idleTicks()}). A ripe find starts a harvest: its blocks are broken
 * in order, one per action, and while one is under way the scan waits. Each target is re-checked as
 * it is broken; one that has changed since it was planned has the rest of the harvest planned again.
 * A harvest under way is saved with the drops it has collected, and planned again once loaded.
 *
 * <p>Once every block is broken the harvest's drops are worked through in order: one matching
 * germling is taken from them to replant the plot, or else one from the inventory, the rest go into
 * the inventory, and whatever does not fit is dropped at the plot, or at the farm itself while the
 * plot is not loaded. The farm never stops for a full inventory.
 *
 * <p>Every harvested position whose ground can hold it becomes a Plot ({@link FarmPlots}),
 * remembered with its germling, as does one the scan finds holding an unripe plant the farm tends
 * ({@link TendLogic#growing}). A harvest that {@linkplain TendLogic.Harvest#keepsPlot keeps its plot}
 * (a stalk's base, which it never breaks) has nothing to replant: the plant left standing keeps the
 * Plot. A Plot left unplanted is an Empty Plot: the scan replants it, its
 * ground repaired first ({@link TendLogic#plotGround}), as soon as its germling is in the inventory.
 * A Plot is forgotten once its ground can no longer hold it or anything other than its own plant
 * stands on it; cover that planting replaces (snow, grass) leaves it an Empty Plot.
 *
 * <p>A position just outside the Reach is looked after from the Reach position beside it when the
 * logic names it {@linkplain TendLogic#attached attached} there (a pod on a log in the Reach), and
 * once it is a Plot, from then on: it is harvested, kept and forgotten like any other Plot.
 *
 * <p>Each tick, before any action, the farm pays its upkeep: {@link TendLogic#upkeep}'s RF/t for
 * every Plot, Empty Plots included, fractions carried until they make a whole RF. While it cannot
 * pay, it does nothing and reports No power; its Plots are kept.
 *
 * <p>Every action checks everything it needs without side effects first (loaded, claim protection,
 * RF), then changes the world, and charges only once the world reports the change. A harvest only
 * starts while the buffer holds its first break and the replant; a position the farm cannot pay for
 * holds the scan, reading nothing, until it can. A host that does not permit work
 * ({@link MachineContext#workEnabled}, the farm's redstone control) pauses everything. Nothing
 * touches an unloaded position.
 *
 * <p>A picking farm ({@link TendLogic#picks}) also keeps Picking Plots: the scan takes one on
 * wherever {@link TendLogic#pickPlot} names one in the Reach's lowest levels, and forgets one once
 * it no longer stands. Each tick the farm runs, after its actions, every Picking Plot gets a
 * simulated random tick with the world's {@code randomTickSpeed} in 4096; each one picks one
 * {@link TendLogic.Bearing#produce} with the bearing's chance, charged its cost, into the inventory
 * or, when that is full, dropped at the Plot. A Picking Plot pays upkeep like any other.
 */
public final class TendFarmComponent implements MachineComponent, MachineComponent.HudContributor {

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
    public static final String HUD_REACH = "jade.logistics.farm.reach";
    public static final String HUD_REACH_SIZE = "jade.logistics.farm.reach_size";
    public static final String HUD_TREES = "jade.logistics.farm.trees";
    public static final String HUD_TREES_COUNT = "jade.logistics.farm.trees_count";

    /** How far the Reach extends from the farm on every horizontal side. */
    public static final int REACH = 3;

    /** How far the Reach extends below the farm, and above it unless its logic reaches higher ({@link TendLogic#reachAbove}). */
    public static final int REACH_HEIGHT = 1;

    /** A block gets a random tick each tick with a chance of {@code randomTickSpeed} in this many. */
    static final int RANDOM_TICK_BLOCKS = 4096;

    /** Side of the Reach's square. */
    public static final int REACH_SIZE = 2 * REACH + 1;

    /** Ticks the buffer must stay empty before the lamp goes off. */
    static final int LAMP_OFF_TICKS = 20;

    /** Every position of the shared Reach relative to the farm, in scan order. */
    static final List<BlockPos> REACH_OFFSETS = reachOffsets(REACH_HEIGHT);

    private record Target(BlockPos pos, Block block) {}

    /**
     * A Plot's ground ({@link TendLogic#support}) at {@code pos}, as read, and as
     * {@link TendLogic#plotGround} makes it ready for planting.
     */
    private record Ground(BlockPos pos, BlockState current, BlockState ready) {
        boolean needsRepair() {
            return current != ready;
        }
    }

    private final String id;
    private final TendLogic logic;
    private final Container items;
    private final FarmOutput inventory;
    private final EnergyStorageComponent energy;
    private final Supplier<FarmSettings> settings;
    private final Supplier<MachineTier> tier;
    private final WorldFactory worlds;
    private final FarmStatusTracker tracker = new FarmStatusTracker();
    private final WorkBudget budget = new WorkBudget();
    private final CountingWorld counted = new CountingWorld();
    private final ArrayDeque<Target> targets = new ArrayDeque<>();
    private final List<ItemStack> collected = new ArrayList<>();
    private final FarmPlots plots = new FarmPlots();
    private final PickPlots pickPlots = new PickPlots();
    // Every position of this farm's Reach relative to it, in scan order.
    private final List<BlockPos> reach;

    // Where the farm's Plots and harvest were recorded; null until its first tick.
    @Nullable private BlockPos origin;
    @Nullable private TendLogic.Harvest harvest;
    // Set when a harvest loaded from a save is still to be planned again.
    private boolean resumeHarvest;
    @Nullable private PlotScanner scanner;
    private int scannerBase;
    private int scannerMax;
    private int savedCursor;
    private int stepsInPass;
    private int cooldown;
    // RF the scan waits for before visiting its current position again; 0 when not waiting.
    private long heldPower;
    @Nullable private UUID owner;
    private int checksOverride;
    private int actionsOverride;
    // Test seam: the chance a tick picks each Picking Plot, in place of random ticks; < 0 when unset.
    private double pickOverride = -1;
    private FarmStatus status = FarmStatus.IDLE;
    // What the last NO_POWER was short of, so the status clears as soon as it is there.
    private long powerNeeded;
    // Upkeep owed below one RF, in thousandths of an RF; not saved.
    private long upkeepOwed;
    private boolean upkeepUnpaid;
    // Consecutive ticks the buffer has been empty, up to LAMP_OFF_TICKS; not saved.
    private int emptyTicks = LAMP_OFF_TICKS;
    private boolean dirty;

    public TendFarmComponent(
            String id,
            TendLogic logic,
            ItemStoreComponent items,
            EnergyStorageComponent energy,
            Supplier<FarmSettings> settings,
            Supplier<MachineTier> tier,
            WorldFactory worlds) {
        this.id = id;
        this.logic = logic;
        this.items = items.container();
        this.inventory = new FarmOutput(this.items, 0, this.items.getContainerSize());
        this.energy = energy;
        this.settings = settings;
        this.tier = tier;
        this.worlds = worlds;
        this.reach = logic.reachAbove() == REACH_HEIGHT ? REACH_OFFSETS : reachOffsets(logic.reachAbove());
    }

    /** The Reach's offsets, from {@code above} the farm down to {@link #REACH_HEIGHT} below it. */
    static List<BlockPos> reachOffsets(int above) {
        List<BlockPos> offsets = new ArrayList<>();
        for (int dy = above; dy >= -REACH_HEIGHT; dy--) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                for (int dx = -REACH; dx <= REACH; dx++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        offsets.add(new BlockPos(dx, dy, dz));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    @Override
    public String id() {
        return id;
    }

    // ==================== State ====================

    public FarmStatus status() {
        return status;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    /** Side of the Reach's square. */
    public int reachSize() {
        return REACH_SIZE;
    }

    /** The Plots the farm remembers, Picking Plots included. */
    public int plotsTotal() {
        return plots.size() + pickPlots.size();
    }

    /** The Picking Plots the farm remembers: for the Orchard, the trees it picks from. */
    public int pickPlotsTotal() {
        return pickPlots.size();
    }

    /** The Empty Plots: those waiting for a germling. */
    public int emptyPlots() {
        return plots.emptyCount();
    }

    /**
     * Test seam: inspects {@code checksPerSecond} positions and performs {@code actionsPerSecond}
     * actions a second instead of the tier's; 0 or less for both restores them.
     */
    public void setRateOverride(int checksPerSecond, int actionsPerSecond) {
        if (checksPerSecond <= 0 && actionsPerSecond <= 0) {
            checksOverride = 0;
            actionsOverride = 0;
            return;
        }
        checksOverride = Math.max(1, checksPerSecond);
        actionsOverride = Math.max(1, actionsPerSecond);
    }

    /**
     * Test seam: picks each Picking Plot with {@code chancePerTick} every tick, in place of random
     * ticks and the bearing's chance; less than 0 restores them.
     */
    public void setPickOverride(double chancePerTick) {
        pickOverride = chancePerTick < 0 ? -1 : Math.min(1, chancePerTick);
    }

    // ==================== Tick ====================

    @Override
    public void onLoad(MachineContext ctx) {
        // Farms loaded together reach their first action on different ticks.
        budget.stagger(ctx.pos().asLong());
    }

    @Override
    public void serverTick(MachineContext ctx) {
        FarmWorldAccess opened = worlds.open(ctx, owner);
        if (opened == null) {
            return;
        }
        FarmSettings s = settings.get();
        counted.attach(opened);
        ensureScanner(s);
        settleAt(ctx.pos());

        boolean paused = !ctx.workEnabled();
        if (!paused) {
            FarmTierProfile profile = s.profile(tier.get());
            if (actionsOverride > 0) {
                profile = profile.withRates(checksOverride, actionsOverride);
            }
            budget.refill(profile.actionsPerSecond(), profile.checksPerSecond(), s.readsPerCheck());
            if (cooldown > 0) {
                cooldown--;
            }
            upkeepUnpaid = !payUpkeep(s);
            if (!upkeepUnpaid) {
                work(ctx.pos(), s);
                ripen(ctx.random(), s);
            }
        }
        updateStatus(ctx, paused, s);

        if (dirty) {
            dirty = false;
            ctx.markChanged();
        }
    }

    /**
     * Records the farm's position; when its saved state came from a farm elsewhere (a copied block
     * entity, a cloned or templated farm), drops that farm's Plots and harvest under way.
     */
    private void settleAt(BlockPos pos) {
        if (pos.equals(origin)) {
            return;
        }
        if (origin != null) {
            plots.clear();
            pickPlots.clear();
            harvest = null;
            resumeHarvest = false;
            targets.clear();
            collected.clear();
        }
        origin = pos.immutable();
        dirty = true;
    }

    private void ensureScanner(FarmSettings s) {
        if (scanner == null || scannerBase != s.idleBackoffBaseTicks() || scannerMax != s.idleBackoffMaxTicks()) {
            int cursor = scanner != null ? scanner.cursor() : savedCursor;
            scannerBase = s.idleBackoffBaseTicks();
            scannerMax = s.idleBackoffMaxTicks();
            scanner = PlotScanner.restore(reach.size(), cursor, scannerBase, scannerMax);
            stepsInPass = 0;
        }
    }

    /**
     * Pays this tick's upkeep for every Plot, Empty and Picking Plots included, in whole RF, the fraction left
     * over carried to the next tick. False, paying and carrying nothing, when the buffer is short.
     */
    private boolean payUpkeep(FarmSettings s) {
        long owed = upkeepOwed + plotsTotal() * Math.round(s.upkeep(logic.upkeep()) * 1000);
        long due = owed / 1000;
        if (energy.amount() < due) {
            powerNeeded = due;
            return false;
        }
        if (due > 0) {
            energy.consume(due);
        }
        upkeepOwed = owed - due * 1000;
        return true;
    }

    /** Spends this tick's actions: on the harvest under way, or on the scan once there is none. */
    private void work(BlockPos origin, FarmSettings s) {
        while (budget.hasAction()) {
            FarmStatus outcome;
            if (harvest != null) {
                long mark = counted.reads();
                outcome = breakNext(origin, s);
                budget.spendReads(counted.reads() - mark);
                tracker.record(outcome);
            } else {
                outcome = scan(origin, s);
            }
            if (outcome != FarmStatus.WORKING) {
                return;
            }
            budget.spendAction();
        }
    }

    /**
     * Visits positions while the checks last, until one starts a harvest: returns WORKING once the
     * harvest's first action is taken, or the reason the scan stopped.
     */
    private FarmStatus scan(BlockPos origin, FarmSettings s) {
        int readsPerCheck = s.readsPerCheck();
        while (budget.reads() >= readsPerCheck) {
            if (heldPower > 0 && energy.amount() < heldPower) {
                return FarmStatus.NO_POWER;
            }
            if (cooldown > 0) {
                return FarmStatus.IDLE;
            }
            BlockPos pos = origin.offset(reach.get(scanner.cursor()));
            tracker.step();
            dirty = true;
            counted.forget();
            long mark = counted.reads();
            FarmStatus outcome = visit(origin, pos, s);
            budget.spendReads(Math.max(readsPerCheck, counted.reads() - mark));
            tracker.record(outcome);
            if (outcome == FarmStatus.NO_POWER) {
                heldPower = powerNeeded;
                return outcome;
            }
            heldPower = 0;
            scanner.next();
            stepsInPass++;
            if (outcome == FarmStatus.WORKING) {
                scanner.markWorked();
            }
            if (stepsInPass >= scanner.positionCount()) {
                stepsInPass = 0;
                cooldown += scanner.idleTicks();
            }
            if (outcome == FarmStatus.WORKING) {
                return outcome;
            }
        }
        return FarmStatus.IDLE;
    }

    /** Looks at {@code pos} and starts harvesting whatever is ripe there. */
    private FarmStatus visit(BlockPos origin, BlockPos pos, FarmSettings s) {
        if (!counted.isLoaded(pos)) {
            return FarmStatus.IDLE;
        }
        FarmStatus outcome = visitPlot(origin, pos, s);
        for (BlockPos beside : beyondReach(origin, pos)) {
            if (outcome == FarmStatus.WORKING || outcome == FarmStatus.NO_POWER) {
                return outcome;
            }
            if (!counted.isLoaded(beside)) {
                continue;
            }
            FarmStatus found = visitPlot(origin, beside, s);
            if (outcome == FarmStatus.IDLE) {
                outcome = found;
            } else if (found == FarmStatus.WORKING || found == FarmStatus.NO_POWER) {
                return found;
            }
        }
        return outcome;
    }

    /**
     * The neighbours of {@code pos} outside the Reach that the scan looks after from it: those
     * {@link TendLogic#attached} names, and the Plots remembered there.
     */
    private List<BlockPos> beyondReach(BlockPos origin, BlockPos pos) {
        List<BlockPos> beside = List.of();
        Collection<BlockPos> attached = null;
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);
            if (inReach(origin, next)) {
                continue;
            }
            if (plots.germling(next) == null) {
                if (attached == null) {
                    attached = logic.attached(counted, pos);
                }
                if (!attached.contains(next)) {
                    continue;
                }
            }
            if (beside.isEmpty()) {
                beside = new ArrayList<>(2);
            }
            beside.add(next);
        }
        return beside;
    }

    /** Whether {@code pos} lies in the Reach of the farm at {@code origin}, the farm itself included. */
    private boolean inReach(BlockPos origin, BlockPos pos) {
        int dy = pos.getY() - origin.getY();
        return Math.abs(pos.getX() - origin.getX()) <= REACH
                && Math.abs(pos.getZ() - origin.getZ()) <= REACH
                && dy >= -REACH_HEIGHT
                && dy <= logic.reachAbove();
    }

    /** Harvests what is ripe at {@code pos}, else looks after its Plot or takes it on as one. */
    private FarmStatus visitPlot(BlockPos origin, BlockPos pos, FarmSettings s) {
        TendLogic.Harvest found = logic.ripe(counted, pos);
        if (found != null) {
            return startHarvest(origin, found, s);
        }
        ItemStack germling = plots.germling(pos);
        if (germling != null) {
            return tendPlot(pos, germling, s);
        }
        ItemStack growing = logic.growing(counted, pos);
        if (growing != null && readGround(pos) != null) {
            plots.remember(pos, growing, false);
            dirty = true;
        }
        if (logic.picks()) {
            notePickPlot(origin, pos);
        }
        return FarmStatus.IDLE;
    }

    /**
     * Looks after the Plot at {@code pos}: forgets it once its ground can no longer hold it or
     * something else stands on it, and replants it from the inventory, repairing its ground first if
     * needed, once it is empty and its germling is there.
     */
    private FarmStatus tendPlot(BlockPos pos, ItemStack germling, FarmSettings s) {
        BlockState occupant = counted.seen(pos);
        if (occupant == null) {
            occupant = counted.state(pos);
        }
        Ground ground = readGround(pos);
        boolean planted = !occupant.isAir() && logic.isPlanted(occupant, germling);
        if (ground == null || !planted && !isOpen(occupant)) {
            plots.forget(pos);
            return FarmStatus.IDLE;
        }
        if (planted) {
            plots.markEmpty(pos, false);
            return FarmStatus.IDLE;
        }
        plots.markEmpty(pos, true);
        if (inventory.count(germling) == 0) {
            return FarmStatus.WAITING_FOR_SEEDS;
        }
        long cost = plantCost(ground, s);
        if (energy.amount() < cost) {
            return noPower(cost);
        }
        if (!replant(pos, germling, ground, s)) {
            return FarmStatus.BLOCKED;
        }
        inventory.take(germling, 1);
        plots.markEmpty(pos, false);
        return FarmStatus.WORKING;
    }

    /** Whether planting can take {@code occupant}'s place: air, or cover planting replaces (snow, grass). */
    private static boolean isOpen(BlockState occupant) {
        return occupant.isAir() || occupant.canBeReplaced() && occupant.getFluidState().isEmpty();
    }

    private boolean isOpen(BlockPos plot) {
        return counted.isLoaded(plot) && isOpen(counted.state(plot));
    }

    /** The ground {@code plot} grows on; {@code null} when it cannot hold a Plot or is not loaded. */
    @Nullable
    private Ground readGround(BlockPos plot) {
        BlockPos foot = logic.support(counted, plot);
        if (foot == null || !counted.isLoaded(foot)) {
            return null;
        }
        BlockState current = counted.state(foot);
        BlockState ready = logic.plotGround(current);
        return ready != null ? new Ground(foot.immutable(), current, ready) : null;
    }

    /** RF a plant on {@code ground} takes: the plant, and the till if the ground needs repair. */
    private static long plantCost(@Nullable Ground ground, FarmSettings s) {
        return s.cost(FarmCost.PLANT) + (tills(ground) ? s.cost(FarmCost.TILL) : 0);
    }

    private static boolean tills(@Nullable Ground ground) {
        return ground != null && ground.needsRepair();
    }

    // ==================== Harvest ====================

    /**
     * Takes {@code found} as the farm's harvest and breaks its first block as this action. It only
     * starts while the buffer holds that break and the replant, if it has one to make.
     */
    private FarmStatus startHarvest(BlockPos origin, TendLogic.Harvest found, FarmSettings s) {
        List<Target> planned = targets(found);
        if (planned.isEmpty()) {
            return FarmStatus.IDLE;
        }
        BlockState first = counted.seen(planned.get(0).pos());
        long needed = s.cost(logic.harvestCost(first))
                + (found.germling().isEmpty() || found.keepsPlot() ? 0 : s.cost(FarmCost.PLANT));
        if (energy.amount() < needed) {
            return noPower(needed);
        }
        harvest = found;
        targets.addAll(planned);
        collected.clear();
        dirty = true;
        return breakNext(origin, s);
    }

    /**
     * Breaks the next block of the harvest; finishes it once the last is broken. A target already
     * gone is passed over and one claim protection refuses is left standing; any other change since
     * it was planned has the rest of the harvest planned again ({@link TendLogic#resume}), as has a
     * harvest taken up again after a reload.
     */
    private FarmStatus breakNext(BlockPos origin, FarmSettings s) {
        boolean refused = false;
        if (resumeHarvest) {
            resumeHarvest = false;
            if (!counted.isLoaded(harvest.plot())) {
                resumeHarvest = true;
                return FarmStatus.IDLE;
            }
            replan();
        }
        boolean replanned = false;
        while (!targets.isEmpty()) {
            Target target = targets.peek();
            BlockPos pos = target.pos();
            if (!counted.isLoaded(pos)) {
                targets.poll();
                continue;
            }
            BlockState state = counted.state(pos);
            if (state.isAir()) {
                targets.poll();
                continue;
            }
            if (state.getBlock() != target.block()) {
                // Changed since planned: never break on a stale plan, plan the rest again once.
                targets.clear();
                if (!replanned) {
                    replanned = true;
                    replan();
                }
                continue;
            }
            // Natural leaves go through their own path: only the canopy of a tree being felled.
            boolean leaves = LevelTreeView.isNaturalLeaves(state);
            if (leaves ? !counted.canBreakLeaves(pos) : !counted.canModify(pos)) {
                targets.poll();
                refused = true;
                continue;
            }
            long cost = s.cost(logic.harvestCost(state));
            if (energy.amount() < cost) {
                return noPower(cost);
            }
            List<ItemStack> drops = counted.breakBlock(pos);
            targets.poll();
            dirty = true;
            if (drops == null) {
                refused = true;
                continue;
            }
            energy.consume(cost);
            drops.forEach(drop -> collected.add(drop.copy()));
            if (targets.isEmpty()) {
                finishHarvest(origin, s);
            }
            return FarmStatus.WORKING;
        }
        finishHarvest(origin, s);
        return refused ? FarmStatus.BLOCKED : FarmStatus.IDLE;
    }

    /** The blocks of {@code found} as they stood when planned; air and unloaded positions are left out. */
    private List<Target> targets(TendLogic.Harvest found) {
        List<Target> planned = new ArrayList<>(found.blocks().size());
        for (BlockPos pos : found.blocks()) {
            BlockState state = counted.seen(pos);
            if (state == null) {
                if (!counted.isLoaded(pos)) {
                    continue;
                }
                state = counted.state(pos);
            }
            if (!state.isAir()) {
                planned.add(new Target(pos, state.getBlock()));
            }
        }
        return planned;
    }

    /** Plans what is left of the harvest under way again; leaves no targets when nothing is. */
    private void replan() {
        counted.forget();
        TendLogic.Harvest rest = logic.resume(counted, harvest);
        targets.clear();
        if (rest != null) {
            harvest = rest;
            targets.addAll(targets(rest));
        }
        dirty = true;
    }

    /**
     * Replants the harvest's plot from its drops, stores the rest, and drops what does not fit at the
     * plot, or at the farm, {@code origin}, while the plot is not loaded.
     */
    private void finishHarvest(BlockPos origin, FarmSettings s) {
        TendLogic.Harvest done = harvest;
        harvest = null;
        resumeHarvest = false;
        targets.clear();
        dirty = true;
        if (done == null) {
            collected.clear();
            return;
        }
        List<ItemStack> drops = new ArrayList<>(collected);
        collected.clear();
        Ground ground = readGround(done.plot());
        ItemStack germling = takeGermling(drops, done);
        boolean planted = false;
        if (!germling.isEmpty()) {
            planted = replant(done.plot(), germling, ground, s);
            if (!planted) {
                drops.add(germling);
            }
        } else if (!done.germling().isEmpty()
                && inventory.count(done.germling()) > 0
                && replant(done.plot(), done.germling(), ground, s)) {
            inventory.take(done.germling(), 1);
            planted = true;
        }
        if (ground == null || done.germling().isEmpty()) {
            plots.forget(done.plot());
        } else {
            // A plant left standing (a refused break, a stalk's base) fails the replant: the Plot is not empty.
            plots.remember(done.plot(), done.germling(), !planted && isOpen(done.plot()));
        }
        List<ItemStack> spill = new ArrayList<>();
        for (ItemStack drop : drops) {
            ItemStack rest = inventory.insert(drop);
            if (!rest.isEmpty()) {
                spill.add(rest);
            }
        }
        if (!spill.isEmpty()) {
            counted.dropStacks(counted.isLoaded(done.plot()) ? done.plot() : origin, spill);
            tracker.record(FarmStatus.INVENTORY_FULL);
        }
    }

    /** Removes one germling that replants {@code harvest} from {@code drops}; empty if there is none. */
    private static ItemStack takeGermling(List<ItemStack> drops, TendLogic.Harvest harvest) {
        for (Iterator<ItemStack> it = drops.iterator(); it.hasNext(); ) {
            ItemStack drop = it.next();
            if (harvest.replants(drop)) {
                ItemStack one = drop.split(1);
                if (drop.isEmpty()) {
                    it.remove();
                }
                return one;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Plants {@code germling} on {@code plot}, first repairing its {@code ground} if it needs it;
     * {@code null} plants on the ground as it is. False, changing and spending nothing, when it
     * cannot; a repair that succeeds is charged even if the plant then fails.
     */
    private boolean replant(BlockPos plot, ItemStack germling, @Nullable Ground ground, FarmSettings s) {
        if (!counted.isLoaded(plot)) {
            return false;
        }
        BlockPos foot = ground != null ? ground.pos() : logic.support(counted, plot);
        if (foot != null && !counted.isLoaded(foot)) {
            return false;
        }
        BlockState plant = logic.plantState(germling, plot, counted);
        boolean repair = tills(ground);
        if (plant == null
                || energy.amount() < plantCost(ground, s)
                || !counted.canModify(plot)
                || repair && !counted.canModify(foot)) {
            return false;
        }
        BlockState current = counted.state(plot);
        if (!(current.isAir() || current.canBeReplaced())) {
            return false;
        }
        if (repair) {
            if (!counted.setBlock(foot, ground.ready(), Block.UPDATE_ALL)) {
                return false;
            }
            energy.consume(s.cost(FarmCost.TILL));
        }
        if (!counted.canSurvive(plant, plot) || !counted.setBlock(plot, plant, Block.UPDATE_ALL)) {
            return false;
        }
        energy.consume(s.cost(FarmCost.PLANT));
        return true;
    }

    private FarmStatus noPower(long needed) {
        powerNeeded = needed;
        return FarmStatus.NO_POWER;
    }

    // ==================== Picking ====================

    /**
     * Takes on the Picking Plot {@link TendLogic#pickPlot} names for {@code pos} while it lies in the
     * Reach's lowest levels, and forgets {@code pos} as one once it names another or none.
     */
    private void notePickPlot(BlockPos origin, BlockPos pos) {
        BlockPos plot = logic.pickPlot(counted, pos);
        if (plot != null && !inPlotLevels(origin, plot)) {
            plot = null;
        }
        if (!pos.equals(plot) && pickPlots.remove(pos)) {
            dirty = true;
        }
        if (plot != null && pickPlots.add(plot)) {
            dirty = true;
        }
    }

    /** Whether {@code plot} lies in the shared Reach: {@value #REACH_HEIGHT} above or below the farm at most. */
    private static boolean inPlotLevels(BlockPos origin, BlockPos plot) {
        int dx = plot.getX() - origin.getX();
        int dy = plot.getY() - origin.getY();
        int dz = plot.getZ() - origin.getZ();
        return Math.abs(dx) <= REACH && Math.abs(dz) <= REACH && Math.abs(dy) <= REACH_HEIGHT && !plot.equals(origin);
    }

    /**
     * Gives every Picking Plot that rolls one this tick a simulated random tick, which picks what it
     * bears when the bearing's chance comes up. A Plot found bearing nothing is forgotten.
     */
    private void ripen(RandomSource random, FarmSettings s) {
        if (pickPlots.size() == 0) {
            return;
        }
        int speed = counted.randomTickSpeed();
        for (BlockPos plot : pickPlots.snapshot()) {
            boolean ticked = pickOverride >= 0 || random.nextInt(RANDOM_TICK_BLOCKS) < speed;
            if (!ticked || !counted.isLoaded(plot)) {
                continue;
            }
            TendLogic.Bearing bearing = logic.bearing(counted, plot);
            if (bearing == null) {
                pickPlots.remove(plot);
                dirty = true;
                continue;
            }
            double chance = pickOverride >= 0 ? pickOverride : bearing.chance();
            if (random.nextDouble() < chance) {
                tracker.record(pick(plot, bearing, random, s));
            }
        }
    }

    /**
     * Picks one of {@code bearing}'s produce off the Plot at {@code plot}: charged its cost, into the
     * inventory, and dropped at the Plot when that is full.
     */
    private FarmStatus pick(BlockPos plot, TendLogic.Bearing bearing, RandomSource random, FarmSettings s) {
        long cost = s.cost(bearing.cost());
        if (energy.amount() < cost) {
            return noPower(cost);
        }
        if (!counted.canModify(plot)) {
            return FarmStatus.BLOCKED;
        }
        energy.consume(cost);
        dirty = true;
        ItemStack rest = inventory.insert(bearing.produce().copy());
        counted.rustle(bearing.sources().get(random.nextInt(bearing.sources().size())));
        if (!rest.isEmpty()) {
            counted.dropStacks(plot, List.of(rest));
            return FarmStatus.INVENTORY_FULL;
        }
        return FarmStatus.WORKING;
    }

    // ==================== Status ====================

    /**
     * Paused, else No power while the upkeep went unpaid or the buffer is short of the cheapest
     * action, ripe produce or not, else the tracked status.
     */
    private void updateStatus(MachineContext ctx, boolean paused, FarmSettings s) {
        if (paused) {
            status = FarmStatus.PAUSED;
        } else if (upkeepUnpaid || energy.amount() < cheapestAction(s)) {
            status = FarmStatus.NO_POWER;
        } else {
            status = tracker.status(reach.size(), this::stillApplies);
        }
        showLamp(ctx);
    }

    private long cheapestAction(FarmSettings s) {
        long cheapest = Long.MAX_VALUE;
        for (FarmCost cost : logic.actions()) {
            cheapest = Math.min(cheapest, s.cost(cost));
        }
        return cheapest;
    }

    private boolean stillApplies(FarmStatus problem) {
        return switch (problem) {
            case NO_POWER -> energy.amount() < powerNeeded;
            case INVENTORY_FULL -> !hasEmptySlot();
            case WAITING_FOR_SEEDS -> plots.emptyCount() > 0;
            default -> true;
        };
    }

    private boolean hasEmptySlot() {
        for (int slot = 0; slot < items.getContainerSize(); slot++) {
            if (items.getItem(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The block's {@code lamp} state: off once the buffer has stayed empty for {@link #LAMP_OFF_TICKS},
     * else the status colour, so a trickle-fed farm whose buffer touches 0 between payments stays lit.
     */
    private void showLamp(MachineContext ctx) {
        emptyTicks = energy.amount() > 0 ? 0 : Math.min(emptyTicks + 1, LAMP_OFF_TICKS);
        BlockState state = ctx.blockState();
        FarmLamp lamp = emptyTicks < LAMP_OFF_TICKS ? FarmLamp.forStatus(status) : FarmLamp.OFF;
        if (state.hasProperty(FarmMachineBlock.LAMP) && state.getValue(FarmMachineBlock.LAMP) != lamp) {
            // setBlock relights on any flags; no neighbour reacts to the lamp.
            ctx.setBlockState(state.setValue(FarmMachineBlock.LAMP, lamp), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void contributeHud(MachineHudModel hud) {
        if (status == FarmStatus.WAITING_FOR_SEEDS) {
            hud.line(HUD_STATUS, status.translationKey(), Integer.toString(plots.emptyCount()));
        } else {
            hud.line(HUD_STATUS, status.translationKey());
        }
        hud.line(HUD_REACH, HUD_REACH_SIZE, Integer.toString(REACH_SIZE));
        if (logic.picks()) {
            hud.line(HUD_TREES, HUD_TREES_COUNT, Integer.toString(pickPlots.size()));
        }
    }

    // ==================== Persistence ====================

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (origin != null) {
            tag.putLong("Origin", origin.asLong());
        }
        tag.putInt("Cursor", scanner != null ? scanner.cursor() : savedCursor);
        if (owner != null) {
            tag.putString("Owner", owner.toString());
        }
        plots.save(tag, "Plots", registries);
        pickPlots.save(tag, "PickPlots");
        if (harvest != null) {
            tag.put("Harvest", new SavedHarvest(harvest, collected).save(registries));
        }
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        origin = tag.contains("Origin") ? BlockPos.of(NbtCompat.getLong(tag, "Origin", 0L)) : null;
        savedCursor = NbtCompat.getInt(tag, "Cursor", 0);
        scanner = null;
        heldPower = 0;
        owner = parseUuid(NbtCompat.getString(tag, "Owner", ""));
        plots.load(tag, "Plots", registries);
        pickPlots.load(tag, "PickPlots");
        harvest = null;
        resumeHarvest = false;
        targets.clear();
        collected.clear();
        if (NbtCompat.hasCompound(tag, "Harvest")) {
            // Taken up under way, planned again from its plot on the next action.
            SavedHarvest saved = SavedHarvest.load(NbtCompat.getCompoundOrEmpty(tag, "Harvest"), registries);
            harvest = saved.harvest();
            collected.addAll(saved.drops());
            resumeHarvest = true;
        }
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

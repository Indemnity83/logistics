package com.logistics.forestry.farm;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.farmland.FarmlandHydration;
import com.logistics.core.lib.farmland.HydrationSource;
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
import com.logistics.forestry.logic.FellPlan;
import com.logistics.forestry.logic.FellPlanner;
import com.logistics.forestry.logic.TreeFeller;
import com.logistics.forestry.logic.TreeView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The farm's work loop over the plots of its {@link RingArea}, bounded every tick by a
 * {@link WorkBudget}: its tier's actions and checks per second.
 *
 * <p>Two lanes share the actions. The <b>cultivate lane</b> inspects the next plot from a
 * {@link PlotScanner}, for one check: while the plot's column holds anything that is not the farm's
 * own growth it clears one block of it ({@link ColumnClearer}); once the column is clear it asks
 * the {@link FarmLogic} what the plot needs and performs that one {@link PlotAction} at once, if the
 * farm can afford it. Nothing is queued: a plot with work is only inspected while an action is
 * there to spend on it. The <b>harvest lane</b> breaks the active multi-block harvest (a tree), one
 * block per action, top-down. While both have work they take turns by {@code harvest_share};
 * either takes every action while the other has none. A full pass that finds no work backs the
 * cultivate lane off ({@link PlotScanner#idleTicks()}).
 *
 * <p>Block reads are charged to the check bucket: a plot inspection costs at least one check, and
 * a tree's plan ({@link FellPlanner}) spends checks as it reads, over as many ticks as it takes.
 * Only one tree is planned and one harvested at a time; a tree found while another is being
 * harvested is passed over until a later pass. Every target is re-checked as it is broken: a block
 * that has changed since it was planned drops the rest of the plan, which is made again from the
 * harvest's plot.
 *
 * <p>Every per-plot question goes to the logic its {@link FarmPlan} assigns to that plot. The ring
 * surrounds the {@link FarmGeometry#COURTYARD}, the farm included, and reaches out to the outer
 * size of the farm's tier. The courtyard is never touched: every world write goes through a
 * {@link CourtyardGuard}, which refuses any in a courtyard column at any Y. The plots are one block
 * above the farm, so their soil, {@code plot.below()}, is level with the farm block.
 *
 * <p>The farm-wide gates live here, not in the logic. A redstone signal pauses everything. Every
 * action first checks everything it needs without side effects (loaded, claim protection, RF,
 * water, germlings, soil, room for drops), then changes the world, and charges only once the world
 * reports the change; a failed action costs nothing. A full output buffer stops every op that
 * breaks a block. Nothing touches an unloaded position, and a block entity or an unbreakable block
 * is never broken.
 *
 * <p>Once a harvest completes and its plot stands empty, the soil under every ring plot it broke a
 * block on becomes its logic's {@link FarmLogic#soilAfterHarvest}. Drops and the loose items swept
 * from the ring afterwards go to the output slots, and what does not fit waits in an overflow list.
 *
 * <p>A placed farm starts in {@link FarmState#SURVEY}: it changes nothing and spends nothing, but
 * inspects the ring at the same pace and back-off, through the same check budget, recording in a
 * {@link FarmSurvey} what working each plot would clear: its column, and the soil its logic would
 * replace. {@link #start} makes it {@link FarmState#ACTIVE}.
 *
 * <p>A working farm with a water tank irrigates its ring: while it is loaded, vanilla farmland on a
 * ring plot's soil cell that finds no water nearby asks the farm instead ({@link FarmlandHydration}),
 * once per random tick, and the farm keeps it wet for {@code water_per_irrigation_mb} scaled by the
 * {@link Hydration climate}; see {@link #irrigate}. Tilling, planting and harvesting never need water.
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
    public static final String HUD_PLOTS = "jade.logistics.farm.plots";
    public static final String HUD_PLOTS_VALUE = "jade.logistics.farm.plots_value";

    /** Loose items are swept from the soil layer to this far above the plots. */
    private static final int SWEEP_HEIGHT = 32;

    /** Reads a tree's seed probe needs: the plot and the blocks above it it may grow from. */
    private static final int SEED_PROBE_READS = TreeFeller.SEED_SEARCH_HEIGHT + 1;

    /** Reads a visit keeps back from tree planning for the rest of the logic's decision. */
    private static final int DECISION_READS = 16;

    private record HarvestTarget(BlockPos pos, Block block) {}

    /** What one tick works with. */
    private record Tick(FarmWorldAccess world, FarmSettings settings, RingArea area, FarmPlan plan) {}

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
    private final BlockedPlots blockedPlots = new BlockedPlots();
    private final ArrayDeque<HarvestTarget> harvest = new ArrayDeque<>();
    private final WorkBudget budget = new WorkBudget();
    private final CountingWorld counted = new CountingWorld();
    // The current tick's guarded world as a tree view, which a planner reads through across ticks.
    private TreeView trees = pos -> TreeView.Kind.UNLOADED;
    private final TreeView plannerView = pos -> trees.kind(pos);

    @Nullable private PlotScanner scanner;
    private int scannerBase;
    private int scannerMax;
    private int savedCursor;
    private int stepsInPass;
    private int cooldown;
    // The plot the cultivate lane waits on until the buffer holds heldPower RF; -1 when not waiting.
    private int heldIndex = -1;
    private long heldPower;
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
    // The one tree being planned, and the plot inspection or harvest re-plan waiting on it.
    @Nullable private FellPlanner planner;
    private int plannerIndex = -1;
    private boolean plannerReplans;
    // Set while a plot is inspected: its tree plan is still in progress, or must wait for the harvest.
    private boolean visitPending;
    private boolean visitDeferred;
    private boolean inVisit;
    // Set while the plot of an interrupted harvest is asked again: its canopy may be gone already.
    private boolean replanning;
    private long visitMark;
    private FarmStatus status = FarmStatus.IDLE;
    private FarmState state = FarmState.SURVEY;
    // The survey pass in progress, and the last one completed.
    private FarmSurvey surveyPass = new FarmSurvey();
    @Nullable private FarmSurvey surveyed;
    // What the preview shows while working: the ring after Show area, or the new lanes after an upgrade.
    private FarmSurvey previewSurvey = FarmSurvey.EMPTY;
    // Shown by the toggle until toggled off; a placed or loaded surveying farm starts shown. Never saved.
    private boolean previewToggled = true;
    // The end of an upgrade's flash, which shows whatever the toggle says.
    private long previewUntil;
    // The outer side of the ring the last tick worked.
    private int workedOuter;
    // Set while an update tag is written; read back from one on the client.
    @Nullable private FarmPreview pendingPreview;
    private FarmPreview syncedPreview = FarmPreview.NONE;
    // What the last NO_POWER / NO_GERMLINGS / NO_SOIL was short of, so the status clears as soon as it is there.
    private long powerNeeded;
    private ItemStack germlingNeeded = ItemStack.EMPTY;
    private int germlingCountNeeded;
    private FarmLogic germlingLogic;
    private FarmLogic soilLogic;
    private boolean dirty;
    // The ring irrigated as of the last tick: working, with a tank and no redstone signal; else null.
    @Nullable private RingArea irrigated;
    private int irrigationMb;
    @Nullable private FarmlandHydration.Registration irrigation;
    @Nullable private ServerLevel irrigationLevel;
    @Nullable private RingArea irrigationArea;
    private long harvestActions;
    private long cultivateActions;

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

    public FarmState state() {
        return state;
    }

    /** Starts working the ring and hides the preview. Returns false, changing nothing, unless the farm is surveying. */
    public boolean start() {
        if (state != FarmState.SURVEY) {
            return false;
        }
        state = FarmState.ACTIVE;
        previewToggled = false;
        previewUntil = 0;
        cooldown = 0;
        dirty = true;
        return true;
    }

    /**
     * Hides the area preview if it shows at {@code now}, cutting an upgrade's flash short; otherwise
     * shows it until toggled off, surveying a working farm's ring as it stands. A surveying farm
     * keeps surveying either way.
     */
    public boolean toggleArea(MachineContext ctx, long now) {
        if (previewShown(now)) {
            previewToggled = false;
            previewUntil = 0;
        } else {
            if (state == FarmState.ACTIVE) {
                previewSurvey = surveyNow(ctx, 0);
            }
            previewToggled = true;
        }
        return true;
    }

    /**
     * After the tier has widened the ring: surveys the lanes it added, all at once, shows the preview
     * of them from {@code now}, and lets the scan reach them straight away.
     */
    public FarmSurvey expanded(MachineContext ctx, long now) {
        RingArea area = area(ctx, settings.get());
        int from = area.plotCount();
        if (workedOuter > area.inner() && workedOuter < area.outer()) {
            from = new RingArea(area.origin(), area.inner(), workedOuter).plotCount();
        }
        previewSurvey = surveyNow(ctx, from);
        previewUntil = now + FarmPreview.SHOW_TICKS;
        cooldown = 0;
        dirty = true;
        return previewSurvey;
    }

    /**
     * What the preview reports: while surveying, the last full pass, or the pass so far before one
     * completes; while working, what Show area or the last upgrade found.
     */
    public FarmSurvey survey() {
        if (state == FarmState.SURVEY) {
            return surveyed != null ? surveyed : surveyPass;
        }
        return previewSurvey;
    }

    /** Game time an upgrade's flash of the preview shows until. */
    long previewUntil() {
        return previewUntil;
    }

    /** Whether the preview shows at {@code now}: while toggled on, or during an upgrade's flash. */
    public boolean previewShown(long now) {
        return previewToggled || now < previewUntil;
    }

    /** The preview as the last update tag carried it; on the client, what to draw. */
    public FarmPreview syncedPreview() {
        return syncedPreview;
    }

    /** Writes an update tag through {@code save}, carrying the preview as it stands at {@code now}. */
    public <T> T withPreview(MachineContext ctx, long now, Supplier<T> save) {
        RingArea area = area(ctx, settings.get());
        FarmSurvey found = survey();
        int height = clearanceHeight(logic);
        FarmPreview preview =
                new FarmPreview(state, previewToggled, previewUntil, area.inner(), area.outer(), height, found);
        if (!preview.visible(now)) {
            // Positions only while they are drawn.
            FarmSurvey counts =
                    FarmSurvey.of(found.natural(), found.flagged(), found.protectedCount(), List.of(), List.of());
            preview = new FarmPreview(state, previewToggled, previewUntil, area.inner(), area.outer(), height, counts);
        }
        pendingPreview = preview;
        try {
            return save.get();
        } finally {
            pendingPreview = null;
        }
    }

    /** Side of the square whose ring is worked: a 15 means the ring reaches 15×15. */
    public int areaSize() {
        return outerOverride > 0 ? outerOverride : settings.get().profile(tier.get()).outerSize();
    }

    /** Plots in the worked ring. */
    public int plotsTotal() {
        int outer = areaSize();
        int inner = outerOverride > 0 ? innerOverride : FarmGeometry.COURTYARD;
        return outer * outer - inner * inner;
    }

    /** Ring plots that came back blocked on their most recent visit; see {@link BlockedPlots}. */
    public int plotsBlocked() {
        return Math.min(blockedPlots.count(), plotsTotal());
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

    /** Blocks still to break in the active harvest. */
    int harvestRemaining() {
        return harvest.size();
    }

    boolean isPlanning() {
        return planner != null && !planner.isDone();
    }

    /** Actions the harvest lane has taken since this component was built. */
    long harvestActions() {
        return harvestActions;
    }

    /** Actions the cultivate lane has taken since this component was built. */
    long cultivateActions() {
        return cultivateActions;
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
        if (state == FarmState.ACTIVE && output.drainOverflow()) {
            dirty = true;
        }
        RingArea area = area(ctx, s);
        workedOuter = area.outer();
        counted.attach(opened);
        FarmWorldAccess world = new CourtyardGuard(counted, area.keepOut());
        trees = world.treeView();
        FarmPlan plan = plan(area);
        ensureScanner(area, s);

        boolean paused = world.hasNeighborSignal(ctx.pos());
        updateIrrigation(ctx, area, paused, s);
        if (!paused) {
            FarmTierProfile profile = s.profile(tier.get());
            budget.refill(profile.actionsPerSecond(), profile.checksPerSecond(), s.readsPerCheck());
            Tick tick = new Tick(world, s, area, plan);
            if (state == FarmState.ACTIVE) {
                work(tick);
            } else {
                survey(ctx, tick);
            }
        }
        updateStatus(ctx, s, area, paused);

        if (dirty) {
            dirty = false;
            ctx.markChanged();
        }
    }

    /**
     * Spends this tick's budget. Each round, an unfinished tree plan reads on first; then, while an
     * action is left, the lane whose turn it is acts, or the other one when it cannot.
     */
    private void work(Tick t) {
        boolean harvestOpen = true;
        boolean cultivateOpen = true;
        if (cooldown > 0) {
            cooldown--;
            cultivateOpen = planner != null || replanHarvest;
        }
        double share = t.settings().harvestShare();
        // A lane that had the turn but could not use it, while it still had work: the other lane's
        // action then counts against the turns, so the shares hold.
        boolean turnLost = false;
        while (true) {
            boolean harvestReady = harvestOpen && !harvest.isEmpty();
            boolean cultivateReady = cultivateOpen;
            if (!harvestReady && !cultivateReady) {
                return;
            }
            if (cultivateReady && isPlanning()) {
                advancePlanner();
                if (isPlanning()) {
                    cultivateOpen = false;
                    continue;
                }
            }
            if (!budget.hasAction()) {
                return;
            }
            boolean contended = harvestReady && cultivateReady;
            WorkBudget.Lane lane = contended
                    ? budget.turn(share)
                    : harvestReady ? WorkBudget.Lane.HARVEST : WorkBudget.Lane.CULTIVATE;
            boolean acted = lane == WorkBudget.Lane.HARVEST ? harvestStep(t) : cultivateStep(t);
            if (acted) {
                budget.spendAction();
                if (lane == WorkBudget.Lane.HARVEST) {
                    harvestActions++;
                } else {
                    cultivateActions++;
                }
                if (contended || turnLost) {
                    budget.took(lane, share);
                }
                turnLost = false;
            } else {
                // A cultivate lane whose pass came up empty is idle, not short of its turn.
                turnLost = contended && (lane == WorkBudget.Lane.HARVEST || cooldown <= 0);
                if (lane == WorkBudget.Lane.HARVEST) {
                    harvestOpen = false;
                } else {
                    cultivateOpen = false;
                }
            }
        }
    }

    private void ensureScanner(RingArea area, FarmSettings s) {
        int plotCount = area.plotCount();
        blockedPlots.resize(plotCount);
        if (scanner == null || scannerBase != s.idleBackoffBaseTicks() || scannerMax != s.idleBackoffMaxTicks()) {
            int cursor = scanner != null ? scanner.cursor() : savedCursor;
            scannerBase = s.idleBackoffBaseTicks();
            scannerMax = s.idleBackoffMaxTicks();
            scanner = PlotScanner.restore(plotCount, cursor, scannerBase, scannerMax);
            stepsInPass = 0;
            surveyPass = new FarmSurvey();
        } else if (scanner.plotCount() != plotCount) {
            scanner.resize(plotCount);
            stepsInPass = 0;
            surveyPass = new FarmSurvey();
        }
        if (heldIndex >= plotCount) {
            heldIndex = -1;
        }
        if (planner != null
                && !plannerReplans
                && (plannerIndex < 0 || plannerIndex >= plotCount || !area.plot(plannerIndex).equals(planner.plot()))) {
            dropPlanner(); // the ring changed under it
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

    // ==================== Survey ====================

    /**
     * Inspects plots while the checks last, as the cultivate lane does, and records what working
     * each would clear. A completed pass becomes the survey and backs off like an idle one.
     */
    private void survey(MachineContext ctx, Tick t) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        int readsPerCheck = t.settings().readsPerCheck();
        while (budget.reads() >= readsPerCheck) {
            int index = scanner.next();
            stepsInPass++;
            dirty = true;
            counted.forget();
            long mark = counted.reads();
            boolean blocked = surveyPlot(t.world(), t.area(), t.plan(), index, surveyPass);
            budget.spendReads(Math.max(readsPerCheck, counted.reads() - mark));
            blockedPlots.record(index, blocked ? FarmStatus.BLOCKED : FarmStatus.IDLE);
            if (stepsInPass >= scanner.plotCount()) {
                stepsInPass = 0;
                cooldown += scanner.idleTicks();
                FarmSurvey done = surveyPass;
                surveyPass = new FarmSurvey();
                if (!done.equals(surveyed)) {
                    surveyed = done;
                    ctx.sync();
                }
                return;
            }
        }
    }

    /** Surveys the ring's plots from {@code from} to its end now, all at once, outside the budget. */
    private FarmSurvey surveyNow(MachineContext ctx, int from) {
        FarmWorldAccess opened = worlds.open(ctx, owner);
        if (opened == null) {
            return FarmSurvey.EMPTY;
        }
        RingArea area = area(ctx, settings.get());
        FarmWorldAccess world = new CourtyardGuard(opened, area.keepOut());
        FarmPlan plan = plan(area);
        FarmSurvey found = new FarmSurvey();
        for (int index = Math.max(0, from); index < area.plotCount(); index++) {
            surveyPlot(world, area, plan, index, found);
        }
        return found;
    }

    /**
     * Records into {@code into} what working plot {@code index} would clear, changing nothing: its
     * column from the top down to the first block it could never clear, then the soil its logic
     * would replace once the column is clear. Returns whether such a block blocks the plot.
     */
    private boolean surveyPlot(FarmWorldAccess world, RingArea area, FarmPlan plan, int index, FarmSurvey into) {
        BlockPos plot = area.plot(index);
        if (!world.isLoaded(plot)) {
            return false;
        }
        FarmLogic plotLogic = plan.logicFor(index);
        Set<BlockPos> cleared = new HashSet<>();
        for (int dy = clearanceHeight(plotLogic) - 1; dy >= 0; dy--) {
            BlockPos pos = plot.above(dy);
            BlockState block = world.state(pos);
            if (block.isAir() || plotLogic.isOwnGrowth(block)) {
                continue;
            }
            ClearClassifier.Kind kind = classify(world, pos, block);
            if (kind == ClearClassifier.Kind.PROTECTED) {
                into.skipped(pos);
                return true;
            }
            into.cleared(pos, kind);
            cleared.add(pos);
        }
        PlotAction action =
                plotLogic.decide(new SurveyWorld(world, cleared), plan.regionFor(index), plot, inputsFor(plotLogic));
        BlockPos soilPos = plot.below();
        if (action instanceof PlotAction.PlaceSoil soil && soil.pos().equals(soilPos) && world.isLoaded(soilPos)) {
            BlockState ground = world.state(soilPos);
            if (!ground.isAir()) {
                ClearClassifier.Kind kind = classify(world, soilPos, ground);
                if (kind == ClearClassifier.Kind.PROTECTED) {
                    into.skipped(soilPos);
                    return true;
                }
                into.cleared(soilPos, kind);
            }
        }
        return false;
    }

    /** {@link ClearClassifier#classify} where the block stands; one claim protection refuses is protected too. */
    private static ClearClassifier.Kind classify(FarmWorldAccess world, BlockPos pos, BlockState state) {
        if (!world.canModify(pos)) {
            return ClearClassifier.Kind.PROTECTED;
        }
        return ClearClassifier.classify(
                state, world.destroySpeed(pos), world.naturalClearable(state), world.neverClear(state));
    }

    // ==================== Cultivate lane ====================

    /**
     * Inspects plots until one has work the farm can do, does it, and returns true. Returns false,
     * having changed nothing more, once the checks run out, a tree plan has to wait for more reads,
     * or a pass found nothing to do.
     *
     * <p>A plot whose work the buffer cannot pay for holds the lane: the scan stays on it, reading
     * nothing, until the buffer holds that much RF, then inspects it again. The wait is not a step
     * of the pass, so it never backs the lane off.
     */
    private boolean cultivateStep(Tick t) {
        int readsPerCheck = t.settings().readsPerCheck();
        // Inspections leave the harvest lane the reads for its next break.
        while (budget.reads() >= readsPerCheck + harvestReserve()) {
            if (heldIndex >= 0 && energy.amount() < heldPower) {
                return false;
            }
            if (isPlanning()) {
                return false;
            }
            if (planner == null && replanHarvest && !output.isFull()) {
                FarmStatus outcome = replan(t);
                if (visitPending) {
                    return false;
                }
                if (outcome == FarmStatus.WORKING) {
                    return true;
                }
                continue;
            }
            if (planner != null) {
                // The tree plan is done: inspect again what was waiting on it.
                FarmStatus outcome = plannerReplans ? replan(t) : revisit(t, plannerIndex);
                if (outcome == FarmStatus.WORKING) {
                    return true;
                }
                continue;
            }
            if (cooldown > 0) {
                return false;
            }
            int index = heldIndex >= 0 ? heldIndex : scanner.cursor();
            boolean scanned = index == scanner.cursor();
            heldIndex = -1;
            tracker.step();
            dirty = true;
            FarmStatus outcome = visit(t, index);
            boolean worked = outcome == FarmStatus.WORKING;
            if (outcome == FarmStatus.NO_POWER && !visitPending) {
                heldIndex = index;
                heldPower = powerNeeded;
                return false;
            }
            if (!scanned) {
                // A plot a finished tree plan was waiting on, out of scan order.
                if (worked) {
                    scanner.markWorked();
                    return true;
                }
                continue;
            }
            scanner.next();
            stepsInPass++;
            if (worked) {
                scanner.markWorked();
            }
            if (stepsInPass >= scanner.plotCount()) {
                stepsInPass = 0;
                cooldown += scanner.idleTicks();
            }
            if (visitPending) {
                return false;
            }
            if (worked) {
                return true;
            }
        }
        return false;
    }

    /**
     * Re-inspects the plot a finished tree plan was made for; the plan is used up either way. Work
     * the buffer cannot pay for holds the lane on that plot.
     */
    private FarmStatus revisit(Tick t, int index) {
        FarmStatus outcome = visit(t, index);
        if (outcome == FarmStatus.WORKING) {
            scanner.markWorked();
        } else if (outcome == FarmStatus.NO_POWER && !visitPending) {
            heldIndex = index;
            heldPower = powerNeeded;
        }
        return outcome;
    }

    /** The plot the cultivate lane is waiting on for RF, or -1. */
    int heldPlot() {
        return heldIndex;
    }

    /** Where the scan stands: the next plot it inspects. */
    int scanCursor() {
        return scanner != null ? scanner.cursor() : savedCursor;
    }

    /** Inspects plot {@code index} and does what it needs; records the outcome unless it has to wait. */
    private FarmStatus visit(Tick t, int index) {
        beginVisit();
        BlockPos plot = t.area().plot(index);
        FarmStatus outcome =
                t.world().isLoaded(plot) ? workPlot(t, index, plot) : FarmStatus.IDLE;
        endVisit(t.settings());
        if (visitPending) {
            plannerIndex = index;
            plannerReplans = false;
            return FarmStatus.IDLE;
        }
        if (visitDeferred) {
            return FarmStatus.IDLE;
        }
        tracker.record(outcome);
        blockedPlots.record(index, outcome);
        return outcome;
    }

    private void beginVisit() {
        visitPending = false;
        visitDeferred = false;
        inVisit = true;
        visitMark = counted.reads();
        if (planner == null) {
            counted.forget();
        }
    }

    /** Charges the visit's reads, at least one check, and lets go of a plan it has used. */
    private void endVisit(FarmSettings s) {
        inVisit = false;
        budget.spendReads(Math.max(s.readsPerCheck(), counted.reads() - visitMark));
        if (!visitPending && planner != null && planner.isDone()) {
            dropPlanner();
        }
    }

    /** Reads left to the visit in progress. */
    private int visitReads() {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, budget.reads() - (counted.reads() - visitMark)));
    }

    private void advancePlanner() {
        long mark = counted.reads();
        planner.step((int) Math.min(Integer.MAX_VALUE, budget.reads()));
        chargeReads(mark);
    }

    /** Charges the reads made since {@code mark}; a visit charges its own reads when it ends. */
    private void chargeReads(long mark) {
        if (!inVisit) {
            budget.spendReads(counted.reads() - mark);
        }
    }

    private void dropPlanner() {
        planner = null;
        plannerIndex = -1;
        plannerReplans = false;
    }

    /**
     * The tree at {@code plot}, for the logic deciding in the visit in progress. A finished plan
     * for it is handed over; otherwise planning starts with the visit's reads. A plan that needs
     * more, or a tree found while another is being harvested, marks the visit to be discarded.
     */
    private FellPlan fellPlan(BlockPos plot, TreeFeller.Limits limits) {
        if (planner != null && planner.isDone() && planner.plot().equals(plot) && planner.limits().equals(limits)) {
            return planner.result();
        }
        if (planner != null) {
            // One plan at a time.
            visitDeferred = true;
            return FellPlan.skip(FellPlan.Reason.NO_TREE);
        }
        boolean harvesting = !harvest.isEmpty();
        FellPlanner started = new FellPlanner(plannerView, plot, limits, !replanning);
        int reads = Math.max(0, visitReads() - DECISION_READS);
        FellPlan plan = started.step(harvesting ? Math.min(reads, SEED_PROBE_READS) : reads);
        if (plan != null && (!harvesting || !plan.isFound())) {
            return plan;
        }
        if (harvesting) {
            visitDeferred = true;
        } else {
            planner = started;
            visitPending = true;
        }
        return FellPlan.skip(FellPlan.Reason.NO_TREE);
    }

    /** Clears one block from {@code plot}'s column, or, once it is clear, does what its logic asks. */
    private FarmStatus workPlot(Tick t, int index, BlockPos plot) {
        FarmWorldAccess world = t.world();
        FarmLogic plotLogic = t.plan().logicFor(index);
        ColumnClearer.ColumnView column = pos -> {
            BlockState state = world.state(pos);
            return state.isAir()
                    ? ColumnClearer.Kind.EMPTY
                    : ColumnClearer.classify(
                            state, world.destroySpeed(pos), plotLogic.isOwnGrowth(state), world.neverClear(state));
        };
        return switch (ColumnClearer.next(column, plot, clearanceHeight(plotLogic))) {
            case ColumnClearer.Clearing.Clear clear -> {
                PlotAction action =
                        plotLogic.decide(new PlanningWorld(world), t.plan().regionFor(index), plot, inputsFor(plotLogic));
                yield visitPending || visitDeferred ? FarmStatus.IDLE : execute(t, plotLogic, plot, action);
            }
            case ColumnClearer.Clearing.Break breakBlock -> clearBlock(t, breakBlock.pos());
            case ColumnClearer.Clearing.Drain drain -> drain(t, drain.pos());
            case ColumnClearer.Clearing.Blocked blocked -> FarmStatus.BLOCKED;
        };
    }

    private FarmStatus execute(Tick t, FarmLogic plotLogic, BlockPos plot, PlotAction action) {
        return switch (action) {
            case PlotAction.Harvest harvestAction -> startHarvest(t, plot, harvestAction.blocks(), false);
            case PlotAction.PlaceSoil placeSoil -> placeSoil(t, plotLogic, placeSoil.pos());
            case PlotAction.Till till -> till(t, till.pos(), till.result());
            case PlotAction.Plant plant -> plant(t, plotLogic, plant.positions(), plant.germling());
            case PlotAction.None none -> FarmStatus.IDLE;
            case PlotAction.Blocked blocked ->
                blocked.reason() == PlotAction.Reason.TOO_DARK ? FarmStatus.TOO_DARK : FarmStatus.BLOCKED;
        };
    }

    private FarmStatus clearBlock(Tick t, BlockPos pos) {
        long cost = t.settings().cost(FarmCost.CLEAR);
        if (output.isFull()) {
            return FarmStatus.OUTPUT_FULL;
        }
        if (energy.amount() < cost) {
            return noPower(cost);
        }
        if (!t.world().canModify(pos)) {
            return FarmStatus.BLOCKED;
        }
        List<ItemStack> drops = t.world().breakBlock(pos);
        if (drops == null) {
            return FarmStatus.BLOCKED;
        }
        drops.forEach(output::accept);
        energy.consume(cost);
        return FarmStatus.WORKING;
    }

    private FarmStatus drain(Tick t, BlockPos pos) {
        long cost = t.settings().cost(FarmCost.CLEAR);
        if (energy.amount() < cost) {
            return noPower(cost);
        }
        if (!t.world().canModify(pos) || !t.world().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) {
            return FarmStatus.BLOCKED;
        }
        energy.consume(cost);
        return FarmStatus.WORKING;
    }

    /**
     * Lays one soil item at {@code pos}. Ground in the way is broken first, as a clear: its drops
     * kept and its RF charged. The soil and its RF are only spent once it is laid, so a placement
     * refused after the break leaves the cell empty for a later pass.
     */
    private FarmStatus placeSoil(Tick t, FarmLogic plotLogic, BlockPos pos) {
        FarmWorldAccess world = t.world();
        if (!world.isLoaded(pos)) {
            return FarmStatus.IDLE;
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
        if (!world.canModify(pos)
                || current.hasBlockEntity()
                || world.destroySpeed(pos) < 0
                || world.neverClear(current)) {
            return FarmStatus.BLOCKED;
        }
        // Air and fluids are simply overwritten; anything else is broken first.
        boolean overwrite = current.isAir() || current.getBlock() instanceof LiquidBlock;
        long soilCost = t.settings().cost(FarmCost.SOIL);
        long breakCost = overwrite ? 0 : t.settings().cost(FarmCost.CLEAR);
        if (energy.amount() < soilCost + breakCost) {
            return noPower(soilCost + breakCost);
        }
        if (!overwrite) {
            if (output.isFull()) {
                return FarmStatus.OUTPUT_FULL;
            }
            List<ItemStack> drops = world.breakBlock(pos);
            if (drops == null) {
                return FarmStatus.BLOCKED;
            }
            drops.forEach(output::accept);
            energy.consume(breakCost);
        }
        if (!world.setBlock(pos, soil, Block.UPDATE_ALL)) {
            return overwrite ? FarmStatus.BLOCKED : FarmStatus.WORKING;
        }
        items.removeItem(slot, 1);
        energy.consume(soilCost);
        tracker.clear(FarmStatus.NO_SOIL);
        return FarmStatus.WORKING;
    }

    /** Tills {@code pos} into {@code result}, for RF only: irrigation wets it later. */
    private FarmStatus till(Tick t, BlockPos pos, BlockState result) {
        FarmWorldAccess world = t.world();
        if (!world.isLoaded(pos) || world.state(pos) == result) {
            return FarmStatus.IDLE;
        }
        long cost = t.settings().cost(FarmCost.TILL);
        if (energy.amount() < cost) {
            return noPower(cost);
        }
        if (!world.canModify(pos) || !world.setBlock(pos, result, Block.UPDATE_ALL)) {
            return FarmStatus.BLOCKED;
        }
        energy.consume(cost);
        return FarmStatus.WORKING;
    }

    /**
     * Plants one {@code germling} in every cell, all or none: every cell is checked first, and if a
     * cell still refuses its plant, the cells already planted are cleared again and nothing is spent.
     */
    private FarmStatus plant(Tick t, FarmLogic plotLogic, List<BlockPos> positions, ItemStack germling) {
        FarmWorldAccess world = t.world();
        if (positions.isEmpty()) {
            return FarmStatus.IDLE;
        }
        for (BlockPos pos : positions) {
            if (!world.isLoaded(pos)) {
                return FarmStatus.IDLE;
            }
        }
        long cost = t.settings().cost(FarmCost.PLANT) * positions.size();
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
            if (plant == null || !world.canModify(pos)) {
                return FarmStatus.BLOCKED;
            }
            BlockState current = world.state(pos);
            if (!(current.isAir() || current.canBeReplaced()) || !world.canSurvive(plant, pos)) {
                return FarmStatus.BLOCKED;
            }
            plants.add(plant);
        }
        for (int i = 0; i < positions.size(); i++) {
            if (!world.setBlock(positions.get(i), plants.get(i), Block.UPDATE_ALL)) {
                for (int j = 0; j < i; j++) {
                    world.setBlock(positions.get(j), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                return FarmStatus.BLOCKED;
            }
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

    // ==================== Harvest lane ====================

    /**
     * Takes {@code blocks} as the farm's harvest and breaks the first of them as this action; the
     * harvest lane breaks the rest, and holds the harvest while it stalls on power or output room.
     * A harvest asked for while another is under way waits for a later pass. {@code resumed} keeps
     * the plots the interrupted harvest already broke.
     */
    private FarmStatus startHarvest(Tick t, BlockPos plot, List<BlockPos> blocks, boolean resumed) {
        if (blocks.isEmpty()) {
            return FarmStatus.IDLE;
        }
        if (!harvest.isEmpty()) {
            visitDeferred = true;
            return FarmStatus.IDLE;
        }
        if (output.isFull()) {
            return FarmStatus.OUTPUT_FULL;
        }
        List<HarvestTarget> targets = targets(t.world(), blocks);
        if (targets.isEmpty()) {
            return FarmStatus.IDLE;
        }
        harvest.addAll(targets);
        harvestBase = plot.immutable();
        if (!resumed) {
            harvestedPlots.clear();
        }
        dirty = true;
        return breakNext(t);
    }

    /** The blocks of a harvest as they stood when planned; air and unloaded positions are left out. */
    private List<HarvestTarget> targets(FarmWorldAccess world, List<BlockPos> blocks) {
        List<HarvestTarget> targets = new ArrayList<>(blocks.size());
        for (BlockPos pos : blocks) {
            BlockState state = counted.seen(pos);
            if (state == null) {
                if (!world.isLoaded(pos)) {
                    continue;
                }
                state = world.state(pos);
            }
            if (!state.isAir()) {
                targets.add(new HarvestTarget(pos, state.getBlock()));
            }
        }
        return targets;
    }

    /** One harvest-lane action: breaks the next block of the harvest; false if none was broken. */
    private boolean harvestStep(Tick t) {
        FarmStatus outcome = breakNext(t);
        tracker.record(outcome);
        return outcome == FarmStatus.WORKING;
    }

    /**
     * Breaks the next block of the harvest. Before breaking, the target must still be loaded, still
     * the block that was planned, and modifiable. One already gone is passed over and one claim
     * protection refuses is left standing; any other change drops the rest of the plan, which is
     * made again from the harvest's plot.
     */
    private FarmStatus breakNext(Tick t) {
        FarmWorldAccess world = t.world();
        boolean refused = false;
        while (!harvest.isEmpty()) {
            if (output.isFull()) {
                return FarmStatus.OUTPUT_FULL;
            }
            if ((inVisit ? visitReads() : budget.reads()) < nextBreakReads()) {
                return FarmStatus.IDLE;
            }
            long mark = counted.reads();
            HarvestTarget target = harvest.peek();
            BlockPos pos = target.pos();
            BlockState state = world.isLoaded(pos) ? world.state(pos) : null;
            FarmStatus outcome = breakTarget(t, target, state);
            chargeReads(mark);
            if (outcome == FarmStatus.WORKING) {
                tracker.clear(FarmStatus.OUTPUT_FULL);
                if (harvest.isEmpty()) {
                    finishHarvest(t);
                }
                return outcome;
            }
            if (outcome == FarmStatus.BLOCKED) {
                refused = true;
            } else if (outcome != null) {
                return outcome;
            }
            if (harvest.isEmpty() && !replanHarvest) {
                finishHarvest(t);
            }
        }
        return refused ? FarmStatus.BLOCKED : FarmStatus.IDLE;
    }

    /**
     * Breaks {@code target}, standing as {@code state} (null when unloaded). Returns WORKING when
     * broken, BLOCKED when refused and passed over, null when gone and passed over or when the plan
     * was dropped, or the problem that stalls the harvest with the target still in place.
     */
    @Nullable
    private FarmStatus breakTarget(Tick t, HarvestTarget target, @Nullable BlockState state) {
        FarmWorldAccess world = t.world();
        if (state != null && state.isAir()) {
            harvest.poll();
            return null;
        }
        if (state == null || state.getBlock() != target.block()) {
            // Changed since planned: never break on a stale plan.
            harvest.clear();
            replanHarvest = true;
            dirty = true;
            return null;
        }
        if (!world.canModify(target.pos())) {
            harvest.poll();
            return FarmStatus.BLOCKED;
        }
        long cost = t.settings().cost(harvestLogic(t, harvestBase).harvestCost(state));
        if (energy.amount() < cost) {
            return noPower(cost);
        }
        List<ItemStack> drops = world.breakBlock(target.pos());
        harvest.poll();
        dirty = true;
        if (drops == null) {
            return FarmStatus.BLOCKED;
        }
        energy.consume(cost);
        drops.forEach(output::accept);
        if (harvestBase != null
                && target.pos().getY() == harvestBase.getY()
                && t.area().contains(target.pos())) {
            harvestedPlots.add(target.pos().immutable());
        }
        return FarmStatus.WORKING;
    }

    /** Reads kept back for the harvest lane while a harvest is under way. */
    private int harvestReserve() {
        return harvest.isEmpty() ? 0 : nextBreakReads();
    }

    /**
     * Reads the next break may make: the target's check, and for the last one the soil restored
     * under every plot the harvest broke a block on.
     */
    private int nextBreakReads() {
        return harvest.size() > 1 ? 1 : 1 + 2 * (harvestedPlots.size() + 1);
    }

    /** The logic that pays for the harvest standing on {@code plot}. */
    private FarmLogic harvestLogic(Tick t, @Nullable BlockPos plot) {
        int index = plot != null ? t.area().indexOf(plot) : -1;
        return index >= 0 ? t.plan().logicFor(index) : logic;
    }

    /** Restores the harvest's soil, sweeps the ring, and lets cultivation carry on straight away. */
    private void finishHarvest(Tick t) {
        long mark = counted.reads();
        completeHarvest(t);
        chargeReads(mark);
        for (ItemStack stack : t.world().collectItems(sweepBox(t.area()), t.area()::contains)) {
            output.accept(stack);
        }
        scanner.markWorked();
        cooldown = 0;
    }

    /** Restores the soil under the harvest's plot and every plot it broke a block on. */
    private void completeHarvest(Tick t) {
        if (harvestBase != null) {
            harvestedPlots.add(harvestBase);
        }
        for (BlockPos plot : harvestedPlots) {
            restoreSoil(t, plot);
        }
        harvestedPlots.clear();
        harvestBase = null;
        replanHarvest = false;
        dirty = true;
    }

    /**
     * Turns the soil under {@code plot}, emptied by a completed harvest, into its logic's
     * after-harvest soil. A position that is not a plot of the ring is left alone.
     */
    private void restoreSoil(Tick t, BlockPos plot) {
        FarmWorldAccess world = t.world();
        int index = t.area().indexOf(plot);
        BlockPos soilPos = plot.below();
        if (index < 0 || !world.isLoaded(plot) || !world.isLoaded(soilPos) || !world.state(plot).isAir()) {
            return;
        }
        BlockState soil = world.state(soilPos);
        BlockState after = t.plan().logicFor(index).soilAfterHarvest(soil);
        if (after == null || after == soil || !world.canModify(soilPos)) {
            return;
        }
        world.setBlock(soilPos, after, Block.UPDATE_ALL);
    }

    /**
     * Plans the interrupted harvest again from its plot, after a reload or a changed target: the
     * plot's logic is asked again, and a harvest it answers resumes; anything else completes the
     * harvest as it stands.
     */
    private FarmStatus replan(Tick t) {
        BlockPos base = harvestBase;
        int index = base != null ? t.area().indexOf(base) : -1;
        if (base == null || !harvest.isEmpty() || index < 0 || !t.world().isLoaded(base)) {
            visitPending = false;
            visitDeferred = false;
            replanHarvest = false;
            harvestBase = null;
            harvestedPlots.clear();
            dropPlanner();
            dirty = true;
            return FarmStatus.IDLE;
        }
        beginVisit();
        FarmLogic baseLogic = t.plan().logicFor(index);
        replanning = true;
        PlotAction action;
        try {
            action = baseLogic.decide(new PlanningWorld(t.world()), t.plan().regionFor(index), base, inputsFor(baseLogic));
        } finally {
            replanning = false;
        }
        FarmStatus outcome = FarmStatus.IDLE;
        if (!visitPending && !visitDeferred) {
            replanHarvest = false;
            if (action instanceof PlotAction.Harvest replanned && !replanned.blocks().isEmpty()) {
                outcome = startHarvest(t, base, replanned.blocks(), true);
                if (harvest.isEmpty() && harvestBase != null && !replanHarvest) {
                    // Nothing of it left to take up again.
                    completeHarvest(t);
                }
            } else {
                // Nothing left standing: the harvest is complete.
                completeHarvest(t);
            }
        }
        endVisit(t.settings());
        if (visitPending) {
            plannerIndex = index;
            plannerReplans = true;
            return FarmStatus.IDLE;
        }
        if (visitDeferred) {
            replanHarvest = true;
            return FarmStatus.IDLE;
        }
        tracker.record(outcome);
        return outcome;
    }

    private static AABB sweepBox(PlotArea area) {
        BlockPos min = area.min();
        BlockPos max = area.max();
        return new AABB(
                min.getX(), min.getY() - 1, min.getZ(), max.getX() + 1, max.getY() + SWEEP_HEIGHT + 1, max.getZ() + 1);
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
    }

    /** The world a logic decides from in a visit: reads pass through, trees are planned by the farm. */
    private final class PlanningWorld implements FarmWorld {

        private final FarmWorld world;

        PlanningWorld(FarmWorld world) {
            this.world = world;
        }

        @Override
        public boolean isLoaded(BlockPos pos) {
            return world.isLoaded(pos);
        }

        @Override
        public BlockState state(BlockPos pos) {
            return world.state(pos);
        }

        @Override
        public int light(BlockPos pos) {
            return world.light(pos);
        }

        @Override
        public boolean canSurvive(BlockState state, BlockPos pos) {
            return world.canSurvive(state, pos);
        }

        @Override
        public FarmClimate climate(BlockPos pos) {
            return world.climate(pos);
        }

        @Override
        public boolean rainingAt(BlockPos pos) {
            return world.rainingAt(pos);
        }

        @Override
        public TreeView.Kind treeKind(BlockState state) {
            return world.treeKind(state);
        }

        @Override
        public TreeView treeView() {
            return world.treeView();
        }

        @Override
        public FellPlan fellPlan(BlockPos plot, TreeFeller.Limits limits) {
            return FarmComponent.this.fellPlan(plot, limits);
        }
    }

    /** The world a logic decides from in a survey: what the farm would clear reads as air, and no tree is planned. */
    private static final class SurveyWorld implements FarmWorld {

        private final FarmWorld world;
        private final Set<BlockPos> cleared;

        SurveyWorld(FarmWorld world, Set<BlockPos> cleared) {
            this.world = world;
            this.cleared = cleared;
        }

        @Override
        public boolean isLoaded(BlockPos pos) {
            return world.isLoaded(pos);
        }

        @Override
        public BlockState state(BlockPos pos) {
            return cleared.contains(pos) ? Blocks.AIR.defaultBlockState() : world.state(pos);
        }

        @Override
        public int light(BlockPos pos) {
            return world.light(pos);
        }

        @Override
        public boolean canSurvive(BlockState state, BlockPos pos) {
            return world.canSurvive(state, pos);
        }

        @Override
        public FarmClimate climate(BlockPos pos) {
            return world.climate(pos);
        }

        @Override
        public boolean rainingAt(BlockPos pos) {
            return world.rainingAt(pos);
        }

        @Override
        public TreeView.Kind treeKind(BlockState state) {
            return world.treeKind(state);
        }

        @Override
        public boolean neverClear(BlockState state) {
            return world.neverClear(state);
        }

        @Override
        public boolean naturalClearable(BlockState state) {
            return world.naturalClearable(state);
        }

        @Override
        public FellPlan fellPlan(BlockPos plot, TreeFeller.Limits limits) {
            // A tree is the farm's own growth, harvested rather than cleared.
            return FellPlan.skip(FellPlan.Reason.NO_TREE);
        }
    }

    // ==================== Irrigation ====================

    /**
     * Vanilla asking whether the farmland at {@code pos} is near water, once it found none. True,
     * paying {@code water_per_irrigation_mb} scaled by the climate at {@code pos}, for a soil cell of
     * the ring of a working farm whose tank holds that much and that sees no redstone signal. Rain
     * wets farmland for free, so a plot in the rain is answered false and costs nothing; so is one the
     * tank cannot pay for in full, which then dries a level.
     */
    boolean irrigate(FarmWorld world, BlockPos pos) {
        RingArea area = irrigated;
        if (area == null || water == null || pos.getY() != area.origin().getY() - 1 || !area.contains(pos)) {
            return false;
        }
        if (world.rainingAt(pos.above())) {
            return false;
        }
        long needed = FluidUnits.mb(Hydration.waterMb(irrigationMb, world.climate(pos)));
        FluidTankComponent tank = water.tank();
        if (tank.getAmount() < needed) {
            return false;
        }
        if (needed > 0) {
            tank.extract(tank.getFluidKey(), needed, false);
        }
        return true;
    }

    /** The ring {@link #irrigate} wets now, or null; as of the last tick. */
    @Nullable
    RingArea irrigatedArea() {
        return irrigated;
    }

    /** Stops irrigating until the next tick finds the farm working; for a block entity being removed or unloaded. */
    public void detach() {
        irrigated = null;
        unregisterIrrigation();
    }

    /** Registers the ring with {@link FarmlandHydration} while working with a tank, again whenever the ring or level changes. */
    private void updateIrrigation(MachineContext ctx, RingArea area, boolean paused, FarmSettings s) {
        boolean working = state == FarmState.ACTIVE && water != null;
        irrigated = working && !paused ? area : null;
        irrigationMb = s.waterPerIrrigationMb();
        if (!working || !(ctx.level() instanceof ServerLevel level)) {
            unregisterIrrigation();
            return;
        }
        if (irrigation != null && level == irrigationLevel && area.equals(irrigationArea)) {
            return;
        }
        unregisterIrrigation();
        irrigation = FarmlandHydration.register(level, new Irrigation(level, ctx.pos(), area));
        irrigationLevel = level;
        irrigationArea = area;
    }

    private void unregisterIrrigation() {
        if (irrigation != null) {
            irrigation.unregister();
        }
        irrigation = null;
        irrigationLevel = null;
        irrigationArea = null;
    }

    /** The ring's soil layer as a {@link HydrationSource}. */
    private final class Irrigation implements HydrationSource {

        private final BlockPos anchor;
        private final BoundingBox bounds;
        private final FarmWorld world;

        Irrigation(ServerLevel level, BlockPos anchor, RingArea area) {
            this.anchor = anchor.immutable();
            int soilY = area.origin().getY() - 1;
            BlockPos min = area.min();
            BlockPos max = area.max();
            this.bounds = new BoundingBox(min.getX(), soilY, min.getZ(), max.getX(), soilY, max.getZ());
            this.world = new LevelFarmWorld(level, owner);
        }

        @Override
        public BlockPos anchor() {
            return anchor;
        }

        @Override
        public BoundingBox bounds() {
            return bounds;
        }

        @Override
        public boolean hydrate(ServerLevel level, BlockPos pos) {
            return irrigate(world, pos);
        }
    }

    // ==================== Status ====================

    private void updateStatus(MachineContext ctx, FarmSettings s, PlotArea area, boolean paused) {
        if (paused) {
            status = FarmStatus.PAUSED;
        } else if (state == FarmState.SURVEY) {
            status = FarmStatus.SURVEYING;
        } else {
            status = tracker.status(area.plotCount(), problem -> stillApplies(problem, s));
        }
        BlockState state = ctx.blockState();
        boolean lit = energy.amount() > 0;
        if (state.hasProperty(FarmMachineBlock.LIT) && state.getValue(FarmMachineBlock.LIT) != lit) {
            ctx.setBlockState(state.setValue(FarmMachineBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    private boolean stillApplies(FarmStatus problem, FarmSettings s) {
        return switch (problem) {
            case NO_POWER -> energy.amount() < powerNeeded;
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
        int blocked = plotsBlocked();
        hud.line(HUD_PLOTS, HUD_PLOTS_VALUE, Integer.toString(plotsTotal() - blocked), Integer.toString(blocked));
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
        tag.putString("State", state.id());
        if (pendingPreview != null) {
            tag.put("Preview", savePreview(pendingPreview));
        }
    }

    private static CompoundTag savePreview(FarmPreview preview) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Shown", preview.shown());
        tag.putLong("Until", preview.until());
        tag.putInt("Inner", preview.inner());
        tag.putInt("Outer", preview.outer());
        tag.putInt("Height", preview.height());
        FarmSurvey survey = preview.survey();
        tag.putInt("Natural", survey.natural());
        tag.putInt("Flagged", survey.flagged());
        tag.putInt("Protected", survey.protectedCount());
        tag.putLongArray("FlaggedAt", survey.flaggedAt().stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray("ProtectedAt", survey.protectedAt().stream().mapToLong(BlockPos::asLong).toArray());
        return tag;
    }

    private static FarmPreview loadPreview(FarmState state, CompoundTag tag) {
        FarmSurvey survey = FarmSurvey.of(
                NbtCompat.getInt(tag, "Natural", 0),
                NbtCompat.getInt(tag, "Flagged", 0),
                NbtCompat.getInt(tag, "Protected", 0),
                positions(NbtCompat.getLongArray(tag, "FlaggedAt", new long[0])),
                positions(NbtCompat.getLongArray(tag, "ProtectedAt", new long[0])));
        return new FarmPreview(
                state,
                NbtCompat.getBoolean(tag, "Shown", false),
                NbtCompat.getLong(tag, "Until", 0L),
                NbtCompat.getInt(tag, "Inner", 0),
                NbtCompat.getInt(tag, "Outer", 0),
                NbtCompat.getInt(tag, "Height", 0),
                survey);
    }

    private static List<BlockPos> positions(long[] packed) {
        List<BlockPos> positions = new ArrayList<>(Math.min(packed.length, FarmSurvey.MAX_LISTED));
        for (int i = 0; i < packed.length && i < FarmSurvey.MAX_LISTED; i++) {
            positions.add(BlockPos.of(packed[i]));
        }
        return positions;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        savedCursor = NbtCompat.getInt(tag, "Cursor", 0);
        scanner = null;
        heldIndex = -1;
        owner = parseUuid(NbtCompat.getString(tag, "Owner", ""));
        harvest.clear();
        dropPlanner();
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
        state = FarmState.fromId(NbtCompat.getString(tag, "State", ""));
        previewToggled = state == FarmState.SURVEY;
        // Only an update tag carries the preview; it is never saved.
        syncedPreview = NbtCompat.hasCompound(tag, "Preview")
                ? loadPreview(state, NbtCompat.getCompoundOrEmpty(tag, "Preview"))
                : FarmPreview.NONE;
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

package com.logistics.forestry.farm;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.MachineHudModel;
import com.logistics.core.machine.area.PlotScanner;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.tier.MachineTier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A tend farm's work loop: it watches every position of its Reach, {@value #REACH} blocks out on
 * every side (corners included) and {@value #REACH_HEIGHT} above and below the farm, the farm's own
 * position left out, and harvests whatever its {@link TendLogic} finds ripe there. It never clears,
 * lays soil or plants anywhere it has not just harvested.
 *
 * <p>The scan visits one position per check, round-robin, bounded every tick by a
 * {@link WorkBudget}: its tier's checks and actions per second. A pass that finds nothing ripe backs
 * the scan off ({@link PlotScanner#idleTicks()}). A ripe find starts a harvest: its blocks are broken
 * in order, one per action, and while one is under way the scan waits. Each target is re-checked as
 * it is broken; one that has changed since it was planned drops the rest of the harvest.
 *
 * <p>Once every block is broken the harvest's drops are worked through in order: one matching
 * germling is taken from them to replant the plot, the rest go into the inventory, and whatever does
 * not fit is dropped at the plot. The farm never stops for a full inventory.
 *
 * <p>Every action checks everything it needs without side effects first (loaded, claim protection,
 * RF), then changes the world, and charges only once the world reports the change. A harvest only
 * starts while the buffer holds its first break and the replant; a position the farm cannot pay for
 * holds the scan, reading nothing, until it can. A host that does not permit work
 * ({@link MachineContext#workEnabled}, the farm's redstone control) pauses everything. Nothing
 * touches an unloaded position.
 */
public final class TendFarmComponent implements FarmEngine {

    /** How far the Reach extends from the farm on every horizontal side. */
    public static final int REACH = 3;

    /** How far the Reach extends above and below the farm. */
    public static final int REACH_HEIGHT = 1;

    /** Side of the Reach's square. */
    public static final int REACH_SIZE = 2 * REACH + 1;

    /** Every position of the Reach relative to the farm, in scan order. */
    static final List<BlockPos> REACH_OFFSETS = reachOffsets();

    private record Target(BlockPos pos, Block block) {}

    private final String id;
    private final TendLogic logic;
    private final Container items;
    private final FarmOutput inventory;
    private final EnergyStorageComponent energy;
    private final Supplier<FarmSettings> settings;
    private final Supplier<MachineTier> tier;
    private final FarmComponent.WorldFactory worlds;
    private final FarmStatusTracker tracker = new FarmStatusTracker();
    private final WorkBudget budget = new WorkBudget();
    private final CountingWorld counted = new CountingWorld();
    private final ArrayDeque<Target> targets = new ArrayDeque<>();
    private final List<ItemStack> collected = new ArrayList<>();

    @Nullable private TendLogic.Harvest harvest;
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
    private FarmStatus status = FarmStatus.IDLE;
    // What the last NO_POWER was short of, so the status clears as soon as it is there.
    private long powerNeeded;
    private boolean dirty;

    public TendFarmComponent(
            String id,
            TendLogic logic,
            ItemStoreComponent items,
            EnergyStorageComponent energy,
            Supplier<FarmSettings> settings,
            Supplier<MachineTier> tier,
            FarmComponent.WorldFactory worlds) {
        this.id = id;
        this.logic = logic;
        this.items = items.container();
        this.inventory = new FarmOutput(this.items, 0, this.items.getContainerSize());
        this.energy = energy;
        this.settings = settings;
        this.tier = tier;
        this.worlds = worlds;
    }

    private static List<BlockPos> reachOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int dy = REACH_HEIGHT; dy >= -REACH_HEIGHT; dy--) {
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

    @Override
    public FarmStatus status() {
        return status;
    }

    @Override
    @Nullable
    public UUID owner() {
        return owner;
    }

    @Override
    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    @Override
    public int areaSize() {
        return REACH_SIZE;
    }

    @Override
    public int plotsTotal() {
        return 0;
    }

    @Override
    public int plotsBlocked() {
        return 0;
    }

    @Override
    public void setRateOverride(int checksPerSecond, int actionsPerSecond) {
        if (checksPerSecond <= 0 && actionsPerSecond <= 0) {
            checksOverride = 0;
            actionsOverride = 0;
            return;
        }
        checksOverride = Math.max(1, checksPerSecond);
        actionsOverride = Math.max(1, actionsPerSecond);
    }

    /** Always working: a tend farm has no survey. */
    @Override
    public FarmState state() {
        return FarmState.ACTIVE;
    }

    @Override
    public FarmSurvey survey() {
        return FarmSurvey.EMPTY;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean toggleArea(MachineContext ctx, long now) {
        return false;
    }

    @Override
    public FarmSurvey expanded(MachineContext ctx, long now) {
        return FarmSurvey.EMPTY;
    }

    @Override
    public boolean previewShown(long now) {
        return false;
    }

    @Override
    public FarmPreview syncedPreview() {
        return FarmPreview.NONE;
    }

    @Override
    public <T> T withPreview(MachineContext ctx, long now, Supplier<T> save) {
        return save.get();
    }

    @Override
    public void setAreaOverride(int inner, int outer) {}

    @Override
    public void setClearanceHeightOverride(int height) {}

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
            work(ctx.pos(), s);
        }
        updateStatus(ctx, paused);

        if (dirty) {
            dirty = false;
            ctx.markChanged();
        }
    }

    private void ensureScanner(FarmSettings s) {
        if (scanner == null || scannerBase != s.idleBackoffBaseTicks() || scannerMax != s.idleBackoffMaxTicks()) {
            int cursor = scanner != null ? scanner.cursor() : savedCursor;
            scannerBase = s.idleBackoffBaseTicks();
            scannerMax = s.idleBackoffMaxTicks();
            scanner = PlotScanner.restore(REACH_OFFSETS.size(), cursor, scannerBase, scannerMax);
            stepsInPass = 0;
        }
    }

    /** Spends this tick's actions: on the harvest under way, or on the scan once there is none. */
    private void work(BlockPos origin, FarmSettings s) {
        while (budget.hasAction()) {
            FarmStatus outcome;
            if (harvest != null) {
                long mark = counted.reads();
                outcome = breakNext(s);
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
            BlockPos pos = origin.offset(REACH_OFFSETS.get(scanner.cursor()));
            tracker.step();
            dirty = true;
            counted.forget();
            long mark = counted.reads();
            FarmStatus outcome = visit(pos, s);
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
            if (stepsInPass >= scanner.plotCount()) {
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
    private FarmStatus visit(BlockPos pos, FarmSettings s) {
        if (!counted.isLoaded(pos)) {
            return FarmStatus.IDLE;
        }
        TendLogic.Harvest found = logic.ripe(counted, pos);
        return found != null ? startHarvest(found, s) : FarmStatus.IDLE;
    }

    // ==================== Harvest ====================

    /**
     * Takes {@code found} as the farm's harvest and breaks its first block as this action. It only
     * starts while the buffer holds that break and the replant.
     */
    private FarmStatus startHarvest(TendLogic.Harvest found, FarmSettings s) {
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
        if (planned.isEmpty()) {
            return FarmStatus.IDLE;
        }
        BlockState first = counted.seen(planned.get(0).pos());
        long needed = s.cost(logic.harvestCost(first))
                + (found.germling().isEmpty() ? 0 : s.cost(FarmCost.PLANT));
        if (energy.amount() < needed) {
            return noPower(needed);
        }
        harvest = found;
        targets.addAll(planned);
        collected.clear();
        dirty = true;
        return breakNext(s);
    }

    /**
     * Breaks the next block of the harvest; finishes it once the last is broken. A target already
     * gone is passed over and one claim protection refuses is left standing; any other change since
     * it was planned drops the rest of the harvest.
     */
    private FarmStatus breakNext(FarmSettings s) {
        boolean refused = false;
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
                // Changed since planned: never break on a stale plan.
                targets.clear();
                break;
            }
            if (!counted.canModify(pos)) {
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
                finishHarvest(s);
            }
            return FarmStatus.WORKING;
        }
        finishHarvest(s);
        return refused ? FarmStatus.BLOCKED : FarmStatus.IDLE;
    }

    /** Replants the harvest's plot from its drops, stores the rest, and drops what does not fit at the plot. */
    private void finishHarvest(FarmSettings s) {
        TendLogic.Harvest done = harvest;
        harvest = null;
        targets.clear();
        dirty = true;
        if (done == null) {
            collected.clear();
            return;
        }
        List<ItemStack> drops = new ArrayList<>(collected);
        collected.clear();
        ItemStack germling = takeGermling(drops, done);
        if (!germling.isEmpty() && !replant(done, germling, s)) {
            drops.add(germling);
        }
        List<ItemStack> spill = new ArrayList<>();
        for (ItemStack drop : drops) {
            ItemStack rest = inventory.insert(drop);
            if (!rest.isEmpty()) {
                spill.add(rest);
            }
        }
        if (!spill.isEmpty()) {
            counted.dropStacks(done.plot(), spill);
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

    /** Plants {@code germling} on the harvest's plot; false, spending nothing, when it cannot. */
    private boolean replant(TendLogic.Harvest done, ItemStack germling, FarmSettings s) {
        BlockPos plot = done.plot();
        if (!counted.isLoaded(plot)) {
            return false;
        }
        BlockState plant = logic.plantState(germling, plot, counted);
        long cost = s.cost(FarmCost.PLANT);
        if (plant == null || energy.amount() < cost || !counted.canModify(plot)) {
            return false;
        }
        BlockState current = counted.state(plot);
        if (!(current.isAir() || current.canBeReplaced()) || !counted.canSurvive(plant, plot)) {
            return false;
        }
        if (!counted.setBlock(plot, plant, Block.UPDATE_ALL)) {
            return false;
        }
        energy.consume(cost);
        return true;
    }

    private FarmStatus noPower(long needed) {
        powerNeeded = needed;
        return FarmStatus.NO_POWER;
    }

    // ==================== Status ====================

    private void updateStatus(MachineContext ctx, boolean paused) {
        status = paused ? FarmStatus.PAUSED : tracker.status(REACH_OFFSETS.size(), this::stillApplies);
        showLamp(ctx);
    }

    private boolean stillApplies(FarmStatus problem) {
        return switch (problem) {
            case NO_POWER -> energy.amount() < powerNeeded;
            case INVENTORY_FULL -> !hasEmptySlot();
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

    /** The block's status lamp: lit while the farm holds any power. */
    private void showLamp(MachineContext ctx) {
        BlockState state = ctx.blockState();
        boolean lit = energy.amount() > 0;
        if (state.hasProperty(FarmMachineBlock.LIT) && state.getValue(FarmMachineBlock.LIT) != lit) {
            ctx.setBlockState(state.setValue(FarmMachineBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override
    public void contributeHud(MachineHudModel hud) {
        hud.line(FarmComponent.HUD_STATUS, status.translationKey());
        hud.line(FarmComponent.HUD_PLOT, FarmComponent.HUD_PLOT_SIZE, Integer.toString(REACH_SIZE));
    }

    // ==================== Persistence ====================

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Cursor", scanner != null ? scanner.cursor() : savedCursor);
        if (owner != null) {
            tag.putString("Owner", owner.toString());
        }
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        savedCursor = NbtCompat.getInt(tag, "Cursor", 0);
        scanner = null;
        heldPower = 0;
        owner = parseUuid(NbtCompat.getString(tag, "Owner", ""));
        harvest = null;
        targets.clear();
        collected.clear();
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

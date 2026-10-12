package com.logistics.forestry.farm;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.MachineHudModel;
import com.logistics.core.machine.area.PlotScanner;
import com.logistics.core.machine.area.WorkArea;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.forestry.fertilizer.FertilizerCharges;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The farm's work loop. Every work interval it takes the next plot of its {@link WorkArea} from a
 * {@link PlotScanner}, asks the {@link FarmLogic} what the plot needs, and performs that one
 * {@link PlotAction} if the farm can afford it.
 *
 * <p>The farm-wide gates live here, not in the logic. A redstone signal pauses everything. Every op
 * costs RF and is skipped whole when the buffer cannot pay for it; irrigating also costs water,
 * planting needs germlings and laying soil needs soil. A full output buffer stops harvesting only.
 * Nothing touches an unloaded position, and every write first asks claim protection.
 *
 * <p>A harvest of several blocks (a tree) is broken a few blocks per tick until done, ahead of the
 * scan. Drops and the loose items swept from the area afterwards go to the output slots, and what
 * does not fit waits in an overflow list.
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

    /** Loose items are swept from one block below the plot to this far above it. */
    private static final int SWEEP_HEIGHT = 32;

    private static final int MAX_MOISTURE = Collections.max(BlockStateProperties.MOISTURE.getPossibleValues());

    private record HarvestTarget(BlockPos pos, Block block) {}

    private final String id;
    private final FarmLogic logic;
    private final Container items;
    private final EnergyStorageComponent energy;
    @Nullable private final FluidStoreComponent water;
    private final Supplier<FarmSettings> settings;
    private final WorldFactory worlds;
    private final FarmOutput output;
    private final Inputs inputs = new Inputs();
    private final FarmStatusTracker tracker = new FarmStatusTracker();
    private final ArrayDeque<HarvestTarget> harvest = new ArrayDeque<>();

    @Nullable private PlotScanner scanner;
    private int scannerBase;
    private int scannerMax;
    private int savedCursor;
    private int stepsInPass;
    private int cooldown;
    private int charges;
    @Nullable private UUID owner;
    private int areaSizeOverride;
    // Only the harvest's plot persists; the blocks are re-planned from it after a reload.
    @Nullable private BlockPos harvestBase;
    private boolean replanHarvest;
    private FarmStatus status = FarmStatus.IDLE;
    // What the last NO_POWER / NO_GERMLINGS was short of, so the status clears as soon as it is there.
    private long powerNeeded;
    private ItemStack germlingNeeded = ItemStack.EMPTY;
    private int germlingCountNeeded;
    private boolean dirty;

    public FarmComponent(
            String id,
            FarmLogic logic,
            ItemStoreComponent items,
            EnergyStorageComponent energy,
            @Nullable FluidStoreComponent water,
            Supplier<FarmSettings> settings,
            WorldFactory worlds) {
        this.id = id;
        this.logic = logic;
        this.items = items.container();
        this.energy = energy;
        this.water = water;
        this.settings = settings;
        this.worlds = worlds;
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

    /** Fertilizer charges banked from consumed fertilizer items. */
    public int charges() {
        return charges;
    }

    /** Width of the square plot currently worked. */
    public int areaSize() {
        return areaSize(settings.get());
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    /** Test seam: works a plot of {@code size} (odd) instead of the configured one; 0 or less restores it. */
    public void setAreaSizeOverride(int size) {
        if (size > 0 && size % 2 == 0) {
            throw new IllegalArgumentException("area size must be odd: " + size);
        }
        areaSizeOverride = Math.max(0, size);
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
        FarmWorldAccess world = worlds.open(ctx, owner);
        if (world == null) {
            return;
        }
        FarmSettings s = settings.get();
        if (output.drainOverflow()) {
            dirty = true;
        }
        WorkArea area = area(ctx, s);
        ensureScanner(area.plotCount(), s);

        boolean paused = world.hasNeighborSignal(ctx.pos());
        if (!paused) {
            work(world, s, area);
        }
        updateStatus(ctx, s, area, paused);

        if (dirty) {
            dirty = false;
            ctx.markChanged();
        }
    }

    private void work(FarmWorldAccess world, FarmSettings s, WorkArea area) {
        if (replanHarvest) {
            replanHarvest(world);
        }
        if (!harvest.isEmpty()) {
            FarmStatus outcome = continueHarvest(world, s, area);
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
        step(world, s, area);
    }

    private void step(FarmWorldAccess world, FarmSettings s, WorkArea area) {
        tracker.step();
        int index = scanner.next();
        stepsInPass++;
        dirty = true;

        BlockPos plot = area.plot(index);
        FarmStatus outcome = world.isLoaded(plot)
                ? execute(world, s, area, plot, logic.decide(world, plot, inputs))
                : FarmStatus.IDLE;
        tracker.record(outcome);
        // Logics only ask to irrigate when there is water, so a dry tank is reported here.
        if (water != null && !inputs.hasWater()) {
            tracker.record(FarmStatus.NO_WATER);
        }
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

    private WorkArea area(MachineContext ctx, FarmSettings s) {
        BlockState state = ctx.blockState();
        Direction facing =
                state.hasProperty(FarmMachineBlock.FACING) ? state.getValue(FarmMachineBlock.FACING) : Direction.NORTH;
        return WorkArea.inFront(ctx.pos(), facing, areaSize(s));
    }

    private int areaSize(FarmSettings s) {
        return areaSizeOverride > 0 ? areaSizeOverride : s.areaSize();
    }

    // ==================== Ops ====================

    private FarmStatus execute(FarmWorldAccess world, FarmSettings s, WorkArea area, BlockPos plot, PlotAction action) {
        return switch (action) {
            case PlotAction.Harvest harvestAction -> startHarvest(world, s, area, plot, harvestAction.blocks());
            case PlotAction.PlaceSoil placeSoil -> placeSoil(world, s, placeSoil.pos());
            case PlotAction.Till till -> till(world, s, till.pos(), till.result());
            case PlotAction.Irrigate irrigate -> irrigate(world, s, irrigate.pos());
            case PlotAction.Plant plant -> plant(world, s, plant.positions(), plant.germling());
            case PlotAction.Fertilize fertilize -> fertilize(world, s, fertilize.pos());
            case PlotAction.None none -> FarmStatus.IDLE;
            case PlotAction.Blocked blocked ->
                blocked.reason() == PlotAction.Reason.TOO_DARK ? FarmStatus.TOO_DARK : FarmStatus.BLOCKED;
        };
    }

    private FarmStatus startHarvest(
            FarmWorldAccess world, FarmSettings s, WorkArea area, BlockPos plot, List<BlockPos> blocks) {
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
        return continueHarvest(world, s, area);
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
    private FarmStatus continueHarvest(FarmWorldAccess world, FarmSettings s, WorkArea area) {
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
        }
        if (harvest.size() != pending) {
            dirty = true;
        }
        if (harvest.isEmpty()) {
            harvestBase = null;
            for (ItemStack stack : world.collectItems(sweepBox(area))) {
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

    private void replanHarvest(FarmWorldAccess world) {
        replanHarvest = false;
        BlockPos base = harvestBase;
        harvestBase = null;
        dirty = true;
        if (base == null || !harvest.isEmpty() || !world.isLoaded(base)) {
            return;
        }
        if (logic.decide(world, base, inputs) instanceof PlotAction.Harvest replanned) {
            queueHarvest(world, replanned.blocks());
            if (!harvest.isEmpty()) {
                harvestBase = base;
            }
        }
    }

    private static AABB sweepBox(WorkArea area) {
        BlockPos min = area.min();
        BlockPos max = area.max();
        return new AABB(
                min.getX(), min.getY() - 1, min.getZ(), max.getX() + 1, max.getY() + SWEEP_HEIGHT + 1, max.getZ() + 1);
    }

    private FarmStatus placeSoil(FarmWorldAccess world, FarmSettings s, BlockPos pos) {
        if (!world.isLoaded(pos)) {
            return FarmStatus.IDLE;
        }
        if (energy.amount() < s.rfSoil()) {
            return noPower(s.rfSoil());
        }
        int slot = soilSlot();
        if (slot < 0) {
            return FarmStatus.NO_SOIL;
        }
        BlockState soil = ((BlockItem) items.getItem(slot).getItem()).getBlock().defaultBlockState();
        BlockState current = world.state(pos);
        if (current.is(soil.getBlock())) {
            return FarmStatus.IDLE;
        }
        // Never fill in water, a source, a flow or a waterlogged block.
        if (!current.getFluidState().isEmpty()) {
            return FarmStatus.BLOCKED;
        }
        boolean open = current.isAir() || current.canBeReplaced();
        // Soil goes only where there was nothing, or ground the farm may dig up; never a player's floor.
        if (!world.canModify(pos) || current.hasBlockEntity() || !(open || world.isReplaceableSoil(pos))) {
            return FarmStatus.BLOCKED;
        }
        if (!open) {
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
        if (!world.canModify(pos)) {
            return FarmStatus.BLOCKED;
        }
        world.setBlock(pos, result, Block.UPDATE_ALL);
        energy.consume(s.rfTill());
        return FarmStatus.WORKING;
    }

    private FarmStatus irrigate(FarmWorldAccess world, FarmSettings s, BlockPos pos) {
        if (water == null || !world.isLoaded(pos)) {
            return FarmStatus.IDLE;
        }
        BlockState state = world.state(pos);
        if (!state.hasProperty(BlockStateProperties.MOISTURE)
                || state.getValue(BlockStateProperties.MOISTURE) >= MAX_MOISTURE) {
            return FarmStatus.IDLE;
        }
        if (energy.amount() < s.rfIrrigate()) {
            return noPower(s.rfIrrigate());
        }
        FluidTankComponent tank = water.tank();
        long needed = FluidUnits.mb(s.waterPerIrrigateMb());
        if (tank.getAmount() < needed) {
            return FarmStatus.NO_WATER;
        }
        if (!world.canModify(pos)) {
            return FarmStatus.BLOCKED;
        }
        // Moisture is a visual-only change, so neighbours are not updated.
        world.setBlock(pos, state.setValue(BlockStateProperties.MOISTURE, MAX_MOISTURE), Block.UPDATE_CLIENTS);
        if (needed > 0) {
            tank.extract(tank.getFluidKey(), needed, false);
        }
        energy.consume(s.rfIrrigate());
        tracker.clear(FarmStatus.NO_WATER);
        return FarmStatus.WORKING;
    }

    private FarmStatus plant(FarmWorldAccess world, FarmSettings s, List<BlockPos> positions, ItemStack germling) {
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
        if (germling.isEmpty() || inputs.germlingCount(germling) < positions.size()) {
            germlingNeeded = germling;
            germlingCountNeeded = positions.size();
            return FarmStatus.NO_GERMLINGS;
        }
        List<BlockState> plants = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            BlockState plant = logic.plantState(germling, pos, world);
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
        takeGermlings(germling, positions.size());
        energy.consume(cost);
        tracker.clear(FarmStatus.NO_GERMLINGS);
        return FarmStatus.WORKING;
    }

    private FarmStatus fertilize(FarmWorldAccess world, FarmSettings s, BlockPos pos) {
        // Fertilizer is optional: without it plants simply grow at their own pace.
        if (!world.isLoaded(pos) || !world.canFertilize(pos) || !inputs.hasFertilizer()) {
            return FarmStatus.IDLE;
        }
        if (energy.amount() < s.rfFertilize()) {
            return noPower(s.rfFertilize());
        }
        if (!world.canModify(pos)) {
            return FarmStatus.BLOCKED;
        }
        if (charges <= 0) {
            ItemStack fertilizer = items.getItem(FarmMachine.FERTILIZER_SLOT);
            charges += FertilizerCharges.charges(fertilizer);
            items.removeItem(FarmMachine.FERTILIZER_SLOT, 1);
        }
        charges--;
        energy.consume(s.rfFertilize());
        // The charge is spent like bone meal is, even when the plant's own growth roll fails.
        world.fertilize(pos);
        return FarmStatus.WORKING;
    }

    private FarmStatus noPower(long needed) {
        powerNeeded = needed;
        return FarmStatus.NO_POWER;
    }

    // ==================== Inventory ====================

    /** First soil slot holding a placeable block, or -1. */
    private int soilSlot() {
        if (!logic.laysSoil()) {
            return -1;
        }
        for (int slot = FarmMachine.SOIL_START; slot < FarmMachine.SOIL_END; slot++) {
            if (items.getItem(slot).getItem() instanceof BlockItem) {
                return slot;
            }
        }
        return -1;
    }

    private void takeGermlings(ItemStack kind, int count) {
        int remaining = count;
        if (logic.replantsFromOutput()) {
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

    /** The logic's view of the inventory. */
    private final class Inputs implements FarmInputs {

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
                addKind(kinds, items.getItem(slot));
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
            if (kind.isEmpty()) {
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
            return soilSlot() >= 0;
        }

        @Override
        public boolean hasWater() {
            return water != null && water.tank().getAmount() >= FluidUnits.mb(settings.get().waterPerIrrigateMb());
        }

        @Override
        public boolean hasFertilizer() {
            return charges > 0 || FertilizerCharges.charges(items.getItem(FarmMachine.FERTILIZER_SLOT)) > 0;
        }
    }

    // ==================== Status ====================

    private void updateStatus(MachineContext ctx, FarmSettings s, WorkArea area, boolean paused) {
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
            case NO_WATER -> water == null || water.tank().getAmount() < FluidUnits.mb(s.waterPerIrrigateMb());
            case NO_GERMLINGS -> germlingNeeded.isEmpty() || inputs.germlingCount(germlingNeeded) < germlingCountNeeded;
            case NO_SOIL -> soilSlot() < 0;
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
        tag.putInt("Charges", charges);
        if (owner != null) {
            tag.putString("Owner", owner.toString());
        }
        if (harvestBase != null) {
            tag.putBoolean("Harvesting", true);
            tag.putLong("HarvestBase", harvestBase.asLong());
        }
        output.save(tag, "Overflow", registries);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        savedCursor = NbtCompat.getInt(tag, "Cursor", 0);
        scanner = null;
        charges = Math.max(0, NbtCompat.getInt(tag, "Charges", 0));
        owner = parseUuid(NbtCompat.getString(tag, "Owner", ""));
        harvest.clear();
        harvestBase = NbtCompat.getBoolean(tag, "Harvesting", false)
                ? BlockPos.of(NbtCompat.getLong(tag, "HarvestBase", 0L))
                : null;
        replanHarvest = harvestBase != null;
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

package com.logistics.forestry.farm;

import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineComponentContainer;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.area.RingArea;
import com.logistics.core.machine.tier.MachineTier;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A farm's real components ({@link FarmMachine#assemble}) on a fake host and a {@link FakeFarmWorld},
 * started ({@link FarmComponent#start}) unless built with {@link #surveying}.
 * The host is a furnace block state, which carries the same horizontal {@code facing} and
 * {@code lit} properties as a farm block, facing south at the origin.
 */
final class FarmHarness {

    /**
     * 20 checks and 20 actions a second, one of each a tick, with a 10 000 RF buffer taking
     * 1 000 RF/t and an 8 000 mB tank, at every tier.
     */
    static final FarmTierProfile PROFILE = new FarmTierProfile(11, 20, 20, 10_000, 1_000, 8_000);

    /** Every cost different, so tests can tell which was charged. */
    static final Map<FarmCost, Long> COSTS = Map.of(
            FarmCost.CLEAR, 30L,
            FarmCost.HARVEST_CROP, 40L,
            FarmCost.HARVEST_LOG, 45L,
            FarmCost.HARVEST_LEAVES, 7L,
            FarmCost.TILL, 60L,
            FarmCost.SOIL, 65L,
            FarmCost.PLANT, 100L,
            FarmCost.MOISTEN, 20L);

    /** One plot and one action per tick while busy, so tests can count steps in ticks. */
    static final FarmSettings SETTINGS = new FarmSettings(COSTS, 250, 0.5, 64, 20, 200, FarmSettings.everyTier(PROFILE));

    /** The shipped tier profiles, each at the test rate of one check and one action a tick. */
    static Map<MachineTier, FarmTierProfile> shippedTiersAtTestRate() {
        Map<MachineTier, FarmTierProfile> tiers = FarmSettings.defaultTiers();
        tiers.replaceAll((tier, profile) -> profile.withRates(PROFILE.checksPerSecond(), PROFILE.actionsPerSecond()));
        return tiers;
    }

    final ScriptedFarmLogic logic;
    final FakeFarmWorld world = new FakeFarmWorld();
    final Host host = new Host();
    final MachineComponentContainer components = new MachineComponentContainer();
    final FarmMachine machine;
    FarmSettings settings = SETTINGS;
    private int inner = 1;
    private int outer = 3;

    /** A farm working the 8-plot ring around a 1×1 centre. */
    FarmHarness(ScriptedFarmLogic logic) {
        this(logic, SETTINGS);
    }

    /** A farm working the 8-plot ring around a 1×1 centre, its buffers sized from {@code settings}. */
    FarmHarness(ScriptedFarmLogic logic, FarmSettings settings) {
        this((FarmLogic) logic, settings);
    }

    /** A farm of any logic; {@link #logic} is then null unless it is scripted. */
    FarmHarness(FarmLogic logic, FarmSettings settings) {
        this(logic, settings, true);
    }

    private FarmHarness(FarmLogic logic, FarmSettings settings, boolean start) {
        this.logic = logic instanceof ScriptedFarmLogic scripted ? scripted : null;
        this.settings = settings;
        this.machine = FarmMachine.assemble(
                new MachineBuilder(components, () -> {}), logic, () -> settings, (ctx, owner) -> world);
        areaOverride(inner, outer);
        if (start) {
            farm().start();
        }
    }

    /** A farm as placed: surveying, not started. */
    static FarmHarness surveying(FarmLogic logic, FarmSettings settings) {
        return new FarmHarness(logic, settings, false);
    }

    /** Works the ring between squares of {@code inner} and {@code outer}; 0 for both works the tier's. */
    void areaOverride(int inner, int outer) {
        farm().setAreaOverride(inner, outer);
        this.inner = inner;
        this.outer = outer;
    }

    void tick() {
        components.serverTick(host);
    }

    /** RF one {@code cost} action takes. */
    long cost(FarmCost cost) {
        return settings.cost(cost);
    }

    void ticks(int count) {
        for (int i = 0; i < count; i++) {
            tick();
        }
    }

    /** The ring the farm works around the host, one block above it. */
    RingArea area() {
        BlockPos origin = host.pos().above();
        return outer > 0
                ? new RingArea(origin, inner, outer)
                : FarmGeometry.ring(origin, settings.profile(machine.tier().tier()).outerSize());
    }

    /** Plot {@code index} of the host's ring, in scan order. */
    BlockPos plot(int index) {
        return area().plot(index);
    }

    FarmComponent farm() {
        return machine.farm();
    }

    Container items() {
        return machine.items().container();
    }

    void power(long rf) {
        machine.energy().setAmount(rf);
    }

    long energy() {
        return machine.energy().amount();
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        components.save(tag, host.registries());
        return tag;
    }

    void load(CompoundTag tag) {
        components.load(tag, host.registries());
    }

    /** The block entity's side of the farm: position, block state (lit or not) and dirty marking. */
    static final class Host implements MachineContext {
        private final HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        private final RandomSource random = RandomSource.create(42L);
        BlockState state = Blocks.FURNACE.defaultBlockState().setValue(FarmMachineBlock.FACING, Direction.SOUTH);
        int changes;

        boolean lit() {
            return state.getValue(FarmMachineBlock.LIT);
        }

        @Override
        @Nullable
        public Level level() {
            return null;
        }

        @Override
        public BlockPos pos() {
            return BlockPos.ZERO;
        }

        @Override
        public BlockState blockState() {
            return state;
        }

        @Override
        public HolderLookup.Provider registries() {
            return registries;
        }

        @Override
        public void markChanged() {
            changes++;
        }

        @Override
        public void sync() {}

        @Override
        public void setBlockState(BlockState state, int flags) {
            this.state = state;
        }

        @Override
        @Nullable
        public RecipeManager recipeManager() {
            return null;
        }

        @Override
        public RandomSource random() {
            return random;
        }
    }
}

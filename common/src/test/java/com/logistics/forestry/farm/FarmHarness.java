package com.logistics.forestry.farm;

import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineComponentContainer;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.SlotRole;
import com.logistics.core.machine.tier.MachineTier;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A farm's real components ({@link FarmMachine#assemble}) on a fake host and a {@link FakeFarmWorld}.
 * The host is a furnace block state, which carries the same horizontal {@code facing} and
 * {@code lit} properties as a farm block, facing south at the origin.
 */
final class FarmHarness {

    /** 20 checks and 20 actions a second, one of each a tick, with a 10 000 RF buffer taking 1 000 RF/t, at every tier. */
    static final FarmTierProfile PROFILE = new FarmTierProfile(20, 20, 10_000, 1_000);

    /** Every cost different, so tests can tell which was charged. */
    static final Map<FarmCost, Long> COSTS = Map.of(
            FarmCost.HARVEST_CROP, 40L,
            FarmCost.HARVEST_LOG, 45L,
            FarmCost.HARVEST_LEAVES, 7L,
            FarmCost.TILL, 60L,
            FarmCost.PLANT, 100L);

    /** No upkeep, so tests can count action costs exactly; upkeep tests set their own. */
    static final Map<FarmUpkeep, Double> NO_UPKEEP = Map.of(FarmUpkeep.CROP_FARM, 0.0, FarmUpkeep.ARBORETUM, 0.0);

    /** One position and one action per tick while busy, so tests can count steps in ticks. */
    static final FarmSettings SETTINGS =
            new FarmSettings(COSTS, NO_UPKEEP, 64, 20, 200, FarmSettings.everyTier(PROFILE));

    final FakeFarmWorld world = new FakeFarmWorld();
    final Host host = new Host();
    final MachineComponentContainer components = new MachineComponentContainer();
    final FarmMachine machine;
    final FarmSettings settings;

    private FarmHarness(TendLogic logic, FarmSettings settings) {
        this.settings = settings;
        this.machine = FarmMachine.assemble(
                new MachineBuilder(components, () -> {}), logic, () -> settings, (ctx, owner) -> world);
    }

    /** A farm of {@code logic}, working its Reach around the host. */
    static FarmHarness tending(TendLogic logic) {
        return tending(logic, SETTINGS);
    }

    /** A farm of {@code logic}, its buffer sized from {@code settings}. */
    static FarmHarness tending(TendLogic logic, FarmSettings settings) {
        return new FarmHarness(logic, settings);
    }

    /**
     * What a farm from before the farms tended saved: {@code slots} in its 17 germling, soil and
     * output slots, its owner, and its survey state, overflow and preview, which no farm reads now.
     */
    static CompoundTag ringFarmSave(IntFunction<ItemStack> slots) {
        HolderLookup.Provider registries = new Host().registries();
        SlotRole[] roles = new SlotRole[17];
        Arrays.fill(roles, SlotRole.GENERIC);
        ItemStoreComponent inventory = new MachineBuilder(new MachineComponentContainer(), () -> {})
                .items("inventory")
                .slots(roles)
                .openAccess()
                .build();
        for (int slot = 0; slot < roles.length; slot++) {
            inventory.container().setItem(slot, slots.apply(slot));
        }
        CompoundTag items = new CompoundTag();
        inventory.save(items, registries);

        CompoundTag farm = new CompoundTag();
        farm.putInt("Cursor", 5);
        farm.putString("Owner", UUID.randomUUID().toString());
        farm.putString("State", "active");
        ListTag overflow = new ListTag();
        ItemStack.CODEC
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), new ItemStack(Blocks.DIRT.asItem(), 3))
                .result()
                .ifPresent(overflow::add);
        farm.put("Overflow", overflow);
        CompoundTag preview = new CompoundTag();
        preview.putBoolean("Shown", true);
        preview.putInt("Inner", 7);
        preview.putInt("Outer", 11);
        farm.put("Preview", preview);

        CompoundTag components = new CompoundTag();
        components.put("inventory", items);
        components.put("farm", farm);
        CompoundTag root = new CompoundTag();
        root.put("components", components);
        return root;
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

    TendFarmComponent engine() {
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

    /** The shipped tier profiles, each at the test rate of one check and one action a tick. */
    static Map<MachineTier, FarmTierProfile> shippedTiersAtTestRate() {
        Map<MachineTier, FarmTierProfile> tiers = FarmSettings.defaultTiers();
        tiers.replaceAll((tier, profile) -> profile.withRates(PROFILE.checksPerSecond(), PROFILE.actionsPerSecond()));
        return tiers;
    }

    /**
     * The block entity's side of the farm: position, block state (lit or not), dirty marking, and
     * whether its redstone control permits work.
     */
    static final class Host implements MachineContext {
        private final HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        private final RandomSource random = RandomSource.create(42L);
        BlockState state = Blocks.FURNACE.defaultBlockState().setValue(FarmMachineBlock.FACING, Direction.SOUTH);
        int changes;
        int stateWrites;
        boolean workEnabled = true;

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
            stateWrites++;
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

        @Override
        public boolean workEnabled() {
            return workEnabled;
        }
    }
}

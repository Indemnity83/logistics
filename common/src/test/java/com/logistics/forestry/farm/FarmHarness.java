package com.logistics.forestry.farm;

import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineComponentContainer;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.area.WorkArea;
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
 * A farm's real components ({@link FarmMachine#assemble}) on a fake host and a {@link FakeFarmWorld}.
 * The host is a furnace block state, which carries the same horizontal {@code facing} and
 * {@code lit} properties as a farm block, facing south at the origin.
 */
final class FarmHarness {

    /** A 3×3 plot worked one plot per tick, so tests can count steps in ticks. */
    static final FarmSettings SETTINGS =
            new FarmSettings(3, 1, 10_000, 1_000, 40, 100, 60, 60, 20, 8_000, 50, 4, 20, 200);

    final ScriptedFarmLogic logic;
    final FakeFarmWorld world = new FakeFarmWorld();
    final Host host = new Host();
    final MachineComponentContainer components = new MachineComponentContainer();
    final FarmMachine machine;
    FarmSettings settings = SETTINGS;

    FarmHarness(ScriptedFarmLogic logic) {
        this.logic = logic;
        this.machine = FarmMachine.assemble(
                new MachineBuilder(components, () -> {}), logic, () -> settings, (ctx, owner) -> world);
    }

    void tick() {
        components.serverTick(host);
    }

    void ticks(int count) {
        for (int i = 0; i < count; i++) {
            tick();
        }
    }

    /** Plot {@code index} of the host's area, in scan order. */
    BlockPos plot(int index) {
        return WorkArea.inFront(host.pos(), Direction.SOUTH, settings.areaSize()).plot(index);
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

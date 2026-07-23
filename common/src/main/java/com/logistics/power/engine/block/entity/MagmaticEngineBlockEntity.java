package com.logistics.power.engine.block.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.power.EngineBuilder;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.FluidSyncComponent;
import com.logistics.power.engine.block.MagmaticEngineBlock;
import com.logistics.power.engine.magmatic.MagmaticEngineComponent;
import com.logistics.power.engine.magmatic.MagmaticEngineFuels;
import com.logistics.power.engine.magmatic.MagmaticEngineProfile;
import com.logistics.power.engine.ui.MagmaticEngineScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Magmatic Engine: consumes liquid lava (fluid pipe / capability) as a fixed-duration heat source that
 * heat-soaks the engine and generates continuously with temperature. Composed of an RF output buffer, a
 * single lava tank, and the {@link MagmaticEngineComponent} simulation, plus a piston cycle for animation +
 * export. Exposes the tank through one insert-only, lava-filtered fluid view. It cannot overheat.
 */
public class MagmaticEngineBlockEntity extends EngineEntity implements HasFluidStorage, MenuBehavior.HasMenu {

    // Synced GUI data indices.
    public static final int DATA_ENERGY = 0; // 0..10000 fill fraction
    public static final int DATA_ATTEMPTED = 1;
    public static final int DATA_ACCEPTED = 2;
    public static final int DATA_WASTED = 3;
    public static final int DATA_BURN_REMAINING = 4;
    public static final int DATA_BURN_TOTAL = 5;
    public static final int DATA_STAGE = 6; // HeatStage ordinal
    public static final int DATA_TEMPERATURE = 7; // boiler temperature (°C)
    public static final int DATA_LAVA_ID = 8;
    public static final int DATA_LAVA_AMOUNT = 9;
    public static final int DATA_LAVA_CAPACITY = 10;
    public static final int DATA_POWERED = 11;
    public static final int DATA_LIT = 12;
    public static final int DATA_COUNT = 13;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY -> energyFraction();
                case DATA_ATTEMPTED -> (int) magmaSim.lastAttempted();
                case DATA_ACCEPTED -> (int) magmaSim.lastAccepted();
                case DATA_WASTED -> (int) magmaSim.lastWasted();
                case DATA_BURN_REMAINING -> magmaSim.remainingBurnTicks();
                case DATA_BURN_TOTAL -> magmaSim.batchBurnTicks();
                case DATA_STAGE -> magmaSim.stage().ordinal();
                case DATA_TEMPERATURE -> magmaSim.temperatureCelsius();
                case DATA_LAVA_ID -> fluidId(lavaTank);
                case DATA_LAVA_AMOUNT -> amountMb(lavaTank);
                case DATA_LAVA_CAPACITY -> capacityMb(lavaTank);
                case DATA_POWERED -> isPowered() ? 1 : 0;
                case DATA_LIT -> magmaSim.lit() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-side data source only; the client uses a SimpleContainerData populated by sync.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private EngineEnergyOutputComponent energy;
    private FluidStoreComponent lavaTank;
    private MagmaticEngineComponent magmaSim;
    private IFluidStorage fluidView;

    public MagmaticEngineBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.MAGMATIC_ENGINE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MagmaticEngineBlockEntity entity) {
        EngineEntity.tick(level, pos, state, entity);
    }

    @Override
    protected void configure(EngineBuilder engine) {
        MagmaticEngineProfile profile = MagmaticEngineProfile.of(
                LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_OUTPUT),
                LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_BUFFER_CAPACITY),
                (int) (long) LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_TANK_CAPACITY),
                (int) (long) LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_BUCKET_BURN_TICKS));

        energy = engine.energyOutput("energy")
                .capacity(() -> LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_BUFFER_CAPACITY))
                .build();
        lavaTank = engine.fluids("lavaTank")
                .capacity(FluidUnits.mb(LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_TANK_CAPACITY)))
                .build();
        engine.add(new FluidSyncComponent(lavaTank));
        magmaSim = engine.add(new MagmaticEngineComponent(
                "magmaSim", energy, lavaTank, MagmaticEngineFuels::isLava, this::isPowered, profile, this::setChanged));
        engine.pistonCycle("cycle")
                .energy(energy)
                .overheated(() -> false)
                .powered(this::isPowered)
                .pistonSpeed(() -> magmaSim.pistonSpeed())
                .outputPower(profile::hotOutputPerTick)
                .outputFace(() -> MagmaticEngineBlock.getOutputDirection(getBlockState()))
                .sendsEnergyContinuously(true)
                .build();

        fluidView = new MagmaticEngineFluidView(lavaTank);
    }

    @Override
    @Nullable
    public IFluidStorage fluidStorage(@Nullable Direction side) {
        return fluidView;
    }

    // ==================== GUI/HUD accessors ====================

    public FluidStoreComponent lavaTank() {
        return lavaTank;
    }

    public MagmaticEngineComponent simulation() {
        return magmaSim;
    }

    public long getEnergyStored() {
        return energy.getAmount();
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("block.logistics.power.magmatic_engine");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                ContainerLevelAccess accessor = level == null
                        ? ContainerLevelAccess.NULL
                        : ContainerLevelAccess.create(level, getBlockPos());
                return new MagmaticEngineScreenHandler(syncId, playerInventory, containerData, accessor);
            }
        };
    }

    private int energyFraction() {
        long capacity = energy.getCapacity();
        return capacity <= 0 ? 0 : (int) Math.min(10_000L, energy.getAmount() * 10_000L / capacity);
    }

    private static int fluidId(FluidStoreComponent store) {
        return store.tank().isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(store.tank().getFluidKey().getFluid());
    }

    private static int amountMb(FluidStoreComponent store) {
        return (int) Math.min(FluidUnits.toMillibuckets(store.tank().getAmount()), Integer.MAX_VALUE);
    }

    private static int capacityMb(FluidStoreComponent store) {
        return (int) Math.min(FluidUnits.toMillibuckets(store.tank().getCapacity()), Integer.MAX_VALUE);
    }

    /** Insert-only, lava-filtered view: pipes fill the tank; nothing is drainable. */
    private record MagmaticEngineFluidView(FluidStoreComponent lavaTank) implements IFluidStorage {
        @Override
        public long insert(IFluidKey fluid, long maxAmount, boolean simulate) {
            return MagmaticEngineFuels.isLava(fluid.getFluid())
                    ? lavaTank.tank().insert(fluid, maxAmount, simulate)
                    : 0;
        }

        @Override
        public long extract(IFluidKey fluid, long maxAmount, boolean simulate) {
            return 0; // the boiler consumes lava internally; not pipe-drainable
        }

        @Override
        public Iterable<IFluidView> contents() {
            return lavaTank.tank().contents();
        }
    }
}

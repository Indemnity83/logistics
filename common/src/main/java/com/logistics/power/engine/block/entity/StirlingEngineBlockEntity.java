package com.logistics.power.engine.block.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.items.ItemInventoryComponent;
import com.logistics.core.lib.power.EngineBuilder;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.lib.power.FuelHelper;
import com.logistics.core.lib.power.component.EngineBurnComponent;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.lib.power.component.EngineHeatComponent;
import com.logistics.core.lib.power.fuel.FuelSource;
import com.logistics.core.lib.power.gen.Generation;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.core.machine.MachineContext;
import com.logistics.power.engine.block.StirlingEngineBlock;
import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jetbrains.annotations.Nullable;

/**
 * The Stirling Engine: burns solid fuel to generate energy. It has no cooling, so a blocked output
 * builds heat until it overheats and thermally shuts down. Generation is PID-controlled toward a
 * target temperature (wrapped here as a {@link Generation} strategy).
 */
public class StirlingEngineBlockEntity extends EngineEntity
        implements ContainerSingleItem.BlockContainerSingleItem, WorldlyContainer, HasItemStorage, MenuBehavior.HasMenu {

    // PID controller settings (tuned via run/pid_simulator.py)
    private static final double PID_KP = 0.2;
    private static final double PID_KI = 0.0002;
    private static final double PID_KD = 0.3;
    private static final double TARGET_TEMPERATURE = 150;
    private static final double DEFAULT_MIN_GENERATION = 3.0;
    private static final double TEMPERATURE_FLOOR = 20;

    // Property delegate indices for GUI
    public static final int PROPERTY_BURN_TIME = 0;
    public static final int PROPERTY_FUEL_TIME = 1;
    public static final int PROPERTY_HEAT = 2;
    public static final int PROPERTY_ENERGY = 3;
    public static final int PROPERTY_GENERATION = 4;
    public static final int PROPERTY_COUNT = 5;

    // Assigned in configure() — see the note there on why these can't be field initializers.
    private ItemInventoryComponent inventory;
    private StirlingGenerationPlanner generationPlanner;
    private EngineEnergyOutputComponent energy;
    private EngineHeatComponent heat;
    private EngineBurnComponent burn;

    private final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_BURN_TIME -> burn.burnTime();
                case PROPERTY_FUEL_TIME -> burn.fuelTime();
                case PROPERTY_HEAT -> (int) getTemperature();
                case PROPERTY_ENERGY -> (int) (getEnergy() / 100);
                case PROPERTY_GENERATION -> (int) (generationPlanner.currentGeneration() * 100);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-side delegate; the client syncs into its own SimpleContainerData, so this is unused.
        }

        @Override
        public int getCount() {
            return PROPERTY_COUNT;
        }
    };

    public StirlingEngineBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StirlingEngineBlockEntity entity) {
        EngineEntity.tick(level, pos, state, entity);
    }

    // configure() runs from the EngineEntity super-constructor, before this subclass's field
    // initializers would run — so the fuel slot and PID planner are created here (not as field
    // initializers) to be non-null when the burn component captures them.
    @Override
    protected void configure(EngineBuilder engine) {
        inventory = new ItemInventoryComponent(1, this::setChanged);
        generationPlanner =
                new StirlingGenerationPlanner(PID_KP, PID_KI, PID_KD, TARGET_TEMPERATURE, DEFAULT_MIN_GENERATION);

        energy = engine.energyOutput("energy")
                .capacity(() -> LogisticsConfigHost.get(LogisticsPower.CONFIG.STIRLING_BUFFER_CAPACITY))
                .build();
        heat = engine.heat("heat")
                .energy(energy)
                .canOverheat(true)
                .floor(TEMPERATURE_FLOOR)
                .running(this::isRunning)
                .compression(this::isCompression)
                .build();
        burn = engine.burn("burn")
                .energy(energy)
                .heat(heat)
                .powered(this::isPowered)
                .fuel(fuelSource())
                .generation(pidGeneration())
                .lit(this::setLit)
                .build();
        engine.pistonCycle("cycle")
                .energy(energy)
                .heat(heat)
                .powered(this::isPowered)
                .pistonSpeed(heat::getPistonSpeed)
                .outputPower(this::currentOutputPower)
                .outputFace(() -> StirlingEngineBlock.getOutputDirection(getBlockState()))
                .sendsEnergyContinuously(true)
                .build();
    }

    /** Adapts the PID planner to the generic {@link Generation} strategy. */
    private Generation pidGeneration() {
        return new Generation() {
            @Override
            public long generate(MachineContext ctx, double temperature, long stored, long capacity) {
                return generationPlanner.generate(
                        temperature,
                        stored,
                        capacity,
                        LogisticsConfigHost.get(LogisticsPower.CONFIG.STIRLING_MIN_OUTPUT),
                        LogisticsConfigHost.get(LogisticsPower.CONFIG.STIRLING_MAX_OUTPUT));
            }

            @Override
            public void reset() {
                generationPlanner.reset();
            }

            @Override
            public void save(CompoundTag tag, HolderLookup.Provider registries) {
                tag.putDouble("CurrentGeneration", generationPlanner.currentGeneration());
                tag.putDouble("GenerationCarryover", generationPlanner.generationCarry());
                tag.putDouble("PIDIntegral", generationPlanner.pidIntegral());
            }

            @Override
            public void load(CompoundTag tag, HolderLookup.Provider registries) {
                generationPlanner.restore(
                        NbtCompat.getDouble(tag, "CurrentGeneration", DEFAULT_MIN_GENERATION),
                        NbtCompat.getDouble(tag, "GenerationCarryover", 0.0),
                        NbtCompat.getDouble(tag, "PIDIntegral", 0.0));
            }
        };
    }

    /** Consumes one solid fuel item from the fuel slot and reports its burn duration. */
    private FuelSource fuelSource() {
        return ctx -> {
            Level level = ctx.level();
            if (level == null) {
                return 0;
            }
            ItemStack stack = inventory.getItem(0);
            int burnTicks = FuelHelper.getBurnDuration(level, stack);
            if (burnTicks <= 0) {
                return 0;
            }
            if (stack.is(Items.LAVA_BUCKET)) {
                inventory.setItem(0, new ItemStack(Items.BUCKET));
            } else {
                stack.shrink(1);
            }
            return burnTicks;
        };
    }

    private boolean isCompression() {
        return components.find(com.logistics.core.lib.power.component.EnginePistonCycleComponent.class)
                .map(com.logistics.core.lib.power.component.EnginePistonCycleComponent::isCompression)
                .orElse(false);
    }

    private long currentOutputPower() {
        return generationPlanner.outputPower(
                getTemperature(),
                TEMPERATURE_FLOOR,
                LogisticsConfigHost.get(LogisticsPower.CONFIG.STIRLING_MIN_OUTPUT),
                LogisticsConfigHost.get(LogisticsPower.CONFIG.STIRLING_MAX_OUTPUT));
    }

    private void setLit(MachineContext ctx, boolean lit) {
        Level level = ctx.level();
        if (level == null) {
            return;
        }
        BlockState state = ctx.blockState();
        if (state.hasProperty(StirlingEngineBlock.LIT) && state.getValue(StirlingEngineBlock.LIT) != lit) {
            level.setBlock(ctx.pos(), state.setValue(StirlingEngineBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    // ==================== HasItemStorage (fuel slot, hidden on the output face) ====================

    @Override
    @Nullable
    public IItemStorage itemStorage(@Nullable Direction side) {
        if (side != null && side == StirlingEngineBlock.getOutputDirection(getBlockState())) {
            return null;
        }
        return new IItemStorage() {
            @Override
            public long insert(IItemKey item, long maxAmount, boolean simulate) {
                if (level == null || !FuelHelper.isFuel(level, item.toStack(1))) {
                    return 0;
                }
                ItemStack current = inventory.getItem(0);
                ItemStack template = item.toStack(1);
                long clampedAmount = Math.min(maxAmount, template.getMaxStackSize());
                if (current.isEmpty()) {
                    if (!simulate) inventory.setItem(0, item.toStack((int) clampedAmount));
                    return clampedAmount;
                } else if (ItemStack.isSameItemSameComponents(current, template)) {
                    long canFit = current.getMaxStackSize() - current.getCount();
                    long toInsert = Math.min(maxAmount, canFit);
                    if (toInsert > 0 && !simulate) {
                        current.grow((int) toInsert);
                        inventory.setChanged();
                    }
                    return toInsert;
                }
                return 0;
            }

            @Override
            public long extract(IItemKey item, long maxAmount, boolean simulate) {
                ItemStack current = inventory.getItem(0);
                if (current.isEmpty() || !item.matches(current)) return 0;
                long toExtract = Math.min(maxAmount, current.getCount());
                if (!simulate) inventory.removeItem(0, (int) toExtract);
                return toExtract;
            }

            @Override
            public Iterable<IItemView> contents() {
                ItemStack stack = inventory.getItem(0);
                if (stack.isEmpty()) return java.util.Collections.emptyList();
                IItemKey key = ItemStorageLookup.of(stack);
                IItemView view = new IItemView() {
                    @Override
                    public IItemKey resource() {
                        return key;
                    }

                    @Override
                    public long amount() {
                        return stack.getCount();
                    }
                };
                return java.util.Collections.singletonList(view);
            }
        };
    }

    // ==================== GUI accessors ====================

    public int getBurnTime() {
        return burn.burnTime();
    }

    public int getFuelTime() {
        return burn.fuelTime();
    }

    public ContainerData getPropertyDelegate() {
        return propertyDelegate;
    }

    // ==================== SingleStackInventory ====================

    public ItemStack getTheItem() {
        return inventory.getItem(0);
    }

    public void setTheItem(ItemStack stack) {
        inventory.setItem(0, stack);
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    public boolean isValid(int slot, ItemStack stack) {
        if (level == null) {
            return true;
        }
        return FuelHelper.isFuel(level, stack);
    }

    @Override
    public boolean isEmpty() {
        return inventory.getItem(0).isEmpty();
    }

    // ==================== WorldlyContainer (fuel from every face except output) ====================

    private static final int[] SLOTS_FOR_INPUT = new int[]{0};
    private static final int[] SLOTS_FOR_OUTPUT = new int[]{};

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == StirlingEngineBlock.getOutputDirection(getBlockState()) ? SLOTS_FOR_OUTPUT : SLOTS_FOR_INPUT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (direction == StirlingEngineBlock.getOutputDirection(getBlockState())) {
            return false;
        }
        return isValid(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction != StirlingEngineBlock.getOutputDirection(getBlockState());
    }

    // ==================== Menu ====================

    @Override
    public net.minecraft.world.MenuProvider createMenuProvider() {
        return new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("block.logistics.power.stirling_engine");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new StirlingEngineScreenHandler(
                        syncId, playerInventory, StirlingEngineBlockEntity.this, propertyDelegate);
            }
        };
    }

    // ==================== Persistence (engine components + the BE-local fuel inventory) ====================

    @Override
    protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveLogisticsData(tag, registries);
        inventory.writeNbt(tag, "Inventory", registries);
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadLogisticsData(tag, registries);
        inventory.readNbt(tag, "Inventory", registries);
    }
}

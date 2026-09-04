package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.FluidSupplierModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Screen handler for the Fluid Supplier Pipe GUI.
 *
 * <p>State is read live from the {@link FluidSupplierModule} through {@link ContainerData}: [0] target
 * mB, [1] buffered mB, [2] the filter fluid's registry id (0 = none). {@code ContainerData} syncs as a
 * signed short, so the target is capped at {@link FluidSupplierModule#MAX_TARGET_MB} and the readouts are
 * clamped into range here — display only; the authoritative state stays on the module.
 *
 * <p>Edits arrive from the client via {@link com.logistics.pipe.network.packet.SetFluidSupplierPacket},
 * whose handler resolves the player's open menu and calls {@link #applyFromClient}.
 */
public class FluidSupplierScreenHandler extends AbstractContainerMenu {
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_START_X = 8;

    // Below the target field, two 20px step-button rows, and the mode-button row (see FluidSupplierScreen).
    public static final int INVENTORY_LABEL_Y = 115;
    private static final int PLAYER_INV_START_Y = 125;
    private static final int HOTBAR_Y = 183;
    public static final int PANEL_HEIGHT = HOTBAR_Y + SLOT_SIZE + 6;

    private static final int SHORT_MAX = 32767;

    private static final int DATA_TARGET = 0;
    private static final int DATA_BUFFER = 1;
    private static final int DATA_FLUID_ID = 2;
    private static final int DATA_FULFILLMENT_MODE = 3;
    private static final int DATA_MIN_THRESHOLD = 4;

    private final ContainerLevelAccess context;
    @Nullable private final PipeBlockEntity pipeEntity;
    @Nullable private final String targetModuleStateKey;

    private final int[] syncedData = new int[5];
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (pipeEntity != null) {
                return switch (index) {
                    case DATA_TARGET -> clampShort(readLong(FluidSupplierModule::getTargetMb));
                    case DATA_BUFFER -> clampShort(readLong(FluidSupplierModule::getBufferMb));
                    case DATA_FLUID_ID -> readFluidId();
                    case DATA_FULFILLMENT_MODE -> readInt(FluidSupplierModule::getFulfillmentModeOrdinal);
                    case DATA_MIN_THRESHOLD -> readInt(FluidSupplierModule::getMinThresholdOrdinal);
                    default -> 0;
                };
            }
            return syncedData[index];
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < syncedData.length) syncedData[index] = value;
        }

        @Override
        public int getCount() {
            return syncedData.length;
        }
    };

    public FluidSupplierScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, null, null);
    }

    public FluidSupplierScreenHandler(int syncId, Container playerInventory, @Nullable PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, null);
    }

    public FluidSupplierScreenHandler(
            int syncId, Container playerInventory,
            @Nullable PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        super(LogisticsPipe.SCREEN.FLUID_SUPPLIER, syncId);
        this.pipeEntity = pipeEntity;
        this.targetModuleStateKey = targetModuleStateKey;
        this.context = pipeEntity != null
                ? ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos())
                : ContainerLevelAccess.NULL;
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    private void addPlayerInventorySlots(Container playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        SLOT_START_X + col * SLOT_SIZE, PLAYER_INV_START_Y + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, SLOT_START_X + col * SLOT_SIZE, HOTBAR_Y));
        }
    }

    // ==================== Client-visible synced state ====================

    /** Configured target (mB), clamped to the synced short range. */
    public int getTargetMb() {
        return data.get(DATA_TARGET);
    }

    /** Currently buffered fluid (mB), clamped to the synced short range. */
    public int getBufferMb() {
        return data.get(DATA_BUFFER);
    }

    /** The filter fluid's registry id, or {@code 0} ({@link Fluids#EMPTY}) when none is set. */
    public int getFluidId() {
        return data.get(DATA_FLUID_ID);
    }

    /** Current {@link FulfillmentMode} ordinal (Partial/Full toggle). */
    public int getFulfillmentModeOrdinal() {
        return data.get(DATA_FULFILLMENT_MODE);
    }

    /** Current {@link FluidSupplierModule.MinThreshold} ordinal. */
    public int getMinThresholdOrdinal() {
        return data.get(DATA_MIN_THRESHOLD);
    }

    // ==================== Server-side edits ====================

    /**
     * Apply a client edit: the target amount and the two mode ordinals, always applied together (see
     * {@link com.logistics.pipe.network.packet.SetFluidSupplierPacket}'s "always send full state" note).
     * The fluid filter itself is set/cleared separately — see {@link #onGaugeClicked}.
     *
     * <p>Both ordinals are validated up front — {@code SetFluidSupplierPacket} carries them as raw ints
     * off the wire, and {@code FluidSupplierModule}'s ordinal setters throw on an out-of-range value
     * rather than clamp. Checking first avoids a torn update (target applied, then an exception mid-way
     * through the mode writes) from a malformed or stale packet.
     */
    public void applyFromClient(ServerPlayer player, long targetMb, int fulfillmentModeOrdinal, int minThresholdOrdinal) {
        if (pipeEntity == null) return;
        if (fulfillmentModeOrdinal < 0 || fulfillmentModeOrdinal >= FulfillmentMode.values().length) return;
        if (minThresholdOrdinal < 0 || minThresholdOrdinal >= FluidSupplierModule.MinThreshold.values().length) return;
        PipeModuleHelper.withModule(pipeEntity, FluidSupplierModule.class, targetModuleStateKey, (ctx, module) -> {
            module.setTargetMb(ctx, targetMb);
            module.setFulfillmentModeFromOrdinal(ctx, fulfillmentModeOrdinal);
            module.setMinThresholdFromOrdinal(ctx, minThresholdOrdinal);
        });
        broadcastChanges();
    }

    /**
     * Handle a click on the fluid gauge (see {@link com.logistics.pipe.network.packet.ClickFluidSupplierGaugePacket}).
     * If the player is carrying a bucket-like item that resolves to a fluid, the filter is set or
     * replaced from it — mirroring {@code FluidSupplierModule#onUseWithItem}'s in-world bucket
     * interaction. Otherwise, if a filter is currently set, it is cleared. Both actions are refused
     * (with a message) while the filter is locked (fluid held or on order).
     */
    public void onGaugeClicked(ServerPlayer player) {
        if (pipeEntity == null) return;
        Fluid fromCarried = FluidSupplierModule.fluidFromBucket(getCarried());
        PipeModuleHelper.withModule(pipeEntity, FluidSupplierModule.class, targetModuleStateKey, (ctx, module) -> {
            if (fromCarried != null) {
                if (module.isFilterLocked(ctx)) {
                    player.sendOverlayMessage(Component.translatable("message.logistics.fluid_supplier.locked"));
                } else if (module.getFilterFluid(ctx) != fromCarried) {
                    module.setFilterFluid(ctx, fromCarried);
                    player.sendOverlayMessage(Component.translatable(
                            "message.logistics.fluid_supplier.filter_set", FluidSupplierModule.fluidName(fromCarried)));
                }
            } else if (module.getFilterFluid(ctx) != null) {
                if (module.isFilterLocked(ctx)) {
                    player.sendOverlayMessage(Component.translatable("message.logistics.fluid_supplier.locked"));
                } else {
                    module.setFilterFluid(ctx, null);
                    player.sendOverlayMessage(Component.translatable("message.logistics.fluid_supplier.cleared"));
                }
            }
        });
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return PipeMenuValidity.stillValid(context, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    // ==================== Helpers ====================

    /** Reads a {@code long} state value from the module through a fresh scoped context. */
    private long readLong(java.util.function.ToLongBiFunction<FluidSupplierModule, PipeContext> reader) {
        if (pipeEntity == null) return 0;
        long[] out = {0};
        PipeModuleHelper.withModule(pipeEntity, FluidSupplierModule.class, targetModuleStateKey,
                (ctx, module) -> out[0] = reader.applyAsLong(module, ctx));
        return out[0];
    }

    /** Reads an {@code int} state value from the module through a fresh scoped context. */
    private int readInt(java.util.function.ToIntBiFunction<FluidSupplierModule, PipeContext> reader) {
        if (pipeEntity == null) return 0;
        int[] out = {0};
        PipeModuleHelper.withModule(pipeEntity, FluidSupplierModule.class, targetModuleStateKey,
                (ctx, module) -> out[0] = reader.applyAsInt(module, ctx));
        return out[0];
    }

    private static int clampShort(long value) {
        if (value < 0) return 0;
        return (int) Math.min(SHORT_MAX, value);
    }

    private int readFluidId() {
        if (pipeEntity == null) return 0;
        int[] out = {0};
        PipeModuleHelper.withModule(pipeEntity, FluidSupplierModule.class, targetModuleStateKey, (ctx, module) -> {
            Fluid fluid = module.getFilterFluid(ctx);
            out[0] = fluid == null ? 0 : BuiltInRegistries.FLUID.getId(fluid);
        });
        return out[0];
    }
}

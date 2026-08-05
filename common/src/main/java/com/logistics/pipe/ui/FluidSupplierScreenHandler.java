package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
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
    private static final int PLAYER_INV_START_Y = 84;
    private static final int HOTBAR_Y = 142;

    private static final int SHORT_MAX = 32767;

    private static final int DATA_TARGET = 0;
    private static final int DATA_BUFFER = 1;
    private static final int DATA_FLUID_ID = 2;

    private final ContainerLevelAccess context;
    @Nullable private final PipeBlockEntity pipeEntity;
    @Nullable private final String targetModuleStateKey;

    private final int[] syncedData = new int[3];
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (pipeEntity != null) {
                return switch (index) {
                    case DATA_TARGET -> clampShort(readLong(FluidSupplierModule::getTargetMb));
                    case DATA_BUFFER -> clampShort(readLong(FluidSupplierModule::getBufferMb));
                    case DATA_FLUID_ID -> readFluidId();
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

    // ==================== Server-side edits ====================

    /**
     * Apply a client edit. When {@code clearFluid} is set the filter is cleared unless the module is
     * locked (fluid held or on order), in which case the player is told; otherwise the target is set.
     */
    public void applyFromClient(ServerPlayer player, long targetMb, boolean clearFluid) {
        if (pipeEntity == null) return;
        PipeModuleHelper.withModule(pipeEntity, FluidSupplierModule.class, targetModuleStateKey, (ctx, module) -> {
            if (clearFluid) {
                if (module.isFilterLocked(ctx)) {
                    player.displayClientMessage(
                            Component.translatable("message.logistics.fluid_supplier.locked"), true);
                } else {
                    module.setFilterFluid(ctx, null);
                }
            } else {
                module.setTargetMb(ctx, targetMb);
            }
        });
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return context.evaluate(
                (level, pos) -> player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0 * 64.0,
                true);
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

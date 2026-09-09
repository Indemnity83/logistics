package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.SatelliteModule;
import com.logistics.pipe.network.NetworkRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Screen handler for the Satellite Logistics Pipe GUI.
 * Exposes the current satellite ID (integer 1-512, up to MAX_ID) via ContainerData and handles
 * +/- button clicks to cycle through available IDs, skipping duplicates.
 */
public class SatelliteScreenHandler extends AbstractContainerMenu {
    public static final int MAX_ID = 512;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_START_X = 8;
    private static final int PLAYER_INV_START_Y = 38;
    private static final int HOTBAR_Y = 96;

    private final ContainerLevelAccess context;
    @Nullable private final PipeBlockEntity pipeEntity;
    @Nullable private final String targetModuleStateKey;

    // Synced data: [0] = current satellite ID (0=unset), [1] = boolean flag (0/1) for whether
    // the current ID is taken by another satellite pipe (not a bitmask — MAX_ID exceeds 32 bits)
    private final int[] syncedData = new int[2];
    private final ContainerData satelliteData = new ContainerData() {
        @Override
        public int get(int index) {
            if (pipeEntity != null) {
                if (index == 0) return getCurrentIdFromModule();
                if (index == 1) return isCurrentIdTakenInt();
            }
            return syncedData[index];
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < 2) syncedData[index] = value;
        }

        @Override
        public int getCount() { return 2; }
    };

    public SatelliteScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, null, null);
    }

    public SatelliteScreenHandler(int syncId, Container playerInventory,
            @Nullable PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, null);
    }

    public SatelliteScreenHandler(int syncId, Container playerInventory,
            @Nullable PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        super(LogisticsPipe.SCREEN.SATELLITE, syncId);
        this.pipeEntity = pipeEntity;
        this.targetModuleStateKey = targetModuleStateKey;
        this.context = pipeEntity != null
                ? ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos())
                : ContainerLevelAccess.NULL;
        addPlayerInventorySlots(playerInventory);
        addDataSlots(satelliteData);
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

    /** button 0 = decrement, button 1 = increment; skips taken IDs. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (pipeEntity == null) return false;

        int currentId = getCurrentIdFromModule();
        Set<String> taken = getNetworkTakenStrings();

        int nextId = -1;
        if (button == 0) { // decrement
            int candidate = currentId;
            for (int i = 0; i < MAX_ID; i++) {
                candidate = (candidate <= 1) ? MAX_ID : candidate - 1;
                if (!taken.contains(String.valueOf(candidate))) {
                    nextId = candidate;
                    break;
                }
            }
        } else if (button == 1) { // increment
            int candidate = currentId;
            for (int i = 0; i < MAX_ID; i++) {
                candidate = (candidate >= MAX_ID) ? 1 : candidate + 1;
                if (!taken.contains(String.valueOf(candidate))) {
                    nextId = candidate;
                    break;
                }
            }
        }

        if (nextId > 0 && nextId != currentId) {
            int finalId = nextId;
            PipeModuleHelper.withModule(pipeEntity, SatelliteModule.class, targetModuleStateKey, (ctx, module) ->
                    module.setSatelliteId(ctx, finalId));
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return PipeMenuValidity.stillValid(context, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    public int getCurrentId() { return satelliteData.get(0); }

    /** Returns true if the current ID is taken by another satellite pipe. */
    public boolean isCurrentIdTaken() {
        return satelliteData.get(1) == 1;
    }

    private int getCurrentIdFromModule() {
        if (pipeEntity == null) return 0;
        int[] result = {0};
        PipeModuleHelper.withModule(pipeEntity, SatelliteModule.class, targetModuleStateKey, (ctx, module) ->
                result[0] = module.getSatelliteIdInt(ctx));
        return result[0];
    }

    /** Returns satellite IDs taken by OTHER pipes (not this one). */
    private Set<String> getNetworkTakenStrings() {
        if (pipeEntity == null) return Set.of();
        String[] ownId = {""};
        PipeModuleHelper.withModule(pipeEntity, SatelliteModule.class, targetModuleStateKey, (ctx, module) ->
                ownId[0] = module.getSatelliteId(ctx));
        var network = NetworkRegistry.getNetwork(pipeEntity.getLevel(), pipeEntity.getBlockPos());
        if (network == null) return Set.of();
        Set<String> taken = new HashSet<>(network.getAvailableSatelliteIds());
        // Only exclude ownId if this pipe is the confirmed owner in the network
        if (!ownId[0].isEmpty() && pipeEntity.getBlockPos().equals(network.findSatellite(ownId[0]))) {
            taken.remove(ownId[0]);
        }
        return taken;
    }

    /** Returns 1 if the current ID is taken by another satellite pipe, 0 otherwise. */
    private int isCurrentIdTakenInt() {
        int currentId = getCurrentIdFromModule();
        if (currentId <= 0) return 0;
        return getNetworkTakenStrings().contains(String.valueOf(currentId)) ? 1 : 0;
    }
}

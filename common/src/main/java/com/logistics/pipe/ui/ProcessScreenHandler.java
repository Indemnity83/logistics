package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ProcessModule;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.pipe.network.NetworkRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen handler for the Process Logistics Pipe GUI.
 *
 * <p>Ghost slots 0-8: inputs (row). Ghost slot 9: output.
 * ContainerData[0-8]: input counts. [9]: output count. [10]: active. [11]: satellite id. [12]: satellite registered (1=yes).
 * Button 0 = satellite prev, button 1 = satellite next (cycles through Off + registered IDs only).
 */
public class ProcessScreenHandler extends CustomSlotScreenHandler {
    private static final int INPUT_SLOTS = ProcessModule.MAX_INPUTS;   // 9
    private static final int OUTPUT_SLOTS = ProcessModule.MAX_OUTPUTS; // 1
    private static final int TOTAL_GHOST_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;

    private static final int DATA_INPUT_COUNT_0 = 0;          // indices 0-8
    private static final int DATA_OUTPUT_COUNT = INPUT_SLOTS;  // index 9
    private static final int DATA_ACTIVE = INPUT_SLOTS + 1;    // index 10
    private static final int DATA_SAT_ID = INPUT_SLOTS + 2;    // index 11
    private static final int DATA_SAT_TAKEN = INPUT_SLOTS + 3; // index 12
    private static final int DATA_SIZE = INPUT_SLOTS + 4;      // 13

    static final int BUTTON_PREV = 0;
    static final int BUTTON_NEXT = 1;

    // Input row: 9 slots in a single row at (8,18), 18px spacing
    private static final int GRID_X = 8;
    private static final int GRID_Y = 18;
    private static final int SLOT_SIZE = 18;
    // Output slot: centered in 24px box at (88,54)
    private static final int OUTPUT_SLOT_X = 88;
    private static final int OUTPUT_SLOT_Y = 48;
    // Player inventory
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 92;
    private static final int HOTBAR_Y = 150;

    private final ProcessInventory processInventory;
    private final ContainerLevelAccess context;
    private final ContainerData data;
    @Nullable private final PipeBlockEntity pipeEntity;
    @Nullable private final String targetModuleStateKey;

    public ProcessScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, null);
    }

    public ProcessScreenHandler(int syncId, Container playerInventory,
            @Nullable PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, null);
    }

    public ProcessScreenHandler(int syncId, Container playerInventory,
            @Nullable PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        super(LogisticsPipe.SCREEN.PROCESS, syncId);
        this.pipeEntity = pipeEntity;
        this.targetModuleStateKey = targetModuleStateKey;
        this.processInventory = new ProcessInventory(pipeEntity, targetModuleStateKey);
        this.data = new SimpleContainerData(DATA_SIZE);

        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());
            PipeModuleHelper.withModule(pipeEntity, ProcessModule.class, targetModuleStateKey, (ctx, module) -> {
                for (int i = 0; i < INPUT_SLOTS; i++) {
                    data.set(DATA_INPUT_COUNT_0 + i, module.getInputCount(ctx, i));
                }
                data.set(DATA_OUTPUT_COUNT, module.getOutputCount(ctx, 0));
                data.set(DATA_ACTIVE, module.isActive(ctx) ? 1 : 0);
                data.set(DATA_SAT_ID, module.getInputSatelliteId(ctx));
            });
            // Init satellite-registered flag (outside module lambda since we need the network)
            int satId = data.get(DATA_SAT_ID);
            if (satId > 0 && pipeEntity.getLevel() != null) {
                ILogisticsNetwork network = NetworkRegistry.getNetwork(pipeEntity.getLevel(), pipeEntity.getBlockPos());
                data.set(DATA_SAT_TAKEN, network != null && network.findSatellite(String.valueOf(satId)) != null ? 1 : 0);
            }
        } else {
            this.context = ContainerLevelAccess.NULL;
        }

        addGhostSlots();
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    private void addGhostSlots() {
        // 9 input slots in a single row
        for (int i = 0; i < INPUT_SLOTS; i++) {
            addSlot(new GhostSlot(processInventory, i, GRID_X + i * SLOT_SIZE, GRID_Y));
        }
        // Single output slot (centered in 24px box at 84,50)
        addSlot(new GhostSlot(processInventory, INPUT_SLOTS, OUTPUT_SLOT_X, OUTPUT_SLOT_Y));
    }

    private void addPlayerInventorySlots(Container playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_INV_X + col * SLOT_SIZE, PLAYER_INV_Y + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * SLOT_SIZE, HOTBAR_Y));
        }
    }

    /**
     * Button 0 = satellite prev, button 1 = satellite next.
     * Cycles through: Off (0) + IDs currently registered in the network, in order.
     */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (pipeEntity == null) return false;
        // The button id is an unconstrained int straight off the wire.
        if (button != BUTTON_PREV && button != BUTTON_NEXT) return false;

        int currentId = getCurrentSatelliteIdFromModule();
        List<Integer> available = getAvailableSatelliteIds();

        int nextId = resolveSatelliteId(available, currentId, button);
        if (nextId != currentId) {
            PipeModuleHelper.withModule(pipeEntity, ProcessModule.class, targetModuleStateKey, (ctx, module) ->
                    module.setInputSatelliteId(ctx, nextId));
        }
        broadcastChanges();
        return true;
    }

    /**
     * Satellite id that a prev/next press selects from {@code available}, or {@code currentId}
     * for any other button. {@code indexOf} misses when the configured satellite is no longer
     * registered, so the index is clamped rather than passed through to {@code get}.
     */
    static int resolveSatelliteId(List<Integer> available, int currentId, int button) {
        if (available.isEmpty()) return currentId;

        int idx = available.indexOf(currentId);
        if (button == BUTTON_PREV) {
            idx = (idx <= 0) ? available.size() - 1 : idx - 1;
        } else if (button == BUTTON_NEXT) {
            idx = (idx >= available.size() - 1) ? 0 : idx + 1;
        } else {
            return currentId;
        }
        return available.get(Math.max(idx, 0));
    }

    @Override
    public boolean stillValid(Player player) {
        return PipeMenuValidity.stillValid(context, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean handleCustomSlotClick(int slotIndex, int button, boolean quickMove, Player player) {
        if (slotIndex >= 0 && slotIndex < TOTAL_GHOST_SLOTS) {
            if (quickMove) return true;

            ItemStack cursor = getCarried();
            boolean isInput = slotIndex < INPUT_SLOTS;
            int moduleSlot = isInput ? slotIndex : slotIndex - INPUT_SLOTS;

            if (button == 1 || cursor.isEmpty()) {
                // Right-click or empty cursor: clear slot
                processInventory.setItem(slotIndex, ItemStack.EMPTY);
                context.execute((level, pos) -> PipeModuleHelper.withModule(
                        level.getBlockEntity(pos) instanceof PipeBlockEntity e ? e : null,
                        ProcessModule.class,
                        targetModuleStateKey,
                        (ctx, module) -> {
                            if (isInput) module.setInput(ctx, moduleSlot, "", 1, "");
                            else module.setOutput(ctx, moduleSlot, "", 1);
                        }));
                broadcastChanges();
                return true;
            }

            // Left-click with item: place ghost item
            String itemId = BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString();
            int count = cursor.getCount();
            processInventory.setItem(slotIndex, cursor.copyWithCount(count));

            context.execute((level, pos) -> PipeModuleHelper.withModule(
                    level.getBlockEntity(pos) instanceof PipeBlockEntity e ? e : null,
                    ProcessModule.class,
                    targetModuleStateKey,
                    (ctx, module) -> {
                        if (isInput) {
                            String currentDest = module.getInputDest(ctx, moduleSlot);
                            module.setInput(ctx, moduleSlot, itemId, count, currentDest);
                        } else {
                            module.setOutput(ctx, moduleSlot, itemId, count);
                        }
                    }));
            broadcastChanges();
            return true;
        }

        return false;
    }

    @Override
    public void broadcastChanges() {
        context.execute((level, pos) -> {
            int[] satIdHolder = {0};
            PipeModuleHelper.withModule(
                    level.getBlockEntity(pos) instanceof PipeBlockEntity e ? e : null,
                    ProcessModule.class,
                    targetModuleStateKey,
                    (ctx, module) -> {
                        for (int i = 0; i < INPUT_SLOTS; i++) {
                            data.set(DATA_INPUT_COUNT_0 + i, module.getInputCount(ctx, i));
                        }
                        data.set(DATA_OUTPUT_COUNT, module.getOutputCount(ctx, 0));
                        data.set(DATA_ACTIVE, module.isActive(ctx) ? 1 : 0);
                        satIdHolder[0] = module.getInputSatelliteId(ctx);
                        data.set(DATA_SAT_ID, satIdHolder[0]);
                    });
            int satId = satIdHolder[0];
            if (satId > 0) {
                ILogisticsNetwork network = NetworkRegistry.getNetwork(level, pos);
                boolean registered = network != null && network.findSatellite(String.valueOf(satId)) != null;
                data.set(DATA_SAT_TAKEN, registered ? 1 : 0);
            } else {
                data.set(DATA_SAT_TAKEN, 0);
            }
        });
        super.broadcastChanges();
    }

    public boolean isActive() { return data.get(DATA_ACTIVE) == 1; }
    public int getCurrentSatelliteId() { return data.get(DATA_SAT_ID); }
    public boolean isSatelliteRegistered() { return data.get(DATA_SAT_TAKEN) == 1; }

    private int getCurrentSatelliteIdFromModule() {
        if (pipeEntity == null) return 0;
        int[] result = {0};
        PipeModuleHelper.withModule(pipeEntity, ProcessModule.class, targetModuleStateKey, (ctx, module) ->
                result[0] = module.getInputSatelliteId(ctx));
        return result[0];
    }

    /** Returns [0 (Off), ...registered satellite IDs sorted...]. */
    private List<Integer> getAvailableSatelliteIds() {
        List<Integer> ids = new ArrayList<>();
        ids.add(0); // Off
        if (pipeEntity != null && pipeEntity.getLevel() != null) {
            ILogisticsNetwork network = NetworkRegistry.getNetwork(pipeEntity.getLevel(), pipeEntity.getBlockPos());
            if (network != null) {
                network.getAvailableSatelliteIds().stream()
                        .map(s -> { try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; } })
                        .filter(i -> i > 0)
                        .sorted()
                        .forEach(ids::add);
            }
        }
        return ids;
    }

    private static class GhostSlot extends Slot {
        GhostSlot(Container inv, int index, int x, int y) { super(inv, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }
}

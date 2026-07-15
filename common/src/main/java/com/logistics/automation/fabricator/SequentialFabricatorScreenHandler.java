package com.logistics.automation.fabricator;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.fabricator.FabricatorProcessorComponent.Output;
import com.logistics.core.lib.platform.ServerNetworking;
import com.logistics.core.machine.MachineData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for the Sequential Fabricator: 12 real input slots plus a client-synced list of craftable
 * outputs (rendered as selection widgets, not container slots — the product is ejected, never stored).
 * Progress and energy sync via {@link ContainerData}; the output list syncs via
 * {@link SyncFabricatorOutputsPacket} to the viewing player whenever it changes.
 */
public class SequentialFabricatorScreenHandler extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = SequentialFabricatorBlockEntity.INPUT_SLOTS;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;

    private final Container inventory;
    private final ContainerData data;

    // Server-side sync state.
    @Nullable private final SequentialFabricatorBlockEntity blockEntity;
    @Nullable private final ServerPlayer viewer;
    private BlockPos pos = BlockPos.ZERO;
    private String lastSignature = "";

    // Client-side synced outputs.
    private List<Output> clientOutputs = new ArrayList<>();

    /** Client-side constructor. */
    public SequentialFabricatorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(MachineData.COUNT), null);
    }

    /** Server-side constructor. */
    public SequentialFabricatorScreenHandler(
            int syncId, Inventory playerInventory, SequentialFabricatorBlockEntity blockEntity, ContainerData data) {
        this(syncId, playerInventory, blockEntity, data, blockEntity);
    }

    private SequentialFabricatorScreenHandler(
            int syncId,
            Inventory playerInventory,
            Container inventory,
            ContainerData data,
            @Nullable SequentialFabricatorBlockEntity blockEntity) {
        super(LogisticsAutomation.MENU.SEQUENTIAL_FABRICATOR, syncId);
        checkContainerSize(inventory, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, MachineData.COUNT);

        this.inventory = inventory;
        this.data = data;
        this.blockEntity = blockEntity;
        this.viewer = playerInventory.player instanceof ServerPlayer sp ? sp : null;
        if (blockEntity != null) {
            this.pos = blockEntity.getBlockPos();
        }

        inventory.startOpen(playerInventory.player);

        // Input material pool: 3 columns × 4 rows (positions match the GUI texture).
        for (int i = 0; i < MACHINE_SLOT_COUNT; i++) {
            int col = i % 3;
            int row = i / 3;
            this.addSlot(new Slot(inventory, i, 38 + col * 18, 17 + row * 18));
        }

        // Player inventory (3 rows of 9).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        // Player hotbar.
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
        }

        this.addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < PLAYER_INVENTORY_START) {
                // Machine slot -> player inventory.
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory -> machine input slots.
                if (!this.moveItemStackTo(stack, 0, MACHINE_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity == null || viewer == null) {
            return;
        }
        List<Output> outputs = blockEntity.currentOutputs();
        String signature = signature(outputs);
        if (signature.equals(lastSignature)) {
            return;
        }
        lastSignature = signature;

        List<String> ids = new ArrayList<>(outputs.size());
        List<ItemStack> results = new ArrayList<>(outputs.size());
        List<Integer> states = new ArrayList<>(outputs.size());
        for (Output output : outputs) {
            ids.add(output.id().toString());
            results.add(output.result());
            states.add(output.state());
        }
        ServerNetworking.send(viewer, new SyncFabricatorOutputsPacket(pos, ids, results, states));
    }

    private static String signature(List<Output> outputs) {
        StringBuilder sb = new StringBuilder();
        for (Output output : outputs) {
            sb.append(output.id()).append(':').append(output.state()).append(';');
        }
        return sb.toString();
    }

    // ----- client-side accessors -----

    public BlockPos getPos() {
        return pos;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public List<Output> getClientOutputs() {
        return clientOutputs;
    }

    public void setClientOutputs(List<Output> outputs) {
        this.clientOutputs = outputs;
    }

    /** Progress arrow width (0..24 px) from the synced progress fraction. */
    public int getProgressArrowWidth() {
        return MachineData.barPixels(data, MachineData.PROGRESS, 24);
    }

    /** Energy bar height (0..30 px) from the synced energy fill fraction. */
    public int getEnergyBarHeight() {
        return MachineData.barPixels(data, MachineData.ENERGY, 30);
    }
}

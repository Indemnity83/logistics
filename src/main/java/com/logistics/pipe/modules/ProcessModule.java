package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.RoutingModule;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.*;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.core.lib.network.ProviderCanFulfill;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.ui.ProcessScreenHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Process pipe module — presents machine outputs as available to the network,
 * sources inputs from the network (routing them to satellite pipes or self),
 * and extracts outputs from the adjacent machine once they appear.
 *
 * <p>V1 scope: up to 9 inputs (MAX_INPUTS), up to 1 output (MAX_OUTPUTS), fixed items only (no tags).
 * Multiple concurrent orders are queued and processed in FIFO order.
 */
public class ProcessModule implements Module, TickingModule, RoutingModule, DispatchableModule {
    // NBT keys — process config (public for screen handler access)
    public static final String KEY_INPUTS = "inputs";
    public static final String KEY_OUTPUTS = "outputs";
    // Per-entry keys in inputs/outputs lists
    public static final String ENTRY_ITEM = "item";
    public static final String ENTRY_COUNT = "count";
    public static final String ENTRY_DEST = "dest"; // inputs only: "" = local, else satellite ID
    // NBT key — order queue (ListTag of CompoundTag entries)
    private static final String QUEUE = "queue";
    private static final String ENTRY_REQ = "req";         // requester "x,y,z"
    private static final String ENTRY_DLV = "dlv";         // delivery UUID string
    private static final String ENTRY_EXEC = "exec";       // executions promised (long)
    private static final String ENTRY_EXTR = "extr";       // outputs extracted so far (long)
    private static final String ENTRY_ORDERS = "orders";   // ListTag<StringTag> of pending input order UUIDs
    private static final String ENTRY_REQUESTED = "req_amount"; // original requested amount
    // Per-entry snapshot of output config (persisted at dispatch time to avoid config-change issues)
    private static final String ENTRY_OUTPUT_ITEM = "output_item"; // item ID of the output
    private static final String ENTRY_OUTPUT_COUNT = "output_count"; // count per execution
    // Per-entry extraction tracking
    private static final String ENTRY_EXTR_SLOT = "extr_0"; // items extracted from output slot 0
    private static final String ENTRY_EXTR_REQ = "extr_req"; // total items sent to requester
    // Global satellite destination for all inputs (0 = local/self)
    private static final String KEY_INPUT_SATELLITE = "input_satellite";
    // Timing
    private static final String TICKS_SCAN = "ticks_scan";
    private static final String TICKS_EXTRACT = "ticks_extract";
    private static final int SCAN_INTERVAL = 20;
    private static final int EXTRACT_INTERVAL = 6;
    // Priority: between Provider (1) and Crafter (5)
    static final int PROCESS_PRIORITY = 3;
    // Maximum number of queued orders (soft cap to prevent unbounded growth)
    private static final int MAX_QUEUE_SIZE = 64;

    // ==================== Config NBT Accessors ====================

    public String getInputItem(PipeContext ctx, int slot) {
        return getEntryString(ctx, KEY_INPUTS, slot, ENTRY_ITEM);
    }

    public int getInputCount(PipeContext ctx, int slot) {
        return getEntryInt(ctx, KEY_INPUTS, slot, ENTRY_COUNT, 1);
    }

    public String getInputDest(PipeContext ctx, int slot) {
        return getEntryString(ctx, KEY_INPUTS, slot, ENTRY_DEST);
    }

    public int getInputSatelliteId(PipeContext ctx) {
        return ctx.getInt(this, KEY_INPUT_SATELLITE, 0);
    }

    public void setInputSatelliteId(PipeContext ctx, int id) {
        ctx.saveInt(this, KEY_INPUT_SATELLITE, Math.max(0, id));
        ctx.markDirtyAndSync();
    }

    public void setInput(PipeContext ctx, int slot, String itemId, int count, String dest) {
        setEntry(ctx, KEY_INPUTS, slot, itemId, count, dest);
    }

    public String getOutputItem(PipeContext ctx, int slot) {
        return getEntryString(ctx, KEY_OUTPUTS, slot, ENTRY_ITEM);
    }

    public int getOutputCount(PipeContext ctx, int slot) {
        return getEntryInt(ctx, KEY_OUTPUTS, slot, ENTRY_COUNT, 1);
    }

    public void setOutput(PipeContext ctx, int slot, String itemId, int count) {
        setEntry(ctx, KEY_OUTPUTS, slot, itemId, count, null);
    }

    public boolean isActive(PipeContext ctx) {
        return !getQueue(ctx).isEmpty();
    }

    // ==================== Queue Helpers ====================

    private ListTag getQueue(PipeContext ctx) {
        CompoundTag state = ctx.moduleState(getStateKey());
        ListTag queue = NbtCompat.getListOrEmpty(state, QUEUE);
        if (!queue.isEmpty()) return queue;

        // One-shot migration: convert a legacy "active_job" CompoundTag into a queue entry
        if (!state.contains("active_job")) return queue;
        CompoundTag legacy = NbtCompat.getCompoundOrEmpty(state, "active_job");

        CompoundTag migrated = new CompoundTag();
        migrated.putString(ENTRY_REQ, NbtCompat.getString(legacy, "req", ""));
        String legacyDlv = NbtCompat.getString(legacy, "dlv", "");
        if (!legacyDlv.isEmpty()) migrated.putString(ENTRY_DLV, legacyDlv);
        migrated.putLong(ENTRY_EXEC, NbtCompat.getLong(legacy, "exec", 0));
        migrated.putLong(ENTRY_EXTR, NbtCompat.getLong(legacy, "extr", 0));
        migrated.putLong(ENTRY_REQUESTED, NbtCompat.getLong(legacy, "req_amount", 0));
        migrated.put(ENTRY_ORDERS, NbtCompat.getListOrEmpty(legacy, "orders"));
        // Populate output snapshot from current config (best-effort; may be empty if config changed)
        String outputItem = getOutputItem(ctx, 0);
        migrated.putString(ENTRY_OUTPUT_ITEM, outputItem);
        migrated.putInt(ENTRY_OUTPUT_COUNT, outputItem.isEmpty() ? 0 : getOutputCount(ctx, 0));

        queue = new ListTag();
        queue.add(migrated);
        state.put(QUEUE, queue);
        state.remove("active_job");
        ctx.markDirtyAndSync();
        return queue;
    }

    private void saveQueue(PipeContext ctx, ListTag queue) {
        ctx.moduleState(getStateKey()).put(QUEUE, queue);
        ctx.markDirtyAndSync();
    }

    // ==================== Private NBT Helpers ====================

    private String getEntryString(PipeContext ctx, String listKey, int slot, String field) {
        ListTag list = NbtCompat.getListOrEmpty(ctx.moduleState(getStateKey()), listKey);
        if (slot < 0 || slot >= list.size()) return "";
        return list.getCompound(slot)
                .map(e -> NbtCompat.getString(e, field, ""))
                .orElse("");
    }

    private int getEntryInt(PipeContext ctx, String listKey, int slot, String field, int def) {
        ListTag list = NbtCompat.getListOrEmpty(ctx.moduleState(getStateKey()), listKey);
        if (slot < 0 || slot >= list.size()) return def;
        return list.getCompound(slot)
                .map(e -> NbtCompat.getInt(e, field, def))
                .orElse(def);
    }

    private void setEntry(PipeContext ctx, String listKey, int slot, String itemId, int count,
            @Nullable String dest) {
        int maxSlot = KEY_INPUTS.equals(listKey) ? MAX_INPUTS : MAX_OUTPUTS;
        if (slot < 0 || slot >= maxSlot) return;
        ListTag list = NbtCompat.getListOrEmpty(ctx.moduleState(getStateKey()), listKey);

        // Expand list to required size
        while (list.size() <= slot) {
            list.add(new CompoundTag());
        }

        if (itemId == null || itemId.isEmpty()) {
            // Clear this slot and trim trailing empty entries
            list.set(slot, new CompoundTag());
        } else {
            CompoundTag entry = new CompoundTag();
            entry.putString(ENTRY_ITEM, itemId);
            entry.putInt(ENTRY_COUNT, Math.max(1, count));
            if (dest != null) {
                entry.putString(ENTRY_DEST, dest);
            }
            list.set(slot, entry);
        }

        // Remove trailing empty entries
        while (!list.isEmpty()) {
            CompoundTag last = list.getCompound(list.size() - 1).orElse(new CompoundTag());
            if (!last.contains(ENTRY_ITEM) || NbtCompat.getString(last, ENTRY_ITEM, "").isEmpty()) {
                list.remove(list.size() - 1);
            } else {
                break;
            }
        }

        ctx.moduleState(getStateKey()).put(listKey, list);
        ctx.markDirtyAndSync();
    }

    // ==================== Module Interface ====================

    @Override
    public void onTick(PipeContext ctx) {
        int ticks = ctx.getInt(this, TICKS_SCAN, 0) + 1;
        ctx.saveInt(this, TICKS_SCAN, ticks);
        if (ticks >= SCAN_INTERVAL) {
            ctx.saveInt(this, TICKS_SCAN, 0);
            updateProcessSupply(ctx);
        }

        // If there's an active job: try to extract outputs every 6 ticks
        if (isActive(ctx)) {
            int extractTicks = ctx.getInt(this, TICKS_EXTRACT, 0) + 1;
            ctx.saveInt(this, TICKS_EXTRACT, extractTicks);
            if (extractTicks >= EXTRACT_INTERVAL) {
                ctx.saveInt(this, TICKS_EXTRACT, 0);
                collectOutputs(ctx);
            }
        }
    }

    @Override
    public InteractionResult onWrench(PipeContext ctx, Player player) {
        if (ctx.world().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        Level world = ctx.world();
        BlockPos pos = ctx.pos();
        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ProcessScreenHandler(syncId, inv,
                        world.getBlockEntity(pos) instanceof PipeBlockEntity entity ? entity : null),
                Component.translatable("screen.logistics.process")));
        return InteractionResult.SUCCESS;
    }

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        // Intercept items destined for this pipe (local inputs for the active job)
        if (item.getDestination() != null && item.getDestination().equals(ctx.pos())) {
            List<Direction> inventoryFaces = ctx.getInventoryConnections();
            for (Direction d : inventoryFaces) {
                if (options.contains(d)) return RoutePlan.reroute(d);
            }
        }
        return RoutePlan.pass();
    }

    @Override
    public long onDispatch(PipeContext ctx, BlockPos requester, ItemVariant item, long amount, UUID deliveryId) {
        // Find which output matches the requested item
        int matchedOutput = -1;
        for (int i = 0; i < MAX_OUTPUTS; i++) {
            String outId = getOutputItem(ctx, i);
            if (outId.isEmpty()) continue;
            ItemStack outStack = resolveItem(outId);
            if (!outStack.isEmpty() && item.matches(outStack)) {
                matchedOutput = i;
                break;
            }
        }
        if (matchedOutput < 0) return 0;

        int outCount = getOutputCount(ctx, matchedOutput);
        if (outCount <= 0) return 0;

        long executions = (amount + outCount - 1) / outCount; // ceil division
        // Cap to the originally-requested amount: the machine may produce more per batch than
        // was ordered. Excess is routed to a sink rather than force-routed to the requester.
        long actualAmount = Math.min(amount, executions * outCount);

        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return 0;

        ListTag queue = getQueue(ctx);
        if (queue.size() >= MAX_QUEUE_SIZE) {
            LogisticsPipe.LOGGER.warn("[Process @ {}] Queue full ({} entries); rejecting dispatch for '{}'", ctx.pos(), queue.size(), getOutputItem(ctx, matchedOutput));
            return 0;
        }

        int satId = getInputSatelliteId(ctx);
        String globalDest = satId > 0 ? String.valueOf(satId) : "";

        // Place orders for each input
        ListTag orderIds = new ListTag();
        for (int i = 0; i < MAX_INPUTS; i++) {
            String inputItem = getInputItem(ctx, i);
            if (inputItem.isEmpty()) continue;
            int inputCount = getInputCount(ctx, i);

            ItemStack inputStack = resolveItem(inputItem);
            if (inputStack.isEmpty()) continue;

            ItemVariant inputVariant = ItemVariant.of(inputStack);
            long needed = executions * inputCount;

            // Resolve destination: per-input dest first, then global, then self
            String perInputDest = getInputDest(ctx, i);
            String effectiveDest = !perInputDest.isEmpty() ? perInputDest : globalDest;
            BlockPos orderDest;
            if (effectiveDest.isEmpty()) {
                orderDest = ctx.pos(); // local: arrive at this pipe, then route to machine
            } else {
                orderDest = network.findSatellite(effectiveDest);
                if (orderDest == null) {
                    LogisticsPipe.LOGGER.warn("[Process @ {}] Satellite '{}' not found for input {}; aborting dispatch", ctx.pos(), effectiveDest, i);
                    return 0;
                }
            }

            UUID orderId = network.placeOrder(inputVariant, needed, orderDest);
            orderIds.add(StringTag.valueOf(orderId.toString()));
        }

        // Append entry to queue with snapshot of output config
        CompoundTag entry = new CompoundTag();
        entry.putString(ENTRY_REQ, requester.getX() + "," + requester.getY() + "," + requester.getZ());
        if (deliveryId != null) entry.putString(ENTRY_DLV, deliveryId.toString());
        entry.putLong(ENTRY_EXEC, executions);
        entry.putLong(ENTRY_EXTR, 0L);
        entry.putLong(ENTRY_REQUESTED, amount);
        entry.put(ENTRY_ORDERS, orderIds);
        // Persist output config snapshot to avoid config-change issues
        entry.putString(ENTRY_OUTPUT_ITEM, getOutputItem(ctx, matchedOutput));
        entry.putInt(ENTRY_OUTPUT_COUNT, outCount);

        queue.add(entry);
        saveQueue(ctx, queue);

        return actualAmount;
    }

    @Override
    public void onDetach(PipeContext ctx) {
        cancelAllJobs(ctx);
        ILogisticsNetwork network = ctx.network();
        if (network != null) {
            network.registerSupply(ctx.pos(), Map.of(), PROCESS_PRIORITY);
            network.unregisterProviderCheck(ctx.pos());
        }
    }

    // ==================== Supply Registration ====================

    private void updateProcessSupply(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return;

        // Check if any outputs are configured
        boolean anyOutput = false;
        for (int i = 0; i < MAX_OUTPUTS; i++) {
            if (!getOutputItem(ctx, i).isEmpty()) { anyOutput = true; break; }
        }

        if (!anyOutput) {
            network.registerSupply(ctx.pos(), Map.of(), PROCESS_PRIORITY);
            network.unregisterProviderCheck(ctx.pos());
            return;
        }

        // Check global satellite destination exists
        int satId = getInputSatelliteId(ctx);
        if (satId > 0 && network.findSatellite(String.valueOf(satId)) == null) {
            network.registerSupply(ctx.pos(), Map.of(), PROCESS_PRIORITY);
            network.unregisterProviderCheck(ctx.pos());
            return;
        }

        // Advertise 0 — signals "produceable on demand" (like CraftingModule)
        // The network will validate ingredients via buildProviderCheck before dispatching.
        // Supply is always advertised even while jobs are queued, so additional orders can arrive.
        Map<ItemVariant, Long> supply = new HashMap<>();
        for (int i = 0; i < MAX_OUTPUTS; i++) {
            String outId = getOutputItem(ctx, i);
            if (outId.isEmpty()) continue;
            ItemStack outStack = resolveItem(outId);
            if (!outStack.isEmpty()) {
                supply.put(ItemVariant.of(outStack), 0L);
            }
        }

        network.registerSupply(ctx.pos(), supply, PROCESS_PRIORITY);

        // Register ingredient-chain validator
        network.registerProviderCheck(ctx.pos(), buildProviderCheck(ctx));
    }

    private ProviderCanFulfill buildProviderCheck(PipeContext ctx) {
        return (amount, checker) -> {
            // Compute executions from first configured output count
            int outCount = 0;
            for (int i = 0; i < MAX_OUTPUTS; i++) {
                if (!getOutputItem(ctx, i).isEmpty()) {
                    outCount = Math.max(1, getOutputCount(ctx, i));
                    break;
                }
            }
            if (outCount <= 0) return List.of();

            long executions = (amount + outCount - 1) / outCount;

            // Check each input can be satisfied
            for (int i = 0; i < MAX_INPUTS; i++) {
                String inputId = getInputItem(ctx, i);
                if (inputId.isEmpty()) continue;
                int inputCount = getInputCount(ctx, i);
                ItemStack inputStack = resolveItem(inputId);
                if (inputStack.isEmpty()) continue;
                List<ItemVariant> missing = checker.check(inputStack, executions * inputCount);
                if (!missing.isEmpty()) return missing;
            }
            return List.of();
        };
    }

    // ==================== Output Collection ====================

    private void collectOutputs(PipeContext ctx) {
        ListTag queue = getQueue(ctx);
        if (queue.isEmpty()) return;

        CompoundTag entry = queue.getCompound(0).orElse(null);
        if (entry == null) {
            queue.remove(0);
            saveQueue(ctx, queue);
            return;
        }

        BlockPos requester = parseBlockPos(NbtCompat.getString(entry, ENTRY_REQ, ""));
        if (requester == null) {
            // Cancel reserved orders before removing entry
            ILogisticsNetwork network = ctx.network();
            if (network != null) cancelEntryOrders(network, entry);
            queue.remove(0);
            saveQueue(ctx, queue);
            return;
        }

        long executions = NbtCompat.getLong(entry, ENTRY_EXEC, 0);
        long extracted = NbtCompat.getLong(entry, ENTRY_EXTR, 0);

        // Use snapshot of output config from when entry was created
        String outputItem = NbtCompat.getString(entry, ENTRY_OUTPUT_ITEM, "");
        int outputCount = NbtCompat.getInt(entry, ENTRY_OUTPUT_COUNT, 0);

        if (outputItem.isEmpty() || outputCount <= 0) {
            // Cancel reserved orders before removing entry
            ILogisticsNetwork network = ctx.network();
            if (network != null) cancelEntryOrders(network, entry);
            queue.remove(0);
            saveQueue(ctx, queue);
            return;
        }

        long totalOutputsExpected = executions * outputCount;

        long requested = NbtCompat.getLong(entry, ENTRY_REQUESTED, totalOutputsExpected);
        long totalSentToRequester = NbtCompat.getLong(entry, ENTRY_EXTR_REQ, 0);
        long totalExtracted = extracted;
        boolean changed = false;

        // Extract using the snapshot config
        ItemStack outStack = resolveItem(outputItem);
        if (outStack.isEmpty()) {
            // Item ID in snapshot can no longer be resolved (e.g. mod removed); cancel and drop entry
            ILogisticsNetwork network = ctx.network();
            if (network != null) cancelEntryOrders(network, entry);
            queue.remove(0);
            saveQueue(ctx, queue);
            return;
        }
        long alreadyExtracted = NbtCompat.getLong(entry, ENTRY_EXTR_SLOT, 0);
        long stillNeeded = totalOutputsExpected - alreadyExtracted;

        if (stillNeeded > 0) {
            // Only route up to the originally requested amount to the requester; extras go to a sink
            long requestedRemaining = Math.max(0, requested - totalSentToRequester);
            long toRequester = Math.min(stillNeeded, requestedRemaining);

            ItemVariant variant = ItemVariant.of(outStack);
            long extractedNow = extractAndRoute(ctx, variant, stillNeeded, requester, toRequester);

            if (extractedNow > 0) {
                long sentNow = Math.min(extractedNow, toRequester);
                entry.putLong(ENTRY_EXTR_SLOT, alreadyExtracted + extractedNow);
                entry.putLong(ENTRY_EXTR_REQ, totalSentToRequester + sentNow);
                totalSentToRequester += sentNow;
                totalExtracted += extractedNow;
                changed = true;
            }
        }

        // Check if this entry is complete
        if (totalExtracted >= totalOutputsExpected) {
            queue.remove(0);
            saveQueue(ctx, queue);
        } else if (changed) {
            entry.putLong(ENTRY_EXTR, totalExtracted);
            queue.set(0, entry);
            saveQueue(ctx, queue);
        }
    }

    private long extractAndRoute(PipeContext ctx, ItemVariant variant, long needed, BlockPos requester, long toRequester) {
        long extracted = 0;
        for (Direction dir : ctx.getInventoryConnections()) {
            BlockPos neighborPos = ctx.pos().relative(dir);
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), neighborPos, dir.getOpposite());
            if (storage == null) continue;

            try (Transaction tx = Transaction.openOuter()) {
                long toExtract = needed - extracted;
                long got = 0;
                for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
                    if (!view.getResource().equals(variant)) continue;
                    long canGet = Math.min(view.getAmount(), toExtract - got);
                    if (canGet <= 0) continue;
                    got += storage.extract(variant, canGet, tx);
                    if (got >= toExtract) break;
                }
                if (got > 0) {
                    tx.commit();
                    extracted += got;
                    long forRequester = Math.min(got, toRequester);
                    toRequester -= forRequester;
                    long forSink = got - forRequester;
                    long rem = forRequester;
                    while (rem > 0) {
                        int chunk = (int) Math.min(rem, Integer.MAX_VALUE);
                        TravelingItem t = new TravelingItem(
                                variant.toStack(chunk), dir.getOpposite(),
                                LogisticsPipe.CONFIG.ITEM_NETWORK_SPEED, requester);
                        t.setDeliveryId(getFirstEntryDeliveryId(ctx));
                        ctx.blockEntity().forceAddItem(t, dir);
                        rem -= chunk;
                    }
                    rem = forSink;
                    while (rem > 0) {
                        // Route excess to a sink (null destination = find default route)
                        int chunk = (int) Math.min(rem, Integer.MAX_VALUE);
                        TravelingItem t = new TravelingItem(
                                variant.toStack(chunk), dir.getOpposite(),
                                LogisticsPipe.CONFIG.ITEM_NETWORK_SPEED, null);
                        ctx.blockEntity().forceAddItem(t, dir);
                        rem -= chunk;
                    }
                    if (extracted >= needed) break;
                }
            }
        }
        return extracted;
    }

    @Nullable
    private UUID getFirstEntryDeliveryId(PipeContext ctx) {
        ListTag queue = getQueue(ctx);
        if (queue.isEmpty()) return null;
        CompoundTag entry = queue.getCompound(0).orElse(null);
        if (entry == null) return null;
        String dlv = NbtCompat.getString(entry, ENTRY_DLV, "");
        if (dlv.isEmpty()) return null;
        try { return UUID.fromString(dlv); } catch (Exception e) { return null; }
    }

    private void cancelAllJobs(PipeContext ctx) {
        ListTag queue = getQueue(ctx);
        ILogisticsNetwork network = ctx.network();
        if (network != null) {
            for (int qi = 0; qi < queue.size(); qi++) {
                CompoundTag entry = queue.getCompound(qi).orElse(null);
                if (entry == null) continue;
                cancelEntryOrders(network, entry);
            }
        }
        ctx.moduleState(getStateKey()).remove(QUEUE);
        ctx.markDirtyAndSync();
    }

    private void cancelEntryOrders(ILogisticsNetwork network, CompoundTag entry) {
        if (network == null || entry == null) return;
        ListTag orders = NbtCompat.getListOrEmpty(entry, ENTRY_ORDERS);
        for (int i = 0; i < orders.size(); i++) {
            String uuidStr = NbtCompat.getStringAt(orders, i, "");
            if (!uuidStr.isEmpty()) {
                try { network.cancelOrder(UUID.fromString(uuidStr)); } catch (Exception ignored) {}
            }
        }
    }

    // ==================== Helpers ====================

    @Nullable
    private static BlockPos parseBlockPos(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            String[] parts = s.split(",");
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (Exception e) { return null; }
    }

    private static ItemStack resolveItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) return ItemStack.EMPTY;
        try {
            ResourceId rid = ResourceId.tryParse(itemId);
            if (rid == null) return ItemStack.EMPTY;
            var holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
            if (holder.isEmpty()) return ItemStack.EMPTY;
            var item = holder.get().value();
            if (item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
            return new ItemStack(item);
        } catch (Exception e) { return ItemStack.EMPTY; }
    }

    // ==================== Constants ====================

    public static final int MAX_INPUTS = 9;
    public static final int MAX_OUTPUTS = 1;

}

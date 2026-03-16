package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.network.ILogisticsNetwork;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.network.ProviderCanFulfill;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
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
 * <p>V1 scope: up to 9 inputs (MAX_INPUTS), up to 1 output (MAX_OUTPUTS), fixed items only (no tags),
 * one active job at a time.
 */
public class ProcessModule implements Module {
    // NBT keys — process config (public for screen handler access)
    public static final String KEY_INPUTS = "inputs";
    public static final String KEY_OUTPUTS = "outputs";
    // Per-entry keys in inputs/outputs lists
    public static final String ENTRY_ITEM = "item";
    public static final String ENTRY_COUNT = "count";
    public static final String ENTRY_DEST = "dest"; // inputs only: "" = local, else satellite ID
    // NBT key — active job
    private static final String KEY_JOB = "active_job";
    private static final String JOB_REQ = "req";       // requester "x,y,z"
    private static final String JOB_DLV = "dlv";       // delivery UUID string
    private static final String JOB_EXEC = "exec";     // executions promised (long)
    private static final String JOB_EXTR = "extr";     // outputs extracted so far (long)
    private static final String JOB_ORDERS = "orders"; // ListTag<StringTag> of pending input order UUIDs
    private static final String JOB_REQUESTED = "req_amount"; // original requested amount (may be less than executions * outCount)
    // Global satellite destination for all inputs (0 = local/self)
    private static final String KEY_INPUT_SATELLITE = "input_satellite";
    // Timing
    private static final String TICKS_SCAN = "ticks_scan";
    private static final String TICKS_EXTRACT = "ticks_extract";
    private static final int SCAN_INTERVAL = 20;
    private static final int EXTRACT_INTERVAL = 6;
    // Priority: between Provider (1) and Crafter (5)
    static final int PROCESS_PRIORITY = 3;

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
        return ctx.moduleState(getStateKey()).contains(KEY_JOB);
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
        if (slot < 0) return;
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
        if (ctx.world().isClientSide()) return;

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
    public InteractionResult onUseWithoutItem(PipeContext ctx, UseOnContext usage) {
        if (ctx.world().isClientSide()) return InteractionResult.SUCCESS;
        if (!(usage.getPlayer() instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

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
        if (ctx.world().isClientSide()) return 0;
        if (isActive(ctx)) return 0; // Only one job at a time

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
        long actualAmount = executions * outCount;

        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return 0;

        // Validate global satellite destination exists
        int satId = getInputSatelliteId(ctx);
        String globalDest = satId > 0 ? String.valueOf(satId) : "";
        if (!globalDest.isEmpty() && network.findSatellite(globalDest) == null) {
            LogisticsPipe.LOGGER.warn("[Process @ {}] Satellite '{}' not found; aborting dispatch", ctx.pos(), satId);
            return 0;
        }

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

            BlockPos orderDest;
            if (globalDest.isEmpty()) {
                orderDest = ctx.pos(); // local: arrive at this pipe, then route to machine
            } else {
                orderDest = network.findSatellite(globalDest);
                if (orderDest == null) return 0;
            }

            UUID orderId = network.placeOrder(inputVariant, needed, orderDest);
            orderIds.add(StringTag.valueOf(orderId.toString()));
        }

        // Record active job
        CompoundTag job = new CompoundTag();
        job.putString(JOB_REQ, requester.getX() + "," + requester.getY() + "," + requester.getZ());
        if (deliveryId != null) job.putString(JOB_DLV, deliveryId.toString());
        job.putLong(JOB_EXEC, executions);
        job.putLong(JOB_EXTR, 0L);
        job.putLong(JOB_REQUESTED, amount); // cap: only route this many to requester, extras go to sink
        job.put(JOB_ORDERS, orderIds);
        ctx.moduleState(getStateKey()).put(KEY_JOB, job);
        ctx.markDirtyAndSync();

        return actualAmount;
    }

    @Override
    public void onDetach(PipeContext ctx) {
        cancelActiveJob(ctx);
        ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
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

        // Don't offer supply while a job is active (one job at a time)
        if (isActive(ctx)) {
            network.registerSupply(ctx.pos(), Map.of(), PROCESS_PRIORITY);
            return;
        }

        // Advertise 0 — signals "produceable on demand" (like CraftingModule)
        // The network will validate ingredients via buildProviderCheck before dispatching.
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
        CompoundTag state = ctx.moduleState(getStateKey());
        CompoundTag job = NbtCompat.getCompoundOrEmpty(state, KEY_JOB);

        BlockPos requester = parseBlockPos(NbtCompat.getString(job, JOB_REQ, ""));
        if (requester == null) { clearJob(ctx); return; }

        long executions = NbtCompat.getLong(job, JOB_EXEC, 0);
        long extracted = NbtCompat.getLong(job, JOB_EXTR, 0);

        // Compute total outputs expected across all output slots
        long totalOutputsExpected = 0;
        for (int i = 0; i < MAX_OUTPUTS; i++) {
            String outId = getOutputItem(ctx, i);
            if (outId.isEmpty()) continue;
            totalOutputsExpected += executions * getOutputCount(ctx, i);
        }

        if (totalOutputsExpected <= 0) { clearJob(ctx); return; }

        long requested = NbtCompat.getLong(job, JOB_REQUESTED, totalOutputsExpected);
        long totalSentToRequester = NbtCompat.getLong(job, "extr_req", 0);
        long totalExtracted = extracted;
        boolean changed = false;

        for (int i = 0; i < MAX_OUTPUTS; i++) {
            String outId = getOutputItem(ctx, i);
            if (outId.isEmpty()) continue;
            ItemStack outStack = resolveItem(outId);
            if (outStack.isEmpty()) continue;

            int outCount = getOutputCount(ctx, i);
            long alreadyExtracted = NbtCompat.getLong(job, "extr_" + i, 0);
            long stillNeeded = executions * outCount - alreadyExtracted;
            if (stillNeeded <= 0) continue;

            // Only route up to the originally requested amount to the requester; extras go to a sink
            long requestedRemaining = Math.max(0, requested - totalSentToRequester);
            long toRequester = Math.min(stillNeeded, requestedRemaining);

            ItemVariant variant = ItemVariant.of(outStack);
            long extractedNow = extractAndRoute(ctx, variant, stillNeeded, requester, toRequester);

            if (extractedNow > 0) {
                long sentNow = Math.min(extractedNow, toRequester);
                job.putLong("extr_" + i, alreadyExtracted + extractedNow);
                job.putLong("extr_req", totalSentToRequester + sentNow);
                totalSentToRequester += sentNow;
                totalExtracted += extractedNow;
                changed = true;
            }
        }

        if (changed) {
            job.putLong(JOB_EXTR, totalExtracted);
            state.put(KEY_JOB, job);
            ctx.markDirtyAndSync();
        }

        // Check if job is complete
        if (totalExtracted >= totalOutputsExpected) {
            clearJob(ctx);
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
                    long forSink = got - forRequester;
                    if (forRequester > 0) {
                        int safe = (int) Math.min(forRequester, Integer.MAX_VALUE);
                        TravelingItem t = new TravelingItem(
                                variant.toStack(safe), dir.getOpposite(),
                                LogisticsPipe.CONFIG.ITEM_NETWORK_SPEED, requester);
                        t.setDeliveryId(getJobDeliveryId(ctx));
                        ctx.blockEntity().forceAddItem(t, dir);
                    }
                    if (forSink > 0) {
                        // Route excess to a sink (null destination = find default route)
                        int safe = (int) Math.min(forSink, Integer.MAX_VALUE);
                        TravelingItem t = new TravelingItem(
                                variant.toStack(safe), dir.getOpposite(),
                                LogisticsPipe.CONFIG.ITEM_NETWORK_SPEED, null);
                        ctx.blockEntity().forceAddItem(t, dir);
                    }
                    break;
                }
            }
        }
        return extracted;
    }

    @Nullable
    private UUID getJobDeliveryId(PipeContext ctx) {
        CompoundTag job = NbtCompat.getCompoundOrEmpty(ctx.moduleState(getStateKey()), KEY_JOB);
        String dlv = NbtCompat.getString(job, JOB_DLV, "");
        if (dlv.isEmpty()) return null;
        try { return UUID.fromString(dlv); } catch (Exception e) { return null; }
    }

    private void clearJob(PipeContext ctx) {
        ctx.moduleState(getStateKey()).remove(KEY_JOB);
        ctx.markDirtyAndSync();
    }

    private void cancelActiveJob(PipeContext ctx) {
        if (!isActive(ctx)) return;
        CompoundTag job = NbtCompat.getCompoundOrEmpty(ctx.moduleState(getStateKey()), KEY_JOB);
        ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
        if (network != null) {
            ListTag orders = NbtCompat.getListOrEmpty(job, JOB_ORDERS);
            for (int i = 0; i < orders.size(); i++) {
                String uuidStr = NbtCompat.getStringAt(orders, i, "");
                if (!uuidStr.isEmpty()) {
                    try { network.cancelOrder(UUID.fromString(uuidStr)); } catch (Exception ignored) {}
                }
            }
        }
        clearJob(ctx);
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

    // ==================== Nested helper interface for ProviderCanFulfill ====================

    @FunctionalInterface
    private interface IngredientCheck {
        List<ItemVariant> check(ItemVariant item, long amount);
    }
}

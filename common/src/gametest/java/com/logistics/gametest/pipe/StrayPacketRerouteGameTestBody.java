package com.logistics.gametest.pipe;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;
import com.logistics.automation.refinery.RefineryBlockEntity;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.block.entity.PowerJunctionBlockEntity;
import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import com.logistics.pipe.modules.FluidSupplierModule;
import com.logistics.pipe.modules.SupplierModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * Shared GameTest bodies for re-homing a stranded packet onto a standing order (see
 * {@code common/build.gradle} for how these are compiled into both loaders' {@code gametest} source
 * sets).
 *
 * <p>Suppliers order but never register as sinks, so sink resolution cannot see them. A packet that
 * arrives with no destination — a delivery that expired, a destination that vanished, or fluid minted
 * for an order that is already gone — used to be dropped on the floor, and a fluid packet was voided
 * outright. These tests drive that recovery through real pipes: nothing but the order book can
 * deliver the stray stack to where it ends up.
 */
public class StrayPacketRerouteGameTestBody {

    // Item layout (y=1): [basic_logistics_pipe] → [supplier_logistics_pipe] → [chest]
    private static final BlockPos ITEM_INJECT = new BlockPos(0, 1, 0);
    private static final BlockPos ITEM_SUPPLIER = new BlockPos(1, 1, 0);
    private static final BlockPos CHEST = new BlockPos(2, 1, 0);

    // Fluid layout (y=1): [basic_logistics_pipe] → [fluid_supplier_pipe] → [refinery]
    private static final BlockPos FLUID_INJECT = new BlockPos(1, 1, 0);
    private static final BlockPos FLUID_SUPPLIER = new BlockPos(2, 1, 0);
    private static final BlockPos REFINERY = new BlockPos(3, 1, 0);

    private static final long STRAY_PACKET_MB = 1000L;
    private static final int STRAY_INGOTS = 4;
    private static final int SUPPLY_TARGET = 16;

    // ==================== Setup helpers ====================

    /** Place a Power Junction filled to capacity so modules and routing can pay their costs. */
    private static void placeChargedJunction(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPipe.BLOCK.POWER_JUNCTION);
        PowerJunctionBlockEntity junction = (PowerJunctionBlockEntity) context.getBlockEntity(pos);
        IEnergyStorage storage = junction.energyStorage(null);
        long remaining = PowerJunctionBlockEntity.CAPACITY;
        while (remaining > 0) {
            long inserted = storage.insert(remaining, false);
            if (inserted <= 0) break;
            remaining -= inserted;
        }
    }

    private static <T extends Module> T module(GameTestHelper context, BlockPos pos, Class<T> type) {
        PipeBlockEntity pipe = (PipeBlockEntity) context.getBlockEntity(pos);
        if (pipe == null || !(pipe.getBlockState().getBlock() instanceof PipeBlock pipeBlock)
                || pipeBlock.getPipe() == null) {
            throw new IllegalStateException("Expected a logistics pipe at " + pos);
        }
        T found = pipeBlock.getPipe().getModule(type, pipe);
        if (found == null) {
            throw new IllegalStateException(type.getSimpleName() + " missing from the pipe at " + pos);
        }
        return found;
    }

    private static PipeContext contextAt(GameTestHelper context, BlockPos pos) {
        return ((PipeBlockEntity) context.getBlockEntity(pos)).createContext();
    }

    /** Inject a stack into a pipe from its west face, as an upstream pipe would — no destination. */
    private static void injectFromWest(GameTestHelper context, BlockPos pos, ItemStack stack) {
        PipeBlockEntity pipe = (PipeBlockEntity) context.getBlockEntity(pos);
        if (pipe == null) {
            context.fail("Expected a pipe block entity at " + pos);
            return;
        }
        TravelingItem stray = new TravelingItem(stack, Direction.EAST, 0.5f);
        if (stray.getDestination() != null) {
            context.fail("The injected stack must start with no destination");
            return;
        }
        if (!pipe.forceAddItem(stray, Direction.WEST)) {
            context.fail("Pipe at " + pos + " should accept the injected " + stack.getItem());
        }
    }

    private static long chestCount(GameTestHelper context) {
        ChestBlockEntity chest = (ChestBlockEntity) context.getBlockEntity(CHEST);
        if (chest == null) return 0;
        long count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(Items.IRON_INGOT)) count += stack.getCount();
        }
        return count;
    }

    /** mB of water currently in the refinery's input tank. */
    private static long refineryWaterMb(GameTestHelper context) {
        RefineryBlockEntity refinery = (RefineryBlockEntity) context.getBlockEntity(REFINERY);
        IFluidStorage storage = refinery.fluidStorage(null);
        long total = 0;
        for (IFluidView view : storage.contents()) {
            if (view.resource().getFluid() == Fluids.WATER) total += view.amount();
        }
        return FluidUnits.toMillibuckets(total);
    }

    private static ItemStack strayWaterPacket() {
        ItemStack stack = new ItemStack(LogisticsPipe.ITEM.FLUID_PACKET);
        stack.set(LogisticsPipe.DATA.FLUID_PACKET, new FluidPacket(Fluids.WATER, STRAY_PACKET_MB));
        return stack;
    }

    /** The item layout, with no provider anywhere: only the stray stack can ever reach the chest. */
    private static void placeItemLayout(GameTestHelper context) {
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(ITEM_SUPPLIER, LogisticsPipe.BLOCK.SUPPLIER_LOGISTICS_PIPE);
        context.setBlock(ITEM_INJECT, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        placeChargedJunction(context, ITEM_INJECT.above());
    }

    // ==================== Tests ====================

    /**
     * A stray stack with no destination is re-homed onto a supplier's standing order and physically
     * delivered. The supplier is the only thing in the layout that wants iron, it registers no sink,
     * and there is no provider — so the order book is the only path from the pipe to the chest.
     */
    public static void testStrayItemReachesAnOrderingSupplier(GameTestHelper context) {
        placeItemLayout(context);

        // Let the pipe pick its supplied face, then configure it so it starts ordering iron.
        context.runAfterDelay(5, () -> module(context, ITEM_SUPPLIER, SupplierModule.class)
                .setSupplyConfig(contextAt(context, ITEM_SUPPLIER), 0, "minecraft:iron_ingot", SUPPLY_TARGET));

        // The supplier re-checks every 20 ticks, so its order is standing well before this lands.
        context.runAfterDelay(45, () ->
                injectFromWest(context, ITEM_INJECT, new ItemStack(Items.IRON_INGOT, STRAY_INGOTS)));

        context.succeedWhen(() -> {
            long count = chestCount(context);
            if (count < STRAY_INGOTS) {
                throw new GameTestAssertException(
                        "Stray iron should have been re-homed onto the supplier's order; chest holds " + count);
            }
        });
    }

    /**
     * Control: with nothing ordering iron, the same stray stack is still dropped on the floor. Without
     * this, the test above would also pass if some other route quietly delivered the stack.
     */
    public static void testStrayItemWithNoOrderStillDrops(GameTestHelper context) {
        placeItemLayout(context);
        // Deliberately not configured: the supplier orders nothing, so no order can claim the stack.

        boolean[] injected = {false};
        context.runAfterDelay(45, () -> {
            injectFromWest(context, ITEM_INJECT, new ItemStack(Items.IRON_INGOT, STRAY_INGOTS));
            injected[0] = true;
        });

        // Watch for the drop instead of sampling a fixed tick later. The stack is dropped at the
        // pipe's centre two ticks after it arrives, but the loose ItemEntity then drifts: one was
        // measured five blocks out and three down some fifty ticks on -- outside any box tight
        // enough to exclude the neighbouring structures, whose own drops sit six blocks away.
        context.succeedWhen(() -> {
            if (!injected[0]) {
                throw new GameTestAssertException("Waiting for the stray stack to be injected");
            }
            if (chestCount(context) > 0) {
                context.fail("Nothing ordered iron, so nothing should have been delivered to the chest");
                return;
            }
            AABB atPipe = new AABB(context.absolutePos(ITEM_INJECT)).inflate(1.5);
            boolean dropped = !context.getLevel()
                    .getEntitiesOfClass(ItemEntity.class, atPipe, e -> e.getItem().is(Items.IRON_INGOT))
                    .isEmpty();
            if (!dropped) {
                throw new GameTestAssertException(
                        "An unclaimable stray stack should still be dropped as a ground item");
            }
        });
    }

    /**
     * A stray fluid packet is re-homed the same way. This is the case that actually destroys resources
     * today: an undeliverable packet is voided rather than dropped, so the mB it carries is gone. No
     * fluid provider exists here, so the only water that can reach the refinery is the stray packet.
     */
    public static void testStrayFluidPacketReachesAnOrderingSupplier(GameTestHelper context) {
        long target = Math.min(2 * STRAY_PACKET_MB,
                LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_INPUT_TANK_MB));

        context.setBlock(REFINERY, LogisticsAutomation.BLOCK.REFINERY);
        context.setBlock(FLUID_SUPPLIER, LogisticsPipe.BLOCK.FLUID_SUPPLIER_LOGISTICS_PIPE);
        context.setBlock(FLUID_INJECT, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        placeChargedJunction(context, FLUID_INJECT.above());

        context.runAfterDelay(5, () -> {
            FluidSupplierModule supplier = module(context, FLUID_SUPPLIER, FluidSupplierModule.class);
            PipeContext ctx = contextAt(context, FLUID_SUPPLIER);
            supplier.setFilterFluid(ctx, Fluids.WATER);
            supplier.setTargetMb(ctx, target);
        });

        context.runAfterDelay(45, () -> injectFromWest(context, FLUID_INJECT, strayWaterPacket()));

        context.succeedWhen(() -> {
            long tankMb = refineryWaterMb(context);
            if (tankMb < STRAY_PACKET_MB) {
                throw new GameTestAssertException(
                        "Stray packet should have been re-homed onto the fluid order; tank holds " + tankMb + " mB");
            }
        });
    }
}

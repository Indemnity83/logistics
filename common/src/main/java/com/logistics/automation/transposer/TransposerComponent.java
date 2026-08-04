package com.logistics.automation.transposer;

import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

/**
 * The Transposer's conversion behavior as a {@link MachineComponent}: one bucket ⇄ 1000 mB per operation.
 *
 * <p>Each server tick it inspects the input slot and performs at most one atomic conversion:
 * <ul>
 *   <li><b>Fill</b> — an empty {@code minecraft:bucket} plus ≥ 1000 mB in the tank becomes the filled
 *       bucket for that fluid; the tank loses 1000 mB.</li>
 *   <li><b>Empty</b> — a recognized filled bucket (water/lava or a Logistics fluid bucket) plus room for
 *       1000 mB of that fluid becomes a plain empty bucket; the tank gains 1000 mB.</li>
 * </ul>
 *
 * <p><b>Atomicity:</b> nothing is consumed and neither slot nor tank changes unless the exact output can be
 * placed in the output slot. A blocked/incompatible/full output slot is a no-op, and any input that isn't a
 * recognized bucket (or a fluid mismatch against the tank) is rejected untouched.
 */
final class TransposerComponent implements MachineComponent {

    private static final int OUTPUT_INDEX = 0;

    private final String id;
    private final ItemStoreComponent items;
    private final FluidStoreComponent fluidStore;

    TransposerComponent(String id, ItemStoreComponent items, FluidStoreComponent fluidStore) {
        this.id = id;
        this.items = items;
        this.fluidStore = fluidStore;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        ItemStack input = items.input();
        if (input.isEmpty()) {
            return;
        }

        FluidTankComponent tank = fluidStore.tank();
        long bucket = FluidUnits.mb(1_000);

        if (input.is(Items.BUCKET)) {
            fill(tank, bucket);
            return;
        }
        empty(input.getItem(), tank, bucket);
    }

    /** Empty bucket + tank fluid → filled bucket, only if the exact filled bucket fits the output. */
    private void fill(FluidTankComponent tank, long bucket) {
        if (tank.isEmpty()) {
            return;
        }
        IFluidKey key = tank.getFluidKey();
        // Verify a full bucket is available before touching anything.
        if (tank.extract(key, bucket, true) != bucket) {
            return;
        }
        Item filled = TransposerBuckets.filledBucketForFluid(key.getFluid());
        if (filled == null) {
            return;
        }
        ItemStack result = new ItemStack(filled);
        if (!items.canAcceptInto(OUTPUT_INDEX, result)) {
            return; // blocked output → no-op
        }
        tank.extract(key, bucket, false);
        items.consumeInput(1);
        items.produceInto(OUTPUT_INDEX, result);
    }

    /** Filled bucket + tank room → empty bucket, only if the empty bucket fits the output. */
    private void empty(Item filledBucket, FluidTankComponent tank, long bucket) {
        Fluid fluid = TransposerBuckets.fluidForFilledBucket(filledBucket);
        if (fluid == null) {
            return; // not a recognized bucket → reject
        }
        IFluidKey key = SimpleFluidKey.of(fluid);
        // Verify the tank accepts exactly one bucket (empty, or same fluid with room) before touching anything.
        if (tank.insert(key, bucket, true) != bucket) {
            return; // fluid mismatch or full → reject
        }
        ItemStack result = new ItemStack(Items.BUCKET);
        if (!items.canAcceptInto(OUTPUT_INDEX, result)) {
            return; // blocked output → no-op
        }
        tank.insert(key, bucket, false);
        items.consumeInput(1);
        items.produceInto(OUTPUT_INDEX, result);
    }
}

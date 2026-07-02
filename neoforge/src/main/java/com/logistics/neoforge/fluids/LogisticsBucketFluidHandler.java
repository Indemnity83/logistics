package com.logistics.neoforge.fluids;

import com.logistics.LogisticsCore;
import com.logistics.core.item.LogisticsBucketItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Exposes each filled {@link LogisticsBucketItem} as a one-bucket fluid handler that drains to a plain
 * {@code minecraft:bucket} — the NeoForge-1.21.1 ({@link IFluidHandlerItem}) analog of Fabric's
 * {@code FullItemFluidStorage}. Empty-bucket -> filled is handled by vanilla via the fluid's getBucket().
 */
public final class LogisticsBucketFluidHandler implements IFluidHandlerItem {

    private ItemStack container;

    public LogisticsBucketFluidHandler(ItemStack container) {
        this.container = container;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    private FluidStack contents() {
        if (container.getItem() instanceof LogisticsBucketItem bucket) {
            Fluid fluid = BuiltInRegistries.FLUID.get(LogisticsCore.resource(bucket.fluidName()).toIdentifier());
            if (fluid != null) {
                return new FluidStack(fluid, FluidType.BUCKET_VOLUME);
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return contents();
    }

    @Override
    public int getTankCapacity(int tank) {
        return FluidType.BUCKET_VOLUME;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack contents = contents();
        if (contents.isEmpty() || resource.isEmpty()
                || resource.getFluid() != contents.getFluid()
                || resource.getAmount() < FluidType.BUCKET_VOLUME) {
            return FluidStack.EMPTY;
        }
        return drainInternal(contents, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack contents = contents();
        if (contents.isEmpty() || maxDrain < FluidType.BUCKET_VOLUME) {
            return FluidStack.EMPTY;
        }
        return drainInternal(contents, action);
    }

    private FluidStack drainInternal(FluidStack contents, FluidAction action) {
        if (action.execute()) {
            container = new ItemStack(Items.BUCKET);
        }
        return contents;
    }
}

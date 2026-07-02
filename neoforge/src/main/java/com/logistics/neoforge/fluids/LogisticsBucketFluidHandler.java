package com.logistics.neoforge.fluids;

import com.logistics.LogisticsCore;
import com.logistics.core.item.LogisticsBucketItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Exposes each filled {@link LogisticsBucketItem} as a one-bucket fluid handler that drains to a plain
 * {@code minecraft:bucket} — the NeoForge-26.2 ({@code ResourceHandler<FluidResource>}) analog of Fabric's
 * {@code FullItemFluidStorage}. Mirrors vanilla {@code BucketResourceHandler} but recognises our custom
 * (non-{@code BucketItem}) buckets, mapping each to its {@code logistics:core/<name>} fluid.
 */
public final class LogisticsBucketFluidHandler extends ItemAccessResourceHandler<FluidResource> {

    public LogisticsBucketFluidHandler(ItemAccess itemAccess) {
        super(itemAccess, 1);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        if (accessResource.getItem() instanceof LogisticsBucketItem bucket) {
            Fluid fluid = BuiltInRegistries.FLUID.getValue(LogisticsCore.resource(bucket.fluidName()).toIdentifier());
            return FluidResource.of(fluid);
        }
        return FluidResource.EMPTY;
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        return getResourceFrom(accessResource, index).isEmpty() ? 0 : FluidType.BUCKET_VOLUME;
    }

    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        if (newAmount == 0) {
            return ItemResource.of(Items.BUCKET);
        } else if (newAmount == FluidType.BUCKET_VOLUME) {
            return ItemResource.of(newResource.getFluid().getBucket());
        }
        return ItemResource.EMPTY;
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return FluidType.BUCKET_VOLUME;
    }
}

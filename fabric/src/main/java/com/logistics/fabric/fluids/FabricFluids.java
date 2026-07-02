package com.logistics.fabric.fluids;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.fluids.FluidDisplay;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Registers the custom fluids (source + flowing pair per {@link LogisticsCore#CUSTOM_FLUIDS}) into
 * {@code BuiltInRegistries.FLUID} on Fabric. Keeps the registered source fluids by name so the client
 * render handlers and bucket items can look them up.
 */
public final class FabricFluids {

    private static final Map<String, FlowingFluid> SOURCES = new LinkedHashMap<>();

    private FabricFluids() {}

    public static void register() {
        for (LogisticsCore.FluidDef def : LogisticsCore.CUSTOM_FLUIDS) {
            String name = def.name();
            FlowingFluid[] pair = new FlowingFluid[2]; // [0] source, [1] flowing
            pair[0] = new FabricLogisticsFluid(
                true, () -> pair[0], () -> pair[1], () -> LogisticsCore.BUCKET.forFluid(name));
            pair[1] = new FabricLogisticsFluid(
                false, () -> pair[0], () -> pair[1], () -> LogisticsCore.BUCKET.forFluid(name));
            Registry.register(BuiltInRegistries.FLUID, LogisticsCore.resource(name).toIdentifier(), pair[0]);
            Registry.register(BuiltInRegistries.FLUID, LogisticsCore.resource("flowing_" + name).toIdentifier(), pair[1]);
            SOURCES.put(name, pair[0]);

            // Without a name handler, Fabric's default resolves a block-less fluid to a "block.<ns>/<path>"
            // key we don't ship, so Jade's built-in fluid element (tanks, pipes) shows a raw key. Point both
            // the source and flowing forms at the source's shared display name.
            FluidVariantAttributeHandler nameHandler = nameHandler(pair[0]);
            FluidVariantAttributes.register(pair[0], nameHandler);
            FluidVariantAttributes.register(pair[1], nameHandler);
        }
    }

    private static FluidVariantAttributeHandler nameHandler(Fluid source) {
        return new FluidVariantAttributeHandler() {
            @Override
            public Component getName(FluidVariant variant) {
                return FluidDisplay.name(source);
            }
        };
    }

    /** Registered source fluids by name (in {@link LogisticsCore#CUSTOM_FLUIDS} order). */
    public static Map<String, FlowingFluid> sources() {
        return SOURCES;
    }

    /**
     * Exposes each filled bucket as a full one-bucket fluid container that empties to a plain bucket, so
     * the tank's {@code FluidContainerInteraction} (via {@code FluidStorageUtil}) can fill/drain it.
     */
    public static void registerBucketStorage() {
        LogisticsCore.BUCKET.all().forEach((name, bucketItem) -> {
            FlowingFluid fluid = SOURCES.get(name);
            FluidStorage.ITEM.registerForItems(
                (stack, context) -> new FullItemFluidStorage(
                    context, Items.BUCKET, FluidVariant.of(fluid), FluidConstants.BUCKET),
                bucketItem);
        });
    }
}

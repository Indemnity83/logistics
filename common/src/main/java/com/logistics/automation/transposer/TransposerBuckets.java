package com.logistics.automation.transposer;

import com.logistics.LogisticsCore;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Bidirectional bucket ↔ fluid mapping for the Transposer.
 *
 * <p>Covers the vanilla water/lava buckets and every custom Logistics fluid: tank-only fluids use a
 * {@link com.logistics.core.item.LogisticsBucketItem}, placeable fluids (crude oil) use a vanilla
 * {@code BucketItem}. Both resolve through {@link LogisticsCore.BUCKET#forFluid(String)}, so this class
 * doesn't special-case the bucket item type — it walks {@link LogisticsCore#CUSTOM_FLUIDS} and maps each
 * fluid to its registered bucket.
 *
 * <p>The maps are built lazily on first use so registration (fluids + buckets, per loader) has completed.
 */
final class TransposerBuckets {

    @Nullable private static Map<Item, Fluid> filledToFluid;
    @Nullable private static Map<Fluid, Item> fluidToFilled;

    private TransposerBuckets() {}

    private static synchronized void ensureMaps() {
        if (filledToFluid != null) {
            return;
        }
        Map<Item, Fluid> toFluid = new IdentityHashMap<>();
        Map<Fluid, Item> toBucket = new IdentityHashMap<>();

        toFluid.put(Items.WATER_BUCKET, Fluids.WATER);
        toFluid.put(Items.LAVA_BUCKET, Fluids.LAVA);
        toBucket.put(Fluids.WATER, Items.WATER_BUCKET);
        toBucket.put(Fluids.LAVA, Items.LAVA_BUCKET);

        for (LogisticsCore.FluidDef def : LogisticsCore.CUSTOM_FLUIDS) {
            Fluid fluid = BuiltInRegistries.FLUID.getValue(LogisticsCore.resource(def.name()).toIdentifier());
            Item bucket = LogisticsCore.BUCKET.forFluid(def.name());
            if (fluid == null || fluid == Fluids.EMPTY || bucket == null || bucket == Items.AIR) {
                continue;
            }
            toFluid.put(bucket, fluid);
            toBucket.put(fluid, bucket);
        }

        filledToFluid = toFluid;
        fluidToFilled = toBucket;
    }

    /** The fluid a recognized filled bucket empties into, or {@code null} if {@code item} isn't one. */
    @Nullable
    static Fluid fluidForFilledBucket(Item item) {
        ensureMaps();
        return filledToFluid.get(item);
    }

    /** The filled bucket item for {@code fluid}, or {@code null} if the fluid has no bucket form. */
    @Nullable
    static Item filledBucketForFluid(Fluid fluid) {
        ensureMaps();
        return fluidToFilled.get(fluid);
    }

    /** Whether the stack is a convertible input: an empty bucket or a recognized filled bucket. */
    static boolean isConvertibleInput(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item == Items.BUCKET || fluidForFilledBucket(item) != null;
    }
}

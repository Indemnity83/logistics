package com.logistics.core.lib.platform;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * A block or item that expands into extra creative-menu entries beyond its plain stack — e.g. a copper
 * pipe's weathering states. Consumed by {@link LogisticsCreativeTab#addWithVariants}, which appends the
 * plain stack first and then whatever this method contributes.
 */
public interface CreativeVariantProvider {
    void appendCreativeMenuVariants(List<ItemStack> variants, ItemStack baseStack);
}

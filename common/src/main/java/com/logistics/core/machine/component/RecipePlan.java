package com.logistics.core.machine.component;

import net.minecraft.world.item.ItemStack;

/**
 * A resolved recipe in RF-cost terms: the total energy the machine must spend, the produced
 * output, and any experience reward. Resolvers translate a machine's current inputs (custom or
 * vanilla recipes) into this executable form.
 */
public record RecipePlan(long energyRequired, ItemStack result, float experience) {}

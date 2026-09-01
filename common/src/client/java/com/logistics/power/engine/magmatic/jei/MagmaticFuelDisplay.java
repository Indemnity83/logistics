package com.logistics.power.engine.magmatic.jei;

import net.minecraft.world.level.material.Fluid;

/**
 * One Magmatic Engine fuel row, resolved for display.
 *
 * <p>The burn and output figures come from config, which is server-authoritative but not synced, so they
 * reflect the viewing client's own settings rather than the server's.
 *
 * @param batchBurnTicks how long one {@code batchMb} batch burns
 * @param coldRf         output per tick at the cold end of the heat curve
 * @param hotRf          output per tick at the hot end
 */
public record MagmaticFuelDisplay(
        Fluid fluid, int batchMb, int batchBurnTicks, long coldRf, long warmRf, long hotRf) {}

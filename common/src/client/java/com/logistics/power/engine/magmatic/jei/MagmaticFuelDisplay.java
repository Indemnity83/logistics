package com.logistics.power.engine.magmatic.jei;

import net.minecraft.world.level.material.Fluid;

/**
 * One Magmatic Engine fuel row, resolved for display.
 *
 * <p>The burn and output figures are read from config when this is built. Config is server-authoritative but
 * is not synced to clients, so on a server with non-default power settings these show the viewing client's
 * own values. They are exact in single-player, which is where the recipe browser is overwhelmingly used.
 *
 * @param batchBurnTicks how long one {@code batchMb} batch burns
 * @param coldRf         output per tick at the cold end of the heat curve
 * @param hotRf          output per tick at the hot end
 */
public record MagmaticFuelDisplay(
        Fluid fluid, int batchMb, int batchBurnTicks, long coldRf, long warmRf, long hotRf) {}

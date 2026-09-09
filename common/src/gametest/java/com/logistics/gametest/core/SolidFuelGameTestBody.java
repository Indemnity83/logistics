package com.logistics.gametest.core;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.power.FuelHelper;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The mod's solid fuels burn, and burn for the documented number of ticks. Asserted through
 * {@link FuelHelper}, the same seam the Stirling Engine consults, so this covers both furnaces and
 * the engine. Registering fuels is loader-specific, which is exactly why it is checked per loader.
 */
public class SolidFuelGameTestBody {

    public static void testPeatBurns(GameTestHelper context) {
        assertBurns(context, LogisticsCore.ITEM.PEAT, 2000);
    }

    public static void testBitumenBurns(GameTestHelper context) {
        assertBurns(context, LogisticsCore.ITEM.BITUMEN, 3200);
    }

    public static void testTarBurns(GameTestHelper context) {
        assertBurns(context, LogisticsCore.ITEM.TAR, 800);
    }

    private static void assertBurns(GameTestHelper context, Item item, int expectedTicks) {
        ItemStack stack = new ItemStack(item);

        if (!FuelHelper.isFuel(context.getLevel(), stack)) {
            context.fail(item + " must be a furnace fuel");
            return;
        }

        int duration = FuelHelper.getBurnDuration(context.getLevel(), stack);
        if (duration != expectedTicks) {
            context.fail(item + " must burn for " + expectedTicks + " ticks, got " + duration);
            return;
        }

        context.succeed();
    }
}

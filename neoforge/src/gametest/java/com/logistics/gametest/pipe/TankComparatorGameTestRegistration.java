package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link TankComparatorGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration here is the
 * legacy reflection model: {@link GameTestHolder} makes NeoForge scan this type, and
 * {@link PrefixGameTestTemplate}(false) keeps the class name out of the template id so every test
 * can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class TankComparatorGameTestRegistration {

    private TankComparatorGameTestRegistration() {}

    @GameTest(template = "empty", batch = "tankcomparator")
    public static void emptyTankReadsZero(GameTestHelper context) {
        TankComparatorGameTestBody.emptyTankReadsZero(context);
    }

    @GameTest(template = "empty", batch = "tankcomparator")
    public static void fullTankReadsFifteen(GameTestHelper context) {
        TankComparatorGameTestBody.fullTankReadsFifteen(context);
    }

    @GameTest(template = "empty", batch = "tankcomparator")
    public static void columnFillIsReadNotCellFill(GameTestHelper context) {
        TankComparatorGameTestBody.columnFillIsReadNotCellFill(context);
    }

    @GameTest(template = "empty", batch = "tankcomparator")
    public static void everyCellInAColumnReadsTheSame(GameTestHelper context) {
        TankComparatorGameTestBody.everyCellInAColumnReadsTheSame(context);
    }

    @GameTest(template = "empty", batch = "tankcomparator")
    public static void pipesAdvertiseNoComparatorOutput(GameTestHelper context) {
        TankComparatorGameTestBody.pipesAdvertiseNoComparatorOutput(context);
    }
}

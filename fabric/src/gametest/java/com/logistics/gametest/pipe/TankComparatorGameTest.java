package com.logistics.gametest.pipe;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the tank comparator GameTests. Test logic lives in
 * {@link TankComparatorGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
public class TankComparatorGameTest {

    @GameTest
    public void emptyTankReadsZero(GameTestHelper context) {
        TankComparatorGameTestBody.emptyTankReadsZero(context);
    }

    @GameTest
    public void fullTankReadsFifteen(GameTestHelper context) {
        TankComparatorGameTestBody.fullTankReadsFifteen(context);
    }

    @GameTest
    public void columnFillIsReadNotCellFill(GameTestHelper context) {
        TankComparatorGameTestBody.columnFillIsReadNotCellFill(context);
    }

    @GameTest
    public void everyCellInAColumnReadsTheSame(GameTestHelper context) {
        TankComparatorGameTestBody.everyCellInAColumnReadsTheSame(context);
    }

    @GameTest
    public void pipesAdvertiseNoComparatorOutput(GameTestHelper context) {
        TankComparatorGameTestBody.pipesAdvertiseNoComparatorOutput(context);
    }
}

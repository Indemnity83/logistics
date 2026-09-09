package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the tank comparator GameTests. Test logic lives in
 * {@link TankComparatorGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
public class TankComparatorGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void emptyTankReadsZero(GameTestHelper context) {
        TankComparatorGameTestBody.emptyTankReadsZero(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void fullTankReadsFifteen(GameTestHelper context) {
        TankComparatorGameTestBody.fullTankReadsFifteen(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void columnFillIsReadNotCellFill(GameTestHelper context) {
        TankComparatorGameTestBody.columnFillIsReadNotCellFill(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void everyCellInAColumnReadsTheSame(GameTestHelper context) {
        TankComparatorGameTestBody.everyCellInAColumnReadsTheSame(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void pipesAdvertiseNoComparatorOutput(GameTestHelper context) {
        TankComparatorGameTestBody.pipesAdvertiseNoComparatorOutput(context);
    }
}

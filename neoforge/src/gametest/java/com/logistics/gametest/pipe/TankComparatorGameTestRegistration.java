package com.logistics.gametest.pipe;

import com.logistics.gametest.GameTestCase;
import com.logistics.gametest.GameTestRegistrationSupport;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Wires {@link TankComparatorGameTestBody}'s methods into MC's data-driven GameTest registries —
 * see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class TankComparatorGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("pipe/empty_tank_reads_zero", 100, TankComparatorGameTestBody::emptyTankReadsZero),
        new GameTestCase("pipe/full_tank_reads_fifteen", 100, TankComparatorGameTestBody::fullTankReadsFifteen),
        new GameTestCase(
            "pipe/column_fill_is_read_not_cell_fill", 100, TankComparatorGameTestBody::columnFillIsReadNotCellFill),
        new GameTestCase(
            "pipe/every_cell_in_a_column_reads_the_same",
            100,
            TankComparatorGameTestBody::everyCellInAColumnReadsTheSame),
        new GameTestCase(
            "pipe/pipes_advertise_no_comparator_output",
            100,
            TankComparatorGameTestBody::pipesAdvertiseNoComparatorOutput));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private TankComparatorGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/tank_comparator", TESTS, FUNCTIONS);
    }
}

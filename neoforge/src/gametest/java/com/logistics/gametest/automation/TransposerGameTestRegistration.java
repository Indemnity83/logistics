package com.logistics.gametest.automation;

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
 * Wires {@link TransposerGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class TransposerGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("automation/transposer_placement", 100, TransposerGameTestBody::placement),
        new GameTestCase("automation/transposer_fill_from_lava", 80, TransposerGameTestBody::fillFromLava),
        new GameTestCase("automation/transposer_fill_from_custom_fluid", 80, TransposerGameTestBody::fillFromCustomFluid),
        new GameTestCase("automation/transposer_empty_custom_fluid_bucket", 80, TransposerGameTestBody::emptyCustomFluidBucket),
        new GameTestCase("automation/transposer_fill_seed_oil_bucket", 80, TransposerGameTestBody::fillSeedOilBucket),
        new GameTestCase("automation/transposer_empty_seed_oil_bucket", 80, TransposerGameTestBody::emptySeedOilBucket),
        new GameTestCase(
            "automation/transposer_insufficient_tank_amount_rejected", 80, TransposerGameTestBody::insufficientTankAmountRejected),
        new GameTestCase("automation/transposer_empty_lava_bucket", 80, TransposerGameTestBody::emptyLavaBucket),
        new GameTestCase("automation/transposer_fluid_mismatch_rejected", 80, TransposerGameTestBody::fluidMismatchRejected),
        new GameTestCase("automation/transposer_blocked_output_is_no_op", 80, TransposerGameTestBody::blockedOutputIsNoOp),
        new GameTestCase("automation/transposer_no_energy_is_no_op", 80, TransposerGameTestBody::noEnergyIsNoOp),
        new GameTestCase(
            "automation/transposer_fills_bucket_via_real_engine_and_hoppers",
            120,
            TransposerGameTestBody::fillsBucketViaRealEngineAndHoppers));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private TransposerGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "automation/transposer", TESTS, FUNCTIONS);
    }
}

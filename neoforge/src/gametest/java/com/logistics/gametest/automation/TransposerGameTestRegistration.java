package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link TransposerGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class TransposerGameTestRegistration {

    private TransposerGameTestRegistration() {}

    @GameTest(template = "empty", batch = "transposer")
    public static void placement(GameTestHelper context) {
        TransposerGameTestBody.placement(context);
    }

    /**
    * Wiki claim (Usage): "An empty bucket plus at least 1,000 mB in the tank becomes a filled
    * bucket of that fluid; the tank loses 1,000 mB." (Power): "A bucket fill/empty costs 800 RF."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "transposer", timeoutTicks = 80)
    public static void fillFromLava(GameTestHelper context) {
        TransposerGameTestBody.fillFromLava(context);
    }

    /** Empty bucket + tank of a custom mod fluid + full energy → that fluid's bucket out, tank drained. */
    @GameTest(template = "empty", batch = "transposer", timeoutTicks = 80)
    public static void fillFromCustomFluid(GameTestHelper context) {
        TransposerGameTestBody.fillFromCustomFluid(context);
    }

    /**
    * Wiki claim (Usage): "A filled bucket — water, lava, or any Logistics fluid bucket — plus room
    * for 1,000 mB becomes a plain empty bucket; the tank gains 1,000 mB."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "transposer", timeoutTicks = 80)
    public static void emptyCustomFluidBucket(GameTestHelper context) {
        TransposerGameTestBody.emptyCustomFluidBucket(context);
    }

    /** Empty bucket + tank of seed oil + full energy → seed oil bucket out, tank drained by 1000 mB. */
    @GameTest(template = "empty", batch = "transposer", timeoutTicks = 80)
    public static void fillSeedOilBucket(GameTestHelper context) {
        TransposerGameTestBody.fillSeedOilBucket(context);
    }

    /** Seed oil bucket + room in the tank + full energy → empty bucket out, tank gains 1000 mB of seed oil. */
    @GameTest(template = "empty", batch = "transposer", timeoutTicks = 80)
    public static void emptySeedOilBucket(GameTestHelper context) {
        TransposerGameTestBody.emptySeedOilBucket(context);
    }

    /**
    * Wiki claim (Usage): "The conversion is atomic: if the output slot can't hold the result or
    * there isn't enough RF banked, nothing is consumed and the tank is untouched." An under-full
    * tank is the fluid-side equivalent — there isn't a valid result to produce, so nothing runs.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "transposer", timeoutTicks = 80)
    public static void insufficientTankAmountRejected(GameTestHelper context) {
        TransposerGameTestBody.insufficientTankAmountRejected(context);
    }

    /** Lava bucket + room in the tank + full energy → empty bucket out, tank gains 1000 mB of lava. */
    @GameTest(template = "empty", batch = "transposer", timeoutTicks = 80)
    public static void emptyLavaBucket(GameTestHelper context) {
        TransposerGameTestBody.emptyLavaBucket(context);
    }

    /** Filled bucket of one fluid against a tank holding another + full energy → rejected: untouched. */
    @GameTest(template = "empty", batch = "transposer", timeoutTicks = 80)
    public static void fluidMismatchRejected(GameTestHelper context) {
        TransposerGameTestBody.fluidMismatchRejected(context);
    }

    /**
    * Wiki claim (Usage): "The conversion is atomic: if the output slot can't hold the result...
    * nothing is consumed and the tank is untouched." Neither the input bucket nor the tank may
    * change when the output is blocked.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "transposer", timeoutTicks = 80)
    public static void blockedOutputIsNoOp(GameTestHelper context) {
        TransposerGameTestBody.blockedOutputIsNoOp(context);
    }

    /**
    * Wiki claim (Usage): "...if there isn't enough RF banked, nothing is consumed and the tank is
    * untouched." A valid fill recipe must still not run without energy to spend.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "transposer", timeoutTicks = 80)
    public static void noEnergyIsNoOp(GameTestHelper context) {
        TransposerGameTestBody.noEnergyIsNoOp(context);
    }

    /**
    * Wiki claim (Usage/Power): "An empty bucket plus at least 1,000 mB in the tank becomes a
    * filled bucket... A bucket fill/empty costs 800 RF."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "transposer", timeoutTicks = 100)
    public static void fillsBucketViaRealEngineAndHoppers(GameTestHelper context) {
        TransposerGameTestBody.fillsBucketViaRealEngineAndHoppers(context);
    }
}

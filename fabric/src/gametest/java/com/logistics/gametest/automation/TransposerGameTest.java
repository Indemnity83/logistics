package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Transposer GameTests. Test logic lives in
 * {@link TransposerGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class TransposerGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void placement(GameTestHelper context) {
        TransposerGameTestBody.placement(context);
    }

    /**
    * Wiki claim (Usage): "An empty bucket plus at least 1,000 mB in the tank becomes a filled
    * bucket of that fluid; the tank loses 1,000 mB." (Power): "A bucket fill/empty costs 800 RF."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void fillFromLava(GameTestHelper context) {
        TransposerGameTestBody.fillFromLava(context);
    }

    /** Empty bucket + tank of a custom mod fluid + full energy → that fluid's bucket out, tank drained. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void fillFromCustomFluid(GameTestHelper context) {
        TransposerGameTestBody.fillFromCustomFluid(context);
    }

    /**
    * Wiki claim (Usage): "A filled bucket — water, lava, or any Logistics fluid bucket — plus room
    * for 1,000 mB becomes a plain empty bucket; the tank gains 1,000 mB."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void emptyCustomFluidBucket(GameTestHelper context) {
        TransposerGameTestBody.emptyCustomFluidBucket(context);
    }

    /** Empty bucket + tank of seed oil + full energy → seed oil bucket out, tank drained by 1000 mB. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void fillSeedOilBucket(GameTestHelper context) {
        TransposerGameTestBody.fillSeedOilBucket(context);
    }

    /** Seed oil bucket + room in the tank + full energy → empty bucket out, tank gains 1000 mB of seed oil. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void emptySeedOilBucket(GameTestHelper context) {
        TransposerGameTestBody.emptySeedOilBucket(context);
    }

    /**
    * Wiki claim (Usage): "The conversion is atomic: if the output slot can't hold the result or
    * there isn't enough RF banked, nothing is consumed and the tank is untouched." An under-full
    * tank is the fluid-side equivalent — there isn't a valid result to produce, so nothing runs.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void insufficientTankAmountRejected(GameTestHelper context) {
        TransposerGameTestBody.insufficientTankAmountRejected(context);
    }

    /** Lava bucket + room in the tank + full energy → empty bucket out, tank gains 1000 mB of lava. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void emptyLavaBucket(GameTestHelper context) {
        TransposerGameTestBody.emptyLavaBucket(context);
    }

    /** Filled bucket of one fluid against a tank holding another + full energy → rejected: untouched. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void fluidMismatchRejected(GameTestHelper context) {
        TransposerGameTestBody.fluidMismatchRejected(context);
    }

    /**
    * Wiki claim (Usage): "The conversion is atomic: if the output slot can't hold the result...
    * nothing is consumed and the tank is untouched." Neither the input bucket nor the tank may
    * change when the output is blocked.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void blockedOutputIsNoOp(GameTestHelper context) {
        TransposerGameTestBody.blockedOutputIsNoOp(context);
    }

    /**
    * Wiki claim (Usage): "...if there isn't enough RF banked, nothing is consumed and the tank is
    * untouched." A valid fill recipe must still not run without energy to spend.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void noEnergyIsNoOp(GameTestHelper context) {
        TransposerGameTestBody.noEnergyIsNoOp(context);
    }

    /**
    * Wiki claim (Usage/Power): "An empty bucket plus at least 1,000 mB in the tank becomes a
    * filled bucket... A bucket fill/empty costs 800 RF."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void fillsBucketViaRealEngineAndHoppers(GameTestHelper context) {
        TransposerGameTestBody.fillsBucketViaRealEngineAndHoppers(context);
    }
}

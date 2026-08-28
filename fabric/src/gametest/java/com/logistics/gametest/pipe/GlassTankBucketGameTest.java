package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Fabric entrypoint wiring for the glass tank bucket GameTests. Test logic lives in
 * {@link GlassTankBucketGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
public class GlassTankBucketGameTest {

    /** Survival: the held bucket itself becomes the filled bucket, and the tank empties. */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void emptyBucketDrainsGlassTankInSurvival(GameTestHelper context) {
        GlassTankBucketGameTestBody.emptyBucketDrainsGlassTankInSurvival(context);
    }

    /**
    * Creative: Fabric's {@code FluidStorageUtil} deliberately leaves the held container item alone and
    * grants a copy elsewhere in the inventory instead (so testers don't burn through their held stack).
    * The tank still empties either way; only the inventory grant is Fabric-specific, so it is asserted
    * here rather than in the shared body.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void emptyBucketDrainsGlassTankInCreative(GameTestHelper context) {
        Player player = GlassTankBucketGameTestBody.emptyBucketDrainsGlassTankInCreative(context);
        context.assertTrue(player.getInventory().contains(new ItemStack(Items.WATER_BUCKET)),
                "Expected a water bucket to land in the creative player's inventory");
        context.succeed();
    }
}

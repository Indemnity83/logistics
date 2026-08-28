package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link GlassTankBucketGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class GlassTankBucketGameTestRegistration {

    private GlassTankBucketGameTestRegistration() {}

    /** Survival: the held bucket itself becomes the filled bucket, and the tank empties. */
    @GameTest(template = "empty", batch = "glasstankbucket")
    public static void emptyBucketDrainsGlassTankInSurvival(GameTestHelper context) {
        GlassTankBucketGameTestBody.emptyBucketDrainsGlassTankInSurvival(context);
    }

    /**
     * Creative: the held container item is left alone and the tank still empties. Unlike Fabric,
     * NeoForge's {@code FluidUtil} does not grant a copy of the filled bucket elsewhere in the
     * inventory, so that assertion stays in the Fabric wrapper.
     */
    @GameTest(template = "empty", batch = "glasstankbucket")
    public static void emptyBucketDrainsGlassTankInCreative(GameTestHelper context) {
        GlassTankBucketGameTestBody.emptyBucketDrainsGlassTankInCreative(context);
        context.succeed();
    }
}

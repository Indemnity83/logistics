package com.logistics.power.block;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractBatteryBlockEntity;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.level.block.RenderShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BatteryBlock}'s state definition and render shape, run against every
 * registered tier.
 *
 * <p>These use the <em>registered</em> blocks rather than a throwaway instance on purpose.
 * Constructing a block whose id has no matching registered item leaves a data-component
 * initializer pointing at a missing item, and the next {@code bindDataComponents()} in the same
 * JVM then dies with "Missing element ResourceKey[minecraft:item / ...]" — surfacing as an
 * ordering-dependent failure in whichever other class shares the test worker.
 */
class BatteryBlockTest extends MinecraftTestEnvironment {

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private static BatteryBlock blockFor(BatteryTier tier) {
        return (BatteryBlock) switch (tier) {
            case TIN -> LogisticsPower.BLOCK.TIN_BATTERY;
            case COPPER -> LogisticsPower.BLOCK.COPPER_BATTERY;
            case GOLD -> LogisticsPower.BLOCK.GOLD_BATTERY;
            case AMETHYST -> LogisticsPower.BLOCK.AMETHYST_BATTERY;
            case ECHO -> LogisticsPower.BLOCK.ECHO_BATTERY;
        };
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BatteryTier.class)
    void eachRegisteredBlockCarriesItsOwnTier(BatteryTier tier) {
        assertEquals(tier, blockFor(tier).tier());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BatteryTier.class)
    void rendersFromModel(BatteryTier tier) {
        BatteryBlock block = blockFor(tier);
        assertEquals(RenderShape.MODEL, block.getRenderShape(block.defaultBlockState()));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BatteryTier.class)
    void hasChargeStatePropertyDefaultingToZero(BatteryTier tier) {
        BatteryBlock block = blockFor(tier);
        assertTrue(block.defaultBlockState().hasProperty(AbstractBatteryBlockEntity.CHARGE),
                "battery block must expose the charge property the multipart blockstate reads");
        assertEquals(0, block.defaultBlockState().getValue(AbstractBatteryBlockEntity.CHARGE));
    }
}

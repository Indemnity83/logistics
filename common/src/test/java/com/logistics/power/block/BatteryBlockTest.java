package com.logistics.power.block;

import com.logistics.core.lib.power.AbstractBatteryBlockEntity;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@link BatteryBlock}'s state definition and render shape. */
class BatteryBlockTest extends MinecraftTestEnvironment {

    private final BatteryBlock block = new BatteryBlock(BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, Identifier.parse("logistics:test_battery"))));

    @Test
    void rendersFromModel() {
        assertEquals(RenderShape.MODEL, block.getRenderShape(block.defaultBlockState()));
    }

    @Test
    void hasChargeStatePropertyDefaultingToZero() {
        assertTrue(block.defaultBlockState().hasProperty(AbstractBatteryBlockEntity.CHARGE),
                "battery block must expose the charge property the multipart blockstate reads");
        assertEquals(0, block.defaultBlockState().getValue(AbstractBatteryBlockEntity.CHARGE));
    }

    @Test
    void exposesMapCodec() {
        assertNotNull(BatteryBlock.CODEC);
    }
}

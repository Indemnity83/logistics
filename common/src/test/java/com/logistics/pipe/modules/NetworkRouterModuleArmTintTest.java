package com.logistics.pipe.modules;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@link NetworkRouterModule#getArmTint} — green/red/blue arm status colors. */
class NetworkRouterModuleArmTintTest extends MinecraftTestEnvironment {

    private static final int GREEN = 0x5AAE4D;
    private static final int RED = 0xD93F3F;
    private static final int BLUE = 0x3F76E4;

    private final NetworkRouterModule module = new NetworkRouterModule();

    private static PipeContext context(FakePipeAccess access) {
        return new PipeContext(null, BlockPos.ZERO, Blocks.STONE.defaultBlockState(), access);
    }

    @Test
    void inventoryArm_isBlue() {
        FakePipeAccess access = new FakePipeAccess()
                .setConnection(Direction.NORTH, PipeConnection.Type.INVENTORY);
        assertEquals(BLUE, module.getArmTint(context(access), Direction.NORTH));
    }

    @Test
    void poweredNetworkArm_isGreen() {
        FakePipeAccess access = new FakePipeAccess()
                .setConnection(Direction.EAST, PipeConnection.Type.PIPE)
                .setPoweredArmMask(1 << Direction.EAST.get3DDataValue());
        assertEquals(GREEN, module.getArmTint(context(access), Direction.EAST));
    }

    @Test
    void unpoweredNetworkArm_isRed() {
        FakePipeAccess access = new FakePipeAccess()
                .setConnection(Direction.EAST, PipeConnection.Type.PIPE); // mask bit not set
        assertEquals(RED, module.getArmTint(context(access), Direction.EAST));
    }

    @Test
    void inventoryArm_isBlueEvenWhenPowerBitSet() {
        // Inventory takes precedence over the powered mask — it's an I/O marker, not a power link.
        FakePipeAccess access = new FakePipeAccess()
                .setConnection(Direction.UP, PipeConnection.Type.INVENTORY)
                .setPoweredArmMask(1 << Direction.UP.get3DDataValue());
        assertEquals(BLUE, module.getArmTint(context(access), Direction.UP));
    }
}

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

/** Unit tests for {@link NetworkRouterModule#getArmTint} — green for a powered link, red otherwise. */
class NetworkRouterModuleArmTintTest extends MinecraftTestEnvironment {

    // Reference the production constants so the test follows any future color change.
    private static final int GREEN = NetworkRouterModule.ARM_TINT_POWERED;
    private static final int RED = NetworkRouterModule.ARM_TINT_UNPOWERED;

    private final NetworkRouterModule module = new NetworkRouterModule();

    private static PipeContext context(FakePipeAccess access) {
        return new PipeContext(null, BlockPos.ZERO, Blocks.STONE.defaultBlockState(), access);
    }

    @Test
    void poweredArm_isGreen() {
        FakePipeAccess access = new FakePipeAccess()
                .setConnection(Direction.EAST, PipeConnection.Type.PIPE)
                .setPoweredArmMask(1 << Direction.EAST.get3DDataValue());
        assertEquals(GREEN, module.getArmTint(context(access), Direction.EAST));
    }

    @Test
    void unpoweredArm_isRed() {
        FakePipeAccess access = new FakePipeAccess()
                .setConnection(Direction.EAST, PipeConnection.Type.PIPE); // mask bit not set
        assertEquals(RED, module.getArmTint(context(access), Direction.EAST));
    }

    @Test
    void inventoryArm_isRed() {
        // Inventories (and machines) are not power links — the mask never marks them, so they're red.
        FakePipeAccess access = new FakePipeAccess()
                .setConnection(Direction.NORTH, PipeConnection.Type.INVENTORY);
        assertEquals(RED, module.getArmTint(context(access), Direction.NORTH));
    }

    @Test
    void core_isGreen_whenAnyArmIsPowered() {
        FakePipeAccess access = new FakePipeAccess()
                .setPoweredArmMask(1 << Direction.EAST.get3DDataValue());
        assertEquals(GREEN, module.getCoreTint(context(access)));
    }

    @Test
    void core_isRed_whenNoArmIsPowered() {
        assertEquals(RED, module.getCoreTint(context(new FakePipeAccess())));
    }
}

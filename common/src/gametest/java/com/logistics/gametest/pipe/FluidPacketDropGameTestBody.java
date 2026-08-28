package com.logistics.gametest.pipe;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Shared fluid packet drop GameTest bodies, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Loader-specific glue wires these into each loader's
 * own registration mechanism: Fabric's {@code @GameTest}-annotated {@code FluidPacketDropGameTest}
 * delegates to these methods, and NeoForge's {@code FluidPacketDropGameTestRegistration} references
 * them directly as {@code Consumer<GameTestHelper>} method references.
 *
 * <p>An undeliverable fluid packet must be voided rather than spawned as a pickup-able ground item,
 * and normal items must still drop as usual.
 */
public class FluidPacketDropGameTestBody {

    private static final BlockPos FLUID_PIPE = new BlockPos(0, 1, 0);
    private static final BlockPos ITEM_PIPE = new BlockPos(2, 1, 0);

    private static ItemStack fluidPacketStack() {
        long maxMb = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_MAX_MB);
        ItemStack stack = new ItemStack(LogisticsPipe.ITEM.FLUID_PACKET);
        stack.set(LogisticsPipe.DATA.FLUID_PACKET, new FluidPacket(Fluids.WATER, maxMb));
        return stack;
    }

    private static PipeBlockEntity placePipeCarrying(GameTestHelper context, BlockPos pos, ItemStack stack) {
        context.setBlock(pos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        PipeBlockEntity pipe = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipe == null) {
            context.fail("Transport pipe should have a block entity");
            return null;
        }
        TravelingItem item = new TravelingItem(stack, Direction.WEST, 0.1f);
        pipe.forceAddItem(item, Direction.WEST);
        return pipe;
    }

    /**
     * A fluid packet stranded in a broken pipe must not spawn as a ground item — it's voided instead.
     */
    public static void testFluidPacketNeverDropsOnPipeBreak(GameTestHelper context) {
        placePipeCarrying(context, FLUID_PIPE, fluidPacketStack());

        ServerLevel level = context.getLevel();
        BlockPos absPos = context.absolutePos(FLUID_PIPE);
        level.destroyBlock(absPos, false);

        List<ItemEntity> drops = level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(absPos).inflate(2.0),
                e -> e.getItem().is(LogisticsPipe.ITEM.FLUID_PACKET));

        if (!drops.isEmpty()) {
            context.fail("Fluid packet should never spawn as a ground item, found " + drops.size());
        }
        context.succeed();
    }

    /**
     * Control: a normal item stranded the same way must still drop as usual.
     */
    public static void testNormalItemStillDropsOnPipeBreak(GameTestHelper context) {
        placePipeCarrying(context, ITEM_PIPE, new ItemStack(Items.COBBLESTONE, 3));

        ServerLevel level = context.getLevel();
        BlockPos absPos = context.absolutePos(ITEM_PIPE);
        level.destroyBlock(absPos, false);

        List<ItemEntity> drops = level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(absPos).inflate(2.0),
                e -> e.getItem().is(Items.COBBLESTONE));

        if (drops.size() != 1 || drops.get(0).getItem().getCount() != 3) {
            context.fail("Expected exactly one dropped stack of 3 cobblestone, found " + drops.size());
        }
        context.succeed();
    }
}

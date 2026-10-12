package com.logistics.gametest.forestry;

import com.logistics.forestry.logic.FellPlan;
import com.logistics.forestry.logic.LevelTreeView;
import com.logistics.forestry.logic.TreeFeller;
import com.logistics.forestry.logic.TreeView.Kind;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared Tree Feller GameTest bodies, compiled directly into both loaders' {@code gametest} source
 * sets. Fabric's {@code TreeFellerGameTest} delegates to these methods and NeoForge's
 * {@code TreeFellerGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Nothing here breaks blocks: the plan is only checked against the hand-built tree.
 */
public class TreeFellerGameTestBody {

    /**
     * {@link LevelTreeView} classifies real blocks through the shipped forestry tags and leaf
     * persistence, and {@link TreeFeller} plans exactly the natural tree around a trunk that has a
     * bee nest, a player-placed leaf and a planted propagule against it.
     */
    public static void testLevelTreeViewClassifiesAndPlansATree(GameTestHelper context) {
        BlockState naturalLeaves = Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, false)
                .setValue(LeavesBlock.DISTANCE, 1);
        BlockState persistentLeaves = Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true);

        BlockPos plot = new BlockPos(2, 1, 2);
        List<BlockPos> trunk = List.of(
                new BlockPos(2, 2, 2), new BlockPos(2, 3, 2), new BlockPos(2, 4, 2), new BlockPos(2, 5, 2));
        List<BlockPos> leaves = List.of(
                new BlockPos(1, 5, 2), new BlockPos(3, 5, 2), new BlockPos(2, 5, 1), new BlockPos(2, 5, 3),
                new BlockPos(2, 6, 2));
        BlockPos beeNest = new BlockPos(3, 3, 2);
        BlockPos persistentLeaf = new BlockPos(1, 3, 2);
        BlockPos mangroveRoots = new BlockPos(6, 1, 6);
        BlockPos air = new BlockPos(6, 4, 6);
        // planted germling against the trunk: must stay out of the plan
        BlockPos groundedPropagule = new BlockPos(1, 2, 2);
        BlockPos hangingPropagule = new BlockPos(5, 3, 5);

        context.setBlock(plot, Blocks.MUDDY_MANGROVE_ROOTS);
        trunk.forEach(pos -> context.setBlock(pos, Blocks.OAK_LOG));
        leaves.forEach(pos -> context.setBlock(pos, naturalLeaves));
        context.setBlock(beeNest, Blocks.BEE_NEST);
        context.setBlock(persistentLeaf, persistentLeaves);
        context.setBlock(mangroveRoots, Blocks.MANGROVE_ROOTS);
        context.setBlock(groundedPropagule.below(), Blocks.DIRT);
        context.setBlock(
                groundedPropagule,
                Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(MangrovePropaguleBlock.HANGING, false));
        context.setBlock(
                hangingPropagule.above(),
                Blocks.MANGROVE_LEAVES.defaultBlockState()
                        .setValue(LeavesBlock.PERSISTENT, false)
                        .setValue(LeavesBlock.DISTANCE, 1));
        context.setBlock(
                hangingPropagule,
                Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(MangrovePropaguleBlock.HANGING, true));

        LevelTreeView view = new LevelTreeView(context.getLevel());
        if (!expectKind(context, view, trunk.get(0), Kind.WOOD, "oak log")
                || !expectKind(context, view, leaves.get(0), Kind.CANOPY, "natural oak leaves")
                || !expectKind(context, view, persistentLeaf, Kind.OTHER, "persistent oak leaves")
                || !expectKind(context, view, beeNest, Kind.NEVER_TOUCH, "bee nest")
                || !expectKind(context, view, plot, Kind.OTHER, "muddy mangrove roots")
                || !expectKind(context, view, mangroveRoots, Kind.WOOD, "mangrove roots")
                || !expectKind(context, view, air, Kind.OTHER, "air")
                || !expectKind(context, view, groundedPropagule, Kind.OTHER, "grounded mangrove propagule")
                || !expectKind(context, view, hangingPropagule, Kind.CANOPY, "hanging mangrove propagule")) {
            return;
        }
        BlockPos unloaded = context.absolutePos(air).offset(1_000_000, 0, 0);
        if (view.kind(unloaded) != Kind.UNLOADED) {
            context.fail("a far-away unloaded position should classify as UNLOADED, was " + view.kind(unloaded));
            return;
        }

        FellPlan plan = TreeFeller.plan(view, context.absolutePos(plot), TreeFeller.Limits.DEFAULT);

        List<BlockPos> expected = List.of(
                        new BlockPos(2, 6, 2),
                        new BlockPos(1, 5, 2),
                        new BlockPos(2, 5, 1),
                        new BlockPos(2, 5, 3),
                        new BlockPos(3, 5, 2),
                        new BlockPos(2, 5, 2),
                        new BlockPos(2, 4, 2),
                        new BlockPos(2, 3, 2),
                        new BlockPos(2, 2, 2))
                .stream()
                .map(context::absolutePos)
                .toList();
        if (!plan.isFound() || !plan.blocks().equals(expected)) {
            context.fail("expected plan " + expected + ", got " + plan);
            return;
        }
        context.succeed();
    }

    private static boolean expectKind(
            GameTestHelper context, LevelTreeView view, BlockPos relative, Kind expected, String what) {
        Kind actual = view.kind(context.absolutePos(relative));
        if (actual != expected) {
            context.fail(what + " should classify as " + expected + ", was " + actual);
            return false;
        }
        return true;
    }
}

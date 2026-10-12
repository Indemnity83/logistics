package com.logistics.gametest.core;

import com.logistics.LogisticsCore;
import com.logistics.core.marker.MarkerBlock;
import com.logistics.core.marker.MarkerBlockEntity;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared Marker GameTest bodies, compiled directly into both loaders' {@code gametest} source sets
 * (see {@code common/build.gradle}). Fabric's {@code MarkerGameTest} delegates to these methods and
 * NeoForge's {@code MarkerGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Markers find each other by scanning up to {@code MarkerManager.MAX_MARKER_DISTANCE} blocks
 * along the four horizontal directions at their own height, which reaches well into neighbouring
 * test structures. Each test therefore builds on its own layer ({@code y}), so no two tests'
 * markers ever share a row.
 *
 * <p>Markers are activated the way a player does it: a {@link FeedbackRecordingPlayer} holding a
 * real Wrench uses it on the block, which reaches {@code MarkerBlock#onWrench} through
 * {@code WrenchItem#useOn}.
 */
public class MarkerGameTestBody {

    private static final String AREA_DEFINED = "marker.activated.detail.area_defined";
    private static final String ACTIVATED_SOLO = "marker.activated.solo";
    private static final String DEACTIVATED = "marker.deactivated";
    private static final String INVALID_L_SHAPE = "marker.activation.failed.invalid_l_shape";
    private static final String TOO_SMALL = "marker.activation.failed.too_small";

    /**
     * Wiki claim (Defining an area): "Place three markers in a right-triangle (L-shape)... Activate
     * the markers with the Wrench to project the resulting rectangular boundary." and "A successful
     * activation reports the defined area size (e.g. area defined: 9x9)."
     *
     * <p>Wrenching the corner of a 4x3 L activates all three markers with the same rectangle,
     * flags only the corner as the corner, and reports {@code 4x3}.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    public static void testWrenchingCornerDefinesArea(GameTestHelper context) {
        LShape shape = LShape.at(1);
        shape.place(context);
        FeedbackRecordingPlayer player = FeedbackRecordingPlayer.holdingWrench(context);

        context.useBlock(shape.corner(), player);

        if (!player.lastFeedbackIs(context, AREA_DEFINED, 4, 3)) return;
        if (!shape.assertActivated(context)) return;
        context.succeed();
    }

    /**
     * Wiki claim (Defining an area): "Place three markers in a right-triangle (L-shape)... Activate
     * the markers with the Wrench." Any marker of the L will do — wrenching an end of one leg finds
     * the corner through the other marker and activates the whole set, not just the one clicked.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    public static void testWrenchingEdgeMarkerActivatesWholeSet(GameTestHelper context) {
        LShape shape = LShape.at(2);
        shape.place(context);
        FeedbackRecordingPlayer player = FeedbackRecordingPlayer.holdingWrench(context);

        context.useBlock(shape.zEdge(), player);

        if (!player.lastFeedbackIs(context, AREA_DEFINED, 4, 3)) return;
        if (!shape.assertActivated(context)) return;
        context.succeed();
    }

    /**
     * Wiki claim (Defining an area): "Activation fails if there are not three markers in a right
     * triangle." Two markers in a line with no third to turn the corner leave both inactive.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    public static void testActivationFailsWithoutRightTriangle(GameTestHelper context) {
        int y = 3;
        BlockPos first = new BlockPos(0, y, 0);
        BlockPos second = new BlockPos(3, y, 0);
        placeMarker(context, first);
        placeMarker(context, second);
        FeedbackRecordingPlayer player = FeedbackRecordingPlayer.holdingWrench(context);

        context.useBlock(first, player);

        if (!player.lastFeedbackIs(context, INVALID_L_SHAPE)) return;
        if (!assertAllInactive(context, first, second)) return;
        context.succeed();
    }

    /**
     * Wiki claim (Defining an area): "Activation fails ... if the resulting area is below the
     * minimum size." An L only two blocks wide is refused, the feedback names the 3x3 minimum, and
     * no marker of the set activates.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    public static void testActivationFailsBelowMinimumSize(GameTestHelper context) {
        int y = 4;
        BlockPos corner = new BlockPos(0, y, 0);
        BlockPos xEdge = new BlockPos(1, y, 0);
        BlockPos zEdge = new BlockPos(0, y, 2);
        placeMarker(context, corner);
        placeMarker(context, xEdge);
        placeMarker(context, zEdge);
        FeedbackRecordingPlayer player = FeedbackRecordingPlayer.holdingWrench(context);

        context.useBlock(corner, player);

        if (!player.lastFeedbackIs(context, TOO_SMALL, 3, 3)) return;
        if (!assertAllInactive(context, corner, xEdge, zEdge)) return;
        context.succeed();
    }

    /**
     * Wiki claim (Defining an area): "Activation fails if there are not three markers in a right
     * triangle."
     *
     * <p>NOTE: a marker with no other marker in line with it is the exception — it activates on its
     * own (lit, projecting its beams) with no area defined, rather than failing. Asserts the code's
     * behavior; see WIKI_DISCREPANCIES.md § Marker.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    public static void testLoneMarkerActivatesWithoutArea(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 5, 0);
        placeMarker(context, pos);
        FeedbackRecordingPlayer player = FeedbackRecordingPlayer.holdingWrench(context);

        context.useBlock(pos, player);

        if (!player.lastFeedbackIs(context, ACTIVATED_SOLO)) return;
        MarkerBlockEntity marker = markerAt(context, pos);
        if (marker == null) return;
        if (!marker.isActive()) {
            context.fail("A lone marker should activate on its own when wrenched");
            return;
        }
        if (marker.hasValidBounds() || !marker.getConnectedMarkers().isEmpty()) {
            context.fail("A lone marker should define no area and have no connections");
            return;
        }
        context.succeed();
    }

    /**
     * Wiki claim (Defining an area): "Wrenching an active marker again deactivates the connected
     * set." Wrenching one end of an active L — not the marker that was clicked to activate it —
     * turns off all three and clears the area they defined.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    public static void testWrenchingActiveMarkerDeactivatesConnectedSet(GameTestHelper context) {
        LShape shape = LShape.at(6);
        shape.place(context);
        FeedbackRecordingPlayer player = FeedbackRecordingPlayer.holdingWrench(context);

        context.useBlock(shape.corner(), player);
        if (!shape.assertActivated(context)) return;

        context.useBlock(shape.xEdge(), player);

        if (!player.lastFeedbackIs(context, DEACTIVATED)) return;
        if (!assertAllInactive(context, shape.corner(), shape.xEdge(), shape.zEdge())) return;
        context.succeed();
    }

    /**
     * Breaking one marker of an active set turns the rest of the set off, so no surviving marker
     * keeps advertising an area whose outline is no longer standing.
     */
    public static void testBreakingActiveMarkerDeactivatesTheRest(GameTestHelper context) {
        LShape shape = LShape.at(7);
        shape.place(context);
        FeedbackRecordingPlayer player = FeedbackRecordingPlayer.holdingWrench(context);

        context.useBlock(shape.corner(), player);
        if (!shape.assertActivated(context)) return;

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.gameMode.destroyBlock(context.absolutePos(shape.xEdge()));

        if (context.getBlockState(shape.xEdge()).getBlock() instanceof MarkerBlock) {
            context.fail("The broken marker should be gone");
            return;
        }
        if (!assertAllInactive(context, shape.corner(), shape.zEdge())) return;
        context.succeed();
    }

    /**
     * Wiki claims (infobox + Defining an area): hardness 0, blast resistance 0, drops Marker; and
     * "Markers ... break instantly with any tool, dropping themselves."
     *
     * <p>Broken by a survival player's bare hand, the slowest tool there is, so it breaks instantly
     * with anything.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    public static void testMarkerBreaksInstantlyAndDropsItself(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 8, 0);
        placeMarker(context, pos);
        BlockPos absolute = context.absolutePos(pos);
        BlockState state = context.getBlockState(pos);

        if (state.getDestroySpeed(context.getLevel(), absolute) != 0.0f) {
            context.fail("Marker hardness should be 0, was " + state.getDestroySpeed(context.getLevel(), absolute));
            return;
        }
        if (state.getBlock().getExplosionResistance() != 0.0f) {
            context.fail("Marker blast resistance should be 0, was " + state.getBlock().getExplosionResistance());
            return;
        }

        FeedbackRecordingPlayer player = new FeedbackRecordingPlayer(context);
        float progress = state.getDestroyProgress(player, context.getLevel(), absolute);
        if (progress < 1.0f) {
            context.fail("A bare hand should break a marker in one tick, but progressed only " + progress);
            return;
        }

        player.gameMode.destroyBlock(absolute);

        if (!context.getBlockState(pos).isAir()) {
            context.fail("Marker should be broken");
            return;
        }
        // Asserted at the drop, before the item can drift towards a neighbouring structure.
        context.assertItemEntityCountIs(LogisticsCore.BLOCK.MARKER.asItem(), pos, 1.5, 1);
        context.succeed();
    }

    /**
     * Wiki claims (infobox + Defining an area): stackable to 64; "Markers have no collision — you
     * can walk through them."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Defining_an_area">wiki/Marker.txt § Defining an area</a>
     */
    public static void testMarkerHasNoCollisionAndStacksTo64(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 9, 0);
        placeMarker(context, pos);

        if (!context.getBlockState(pos).getCollisionShape(context.getLevel(), context.absolutePos(pos)).isEmpty()) {
            context.fail("A marker should have no collision shape");
            return;
        }
        int maxStack = new ItemStack(LogisticsCore.BLOCK.MARKER).getMaxStackSize();
        if (maxStack != 64) {
            context.fail("Markers should stack to 64, stack to " + maxStack);
            return;
        }
        context.succeed();
    }

    /**
     * An active marker's area and connections are its own saved state — nothing in the world
     * rebuilds them — so they must come back from NBT when the block entity is reconstructed.
     */
    public static void testActiveMarkerSurvivesReconstruction(GameTestHelper context) {
        LShape shape = LShape.at(10);
        shape.place(context);
        FeedbackRecordingPlayer player = FeedbackRecordingPlayer.holdingWrench(context);

        context.useBlock(shape.corner(), player);
        if (!shape.assertActivated(context)) return;

        HolderLookup.Provider registries = context.getLevel().registryAccess();
        MarkerBlockEntity original = markerAt(context, shape.corner());
        if (original == null) return;
        CompoundTag saved = original.saveCustomOnly(registries);
        BlockState activeState = context.getBlockState(shape.corner());

        // Block entity first, as a chunk unload, so removing the block leaves the rest of the set active.
        context.getLevel().removeBlockEntity(context.absolutePos(shape.corner()));
        context.setBlock(shape.corner(), Blocks.AIR);
        context.setBlock(shape.corner(), activeState);

        MarkerBlockEntity rebuilt = markerAt(context, shape.corner());
        if (rebuilt == null) return;
        if (rebuilt.hasValidBounds() || !rebuilt.getConnectedMarkers().isEmpty()) {
            context.fail("A replaced marker should start with no area before its saved data is loaded");
            return;
        }
        rebuilt.loadCustomOnly(saved, registries);

        if (!shape.assertActivated(context)) return;
        context.succeed();
    }

    /**
     * A marker needs a solid block under it, so removing that block pops the marker off and drops
     * it, as a torch does.
     */
    public static void testMarkerPopsOffWhenFloorRemoved(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 11, 0);
        placeMarker(context, pos);

        context.setBlock(pos.below(), Blocks.AIR);

        if (!context.getBlockState(pos).isAir()) {
            context.fail("A marker with nothing under it should pop off, but found " + context.getBlockState(pos));
            return;
        }
        // Asserted at the drop, before the item can drift towards a neighbouring structure.
        context.assertItemEntityCountIs(LogisticsCore.BLOCK.MARKER.asItem(), pos, 1.5, 1);
        context.succeed();
    }

    /**
     * Removing one marker of an active set without a player — here by replacing it directly, as a
     * machine or another mod would — turns the rest of the set off, the same as breaking it.
     */
    public static void testRemovingActiveMarkerWithoutPlayerDeactivatesTheRest(GameTestHelper context) {
        LShape shape = LShape.at(12);
        shape.place(context);
        FeedbackRecordingPlayer player = FeedbackRecordingPlayer.holdingWrench(context);

        context.useBlock(shape.corner(), player);
        if (!shape.assertActivated(context)) return;

        context.setBlock(shape.xEdge(), Blocks.AIR);

        if (!assertAllInactive(context, shape.corner(), shape.zEdge())) return;
        context.succeed();
    }

    /**
     * An active marker that pops off because its floor was removed takes the rest of its set down
     * with it.
     */
    public static void testActiveMarkerPoppingOffDeactivatesTheRest(GameTestHelper context) {
        LShape shape = LShape.at(13);
        shape.place(context);
        FeedbackRecordingPlayer player = FeedbackRecordingPlayer.holdingWrench(context);

        context.useBlock(shape.corner(), player);
        if (!shape.assertActivated(context)) return;

        context.setBlock(shape.xEdge().below(), Blocks.AIR);

        if (!context.getBlockState(shape.xEdge()).isAir()) {
            context.fail("The marker with nothing under it should pop off");
            return;
        }
        context.assertItemEntityCountIs(LogisticsCore.BLOCK.MARKER.asItem(), shape.xEdge(), 1.5, 1);
        if (!assertAllInactive(context, shape.corner(), shape.zEdge())) return;
        context.succeed();
    }

    private static void placeMarker(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos.below(), Blocks.STONE);
        context.setBlock(pos, LogisticsCore.BLOCK.MARKER);
    }

    private static MarkerBlockEntity markerAt(GameTestHelper context, BlockPos pos) {
        MarkerBlockEntity marker = (MarkerBlockEntity) context.getBlockEntity(pos);
        if (marker == null) {
            context.fail("Expected a marker block entity at " + pos);
        }
        return marker;
    }

    private static boolean assertAllInactive(GameTestHelper context, BlockPos... positions) {
        for (BlockPos pos : positions) {
            MarkerBlockEntity marker = markerAt(context, pos);
            if (marker == null) return false;
            if (marker.isActive()) {
                context.fail("Marker at " + pos + " should be inactive");
                return false;
            }
            if (marker.hasValidBounds() || !marker.getConnectedMarkers().isEmpty()) {
                context.fail("Inactive marker at " + pos + " should define no area and have no connections");
                return false;
            }
        }
        return true;
    }

    /**
     * Three markers on layer {@code y}: a corner, one leg three blocks along X and one two blocks
     * along Z — a 4x3 area, the markers' own columns included.
     */
    private record LShape(BlockPos corner, BlockPos xEdge, BlockPos zEdge) {

        static LShape at(int y) {
            return new LShape(new BlockPos(0, y, 0), new BlockPos(3, y, 0), new BlockPos(0, y, 2));
        }

        void place(GameTestHelper context) {
            placeMarker(context, corner);
            placeMarker(context, xEdge);
            placeMarker(context, zEdge);
        }

        /** All three markers active, sharing the 4x3 rectangle, each connected to the other two. */
        boolean assertActivated(GameTestHelper context) {
            BlockPos a = context.absolutePos(corner);
            BlockPos b = context.absolutePos(new BlockPos(xEdge.getX(), corner.getY(), zEdge.getZ()));
            BlockPos expectedMin = new BlockPos(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()));
            BlockPos expectedMax = new BlockPos(Math.max(a.getX(), b.getX()), a.getY(), Math.max(a.getZ(), b.getZ()));

            for (BlockPos pos : List.of(corner, xEdge, zEdge)) {
                MarkerBlockEntity marker = markerAt(context, pos);
                if (marker == null) return false;
                if (!marker.isActive()) {
                    context.fail("Marker at " + pos + " should be active");
                    return false;
                }
                if (!expectedMin.equals(marker.getBoundMin()) || !expectedMax.equals(marker.getBoundMax())) {
                    context.fail("Marker at " + pos + " should define " + expectedMin + " to " + expectedMax + ", defines "
                            + marker.getBoundMin() + " to " + marker.getBoundMax());
                    return false;
                }
                if (marker.isCornerMarker() != pos.equals(corner)) {
                    context.fail("Only the corner marker should be flagged as the corner, but " + pos + " is "
                            + (marker.isCornerMarker() ? "" : "not ") + "flagged");
                    return false;
                }
                Set<BlockPos> expectedConnections = new HashSet<>();
                for (BlockPos other : List.of(corner, xEdge, zEdge)) {
                    if (!other.equals(pos)) expectedConnections.add(context.absolutePos(other));
                }
                if (!Set.copyOf(marker.getConnectedMarkers()).equals(expectedConnections)
                        || marker.getConnectedMarkers().size() != 2) {
                    context.fail("Marker at " + pos + " should connect to the other two, connects to "
                            + marker.getConnectedMarkers());
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * A survival server player that records the chat feedback it is sent instead of sending it.
     * Never logged in or added to the level, so it needs no network connection and leaves nothing
     * behind in the shared test level.
     */
    private static final class FeedbackRecordingPlayer extends ServerPlayer {
        private final List<Component> feedback = new ArrayList<>();

        FeedbackRecordingPlayer(GameTestHelper context) {
            super(
                    context.getLevel().getServer(),
                    context.getLevel(),
                    new GameProfile(UUID.randomUUID(), "marker-test-player"),
                    ClientInformation.createDefault());
            // The test server's default game mode is creative, whose players break blocks without drops.
            GameType.SURVIVAL.updatePlayerAbilities(getAbilities());
        }

        static FeedbackRecordingPlayer holdingWrench(GameTestHelper context) {
            FeedbackRecordingPlayer player = new FeedbackRecordingPlayer(context);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LogisticsCore.ITEM.WRENCH));
            return player;
        }

        @Override
        public boolean isCreative() {
            return false;
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public void sendSystemMessage(Component message, boolean overlay) {
            feedback.add(message);
        }

        /** Fails the test unless the latest feedback is the translation {@code key} with {@code args}. */
        boolean lastFeedbackIs(GameTestHelper context, String key, Object... args) {
            if (feedback.isEmpty()) {
                context.fail("Expected feedback '" + key + "', but the player was sent nothing");
                return false;
            }
            Component last = feedback.get(feedback.size() - 1);
            String actual = last.getContents() instanceof TranslatableContents translatable
                    ? translatable.getKey() + Arrays.toString(translatable.getArgs())
                    : last.getString();
            String expected = key + Arrays.toString(args);
            if (!expected.equals(actual)) {
                context.fail("Expected feedback '" + expected + "', got '" + actual + "'");
                return false;
            }
            return true;
        }
    }
}

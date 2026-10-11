package com.logistics.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/**
 * Contracts the shared bodies rely on from the GameTest harness itself, rather than from the mod.
 */
public class HarnessGameTestBody {

    /** The x/y/z extent every shared body is laid out in, from relative (0, 0, 0). */
    private static final int FOOTPRINT = 8;

    /**
     * The whole footprint is ready when a body starts, not just the chunk the structure sits in.
     *
     * <p>The runner force-loads only the chunks under a test's structure, and waits only for those
     * to be entity-ticking. An entity added in any other chunk can land before the entity manager
     * has caught up with it, and is then invisible to AABB queries for a few ticks: a stack spawned
     * beside a quarry reads as absent, and a broken chest's contents cannot be collected. So the
     * structure has to cover everything the bodies touch.
     */
    public static void testStructureCoversTheSharedFootprint(GameTestHelper context) {
        AABB bounds = context.getBounds();
        if (bounds.getXsize() < FOOTPRINT || bounds.getYsize() < FOOTPRINT || bounds.getZsize() < FOOTPRINT) {
            context.fail("The test structure is " + (int) bounds.getXsize() + "x" + (int) bounds.getYsize() + "x"
                    + (int) bounds.getZsize() + ", smaller than the " + FOOTPRINT + "x" + FOOTPRINT + "x" + FOOTPRINT
                    + " footprint the shared bodies are laid out in");
            return;
        }

        ServerLevel level = context.getLevel();
        BlockPos corner = context.absolutePos(new BlockPos(FOOTPRINT - 1, 0, FOOTPRINT - 1));
        BlockPos origin = context.absolutePos(BlockPos.ZERO);
        int fromX = SectionPos.blockToSectionCoord(Math.min(origin.getX(), corner.getX()));
        int toX = SectionPos.blockToSectionCoord(Math.max(origin.getX(), corner.getX()));
        int fromZ = SectionPos.blockToSectionCoord(Math.min(origin.getZ(), corner.getZ()));
        int toZ = SectionPos.blockToSectionCoord(Math.max(origin.getZ(), corner.getZ()));
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                if (!entitiesVisibleIn(level, new ChunkPos(x, z))) {
                    context.fail("Chunk " + x + ", " + z + " under the footprint is not entity-ticking at the start of the test");
                    return;
                }
            }
        }

        BlockPos farCorner = new BlockPos(FOOTPRINT - 1, 1, FOOTPRINT - 1);
        ItemEntity item = context.spawnItem(Items.EMERALD, farCorner);
        boolean found = !level.getEntitiesOfClass(ItemEntity.class, new AABB(context.absolutePos(farCorner)).inflate(1.0),
                entity -> entity == item).isEmpty();
        item.discard();
        if (!found) {
            context.fail("An item spawned at the far corner of the footprint was invisible to an area query");
            return;
        }
        context.succeed();
    }

    /** Whether an entity added to {@code chunk} now is visible to AABB queries. */
    private static boolean entitiesVisibleIn(ServerLevel level, ChunkPos chunk) {
        return level.areEntitiesLoaded(chunk.toLong()) && level.isPositionEntityTicking(chunk.getWorldPosition());
    }
}

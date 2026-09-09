package com.logistics.power.cable;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsPower;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The render mask is authored by the server and synced to the client, while the connection cache
 * that {@code CableBlock.getShape} reads is rebuilt from whatever view of the world the caller has.
 * A client-side rebuild — a shape query, e.g. the outline highlight while the player looks at the
 * cable — must therefore refresh the cache without replacing the synced mask the renderer draws.
 */
class CableRenderConnectionMaskTest extends MinecraftTestEnvironment {

    private static CableBlock cableBlock;

    private static final Function<Direction, CableBlock.ConnectionType> ONE_ARM_NORTH =
            dir -> dir == Direction.NORTH ? CableBlock.ConnectionType.CABLE : CableBlock.ConnectionType.NONE;
    private static final Function<Direction, CableBlock.ConnectionType> ONE_ARM_SOUTH =
            dir -> dir == Direction.SOUTH ? CableBlock.ConnectionType.DEVICE : CableBlock.ConnectionType.NONE;

    @BeforeAll
    static void createTestCable() {
        cableBlock = new CableBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.parse("logistics:test_cable"))));
        // The loader service that normally builds block-entity types has no implementation on the
        // common test classpath, and BlockEntityType's constructor is private here, so borrow a
        // vanilla type. Nothing under test reads it — the block entity only needs a non-null type
        // to construct.
        LogisticsPower.ENTITY.CABLE_BLOCK_ENTITY = borrowedType();
    }

    @SuppressWarnings("unchecked")
    private static BlockEntityType<CableBlockEntity> borrowedType() {
        return (BlockEntityType<CableBlockEntity>) (BlockEntityType<?>) BlockEntityType.FURNACE;
    }

    private static CableBlockEntity cable() {
        // BlockEntity's constructor validates the state against the type's valid blocks, and the
        // borrowed vanilla type only accepts its own block. The connection resolver is injected, so
        // the state is inert here.
        return new CableBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
    }

    @Test
    void authoritativeRebuildPublishesTheRenderMask() {
        CableBlockEntity cable = cable();

        cable.rebuildConnectionCache(ONE_ARM_NORTH, true);

        assertThat(cable.getRenderConnectionMask())
                .as("the server's own view of its neighbours is what gets synced and drawn")
                .isEqualTo(maskOf(ONE_ARM_NORTH));
    }

    @Test
    void clientSideRebuildKeepsTheSyncedRenderMask() {
        CableBlockEntity cable = cable();
        cable.rebuildConnectionCache(ONE_ARM_NORTH, true);
        int syncedMask = cable.getRenderConnectionMask();

        // A neighbour change invalidates the cache; a shape query then rebuilds it client-side,
        // where the local view disagrees with what the server last sent.
        cable.invalidateConnectionCache();
        cable.rebuildConnectionCache(ONE_ARM_SOUTH, false);

        assertThat(cable.getRenderConnectionMask())
                .as("a client-side rebuild must not replace the server-authored render mask")
                .isEqualTo(syncedMask);
    }

    @Test
    void clientSideRebuildStillRefreshesTheShapeCache() {
        CableBlockEntity cable = cable();
        cable.rebuildConnectionCache(ONE_ARM_NORTH, true);

        cable.invalidateConnectionCache();
        cable.rebuildConnectionCache(ONE_ARM_SOUTH, false);

        assertThat(cable.getCachedConnectionType(Direction.SOUTH)).isEqualTo(CableBlock.ConnectionType.DEVICE);
        assertThat(cable.getCachedConnectionType(Direction.NORTH)).isEqualTo(CableBlock.ConnectionType.NONE);
    }

    private static int maskOf(Function<Direction, CableBlock.ConnectionType> connections) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            CableBlock.ConnectionType type = connections.apply(dir);
            if (type != CableBlock.ConnectionType.NONE) {
                mask |= type.ordinal() << (dir.get3DDataValue() * 2);
            }
        }
        return mask;
    }
}

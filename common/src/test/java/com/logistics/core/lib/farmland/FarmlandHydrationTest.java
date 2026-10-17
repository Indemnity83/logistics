package com.logistics.core.lib.farmland;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** No level can be built in a unit test, so every source here lives in the null level. */
@DisplayName("Farmland hydration registry")
class FarmlandHydrationTest {

    private static final ServerLevel LEVEL = null;

    /** Wets every position it is asked about, recording each. */
    private static final class Source implements HydrationSource {
        final BoundingBox bounds;
        final List<BlockPos> asked = new ArrayList<>();
        boolean wets = true;

        Source(BoundingBox bounds) {
            this.bounds = bounds;
        }

        @Override
        public BlockPos anchor() {
            return bounds.getCenter();
        }

        @Override
        public BoundingBox bounds() {
            return bounds;
        }

        @Override
        public boolean hydrate(ServerLevel level, BlockPos pos) {
            asked.add(pos.immutable());
            return wets;
        }
    }

    private final List<FarmlandHydration.Registration> registrations = new ArrayList<>();

    private FarmlandHydration.Registration register(Source source) {
        FarmlandHydration.Registration registration = FarmlandHydration.register(LEVEL, source);
        registrations.add(registration);
        return registration;
    }

    @AfterEach
    void unregisterAll() {
        registrations.forEach(FarmlandHydration.Registration::unregister);
        FarmlandHydration.installHook((level, source) -> () -> {});
    }

    @Test
    @DisplayName("a registered source wets what its bounds hold, and is never asked about anything else")
    void wetsOnlyInsideBounds() {
        Source source = new Source(new BoundingBox(-5, 64, -5, 5, 64, 5));
        register(source);

        assertThat(FarmlandHydration.hydrate(LEVEL, new BlockPos(5, 64, -5))).isTrue();
        assertThat(FarmlandHydration.hydrate(LEVEL, new BlockPos(6, 64, 0))).isFalse();
        assertThat(FarmlandHydration.hydrate(LEVEL, new BlockPos(0, 65, 0))).isFalse();
        assertThat(FarmlandHydration.hydrate(LEVEL, new BlockPos(100, 64, 100))).isFalse();

        assertThat(source.asked).containsExactly(new BlockPos(5, 64, -5));
    }

    @Test
    @DisplayName("a source spanning several chunks answers in each of them")
    void spansChunks() {
        Source source = new Source(new BoundingBox(-20, 64, -20, 20, 64, 20));
        register(source);

        for (int x = -20; x <= 20; x += 8) {
            for (int z = -20; z <= 20; z += 8) {
                assertThat(FarmlandHydration.hydrate(LEVEL, new BlockPos(x, 64, z))).as("%s, %s", x, z).isTrue();
            }
        }
        assertThat(FarmlandHydration.sourceCount(LEVEL)).isOne();
    }

    @Test
    @DisplayName("unregistering ends a source everywhere, once, however often it is called")
    void unregisters() {
        Source source = new Source(new BoundingBox(-20, 64, -20, 20, 64, 20));
        FarmlandHydration.Registration registration = register(source);

        registration.unregister();
        registration.unregister();

        assertThat(FarmlandHydration.hydrate(LEVEL, new BlockPos(0, 64, 0))).isFalse();
        assertThat(FarmlandHydration.hydrate(LEVEL, new BlockPos(-20, 64, 20))).isFalse();
        assertThat(source.asked).isEmpty();
        assertThat(FarmlandHydration.sourceCount(LEVEL)).isZero();
        assertThat(FarmlandHydration.levelCount()).as("no level kept without sources").isZero();
    }

    @Test
    @DisplayName("re-registering with wider bounds replaces the old ones: the new edge wets, nothing is left behind")
    void resizes() {
        FarmlandHydration.Registration small = register(new Source(new BoundingBox(-5, 64, -5, 5, 64, 5)));
        BlockPos edge = new BlockPos(9, 64, 9);
        assertThat(FarmlandHydration.hydrate(LEVEL, edge)).isFalse();

        small.unregister();
        Source wide = new Source(new BoundingBox(-9, 64, -9, 9, 64, 9));
        register(wide);

        assertThat(FarmlandHydration.hydrate(LEVEL, edge)).isTrue();
        assertThat(FarmlandHydration.sourceCount(LEVEL)).isOne();
        assertThat(wide.asked).containsExactly(edge);
    }

    @Test
    @DisplayName("overlapping sources: the first that wets pays; one that declines passes the check on")
    void overlapping() {
        Source declines = new Source(new BoundingBox(-5, 64, -5, 5, 64, 5));
        declines.wets = false;
        Source wets = new Source(new BoundingBox(0, 64, 0, 10, 64, 10));
        register(declines);
        register(wets);

        assertThat(FarmlandHydration.hydrate(LEVEL, new BlockPos(2, 64, 2))).isTrue();
        assertThat(declines.asked).containsExactly(new BlockPos(2, 64, 2));
        assertThat(wets.asked).containsExactly(new BlockPos(2, 64, 2));

        declines.wets = true;
        assertThat(FarmlandHydration.hydrate(LEVEL, new BlockPos(3, 64, 3))).isTrue();
        assertThat(wets.asked).as("the first paid").hasSize(1);
    }

    @Test
    @DisplayName("clearing a level ends every source in it and the loader's part of each")
    void clearsLevel() {
        List<HydrationSource> detached = new ArrayList<>();
        FarmlandHydration.installHook((level, source) -> () -> detached.add(source));
        Source first = new Source(new BoundingBox(-5, 64, -5, 5, 64, 5));
        Source second = new Source(new BoundingBox(30, 64, 30, 40, 64, 40));
        FarmlandHydration.Registration registration = register(first);
        register(second);

        FarmlandHydration.clearLevel(LEVEL);

        assertThat(FarmlandHydration.hydrate(LEVEL, new BlockPos(0, 64, 0))).isFalse();
        assertThat(FarmlandHydration.hydrate(LEVEL, new BlockPos(35, 64, 35))).isFalse();
        assertThat(FarmlandHydration.levelCount()).isZero();
        assertThat(detached).containsExactly(first, second);

        registration.unregister();
        assertThat(detached).as("ended once").hasSize(2);
    }

    @Test
    @DisplayName("the index files a source under every chunk its bounds touch, and empties as sources go")
    void indexChunks() {
        HydrationIndex index = new HydrationIndex();
        Source source = new Source(new BoundingBox(-1, 64, -1, 16, 64, 0));

        index.add(source);
        assertThat(index.chunkCount()).as("x -1..16 and z -1..0: 3 by 2 chunks").isEqualTo(6);

        index.remove(source);
        assertThat(index.chunkCount()).isZero();
        assertThat(index.isEmpty()).isTrue();
    }
}

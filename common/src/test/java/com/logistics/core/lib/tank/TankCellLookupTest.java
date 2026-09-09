package com.logistics.core.lib.tank;

import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.test.MinecraftTestEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Covers the cross-mod finder registry and the column-global gas predicate.
 *
 * <p>{@link TankCellLookup} holds process-wide static state with no reset hook, so each test uses its own
 * far-apart {@link BlockPos}es: registered finders only ever match their own sentinel positions, which keeps
 * the suite independent of registration order and of finders left behind by earlier tests.
 */
@DisplayName("TankCellLookup")
class TankCellLookupTest extends MinecraftTestEnvironment {

    /** A cell whose identity we can assert against; contents are irrelevant here. */
    private record StubCell(String tag) implements TankCell {
        @Override public IFluidKey fluid() { return SimpleFluidKey.BLANK; }
        @Override public long amount() { return 0; }
        @Override public long capacity() { return 0; }
        @Override public void setContents(IFluidKey fluid, long amount) {}
    }

    @Test
    @DisplayName("find returns null when no finder matches")
    void findNoMatch() {
        assertThat(TankCellLookup.find(null, new BlockPos(1000, 0, 0))).isNull();
    }

    @Test
    @DisplayName("find returns the cell from a matching finder")
    void findMatch() {
        BlockPos pos = new BlockPos(1010, 0, 0);
        StubCell cell = new StubCell("a");
        TankCellLookup.register((level, p) -> p.equals(pos) ? cell : null);

        assertThat(TankCellLookup.find(null, pos)).isSameAs(cell);
    }

    @Test
    @DisplayName("find consults the chain and the first non-null result wins")
    void findFirstNonNullWins() {
        BlockPos pos = new BlockPos(1020, 0, 0);
        StubCell first = new StubCell("first");
        StubCell second = new StubCell("second");
        TankCellLookup.register((level, p) -> p.equals(pos) ? first : null);
        TankCellLookup.register((level, p) -> p.equals(pos) ? second : null);

        assertThat(TankCellLookup.find(null, pos)).isSameAs(first);
    }

    @Test
    @DisplayName("find skips finders that return null and falls through to the next")
    void findFallsThrough() {
        BlockPos pos = new BlockPos(1030, 0, 0);
        StubCell match = new StubCell("match");
        TankCellLookup.register((level, p) -> null); // never matches
        TankCellLookup.register((level, p) -> p.equals(pos) ? match : null);

        assertThat(TankCellLookup.find(null, pos)).isSameAs(match);
    }

    @Test
    @DisplayName("a finder that throws is skipped and the next finder still resolves")
    void findSkipsThrowingFinder() {
        BlockPos pos = new BlockPos(1040, 0, 0);
        StubCell match = new StubCell("match");
        // Throw only at this test's sentinel position so the finder doesn't pollute other tests' lookups.
        TankCellLookup.register((level, p) -> {
            if (p.equals(pos)) {
                throw new IllegalStateException("buggy third-party finder");
            }
            return null;
        });
        TankCellLookup.register((level, p) -> p.equals(pos) ? match : null);

        // The throwing finder must not abort the lookup for every tank.
        assertThat(TankCellLookup.find(null, pos)).isSameAs(match);
    }

    @Test
    @DisplayName("register rejects a null finder")
    void registerRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> TankCellLookup.register(null));
    }

    @Test
    @DisplayName("registerGasPredicate rejects a null predicate")
    void registerGasPredicateRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> TankCellLookup.registerGasPredicate(null));
    }

    @Test
    @DisplayName("isGas defaults to false before any predicate is registered")
    void gasDefaultsFalse() throws Exception {
        // The predicate is process-wide static state with no reset hook, so the as-shipped default is
        // only observable on a copy of the class nothing has registered into. Registering "everything
        // is a liquid" here and asserting it would just be gasPredicateLastWins under another name.
        assertThat(isGasOnAnUnconfiguredLookup(SimpleFluidKey.of(Fluids.WATER))).isFalse();
        assertThat(isGasOnAnUnconfiguredLookup(SimpleFluidKey.of(Fluids.LAVA))).isFalse();
    }

    private static boolean isGasOnAnUnconfiguredLookup(IFluidKey fluid) throws Exception {
        Class<?> fresh = new UnconfiguredLookupLoader().loadClass(TankCellLookup.class.getName());
        assertThat(fresh).as("a separate class, with its own statics").isNotSameAs(TankCellLookup.class);
        return (boolean) fresh.getMethod("isGas", IFluidKey.class).invoke(null, fluid);
    }

    /** Loads {@link TankCellLookup} itself and delegates everything else, so its statics start unset. */
    private static final class UnconfiguredLookupLoader extends ClassLoader {
        private UnconfiguredLookupLoader() {
            super(TankCellLookup.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals(TankCellLookup.class.getName())) {
                return super.loadClass(name, resolve);
            }
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                byte[] bytes;
                try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                    bytes = Objects.requireNonNull(in, "TankCellLookup class bytes").readAllBytes();
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
                loaded = defineClass(name, bytes, 0, bytes.length);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    @Test
    @DisplayName("isGas reflects the registered predicate")
    void gasUsesPredicate() {
        IFluidKey lava = SimpleFluidKey.of(Fluids.LAVA);
        TankCellLookup.registerGasPredicate(key -> key.equals(lava));

        assertThat(TankCellLookup.isGas(lava)).isTrue();
        assertThat(TankCellLookup.isGas(SimpleFluidKey.of(Fluids.WATER))).isFalse();
    }

    @Test
    @DisplayName("registering a gas predicate replaces the previous one")
    void gasPredicateLastWins() {
        TankCellLookup.registerGasPredicate(key -> true);
        TankCellLookup.registerGasPredicate(key -> false);

        assertThat(TankCellLookup.isGas(SimpleFluidKey.of(Fluids.WATER))).isFalse();
    }
}

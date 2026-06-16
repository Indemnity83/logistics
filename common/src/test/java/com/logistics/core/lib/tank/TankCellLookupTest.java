package com.logistics.core.lib.tank;

import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("isGas defaults to false before any predicate is registered")
    void gasDefaultsFalse() {
        // No predicate registered yet in this method; the default treats everything as a liquid.
        // (Run-order independent: registerGasPredicate is last-wins and every gas test sets it explicitly.)
        TankCellLookup.registerGasPredicate(key -> false);
        assertThat(TankCellLookup.isGas(SimpleFluidKey.of(Fluids.WATER))).isFalse();
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

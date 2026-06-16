package com.logistics.core.lib.tank;

import com.logistics.core.lib.fluids.IFluidKey;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader- and mod-agnostic registry for discovering a {@link TankCell} at a world position, so a vertical
 * tank column can span block entities from different mods.
 *
 * <p>Unlike {@link com.logistics.core.lib.fluids.FluidStorageLookup} this holds a <em>chain</em> of
 * finders, not a single one: each mod registers its own finder, and {@link #find} returns the first
 * non-null result. Logistics registers a default finder during init that catches any block entity
 * implementing {@link TankCell} (its own Glass Tank). A companion mod that soft-depends on logistics
 * registers a finder wrapping its tank block entities in a {@code TankCell} adapter — it must not make
 * its block entity {@code implements TankCell} directly, since that interface is absent when logistics is
 * not installed.
 *
 * <p>The gas predicate is registered here too so every cell in a cross-mod column settles by one rule
 * (liquids to the bottom, gases to the top); see {@link TankColumns}.
 */
public final class TankCellLookup {

    /** Finds a {@link TankCell} for a given world position, or {@code null} if none is present. */
    @FunctionalInterface
    public interface Finder {
        @Nullable TankCell find(Level level, BlockPos pos);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/tank");

    private static final List<Finder> FINDERS = new CopyOnWriteArrayList<>();
    private static volatile Predicate<IFluidKey> gasPredicate = key -> false;

    private TankCellLookup() {}

    /**
     * Register a finder. Finders are consulted in registration order and the first non-null result wins;
     * because each mod's finder matches only its own block entity class, registration order is irrelevant.
     */
    public static void register(Finder finder) {
        FINDERS.add(Objects.requireNonNull(finder, "finder"));
    }

    /**
     * Find the {@link TankCell} at {@code pos}, or {@code null} if no registered finder matches. A finder
     * that throws is logged and skipped so one misbehaving mod cannot abort lookups for every tank.
     */
    @Nullable
    public static TankCell find(Level level, BlockPos pos) {
        for (Finder finder : FINDERS) {
            TankCell cell;
            try {
                cell = finder.find(level, pos);
            } catch (RuntimeException e) {
                LOGGER.error("Tank cell finder {} failed at {}", finder.getClass().getName(), pos, e);
                continue;
            }
            if (cell != null) {
                return cell;
            }
        }
        return null;
    }

    /**
     * Register the column-global gas predicate. Called once during loader initialization (logistics wires
     * it to {@code PlatformService.isLighterThanAir}). The last registration wins.
     */
    public static void registerGasPredicate(Predicate<IFluidKey> predicate) {
        gasPredicate = Objects.requireNonNull(predicate, "predicate");
    }

    /** Whether {@code fluid} settles upward (a gas) rather than downward (a liquid). */
    public static boolean isGas(IFluidKey fluid) {
        return gasPredicate.test(fluid);
    }
}

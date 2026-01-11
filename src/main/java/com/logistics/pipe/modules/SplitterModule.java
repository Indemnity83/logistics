package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Distributes items evenly across valid output directions.
 *
 * State is stored per-pipe-instance in the PipeBlockEntity NBT under the "round_robin" key.
 */
public class SplitterModule implements Module {
    private static final String NEXT_OUTPUT_INDEX = "next_index"; // NBT key for save compatibility

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> options) {
        // reset distribution order
        ctx.saveInt(this, NEXT_OUTPUT_INDEX, 0);
    }

    @Override
    public RoutePlan route(PipeContext ctx, com.logistics.pipe.runtime.TravelingItem item, List<Direction> options) {
        // If there are no options, drop the items
        if (options == null || options.isEmpty()) {
            return RoutePlan.drop();
        }

        // Ensure deterministic ordering so "round robin" is meaningful.
        // (Never rely on the input list order unless the engine guarantees it.)
        List<Direction> ordered = new ArrayList<>(options);
        ordered.sort(Comparator.comparingInt(Direction::getIndex));

        // Load / update per-pipe state.
        int next = ctx.getInt(this, NEXT_OUTPUT_INDEX, 0);
        int idx = Math.floorMod(next, ordered.size());

        Direction out = ordered.get(idx);

        // Advance pointer for next time.
        ctx.saveInt(this, NEXT_OUTPUT_INDEX, (idx + 1) % ordered.size());

        // Route to the chosen direction.
        return RoutePlan.reroute(List.of(out));
    }
}

package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MergerModule implements Module {
    private static final String OUTPUT_DIRECTION = "output_direction";

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> options) {
        List<Direction> outputs = ctx.getConnectedDirections();
        if (outputs.isEmpty()) {
            setOutputDirection(ctx, null);
            return;
        }

        Direction current = getOutputDirection(ctx);
        if (current == null || !outputs.contains(current)) {
            setOutputDirection(ctx, outputs.getFirst());
        }
    }

    @Override
    public RoutePlan route(PipeContext ctx, com.logistics.pipe.runtime.TravelingItem item, List<Direction> options) {
        Direction out = getOutputDirection(ctx);

        // If the output direction is not configured or no longer valid, drop the stack on the ground at the pipe.
        if (out == null || options == null || options.isEmpty() || !options.contains(out)) {
            return RoutePlan.drop();
        }

        return RoutePlan.reroute(out);
    }

    @Override
    public void onWrenchUse(PipeContext ctx, ItemUsageContext usage) {
        List<Direction> connected = ctx.getConnectedDirections();

        // No valid outputs: clear config.
        if (connected.isEmpty()) {
            setOutputDirection(ctx, null);
            return;
        }

        Direction current = getOutputDirection(ctx);
        Direction next = nextInCycle(connected, current);

        setOutputDirection(ctx, next);
    }

    @Override
    public boolean canAcceptFrom(PipeContext ctx, Direction from, ItemStack stack) {
        Direction out = getOutputDirection(ctx);
        return out == null || from != out;
    }

    private @Nullable Direction getOutputDirection(PipeContext ctx) {
        NbtCompound state = ctx.moduleState(getStateKey());
        if (!state.contains(OUTPUT_DIRECTION)) {
            return null;
        }
        return state.getString(OUTPUT_DIRECTION)
            .map(Direction::byId)
            .orElse(null);
    }

    private void setOutputDirection(PipeContext ctx, @Nullable Direction direction) {
        if (direction == null) {
            ctx.remove(this, OUTPUT_DIRECTION);
        } else {
            ctx.saveString(this, OUTPUT_DIRECTION, direction.getId());
            ctx.setFeatureFace(direction);
        }

        ctx.blockEntity().markDirty();
    }

    private Direction nextInCycle(List<Direction> ordered, @Nullable Direction current) {
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("ordered directions must not be empty");
        }

        int idx = (current == null) ? -1 : ordered.indexOf(current);
        return (idx < 0) ? ordered.getFirst() : ordered.get((idx + 1) % ordered.size());
    }

}

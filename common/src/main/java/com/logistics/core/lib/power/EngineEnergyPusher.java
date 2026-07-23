package com.logistics.core.lib.power;

import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.EnergyPushService;
import com.logistics.core.lib.energy.IEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Pushes energy from an engine's output buffer into the block it faces, and debits the buffer by
 * whatever was accepted.
 *
 * <p>Extraction pipes are {@link DirectEnergyReceiver}s kept off the loader energy grid, so they are
 * fed straight through their own {@code energyStorage(side)}; everything else goes through the
 * loader-specific {@link EnergyPushService}.
 */
public final class EngineEnergyPusher {

    private EngineEnergyPusher() {}

    /** The RF that may be sent this tick, given the output rate and what is buffered. */
    static long sendableAmount(long outputPower, long buffered) {
        return Math.max(0L, Math.min(outputPower, buffered));
    }

    /**
     * Push up to {@code outputPower} RF from {@code buffer} into the neighbor at the output face,
     * debiting the buffer by the amount accepted.
     *
     * @return the amount actually sent ({@code >= 0})
     */
    public static long push(
            @Nullable Level level, BlockPos pos, Direction outputDir, EnergyComponent buffer, long outputPower) {
        if (level == null) {
            return 0L;
        }
        long maxSend = sendableAmount(outputPower, buffer.getAmount());
        if (maxSend <= 0) {
            return 0L;
        }

        BlockPos targetPos = pos.relative(outputDir);
        long sent = sendTo(level, targetPos, outputDir.getOpposite(), buffer, maxSend);
        if (sent > 0) {
            buffer.consume(Math.min(sent, buffer.getAmount()));
        }
        return sent;
    }

    private static long sendTo(
            Level level, BlockPos targetPos, Direction fromDirection, EnergyComponent source, long maxSend) {
        if (level.getBlockEntity(targetPos) instanceof DirectEnergyReceiver receiver
                && receiver instanceof HasEnergyStorage hasStorage) {
            IEnergyStorage storage = hasStorage.energyStorage(fromDirection);
            return storage == null ? 0L : storage.insert(maxSend, false);
        }
        EnergyPushService pushService = EnergyPushService.get();
        return pushService == null ? 0L : pushService.push(level, targetPos, fromDirection, source, maxSend);
    }
}

package com.logistics.core.machine.component;

import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.IPipeAccess;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Registers the host machine as an energy source for any adjacent logistics pipe network, so the
 * network can draw the host's stored RF (read through its {@code HasEnergyStorage}) to power pipe
 * modules. The host therefore bridges the power network (which fills its buffer) into the logistics
 * network (which drains it).
 *
 * <p>References only {@code core.lib} abstractions, so it stays usable from any domain. The host must
 * call {@link #unregisterAll} when it is removed (no component removal hook exists in the framework).
 */
public final class NetworkEnergySourceComponent implements MachineComponent {

    /** How often (in ticks) to refresh network registrations once already registered. */
    private static final int SCAN_INTERVAL = 20;

    private final String id;
    private final Set<ILogisticsNetwork> registered = new HashSet<>();

    // Start one short of the interval so the first tick scans immediately (register as soon as placed).
    private int scanTick = SCAN_INTERVAL - 1;

    public NetworkEnergySourceComponent(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        Level level = ctx.level();
        if (level == null) {
            return;
        }
        scanTick++;
        // Scan every tick until registered (so a freshly placed junction powers the network as soon
        // as it forms), then back off to the interval to keep periodic scans cheap.
        if (!registered.isEmpty() && scanTick < SCAN_INTERVAL) {
            return;
        }
        scanTick = 0;

        BlockPos pos = ctx.pos();
        Set<ILogisticsNetwork> current = new HashSet<>();
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof IPipeAccess pipe) {
                ILogisticsNetwork net = pipe.getNetwork();
                if (net != null) {
                    current.add(net);
                }
            }
        }

        for (ILogisticsNetwork net : current) {
            if (registered.add(net)) {
                net.registerEnergySource(pos);
            }
        }

        Set<ILogisticsNetwork> stale = new HashSet<>(registered);
        stale.removeAll(current);
        for (ILogisticsNetwork net : stale) {
            net.unregisterEnergySource(pos);
            registered.remove(net);
        }
    }

    /** Unregister from every network this component registered with. Call when the host is removed. */
    public void unregisterAll(MachineContext ctx) {
        BlockPos pos = ctx.pos();
        for (ILogisticsNetwork net : registered) {
            net.unregisterEnergySource(pos);
        }
        registered.clear();
    }
}

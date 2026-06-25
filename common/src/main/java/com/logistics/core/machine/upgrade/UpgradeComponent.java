package com.logistics.core.machine.upgrade;

import com.logistics.core.machine.MachineComponent;

/**
 * Hosts a machine's upgrade inventory and exposes the resulting {@link MachineModifiers}.
 *
 * <p>Currently a stub that always reports identity modifiers — the seam exists so machines already
 * read cost/speed/range/capacity through it. Real upgrade items will make {@link #modifiers()}
 * return a non-identity set without touching the machines that consume it.
 */
public final class UpgradeComponent implements MachineComponent {

    private final String id;

    public UpgradeComponent(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    /** The modifiers contributed by the installed upgrades (identity until real upgrades exist). */
    public MachineModifiers modifiers() {
        return MachineModifiers.identity();
    }
}

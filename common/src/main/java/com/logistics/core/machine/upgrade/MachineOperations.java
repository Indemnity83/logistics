package com.logistics.core.machine.upgrade;

/** Operations shared by several machines, so one upgrade item works in each of them. */
public enum MachineOperations implements OperationKey {
    /** The square plot a machine works in front of itself; range widens it on every side. */
    WORK_AREA("machine.work_area");

    private final String namespace;

    MachineOperations(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String namespace() {
        return namespace;
    }
}

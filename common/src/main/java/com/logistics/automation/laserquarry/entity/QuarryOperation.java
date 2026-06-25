package com.logistics.automation.laserquarry.entity;

import com.logistics.core.machine.upgrade.OperationKey;

/** The quarry's upgrade-modifiable operations. */
public enum QuarryOperation implements OperationKey {
    BREAK_BLOCK("quarry.break_block"),
    MOVE_ARM("quarry.move_arm"),
    BUILD_FRAME("quarry.build_frame");

    private final String namespace;

    QuarryOperation(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String namespace() {
        return namespace;
    }
}

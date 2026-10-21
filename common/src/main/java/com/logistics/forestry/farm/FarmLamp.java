package com.logistics.forestry.farm;

import net.minecraft.util.StringRepresentable;

/**
 * A farm's status lamp, shown on the block (its {@link FarmMachineBlock#LAMP} state) and in the GUI.
 * {@link #forStatus} is the one status-to-colour mapping both use.
 */
public enum FarmLamp implements StringRepresentable {
    /** Unpowered: the buffer is empty. The block only; the GUI lamp always shows the status colour. */
    OFF("off"),
    RED("red"),
    AMBER("amber"),
    GREEN("green");

    private final String id;

    FarmLamp(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Green while the farm works or has nothing ripe, amber while it needs attention, red when it cannot work. Never {@link #OFF}. */
    public static FarmLamp forStatus(FarmStatus status) {
        return switch (status) {
            case WORKING, IDLE -> GREEN;
            case WAITING_FOR_SEEDS, INVENTORY_FULL, PAUSED -> AMBER;
            case NO_POWER, BLOCKED -> RED;
        };
    }
}

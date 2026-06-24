package com.logistics.automation.laserquarry.entity;

import com.logistics.core.lib.compat.NbtCompat;
import net.minecraft.nbt.CompoundTag;

/**
 * Mutable holder for the laser quarry's marker-defined mining bounds.
 * When {@link #isCustom()} is {@code false}, callers fall back to the default
 * area defined in {@code LogisticsConfig}.
 */
public final class QuarryBounds {
    private boolean custom = false;
    private int minX = 0;
    private int minZ = 0;
    private int maxX = 0;
    private int maxZ = 0;

    public boolean isCustom() {
        return custom;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public void setCustom(int minX, int minZ, int maxX, int maxZ) {
        this.custom = true;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }

    public void clear() {
        this.custom = false;
        this.minX = 0;
        this.minZ = 0;
        this.maxX = 0;
        this.maxZ = 0;
    }

    public void save(CompoundTag nbt) {
        nbt.putBoolean("UseCustomBounds", custom);
        if (custom) {
            nbt.putInt("CustomBoundsMinX", minX);
            nbt.putInt("CustomBoundsMinZ", minZ);
            nbt.putInt("CustomBoundsMaxX", maxX);
            nbt.putInt("CustomBoundsMaxZ", maxZ);
        }
    }

    public void load(CompoundTag nbt) {
        custom = NbtCompat.getBoolean(nbt, "UseCustomBounds", false);
        if (custom) {
            minX = NbtCompat.getInt(nbt, "CustomBoundsMinX", 0);
            minZ = NbtCompat.getInt(nbt, "CustomBoundsMinZ", 0);
            maxX = NbtCompat.getInt(nbt, "CustomBoundsMaxX", 0);
            maxZ = NbtCompat.getInt(nbt, "CustomBoundsMaxZ", 0);
        } else {
            minX = 0;
            minZ = 0;
            maxX = 0;
            maxZ = 0;
        }
    }
}

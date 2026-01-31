package com.logistics.api;

/**
 * Interface for energy containers that can store and transfer energy.
 * Energy is measured in RF (Redstone Flux), compatible with FE at 1:1 ratio.
 *
 * This uses the same unit system as Team Reborn Energy and Forge Energy,
 * enabling cross-mod compatibility on Fabric (via TR Energy) and future
 * NeoForge support (via Forge Energy).
 */
public interface EnergyStorage {
    /**
     * @return The current amount of energy stored, in RF.
     */
    long getAmount();

    /**
     * @return The maximum capacity of this storage, in RF.
     */
    long getCapacity();

    /**
     * Try to insert energy into this storage.
     *
     * @param maxAmount The maximum amount of energy to insert, in RF.
     * @param simulate  If true, don't actually insert - just report how much would be accepted.
     * @return The amount of energy that was (or would be) accepted, in RF.
     */
    long insert(long maxAmount, boolean simulate);

    /**
     * Try to extract energy from this storage.
     *
     * @param maxAmount The maximum amount of energy to extract, in RF.
     * @param simulate  If true, don't actually extract - just report how much would be extracted.
     * @return The amount of energy that was (or would be) extracted, in RF.
     */
    long extract(long maxAmount, boolean simulate);

    /**
     * @return true if this storage can accept energy.
     */
    boolean canInsert();

    /**
     * @return true if energy can be extracted from this storage.
     */
    boolean canExtract();

    /**
     * @return true if this storage is empty (amount == 0).
     */
    default boolean isEmpty() {
        return getAmount() == 0;
    }

    /**
     * @return true if this storage is full (amount >= capacity).
     */
    default boolean isFull() {
        return getAmount() >= getCapacity();
    }

    // Conversion constants for display purposes
    long RF_PER_SECOND_AT_1_RF_PER_TICK = 20L; // 1 RF/t = 20 RF/s (at 20 ticks/second)
}

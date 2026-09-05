package com.logistics.core.lib.energy;

/**
 * Loader-agnostic energy storage contract.
 *
 * <p>Uses simulate-boolean semantics (not transactions) to remain compatible with both
 * Fabric (Team Reborn Energy) and NeoForge energy APIs. Loader-specific adapters
 * bridge this interface to each platform's native API.
 *
 * <p>This is the extension point for the energy abstraction layer. Any block entity
 * that participates in the energy system should implement {@link
 * com.logistics.core.lib.block.capability.HasEnergyStorage} and return this type.
 */
public interface IEnergyStorage {

    /**
     * Insert energy into this storage.
     *
     * @param maxAmount the maximum amount to insert
     * @param simulate  if true, perform a dry run without modifying state
     * @return the amount actually inserted (or that would be inserted if simulating)
     */
    long insert(long maxAmount, boolean simulate);

    /**
     * Extract energy from this storage.
     *
     * @param maxAmount the maximum amount to extract
     * @param simulate  if true, perform a dry run without modifying state
     * @return the amount actually extracted (or that would be extracted if simulating)
     */
    long extract(long maxAmount, boolean simulate);

    /** Returns the current amount of energy stored. */
    long getAmount();

    /** Returns the maximum energy this storage can hold. */
    long getCapacity();

    /** Returns true if this storage can accept energy. */
    default boolean canInsert() {
        return true;
    }

    /** Returns true if this storage can provide energy. */
    default boolean canExtract() {
        return true;
    }

    /**
     * Whether this storage accepts energy on this face <em>at all</em>, ignoring how full it is.
     *
     * <p>Distinct from {@link #canInsert()}, which several callers use to decide whether to send
     * energy right now. This one answers the placement question — "is there something here worth
     * facing" — where momentary fullness is not an answer: an engine's facing is chosen once, so a
     * neighbour that happens to be full at that instant would be skipped permanently.
     *
     * <p>NeoForge's {@code EnergyHandler} exposes no direction flag — its javadoc says the only way
     * to know is to try inserting — so that adapter reports presence for third-party handlers rather
     * than probing. Fullness is left to the transfer call, which is where it belongs.
     */
    default boolean acceptsEnergy() {
        return canInsert();
    }
}

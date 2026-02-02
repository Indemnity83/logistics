package com.logistics.core.lib.power;

/**
 * Marker interface for early-game energy sources with transfer restrictions.
 *
 * <p>Energy sources implementing this interface (e.g., Redstone Engine) have limited
 * compatibility with advanced blocks. Only blocks explicitly registered with
 * {@link LowTierEnergyAcceptanceRegistry} can receive energy from low-tier sources.
 *
 * <p>This interface enables progression gating by preventing early-game engines from
 * powering advanced machinery, encouraging players to upgrade their power infrastructure
 * as they advance through the mod's content.
 *
 * <p><b>Design pattern:</b> Energy sources check for {@link AcceptsLowTierEnergy}
 * registration before attempting transfer. If the target block is not registered or
 * returns false from {@link AcceptsLowTierEnergy#acceptsLowTierEnergyFrom}, the transfer
 * is blocked.
 */
public interface LowTierEnergySource {}

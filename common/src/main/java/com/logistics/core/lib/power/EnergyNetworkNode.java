package com.logistics.core.lib.power;

/**
 * A block that belongs to an energy transport network which distributes on its own.
 *
 * <p>A cable is the only implementor. The marker exists so a buffer can recognise one without
 * reaching into the power domain, and it is consulted by exactly one rule: a buffer never pushes
 * into a network node.
 *
 * <p>Pushing would bypass the network's allocation. A buffer's push runs on its own block-entity
 * tick and spends whatever of the cable's per-tick budget is still going, so the first battery in a
 * bank to tick empties into the network and the rest find nothing left — a bank that drains one
 * battery at a time, in an order the player never chose. The network already offers every buffer as
 * a source and shares the draw across them, so it only has to be left to do it.
 *
 * <p>Generators are the opposite case and still push: they are deliberately kept out of the
 * network's own source list so they can output on their own cadence.
 */
public interface EnergyNetworkNode {}

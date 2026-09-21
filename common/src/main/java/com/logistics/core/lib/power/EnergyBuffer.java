package com.logistics.core.lib.power;

/**
 * A block whose job is to hold energy rather than generate or consume it.
 *
 * <p>Marks a device as a destination of last resort: energy should reach a buffer from a generator
 * and leave it for a machine, but moving it from one buffer to another achieves nothing. Left
 * unmarked, buffers offer themselves to each other as both supplier and consumer, and the one the
 * network happens to sort first empties into the rest — which reads in game as a battery that
 * refuses to charge, and as a charging order that depends on where blocks sit rather than on
 * anything the player did.
 */
public interface EnergyBuffer {}

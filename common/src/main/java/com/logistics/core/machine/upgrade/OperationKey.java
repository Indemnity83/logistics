package com.logistics.core.machine.upgrade;

/**
 * Stable key identifying a machine operation whose energy cost, work speed, or range an upgrade can
 * modify. Operations are namespaced per machine (e.g. {@code "quarry.break_block"},
 * {@code "pump.extract_fluid"}, {@code "recipe.process"}) so a shared modifier layer can target them.
 */
public interface OperationKey {

    /** Namespaced identifier, e.g. {@code "quarry.move_arm"}. */
    String namespace();
}

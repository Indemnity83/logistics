package com.logistics.gametest;

import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry entry point for GameTest bodies on NeoForge.
 *
 * <p>MC replaced the old {@code @GameTest}-annotated/reflection-scanned test model with a
 * data-driven registry: every test body is a named {@code Consumer<GameTestHelper>} function in
 * {@link net.minecraft.core.registries.Registries#TEST_FUNCTION}, referenced by a
 * {@link net.minecraft.gametest.framework.GameTestInstance} that also carries the structure/max
 * ticks/etc. ({@link net.neoforged.neoforge.event.RegisterGameTestsEvent}, used per-domain, wires
 * the instance side).
 *
 * <p>{@code TEST_FUNCTION} is a plain {@link net.minecraft.core.registries.BuiltInRegistries}
 * entry, so it goes through the same {@code DeferredRegister} + {@code RegisterEvent} unfreeze
 * mechanism as any other vanilla registry (Blocks, Items, ...) — NOT vanilla's own
 * {@code TestFunctionLoader}/bootstrap hook, which runs before FML loads any mod class and is
 * unreachable from mod code.
 */
public final class GameTestFunctions {

    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTION =
        DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, "logistics_gametest");

    private GameTestFunctions() {}
}

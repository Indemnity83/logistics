package com.logistics.fluid.block;

import com.logistics.core.LogisticsConfig;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The fluid pipe variants. Each constant is the single source of truth for that pipe's behaviour — its
 * role, whether it connects to external fluid handlers, its transfer rate, its buffer capacity, and the
 * asset base name used by the renderer — so the block entity branches on the kind rather than scattering
 * {@code instanceof}/{@code ==} checks.
 *
 * <ul>
 *   <li>{@link #COPPER} — passive transport at 2× the base rate.</li>
 *   <li>{@link #EXTRACTOR} — the "wooden" extractor: pulls fluid from adjacent handlers when powered.</li>
 *   <li>{@link #STONE} — passive transport at the base rate (1×, the slow tier).</li>
 *   <li>{@link #GOLD} — passive transport at 4× the base rate.</li>
 *   <li>{@link #INSERTION} — passive transport that fills adjacent tanks first, spilling to pipes only once
 *       they are full.</li>
 *   <li>{@link #MERGER} — directional check valve: a wrench-set output face; fluid leaves only that face
 *       and never flows back through it.</li>
 *   <li>{@link #VOID} — destroys fluid drawn from adjacent pipes (connects to pipes only).</li>
 *   <li>{@link #BYPASS} — passive transport that connects to pipes only, never to handlers.</li>
 * </ul>
 */
public enum FluidPipeKind implements StringRepresentable {
    COPPER("copper", "copper_fluid_pipe", Role.PASSIVE, true, 2, false),
    EXTRACTOR("extractor", "fluid_extractor_pipe", Role.EXTRACTOR, true, 1, false),
    STONE("stone", "stone_fluid_pipe", Role.PASSIVE, true, 1, false),
    GOLD("gold", "gold_fluid_pipe", Role.PASSIVE, true, 4, false),
    INSERTION("insertion", "insertion_fluid_pipe", Role.PASSIVE, true, 2, true),
    MERGER("merger", "merger_fluid_pipe", Role.DIRECTIONAL, true, 3, false),
    VOID("void", "void_fluid_pipe", Role.VOID, false, 1, false),
    BYPASS("bypass", "bypass_fluid_pipe", Role.PASSIVE, false, 2, false);

    /** What a pipe fundamentally does. Mutually exclusive; distinct from the connect-to-handlers flag. */
    private enum Role {
        /** Normal communicating-vessels mover that also pushes into adjacent handlers. */
        PASSIVE,
        /** Pulls fluid from adjacent handlers when powered. */
        EXTRACTOR,
        /** One-way check valve with a wrench-set output face. */
        DIRECTIONAL,
        /** Bottomless sink that destroys fluid. */
        VOID
    }

    public static final Codec<FluidPipeKind> CODEC = StringRepresentable.fromEnum(FluidPipeKind::values);

    private final String name;
    private final String modelBase;
    private final Role role;
    private final boolean connectsToHandlers;
    private final int rateMultiplier;
    private final boolean handlerPriority;

    FluidPipeKind(
            String name,
            String modelBase,
            Role role,
            boolean connectsToHandlers,
            int rateMultiplier,
            boolean handlerPriority) {
        this.name = name;
        this.modelBase = modelBase;
        this.role = role;
        this.connectsToHandlers = connectsToHandlers;
        this.rateMultiplier = rateMultiplier;
        this.handlerPriority = handlerPriority;
    }

    public boolean isExtractor() {
        return role == Role.EXTRACTOR;
    }

    public boolean isVoid() {
        return role == Role.VOID;
    }

    /** True for the merger check valve, which directs flow out a single wrench-set face. */
    public boolean isDirectional() {
        return role == Role.DIRECTIONAL;
    }

    /**
     * True for kinds that take part in the normal undirected body equilibrium and push omnidirectionally
     * into adjacent handlers. The extractor (pulls), merger (directs), and void (sinks) move fluid by their
     * own bespoke rules instead.
     */
    public boolean isPassiveMover() {
        return role == Role.PASSIVE;
    }

    /** Whether this pipe connects to external (non-pipe) fluid handlers. False for void and bypass. */
    public boolean connectsToHandlers() {
        return connectsToHandlers;
    }

    /** True for the insertion pipe: route fluid into adjacent tanks with room before spilling to other pipes. */
    public boolean prioritizesHandlers() {
        return handlerPriority;
    }

    /**
     * This pipe's transfer rate in mB/tick, used to pace flow through it, into handlers, and extraction.
     * A single configurable base rate scaled by a per-tier multiplier (stone/extractor/void 1×, copper/bypass
     * 2×, merger 3×, gold 4×).
     */
    public long transferRate(LogisticsConfig.FluidPipeConfig cfg) {
        return (long) cfg.baseTransferRate * rateMultiplier;
    }

    /** This pipe's internal buffer capacity in mB (shared base capacity across all kinds). */
    public long capacity(LogisticsConfig.FluidPipeConfig cfg) {
        return cfg.baseCapacity;
    }

    /** Asset base name (under {@code block/pipe/}) for this kind's {@code _core}/{@code _arm} models. */
    public String modelBase() {
        return modelBase;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

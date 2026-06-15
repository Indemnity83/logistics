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
 *   <li>{@link #COPPER} — passive transport at the base rate.</li>
 *   <li>{@link #EXTRACTOR} — the "wooden" extractor: pulls fluid from adjacent handlers when powered.</li>
 *   <li>{@link #STONE} — passive transport at a slower rate.</li>
 *   <li>{@link #GOLD} — passive transport at a faster rate.</li>
 *   <li>{@link #MERGER} — directional check valve: a wrench-set output face; fluid leaves only that face
 *       and never flows back through it (mirrors the item Merger Pipe).</li>
 *   <li>{@link #VOID} — destroys fluid drawn from adjacent pipes (connects to pipes only).</li>
 *   <li>{@link #BYPASS} — passive transport that connects to pipes only, never to handlers.</li>
 * </ul>
 */
public enum FluidPipeKind implements StringRepresentable {
    COPPER("copper", "copper_fluid_pipe", Role.PASSIVE, true),
    EXTRACTOR("extractor", "fluid_extractor_pipe", Role.EXTRACTOR, true),
    STONE("stone", "stone_fluid_pipe", Role.PASSIVE, true),
    GOLD("gold", "gold_fluid_pipe", Role.PASSIVE, true),
    MERGER("merger", "merger_fluid_pipe", Role.DIRECTIONAL, true),
    VOID("void", "void_fluid_pipe", Role.VOID, false),
    BYPASS("bypass", "bypass_fluid_pipe", Role.PASSIVE, false);

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

    FluidPipeKind(String name, String modelBase, Role role, boolean connectsToHandlers) {
        this.name = name;
        this.modelBase = modelBase;
        this.role = role;
        this.connectsToHandlers = connectsToHandlers;
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

    /** This pipe's transfer rate in mB/tick, used to pace flow through it, into handlers, and extraction. */
    public long transferRate(LogisticsConfig.FluidPipeConfig cfg) {
        return switch (this) {
            case COPPER, EXTRACTOR, BYPASS -> cfg.copperTransferRate;
            case STONE -> cfg.stoneTransferRate;
            case GOLD -> cfg.goldTransferRate;
            case MERGER -> cfg.mergerTransferRate;
            case VOID -> cfg.voidRate;
        };
    }

    /** This pipe's internal buffer capacity in mB. */
    public long capacity(LogisticsConfig.FluidPipeConfig cfg) {
        return isExtractor() ? cfg.woodenCapacity : cfg.copperCapacity;
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

package com.logistics.fluid.block;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The two fluid pipe variants in the MVP.
 *
 * <ul>
 *   <li>{@link #COPPER} — passive transport: buffers fluid and pushes it to connected fluid pipes
 *       and handlers.</li>
 *   <li>{@link #EXTRACTOR} — the "wooden" extractor: additionally pulls fluid from adjacent
 *       handlers into the network when driven by an engine.</li>
 * </ul>
 */
public enum FluidPipeKind implements StringRepresentable {
    COPPER("copper"),
    EXTRACTOR("extractor");

    public static final Codec<FluidPipeKind> CODEC = StringRepresentable.fromEnum(FluidPipeKind::values);

    private final String name;

    FluidPipeKind(String name) {
        this.name = name;
    }

    public boolean isExtractor() {
        return this == EXTRACTOR;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

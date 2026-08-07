package com.logistics.automation.transposer;

import com.logistics.core.lib.recipe.FluidResult;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** A recipe's fluid side, signed relative to the machine's tank: negative drains it, positive deposits into it. */
public record SignedFluidAmount(Fluid fluid, int amount) {

    public SignedFluidAmount {
        if (fluid == Fluids.EMPTY) {
            throw new IllegalArgumentException("fluid must not be empty");
        }
        if (amount == 0 || amount == Integer.MIN_VALUE) {
            throw new IllegalArgumentException("amount must be nonzero and not Integer.MIN_VALUE, got " + amount);
        }
    }

    public static final Codec<SignedFluidAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(SignedFluidAmount::fluid),
                    Codec.INT.fieldOf("amount").forGetter(SignedFluidAmount::amount))
            .apply(instance, SignedFluidAmount::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SignedFluidAmount> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.FLUID), SignedFluidAmount::fluid,
            ByteBufCodecs.INT, SignedFluidAmount::amount,
            SignedFluidAmount::new);

    /** Whether this drains from the tank (a Fill-mode recipe requiring the fluid present to run). */
    public boolean isInput() {
        return amount < 0;
    }

    /** The unsigned {@link FluidResult}, for handing to {@link com.logistics.core.machine.component.RecipePlan}. */
    public FluidResult toFluidResult() {
        return new FluidResult(fluid, Math.abs(amount));
    }
}

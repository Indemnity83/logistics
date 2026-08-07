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

/**
 * A transposer recipe's fluid side, signed relative to the machine's tank: negative drains the fluid
 * from the tank (Fill mode — the recipe needs it present to run), positive deposits it into the tank
 * (Empty mode — the recipe produces it). One field instead of separate input/output fluid fields, so a
 * recipe author never has to remember which of two names means "drains" vs "fills".
 */
public record SignedFluidAmount(Fluid fluid, int amount) {

    public SignedFluidAmount {
        if (fluid == Fluids.EMPTY) {
            throw new IllegalArgumentException("fluid must not be empty");
        }
        if (amount == 0) {
            throw new IllegalArgumentException("amount must not be zero");
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

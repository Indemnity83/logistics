package com.logistics.pipe.fluid;

import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;

/**
 * A discrete parcel of fluid traveling through a fluid pipe — the fluid analogue of
 * {@link com.logistics.core.lib.pipe.TravelingItem}. Fluid is moved cellularly: a parcel dwells in a pipe
 * for a per-fluid countdown (its spread rate) and then hops one pipe along, splitting at junctions.
 *
 * <p>Quantities are in millibuckets (mB). Mutable: the runtime decrements {@link #countdown}, shrinks
 * {@link #amount} as fluid hops out, and flips {@link #blocked}.
 *
 * <ul>
 *   <li><b>heading</b> — the direction this parcel is traveling; it entered from {@code heading.getOpposite()}.
 *       Hops prefer to continue forward and exclude the reverse (incoming) direction, unless the parcel is
 *       {@link #blocked}, in which case it may also flow back the way it came (reflect off a dead end / full
 *       downstream).</li>
 *   <li><b>countdown</b> — ticks left before this parcel may hop, seeded from the fluid's spread rate
 *       ({@code Fluid#getTickDelay}). 0 = ready to move.</li>
 *   <li><b>blocked</b> — set when a ready parcel could not move all of its fluid forward last tick; lets it
 *       reflect on the next tick instead of freezing.</li>
 * </ul>
 */
public final class TravelingFluid {

    /**
     * The save/load codec. Built lazily (nested holder) so merely loading this class does not touch the fluid
     * registry — keeps loader-agnostic common code and unit tests off a not-yet-bootstrapped game registry.
     */
    public static Codec<TravelingFluid> codec() {
        return Codecs.CODEC;
    }

    private static final class Codecs {
        private static final Codec<SimpleFluidKey> FLUID_KEY = RecordCodecBuilder.create(instance -> instance
                .group(
                        BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(SimpleFluidKey::fluid),
                        DataComponentPatch.CODEC
                                .optionalFieldOf("components", DataComponentPatch.EMPTY)
                                .forGetter(SimpleFluidKey::components))
                .apply(instance, SimpleFluidKey::new));

        private static final Codec<TravelingFluid> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        FLUID_KEY
                                .fieldOf("fluid")
                                .forGetter(t -> new SimpleFluidKey(t.fluid.getFluid(), t.fluid.getComponents())),
                        Codec.LONG.fieldOf("amount").forGetter(t -> t.amount),
                        Codec.INT
                                .xmap(Direction::from3DDataValue, Direction::get3DDataValue)
                                .fieldOf("heading")
                                .forGetter(t -> t.heading),
                        Codec.INT.optionalFieldOf("countdown", 0).forGetter(t -> t.countdown),
                        Codec.BOOL.optionalFieldOf("blocked", false).forGetter(t -> t.blocked))
                .apply(instance, TravelingFluid::fromCodec));
    }

    private static TravelingFluid fromCodec(
            SimpleFluidKey fluid, long amount, Direction heading, int countdown, boolean blocked) {
        TravelingFluid parcel = new TravelingFluid(fluid, amount, heading, countdown);
        parcel.blocked = blocked;
        return parcel;
    }

    private final IFluidKey fluid;
    private long amount;
    private Direction heading;
    private int countdown;
    private boolean blocked;

    public TravelingFluid(IFluidKey fluid, long amount, Direction heading, int countdown) {
        this.fluid = fluid;
        this.amount = amount;
        this.heading = heading;
        this.countdown = countdown;
    }

    public IFluidKey fluid() {
        return fluid;
    }

    public Fluid vanillaFluid() {
        return fluid.getFluid();
    }

    public long amount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public void add(long delta) {
        this.amount += delta;
    }

    public Direction heading() {
        return heading;
    }

    public int countdown() {
        return countdown;
    }

    /** Decrements the dwell countdown toward 0 (never below). Returns true once the parcel is ready to hop. */
    public boolean tickCountdown() {
        if (countdown > 0) {
            countdown--;
        }
        return countdown <= 0;
    }

    public boolean ready() {
        return countdown <= 0;
    }

    public boolean blocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}

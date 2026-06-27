package com.logistics.core.lib.pipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.logistics.test.MinecraftTestEnvironment;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Traveling fluid invariants")
class TravelingFluidTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("rejects a negative amount at construction")
    void rejectsNegativeAtConstruction() {
        // The amount invariant doesn't touch the fluid identity, so a null key keeps this off the registry.
        assertThatThrownBy(() -> new TravelingFluid(null, -1, Direction.NORTH, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects mutations that would drive the amount negative")
    void rejectsNegativeMutation() {
        TravelingFluid parcel = new TravelingFluid(null, 10, Direction.NORTH, 0);
        assertThatThrownBy(() -> parcel.setAmount(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parcel.add(-11)).isInstanceOf(IllegalArgumentException.class);

        parcel.add(-10); // draining exactly to empty is allowed
        assertThat(parcel.amount()).isZero();
    }

    @Test
    @DisplayName("decodes a valid parcel (positive control)")
    void decodesValidParcel() {
        assertThat(parse(parcelJson(5, 2)).isSuccess()).isTrue();
    }

    @Test
    @DisplayName("decode rejects a negative amount")
    void decodeRejectsNegativeAmount() {
        assertThat(parse(parcelJson(-5, 2)).isError()).isTrue();
    }

    @Test
    @DisplayName("decode rejects a heading outside 0..5")
    void decodeRejectsOutOfRangeHeading() {
        assertThat(parse(parcelJson(5, 6)).isError()).isTrue();
        assertThat(parse(parcelJson(5, -1)).isError()).isTrue();
    }

    private static DataResult<TravelingFluid> parse(JsonObject json) {
        return TravelingFluid.codec().parse(JsonOps.INSTANCE, json);
    }

    private static JsonObject parcelJson(long amount, int heading) {
        JsonObject fluid = new JsonObject();
        fluid.add("fluid", new JsonPrimitive("minecraft:water"));
        JsonObject json = new JsonObject();
        json.add("fluid", fluid);
        json.add("amount", new JsonPrimitive(amount));
        json.add("heading", new JsonPrimitive(heading));
        return json;
    }
}

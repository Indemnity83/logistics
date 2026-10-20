package com.logistics.core.machine.tier;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonPrimitive;
import com.logistics.test.MinecraftTestEnvironment;
import com.mojang.serialization.JsonOps;
import java.util.Arrays;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MachineTierTest extends MinecraftTestEnvironment {

    @Test
    void idsAreTheGoldenStrings() {
        assertThat(Arrays.stream(MachineTier.values()).map(MachineTier::id))
                .containsExactly("basic", "industrial", "precision", "infernal");
    }

    @ParameterizedTest
    @EnumSource(MachineTier.class)
    void bothCodecsEncodeToTheIdString(MachineTier tier) {
        assertThat(MachineTier.CODEC.encodeStart(JsonOps.INSTANCE, tier).getOrThrow())
                .isEqualTo(new JsonPrimitive(tier.id()));
        assertThat(MachineTier.LENIENT_CODEC.encodeStart(JsonOps.INSTANCE, tier).getOrThrow())
                .isEqualTo(new JsonPrimitive(tier.id()));

        Tag nbt = MachineTier.CODEC.encodeStart(NbtOps.INSTANCE, tier).getOrThrow();
        assertThat(nbt).isInstanceOf(StringTag.class);
    }

    @ParameterizedTest
    @EnumSource(MachineTier.class)
    void bothCodecsRoundTrip(MachineTier tier) {
        Tag nbt = MachineTier.CODEC.encodeStart(NbtOps.INSTANCE, tier).getOrThrow();
        assertThat(MachineTier.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow()).isEqualTo(tier);
        assertThat(MachineTier.LENIENT_CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow()).isEqualTo(tier);
        assertThat(MachineTier.byId(tier.id())).contains(tier);
    }

    @Test
    void strictCodecRejectsAnUnknownId() {
        assertThat(MachineTier.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("mythic")).isError())
                .isTrue();
    }

    @Test
    void lenientDecodeFallsBackToBasic() {
        assertThat(MachineTier.LENIENT_CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("mythic")).getOrThrow())
                .isEqualTo(MachineTier.BASIC);
        assertThat(MachineTier.byIdOrBasic("mythic")).isEqualTo(MachineTier.BASIC);
        assertThat(MachineTier.byIdOrBasic("")).isEqualTo(MachineTier.BASIC);
        assertThat(MachineTier.byIdOrBasic(null)).isEqualTo(MachineTier.BASIC);
        assertThat(MachineTier.byIdOrBasic("precision")).isEqualTo(MachineTier.PRECISION);
    }

    @Test
    void nextAndPreviousWalkTheLadderAndStopAtTheEnds() {
        assertThat(MachineTier.BASIC.previous()).isEmpty();
        assertThat(MachineTier.BASIC.next()).contains(MachineTier.INDUSTRIAL);
        assertThat(MachineTier.INDUSTRIAL.next()).contains(MachineTier.PRECISION);
        assertThat(MachineTier.PRECISION.next()).contains(MachineTier.INFERNAL);
        assertThat(MachineTier.INFERNAL.next()).isEmpty();
        assertThat(MachineTier.INFERNAL.previous()).contains(MachineTier.PRECISION);
    }

    @Test
    void ranksAscendFromZero() {
        MachineTier[] tiers = MachineTier.values();
        for (int i = 0; i < tiers.length; i++) {
            assertThat(tiers[i].rank()).isEqualTo(i);
        }
    }

    @Test
    void translationKeyUsesTheId() {
        assertThat(MachineTier.INFERNAL.translationKey()).isEqualTo("tier.logistics.infernal");
    }
}

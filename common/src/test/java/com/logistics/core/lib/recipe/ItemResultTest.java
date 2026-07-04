package com.logistics.core.lib.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ItemResult")
class ItemResultTest extends MinecraftTestEnvironment {

    // ==================== getters ====================

    @Test
    @DisplayName("of(item, count) produces a matching result stack")
    void toStackMatchesItemAndCount() {
        ItemStack stack = ItemResult.of(Items.IRON_INGOT, 4).toStack();

        assertThat(stack.is(Items.IRON_INGOT)).isTrue();
        assertThat(stack.getCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("each toStack() call is a fresh, independent stack")
    void toStackReturnsFreshStacks() {
        ItemResult result = ItemResult.of(Items.IRON_INGOT, 4);
        ItemStack first = result.toStack();
        first.shrink(1);

        assertThat(result.toStack().getCount()).isEqualTo(4);
    }

    // ==================== serialization ====================

    private RegistryAccess registries;
    private RegistryOps<Tag> ops;

    @BeforeEach
    void setUp() {
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ops = registries.createSerializationContext(NbtOps.INSTANCE);
    }

    @Test
    @DisplayName("codec round-trips the item and count")
    void codecRoundTrip() {
        ItemResult original = ItemResult.of(Items.GOLD_INGOT, 2);

        Tag encoded = ItemResult.CODEC.encodeStart(ops, original).getOrThrow();
        ItemResult decoded = ItemResult.CODEC.parse(ops, encoded).getOrThrow();

        assertThat(decoded.toStack().is(Items.GOLD_INGOT)).isTrue();
        assertThat(decoded.toStack().getCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("stream codec round-trips the item and count for recipe sync")
    void streamCodecRoundTrip() {
        ItemResult original = ItemResult.of(Items.GOLD_INGOT, 2);

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        ItemResult.STREAM_CODEC.encode(buf, original);
        ItemResult decoded = ItemResult.STREAM_CODEC.decode(buf);

        assertThat(decoded.toStack().is(Items.GOLD_INGOT)).isTrue();
        assertThat(decoded.toStack().getCount()).isEqualTo(2);
    }
}

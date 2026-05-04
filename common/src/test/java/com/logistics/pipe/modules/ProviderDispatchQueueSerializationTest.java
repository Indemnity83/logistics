package com.logistics.pipe.modules;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link ProviderDispatchQueue} correctly preserves item data components
 * (custom names, enchantments, etc.) through enqueue → NBT round-trip → reconstruction.
 *
 * <h2>Regression context</h2>
 * <p>Prior to this fix, {@link ProviderDispatchQueue.Entry} stored only a {@code String itemId}.
 * When the provider drained the queue, it reconstructed {@code ItemVariant.of(new ItemStack(item))}
 * — a bare variant with no components. Because {@link net.fabricmc.fabric.api.transfer.v1.item.ItemVariant}
 * equality includes all data components, the bare variant never matched enchanted (or otherwise
 * component-bearing) items in storage, so extraction always failed silently.
 *
 * <p>The fix adds a {@code @Nullable CompoundTag itemTag} to each entry carrying the full
 * serialized {@link ItemStack}. This test verifies that the tag survives the round-trip and
 * that the reconstructed {@link ItemStack} carries the same components.
 */
@DisplayName("ProviderDispatchQueue serialization")
class ProviderDispatchQueueSerializationTest extends MinecraftTestEnvironment {

    private RegistryOps<Tag> ops;

    @BeforeEach
    void setUp() {
        ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
                .createSerializationContext(NbtOps.INSTANCE);
    }

    @Test
    @DisplayName("itemTag round-trips through CompoundTag serialization")
    void itemTagRoundTrips() {
        // Encode a stack with a custom display name — a component that doesn't require
        // a live server registry, unlike enchantments which use dynamic registry holders.
        ItemStack original = new ItemStack(Items.DIAMOND_SWORD);
        original.set(DataComponents.CUSTOM_NAME, Component.literal("Vorpal Blade"));

        CompoundTag itemTag = (CompoundTag) ItemStack.CODEC.encodeStart(ops, original)
                .result()
                .orElseThrow(() -> new AssertionError("Encode failed"));

        ProviderDispatchQueue queue = new ProviderDispatchQueue();
        queue.enqueue("minecraft:diamond_sword", itemTag, 1, BlockPos.ZERO, null);

        ProviderDispatchQueue.Entry entry = queue.peekHead();
        assertThat(entry.itemTag()).isNotNull();

        // Decode back to ItemStack and verify components survived
        ItemStack decoded = ItemStack.CODEC.parse(ops, entry.itemTag())
                .result()
                .orElseThrow(() -> new AssertionError("Decode failed"));

        assertThat(decoded.getItem()).isEqualTo(Items.DIAMOND_SWORD);
        assertThat(decoded.get(DataComponents.CUSTOM_NAME))
                .isNotNull()
                .extracting(Component::getString)
                .isEqualTo("Vorpal Blade");
    }

    @Test
    @DisplayName("itemTag is null for bare items — no unnecessary component storage")
    void bareItemProducesNullTag() {
        ProviderDispatchQueue queue = new ProviderDispatchQueue();
        queue.enqueue("minecraft:diamond", 64, BlockPos.ZERO, null);

        assertThat(queue.peekHead().itemTag()).isNull();
    }

    @Test
    @DisplayName("itemTag survives partial consumption")
    void itemTagSurvivesPartialConsumption() {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Excalibur"));

        CompoundTag tag = (CompoundTag) ItemStack.CODEC.encodeStart(ops, stack)
                .result()
                .orElseThrow();

        ProviderDispatchQueue queue = new ProviderDispatchQueue();
        queue.enqueue("minecraft:iron_sword", tag, 10, BlockPos.ZERO, null);
        queue.consumeFromHead(3);

        ProviderDispatchQueue.Entry head = queue.peekHead();
        assertThat(head.remaining()).isEqualTo(7);
        assertThat(head.itemTag()).isEqualTo(tag);
    }
}

package com.logistics.pipe.runtime;

import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Verifies that {@link TravelingItem} serializes correctly using registry-aware ops.
 *
 * <p>The fix in this area replaced {@code NbtOps.INSTANCE} with {@code RegistryOps} at all
 * {@link net.minecraft.world.item.ItemStack#CODEC} call sites in the pipe layer. Plain items
 * encode the same way under either ops, but registry-backed components (enchantments, etc.)
 * throw {@link IllegalStateException} with bare {@code NbtOps.INSTANCE}.
 *
 * <p>Enchanted-item serialization is tested end-to-end in
 * {@code PipeFlowGameTest.testEnchantedItemTravelsThroughPipe}, which has access to a live
 * {@code ServerLevel} and can resolve {@code Holder<Enchantment>} from the dynamic registry.
 */
@DisplayName("TravelingItem serialization")
class TravelingItemSerializationTest extends MinecraftTestEnvironment {

    private static final float TOLERANCE = 0.0001f;

    private RegistryOps<Tag> ops;

    @BeforeEach
    void setUp() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ops = registries.createSerializationContext(NbtOps.INSTANCE);
    }

    @Test
    @DisplayName("round-trips item, direction, speed, and progress through NBT")
    void roundTripPreservesFields() {
        TravelingItem original = new TravelingItem(new ItemStack(Items.DIAMOND, 3), Direction.EAST, 0.05f);
        original.setProgress(0.35f);

        Tag encoded = TravelingItem.CODEC.encodeStart(ops, original).getOrThrow();
        TravelingItem decoded = TravelingItem.CODEC.parse(ops, encoded).getOrThrow();

        assertThat(decoded.getStack().getItem()).isEqualTo(Items.DIAMOND);
        assertThat(decoded.getStack().getCount()).isEqualTo(3);
        assertThat(decoded.getDirection()).isEqualTo(Direction.EAST);
        assertThat(decoded.getSpeed()).isCloseTo(0.05f, within(TOLERANCE));
        assertThat(decoded.getProgress()).isCloseTo(0.35f, within(TOLERANCE));
    }

    @Test
    @DisplayName("round-trips destination and routing state")
    void roundTripPreservesRoutingState() {
        BlockPos destination = new BlockPos(10, 64, -5);
        TravelingItem original = new TravelingItem(new ItemStack(Items.EMERALD), Direction.NORTH, 0.08f, destination);
        original.setRouted(true);

        Tag encoded = TravelingItem.CODEC.encodeStart(ops, original).getOrThrow();
        TravelingItem decoded = TravelingItem.CODEC.parse(ops, encoded).getOrThrow();

        assertThat(decoded.getDestination()).isEqualTo(destination);
        assertThat(decoded.isRouted()).isTrue();
    }

    @Test
    @DisplayName("round-trips a stack with count greater than one")
    void roundTripPreservesStackCount() {
        TravelingItem original = new TravelingItem(new ItemStack(Items.IRON_INGOT, 16), Direction.SOUTH, 0.1f);

        Tag encoded = TravelingItem.CODEC.encodeStart(ops, original).getOrThrow();
        TravelingItem decoded = TravelingItem.CODEC.parse(ops, encoded).getOrThrow();

        assertThat(decoded.getStack().getCount()).isEqualTo(16);
        assertThat(decoded.getStack().getItem()).isEqualTo(Items.IRON_INGOT);
    }
}

package com.logistics.core.lib.block;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.TagValueInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link BaseBlockEntity} template methods receive a valid
 * {@link HolderLookup.Provider} during load — even when {@code level} is {@code null}.
 *
 * <h2>Regression context (issue #287 / PR #283)</h2>
 * <p>PR #283 introduced a crash-fix that replaced bare {@code NbtOps.INSTANCE} with
 * {@code RegistryOps} when serializing {@code ItemStack}s. The call sites inside
 * {@code loadLogisticsData} used {@code level.registryAccess()} to obtain the ops.
 *
 * <p>However, {@code loadAdditional} is called by Minecraft during chunk deserialization,
 * <em>before</em> {@link net.minecraft.world.level.block.entity.BlockEntity#setLevel setLevel}
 * is invoked. At that point {@code level} is {@code null}, so every {@code level.registryAccess()}
 * call threw {@code NullPointerException}, aborting the entire {@code loadLogisticsData} body.
 *
 * <p>The symptom was two-fold:
 * <ol>
 *   <li>Items in transit were lost after reload (the NPE fired while loading them).</li>
 *   <li>Filters (module state) disappeared from pipes that had items in transit, because
 *       filter state was loaded <em>after</em> the items in the same method — the aborted
 *       body never reached it.</li>
 * </ol>
 *
 * <p>The fix threads {@code HolderLookup.Provider registries} from
 * {@code loadAdditional}/{@code saveAdditional} into the template methods so subclasses
 * never need {@code level.registryAccess()}.
 */
@DisplayName("BaseBlockEntity persistence")
class BaseBlockEntityPersistenceTest extends MinecraftTestEnvironment {

    /**
     * A minimal concrete {@code BaseBlockEntity} with two fields:
     * <ul>
     *   <li>{@code storedItem} — an {@code ItemStack}, serialized via {@code ItemStack.CODEC}
     *       (requires a registry-aware ops context).</li>
     *   <li>{@code storedLabel} — a plain string, loaded <em>after</em> the item.</li>
     * </ul>
     * The ordering mirrors {@code PipeBlockEntity}: items-in-transit are loaded before
     * module state. If item loading throws, the label is never reached.
     */
    private static class TwoFieldEntity extends BaseBlockEntity {

        ItemStack storedItem = ItemStack.EMPTY;
        String storedLabel = "";

        TwoFieldEntity() {
            // Use a real type/block pair to satisfy BlockEntity's validateBlockState check.
            // The type itself is irrelevant to our logic — we only test saveCustomOnly /
            // loadCustomOnly which call saveAdditional/loadAdditional directly.
            super(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        }

        @Override
        protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
            if (!storedItem.isEmpty()) {
                RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
                ItemStack.CODEC.encodeStart(ops, storedItem).result()
                        .ifPresent(encoded -> tag.put("Item", encoded));
            }
            if (!storedLabel.isEmpty()) {
                tag.putString("Label", storedLabel);
            }
        }

        @Override
        protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
            // Load item FIRST, then label — intentional ordering to mirror PipeBlockEntity.
            if (tag.contains("Item")) {
                RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
                storedItem = ItemStack.CODEC.parse(ops, tag.get("Item")).result()
                        .orElse(ItemStack.EMPTY);
            }
            storedLabel = tag.getString("Label").orElse("");
        }
    }

    private HolderLookup.Provider registries;

    @BeforeEach
    void setUp() {
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    @DisplayName("item and label both survive a save/load cycle when level is null")
    void bothFieldsSurviveSaveLoadWithNullLevel() {
        TwoFieldEntity writer = new TwoFieldEntity();
        writer.storedItem = new ItemStack(Items.DIAMOND, 7);
        writer.storedLabel = "filter-state";

        CompoundTag saved = writer.saveCustomOnly(registries);

        TwoFieldEntity reader = new TwoFieldEntity();
        assertThat(reader.hasLevel()).as("level must be absent to reproduce the regression").isFalse();
        reader.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));

        assertThat(reader.storedItem.getItem()).isEqualTo(Items.DIAMOND);
        assertThat(reader.storedItem.getCount()).isEqualTo(7);
        assertThat(reader.storedLabel).isEqualTo("filter-state");
    }

    @Test
    @DisplayName("label loads even when item serialization precedes it — regression for issue #287")
    void labelLoadsEvenWhenItemLoadingPrecedesIt() {
        // Before the fix, PipeBlockEntity.loadLogisticsData called level.registryAccess()
        // to build RegistryOps. level was null at load time → NPE → method aborted.
        // Module state (filters / label here) came AFTER items in the method body and
        // was therefore never reached, making filters vanish from any pipe that had
        // items in transit when the world was saved.
        TwoFieldEntity writer = new TwoFieldEntity();
        writer.storedItem = new ItemStack(Items.IRON_INGOT);
        writer.storedLabel = "expected-to-survive";

        CompoundTag saved = writer.saveCustomOnly(registries);

        TwoFieldEntity reader = new TwoFieldEntity();
        reader.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));

        assertThat(reader.storedLabel)
                .as("label (analogous to filter state) must load even though it follows item loading")
                .isEqualTo("expected-to-survive");
    }

    @Test
    @DisplayName("empty state round-trips without error")
    void emptyStateRoundTrips() {
        TwoFieldEntity writer = new TwoFieldEntity();
        CompoundTag saved = writer.saveCustomOnly(registries);

        TwoFieldEntity reader = new TwoFieldEntity();
        reader.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));

        assertThat(reader.storedItem.isEmpty()).isTrue();
        assertThat(reader.storedLabel).isEmpty();
    }
}

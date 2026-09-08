package com.logistics.pipe.modules;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.core.lib.storage.ContainerItemStorage;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the extractor throughput ladder: a higher tier must never move fewer items than the tier
 * it upgrades from.
 *
 * <p>Two separate claims are covered — that the filtering (Advanced) extractor pulls as much per
 * pull as the plain (Basic) one given the same budget, and that the registered tiers themselves are
 * strictly ordered.
 */
@DisplayName("Extractor throughput")
class ExtractorThroughputTest extends MinecraftTestEnvironment {

    private static final BlockPos PIPE_POS = BlockPos.ZERO;
    private static final Direction FACE = Direction.NORTH;
    private static final BlockPos CHEST_POS = PIPE_POS.relative(FACE);

    private SimpleContainer chest;

    @AfterEach
    void tearDown() {
        ItemStorageLookup.register((world, pos, dir) -> null);
    }

    // ==================== Basic vs Advanced, same budget ====================

    @Test
    @DisplayName("the filtering extractor keeps filling its budget across resources, as the plain one does")
    void advancedExtractor_topsUpAcrossResources() {
        List<ItemStack> mixed = List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.GOLD_INGOT, 4),
                new ItemStack(Items.COPPER_INGOT, 4));

        int basic = pull(new BasicExtractorModule(8, 1), 1, 0, mixed);
        int advanced = pull(new AdvancedExtractorModule(8, 1), 1, 0, mixed);

        assertThat(basic)
                .as("the plain extractor fills its 8-item budget from three 4-item stacks")
                .isEqualTo(8);
        assertThat(advanced)
                .as("the filtering extractor must not stop at the first matching resource")
                .isEqualTo(basic);
    }

    @Test
    @DisplayName("the filtering extractor accepts what fits when the pipe is nearly full")
    void advancedExtractor_acceptsPartialRoom() {
        int room = 4;
        int occupied = PipeBlockEntity.VIRTUAL_CAPACITY - room;
        List<ItemStack> stock = List.of(new ItemStack(Items.IRON_INGOT, 64));

        int basic = pull(new BasicExtractorModule(8, 1), 1, occupied, stock);
        int advanced = pull(new AdvancedExtractorModule(8, 1), 1, occupied, stock);

        assertThat(basic)
                .as("the plain extractor takes what fits rather than nothing")
                .isEqualTo(room);
        assertThat(advanced)
                .as("the filtering extractor must not go all-or-nothing on room")
                .isEqualTo(basic);
    }

    // ==================== The registered ladder ====================

    @Test
    @DisplayName("every tier is strictly faster than the one it upgrades from")
    void tiers_areStrictlyOrdered() {
        ExtractorTier[] ladder = ExtractorTier.values();

        for (int i = 1; i < ladder.length; i++) {
            assertThat(ladder[i].itemsPerTick())
                    .as("%s must out-pull %s", ladder[i], ladder[i - 1])
                    .isGreaterThan(ladder[i - 1].itemsPerTick());
        }
    }

    @Test
    @DisplayName("a MkIII pull moves more than a MkII pull from the same inventory")
    void mkiii_outPullsMkii() {
        List<ItemStack> stock = List.of(new ItemStack(Items.IRON_INGOT, 64));

        int mkii = pull(new BasicExtractorModule(ExtractorTier.MKII), ExtractorTier.MKII.ticksBetweenPulls(), 0, stock);
        int mkiii = pull(
                new AdvancedExtractorModule(ExtractorTier.MKIII), ExtractorTier.MKIII.ticksBetweenPulls(), 0, stock);

        assertThat(mkii).isEqualTo(ExtractorTier.MKII.itemsPerPull());
        assertThat(mkiii)
                .as("the diamond-tier module must not pull less than the gold-tier one it upgrades from")
                .isGreaterThan(mkii);
    }

    // ==================== Harness ====================

    /**
     * Runs one extractor for {@code ticks} ticks against a fresh chest and pipe, and returns how
     * many items it moved into the pipe.
     */
    private <M extends Module & TickingModule> int pull(
            M module, int ticks, int prefillPipe, List<ItemStack> contents) {
        chest = new SimpleContainer(27);
        for (int slot = 0; slot < contents.size(); slot++) {
            chest.setItem(slot, contents.get(slot).copy());
        }
        ItemStorageLookup.register((world, pos, dir) ->
                CHEST_POS.equals(pos) ? new ContainerItemStorage(chest, dir) : null);

        FakePipeAccess access = new FakePipeAccess().setConnection(FACE, PipeConnection.Type.INVENTORY);
        for (int filled = 0; filled < prefillPipe; ) {
            int batch = Math.min(64, prefillPipe - filled);
            access.forceAddItem(new TravelingItem(new ItemStack(Items.STONE, batch), FACE, 0.02f), FACE);
            filled += batch;
        }

        PipeContext ctx = new PipeContext(null, PIPE_POS, null, access);
        module.onConnectionsChanged(ctx, List.of());
        for (int t = 0; t < ticks; t++) {
            module.onTick(ctx);
        }

        return access.getTravelingItems().stream().mapToInt(i -> i.getStack().getCount()).sum() - prefillPipe;
    }
}

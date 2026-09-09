package com.logistics.pipe.modules;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.EnergyComponent;
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
 * Item conservation when an extractor meets a pipe with no room.
 *
 * <p>Extractors take items out of the neighbouring inventory <em>before</em> handing them to the
 * pipe, and none of them looks at what {@code forceAddItem} returns. The real pipe still enforces
 * its capacity under a forced insert and drops whatever it could not take on the floor, so an
 * extractor that pulls more than fits scatters the difference in world. Each module therefore has
 * to check the room itself, and these tests are what hold those checks in place.
 */
@DisplayName("Extractors at a full pipe")
class ExtractorSpillTest extends MinecraftTestEnvironment {

    private static final BlockPos PIPE_POS = BlockPos.ZERO;
    private static final Direction FACE = Direction.NORTH;
    private static final BlockPos CHEST_POS = PIPE_POS.relative(FACE);

    private SimpleContainer chest;

    @AfterEach
    void tearDown() {
        ItemStorageLookup.register((world, pos, dir) -> null);
    }

    @Test
    @DisplayName("the plain extractor leaves a full pipe alone instead of spilling the pull")
    void basicExtractor_atAFullPipe_spillsNothing() {
        FakePipeAccess pipe = fullPipe();

        run(new BasicExtractorModule(8, 1), 1, pipe);

        assertNothingSpilled(pipe, 64);
    }

    @Test
    @DisplayName("the filtering extractor leaves a full pipe alone instead of spilling the pull")
    void advancedExtractor_atAFullPipe_spillsNothing() {
        FakePipeAccess pipe = fullPipe();

        run(new AdvancedExtractorModule(8, 1), 1, pipe);

        assertNothingSpilled(pipe, 64);
    }

    @Test
    @DisplayName("the powered extraction pipe leaves a full pipe alone instead of spilling the pull")
    void extractionModule_atAFullPipe_spillsNothing() {
        FakePipeAccess pipe = fullPipe();
        pipe.setEnergy(poweredBuffer());

        run(new ExtractionModule(), 20, pipe);

        assertNothingSpilled(pipe, 64);
    }

    @Test
    @DisplayName("an extractor with more budget than room takes only what fits")
    void extractor_withLessRoomThanBudget_spillsNothing() {
        int room = 4;
        FakePipeAccess pipe = pipeHolding(PipeBlockEntity.VIRTUAL_CAPACITY - room);

        run(new BasicExtractorModule(8, 1), 1, pipe);

        assertThat(spilled(pipe))
                .as("a pull larger than the remaining room must be trimmed, not dropped on the floor")
                .isZero();
        assertThat(chest.getItem(0).getCount())
                .as("only the items that fit may leave the chest")
                .isEqualTo(64 - room);
        assertThat(occupancy(pipe)).isEqualTo(PipeBlockEntity.VIRTUAL_CAPACITY);
    }

    // ==================== Harness ====================

    private void assertNothingSpilled(FakePipeAccess pipe, int expectedInChest) {
        assertThat(spilled(pipe))
                .as("items pulled into a full pipe are dropped in world and lost to the network")
                .isZero();
        assertThat(chest.getItem(0).getCount())
                .as("nothing may leave the chest when the pipe cannot hold it")
                .isEqualTo(expectedInChest);
        assertThat(occupancy(pipe)).isEqualTo(PipeBlockEntity.VIRTUAL_CAPACITY);
    }

    private static int spilled(FakePipeAccess pipe) {
        return pipe.getDroppedItems().stream()
                .mapToInt(item -> item.getStack().getCount())
                .sum();
    }

    private static int occupancy(FakePipeAccess pipe) {
        return pipe.getTravelingItems().stream()
                .mapToInt(item -> item.getStack().getCount())
                .sum();
    }

    private static FakePipeAccess fullPipe() {
        return pipeHolding(PipeBlockEntity.VIRTUAL_CAPACITY);
    }

    private static FakePipeAccess pipeHolding(int occupied) {
        FakePipeAccess pipe = new FakePipeAccess().setConnection(FACE, PipeConnection.Type.INVENTORY);
        for (int filled = 0; filled < occupied; ) {
            int batch = Math.min(64, occupied - filled);
            pipe.forceAddItem(new TravelingItem(new ItemStack(Items.STONE, batch), FACE, 0.02f), FACE);
            filled += batch;
        }
        return pipe;
    }

    private static EnergyComponent poweredBuffer() {
        EnergyComponent energy = new EnergyComponent(2560, 2560, 2560, () -> {});
        energy.setAmount(2560);
        return energy;
    }

    /** Ticks {@code module} against a chest holding a full stack of iron. */
    private <M extends Module & TickingModule> void run(M module, int ticks, FakePipeAccess pipe) {
        chest = new SimpleContainer(27);
        chest.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
        ItemStorageLookup.register((world, pos, dir) ->
                CHEST_POS.equals(pos) ? new ContainerItemStorage(chest, dir) : null);

        PipeContext ctx = new PipeContext(null, PIPE_POS, null, pipe);
        module.onConnectionsChanged(ctx, List.of());
        for (int t = 0; t < ticks; t++) {
            module.onTick(ctx);
        }
    }
}

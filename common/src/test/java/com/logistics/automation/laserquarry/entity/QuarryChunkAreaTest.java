package com.logistics.automation.laserquarry.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.DomainRegistrations;
import com.logistics.LogisticsAutomation;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.core.machine.FakeMachineContext;
import com.logistics.core.machine.component.ChunkArea;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.upgrade.MachineModifiers;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The quarry's half of the chunk-loading toggle: the area it hands its {@code ChunkLoadingComponent}
 * each tick. A {@code null} area is what stops the tickets being renewed, so it lapses on its own;
 * that the tickets really are acquired and released on a live server is proven by
 * {@code QuarryGameTestBody#testQuarryChunkLoadingToggle}.
 *
 * @see <a href="https://logistics.fandom.com/wiki/Laser_Quarry#Chunk_loading">wiki/Laser Quarry.txt § Chunk loading</a>
 */
@DisplayName("Quarry chunk area")
class QuarryChunkAreaTest extends MinecraftTestEnvironment {

    private static final BlockPos QUARRY_POS = new BlockPos(100, 64, 100);

    private Config quarryConfig;
    private boolean originalLoadChunks;

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    @BeforeEach
    void rememberConfig() {
        quarryConfig = ConfigRegistry.config(LogisticsAutomation.CONFIG.QUARRY_LOAD_CHUNKS.configId());
        originalLoadChunks = quarryConfig.get(LogisticsAutomation.CONFIG.QUARRY_LOAD_CHUNKS);
    }

    @AfterEach
    void restoreConfig() {
        quarryConfig.set(LogisticsAutomation.CONFIG.QUARRY_LOAD_CHUNKS, originalLoadChunks);
    }

    private void setLoadChunks(boolean value) {
        quarryConfig.set(LogisticsAutomation.CONFIG.QUARRY_LOAD_CHUNKS, value);
    }

    private static QuarryComponent quarry() {
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 10_000L, 128L, 0L, () -> {});
        return new QuarryComponent("quarry", QUARRY_POS, energy, MachineModifiers.identity());
    }

    private static FakeMachineContext facing(Direction facing) {
        return new FakeMachineContext(
                QUARRY_POS,
                LogisticsAutomation.BLOCK.LASER_QUARRY.defaultBlockState().setValue(LaserQuarryBlock.FACING, facing));
    }

    private static List<ChunkPos> boundaryOf(ChunkArea area) {
        List<ChunkPos> chunks = new ArrayList<>();
        area.boundary().forEach(chunks::add);
        return chunks;
    }

    /**
     * Wiki claim (Chunk loading): "By default the quarry only runs while its chunk stays loaded" —
     * with {@code load_chunks} off, the quarry asks for no chunks at all.
     */
    @Test
    @DisplayName("with load_chunks off the quarry asks for no chunks")
    void toggleOffRequestsNothing() {
        setLoadChunks(false);

        assertThat(quarry().chunkArea(facing(Direction.NORTH))).isNull();
    }

    /**
     * Wiki claim (Chunk loading): "Turning on the chunk-loading setting makes a running quarry hold
     * itself and its work area loaded" — the quarry's own chunk is the center, and every chunk the
     * default 16×16 work area touches is in the boundary.
     */
    @Test
    @DisplayName("with load_chunks on the quarry asks for its own chunk and its default work area")
    void toggleOnRequestsQuarryChunkAndDefaultArea() {
        setLoadChunks(true);

        ChunkArea area = quarry().chunkArea(facing(Direction.NORTH));

        assertThat(area).isNotNull();
        assertThat(area.center()).isEqualTo(ChunkPos.containing(QUARRY_POS));
        // Facing north from (100, _, 100), the 16-wide area spans x 92..107 and z 84..99.
        assertThat(boundaryOf(area))
                .containsExactlyInAnyOrder(
                        new ChunkPos(5, 5), new ChunkPos(5, 6), new ChunkPos(6, 5), new ChunkPos(6, 6));
    }

    /**
     * Wiki claim (Chunk loading): "...hold itself and its work area loaded" — a marker-defined area
     * is followed exactly, including where it reaches across chunk borders on both axes.
     */
    @Test
    @DisplayName("with load_chunks on the quarry asks for every chunk its custom area touches")
    void toggleOnRequestsEveryChunkOfCustomArea() {
        setLoadChunks(true);
        QuarryComponent quarry = quarry();
        quarry.setCustomBounds(-5, 10, 20, 40);

        ChunkArea area = quarry.chunkArea(facing(Direction.NORTH));

        assertThat(area).isNotNull();
        assertThat(area.center()).isEqualTo(ChunkPos.containing(QUARRY_POS));
        List<ChunkPos> expected = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z <= 2; z++) {
                expected.add(new ChunkPos(x, z));
            }
        }
        assertThat(boundaryOf(area)).containsExactlyInAnyOrderElementsOf(expected);
    }

    /**
     * Wiki claim (Chunk loading): "The chunk tickets are released once the quarry finishes" — a
     * finished quarry stops asking even with {@code load_chunks} on, so its tickets lapse.
     */
    @Test
    @DisplayName("a finished quarry stops asking for chunks even with load_chunks on")
    void finishedQuarryRequestsNothing() {
        setLoadChunks(true);
        QuarryComponent quarry = quarry();
        CompoundTag finished = new CompoundTag();
        finished.putBoolean("MiningFinished", true);
        quarry.load(finished, null);

        assertThat(quarry.isFinished()).isTrue();
        assertThat(quarry.chunkArea(facing(Direction.NORTH))).isNull();
    }
}

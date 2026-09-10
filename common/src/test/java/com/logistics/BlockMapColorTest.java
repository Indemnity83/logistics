package com.logistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Blocks that read as solid terrain or machinery are drawn on in-game maps; see-through ones are not.
 *
 * <p>{@code BlockBehaviour.Properties.of()} defaults its map color to {@link MapColor#NONE}, and the
 * map scan skips NONE and keeps descending, so a block left at the default is drawn as whatever lies
 * beneath it. Nothing — not the compiler, not datagen — warns about that, which is why the whole
 * registry is checked here rather than trusting each registration site.
 *
 * <p>The crude-oil {@code LiquidBlock} is registered per loader (see
 * {@code LogisticsCore#registerFluidBlock}), so it is not in the common test registry and is not
 * covered here.
 */
@DisplayName("Block map colors")
class BlockMapColorTest extends MinecraftTestEnvironment {

    /**
     * See-through or sub-cube blocks, left off the map on purpose. Vanilla does the same for glass,
     * iron bars and torches: drawing them would hide the terrain they stand on.
     */
    private static final Set<String> SEE_THROUGH = Set.of(
        "core/marker",
        "core/quartz_crystal",
        "automation/laser_quarry_frame");

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** Pipes, tanks and cables are all see-through; the power junction is a full machine cube. */
    private static boolean isSeeThrough(String path) {
        return (path.startsWith("pipe/") && !path.equals("pipe/power_junction"))
            || path.endsWith("_cable")
            || SEE_THROUGH.contains(path);
    }

    private static Set<String> registeredPaths() {
        return BuiltInRegistries.BLOCK.entrySet().stream()
            .map(entry -> ResourceId.wrap(entry.getKey().location()))
            .filter(id -> LogisticsMod.MOD_ID.equals(id.getNamespace()))
            .map(ResourceId::getPath)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static MapColor mapColorOf(String path) {
        Block block = BuiltInRegistries.BLOCK.get(LogisticsMod.modId(path).toIdentifier());
        return block.defaultBlockState().getMapColor(null, BlockPos.ZERO);
    }

    @Test
    @DisplayName("every solid block is drawn on a map")
    void everySolidBlockHasAMapColor() {
        List<String> invisible = new ArrayList<>();
        for (String path : registeredPaths()) {
            if (!isSeeThrough(path) && mapColorOf(path) == MapColor.NONE) {
                invisible.add(path);
            }
        }

        assertThat(invisible).as("solid blocks left at the default MapColor.NONE").isEmpty();
    }

    @Test
    @DisplayName("see-through blocks stay off the map")
    void seeThroughBlocksHaveNoMapColor() {
        List<String> drawn = new ArrayList<>();
        for (String path : registeredPaths()) {
            if (isSeeThrough(path) && mapColorOf(path) != MapColor.NONE) {
                drawn.add(path);
            }
        }

        assertThat(drawn).as("see-through blocks given a map color").isEmpty();
    }

    @Test
    @DisplayName("the see-through list names blocks that still exist")
    void seeThroughListDoesNotRot() {
        assertThat(registeredPaths()).containsAll(SEE_THROUGH);
    }
}

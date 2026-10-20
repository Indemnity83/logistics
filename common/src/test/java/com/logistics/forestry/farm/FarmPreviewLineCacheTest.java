package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Farm preview line cache")
class FarmPreviewLineCacheTest extends MinecraftTestEnvironment {

    private final AtomicInteger builds = new AtomicInteger();
    private final FarmPreviewLineCache<Object> cache = new FarmPreviewLineCache<>((preview, origin) -> {
        builds.incrementAndGet();
        return FarmPreviewLines.of(preview.inner(), preview.outer(), preview.height(), List.of(), List.of());
    });

    private static FarmPreview preview(int outer, List<BlockPos> flagged) {
        return new FarmPreview(
                FarmState.ACTIVE, true, 0, 7, outer, 5, FarmSurvey.of(0, flagged.size(), 0, flagged, List.of()));
    }

    @Test
    @DisplayName("the same preview and origin return the same lines without rebuilding")
    void sameInputsHit() {
        Object farm = new Object();
        FarmPreview preview = preview(11, List.of(new BlockPos(1, 2, 3)));
        BlockPos origin = new BlockPos(10, 64, 10);

        List<FarmPreviewLines.Line> first = cache.lines(farm, preview, origin);

        assertThat(cache.lines(farm, preview, origin)).isSameAs(first);
        assertThat(cache.lines(farm, preview(11, List.of(new BlockPos(1, 2, 3))), new BlockPos(10, 64, 10)))
                .as("an equal preview read from a later update tag")
                .isSameAs(first);
        assertThat(builds).hasValue(1);
    }

    @Test
    @DisplayName("a changed preview or origin rebuilds the lines")
    void changedInputsRebuild() {
        Object farm = new Object();
        BlockPos origin = new BlockPos(10, 64, 10);
        List<FarmPreviewLines.Line> first = cache.lines(farm, preview(11, List.of()), origin);

        List<FarmPreviewLines.Line> wider = cache.lines(farm, preview(13, List.of()), origin);
        List<FarmPreviewLines.Line> flagged = cache.lines(farm, preview(13, List.of(new BlockPos(0, 1, 0))), origin);
        List<FarmPreviewLines.Line> moved = cache.lines(farm, preview(13, List.of(new BlockPos(0, 1, 0))), origin.east());

        assertThat(wider).isNotSameAs(first);
        assertThat(flagged).isNotSameAs(wider);
        assertThat(moved).isNotSameAs(flagged);
        assertThat(builds).hasValue(4);
    }

    @Test
    @DisplayName("each farm keeps its own lines, and a forgotten one rebuilds")
    void perOwner() {
        Object a = new Object();
        Object b = new Object();
        FarmPreview preview = preview(11, List.of());
        BlockPos origin = BlockPos.ZERO;

        List<FarmPreviewLines.Line> forA = cache.lines(a, preview, origin);
        List<FarmPreviewLines.Line> forB = cache.lines(b, preview, origin);
        assertThat(forB).isNotSameAs(forA);
        assertThat(cache.lines(a, preview, origin)).isSameAs(forA);

        cache.forget(a);
        assertThat(cache.lines(a, preview, origin)).isNotSameAs(forA);
        assertThat(builds).hasValue(3);
    }
}

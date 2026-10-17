package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Farm preview lines")
class FarmPreviewLinesTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("two squares at the soil plane with corner posts, then a box per flagged and protected block")
    void lines() {
        List<FarmPreviewLines.Line> lines =
                FarmPreviewLines.of(7, 11, 5, List.of(new BlockPos(4, 1, 0)), List.of(new BlockPos(-5, 2, 5)));

        assertThat(lines).hasSize(8 + 8 + 12 + 12);
        List<FarmPreviewLines.Line> courtyard =
                lines.stream().filter(l -> l.color() == FarmPreviewLines.COURTYARD_COLOR).toList();
        List<FarmPreviewLines.Line> outer =
                lines.stream().filter(l -> l.color() == FarmPreviewLines.OUTER_COLOR).toList();
        assertThat(courtyard).hasSize(8);
        assertThat(outer).hasSize(8);
        // The 7×7 courtyard spans blocks -3..3, so its edges run from -3 to 4; the 11×11 ring's from -5 to 6.
        assertThat(courtyard).allSatisfy(l -> assertThat(l.x0()).isIn(-3f, 4f));
        assertThat(outer).allSatisfy(l -> assertThat(l.z1()).isIn(-5f, 6f));
        assertThat(outer).filteredOn(l -> l.y0() != l.y1())
                .hasSize(4)
                .allSatisfy(post -> assertThat(post.y1()).as("posts reach the clearance height").isEqualTo(6f));
        assertThat(outer).allSatisfy(l -> assertThat(l.y0()).as("squares sit on the soil plane").isEqualTo(FarmPreviewLines.LIFT));

        List<FarmPreviewLines.Line> flagged =
                lines.stream().filter(l -> l.color() == FarmPreviewLines.FLAGGED_COLOR).toList();
        assertThat(flagged).hasSize(12);
        assertThat(flagged.stream().mapToDouble(FarmPreviewLines.Line::x0).min().orElseThrow())
                .isEqualTo(4 - FarmPreviewLines.INFLATE, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(flagged.stream().mapToDouble(FarmPreviewLines.Line::x0).max().orElseThrow())
                .isEqualTo(5 + FarmPreviewLines.INFLATE, org.assertj.core.data.Offset.offset(1e-6));
        List<FarmPreviewLines.Line> skipped =
                lines.stream().filter(l -> l.color() == FarmPreviewLines.PROTECTED_COLOR).toList();
        assertThat(skipped).hasSize(12);
        assertThat(skipped.stream().mapToDouble(FarmPreviewLines.Line::y1).max().orElseThrow())
                .isEqualTo(3 + FarmPreviewLines.INFLATE, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    @DisplayName("nothing is drawn for a ring of no size")
    void empty() {
        assertThat(FarmPreviewLines.of(0, 0, 5, List.of(), List.of())).isEmpty();
    }
}

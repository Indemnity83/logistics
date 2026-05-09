package com.logistics.pipe.modules;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PipeOnlyModule}.
 *
 * <p>{@code PipeContext} is {@code @Nullable} and unused in {@code allowsConnection()} —
 * {@code null} is passed throughout.
 *
 * <p>The {@code true} return case (PipeBlock neighbor) cannot be tested without restructuring:
 * it requires a registered mod {@code PipeBlock} instance, which is not available in the
 * vanilla bootstrap. See TESTING.md.
 */
@DisplayName("PipeOnlyModule")
class PipeOnlyModuleTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("non-PipeBlock neighbor denies connection")
    void nonPipeBlock_deniesConnection() {
        PipeOnlyModule module = new PipeOnlyModule();
        boolean result = module.allowsConnection(null, Direction.NORTH, Blocks.STONE);
        assertThat(result).isFalse();
    }
}

package com.logistics.pipe.modules;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BlockConnectionModule}.
 *
 * <p>{@code PipeContext} is {@code @Nullable} and unused in {@code allowsConnection()} —
 * {@code null} is passed throughout.
 *
 * <p>The PipeBlock-neighbor case (returning {@code false} for the blocked pipe type)
 * cannot be tested without restructuring: it requires registered mod {@code PipeBlock}
 * instances, which are not available in the vanilla bootstrap. See TESTING.md.
 */
@DisplayName("BlockConnectionModule")
class BlockConnectionModuleTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("non-PipeBlock neighbor always allows connection")
    void nonPipeBlock_alwaysAllowsConnection() {
        BlockConnectionModule module = new BlockConnectionModule(() -> null);
        boolean result = module.allowsConnection(null, Direction.NORTH, Blocks.STONE);
        assertThat(result).isTrue();
    }
}

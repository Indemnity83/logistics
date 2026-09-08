package com.logistics.core.machine;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.menu.MenuValidity;
import com.logistics.core.machine.component.SlotRole;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * How long a machine screen stays open. Every machine menu — Macerator, Kiln, Sawmill, Crucible,
 * Refinery, Alloy Smelter, Transposer, Sequential Fabricator — routes its {@code stillValid}
 * through {@link MachineEntity}, so the rule is pinned once here.
 *
 * <p>Covers what is decidable without a level: a machine that was never placed, and the reach
 * boundary. The real-world case — a placed machine mined out from under an open screen — needs a
 * live level to distinguish a stale block entity from a live one, and is covered by the
 * {@code core/macerator_screen_closes_when_broken} GameTest.
 *
 * <p>The machine-is-gone case passes no player on purpose: a machine that is not there must close
 * for everyone, so the player is never consulted.
 */
@DisplayName("Machine menu validity")
class MachineMenuValidityTest extends MinecraftTestEnvironment {

    /** No player is needed to decide that a machine which is not there closes its screen. */
    private static final Player ANY_PLAYER = null;

    private static double blocks(double distance) {
        return distance * distance;
    }

    /** A machine shell backed by a vanilla BE type so no mod registration is needed. */
    private static final class TestMachine extends MachineEntity {
        TestMachine() {
            super(BlockEntityTypes.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        }

        @Override
        protected void configure(MachineBuilder machine) {
            machine.items("inventory").slots(SlotRole.INPUT, SlotRole.OUTPUT).build();
        }

        @Override
        public MenuProvider createMenuProvider() {
            return null;
        }
    }

    @Test
    @DisplayName("a machine that is not in a level closes its screen")
    void anUnplacedMachineClosesItsScreen() {
        assertThat(new TestMachine().stillValid(ANY_PLAYER)).isFalse();
    }

    @Test
    @DisplayName("a player at the edge of reach keeps the screen open")
    void acceptsPlayerAtTheEdgeOfReach() {
        assertThat(MenuValidity.isWithinReach(blocks(7.9))).isTrue();
        assertThat(MenuValidity.isWithinReach(blocks(8.0))).isTrue();
    }

    @Test
    @DisplayName("a player past reach does not")
    void rejectsPlayerPastReach() {
        assertThat(MenuValidity.isWithinReach(blocks(8.1))).isFalse();
    }

    @Test
    @DisplayName("matches vanilla's block interaction range")
    void matchesVanillaInteractionRange() {
        // isWithinBlockInteractionRange(pos, 4.0): the 4.5 block_interaction_range attribute
        // padded by 4, measured to the block AABB.
        assertThat(MenuValidity.MAX_REACH).isEqualTo(8.0);
    }
}

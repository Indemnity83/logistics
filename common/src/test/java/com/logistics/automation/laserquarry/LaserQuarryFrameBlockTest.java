package com.logistics.automation.laserquarry;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsAutomation;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The frame's lifecycle contract: the quarry places and removes it, a player can always
 * clear a stranded one by hand, and nothing deletes it behind the player's back.
 */
@DisplayName("Laser quarry frame block")
class LaserQuarryFrameBlockTest extends MinecraftTestEnvironment {

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private static Block frame() {
        return LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME;
    }

    private static BlockState frameState() {
        return frame().defaultBlockState();
    }

    @Test
    @DisplayName("can be mined in survival")
    void canBeMinedInSurvival() {
        assertThat(frameState().getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO))
                .as("frame destroy time; -1 is bedrock and can never be broken in survival")
                .isGreaterThan(0.0f);
    }

    @Test
    @DisplayName("is not blast proof")
    void isNotBlastProof() {
        assertThat(frame().getExplosionResistance())
                .as("frame blast resistance; bedrock is 3,600,000")
                .isLessThan(100.0f);
    }

    @Test
    @DisplayName("never decays on a random tick")
    void neverDecaysOnRandomTick() {
        assertThat(frameState().isRandomlyTicking())
                .as("random-tick decay guesses at ownership and deletes frames with no drops")
                .isFalse();
    }

    @Test
    @DisplayName("drops nothing when broken")
    void dropsNothingWhenBroken() {
        // getLootTable() returns the key directly on this version, not an Optional.
        assertThat(frame().getLootTable())
                .as("the quarry builds the frame for free, so breaking it must not return items")
                .isEqualTo(BuiltInLootTables.EMPTY);
    }
}

package com.logistics.core.lib.block.capability;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.block.capability.PipeConnection.Type;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The signature is what tells a pipe its rendered arms have changed. Connectedness alone is not
 * enough: a side can stay connected and still change what it is connected to, which renders
 * differently.
 */
@DisplayName("Connection signature")
class PipeConnectionSignatureTest {

    private static Type[] types(Type... byDirection) {
        Type[] all = new Type[Direction.values().length];
        java.util.Arrays.fill(all, Type.NONE);
        System.arraycopy(byDirection, 0, all, 0, byDirection.length);
        return all;
    }

    @Test
    @DisplayName("unchanged connections produce an unchanged signature")
    void stableWhenNothingChanges() {
        assertThat(Type.signature(types(Type.PIPE, Type.INVENTORY)))
                .isEqualTo(Type.signature(types(Type.PIPE, Type.INVENTORY)));
    }

    @Test
    @DisplayName("a side that stays connected but changes type is a change")
    void powerBecomingInventoryIsAChange() {
        // The reported case: a Power Junction replaced by a chest in the same tick. The side is
        // connected before and after, so a connectedness mask sees nothing.
        assertThat(Type.signature(types(Type.POWER)))
                .isNotEqualTo(Type.signature(types(Type.INVENTORY)));
    }

    @Test
    @DisplayName("the connectedness mask alone cannot see it")
    void theConnectednessMaskIsBlindToATypeChange() {
        // Why the sync gate needed widening: both states connect on exactly the same sides, so the
        // mask the gate used to compare is byte-for-byte identical across the change.
        Type[] before = types(Type.POWER);
        Type[] after = types(Type.INVENTORY);

        assertThat(connectednessMask(before))
                .as("the old gate saw no difference here")
                .isEqualTo(connectednessMask(after));
        assertThat(Type.signature(before)).isNotEqualTo(Type.signature(after));
    }

    /** The mask {@code handleConnectionChanges} used to compare: one bit per connected side. */
    private static int connectednessMask(Type[] byDirection) {
        int mask = 0;
        for (Direction direction : Direction.values()) {
            if (byDirection[direction.ordinal()] != Type.NONE) {
                mask |= 1 << direction.get3DDataValue();
            }
        }
        return mask;
    }

    @Test
    @DisplayName("a pipe arm becoming an inventory arm is a change")
    void pipeBecomingInventoryIsAChange() {
        assertThat(Type.signature(types(Type.PIPE)))
                .isNotEqualTo(Type.signature(types(Type.INVENTORY)));
    }

    @Test
    @DisplayName("connecting and disconnecting are still changes")
    void connectednessStillCounts() {
        assertThat(Type.signature(types(Type.NONE))).isNotEqualTo(Type.signature(types(Type.PIPE)));
    }

    @Test
    @DisplayName("every direction is tracked independently")
    void eachDirectionIsDistinct() {
        java.util.Set<Integer> signatures = new java.util.HashSet<>();
        for (Direction direction : Direction.values()) {
            Type[] all = types();
            all[direction.ordinal()] = Type.INVENTORY;
            signatures.add(Type.signature(all));
        }
        assertThat(signatures)
                .as("a change on one side must not look like a change on another")
                .hasSize(Direction.values().length);
    }
}

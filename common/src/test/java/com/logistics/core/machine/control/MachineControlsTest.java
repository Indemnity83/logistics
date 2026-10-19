package com.logistics.core.machine.control;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.inventory.ContainerData;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Machine controls sync and button protocol")
class MachineControlsTest {

    /** A mutable {@link MachineStatus} recording every mode change it is asked for. */
    static final class FakeStatus implements MachineStatus {
        @Nullable
        RedstoneControlComponent redstone = new RedstoneControlComponent("redstone", RedstoneMode.IGNORED, () -> {});
        boolean signal;
        boolean enabled = true;
        boolean removed;
        boolean buffer = true;
        long stored;
        long capacity;
        long rate;
        long maxRate;
        final List<RedstoneMode> modeChanges = new ArrayList<>();

        @Override
        @Nullable
        public RedstoneControlComponent redstoneControl() {
            return redstone;
        }

        @Override
        public boolean hasRedstoneSignal() {
            return signal;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setRedstoneMode(RedstoneMode mode) {
            modeChanges.add(mode);
            if (redstone != null) {
                redstone.setMode(mode);
            }
        }

        @Override
        public boolean isRemoved() {
            return removed;
        }

        @Override
        public boolean hasEnergyBuffer() {
            return buffer;
        }

        @Override
        public long energyStored() {
            return stored;
        }

        @Override
        public long energyCapacity() {
            return capacity;
        }

        @Override
        public long energyRate() {
            return rate;
        }

        @Override
        public long energyMaxRate() {
            return maxRate;
        }
    }

    /** Copies every server slot into a fresh client instance, truncated to a short as container sync does. */
    private static MachineControls synced(MachineControls server) {
        MachineControls client = MachineControls.client();
        ContainerData from = server.data();
        for (int i = 0; i < MachineControls.COUNT; i++) {
            client.data().set(i, (short) from.get(i));
        }
        return client;
    }

    private static int[] snapshot(ContainerData data) {
        int[] values = new int[data.getCount()];
        for (int i = 0; i < values.length; i++) {
            values[i] = data.get(i);
        }
        return values;
    }

    static Stream<Arguments> statusCombinations() {
        Stream.Builder<Arguments> combos = Stream.builder();
        for (RedstoneMode mode : RedstoneMode.values()) {
            for (int bits = 0; bits < 16; bits++) {
                combos.add(Arguments.of(mode, (bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0));
            }
        }
        return combos.build();
    }

    @ParameterizedTest(name = "{0} signal={1} enabled={2} supported={3} buffer={4}")
    @MethodSource("statusCombinations")
    @DisplayName("the packed status word round-trips every mode and flag combination")
    void statusWordRoundTrips(RedstoneMode mode, boolean signal, boolean enabled, boolean supported, boolean buffer) {
        FakeStatus status = new FakeStatus();
        status.redstone = supported ? new RedstoneControlComponent("redstone", mode, () -> {}) : null;
        status.signal = signal;
        status.enabled = enabled;
        status.buffer = buffer;

        MachineControls client = synced(MachineControls.server(status));

        assertThat(client.supported()).isEqualTo(supported);
        assertThat(client.signal()).isEqualTo(signal);
        assertThat(client.enabled()).isEqualTo(enabled);
        assertThat(client.hasBuffer()).isEqualTo(buffer);
        assertThat(client.mode()).isEqualTo(supported ? mode : RedstoneMode.IGNORED);
    }

    @Test
    @DisplayName("the mode is packed by sync id, not ordinal")
    void modeIsPackedBySyncId() {
        for (RedstoneMode mode : RedstoneMode.values()) {
            int word = MachineControls.pack(mode, false, false, true, false);
            assertThat(word & MachineControls.MODE_MASK).isEqualTo(mode.syncId());
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 32_767, 32_768, 65_535, 65_536, 1_000_000, 123_456_789, Integer.MAX_VALUE})
    @DisplayName("energy values survive short-sized sync at any magnitude")
    void wideValuesRoundTrip(long value) {
        FakeStatus status = new FakeStatus();
        status.stored = value;
        status.capacity = value;
        status.rate = value;
        status.maxRate = value;

        MachineControls client = synced(MachineControls.server(status));

        assertThat(client.stored()).isEqualTo(value);
        assertThat(client.capacity()).isEqualTo(value);
        assertThat(client.rate()).isEqualTo(value);
        assertThat(client.maxRate()).isEqualTo(value);
    }

    @Test
    @DisplayName("values beyond an int saturate at Integer.MAX_VALUE, and negatives at zero")
    void wideValuesSaturate() {
        FakeStatus status = new FakeStatus();
        status.stored = Long.MAX_VALUE;
        status.capacity = Integer.MAX_VALUE + 1L;
        status.rate = -5;
        status.maxRate = 10_000_000_000L;

        MachineControls client = synced(MachineControls.server(status));

        assertThat(client.stored()).isEqualTo(Integer.MAX_VALUE);
        assertThat(client.capacity()).isEqualTo(Integer.MAX_VALUE);
        assertThat(client.rate()).isZero();
        assertThat(client.maxRate()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("the server data reads the status live on every get")
    void serverDataIsLive() {
        FakeStatus status = new FakeStatus();
        MachineControls server = MachineControls.server(status);
        assertThat(server.rate()).isZero();

        status.rate = 40_000;
        status.signal = true;

        assertThat(server.rate()).isEqualTo(40_000);
        assertThat(server.signal()).isTrue();
    }

    @Test
    @DisplayName("client and server data have the same slot count")
    void clientAndServerCountsMatch() {
        assertThat(MachineControls.client().data().getCount()).isEqualTo(MachineControls.COUNT);
        assertThat(MachineControls.server(new FakeStatus()).data().getCount()).isEqualTo(MachineControls.COUNT);
    }

    @Test
    @DisplayName("an unsynced client reports no redstone control")
    void unsyncedClientIsUnsupported() {
        MachineControls client = MachineControls.client();
        assertThat(client.supported()).isFalse();
        for (RedstoneMode mode : RedstoneMode.values()) {
            assertThat(client.click(() -> false, MachineControls.redstoneButton(mode))).isFalse();
        }
    }

    @Test
    @DisplayName("redstone buttons are the base id plus the sync id")
    void redstoneButtonIds() {
        for (RedstoneMode mode : RedstoneMode.values()) {
            assertThat(MachineControls.redstoneButton(mode)).isEqualTo(MachineControls.BUTTON_BASE + mode.syncId());
        }
    }

    // ==================== Client validation ====================

    @Test
    @DisplayName("the client accepts a different mode but changes nothing")
    void clientValidatesWithoutMutating() {
        FakeStatus status = new FakeStatus();
        status.redstone = new RedstoneControlComponent("redstone", RedstoneMode.LOW, () -> {});
        MachineControls client = synced(MachineControls.server(status));
        int[] before = snapshot(client.data());

        assertThat(client.click(() -> false, MachineControls.redstoneButton(RedstoneMode.HIGH))).isTrue();
        assertThat(client.click(() -> false, MachineControls.redstoneButton(RedstoneMode.IGNORED))).isTrue();

        assertThat(snapshot(client.data())).isEqualTo(before);
        assertThat(client.mode()).isEqualTo(RedstoneMode.LOW);
    }

    @Test
    @DisplayName("the client never consults the player")
    void clientIgnoresThePlayer() {
        FakeStatus status = new FakeStatus();
        MachineControls client = synced(MachineControls.server(status));

        assertThat(client.clickMenuButton(null, MachineControls.redstoneButton(RedstoneMode.HIGH))).isTrue();
    }

    @Test
    @DisplayName("the client rejects the current mode")
    void clientRejectsCurrentMode() {
        FakeStatus status = new FakeStatus();
        status.redstone = new RedstoneControlComponent("redstone", RedstoneMode.HIGH, () -> {});
        MachineControls client = synced(MachineControls.server(status));

        assertThat(client.click(() -> false, MachineControls.redstoneButton(RedstoneMode.HIGH))).isFalse();
    }

    @Test
    @DisplayName("the client rejects an unsupported host")
    void clientRejectsUnsupported() {
        FakeStatus status = new FakeStatus();
        status.redstone = null;
        MachineControls client = synced(MachineControls.server(status));

        assertThat(client.click(() -> false, MachineControls.redstoneButton(RedstoneMode.HIGH))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 99, MachineControls.BUTTON_BASE + 3, MachineControls.BUTTON_BASE + 27, 128, 1_000})
    @DisplayName("the client rejects out-of-range and unknown ids")
    void clientRejectsBadIds(int id) {
        MachineControls client = synced(MachineControls.server(new FakeStatus()));
        assertThat(client.click(() -> false, id)).isFalse();
    }

    // ==================== Server handling ====================

    @ParameterizedTest
    @EnumSource(RedstoneMode.class)
    @DisplayName("the server applies a valid click")
    void serverAppliesMode(RedstoneMode mode) {
        FakeStatus status = new FakeStatus();
        MachineControls server = MachineControls.server(status);

        assertThat(server.click(() -> false, MachineControls.redstoneButton(mode))).isTrue();

        assertThat(status.modeChanges).containsExactly(mode);
        assertThat(status.redstone.mode()).isEqualTo(mode);
        assertThat(server.mode()).isEqualTo(mode);
    }

    @Test
    @DisplayName("the server rejects a spectator")
    void serverRejectsSpectator() {
        FakeStatus status = new FakeStatus();
        MachineControls server = MachineControls.server(status);

        assertThat(server.click(() -> true, MachineControls.redstoneButton(RedstoneMode.HIGH))).isFalse();
        assertThat(status.modeChanges).isEmpty();
        assertThat(status.redstone.mode()).isEqualTo(RedstoneMode.IGNORED);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 99, MachineControls.BUTTON_BASE + 3, MachineControls.BUTTON_BASE + 27, 128, 1_000})
    @DisplayName("the server rejects out-of-range and unknown ids")
    void serverRejectsBadIds(int id) {
        FakeStatus status = new FakeStatus();
        MachineControls server = MachineControls.server(status);

        assertThat(server.click(() -> false, id)).isFalse();
        assertThat(status.modeChanges).isEmpty();
    }

    @Test
    @DisplayName("ids outside the reserved range never consult the player")
    void outOfRangeIdsFallThrough() {
        MachineControls server = MachineControls.server(new FakeStatus());
        assertThat(server.clickMenuButton(null, 0)).isFalse();
        assertThat(server.clickMenuButton(null, MachineControls.BUTTON_BASE + MachineControls.BUTTON_RANGE)).isFalse();
    }

    @Test
    @DisplayName("the server rejects a host without redstone control")
    void serverRejectsUnsupported() {
        FakeStatus status = new FakeStatus();
        status.redstone = null;
        MachineControls server = MachineControls.server(status);

        assertThat(server.click(() -> false, MachineControls.redstoneButton(RedstoneMode.HIGH))).isFalse();
        assertThat(status.modeChanges).isEmpty();
    }

    @Test
    @DisplayName("the server rejects a removed host")
    void serverRejectsRemoved() {
        FakeStatus status = new FakeStatus();
        status.removed = true;
        MachineControls server = MachineControls.server(status);

        assertThat(server.click(() -> false, MachineControls.redstoneButton(RedstoneMode.HIGH))).isFalse();
        assertThat(status.modeChanges).isEmpty();
        assertThat(status.redstone.mode()).isEqualTo(RedstoneMode.IGNORED);
    }
}

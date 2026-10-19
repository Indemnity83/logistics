package com.logistics.core.machine.control;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BooleanSupplier;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class RedstoneControlComponentTest {

    private int changes;

    private RedstoneControlComponent component(RedstoneMode defaultMode) {
        return new RedstoneControlComponent("redstone", defaultMode, () -> changes++);
    }

    private static CompoundTag save(RedstoneControlComponent component) {
        CompoundTag tag = new CompoundTag();
        component.save(tag, null);
        return tag;
    }

    @Test
    void defaultModeIsNotWritten() {
        assertThat(save(component(RedstoneMode.IGNORED)).isEmpty()).isTrue();
        assertThat(save(component(RedstoneMode.HIGH)).isEmpty()).isTrue();
    }

    @Test
    void nonDefaultModeRoundTrips() {
        RedstoneControlComponent writer = component(RedstoneMode.IGNORED);
        writer.setMode(RedstoneMode.LOW);
        CompoundTag tag = save(writer);
        assertThat(tag.getString("Mode")).contains("low");

        RedstoneControlComponent reader = component(RedstoneMode.IGNORED);
        reader.load(tag, null);
        assertThat(reader.mode()).isEqualTo(RedstoneMode.LOW);
    }

    @Test
    void returningToDefaultRemovesTheKey() {
        RedstoneControlComponent component = component(RedstoneMode.HIGH);
        component.setMode(RedstoneMode.IGNORED);
        assertThat(save(component).contains("Mode")).isTrue();

        component.setMode(RedstoneMode.HIGH);
        assertThat(save(component).isEmpty()).isTrue();
    }

    @Test
    void missingKeyResetsToDefault() {
        RedstoneControlComponent component = component(RedstoneMode.IGNORED);
        component.setMode(RedstoneMode.HIGH);

        component.load(new CompoundTag(), null);

        assertThat(component.mode()).isEqualTo(RedstoneMode.IGNORED);
    }

    @Test
    void unknownNameFallsBackToDefault() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Mode", "sideways");
        RedstoneControlComponent component = component(RedstoneMode.HIGH);
        component.setMode(RedstoneMode.LOW);

        component.load(tag, null);

        assertThat(component.mode()).isEqualTo(RedstoneMode.HIGH);
    }

    @Test
    void legacyLoadUsesDefault() {
        RedstoneControlComponent component = component(RedstoneMode.HIGH);
        component.setMode(RedstoneMode.LOW);

        component.loadLegacy(new CompoundTag(), null);

        assertThat(component.mode()).isEqualTo(RedstoneMode.HIGH);
    }

    @Test
    void ignoredNeverEvaluatesTheSignal() {
        BooleanSupplier throwing = () -> {
            throw new AssertionError("signal evaluated");
        };

        assertThat(component(RedstoneMode.IGNORED).allows(throwing)).isTrue();
    }

    @Test
    void lowAndHighApplyTheSignal() {
        RedstoneControlComponent component = component(RedstoneMode.IGNORED);
        component.setMode(RedstoneMode.LOW);
        assertThat(component.allows(() -> false)).isTrue();
        assertThat(component.allows(() -> true)).isFalse();

        component.setMode(RedstoneMode.HIGH);
        assertThat(component.allows(() -> false)).isFalse();
        assertThat(component.allows(() -> true)).isTrue();
    }

    @Test
    void setModeNotifiesOnlyOnChange() {
        RedstoneControlComponent component = component(RedstoneMode.IGNORED);

        component.setMode(RedstoneMode.IGNORED);
        assertThat(changes).isZero();

        component.setMode(RedstoneMode.HIGH);
        assertThat(changes).isEqualTo(1);

        component.setMode(RedstoneMode.HIGH);
        assertThat(changes).isEqualTo(1);
    }
}

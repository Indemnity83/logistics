package com.logistics.core.machine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class MachineComponentContainerTest extends MinecraftTestEnvironment {

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    /** Records tick order and persists a single int under {@code v}, with a legacy root fallback. */
    private static final class RecordingComponent implements MachineComponent {
        private final String id;
        private final List<String> tickLog;
        private final String legacyKey;
        int value;

        RecordingComponent(String id, List<String> tickLog, String legacyKey) {
            this.id = id;
            this.tickLog = tickLog;
            this.legacyKey = legacyKey;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public void serverTick(MachineContext ctx) {
            tickLog.add(id);
        }

        @Override
        public void save(CompoundTag tag, HolderLookup.Provider registries) {
            if (value != 0) {
                tag.putInt("v", value);
            }
        }

        @Override
        public void load(CompoundTag tag, HolderLookup.Provider registries) {
            value = NbtCompat.getInt(tag, "v", 0);
        }

        @Override
        public void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {
            value = NbtCompat.getInt(root, legacyKey, 0);
        }
    }

    @Test
    void ticksInRegistrationOrder() {
        List<String> log = new ArrayList<>();
        MachineComponentContainer container = new MachineComponentContainer();
        container.add(new RecordingComponent("energy", log, "Energy"));
        container.add(new RecordingComponent("processor", log, "Progress"));

        container.serverTick(new FakeMachineContext());

        assertThat(log).containsExactly("energy", "processor");
    }

    @Test
    void forEachVisitsAllMatchingInRegistrationOrder() {
        MachineComponentContainer container = new MachineComponentContainer();
        container.add(new RecordingComponent("a", new ArrayList<>(), "A"));
        container.add(new RecordingComponent("b", new ArrayList<>(), "B"));

        List<String> visited = new ArrayList<>();
        container.forEach(MachineComponent.class, c -> visited.add(c.id()));

        assertThat(visited).containsExactly("a", "b");
    }

    @Test
    void duplicateIdRejected() {
        MachineComponentContainer container = new MachineComponentContainer();
        container.add(new RecordingComponent("energy", new ArrayList<>(), "Energy"));

        assertThatThrownBy(() -> container.add(new RecordingComponent("energy", new ArrayList<>(), "Energy")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void savesAndLoadsEachComponentById() {
        MachineComponentContainer writer = new MachineComponentContainer();
        RecordingComponent a = writer.add(new RecordingComponent("energy", new ArrayList<>(), "Energy"));
        RecordingComponent b = writer.add(new RecordingComponent("processor", new ArrayList<>(), "Progress"));
        a.value = 42;
        b.value = 7;

        CompoundTag root = new CompoundTag();
        writer.save(root, registries);

        MachineComponentContainer reader = new MachineComponentContainer();
        RecordingComponent ra = reader.add(new RecordingComponent("energy", new ArrayList<>(), "Energy"));
        RecordingComponent rb = reader.add(new RecordingComponent("processor", new ArrayList<>(), "Progress"));
        reader.load(root, registries);

        assertThat(ra.value).isEqualTo(42);
        assertThat(rb.value).isEqualTo(7);
    }

    @Test
    void readsLegacyRootKeysWhenNoComponentsTag() {
        CompoundTag legacyRoot = new CompoundTag();
        legacyRoot.putInt("Energy", 500);
        legacyRoot.putInt("Progress", 9);

        MachineComponentContainer container = new MachineComponentContainer();
        RecordingComponent energy = container.add(new RecordingComponent("energy", new ArrayList<>(), "Energy"));
        RecordingComponent processor = container.add(new RecordingComponent("processor", new ArrayList<>(), "Progress"));
        container.load(legacyRoot, registries);

        assertThat(energy.value).isEqualTo(500);
        assertThat(processor.value).isEqualTo(9);
    }
}

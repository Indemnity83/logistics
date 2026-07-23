package com.logistics.core.lib.power;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.power.component.EngineBurnComponent;
import com.logistics.core.lib.power.component.EnginePistonCycleComponent;
import com.logistics.core.lib.power.gen.FixedGeneration;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.Test;

/**
 * Persistence of an {@link EngineEntity} composed from the real engine components, including migration
 * of legacy saves.
 */
class EnginePersistenceTest extends MinecraftTestEnvironment {

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    /** An engine shell backed by a vanilla BE type so no mod registration is needed. */
    private static final class TestEngine extends EngineEntity {
        EngineBurnComponent burn;
        EnginePistonCycleComponent cycle;

        TestEngine() {
            super(BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        }

        @Override
        protected void configure(EngineBuilder engine) {
            var energy = engine.energyOutput("energy").capacity(() -> 10_000L).build();
            var heat = engine.heat("heat").energy(energy).running(() -> false).build();
            burn = engine.burn("burn")
                    .energy(energy)
                    .heat(heat)
                    .fuel(ctx -> 0)
                    .generation(new FixedGeneration(0))
                    .build();
            cycle = engine.pistonCycle("cycle").energy(energy).overheated(heat::isOverheated).build();
        }

        void save(CompoundTag tag) {
            saveLogisticsData(tag, registries());
        }

        void load(CompoundTag tag) {
            loadLogisticsData(tag, registries());
        }
    }

    private static CompoundTag legacySave() {
        CompoundTag legacy = new CompoundTag();
        legacy.putLong("StoredEnergy", 1234);
        legacy.putDouble("Temperature", 99.0);
        legacy.putInt("HeatStage", HeatStage.WARM.ordinal());
        legacy.putFloat("CycleProgress", 0.3f);
        legacy.putInt("CyclePhase", CyclePhase.EXPANSION.ordinal());
        legacy.putInt("BurnTimeRemaining", 100);
        legacy.putInt("TotalFuelTime", 200);
        return legacy;
    }

    @Test
    void migratesLegacyRootKeys() {
        TestEngine reader = new TestEngine();
        reader.load(legacySave());

        assertThat(reader.getEnergy()).isEqualTo(1234);
        assertThat(reader.getTemperature()).isEqualTo(99.0);
        assertThat(reader.getHeatStage()).isEqualTo(HeatStage.WARM);
        assertThat(reader.getProgress()).isEqualTo(0.3f);
        assertThat(reader.cycle.cyclePhase()).isEqualTo(CyclePhase.EXPANSION);
        assertThat(reader.burn.burnTime()).isEqualTo(100);
        assertThat(reader.burn.fuelTime()).isEqualTo(200);
    }

    @Test
    void newFormatRoundTripsThroughComponentsTag() {
        TestEngine writer = new TestEngine();
        writer.load(legacySave()); // seed state via the legacy path

        CompoundTag saved = new CompoundTag();
        writer.save(saved);
        assertThat(saved.contains("components")).isTrue();

        TestEngine reader = new TestEngine();
        reader.load(saved);

        assertThat(reader.getEnergy()).isEqualTo(1234);
        assertThat(reader.getTemperature()).isEqualTo(99.0);
        assertThat(reader.getHeatStage()).isEqualTo(HeatStage.WARM);
        assertThat(reader.getProgress()).isEqualTo(0.3f);
        assertThat(reader.cycle.cyclePhase()).isEqualTo(CyclePhase.EXPANSION);
        assertThat(reader.burn.burnTime()).isEqualTo(100);
        assertThat(reader.burn.fuelTime()).isEqualTo(200);
    }
}

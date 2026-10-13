package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.LogisticsForestry;
import com.logistics.core.machine.MachineHudModel;
import com.logistics.test.MinecraftTestEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Farm settings and status text")
class FarmSettingsTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("config defaults")
    void configDefaults() {
        assertThat(LogisticsForestry.CONFIG.FARM_WORK_INTERVAL_TICKS.definition().defaultValue()).isEqualTo(10);
        assertThat(LogisticsForestry.CONFIG.FARM_ENERGY_CAPACITY.definition().defaultValue()).isEqualTo(20_000L);
        assertThat(LogisticsForestry.CONFIG.FARM_MAX_ENERGY_INPUT.definition().defaultValue()).isEqualTo(128L);
        assertThat(LogisticsForestry.CONFIG.FARM_RF_HARVEST_PER_BLOCK.definition().defaultValue()).isEqualTo(40L);
        assertThat(LogisticsForestry.CONFIG.FARM_RF_CLEAR_PER_BLOCK.definition().defaultValue()).isEqualTo(40L);
        assertThat(LogisticsForestry.CONFIG.FARM_RF_PLANT.definition().defaultValue()).isEqualTo(100L);
        assertThat(LogisticsForestry.CONFIG.FARM_RF_TILL.definition().defaultValue()).isEqualTo(60L);
        assertThat(LogisticsForestry.CONFIG.FARM_RF_SOIL.definition().defaultValue()).isEqualTo(60L);
        assertThat(LogisticsForestry.CONFIG.FARM_RF_MOISTEN.definition().defaultValue()).isEqualTo(20L);
        assertThat(LogisticsForestry.CONFIG.FARM_WATER_CAPACITY_MB.definition().defaultValue()).isEqualTo(8_000);
        assertThat(LogisticsForestry.CONFIG.FARM_WATER_PER_TILL_MB.definition().defaultValue()).isEqualTo(250);
        assertThat(LogisticsForestry.CONFIG.FARM_HARVEST_BLOCKS_PER_TICK.definition().defaultValue()).isEqualTo(4);
        assertThat(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_BASE_TICKS.definition().defaultValue()).isEqualTo(20);
        assertThat(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_MAX_TICKS.definition().defaultValue()).isEqualTo(200);
    }

    @Test
    @DisplayName("out-of-range settings are normalised into a workable farm")
    void normalises() {
        FarmSettings settings = new FarmSettings(0, 0, -1, -5, -5, -5, -5, -5, -5, 0, -1, 0, 0, -3);

        assertThat(settings.workIntervalTicks()).isEqualTo(1);
        assertThat(settings.energyCapacity()).isEqualTo(1);
        assertThat(settings.rfPlant()).isZero();
        assertThat(settings.rfClearPerBlock()).isZero();
        assertThat(settings.waterPerTillMb()).isZero();
        assertThat(settings.harvestBlocksPerTick()).isEqualTo(1);
        assertThat(settings.idleBackoffBaseTicks()).isEqualTo(1);
        assertThat(settings.idleBackoffMaxTicks()).isEqualTo(1);
    }

    @Test
    @DisplayName("every status has English text")
    void everyStatusIsTranslated() throws IOException {
        JsonObject lang = englishLang();
        for (FarmStatus status : FarmStatus.values()) {
            assertThat(lang.has(status.translationKey())).as(status.translationKey()).isTrue();
        }
        assertThat(lang.has(FarmComponent.HUD_STATUS)).isTrue();
        assertThat(lang.has(FarmComponent.HUD_PLOT)).isTrue();
        assertThat(lang.has(FarmComponent.HUD_PLOT_SIZE)).isTrue();
    }

    @Test
    @DisplayName("the Jade HUD carries the status and the plot size")
    void hudLines() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic());
        MachineHudModel hud = new MachineHudModel();
        farm.farm().contributeHud(hud);
        CompoundTag data = new CompoundTag();
        hud.save(data);

        assertThat(MachineHudModel.entries(data)).containsExactly(
                new MachineHudModel.LineEntry(FarmComponent.HUD_STATUS, FarmStatus.IDLE.translationKey(), java.util.List.of()),
                new MachineHudModel.LineEntry(FarmComponent.HUD_PLOT, FarmComponent.HUD_PLOT_SIZE, java.util.List.of("3")));
    }

    @Test
    @DisplayName("an unknown synced status reads as idle")
    void byOrdinal() {
        assertThat(FarmStatus.byOrdinal(FarmStatus.OUTPUT_FULL.ordinal())).isEqualTo(FarmStatus.OUTPUT_FULL);
        assertThat(FarmStatus.byOrdinal(-1)).isEqualTo(FarmStatus.IDLE);
        assertThat(FarmStatus.byOrdinal(FarmStatus.values().length)).isEqualTo(FarmStatus.IDLE);
    }

    private static JsonObject englishLang() throws IOException {
        try (InputStream in = FarmSettingsTest.class.getResourceAsStream("/assets/logistics/lang/en_us.json")) {
            assertThat(in).as("en_us.json on the test classpath").isNotNull();
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}

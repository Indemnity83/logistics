package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigKey;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;
import com.logistics.core.machine.MachineHudModel;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.core.machine.tier.TieredBlockItem;
import com.logistics.forestry.farm.crop.CropTendLogic;
import com.logistics.test.MinecraftTestEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Farm settings and status text")
class FarmSettingsTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("config defaults: per-action RF, 64 reads a check")
    void configDefaults() {
        Map<FarmCost, Long> expected = Map.of(
                FarmCost.HARVEST_CROP, 50L,
                FarmCost.HARVEST_LOG, 25L,
                FarmCost.HARVEST_LEAVES, 5L,
                FarmCost.TILL, 25L,
                FarmCost.PLANT, 25L);
        for (FarmCost cost : FarmCost.values()) {
            ConfigKey<Long> key = LogisticsForestry.CONFIG.farmCost(cost);
            assertThat(key.definition().path().fullPath()).as("%s", cost).isEqualTo(cost.configName());
            assertThat(key.definition().defaultValue()).as("%s", cost).isEqualTo(expected.get(cost));
        }
        assertThat(FarmCost.values()).extracting(FarmCost::configName).containsExactly(
                "rf_harvest_crop", "rf_harvest_log", "rf_harvest_leaves", "rf_till", "rf_plant");
        assertThat(LogisticsForestry.CONFIG.FARM_READS_PER_CHECK.definition().defaultValue()).isEqualTo(64);
        assertThat(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_BASE_TICKS.definition().defaultValue()).isEqualTo(20);
        assertThat(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_MAX_TICKS.definition().defaultValue()).isEqualTo(200);
        assertThat(FarmSettings.fromConfig()).isEqualTo(FarmSettings.defaults());
    }

    @Test
    @DisplayName("the dropped keys are gone from the farm config")
    void droppedKeys() {
        Config config = ConfigRegistry.config(LogisticsForestry.CONFIG.FARM_READS_PER_CHECK.configId());
        assertThat(config.definitions())
                .extracting(definition -> definition.path().fullPath())
                .contains("reads_per_check", "tier.basic.checks_per_second")
                .doesNotContain("work_interval_ticks", "harvest_blocks_per_tick", "rf_harvest_per_block",
                        "water_per_till_mb", "rf_moisten", "water_per_moisture_mb", "tilled_moisture",
                        "rf_moisture_per_level", "water_per_irrigation_mb", "tier.basic.water_capacity_mb",
                        "harvest_share", "rf_clear_per_block", "rf_soil", "tier.basic.outer_size");
    }

    @Test
    @DisplayName("a check buys at least 16 reads")
    void readsValidation() {
        Config config = ConfigRegistry.config(LogisticsForestry.CONFIG.FARM_READS_PER_CHECK.configId());

        assertThat(config.trySet(LogisticsForestry.CONFIG.FARM_READS_PER_CHECK, 8)).isFalse();
        assertThat(LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_READS_PER_CHECK)).isEqualTo(64);
    }

    @Test
    @DisplayName("each tier's profile defaults: rates and buffers doubling per tier")
    void tierDefaults() {
        assertThat(FarmSettings.fromConfig().tiers()).containsExactly(
                entry(MachineTier.BASIC, new FarmTierProfile(16, 4, 20_000, 64)),
                entry(MachineTier.INDUSTRIAL, new FarmTierProfile(32, 8, 40_000, 128)),
                entry(MachineTier.PRECISION, new FarmTierProfile(64, 16, 80_000, 256)),
                entry(MachineTier.INFERNAL, new FarmTierProfile(128, 32, 160_000, 512)));
        for (MachineTier tier : MachineTier.values()) {
            assertThat(FarmTierProfile.fromConfig(tier)).as("%s", tier).isEqualTo(FarmTierProfile.defaults(tier));
        }
    }

    @Test
    @DisplayName("each tier's keys sit under tier.<id> in the farm config")
    void tierKeyPaths() {
        LogisticsForestry.CONFIG.FarmTierKeys keys = LogisticsForestry.CONFIG.farmTier(MachineTier.INDUSTRIAL);

        assertThat(keys.checksPerSecond().definition().path().fullPath()).isEqualTo("tier.industrial.checks_per_second");
        assertThat(keys.actionsPerSecond().definition().path().fullPath()).isEqualTo("tier.industrial.actions_per_second");
        assertThat(keys.energyCapacity().definition().path().fullPath()).isEqualTo("tier.industrial.energy_capacity");
        assertThat(keys.maxEnergyInput().definition().path().fullPath()).isEqualTo("tier.industrial.max_energy_input");
    }

    @Test
    @DisplayName("tier rates, buffers and the input limit must be at least 1")
    void bufferValidation() {
        LogisticsForestry.CONFIG.FarmTierKeys keys = LogisticsForestry.CONFIG.FARM_TIER_PRECISION;
        Config config = ConfigRegistry.config(keys.energyCapacity().configId());

        assertThat(config.trySet(keys.energyCapacity(), 0L)).isFalse();
        assertThat(config.trySet(keys.maxEnergyInput(), 0L)).isFalse();
        assertThat(config.trySet(keys.checksPerSecond(), 0)).isFalse();
        assertThat(config.trySet(keys.actionsPerSecond(), 0)).isFalse();
        assertThat(LogisticsConfigHost.get(keys.energyCapacity())).isEqualTo(80_000L);
    }

    @Test
    @DisplayName("out-of-range settings are normalised into a workable farm")
    void normalises() {
        FarmSettings settings = new FarmSettings(Map.of(FarmCost.PLANT, -5L), 0, 0, -3, Map.of());

        assertThat(settings.cost(FarmCost.PLANT)).isZero();
        assertThat(settings.cost(FarmCost.HARVEST_CROP)).as("missing costs take their defaults").isEqualTo(50);
        assertThat(settings.readsPerCheck()).isEqualTo(1);
        assertThat(settings.idleBackoffBaseTicks()).isEqualTo(1);
        assertThat(settings.idleBackoffMaxTicks()).isEqualTo(1);
        assertThat(settings.tiers()).as("missing tiers take their defaults").isEqualTo(FarmSettings.defaultTiers());
    }

    @Test
    @DisplayName("a tier profile is normalised: positive rates and buffers")
    void normalisesProfile() {
        FarmTierProfile profile = new FarmTierProfile(0, -2, 0, -1);

        assertThat(profile.checksPerSecond()).isEqualTo(1);
        assertThat(profile.actionsPerSecond()).isEqualTo(1);
        assertThat(profile.energyCapacity()).isEqualTo(1);
        assertThat(profile.maxEnergyInput()).isZero();
    }

    @Test
    @DisplayName("every status has English text")
    void everyStatusIsTranslated() throws IOException {
        JsonObject lang = englishLang();
        for (FarmStatus status : FarmStatus.values()) {
            assertThat(lang.has(status.translationKey())).as(status.translationKey()).isTrue();
        }
        assertThat(lang.has(TendFarmComponent.HUD_STATUS)).isTrue();
        assertThat(lang.has(TendFarmComponent.HUD_REACH)).isTrue();
        assertThat(lang.has(TendFarmComponent.HUD_REACH_SIZE)).isTrue();
        assertThat(lang.has(FarmScreenText.TITLE)).isTrue();
        assertThat(lang.has(FarmScreenText.TOOLTIP_REACH)).isTrue();
        assertThat(lang.has(FarmScreenText.TOOLTIP_PLOTS)).isTrue();
        for (MachineTier tier : MachineTier.values()) {
            assertThat(lang.has(tier.translationKey())).as(tier.translationKey()).isTrue();
        }
        assertThat(lang.has(TieredBlockItem.TOOLTIP_TIER)).isTrue();
    }

    @Test
    @DisplayName("the Jade HUD carries the status and the Reach's size")
    void hudLines() {
        FarmHarness farm = FarmHarness.tending(new CropTendLogic());
        MachineHudModel hud = new MachineHudModel();
        farm.engine().contributeHud(hud);
        CompoundTag data = new CompoundTag();
        hud.save(data);

        assertThat(MachineHudModel.entries(data)).containsExactly(
                new MachineHudModel.LineEntry(
                        TendFarmComponent.HUD_STATUS, FarmStatus.IDLE.translationKey(), java.util.List.of()),
                new MachineHudModel.LineEntry(
                        TendFarmComponent.HUD_REACH, TendFarmComponent.HUD_REACH_SIZE, java.util.List.of("7")));
    }

    @Test
    @DisplayName("an unknown synced status reads as idle")
    void byOrdinal() {
        assertThat(FarmStatus.byOrdinal(FarmStatus.INVENTORY_FULL.ordinal())).isEqualTo(FarmStatus.INVENTORY_FULL);
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

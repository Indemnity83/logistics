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
    @DisplayName("config defaults: per-action RF, 100 mB per till, an even harvest share, 64 reads a check")
    void configDefaults() {
        Map<FarmCost, Long> expected = Map.of(
                FarmCost.CLEAR, 25L,
                FarmCost.HARVEST_CROP, 50L,
                FarmCost.HARVEST_LOG, 25L,
                FarmCost.HARVEST_LEAVES, 5L,
                FarmCost.TILL, 25L,
                FarmCost.SOIL, 50L,
                FarmCost.PLANT, 25L,
                FarmCost.MOISTEN, 5L);
        for (FarmCost cost : FarmCost.values()) {
            ConfigKey<Long> key = LogisticsForestry.CONFIG.farmCost(cost);
            assertThat(key.definition().path().fullPath()).as("%s", cost).isEqualTo(cost.configName());
            assertThat(key.definition().defaultValue()).as("%s", cost).isEqualTo(expected.get(cost));
        }
        assertThat(FarmCost.values()).extracting(FarmCost::configName).containsExactly(
                "rf_clear_per_block", "rf_harvest_crop", "rf_harvest_log", "rf_harvest_leaves",
                "rf_till", "rf_soil", "rf_plant", "rf_moisten");
        assertThat(LogisticsForestry.CONFIG.FARM_WATER_PER_TILL_MB.definition().defaultValue()).isEqualTo(100);
        assertThat(LogisticsForestry.CONFIG.FARM_HARVEST_SHARE.definition().defaultValue()).isEqualTo(0.5);
        assertThat(LogisticsForestry.CONFIG.FARM_READS_PER_CHECK.definition().defaultValue()).isEqualTo(64);
        assertThat(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_BASE_TICKS.definition().defaultValue()).isEqualTo(20);
        assertThat(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_MAX_TICKS.definition().defaultValue()).isEqualTo(200);
        assertThat(FarmSettings.fromConfig()).isEqualTo(FarmSettings.defaults());
    }

    @Test
    @DisplayName("the dropped keys are gone from the farm config")
    void droppedKeys() {
        Config config = ConfigRegistry.config(LogisticsForestry.CONFIG.FARM_WATER_PER_TILL_MB.configId());
        assertThat(config.definitions())
                .extracting(definition -> definition.path().fullPath())
                .contains("water_per_till_mb", "harvest_share", "reads_per_check", "tier.basic.checks_per_second")
                .doesNotContain("work_interval_ticks", "harvest_blocks_per_tick", "rf_harvest_per_block");
    }

    @Test
    @DisplayName("the harvest share leaves cultivation at least a quarter; a check buys at least 16 reads")
    void shareAndReadsValidation() {
        Config config = ConfigRegistry.config(LogisticsForestry.CONFIG.FARM_HARVEST_SHARE.configId());

        assertThat(config.trySet(LogisticsForestry.CONFIG.FARM_HARVEST_SHARE, 0.8)).isFalse();
        assertThat(config.trySet(LogisticsForestry.CONFIG.FARM_HARVEST_SHARE, -0.1)).isFalse();
        assertThat(config.trySet(LogisticsForestry.CONFIG.FARM_READS_PER_CHECK, 8)).isFalse();
        assertThat(LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_HARVEST_SHARE)).isEqualTo(0.5);
        assertThat(LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_READS_PER_CHECK)).isEqualTo(64);
        assertThat(FarmSettings.defaults().withHarvestShare(0.9).harvestShare()).isEqualTo(FarmSettings.MAX_HARVEST_SHARE);
    }

    @Test
    @DisplayName("each tier's profile defaults: 11/15/19/23 rings, rates, buffers and tanks doubling per tier")
    void tierDefaults() {
        assertThat(FarmSettings.fromConfig().tiers()).containsExactly(
                entry(MachineTier.BASIC, new FarmTierProfile(11, 16, 4, 20_000, 64, 8_000)),
                entry(MachineTier.INDUSTRIAL, new FarmTierProfile(15, 32, 8, 40_000, 128, 16_000)),
                entry(MachineTier.PRECISION, new FarmTierProfile(19, 64, 16, 80_000, 256, 32_000)),
                entry(MachineTier.INFERNAL, new FarmTierProfile(23, 128, 32, 160_000, 512, 64_000)));
        for (MachineTier tier : MachineTier.values()) {
            assertThat(FarmTierProfile.fromConfig(tier)).as("%s", tier).isEqualTo(FarmTierProfile.defaults(tier));
        }
    }

    @Test
    @DisplayName("each tier's keys sit under tier.<id> in the farm config")
    void tierKeyPaths() {
        LogisticsForestry.CONFIG.FarmTierKeys keys = LogisticsForestry.CONFIG.farmTier(MachineTier.INDUSTRIAL);

        assertThat(keys.outerSize().definition().path().fullPath()).isEqualTo("tier.industrial.outer_size");
        assertThat(keys.checksPerSecond().definition().path().fullPath()).isEqualTo("tier.industrial.checks_per_second");
        assertThat(keys.actionsPerSecond().definition().path().fullPath()).isEqualTo("tier.industrial.actions_per_second");
        assertThat(keys.energyCapacity().definition().path().fullPath()).isEqualTo("tier.industrial.energy_capacity");
        assertThat(keys.maxEnergyInput().definition().path().fullPath()).isEqualTo("tier.industrial.max_energy_input");
        assertThat(keys.waterCapacityMb().definition().path().fullPath()).isEqualTo("tier.industrial.water_capacity_mb");
    }

    @Test
    @DisplayName("a tier's outer size must be odd, wider than the courtyard, and no narrower than the tier below")
    void outerSizeValidation() {
        Config config = ConfigRegistry.config(LogisticsForestry.CONFIG.FARM_TIER_BASIC.outerSize().configId());
        ConfigKey<Integer> basic = LogisticsForestry.CONFIG.FARM_TIER_BASIC.outerSize();
        ConfigKey<Integer> industrial = LogisticsForestry.CONFIG.FARM_TIER_INDUSTRIAL.outerSize();
        try {
            assertThat(config.trySet(basic, 12)).as("even").isFalse();
            assertThat(config.trySet(basic, FarmGeometry.COURTYARD)).as("no ring").isFalse();
            assertThat(config.trySet(basic, 5)).as("inside the courtyard").isFalse();
            assertThat(config.trySet(basic, 9)).as("one lane").isTrue();
            assertThat(config.trySet(industrial, 7)).as("narrower than basic").isFalse();
            assertThat(config.trySet(industrial, 9)).as("as wide as basic").isTrue();
            assertThat(config.trySet(industrial, 13)).isTrue();
        } finally {
            config.set(basic, 11);
            config.set(industrial, 15);
        }
        assertThat(LogisticsConfigHost.get(basic)).isEqualTo(11);
        assertThat(LogisticsConfigHost.get(industrial)).isEqualTo(15);
    }

    @Test
    @DisplayName("tier rates, buffers and the input limit must be at least 1")
    void bufferValidation() {
        LogisticsForestry.CONFIG.FarmTierKeys keys = LogisticsForestry.CONFIG.FARM_TIER_PRECISION;
        Config config = ConfigRegistry.config(keys.energyCapacity().configId());

        assertThat(config.trySet(keys.energyCapacity(), 0L)).isFalse();
        assertThat(config.trySet(keys.maxEnergyInput(), 0L)).isFalse();
        assertThat(config.trySet(keys.waterCapacityMb(), 0)).isFalse();
        assertThat(config.trySet(keys.checksPerSecond(), 0)).isFalse();
        assertThat(config.trySet(keys.actionsPerSecond(), 0)).isFalse();
        assertThat(LogisticsConfigHost.get(keys.energyCapacity())).isEqualTo(80_000L);
    }

    @Test
    @DisplayName("out-of-range settings are normalised into a workable farm")
    void normalises() {
        FarmSettings settings = new FarmSettings(Map.of(FarmCost.PLANT, -5L), -1, Double.NaN, 0, 0, -3, Map.of());

        assertThat(settings.cost(FarmCost.PLANT)).isZero();
        assertThat(settings.cost(FarmCost.CLEAR)).as("missing costs take their defaults").isEqualTo(25);
        assertThat(settings.waterPerTillMb()).isZero();
        assertThat(settings.harvestShare()).isEqualTo(0.5);
        assertThat(settings.readsPerCheck()).isEqualTo(1);
        assertThat(settings.idleBackoffBaseTicks()).isEqualTo(1);
        assertThat(settings.idleBackoffMaxTicks()).isEqualTo(1);
        assertThat(settings.tiers()).as("missing tiers take their defaults").isEqualTo(FarmSettings.defaultTiers());
        assertThat(settings.withHarvestShare(-1).harvestShare()).isZero();
    }

    @Test
    @DisplayName("a tier profile is normalised: an odd ring wider than the courtyard, positive rates and buffers")
    void normalisesProfile() {
        FarmTierProfile profile = new FarmTierProfile(4, 0, -2, 0, -1, 0);

        assertThat(profile.outerSize()).isEqualTo(FarmGeometry.COURTYARD + 2);
        assertThat(new FarmTierProfile(14, 1, 1, 1, 1, 1).outerSize()).as("even rounds up").isEqualTo(15);
        assertThat(profile.checksPerSecond()).isEqualTo(1);
        assertThat(profile.actionsPerSecond()).isEqualTo(1);
        assertThat(profile.energyCapacity()).isEqualTo(1);
        assertThat(profile.maxEnergyInput()).isZero();
        assertThat(profile.waterCapacityMb()).isEqualTo(1);
    }

    @Test
    @DisplayName("no tier's ring is narrower than the tier below it")
    void ringsNeverShrinkUpTheLadder() {
        Map<MachineTier, FarmTierProfile> tiers = FarmSettings.defaultTiers();
        tiers.put(MachineTier.PRECISION, new FarmTierProfile(9, 64, 16, 80_000, 256, 32_000));
        FarmSettings settings = FarmSettings.defaults().withTiers(tiers);

        assertThat(settings.profile(MachineTier.PRECISION).outerSize()).isEqualTo(15);
        assertThat(settings.profile(MachineTier.PRECISION).energyCapacity()).isEqualTo(80_000);
        assertThat(settings.profile(MachineTier.INFERNAL).outerSize()).isEqualTo(23);
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
        assertThat(lang.has(FarmComponent.HUD_PLOTS)).isTrue();
        assertThat(lang.has(FarmComponent.HUD_PLOTS_VALUE)).isTrue();
        assertThat(lang.has(FarmScreenText.TITLE)).isTrue();
        assertThat(lang.has(FarmScreenText.TOOLTIP_AREA)).isTrue();
        assertThat(lang.has(FarmScreenText.TOOLTIP_PLOTS)).isTrue();
        for (MachineTier tier : MachineTier.values()) {
            assertThat(lang.has(tier.translationKey())).as(tier.translationKey()).isTrue();
        }
        assertThat(lang.has(TieredBlockItem.TOOLTIP_TIER)).isTrue();
    }

    @Test
    @DisplayName("the Jade HUD carries the status, the plot size and the plot counts")
    void hudLines() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic());
        MachineHudModel hud = new MachineHudModel();
        farm.farm().contributeHud(hud);
        CompoundTag data = new CompoundTag();
        hud.save(data);

        assertThat(MachineHudModel.entries(data)).containsExactly(
                new MachineHudModel.LineEntry(FarmComponent.HUD_STATUS, FarmStatus.IDLE.translationKey(), java.util.List.of()),
                new MachineHudModel.LineEntry(FarmComponent.HUD_PLOT, FarmComponent.HUD_PLOT_SIZE, java.util.List.of("3")),
                new MachineHudModel.LineEntry(
                        FarmComponent.HUD_PLOTS, FarmComponent.HUD_PLOTS_VALUE, java.util.List.of("8", "0")));
    }

    @Test
    @DisplayName("the Jade plot counts subtract the blocked plots from the ring")
    void hudPlotCounts() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic()
                .script((world, plot) -> PlotAction.blocked(PlotAction.Reason.OBSTRUCTED)));
        farm.ticks(400);
        MachineHudModel hud = new MachineHudModel();
        farm.farm().contributeHud(hud);
        CompoundTag data = new CompoundTag();
        hud.save(data);

        assertThat(MachineHudModel.entries(data)).contains(
                new MachineHudModel.LineEntry(
                        FarmComponent.HUD_PLOTS, FarmComponent.HUD_PLOTS_VALUE, java.util.List.of("0", "8")));
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

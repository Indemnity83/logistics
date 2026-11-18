package com.logistics.power.screen;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.power.engine.EngineHudData;
import com.logistics.power.engine.EngineHudLines;
import com.logistics.test.MinecraftTestEnvironment;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A heat stage reads the same in Jade and on the engine screens: its own name. */
@DisplayName("Engine heat stage names")
class EngineStageNameTest extends MinecraftTestEnvironment {

    private static final JsonObject EN_US = load();

    private static JsonObject load() {
        var stream = Objects.requireNonNull(
                EngineStageNameTest.class.getClassLoader().getResourceAsStream("assets/logistics/lang/en_us.json"),
                "en_us.json is not on the test classpath");
        return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /** "WARM" -> "Warm". */
    private static String nameOf(HeatStage stage) {
        String lower = stage.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String english(String key) {
        return EN_US.has(key) ? EN_US.get(key).getAsString() : null;
    }

    /** The English text of every translatable in {@code line}, depth first. */
    private static List<String> translatedParts(Component line) {
        List<String> parts = new ArrayList<>();
        if (line.getContents() instanceof TranslatableContents translatable) {
            parts.add(english(translatable.getKey()));
        }
        for (Component sibling : line.getSiblings()) {
            parts.addAll(translatedParts(sibling));
        }
        return parts;
    }

    @Test
    @DisplayName("Jade's stage row shows the stage's name")
    void jadeStageRowShowsTheStageName() {
        for (HeatStage stage : HeatStage.values()) {
            CompoundTag data = new CompoundTag();
            data.putString(EngineHudData.KEY_STAGE, stage.name());
            data.putBoolean(EngineHudData.KEY_HAS_HEAT, true);
            Component stageRow = EngineHudLines.build(data, true).getFirst();

            assertThat(translatedParts(stageRow)).as("Jade stage row at %s", stage).contains(nameOf(stage));
        }
    }

    @Test
    @DisplayName("the Magmatic Engine screen labels each stage it reaches with that stage's name")
    void magmaticScreenUsesTheStageName() {
        for (HeatStage stage : List.of(HeatStage.COLD, HeatStage.COOL, HeatStage.WARM)) {
            assertThat(english(MagmaticEngineScreen.thermalKey(stage)))
                    .as("Magmatic screen at %s", stage)
                    .isEqualTo(nameOf(stage));
        }
    }
}

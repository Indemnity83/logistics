package com.logistics.power.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

/**
 * Guards against the Jade rendering bug where a line whose <em>root</em> is a no-args translatable is
 * rebuilt by {@code JadeLanguages.toCleanTranslation()} from its key alone, silently dropping appended
 * siblings (the ": " + value). Label+value lines must therefore not have a bare translatable root.
 */
class EngineHudLinesTest extends MinecraftTestEnvironment {

    @Test
    void labelValueLinesDoNotHaveABareTranslatableRoot() {
        CompoundTag data = new CompoundTag();
        data.putString(EngineHudData.KEY_STAGE, "WARM");
        data.putBoolean(EngineHudData.KEY_HAS_HEAT, true);
        data.putDouble(EngineHudData.KEY_TEMP, 123);
        data.putDouble(EngineHudData.KEY_MAX_TEMP, 250);
        data.putLong(EngineHudData.KEY_OUTPUT, 40);
        data.putBoolean(EngineHudData.KEY_RUNNING, true);

        List<Component> lines = EngineHudLines.build(data, true);

        assertThat(lines).isNotEmpty();
        for (Component line : lines) {
            // A line with appended siblings must not be rooted in a no-args translatable, or Jade drops them.
            if (!line.getSiblings().isEmpty() && line.getContents() instanceof TranslatableContents root) {
                assertThat(root.getArgs())
                        .as("line \"%s\" would lose its value in Jade", line.getString())
                        .isNotEmpty();
            }
        }
    }
}

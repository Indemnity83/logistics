package com.logistics.power.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.laserquarry.QuarryHudData;
import com.logistics.automation.laserquarry.QuarryHudLines;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.core.machine.MachineHudData;
import com.logistics.core.machine.MachineHudLines;
import com.logistics.pipe.PipeHudLines;
import com.logistics.power.PowerInfraHudData;
import com.logistics.power.PowerInfraHudLines;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

/**
 * Guards against the Jade rendering bug where a line whose <em>root</em> is a no-args translatable is
 * rebuilt by {@code JadeLanguages.toCleanTranslation()} from its key alone, silently dropping appended
 * siblings (the ": " + value). Every shared HUD builder must therefore keep label+value lines off a bare
 * translatable root, so this covers all of them, not just the engine.
 */
class EngineHudLinesTest extends MinecraftTestEnvironment {

    @Test
    void labelValueLinesDoNotHaveABareTranslatableRoot() {
        CompoundTag engineData = new CompoundTag();
        engineData.putString(EngineHudData.KEY_STAGE, "WARM");
        engineData.putBoolean(EngineHudData.KEY_HAS_HEAT, true);
        engineData.putDouble(EngineHudData.KEY_TEMP, 123);
        engineData.putDouble(EngineHudData.KEY_MAX_TEMP, 250);
        engineData.putLong(EngineHudData.KEY_OUTPUT, 40);
        engineData.putBoolean(EngineHudData.KEY_RUNNING, true);
        assertLabelValueLines(EngineHudLines.build(engineData, true), "engine", "123°C", "40 RF/t");

        CompoundTag quarryData = new CompoundTag();
        quarryData.putString(QuarryHudData.KEY_PHASE, "MINING");
        quarryData.putLong("powerIn", 120);
        assertLabelValueLines(QuarryHudLines.build(quarryData, true), "quarry", "120 RF/t");

        CompoundTag machineData = new CompoundTag();
        machineData.putBoolean(MachineHudData.KEY_PROCESSING, true);
        machineData.putFloat("progress", 0.42f);
        assertLabelValueLines(MachineHudLines.build(machineData), "machine", "42%");

        TravelingItem traveling = new TravelingItem(new ItemStack(Items.STONE), Direction.NORTH, 0.5f);
        List<TravelingItem> items = List.of(traveling, traveling, traveling); // three items in transit
        assertLabelValueLines(PipeHudLines.build(items, false), "pipe", "3");

        CompoundTag sinkData = new CompoundTag();
        sinkData.putString(PowerInfraHudData.KEY_TYPE, PowerInfraHudData.TYPE_CREATIVE_SINK);
        sinkData.putLong(PowerInfraHudData.KEY_DRAIN_RATE, 64);
        sinkData.putLong(PowerInfraHudData.KEY_RECEIVED, 32);
        assertLabelValueLines(PowerInfraHudLines.creativeSink(sinkData), "creative sink", "64 RF/t", "32 RF/t");
    }

    /**
     * Asserts the builder produced lines, none of which would lose their value to Jade's clean-translation
     * pass, and that the given representative values survive in the rendered text.
     */
    private static void assertLabelValueLines(List<Component> lines, String builder, String... expectedValues) {
        assertThat(lines).as("%s HUD lines", builder).isNotEmpty();

        for (Component line : lines) {
            // A line with appended siblings must not be rooted in a no-args translatable, or Jade drops them.
            if (!line.getSiblings().isEmpty() && line.getContents() instanceof TranslatableContents root) {
                assertThat(root.getArgs())
                        .as("%s line \"%s\" would lose its value in Jade", builder, line.getString())
                        .isNotEmpty();
            }
        }

        String rendered = lines.stream().map(Component::getString).reduce("", (a, b) -> a + "\n" + b);
        for (String expected : expectedValues) {
            assertThat(rendered).as("%s HUD preserves value \"%s\"", builder, expected).contains(expected);
        }
    }
}

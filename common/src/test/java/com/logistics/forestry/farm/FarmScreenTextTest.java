package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.machine.tier.MachineTier;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Farm screen text")
class FarmScreenTextTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("the title is the block name with the tier in parentheses")
    void title() {
        Component name = Component.literal("Crop Farm");
        TranslatableContents title = translatable(FarmScreenText.title(name, MachineTier.INDUSTRIAL));

        assertThat(title.getKey()).isEqualTo(FarmScreenText.TITLE);
        assertThat(title.getArgs()[0]).isEqualTo(name);
        assertThat(translatable((Component) title.getArgs()[1]).getKey())
                .isEqualTo(MachineTier.INDUSTRIAL.translationKey());
    }

    @Test
    @DisplayName("the lamp tooltip is the status, then tier and footprint, then plot counts in gray")
    void statusTooltip() {
        List<Component> lines = FarmScreenText.statusTooltip(FarmStatus.BLOCKED, MachineTier.PRECISION, 19, 300, 12);

        assertThat(lines).hasSize(3);
        assertThat(translatable(lines.get(0)).getKey()).isEqualTo(FarmStatus.BLOCKED.translationKey());

        TranslatableContents area = translatable(lines.get(1));
        assertThat(area.getKey()).isEqualTo(FarmScreenText.TOOLTIP_AREA);
        assertThat(translatable((Component) area.getArgs()[0]).getKey())
                .isEqualTo(MachineTier.PRECISION.translationKey());
        assertThat(area.getArgs()[1]).isEqualTo(19);
        assertThat(area.getArgs()[2]).isEqualTo(FarmGeometry.COURTYARD);
        assertThat(lines.get(1).getStyle().getColor()).isEqualTo(Component.empty().withStyle(ChatFormatting.GRAY).getStyle().getColor());

        TranslatableContents plots = translatable(lines.get(2));
        assertThat(plots.getKey()).isEqualTo(FarmScreenText.TOOLTIP_PLOTS);
        assertThat(plots.getArgs()).containsExactly(300, 12);
        assertThat(lines.get(2).getStyle().getColor()).isEqualTo(lines.get(1).getStyle().getColor());
    }

    private static TranslatableContents translatable(Component component) {
        assertThat(component.getContents()).isInstanceOf(TranslatableContents.class);
        return (TranslatableContents) component.getContents();
    }
}

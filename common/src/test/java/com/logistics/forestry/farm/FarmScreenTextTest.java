package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsForestry;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
    @DisplayName("each farm's side tabs are keyed by its block, so each has its own Info text")
    void tabKey() {
        DomainRegistrations.ensureRegistered();

        assertThat(FarmScreenText.tabKey(LogisticsForestry.BLOCK.CROP_FARM.getName())).isEqualTo("crop_farm");
        assertThat(FarmScreenText.tabKey(LogisticsForestry.BLOCK.ARBORETUM.getName())).isEqualTo("arboretum");
        assertThat(FarmScreenText.tabKey(Component.literal("Crop Farm"))).isEqualTo(FarmScreenText.FALLBACK_TAB_KEY);
        assertThat(FarmScreenText.tabKey(Component.translatable("container.chest")))
                .isEqualTo(FarmScreenText.FALLBACK_TAB_KEY);
    }

    @ParameterizedTest
    @EnumSource(value = FarmStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "WAITING_FOR_SEEDS")
    @DisplayName("every status but waiting for seeds reads as its own text, with nothing to fill in")
    void statusLine(FarmStatus status) {
        TranslatableContents line = translatable(FarmScreenText.status(status, 4));

        assertThat(line.getKey()).isEqualTo(status.translationKey());
        assertThat(line.getArgs()).isEmpty();
    }

    @Test
    @DisplayName("waiting for seeds carries the Empty Plot count")
    void waitingForSeedsCountsEmptyPlots() {
        TranslatableContents line = translatable(FarmScreenText.status(FarmStatus.WAITING_FOR_SEEDS, 4));

        assertThat(line.getKey()).isEqualTo(FarmStatus.WAITING_FOR_SEEDS.translationKey());
        assertThat(line.getArgs()).containsExactly(4);
    }

    @Test
    @DisplayName("the lamp tooltip is the status, then the Reach, then the Plot and Empty Plot counts in gray")
    void statusTooltip() {
        List<Component> lines = FarmScreenText.statusTooltip(FarmStatus.WAITING_FOR_SEEDS, 7, 30, 2);

        assertThat(lines).hasSize(3);
        TranslatableContents status = translatable(lines.get(0));
        assertThat(status.getKey()).isEqualTo(FarmStatus.WAITING_FOR_SEEDS.translationKey());
        assertThat(status.getArgs()).containsExactly(2);
        assertThat(lines.get(0).getStyle().getColor()).isNull();

        TranslatableContents reach = translatable(lines.get(1));
        assertThat(reach.getKey()).isEqualTo(FarmScreenText.TOOLTIP_REACH);
        assertThat(reach.getArgs()).containsExactly(7);
        assertThat(lines.get(1).getStyle().getColor())
                .isEqualTo(Component.empty().withStyle(ChatFormatting.GRAY).getStyle().getColor());

        TranslatableContents plots = translatable(lines.get(2));
        assertThat(plots.getKey()).isEqualTo(FarmScreenText.TOOLTIP_PLOTS);
        assertThat(plots.getArgs()).containsExactly(30, 2);
        assertThat(lines.get(2).getStyle().getColor()).isEqualTo(lines.get(1).getStyle().getColor());
    }

    private static TranslatableContents translatable(Component component) {
        assertThat(component.getContents()).isInstanceOf(TranslatableContents.class);
        return (TranslatableContents) component.getContents();
    }
}

package com.logistics.core.lib.client.screen.tab;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.client.screen.tab.tabs.InfoTab;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Every translation key the side-tab framework and its tabs show has an English entry. */
@DisplayName("Side tab translations")
class SideTabTranslationTest {

    @Test
    @DisplayName("every key used by the host, the info tab and each tabbed screen exists in en_us")
    void everyKeyIsTranslated() {
        List<String> keys = new ArrayList<>(List.of(SideTabHost.NO_ROOM_KEY, InfoTab.TITLE_KEY));
        SideTabScreenContractTest.screens().forEach(screen -> keys.add(InfoTab.textKey(screen.key())));

        assertThat(keys).allSatisfy(key -> assertThat(EnglishLang.translations().containsKey(key))
                .as("en_us.json has " + key)
                .isTrue());
    }
}

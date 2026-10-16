package com.logistics.core.lib.client.screen.tab;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** The shipped {@code en_us.json}, and a fake font that measures text as the player would read it. */
final class EnglishLang {
    private static Map<String, String> translations;

    private EnglishLang() {
    }

    static synchronized Map<String, String> translations() {
        if (translations == null) {
            translations = load();
        }
        return translations;
    }

    /** The English text of {@code text}: a translatable component's entry, otherwise its plain string. */
    static String resolve(Component text) {
        if (text.getContents() instanceof TranslatableContents translatable) {
            return translations().getOrDefault(translatable.getKey(), translatable.getKey());
        }
        return text.getString();
    }

    /** {@link TabFixtures.FakeText}'s metrics (6px per character, 9px lines) over the English text. */
    static final class Text implements TabText {
        @Override
        public int width(Component text) {
            return resolve(text).length() * 6;
        }

        @Override
        public int lineHeight() {
            return 9;
        }

        @Override
        public int wrappedLineCount(Component text, int maxWidth) {
            return Math.max(1, (width(text) + maxWidth - 1) / maxWidth);
        }

        @Override
        public Component ellipsize(Component text, int maxWidth) {
            return TabFixtures.ellipsizeAt6px(resolve(text), text, maxWidth);
        }
    }

    private static Map<String, String> load() {
        try (InputStream in = EnglishLang.class.getClassLoader()
                .getResourceAsStream("assets/logistics/lang/en_us.json")) {
            if (in == null) {
                throw new IllegalStateException("assets/logistics/lang/en_us.json is not on the test classpath");
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                map.put(entry.getKey(), entry.getValue().getAsString());
            }
            return Map.copyOf(map);
        } catch (IOException e) {
            throw new UncheckedIOException("could not read en_us.json", e);
        }
    }
}

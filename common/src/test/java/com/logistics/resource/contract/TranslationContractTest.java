package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.logistics.DomainRegistrations;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.test.MinecraftTestEnvironment;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every block and item we register has an English name.
 *
 * <p>A missing key is not an error at any layer: Minecraft renders the untranslated key itself, so
 * the block shows up in the creative menu, in JEI and on the HUD as {@code block.logistics.core.foo}.
 * Nothing warns, and only looking at the thing in game reveals it — which is exactly what registering
 * a block and forgetting the lang entry produces.
 */
@DisplayName("Translation contract")
class TranslationContractTest extends MinecraftTestEnvironment {

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    @Test
    @DisplayName("every registered block has an English name")
    void everyRegisteredBlockIsTranslated() {
        assertTranslated(BuiltInRegistries.BLOCK, Block::getDescriptionId, "blocks");
    }

    @Test
    @DisplayName("every registered item has an English name")
    void everyRegisteredItemIsTranslated() {
        assertTranslated(BuiltInRegistries.ITEM, Item::getDescriptionId, "items");
    }

    /**
     * Sanity guard, not a coverage claim: an unreadable or emptied lang file would otherwise make
     * every check above fail loudly rather than the file itself being named as the problem.
     */
    @Test
    @DisplayName("the shipped translation set has not collapsed")
    void translationSetHasNotCollapsed() {
        assertThat(translations())
            .as("shipped en_us translations; lower this floor deliberately if content was removed")
            .hasSizeGreaterThanOrEqualTo(400);
    }

    private static <T> void assertTranslated(
            Registry<T> registry, java.util.function.Function<T, String> descriptionId, String what) {
        Map<String, String> translations = translations();
        Set<String> untranslated = new TreeSet<>();

        for (Map.Entry<net.minecraft.resources.ResourceKey<T>, T> entry : registry.entrySet()) {
            ResourceId id = ResourceId.wrap(entry.getKey().location());
            if (!ResourceFiles.NAMESPACE.equals(id.getNamespace())) {
                continue;
            }
            String key = descriptionId.apply(entry.getValue());
            String name = translations.get(key);
            if (name == null || name.isBlank()) {
                untranslated.add(id.getPath() + " (" + key + ")");
            }
        }

        assertThat(untranslated)
            .as("registered " + what + " with no en_us entry — these show their raw translation key "
                + "wherever the game names them; add the key to assets/logistics/lang/en_us.json")
            .isEmpty();
    }

    private static Map<String, String> translations() {
        Path file = ResourceFiles.assetRoot().resolve("lang").resolve("en_us.json");
        JsonObject json = ResourceFiles.parse(file);
        Map<String, String> translations = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            translations.put(entry.getKey(), entry.getValue().getAsString());
        }
        return translations;
    }
}

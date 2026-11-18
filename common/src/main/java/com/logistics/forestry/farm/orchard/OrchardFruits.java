package com.logistics.forestry.farm.orchard;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.logistics.LogisticsMod;
import com.logistics.core.lib.platform.ResourceReloadRegistrar;
import com.logistics.core.lib.resource.ResourceId;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * The Orchard's fruit definitions ({@link OrchardFruit}), loaded from every data pack on each
 * resource reload and kept in a stable order: by definition id, namespace then path. A definition
 * that fails to parse is logged and left out.
 */
public final class OrchardFruits {

    private static final String EXTENSION = ".json";

    private static volatile List<OrchardFruit> fruits = List.of();

    private OrchardFruits() {}

    /** Every loaded definition, in load order: the first one a tree's leaves bear wins. */
    public static List<OrchardFruit> all() {
        return fruits;
    }

    /** Replaces the loaded definitions; for the reload listener and tests. */
    public static void set(List<OrchardFruit> loaded) {
        fruits = List.copyOf(loaded);
    }

    /** Registers the server-data reload listener that loads the definitions. */
    public static void registerReloadListener() {
        ResourceReloadRegistrar.INSTANCE.register(LogisticsMod.modId("orchard_fruit"), new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(
                    SharedState sharedState, Executor background, PreparationBarrier barrier, Executor game) {
                return CompletableFuture.supplyAsync(() -> read(sharedState.resourceManager()), background)
                        .thenCompose(barrier::wait)
                        .thenAcceptAsync(OrchardFruits::set, game);
            }
        });
    }

    private static List<OrchardFruit> read(ResourceManager manager) {
        Map<ResourceId, JsonElement> files = new TreeMap<>(ORDER);
        for (var entry : manager.listResources(OrchardFruit.DIRECTORY, path -> path.getPath().endsWith(EXTENSION))
                .entrySet()) {
            ResourceId file = ResourceId.wrap(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                files.put(definitionId(file), JsonParser.parseReader(reader));
            } catch (Exception e) {
                LogisticsMod.LOGGER.error("Could not read orchard fruit {}: {}", file, e.getMessage());
            }
        }
        return parse(files);
    }

    /**
     * Parses {@code files}, keyed by definition id, in the stable load order; the ones that fail are
     * logged and left out.
     */
    public static List<OrchardFruit> parse(Map<ResourceId, JsonElement> files) {
        Map<ResourceId, JsonElement> ordered = new TreeMap<>(ORDER);
        ordered.putAll(files);
        List<OrchardFruit> parsed = new ArrayList<>();
        ordered.forEach((id, json) -> {
            try {
                parsed.add(OrchardFruit.parse(id, json));
            } catch (RuntimeException e) {
                LogisticsMod.LOGGER.error("Skipping orchard fruit {}: {}", id, e.getMessage());
            }
        });
        return List.copyOf(parsed);
    }

    /** Load order: namespace, then path. */
    static final Comparator<ResourceId> ORDER =
            Comparator.comparing(ResourceId::getNamespace).thenComparing(ResourceId::getPath);

    /** {@code ns:logistics/orchard/fruit/apple.json} is the definition {@code ns:apple}. */
    private static ResourceId definitionId(ResourceId file) {
        String path = file.getPath();
        path = path.substring(OrchardFruit.DIRECTORY.length() + 1, path.length() - EXTENSION.length());
        return ResourceId.in(file.getNamespace(), path);
    }
}

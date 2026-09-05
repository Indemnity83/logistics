package com.logistics.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.core.lib.resource.ResourceId;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Verifies shipped server data — loot tables, worldgen, and registry-backed ids — deserializes into
 * the live registries, reading what the game loaded rather than re-parsing the files.
 *
 * <p>Client resources (models, item definitions, blockstates) have no server-side registry and are
 * out of scope. Recipes have their own body, {@link RecipeLoadingGameTestBody}.
 */
public class ServerDataLoadingGameTestBody {

    private static final String NAMESPACE = "logistics";

    // A live enumeration that matches nothing passes without checking anything, which is the one
    // way these tests can be silently worthless. Each check asserts it actually examined a
    // plausible number of entries; lower a floor deliberately if content is genuinely removed.
    private static final int MINIMUM_LOOT_TABLES = 68;
    private static final int MINIMUM_BLOCKS = 60;
    private static final int MINIMUM_FEATURES = 7;
    private static final int MINIMUM_PLACED_FEATURES = 7;
    private static final int MINIMUM_ITEM_TAGS = 18;
    private static final int MINIMUM_BLOCK_TAGS = 10;

    /**
     * Every shipped loot table deserializes into the live loot registry.
     *
     * <p>Walks the datapack stack rather than the classpath so it behaves the same regardless of
     * how each loader's dev environment lays mod resources out on disk.
     */
    public static void allLogisticsLootTablesLoad(GameTestHelper context) {
        MinecraftServer server = context.getLevel().getServer();
        Optional<? extends HolderLookup.RegistryLookup<LootTable>> maybeLookup =
            server.reloadableRegistries().lookup().lookup(Registries.LOOT_TABLE);
        if (maybeLookup.isEmpty()) {
            context.fail("Loot table registry is not available on the server");
            return;
        }
        HolderLookup.RegistryLookup<LootTable> lookup = maybeLookup.get();

        List<String> missing = new ArrayList<>();
        int examined = 0;
        FileToIdConverter converter = FileToIdConverter.json("loot_table");
        for (var fileId : converter.listMatchingResources(server.getResourceManager()).keySet()) {
            if (!NAMESPACE.equals(ResourceId.wrap(fileId).getNamespace())) {
                continue;
            }
            examined++;
            ResourceId id = ResourceId.wrap(converter.fileToId(fileId));
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id.toIdentifier());
            if (lookup.get(key).isEmpty()) {
                missing.add(id.toString());
            }
        }

        if (!missing.isEmpty()) {
            context.fail("Logistics loot tables failed to load: " + String.join(", ", missing));
            return;
        }
        if (examined < MINIMUM_LOOT_TABLES) {
            context.fail(vacuous("loot tables", examined, MINIMUM_LOOT_TABLES));
            return;
        }
        context.succeed();
    }

    /**
     * Every block we register that declares a loot table has that table actually loaded.
     *
     * <p>This is the direction the file-driven checks can't see: a block pointing at a loot table
     * that was never shipped (or that failed to load) drops nothing when broken, and no amount of
     * validating the files that <em>do</em> exist reveals the one that doesn't.
     */
    public static void everyLogisticsBlockLootTableIsLoaded(GameTestHelper context) {
        MinecraftServer server = context.getLevel().getServer();
        Optional<? extends HolderLookup.RegistryLookup<LootTable>> maybeLookup =
            server.reloadableRegistries().lookup().lookup(Registries.LOOT_TABLE);
        if (maybeLookup.isEmpty()) {
            context.fail("Loot table registry is not available on the server");
            return;
        }
        HolderLookup.RegistryLookup<LootTable> lookup = maybeLookup.get();

        List<String> missing = new ArrayList<>();
        int examined = 0;
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceId blockId = ResourceId.wrap(entry.getKey().identifier());
            if (!NAMESPACE.equals(blockId.getNamespace())) {
                continue;
            }
            examined++;
            Block block = entry.getValue();
            // An empty Optional means the block deliberately drops nothing, which is a valid
            // choice; only a declared-but-unloaded table is a defect.
            block.getLootTable().ifPresent(table -> {
                if (lookup.get(table).isEmpty()) {
                    missing.add(blockId + " -> " + ResourceId.wrap(table.identifier()));
                }
            });
        }

        if (!missing.isEmpty()) {
            context.fail("Blocks declaring a loot table that never loaded: " + String.join(", ", missing));
            return;
        }
        if (examined < MINIMUM_BLOCKS) {
            context.fail(vacuous("blocks", examined, MINIMUM_BLOCKS));
            return;
        }
        context.succeed();
    }

    /** Every shipped configured feature deserializes into the live worldgen registry. */
    public static void allLogisticsConfiguredFeaturesLoad(GameTestHelper context) {
        checkWorldgen(
            context, Registries.FEATURE, "worldgen/feature", MINIMUM_FEATURES);
    }

    /** Every shipped placed feature deserializes into the live worldgen registry. */
    public static void allLogisticsPlacedFeaturesLoad(GameTestHelper context) {
        checkWorldgen(context, Registries.PLACED_FEATURE, "worldgen/placed_feature", MINIMUM_PLACED_FEATURES);
    }

    /**
     * Worldgen registries are dynamic — populated from the datapack at world load — so an entry
     * that fails to deserialize is simply absent rather than raising at startup.
     *
     * @param registryKey dynamic registry the shipped files load into
     * @param directory   datapack directory those files live in
     */
    private static <T> void checkWorldgen(
            GameTestHelper context, ResourceKey<Registry<T>> registryKey, String directory, int minimum) {
        MinecraftServer server = context.getLevel().getServer();
        Optional<Registry<T>> maybeRegistry = context.getLevel().registryAccess().lookup(registryKey);
        if (maybeRegistry.isEmpty()) {
            context.fail("Registry is not available on the server: " + directory);
            return;
        }
        Registry<T> registry = maybeRegistry.get();

        List<String> missing = new ArrayList<>();
        int examined = 0;
        FileToIdConverter converter = FileToIdConverter.json(directory);
        for (var fileId : converter.listMatchingResources(server.getResourceManager()).keySet()) {
            if (!NAMESPACE.equals(ResourceId.wrap(fileId).getNamespace())) {
                continue;
            }
            examined++;
            ResourceId id = ResourceId.wrap(converter.fileToId(fileId));
            if (!registry.containsKey(ResourceKey.create(registryKey, id.toIdentifier()))) {
                missing.add(id.toString());
            }
        }

        if (!missing.isEmpty()) {
            context.fail("Logistics " + directory + " entries failed to load: " + String.join(", ", missing));
            return;
        }
        if (examined < minimum) {
            context.fail(vacuous(directory, examined, minimum));
            return;
        }
        context.succeed();
    }

    private static String vacuous(String what, int examined, int minimum) {
        return "Only examined " + examined + " logistics " + what + " (expected at least " + minimum
            + ") — this check passed without inspecting anything meaningful, so the enumeration is "
            + "probably broken rather than the data being clean";
    }

    /** Every {@code logistics:} item a shipped tag lists survives into the loaded tag. */
    public static void allLogisticsItemTagEntriesLoad(GameTestHelper context) {
        checkTagEntries(context, "item", BuiltInRegistries.ITEM, Registries.ITEM, MINIMUM_ITEM_TAGS);
    }

    /** Every {@code logistics:} block a shipped tag lists survives into the loaded tag. */
    public static void allLogisticsBlockTagEntriesLoad(GameTestHelper context) {
        checkTagEntries(context, "block", BuiltInRegistries.BLOCK, Registries.BLOCK, MINIMUM_BLOCK_TAGS);
    }

    /**
     * Asserts each {@code logistics:} id a tag file lists is actually in the tag the game loaded.
     *
     * <p>This is the check a static pass cannot make, and the failure it catches is worse than a
     * single bad entry: one unresolvable id makes Minecraft discard the <em>whole tag</em>
     * ({@code Couldn't load tag … as it is missing following references}). Renaming one block and
     * missing one tag file takes every other entry in that tag down with it — verified by mutation,
     * where a single typo in {@code minecraft:mineable/pickaxe} removed all 25 of our blocks from it.
     * It is logged as an error but does not stop startup, so the game runs on with a dead tag.
     *
     * <p>Two details make it non-vacuous:
     *
     * <ul>
     *   <li>It keys off our <em>entries</em>, not our files. Our tags live under the {@code c} and
     *       {@code minecraft} namespaces, so there is no file-owner mapping to rely on — but a
     *       {@code logistics:} id in any tag is ours by construction.</li>
     *   <li>It reads the whole resource <em>stack</em>. We add entries to six {@code minecraft:} tag
     *       files vanilla also ships; taking only the winning resource would read vanilla's copy and
     *       silently examine nothing for those tags.</li>
     * </ul>
     *
     * <p>Biome tags are out of scope: they belong to a dynamic registry rather than
     * {@code BuiltInRegistries}, and the one we ship lists no {@code logistics:} entries.
     */
    private static <T> void checkTagEntries(
            GameTestHelper context,
            String registryDirectory,
            Registry<T> registry,
            ResourceKey<? extends Registry<T>> registryKey,
            int minimumTags) {
        ResourceManager resources = context.getLevel().getServer().getResourceManager();
        String prefix = "tags/" + registryDirectory;
        String suffix = ".json";

        List<String> failures = new ArrayList<>();
        int examined = 0;

        for (var entry : resources.listResourceStacks(prefix, id -> id.getPath().endsWith(suffix)).entrySet()) {
            ResourceId fileId = ResourceId.wrap(entry.getKey());
            List<String> declared = new ArrayList<>();
            for (Resource resource : entry.getValue()) {
                declared.addAll(logisticsEntriesIn(resource, fileId, failures));
            }
            if (declared.isEmpty()) {
                continue;
            }
            examined++;

            String path = fileId.getPath();
            String tagPath = path.substring(prefix.length() + 1, path.length() - suffix.length());
            TagKey<T> tagKey = TagKey.create(registryKey, ResourceId.in(fileId.getNamespace(), tagPath).toIdentifier());

            for (String declaredId : declared) {
                // tryParse, not parse: a malformed id (uppercase, a space) is exactly the typo this
                // test exists to report, and parse would throw over the top of the useful message.
                ResourceId wanted = ResourceId.tryParse(declaredId);
                if (wanted == null) {
                    failures.add("'" + declaredId + "' in #" + fileId.getNamespace() + ":" + tagPath
                        + " is not a valid id, so no entry can resolve it");
                    continue;
                }
                boolean present = false;
                for (Holder<T> holder : registry.getTagOrEmpty(tagKey)) {
                    if (holder.is(wanted.toIdentifier())) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    failures.add(declaredId + " is listed in #" + fileId.getNamespace() + ":" + tagPath
                        + " but is not in the loaded tag");
                }
            }
        }

        if (!failures.isEmpty()) {
            context.fail("Logistics " + registryDirectory + " tag entries were dropped on load: "
                + String.join(", ", failures));
            return;
        }
        if (examined < minimumTags) {
            context.fail(vacuous(registryDirectory + " tags", examined, minimumTags));
            return;
        }
        context.succeed();
    }

    /**
     * The {@code logistics:} ids one tag file lists, in either the plain or object entry form.
     *
     * <p>This runs over every tag file in the datapack stack — vanilla's, each loader's, and any other
     * mod's — so it must not blame us for their data. A file that never mentions our namespace is
     * skipped before parsing, which both scopes the failures and avoids parsing hundreds of files we
     * have nothing to say about.
     */
    private static List<String> logisticsEntriesIn(Resource resource, ResourceId fileId, List<String> failures) {
        List<String> ids = new ArrayList<>();
        String contents;
        try (BufferedReader reader = resource.openAsReader()) {
            contents = reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException | RuntimeException e) {
            // Unreadable, and we cannot tell whether it was ours; say so without claiming it is.
            failures.add("could not read tag file " + fileId + " from pack " + resource.sourcePackId() + ": " + e);
            return ids;
        }
        if (!contents.contains(NAMESPACE + ":")) {
            return ids;
        }

        try {
            JsonElement values = JsonParser.parseString(contents).getAsJsonObject().get("values");
            if (values == null || !values.isJsonArray()) {
                return ids;
            }
            for (JsonElement value : values.getAsJsonArray()) {
                String id = null;
                if (value.isJsonPrimitive()) {
                    id = value.getAsString();
                } else if (value.isJsonObject() && value.getAsJsonObject().has("id")) {
                    JsonObject object = value.getAsJsonObject();
                    // {"id": …, "required": false} is the whole reason the object form exists: the
                    // entry is allowed to be absent and the tag still builds, so it is not a defect.
                    if (object.has("required") && !object.get("required").getAsBoolean()) {
                        continue;
                    }
                    id = object.get("id").getAsString();
                }
                // A '#' entry is a tag reference, resolved by the static TagContractTest.
                if (id != null && id.startsWith(NAMESPACE + ":")) {
                    ids.add(id);
                }
            }
        } catch (RuntimeException e) {
            failures.add("could not parse tag file " + fileId + " from pack " + resource.sourcePackId() + ": " + e);
        }
        return ids;
    }
}

package com.logistics.gametest;

import com.logistics.core.lib.resource.ResourceId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
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
    private static final int MINIMUM_CONFIGURED_FEATURES = 7;
    private static final int MINIMUM_PLACED_FEATURES = 7;

    /**
     * Every shipped loot table deserializes into the live loot registry.
     *
     * <p>Walks the datapack stack rather than the classpath so it behaves the same regardless of
     * how each loader's dev environment lays mod resources out on disk.
     */
    public static void allLogisticsLootTablesLoad(GameTestHelper context) {
        MinecraftServer server = context.getLevel().getServer();
        Optional<HolderGetter<LootTable>> maybeLookup =
            server.reloadableRegistries().lookup().lookup(Registries.LOOT_TABLE);
        if (maybeLookup.isEmpty()) {
            context.fail("Loot table registry is not available on the server");
            return;
        }
        HolderGetter<LootTable> lookup = maybeLookup.get();

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
        Optional<HolderGetter<LootTable>> maybeLookup =
            server.reloadableRegistries().lookup().lookup(Registries.LOOT_TABLE);
        if (maybeLookup.isEmpty()) {
            context.fail("Loot table registry is not available on the server");
            return;
        }
        HolderGetter<LootTable> lookup = maybeLookup.get();

        List<String> missing = new ArrayList<>();
        int examined = 0;
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceId blockId = ResourceId.wrap(entry.getKey().location());
            if (!NAMESPACE.equals(blockId.getNamespace())) {
                continue;
            }
            examined++;
            Block block = entry.getValue();
            // On 1.21.1 getLootTable() is non-optional and a block that deliberately drops nothing
            // reports BuiltInLootTables.EMPTY; only a declared-but-unloaded table is a defect.
            ResourceKey<LootTable> table = block.getLootTable();
            if (BuiltInLootTables.EMPTY.equals(table)) {
                continue;
            }
            if (lookup.get(table).isEmpty()) {
                missing.add(blockId + " -> " + ResourceId.wrap(table.location()));
            }
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
            context, Registries.CONFIGURED_FEATURE, "worldgen/configured_feature", MINIMUM_CONFIGURED_FEATURES);
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
        Optional<Registry<T>> maybeRegistry = context.getLevel().registryAccess().registry(registryKey);
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
}

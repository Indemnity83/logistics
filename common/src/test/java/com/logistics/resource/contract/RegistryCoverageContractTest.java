package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsCore;
import com.logistics.test.MinecraftTestEnvironment;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ties the registry to the shipped resource tree, in both directions.
 *
 * <p>The other contract tests in this package start from a file and follow its references. That can
 * only find broken links between files that exist — it cannot see a block or item that was
 * registered and never given a resource at all, which is how a registered thing ends up rendering
 * as the missing-texture checkerboard in a world.
 *
 * <p>The reverse direction matters just as much, and guards this test itself: if
 * {@link DomainRegistrations} ever stops driving part of the registration, the resources for the
 * missing part surface here as orphans rather than quietly shrinking what the forward checks cover.
 *
 * <p>Coverage stops at the definition/blockstate file. That file's own references are followed by
 * {@link ItemDefinitionContractTest}, {@link BlockstateContractTest}, and
 * {@link ModelReferenceContractTest}, so registry to definition to model to texture is covered
 * end to end by the package as a whole.
 */
@DisplayName("Registry coverage")
class RegistryCoverageContractTest extends MinecraftTestEnvironment {

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /**
     * Fluids that can be placed in the world get their {@code LiquidBlock} and bucket registered per
     * loader, so common ships the resources but registers neither. Derived from the fluid list
     * itself rather than named here, so adding a placeable fluid does not need a test edit.
     */
    private static Set<String> loaderRegistered() {
        return LogisticsCore.CUSTOM_FLUIDS.stream()
            .filter(LogisticsCore.FluidDef::placeable)
            .map(fluid -> "core/" + fluid.name())
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> registeredPaths(Registry<?> registry) {
        return registry.entrySet().stream()
            .map(entry -> ResourceId.wrap(entry.getKey().identifier()))
            .filter(id -> ResourceFiles.NAMESPACE.equals(id.getNamespace()))
            .map(ResourceId::getPath)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    /** Ids under {@code assets/logistics/<kind>} in the given root, e.g. {@code power/gold_cable}. */
    private static Set<String> idsUnder(Path root) {
        if (!Files.isDirectory(root)) {
            return Set.of();
        }
        return ResourceFiles.jsonFilesUnder(root).stream()
            .map(file -> {
                String relative = root.relativize(file).toString().replace(File.separatorChar, '/');
                return relative.substring(0, relative.length() - ".json".length());
            })
            .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Ids covered for {@code kind}: shipped by common, or shipped by every loader. Cable
     * blockstates are the current example of the second case — common ships none and both loaders
     * ship their own. Requiring <em>every</em> loader means dropping one loader's copy fails here
     * rather than silently shipping a cable with no model on that loader.
     */
    private static Set<String> covered(String kind) {
        Set<String> ids = new TreeSet<>(idsUnder(ResourceFiles.assetRoot().resolve(kind)));

        Set<String> inEveryLoader = null;
        for (String module : ResourceFiles.LOADER_MODULES) {
            Set<String> loaderIds = idsUnder(ResourceFiles.loaderAssetRoot(module).resolve(kind));
            if (inEveryLoader == null) {
                inEveryLoader = new TreeSet<>(loaderIds);
            } else {
                inEveryLoader.retainAll(loaderIds);
            }
        }
        if (inEveryLoader != null) {
            ids.addAll(inEveryLoader);
        }
        return ids;
    }

    // ==================== registry to resource ====================

    @Test
    @DisplayName("every registered item has an item definition")
    void everyRegisteredItemHasAnItemDefinition() {
        Set<String> missing = new TreeSet<>(registeredPaths(BuiltInRegistries.ITEM));
        missing.removeAll(covered("items"));

        assertThat(missing)
            .as("registered items with no item definition — these render as the missing-texture "
                + "checkerboard in game; add assets/logistics/items/<id>.json")
            .isEmpty();
    }

    @Test
    @DisplayName("every registered block has a blockstate")
    void everyRegisteredBlockHasABlockstate() {
        Set<String> missing = new TreeSet<>(registeredPaths(BuiltInRegistries.BLOCK));
        missing.removeAll(covered("blockstates"));

        assertThat(missing)
            .as("registered blocks with no blockstate, in common or in every loader; "
                + "add assets/logistics/blockstates/<id>.json")
            .isEmpty();
    }

    // ==================== resource to registry ====================

    @Test
    @DisplayName("every item definition belongs to a registered item")
    void everyItemDefinitionBelongsToARegisteredItem() {
        Set<String> orphans = new TreeSet<>(idsUnder(ResourceFiles.assetRoot().resolve("items")));
        orphans.removeAll(registeredPaths(BuiltInRegistries.ITEM));
        orphans.removeAll(loaderRegistered());

        assertThat(orphans)
            .as("item definitions with no registered item — either the item was removed and its "
                + "definition left behind, or DomainRegistrations no longer registers it")
            .isEmpty();
    }

    @Test
    @DisplayName("every blockstate belongs to a registered block")
    void everyBlockstateBelongsToARegisteredBlock() {
        Set<String> orphans = new TreeSet<>(idsUnder(ResourceFiles.assetRoot().resolve("blockstates")));
        orphans.removeAll(registeredPaths(BuiltInRegistries.BLOCK));
        orphans.removeAll(loaderRegistered());

        assertThat(orphans)
            .as("blockstates with no registered block — either the block was removed and its "
                + "blockstate left behind, or DomainRegistrations no longer registers it")
            .isEmpty();
    }

    // ==================== guards ====================

    @Test
    @DisplayName("registration actually populated the registries")
    void registrationPopulatedTheRegistries() {
        // Both directions above pass trivially against an empty registry on the forward side, so
        // assert the domains really registered. Floors are a deletion alarm, not a coverage claim.
        assertThat(registeredPaths(BuiltInRegistries.ITEM)).as("registered items").hasSizeGreaterThan(150);
        assertThat(registeredPaths(BuiltInRegistries.BLOCK)).as("registered blocks").hasSizeGreaterThan(60);
    }
}

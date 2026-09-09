package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.test.MinecraftTestEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ties each registered block's harvest properties to the vanilla tool tags we ship for it.
 *
 * <p>The two halves only mean something together: {@code mineable/*} decides which tool is
 * <em>correct</em> and {@code needs_*_tool} which tier, while {@code requiresCorrectToolForDrops}
 * on the block decides whether being correct matters at all. A block can therefore be silently
 * wrong in either direction — mined at hand speed forever, or destroyed with no drop by the tool
 * a player reaches for first — without any single file looking wrong on its own.
 */
@DisplayName("Tool harvest contract")
class ToolHarvestContractTest extends MinecraftTestEnvironment {

    /**
     * Above this hardness a block is slow enough by hand that punching it out for free reads as an
     * oversight rather than a choice. Sits above our softest free-to-punch block (quartz crystal at
     * 0.8) and below our softest tool-gated one (the stirling engine at 1.5).
     */
    private static final float BARE_HAND_HARVEST_LIMIT = 1.0f;

    /** Blocks no tool tag can help, each with the reason it is exempt. */
    private static final Map<String, String> NOT_TOOL_HARVESTED = Map.of(
        "logistics:core/marker",
            "strength 0.0 — breaks instantly, so no tool could speed it up",
        "logistics:automation/laser_quarry_frame",
            "placed and torn down by its quarry and ships no loot table, so it never drops");

    /** Blocks unobtainable in survival, so a survival harvest rule says nothing about them. */
    private static final Map<String, String> CREATIVE_ONLY = Map.of(
        "logistics:power/creative_engine", "creative-only: no crafting recipe",
        "logistics:power/creative_sink", "creative-only: no crafting recipe");

    /**
     * Tool-gated blocks a pickaxe is deliberately allowed to destroy, each with its reason. Empty,
     * and meant to stay that way — an entry here is a block that silently voids itself for anyone
     * who reaches for the obvious tool, so adding one is a product decision, not a formality.
     */
    private static final Map<String, String> PICKAXE_MAY_DESTROY = Map.of();

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    @Test
    @DisplayName("every block we ship is mineable with some tool")
    void everyBlockIsMineableWithSomeTool() {
        Set<String> mineable = idsInTagDirectory("mineable");
        Set<String> missing = new TreeSet<>(ourBlocks().keySet());
        missing.removeAll(mineable);
        missing.removeAll(NOT_TOOL_HARVESTED.keySet());

        assertThat(missing)
            .as("blocks in no minecraft:mineable/* tag — these are mined at bare-hand speed no "
                + "matter what the player is holding; add each to the tag for its tool, or to "
                + "NOT_TOOL_HARVESTED with the reason no tool applies")
            .isEmpty();
    }

    @Test
    @DisplayName("a block slow to break by hand is not free to break by hand")
    void everySlowBlockRequiresItsTool() {
        List<String> failures = new ArrayList<>();

        ourBlocks().forEach((id, block) -> {
            if (CREATIVE_ONLY.containsKey(id) || NOT_TOOL_HARVESTED.containsKey(id)) {
                return;
            }
            if (block.defaultDestroyTime() <= BARE_HAND_HARVEST_LIMIT) {
                return;
            }
            if (!block.defaultBlockState().requiresCorrectToolForDrops()) {
                failures.add(id + " (strength " + block.defaultDestroyTime()
                    + ") drops itself to bare hands");
            }
        });

        assertThat(failures)
            .as("blocks harder than " + BARE_HAND_HARVEST_LIMIT + " that still drop to bare hands; "
                + "add requiresCorrectToolForDrops() so their tool tags mean something, or record "
                + "why the block is exempt")
            .isEmpty();
    }

    @Test
    @DisplayName("every tier-gated block actually requires its tool")
    void everyTierGatedBlockRequiresItsTool() {
        Map<String, Block> blocks = ourBlocks();
        List<String> failures = new ArrayList<>();

        for (String id : idsInNeedsToolTags()) {
            Block block = blocks.get(id);
            if (block == null) {
                failures.add(id + " is tier-gated but is not a block we register");
            } else if (!block.defaultBlockState().requiresCorrectToolForDrops()) {
                failures.add(id + " is tier-gated but does not require the correct tool, so the "
                    + "tier gate never applies");
            }
        }

        assertThat(failures).as("inert needs_*_tool entries").isEmpty();
    }

    @Test
    @DisplayName("every block that requires a tool has one it accepts")
    void everyToolGatedBlockHasAMineableTag() {
        Set<String> mineable = idsInTagDirectory("mineable");
        List<String> failures = new ArrayList<>();

        ourBlocks().forEach((id, block) -> {
            if (block.defaultBlockState().requiresCorrectToolForDrops() && !mineable.contains(id)) {
                failures.add(id + " requires the correct tool but is in no mineable/* tag, so no "
                    + "tool is correct and it can never be harvested");
            }
        });

        assertThat(failures).as("blocks that cannot be harvested by any tool").isEmpty();
    }

    /**
     * The stricter half of the rule above. Being harvestable by <em>some</em> tool is not enough:
     * everything tool-gated we ship is machinery, ore or metal, so the pickaxe is what a player
     * actually swings at it. A tool-gated block outside {@code mineable/pickaxe} is destroyed for
     * no drop by that reflex — the failure mode is silent, and costs the player the block.
     */
    @Test
    @DisplayName("no tool-gated block is destroyed by a pickaxe")
    void everyToolGatedBlockAcceptsAPickaxe() {
        Set<String> pickaxeMineable = idsInTag("mineable/pickaxe");
        List<String> failures = new ArrayList<>();

        ourBlocks().forEach((id, block) -> {
            if (!block.defaultBlockState().requiresCorrectToolForDrops()
                || pickaxeMineable.contains(id)
                || PICKAXE_MAY_DESTROY.containsKey(id)) {
                return;
            }
            failures.add(id + " requires the correct tool but is not in mineable/pickaxe, so a "
                + "pickaxe breaks it with no drop");
        });

        assertThat(failures)
            .as("blocks a pickaxe destroys instead of harvesting; add each to mineable/pickaxe "
                + "(keeping any other mineable tag it has), or to PICKAXE_MAY_DESTROY with the "
                + "reason losing the block to a pickaxe is intended")
            .isEmpty();
    }

    /** Every block the domains register, keyed by its full id. */
    private static Map<String, Block> ourBlocks() {
        Map<String, Block> blocks = new TreeMap<>();
        BuiltInRegistries.BLOCK.entrySet().forEach(entry -> {
            ResourceId id = ResourceId.wrap(entry.getKey().identifier());
            if (ResourceFiles.NAMESPACE.equals(id.getNamespace())) {
                blocks.put(ResourceFiles.NAMESPACE + ":" + id.getPath(), entry.getValue());
            }
        });
        return blocks;
    }

    /** Our block ids in one tag, named as {@code mineable/pickaxe} under {@code minecraft/tags/block}. */
    private static Set<String> idsInTag(String tag) {
        Path file = ResourceFiles.dataRoot().resolve("minecraft/tags/block").resolve(tag + ".json");
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("expected a shipped block tag at " + file);
        }
        return ourValuesIn(List.of(file));
    }

    /** Our block ids across every tag under {@code minecraft/tags/block/<directory>}. */
    private static Set<String> idsInTagDirectory(String directory) {
        Path root = ResourceFiles.dataRoot().resolve("minecraft/tags/block").resolve(directory);
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("expected shipped block tags at " + root);
        }
        return ourValuesIn(ResourceFiles.jsonFilesUnder(root));
    }

    /** Our block ids across every {@code needs_<tier>_tool} tag we ship. */
    private static Set<String> idsInNeedsToolTags() {
        Path root = ResourceFiles.dataRoot().resolve("minecraft/tags/block");
        List<Path> files = ResourceFiles.jsonFilesUnder(root).stream()
            .filter(file -> root.equals(file.getParent()))
            .filter(file -> {
                String name = file.getFileName().toString();
                return name.startsWith("needs_") && name.endsWith("_tool.json");
            })
            .toList();
        if (files.isEmpty()) {
            throw new IllegalStateException("expected needs_*_tool tags under " + root);
        }
        return ourValuesIn(files);
    }

    private static Set<String> ourValuesIn(List<Path> files) {
        Set<String> ids = new LinkedHashSet<>();
        for (Path file : files) {
            var values = ResourceFiles.parse(file).getAsJsonArray("values");
            if (values == null) {
                continue;
            }
            values.forEach(entry -> {
                String value = entry.isJsonObject()
                    ? entry.getAsJsonObject().get("id").getAsString()
                    : entry.getAsString();
                if (value.startsWith(ResourceFiles.NAMESPACE + ":")) {
                    ids.add(value);
                }
            });
        }
        return ids;
    }
}

package com.logistics.forestry.humus;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Humus tree shape")
class HumusTreeShapeTest extends MinecraftTestEnvironment {

    /** Seeds grown per definition: enough to see every trunk height and decoration roll. */
    private static final int SEEDS = 200;

    private static HolderLookup.RegistryLookup<Feature> features;

    @BeforeAll
    static void loadVanillaFeatures() throws ReflectiveOperationException {
        bootstrapMinecraft();
        features = VanillaRegistries.createWorldLookup().lookupOrThrow(Registries.FEATURE);
        bindVanillaBlockTags();
    }

    /**
     * Tree placement asks block tags, which only a data pack load binds. Binds vanilla's block tags
     * onto each block's holder; the test registries are never frozen, which is where vanilla binds them.
     */
    private static void bindVanillaBlockTags() throws ReflectiveOperationException {
        Map<Holder.Reference<Block>, List<TagKey<Block>>> tagsByBlock = new HashMap<>();
        BuiltInRegistries.BLOCK.listElements().forEach(holder -> tagsByBlock.put(holder, new ArrayList<>()));
        try (MultiPackResourceManager resources = new MultiPackResourceManager(
                PackType.SERVER_DATA, List.of(ServerPacksSource.createVanillaPackSource().fullResources()))) {
            TagLoader<Holder<Block>> loader = new TagLoader<>(
                    (id, required) -> BuiltInRegistries.BLOCK.get(id), Registries.tagsDirPath(Registries.BLOCK));
            loader.build(loader.load(resources)).forEach((id, contents) -> {
                TagKey<Block> tag = TagKey.create(Registries.BLOCK, id);
                contents.forEach(holder -> tagsByBlock.get((Holder.Reference<Block>) holder).add(tag));
            });
        }
        Method bindTags = Holder.Reference.class.getDeclaredMethod("bindTags", Collection.class);
        bindTags.setAccessible(true);
        for (Map.Entry<Holder.Reference<Block>, List<TagKey<Block>>> entry : tagsByBlock.entrySet()) {
            bindTags.invoke(entry.getKey(), entry.getValue());
        }
    }

    private static Feature definition(ResourceKey<Feature> key) {
        return features.getOrThrow(key).value();
    }

    private static TreeFeature reshape(ResourceKey<Feature> key) {
        return HumusTreeShape.of(definition(key)).orElseThrow();
    }

    /** A sapling's tree definition and the wood and leaves its Humus tree must be made of. */
    record Species(String name, ResourceKey<Feature> definition, Block log, Set<Block> leaves) {
        Species(String name, ResourceKey<Feature> definition, Block log, Block leaves) {
            this(name, definition, log, Set.of(leaves));
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Species> species() {
        return Stream.of(
                new Species("oak", TreeFeatures.OAK, Blocks.OAK_LOG, Blocks.OAK_LEAVES),
                new Species("spruce", TreeFeatures.SPRUCE, Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES),
                new Species("cherry", TreeFeatures.CHERRY, Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES),
                new Species("mangrove", TreeFeatures.MANGROVE, Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES),
                new Species("azalea", TreeFeatures.AZALEA_TREE, Blocks.OAK_LOG,
                        Set.of(Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES)),
                new Species("dark oak (2x2)", TreeFeatures.DARK_OAK, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES),
                new Species("pale oak (2x2)", TreeFeatures.PALE_OAK_BONEMEAL, Blocks.PALE_OAK_LOG,
                        Blocks.PALE_OAK_LEAVES));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("species")
    @DisplayName("grows a straight 4-6 log trunk under a radius-2 blob canopy, leaving the ground untouched")
    void shape(Species species) {
        TreeFeature tree = reshape(species.definition());
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            Grown grown = Grown.of(tree, seed);
            int height = grown.trunkHeight();
            minHeight = Math.min(minHeight, height);
            maxHeight = Math.max(maxHeight, height);
            assertThat(grown.logsOffTrunk()).as("logs outside the straight trunk").isEmpty();
            assertThat(grown.canopyRadius()).isEqualTo(2);
            assertThat(grown.lowestLeafY()).isGreaterThan(0);
            assertThat(grown.placed().keySet())
                    .as("blocks set in the ground")
                    .noneMatch(pos -> pos.getY() < 0);
        }
        assertThat(minHeight).isEqualTo(4);
        assertThat(maxHeight).isEqualTo(6);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("species")
    @DisplayName("takes its wood and leaves from the sapling's own tree definition")
    void blocks(Species species) {
        TreeFeature tree = reshape(species.definition());
        Set<Block> logs = new HashSet<>();
        Set<Block> leaves = new HashSet<>();
        for (long seed = 0; seed < SEEDS; seed++) {
            Grown grown = Grown.of(tree, seed);
            grown.matching(state -> state.is(BlockTags.LOGS)).values().forEach(state -> logs.add(state.getBlock()));
            grown.matching(state -> state.is(BlockTags.LEAVES)).values().forEach(state -> leaves.add(state.getBlock()));
        }
        assertThat(logs).containsExactly(species.log());
        assertThat(leaves).containsExactlyInAnyOrderElementsOf(species.leaves());
    }

    /** Tree definitions with decorations that always or often fire, and roots. */
    static Stream<ResourceKey<Feature>> decorated() {
        return Stream.of(
                TreeFeatures.FANCY_OAK_BEES,
                TreeFeatures.OAK_BEES_0002_LEAF_LITTER,
                TreeFeatures.JUNGLE_TREE,
                TreeFeatures.MEGA_SPRUCE,
                TreeFeatures.PALE_OAK_CREAKING,
                TreeFeatures.MANGROVE,
                TreeFeatures.TALL_MANGROVE);
    }

    @ParameterizedTest
    @MethodSource("decorated")
    @DisplayName("drops roots and every decoration but hanging germlings")
    void onlyGermlingDecorations(ResourceKey<Feature> definition) {
        TreeFeature tree = reshape(definition);
        for (long seed = 0; seed < SEEDS; seed++) {
            Grown grown = Grown.of(tree, seed);
            assertThat(grown.matching(state -> !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)
                            && !state.is(Blocks.MANGROVE_PROPAGULE)))
                    .as("blocks other than the tree's own wood, leaves and hanging propagules")
                    .isEmpty();
            assertThat(grown.placed().keySet()).as("blocks set in the ground").noneMatch(pos -> pos.getY() < 0);
        }
    }

    @Test
    @DisplayName("hangs mangrove propagules from the leaves fully grown")
    void mangrovePropagules() {
        TreeFeature tree = reshape(TreeFeatures.MANGROVE);
        int propagules = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            Grown grown = Grown.of(tree, seed);
            for (Map.Entry<BlockPos, BlockState> hanging :
                    grown.matching(state -> state.is(Blocks.MANGROVE_PROPAGULE)).entrySet()) {
                propagules++;
                assertThat(hanging.getValue().getValue(MangrovePropaguleBlock.HANGING)).isTrue();
                assertThat(hanging.getValue().getValue(MangrovePropaguleBlock.AGE)).isEqualTo(4);
                assertThat(grown.placed().get(hanging.getKey().above()))
                        .as("block above a propagule")
                        .matches(state -> state != null && state.is(Blocks.MANGROVE_LEAVES));
            }
        }
        assertThat(propagules).as("propagules hung over " + SEEDS + " trees").isPositive();
    }

    @Test
    @DisplayName("grows no tree without a tree definition")
    void noDefinition() {
        assertThat(HumusTreeShape.of(null)).isEmpty();
        assertThat(HumusTreeShape.of(definition(TreeFeatures.HUGE_RED_MUSHROOM))).isEmpty();
    }

    /** A tree placed at the origin over grass at y < 0, in a fake world that records every block set. */
    record Grown(Map<BlockPos, BlockState> placed) {

        static final BlockState GROUND = Blocks.GRASS_BLOCK.defaultBlockState();

        static Grown of(TreeFeature tree, long seed) {
            Map<BlockPos, BlockState> placed = new HashMap<>();
            WorldGenLevel level = fakeLevel(placed);
            boolean grew = tree.place(level, null, RandomSource.create(seed), BlockPos.ZERO);
            assertThat(grew).as("tree placed with seed " + seed).isTrue();
            return new Grown(placed);
        }

        int trunkHeight() {
            int height = 0;
            while (isLog(placed.get(BlockPos.ZERO.above(height)))) {
                height++;
            }
            return height;
        }

        Map<BlockPos, BlockState> logsOffTrunk() {
            return placed.entrySet().stream()
                    .filter(e -> isLog(e.getValue()))
                    .filter(e -> e.getKey().getX() != 0 || e.getKey().getZ() != 0 || e.getKey().getY() < 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        Map<BlockPos, BlockState> matching(Predicate<BlockState> predicate) {
            return placed.entrySet().stream()
                    .filter(e -> predicate.test(e.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        int canopyRadius() {
            return matching(state -> state.is(BlockTags.LEAVES)).keySet().stream()
                    .mapToInt(pos -> Math.max(Math.abs(pos.getX()), Math.abs(pos.getZ())))
                    .max()
                    .orElse(-1);
        }

        int lowestLeafY() {
            return matching(state -> state.is(BlockTags.LEAVES)).keySet().stream()
                    .mapToInt(BlockPos::getY)
                    .min()
                    .orElse(Integer.MAX_VALUE);
        }

        private static boolean isLog(BlockState state) {
            return state != null && state.is(BlockTags.LOGS);
        }

        private static WorldGenLevel fakeLevel(Map<BlockPos, BlockState> placed) {
            InvocationHandler handler = (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getBlockState":
                        return stateAt(placed, (BlockPos) args[0]);
                    case "getFluidState":
                        return stateAt(placed, (BlockPos) args[0]).getFluidState();
                    case "isStateAtPosition":
                        @SuppressWarnings("unchecked")
                        Predicate<BlockState> predicate = (Predicate<BlockState>) args[1];
                        return predicate.test(stateAt(placed, (BlockPos) args[0]));
                    case "isFluidAtPosition":
                        @SuppressWarnings("unchecked")
                        Predicate<FluidState> fluidPredicate = (Predicate<FluidState>) args[1];
                        return fluidPredicate.test(stateAt(placed, (BlockPos) args[0]).getFluidState());
                    case "setBlock":
                        placed.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                        return true;
                    case "scheduleTick":
                        return null;
                    case "getRandom":
                        return RandomSource.create(0L);
                    case "getMinY":
                        return -64;
                    case "getMaxY":
                        return 319;
                    case "getHeight":
                        return 384;
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    case "toString":
                        return "FakeWorldGenLevel";
                    default:
                        if (method.isDefault()) {
                            return InvocationHandler.invokeDefault(proxy, method, args);
                        }
                        throw new UnsupportedOperationException("Fake level does not support " + method);
                }
            };
            return (WorldGenLevel) Proxy.newProxyInstance(
                    WorldGenLevel.class.getClassLoader(), new Class<?>[] {WorldGenLevel.class}, handler);
        }

        private static BlockState stateAt(Map<BlockPos, BlockState> placed, BlockPos pos) {
            BlockState state = placed.get(pos);
            if (state != null) {
                return state;
            }
            return pos.getY() < 0 ? GROUND : Blocks.AIR.defaultBlockState();
        }
    }
}

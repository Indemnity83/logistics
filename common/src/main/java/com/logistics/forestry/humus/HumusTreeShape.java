package com.logistics.forestry.humus;

import com.google.gson.JsonObject;
import com.logistics.forestry.logic.GrowthAge;
import com.mojang.serialization.JsonOps;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.treedecorators.AttachedToLeavesDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import org.jetbrains.annotations.Nullable;

/**
 * The one small tree every sapling on Humus grows: vanilla oak's straight 4–6 log trunk and radius-2
 * blob canopy, in the wood and leaves of the sapling's own tree definition. Roots and decorations
 * are dropped, except decorations that hang germlings from the leaves, whose germlings are placed
 * fully grown and need only one empty block under a leaf. Humus is in {@code #minecraft:dirt}, which a
 * trunk leaves as it is, so the Humus stays Humus.
 */
public final class HumusTreeShape {

    private HumusTreeShape() {}

    /** The small tree for {@code definition}, or empty when it is not a tree definition. */
    public static Optional<ConfiguredFeature<?, ?>> of(@Nullable ConfiguredFeature<?, ?> definition) {
        if (definition == null || !(definition.config() instanceof TreeConfiguration tree)) {
            return Optional.empty();
        }
        List<TreeDecorator> decorators = tree.decorators.stream()
                .filter(AttachedToLeavesDecorator.class::isInstance)
                .map(decorator -> withOneEmptyBlockBelow((AttachedToLeavesDecorator) decorator))
                .<TreeDecorator>map(HangingGermlings::new)
                .toList();
        return Optional.of(new ConfiguredFeature<>(
                Feature.TREE,
                new TreeConfiguration.TreeConfigurationBuilder(
                                tree.trunkProvider,
                                new StraightTrunkPlacer(4, 2, 0),
                                tree.foliageProvider,
                                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                                new TwoLayersFeatureSize(1, 0, 1))
                        .decorators(decorators)
                        .ignoreVines()
                        .build()));
    }

    /**
     * {@code hanging} needing only one empty block under a leaf, so the lowest leaves of the small
     * tree can bear germlings too. Its fields are protected, so it is rebuilt through its codec.
     */
    private static AttachedToLeavesDecorator withOneEmptyBlockBelow(AttachedToLeavesDecorator hanging) {
        JsonObject encoded = AttachedToLeavesDecorator.CODEC
                .codec()
                .encodeStart(JsonOps.INSTANCE, hanging)
                .getOrThrow()
                .getAsJsonObject();
        encoded.addProperty("required_empty_blocks", 1);
        return AttachedToLeavesDecorator.CODEC
                .codec()
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
    }

    /** {@code state} with its {@code age} at its maximum. */
    static BlockState fullyGrown(BlockState state) {
        IntegerProperty age = GrowthAge.of(state);
        return age != null ? state.setValue(age, GrowthAge.max(age)) : state;
    }

    /**
     * Runs a leaf-hanging decorator, keeping only the germlings it hangs, each fully grown. The tree
     * it belongs to is built per growth and never serialized, so {@link #type()} is only nominal.
     */
    private static final class HangingGermlings extends TreeDecorator {
        private final TreeDecorator hanging;

        HangingGermlings(TreeDecorator hanging) {
            this.hanging = hanging;
        }

        @Override
        protected TreeDecoratorType<?> type() {
            return TreeDecoratorType.ATTACHED_TO_LEAVES;
        }

        @Override
        public void place(Context context) {
            hanging.place(new Context(
                    context.level(),
                    (pos, state) -> {
                        if (state.is(HumusTrees.SAPLINGS)) {
                            context.setBlock(pos, fullyGrown(state));
                        }
                    },
                    context.random(),
                    new HashSet<>(context.logs()),
                    new HashSet<>(context.leaves()),
                    new HashSet<>(context.roots())));
        }
    }
}

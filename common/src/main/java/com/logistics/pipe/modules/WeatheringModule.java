package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.IModuleHost;
import com.logistics.core.lib.pipe.ModularPipe;
import com.logistics.core.lib.pipe.ModularPipeBlock;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeFamily;
import com.logistics.core.lib.pipe.RandomTickModule;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.data.PipeDataComponents.WeatheringState;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

/**
 * Handles copper pipe oxidation and waxing behavior similar to vanilla copper blocks.
 * Oxidation progresses through 4 stages: unaffected → exposed → weathered → oxidized.
 * Waxing with honeycomb prevents further oxidation.
 * Scraping with an axe removes wax or reverses oxidation by one stage.
 */
public class WeatheringModule implements Module, RandomTickModule {
    private static final String OXIDATION_KEY = "oxidation_stage";
    private static final String WAXED_KEY = "waxed";

    /** Resolves a model base name to a full {@link ResourceId} in the owning domain's asset namespace. */
    private final Function<String, ResourceId> modelResolver;

    /** Asset base name for this pipe's weathering models, e.g. {@code "copper_transport_pipe"}. */
    private final String modelBase;

    /**
     * Whether arms have a distinct {@code _arm_extended} model for inventory/handler connections. Item pipes
     * do (long arm into the inventory); fluid pipes render the same short arm regardless, so they pass
     * {@code false} and always use the {@code _arm} model.
     */
    private final boolean extendedArms;

    /** Item copper transport pipe defaults (existing behavior). */
    public WeatheringModule() {
        this(LogisticsPipe::model, "copper_transport_pipe", true);
    }

    /** Domain-neutral constructor (extended arms enabled): supply the model resolver and asset base. */
    public WeatheringModule(Function<String, ResourceId> modelResolver, String modelBase) {
        this(modelResolver, modelBase, true);
    }

    /** Domain-neutral constructor: supply the model resolver, asset base, and whether arms have an extended variant. */
    public WeatheringModule(Function<String, ResourceId> modelResolver, String modelBase, boolean extendedArms) {
        this.modelResolver = modelResolver;
        this.modelBase = modelBase;
        this.extendedArms = extendedArms;
    }

    public static final int STAGE_UNAFFECTED = 0;
    public static final int STAGE_EXPOSED = 1;
    public static final int STAGE_WEATHERED = 2;
    public static final int STAGE_OXIDIZED = 3;

    private static final String[] STAGE_SUFFIXES = {"", "_exposed", "_weathered", "_oxidized"};

    /**
     * Returns the model suffix for the given oxidation stage, clamping to valid range.
     * This prevents ArrayIndexOutOfBoundsException from malformed component data.
     */
    private static String getStageSuffix(int stage) {
        int clampedStage = Math.max(STAGE_UNAFFECTED, Math.min(stage, STAGE_OXIDIZED));
        return STAGE_SUFFIXES[clampedStage];
    }

    public int getOxidationStage(PipeContext ctx) {
        return ctx.getInt(this, OXIDATION_KEY, STAGE_UNAFFECTED);
    }

    public boolean isWaxed(PipeContext ctx) {
        return ctx.getInt(this, WAXED_KEY, 0) == 1;
    }

    @Override
    public void randomTick(PipeContext ctx, RandomSource rand) {
        if (ctx.world().isClientSide()) {
            return;
        }
        tryOxidize(ctx, rand);
    }

    public void tryOxidize(PipeContext ctx, RandomSource rand) {
        if (isWaxed(ctx)) return;

        int stage = getOxidationStage(ctx);
        if (stage >= STAGE_OXIDIZED) return;

        // Random-tick gate: vanilla copper's 1125/64 odds.
        if (rand.nextInt(1125) >= 64) return;

        // Only consider neighbors of this pipe's own family (item pipes never influence fluid pipes).
        PipeFamily selfFamily =
                (ctx.state().getBlock() instanceof ModularPipeBlock self) ? self.family() : null;

        // Scan same-family weathering pipes within Manhattan distance 4, mirroring vanilla copper:
        // progression scales with how many neighbors are further along, and aborts entirely if any
        // neighbor is less oxidized than this pipe.
        int weatheringNeighbors = 0;
        int moreOxidizedNeighbors = 0;
        BlockPos origin = ctx.pos();

        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-4, -4, -4), origin.offset(4, 4, 4))) {
            if (p.equals(origin)) continue;
            if (origin.distManhattan(p) > 4) continue;

            if (!(ctx.world().getBlockState(p).getBlock() instanceof ModularPipeBlock pipeBlock)) continue;
            if (pipeBlock.family() != selfFamily) continue;
            if (!(ctx.world().getBlockEntity(p) instanceof IModuleHost be)) continue;

            ModularPipe pipe = pipeBlock.modularPipe();
            if (pipe == null || pipe.getModule(WeatheringModule.class, be) == null) continue;

            PipeContext neighbor = new PipeContext(ctx.world(), p, ctx.world().getBlockState(p), be);
            if (isWaxed(neighbor)) continue;

            int neighborStage = getOxidationStage(neighbor);
            if (neighborStage < stage) return;

            weatheringNeighbors++;
            if (neighborStage > stage) moreOxidizedNeighbors++;
        }

        double chance = oxidationChance(stage, weatheringNeighbors, moreOxidizedNeighbors);
        if (rand.nextDouble() < chance) {
            ctx.saveInt(this, OXIDATION_KEY, stage + 1);
            ctx.markDirtyAndSync();
        }
    }

    /**
     * Probability that a pipe at {@code stage} advances one oxidation stage this tick, given how many
     * same-family weathering neighbors it has and how many of those are further oxidized. Mirrors
     * vanilla copper: the chance grows with the share of more-oxidized neighbors and is dampened for
     * an as-yet unaffected pipe. Never exceeds 1.0 because a more-oxidized neighbor is also counted
     * as a weathering neighbor, so {@code moreOxidizedNeighbors <= weatheringNeighbors}.
     */
    static double oxidationChance(int stage, int weatheringNeighbors, int moreOxidizedNeighbors) {
        double ratio = (moreOxidizedNeighbors + 1.0) / (weatheringNeighbors + 1.0);
        double stageMultiplier = (stage == STAGE_UNAFFECTED) ? 0.75 : 1.0;
        return stageMultiplier * ratio * ratio;
    }

    @Override
    public InteractionResult onUseWithItem(PipeContext ctx, UseOnContext usage) {
        ItemStack stack = usage.getItemInHand();
        Player player = usage.getPlayer();

        // Handle honeycomb waxing
        if (stack.is(Items.HONEYCOMB)) {
            return handleWaxing(ctx, usage, player, stack);
        }

        // Handle axe scraping
        if (stack.is(ItemTags.AXES)) {
            return handleScraping(ctx, usage, player, stack);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleWaxing(PipeContext ctx, UseOnContext usage, Player player, ItemStack stack) {
        if (isWaxed(ctx)) {
            return InteractionResult.PASS;
        }

        if (ctx.world().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ctx.saveInt(this, WAXED_KEY, 1);
        ctx.markDirtyAndSync();

        ctx.world().playSound(null, ctx.pos(), SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0f, 1.0f);

        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    private InteractionResult handleScraping(PipeContext ctx, UseOnContext usage, Player player, ItemStack stack) {
        boolean waxed = isWaxed(ctx);
        int stage = getOxidationStage(ctx);

        // Nothing to scrape
        if (!waxed && stage == STAGE_UNAFFECTED) {
            return InteractionResult.PASS;
        }

        if (ctx.world().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (waxed) {
            // Remove wax first, keep oxidation stage
            ctx.saveInt(this, WAXED_KEY, 0);
            ctx.world().playSound(null, ctx.pos(), SoundEvents.AXE_WAX_OFF.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
        } else {
            // Reduce oxidation by one stage
            ctx.saveInt(this, OXIDATION_KEY, stage - 1);
            ctx.world().playSound(null, ctx.pos(), SoundEvents.AXE_SCRAPE.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        ctx.markDirtyAndSync();

        if (player != null && !player.getAbilities().instabuild) {
            EquipmentSlot slot = usage.getHand() == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, player, slot);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable ResourceId getCoreModel(PipeContext ctx) {
        int stage = getOxidationStage(ctx);
        if (stage == STAGE_UNAFFECTED) {
            return null; // Use default model
        }
        String suffix = getStageSuffix(stage);
        return modelResolver.apply(modelBase + "_core" + suffix);
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        int stage = getOxidationStage(ctx);
        if (stage == STAGE_UNAFFECTED) {
            return null; // Use default model
        }
        String suffix = getStageSuffix(stage);
        String armType = (extendedArms && ctx.isInventoryConnection(direction)) ? "_arm_extended" : "_arm";
        return modelResolver.apply(modelBase + armType + suffix);
    }

    // --- Item component handling ---

    @Override
    public void addItemComponents(DataComponentMap.Builder builder, PipeContext ctx) {
        int stage = getOxidationStage(ctx);
        boolean waxed = isWaxed(ctx);

        WeatheringState state = new WeatheringState(stage, waxed);
        if (!state.isDefault()) {
            builder.set(LogisticsPipe.DATA.WEATHERING_STATE, state);
        }
    }

    @Override
    public void readItemComponents(DataComponentGetter components, PipeContext ctx) {
        WeatheringState state = components.get(LogisticsPipe.DATA.WEATHERING_STATE);
        if (state == null || state.isDefault()) return;

        ctx.saveInt(this, OXIDATION_KEY, state.oxidationStage());
        ctx.saveInt(this, WAXED_KEY, state.waxed() ? 1 : 0);
    }

    @Override
    public List<String> getCustomModelDataStrings(PipeContext ctx) {
        int stage = getOxidationStage(ctx);
        boolean waxed = isWaxed(ctx);

        if (stage == STAGE_UNAFFECTED && !waxed) {
            return List.of();
        }

        String modelKey = getModelKey(stage, waxed);
        return List.of(modelKey);
    }

    @Override
    public String getItemNameSuffix(PipeContext ctx) {
        int stage = getOxidationStage(ctx);
        boolean waxed = isWaxed(ctx);
        return buildItemNameSuffix(stage, waxed);
    }

    @Override
    public String getItemNameSuffixFromComponents(DataComponentGetter components) {
        WeatheringState state = components.get(LogisticsPipe.DATA.WEATHERING_STATE);
        if (state == null || state.isDefault()) {
            return "";
        }
        return buildItemNameSuffix(state.oxidationStage(), state.waxed());
    }

    @Override
    public void appendCreativeMenuVariants(List<ItemStack> stacks, ItemStack baseStack) {
        // Add all oxidation stages (unwaxed)
        for (int stage = STAGE_EXPOSED; stage <= STAGE_OXIDIZED; stage++) {
            stacks.add(createVariant(baseStack, stage, false));
        }

        // Add all waxed variants (including waxed unaffected)
        for (int stage = STAGE_UNAFFECTED; stage <= STAGE_OXIDIZED; stage++) {
            stacks.add(createVariant(baseStack, stage, true));
        }
    }

    private static ItemStack createVariant(ItemStack baseStack, int stage, boolean waxed) {
        ItemStack stack = baseStack.copy();
        stack.set(LogisticsPipe.DATA.WEATHERING_STATE, new WeatheringState(stage, waxed));

        // Add custom model data string key for item model variant selection
        if (stage > 0 || waxed) {
            String modelKey = getModelKey(stage, waxed);
            stack.set(
                    DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(List.of(), List.of(), List.of(modelKey), List.of()));
        }

        return stack;
    }

    private static String buildItemNameSuffix(int stage, boolean waxed) {
        String oxidationSuffix =
                switch (stage) {
                    case STAGE_EXPOSED -> ".exposed";
                    case STAGE_WEATHERED -> ".weathered";
                    case STAGE_OXIDIZED -> ".oxidized";
                    default -> "";
                };

        if (waxed) {
            return ".waxed" + oxidationSuffix;
        }
        return oxidationSuffix;
    }

    private static String getModelKey(int stage, boolean waxed) {
        String stageName =
                switch (stage) {
                    case STAGE_EXPOSED -> "exposed";
                    case STAGE_WEATHERED -> "weathered";
                    case STAGE_OXIDIZED -> "oxidized";
                    default -> "";
                };

        if (waxed && !stageName.isEmpty()) {
            return "waxed_" + stageName;
        } else if (waxed) {
            return "waxed";
        }
        return stageName;
    }
}

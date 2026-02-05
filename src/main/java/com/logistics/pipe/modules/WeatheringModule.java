package com.logistics.pipe.modules;

import com.logistics.LogisticsMod;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.data.PipeDataComponents;
import com.logistics.pipe.data.PipeDataComponents.WeatheringState;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
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
public class WeatheringModule implements Module {
    private static final String OXIDATION_KEY = "oxidation_stage";
    private static final String WAXED_KEY = "waxed";

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
    public boolean hasRandomTicks() {
        return true;
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

        // Step 1: random tick gate (vanilla copper uses 1125/64 odds)
        if (rand.nextInt(1125) >= 64) return;

        // Step 2: scan neighbors within Manhattan distance 4
        int a = 0; // nearby non-waxed weathering pipes
        int b = 0; // nearby pipes more oxidized than me
        BlockPos origin = ctx.pos();

        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-4, -4, -4), origin.offset(4, 4, 4))) {
            if (p.equals(origin)) continue;
            if (origin.distManhattan(p) > 4) continue;

            if (!(ctx.world().getBlockState(p).getBlock() instanceof PipeBlock pipeBlock)) continue;
            if (!(ctx.world().getBlockEntity(p) instanceof PipeBlockEntity be)) continue;

            Pipe pipe = pipeBlock.getPipe();
            if (pipe == null || pipe.getModule(WeatheringModule.class) == null) continue;

            PipeContext neighbor = new PipeContext(ctx.world(), p, ctx.world().getBlockState(p), be);
            if (isWaxed(neighbor)) continue;

            int neighborStage = getOxidationStage(neighbor);

            // Abort if any neighbor is less oxidized
            if (neighborStage < stage) return;

            a++;
            if (neighborStage > stage) b++;
        }

        // Step 3: compute progression chance
        double c = (b + 1.0) / (a + 1.0);
        double m = (stage == STAGE_UNAFFECTED) ? 0.75 : 1.0;
        double chance = m * c * c;

        // Step 4: roll for progression
        if (rand.nextDouble() < chance) {
            ctx.saveInt(this, OXIDATION_KEY, stage + 1);
            ctx.markDirtyAndSync();
        }
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
        if (stack.getItem() instanceof AxeItem) {
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
            ctx.world().playSound(null, ctx.pos(), SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0f, 1.0f);
        } else {
            // Reduce oxidation by one stage
            ctx.saveInt(this, OXIDATION_KEY, stage - 1);
            ctx.world().playSound(null, ctx.pos(), SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0f, 1.0f);
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
    public @Nullable Identifier getCoreModel(PipeContext ctx) {
        int stage = getOxidationStage(ctx);
        if (stage == STAGE_UNAFFECTED) {
            return null; // Use default model
        }
        String suffix = getStageSuffix(stage);
        return Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/pipe/copper_transport_pipe_core" + suffix);
    }

    @Override
    public @Nullable Identifier getPipeArm(PipeContext ctx, Direction direction) {
        int stage = getOxidationStage(ctx);
        if (stage == STAGE_UNAFFECTED) {
            return null; // Use default model
        }
        String suffix = getStageSuffix(stage);
        String armType = ctx.isInventoryConnection(direction) ? "_arm_extended" : "_arm";
        return Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/pipe/copper_transport_pipe" + armType + suffix);
    }

    // --- Item component handling ---

    @Override
    public void addItemComponents(DataComponentMap.Builder builder, PipeContext ctx) {
        int stage = getOxidationStage(ctx);
        boolean waxed = isWaxed(ctx);

        WeatheringState state = new WeatheringState(stage, waxed);
        if (!state.isDefault()) {
            builder.set(PipeDataComponents.WEATHERING_STATE, state);
        }
    }

    @Override
    public void readItemComponents(DataComponentGetter components, PipeContext ctx) {
        WeatheringState state = components.get(PipeDataComponents.WEATHERING_STATE);
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
    public String getItemNameSuffixFromComponents(net.minecraft.core.component.DataComponentGetter components) {
        WeatheringState state = components.get(PipeDataComponents.WEATHERING_STATE);
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
        stack.set(PipeDataComponents.WEATHERING_STATE, new WeatheringState(stage, waxed));

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

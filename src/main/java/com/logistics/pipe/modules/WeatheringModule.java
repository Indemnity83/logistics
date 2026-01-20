package com.logistics.pipe.modules;

import com.logistics.LogisticsDataComponents;
import com.logistics.LogisticsDataComponents.WeatheringState;
import com.logistics.LogisticsMod;
import com.logistics.block.PipeBlock;
import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import java.util.List;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
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
    public void randomTick(PipeContext ctx, Random rand) {
        if (ctx.world().isClient()) {
            return;
        }
        tryOxidize(ctx, rand);
    }

    public void tryOxidize(PipeContext ctx, Random rand) {
        if (isWaxed(ctx)) return;

        int stage = getOxidationStage(ctx);
        if (stage >= STAGE_OXIDIZED) return;

        // Step 1: random tick gate (vanilla copper uses 1125/64 odds)
        if (rand.nextInt(1125) >= 64) return;

        // Step 2: scan neighbors within Manhattan distance 4
        int a = 0; // nearby non-waxed weathering pipes
        int b = 0; // nearby pipes more oxidized than me
        BlockPos origin = ctx.pos();

        for (BlockPos p : BlockPos.iterateOutwards(origin, 4, 4, 4)) {
            if (p.equals(origin)) continue;
            if (origin.getManhattanDistance(p) > 4) continue;

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
    public ActionResult onUseWithItem(PipeContext ctx, ItemUsageContext usage) {
        ItemStack stack = usage.getStack();
        PlayerEntity player = usage.getPlayer();

        // Handle honeycomb waxing
        if (stack.isOf(Items.HONEYCOMB)) {
            return handleWaxing(ctx, usage, player, stack);
        }

        // Handle axe scraping
        if (stack.getItem() instanceof AxeItem) {
            return handleScraping(ctx, usage, player, stack);
        }

        return ActionResult.PASS;
    }

    private ActionResult handleWaxing(PipeContext ctx, ItemUsageContext usage, PlayerEntity player, ItemStack stack) {
        if (isWaxed(ctx)) {
            return ActionResult.PASS;
        }

        if (ctx.world().isClient()) {
            return ActionResult.SUCCESS;
        }

        ctx.saveInt(this, WAXED_KEY, 1);
        ctx.markDirtyAndSync();

        ctx.world().playSound(null, ctx.pos(), SoundEvents.ITEM_HONEYCOMB_WAX_ON, SoundCategory.BLOCKS, 1.0f, 1.0f);

        if (player != null && !player.isCreative()) {
            stack.decrement(1);
        }

        return ActionResult.SUCCESS;
    }

    private ActionResult handleScraping(PipeContext ctx, ItemUsageContext usage, PlayerEntity player, ItemStack stack) {
        boolean waxed = isWaxed(ctx);
        int stage = getOxidationStage(ctx);

        // Nothing to scrape
        if (!waxed && stage == STAGE_UNAFFECTED) {
            return ActionResult.PASS;
        }

        if (ctx.world().isClient()) {
            return ActionResult.SUCCESS;
        }

        if (waxed) {
            // Remove wax first, keep oxidation stage
            ctx.saveInt(this, WAXED_KEY, 0);
            ctx.world().playSound(null, ctx.pos(), SoundEvents.ITEM_AXE_WAX_OFF, SoundCategory.BLOCKS, 1.0f, 1.0f);
        } else {
            // Reduce oxidation by one stage
            ctx.saveInt(this, OXIDATION_KEY, stage - 1);
            ctx.world().playSound(null, ctx.pos(), SoundEvents.ITEM_AXE_SCRAPE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }

        ctx.markDirtyAndSync();

        if (player != null && !player.isCreative()) {
            stack.damage(1, player, usage.getHand());
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public @Nullable Identifier getCoreModel(PipeContext ctx) {
        int stage = getOxidationStage(ctx);
        if (stage == STAGE_UNAFFECTED) {
            return null; // Use default model
        }
        String suffix = STAGE_SUFFIXES[stage];
        return Identifier.of(LogisticsMod.MOD_ID, "block/copper_transport_pipe_core" + suffix);
    }

    @Override
    public @Nullable Identifier getPipeArm(PipeContext ctx, Direction direction) {
        int stage = getOxidationStage(ctx);
        if (stage == STAGE_UNAFFECTED) {
            return null; // Use default model
        }
        String suffix = STAGE_SUFFIXES[stage];
        String armType = ctx.isInventoryConnection(direction) ? "_arm_extended" : "_arm";
        return Identifier.of(LogisticsMod.MOD_ID, "block/copper_transport_pipe" + armType + suffix);
    }

    // --- Item component handling ---

    @Override
    public void addItemComponents(ComponentMap.Builder builder, PipeContext ctx) {
        int stage = getOxidationStage(ctx);
        boolean waxed = isWaxed(ctx);

        WeatheringState state = new WeatheringState(stage, waxed);
        if (!state.isDefault()) {
            builder.add(LogisticsDataComponents.WEATHERING_STATE, state);
        }
    }

    @Override
    public void readItemComponents(ComponentsAccess components, PipeContext ctx) {
        WeatheringState state = components.get(LogisticsDataComponents.WEATHERING_STATE);
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
    public String getItemNameSuffixFromComponents(ComponentsAccess components) {
        WeatheringState state = components.get(LogisticsDataComponents.WEATHERING_STATE);
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
        stack.set(LogisticsDataComponents.WEATHERING_STATE, new WeatheringState(stage, waxed));

        String modelKey = getModelKey(stage, waxed);
        if (!modelKey.isEmpty()) {
            stack.set(
                    DataComponentTypes.CUSTOM_MODEL_DATA,
                    new CustomModelDataComponent(List.of(), List.of(), List.of(modelKey), List.of()));
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

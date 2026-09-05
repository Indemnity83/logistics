package com.logistics.pipe.block;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.fluids.FluidContainerInteraction;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.platform.PlatformService;
import com.logistics.core.lib.tank.TankColumn;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.logistics.pipe.tank.TankTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Glass Tank: stores one fluid and stacks vertically into a shared column (see {@link TankColumn}).
 * Right-click with a bucket fills/drains; with a potion or glass bottle, deposits/draws 1/3 bucket of
 * water (potions are stored as water carrying a {@code potion_contents} component). Empty-hand reads the
 * contents to the action bar. Behaviour lives in {@link GlassTankBlockEntity} and the column core.
 */
public class GlassTankBlock extends BaseEntityBlock {

    /** True when a tank sits directly below, so the seamless {@code side_stacked} texture is used. */
    public static final BooleanProperty JOINED_BELOW = BooleanProperty.create("joined_below");

    /** Block light (0-15) emitted while the tank holds a light-emitting fluid; driven by the block entity. */
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);

    public GlassTankBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIGHT_LEVEL)));
        registerDefaultState(defaultBlockState().setValue(JOINED_BELOW, false).setValue(LIGHT_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(JOINED_BELOW, LIGHT_LEVEL);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GlassTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, LogisticsPipe.ENTITY.GLASS_TANK_BLOCK_ENTITY, GlassTankBlockEntity::tick);
    }

    // ==================== Interaction ====================

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof GlassTankBlockEntity tank) {
            if (stack.is(Items.POTION) || stack.is(Items.GLASS_BOTTLE)) {
                return bottleInteraction(stack, level, pos, player, hand, tank)
                        ? InteractionResult.SUCCESS
                        : InteractionResult.PASS;
            }
            InteractionResult result = FluidContainerInteraction.interact(level, pos, player, hand, hit.getDirection());
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GlassTankBlockEntity tank) {
            player.sendOverlayMessage(describe(tank.column()));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Deposits a held potion into the tank, or draws a bottle of water into a held glass bottle. Mutates
     * only on the server; the client returns success and is re-synced.
     */
    private static boolean bottleInteraction(
            ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand, GlassTankBlockEntity tank) {
        long bottle = TankTier.bottleNative();
        TankColumn column = tank.column();

        if (stack.is(Items.POTION)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) {
                return false;
            }
            IFluidKey resource = isPlainWater(contents)
                    ? SimpleFluidKey.of(Fluids.WATER)
                    : new SimpleFluidKey(
                            Fluids.WATER,
                            DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, contents).build());
            if (column.room() < bottle
                    || (!column.shared().isBlank() && !sameFluid(column.shared(), resource))) {
                return false;
            }
            if (level.isClientSide()) {
                return true;
            }
            if (!column.depositBottle(resource, bottle)) {
                return false;
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            consumeAndReturn(player, hand, stack, new ItemStack(Items.GLASS_BOTTLE));
            return true;
        }

        // Empty glass bottle: only water can be bottled back out.
        IFluidKey current = column.shared();
        if (current.isBlank() || current.getFluid() != Fluids.WATER || column.total() < bottle) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }
        if (!column.extractBottle(current, bottle)) {
            return false;
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        consumeAndReturn(player, hand, stack, bottleFor(current));
        return true;
    }

    private static boolean isPlainWater(PotionContents contents) {
        return contents.is(Potions.WATER)
                && contents.customEffects().isEmpty()
                && contents.customColor().isEmpty()
                && contents.customName().isEmpty();
    }

    private static ItemStack bottleFor(IFluidKey water) {
        PotionContents contents = potionOf(water);
        if (contents == null) {
            return PotionContents.createItemStack(Items.POTION, Potions.WATER);
        }
        ItemStack bottle = new ItemStack(Items.POTION);
        bottle.set(DataComponents.POTION_CONTENTS, contents);
        return bottle;
    }

    @Nullable
    private static PotionContents potionOf(IFluidKey key) {
        for (var entry : key.getComponents().entrySet()) {
            if (entry.getKey() == DataComponents.POTION_CONTENTS) {
                return (PotionContents) entry.getValue().orElse(null);
            }
        }
        return null;
    }

    private static boolean sameFluid(IFluidKey a, IFluidKey b) {
        return a.getFluid() == b.getFluid() && a.getComponents().equals(b.getComponents());
    }

    private static void consumeAndReturn(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        if (player.getAbilities().instabuild) {
            return;
        }
        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(hand, result);
        } else if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
    }

    private static Component describe(TankColumn column) {
        IFluidKey shared = column.shared();
        if (shared.isBlank() || column.total() <= 0) {
            return Component.translatable("tooltip.logistics.fluid.tank_empty");
        }
        long perMb = Math.max(1, PlatformService.INSTANCE.fluidUnitsPerMillibucket());
        long amountMb = column.total() / perMb;
        long capacityMb = column.capacity() / perMb;
        return Component.translatable(
                "tooltip.logistics.fluid.tank_contents", fluidName(shared.getFluid()), amountMb, capacityMb);
    }

    /** A fluid's display name: its world block's name, or an id-derived key for block-less fluids. */
    private static Component fluidName(net.minecraft.world.level.material.Fluid fluid) {
        return com.logistics.core.lib.fluids.FluidDisplay.name(fluid);
    }

    // ==================== Stacking ====================

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        return defaultBlockState().setValue(JOINED_BELOW, below.getBlock() instanceof GlassTankBlock);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess scheduledTickAccess,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (direction == Direction.DOWN) {
            state = state.setValue(JOINED_BELOW, neighborState.getBlock() instanceof GlassTankBlock);
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    /** Hide the shared top/bottom face between two stacked tanks so a column looks seamless. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState neighborState, Direction direction) {
        if (direction.getAxis() == Direction.Axis.Y && neighborState.getBlock() instanceof GlassTankBlock) {
            return true;
        }
        return super.skipRendering(state, neighborState, direction);
    }

    // ==================== Comparator ====================

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GlassTankBlockEntity tank && tank.capacity() > 0) {
            if (tank.amount() <= 0) {
                return 0;
            }
            return Math.max(1, (int) (15L * tank.amount() / tank.capacity()));
        }
        return 0;
    }
}

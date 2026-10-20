package com.logistics.forestry.farm;

import com.logistics.LogisticsForestry;
import com.logistics.core.lib.block.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Base block for every farm type. A farm sits at the edge of its plot and faces into it: placed,
 * it faces the way the player was looking, so the plot lies ahead of where they stood. All farm
 * blocks share {@link FarmBlockEntity}; a subclass only supplies its {@link FarmLogic}.
 *
 * <p>Not named {@code FarmBlock}: that is vanilla farmland on older Minecraft versions.
 */
public abstract class FarmMachineBlock extends MachineBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<FarmLamp> LAMP = EnumProperty.create("lamp", FarmLamp.class);

    protected FarmMachineBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LAMP, FarmLamp.OFF));
    }

    /** This farm type's behaviour; the same instance on every call. */
    public abstract FarmLogic logic();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LAMP);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer != null && level.getBlockEntity(pos) instanceof FarmBlockEntity farm) {
            farm.setOwner(placer.getUUID());
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FarmBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, LogisticsForestry.ENTITY.FARM, FarmBlockEntity::tick);
    }
}

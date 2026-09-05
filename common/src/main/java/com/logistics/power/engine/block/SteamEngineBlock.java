package com.logistics.power.engine.block;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.SteamEngineBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

/**
 * Steam Engine - boils solid fuel + water into stored pressure, which drives up to 40 RF/t.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>FACING determines output direction; POWERED gates operation (redstone-controlled)</li>
 *   <li>A solid-fuel slot and a water tank; pressure is an internal reserve</li>
 *   <li>Never overheats — output is stable at pressure and sags only when starved</li>
 *   <li>LIT reflects a maintained firebox (boiling or stoked)</li>
 * </ul>
 */
public class SteamEngineBlock extends AbstractEngineBlock<SteamEngineBlockEntity> {
    public static final BooleanProperty LIT = BlockStateProperties.LIT; // true when the firebox is lit

    public SteamEngineBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected List<Property<?>> getAdditionalProperties() {
        return List.of(LIT);
    }

    @Override
    protected BlockState applyAdditionalPlacementState(BlockState base, BlockPlaceContext ctx) {
        return base.setValue(LIT, false);
    }

    // No onWrench override: the steam engine never overheats, so a wrench just rotates (super handles it).

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, LogisticsPower.ENTITY.STEAM_ENGINE_BLOCK_ENTITY, SteamEngineBlockEntity::tick);
    }
}

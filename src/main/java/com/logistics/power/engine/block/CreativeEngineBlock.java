package com.logistics.power.engine.block;

import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.registry.PowerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Creative Engine - a special engine for Creative Mode that generates configurable energy.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>FACING property determines output direction (where energy is pushed)</li>
 *   <li>Requires redstone signal to function</li>
 *   <li>Default output: 20 RF/t</li>
 *   <li>Sneak + right-click with wrench doubles output rate (up to 1280 RF/t)</li>
 *   <li>Cannot overheat - always safe to use</li>
 * </ul>
 */
public class CreativeEngineBlock extends AbstractEngineBlock<CreativeEngineBlockEntity> {
    public static final MapCodec<CreativeEngineBlock> CODEC = createCodec(CreativeEngineBlock::new);

    public CreativeEngineBlock(Settings settings) {
        super(settings, BlockSoundGroup.STONE);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected CreativeEngineBlockEntity getEngineBlockEntity(BlockEntity be) {
        return be instanceof CreativeEngineBlockEntity ? (CreativeEngineBlockEntity) be : null;
    }

    @Override
    protected boolean handleSpecialWrench(World world, BlockPos pos, PlayerEntity player, BlockState state) {
        // Sneak + wrench: cycle output level
        if (player.isSneaking() && world.getBlockEntity(pos) instanceof CreativeEngineBlockEntity engine) {
            if (!world.isClient()) {
                long newRate = engine.cycleOutputLevel();
                player.sendMessage(Text.translatable("message.logistics.power.creative_engine.output", newRate), true);
            }
            return true;
        }
        return false;
    }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, PowerBlockEntities.CREATIVE_ENGINE_BLOCK_ENTITY, CreativeEngineBlockEntity::tick);
    }
}

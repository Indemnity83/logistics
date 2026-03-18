package com.logistics.pipe.item;

import com.logistics.pipe.modules.Module;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/**
 * An item that carries chassis module behavior.
 * When placed in a chassis slot, {@link #createModule()} is called to instantiate
 * the module that will be active for that slot.
 */
public class ModuleItem extends Item {
    private final Supplier<? extends Module> factory;

    public ModuleItem(Properties props, Supplier<? extends Module> factory) {
        super(props);
        this.factory = factory;
    }

    public Module createModule() {
        return factory.get();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        return createModule().openItemConfig(serverPlayer, hand, player.getItemInHand(hand));
    }
}

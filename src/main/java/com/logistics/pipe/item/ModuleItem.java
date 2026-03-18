package com.logistics.pipe.item;

import com.logistics.core.lib.pipe.Module;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);
        InteractionResult result = createModule().openItemConfig(serverPlayer, hand, stack);
        return new InteractionResultHolder<>(result, stack);
    }
}

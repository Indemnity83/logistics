package com.logistics.pipe.item;

import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.compat.NbtCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * An item that carries chassis module behavior.
 * When placed in a chassis slot, {@link #createModule()} is called to instantiate
 * the module that will be active for that slot.
 */
public class ModuleItem extends Item {
    public static final String MODULE_ID_KEY = "logistics_module_id";

    private final Supplier<? extends Module> factory;

    public ModuleItem(Properties props, Supplier<? extends Module> factory) {
        super(props);
        this.factory = factory;
    }

    public Module createModule() {
        return factory.get();
    }

    public static String moduleStateKey(ItemStack stack, Module module) {
        return "module." + ensureModuleId(stack) + "." + module.getStateKey();
    }

    public static String ensureModuleId(ItemStack stack) {
        String moduleId = getModuleId(stack);
        if (!moduleId.isBlank()) return moduleId;

        moduleId = UUID.randomUUID().toString();
        CompoundTag tag = copyCustomData(stack);
        tag.putString(MODULE_ID_KEY, moduleId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return moduleId;
    }

    public static String getModuleId(ItemStack stack) {
        return NbtCompat.getString(copyCustomData(stack), MODULE_ID_KEY, "");
    }

    public static CustomData customDataWithModuleState(ItemStack stack, CompoundTag moduleState) {
        CompoundTag tag = moduleState.copy();
        tag.remove(MODULE_ID_KEY);
        tag.putString(MODULE_ID_KEY, ensureModuleId(stack));
        return CustomData.of(tag);
    }

    public static boolean isModuleIdentityKey(String key) {
        return MODULE_ID_KEY.equals(key);
    }

    private static CompoundTag copyCustomData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
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

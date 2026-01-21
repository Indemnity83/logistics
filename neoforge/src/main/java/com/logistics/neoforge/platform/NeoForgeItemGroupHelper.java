package com.logistics.neoforge.platform;

import com.logistics.platform.services.IItemGroupHelper;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * NeoForge implementation of IItemGroupHelper using CreativeModeTab.Builder.
 */
public class NeoForgeItemGroupHelper implements IItemGroupHelper {

    @Override
    @SuppressWarnings("unchecked")
    public net.minecraft.item.ItemGroup createGroup(
            net.minecraft.util.Identifier id,
            net.minecraft.text.Text displayName,
            Supplier<net.minecraft.item.ItemStack> icon,
            Consumer<net.minecraft.item.ItemGroup.Entries> entries) {
        // This implementation requires mapping conversion
        // For now, create a basic creative tab
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath());

        CreativeModeTab tab = CreativeModeTab.builder()
                .title((Component) (Object) displayName)
                .icon(() -> (ItemStack) (Object) icon.get())
                .displayItems((params, output) -> {
                    // Convert to Yarn entries interface
                    entries.accept(stack -> output.accept((ItemStack) (Object) stack));
                })
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, location, tab);

        return (net.minecraft.item.ItemGroup) (Object) tab;
    }
}

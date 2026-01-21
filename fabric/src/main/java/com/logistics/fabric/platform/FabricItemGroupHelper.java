package com.logistics.fabric.platform;

import com.logistics.platform.services.IItemGroupHelper;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Fabric implementation of IItemGroupHelper using FabricItemGroup.
 */
public class FabricItemGroupHelper implements IItemGroupHelper {

    @Override
    public ItemGroup createGroup(
            Identifier id, Text displayName, Supplier<ItemStack> icon, Consumer<ItemGroup.Entries> entries) {
        return FabricItemGroup.builder()
                .icon(icon)
                .displayName(displayName)
                .entries((displayContext, entriesCollector) -> entries.accept(entriesCollector))
                .build();
    }
}

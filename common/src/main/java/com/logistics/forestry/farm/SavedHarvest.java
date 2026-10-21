package com.logistics.forestry.farm;

import com.logistics.core.lib.compat.NbtCompat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/**
 * A harvest under way as a farm saves it: its plot, its germling and the drops collected so far. Its
 * blocks are not saved; the farm plans them again from the plot once loaded.
 */
record SavedHarvest(TendLogic.Harvest harvest, List<ItemStack> drops) {

    CompoundTag save(HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        CompoundTag saved = new CompoundTag();
        saved.putLong("Plot", harvest.plot().asLong());
        if (!harvest.germling().isEmpty()) {
            ItemStack.CODEC.encodeStart(ops, harvest.germling()).result().ifPresent(g -> saved.put("Germling", g));
        }
        ListTag list = new ListTag();
        for (ItemStack drop : drops) {
            ItemStack.CODEC.encodeStart(ops, drop).result().ifPresent(list::add);
        }
        saved.put("Drops", list);
        return saved;
    }

    /** The harvest saved in {@code saved}, with no blocks planned. */
    static SavedHarvest load(CompoundTag saved, HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ItemStack germling = NbtCompat.hasCompound(saved, "Germling")
                ? ItemStack.CODEC.parse(ops, NbtCompat.getCompoundOrEmpty(saved, "Germling")).result().orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        BlockPos plot = BlockPos.of(NbtCompat.getLong(saved, "Plot", 0L));
        List<ItemStack> drops = new ArrayList<>();
        ListTag list = NbtCompat.getListOrEmpty(saved, "Drops");
        for (int i = 0; i < list.size(); i++) {
            NbtCompat.ifHasCompoundAt(list, i, drop -> ItemStack.CODEC
                    .parse(ops, drop)
                    .result()
                    .filter(stack -> !stack.isEmpty())
                    .ifPresent(drops::add));
        }
        return new SavedHarvest(new TendLogic.Harvest(plot, List.of(), germling), drops);
    }
}

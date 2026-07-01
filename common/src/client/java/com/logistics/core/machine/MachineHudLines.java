package com.logistics.core.machine;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.fluids.FluidDisplay;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;

/**
 * Builds the machine HUD lines from the synced tag written by {@code MachineHudData}. Only a progress line
 * while the machine is actively processing — item slots and energy are already shown by Jade's built-ins.
 */
public final class MachineHudLines {

    private MachineHudLines() {}

    public static List<Component> build(CompoundTag data) {
        List<Component> lines = new ArrayList<>();
        if (NbtCompat.getBoolean(data, MachineHudData.KEY_PROCESSING, false)) {
            float progress = NbtCompat.getFloat(data, "progress", 0);
            lines.add(Component.translatable("jade.logistics.machine.progress")
                    .append(Component.literal(": "))
                    .append(Component.literal(String.format("%.0f%%", progress * 100)).withStyle(ChatFormatting.GREEN)));
        }
        int fluidAmount = NbtCompat.getInt(data, MachineHudData.KEY_FLUID_AMOUNT, 0);
        if (fluidAmount > 0) {
            Fluid fluid = BuiltInRegistries.FLUID.byId(NbtCompat.getInt(data, MachineHudData.KEY_FLUID_ID, 0));
            int capacity = NbtCompat.getInt(data, MachineHudData.KEY_FLUID_CAPACITY, 0);
            lines.add(Component.translatable(
                    "tooltip.logistics.fluid.tank_contents", FluidDisplay.name(fluid), fluidAmount, capacity));
        }
        return lines;
    }
}

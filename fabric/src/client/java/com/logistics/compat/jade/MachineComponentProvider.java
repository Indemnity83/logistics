package com.logistics.compat.jade;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.machine.MachineHudLines;
import com.logistics.core.machine.MachineHudModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier; // raw-id-ok: Jade's IJadeProvider.getUid() returns Identifier
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.ProgressView;

/**
 * Client half of the machine Jade integration: renders the HUD model synced by
 * {@link MachineServerDataProvider}. Progress text is shared in {@link MachineHudLines}; fluid tanks are
 * drawn here as a capacity-relative bar (the built-in Jade fluid element is stripped so they don't
 * double up), because the Jade element API is version-specific.
 */
public final class MachineComponentProvider implements IBlockComponentProvider {
    public static final MachineComponentProvider INSTANCE = new MachineComponentProvider();

    private static final Identifier UID = // raw-id-ok: Jade keys providers by Identifier
            LogisticsMod.modId("machine").toIdentifier();

    private MachineComponentProvider() {}

    @Override
    public Identifier getUid() { // raw-id-ok: overrides Jade API returning Identifier
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        return 2000; // After Jade's built-in fluid bar (priority 1000) so remove() can replace it.
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        for (Component line : MachineHudLines.build(accessor.getServerData())) {
            tooltip.add(line);
        }
        for (MachineHudModel.Entry entry : MachineHudModel.entries(accessor.getServerData())) {
            if (entry instanceof MachineHudModel.FluidEntry fluid) {
                renderFluidBar(tooltip, fluid);
            }
        }
    }

    private static void renderFluidBar(ITooltip tooltip, MachineHudModel.FluidEntry entry) {
        ResourceId id = ResourceId.tryParse(entry.fluidId());
        if (id == null) {
            return;
        }
        Fluid fluid = BuiltInRegistries.FLUID.getValue(id.toIdentifier());
        if (fluid == Fluids.EMPTY || entry.capacityMb() <= 0) {
            return;
        }
        // The look-at HUD carries millibuckets; Jade elements measure fluid in native volume.
        long perMb = JadeFluidObject.bucketVolume() / 1000;
        JadeFluidObject object = JadeFluidObject.of(fluid, entry.amountMb() * perMb, entry.components());
        FluidView view = FluidView.readDefault(new FluidView.Data(object, entry.capacityMb() * perMb));
        if (view == null) {
            return;
        }
        // Replace Jade's inconsistent built-in fluid element with our own bar.
        tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
        Component text = Component.translatable(
                "jade.fluid", view.fluidName, Component.translatable("jade.fluid.with_capacity", view.current, view.max));
        ProgressView bar = new ProgressView(
                ProgressView.Part.of(view.ratio, view.overlay),
                text,
                JadeUI.progressStyle().canDecrease(true),
                BoxStyle.nestedBox());
        tooltip.add(JadeUI.progress(bar));
    }
}

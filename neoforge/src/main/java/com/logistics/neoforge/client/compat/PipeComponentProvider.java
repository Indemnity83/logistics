package com.logistics.neoforge.client.compat;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.pipe.PipeHud;
import com.logistics.pipe.PipeHudLines;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier; // raw-id-ok: Jade's IJadeProvider.getUid() returns Identifier
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

/**
 * Jade integration for pipes: items in transit, each module's status (filters, modes, recipes…) and a row
 * of installed-module icons for chassis pipes. Reads the client block entity directly — module and
 * traveling-item state are both synced. Item rows render as icons via {@link TooltipPipeHud}.
 */
public final class PipeComponentProvider implements IBlockComponentProvider {
    public static final PipeComponentProvider INSTANCE = new PipeComponentProvider();

    private static final Identifier UID = // raw-id-ok: Jade keys providers by Identifier
            LogisticsMod.modId("pipe").toIdentifier();

    private PipeComponentProvider() {}

    @Override
    public Identifier getUid() { // raw-id-ok: overrides Jade API returning Identifier
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        // Run after Jade's built-in energy element (priority 1000) so we can strip it below.
        return 2000;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof PipeBlockEntity pipe)) {
            return;
        }
        // A pipe's energy buffer is an internal implementation detail — drop Jade's generic energy bar.
        tooltip.remove(JadeIds.UNIVERSAL_ENERGY_STORAGE);

        boolean details = accessor.showDetails();
        for (Component line : PipeHudLines.build(pipe.getTravelingItems(), details)) {
            tooltip.add(line);
        }

        BlockState state = accessor.getBlockState();
        // One row per installed chassis module: a small item icon, a little padding, then its name.
        for (ItemStack module : PipeHudLines.installedModules(state, pipe)) {
            tooltip.add(JadeUI.smallItem(module));
            tooltip.append(JadeUI.spacer(3, 0));
            tooltip.append(module.getHoverName());
        }

        PipeHudLines.appendModuleHud(state, pipe, new TooltipPipeHud(tooltip, details));
    }

    /** Renders a module's HUD output: text lines as-is, item rows as scaled inventory-style icons. */
    private record TooltipPipeHud(ITooltip tooltip, boolean details) implements PipeHud {
        @Override
        public boolean showDetails() {
            return details;
        }

        @Override
        public void line(Component text) {
            tooltip.add(text);
        }

        @Override
        public void items(List<ItemStack> stacks) {
            // One row per item, Jade's item-storage style: smallItem (its own icon element — handles
            // sizing and alignment), then "N× Name" text — matching the vanilla chest readout.
            for (ItemStack stack : stacks) {
                if (stack.isEmpty()) {
                    continue;
                }
                tooltip.add(JadeUI.smallItem(stack));
                Component label = stack.getCount() > 1
                        ? Component.literal(stack.getCount() + "× ").append(stack.getHoverName())
                        : stack.getHoverName().copy();
                tooltip.append(label);
            }
        }
    }
}

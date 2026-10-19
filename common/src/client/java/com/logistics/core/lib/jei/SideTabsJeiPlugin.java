package com.logistics.core.lib.jei;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.screen.tab.SideTabScreen;
import com.logistics.core.lib.client.screen.tab.TabRect;
import com.logistics.core.lib.resource.ResourceId;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

/**
 * Keeps JEI's ingredient list and bookmarks clear of side tabs, on every container screen that
 * implements {@link SideTabScreen}. Discovered by JEI via the "jei_mod_plugin" Fabric entrypoint in
 * fabric.mod.json and the {@link JeiPlugin} annotation on NeoForge.
 *
 * <p>Reports the tabs' current animated rects. JEI re-polls {@code getGuiExtraAreas} before every
 * frame of an open screen (its {@code GuiEventHandler.updateForScreenRender}, hooked to Fabric's
 * {@code ScreenEvents.beforeExtract} and NeoForge's {@code ScreenEvent.Render.Pre}) and relayouts
 * whenever the returned set changes, so the areas follow the animation without a stable envelope.
 */
@JeiPlugin
public class SideTabsJeiPlugin implements IModPlugin {

    private static final ResourceId PLUGIN_ID = LogisticsMod.modId("jei_side_tabs_plugin");

    @Override
    public net.minecraft.resources.Identifier getPluginUid() { // raw-id-ok: JEI IModPlugin signature
        return PLUGIN_ID.toIdentifier();
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(AbstractContainerScreen.class, new SideTabAreas());
    }

    private static final class SideTabAreas implements IGuiContainerHandler<AbstractContainerScreen<?>> {
        @Override
        public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> screen) {
            if (!(screen instanceof SideTabScreen tabScreen)) {
                return List.of();
            }
            List<TabRect> rects = tabScreen.sideTabs().exclusionAreas();
            List<Rect2i> areas = new ArrayList<>(rects.size());
            for (TabRect rect : rects) {
                areas.add(new Rect2i(rect.x(), rect.y(), rect.w(), rect.h()));
            }
            return areas;
        }
    }
}

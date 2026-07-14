package com.logistics.test.ui;

import com.logistics.core.lib.ui.Tab;
import com.logistics.core.lib.ui.TabbedContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * A test screen that demonstrates the TabbedContainerScreen framework.
 * It uses an existing StirlingEngineScreenHandler (which has 1 fuel slot).
 * This test involves adding a "Data" tab that shows different information.
 */
public class StirlingEngineTestScreen extends TabbedContainerScreen<StirlingEngineScreenHandler> {

    public StirlingEngineTestScreen(StirlingEngineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);

        // Tab 1: The "Fuel" view (shows the existing fuel slot)
        addTab(new Tab<>(
                "Fuel",
                null, 
                Tab.TabSide.LEFT,
                context -> {
                    // The fuel slot is index 0 in StirlingEngineScreenHandler.
                    context.addSlot(0, 10, 10);
                }
        ));

        // Tab 2: The "Energy" view (shows energy info)
        addTab(new Tab<>(
                "Energy",
                null,
                Tab.TabSide.RIGHT,
                context -> {
                    // Adding a button as an example of UI element injection
                    context.addButton("Clear Info", 10, 10, btn -> {
                        // This is a dummy action to test button interaction.
                    });
                }
        ));
    }

    @Override
    protected void init() {
        super.init();
        // For testing, let's set the Left tab as active by default.
        setActiveTab(Tab.TabSide.LEFT, getTabs().get(0));
    }

    @Override
    protected void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        // This is where we will implement the rendering of injected elements.
    }
}

package com.logistics.test.ui;

import com.logistics.core.lib.ui.Tab;
import com.logistics.core.lib.ui.TabbedContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class TestMachineScreen extends TabbedContainerScreen<TestMachineScreenHandler> {

    public TestMachineScreen(TestMachineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);

        // Tab 1: Input focus (Uses slots 0 and 1)
        addTab(new Tab<>(
                "Inputs",
                null, // No icon for test
                Tab.TabSide.LEFT,
                context -> {
                    context.addSlot(0, 10, 10);
                    context.addSlot(1, 10, 28);
                }
        ));

        // Tab 2: Output focus (Uses slots 2 and 3)
        addTab(new Tab<>(
                "Outputs",
                null, // No icon for test
                Tab.TabSide.RIGHT,
                context -> {
                    context.addSlot(2, 10, 10);
                    context.addSlot(3, 10, 28);
                }
        ));
    }

    @Override
    protected void init() {
        super.init();
        // Example of manual activation for testing purposes
        // In a real scenario, this would be triggered by a button or event.
        // setActiveTab(Tab.TabSide.LEFT, getTabs().get(0));
    }
}

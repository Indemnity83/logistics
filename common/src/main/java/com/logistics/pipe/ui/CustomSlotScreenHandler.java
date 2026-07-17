package com.logistics.pipe.ui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;

/**
 * Base menu for pipe UIs that manage custom (filter / ghost) slots by intercepting slot clicks.
 *
 * <p>Quarantines the sole cross-version divergence in the {@link #clicked} override signature so
 * every subclass stays byte-identical across branches. The click-action type is
 * {@code ContainerInput} on mc/26.x and {@code ClickType} on mc/1.21.x; this class is the only
 * place either name appears. Subclasses implement {@link #handleCustomSlotClick} with a
 * version-stable signature and never reference it.
 */
public abstract class CustomSlotScreenHandler extends AbstractContainerMenu {

    protected CustomSlotScreenHandler(MenuType<?> menuType, int syncId) {
        super(menuType, syncId);
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        if (handleCustomSlotClick(slotIndex, button, actionType == ClickType.QUICK_MOVE, player)) {
            return;
        }
        super.clicked(slotIndex, button, actionType, player);
    }

    /**
     * Handle a click on one of this menu's custom slots.
     *
     * @param quickMove whether this was a shift/quick-move click
     * @return {@code true} if the click was fully handled and vanilla handling should be skipped;
     *     {@code false} to fall through to {@link AbstractContainerMenu#clicked}
     */
    protected abstract boolean handleCustomSlotClick(int slotIndex, int button, boolean quickMove, Player player);
}

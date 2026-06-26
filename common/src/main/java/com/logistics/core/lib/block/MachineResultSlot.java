package com.logistics.core.lib.block;

import com.logistics.core.lib.block.capability.HasExperienceStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Output slot for a machine GUI: rejects insertion and, when a player takes the result, releases the
 * machine's banked processing experience at the player — the way a vanilla furnace result slot does.
 */
public class MachineResultSlot extends Slot {

    public MachineResultSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        if (player.level() instanceof ServerLevel level && this.container instanceof HasExperienceStorage xpStore) {
            int xp = xpStore.drainExperience(level.getRandom());
            if (xp > 0) {
                ExperienceOrb.award(level, player.position(), xp);
            }
        }
        super.onTake(player, stack);
    }
}

package com.logistics.pipe.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

/**
 * A requester sync payload is addressed to one open menu and must never be applied by another.
 *
 * <p>Regression cover for #931: the payload was sent to every player on the server and applied by
 * any open requester screen, replacing the viewer's item list with another network's contents and
 * repointing {@code pipePos} at another player's pipe.
 */
class RequesterSyncAddressingTest extends MinecraftTestEnvironment {

    private static final BlockPos OTHER_PLAYERS_PIPE = new BlockPos(100, 64, -250);

    private static RequesterScreenHandler clientMenu(int containerId) {
        return new RequesterScreenHandler(containerId, new SimpleContainer(36));
    }

    private static SyncRequesterInventoryPacket syncFor(int containerId) {
        return new SyncRequesterInventoryPacket(
                containerId, OTHER_PLAYERS_PIPE, List.of(new ItemStack(Items.DIAMOND, 7)), List.of(7L));
    }

    @Test
    void appliesSyncAddressedToThisMenu() {
        RequesterScreenHandler menu = clientMenu(3);

        assertThat(menu.applySync(syncFor(3))).isTrue();
        assertThat(menu.getPipePos()).isEqualTo(OTHER_PLAYERS_PIPE);
        assertThat(menu.getAllItems()).hasSize(1);
    }

    @Test
    void ignoresSyncAddressedToAnotherMenu() {
        RequesterScreenHandler menu = clientMenu(4);

        assertThat(menu.applySync(syncFor(3))).isFalse();
        assertThat(menu.getPipePos()).isEqualTo(BlockPos.ZERO);
        assertThat(menu.getAllItems()).isEmpty();
    }

    @Test
    void hasNoSyncTargetWhenNoServerPlayerOpenedIt() {
        assertThat(RequesterScreenHandler.viewerOf(new SimpleContainer(36))).isNull();
    }
}

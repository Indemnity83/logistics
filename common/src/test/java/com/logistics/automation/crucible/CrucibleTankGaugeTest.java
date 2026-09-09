package com.logistics.automation.crucible;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Crucible's tank gauge is scaled by the capacity the <em>server</em> published, not by the capacity
 * in the client's own config. The two agree in single player, where both sides read the same file; on a
 * multiplayer server whose {@code crucible.tank_capacity_mb} differs from a joining client's, scaling by
 * the client's value draws a full tank at the wrong height and never corrects itself.
 *
 * <p>Every case drives the divergence from the synced side and leaves the config alone, so no shared
 * config state leaks into later tests. Amounts stay under 32,767 mB so this stays a test about which
 * capacity is read, not about how a value survives {@code ContainerData}'s short-sized sync.
 */
@DisplayName("Crucible tank gauge")
class CrucibleTankGaugeTest extends MinecraftTestEnvironment {

    /** The client's own config value — never written to, so it stays the shipped default. */
    private static final int CLIENT_CAPACITY_MB =
            (int) (long) LogisticsConfigHost.get(LogisticsAutomation.CONFIG.CRUCIBLE_TANK_CAPACITY_MB);

    /** A client's view of the menu, fed exactly what a server with {@code capacityMb} would publish. */
    private static CrucibleScreenHandler menuSyncedWith(int amountMb, int capacityMb) {
        SimpleContainerData data = new SimpleContainerData(CrucibleBlockEntity.DATA_COUNT);
        data.set(CrucibleBlockEntity.DATA_FLUID_ID, 0);
        data.set(CrucibleBlockEntity.DATA_FLUID_AMOUNT, amountMb);
        data.set(CrucibleBlockEntity.DATA_FLUID_CAPACITY, capacityMb);
        return new CrucibleScreenHandler(0, new Inventory(null, new EntityEquipment()), new SimpleContainer(1), data);
    }

    @Test
    @DisplayName("a full tank reads full when the server's capacity is below the client's")
    void fullTankReadsFullBelowTheClientsCapacity() {
        int serverCapacity = 4_000;
        assertThat(serverCapacity).isLessThan(CLIENT_CAPACITY_MB);

        assertThat(menuSyncedWith(serverCapacity, serverCapacity).getTankFillFraction())
                .isEqualTo(1f, within(0.0001f));
        assertThat(menuSyncedWith(serverCapacity / 2, serverCapacity).getTankFillFraction())
                .isEqualTo(0.5f, within(0.0001f));
    }

    @Test
    @DisplayName("a half-full tank reads half when the server's capacity is above the client's")
    void halfTankReadsHalfAboveTheClientsCapacity() {
        int serverCapacity = 20_000;
        assertThat(serverCapacity).isGreaterThan(CLIENT_CAPACITY_MB);

        assertThat(menuSyncedWith(serverCapacity / 2, serverCapacity).getTankFillFraction())
                .isEqualTo(0.5f, within(0.0001f));
        assertThat(menuSyncedWith(serverCapacity, serverCapacity).getTankFillFraction())
                .isEqualTo(1f, within(0.0001f));
    }

    @Test
    @DisplayName("the reported capacity is the server's, not the client's config")
    void reportedCapacityIsTheServers() {
        assertThat(menuSyncedWith(0, 4_000).getTankCapacityMb()).isEqualTo(4_000);
        assertThat(menuSyncedWith(0, 20_000).getTankCapacityMb()).isEqualTo(20_000);
    }

    @Test
    @DisplayName("an empty or not-yet-synced tank reads empty rather than dividing by zero")
    void emptyAndUnsyncedTanksReadEmpty() {
        assertThat(menuSyncedWith(0, 4_000).getTankFillFraction()).isZero();
        assertThat(menuSyncedWith(1_000, 0).getTankFillFraction()).isZero();
    }
}

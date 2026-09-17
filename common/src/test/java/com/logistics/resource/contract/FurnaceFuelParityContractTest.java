package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsMod;
import com.logistics.core.lib.power.FurnaceFuels;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Fuel is an item component, so a fuel is declared once at registration and both loaders read the
 * same thing. What can still go wrong is a burn time listed in {@link FurnaceFuels} that never
 * reaches the item it names — so that is what this pins.
 */
@DisplayName("Furnace fuel components")
class FurnaceFuelParityContractTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("every shared burn time reaches its item as a cooking-fuel component")
    void burnTimesReachTheirItems() {
        DomainRegistrations.ensureRegistered();
        assertThat(FurnaceFuels.BURN_TIMES).isNotEmpty();
        FurnaceFuels.BURN_TIMES.forEach((path, burnTime) -> {
            Item item = BuiltInRegistries.ITEM.getValue(LogisticsMod.modId(path).toIdentifier());
            // An unknown id resolves to AIR rather than null, so "not null" would prove nothing.
            assertThat(item).as("registered item for %s", path).isNotSameAs(Items.AIR);

            CookingFuel fuel = item.components().get(DataComponents.COOKING_FUEL);
            assertThat(fuel).as("cooking-fuel component on %s", path).isNotNull();
            assertThat(fuel.burnTime())
                    .as("burn time on %s is a plain constant", path)
                    .isInstanceOf(ResolvableInt.Constant.class);
            assertThat(((ResolvableInt.Constant) fuel.burnTime()).value())
                    .as("burn time on %s", path)
                    .isEqualTo(burnTime);
        });
    }

    @Test
    @DisplayName("every shared burn time names a real item path")
    void sharedBurnTimesNameRealItems() {
        FurnaceFuels.BURN_TIMES.forEach((path, burnTime) -> {
            assertThat(path).as("item path").doesNotStartWith("logistics:").doesNotStartWith("/");
            assertThat(burnTime).as("burn time for %s", path).isPositive();
        });
    }
}

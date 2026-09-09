package com.logistics.pipe.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.modules.SupplierModule;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A Supplier ghost slot must edit the configuration it is showing.
 *
 * <p>Regression cover for #1087: clearing any but the last supply slot leaves a gap in the
 * configuration. The pipe menu compacted the surviving configs into the leading ghost slots, while
 * a click saves under the clicked ghost slot's own index — so an edit landed on a different entry
 * than the one on screen, duplicating the configuration and leaving a later clear undone.
 */
@DisplayName("Supplier ghost slots")
class SupplierSlotKeyingTest extends MinecraftTestEnvironment {

    private static final String COBBLESTONE = "minecraft:cobblestone";
    private static final String IRON_INGOT = "minecraft:iron_ingot";

    private SupplierModule module;
    private PipeContext ctx;

    /** A Supplier configured with two supplies whose first slot the player then cleared. */
    @BeforeEach
    void setUp() {
        module = new SupplierModule();
        ctx = new PipeContext(null, BlockPos.ZERO, null, new FakePipeAccess());
        module.setSupplyConfig(ctx, 0, COBBLESTONE, 32);
        module.setSupplyConfig(ctx, 1, IRON_INGOT, 500);
        module.setSupplyConfig(ctx, 0, "", 0);
    }

    @Test
    @DisplayName("show each supply in the slot it is configured for")
    void ghostSlotsFollowTheConfiguredSlot() {
        SupplyInventory inventory = new SupplyInventory(null);

        inventory.loadFromModule(module, ctx);

        assertThat(inventory.getItem(0).isEmpty()).isTrue();
        assertThat(inventory.getItem(1).getItem()).isEqualTo(Items.IRON_INGOT);
    }

    @Test
    @DisplayName("report the stock target configured for the slot that was clicked")
    void configuredTargetFollowsTheConfiguredSlot() {
        assertThat(module.getSupplyConfig(ctx, 1))
                .isEqualTo(new SupplierModule.SupplyConfig(IRON_INGOT, 500));
        assertThat(module.getSupplyConfig(ctx, 0)).isNull();
    }
}

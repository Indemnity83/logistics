package com.logistics.pipe.modules;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Simple Modules")
class SimpleModulesTest extends MinecraftTestEnvironment {

    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== VoidModule ====================

    @Test
    @DisplayName("VoidModule.route returns DISCARD regardless of item or options")
    void voidModule_route_returnsDiscard() {
        VoidModule module = new VoidModule();
        TravelingItem item = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.NORTH, 0.1f, null);

        RoutePlan plan = module.route(ctx, item, List.of(Direction.NORTH, Direction.SOUTH));

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.DISCARD);
    }

    @Test
    @DisplayName("VoidModule.route returns DISCARD even with empty options list")
    void voidModule_route_returnsDiscard_emptyOptions() {
        VoidModule module = new VoidModule();
        TravelingItem item = new TravelingItem(new ItemStack(Items.DIRT), Direction.UP, 0.1f, null);

        RoutePlan plan = module.route(ctx, item, List.of());

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.DISCARD);
    }

    // ==================== TerminusModule ====================

    @Test
    @DisplayName("TerminusModule.isDefaultRoute always returns false")
    void terminusModule_isDefaultRoute_alwaysFalse() {
        TerminusModule module = new TerminusModule(4);
        assertThat(module.isDefaultRoute(ctx)).isFalse();
    }

    @Test
    @DisplayName("TerminusModule.setDefaultRoute(true) has no effect — isDefaultRoute remains false")
    void terminusModule_setDefaultRoute_isNoOp() {
        TerminusModule module = new TerminusModule(4);
        module.setDefaultRoute(ctx, true);
        assertThat(module.isDefaultRoute(ctx)).isFalse();
    }

    @Test
    @DisplayName("TerminusModule inherits filter slot behavior from SinkModule")
    void terminusModule_inheritsFilterBehavior() {
        TerminusModule module = new TerminusModule(4);
        module.setFilter(ctx, 0, "minecraft:diamond");
        assertThat(module.getFilters(ctx).asList()).contains("minecraft:diamond");
    }

    // ==================== TransportModule ====================

    @Test
    @DisplayName("TransportModule.getMaxSpeed returns pipe.min_speed from config")
    void transportModule_getMaxSpeed_returnsConfiguredMinSpeed() {
        TransportModule module = new TransportModule();
        assertThat(module.getMaxSpeed(ctx)).isEqualTo(LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED));
    }

    @Test
    @DisplayName("TransportModule.getDrag returns pipe.drag from config")
    void transportModule_getDrag_returnsConfiguredDrag() {
        TransportModule module = new TransportModule();
        assertThat(module.getDrag(ctx)).isEqualTo(LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_DRAG));
    }

    @Test
    @DisplayName("TransportModule tracks pipe.min_speed and pipe.drag changed after it was constructed")
    void transportModule_tracksConfigReload() {
        // Built once at registration, exactly as PipeTypes builds the stone transport pipe's module.
        TransportModule module = new TransportModule();

        Config pipes = ConfigRegistry.config(LogisticsPipe.CONFIG.PIPE_MIN_SPEED.configId());
        float originalMinSpeed = LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED);
        float originalDrag = LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_DRAG);
        try {
            assertThat(pipes.trySet(LogisticsPipe.CONFIG.PIPE_MIN_SPEED, 0.05f)).isTrue();
            assertThat(pipes.trySet(LogisticsPipe.CONFIG.PIPE_DRAG, 0.02f)).isTrue();

            assertThat(module.getMaxSpeed(ctx)).isEqualTo(0.05f);
            assertThat(module.getDrag(ctx)).isEqualTo(0.02f);
        } finally {
            pipes.set(LogisticsPipe.CONFIG.PIPE_MIN_SPEED, originalMinSpeed);
            pipes.set(LogisticsPipe.CONFIG.PIPE_DRAG, originalDrag);
        }
    }

    // ==================== NetworkRouterModule ====================

    @Test
    @DisplayName("NetworkRouterModule.getAcceleration returns a positive constant")
    void networkRouterModule_getAcceleration_isPositive() {
        NetworkRouterModule module = new NetworkRouterModule();
        assertThat(module.getAcceleration(ctx)).isGreaterThan(0f);
    }

    @Test
    @DisplayName("NetworkRouterModule.getMaxSpeed returns pipe.injectSpeed from config")
    void networkRouterModule_getMaxSpeed_returnsNetworkSpeed() {
        NetworkRouterModule module = new NetworkRouterModule();
        assertThat(module.getMaxSpeed(ctx)).isEqualTo(LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_INJECT_SPEED));
    }
}

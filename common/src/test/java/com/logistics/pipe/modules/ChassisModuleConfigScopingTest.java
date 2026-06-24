package com.logistics.pipe.modules;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.IPipeAccess;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.ChassisPipe;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for issue #494: reconfiguring a module while it is installed in a chassis
 * pipe must write to the module's <em>scoped</em> state partition ({@code module.<uuid>.<statekey>}),
 * not the class-default key.
 *
 * <p>The defect was that most module config screens opened from {@code onWrench} did not thread the
 * scoped state key, so GUI edits landed on the class-default key while the module's own
 * routing/HUD logic reads through the scoped key — leaving the change invisible (a stale Jade HUD,
 * and potentially stale behavior).
 *
 * <p>The GUI wiring itself (screen handlers + companion inventories) needs a live {@code Level} and
 * menu harness to drive end-to-end, which this unit-test layer does not provide. These tests instead
 * lock the contract the fix depends on: once a module's scoped partition exists, config written
 * through the scoped context is what the module reads back, and a write to the class-default key
 * (the old, unthreaded path) does <em>not</em> leak into the scoped read. If this isolation ever
 * regresses, the chassis re-config bug returns.
 */
@DisplayName("Chassis module config scoping (#494)")
class ChassisModuleConfigScopingTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("Provider mode written via scoped context is isolated from a stale default-key write")
    void provider_modeScopedIsolatedFromDefaultKey() {
        ProviderModule module = new ProviderModule(8, 1);
        Scope scope = scopeFor(module);

        // Fixed GUI path: write through the scoped context. This also populates the scoped partition.
        module.setModeFromOrdinal(scope.scoped(), ProviderModule.ProviderMode.RESERVE.ordinal());
        // Old buggy path: write to the class-default key.
        module.setModeFromOrdinal(scope.base(), ProviderModule.ProviderMode.SAMPLE.ordinal());

        assertThat(module.getModeOrdinal(scope.scoped()))
                .isEqualTo(ProviderModule.ProviderMode.RESERVE.ordinal());
        // The stale default-key write is invisible to the scoped read.
        assertThat(module.getModeOrdinal(scope.base()))
                .isEqualTo(ProviderModule.ProviderMode.SAMPLE.ordinal());
    }

    @Test
    @DisplayName("Provider filter-inverted flag is scoped")
    void provider_filterInvertedScoped() {
        ProviderModule module = new ProviderModule(8, 1);
        Scope scope = scopeFor(module);

        module.setFilterInverted(scope.scoped(), true);
        module.setFilterInverted(scope.base(), false);

        assertThat(module.isFilterInverted(scope.scoped())).isTrue();
        assertThat(module.isFilterInverted(scope.base())).isFalse();
    }

    @Test
    @DisplayName("Sink filter slot is scoped")
    void sink_filterScoped() {
        SinkModule module = new SinkModule(7);
        Scope scope = scopeFor(module);

        module.setFilter(scope.scoped(), 0, "minecraft:iron_ingot");
        module.setFilter(scope.base(), 0, "minecraft:gold_ingot");

        assertThat(module.getFilters(scope.scoped()).get(0)).isEqualTo("minecraft:iron_ingot");
        assertThat(module.getFilters(scope.base()).get(0)).isEqualTo("minecraft:gold_ingot");
    }

    @Test
    @DisplayName("AdvancedExtractor filter-inverted flag is scoped")
    void advancedExtractor_filterInvertedScoped() {
        AdvancedExtractorModule module = new AdvancedExtractorModule(8, 20);
        Scope scope = scopeFor(module);

        module.setFilterInverted(scope.scoped(), true);
        module.setFilterInverted(scope.base(), false);

        assertThat(module.isFilterInverted(scope.scoped())).isTrue();
        assertThat(module.isFilterInverted(scope.base())).isFalse();
    }

    @Test
    @DisplayName("Satellite id is scoped")
    void satellite_idScoped() {
        SatelliteModule module = new SatelliteModule();
        Scope scope = scopeFor(module);

        module.setSatelliteId(scope.scoped(), "scoped-sat");
        module.setSatelliteId(scope.base(), "default-sat");

        assertThat(module.getSatelliteId(scope.scoped())).isEqualTo("scoped-sat");
        assertThat(module.getSatelliteId(scope.base())).isEqualTo("default-sat");
    }

    @Test
    @DisplayName("ItemFilter filter state is scoped")
    void itemFilter_filterStateScoped() {
        ItemFilterModule module = new ItemFilterModule();
        Scope scope = scopeFor(module);

        // The filter's on-disk format (ItemStack CODEC) needs a live world to encode, which this
        // layer lacks; assert the FILTERS compound is partitioned per scope at the raw-state level.
        CompoundTag scopedFilters = new CompoundTag();
        scopedFilters.putString("marker", "scoped");
        scope.scoped().putCompoundTag(module, ItemFilterModule.FILTERS, scopedFilters);

        CompoundTag baseFilters = new CompoundTag();
        baseFilters.putString("marker", "base");
        scope.base().putCompoundTag(module, ItemFilterModule.FILTERS, baseFilters);

        assertThat(NbtCompat.getString(
                        scope.scoped().getCompoundTag(module, ItemFilterModule.FILTERS), "marker", ""))
                .isEqualTo("scoped");
        assertThat(NbtCompat.getString(
                        scope.base().getCompoundTag(module, ItemFilterModule.FILTERS), "marker", ""))
                .isEqualTo("base");
    }

    // ==================== Helpers ====================

    /** A base context (class-default key) plus a scoped context for the same module instance. */
    private record Scope(PipeContext base, PipeContext scoped) {}

    private static Scope scopeFor(com.logistics.core.lib.pipe.Module module) {
        ItemStack stack = new ItemStack(Items.STICK);
        StatefulFakePipe pipe = new StatefulFakePipe();
        PipeContext base = new PipeContext(null, BlockPos.ZERO, null, pipe);
        String stateKey = ChassisPipe.moduleStateKey(stack, module);
        return new Scope(base, base.withModuleStateKey(module, stateKey));
    }

    /**
     * In-memory {@link IPipeAccess} that distinguishes between "create" and "already exists"
     * semantics. Implementing {@link #existingModuleState} faithfully is required: the scoped/legacy
     * migration in {@link PipeContext#moduleState(com.logistics.core.lib.pipe.Module)} only backfills
     * from the default key when the scoped partition does not yet exist.
     */
    private static class StatefulFakePipe implements IPipeAccess {
        private final Map<String, CompoundTag> states = new HashMap<>();

        @Override
        public CompoundTag moduleState(String key) {
            return states.computeIfAbsent(key, ignored -> new CompoundTag());
        }

        @Override
        public @Nullable CompoundTag existingModuleState(String key) {
            return states.get(key);
        }

        @Override
        public void clearModuleState(String key) {
            states.remove(key);
        }

        @Override
        public @Nullable EnergyComponent getEnergy() {
            return null;
        }

        @Override
        public void markDirty() {}

        @Override
        public PipeConnection.Type getCachedConnectionType(Direction direction) {
            return PipeConnection.Type.NONE;
        }

        @Override
        public PipeConnection.Type getConnectionType(Level world, BlockPos pos, Direction direction) {
            return PipeConnection.Type.NONE;
        }

        @Override
        public boolean isNeighborPipe(Level world, BlockPos pos, Direction direction) {
            return false;
        }

        @Override
        public boolean isPowered() {
            return false;
        }

        @Override
        public @Nullable ILogisticsNetwork getNetwork() {
            return null;
        }

        @Override
        public List<TravelingItem> getTravelingItems() {
            return List.of();
        }

        @Override
        public boolean forceAddItem(TravelingItem item, Direction fromDirection) {
            return false;
        }
    }
}

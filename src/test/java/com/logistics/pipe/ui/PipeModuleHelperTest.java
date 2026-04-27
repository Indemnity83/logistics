package com.logistics.pipe.ui;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.IPipeAccess;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.ChassisPipe;
import com.logistics.pipe.item.ModuleItem;
import com.logistics.pipe.modules.ExtractionModule;
import com.logistics.pipe.modules.SupplierModule;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PipeModuleHelper")
class PipeModuleHelperTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("scoped contexts keep duplicate supplier module state separate")
    void scopedContexts_keepDuplicateSupplierModuleStateSeparate() {
        SupplierModule first = new SupplierModule();
        SupplierModule second = new SupplierModule();
        ItemStack firstStack = new ItemStack(Items.STICK);
        ItemStack secondStack = new ItemStack(Items.STICK);
        FakePipeAccess pipe = new FakePipeAccess();
        PipeContext baseContext = new PipeContext(null, BlockPos.ZERO, null, pipe);
        String firstStateKey = ChassisPipe.moduleStateKey(firstStack, first);
        String secondStateKey = ChassisPipe.moduleStateKey(secondStack, second);

        PipeContext firstContext = baseContext.withModuleStateKey(first, firstStateKey);
        PipeContext secondContext = baseContext.withModuleStateKey(second, secondStateKey);

        first.setSupplyConfig(firstContext, 0, "minecraft:iron_ingot", 8);
        second.setSupplyConfig(secondContext, 0, "minecraft:gold_ingot", 16);

        assertThat(first.getSupplyConfigs(firstContext))
                .containsExactly(new SupplierModule.SupplyConfig("minecraft:iron_ingot", 8));
        assertThat(second.getSupplyConfigs(secondContext))
                .containsExactly(new SupplierModule.SupplyConfig("minecraft:gold_ingot", 16));
        assertThat(first.getSupplyConfigs(baseContext)).isEmpty();
        assertThat(firstStateKey).isNotEqualTo(secondStateKey);
        assertThat(ModuleItem.getModuleId(firstStack)).isNotBlank();
    }

    @Test
    @DisplayName("scoped contexts keep duplicate extraction module state separate")
    void scopedContexts_keepDuplicateExtractionModuleStateSeparate() {
        ExtractionModule first = new ExtractionModule();
        ExtractionModule second = new ExtractionModule();
        ItemStack firstStack = new ItemStack(Items.STICK);
        ItemStack secondStack = new ItemStack(Items.STICK);
        FakePipeAccess pipe = new FakePipeAccess();
        PipeContext baseContext = new PipeContext(null, BlockPos.ZERO, null, pipe);
        String firstStateKey = ChassisPipe.moduleStateKey(firstStack, first);
        String secondStateKey = ChassisPipe.moduleStateKey(secondStack, second);

        PipeContext firstContext = baseContext.withModuleStateKey(first, firstStateKey);
        PipeContext secondContext = baseContext.withModuleStateKey(second, secondStateKey);

        firstContext.saveInt(first, "extract_direction", 0);
        secondContext.saveInt(second, "extract_direction", 3);

        assertThat(firstContext.getInt(first, "extract_direction", -1)).isEqualTo(0);
        assertThat(secondContext.getInt(second, "extract_direction", -1)).isEqualTo(3);
        assertThat(firstStateKey).isNotEqualTo(secondStateKey);
        assertThat(ModuleItem.getModuleId(firstStack)).isNotBlank();
    }

    @Test
    @DisplayName("scoped context migrates legacy class-key state")
    void scopedContext_migratesLegacyClassKeyState() {
        SupplierModule supplier = new SupplierModule();
        ItemStack stack = new ItemStack(Items.STICK);
        FakePipeAccess pipe = new FakePipeAccess();
        PipeContext baseContext = new PipeContext(null, BlockPos.ZERO, null, pipe);
        supplier.setSupplyConfig(baseContext, 0, "minecraft:diamond", 4);
        String stateKey = ChassisPipe.moduleStateKey(stack, supplier);

        PipeContext scopedContext = baseContext.withModuleStateKey(supplier, stateKey);

        assertThat(supplier.getSupplyConfigs(scopedContext))
                .containsExactly(new SupplierModule.SupplyConfig("minecraft:diamond", 4));
        assertThat(pipe.existingModuleState(stateKey)).isNotNull();

        supplier.setSupplyConfig(scopedContext, 0, "", 0);

        assertThat(supplier.getSupplyConfigs(scopedContext)).isEmpty();
    }

    private static class FakePipeAccess implements IPipeAccess {
        private final Map<String, CompoundTag> states = new HashMap<>();

        @Override
        public CompoundTag moduleState(String key) {
            return states.computeIfAbsent(key, ignored -> new CompoundTag());
        }

        @Override
        public CompoundTag existingModuleState(String key) {
            return states.get(key);
        }

        @Override
        public void clearModuleState(String key) {
            states.remove(key);
        }

        @Override
        public EnergyComponent getEnergy() {
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
        public ILogisticsNetwork getNetwork() {
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

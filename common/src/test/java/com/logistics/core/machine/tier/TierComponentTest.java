package com.logistics.core.machine.tier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsCore;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.MachineHudModel;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class TierComponentTest extends MinecraftTestEnvironment {

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private int syncs;
    private final List<MachineTier> heard = new ArrayList<>();

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private TierComponent tier() {
        TierComponent component = new TierComponent("tier", () -> syncs++);
        component.onChange(heard::add);
        return component;
    }

    @Test
    void startsBasic() {
        assertThat(tier().tier()).isEqualTo(MachineTier.BASIC);
    }

    @Test
    void savesTheIdAsAStringAndNoNumber() {
        TierComponent component = tier();
        component.setTier(MachineTier.INDUSTRIAL);

        CompoundTag tag = new CompoundTag();
        component.save(tag, registries);

        assertThat(tag.get(TierComponent.NBT_KEY)).isInstanceOf(StringTag.class);
        assertThat(NbtCompat.getString(tag, TierComponent.NBT_KEY, "")).isEqualTo("industrial");
        assertThat(tag.keySet()).allSatisfy(key -> assertThat(tag.get(key)).isNotInstanceOf(NumericTag.class));
    }

    @Test
    void loadRoundTripsAndNotifiesWithoutSyncing() {
        TierComponent writer = tier();
        writer.setTier(MachineTier.PRECISION);
        CompoundTag tag = new CompoundTag();
        writer.save(tag, registries);
        syncs = 0;
        heard.clear();

        TierComponent reader = tier();
        reader.load(tag, registries);

        assertThat(reader.tier()).isEqualTo(MachineTier.PRECISION);
        assertThat(heard).containsExactly(MachineTier.PRECISION);
        assertThat(syncs).isZero();
    }

    @Test
    void unknownIdLoadsAsBasicWithoutThrowing() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TierComponent.NBT_KEY, "mythic");
        TierComponent component = tier();

        assertThatCode(() -> component.load(tag, registries)).doesNotThrowAnyException();
        assertThat(component.tier()).isEqualTo(MachineTier.BASIC);
    }

    @Test
    void missingKeyLoadsAsBasicWithoutThrowing() {
        TierComponent component = tier();

        assertThatCode(() -> component.load(new CompoundTag(), registries)).doesNotThrowAnyException();
        assertThat(component.tier()).isEqualTo(MachineTier.BASIC);
        assertThat(heard).isEmpty();
    }

    @Test
    void loadingBasicOverAHigherTierResetsIt() {
        TierComponent component = tier();
        component.setTier(MachineTier.INFERNAL);

        component.load(new CompoundTag(), registries);

        assertThat(component.tier()).isEqualTo(MachineTier.BASIC);
    }

    @Test
    void setTierFiresListenersAndSyncsOnce() {
        TierComponent component = tier();

        assertThat(component.setTier(MachineTier.INDUSTRIAL)).isTrue();

        assertThat(heard).containsExactly(MachineTier.INDUSTRIAL);
        assertThat(syncs).isEqualTo(1);
    }

    @Test
    void settingTheSameTierIsANoOp() {
        TierComponent component = tier();
        component.setTier(MachineTier.INDUSTRIAL);
        heard.clear();
        syncs = 0;

        assertThat(component.setTier(MachineTier.INDUSTRIAL)).isFalse();
        assertThat(component.setTier(MachineTier.INDUSTRIAL)).isFalse();

        assertThat(heard).isEmpty();
        assertThat(syncs).isZero();
    }

    @Test
    @Disabled("The tier HUD line is hidden while nothing can change a tier")
    void contributesATierHudLine() {
        TierComponent component = tier();
        component.setTier(MachineTier.PRECISION);
        MachineHudModel hud = new MachineHudModel();

        component.contributeHud(hud);
        CompoundTag data = new CompoundTag();
        hud.save(data);

        assertThat(MachineHudModel.entries(data))
                .containsExactly(new MachineHudModel.LineEntry(
                        TierComponent.HUD_TIER, "tier.logistics.precision", List.of()));
    }

    @Test
    void itemComponentRoundTrips() {
        TierComponent writer = tier();
        writer.setTier(MachineTier.INFERNAL);
        DataComponentMap.Builder builder = DataComponentMap.builder();
        writer.addItemComponents(builder);
        DataComponentMap map = builder.build();

        assertThat(map.get(LogisticsCore.DATA.MACHINE_TIER)).isEqualTo(MachineTier.INFERNAL);

        TierComponent reader = tier();
        reader.readItemComponents(map::get);
        assertThat(reader.tier()).isEqualTo(MachineTier.INFERNAL);
    }

    @Test
    void basicTierLeavesTheItemUntagged() {
        DataComponentMap.Builder builder = DataComponentMap.builder();
        tier().addItemComponents(builder);

        assertThat(builder.build().has(LogisticsCore.DATA.MACHINE_TIER)).isFalse();
    }

    @Test
    void itemWithoutATierLeavesTheMachineUnchanged() {
        TierComponent reader = tier();
        reader.setTier(MachineTier.INDUSTRIAL);
        heard.clear();

        reader.readItemComponents(DataComponentMap.EMPTY::get);

        assertThat(reader.tier()).isEqualTo(MachineTier.INDUSTRIAL);
        assertThat(heard).isEmpty();
    }

    /** A machine shell on a vanilla BE type, so only the data component needs registering. */
    private static final class TieredTestMachine extends MachineEntity implements TieredMachine {
        private TierComponent tier;

        TieredTestMachine() {
            super(BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        }

        @Override
        protected void configure(MachineBuilder machine) {
            tier = machine.tier("tier");
        }

        @Override
        public TierComponent tierComponent() {
            return tier;
        }

        @Override
        public MenuProvider createMenuProvider() {
            return null;
        }
    }

    @Test
    void machineEntityCarriesItsTierThroughItsItemForm() {
        TieredTestMachine broken = new TieredTestMachine();
        broken.tierComponent().setTier(MachineTier.PRECISION);

        DataComponentMap drop = broken.collectComponents();
        assertThat(drop.get(LogisticsCore.DATA.MACHINE_TIER)).isEqualTo(MachineTier.PRECISION);

        TieredTestMachine placed = new TieredTestMachine();
        placed.applyComponents(drop, DataComponentPatch.EMPTY);

        assertThat(placed.tier()).isEqualTo(MachineTier.PRECISION);
        // Consumed as an implicit component, not kept as a stray component on the block entity.
        assertThat(placed.components().has(LogisticsCore.DATA.MACHINE_TIER)).isFalse();
    }
}

package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.machine.area.RingArea;
import com.logistics.core.machine.control.MachineControls;
import com.logistics.core.machine.control.RedstoneMode;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A placed farm only surveys: it changes nothing and spends nothing until Start, and what its
 * survey reports is exactly what working the ring then clears and skips.
 */
@DisplayName("Farm survey and Start")
class FarmSurveyTest extends MinecraftTestEnvironment {

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** Lays dirt under any plot whose soil is not dirt, and wants nothing else. */
    private static ScriptedFarmLogic dirtLayer() {
        ScriptedFarmLogic logic = new ScriptedFarmLogic(true, true).script((world, plot) ->
                world.state(plot.below()).is(Blocks.DIRT) ? PlotAction.NONE : new PlotAction.PlaceSoil(plot.below()));
        logic.clearanceHeight = 3;
        return logic;
    }

    private static final BlockState PERSISTENT_LEAVES =
            Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

    /** What the fixture holds, by kind. */
    private record Fixture(Set<BlockPos> natural, Set<BlockPos> flagged, Set<BlockPos> protectedBlocks) {}

    /**
     * Over the 16-plot ring between 3×3 and 5×5: natural blocks (short grass, stone soil, a water
     * source), flagged ones (planks, persistent leaves, glass soil, cobblestone over a chest),
     * protected ones (a chest, a never-clear block in a column and as soil) and the farm's own
     * wheat, which is not counted. Every other plot stands on dirt.
     */
    private static Fixture fixture(FarmHarness farm) {
        FakeFarmWorld world = farm.world;
        world.natural = state -> state.is(Blocks.SHORT_GRASS) || state.is(Blocks.STONE) || state.is(Blocks.DIRT)
                || state.is(Blocks.WATER) || state.is(Blocks.OAK_LEAVES);
        world.neverClear = state -> state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN);
        RingArea ring = farm.area();
        for (int i = 0; i < ring.plotCount(); i++) {
            world.set(ring.plot(i).below(), Blocks.DIRT.defaultBlockState());
        }
        Set<BlockPos> natural = new HashSet<>();
        Set<BlockPos> flagged = new HashSet<>();
        Set<BlockPos> skipped = new HashSet<>();

        place(world, natural, ring.plot(0), Blocks.SHORT_GRASS.defaultBlockState());
        place(world, flagged, ring.plot(1).above(1), Blocks.OAK_PLANKS.defaultBlockState());
        place(world, flagged, ring.plot(2).above(2), Blocks.COBBLESTONE.defaultBlockState());
        place(world, skipped, ring.plot(2), Blocks.CHEST.defaultBlockState());
        place(world, skipped, ring.plot(3).above(1), Blocks.OBSIDIAN.defaultBlockState());
        place(world, natural, ring.plot(4).below(), Blocks.STONE.defaultBlockState());
        place(world, flagged, ring.plot(5).below(), Blocks.GLASS.defaultBlockState());
        place(world, skipped, ring.plot(6).below(), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        world.set(ring.plot(7), Blocks.WHEAT.defaultBlockState());
        place(world, flagged, ring.plot(8).above(2), PERSISTENT_LEAVES);
        place(world, natural, ring.plot(9).above(1), Blocks.OAK_LEAVES.defaultBlockState());
        place(world, natural, ring.plot(10), Blocks.WATER.defaultBlockState());
        return new Fixture(natural, flagged, skipped);
    }

    private static void place(FakeFarmWorld world, Set<BlockPos> kind, BlockPos pos, BlockState state) {
        world.set(pos, state);
        kind.add(pos.immutable());
    }

    private static FarmHarness surveying() {
        FarmHarness farm = FarmHarness.surveying(dirtLayer(), FarmHarness.SETTINGS);
        farm.areaOverride(3, 5);
        return farm;
    }

    private static void stock(FarmHarness farm) {
        farm.power(10_000);
        farm.machine.water().tank().insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(4_000), false);
        farm.items().setItem(FarmMachine.GERMLING_START, new ItemStack(Items.WHEAT_SEEDS, 10));
        farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.DIRT, 32));
    }

    private static List<ItemStack> items(FarmHarness farm) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < FarmMachine.SLOT_COUNT; slot++) {
            stacks.add(farm.items().getItem(slot).copy());
        }
        return stacks;
    }

    private static boolean sameItems(List<ItemStack> a, List<ItemStack> b) {
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.matches(a.get(i), b.get(i))) {
                return false;
            }
        }
        return a.size() == b.size();
    }

    // ==================== Surveying ====================

    @Test
    @DisplayName("a placed farm surveys: over many ticks it changes nothing and spends nothing")
    void surveyChangesAndSpendsNothing() {
        FarmHarness farm = surveying();
        fixture(farm);
        stock(farm);
        Map<BlockPos, BlockState> before = new HashMap<>(farm.world.blocks);
        long water = farm.machine.water().tank().getAmount();
        List<ItemStack> stacks = items(farm);

        farm.ticks(500);

        assertThat(farm.farm().state()).isEqualTo(FarmState.SURVEY);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.SURVEYING);
        assertThat(farm.world.mutations).as("no break or write").isEmpty();
        assertThat(farm.world.blocks).isEqualTo(before);
        assertThat(farm.energy()).isEqualTo(10_000);
        assertThat(farm.machine.water().tank().getAmount()).isEqualTo(water);
        assertThat(sameItems(stacks, items(farm))).as("inventory unchanged").isTrue();
        assertThat(farm.farm().harvestActions() + farm.farm().cultivateActions()).isZero();
        assertThat(farm.host.lit()).as("its buffer holds RF").isTrue();
    }

    @Test
    @DisplayName("the survey inspects one plot per check and backs off after each pass, as working does")
    void surveyPaceAndBackOff() {
        FarmHarness farm = surveying();
        fixture(farm);
        int plots = farm.area().plotCount();

        farm.ticks(plots - 1);
        long readsMidPass = farm.world.readCount;
        farm.tick();
        long readsAfterPass = farm.world.readCount;
        farm.ticks(10);

        assertThat(readsAfterPass).isGreaterThan(readsMidPass);
        assertThat(farm.world.readCount).as("backed off after a full pass").isEqualTo(readsAfterPass);
    }

    @Test
    @DisplayName("the survey sorts what working would clear into natural and flagged, and what it skips into protected")
    void surveyCounts() {
        FarmHarness farm = surveying();
        Fixture fixture = fixture(farm);

        farm.ticks(farm.area().plotCount());

        FarmSurvey survey = farm.farm().survey();
        assertThat(survey.natural()).isEqualTo(fixture.natural().size());
        assertThat(survey.flagged()).isEqualTo(fixture.flagged().size());
        assertThat(survey.toClear()).isEqualTo(fixture.natural().size() + fixture.flagged().size());
        assertThat(survey.protectedCount()).isEqualTo(fixture.protectedBlocks().size());
        assertThat(survey.flaggedAt()).containsExactlyInAnyOrderElementsOf(fixture.flagged());
        assertThat(survey.protectedAt()).containsExactlyInAnyOrderElementsOf(fixture.protectedBlocks());
        assertThat(farm.farm().plotsBlocked()).as("the chest's and both never-clear plots").isEqualTo(3);
    }

    @Test
    @DisplayName("after Start the farm clears exactly what the survey listed, and skips exactly what it protected")
    void surveyMatchesWhatWorkingClears() {
        FarmHarness farm = surveying();
        Fixture fixture = fixture(farm);
        stock(farm);
        farm.ticks(farm.area().plotCount());
        FarmSurvey survey = farm.farm().survey();

        assertThat(farm.farm().start()).isTrue();
        farm.ticks(400);

        Set<BlockPos> cleared = new HashSet<>();
        for (FakeFarmWorld.Mutation m : farm.world.mutations) {
            boolean removed = m.after() == null || (m.after().isAir() && !m.before().isAir());
            if (removed) {
                cleared.add(m.pos());
            }
        }
        Set<BlockPos> expected = new HashSet<>(fixture.natural());
        expected.addAll(fixture.flagged());
        assertThat(cleared).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(cleared).hasSize(survey.toClear());
        Set<BlockPos> flaggedCleared = new HashSet<>(cleared);
        flaggedCleared.retainAll(new HashSet<>(survey.flaggedAt()));
        assertThat(flaggedCleared).hasSize(survey.flagged());
        for (BlockPos pos : survey.protectedAt()) {
            assertThat(farm.world.blocks).as("protected %s stays", pos).containsKey(pos);
            assertThat(farm.world.mutations).noneMatch(m -> m.pos().equals(pos));
        }
        assertThat(farm.world.blocks.get(farm.area().plot(7))).as("own growth is never cleared").isEqualTo(Blocks.WHEAT.defaultBlockState());
    }

    // ==================== Start and Show area ====================

    @Test
    @DisplayName("Start works the ring once; pressed again it does nothing")
    void startOnlyOnce() {
        FarmHarness farm = surveying();
        assertThat(farm.farm().start()).isTrue();
        assertThat(farm.farm().state()).isEqualTo(FarmState.ACTIVE);
        assertThat(farm.farm().start()).isFalse();
        assertThat(farm.farm().state()).isEqualTo(FarmState.ACTIVE);
    }

    @Test
    @DisplayName("a surveying farm shows its preview until hidden; Start hides it; once working the toggle shows it until toggled off")
    void toggleArea() {
        FarmHarness farm = surveying();
        fixture(farm);

        assertThat(farm.farm().previewShown(100)).as("placed: shown").isTrue();
        assertThat(farm.farm().toggleArea(farm.host, 100)).isTrue();
        assertThat(farm.farm().previewShown(100)).as("hidden while surveying").isFalse();
        farm.ticks(farm.area().plotCount());
        assertThat(farm.farm().survey().flagged()).as("the survey keeps running").isEqualTo(4);
        farm.farm().toggleArea(farm.host, 100);
        assertThat(farm.farm().previewShown(100)).isTrue();

        farm.farm().start();
        assertThat(farm.farm().previewShown(100)).as("Start hides it").isFalse();

        assertThat(farm.farm().toggleArea(farm.host, 100)).isTrue();
        assertThat(farm.farm().previewShown(100 + 10 * FarmPreview.SHOW_TICKS)).as("no timeout").isTrue();
        assertThat(farm.farm().survey().flagged()).as("the ring surveyed as it stands").isEqualTo(4);
        farm.farm().toggleArea(farm.host, 200);
        assertThat(farm.farm().previewShown(200)).isFalse();
        assertThat(farm.world.mutations).isEmpty();
    }

    @Test
    @DisplayName("toggling off cuts an upgrade's flash short; with the toggle off the flash still shows for 30 s")
    void toggleCancelsUpgradeFlash() {
        FarmHarness farm = new FarmHarness(dirtLayer());
        farm.tick();
        assertThat(farm.farm().previewShown(1_000)).isFalse();

        farm.farm().expanded(farm.host, 1_000);
        assertThat(farm.farm().previewShown(1_000 + FarmPreview.SHOW_TICKS - 1)).isTrue();
        assertThat(farm.farm().previewShown(1_000 + FarmPreview.SHOW_TICKS)).isFalse();

        farm.farm().toggleArea(farm.host, 1_100);
        assertThat(farm.farm().previewShown(1_100)).as("hidden at once").isFalse();
        assertThat(farm.farm().previewUntil()).isZero();
    }

    @Test
    @DisplayName("on load a working farm's preview starts hidden and a surveying farm's shown")
    void toggleOnLoad() {
        FarmHarness working = new FarmHarness(dirtLayer());
        working.farm().toggleArea(working.host, 0);
        assertThat(working.farm().previewShown(0)).isTrue();
        working.load(working.save());
        assertThat(working.farm().previewShown(0)).isFalse();

        FarmHarness surveying = surveying();
        surveying.farm().toggleArea(surveying.host, 0);
        assertThat(surveying.farm().previewShown(0)).isFalse();
        surveying.load(surveying.save());
        assertThat(surveying.farm().previewShown(0)).isTrue();
    }

    @Test
    @DisplayName("the menu's buttons start the farm and show its area; unknown ids do nothing")
    void menuButtons() {
        FarmHarness farm = surveying();
        long[] now = {200};
        FarmMenu.Buttons buttons = new FarmMenu.Buttons() {
            @Override
            public boolean start() {
                return farm.farm().start();
            }

            @Override
            public boolean toggleArea() {
                return farm.farm().toggleArea(farm.host, now[0]);
            }
        };
        var data = FarmMenu.dataSource(farm.machine, () -> farm.farm().previewShown(now[0]));
        FarmMenu menu = new FarmMenu(
                1, new Inventory(null, new EntityEquipment()), new SimpleContainer(FarmMachine.SLOT_COUNT), data, buttons,
                MachineControls.client());

        assertThat(menu.clickMenuButton(null, -1)).isFalse();
        assertThat(menu.clickMenuButton(null, 99)).isFalse();
        assertThat(menu.clickMenuButton(null, MachineControls.redstoneButton(RedstoneMode.HIGH)))
                .as("a redstone button is the controls', never a farm button").isFalse();
        assertThat(farm.farm().state()).isEqualTo(FarmState.SURVEY);
        assertThat(menu.previewShown()).as("surveying: shown").isTrue();

        assertThat(menu.clickMenuButton(null, FarmMenu.BUTTON_SHOW_AREA)).isTrue();
        assertThat(menu.previewShown()).as("hidden while surveying").isFalse();
        assertThat(farm.farm().state()).isEqualTo(FarmState.SURVEY);
        assertThat(menu.clickMenuButton(null, FarmMenu.BUTTON_SHOW_AREA)).isTrue();
        assertThat(menu.previewShown()).isTrue();

        assertThat(menu.clickMenuButton(null, FarmMenu.BUTTON_START)).isTrue();
        assertThat(farm.farm().state()).isEqualTo(FarmState.ACTIVE);
        assertThat(menu.previewShown()).as("Start hides it").isFalse();
        assertThat(menu.clickMenuButton(null, FarmMenu.BUTTON_START)).isFalse();

        assertThat(menu.clickMenuButton(null, FarmMenu.BUTTON_SHOW_AREA)).isTrue();
        assertThat(menu.previewShown()).as("toggled on").isTrue();
        now[0] += 10 * FarmPreview.SHOW_TICKS;
        assertThat(menu.previewShown()).as("until toggled off").isTrue();
        assertThat(menu.clickMenuButton(null, FarmMenu.BUTTON_SHOW_AREA)).isTrue();
        assertThat(menu.previewShown()).as("toggled off").isFalse();
    }

    @Test
    @DisplayName("the menu syncs the state and the survey counts")
    void menuSyncsSurvey() {
        FarmHarness farm = surveying();
        Fixture fixture = fixture(farm);
        farm.ticks(farm.area().plotCount());
        var data = FarmMenu.dataSource(farm.machine, () -> false);

        assertThat(data.get(FarmMenu.STATE)).isEqualTo(FarmState.SURVEY.ordinal());
        assertThat(data.get(FarmMenu.SURVEY_CLEAR)).isEqualTo(fixture.natural().size() + fixture.flagged().size());
        assertThat(data.get(FarmMenu.SURVEY_FLAGGED)).isEqualTo(fixture.flagged().size());
        assertThat(data.get(FarmMenu.SURVEY_PROTECTED)).isEqualTo(fixture.protectedBlocks().size());

        farm.farm().start();
        SimpleContainerData client = new SimpleContainerData(FarmMenu.DATA_COUNT);
        client.set(FarmMenu.STATE, data.get(FarmMenu.STATE));
        FarmMenu menu = new FarmMenu(
                1, new Inventory(null, new EntityEquipment()), new SimpleContainer(FarmMachine.SLOT_COUNT), client);
        assertThat(menu.state()).isEqualTo(FarmState.ACTIVE);
        client.set(FarmMenu.STATE, 7);
        assertThat(menu.state()).as("an unknown state reads as surveying").isEqualTo(FarmState.SURVEY);
    }

    // ==================== Upgrades ====================

    @Test
    @DisplayName("after an upgrade only the new lanes are surveyed, the preview shows, and they are worked at once")
    void upgradeCountsOnlyNewLanes() {
        FarmSettings tiered = FarmHarness.SETTINGS.withTiers(FarmHarness.shippedTiersAtTestRate());
        FarmHarness farm = new FarmHarness(dirtLayer(), tiered);
        farm.areaOverride(0, 0);
        RingArea basic = farm.area();
        BlockPos oldLane = basic.plot(0);
        farm.world.set(oldLane, Blocks.COBBLESTONE.defaultBlockState());
        farm.tick(); // works the Basic ring, unpowered: nothing changes

        farm.machine.tier().setTier(MachineTier.INDUSTRIAL);
        RingArea industrial = farm.area();
        BlockPos newPlanks = industrial.plot(basic.plotCount());
        BlockPos newGrass = industrial.plot(industrial.plotCount() - 1);
        BlockPos newChest = industrial.plot(basic.plotCount() + 5);
        farm.world.natural = state -> state.is(Blocks.SHORT_GRASS) || state.is(Blocks.DIRT);
        farm.world.set(newPlanks, Blocks.OAK_PLANKS.defaultBlockState());
        farm.world.set(newGrass, Blocks.SHORT_GRASS.defaultBlockState());
        farm.world.set(newChest, Blocks.CHEST.defaultBlockState());
        for (int i = basic.plotCount(); i < industrial.plotCount(); i++) {
            farm.world.set(industrial.plot(i).below(), Blocks.DIRT.defaultBlockState());
        }

        FarmSurvey lanes = farm.farm().expanded(farm.host, 1_000);

        assertThat(lanes.toClear()).isEqualTo(2);
        assertThat(lanes.flagged()).isEqualTo(1);
        assertThat(lanes.flaggedAt()).containsExactly(newPlanks);
        assertThat(lanes.protectedAt()).containsExactly(newChest);
        assertThat(lanes.flaggedAt()).doesNotContain(oldLane);
        assertThat(farm.farm().previewUntil()).isEqualTo(1_000 + FarmPreview.SHOW_TICKS);
        assertThat(farm.world.mutations).as("surveying the lanes changes nothing").isEmpty();

        farm.power(10_000);
        farm.ticks(industrial.plotCount() * 2);
        assertThat(farm.world.blocks).as("the new lanes are worked without confirmation")
                .doesNotContainKeys(newPlanks, newGrass, oldLane)
                .containsKey(newChest);
    }

    // ==================== Persistence and sync ====================

    private static CompoundTag farmTag(CompoundTag saved) {
        return saved.getCompoundOrEmpty("components").getCompoundOrEmpty("farm");
    }

    @Test
    @DisplayName("the state is saved as a string, and a missing or unknown one loads as surveying")
    void statePersistence() {
        FarmHarness farm = surveying();
        assertThat(farmTag(farm.save()).getStringOr("State", "")).isEqualTo("survey");

        farm.farm().start();
        CompoundTag saved = farm.save();
        assertThat(farmTag(saved).getStringOr("State", "")).isEqualTo("active");
        FarmHarness reloaded = surveying();
        reloaded.load(saved);
        assertThat(reloaded.farm().state()).isEqualTo(FarmState.ACTIVE);

        farmTag(saved).remove("State");
        FarmHarness missing = new FarmHarness(dirtLayer());
        missing.load(saved);
        assertThat(missing.farm().state()).isEqualTo(FarmState.SURVEY);

        farmTag(saved).putString("State", "dormant");
        FarmHarness unknown = new FarmHarness(dirtLayer());
        unknown.load(saved);
        assertThat(unknown.farm().state()).isEqualTo(FarmState.SURVEY);
        assertThat(FarmState.fromId("ACTIVE")).isEqualTo(FarmState.SURVEY);
    }

    @Test
    @DisplayName("the update tag carries the preview, with positions only while it is visible; a save never does")
    void updateTagCarriesPreview() {
        FarmHarness farm = surveying();
        Fixture fixture = fixture(farm);
        farm.ticks(farm.area().plotCount());

        assertThat(farmTag(farm.save()).contains("Preview")).as("never saved").isFalse();

        CompoundTag surveying = farm.farm().withPreview(farm.host, 50, farm::save);
        FarmHarness client = surveying();
        client.load(surveying);
        FarmPreview preview = client.farm().syncedPreview();
        assertThat(preview.state()).isEqualTo(FarmState.SURVEY);
        assertThat(preview.inner()).isEqualTo(3);
        assertThat(preview.outer()).isEqualTo(5);
        assertThat(preview.height()).isEqualTo(3);
        assertThat(preview.visible(50)).isTrue();
        assertThat(preview.survey()).isEqualTo(farm.farm().survey());
        assertThat(preview.survey().flaggedAt()).containsExactlyInAnyOrderElementsOf(fixture.flagged());

        farm.farm().start();
        CompoundTag working = farm.farm().withPreview(farm.host, 50, farm::save);
        client.load(working);
        preview = client.farm().syncedPreview();
        assertThat(preview.visible(50)).isFalse();
        assertThat(preview.survey().flaggedAt()).isEmpty();
        assertThat(preview.survey().protectedAt()).isEmpty();

        farm.farm().toggleArea(farm.host, 60);
        client.load(farm.farm().withPreview(farm.host, 61, farm::save));
        preview = client.farm().syncedPreview();
        assertThat(preview.shown()).isTrue();
        assertThat(preview.visible(61)).isTrue();
        assertThat(preview.visible(60 + 10 * FarmPreview.SHOW_TICKS)).as("until toggled off").isTrue();

        farm.farm().toggleArea(farm.host, 70);
        client.load(farm.farm().withPreview(farm.host, 71, farm::save));
        assertThat(client.farm().syncedPreview().visible(71)).isFalse();
        assertThat(preview.survey().flaggedAt()).containsExactlyInAnyOrderElementsOf(fixture.flagged());

        client.load(farm.save());
        assertThat(client.farm().syncedPreview()).isEqualTo(FarmPreview.NONE);
    }

    @Test
    @DisplayName("the survey lists at most 256 positions of each kind, and counts on past them")
    void surveyListIsCapped() {
        FarmSurvey survey = new FarmSurvey();
        for (int i = 0; i < 300; i++) {
            survey.cleared(new BlockPos(i, 0, 0), ClearClassifier.Kind.FLAGGED);
            survey.skipped(new BlockPos(i, 1, 0));
        }
        assertThat(survey.flagged()).isEqualTo(300);
        assertThat(survey.flaggedAt()).hasSize(FarmSurvey.MAX_LISTED);
        assertThat(survey.protectedCount()).isEqualTo(300);
        assertThat(survey.protectedAt()).hasSize(FarmSurvey.MAX_LISTED);
    }
}

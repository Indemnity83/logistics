package com.logistics.automation.jei;

import com.logistics.automation.alloysmelter.AlloySmelterRecipe;
import com.logistics.automation.crucible.CrucibleRecipe;
import com.logistics.automation.fabricator.FabricatorRecipe;
import com.logistics.automation.macerator.MaceratorRecipeWrapper;
import com.logistics.automation.refinery.RefineryRecipe;
import com.logistics.automation.sawmill.SawmillRecipe;
import com.logistics.automation.transposer.TransposerRecipe;
import com.logistics.core.lib.jei.MachineRecipeJeiSync;
import com.logistics.core.lib.jei.SyncMachineRecipesPacket;
import java.util.List;

/**
 * Client-side cache of machine recipes synced from the server via {@link SyncMachineRecipesPacket}.
 * The JEI plugins read these lists in {@code registerRecipes}; getters return empty until the join
 * packet arrives, so singleplayer and multiplayer share the same code path.
 */
public final class ClientMachineRecipes {

    private static volatile List<MaceratorRecipeWrapper> macerator = List.of();
    private static volatile List<SawmillRecipe> sawmill = List.of();
    private static volatile List<CrucibleRecipe> crucible = List.of();
    private static volatile List<AlloySmelterRecipe> alloySmelter = List.of();
    private static volatile List<RefineryRecipe> refinery = List.of();
    private static volatile List<FabricatorRecipe> fabricator = List.of();
    private static volatile List<TransposerRecipe> transposer = List.of();

    private ClientMachineRecipes() {}

    public static void set(SyncMachineRecipesPacket packet) {
        macerator = List.copyOf(packet.macerator());
        sawmill = List.copyOf(packet.sawmill());
        crucible = List.copyOf(packet.crucible());
        alloySmelter = List.copyOf(packet.alloySmelter());
        refinery = List.copyOf(packet.refinery());
        fabricator = List.copyOf(packet.fabricator());
        transposer = List.copyOf(packet.transposer());
        MachineRecipeJeiSync.pushToJei(AutomationJeiSyncAdapter.INSTANCE);
        MachineRecipeJeiSync.pushToJei(AutomationJeiSyncAdapter.INSTANCE);
    }

    /** Drops cached recipes on disconnect so a later server's sync starts clean. */
    public static void clear() {
        macerator = List.of();
        sawmill = List.of();
        crucible = List.of();
        alloySmelter = List.of();
        refinery = List.of();
        fabricator = List.of();
        transposer = List.of();
    }

    public static List<MaceratorRecipeWrapper> macerator() {
        return macerator;
    }

    public static List<SawmillRecipe> sawmill() {
        return sawmill;
    }

    public static List<CrucibleRecipe> crucible() {
        return crucible;
    }

    public static List<AlloySmelterRecipe> alloySmelter() {
        return alloySmelter;
    }

    public static List<RefineryRecipe> refinery() {
        return refinery;
    }

    public static List<FabricatorRecipe> fabricator() {
        return fabricator;
    }

    public static List<TransposerRecipe> transposer() {
        return transposer;
    }

}

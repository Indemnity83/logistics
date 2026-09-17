package com.logistics.power.engine.magmatic.jei;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.jei.JeiRecipeSyncAdapter;
import com.logistics.core.lib.jei.MachineRecipeJeiSync;
import com.logistics.power.engine.magmatic.MagmaticEngineProfile;
import java.util.List;
import mezz.jei.api.recipe.IRecipeManager;
import net.minecraft.world.level.material.Fluids;

/**
 * Keeps the Magmatic Engine's JEI row showing the numbers the server actually runs on.
 *
 * <p>JEI builds its categories at client startup, before a server's config has arrived, so the row registered
 * then is built from local values. {@link #rebuild()} swaps it for one built from whatever
 * {@link LogisticsConfigHost} now resolves to -- the server's values after a join, the local ones again after
 * a disconnect.
 */
public final class MagmaticJeiSyncAdapter implements JeiRecipeSyncAdapter {

    public static final MagmaticJeiSyncAdapter INSTANCE = new MagmaticJeiSyncAdapter();

    private volatile List<MagmaticFuelDisplay> displays = List.of();

    private MagmaticJeiSyncAdapter() {}

    /** The rows to register at JEI startup, built from the config in effect right now. */
    public List<MagmaticFuelDisplay> initial() {
        displays = List.of(build());
        return displays;
    }

    /** Replace the registered row with one built from the config in effect now. */
    public void rebuild() {
        MachineRecipeJeiSync.hideFromJei(this);
        displays = List.of(build());
        MachineRecipeJeiSync.pushToJei(this);
    }

    /**
     * {@code hideRecipes} matches by equality and {@code addRecipes} does not un-hide, so a row whose rebuilt
     * value equals one hidden earlier -- a server whose config matches the client's, or a disconnect returning
     * to the values a join replaced -- would stay hidden without the explicit un-hide.
     */
    @Override
    public void pushToJei(IRecipeManager recipeManager) {
        recipeManager.addRecipes(MagmaticRecipeCategory.RECIPE_TYPE, displays);
        recipeManager.unhideRecipes(MagmaticRecipeCategory.RECIPE_TYPE, displays);
    }

    @Override
    public void hideFromJei(IRecipeManager recipeManager) {
        recipeManager.hideRecipes(MagmaticRecipeCategory.RECIPE_TYPE, displays);
    }

    /** Builds the single lava row from the same profile the engine runs on. */
    private static MagmaticFuelDisplay build() {
        MagmaticEngineProfile profile = MagmaticEngineProfile.of(
            LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_OUTPUT),
            LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_BUFFER_CAPACITY),
            Math.toIntExact(LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_TANK_CAPACITY)),
            Math.toIntExact(LogisticsConfigHost.get(LogisticsPower.CONFIG.MAGMATIC_BUCKET_BURN_TICKS)));
        return new MagmaticFuelDisplay(
            Fluids.LAVA,
            profile.batchMb(),
            profile.batchBurnTicks(),
            profile.coldOutputPerTick(),
            profile.warmOutputPerTick(),
            profile.hotOutputPerTick());
    }
}

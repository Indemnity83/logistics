package com.logistics.automation.fabricator;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.RecipeProcessPlan;
import com.logistics.core.machine.component.RecipeProcessorComponent.LitController;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

/**
 * Drives the Sequential Fabricator: from a player-built selection queue of fabricator recipes it
 * builds the active one (spending a fixed RF/t toward its cost), ejects the product, then advances
 * to the next selected recipe whose ingredients are available — cycling round-robin.
 *
 * <p>Only recipes whose ingredients are currently available in the material pool can be active; a
 * selected-but-unavailable recipe is skipped until its materials return. Selection queue, active
 * recipe, and progress persist across saves.
 */
public final class FabricatorProcessorComponent
        implements MachineComponent, MachineComponent.ProcessState {

    /** Output state for the GUI: 0 = craftable (unselected), 1 = queued, 2 = active. */
    public static final int STATE_AVAILABLE = 0;
    public static final int STATE_QUEUED = 1;
    public static final int STATE_ACTIVE = 2;

    /** One craftable output shown in the GUI. */
    public record Output(ResourceId id, ItemStack result, int state) {}

    private final String id;
    private final ItemStoreComponent items;
    private final EnergyStorageComponent energy;
    private final long rfPerTick;
    private final LitController lit;
    private final Runnable onChanged;

    private final List<ResourceId> selected = new ArrayList<>();
    @Nullable private ResourceId activeId;
    private long energySpent;
    private long activeEnergyRequired;

    // Fabricator recipes are cached per RecipeManager instance (rebuilt on datapack reload).
    @Nullable private RecipeManager cachedManager;
    private List<RecipeHolder<FabricatorRecipe>> cachedRecipes = List.of();
    @Nullable private Map<ResourceId, RecipeHolder<FabricatorRecipe>> cachedById;

    public FabricatorProcessorComponent(
            String id,
            ItemStoreComponent items,
            EnergyStorageComponent energy,
            long rfPerTick,
            LitController lit,
            Runnable onChanged) {
        this.id = id;
        this.items = items;
        this.energy = energy;
        this.rfPerTick = rfPerTick;
        this.lit = lit;
        this.onChanged = onChanged;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        RecipeManager rm = ctx.recipeManager();
        if (rm == null) {
            goIdle(ctx);
            return;
        }
        Map<ResourceId, RecipeHolder<FabricatorRecipe>> byId = recipesById(rm);
        pruneSelection(byId.keySet());

        List<ItemStack> pool = pool();
        ResourceId target = (activeId != null && isCraftable(byId, activeId, pool))
                ? activeId
                : findNext(activeId, byId, pool);
        if (target == null) {
            goIdle(ctx);
            return;
        }
        if (!target.equals(activeId)) {
            activeId = target;
            energySpent = 0;
        }

        FabricatorRecipe recipe = byId.get(activeId).value();
        activeEnergyRequired = recipe.energy();

        RecipeProcessPlan.Result result =
                RecipeProcessPlan.advance(energySpent, recipe.energy(), energy.amount(), rfPerTick, true);
        if (!result.consumedEnergy()) {
            lit.setLit(ctx, false);
            return;
        }
        energy.consume(rfPerTick);
        energySpent = result.energySpent();
        lit.setLit(ctx, true);
        onChanged.run();

        if (result.complete()) {
            completeRun(recipe, byId, ctx);
        }
    }

    private void completeRun(
            FabricatorRecipe recipe, Map<ResourceId, RecipeHolder<FabricatorRecipe>> byId, MachineContext ctx) {
        consumeIngredients(recipe);
        if (ctx.level() instanceof ServerLevel serverLevel) {
            NeighborItemOutput.eject(serverLevel, ctx.pos(), recipe.getResultItem());
        }
        energySpent = 0;
        // Advance strictly past the finished recipe so surplus materials still cycle the queue.
        activeId = findNext(activeId, byId, pool());
        onChanged.run();
    }

    /** Removes and re-adds are a toggle; clears progress if the currently-building recipe is dropped. */
    public void toggle(ResourceId recipeId) {
        if (selected.remove(recipeId)) {
            if (recipeId.equals(activeId)) {
                activeId = null;
                energySpent = 0;
            }
        } else {
            selected.add(recipeId);
        }
        onChanged.run();
    }

    /** Every fabricator recipe currently craftable from the pool, tagged with its selection state. */
    public List<Output> outputs(RecipeManager rm) {
        List<ItemStack> pool = pool();
        List<Output> out = new ArrayList<>();
        for (RecipeHolder<FabricatorRecipe> holder : fabricatorRecipes(rm)) {
            if (holder.value().canCraftFrom(pool)) {
                ResourceId rid = ResourceId.wrap(holder.id());
                int state = rid.equals(activeId) ? STATE_ACTIVE : (selected.contains(rid) ? STATE_QUEUED : STATE_AVAILABLE);
                out.add(new Output(rid, holder.value().getResultItem(), state));
            }
        }
        return out;
    }

    // ----- selection / cycling helpers -----

    @Nullable
    private ResourceId findNext(
            @Nullable ResourceId after, Map<ResourceId, RecipeHolder<FabricatorRecipe>> byId, List<ItemStack> pool) {
        if (selected.isEmpty()) {
            return null;
        }
        int start = after != null ? selected.indexOf(after) : -1;
        int size = selected.size();
        for (int step = 1; step <= size; step++) {
            ResourceId candidate = selected.get(Math.floorMod(start + step, size));
            if (isCraftable(byId, candidate, pool)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isCraftable(
            Map<ResourceId, RecipeHolder<FabricatorRecipe>> byId, ResourceId recipeId, List<ItemStack> pool) {
        RecipeHolder<FabricatorRecipe> holder = byId.get(recipeId);
        return holder != null && holder.value().canCraftFrom(pool);
    }

    /** Consume ingredient counts from the input slots via the recipe's shared greedy allocation. */
    private void consumeIngredients(FabricatorRecipe recipe) {
        recipe.allocateFrom(pool(), (slot, amount) -> items.consumeInput(slot, amount));
    }

    private List<ItemStack> pool() {
        List<ItemStack> pool = new ArrayList<>(items.inputCount());
        for (int i = 0; i < items.inputCount(); i++) {
            pool.add(items.input(i));
        }
        return pool;
    }

    private void pruneSelection(Set<ResourceId> known) {
        selected.removeIf(rid -> !known.contains(rid));
        if (activeId != null && !known.contains(activeId)) {
            activeId = null;
            energySpent = 0;
        }
    }

    private void goIdle(MachineContext ctx) {
        if (activeId != null || energySpent != 0) {
            activeId = null;
            energySpent = 0;
            onChanged.run();
        }
        activeEnergyRequired = 0;
        lit.setLit(ctx, false);
    }

    /** Id-keyed view of {@link #fabricatorRecipes}, cached alongside it per RecipeManager instance. */
    private Map<ResourceId, RecipeHolder<FabricatorRecipe>> recipesById(RecipeManager rm) {
        List<RecipeHolder<FabricatorRecipe>> recipes = fabricatorRecipes(rm);
        if (cachedById == null) {
            Map<ResourceId, RecipeHolder<FabricatorRecipe>> byId = new LinkedHashMap<>();
            for (RecipeHolder<FabricatorRecipe> holder : recipes) {
                byId.put(ResourceId.wrap(holder.id()), holder);
            }
            cachedById = byId;
        }
        return cachedById;
    }

    @SuppressWarnings("unchecked")
    private List<RecipeHolder<FabricatorRecipe>> fabricatorRecipes(RecipeManager rm) {
        if (rm == cachedManager) {
            return cachedRecipes;
        }
        List<RecipeHolder<FabricatorRecipe>> found = new ArrayList<>();
        for (RecipeHolder<?> holder : rm.getRecipes()) {
            if (holder.value() instanceof FabricatorRecipe) {
                found.add((RecipeHolder<FabricatorRecipe>) holder);
            }
        }
        cachedManager = rm;
        cachedRecipes = List.copyOf(found);
        cachedById = null;
        return cachedRecipes;
    }

    // ----- ProcessState / HUD -----

    @Override
    public float progress() {
        if (activeId == null || activeEnergyRequired <= 0) {
            return 0f;
        }
        return Math.min(1f, (float) energySpent / activeEnergyRequired);
    }

    @Override
    public boolean isProcessing() {
        return activeId != null;
    }

    // ----- persistence -----

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (!selected.isEmpty()) {
            ListTag list = new ListTag();
            for (ResourceId rid : selected) {
                list.add(StringTag.valueOf(rid.toString()));
            }
            tag.put("selected", list);
        }
        if (activeId != null) {
            tag.putString("active", activeId.toString());
        }
        if (energySpent > 0) {
            tag.putLong("energySpent", energySpent);
        }
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        selected.clear();
        ListTag list = NbtCompat.getListOrEmpty(tag, "selected");
        for (int i = 0; i < list.size(); i++) {
            ResourceId rid = ResourceId.tryParse(NbtCompat.getStringAt(list, i, ""));
            if (rid != null) {
                selected.add(rid);
            }
        }
        String active = NbtCompat.getString(tag, "active", "");
        activeId = active.isEmpty() ? null : ResourceId.tryParse(active);
        energySpent = NbtCompat.getLong(tag, "energySpent", 0L);
        // activeEnergyRequired is recomputed on the next tick.
    }
}

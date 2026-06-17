package com.logistics.pipe;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.PipeHud;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.pipe.CoreDecoration;
import com.logistics.core.lib.pipe.IModuleHost;
import com.logistics.core.lib.pipe.ModularPipe;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.RandomTickModule;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base for a module-composed pipe definition. Holds the payload-agnostic surface shared by
 * item and fluid pipes: the module list, module lookup, connection filtering, render/cosmetic hooks,
 * item-component round-trip, creative variants, comparator, and use/wrench dispatch.
 *
 * <p>Item transport lives in {@link ItemPipe}; fluid policy lives in
 * {@link FluidPipe}. Cosmetic methods iterate {@link #getModules(PipeContext)},
 * which subclasses override (e.g. chassis pipes inject runtime-installed modules) so inherited base
 * methods see the right module list via virtual dispatch.
 */
public abstract class Pipe implements ModularPipe {
    private final List<Module> modules;
    private Block pipeBlock;
    private boolean hasEnergy = false;

    protected Pipe(Module... modules) {
        this.modules = List.of(modules);
    }

    protected List<Module> getStaticModules() {
        return modules;
    }

    /**
     * Marks this pipe as having energy storage capability.
     * Only pipes with this flag will have energy storage created.
     */
    public Pipe withEnergy() {
        this.hasEnergy = true;
        return this;
    }

    /**
     * Returns whether this pipe has energy storage capability.
     */
    public boolean hasEnergy() {
        return hasEnergy;
    }

    /**
     * Called by the pipe block during registration to establish a back-reference.
     * This allows the pipe to derive model identifiers from the block's registry name.
     */
    public void setPipeBlock(Block block) {
        this.pipeBlock = block;
    }

    /**
     * Get the registry path of this pipe (e.g., "copper_transport_pipe").
     */
    public String getPipeName() {
        if (pipeBlock == null) {
            throw new IllegalStateException("Pipe has not been registered yet");
        }
        return BuiltInRegistries.BLOCK.getKey(pipeBlock).getPath();
    }

    /**
     * Returns this pipe's modules for the given context. Base implementation returns the static list;
     * {@link ItemPipe} overrides it to include chassis-installed dynamic modules.
     */
    protected List<Module> getModules(PipeContext ctx) {
        return modules;
    }

    /**
     * Get the model identifier for the core part of this pipe.
     * Delegates to modules first to allow state-dependent overrides (e.g., powered gold pipe).
     */
    public ResourceId getCoreModelId(PipeContext ctx) {
        for (Module module : getModules(ctx)) {
            ResourceId override = module.getCoreModel(ctx);
            if (override != null) {
                return override;
            }
        }
        return LogisticsMod.modId("block/" + getPipeName() + "_core");
    }

    /**
     * Collect core decoration models from pipe modules.
     */
    public List<CoreDecoration> getCoreDecorations(PipeContext ctx) {
        List<CoreDecoration> models = new ArrayList<>();
        for (Module module : getModules(ctx)) {
            models.addAll(module.getCoreDecorations(ctx));
        }
        return models;
    }

    /**
     * Get the arm model identifier for the given direction.
     * Delegates to modules first to allow them to override with custom models (like feature faces).
     * Falls back to the base arm model if no module provides an override.
     * The returned model is NORTH-facing and should be rotated at render time.
     */
    public ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        for (Module module : getModules(ctx)) {
            ResourceId override = module.getPipeArm(ctx, direction);
            if (override != null) {
                return override;
            }
        }

        String suffix = ctx.isInventoryConnection(direction) ? "_arm_extended" : "_arm";
        return LogisticsMod.modId("block/" + getPipeName() + suffix);
    }

    /**
     * Get decoration model identifiers for an arm in the given direction.
     */
    public List<ResourceId> getPipeDecorations(PipeContext ctx, Direction direction) {
        List<ResourceId> models = new ArrayList<>();
        for (Module module : getModules(ctx)) {
            models.addAll(module.getPipeDecorations(ctx, direction));
        }
        return models;
    }

    /**
     * Get the tint color for the arm in the given direction.
     */
    @Nullable public Integer getArmTint(PipeContext ctx, Direction direction) {
        for (Module module : getModules(ctx)) {
            Integer tint = module.getArmTint(ctx, direction);
            if (tint != null) {
                return tint;
            }
        }
        return null;
    }

    /**
     * Get the tint color for the pipe core.
     */
    @Nullable public Integer getCoreTint(PipeContext ctx) {
        for (Module module : getModules(ctx)) {
            Integer tint = module.getCoreTint(ctx);
            if (tint != null) {
                return tint;
            }
        }
        return null;
    }

    public boolean hasRandomTicks() {
        for (Module module : modules) {
            if (module instanceof RandomTickModule) {
                return true;
            }
        }
        return false;
    }

    public void randomTick(PipeContext ctx, RandomSource random) {
        for (Module module : getModules(ctx)) {
            if (module instanceof RandomTickModule randomTick) {
                randomTick.randomTick(ctx, random);
            }
        }
    }

    // --- Item component helpers ---

    /**
     * Add item components from all modules when the block is broken.
     * Also adds custom model data component if any module provides model data strings.
     */
    public void addItemComponents(DataComponentMap.Builder builder, PipeContext ctx) {
        for (Module module : getModules(ctx)) {
            module.addItemComponents(builder, ctx);
        }

        // Aggregate custom model data strings from all modules
        List<String> modelStrings = new ArrayList<>();
        for (Module module : getModules(ctx)) {
            modelStrings.addAll(module.getCustomModelDataStrings(ctx));
        }
        if (!modelStrings.isEmpty()) {
            builder.set(
                    DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(List.of(), List.of(), modelStrings, List.of()));
        }
    }

    /**
     * Read item components into all modules when the block is placed.
     */
    public void readItemComponents(DataComponentGetter components, PipeContext ctx) {
        for (Module module : getModules(ctx)) {
            module.readItemComponents(components, ctx);
        }
    }

    /**
     * Get the item name suffix from the first module that provides one.
     */
    public String getItemNameSuffix(PipeContext ctx) {
        for (Module module : getModules(ctx)) {
            String suffix = module.getItemNameSuffix(ctx);
            if (!suffix.isEmpty()) {
                return suffix;
            }
        }
        return "";
    }

    /**
     * Get the item name suffix from item components.
     * Used for item display names when we don't have a block context.
     */
    public String getItemNameSuffixFromComponents(DataComponentGetter components) {
        for (Module module : modules) {
            String suffix = module.getItemNameSuffixFromComponents(components);
            if (!suffix.isEmpty()) {
                return suffix;
            }
        }
        return "";
    }

    /**
     * Append creative menu variants from all modules.
     */
    public void appendCreativeMenuVariants(List<ItemStack> stacks, ItemStack baseStack) {
        for (Module module : modules) {
            module.appendCreativeMenuVariants(stacks, baseStack);
        }
    }

    /**
     * Core overlay model with an optional tint color.
     */
    public String getModelBasePath(Direction direction) {
        return "block/" + getPipeName() + "_" + direction.name().toLowerCase();
    }

    protected <T extends Module> T getModule(Class<T> moduleClass) {
        for (Module module : modules) {
            if (moduleClass.isInstance(module)) {
                return moduleClass.cast(module);
            }
        }
        return null;
    }

    /**
     * {@link ModularPipe} entry point. Base implementation scans the static module list; {@link ItemPipe}
     * overrides it to consult chassis-installed modules when the host is a pipe block entity.
     */
    @Override
    public <T extends Module> T getModule(Class<T> moduleClass, IModuleHost host) {
        return getModule(moduleClass);
    }

    /**
     * Appends this pipe's module status to a look-at HUD (Jade).
     */
    public void appendHud(PipeContext ctx, PipeHud hud) {
        for (Module module : getStaticModules()) {
            module.appendHud(ctx, hud);
        }
    }

    public InteractionResult onUseWithItem(PipeContext ctx, net.minecraft.world.item.context.UseOnContext usage) {
        for (Module module : getModules(ctx)) {
            InteractionResult result = module.onUseWithItem(ctx, usage);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onUseWithoutItem(PipeContext ctx, net.minecraft.world.item.context.UseOnContext usage) {
        for (Module module : getModules(ctx)) {
            InteractionResult result = module.onUseWithoutItem(ctx, usage);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onWrench(PipeContext ctx, Player player) {
        for (Module module : getModules(ctx)) {
            InteractionResult result = module.onWrench(ctx, player);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    public boolean hasComparatorOutput() {
        for (Module module : modules) {
            if (module.hasComparatorOutput()) {
                return true;
            }
        }
        return false;
    }

    public int getComparatorOutput(PipeContext ctx) {
        int output = 0;
        for (Module module : getModules(ctx)) {
            output = Math.max(output, module.comparatorOutput(ctx));
        }
        return output;
    }

    public void randomDisplayTick(PipeContext ctx, RandomSource random) {
        for (Module module : getModules(ctx)) {
            module.randomDisplayTick(ctx, random);
        }
    }

    public PipeConnection.Type filterConnection(
            @Nullable PipeContext ctx, Direction direction, Block neighborBlock, PipeConnection.Type candidate) {
        if (candidate == PipeConnection.Type.NONE) {
            return PipeConnection.Type.NONE;
        }
        for (Module module : (ctx != null ? getModules(ctx) : modules)) {
            if (!module.allowsConnection(ctx, direction, neighborBlock)) {
                return PipeConnection.Type.NONE;
            }
        }
        return candidate;
    }
}

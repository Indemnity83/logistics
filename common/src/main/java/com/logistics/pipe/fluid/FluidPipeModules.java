package com.logistics.pipe.fluid;

import com.logistics.core.lib.pipe.CoreDecoration;
import com.logistics.core.lib.pipe.IModuleHost;
import com.logistics.core.lib.pipe.ModularPipe;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.RandomTickModule;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.fluid.block.FluidPipeKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

/**
 * The fluid-pipe analogue of {@code Pipe}'s module composition: a fixed list of payload-agnostic
 * {@link Module}s a fluid pipe hosts for cross-cutting cosmetic/connection behavior (weathering,
 * marking, …). Fluid transport stays in {@code FluidPipeBlockEntity}; this only exposes the slice of
 * the module contract that has nothing to do with item transport.
 *
 * <p>Named {@code FluidPipeModules} to avoid colliding with {@link FluidPipe}, the parcel-budget helper.
 */
public final class FluidPipeModules implements ModularPipe {

    private final List<Module> modules;

    private FluidPipeModules(Module... modules) {
        this.modules = List.of(modules);
    }

    /**
     * The modules a given fluid pipe kind composes. Returns an empty set for every kind today — the
     * weathering (#517) and marking (#518) features attach their modules here on top of this foundation,
     * e.g. {@code new WeatheringModule(LogisticsFluid::model, kind.modelBase())} for copper.
     */
    public static FluidPipeModules forKind(FluidPipeKind kind) {
        return new FluidPipeModules();
    }

    public List<Module> modules() {
        return modules;
    }

    @Override
    @Nullable
    public <T extends Module> T getModule(Class<T> moduleClass, IModuleHost host) {
        for (Module module : modules) {
            if (moduleClass.isInstance(module)) {
                return moduleClass.cast(module);
            }
        }
        return null;
    }

    // ==================== Tick / interaction dispatch ====================

    public boolean hasRandomTicks() {
        for (Module module : modules) {
            if (module instanceof RandomTickModule) {
                return true;
            }
        }
        return false;
    }

    public void randomTick(PipeContext ctx, RandomSource random) {
        for (Module module : modules) {
            if (module instanceof RandomTickModule randomTick) {
                randomTick.randomTick(ctx, random);
            }
        }
    }

    public InteractionResult onUseWithItem(PipeContext ctx, UseOnContext usage) {
        for (Module module : modules) {
            InteractionResult result = module.onUseWithItem(ctx, usage);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onUseWithoutItem(PipeContext ctx, UseOnContext usage) {
        for (Module module : modules) {
            InteractionResult result = module.onUseWithoutItem(ctx, usage);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    // ==================== Rendering hooks ====================

    /** The first module-provided core model override, or {@code null} to use the kind's default core. */
    @Nullable
    public ResourceId coreModelOverride(PipeContext ctx) {
        for (Module module : modules) {
            ResourceId override = module.getCoreModel(ctx);
            if (override != null) {
                return override;
            }
        }
        return null;
    }

    /** The first module-provided arm model override for {@code direction}, or {@code null} for the default. */
    @Nullable
    public ResourceId armModelOverride(PipeContext ctx, Direction direction) {
        for (Module module : modules) {
            ResourceId override = module.getPipeArm(ctx, direction);
            if (override != null) {
                return override;
            }
        }
        return null;
    }

    public List<CoreDecoration> getCoreDecorations(PipeContext ctx) {
        List<CoreDecoration> models = new ArrayList<>();
        for (Module module : modules) {
            models.addAll(module.getCoreDecorations(ctx));
        }
        return models;
    }

    public List<ResourceId> getPipeDecorations(PipeContext ctx, Direction direction) {
        List<ResourceId> models = new ArrayList<>();
        for (Module module : modules) {
            models.addAll(module.getPipeDecorations(ctx, direction));
        }
        return models;
    }

    @Nullable
    public Integer getArmTint(PipeContext ctx, Direction direction) {
        for (Module module : modules) {
            Integer tint = module.getArmTint(ctx, direction);
            if (tint != null) {
                return tint;
            }
        }
        return null;
    }

    @Nullable
    public Integer getCoreTint(PipeContext ctx) {
        for (Module module : modules) {
            Integer tint = module.getCoreTint(ctx);
            if (tint != null) {
                return tint;
            }
        }
        return null;
    }

    // ==================== Item component round-trip ====================

    public void addItemComponents(DataComponentMap.Builder builder, PipeContext ctx) {
        for (Module module : modules) {
            module.addItemComponents(builder, ctx);
        }

        List<String> modelStrings = new ArrayList<>();
        for (Module module : modules) {
            modelStrings.addAll(module.getCustomModelDataStrings(ctx));
        }
        if (!modelStrings.isEmpty()) {
            builder.set(
                    DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(List.of(), List.of(), modelStrings, List.of()));
        }
    }

    public void readItemComponents(DataComponentGetter components, PipeContext ctx) {
        for (Module module : modules) {
            module.readItemComponents(components, ctx);
        }
    }

    public String getItemNameSuffixFromComponents(DataComponentGetter components) {
        for (Module module : modules) {
            String suffix = module.getItemNameSuffixFromComponents(components);
            if (!suffix.isEmpty()) {
                return suffix;
            }
        }
        return "";
    }

    public void appendCreativeMenuVariants(List<ItemStack> stacks, ItemStack baseStack) {
        for (Module module : modules) {
            module.appendCreativeMenuVariants(stacks, baseStack);
        }
    }
}

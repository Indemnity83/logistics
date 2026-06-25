package com.logistics.core.machine.component;

import com.logistics.core.machine.MachineContext;
import org.jetbrains.annotations.Nullable;

/**
 * Translates a machine's current inputs into an executable {@link RecipePlan}, or {@code null}
 * when nothing can be processed. Implementations isolate the version-specific recipe lookup
 * (e.g. {@code RecipeManager.getRecipeFor}) so the processor itself stays loader-agnostic.
 */
@FunctionalInterface
public interface RecipeResolver {

    @Nullable
    RecipePlan resolve(ProcessIO io, MachineContext ctx);
}

package com.logistics.pipe.ui;

import com.logistics.pipe.Pipe;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.pipe.Module;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Helper utility for screen handlers that need to access pipe modules.
 * Simplifies the common pattern of retrieving a module from a pipe block entity.
 */
public class PipeModuleHelper {

    /**
     * Execute an action with a pipe module and context.
     * Handles all the boilerplate of retrieving the block entity, pipe, and module.
     *
     * @param context The container level access
     * @param moduleClass The module class to retrieve
     * @param action The action to perform with the context and module
     * @param <T> The module type
     */
    public static <T extends Module> void withModule(
        ContainerLevelAccess context,
        Class<T> moduleClass,
        BiConsumer<PipeContext, T> action
    ) {
        withModule(context, moduleClass, null, action);
    }

    public static <T extends Module> void withModule(
        ContainerLevelAccess context,
        Class<T> moduleClass,
        @Nullable String stateKey,
        BiConsumer<PipeContext, T> action
    ) {
        context.execute((world, pos) -> {
            if (world.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity) {
                T module = getModule(pipeEntity, moduleClass, stateKey);

                if (module != null) {
                    PipeContext ctx = pipeEntity.createContext();
                    if (stateKey != null) {
                        ctx = ctx.withModuleStateKey(module, stateKey);
                    }
                    action.accept(ctx, module);
                }
            }
        });
    }

    /**
     * Execute an action with a pipe module and context (direct pipe entity access).
     * Handles all the boilerplate of retrieving the pipe and module.
     *
     * @param pipeEntity The pipe block entity
     * @param moduleClass The module class to retrieve
     * @param action The action to perform with the context and module
     * @param <T> The module type
     */
    public static <T extends Module> void withModule(
        PipeBlockEntity pipeEntity,
        Class<T> moduleClass,
        BiConsumer<PipeContext, T> action
    ) {
        withModule(pipeEntity, moduleClass, null, action);
    }

    public static <T extends Module> void withModule(
        PipeBlockEntity pipeEntity,
        Class<T> moduleClass,
        @Nullable String stateKey,
        BiConsumer<PipeContext, T> action
    ) {
        if (pipeEntity != null) {
            T module = getModule(pipeEntity, moduleClass, stateKey);

            if (module != null) {
                PipeContext ctx = pipeEntity.createContext();
                if (stateKey != null) {
                    ctx = ctx.withModuleStateKey(module, stateKey);
                }
                action.accept(ctx, module);
            }
        }
    }

    @Nullable
    public static <T extends Module> T getModule(
        PipeBlockEntity pipeEntity,
        Class<T> moduleClass,
        @Nullable String stateKey
    ) {
        PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
        Pipe pipe = block.getPipe();
        return pipe.getModule(moduleClass, pipeEntity, stateKey);
    }
}

package com.logistics.pipe.ui;

import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.Module;
import net.minecraft.world.inventory.ContainerLevelAccess;

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
     * @param action The action to perform with the module and context
     * @param <T> The module type
     */
    public static <T extends Module> void withModule(
        ContainerLevelAccess context,
        Class<T> moduleClass,
        BiConsumer<T, PipeContext> action
    ) {
        context.execute((world, pos) -> {
            if (world.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity) {
                PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
                Pipe pipe = block.getPipe();
                T module = pipe.getModule(moduleClass);

                if (module != null) {
                    PipeContext ctx = pipeEntity.createContext();
                    action.accept(module, ctx);
                }
            }
        });
    }
}

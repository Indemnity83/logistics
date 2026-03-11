package com.logistics.pipe.item;

import com.logistics.pipe.modules.Module;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * An item that carries chassis module behavior.
 * When placed in a chassis slot, {@link #createModule()} is called to instantiate
 * the module that will be active for that slot.
 */
public class ModuleItem extends Item {
    private final Supplier<? extends Module> factory;

    public ModuleItem(Properties props, Supplier<? extends Module> factory) {
        super(props);
        this.factory = factory;
    }

    public Module createModule() {
        return factory.get();
    }
}

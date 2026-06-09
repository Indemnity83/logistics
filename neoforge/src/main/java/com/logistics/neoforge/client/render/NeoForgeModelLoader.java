package com.logistics.neoforge.client.render;
import com.logistics.core.lib.resource.ResourceId;

import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Registers the NeoForge cable model geometry loader.
 *
 * <p>On mc/1.21.1 cables still render through a model geometry loader (they were not converted to
 * code rendering like pipes and machines), so this remains after the render-in-code work dropped the
 * rest of the extra-model loading ({@code ClientModelRegistry} / {@code FabricModelLoader}).
 */
public final class NeoForgeModelLoader {
    private NeoForgeModelLoader() {}

    /**
     * Subscribes to {@link ModelEvent.RegisterGeometryLoaders} to register the cable model geometry loader.
     */
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(
                ResourceId.in("logistics", "cable_model").toIdentifier(),
                NeoForgeCableBlockModelDefinition.INSTANCE);
    }
}

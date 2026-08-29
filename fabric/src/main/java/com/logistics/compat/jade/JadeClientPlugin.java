package com.logistics.compat.jade;

import snownee.jade.api.IWailaClientRegistration;

/**
 * Client half of the Jade integration, reached from {@link JadeLogisticsPlugin} through {@link
 * java.util.ServiceLoader}. The implementation and its {@code META-INF/services} entry live in the client
 * source set, so a dedicated server (where those entries are stripped) simply finds no services.
 *
 * <p>The seam exists because Jade's tooltip API is client-only ({@code ITooltip} extends a
 * {@code net.minecraft.client} type), so component providers cannot compile in the common source set —
 * yet the entrypoint class itself must, since fabric.mod.json entrypoints cannot be environment-scoped.
 */
public interface JadeClientPlugin {

    void registerClient(IWailaClientRegistration registration);
}

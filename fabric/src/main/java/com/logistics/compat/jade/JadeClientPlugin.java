package com.logistics.compat.jade;

import snownee.jade.api.IWailaClientRegistration;

/**
 * Client half of the Jade integration, reached from {@link JadeLogisticsPlugin} through {@link
 * java.util.ServiceLoader}. The implementation and its {@code META-INF/services} entry must stay in the
 * client source set: a dedicated server finds no services there and skips client-only registration.
 */
public interface JadeClientPlugin {

    void registerClient(IWailaClientRegistration registration);
}

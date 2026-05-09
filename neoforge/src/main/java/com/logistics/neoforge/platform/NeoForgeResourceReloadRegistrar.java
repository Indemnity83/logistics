package com.logistics.neoforge.platform;

import com.logistics.core.lib.platform.ResourceReloadRegistrar;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge implementation of {@link ResourceReloadRegistrar}.
 *
 * <p>Registrations are collected at startup and applied when
 * {@code AddReloadListenerEvent} fires on the NeoForge event bus. Register via
 * {@link #init(IEventBus)} during mod construction.
 */
public final class NeoForgeResourceReloadRegistrar implements ResourceReloadRegistrar {

    private record Registration(ResourceId id, PreparableReloadListener listener) {}

    private final List<Registration> pending = new ArrayList<>();

    public void init(IEventBus gameBus) {
        gameBus.addListener(this::onAddReloadListeners);
    }

    @Override
    public void register(ResourceId id, PreparableReloadListener listener) {
        pending.add(new Registration(id, listener));
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        for (Registration registration : pending) {
            event.addListener(registration.listener());
        }
    }
}

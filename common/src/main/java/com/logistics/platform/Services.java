package com.logistics.platform;

import com.logistics.platform.services.IBlockEntityHelper;
import com.logistics.platform.services.IEventHelper;
import com.logistics.platform.services.IItemGroupHelper;
import com.logistics.platform.services.IItemStorageService;
import java.util.ServiceLoader;

/**
 * Service loader entry point for platform-specific implementations.
 *
 * <p>Each loader (Fabric, NeoForge) provides implementations of these services
 * via META-INF/services/ files. Services are loaded lazily on first access.
 */
public final class Services {

    private Services() {}

    /**
     * Get the item storage service for inventory interaction.
     */
    public static IItemStorageService itemStorage() {
        return ItemStorageHolder.INSTANCE;
    }

    /**
     * Get the block entity helper for creating block entity types.
     */
    public static IBlockEntityHelper blockEntity() {
        return BlockEntityHolder.INSTANCE;
    }

    /**
     * Get the item group helper for creating creative tabs.
     */
    public static IItemGroupHelper itemGroup() {
        return ItemGroupHolder.INSTANCE;
    }

    /**
     * Get the event helper for lifecycle events.
     */
    public static IEventHelper events() {
        return EventHolder.INSTANCE;
    }

    private static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No implementation found for " + clazz.getName()
                        + ". Make sure the platform module is loaded."));
    }

    // Lazy initialization holders (initialized only when accessed)
    private static final class ItemStorageHolder {
        static final IItemStorageService INSTANCE = load(IItemStorageService.class);
    }

    private static final class BlockEntityHolder {
        static final IBlockEntityHelper INSTANCE = load(IBlockEntityHelper.class);
    }

    private static final class ItemGroupHolder {
        static final IItemGroupHelper INSTANCE = load(IItemGroupHelper.class);
    }

    private static final class EventHolder {
        static final IEventHelper INSTANCE = load(IEventHelper.class);
    }
}

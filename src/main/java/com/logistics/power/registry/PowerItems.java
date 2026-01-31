package com.logistics.power.registry;

import com.logistics.LogisticsMod;
import com.logistics.power.item.EngineProbeItem;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class PowerItems {
    private PowerItems() {}

    private static final String DOMAIN = "power/";

    public static final Item ENGINE_PROBE = registerItem(
            "engine_probe",
            new EngineProbeItem(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, id("engine_probe")))
                    .maxCount(1)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, id(name), item);
    }

    private static Identifier id(String name) {
        return Identifier.of(LogisticsMod.MOD_ID, DOMAIN + name);
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering power items");
    }
}

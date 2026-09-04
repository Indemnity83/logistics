package com.logistics.neoforge;

import com.logistics.core.bootstrap.LogisticsCommonBootstrap;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.ResourceReloadRegistrar;
import com.logistics.core.lib.energy.EnergyPushService;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.neoforge.client.NeoForgeClientSetup;
import com.logistics.neoforge.platform.NeoForgeCreativeTabRegistrar;
import com.logistics.neoforge.platform.NeoForgeResourceReloadRegistrar;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.fml.common.Mod;

@Mod("logistics")
public final class LogisticsNeoForge {

    private static final LogisticsCommonBootstrap COMMON_BOOTSTRAP = new LogisticsCommonBootstrap();
    private boolean commonInitialized;

    public LogisticsNeoForge(IEventBus modBus) {
        modBus.addListener(this::onRegister);
        com.logistics.neoforge.fluids.NeoForgeFluids.register(modBus);
        registerEnergyServices();
        NeoForgeCapabilityRegistration.register(modBus);
        NeoForgePacketRegistration.register(modBus);
        NeoForgeCommandRegistration.register(NeoForge.EVENT_BUS);
        NeoForgeChestLootModifier.register(NeoForge.EVENT_BUS);
        NeoForgeMobLootModifier.register(NeoForge.EVENT_BUS);
        NeoForgeNetworkTickHandler.register(NeoForge.EVENT_BUS);
        NeoForgePlayerJoinEvents.register(NeoForge.EVENT_BUS);
        if (FMLEnvironment.dist.isClient()) {
            NeoForgeClientSetup.register(modBus);
        }

        // Wire deferred event registrations on the creative tab and reload registrars
        // (The SPI INSTANCE is the NeoForge impl because it's the only implementation on classpath)
        var creativeTabRegistrar = CreativeTabRegistrar.INSTANCE;
        if (creativeTabRegistrar instanceof NeoForgeCreativeTabRegistrar nfRegistrar) {
            nfRegistrar.init(modBus);
        }
        var reloadRegistrar = ResourceReloadRegistrar.INSTANCE;
        if (reloadRegistrar instanceof NeoForgeResourceReloadRegistrar nfRegistrar) {
            nfRegistrar.init(NeoForge.EVENT_BUS);
        }
    }

    private synchronized void onRegister(RegisterEvent event) {
        if (commonInitialized) {
            return;
        }
        COMMON_BOOTSTRAP.initialize();
        commonInitialized = true;
    }

    private void registerEnergyServices() {
        EnergyPushService.set((level, targetPos, fromDirection, source, maxAmount) -> {
            var target = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, fromDirection);
            if (target == null) {
                return 0L;
            }
            return target.receiveEnergy((int) Math.min(maxAmount, Integer.MAX_VALUE), false);
        });

        AbstractEngineBlock.setEnergyPresenceChecker((world, pos, direction) -> {
            var target = world.getCapability(
                    Capabilities.EnergyStorage.BLOCK, pos.relative(direction), direction.getOpposite());
            return target != null && target.canReceive();
        });
    }
}

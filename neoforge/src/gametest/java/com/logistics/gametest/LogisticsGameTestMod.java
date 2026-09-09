package com.logistics.gametest;

import com.logistics.gametest.automation.AlloySmelterGameTestRegistration;
import com.logistics.gametest.automation.CrucibleGameTestRegistration;
import com.logistics.gametest.automation.KilnGameTestRegistration;
import com.logistics.gametest.automation.QuarryGameTestRegistration;
import com.logistics.gametest.automation.QuarryMiningGameTestRegistration;
import com.logistics.gametest.automation.RefineryGameTestRegistration;
import com.logistics.gametest.automation.SawmillGameTestRegistration;
import com.logistics.gametest.automation.SequentialFabricatorGameTestRegistration;
import com.logistics.gametest.automation.TransposerGameTestRegistration;
import com.logistics.gametest.core.MaceratorGameTestRegistration;
import com.logistics.gametest.core.OreGenerationGameTestRegistration;
import com.logistics.gametest.core.SolidFuelGameTestRegistration;
import com.logistics.gametest.network.NetworkIntegrationGameTestRegistration;
import com.logistics.gametest.pipe.CauldronFluidGameTestRegistration;
import com.logistics.gametest.pipe.FluidConnectionGameTestRegistration;
import com.logistics.gametest.pipe.FluidLightGameTestRegistration;
import com.logistics.gametest.pipe.FluidPacketDropGameTestRegistration;
import com.logistics.gametest.pipe.FluidProviderGameTestRegistration;
import com.logistics.gametest.pipe.FluidPumpGameTestRegistration;
import com.logistics.gametest.pipe.FluidSupplierGameTestRegistration;
import com.logistics.gametest.pipe.GlassTankBucketGameTestRegistration;
import com.logistics.gametest.pipe.ModuleGameTestRegistration;
import com.logistics.gametest.pipe.PipeFlowGameTestRegistration;
import com.logistics.gametest.pipe.PipeInfrastructureGameTestRegistration;
import com.logistics.gametest.pipe.PowerJunctionGameTestRegistration;
import com.logistics.gametest.power.BatteryGameTestRegistration;
import com.logistics.gametest.power.CableGameTestRegistration;
import com.logistics.gametest.power.EngineGameTestRegistration;
import com.logistics.gametest.power.FuelEngineGameTestRegistration;
import com.logistics.gametest.power.MagmaticEngineGameTestRegistration;
import com.logistics.gametest.power.ReactionEngineGameTestRegistration;
import com.logistics.gametest.power.SteamEngineGameTestRegistration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Entry point for the separate {@code logistics_gametest} mod (only present on the
 * {@code :neoforge:runGameTestServer} classpath, never in the shipped jar).
 *
 * <p>Each domain's {@code *GameTestRegistration} class declares its test functions as static
 * fields, so referencing them here forces that static initialization to run — and therefore
 * populate {@link GameTestFunctions#TEST_FUNCTION}'s pending entries — before the register call
 * below hands the DeferredRegister its event bus.
 *
 * <p>{@code power.CableGameTest} has no entry here: every one of its tests exercises Fabric's
 * Team Reborn transactional energy API directly (abort/commit semantics with no NeoForge
 * equivalent), so it stays Fabric-only. Everything else in {@code fabric/src/gametest} has a
 * counterpart below.
 */
@Mod("logistics_gametest")
public final class LogisticsGameTestMod {

    public LogisticsGameTestMod(IEventBus modBus) {
        RecipeLoadingGameTestRegistration.bootstrap();
        ReloadLifecycleGameTestRegistration.bootstrap();
        ServerDataLoadingGameTestRegistration.bootstrap();

        AlloySmelterGameTestRegistration.bootstrap();
        CrucibleGameTestRegistration.bootstrap();
        KilnGameTestRegistration.bootstrap();
        QuarryGameTestRegistration.bootstrap();
        QuarryMiningGameTestRegistration.bootstrap();
        RefineryGameTestRegistration.bootstrap();
        SawmillGameTestRegistration.bootstrap();
        SequentialFabricatorGameTestRegistration.bootstrap();
        TransposerGameTestRegistration.bootstrap();

        MaceratorGameTestRegistration.bootstrap();
        OreGenerationGameTestRegistration.bootstrap();
        SolidFuelGameTestRegistration.bootstrap();

        NetworkIntegrationGameTestRegistration.bootstrap();

        CauldronFluidGameTestRegistration.bootstrap();
        FluidConnectionGameTestRegistration.bootstrap();
        FluidLightGameTestRegistration.bootstrap();
        FluidPacketDropGameTestRegistration.bootstrap();
        FluidProviderGameTestRegistration.bootstrap();
        FluidPumpGameTestRegistration.bootstrap();
        FluidSupplierGameTestRegistration.bootstrap();
        GlassTankBucketGameTestRegistration.bootstrap();
        ModuleGameTestRegistration.bootstrap();
        PipeFlowGameTestRegistration.bootstrap();
        PipeInfrastructureGameTestRegistration.bootstrap();
        PowerJunctionGameTestRegistration.bootstrap();

        BatteryGameTestRegistration.bootstrap();
        CableGameTestRegistration.bootstrap();
        EngineGameTestRegistration.bootstrap();
        FuelEngineGameTestRegistration.bootstrap();
        MagmaticEngineGameTestRegistration.bootstrap();
        ReactionEngineGameTestRegistration.bootstrap();
        SteamEngineGameTestRegistration.bootstrap();

        GameTestFunctions.TEST_FUNCTION.register(modBus);
    }
}

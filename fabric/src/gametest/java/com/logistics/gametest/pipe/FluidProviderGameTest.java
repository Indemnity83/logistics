package com.logistics.gametest.pipe;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.block.entity.PowerJunctionBlockEntity;
import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import com.logistics.pipe.modules.SinkModule;
import com.logistics.pipe.network.PipeNetwork;
import com.logistics.pipe.network.NetworkRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

/**
 * Tick-based game tests for the Fluid Provider Pipe: a logistics pipe placed on a fluid tank that
 * mints fluid packets on demand and dispatches them into the item logistics network.
 *
 * <p>Layout (y=1): [fluid_provider_pipe] → [transport_pipe] → [basic_logistics_pipe] → [chest]
 * with a glass tank above the provider and a charged power junction above the transport pipe.
 *
 * <p>Run in-game: /test run logistics-gametest.fluidprovidergametest.&lt;methodname&gt;
 */
public class FluidProviderGameTest {

    /** Place a filled Power Junction so the network can pay module energy costs. */
    private static void placeChargedPowerJunction(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPipe.BLOCK.POWER_JUNCTION);
        PowerJunctionBlockEntity junction = context.getBlockEntity(pos, PowerJunctionBlockEntity.class);
        IEnergyStorage es = junction.energyStorage(null);
        long remaining = PowerJunctionBlockEntity.CAPACITY;
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted <= 0) break;
            remaining -= inserted;
        }
    }

    private static void enableDefaultRoute(GameTestHelper context, BlockPos sinkPos) {
        PipeBlockEntity pipeEntity = context.getBlockEntity(sinkPos, PipeBlockEntity.class);
        if (pipeEntity == null || !(pipeEntity.getBlockState().getBlock() instanceof PipeBlock pipeBlock)
                || pipeBlock.getPipe() == null) {
            throw new IllegalStateException("Basic logistics pipe missing at sink position");
        }
        SinkModule sink = pipeBlock.getPipe().getModule(SinkModule.class, pipeEntity);
        if (sink == null) {
            throw new IllegalStateException("SinkModule missing from pipe at sink position");
        }
        sink.setDefaultRoute(pipeEntity.createContext(), true);
    }

    private static IItemKey waterPacketKey() {
        long quantum = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_QUANTUM_MB);
        ItemStack packet = new ItemStack(LogisticsPipe.ITEM.FLUID_PACKET);
        packet.set(LogisticsPipe.DATA.FLUID_PACKET, new FluidPacket(Fluids.WATER, quantum));
        return ItemStorageLookup.of(packet);
    }

    /**
     * Happy path: a tank holding 4 quanta of water, an order for 4 packets. The provider mints and
     * dispatches the packets; they route to the default-route sink and land in the chest; the tank is
     * drained by 4×quantum and the network was charged for the packets.
     */
    @GameTest(maxTicks = 160)
    public void testFluidProviderMintsAndDeliversPackets(GameTestHelper context) {
        BlockPos providerPos = new BlockPos(0, 1, 0);
        BlockPos tankPos = new BlockPos(0, 2, 0);
        BlockPos transportPos = new BlockPos(1, 1, 0);
        BlockPos sinkPos = new BlockPos(2, 1, 0);
        BlockPos chestPos = new BlockPos(3, 1, 0);
        BlockPos junctionPos = new BlockPos(1, 2, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(sinkPos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(transportPos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(providerPos, LogisticsPipe.BLOCK.FLUID_PROVIDER_LOGISTICS_PIPE);
        context.setBlock(tankPos, LogisticsPipe.BLOCK.GLASS_TANK);
        placeChargedPowerJunction(context, junctionPos);

        long quantum = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_QUANTUM_MB);
        long rf = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);
        long fillMb = quantum * 4;

        GlassTankBlockEntity tank = context.getBlockEntity(tankPos, GlassTankBlockEntity.class);
        if (tank == null) {
            context.fail("Glass tank should have a block entity");
            return;
        }
        tank.setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(fillMb));

        PowerJunctionBlockEntity junction = context.getBlockEntity(junctionPos, PowerJunctionBlockEntity.class);
        long startEnergy = junction.energyStorage(null).getAmount();

        // Wait for the network to form and the provider to advertise supply, then place the order.
        context.runAfterDelay(15, () -> {
            enableDefaultRoute(context, sinkPos);
            PipeNetwork network = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(providerPos));
            if (network == null) {
                context.fail("Network should have formed at the provider by tick 15");
                return;
            }
            network.placeOrder(waterPacketKey(), 4, context.absolutePos(sinkPos));

            context.succeedWhen(() -> {
                context.assertContainerContains(chestPos, LogisticsPipe.ITEM.FLUID_PACKET);

                GlassTankBlockEntity t = context.getBlockEntity(tankPos, GlassTankBlockEntity.class);
                long remainingMb = FluidUnits.toMillibuckets(t.amount());
                if (remainingMb > fillMb - quantum * 4) {
                    throw context.assertionException(
                            "Tank should be drained by 4 quanta, remaining mB: " + remainingMb);
                }

                long energyNow = junction.energyStorage(null).getAmount();
                if (rf > 0 && energyNow >= startEnergy) {
                    throw context.assertionException("Network energy should have been charged for the packets");
                }
            });
        });
    }

    /**
     * Below one quantum: the tank holds less than a single packet's worth of fluid, so the provider
     * advertises nothing. An order for a packet is never fulfilled — the chest stays empty, the tank is
     * untouched, and no energy is charged.
     */
    @GameTest(maxTicks = 120)
    public void testFluidProviderDoesNotDispatchBelowOneQuantum(GameTestHelper context) {
        BlockPos providerPos = new BlockPos(0, 1, 0);
        BlockPos tankPos = new BlockPos(0, 2, 0);
        BlockPos transportPos = new BlockPos(1, 1, 0);
        BlockPos sinkPos = new BlockPos(2, 1, 0);
        BlockPos chestPos = new BlockPos(3, 1, 0);
        BlockPos junctionPos = new BlockPos(1, 2, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(sinkPos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(transportPos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(providerPos, LogisticsPipe.BLOCK.FLUID_PROVIDER_LOGISTICS_PIPE);
        context.setBlock(tankPos, LogisticsPipe.BLOCK.GLASS_TANK);
        placeChargedPowerJunction(context, junctionPos);

        long quantum = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_QUANTUM_MB);
        long fillMb = Math.max(1, quantum - 1); // below one quantum

        GlassTankBlockEntity tank = context.getBlockEntity(tankPos, GlassTankBlockEntity.class);
        if (tank == null) {
            context.fail("Glass tank should have a block entity");
            return;
        }
        tank.setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(fillMb));

        PowerJunctionBlockEntity junction = context.getBlockEntity(junctionPos, PowerJunctionBlockEntity.class);
        long startEnergy = junction.energyStorage(null).getAmount();

        context.runAfterDelay(15, () -> {
            enableDefaultRoute(context, sinkPos);
            PipeNetwork network = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(providerPos));
            if (network == null) {
                context.fail("Network should have formed at the provider by tick 15");
                return;
            }
            network.placeOrder(waterPacketKey(), 1, context.absolutePos(sinkPos));
        });

        // Give the network ample time to (not) dispatch, then assert nothing changed.
        context.runAfterDelay(90, () -> {
            GlassTankBlockEntity t = context.getBlockEntity(tankPos, GlassTankBlockEntity.class);
            if (FluidUnits.toMillibuckets(t.amount()) != fillMb) {
                context.fail("Tank should be untouched when it holds less than one quantum");
                return;
            }
            long energyNow = junction.energyStorage(null).getAmount();
            if (energyNow != startEnergy) {
                context.fail("No energy should be charged when nothing is dispatched");
                return;
            }
            context.assertContainerEmpty(chestPos);
            context.succeed();
        });
    }
}

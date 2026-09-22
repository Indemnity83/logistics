package com.logistics.pipe.block.entity;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.network.NetworkTopologyListener;
import com.logistics.core.lib.power.NetworkEnergySupplier;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.NetworkEnergySourceComponent;
import com.logistics.pipe.block.PowerJunctionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Logistics Power Junction: a headless machine that bridges the power (cable) network into the
 * logistics pipe network. Cables/engines fill its RF buffer ({@link #MAX_INPUT} RF/t); the logistics
 * network draws RF out of it ({@link NetworkEnergySourceComponent} registers it as an energy source).
 *
 * <p>The buffer is built with a non-zero {@code maxOutput} so the <em>logistics</em> network can pace
 * how fast it drains the junction. That extraction is reached through {@link NetworkEnergySupplier}
 * and nothing else: like any machine, the junction exposes an insert-only capability to the world, so
 * RF that has entered it cannot be pulled back out over a cable.
 */
public class PowerJunctionBlockEntity extends MachineEntity
        implements PipeConnection, NetworkTopologyListener, NetworkEnergySupplier {

    public static final long CAPACITY = 1_000_000L;
    public static final long MAX_INPUT = 128L; // ~32 EU/t at 1 EU = 4 RF

    /**
     * Uncapped on purpose: the meter belongs on the way in, not on the way out.
     *
     * <p>{@link #MAX_INPUT} is the progression lever -- how fast a junction fills is what a player
     * builds around. Once the RF is inside, it already belongs to the logistics network, so there is
     * no reason to ration how fast the network may spend its own buffer; a burst of routing or
     * crafting should be able to draw on the whole of it in one tick.
     *
     * <p>Safe only because the extractable view is reached through {@link NetworkEnergySupplier}
     * alone. The capability the world sees is insert-only, so no cable can pull at this rate.
     */
    private static final long MAX_OUTPUT = Long.MAX_VALUE;

    private EnergyStorageComponent energy;
    private NetworkEnergySourceComponent networkSource;

    public PowerJunctionBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPipe.ENTITY.POWER_JUNCTION_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        energy = machine.energy("energy")
                .capacity(CAPACITY)
                .maxInput(MAX_INPUT)
                .maxOutput(MAX_OUTPUT)
                .build();
        networkSource = machine.add(new NetworkEnergySourceComponent("network"));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PowerJunctionBlockEntity entity) {
        MachineEntity.tick(level, pos, state, entity);
        if (level.isClientSide()) {
            return;
        }
        entity.updateChargeState(state);
    }

    /** Drive the {@link PowerJunctionBlock#CHARGE} block state from the buffer fill, like a battery. */
    private void updateChargeState(BlockState state) {
        if (level == null) {
            return;
        }
        int charge = chargeLevel();
        if (state.hasProperty(PowerJunctionBlock.CHARGE) && state.getValue(PowerJunctionBlock.CHARGE) != charge) {
            level.setBlock(worldPosition, state.setValue(PowerJunctionBlock.CHARGE, charge), Block.UPDATE_CLIENTS);
        }
    }

    private int chargeLevel() {
        long amount = energy.amount();
        if (amount <= 0) {
            return 0;
        }
        return Math.max(1, Math.round((float) amount / CAPACITY * 10));
    }

    @Override
    public IEnergyStorage networkEnergyStorage() {
        return energy.networkEnergyStorage();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // No component removal hook in the framework — unregister the network source explicitly so a
        // broken junction stops being a power source for the network.
        if (level != null && !level.isClientSide()) {
            networkSource.unregisterAll(this);
        }
    }

    // ==================== NetworkTopologyListener ====================

    @Override
    public void onNetworkTopologyChanged() {
        networkSource.requestRescan();
    }

    // ==================== PipeConnection ====================
    // Present as a POWER source so adjacent pipes render a connection arm and recognise it as power.

    @Override
    public PipeConnection.Type getConnectionType(Direction direction) {
        return PipeConnection.Type.POWER;
    }

    @Override
    public boolean addItem(Direction from, ItemStack stack) {
        return false;
    }

    @Override
    @Nullable
    public MenuProvider createMenuProvider() {
        return null; // headless conduit — no GUI
    }
}

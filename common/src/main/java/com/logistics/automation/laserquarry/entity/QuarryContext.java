package com.logistics.automation.laserquarry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The narrow boundary {@link QuarryPhaseRunner} (and other quarry work) depends on instead of the
 * block entity. Exposes only what the work needs — world handles, the mining region, the arm, energy
 * query/spend + action costs, and change/sync hooks — so the runner never reaches back into
 * {@link LaserQuarryBlockEntity}.
 *
 * <p>Energy is exposed as flat operations here; the quarry-specific cost math behind
 * {@code moveCost}/{@code effectiveArmSpeed}/{@code frameBuildCost} is free to move into a dedicated
 * policy later without changing this contract.
 */
public interface QuarryContext {

    ServerLevel level();

    BlockPos pos();

    BlockState quarryState();

    QuarryBounds bounds();

    ArmController arm();

    QuarryOutput output();

    int breakingEntityId();

    int levelMinY();

    // --- energy ---

    long energyStored();

    boolean hasEnergy(long rf);

    void consumeEnergy(long rf);

    long moveCost();

    float effectiveArmSpeed();

    long frameBuildCost();

    float breakCost(float hardness);

    // --- change / sync ---

    void markChanged();

    void sync();
}

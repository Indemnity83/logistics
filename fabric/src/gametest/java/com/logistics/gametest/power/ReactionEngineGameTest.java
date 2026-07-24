package com.logistics.gametest.power;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.fabric.storage.FabricItemKey;
import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.power.engine.block.entity.ReactionEngineBlockEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Game tests for the Reaction Engine: the bufferless-output guarantee (no energy capability), the
 * reactant/catalyst input filters, and a full ignite-and-deliver cycle into an adjacent battery.
 */
public class ReactionEngineGameTest {

    private static Fluid liquidEnder() {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.get(
                LogisticsCore.resource("liquid_ender").toIdentifier());
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testReactionEnginePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.REACTION_ENGINE);

        ReactionEngineBlockEntity engine = (ReactionEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Reaction engine should have block entity");
        }
        context.succeed();
    }

    /** The bufferless guarantee: the engine exposes no energy capability on any face. */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testReactionEngineExposesNoEnergyCapability(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.REACTION_ENGINE);

        ReactionEngineBlockEntity engine = (ReactionEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Reaction engine should have block entity");
            return;
        }
        if (engine.energyStorage(null) != null) {
            context.fail("Reaction engine must expose no energy capability (null side)");
            return;
        }
        for (Direction side : Direction.values()) {
            if (engine.energyStorage(side) != null) {
                context.fail("Reaction engine must expose no energy capability on side " + side);
                return;
            }
        }
        context.succeed();
    }

    /** The reactant tank accepts the launch reactant and rejects a non-reactant fluid. */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testReactionEngineReactantFilter(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.REACTION_ENGINE);

        ReactionEngineBlockEntity engine = (ReactionEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Reaction engine should have block entity");
            return;
        }
        long accepted = engine.fluidStorage(null)
                .insert(SimpleFluidKey.of(liquidEnder()), FluidUnits.mb(1_000), false);
        if (accepted <= 0) {
            context.fail("Reactant tank should accept liquid ender, accepted: " + accepted);
            return;
        }
        long rejected = engine.fluidStorage(null)
                .insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(1_000), false);
        if (rejected != 0) {
            context.fail("Reactant tank should reject water, accepted: " + rejected);
            return;
        }
        context.succeed();
    }

    /** The catalyst slot accepts the launch catalyst, rejects other items, and is hidden on the output face. */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testReactionEngineCatalystFilter(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.REACTION_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.NORTH));

        ReactionEngineBlockEntity engine = (ReactionEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Reaction engine should have block entity");
            return;
        }
        if (engine.itemStorage(Direction.NORTH) != null) {
            context.fail("Catalyst slot must be hidden on the output face (NORTH)");
            return;
        }
        IItemStorage back = engine.itemStorage(Direction.SOUTH);
        if (back == null) {
            context.fail("Catalyst slot should be accessible from the back face");
            return;
        }
        if (back.insert(FabricItemKey.of(new ItemStack(Items.ECHO_SHARD)), 1, false) != 1) {
            context.fail("Catalyst slot should accept an echo shard");
            return;
        }
        if (back.insert(FabricItemKey.of(new ItemStack(Items.DIRT)), 1, false) != 0) {
            context.fail("Catalyst slot should reject non-catalyst items");
            return;
        }
        context.succeed();
    }

    /**
     * A full reaction: with a reactant batch, a catalyst, and redstone, the engine ignites (consuming the
     * catalyst) and pushes energy into an adjacent battery.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testReactionEngineIgnitesAndDelivers(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(0, 1, 0);
        BlockPos batteryPos = new BlockPos(1, 1, 0); // engine output faces EAST toward the battery

        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        context.setBlock(enginePos, LogisticsPower.BLOCK.REACTION_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        ReactionEngineBlockEntity engine = (ReactionEngineBlockEntity) context.getBlockEntity(enginePos);
        if (engine == null) {
            context.fail("Reaction engine should have block entity");
            return;
        }
        engine.reactantTank().tank().setContents(SimpleFluidKey.of(liquidEnder()), FluidUnits.mb(1_000));
        engine.setTheItem(new ItemStack(Items.ECHO_SHARD));

        context.runAfterDelay(20, () -> {
            ReactionEngineBlockEntity eng = (ReactionEngineBlockEntity) context.getBlockEntity(enginePos);
            if (eng == null) {
                context.fail("Reaction engine block entity not found after 20 ticks");
                return;
            }
            if (!eng.simulation().isReacting()) {
                context.fail("Reaction engine should be reacting after 20 ticks");
                return;
            }
            if (!eng.getTheItem().isEmpty()) {
                context.fail("Catalyst should have been consumed at ignition, got: " + eng.getTheItem());
                return;
            }
            BatteryBlockEntity battery = (BatteryBlockEntity) context.getBlockEntity(batteryPos);
            if (battery == null) {
                context.fail("Battery block entity not found");
                return;
            }
            long stored = battery.energyStorage(Direction.WEST).getAmount();
            if (stored <= 0) {
                context.fail("Battery should have received energy from the reaction, got: " + stored);
                return;
            }
            context.succeed();
        });
    }
}

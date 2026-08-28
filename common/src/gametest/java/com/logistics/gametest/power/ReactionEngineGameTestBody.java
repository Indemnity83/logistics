package com.logistics.gametest.power;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.power.engine.block.entity.ReactionEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Shared reaction engine GameTest bodies, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Loader-specific glue wires these into each
 * loader's own registration mechanism: Fabric's {@code @GameTest}-annotated
 * {@code ReactionEngineGameTest} delegates to these methods, and NeoForge's
 * {@code ReactionEngineGameTestRegistration} references them directly as
 * {@code Consumer<GameTestHelper>} method references.
 *
 * <p>Tests the bufferless-output guarantee (no energy capability), the reactant/catalyst input
 * filters, and a full ignite-and-deliver cycle into an adjacent battery.
 */
public class ReactionEngineGameTestBody {

    private static Fluid liquidEnder() {
        return BuiltInRegistries.FLUID.getValue(LogisticsCore.resource("liquid_ender").toIdentifier());
    }

    public static void testReactionEnginePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.REACTION_ENGINE);

        ReactionEngineBlockEntity engine = context.getBlockEntity(pos, ReactionEngineBlockEntity.class);
        if (engine == null) {
            context.fail("Reaction engine should have block entity");
        }
        context.succeed();
    }

    /** The bufferless guarantee: the engine exposes no energy capability on any face. */
    public static void testReactionEngineExposesNoEnergyCapability(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.REACTION_ENGINE);

        ReactionEngineBlockEntity engine = context.getBlockEntity(pos, ReactionEngineBlockEntity.class);
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
    public static void testReactionEngineReactantFilter(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.REACTION_ENGINE);

        ReactionEngineBlockEntity engine = context.getBlockEntity(pos, ReactionEngineBlockEntity.class);
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
    public static void testReactionEngineCatalystFilter(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.REACTION_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.NORTH));

        ReactionEngineBlockEntity engine = context.getBlockEntity(pos, ReactionEngineBlockEntity.class);
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
        if (back.insert(ItemStorageLookup.of(new ItemStack(Items.ECHO_SHARD)), 1, false) != 1) {
            context.fail("Catalyst slot should accept an echo shard");
            return;
        }
        if (back.insert(ItemStorageLookup.of(new ItemStack(Items.DIRT)), 1, false) != 0) {
            context.fail("Catalyst slot should reject non-catalyst items");
            return;
        }
        context.succeed();
    }

    /**
     * A full reaction: with a reactant batch, a catalyst, and redstone, the engine ignites (consuming the
     * catalyst) and pushes energy into an adjacent battery.
     */
    public static void testReactionEngineIgnitesAndDelivers(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(0, 1, 0);
        BlockPos batteryPos = new BlockPos(1, 1, 0); // engine output faces EAST toward the battery

        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        context.setBlock(enginePos, LogisticsPower.BLOCK.REACTION_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        ReactionEngineBlockEntity engine = context.getBlockEntity(enginePos, ReactionEngineBlockEntity.class);
        if (engine == null) {
            context.fail("Reaction engine should have block entity");
            return;
        }
        engine.reactantTank().tank().setContents(SimpleFluidKey.of(liquidEnder()), FluidUnits.mb(1_000));
        engine.setTheItem(new ItemStack(Items.ECHO_SHARD));

        context.runAfterDelay(20, () -> {
            ReactionEngineBlockEntity eng = context.getBlockEntity(enginePos, ReactionEngineBlockEntity.class);
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
            BatteryBlockEntity battery = context.getBlockEntity(batteryPos, BatteryBlockEntity.class);
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

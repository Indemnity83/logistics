package com.logistics.gametest.power;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsPower;
import com.logistics.automation.macerator.MaceratorBlockEntity;
import com.logistics.core.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.cable.CableBlock;
import com.logistics.power.cable.CableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Cable topology and engine interaction: which neighbours a cable connects to, how that connection
 * reacts to a neighbour rotating, and which engines the network may and may not draw from.
 */
public class CableGameTestBody {

    public static void testCreativeEnginePowersCableNetwork(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(0, 1, 1);
        BlockPos firstCablePos = new BlockPos(1, 1, 1);
        BlockPos secondCablePos = new BlockPos(2, 1, 1);
        BlockPos machinePos = new BlockPos(3, 1, 1);

        context.setBlock(firstCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(secondCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.MACERATOR);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        CableBlockEntity firstCable = (CableBlockEntity) context.getBlockEntity(firstCablePos);
        CableBlockEntity secondCable = (CableBlockEntity) context.getBlockEntity(secondCablePos);
        MaceratorBlockEntity machine = (MaceratorBlockEntity) context.getBlockEntity(machinePos);
        if (firstCable == null || secondCable == null || machine == null) {
            context.fail("Expected cable and machine block entities");
            return;
        }
        giveMaceratorWork(machine);

        context.runAfterDelay(20, () -> {
            long machineEnergy = machine.energyStorage(Direction.WEST).getAmount();
            if (machineEnergy <= 0) {
                context.fail("Creative engine should power machine through cable network");
                return;
            }
            if (firstCable.energyStorage(Direction.WEST).getAmount() != 0
                    || secondCable.energyStorage(Direction.WEST).getAmount() != 0) {
                context.fail("Cables should not retain creative engine energy");
                return;
            }
            context.succeed();
        });
    }

    public static void testCableConnectionUpdatesWhenNeighborOutputRotates(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos cablePos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        CableBlockEntity cable = (CableBlockEntity) context.getBlockEntity(cablePos);
        if (cable == null) {
            context.fail("Expected cable block entity");
            return;
        }

        if (cable.getCachedConnectionType(Direction.WEST) != CableBlock.ConnectionType.DEVICE) {
            context.fail("Cable should initially connect to engine output");
            return;
        }

        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.NORTH)
                .setValue(AbstractEngineBlock.POWERED, true));

        if (cable.getCachedConnectionType(Direction.WEST) != CableBlock.ConnectionType.NONE) {
            context.fail("Cable connection cache should update when neighboring output rotates away");
            return;
        }

        context.succeed();
    }

    public static void testCableConnectsToBufferlessEngineOutputs(GameTestHelper context) {
        BlockPos steamEnginePos = new BlockPos(1, 1, 1);
        BlockPos steamCablePos = new BlockPos(2, 1, 1);
        BlockPos reactionEnginePos = new BlockPos(4, 1, 1);
        BlockPos reactionCablePos = new BlockPos(5, 1, 1);

        context.setBlock(steamCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(steamEnginePos, LogisticsPower.BLOCK.STEAM_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST));
        context.setBlock(reactionCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(reactionEnginePos, LogisticsPower.BLOCK.REACTION_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST));

        CableBlockEntity steamCable = (CableBlockEntity) context.getBlockEntity(steamCablePos);
        CableBlockEntity reactionCable = (CableBlockEntity) context.getBlockEntity(reactionCablePos);
        if (steamCable == null || reactionCable == null) {
            context.fail("Expected cable block entities");
            return;
        }
        if (steamCable.getCachedConnectionType(Direction.WEST) != CableBlock.ConnectionType.DEVICE
                || reactionCable.getCachedConnectionType(Direction.WEST) != CableBlock.ConnectionType.DEVICE) {
            context.fail("Cables should connect to bufferless engine output faces");
            return;
        }
        context.succeed();
    }

    public static void testCableDoesNotConnectToRedstoneEngine(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos cablePos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(enginePos, LogisticsCore.BLOCK.REDSTONE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST));

        RedstoneEngineBlockEntity engine = (RedstoneEngineBlockEntity) context.getBlockEntity(enginePos);
        if (engine == null) {
            context.fail("Expected redstone engine block entity");
            return;
        }
        CableBlockEntity cable = (CableBlockEntity) context.getBlockEntity(cablePos);
        if (cable == null) {
            context.fail("Expected cable block entity");
            return;
        }
        if (cable.getCachedConnectionType(Direction.WEST) != CableBlock.ConnectionType.NONE) {
            context.fail("Cable should not connect to redstone engine");
            return;
        }
        context.succeed();
    }

    /**
     * The same layout as {@link #testCreativeEnginePowersCableNetwork}, swapping the engine. A
     * machine with work to do sits behind the cable so the network genuinely wants energy —
     * without a consumer nothing would be pulled either way, and the test would pass without
     * showing that the redstone engine is the reason.
     */
    public static void testRedstoneEngineIsNotPulledByCableNetwork(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos cablePos = new BlockPos(2, 1, 1);
        BlockPos machinePos = new BlockPos(3, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.MACERATOR);
        context.setBlock(enginePos, LogisticsCore.BLOCK.REDSTONE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        MaceratorBlockEntity machine = (MaceratorBlockEntity) context.getBlockEntity(machinePos);
        if (machine == null) {
            context.fail("Expected macerator block entity");
            return;
        }
        giveMaceratorWork(machine);

        long[] engineEnergyWhileDemanded = new long[1];
        context.runAfterDelay(20, () -> {
            RedstoneEngineBlockEntity engine = (RedstoneEngineBlockEntity) context.getBlockEntity(enginePos);
            if (engine == null) {
                context.fail("Expected redstone engine block entity");
                return;
            }
            if (engine.getEnergy() <= 0) {
                context.fail("Redstone engine should have generated energy to be drained of");
                return;
            }
            engineEnergyWhileDemanded[0] = engine.getEnergy();
        });

        context.runAfterDelay(40, () -> {
            RedstoneEngineBlockEntity engine = (RedstoneEngineBlockEntity) context.getBlockEntity(enginePos);
            CableBlockEntity cable = (CableBlockEntity) context.getBlockEntity(cablePos);
            if (engine == null || cable == null) {
                context.fail("Expected redstone engine and cable block entities");
                return;
            }
            if (machine.energyStorage(Direction.EAST).getAmount() != 0) {
                context.fail("Cable network should not deliver redstone engine energy to a demanding machine");
                return;
            }
            if (engine.getEnergy() < engineEnergyWhileDemanded[0]) {
                context.fail("Redstone engine buffer was drained despite demand on the network");
                return;
            }
            if (cable.energyStorage(Direction.WEST).getAmount() != 0) {
                context.fail("Cable should not buffer redstone engine energy");
                return;
            }
            context.succeed();
        });
    }

    private static void giveMaceratorWork(MaceratorBlockEntity machine) {
        machine.setItem(0, new ItemStack(Items.IRON_INGOT));
    }
}

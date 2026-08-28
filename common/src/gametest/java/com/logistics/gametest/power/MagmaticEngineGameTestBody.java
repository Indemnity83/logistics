package com.logistics.gametest.power;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.power.engine.block.entity.MagmaticEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;

/**
 * Shared magmatic engine GameTest bodies, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Loader-specific glue wires these into each
 * loader's own registration mechanism: Fabric's {@code @GameTest}-annotated
 * {@code MagmaticEngineGameTest} delegates to these methods, and NeoForge's
 * {@code MagmaticEngineGameTestRegistration} references them directly as
 * {@code Consumer<GameTestHelper>} method references.
 */
public class MagmaticEngineGameTestBody {

    private static MagmaticEngineBlockEntity placePowered(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPower.BLOCK.MAGMATIC_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));
        return context.getBlockEntity(pos, MagmaticEngineBlockEntity.class);
    }

    private static long batteryStored(GameTestHelper context, BlockPos pos) {
        BatteryBlockEntity b = context.getBlockEntity(pos, BatteryBlockEntity.class);
        return ((EnergyComponent) b.energyStorage(null)).getAmount();
    }

    /** The filtered fluid capability accepts lava (as a pipe would) and rejects everything else. */
    public static void testAcceptsLavaRejectsOther(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.MAGMATIC_ENGINE);
        MagmaticEngineBlockEntity engine = context.getBlockEntity(pos, MagmaticEngineBlockEntity.class);

        long lava = engine.fluidStorage(Direction.UP).insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(1000), false);
        if (lava <= 0 || engine.lavaTank().tank().isEmpty()) {
            context.fail("Lava should be accepted into the tank via the fluid capability");
            return;
        }
        long water = engine.fluidStorage(Direction.UP).insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(1000), false);
        if (water != 0) {
            context.fail("Non-lava fluids must be rejected");
            return;
        }
        context.succeed();
    }

    /** Lava inserted through the capability feeds generation: the engine heat-soaks and delivers RF. */
    public static void testGeneratesFromLavaAndDelivers(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        BlockPos batteryPos = new BlockPos(1, 1, 0); // output faces EAST
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        MagmaticEngineBlockEntity engine = placePowered(context, pos);
        if (engine == null) {
            context.fail("Magmatic engine block entity not found");
            return;
        }
        // Feed lava the way a fluid pipe would — through the exposed capability, not by seeding the tank.
        engine.fluidStorage(Direction.UP).insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(2000), false);

        context.runAfterDelay(100, () -> {
            MagmaticEngineBlockEntity e = context.getBlockEntity(pos, MagmaticEngineBlockEntity.class);
            if (e.simulation().remainingBurnTicks() <= 0) {
                context.fail("Engine should be lit and burning a committed batch");
                return;
            }
            if (e.simulation().temperatureCelsius() <= 20) {
                context.fail("Engine should have heat-soaked above ambient, temp: " + e.simulation().temperatureCelsius());
                return;
            }
            if (e.simulation().lastAccepted() <= 0) {
                context.fail("Engine should be generating RF, rate: " + e.simulation().lastAccepted());
                return;
            }
            if (e.lavaTank().tank().getAmount() >= FluidUnits.mb(2000)) {
                context.fail("Engine should have consumed a lava batch");
                return;
            }
            if (batteryStored(context, batteryPos) <= 0) {
                context.fail("A consumer on the output face should receive RF, stored: "
                        + batteryStored(context, batteryPos));
                return;
            }
            // It cannot overheat — no heat latch on the block.
            if (e.isOverheated()) {
                context.fail("Magmatic engine must never overheat");
                return;
            }
            context.succeed();
        });
    }
}

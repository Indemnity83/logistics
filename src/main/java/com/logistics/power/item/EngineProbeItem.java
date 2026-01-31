package com.logistics.power.item;

import com.logistics.power.engine.block.entity.AbstractEngineBlockEntity;
import com.logistics.power.engine.block.entity.AbstractEngineBlockEntity.HeatStage;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A diagnostic tool for inspecting engine block entities.
 * Right-click on an engine to display its current stats in chat.
 */
public class EngineProbeItem extends Item {

    public EngineProbeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();

        if (world.isClient() || player == null) {
            return ActionResult.SUCCESS;
        }

        if (world.getBlockEntity(pos) instanceof AbstractEngineBlockEntity engine) {
            displayEngineStats(player, engine);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    private void displayEngineStats(PlayerEntity player, AbstractEngineBlockEntity engine) {
        player.sendMessage(Text.literal("=== Engine Stats ===").formatted(Formatting.GOLD), false);

        // Stage with color coding
        HeatStage stage = engine.getHeatStage();
        Formatting stageColor = getStageColor(stage);
        player.sendMessage(Text.literal("Stage: ")
                .append(Text.literal(stage.name()).formatted(stageColor)), false);

        // Temperature info
        double temp = engine.getTemperature();
        double maxTemp = engine.getMaxTemperature();
        double heatLevel = engine.getHeatLevel();
        Formatting tempColor = heatLevel >= 1.0 ? Formatting.RED :
                               heatLevel >= 0.75 ? Formatting.YELLOW : Formatting.GREEN;
        player.sendMessage(Text.literal("Temperature: ")
                .append(Text.literal(String.format("%.0f°C (%.0f Max)", temp, maxTemp)).formatted(tempColor)), false);

        // Energy info (buffer)
        long storedEnergy = engine.getEnergy();
        double energyLevel = engine.getEnergyLevel();
        player.sendMessage(Text.literal("Energy: ")
                .append(Text.literal(String.format("%,d / %,d RF (%.1f%%)",
                        storedEnergy, engine.getCapacity(), energyLevel * 100)).formatted(Formatting.AQUA)), false);

        // Generation rate (Stirling only - PID controlled)
        if (engine instanceof StirlingEngineBlockEntity stirling) {
            double pidRate = stirling.getCurrentGenerationRate();
            player.sendMessage(Text.literal("Generation: ")
                    .append(Text.literal(String.format("%.2f RF/t", pidRate))
                            .formatted(Formatting.GREEN)), false);
        }

        // Creative Engine output level
        if (engine instanceof CreativeEngineBlockEntity creative) {
            int level = creative.getOutputLevelIndex() + 1;
            int maxLevel = CreativeEngineBlockEntity.OUTPUT_LEVELS.length;
            player.sendMessage(Text.literal("Output Level: ")
                    .append(Text.literal(String.format("%d/%d", level, maxLevel))
                            .formatted(Formatting.LIGHT_PURPLE)), false);
        }

        // Output power (all engines)
        long outputPower = engine.getCurrentOutputPower();
        player.sendMessage(Text.literal("Output Power: ")
                .append(Text.literal(String.format("%d RF/t", outputPower))
                        .formatted(Formatting.LIGHT_PURPLE)), false);

        // Running state
        player.sendMessage(Text.literal("Running: ")
                .append(Text.literal(engine.isRunning() ? "Yes" : "No")
                        .formatted(engine.isRunning() ? Formatting.GREEN : Formatting.GRAY)), false);

        // Overheat warning
        if (engine.isOverheated()) {
            player.sendMessage(Text.literal("WARNING: OVERHEATED!").formatted(Formatting.RED, Formatting.BOLD), false);
        }

        // Stirling-specific: burn time
        if (engine instanceof StirlingEngineBlockEntity stirling) {
            int burnTime = stirling.getBurnTime();
            int fuelTime = stirling.getFuelTime();
            if (fuelTime > 0) {
                player.sendMessage(Text.literal("Fuel: ")
                        .append(Text.literal(String.format("%d / %d ticks (%.1f%%)",
                                burnTime, fuelTime, (burnTime / (float) fuelTime) * 100))
                                .formatted(Formatting.YELLOW)), false);
            } else {
                player.sendMessage(Text.literal("Fuel: ")
                        .append(Text.literal("None").formatted(Formatting.GRAY)), false);
            }
        }
    }

    private Formatting getStageColor(HeatStage stage) {
        return switch (stage) {
            case COLD -> Formatting.BLUE;
            case COOL -> Formatting.GREEN;
            case WARM -> Formatting.YELLOW;
            case HOT -> Formatting.RED;
            case OVERHEAT -> Formatting.DARK_RED;
        };
    }
}

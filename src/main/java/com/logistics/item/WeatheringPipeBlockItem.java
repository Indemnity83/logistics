package com.logistics.item;

import com.logistics.LogisticsDataComponents;
import com.logistics.LogisticsDataComponents.WeatheringState;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * A BlockItem that displays oxidation state in its name, similar to vanilla copper blocks.
 */
public class WeatheringPipeBlockItem extends BlockItem {
    private final String baseTranslationKey;

    public WeatheringPipeBlockItem(Block block, Settings settings) {
        super(block, settings);
        this.baseTranslationKey = block.getTranslationKey();
    }

    @Override
    public Text getName(ItemStack stack) {
        WeatheringState state = stack.get(LogisticsDataComponents.WEATHERING_STATE);
        if (state == null || state.isDefault()) {
            return super.getName(stack);
        }

        String suffix = getTranslationSuffix(state);
        return Text.translatable(baseTranslationKey + suffix);
    }

    private String getTranslationSuffix(WeatheringState state) {
        String oxidationSuffix =
                switch (state.oxidationStage()) {
                    case 1 -> ".exposed";
                    case 2 -> ".weathered";
                    case 3 -> ".oxidized";
                    default -> "";
                };

        if (state.waxed()) {
            return ".waxed" + oxidationSuffix;
        }
        return oxidationSuffix;
    }
}

package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.pipe.screen.ItemFilterScreen;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPipeClient implements DomainBootstrap {
    public LogisticsPipeClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            // Pipe markings decoration
            Identifier pipeMarkings = LogisticsPipe.blockModelIdentifier("pipe_markings");

            // Stone transport pipe
            Identifier stoneCore = LogisticsPipe.blockModelIdentifier("stone_transport_pipe_core");
            Identifier stoneArm = LogisticsPipe.blockModelIdentifier("stone_transport_pipe_arm");
            Identifier stoneArmExtended = LogisticsPipe.blockModelIdentifier("stone_transport_pipe_arm_extended");

            // Copper transport pipe
            Identifier copperCore = LogisticsPipe.blockModelIdentifier("copper_transport_pipe_core");
            Identifier copperCoreExposed = LogisticsPipe.blockModelIdentifier("copper_transport_pipe_core_exposed");
            Identifier copperCoreWeathered = LogisticsPipe.blockModelIdentifier("copper_transport_pipe_core_weathered");
            Identifier copperCoreOxidized = LogisticsPipe.blockModelIdentifier("copper_transport_pipe_core_oxidized");
            Identifier copperArm = LogisticsPipe.blockModelIdentifier("copper_transport_pipe_arm");
            Identifier copperArmExposed = LogisticsPipe.blockModelIdentifier("copper_transport_pipe_arm_exposed");
            Identifier copperArmWeathered = LogisticsPipe.blockModelIdentifier("copper_transport_pipe_arm_weathered");
            Identifier copperArmOxidized = LogisticsPipe.blockModelIdentifier("copper_transport_pipe_arm_oxidized");
            Identifier copperArmExtended = LogisticsPipe.blockModelIdentifier("copper_transport_pipe_arm_extended");
            Identifier copperArmExtendedExposed = LogisticsPipe.blockModelIdentifier("copper_transport_pipe_arm_extended_exposed");
            Identifier copperArmExtendedWeathered = LogisticsPipe.blockModelIdentifier("copper_transport_pipe_arm_extended_weathered");
            Identifier copperArmExtendedOxidized = LogisticsPipe.blockModelIdentifier("copper_transport_pipe_arm_extended_oxidized");

            // Gold transport pipe
            Identifier goldCore = LogisticsPipe.blockModelIdentifier("gold_transport_pipe_core");
            Identifier goldCorePowered = LogisticsPipe.blockModelIdentifier("gold_transport_pipe_core_powered");
            Identifier goldArm = LogisticsPipe.blockModelIdentifier("gold_transport_pipe_arm");
            Identifier goldArmPowered = LogisticsPipe.blockModelIdentifier("gold_transport_pipe_arm_powered");
            Identifier goldArmExtended = LogisticsPipe.blockModelIdentifier("gold_transport_pipe_arm_extended");
            Identifier goldArmExtendedPowered = LogisticsPipe.blockModelIdentifier("gold_transport_pipe_arm_extended_powered");

            // Item extractor pipe
            Identifier extractorCore = LogisticsPipe.blockModelIdentifier("item_extractor_pipe_core");
            Identifier extractorArm = LogisticsPipe.blockModelIdentifier("item_extractor_pipe_arm");
            Identifier extractorArmExtended = LogisticsPipe.blockModelIdentifier("item_extractor_pipe_arm_extended");
            Identifier extractorFeature = LogisticsPipe.blockModelIdentifier("item_extractor_pipe_feature");
            Identifier extractorFeatureExtended = LogisticsPipe.blockModelIdentifier("item_extractor_pipe_feature_extended");

            // Item filter pipe
            Identifier filterCore = LogisticsPipe.blockModelIdentifier("item_filter_pipe_core");
            Identifier filterArm = LogisticsPipe.blockModelIdentifier("item_filter_pipe_arm");
            Identifier filterArmExtended = LogisticsPipe.blockModelIdentifier("item_filter_pipe_arm_extended");

            // Item insertion pipe
            Identifier insertionCore = LogisticsPipe.blockModelIdentifier("item_insertion_pipe_core");
            Identifier insertionArm = LogisticsPipe.blockModelIdentifier("item_insertion_pipe_arm");
            Identifier insertionArmExtended = LogisticsPipe.blockModelIdentifier("item_insertion_pipe_arm_extended");

            // Item merger pipe
            Identifier mergerCore = LogisticsPipe.blockModelIdentifier("item_merger_pipe_core");
            Identifier mergerArm = LogisticsPipe.blockModelIdentifier("item_merger_pipe_arm");
            Identifier mergerArmExtended = LogisticsPipe.blockModelIdentifier("item_merger_pipe_arm_extended");
            Identifier mergerFeature = LogisticsPipe.blockModelIdentifier("item_merger_pipe_feature");
            Identifier mergerFeatureExtended = LogisticsPipe.blockModelIdentifier("item_merger_pipe_feature_extended");

            // Item passthrough pipe
            Identifier passthroughCore = LogisticsPipe.blockModelIdentifier("item_passthrough_pipe_core");
            Identifier passthroughArm = LogisticsPipe.blockModelIdentifier("item_passthrough_pipe_arm");
            Identifier passthroughArmExtended = LogisticsPipe.blockModelIdentifier("item_passthrough_pipe_arm_extended");

            // Item void pipe
            Identifier voidCore = LogisticsPipe.blockModelIdentifier("item_void_pipe_core");
            Identifier voidArm = LogisticsPipe.blockModelIdentifier("item_void_pipe_arm");
            Identifier voidArmExtended = LogisticsPipe.blockModelIdentifier("item_void_pipe_arm_extended");

            pluginContext.addModel(MODEL.PIPE_MARKINGS, SimpleUnbakedExtraModel.blockStateModel(pipeMarkings));
            pluginContext.addModel(MODEL.STONE_CORE, SimpleUnbakedExtraModel.blockStateModel(stoneCore));
            pluginContext.addModel(MODEL.STONE_ARM, SimpleUnbakedExtraModel.blockStateModel(stoneArm));
            pluginContext.addModel(MODEL.STONE_ARM_EXTENDED, SimpleUnbakedExtraModel.blockStateModel(stoneArmExtended));
            pluginContext.addModel(MODEL.COPPER_CORE, SimpleUnbakedExtraModel.blockStateModel(copperCore));
            pluginContext.addModel(MODEL.COPPER_CORE_EXPOSED, SimpleUnbakedExtraModel.blockStateModel(copperCoreExposed));
            pluginContext.addModel(MODEL.COPPER_CORE_WEATHERED, SimpleUnbakedExtraModel.blockStateModel(copperCoreWeathered));
            pluginContext.addModel(MODEL.COPPER_CORE_OXIDIZED, SimpleUnbakedExtraModel.blockStateModel(copperCoreOxidized));
            pluginContext.addModel(MODEL.COPPER_ARM, SimpleUnbakedExtraModel.blockStateModel(copperArm));
            pluginContext.addModel(MODEL.COPPER_ARM_EXPOSED, SimpleUnbakedExtraModel.blockStateModel(copperArmExposed));
            pluginContext.addModel(MODEL.COPPER_ARM_WEATHERED, SimpleUnbakedExtraModel.blockStateModel(copperArmWeathered));
            pluginContext.addModel(MODEL.COPPER_ARM_OXIDIZED, SimpleUnbakedExtraModel.blockStateModel(copperArmOxidized));
            pluginContext.addModel(MODEL.COPPER_ARM_EXTENDED, SimpleUnbakedExtraModel.blockStateModel(copperArmExtended));
            pluginContext.addModel(MODEL.COPPER_ARM_EXTENDED_EXPOSED, SimpleUnbakedExtraModel.blockStateModel(copperArmExtendedExposed));
            pluginContext.addModel(MODEL.COPPER_ARM_EXTENDED_WEATHERED, SimpleUnbakedExtraModel.blockStateModel(copperArmExtendedWeathered));
            pluginContext.addModel(MODEL.COPPER_ARM_EXTENDED_OXIDIZED, SimpleUnbakedExtraModel.blockStateModel(copperArmExtendedOxidized));
            pluginContext.addModel(MODEL.GOLD_CORE, SimpleUnbakedExtraModel.blockStateModel(goldCore));
            pluginContext.addModel(MODEL.GOLD_CORE_POWERED, SimpleUnbakedExtraModel.blockStateModel(goldCorePowered));
            pluginContext.addModel(MODEL.GOLD_ARM, SimpleUnbakedExtraModel.blockStateModel(goldArm));
            pluginContext.addModel(MODEL.GOLD_ARM_POWERED, SimpleUnbakedExtraModel.blockStateModel(goldArmPowered));
            pluginContext.addModel(MODEL.GOLD_ARM_EXTENDED, SimpleUnbakedExtraModel.blockStateModel(goldArmExtended));
            pluginContext.addModel(MODEL.GOLD_ARM_EXTENDED_POWERED, SimpleUnbakedExtraModel.blockStateModel(goldArmExtendedPowered));
            pluginContext.addModel(MODEL.EXTRACTOR_CORE, SimpleUnbakedExtraModel.blockStateModel(extractorCore));
            pluginContext.addModel(MODEL.EXTRACTOR_ARM, SimpleUnbakedExtraModel.blockStateModel(extractorArm));
            pluginContext.addModel(MODEL.EXTRACTOR_ARM_EXTENDED, SimpleUnbakedExtraModel.blockStateModel(extractorArmExtended));
            pluginContext.addModel(MODEL.EXTRACTOR_FEATURE, SimpleUnbakedExtraModel.blockStateModel(extractorFeature));
            pluginContext.addModel(MODEL.EXTRACTOR_FEATURE_EXTENDED, SimpleUnbakedExtraModel.blockStateModel(extractorFeatureExtended));
            pluginContext.addModel(MODEL.FILTER_CORE, SimpleUnbakedExtraModel.blockStateModel(filterCore));
            pluginContext.addModel(MODEL.FILTER_ARM, SimpleUnbakedExtraModel.blockStateModel(filterArm));
            pluginContext.addModel(MODEL.FILTER_ARM_EXTENDED, SimpleUnbakedExtraModel.blockStateModel(filterArmExtended));
            pluginContext.addModel(MODEL.INSERTION_CORE, SimpleUnbakedExtraModel.blockStateModel(insertionCore));
            pluginContext.addModel(MODEL.INSERTION_ARM, SimpleUnbakedExtraModel.blockStateModel(insertionArm));
            pluginContext.addModel(MODEL.INSERTION_ARM_EXTENDED, SimpleUnbakedExtraModel.blockStateModel(insertionArmExtended));
            pluginContext.addModel(MODEL.MERGER_CORE, SimpleUnbakedExtraModel.blockStateModel(mergerCore));
            pluginContext.addModel(MODEL.MERGER_ARM, SimpleUnbakedExtraModel.blockStateModel(mergerArm));
            pluginContext.addModel(MODEL.MERGER_ARM_EXTENDED, SimpleUnbakedExtraModel.blockStateModel(mergerArmExtended));
            pluginContext.addModel(MODEL.MERGER_FEATURE, SimpleUnbakedExtraModel.blockStateModel(mergerFeature));
            pluginContext.addModel(MODEL.MERGER_FEATURE_EXTENDED, SimpleUnbakedExtraModel.blockStateModel(mergerFeatureExtended));
            pluginContext.addModel(MODEL.PASSTHROUGH_CORE, SimpleUnbakedExtraModel.blockStateModel(passthroughCore));
            pluginContext.addModel(MODEL.PASSTHROUGH_ARM, SimpleUnbakedExtraModel.blockStateModel(passthroughArm));
            pluginContext.addModel(MODEL.PASSTHROUGH_ARM_EXTENDED, SimpleUnbakedExtraModel.blockStateModel(passthroughArmExtended));
            pluginContext.addModel(MODEL.VOID_CORE, SimpleUnbakedExtraModel.blockStateModel(voidCore));
            pluginContext.addModel(MODEL.VOID_ARM, SimpleUnbakedExtraModel.blockStateModel(voidArm));
            pluginContext.addModel(MODEL.VOID_ARM_EXTENDED, SimpleUnbakedExtraModel.blockStateModel(voidArmExtended));
        });
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsPipe
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering pipe (client)");

        // TODO: Re-enable render layer map once API compatibility is resolved
        // These calls configure transparent rendering for pipes
        // BlockRenderLayerMap.INSTANCE.putBlock(PipeBlocks.STONE_TRANSPORT_PIPE, RenderType.cutout());
        // ... (other pipes)

        BlockEntityRenderers.register(LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

        MenuScreens.register(LogisticsPipe.SCREEN.ITEM_FILTER, ItemFilterScreen::new);
    }

    public static final class MODEL {
        private static final Map<Identifier, ExtraModelKey<BlockStateModel>> TEMP_LOOKUP = new HashMap<>();

        private static ExtraModelKey<BlockStateModel> registerModel(String name) {
            Identifier id = LogisticsPipe.blockModelIdentifier(name);
            ExtraModelKey<BlockStateModel> key = ExtraModelKey.create(id::toString);
            TEMP_LOOKUP.put(id, key);
            return key;
        }

        public static final ExtraModelKey<BlockStateModel> PIPE_MARKINGS = registerModel("pipe_markings");
        public static final ExtraModelKey<BlockStateModel> STONE_CORE = registerModel("stone_transport_pipe_core");
        public static final ExtraModelKey<BlockStateModel> STONE_ARM = registerModel("stone_transport_pipe_arm");
        public static final ExtraModelKey<BlockStateModel> STONE_ARM_EXTENDED = registerModel("stone_transport_pipe_arm_extended");
        public static final ExtraModelKey<BlockStateModel> COPPER_CORE = registerModel("copper_transport_pipe_core");
        public static final ExtraModelKey<BlockStateModel> COPPER_CORE_EXPOSED = registerModel("copper_transport_pipe_core_exposed");
        public static final ExtraModelKey<BlockStateModel> COPPER_CORE_WEATHERED = registerModel("copper_transport_pipe_core_weathered");
        public static final ExtraModelKey<BlockStateModel> COPPER_CORE_OXIDIZED = registerModel("copper_transport_pipe_core_oxidized");
        public static final ExtraModelKey<BlockStateModel> COPPER_ARM = registerModel("copper_transport_pipe_arm");
        public static final ExtraModelKey<BlockStateModel> COPPER_ARM_EXPOSED = registerModel("copper_transport_pipe_arm_exposed");
        public static final ExtraModelKey<BlockStateModel> COPPER_ARM_WEATHERED = registerModel("copper_transport_pipe_arm_weathered");
        public static final ExtraModelKey<BlockStateModel> COPPER_ARM_OXIDIZED = registerModel("copper_transport_pipe_arm_oxidized");
        public static final ExtraModelKey<BlockStateModel> COPPER_ARM_EXTENDED = registerModel("copper_transport_pipe_arm_extended");
        public static final ExtraModelKey<BlockStateModel> COPPER_ARM_EXTENDED_EXPOSED = registerModel("copper_transport_pipe_arm_extended_exposed");
        public static final ExtraModelKey<BlockStateModel> COPPER_ARM_EXTENDED_WEATHERED = registerModel("copper_transport_pipe_arm_extended_weathered");
        public static final ExtraModelKey<BlockStateModel> COPPER_ARM_EXTENDED_OXIDIZED = registerModel("copper_transport_pipe_arm_extended_oxidized");
        public static final ExtraModelKey<BlockStateModel> GOLD_CORE = registerModel("gold_transport_pipe_core");
        public static final ExtraModelKey<BlockStateModel> GOLD_CORE_POWERED = registerModel("gold_transport_pipe_core_powered");
        public static final ExtraModelKey<BlockStateModel> GOLD_ARM = registerModel("gold_transport_pipe_arm");
        public static final ExtraModelKey<BlockStateModel> GOLD_ARM_POWERED = registerModel("gold_transport_pipe_arm_powered");
        public static final ExtraModelKey<BlockStateModel> GOLD_ARM_EXTENDED = registerModel("gold_transport_pipe_arm_extended");
        public static final ExtraModelKey<BlockStateModel> GOLD_ARM_EXTENDED_POWERED = registerModel("gold_transport_pipe_arm_extended_powered");
        public static final ExtraModelKey<BlockStateModel> EXTRACTOR_CORE = registerModel("item_extractor_pipe_core");
        public static final ExtraModelKey<BlockStateModel> EXTRACTOR_ARM = registerModel("item_extractor_pipe_arm");
        public static final ExtraModelKey<BlockStateModel> EXTRACTOR_ARM_EXTENDED = registerModel("item_extractor_pipe_arm_extended");
        public static final ExtraModelKey<BlockStateModel> EXTRACTOR_FEATURE = registerModel("item_extractor_pipe_feature");
        public static final ExtraModelKey<BlockStateModel> EXTRACTOR_FEATURE_EXTENDED = registerModel("item_extractor_pipe_feature_extended");
        public static final ExtraModelKey<BlockStateModel> FILTER_CORE = registerModel("item_filter_pipe_core");
        public static final ExtraModelKey<BlockStateModel> FILTER_ARM = registerModel("item_filter_pipe_arm");
        public static final ExtraModelKey<BlockStateModel> FILTER_ARM_EXTENDED = registerModel("item_filter_pipe_arm_extended");
        public static final ExtraModelKey<BlockStateModel> INSERTION_CORE = registerModel("item_insertion_pipe_core");
        public static final ExtraModelKey<BlockStateModel> INSERTION_ARM = registerModel("item_insertion_pipe_arm");
        public static final ExtraModelKey<BlockStateModel> INSERTION_ARM_EXTENDED = registerModel("item_insertion_pipe_arm_extended");
        public static final ExtraModelKey<BlockStateModel> MERGER_CORE = registerModel("item_merger_pipe_core");
        public static final ExtraModelKey<BlockStateModel> MERGER_ARM = registerModel("item_merger_pipe_arm");
        public static final ExtraModelKey<BlockStateModel> MERGER_ARM_EXTENDED = registerModel("item_merger_pipe_arm_extended");
        public static final ExtraModelKey<BlockStateModel> MERGER_FEATURE = registerModel("item_merger_pipe_feature");
        public static final ExtraModelKey<BlockStateModel> MERGER_FEATURE_EXTENDED = registerModel("item_merger_pipe_feature_extended");
        public static final ExtraModelKey<BlockStateModel> PASSTHROUGH_CORE = registerModel("item_passthrough_pipe_core");
        public static final ExtraModelKey<BlockStateModel> PASSTHROUGH_ARM = registerModel("item_passthrough_pipe_arm");
        public static final ExtraModelKey<BlockStateModel> PASSTHROUGH_ARM_EXTENDED = registerModel("item_passthrough_pipe_arm_extended");
        public static final ExtraModelKey<BlockStateModel> VOID_CORE = registerModel("item_void_pipe_core");
        public static final ExtraModelKey<BlockStateModel> VOID_ARM = registerModel("item_void_pipe_arm");
        public static final ExtraModelKey<BlockStateModel> VOID_ARM_EXTENDED = registerModel("item_void_pipe_arm_extended");

        private static final Map<Identifier, ExtraModelKey<BlockStateModel>> MODEL_LOOKUP = Map.copyOf(TEMP_LOOKUP);

        @Nullable
        public static ExtraModelKey<BlockStateModel> getKey(Identifier modelId) {
            return MODEL_LOOKUP.get(modelId);
        }

        private MODEL() {}
    }
}

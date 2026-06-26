package com.logistics;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.PipeTypes;
import com.logistics.pipe.block.FluidPipeBlock;
import com.logistics.pipe.block.FluidPumpBlock;
import com.logistics.pipe.item.FluidPipeBlockItem;
import com.logistics.pipe.block.GlassTankBlock;
import com.logistics.pipe.item.GlassTankBlockItem;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.block.entity.FluidPumpBlockEntity;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Fluid registration. No longer an independent {@link com.logistics.core.bootstrap.DomainBootstrap}:
 * fluid is now part of the pipe domain (so fluid pipes can compose pipe modules). Registration is driven
 * from {@link LogisticsPipe#initCommon()} via {@link #registerCommon()}; the client renderers are wired
 * from the pipe client bootstrap / NeoForge client setup. It keeps its own {@code fluid} asset namespace.
 */
public final class LogisticsFluid extends LogisticsMod {
    private static final LogisticsFluid INSTANCE = new LogisticsFluid();

    @Override
    protected String domain() {
        return "fluid";
    }

    public static ResourceId resource(String name) {
        return INSTANCE.domainResource(name);
    }

    public static ResourceId model(String name) {
        return INSTANCE.domainModelResource(name);
    }

    /** Register all fluid blocks, block entities, and creative entries. Called by the pipe domain bootstrap. */
    public static void registerCommon() {
        LOGGER.info("Registering {}", INSTANCE.domain());

        BLOCK.register();
        ENTITY.register();
        CREATIVE.register();
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static Block COPPER_FLUID_PIPE;
        public static Block STONE_FLUID_PIPE;
        public static Block GOLD_FLUID_PIPE;
        public static Block INSERTION_FLUID_PIPE;
        public static Block MERGER_FLUID_PIPE;
        public static Block FLUID_EXTRACTOR_PIPE;
        public static Block VOID_FLUID_PIPE;
        public static Block BYPASS_FLUID_PIPE;
        public static Block GLASS_TANK;
        public static Block FLUID_PUMP;

        private static BlockBehaviour.Properties fluidPipeProps(BlockBehaviour.Properties props) {
            return props.mapColor(MapColor.NONE)
                    .strength(0.3f)
                    .sound(SoundType.METAL)
                    .noOcclusion();
        }

        private static BlockBehaviour.Properties tankProps(BlockBehaviour.Properties props) {
            return props.mapColor(MapColor.NONE)
                    .strength(0.5f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isValidSpawn((s, w, p, t) -> false)
                    .isRedstoneConductor((s, w, p) -> false)
                    .isSuffocating((s, w, p) -> false)
                    .isViewBlocking((s, w, p) -> false);
        }

        static void register() {
            COPPER_FLUID_PIPE = INSTANCE.registerBlockWithItem("copper_fluid_pipe",
                    props -> new FluidPipeBlock(fluidPipeProps(props), PipeTypes.COPPER_FLUID_PIPE),
                    FluidPipeBlockItem::new);
            STONE_FLUID_PIPE = INSTANCE.registerBlockWithItem("stone_fluid_pipe",
                    props -> new FluidPipeBlock(fluidPipeProps(props), PipeTypes.STONE_FLUID_PIPE),
                    FluidPipeBlockItem::new);
            GOLD_FLUID_PIPE = INSTANCE.registerBlockWithItem("gold_fluid_pipe",
                    props -> new FluidPipeBlock(fluidPipeProps(props), PipeTypes.GOLD_FLUID_PIPE),
                    FluidPipeBlockItem::new);
            INSERTION_FLUID_PIPE = INSTANCE.registerBlockWithItem("insertion_fluid_pipe",
                    props -> new FluidPipeBlock(fluidPipeProps(props), PipeTypes.INSERTION_FLUID_PIPE),
                    FluidPipeBlockItem::new);
            MERGER_FLUID_PIPE = INSTANCE.registerBlockWithItem("merger_fluid_pipe",
                    props -> new FluidPipeBlock(fluidPipeProps(props), PipeTypes.MERGER_FLUID_PIPE),
                    FluidPipeBlockItem::new);
            FLUID_EXTRACTOR_PIPE = INSTANCE.registerBlockWithItem("fluid_extractor_pipe",
                    props -> new FluidPipeBlock(fluidPipeProps(props), PipeTypes.FLUID_EXTRACTOR_PIPE),
                    FluidPipeBlockItem::new);
            VOID_FLUID_PIPE = INSTANCE.registerBlockWithItem("void_fluid_pipe",
                    props -> new FluidPipeBlock(fluidPipeProps(props), PipeTypes.VOID_FLUID_PIPE),
                    FluidPipeBlockItem::new);
            BYPASS_FLUID_PIPE = INSTANCE.registerBlockWithItem("bypass_fluid_pipe",
                    props -> new FluidPipeBlock(fluidPipeProps(props), PipeTypes.BYPASS_FLUID_PIPE),
                    FluidPipeBlockItem::new);
            GLASS_TANK = INSTANCE.registerBlockWithItem("glass_tank",
                    props -> new GlassTankBlock(tankProps(props)),
                    GlassTankBlockItem::new);
            FLUID_PUMP = INSTANCE.registerBlockWithItem("fluid_pump",
                    props -> new FluidPumpBlock(props.mapColor(MapColor.METAL)
                            .strength(3.5f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()));
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        public static BlockEntityType<FluidPipeBlockEntity> FLUID_PIPE_BLOCK_ENTITY;
        public static BlockEntityType<GlassTankBlockEntity> GLASS_TANK_BLOCK_ENTITY;
        public static BlockEntityType<FluidPumpBlockEntity> FLUID_PUMP_BLOCK_ENTITY;

        static void register() {
            FLUID_PIPE_BLOCK_ENTITY = INSTANCE.registerBlockEntity("fluid_pipe",
                    FluidPipeBlockEntity::new,
                    BLOCK.COPPER_FLUID_PIPE,
                    BLOCK.STONE_FLUID_PIPE,
                    BLOCK.GOLD_FLUID_PIPE,
                    BLOCK.INSERTION_FLUID_PIPE,
                    BLOCK.MERGER_FLUID_PIPE,
                    BLOCK.FLUID_EXTRACTOR_PIPE,
                    BLOCK.VOID_FLUID_PIPE,
                    BLOCK.BYPASS_FLUID_PIPE);
            GLASS_TANK_BLOCK_ENTITY = INSTANCE.registerBlockEntity("glass_tank",
                    GlassTankBlockEntity::new,
                    BLOCK.GLASS_TANK);
            FLUID_PUMP_BLOCK_ENTITY = INSTANCE.registerBlockEntity("fluid_pump",
                    FluidPumpBlockEntity::new,
                    BLOCK.FLUID_PUMP);
        }
    }

    public static final class CREATIVE {
        private CREATIVE() {}

        static void register() {
            // Copper fluid pipe weathering variants (exposed/weathered/oxidized + waxed); the base entry
            // is added below.
            LogisticsCore.CREATIVE.TAB.add(entries -> {
                if (BLOCK.COPPER_FLUID_PIPE instanceof FluidPipeBlock pipeBlock && pipeBlock.fluidPipe() != null) {
                    ItemStack baseStack = new ItemStack(BLOCK.COPPER_FLUID_PIPE);
                    List<ItemStack> variants = new ArrayList<>();
                    pipeBlock.fluidPipe().appendCreativeMenuVariants(variants, baseStack);
                    variants.forEach(entries::accept);
                }
            });

            LogisticsCore.CREATIVE.TAB.add(BLOCK.COPPER_FLUID_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.STONE_FLUID_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.GOLD_FLUID_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.INSERTION_FLUID_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.MERGER_FLUID_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.FLUID_EXTRACTOR_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.VOID_FLUID_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.BYPASS_FLUID_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.GLASS_TANK);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.FLUID_PUMP);
        }
    }
}

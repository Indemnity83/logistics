package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.fluid.block.FluidPipeBlock;
import com.logistics.fluid.block.FluidPipeBlockItem;
import com.logistics.fluid.block.FluidPipeKind;
import com.logistics.fluid.block.GlassTankBlock;
import com.logistics.fluid.block.GlassTankBlockItem;
import com.logistics.fluid.block.entity.FluidPipeBlockEntity;
import com.logistics.fluid.block.entity.GlassTankBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class LogisticsFluid extends LogisticsMod implements DomainBootstrap {
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

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        BLOCK.register();
        ENTITY.register();
        CREATIVE.register();
    }

    public static final class BLOCK {
        private BLOCK() {}

        public static Block COPPER_FLUID_PIPE;
        public static Block FLUID_EXTRACTOR_PIPE;
        public static Block GLASS_TANK;

        private static BlockBehaviour.Properties fluidPipeProps(BlockBehaviour.Properties props) {
            return props.mapColor(MapColor.NONE)
                    .strength(0.25f)
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
                    props -> new FluidPipeBlock(fluidPipeProps(props), FluidPipeKind.COPPER),
                    FluidPipeBlockItem::new);
            FLUID_EXTRACTOR_PIPE = INSTANCE.registerBlockWithItem("fluid_extractor_pipe",
                    props -> new FluidPipeBlock(fluidPipeProps(props), FluidPipeKind.EXTRACTOR),
                    FluidPipeBlockItem::new);
            GLASS_TANK = INSTANCE.registerBlockWithItem("glass_tank",
                    props -> new GlassTankBlock(tankProps(props)),
                    GlassTankBlockItem::new);
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        public static BlockEntityType<FluidPipeBlockEntity> FLUID_PIPE_BLOCK_ENTITY;
        public static BlockEntityType<GlassTankBlockEntity> GLASS_TANK_BLOCK_ENTITY;

        static void register() {
            FLUID_PIPE_BLOCK_ENTITY = INSTANCE.registerBlockEntity("fluid_pipe",
                    FluidPipeBlockEntity::new,
                    BLOCK.COPPER_FLUID_PIPE,
                    BLOCK.FLUID_EXTRACTOR_PIPE);
            GLASS_TANK_BLOCK_ENTITY = INSTANCE.registerBlockEntity("glass_tank",
                    GlassTankBlockEntity::new,
                    BLOCK.GLASS_TANK);
        }
    }

    public static final class CREATIVE {
        private CREATIVE() {}

        static void register() {
            LogisticsCore.CREATIVE.TAB.add(BLOCK.COPPER_FLUID_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.FLUID_EXTRACTOR_PIPE);
            LogisticsCore.CREATIVE.TAB.add(BLOCK.GLASS_TANK);
        }
    }
}

package com.logistics;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigEntries;
import com.indemnity83.configory.ConfigKey;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.LogisticsCreativeTab;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class LogisticsForestry extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsForestry INSTANCE = new LogisticsForestry();

    @Override
    protected String domain() {
        return "forestry";
    }

    public static ResourceId resource(String name) {
        return INSTANCE.domainResource(name);
    }

    public static ResourceId model(String name) {
        return INSTANCE.domainModelResource(name);
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public void registerConfig() {
        CONFIG.register();
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        BLOCK.register();
        ITEM.register();
        ENTITY.register();
        MENU.register();
        RECIPE.register();
        CREATIVE.register();
        ALIAS.register();
    }

    /** Farm consumables: {@code config/logistics/forestry/fertilizer.json}. */
    public static final class CONFIG extends ConfigEntries {
        private static final Config fertilizer = configFor(LogisticsConfigHost.MOD_ID, "forestry.fertilizer");

        private CONFIG() {}

        public static final ConfigKey<Integer> FERTILIZER_CHARGES = fertilizer.defineInt("fertilizer_charges", 8)
                .min(1)
                .describe("Growth charges one Fertilizer gives a farm")
                .register();

        public static final ConfigKey<Integer> BONE_MEAL_CHARGES = fertilizer.defineInt("bone_meal_charges", 1)
                .min(1)
                .describe("Growth charges one Bone Meal gives a farm")
                .register();

        static void register() {}
    }

    public static final class TAG {
        private TAG() {}

        /** Items a farm's fertilizer slot accepts. */
        public static final TagKey<Item> FERTILIZERS =
                TagKey.create(Registries.ITEM, resource("fertilizers").toIdentifier());
    }

    public static final class BLOCK {
        private BLOCK() {}

        static void register() {}
    }

    public static final class ITEM {
        private ITEM() {}

        public static Item FERTILIZER;

        static void register() {
            FERTILIZER = INSTANCE.registerItem("fertilizer", Item::new);
        }
    }

    public static final class ENTITY {
        private ENTITY() {}

        static void register() {}
    }

    public static final class MENU {
        private MENU() {}

        static void register() {}
    }

    public static final class RECIPE {
        private RECIPE() {}

        static void register() {}
    }

    public static final class CREATIVE {
        public static final LogisticsCreativeTab TAB = LogisticsCreativeTab.create(
            LogisticsMod.modId("5_forestry"),
            Component.translatable("itemGroup.logistics.5_forestry"),
            () -> new ItemStack(ITEM.FERTILIZER)
        );

        private CREATIVE() {}

        static void register() {
            TAB.add(ITEM.FERTILIZER);
            CreativeTabRegistrar.INSTANCE.registerTab(TAB);
        }
    }

    public static final class ALIAS {
        private ALIAS() {}

        static void register() {}
    }
}

package com.logistics.core;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.test.MinecraftTestEnvironment;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogisticsCommandTree")
class LogisticsCommandTreeTest extends MinecraftTestEnvironment {

    @BeforeAll
    static void registerDomainConfigs() {
        // The command enumerates registered per-domain configs (childConfigs). Touch a key from each domain so
        // its CONFIG class initializes and its configFor child is registered — force-init only, no sanitize hooks.
        LogisticsPower.CONFIG.REDSTONE_OUTPUT.configId();
        LogisticsAutomation.CONFIG.QUARRY_AREA.configId();
        LogisticsPipe.CONFIG.PIPE_MAX_SPEED.configId();
        LogisticsPipe.CONFIG.FLUID_PUMP_SEARCH_RADIUS.configId();
        LogisticsCore.CONFIG.CRASH_REPORTING_ENABLED.configId();
    }

    @Test
    @DisplayName("builds /logistics with debug and the configory-generated config surface")
    void buildsConfigSurface() {
        CommandNode<CommandSourceStack> root = LogisticsCommandTree.build().build();
        assertThat(root.getName()).isEqualTo("logistics");
        assertThat(root.getChild("debug")).isNotNull();

        // Configory groups each config file under `config <group>` (its id's last segment) with short keys,
        // plus a sibling `reload-configs`. Machines/engines are per-unit files, so their group is the unit name.
        CommandNode<CommandSourceStack> config = root.getChild("config");
        assertThat(config).isNotNull();
        assertThat(config.getChild("quarry").getChild("area")).isNotNull();
        assertThat(config.getChild("pipes").getChild("max_speed")).isNotNull();
        assertThat(config.getChild("reporting").getChild("enabled")).isNotNull();
        assertThat(root.getChild("reload-configs")).isNotNull();
    }

    @Test
    @DisplayName("exposes the dev-only test subcommand in a development environment")
    void exposesTestSubcommandInDev() {
        // TestPlatformService reports a development environment, so the dev-gated
        // /logistics diagnostics test node is registered.
        CommandNode<CommandSourceStack> root = LogisticsCommandTree.build().build();
        CommandNode<CommandSourceStack> diagnostics = root.getChild("diagnostics");
        assertThat(diagnostics.getChild("test")).isNotNull();
    }
}

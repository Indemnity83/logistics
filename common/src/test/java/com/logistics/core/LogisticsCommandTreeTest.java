package com.logistics.core;

import com.logistics.test.MinecraftTestEnvironment;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogisticsCommandTree")
class LogisticsCommandTreeTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("builds /logistics with debug and the configory-generated config surface")
    void buildsConfigSurface() {
        CommandNode<CommandSourceStack> root = LogisticsCommandTree.build().build();
        assertThat(root.getName()).isEqualTo("logistics");
        assertThat(root.getChild("debug")).isNotNull();

        // Configory groups each domain under `config <domain>` with short keys, plus a sibling `reload-configs`.
        CommandNode<CommandSourceStack> config = root.getChild("config");
        assertThat(config).isNotNull();
        assertThat(config.getChild("machines").getChild("quarry_area")).isNotNull();
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

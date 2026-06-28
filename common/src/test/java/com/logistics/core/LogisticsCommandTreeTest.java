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
    @DisplayName("builds /logistics with the diagnostics subcommands")
    void buildsDiagnosticsSubtree() {
        CommandNode<CommandSourceStack> root = LogisticsCommandTree.build().build();
        assertThat(root.getName()).isEqualTo("logistics");

        CommandNode<CommandSourceStack> diagnostics = root.getChild("diagnostics");
        assertThat(diagnostics).isNotNull();
        assertThat(diagnostics.getChild("enable")).isNotNull();
        assertThat(diagnostics.getChild("disable")).isNotNull();
        assertThat(diagnostics.getChild("preview")).isNotNull();

        CommandNode<CommandSourceStack> notify = diagnostics.getChild("notify");
        assertThat(notify).isNotNull();
        assertThat(notify.getChild("on")).isNotNull();
        assertThat(notify.getChild("off")).isNotNull();
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

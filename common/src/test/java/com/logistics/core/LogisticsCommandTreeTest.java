package com.logistics.core;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogisticsCommandTree")
class LogisticsCommandTreeTest {

    @Test
    @DisplayName("builds /logistics with the crashreports subcommands")
    void buildsCrashreportsSubtree() {
        CommandNode<CommandSourceStack> root = LogisticsCommandTree.build().build();
        assertThat(root.getName()).isEqualTo("logistics");

        CommandNode<CommandSourceStack> crash = root.getChild("crashreports");
        assertThat(crash).isNotNull();
        assertThat(crash.getChild("enable")).isNotNull();
        assertThat(crash.getChild("disable")).isNotNull();
        assertThat(crash.getChild("preview")).isNotNull();

        CommandNode<CommandSourceStack> notify = crash.getChild("notify");
        assertThat(notify).isNotNull();
        assertThat(notify.getChild("on")).isNotNull();
        assertThat(notify.getChild("off")).isNotNull();
    }
}

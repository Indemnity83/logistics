package com.logistics.core.lib.client.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MachineModels textures")
class MachineModelTexturesTest {

    @Test
    @DisplayName("every code-generated model's texture is shipped")
    void everyModelTextureShips() {
        List<String> missing = MachineModels.keys().stream()
                .sorted()
                .filter(key -> {
                    String path = "assets/logistics/textures/block/" + MachineModels.get(key).textureBase() + ".png";
                    return MachineModels.class.getClassLoader().getResource(path) == null;
                })
                .map(key -> key + " -> block/" + MachineModels.get(key).textureBase())
                .toList();

        assertThat(missing).as("machine models whose texture is not shipped").isEmpty();
    }
}

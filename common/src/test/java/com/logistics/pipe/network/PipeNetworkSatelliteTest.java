package com.logistics.pipe.network;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link PipeNetwork}'s satellite registry — the name-to-position lookup the Process pipe routes by. */
@DisplayName("PipeNetwork satellite registry")
class PipeNetworkSatelliteTest {

    private static final BlockPos FIRST = new BlockPos(0, 0, 0);
    private static final BlockPos SECOND = new BlockPos(5, 0, 0);

    private PipeNetwork network;

    @BeforeEach
    @SuppressWarnings("deprecation")
    void setUp() {
        network = new PipeNetwork(UUID.randomUUID());
    }

    /**
     * Wiki claim (Usage): "registers a named destination so other pipes can route to it by name
     * rather than location."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Satellite_Logistics_Pipe#Usage">wiki/Satellite Logistics Pipe.txt § Usage</a>
     */
    @Test
    @DisplayName("a registered satellite name resolves to that pipe's position")
    void registeredNameResolvesToItsPipe() {
        network.registerSatellite("1", FIRST);

        assertThat(network.findSatellite("1")).isEqualTo(FIRST);
        assertThat(network.getAvailableSatelliteIds()).containsExactly("1");
    }

    /**
     * Wiki claim (Usage): "Several satellites can share a name to load-balance deliveries."
     *
     * <p>NOTE: wiki/Satellite Logistics Pipe.txt claims shared names load-balance; the registry
     * holds one position per name and ignores a second pipe claiming a name that is taken, so every
     * delivery goes to the first satellite. The satellite GUI also skips names other satellites
     * hold. See WIKI_DISCREPANCIES.md.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Satellite_Logistics_Pipe#Usage">wiki/Satellite Logistics Pipe.txt § Usage</a>
     */
    @Test
    @DisplayName("a second satellite cannot take a name that is already registered")
    void secondSatelliteWithTakenNameIsIgnored() {
        network.registerSatellite("1", FIRST);
        network.registerSatellite("1", SECOND);

        assertThat(network.findSatellite("1")).isEqualTo(FIRST);
    }

    @Test
    @DisplayName("a name is released only by the pipe that holds it")
    void unregisterOnlyReleasesTheHoldersName() {
        network.registerSatellite("1", FIRST);

        network.unregisterSatellite("1", SECOND);
        assertThat(network.findSatellite("1")).isEqualTo(FIRST);

        network.unregisterSatellite("1", FIRST);
        assertThat(network.findSatellite("1")).isNull();
    }
}

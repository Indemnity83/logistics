package com.logistics.core.crash;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Log4j2ErrorLogBridge")
class Log4j2ErrorLogBridgeTest {

    @Test
    @DisplayName("forwards Logistics ERROR exceptions while attached, and stops after detach")
    void attachForwardsAndDetachStops() {
        List<Throwable> captured = new ArrayList<>();
        Log4j2ErrorLogBridge bridge = new Log4j2ErrorLogBridge(captured::add);
        bridge.attach();
        try {
            Logger logistics = LogManager.getLogger("logistics");
            Logger foreign = LogManager.getLogger("net.minecraft.Foo");

            logistics.error("boom", new IllegalStateException("logistics error")); // forwarded
            logistics.warn("noise", new RuntimeException("warn level"));            // below ERROR
            logistics.error("no throwable");                                        // no throwable
            foreign.error("not ours", new RuntimeException("foreign"));             // foreign logger

            assertThat(captured).hasSize(1);
            assertThat(captured.get(0)).isInstanceOf(IllegalStateException.class);
        } finally {
            bridge.detach();
        }

        captured.clear();
        LogManager.getLogger("logistics").error("after detach", new RuntimeException("ignored"));
        assertThat(captured).isEmpty();
    }
}

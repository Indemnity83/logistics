package com.logistics.core.crash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("CrashReporting")
class CrashReportingTest {

    @Test
    @DisplayName("is inactive by default and capture is a no-op when disabled")
    void captureIsNoOpWhenDisabled() {
        assertThat(CrashReporting.isActive()).isFalse();
        assertThatCode(() -> CrashReporting.capture(new RuntimeException("boom"))).doesNotThrowAnyException();
        assertThatCode(() -> CrashReporting.capture(null)).doesNotThrowAnyException();
        assertThat(CrashReporting.isActive()).isFalse();
    }

    @Test
    @DisplayName("redacts the local user name and home directory")
    void redactsLocalIdentity() {
        String home = System.getProperty("user.home");
        String user = System.getProperty("user.name");

        String message = "failed to read " + home + "/saves by " + user;
        String redacted = CrashReporting.redact(message);

        assertThat(redacted).doesNotContain(home);
        assertThat(redacted).contains("~");
        if (user != null && !user.isBlank()) {
            assertThat(redacted).doesNotContain("by " + user);
            assertThat(redacted).contains("<user>");
        }
    }

    @Test
    @DisplayName("redact passes through null and empty unchanged")
    void redactPassesThroughNullAndEmpty() {
        assertThat(CrashReporting.redact(null)).isNull();
        assertThat(CrashReporting.redact("")).isEmpty();
    }

    @Test
    @DisplayName("redacts IPs, UUIDs, any-user home paths, and secret-like values")
    void redactsCommonIdentifiers() {
        assertThat(CrashReporting.redact("connect to 192.168.1.50:25565"))
                .contains("<ip>").doesNotContain("192.168.1.50");
        assertThat(CrashReporting.redact("player 123e4567-e89b-12d3-a456-426614174000 left"))
                .contains("<uuid>").doesNotContain("123e4567");
        assertThat(CrashReporting.redact("read /Users/alice/world"))
                .contains("/Users/<user>").doesNotContain("alice");
        assertThat(CrashReporting.redact("opening /home/bob/.minecraft"))
                .contains("/home/<user>").doesNotContain("bob");
        assertThat(CrashReporting.redact("C:\\Users\\Carol\\saves"))
                .contains("C:\\Users\\<user>").doesNotContain("Carol");
        assertThat(CrashReporting.redact("token=abc123secretvalue"))
                .contains("<redacted>").doesNotContain("abc123secretvalue");
    }

    @Test
    @DisplayName("forwards only Logistics loggers that carry a throwable")
    void forwardsOnlyLogisticsErrorsWithThrowable() {
        // Matches the codebase's logger names (e.g. "logistics", "logistics/config", "Logistics/JEI")
        assertThat(Log4j2ErrorLogBridge.shouldForward("logistics", true)).isTrue();
        assertThat(Log4j2ErrorLogBridge.shouldForward("logistics/config", true)).isTrue();
        assertThat(Log4j2ErrorLogBridge.shouldForward("Logistics/JEI", true)).isTrue();
        assertThat(Log4j2ErrorLogBridge.shouldForward("com.logistics.pipe.Pipe", true)).isTrue();

        // No throwable, foreign loggers, or null name are not forwarded.
        assertThat(Log4j2ErrorLogBridge.shouldForward("logistics", false)).isFalse();
        assertThat(Log4j2ErrorLogBridge.shouldForward("net.minecraft.server.Main", true)).isFalse();
        assertThat(Log4j2ErrorLogBridge.shouldForward("some.other.mod", true)).isFalse();
        assertThat(Log4j2ErrorLogBridge.shouldForward(null, true)).isFalse();
    }
}

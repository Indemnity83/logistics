package com.logistics.core.crash;

import com.logistics.core.LogisticsConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("CrashReporting")
class CrashReportingTest {

    // A syntactically valid DSN on localhost so enabling never reaches the real Sentry project;
    // any queued event just fails to connect and is dropped on a background thread.
    private static final String LOCAL_DSN = "http://examplekey@localhost/1";

    @AfterEach
    void cleanup() {
        CrashReporting.disable();
        LogisticsConfig.CrashReportingConfig cfg = LogisticsConfig.get().crashReporting;
        cfg.enabled = false;
        cfg.notifyOperators = true;
        cfg.dsnOverride = "";
    }

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
    @DisplayName("preview builds a scrubbed report without sending or enabling")
    void previewIsScrubbedAndInert() {
        String json = CrashReporting.previewReport();

        // Contains the synthetic exception, but the mock sensitive values are redacted.
        assertThat(json).contains("RuntimeException").contains("preview");
        assertThat(json).contains("<ip>").doesNotContain("203.0.113.7");
        assertThat(json).contains("<uuid>").doesNotContain("123e4567-e89b-12d3-a456-426614174000");
        assertThat(json).contains("<redacted>").doesNotContain("hunter2");
        assertThat(json).doesNotContain(System.getProperty("user.home"));

        // Previewing neither enables reporting nor sends anything.
        assertThat(CrashReporting.isActive()).isFalse();
    }

    @Test
    @DisplayName("enable/disable toggles the live client and persists the actual state")
    void enableDisableLifecycle() {
        LogisticsConfig.get().crashReporting.dsnOverride = LOCAL_DSN;

        assertThat(CrashReporting.enableReporting()).isTrue();
        assertThat(CrashReporting.isActive()).isTrue();
        // Config is persisted only to reflect the real active state.
        assertThat(LogisticsConfig.get().crashReporting.enabled).isTrue();

        // Capturing on an active client is accepted without throwing (sent async to the dead DSN).
        assertThatCode(() -> CrashReporting.capture(new RuntimeException("test")))
                .doesNotThrowAnyException();

        CrashReporting.disableReporting();
        assertThat(CrashReporting.isActive()).isFalse();
        assertThat(LogisticsConfig.get().crashReporting.enabled).isFalse();
    }

    @Test
    @DisplayName("enable failure leaves reporting off and persists the disabled state")
    void enableFailureIsSafe() {
        // A malformed DSN makes the Sentry client constructor throw inside enable().
        LogisticsConfig.get().crashReporting.dsnOverride = "https://localhost";

        boolean active = CrashReporting.enableReporting();

        assertThat(active).isFalse();
        assertThat(CrashReporting.isActive()).isFalse();
        assertThat(LogisticsConfig.get().crashReporting.enabled).isFalse();
    }

    @Test
    @DisplayName("bootstrap enables only when the config opted in")
    void bootstrapHonorsConfig() {
        LogisticsConfig.get().crashReporting.enabled = false;
        CrashReporting.bootstrap();
        assertThat(CrashReporting.isActive()).isFalse();

        LogisticsConfig.get().crashReporting.enabled = true;
        LogisticsConfig.get().crashReporting.dsnOverride = LOCAL_DSN;
        CrashReporting.bootstrap();
        assertThat(CrashReporting.isActive()).isTrue();
    }

    @Test
    @DisplayName("join-notice and status actions update and report config")
    void joinNoticeAndStatusActions() {
        assertThat(CrashReporting.setJoinNotice(false)).contains("off");
        assertThat(LogisticsConfig.get().crashReporting.notifyOperators).isFalse();

        assertThat(CrashReporting.setJoinNotice(true)).contains("ON");
        assertThat(LogisticsConfig.get().crashReporting.notifyOperators).isTrue();

        assertThat(CrashReporting.statusText()).contains("Sanitized crash reporting");
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

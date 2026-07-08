package com.logistics.core.crash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.LogisticsConfigHost.Configs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CrashReporting")
class CrashReportingTest {

    // A syntactically valid DSN on localhost so enabling never reaches the real Sentry project;
    // any queued event just fails to connect and is dropped on a background thread.
    private static final String LOCAL_DSN = "http://examplekey@localhost/1";

    /** The shared reporting config the crash keys live on. */
    private static Config reporting() {
        return ConfigRegistry.config(Configs.CRASH_REPORTING_ENABLED.configId());
    }

    @AfterEach
    void cleanup() {
        CrashReporting.disable();
        Config reporting = reporting();
        reporting.set(Configs.CRASH_REPORTING_ENABLED, false);
        reporting.set(Configs.CRASH_REPORTING_NOTIFY_OPERATORS, true);
        reporting.set(Configs.CRASH_REPORTING_DSN_OVERRIDE, "");
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

        // The log-writing wrapper is also inert and must not throw.
        assertThatCode(CrashReporting::logPreviewReport).doesNotThrowAnyException();
        assertThat(CrashReporting.isActive()).isFalse();
    }

    @Test
    @DisplayName("reconcile brings the live client in line with the enabled config key")
    void reconcileTogglesLiveClient() {
        Config reporting = reporting();
        reporting.set(Configs.CRASH_REPORTING_DSN_OVERRIDE, LOCAL_DSN);
        reporting.set(Configs.CRASH_REPORTING_ENABLED, true);

        CrashReporting.reconcile();
        assertThat(CrashReporting.isActive()).isTrue();

        // Capturing on an active client is accepted without throwing (sent async to the dead DSN).
        assertThatCode(() -> CrashReporting.capture(new RuntimeException("test"))).doesNotThrowAnyException();

        reporting.set(Configs.CRASH_REPORTING_ENABLED, false);
        CrashReporting.reconcile();
        assertThat(CrashReporting.isActive()).isFalse();
    }

    @Test
    @DisplayName("reconcile with a malformed DSN leaves reporting off without throwing")
    void reconcileFailureIsSafe() {
        Config reporting = reporting();
        // A malformed DSN makes the Sentry client constructor throw inside enable(); reconcile must contain it.
        reporting.set(Configs.CRASH_REPORTING_DSN_OVERRIDE, "https://localhost");
        reporting.set(Configs.CRASH_REPORTING_ENABLED, true);

        assertThatCode(CrashReporting::reconcile).doesNotThrowAnyException();
        assertThat(CrashReporting.isActive()).isFalse();
    }

    @Test
    @DisplayName("reconcile enables only when the config opted in")
    void reconcileHonorsConfig() {
        Config reporting = reporting();
        reporting.set(Configs.CRASH_REPORTING_ENABLED, false);
        CrashReporting.reconcile();
        assertThat(CrashReporting.isActive()).isFalse();

        reporting.set(Configs.CRASH_REPORTING_DSN_OVERRIDE, LOCAL_DSN);
        reporting.set(Configs.CRASH_REPORTING_ENABLED, true);
        CrashReporting.reconcile();
        assertThat(CrashReporting.isActive()).isTrue();
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

        // Look-alike namespaces from other mods must not pass the filter.
        assertThat(Log4j2ErrorLogBridge.shouldForward("logisticsaddons", true)).isFalse();
        assertThat(Log4j2ErrorLogBridge.shouldForward("other.com.logistics.Thing", true)).isFalse();
    }
}

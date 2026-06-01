package com.logistics.core.crash;

import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.platform.PlatformService;
import io.sentry.JsonSerializer;
import io.sentry.SentryClient;
import io.sentry.SentryEvent;
import io.sentry.SentryExceptionFactory;
import io.sentry.SentryOptions;
import io.sentry.SentryStackTraceFactory;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Opt-in sanitized crash reporting via Sentry. Loader-agnostic — the SDK is pure Java, so all calls
 * live in common; loaders only bundle the SDK and provide version/environment via {@link PlatformService}.
 *
 * <p>Design:
 * <ul>
 *   <li><b>Lazy</b>: nothing initializes until {@link #enable()} is called. A disabled install spins
 *       up no transport thread and makes no network calls.</li>
 *   <li><b>Logistics-only</b>: a dedicated {@link SentryClient} is used rather than the global
 *       {@code Sentry.init()}/{@code Sentry.close()} static API, so we never mutate SDK state shared
 *       with another mod that bundles Sentry. A standalone client installs no global integrations
 *       (no uncaught-exception handler, no shutdown hook); exceptions are captured solely through a
 *       {@link LogisticsErrorLogBridge} scoped to the Logistics loggers, so other mods and vanilla
 *       are never reported.</li>
 *   <li><b>Sanitized</b>: PII is off and the host/server name is not attached; messages and exception
 *       values are scrubbed of home directories, IPs, UUIDs, and secret-like values before send. We
 *       never intentionally send player names, UUIDs, IPs, server addresses, chat, or world data.</li>
 * </ul>
 *
 * <p>Toggled by the {@code /logistics crashreports} commands, which persist {@link LogisticsConfig}
 * and call {@link #enable()}/{@link #disable()} so stored and live state stay in sync.
 */
public final class CrashReporting {
    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/crash");

    /**
     * Public Sentry ingest key (DSN). NOT a secret — DSNs are designed to be embedded in shipped
     * clients. Overridable per-install via {@code crashReporting.dsnOverride} in logistics.json.
     */
    private static final String DEFAULT_DSN =
            "https://677897aaa1709d6e63d47ebbed033218@o148290.ingest.us.sentry.io/4511488473169920";

    /** How long the exit hook waits for queued events to flush, in milliseconds. */
    private static final long FLUSH_TIMEOUT_MS = 2_000L;

    private static final AtomicBoolean ACTIVE = new AtomicBoolean(false);
    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);
    private static volatile SentryClient client;
    private static volatile LogisticsErrorLogBridge bridge;

    private CrashReporting() {}

    /** Called once after config load. Enables reporting only if the operator opted in previously. */
    public static void bootstrap() {
        if (LogisticsConfig.get().crashReporting.enabled) {
            enable();
        }
    }

    /** Start sending reports. Idempotent. */
    public static synchronized void enable() {
        if (ACTIVE.get()) {
            return;
        }
        String dsn = resolveDsn();
        if (dsn.isBlank()) {
            LOGGER.warn("Crash reporting enabled but no DSN is configured; not starting Sentry");
            return;
        }
        SentryOptions options = buildOptions();
        // Dedicated client — never Sentry.init(), which would replace the global SDK scope shared
        // with any other mod bundling the same Sentry. The SentryClient constructor swaps in a real
        // async HTTP transport on its own, so events are actually sent.
        client = new SentryClient(options);
        registerExitFlushHookOnce();
        bridge = newBridge();
        bridge.attach();
        ACTIVE.set(true);
        LOGGER.info("Crash reporting enabled (environment={})", options.getEnvironment());
    }

    /** Build the Sentry options used for both the live client and the {@link #previewReport()}. */
    private static SentryOptions buildOptions() {
        boolean dev = isDevelopmentEnvironment();
        SentryOptions options = new SentryOptions();
        options.setDsn(resolveDsn());
        // Belt-and-suspenders: a standalone client installs no integrations anyway, so there is
        // never a global uncaught-exception handler that could capture other mods or vanilla.
        options.setEnableUncaughtExceptionHandler(false);
        options.setSendDefaultPii(false);
        options.setAttachServerName(false);
        // Ignore SENTRY_* env vars / sentry.properties so behavior is fully code-driven.
        options.setEnableExternalConfiguration(false);
        options.setRelease("logistics@" + modVersion());
        options.setEnvironment(dev ? "dev" : "prod");
        options.setBeforeSend((event, hint) -> scrub(event));
        options.setDebug(dev);
        return options;
    }

    // Platform lookups are defensive: crash reporting is non-critical, so if the SPI is somehow
    // unavailable (e.g. a unit-test classloader) we fall back rather than failing to report.
    private static String modVersion() {
        try {
            return PlatformService.INSTANCE.modVersion();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static boolean isDevelopmentEnvironment() {
        try {
            return PlatformService.INSTANCE.isDevelopmentEnvironment();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Stop sending reports and flush. Idempotent. */
    public static synchronized void disable() {
        if (!ACTIVE.getAndSet(false)) {
            return;
        }
        if (bridge != null) {
            bridge.detach();
            bridge = null;
        }
        SentryClient current = client;
        client = null;
        if (current != null) {
            current.close(); // flushes queued events
        }
        LOGGER.info("Crash reporting disabled");
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    // ==================== Command actions ====================
    // These back the /logistics crashreports subcommands. Kept here (not inline in the Brigadier
    // tree) so the persist-and-toggle logic is unit-testable without a command source.

    /** Persist the operator's opt-in choice and toggle the live client. Returns operator feedback. */
    public static String setReportingEnabled(boolean enabled) {
        LogisticsConfig.get().crashReporting.enabled = enabled;
        LogisticsConfig.save();
        if (enabled) {
            enable();
        } else {
            disable();
        }
        return enabled
                ? "Sanitized crash reporting enabled. Thank you for helping improve Logistics!"
                : "Crash reporting disabled.";
    }

    /** Persist whether the join invitation is shown. Returns operator feedback. */
    public static String setJoinNotice(boolean show) {
        LogisticsConfig.get().crashReporting.notifyOperators = show;
        LogisticsConfig.save();
        return "Crash reporting join notice: " + (show ? "ON" : "off");
    }

    /** Human-readable current state for the status command. */
    public static String statusText() {
        LogisticsConfig.CrashReportingConfig cfg = LogisticsConfig.get().crashReporting;
        return "Sanitized crash reporting: " + (cfg.enabled ? "ON" : "off")
                + "\n  join notice: " + (cfg.notifyOperators ? "on" : "off");
    }

    /** Report a throwable when active; a no-op otherwise. */
    public static void capture(Throwable throwable) {
        if (throwable == null || !ACTIVE.get()) {
            return;
        }
        SentryClient current = client;
        if (current != null) {
            current.captureException(throwable);
        }
    }

    /**
     * Build a representative report for a synthetic Logistics error, run it through the exact
     * sanitization pipeline used before sending, and return it as JSON. Does NOT send and works
     * whether or not reporting is enabled, so an operator can inspect the output before opting in.
     * The sample message embeds mock sensitive values so the scrubbing is visible in the result.
     */
    public static String previewReport() {
        SentryOptions options = buildOptions();
        RuntimeException sample = new RuntimeException(
                "Logistics crash-report preview (example, not a real crash). Sensitive values are "
                + "scrubbed before send: home=" + System.getProperty("user.home")
                + " ip=203.0.113.7 uuid=123e4567-e89b-12d3-a456-426614174000 token=hunter2");
        SentryEvent event = new SentryEvent(sample);
        event.setRelease(options.getRelease());
        event.setEnvironment(options.getEnvironment());
        SentryExceptionFactory exceptionFactory =
                new SentryExceptionFactory(new SentryStackTraceFactory(options));
        event.setExceptions(exceptionFactory.getSentryExceptions(sample));
        scrub(event);
        try (StringWriter writer = new StringWriter()) {
            new JsonSerializer(options).serialize(event, writer);
            return writer.toString();
        } catch (IOException e) {
            return "Failed to build preview: " + e;
        }
    }

    private static LogisticsErrorLogBridge newBridge() {
        return new Log4j2ErrorLogBridge(CrashReporting::capture);
    }

    /**
     * Registers a single JVM shutdown hook that flushes the active client's queue on exit. Replaces
     * the flush-on-exit that {@code Sentry.init()} used to provide via its global shutdown
     * integration. Process-local and scoped to our own client — it touches no shared SDK state.
     */
    private static void registerExitFlushHookOnce() {
        if (!SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SentryClient current = client;
            if (current != null) {
                current.flush(FLUSH_TIMEOUT_MS);
            }
        }, "logistics-sentry-flush"));
    }

    private static String resolveDsn() {
        String override = LogisticsConfig.get().crashReporting.dsnOverride;
        return override != null && !override.isBlank() ? override.trim() : DEFAULT_DSN;
    }

    // Free-text identifiers to strip from any text that leaves the process.
    private static final Pattern IPV4 = Pattern.compile("\\b\\d{1,3}(?:\\.\\d{1,3}){3}\\b");
    private static final Pattern UUID = Pattern.compile(
            "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");
    private static final Pattern UNIX_HOME = Pattern.compile("(/(?:Users|home))/[^/\\s]+");
    private static final Pattern WINDOWS_HOME = Pattern.compile("([A-Za-z]:\\\\Users\\\\)[^\\\\\\s]+");
    private static final Pattern SECRET_KV = Pattern.compile(
            "(?i)(password|passwd|token|secret|api[_-]?key|access[_-]?key|dsn)(\\s*[=:]\\s*)\\S+");

    /**
     * Sanitize an event before it leaves the process: drop the server name and strip identifying
     * substrings (home directories, IPs, UUIDs, secret-like {@code key=value} pairs) from the event
     * message and every exception value. Stack frames carry only source file names, not paths, so
     * they need no scrubbing. Best-effort, biased toward over-redaction.
     */
    private static SentryEvent scrub(SentryEvent event) {
        event.setServerName(null);
        Message message = event.getMessage();
        if (message != null) {
            message.setFormatted(redact(message.getFormatted()));
            message.setMessage(redact(message.getMessage()));
        }
        if (event.getExceptions() != null) {
            for (SentryException ex : event.getExceptions()) {
                ex.setValue(redact(ex.getValue()));
            }
        }
        return event;
    }

    static String redact(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String home = System.getProperty("user.home");
        String user = System.getProperty("user.name");
        if (home != null && !home.isBlank()) {
            value = value.replace(home, "~");
        }
        if (user != null && !user.isBlank()) {
            value = value.replace(user, "<user>");
        }
        value = UNIX_HOME.matcher(value).replaceAll("$1/<user>");
        value = WINDOWS_HOME.matcher(value).replaceAll("$1<user>");
        value = UUID.matcher(value).replaceAll("<uuid>");
        value = IPV4.matcher(value).replaceAll("<ip>");
        value = SECRET_KV.matcher(value).replaceAll("$1$2<redacted>");
        return value;
    }
}

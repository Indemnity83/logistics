package com.logistics.core.crash;

import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.platform.PlatformService;
import io.sentry.SentryClient;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opt-in anonymous crash reporting via Sentry. Loader-agnostic — the SDK is pure Java, so all calls
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
 *   <li><b>Anonymous</b>: PII is off and the host name is not attached; messages are scrubbed of the
 *       local user name / home directory before send.</li>
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
        boolean dev = PlatformService.INSTANCE.isDevelopmentEnvironment();
        SentryOptions options = new SentryOptions();
        options.setDsn(dsn);
        // Belt-and-suspenders: a standalone client installs no integrations anyway, so there is
        // never a global uncaught-exception handler that could capture other mods or vanilla.
        options.setEnableUncaughtExceptionHandler(false);
        options.setSendDefaultPii(false);
        options.setAttachServerName(false);
        // Ignore SENTRY_* env vars / sentry.properties so behavior is fully code-driven.
        options.setEnableExternalConfiguration(false);
        options.setRelease("logistics@" + PlatformService.INSTANCE.modVersion());
        options.setEnvironment(dev ? "dev" : "prod");
        options.setBeforeSend((event, hint) -> scrub(event));
        options.setDebug(dev);

        // Dedicated client — never Sentry.init(), which would replace the global SDK scope shared
        // with any other mod bundling the same Sentry. The SentryClient constructor swaps in a real
        // async HTTP transport on its own, so events are actually sent.
        client = new SentryClient(options);
        registerExitFlushHookOnce();
        bridge = newBridge();
        bridge.attach();
        ACTIVE.set(true);
        LOGGER.info("Crash reporting enabled (environment={})", dev ? "dev" : "prod");
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

    /**
     * Best-effort anonymization: strip the local user name and home directory from the event
     * message and exception values before the event leaves the process.
     */
    private static SentryEvent scrub(SentryEvent event) {
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
        return value;
    }
}

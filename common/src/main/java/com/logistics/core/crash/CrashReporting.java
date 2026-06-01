package com.logistics.core.crash;

import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.platform.PlatformService;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
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
 *   <li><b>Logistics-only</b>: Sentry's global uncaught-exception handler is disabled; exceptions are
 *       captured solely through a {@link LogisticsErrorLogBridge} scoped to the Logistics loggers, so
 *       other mods and vanilla are never reported.</li>
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

    private static final AtomicBoolean ACTIVE = new AtomicBoolean(false);
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
        Sentry.init(options -> {
            options.setDsn(dsn);
            // Only Logistics-scoped errors — never install a global handler that would capture
            // other mods or vanilla.
            options.setEnableUncaughtExceptionHandler(false);
            options.setSendDefaultPii(false);
            options.setAttachServerName(false);
            // Ignore SENTRY_* env vars / sentry.properties so behavior is fully code-driven.
            options.setEnableExternalConfiguration(false);
            options.setRelease("logistics@" + PlatformService.INSTANCE.modVersion());
            options.setEnvironment(dev ? "dev" : "prod");
            options.setBeforeSend((event, hint) -> scrub(event));
            options.setDebug(dev);
        });
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
        Sentry.close();
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
        Sentry.captureException(throwable);
    }

    private static LogisticsErrorLogBridge newBridge() {
        return new Log4j2ErrorLogBridge(CrashReporting::capture);
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

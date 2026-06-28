package com.logistics.core.crash;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * {@link LogisticsErrorLogBridge} backed by Log4j2 (Minecraft's slf4j backend at runtime).
 *
 * <p>Adds a forwarding appender to the <em>root</em> logger config, filtered at runtime to ERROR
 * events that (a) carry a {@link Throwable} and (b) originate from a Logistics logger. Attaching to
 * root rather than creating a {@code com.logistics} logger config avoids changing any logger's
 * effective level. All Log4j2-core access is guarded so a non-Log4j2 backend degrades to a no-op.
 */
final class Log4j2ErrorLogBridge implements LogisticsErrorLogBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/crash");
    private static final String APPENDER_NAME = "LogisticsSentryBridge";

    private final Consumer<Throwable> sink;
    private ForwardingAppender appender;

    Log4j2ErrorLogBridge(Consumer<Throwable> sink) {
        this.sink = sink;
    }

    /**
     * True when an event should be reported: it carries a throwable and originates from a Logistics
     * logger.
     */
    static boolean shouldForward(String loggerName, boolean hasThrowable) {
        if (!hasThrowable || loggerName == null) {
            return false;
        }
        String lower = loggerName.toLowerCase(Locale.ROOT);
        // Match only our own namespaces — not a foreign "logisticsaddons" or "other.com.logistics".
        return lower.equals("logistics")
                || lower.startsWith("logistics/")
                || lower.startsWith("logistics.")
                || lower.startsWith("com.logistics.");
    }

    @Override
    public boolean attach() {
        try {
            if (!(LogManager.getContext(false) instanceof LoggerContext ctx)) {
                LOGGER.warn("Log4j2 backend unavailable; crash log capture disabled");
                return false;
            }
            Configuration config = ctx.getConfiguration();
            appender = new ForwardingAppender(APPENDER_NAME, sink);
            appender.start();
            config.getRootLogger().addAppender(appender, Level.ERROR, null);
            ctx.updateLoggers();
            return true;
        } catch (Throwable t) {
            LOGGER.warn("Failed to attach crash log capture: {}", t.toString());
            // Undo any partial registration so a half-attached appender can't linger on root.
            detach();
            return false;
        }
    }

    @Override
    public void detach() {
        try {
            if (appender == null || !(LogManager.getContext(false) instanceof LoggerContext ctx)) {
                return;
            }
            ctx.getConfiguration().getRootLogger().removeAppender(APPENDER_NAME);
            appender.stop();
            ctx.updateLoggers();
        } catch (Throwable t) {
            LOGGER.warn("Failed to detach crash log capture: {}", t.toString());
        } finally {
            appender = null;
        }
    }

    private static final class ForwardingAppender extends AbstractAppender {
        private final Consumer<Throwable> sink;

        ForwardingAppender(String name, Consumer<Throwable> sink) {
            super(name, null, null, true, null);
            this.sink = sink;
        }

        @Override
        public void append(LogEvent event) {
            if (shouldForward(event.getLoggerName(), event.getThrown() != null)) {
                sink.accept(event.getThrown());
            }
        }
    }
}

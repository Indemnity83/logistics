package com.logistics.pipe.render;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Lightweight render profiler for measuring PipeBlockEntityRenderer performance.
 *
 * <p>Enable by setting {@link #ENABLED} to {@code true}. Stats are logged every
 * {@link #REPORT_INTERVAL_SUBMITS} submit calls to the DEBUG log channel.
 *
 * <p>To see output, ensure your log config includes DEBUG for com.logistics, or
 * temporarily change the logger call to {@code .info()} during investigation.
 */
public final class RenderProfiler {

    /** Toggle via {@code /logistics profile} in-game, or set directly for compile-time control. */
    public static volatile boolean ENABLED = false;

    private static final int REPORT_INTERVAL_SUBMITS = 200;
    private static final Logger LOGGER = LogManager.getLogger("Logistics/PipeRenderer");

    private long extractTotalNs = 0;
    private long submitTotalNs = 0;
    private long collectPartsTotalNs = 0;
    private long appendItemLayersTotalNs = 0;
    private int extractCount = 0;
    private int collectPartsCount = 0;
    private int appendItemLayersCount = 0;
    private int submitCount = 0;

    /** Call at the start of {@code extractRenderState()}. Returns a start timestamp. */
    public long startExtract() {
        return System.nanoTime();
    }

    /** Call at the end of {@code extractRenderState()} with the value returned by {@link #startExtract()}. */
    public void endExtract(long startNs) {
        extractTotalNs += System.nanoTime() - startNs;
        extractCount++;
    }

    /** Call at the start of {@code submit()}. Returns a start timestamp. */
    public long startSubmit() {
        return System.nanoTime();
    }

    /**
     * Call at the end of {@code submit()} with the value returned by {@link #startSubmit()}.
     * Also drives the periodic report — triggered every {@link #REPORT_INTERVAL_SUBMITS} submissions.
     */
    public void endSubmit(long startNs) {
        submitTotalNs += System.nanoTime() - startNs;
        submitCount++;
        if (submitCount >= REPORT_INTERVAL_SUBMITS) {
            report();
            reset();
        }
    }

    /** Call around each {@code collectParts()} invocation: record start ns, call this after. */
    public void recordCollectParts(long startNs) {
        collectPartsTotalNs += System.nanoTime() - startNs;
        collectPartsCount++;
    }

    /** Call around each {@code appendItemLayers()} invocation: record start ns, call this after. */
    public void recordAppendItemLayers(long startNs) {
        appendItemLayersTotalNs += System.nanoTime() - startNs;
        appendItemLayersCount++;
    }

    private void report() {
        double avgExtractMs = extractCount > 0 ? (extractTotalNs / 1_000_000.0) / extractCount : 0;
        double avgSubmitMs = submitCount > 0 ? (submitTotalNs / 1_000_000.0) / submitCount : 0;
        double collectPartsTotalMs = collectPartsTotalNs / 1_000_000.0;
        double appendTotalMs = appendItemLayersTotalNs / 1_000_000.0;

        LOGGER.info(String.format(
                "avg extractRenderState: %.3fms (%d calls) | avg submit: %.3fms | "
                        + "collectParts: %d calls (%.3fms total) | appendItemLayers: %d calls (%.3fms total) "
                        + "[over %d submits]",
                avgExtractMs, extractCount, avgSubmitMs,
                collectPartsCount, collectPartsTotalMs,
                appendItemLayersCount, appendTotalMs,
                submitCount));
    }

    private void reset() {
        extractTotalNs = 0;
        submitTotalNs = 0;
        collectPartsTotalNs = 0;
        appendItemLayersTotalNs = 0;
        extractCount = 0;
        collectPartsCount = 0;
        appendItemLayersCount = 0;
        submitCount = 0;
    }
}

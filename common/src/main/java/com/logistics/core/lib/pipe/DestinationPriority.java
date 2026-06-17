package com.logistics.core.lib.pipe;

/** Where a fluid pipe prefers to send fluid at a junction. */
public enum DestinationPriority {
    /** Default: split across all outputs (pipes and handlers) evenly. */
    NORMAL,
    /** Fill adjacent fluid handlers (tanks) before spilling to other pipes. */
    HANDLERS_FIRST
}

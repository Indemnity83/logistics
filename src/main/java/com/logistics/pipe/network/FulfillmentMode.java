package com.logistics.pipe.network;

/**
 * Controls whether an order must be fully satisfied before dispatching.
 */
public enum FulfillmentMode {
    /** Dispatch whatever is available; partial fills are acceptable. */
    PARTIAL,
    /** Only dispatch when the full requested amount is available; never dispatch partial. */
    FULL
}

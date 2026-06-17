package com.logistics.core.lib.pipe;

/**
 * Discriminates the kind of payload a modular pipe carries. Used by family-aware modules
 * (e.g. weathering, marking) so they only ever match pipes of their own family — item pipes
 * never interact with fluid pipes and vice versa.
 */
public enum PipeFamily {
    ITEM,
    FLUID
}

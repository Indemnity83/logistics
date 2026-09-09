package com.logistics.core.lib.network;

/**
 * A block beside a pipe that caches which {@link ILogisticsNetwork} it belongs to and must re-derive
 * that cache when the pipe topology changes under it.
 *
 * <p>A split replaces the network instance for every pipe in the component while leaving each
 * block's own state untouched, so a neighbour holding a reference to the old network has no other
 * way to notice it is now stale.
 */
public interface NetworkTopologyListener {

    /** Called when an adjacent pipe's network has been replaced. */
    void onNetworkTopologyChanged();
}

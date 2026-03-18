/**
 * Logistics network — planning, coordination, and execution layer.
 *
 * <h2>Data model flow</h2>
 * <pre>
 *  Player / RequesterModule
 *         │
 *         ▼
 *  {@link com.logistics.core.lib.network.ItemRequest}
 *    A player-facing request for one item type and quantity.
 *    Submitted to {@link com.logistics.pipe.network.JobCoordinator}.
 *         │
 *         ▼
 *  {@link com.logistics.pipe.network.NetworkJob}
 *    Tracks the lifecycle of a single ItemRequest (PENDING → ACTIVE → COMPLETE/FAILED).
 *    Contains one or more {@link com.logistics.pipe.network.WorkOrder}s produced by planning.
 *         │
 *         ▼
 *  {@link com.logistics.pipe.network.WorkOrder}
 *    A concrete dispatch instruction: "move N of item X from provider P to requester R".
 *    Handed to {@link com.logistics.pipe.network.NetworkController} for execution.
 *         │
 *         ▼
 *  {@link com.logistics.core.lib.network.Order}
 *    An in-flight delivery tracked by the network. Created when a WorkOrder is dispatched
 *    and a provider confirms it can supply the item.
 *         │
 *         ▼
 *  TravelingItem  (in {`@code` com.logistics.pipe.runtime})
 *    A physical item moving through pipe block entities toward its destination.
 *    Created when the item is extracted from the provider's inventory.
 * </pre>
 *
 * <h2>Key classes</h2>
 * <ul>
 *   <li>{@link com.logistics.pipe.network.NetworkController} — pure-Java planning engine,
 *       zero Minecraft coupling, fully unit-testable.</li>
 *   <li>{@link com.logistics.pipe.network.JobCoordinator} — bridges player requests to
 *       network planning; reconciles in-flight jobs on a fixed tick interval.</li>
 *   <li>{@link com.logistics.pipe.network.PipeNetwork} — Minecraft-side facade over
 *       NetworkController; owns the {@link com.logistics.pipe.network.NetworkRegistry}
 *       lifetime for one logical network.</li>
 *   <li>{@link com.logistics.pipe.network.MinecraftWorldView} — implements
 *       {@link com.logistics.core.lib.network.IWorldView} to bridge the pure network
 *       layer with Minecraft block entities.</li>
 * </ul>
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@code packet} — Fabric networking packets (client↔server).</li>
 * </ul>
 */
package com.logistics.pipe.network;

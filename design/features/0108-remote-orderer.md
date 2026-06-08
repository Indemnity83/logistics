# Remote Orderer

> **Status:** 🚧 Planned · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`pipe` domain)
> **Source:** [`../mods/logistics-pipes.md`](../mods/logistics-pipes.md) (Remote Orderer) · **Depends on:** nothing
> **Maps to (roadmap):** Phase 1 — logistics QoL

A handheld item that opens the network request UI from anywhere in range — order items from your logistics network without standing at a Requester pipe. A high-value QoL Modernize: the request UI and order flow already exist; this is a new item that reaches them.

## Problem & goal

Today, requesting items means interacting with a Requester pipe at a fixed spot. The Logistics Pipes Remote Orderer let you pull from the network on the move — a defining convenience of the system.

**Goal:** a handheld item that opens the existing request screen against a chosen/nearby network, balanced by range and/or cost, reusing the order/dispatch machinery wholesale.

## Requirements

### Functional
- Right-clicking the item opens the **request UI** (the same flow as the Requester pipe: browse available network items, search, choose amount, place an order).
- The item targets a network: either (a) **bound** to a specific network/pipe (shift-right-click a pipe to bind, stored on the item), or (b) the **nearest** network within range. Prefer binding for predictability (see Open questions).
- Orders placed go through the normal `ILogisticsNetwork.placeOrder(...)` path; items route to a configured drop/return point or the player (see Open questions on delivery).
- Works only when the target network is loaded and within range; clear feedback when out of range / unbound / no network.

### Balance
- **Range-limited** — not a base-wide wireless terminal from across the world. A generous-but-bounded radius (e.g. tens of blocks) keeps it a convenience, not a teleporter.
- Optionally an **energy/charge cost** or a crafting cost using mid-tier components so it's an earned tool, not a starter item.
- Delivery has to land *somewhere physical* — orders fulfil to a pipe/inventory, not magically into the player's pack (unless we explicitly choose a "remote pickup" design — Open questions).

## Design sketch

The request side is fully built — `RequesterScreenHandler` already queries the network for available items and places orders; `RequesterModule.onWrench` shows the exact `openMenu` idiom. The new work is an **item** that reaches a network by position instead of by being a pipe.

```
common/src/main/java/com/logistics/pipe/item/RemoteOrdererItem.java
```

- `RemoteOrdererItem extends Item`:
  - `use(level, player, hand)`: server-side, resolve the target network — read a bound `BlockPos`/network UUID from the item's data component (`DataComponents.CUSTOM_DATA`), or find the nearest pipe via `NetworkRegistry`; range-check; then `serverPlayer.openMenu(new SimpleMenuProvider(... new RemoteRequesterScreenHandler(syncId, inv, targetPos), title))`.
  - `useOn(...)` / shift-use on a pipe: bind the orderer to that pipe's network, store on the item via `ItemStack.set(DataComponents.CUSTOM_DATA, ...)` (data-component precedent: `pipe/data/PipeDataComponents`).
- **`RemoteRequesterScreenHandler`**: a thin variant of `RequesterScreenHandler` that takes a `BlockPos` and resolves the network via `NetworkRegistry.getNetwork(level, pos)` instead of holding a `PipeBlockEntity`. Reuse the existing item-browse/search/order logic.
- Register the item in `LogisticsPipe.ITEM` and the menu in the menu registrar (`registerMenuType`). Handheld-item-opens-screen precedent: `pipe/item/ModuleItem#use`.
- Client: the existing requester screen (or a near-identical one) bound to the new menu type.

## Scope & non-goals

- **In:** the handheld item, network binding via data component (or nearest-network fallback), the request UI against a remote network, range/feedback handling.
- **Out:** wireless cross-dimension / unlimited-range terminals, a full base-management dashboard, crafting management beyond what the Requester UI already does, per-player permissions.
- **Out:** redesigning the request UI — reuse it.

## Open questions

- **Targeting model: bound vs nearest.** Binding (shift-click a pipe) is predictable and supports multiple networks; nearest is zero-config but ambiguous with overlapping ranges. **Lean: bind, with the item showing its bound target in tooltip.**
- **Delivery destination.** Where do ordered items go — a configured drop pipe/inventory near the player, the network's default route, or a "remote pickup" buffer the player collects from? The original delivered to a set location. **Lean: orders fulfil to the network's existing delivery target (e.g. a Requester/drop point); the orderer is a remote *trigger*, not a remote *teleport*.** Needs a clear decision.
- **Cost/gating:** crafting recipe tier, and whether it consumes energy/charge per use.
- Range value, and whether it scales with anything.

## Done when

- The item opens the request screen against a bound/nearby loaded network within range, on both loaders.
- Placing an order routes items through the normal dispatch path to the agreed delivery destination.
- Out-of-range / unbound / unloaded states give clear player feedback.
- Binding persists on the item across stacking/drops.

## References

- Roadmap: [`../roadmap.md`](../roadmap.md) → Phase 1 → logistics QoL; [`../mods/logistics-pipes.md`](../mods/logistics-pipes.md) → Remote Orderer row
- Code: `pipe/ui/RequesterScreenHandler` (reuse), `pipe/modules/RequesterModule#onWrench` (openMenu idiom), `pipe/item/ModuleItem#use` (handheld→screen), `pipe/network/NetworkRegistry` (resolve network by pos), `core/lib/network/{ILogisticsNetwork,Order}`, `pipe/data/PipeDataComponents` (data-component binding), `LogisticsPipe.java`

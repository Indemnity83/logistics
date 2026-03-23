# Tier System

Logistics organizes pipes and features into **three tiers** that represent progression from simple mechanical operations to advanced network logistics.

## Tier 1: Mechanical Pipes

**No item awareness** - These pipes perform mechanical operations without looking at what's flowing through them.

**Characteristics:**
- Operate on all items equally
- No conditional behavior
- Simple recipes (material + glass)
- Available early in progression

**Examples:**
- [Stone Transport Pipe](../pipes/stone-transport-pipe.md) - Move items slowly
- [Copper Transport Pipe](../pipes/copper-transport-pipe.md) - Standard transport
- [Item Extractor Pipe](../pipes/item-extractor-pipe.md) - Pull from inventories
- [Item Merger Pipe](../pipes/item-merger-pipe.md) - Converge to single output
- [Golden Transport Pipe](../pipes/golden-transport-pipe.md) - Speed boost

## Tier 2: Smart Pipes

**Item-aware routing** - These pipes inspect items and make decisions based on what they see.

**Characteristics:**
- Conditional behavior based on item type
- Can filter and sort
- More complex recipes (advanced materials)
- Mid-game progression

**Examples:**
- [Item Filter Pipe](../pipes/item-filter-pipe.md) - Route by item type
- [Item Insertion Pipe](../pipes/item-insertion-pipe.md) - Prefer inventories with space

## Tier 3: Network Logistics

**System-aware automation** — treats inventories as abstract resources with global routing and request-based delivery. Tier 3 pipes communicate with each other across the entire network, advertising available items, fulfilling requests, and automating crafting without manual intervention.

**Characteristics:**
- Request/provider model — items are pulled on demand, not pushed blindly
- Global A\* pathfinding across the full network
- Autocrafting integration
- End-game progression

**Pipes:**
- [Basic Logistics Pipe](../pipes/basic-logistics-pipe.md) — Foundation for all Tier 3 pipes; accepts and deposits network items
- [Provider Logistics Pipe](../pipes/provider-logistics-pipe.md) — Advertises inventory contents; fulfills network requests
- [Supplier Logistics Pipe](../pipes/supplier-logistics-pipe.md) — Maintains stock levels automatically
- [Requester Logistics Pipe](../pipes/requester-logistics-pipe.md) — Manually request items from the network
- [Crafting Logistics Pipe](../pipes/crafting-logistics-pipe.md) — Crafts items on demand to fulfill requests
- [Process Logistics Pipe](../pipes/process-logistics-pipe.md) — Automates processing machines (furnaces, kilns)
- [Satellite Logistics Pipe](../pipes/satellite-logistics-pipe.md) — Named remote destination for network-addressed delivery
- [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) (MK1–MK5) — Modular pipe; behavior defined by installed modules

### Modules: Tier 3 Advanced

The [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) introduces a **modular layer** within Tier 3. Rather than a fixed function, the chassis is a blank frame that accepts swappable [modules](../modules/index.md) — each module contributes one behavior, and a single chassis can run multiple modules simultaneously (up to 5 in an MK5).

This effectively creates a tier within a tier: you build Tier 3 network infrastructure first, then use chassis pipes with modules to compose highly customized logistics nodes at key points in your network.

**Module categories:**
- **Extraction** — [Extractor Module](../modules/extractor-module.md), [Advanced Extractor Module](../modules/advanced-extractor-module.md)
- **Provider/Supplier** — [Provider Module](../modules/provider-module.md), [Supplier Module](../modules/supplier-module.md)
- **Crafting** — [Crafter Module](../modules/crafter-module.md)
- **Sorting & Routing** — [Quicksort Module](../modules/quicksort-module.md), [Terminus Module](../modules/terminus-module.md)
- **Sinks** — [Item Sink](../modules/item-sink-module.md), [Polymorphic Sink](../modules/polymorphic-sink-module.md), [Enchantment Sink](../modules/enchantment-sink-module.md), [Mod Item Sink](../modules/mod-item-sink-module.md)

See [Modules Overview](../modules/index.md) for the full list.

## Design Philosophy

Each tier builds on the previous:
- **Tier 1** provides physical connectivity
- **Tier 2** adds intelligent routing
- **Tier 3** adds abstract logistics — and at its advanced end, modular composition via chassis pipes

All tiers work together — you'll use pipes from all tiers in a mature network.

## See Also
- [Pipe Networks](pipe-networks.md) - How tiers connect together
- [Pipes](../pipes/index.md) - All pipe types organized by tier
- [Modules](../modules/index.md) - All chassis modules
- [Routing](routing.md) - How different tiers route items

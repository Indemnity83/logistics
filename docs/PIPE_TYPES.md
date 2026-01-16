# Pipe Behaviors and Routing Rules

This document defines pipe behaviors, acceptance rules, and inventory interactions organized by tier. It serves as the specification for how each behavior should function.

**Note:** This document describes **behaviors**, not implementation names. In-game pipe names may differ from the behavioral terms used here.

**Status:** Phase 1 pipes (Tier 1-2) implemented in v0.1.0. Tier 3 components and future behaviors marked with 🔮.

## Core Design Principles

### Three-Tier Architecture
Pipes are organized into three tiers based on their role in the logistics system:
- **Tier 1 (Passive Movement)**: Physical/data link layer—connectivity and flow shaping without decisions
- **Tier 2 (Active Control)**: Network/transport layer—flow initiation, termination, and item-aware routing
- **Tier 3 (Network Logistics)**: Session/application layer—abstract inventory representation and global routing

### Interconnection
All tiers connect to each other. This is a layered system where higher tiers build on lower ones, not segregated eras.

### Resource-Gated Progression
Access to higher tiers requires game progression:
- **Tier 1**: Basic vanilla materials (stone, wood, copper, iron, gold)
- **Tier 2**: Rare materials (diamond, quartz) + additional components
- **Tier 3**: End-game materials (nether stars, shulker shells, etc.)

### Material-Based Identity
Each pipe uses distinct vanilla materials for visual clarity in inventory and world.

## Shared Concepts
- **Routing philosophy**: Try to insert into the next target; drop only if insert fails
- **Acceptance philosophy**: The receiving block decides if it accepts an item
- **ItemStorage exposure**: Controls who can attempt inserts/extracts into a pipe
- **Extract prevention**: Pipes return 0 for extract to prevent external pulling
- **Default insertion**: Any pipe may attempt to insert into inventories when routing
- **Capacity**: Pipes have a virtual capacity of 5 stacks (5 × 64); when full, new items are rejected

## Terminology
- **Expose ItemStorage**: Whether ItemStorage.SIDED returns a Storage on a side
- **Accept from pipe**: Whether addItem() accepts items from another pipe
- **Route into inventories**: Whether routing attempts insert into non-pipe targets

---

# Tier 1: Passive Movement (Physical + Data Link Layer)

Tier 1 pipes provide connectivity and basic flow shaping with mechanical operations but no item-aware decisions. They react to flow but don't make decisions based on what's flowing.

**Characteristics:**
- No item-type awareness or filtering
- Mechanical operations (random, convergence, extraction, deletion)
- Operations happen regardless of item type
- Simple recipes: Material + glass (+ optional component)
- Early-to-mid game materials

## Transport Behavior (Stone) ✅ v0.1.0

**Purpose:** Cheap entry-level backbone connectivity with slower transport.

**Material:** Stone

- **Tier**: 1 (Passive Movement)
- **Expose ItemStorage**: Pipes only (PipeOnlyModule)
- **Accept from pipe**: Yes
- **Route into inventories**: Yes
- **Routing**: Random selection among valid connected directions when multiple outputs exist
- **Speed**: Slower than copper (50% speed?)
- **Visual**: Stone/gray appearance
- **Recipe**: Stone + glass → 8 pipes

## Transport Behavior (Copper) ✅ v0.1.0

**Purpose:** Main backbone connectivity with standard transport speed.

**Material:** Copper

**Implementation note:** Currently implemented as "Copper Transport Pipe".

- **Tier**: 1 (Passive Movement)
- **Expose ItemStorage**: Pipes only (PipeOnlyModule)
- **Accept from pipe**: Yes
- **Route into inventories**: Yes
- **Routing**: Random selection among valid connected directions when multiple outputs exist
- **Speed**: Standard pipe speed
- **Visual**: Copper/orange appearance
- **Recipe**: Copper ingot + glass → 8 pipes

## Acceleration Behavior ✅ v0.1.0

**Purpose:** Speed boost for items when powered by redstone.

**Material:** Gold

**Implementation note:** Currently implemented as "Gold Transport Pipe".

- **Tier**: 1 (Passive Movement)
- **Expose ItemStorage**: Pipes only
- **Accept from pipe**: Yes
- **Route into inventories**: Yes
- **Routing**: Random selection (inherits Transport behavior)
- **Speed**: Applies acceleration to items when powered by redstone
- **Visual**: Gold/yellow appearance with metallic sheen
- **Recipe**: Gold ingot + glass → 8 pipes
- **Special note**: Gold remains a special-case for speed boost

## Merger Behavior ✅ v0.1.0

**Purpose:** Convergence—all inputs route to single configured output.

**Material:** Iron

**Implementation note:** Currently implemented as "Basic Merger Pipe" (to be visually updated to iron appearance).

- **Tier**: 1 (Passive Movement)
- **Expose ItemStorage**: Pipes only
- **Accept from pipe**: Yes, but reject items from the output side
- **Route into inventories**: Yes
- **Routing**: Always route to configured output direction if valid; if invalid, drop item
- **Configuration**: Wrench to cycle output direction
- **Visual**: Iron/gray appearance with directional indicator on output face
- **Recipe**: Iron ingot + glass → 8 pipes

## Extractor Behavior ✅ v0.1.0

**Purpose:** Mechanical extraction—pulls items from adjacent inventories into the pipe network.

**Material:** Wood

**Implementation note:** Currently implemented as "Basic Extractor Pipe" (to be visually updated to wood appearance).

- **Tier**: 1 (Passive Movement)
- **Expose ItemStorage**: Yes, but only on the active face (single-face ingress)
- **Accept from pipe**: Yes (standard pipe behavior)
- **Route into inventories**: Yes
- **Extraction**: Yes, from the active face only
  - **Rate**: 1 item per extraction operation (base tier)
  - **Cooldown**: Configurable extraction interval
- **Configuration**: Wrench to cycle active extraction face
- **Active face logic**:
  - Only exposes ItemStorage on the active face
  - Only extracts from the active face
  - If active face becomes invalid (inventory removed), automatically seeks another available inventory
  - Once locked onto an inventory, adding new inventories will NOT cause it to switch
  - Player can manually cycle the active face using a wrench
- **Visual**: Wood/tan appearance with indicator on active face
- **Recipe**: Wood planks + glass + hopper → 8 pipes
- **Upgrades** 🔮 (Future):
  - Speed-wrapped tiers for faster extraction rates (8/16/32/64 items per operation)
  - Specific upgrade path TBD

## Void Behavior ✅ v0.1.0

**Purpose:** Mechanical deletion—deletes items that enter the pipe.

**Material:** Obsidian

**Implementation note:** Currently implemented as "Void Pipe".

- **Tier**: 1 (Passive Movement)
- **Expose ItemStorage**: Pipes only
- **Accept from pipe**: Yes
- **Route into inventories**: No
- **Routing**: Items are discarded at pipe center (progress = 0.5) to allow player reaction
- **Visual**: Obsidian/black appearance with void indicator
- **Recipe**: Obsidian + glass + ender pearl → 8 pipes

---

# Tier 2: Active Control (Network + Transport Layer)

Tier 2 pipes make decisions based on item type, count, or conditional logic. They inspect what's flowing through them and change behavior accordingly.

**Characteristics:**
- Item-aware or count-aware logic
- Conditional routing and monitoring
- Decision-making based on pipe contents
- More complex recipes with additional components
- Mid-to-late game materials

## Filter Behavior ✅ v0.1.0

**Purpose:** Item-aware routing based on configured per-side filters.

**Material:** Diamond

**Implementation note:** Currently implemented as "Smart Splitter Pipe" (to be visually updated to diamond appearance).

- **Tier**: 2 (Active Control)
- **Expose ItemStorage**: Pipes only
- **Accept from pipe**: Yes
- **Route into inventories**: Yes
- **Routing**:
  - If an item matches one or more filtered directions, choose randomly among those
  - Otherwise, fall back to unfiltered directions (wildcard "*")
  - If no match and no wildcards, drop item
- **Filtering**: Basic item type matching (ignores NBT data)
- **Configuration**: Right-click to open GUI with per-side filter slots (ghost items)
- **UI**: Per-side filters; sides with no filter act as wildcard
- **Visual**: Diamond/cyan appearance
- **Recipe**: Diamond + glass + [additional component TBD] → 8 pipes

## Insertion Behavior ✅ v0.1.1

**Purpose:** Prefer inventories with available space; otherwise continue through pipes.

**Material:** Quartz

**Implementation note:** Implemented as "Item Insertion Pipe".

- **Tier**: 2 (Active Control)
- **Expose ItemStorage**: Pipes only
- **Accept from pipe**: Yes
- **Route into inventories**: Yes
- **Routing**:
  - If any connected inventories can accept the item, choose among those
  - Otherwise, route to connected pipes
  - If no valid directions, drop the item
- **Visual**: Quartz/white appearance
- **Recipe**: Quartz + glass → 8 pipes

---

# Tier 3: Network Logistics (Session + Application Layer) 🔮 Future

Tier 3 represents a shift away from "pipes" entirely into network components. These provide abstract inventory representation, global routing, and high-level logistics services.

**Characteristics:**
- Inventories become abstract resources, not physical locations
- Global pathfinding and request/provider model
- Chassis & modular interfaces instead of pipes
- End-game materials in recipes (nether stars, shulker shells, etc.)

**Key concept:** At Tier 3, pipes become **ports** into a logistics system rather than the system itself.

## Request Table (Phase 2) 🔮

**Purpose:** High-level service requests for items from the network.

- **Tier**: 3 (Network Logistics)
- **Type**: Block (not a pipe)
- **Functionality**:
  - GUI for requesting items by type and quantity
  - Initiates global pathfinding to providers
  - Priority-based fulfillment
  - Request → routing → provider → dispatch flow
- **Connection**: Attaches to pipe network as a service endpoint
- **Recipe concept**: [Complex crafting with end-game materials]

## Provider Pipe (Phase 2) 🔮

**Purpose:** Exposes inventory contents as abstract resources to the network.

- **Tier**: 3 (Network Logistics)
- **Type**: Pipe attachment or module
- **Functionality**:
  - Advertises inventory contents to the network graph
  - Responds to requests from Request Tables
  - Dispatches items when pathfinding selects this provider
- **Connection**: Attaches to existing pipe network
- **Recipe concept**: [Complex crafting with end-game materials]

## Sink Pipe (Phase 2) 🔮

**Purpose:** Provides overflow/destination routing for unmatched items.

- **Tier**: 3 (Network Logistics)
- **Type**: Pipe module or behavior
- **Functionality**:
  - Acts as a destination for items that don't match any requests
  - Lower priority than Request Tables
  - Prevents items from dropping when no explicit destination exists
- **Connection**: Integrates with pipe network
- **Recipe concept**: TBD

## Chassis & Network Cores (Phase 3+) 🔮

**Purpose:** Modular network interfaces for advanced logistics.

- **Tier**: 3 (Network Logistics)
- **Type**: Blocks with module slots
- **Functionality**:
  - Modular interface for advanced logistics modules
  - Context-aware routing cores
  - Crafting logistics modules
  - Supply/demand balancing
- **Connection**: Network-level components
- **Recipe concept**: High-tier materials (nether stars, shulker shells, etc.)

---

# Future Behaviors 🔮

## Obsidian Pipe (Item Vacuum) 🔮 Future

**Not yet implemented.**

- **Tier**: 1 (Passive Movement)
- **Purpose**: Pull loose item entities into the pipe network
- **Material**: Obsidian (different from void pipe)
- **Expose ItemStorage**: Pipes only
- **Accept from pipe**: Yes
- **Route into inventories**: Yes
- **Pickup**: Absorbs item entities within a small radius of the pipe center
- **Recipe concept**: Obsidian + glass + hopper mechanism → 8 pipes

## Emerald Pipe (Overflow Gate) 🔮 Future

**Not yet implemented.**

- **Tier**: 2 (Active Control)
- **Purpose**: Prefer primary direction; overflow to others when blocked
- **Material**: Emerald
- **Expose ItemStorage**: Pipes only
- **Accept from pipe**: Yes
- **Route into inventories**: Yes
- **Routing**: Try preferred output; if invalid/full/rejected, choose among other valid outputs (random or ordered)
- **Configuration**: Wrench to set preferred direction
- **Recipe concept**: Emerald + glass + [component] → 8 pipes

## Pipe Markings (Network Segmentation)

**Implemented on copper transport pipes.**

- **Tier**: 1 (applies to copper transport pipes)
- **Purpose**: Segment networks by color/channel
- **Expose ItemStorage**: Pipes only, compatible colors
- **Accept from pipe**: Yes, compatible colors only
- **Route into inventories**: Yes
- **Use**: Right-click with marking fluid to apply color; sneak + empty hand to clear
- **Recipe**: Marking fluid = water bottle + dye
- **Note**: Channelization via applied markings

---

## Open Questions

**Resolved:**
- ✅ Extract prevention: Pipes return 0 for extract (no external pulling)
- ✅ Tier organization: OSI-inspired three-tier model
- ✅ Connection rules: All tiers interconnect
- ✅ Material-based progression: Each pipe uses distinct vanilla materials
- ✅ Visual clarity: Material appearance for inventory readability

**Open:**
- Stone transport pipe speed: 50%? 66%? 75% of copper?
- Specific recipe components for Tier 2 pipes (circuits, advanced items?)
- Emerald pipe backpressure behavior details
- Extraction upgrade tiers: speed-wrapped versions vs separate pipe types
- Tier 3 component recipes and balance

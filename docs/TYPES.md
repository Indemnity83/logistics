# Pipe Types And Routing Rules (Design Notes)

## Purpose
Define pipe behaviors, acceptance rules, and inventory interaction for consistent
routing and player expectations. This is planning documentation, not finalized
implementation.

## Shared Concepts
- Routing is "dumb": try to insert into the next target, drop only if insert fails.
- Acceptance is "smart": the receiving block decides if it accepts an item.
- ItemStorage exposure controls who can attempt inserts/extracts into a pipe.
- Default policy: pipes return 0 for extract to prevent external pulling.
- Default policy: any pipe may attempt to insert into inventories when routing.
- Capacity: pipes have a virtual capacity of 5 stacks (5 * 64); when full, new
  items are rejected (drop on failure).

## Terminology
- "Expose ItemStorage": whether ItemStorage.SIDED returns a Storage on a side.
- "Accept from pipe": whether addItem(...) accepts items from another pipe.
- "Route into inventories": whether routing attempts insert into non-pipe targets.

## Pipe Categories
- Ingress: brings items from inventories into the network.
- Transport: moves items through the network without special behavior.
- Flow Control: modifies routing or speed.
- Special: unique behaviors that bypass normal routing.

## Ingress Pipes
### Wooden (Tier 1)
- Purpose: extraction-only entry point.
- Expose ItemStorage: Yes, but only on the active face (single-face ingress).
- Accept from pipe: No.
- Route into inventories: Yes.
- Extraction: Yes, from the active face only.
  - Amount: 1 item per extraction.

### Wooden Single-Face Rules
- Holds an "active input face" set by the player.
- Only exposes ItemStorage on the active face.
- Only extracts from the active face.
- If active face is valid, never auto-switch.
- If active face becomes invalid, it remains invalid until player action,
  except:
  - If there are zero adjacent inventories and a new one appears, it can
    auto-select that new face.
- If multiple adjacent inventories appear while inactive, remain inactive
  until the player cycles.

### Extraction Upgrade Tiers (Single-Face Only)
- Wooden (logs + glass): 1 item per extraction.
- Iron-wrapped (wooden + 4 iron ingots in corners): 8 items per extraction.
- Gold-wrapped (wooden + 4 gold ingots in corners): 16 items per extraction.
- Diamond-wrapped (wooden + 4 diamonds in corners): 32 items per extraction.
- Netherite-wrapped (diamond tier + netherite upgrade): full stacks per extraction.

## Transport Pipes
### Cobblestone
- Purpose: basic transport.
- Expose ItemStorage: Pipes only.
- Accept from pipe: Yes.
- Route into inventories: Yes.
- Routing: random among valid connected directions.

### Stone
- Purpose: basic transport (same as cobblestone, likely faster later).
- Expose ItemStorage: Pipes only.
- Accept from pipe: Yes.
- Route into inventories: Yes.
- Routing: random among valid connected directions.

### Obsidian (Item Vacuum)
- Purpose: pull loose item entities into the pipe network.
- Expose ItemStorage: Pipes only.
- Accept from pipe: Yes.
- Route into inventories: Yes.
- Pickup: absorbs item entities within a small radius of the pipe center.

### Sandstone (Bridge)
- Purpose: connect otherwise incompatible transport pipes.
- Expose ItemStorage: Pipes only.
- Accept from pipe: Yes.
- Route into inventories: Yes.
- Connections: links transport pipes that would not normally connect, but does not
  connect directly to machines/inventories.

## Flow Control Pipes
### Iron (Directional)
- Purpose: force output to a single face.
- Expose ItemStorage: Pipes only.
- Accept from pipe: Yes, but reject from the output side.
- Route into inventories: Yes.
- Routing: always choose configured output if valid.

### Gold (Acceleration)
- Purpose: speed up items.
- Expose ItemStorage: Pipes only.
- Accept from pipe: Yes.
- Route into inventories: Yes.
- Routing: random among valid connected directions.
- Speed: higher target speed and/or acceleration rate.

### Copper (Round Robin Splitter)
- Purpose: distribute items evenly across outputs.
- Expose ItemStorage: Pipes only.
- Accept from pipe: Yes.
- Route into inventories: Yes.
- Routing: round robin across valid connected directions.

### Diamond (Filter)
- Purpose: route items by per-side filters.
- Expose ItemStorage: Pipes only.
- Accept from pipe: Yes.
- Route into inventories: Yes.
- Routing: if an item matches one or more filtered directions, choose among those;
  otherwise fall back to unfiltered directions ("*").
- UI: per-side filters with ghost items; sides with no filter act as wildcard.
- Visuals: colored arms indicating output faces.

### Emerald (Overflow Gate)
- Purpose: prefer primary direction, overflow to others when blocked.
- Expose ItemStorage: Pipes only.
- Accept from pipe: Yes.
- Route into inventories: Yes.
- Routing: try preferred output; if invalid/full/rejected, choose among other valid
  outputs (random or ordered).

## Compatibility And Connections (Optional)
### Colored Pipes (Future)
- Purpose: segment networks by color.
- Expose ItemStorage: Pipes only, compatible colors.
- Accept from pipe: Yes, compatible colors.
- Route into inventories: Yes.
- Compatibility should be used consistently for:
  - Connection rendering.
  - Routing direction selection.
  - ItemStorage exposure.

## Open Questions
- Should any pipe allow extract for automation, or keep extract at 0 globally?

## Special Pipes
### Void
- Purpose: delete items that enter the pipe.
- Expose ItemStorage: Pipes only.
- Accept from pipe: Yes.
- Route into inventories: No.
- Routing: items are discarded at pipe center (0.5 progress) to allow player reaction.
- Recipe concept: glass + obsidian + ender pearl.

### Quartz (Comparator Output)
- Purpose: emit redstone strength based on items inside the pipe.
- Expose ItemStorage: Pipes only.
- Accept from pipe: Yes.
- Route into inventories: Yes.
- Signal: comparator output scaled to a virtual capacity of 5 stacks (5 * 64).

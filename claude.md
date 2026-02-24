# Logistics Documentation Site

## Overview
This is the **documentation worktree** for the Logistics Minecraft mod. It contains user-facing documentation built with Zensical, a static site generator similar to MkDocs with Material theme styling.

## Relationship to Main Mod
- **Main mod worktree**: `../logistics-mc-1.21.11/` (and other version-specific worktrees)
- **This worktree**: Documentation site that covers all versions
- **Branch**: `docs` (separate from version-specific branches like `mc/1.21.11`)

The main mod contains comprehensive technical documentation in its `README.md` and `docs/` folder that serves as source material for this user-facing documentation site.

## Documentation Structure

### Current State
```
docs/
├── index.md                          # Landing page (minimal)
└── getting-started/
    └── install.md                    # Placeholder ("TBD")
```

### Target Structure (Domain-Based)
```
docs/
├── index.md                          # Main landing page
├── getting-started/
│   ├── installation.md
│   ├── first-network.md
│   └── understanding-tiers.md
├── core/
│   ├── pipe-networks.md
│   ├── item-transport.md
│   ├── connectivity.md
│   ├── routing.md
│   └── tier-system.md
├── pipes/
│   ├── index.md                      # Pipes overview
│   ├── stone-transport-pipe.md
│   ├── copper-transport-pipe.md
│   ├── item-extractor-pipe.md
│   ├── item-merger-pipe.md
│   ├── golden-transport-pipe.md
│   ├── item-filter-pipe.md
│   ├── item-insertion-pipe.md
│   ├── item-passthrough-pipe.md
│   └── item-void-pipe.md
├── automation/
│   ├── index.md                      # Automation overview
│   └── laser-quarry.md
├── power/
│   ├── index.md                      # Power overview
│   ├── rf-energy.md
│   ├── redstone-engine.md
│   └── stirling-engine.md
├── tools/
│   ├── wrench.md
│   └── marking-fluid.md
└── materials/
    ├── wooden-gear.md
    ├── stone-gear.md
    ├── iron-gear.md
    ├── copper-gear.md
    └── diamond-gear.md
```

### Planned Structure (Wiki-Style)
Inspired by ftbwiki.org, each piece of equipment or concept gets its own dedicated page with heavy cross-linking:

**Core Concepts:**
- Pipe Networks
- Item Transport
- Pipe Connectivity
- Routing
- Tier System (Mechanical → Smart → Network)

**Pipes Domain:**
- Stone Transport Pipe
- Copper Transport Pipe
- Item Extractor Pipe
- Item Merger Pipe
- Golden Transport Pipe
- Item Filter Pipe
- Item Insertion Pipe
- Item Passthrough Pipe
- Item Void Pipe

**Automation Domain:**
- Laser Quarry
- (Future automation items)

**Power Domain:**
- Redstone Engine
- Stirling Engine
- RF Energy System
- (Future power items)

**Tools Domain:**
- Wrench
- Marking Fluid

**Materials/Crafting:**
- Gears (wooden, stone, iron, copper, diamond)
- (Other crafting components)

**Getting Started:**
- Installation
- Your First Pipe Network
- Understanding Tiers

## Technology Stack

### Zensical
- Configuration: `zensical.toml`
- Builds static site from markdown in `docs/` directory
- Material for MkDocs-inspired classic theme
- GitHub integration for "view source" buttons

### Key Commands
(Assuming standard Zensical commands - verify in main mod if needed)
```bash
# Build docs
zensical build

# Serve locally
zensical serve

# Deploy (if configured)
zensical deploy
```

## Source Material

### Main Mod Documentation
Rich source material available in `../logistics-mc-1.21.11/`:

**README.md** - Comprehensive user guide with:
- Feature overview and status
- All pipe types with recipes and usage
- Getting started guide
- Installation instructions

**docs/DESIGN.md** - Technical architecture:
- Three-tier system philosophy
- Module system architecture
- Rendering and routing details
- Future roadmap

**docs/PIPE_TYPES.md** - Detailed pipe behaviors
**docs/MATERIALS.md** - Material progression system
**docs/KILN_PROGRESSION.md** - Crafting progression
**docs/ASSETS.md** - Asset creation guidelines

### Content Adaptation Strategy
1. **User-facing content**: Adapt README.md sections into structured guides
2. **Technical content**: Simplify DESIGN.md for architecture overview
3. **Reference content**: Extract pipe/machine details into reference pages
4. **Visual content**: Include images, diagrams, recipes where helpful

## Documentation Principles

### Wiki-Style Approach
Inspired by **ftbwiki.org**, the documentation follows a granular, interconnected structure:
- **One page per item/block/concept** - Each piece of equipment gets dedicated coverage
- **Heavy cross-linking** - Related items are linked extensively
- **Domain organization** - Group by function (pipes, automation, power, tools)
- **Discoverability** - Users browse and discover through links, not just search
- **Concise, focused content** - Each page covers one topic thoroughly but briefly

### Page Structure Template
Each item/block page should include:

1. **Name & Brief Description** - What it is in 1-2 sentences
2. **Recipe** - Crafting recipe with ingredient links
3. **Behavior/Usage** - How it works, what it does
4. **Configuration** - If applicable (e.g., wrench interactions)
5. **Tips & Tricks** - Practical usage hints
6. **Related Items** - Heavy linking to:
   - Components used in recipe
   - Items it works with
   - Alternatives or upgrades
   - Related concepts

### Audience
- **Primary**: Minecraft players using the mod
- **Secondary**: Mod pack creators, server admins
- **Tertiary**: Developers interested in the mod's design

### Style Guidelines
- Clear, concise language (wiki-style, not guide-style)
- Facts and mechanics, not tutorials
- Recipe boxes for crafting (link all ingredients)
- In-game screenshots/diagrams (when available)
- Extensive cross-references between related items
- Short paragraphs, bullet points for readability

### Tier-Based Organization
Follow the mod's three-tier architecture:
1. **Tier 1 (Mechanical)**: Start here - basic pipes, no item awareness
2. **Tier 2 (Smart)**: Item-aware routing and decisions
3. **Tier 3 (Network)**: Abstract logistics (future)

Present features in progression order so new players aren't overwhelmed.

### Cross-Linking Examples
Heavy linking is key to wiki-style discoverability. Examples:

**Item Extractor Pipe page should link to:**
- [Wrench](../tools/wrench.md) - used to configure extraction face
- [Copper Transport Pipe](copper-transport-pipe.md) - for connecting network
- [Item Merger Pipe](item-merger-pipe.md) - for collecting extracted items
- [Redstone Engine](../power/redstone-engine.md) - extraction requires power
- [Pipe Networks](../core/pipe-networks.md) - concept overview
- Recipe ingredients: [Planks], [Glass]

**Laser Quarry page should link to:**
- [Stirling Engine](../power/stirling-engine.md) - recommended power source
- [RF Energy](../power/rf-energy.md) - power system concept
- [Item Extractor Pipe](../pipes/item-extractor-pipe.md) - for collecting drops
- [Copper Transport Pipe](../pipes/copper-transport-pipe.md) - for transport network
- Recipe ingredients: [Iron Gear](../materials/iron-gear.md), [Diamond Gear](../materials/diamond-gear.md), etc.

**Copper Transport Pipe page should link to:**
- [Marking Fluid](../tools/marking-fluid.md) - for network segmentation
- [Wrench](../tools/wrench.md) - for pipe configuration
- [Stone Transport Pipe](stone-transport-pipe.md) - slower alternative
- [Pipe Connectivity](../core/connectivity.md) - how pipes connect
- Recipe ingredients: [Copper Ingot], [Glass]

## Current Status

### Implemented in Mod (v0.1.0)
- ✅ All Tier 1 mechanical pipes (stone, copper, wood, iron, gold, sandstone, obsidian)
- ✅ All Tier 2 smart pipes (diamond, quartz)
- ✅ Engines (redstone, stirling)
- ✅ Laser quarry
- ✅ Wrench tool
- ✅ Marking fluid (copper pipe segmentation)

### Documentation Status
- ⚠️ Minimal placeholder structure
- ⚠️ No installation guide
- ⚠️ No pipe guides or tutorials
- ⚠️ No reference pages

### Domain Index Pages
Each domain (pipes/, automation/, power/, etc.) should have an `index.md` that:
- Lists all items in that domain with brief descriptions
- Groups items by sub-category if applicable (e.g., Tier 1 vs Tier 2 pipes)
- Links to core concepts relevant to that domain
- Serves as a navigation hub for browsing

**Example - pipes/index.md:**
```markdown
# Pipes

Pipes are the core of the Logistics mod, transporting items through your network.

## Tier 1: Mechanical Pipes
Basic pipes that perform mechanical operations without item awareness.

- [Stone Transport Pipe](stone-transport-pipe.md) - Very slow transport
- [Copper Transport Pipe](copper-transport-pipe.md) - Standard transport
- [Item Extractor Pipe](item-extractor-pipe.md) - Pull from inventories
- [Item Merger Pipe](item-merger-pipe.md) - Converge to single output
- [Golden Transport Pipe](golden-transport-pipe.md) - Speed boost
- [Item Passthrough Pipe](item-passthrough-pipe.md) - Bypass inventories
- [Item Void Pipe](item-void-pipe.md) - Delete items

## Tier 2: Smart Pipes
Item-aware pipes that make routing decisions.

- [Item Filter Pipe](item-filter-pipe.md) - Route by item type
- [Item Insertion Pipe](item-insertion-pipe.md) - Prefer inventories

See also: [Pipe Networks](../core/pipe-networks.md), [Tier System](../core/tier-system.md)
```

### Immediate Priorities
1. Set up domain directory structure (pipes/, automation/, power/, tools/, materials/, core/)
2. Create domain index pages (pipes/index.md, automation/index.md, etc.)
3. Build core concept pages (pipe-networks.md, tier-system.md, etc.)
4. Create individual pipe pages (start with Tier 1, most commonly used)
5. Create power system pages (engines, RF energy)
6. Create tool pages (wrench, marking fluid)
7. Create material/crafting component pages (gears)
8. Update main index.md as navigation hub
9. Create getting-started pages (installation, first-network)

## Multi-Version Support
The mod uses worktrees for different Minecraft versions:
- `logistics-mc-1.21.1` (1.21.1)
- `logistics-mc-1.21.11` (1.21.11 - primary)
- `logistics-mc-26.1` (future version)

Documentation should:
- Primarily target the current stable version (1.21.11)
- Note version-specific differences where relevant
- Use version-agnostic language where possible

## GitHub Integration
Configured in `zensical.toml`:
- Repository: `Indemnity83/logistics`
- Enables "edit this page" functionality
- Links back to main mod repository

## Development Workflow
1. Work in the `docs` branch (this worktree)
2. Reference main mod worktrees for source material
3. Build locally to preview changes
4. Commit and push documentation updates
5. Deploy site (process TBD)

## Page Template Example

Here's a complete example of a wiki-style item page:

```markdown
# Item Extractor Pipe

The **Item Extractor Pipe** is a [Tier 1](../core/tier-system.md) mechanical pipe that actively pulls items from adjacent inventories into your [pipe network](../core/pipe-networks.md).

## Recipe
**Crafting:**
- 1× Planks (any type)
- 1× Glass
- **Yields:** 8× Item Extractor Pipe

## Behavior
The Item Extractor Pipe pulls one item at a time from a connected inventory and inserts it into the pipe network.

**Key features:**
- Extracts from one face only (configurable)
- Pulls one item per operation
- Requires adjacent inventory to extract from
- Connects to other pipes on remaining faces

## Configuration
Use a [Wrench](../tools/wrench.md) to select which face extracts from the inventory:
1. Right-click with wrench
2. Face cycles through available directions
3. Active extraction face indicated by opaque connector

Only one face can extract at a time.

## Tips
- Place extraction face directly against the inventory (chest, furnace, etc.)
- Connect other faces to [Copper Transport Pipes](copper-transport-pipe.md) to build your network
- Use [Item Merger Pipes](item-merger-pipe.md) to collect items from multiple extractors
- Extraction is automatic - no redstone required
- Cannot extract through [Item Passthrough Pipes](item-passthrough-pipe.md)

## See Also
- [Copper Transport Pipe](copper-transport-pipe.md) - Connect extractors to your network
- [Item Merger Pipe](item-merger-pipe.md) - Combine multiple extraction streams
- [Wrench](../tools/wrench.md) - Configure extraction face
- [Pipe Networks](../core/pipe-networks.md) - Understanding pipe connectivity
- [Tier System](../core/tier-system.md) - Tier 1 mechanical pipes
```

### Template Structure Breakdown
1. **Title** - Item name as H1
2. **Introduction** - Brief description with links to key concepts
3. **Recipe** - Crafting requirements (link ingredients when they're mod items)
4. **Behavior** - What it does, how it works
5. **Configuration** - If applicable, how to configure with tools
6. **Tips** - Practical usage advice, common patterns
7. **See Also** - Related items and concepts (extensive links)

## Notes for Claude
- This worktree contains ONLY documentation, no mod code
- Source material is in sibling worktrees (primarily `../logistics-mc-1.21.11/`)
- **Wiki-style, not guide-style**: Factual, concise, heavily cross-linked
- Each item/concept gets one dedicated page
- Focus on user-facing clarity over technical completeness
- When adapting technical docs, simplify for player audience
- Maintain consistency with the mod's three-tier progression model
- Link everything - ingredients, related items, concepts, tools
- Domain organization: pipes/, automation/, power/, tools/, materials/, core/

# Logistics

A Minecraft tech mod about moving items, fluids and energy between machines, from simple transport pipes up to a powered logistics network that stocks, requests and crafts on its own. Player-facing names in `en_us.json` are canonical: when a term here names an in-game thing, it uses that exact name.

## Language

### Progression and tiers

**Tier**:
A named rung on the mod's single progression ladder (Crude, Basic, Sturdy, Industrial, Conductive, Precision, Resonant, Infernal, Dimensional, Deep). Each product line uses a subset of the rungs rather than its own scale.
_Avoid_: Level, grade, "Tier 1/2/3"

**Machine Tier**:
The tier a machine is built at: Basic, Industrial, Precision or Infernal. A higher tier scales the machine's area, speed and buffers, and a machine's tier only ever goes up.
_Avoid_: Farm tier, machine level

**Energy Tier**:
The four tiers shared by cables and batteries: Basic, Conductive, Resonant and Deep.
_Avoid_: Power ladder, energy ladder, Copper/Gold/Ender cable

**Mark**:
A numbered variant of a module or chassis (MkI, MkII, MkIII…) that raises its limits. A mark is not a tier.
_Avoid_: MK1, MK2, Mk 2, "Advanced"

**Upgrade Kit** (planned):
An item used on a machine in the world to raise its machine tier by exactly one step (Industrial, Precision and Infernal Upgrade Kits).
_Avoid_: Range Upgrade, upgrade (for tiers)

**Augment** (planned):
A slot-in modifier item that changes how a machine works (speed, efficiency, yield) without changing its tier.
_Avoid_: Upgrade, machine upgrade

### Crafting components

**Machine Frame**:
The shared crafting component at the centre of every machine recipe, paired with a Redstone Reception Coil.
_Avoid_: Machine Core, per-tier frames

**Gear**:
A mechanical crafting component made in an eight-step ladder from Wooden to Netherite, each crafted around the gear before it.
_Avoid_: Tin Gear (removed)

**Valve**:
A workbench-crafted electronics component, one per material, drawn from a catalogue rather than a strict ladder.

**Chipset**:
A machine-made component built in the Sequential Fabricator, accepted as the automated alternative to a gear in pipe and module recipes.
_Avoid_: Chip (removed)

**Core** (reserved):
Not a component. The cores line was dropped and no item carries the name; don't reintroduce it.
_Avoid_: Core, Machine Core, Sturdy Casing (removed)

### Machines

**Machine**:
A powered block crafted around a Machine Frame, with its own screen, a Redstone Control tab and a status readout.
_Avoid_: Device

**Byproduct**:
An extra output some machine recipes give beside the main result, usually at a chance.
_Avoid_: Secondary output, secondary drop, bonus output

**Sequential Fabricator**:
The machine that builds chipsets, cycling through the ones the player has queued.
_Avoid_: Fabricator

**Fluid Pump**:
The machine that drains fluid source blocks from the world into its own tank.
_Avoid_: Pump

**Laser Quarry**:
The machine that mines out a rectangular **Mining Area** down to bedrock, building a Laser Quarry Frame around it.
_Avoid_: Quarry frame, mining frame, work area (for the quarry)

**Marker**:
A block placed in an L of three and activated with a Wrench to set a custom Laser Quarry mining area. Unrelated to Marking Fluid.

### Farming

**Farm**:
A machine that harvests and replants one kind of produce within its reach, leaving the shape and styling of the field to the player. It never clears, builds or lays soil; the only ground it touches is its own plots. Each kind of produce has its own farm block.
_Avoid_: Multifarm, farm-type selection, ring farm

**Crop Farm**:
The farm for field crops: it harvests ripe crops in its reach and replants them.
_Avoid_: Cropland, Managed Farm

**Arboretum**:
The farm for trees: it fells grown trees standing on Humus in its reach and replants their saplings.
_Avoid_: Tree farm, lumberjack

**Reach**:
The box around a farm block in which the farm tends plants: 3 blocks out on every side, corners included, and 1 block above and below the farm.
_Avoid_: Work area, range, radius, Courtyard, Ring, Lane

**Germling**:
Whatever a farm plants: seeds, saplings, and so on.

**Plot**:
A spot in a farm's reach that the farm has harvested and keeps replanting with the same plant, re-tilling its ground when it has to. A plot left without a germling waits as an **Empty Plot** until a matching germling is available. The farm forgets a plot once something else occupies it or its ground is anything other than farmland, dirt or grass; breaking the farm forgets all its plots.
_Avoid_: Spot, tile, cell

**Humus**:
A crafted soil on which any sapling grows into the same small tree, a straight trunk under a round canopy, made of that sapling's own wood and leaves. It is the only soil an Arboretum tends.
_Avoid_: Compost, mulch

**Fertilizer**:
A crafted item from Apatite and Saltpeter, kept for a future machine augment. Farms do not consume it.

**Saltpeter**:
A mineral from sandstone and Breezes, used for gunpowder and Fertilizer.
_Avoid_: Niter

### Item transport

**Transport Pipe**:
A pipe that carries items with no network awareness. At junctions items go in a random direction unless a special pipe steers them. Comes in Stone, Copper and Golden.
_Avoid_: Item pipe (for transport pipes only), Gold Transport Pipe, wooden pipe

**Item Pipe**:
Either kind of pipe that carries items: a transport pipe or a logistics pipe.

**Traveling Item**:
An item visibly moving through pipes.
_Avoid_: Item in transit, in-flight item

**Marking Fluid**:
A dye-coloured fluid applied to copper pipes so that differently coloured pipes don't connect. Unrelated to the Marker.

### Logistics network

**Logistics Pipe**:
An item pipe that belongs to a logistics network and can advertise stock, make or fulfil requests, sink items or craft.
_Avoid_: Smart pipe, routed pipe, network pipe

**Logistics Network**:
A connected run of item pipes that includes logistics pipes and is powered through a Power Junction, so items are addressed to destinations instead of wandering.
_Avoid_: Network (unqualified), smart routing

**Power Junction**:
The only block where energy enters a logistics network.
_Avoid_: Network battery

**Basic Logistics Pipe**:
The base logistics pipe that every other one is crafted from. It acts as a sink for its adjacent inventory.
_Avoid_: Sink pipe, network backbone

**Chassis Logistics Pipe**:
A logistics pipe that does nothing on its own and takes its behaviour from the modules installed in it, MkI to MkV.
_Avoid_: Chassis MK1

**Module**:
An item installed in a Chassis Logistics Pipe to give it one logistics behaviour.
_Avoid_: Module (for the hidden behaviour every pipe is built from)

**Provider**:
A pipe or module that offers the contents of its adjacent inventory to the network and sends items out when they are ordered. It never asks for anything.

**Supplier**:
A pipe or module that keeps its adjacent inventory stocked to target amounts by placing orders for any shortfall.
_Avoid_: Requester (for suppliers)

**Passive Supplier Module**:
A module that keeps an inventory topped up only by catching matching items already travelling the network. It never places orders.

**Requester Logistics Pipe**:
A pipe through which a player picks items from everything the network holds and has them delivered.
_Avoid_: Requester (as a catch-all for anything that places orders), Remote Orderer

**Request**:
What a player, supplier or crafter asks the network for.
_Avoid_: Job, ItemRequest

**Order**:
The network's standing record of a request until it is delivered or cancelled, as in "on order".
_Avoid_: Promise, reservation, work order

**Sink**:
A destination that volunteers to accept items no one asked for. When several qualify, the one highest in **Sink Priority** wins, then the nearest.
_Avoid_: Item Sink (two words), Mod Sink, Polymorphic Sink

**Default Route**:
The catch-all sink that takes items no other destination will accept. One per network.

**Crafter Module**:
A module (and its Crafting Logistics Pipe form) that offers a recipe's output and, when ordered, orders the ingredients and drives an adjacent vanilla Crafter.
_Avoid_: Crafting module

**Process Logistics Pipe**:
A pipe that runs a non-crafting machine, ordering its inputs and taking out its output.

**Satellite Logistics Pipe**:
A numbered delivery point on another face of a machine that a Process Logistics Pipe can send inputs to.
_Avoid_: Named destination

### Fluids

**Fluid Pipe**:
A pipe that carries fluids, made by sealing a transport pipe with a Slime Ball or Tar. Fluid pipes never connect to item pipes or cables.

**Fluid Line**:
A connected run of fluid pipes, carrying one fluid at a time.
_Avoid_: Fluid network

**Fluid Packet**:
A hidden item that carries a quantity of fluid across a logistics network from a Fluid Provider to a Fluid Supplier.
_Avoid_: Fluid chunk

**Glass Tank**:
A fluid storage block. Tanks stacked vertically act as one column.

### Energy

**RF**:
The mod's single energy unit, always shown as RF and RF/t.
_Avoid_: FE, E, Redstone Flux, energy units

**Cable**:
A block that carries RF between engines, batteries and machines. The slowest cable on a run sets its throughput.
_Avoid_: Power cable

**Cable Grid**:
A connected run of cables, together with the engines, batteries and machines it reaches.
_Avoid_: Network, power network

**Battery**:
A block that stores RF on a cable grid and is drawn on only once the grid's engines can't keep up.
_Avoid_: Network battery

**Engine**:
A block that generates RF and pushes it out of one output face.
_Avoid_: Generator

**Heat Stage**:
An engine's visible temperature band: Cold, Cool, Warm, Hot, Overheat.

**Overheat**:
The state where an engine stops at maximum heat and has to be reset with a Wrench.
_Avoid_: Thermal shutdown, overheated

**Wrench**:
The configuration tool: it rotates faces, opens pipe screens and resets overheated engines without breaking anything.

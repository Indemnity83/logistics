# Modules

Modules are items that are installed into a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) to give it specific behaviors. Each module type provides a distinct logistics function — combining multiple modules in a single chassis creates a multi-function logistics node.

All modules are crafted from a [Blank Module](blank-module.md) base component.

## Module Slots by Chassis Mark

| Chassis | Slots |
|---------|-------|
| [MK1](../pipes/chassis-logistics-pipe.md#mk1-recipe) | 1 |
| [MK2](../pipes/chassis-logistics-pipe.md#mk2-recipe) | 2 |
| [MK3](../pipes/chassis-logistics-pipe.md#mk3-recipe) | 3 |
| [MK4](../pipes/chassis-logistics-pipe.md#mk4-recipe) | 4 |
| [MK5](../pipes/chassis-logistics-pipe.md#mk5-recipe) | 5 |

## Base Component

- **[Blank Module](blank-module.md)** - Required crafting base for all modules

## Extraction Modules

Pull items from adjacent inventories into the logistics network.

- **[Extractor Module](extractor-module.md)** (MK1/II/III) - Extract items from an adjacent inventory; higher marks extract faster or in greater quantities
- **[Advanced Extractor Module](advanced-extractor-module.md)** (MK1/II/III) - Extract with advanced filter control and scheduling options

## Provider & Supplier Modules

Advertise and supply items to the network.

- **[Provider Module](provider-module.md)** (MK1/II) - Advertise adjacent inventory contents to the network; higher mark provides larger item counts
- **[Supplier Module](supplier-module.md)** (Passive/Active) - Maintain stock levels in adjacent inventories by requesting items from the network; Active variant requests proactively

## Crafting Modules

Automate item crafting within the chassis.

- **[Crafter Module](crafter-module.md)** (MK1/II/III) - Define a crafting recipe to fulfill on demand; higher marks support more complex or faster crafting

## Sorting & Routing Modules

Control where items go within the network.

- **[Quicksort Module](quicksort-module.md)** - Sort items by type into designated storage locations
- **[Terminus Module](terminus-module.md)** - Mark a pipe as a network terminus; items reaching this pipe are held or redirected

## Sink Modules

Accept and store specific items or categories.

- **[Item Sink Module](item-sink-module.md)** - Accept a specific configured item type and deposit it into an adjacent inventory
- **[Polymorphic Sink Module](polymorphic-sink-module.md)** - Accept any item that matches an already-stored item type in the adjacent inventory (fills existing stacks)
- **[Enchantment Sink Module](enchantment-sink-module.md)** - Accept any enchanted item and deposit into a book or chest
- **[Mod Item Sink Module](mod-item-sink-module.md)** - Accept any item from a specified mod namespace

## See Also
- [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) - The pipe that holds modules
- [Blank Module](blank-module.md) - Base crafting component for all modules
- [Pipe Networks](../core/pipe-networks.md) - How the logistics network works
- [Tier System](../core/tier-system.md) - Tier 3 network logistics

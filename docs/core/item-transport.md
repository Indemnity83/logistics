# Item Transport

Items in Logistics move through pipes with **authentic visible motion** - you can see them traveling continuously through the network.

## Item Movement

When an item enters a pipe, it becomes a **traveling item** that:
- Moves continuously through the pipe (not instant teleportation)
- Has visible position and speed
- Maintains direction until reaching a junction
- Can be accelerated or slowed by different pipe types

## Speed and Acceleration

**Base Speed:**
- All standard pipes share the same maximum speed
- Items maintain constant velocity unless modified

**Speed Modifiers:**
- [Golden Transport Pipe](../pipes/golden-transport-pipe.md) - Accelerates items when powered by redstone
- [Stone Transport Pipe](../pipes/stone-transport-pipe.md) - Very slow transport
- **Velocity preservation** - Items keep their speed when moving between pipes

## Item Capacity

Pipes have limited internal capacity:
- Each pipe can hold approximately 5 items
- Slow pipes can drop items if overloaded
- Use faster pipes or multiple parallel pipes for high throughput

## Item Lifecycle

1. **Entry** - Item enters pipe from inventory, extractor, or hopper
2. **Travel** - Moves through pipes toward center
3. **Junction** - At pipe center, routing decision is made (see [Routing](routing.md))
4. **Exit** - Travels toward chosen exit direction
5. **Insertion** - Attempts to insert into adjacent inventory
6. **Drop/Complete** - Either inserted successfully or dropped as entity

## Insertion Behavior

When items reach a pipe exit:
- **Adjacent inventory present** - Attempts insertion via Fabric Transfer API
- **Insertion succeeds** - Item enters inventory, complete
- **Insertion fails** - Item drops as entity at pipe location
- **Void pipe** - Item deleted at pipe center, never reaches exit

## See Also
- [Routing](routing.md) - How items choose paths at junctions
- [Pipe Networks](pipe-networks.md) - Network structure
- [Golden Transport Pipe](../pipes/golden-transport-pipe.md) - Speed acceleration
- [Item Void Pipe](../pipes/item-void-pipe.md) - Item deletion

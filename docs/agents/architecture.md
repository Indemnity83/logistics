# Architecture: bootstrap wiring and domain map

Reference for how the mod starts up on each loader, how to add a domain, and where each domain's
main pieces live. The rules (loader-agnostic `common/`, no cross-domain imports) are in
`CLAUDE.md` under "Architecture"; this file is the wiring behind them. For what a term means, see
`GLOSSARY.md`.

## Source sets

| Path | Holds |
|---|---|
| `common/src/main` | Shared server/common code and resources (blockstates, recipes, lang, data) |
| `common/src/client` | Shared vanilla client code: screens, block entity renderers, client bootstrap |
| `common/src/gametest` | GameTest bodies shared by both loaders |
| `common/src/test` | JUnit tests |
| `fabric/src/{main,client,gametest,test}` | Fabric adapters and Fabric-only client wiring |
| `neoforge/src/{main,gametest,test}` | NeoForge adapters; client-only code under `com.logistics.neoforge.client` |

## Common initialization

1. **Fabric:** `fabric.mod.json` declares `com.logistics.fabric.LogisticsFabric` as the `main`
   entrypoint. Its `onInitialize()` runs `LogisticsCommonBootstrap.initialize()`, then registers
   Fabric capabilities, loot modifiers, tick handlers, commands, packets and biome modifications.
2. **NeoForge:** `@Mod("logistics")` constructs `LogisticsNeoForge`, which runs
   `LogisticsCommonBootstrap.initialize()` once from `RegisterEvent`, because NeoForge unfreezes
   the built-in registries during that event.
3. `LogisticsCommonBootstrap.initialize()` calls `DomainBootstraps.all()`, a `ServiceLoader` over
   `common/src/main/resources/META-INF/services/com.logistics.core.bootstrap.DomainBootstrap`.
4. Each domain's `initCommon()` registers its blocks, items, block entities and menus.

NeoForge deferred services (creative tabs, resource reload listeners) collect registrations
through common SPIs (`CreativeTabRegistrar`, `ResourceReloadRegistrar`) and attach to the right
NeoForge event bus from `LogisticsNeoForge`. The NeoForge SPI implementations are listed in
`neoforge/src/main/resources/META-INF/services/`.

## Client initialization

1. **Fabric:** the `client` entrypoint `LogisticsModClient` calls
   `LogisticsClientBootstrap.initialize()`, which discovers `ClientDomainBootstrap`
   implementations from
   `fabric/src/client/resources/META-INF/services/com.logistics.core.bootstrap.ClientDomainBootstrap`
   (one `Logistics<Domain>Client` per domain).
2. **NeoForge:** `LogisticsNeoForge` calls `NeoForgeClientSetup.register(modBus)` only when
   `FMLEnvironment.getDist().isClient()`. `NeoForgeClientSetup` registers screens, block entity
   renderers, block colors, special model renderers, fluid models and client payload handlers on
   NeoForge mod-bus events.

Shared renderers live in `common/src/client`; the loaders only supply event wiring. Cable
blockstates are plain vanilla blockstates in `common/src/main/resources`, read by both loaders,
because cables render through `CableBlockEntityRenderer`.

## Adding a domain

1. Create `common/src/main/java/com/logistics/<domain>/` (and `common/src/client/java/...` for
   shared client code). Loader packages only for loader API wiring.
2. Create `Logistics<Domain> implements DomainBootstrap` in `com.logistics`, implement
   `initCommon()`, and list it in the common `DomainBootstrap` services file.
3. Fabric client, when needed: `Logistics<Domain>Client implements ClientDomainBootstrap` with
   `initClient()`, listed in the Fabric `ClientDomainBootstrap` services file.
4. NeoForge client, when needed: add registrations to `NeoForgeClientSetup` or a small helper under
   `com.logistics.neoforge.client`.

## Domain map

Domains are the `DomainBootstrap` implementations: `LogisticsCore`, `LogisticsPipe`,
`LogisticsPower`, `LogisticsAutomation`, `LogisticsForestry`. Each one's `ALIAS` static class
holds its registry-ID aliases (see the backward-compatibility policy).

**Core** (`com.logistics.core`) — foundation for every domain: tools, crafting intermediates,
config, worldgen, the Redstone Engine, and the shared machine framework (`core.machine`:
`MachineEntity`, `MachineBuilder`, components, upgrades, tiers). `core.lib` is the contract layer
the other domains depend on:

- `core.lib.block` — `BaseBlockEntity` (NBT and tick base; live format read by `loadLogisticsData`,
  old formats by `loadLegacyData`), `BlockEntityTypeFactory`, and the capability interfaces
  `HasItemStorage`, `HasEnergyStorage`, `HasFluidStorage` under `capability/`.
- `core.lib.power` — `EngineEntity` (base for all engines) and `HeatStage`
  (COLD → COOL → WARM → HOT → OVERHEAT).
- `core.lib.network` — logistics network contracts: `ILogisticsNetwork`, `IWorldView`,
  `INetworkGraph`, `NetworkGraph`, `Order`, `IngredientChecker`, `ProviderCanFulfill`.
- `core.lib.platform` — loader seams: `PlatformService`, `ServerNetworking`, and (client source
  set) `ClientNetworking`.
- `core.lib.pipe` — `Module`, `TravelingItem`, `PipeConnectionLookup`.
- `core.lib.energy`, `core.lib.storage`, `core.lib.fluids` — `EnergyCapabilityLookup`,
  `ItemStorageLookup`, `FluidStorageLookup`.

**Pipe** (`com.logistics.pipe`) — item, fluid and chassis pipes. A `Pipe` composes `Module`
instances for behavior (`modules/`: `ProviderModule`, `SupplierModule`, `RequesterModule`,
`SinkModule`, `CraftingModule`, `NetworkRouterModule`; pipe types in `PipeTypes` put
`CraftingModule` first and `NetworkRouterModule` last). The network layer is in `network/`:
`NetworkRegistry`, `PipeNetwork` (implements `ILogisticsNetwork`), `NetworkController`.
`TravelingItem` is an item in transit with progress-based movement.

**Power** (`com.logistics.power`) — engines (`engine/`: Stirling, Magmatic, Steam, Fuel, Reaction,
Creative; all extend `EngineEntity`), cables and batteries. Energy on Fabric goes through the
Team Reborn Energy API.

**Automation** (`com.logistics.automation`) — the machines: kiln, macerator, sawmill, alloy
smelter, crucible, refinery, fabricator, transposer, pump, laser quarry.

**Forestry** (`com.logistics.forestry`) — tend-only farms (`farm/`, see `docs/adr/0001`) and humus.

`com.logistics.api` (`LogisticsApi`, `TransportApi`) is the public API surface.

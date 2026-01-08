# Pipe Module Design Sketch

Goal: capture the "wishful" API first (Jeffrey Way style), then outline modules, then the base Pipe type.

Terminology: when we say “item” in this document, we mean an `ItemStack`.

## Non-goals (initial)

- Global pathfinding or network solvers
- Request/crafting systems
- Backpressure or bouncing on failed inserts
- Cross-pipe coordination beyond local routing

## ItemStack Lifecycle

1) A neighbor attempts to insert via Fabric Transfer API.
2) Modules may reject the transfer (ingress rules, filters, etc.).
3) Accepted stacks become a `TravelingItemStack` traveling from the entry face toward the pipe center.
4) When the traveling stack reaches the pipe center (progress ~0.5), `route(...)` is called and returns **0..N** `TravelingItemStack` values:
    - empty list = discard (void behavior)
    - one item with explicit direction = normal routing (iron/copper behaviors)
    - many items = split behavior
      Each returned traveling item is considered to be traveling from center toward its chosen exit.
5) Traveling continues toward each chosen exit.
6) Only when a traveling item reaches an exit (progress ~1.0), insertion is attempted via Transfer API into the adjacent storage along its direction.
7) If insertion fails at the exit, the stack is dropped.

## 1) Pipe Classes (Wishful API)

```java
class CobblestoneTransportPipe extends Pipe {
  CobblestoneTransportPipe() {
    super(new TransportModule(...));
  }
}

class GoldTransportPipe extends Pipe {
  GoldTransportPipe() {
    super(new TransportModule(...), new AccelerationModule(...));
  }
}

class CobblestoneFluidPipe extends Pipe {
  CobblestoneFluidPipe() {
    super(new FluidTransportModule(...));
  }
}
```

More examples (just composition):

```java
class IronDirectionalPipe extends Pipe {
  IronDirectionalPipe() {
    super(new TransportModule(...), new DirectionalRoutingModule(...));
  }
}

class CopperRoundRobinPipe extends Pipe {
  CopperRoundRobinPipe() {
    super(new TransportModule(...), new RoundRobinModule(...));
  }
}

class QuartzPipe extends Pipe {
  QuartzPipe() {
    super(new TransportModule(...), new ComparatorModule(...));
  }
}

class VoidPipe extends Pipe {
  VoidPipe() {
    super(new VoidModule(...));
  }
}
```

Notes
- Each pipe class is tiny and mostly declarative.
- Modules are the only place with logic.
- No JSON mention yet; the goal is to align the API with how we want it to read.
- `PipeSpec`-based approach may replace per-class pipes later to avoid many block registrations; the wishful API is for readability.

## 2) Modules (Behavior Building Blocks)

Each module is a small, focused unit. Modules should be composable, order-aware if needed, and stateless unless they own explicit runtime state.

Modules must not store per-pipe runtime data in Java fields. All per-pipe state lives in the Pipe Block Entity and is accessed through `PipeContext` using a module-specific namespace.

### Conceptual Categories (not types)

These categories are design groupings to guide where logic lives. All modules still implement the same `Module` interface.

- **Ingress policy** — who may insert into the pipe
- **Extraction** — modules that actively pull items into the pipe
- **Transport** — motion (speed, acceleration, caps)
- **Flow control** — routing decisions at the pipe center
- **System / effects** — redstone, UI, diagnostics
- **Future logistics** — crafting, requests, providers

Candidate modules (initial):

- TransportModule
    - Owns base speed, max speed, acceleration baseline (if any)
    - Supplies default routing when no other routing module exists

- AccelerationModule
    - Applies acceleration when powered (gold pipes)
    - Exposes max speed cap or boosts

- DirectionalRoutingModule
    - Routes items to one face, exposes wrench cycling
    - Optionally rejects items from output side

- RoundRobinModule
    - Chooses next output in a round-robin order
    - Owns per-pipe index state (stored in the Block Entity)

- PipeOnlyIngress
    - Ingress policy: allows inserts only from other pipes

- SingleFaceInventoryIngress
    - Ingress policy: allows inserts only from inventories on the active face
    - Owns active-face state and wrench cycling

- ExtractionModule
    - Actively pulls items from inventories
    - Owns cooldown, items-per-extract, and auto-select logic

- FilterModule (future)
    - Diamond-style filter with per-side ghost slots

- VoidModule
    - Discards items at center
    - Optional particles

- ComparatorModule
    - Exposes redstone output based on items in pipe

- FluidTransportModule (future)
    - Handles FluidStorage routing and movement

Module API sketch (not final):

```java
interface Module {
  default void onTick(PipeContext ctx) {}

  // Motion (modules may contribute/override via composition rules)
  default float getTargetSpeed(PipeContext ctx) { return 0f; }
  default float getAcceleration(PipeContext ctx) { return 0f; }
  default float getMaxSpeed(PipeContext ctx) { return 0f; }

  /**
   * Called exactly once when a TravelingItemStack reaches the pipe center.
   * Return:
   *   - empty list: discard
   *   - one item: normal routing
   *   - many items: split
   */
  default List<TravelingItemStack> route(PipeContext ctx, TravelingItemStack item, List<Direction> options) {
    return List.of(item); // no opinion
  }

  // Acceptance is "all must pass".
  default boolean canAcceptFrom(PipeContext ctx, Direction from, ItemStack stack) { return true; }

  // Event hook: any module may react to wrench use (flow-control and ingress modules are expected to use this most)
  default void onWrenchUse(PipeContext ctx, ItemUsageContext usage) {}
  default int comparatorOutput(PipeContext ctx) { return 0; }
}
```

Module composition rules (draft):
- Speed is resolved by composition rules (e.g., Transport provides a base, Acceleration may modify and/or cap via max speed).
- Routing runs at center; modules are evaluated in order and the first module to return something other than `List.of(item)` wins.
    - Returning an empty list discards immediately.
- Acceptance is "all must pass" (if any module returns false, reject).
- Optional: an explicit ordering or priority list (e.g., Transport first, then modifiers, then routing choosers).
- Future routing modules may need to hold or buffer items at center (e.g., clumping). The `route(...)` contract is expected to evolve if/when that becomes necessary.

## 3) Base Pipe Type

We can choose between a class or interface. Prefer a concrete base class with a clear lifecycle, and a small interface for block integration if needed.

Base class responsibilities:
- Holds list of modules
- Delegates lifecycle hooks to modules
- Aggregates responses (routing, speed, acceptance)
- Provides helper for module ordering/priority

Pipe base class sketch:

```java
abstract class Pipe {
  private final List<Module> modules;

  protected Pipe(Module... modules) {
    this.modules = List.of(modules);
  }

  public float getTargetSpeed(PipeContext ctx) {
    return modules.stream()
      .map(m -> m.getTargetSpeed(ctx))
      .filter(speed -> speed > 0f)
      .findFirst()
      .orElse(DEFAULT_SPEED);
  }

  public float getAcceleration(PipeContext ctx) {
    return modules.stream()
      .map(m -> m.getAcceleration(ctx))
      .filter(accel -> accel > 0f)
      .findFirst()
      .orElse(0f);
  }

  public List<TravelingItemStack> route(PipeContext ctx, TravelingItemStack item, List<Direction> options) {
    for (Module module : modules) {
      List<TravelingItemStack> updated = module.route(ctx, item, options);

      // Convention: a module returns List.of(item) to indicate "no opinion".
      if (updated.isEmpty()) return List.of();
      if (updated.size() != 1 || updated.get(0) != item) return updated;
    }

    // Default behavior when no module handled routing.
    return RandomRouting.splitOrPick(item, options, ctx);
  }

  public boolean canAcceptFrom(PipeContext ctx, Direction from, ItemStack stack) {
    for (Module module : modules) {
      if (!module.canAcceptFrom(ctx, from, stack)) return false;
    }
    return true;
  }
}
```

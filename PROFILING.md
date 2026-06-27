# Profiling Logistics

How to find where Logistics spends server-tick time. Three tiers, cheapest first.

## 1. Spark — the primary tool

[Spark](https://spark.lucko.me) is the standard Minecraft async sampling profiler and tick
monitor. It attributes lag down to individual methods and produces a shareable web report, with
**no instrumentation required** — it samples the JVM, so it already sees Logistics code by class
and method.

This is the right tool both for our own profiling and for diagnosing a player's "your mod lags my
base" report: ask them to capture a profile and paste the link.

**Common commands (in-game or server console):**

| Command | What it does |
|---|---|
| `/spark profiler --timeout 60` | Sample for 60s, then print a link to the web report |
| `/spark profiler --thread server` | Limit sampling to the server thread |
| `/spark health` | TPS, MSPT, CPU, and memory at a glance |
| `/spark tps` | Current tick rate |
| `/spark heapsummary` | Live heap breakdown by class (allocation hunting) |

In the report, our work shows up under named buckets such as `logistics:item_networks`,
`logistics:pathfind`, and `logistics:power_cables` (see tier 2).

### Running Spark in dev

Spark is **not bundled** with the mod — players install it themselves. For local dev it is a
runtime-only dependency (pinned in `gradle.properties` as `spark_fabric_version` /
`spark_neoforge_version`), so it is always present in `runClient` / `runServer`:

```bash
./gradlew :fabric:runClient      # /spark is available immediately
./gradlew :neoforge:runClient
```

Spark sometimes lags a brand-new MC snapshot. If a build for the current Minecraft version isn't on
Modrinth yet, bump the `spark_*_version` properties when one ships, or comment out the `runtimeOnly`
line in the loader `build.gradle` until then (the release jar never references Spark either way).

## 2. Named profiler sections (built-in profiler)

Logistics wraps its hot paths in named [`ProfilerFiller`](https://minecraft.wiki) sections via
`com.logistics.core.lib.LogisticsProfiler`, all prefixed `logistics:`. These appear in **both**
Spark and Minecraft's built-in profiler:

```
/debug start
  … play / let the network run …
/debug stop      # writes debug/profiling/*.txt under the run directory
```

Current sections:

| Section | Covers |
|---|---|
| `logistics:item_networks` | All item-pipe network ticks in a level |
| `logistics:network_dispatch` | A single network's standing-order dispatch loop |
| `logistics:pathfind` | A* routing path search (suspected hot spot in large networks) |
| `logistics:power_cables` | All cable-network power ticks in a level |
| `logistics:cable_scan` / `logistics:cable_transfer` | Device discovery vs. energy transfer within a cable network |
| `logistics:fluid_pipes` | Per fluid-pipe server tick |
| `logistics:machines` | A machine's component tick loop |

**Adding a section:** wrap the work and always balance push/pop in a `try/finally`:

```java
LogisticsProfiler.push("my_section");
try {
    // work
} finally {
    LogisticsProfiler.pop();
}
```

`Profiler.get()` returns a no-op filler when no profiler is active (outside a server tick), so these
calls are safe and effectively free to leave in place.

## 3. Targeted nanosecond timers

For a one-off measurement of a specific operation, the cheapest tier is a `System.nanoTime()` timer
gated behind the `logistics.timing` system property. There is already one around pipe NBT
deserialization (`PipeBlockEntity.loadLogisticsData`):

```bash
./gradlew :fabric:runClient -Dlogistics.timing=true
# logs: [timing] PipeBlockEntity loadLogisticsData at <pos> took <n> ms (items=<n>)  (only when ≥2ms)
```

Use this style for hypothesis-checking a single method; use Spark to find *which* method to check.

## What we deliberately do not do

- **No telemetry from players.** We do not stream performance data off players' machines. Profiling
  is dev/operator-initiated and stays local (Spark reports are uploaded only when *you* run the
  command).
- **No Sentry/APM for profiling.** Sentry's model targets rare error events and request/response
  workloads, not a 20 TPS game loop — wrong tool for continuous tick sampling.

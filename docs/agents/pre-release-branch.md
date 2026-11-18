# Pre-release branches

A pre-release branch tracks a Minecraft snapshot, pre-release or RC ahead of its stable release.
It trails main as a forward-port target and does not become main until the Minecraft version
ships. Read this when cutting one, building one, or pushing to one.

## Stage comes from `minecraft_version`

Whether a branch is pre-release is decided from `minecraft_version` in `gradle.properties`, never
from a branch list: a bare version (`26.2`) is released, anything carrying a qualifier
(`26.3-snapshot-7`, `26.3-pre-1`, `26.3-rc1`) is pre-release. The roles rotate on their own: when
the pre-release ships and `minecraft_version` becomes bare, that branch starts enforcing the
checks, and the next pre-release branch is exempt from the day it is created.

**The `pre-push` hook skips pre-release branches.** On a released `mc/*` branch the hook runs
`./gradlew test` before a direct push. A pre-release branch tracks upstream snapshot artifacts
that get rotated and deleted, so Gradle often cannot resolve its dependencies — and a failed
*configuration* fails every task, not just the tests, which would block every push and train
everyone to pass `--no-verify`. A push there running no checks is expected, not a bypass.

## Third-party mod dependencies

JEI, Jade and spark are all compile-only or dev-only, and none of them publish for a Minecraft
pre-release on day one. Without a decoupling, a pre-release branch would be unbuildable — and
therefore untestable — until every one of them caught up, weeks after the code itself is ready.

Only **JEI** is genuinely coupled: its *artifact id* embeds a Minecraft version
(`mezz.jei:jei-<mc>-fabric`), so bumping `minecraft_version` changes what Gradle asks for and
resolution fails. `jei_mc_version` in `gradle.properties` is that artifact's Minecraft line, kept
separate so a pre-release branch can compile against the last line JEI shipped for. The API surface
the mod uses is stable across lines. Raise it to match `minecraft_version` once JEI catches up.

Jade and spark need no equivalent — their Minecraft line lives in the version string
(`jade_fabric_version=26.2.11+fabric`), so pointing them at an older build is an ordinary version
change. One spark build usually spans several Minecraft lines.

This decouples the *dependencies* only. A new Minecraft release still needs its own API migration
before the branch compiles.

# Crash Reporting & Privacy

Logistics can optionally send **sanitized** crash/error diagnostics to help identify and fix bugs.
This is **disabled by default** and must be turned on explicitly by a server operator (or, in
single-player, by you). Each install opts in independently — enabling it on a server does **not**
enable anything on connected players' clients.

> Logistics is not an official Minecraft product and is not approved by or associated with Mojang or
> Microsoft.

## Turning it on or off

| Command | Effect |
| --- | --- |
| `/logistics crashreports` | Show current status |
| `/logistics crashreports enable` | Opt in to sending sanitized reports |
| `/logistics crashreports disable` | Stop sending reports |
| `/logistics crashreports notify off` | Hide the one-time join invite |
| `/logistics crashreports notify on` | Re-show the join invite |

All of these require operator (gamemaster) permission. Disabling is exactly as easy as enabling.

## What is collected (only when enabled)

- Mod version, Minecraft version, mod-loader version
- Java version and operating-system type
- Stack traces and exception messages **originating from Logistics' own code** (`logistics` loggers)
- Generic runtime context Sentry attaches automatically (e.g. OS family, JVM)

## What is intentionally **not** collected

- Player chat or world data
- Player names, player UUIDs
- IP addresses or server addresses / hostnames
- Full configuration contents
- Anything matched as a secret (`password`, `token`, `secret`, `key`, `dsn`, …)

Before any report leaves the process, a `beforeSend` filter strips the host/server name and redacts
home directories, IP addresses, UUIDs, and secret-like `key=value` pairs from the message and
exception text. Sentry's `send-default-pii` is left **off**, so no PII is attached by default.

This scrubbing is best-effort and biased toward over-redaction. Reports are processed by
[Sentry](https://sentry.io) unless a custom DSN is configured via `crashReporting.dsnOverride` in
`config/logistics.json`.

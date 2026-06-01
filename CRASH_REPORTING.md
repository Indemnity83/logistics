# Crash Reporting & Privacy

Logistics includes an **optional** crash reporter that can send **sanitized** error diagnostics to
the developers so bugs get found and fixed faster. We built it to respect you: it is **off by
default**, it only ever looks at Logistics' own errors, and you can see exactly what it would send
before you ever turn it on.

This page explains, in plain language, what it does, what it collects, what it deliberately does
**not** collect, and how to control it. If anything here is unclear or you spot something that looks
sensitive, please [open an issue](https://github.com/Indemnity83/logistics/issues) — we take this
seriously.

> Logistics is not an official Minecraft product and is not approved by or associated with Mojang or
> Microsoft.

---

## The short version

- ✅ **Off by default.** Nothing is sent unless an operator turns it on.
- ✅ **Opt-in, and easy to opt out.** One command on, one command off.
- ✅ **Logistics only.** It reports errors from this mod's own code — not from Minecraft, your server,
  or other mods.
- ✅ **Sanitized.** No player names, UUIDs, IP addresses, server addresses, chat, or world data.
- ✅ **You can verify it.** `/logistics crashreports preview` shows you a real, sanitized example
  report without sending anything.
- ✅ **Per-install.** Turning it on for a server does **not** turn it on for your players.

If you do nothing, Logistics sends no diagnostics. That's the default, and it's a perfectly good
choice.

---

## Why this exists

When Logistics misbehaves in the real world, the developers usually never hear about it. Most players
don't file bug reports, and even when they do, the useful details are buried in a log file that's
hard to share. That means bugs can linger for weeks because there's simply no signal that they're
happening.

Opting in to crash reporting sends a small, sanitized diagnostic whenever Logistics hits an error.
That gives the developers the one thing they're usually missing: a clear, actionable signal that
something broke, what it was, and which version it happened on — so it can be fixed quickly. It is
purely a debugging aid, not analytics, and it is intentionally narrow.

---

## Your choice, always

Crash reporting is **disabled by default** and must be turned on explicitly. The toggle lives in your
config (`config/logistics.json`) and is controlled with operator-level commands.

**Single-player:** you are the operator, so you decide for your own game.

**Multiplayer:** only a server operator can enable it, and when enabled it reports **server-side**
Logistics errors. Each install has its own setting, so a server operator opting in does **not** opt
in any connected player's client. If you run a client and want client-side reporting, that's a
separate choice you make on your own machine.

---

## Commands

All of these require operator (gamemaster) permission. Turning it off is exactly as easy as turning
it on.

| Command | What it does |
| --- | --- |
| `/logistics crashreports` | Show whether reporting and the join notice are on or off |
| `/logistics crashreports enable` | Opt in to sending sanitized reports |
| `/logistics crashreports disable` | Stop sending reports |
| `/logistics crashreports preview` | Print an example sanitized report **without sending it** |
| `/logistics crashreports notify off` | Hide the one-time join invitation |
| `/logistics crashreports notify on` | Show the join invitation again |

### See for yourself: `preview`

The `preview` command is the heart of our "trust, but verify" approach. It builds a sample error and
runs it through the **exact same sanitization** that a real report goes through, then prints the
result to you — **nothing is sent.** The sample deliberately contains fake sensitive values (a home
directory, an IP, a UUID, a token) so you can watch them get scrubbed in front of you. It works
whether or not reporting is enabled, so you can inspect the output before deciding to opt in.

---

## What is collected (only when enabled)

When reporting is on and Logistics hits an error, a report may include:

- **Mod, Minecraft, and loader versions** — so a bug can be tied to a specific release.
- **Java version and operating-system family** (e.g. "Windows", "Linux") — common environment context.
- **The error itself** — the exception type, message, and stack trace, **originating from Logistics'
  own code**.
- **Generic runtime context** that the reporting library attaches automatically (e.g. JVM and OS
  family). This does not include personal identifiers.

That's it. The goal is "what broke, where, and on what version" — nothing more.

## What is never collected

We deliberately exclude, and actively scrub for:

- ❌ Player names or player UUIDs
- ❌ IP addresses
- ❌ Server addresses or hostnames
- ❌ Chat messages
- ❌ World data, coordinates, or save contents
- ❌ Your full configuration
- ❌ Anything that looks like a secret (values labelled `password`, `token`, `secret`, `key`, `dsn`, …)

We never intentionally send any of the above, and the sanitizer below is a second line of defense in
case any of it ever slipped into an error message.

---

## How your data is protected

Several safeguards work together:

1. **Off by default.** No opt-in, no data. Ever.
2. **Scoped capture.** Only errors logged by Logistics' own code are eligible. The reporter does
   **not** install a global error handler, so errors from Minecraft, your server, or other mods are
   never picked up.
3. **No PII by default.** The reporting library's "send personal information" setting is left
   **off**, and the host/server name is never attached.
4. **Active scrubbing.** Before a report leaves your machine, a sanitizer rewrites the message and
   error text to remove home-directory paths (e.g. `/Users/you/…` → `~`, Windows paths too), IP
   addresses (→ `<ip>`), UUIDs (→ `<uuid>`), and secret-like `key=value` pairs (→ `key=<redacted>`).
5. **Verifiable.** `/logistics crashreports preview` lets you see the sanitized output yourself.

This sanitization is best-effort and intentionally biased toward over-redaction: when in doubt, it
strips it.

---

## Where reports go

Reports are processed by [Sentry](https://sentry.io), a widely used error-monitoring service, on a
project owned by the mod's developers.

**Self-hosting:** if you'd rather send reports to your own Sentry project (or a local endpoint of
your choosing), set `crashReporting.dsnOverride` in `config/logistics.json` to your own DSN. When set,
reports go there instead of the default project.

---

## How to check the current state, or turn it off

- Run `/logistics crashreports` to see the current status at any time.
- Run `/logistics crashreports disable` to stop immediately.
- Or open `config/logistics.json` and set `crashReporting.enabled` to `false`.

When disabled, the reporter sends nothing and runs no background networking.

---

## FAQ

**Will this slow down my game or server?**
No meaningful impact. Reports are only created on an actual error and are sent on a background thread,
so the game thread isn't blocked.

**Does enabling it on my server enable it for my players?**
No. Settings are per-install. A server operator's choice only affects server-side Logistics errors.

**Is it really "anonymous"?**
We call it **sanitized**, not anonymous, on purpose. We don't intentionally collect identifiers and
we actively scrub for them, but no scrubber is perfect, so we'd rather be precise than overstate it.
That's also why `preview` exists — so you can check.

**What if `preview` shows me something sensitive?**
Please [open an issue](https://github.com/Indemnity83/logistics/issues) and tell us what you saw (you
don't need to include the sensitive value itself). We'll tighten the sanitizer.

**Can I turn off just the in-game invitation but leave reporting off?**
Yes — `/logistics crashreports notify off` hides the prompt without changing anything else.

---

*Thank you for considering it. Opting in is genuinely helpful, and turning it down is completely
fine — either way, you're in control.*

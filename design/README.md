# Logistics — Design & Roadmap

This folder is the **planning brain** of the Logistics mod. It holds the durable "why" and "what we decided": the vision, the design principles, a feature-by-feature breakdown of the classic mods we're drawing from, the target module architecture, and the phased roadmap.

It is **not** the user-facing documentation. Player docs live on the `docs` branch (the Zensical site published to <https://indemnity83.github.io/logistics/>). This folder is for contributors and maintainers, and it's allowed to be opinionated and a work-in-progress.

## Operating model

We split the work across three tools, each doing the job it's best at:

| Job | Tool | Why |
|---|---|---|
| **The "why" + decisions** (vision, per-mod breakdowns, architecture) | **Markdown, here in `design/`** | Narrative, versioned, reviewable in PRs. Rarely changes once decided. |
| **Live progress** (what's being built, status, dates) | **GitHub Project #4 "Logistics Roadmap"** | Filterable, dated, status columns, epic→sub-issue hierarchy. |
| **Feedback** (contested calls, community votes) | **GitHub Discussions** (Ideas / Polls) | Open-ended, reactions as a demand signal. |

**Markdown first, then the board.** We author and argue the decisions here. Once a section settles, we decompose it into the Project: each *feature row* in a breakdown becomes a *sub-issue*, grouped under a *mod-area epic*, filed under a *phase milestone*. The doc explains the decision; the issue tracks the work. They are not duplicates.

## What's here

| File | What it is |
|---|---|
| [`vision.md`](vision.md) | The goal: recreate the classic 1.7.10-era tech experience in modern Minecraft. Scope boundaries. |
| [`principles.md`](principles.md) | The decision framework (**Port / Modernize / Skip**), modernization rules, balance philosophy, and the shared decision-table template. |
| [`architecture.md`](architecture.md) | Current domains and the **provisional** module/jar split. |
| [`roadmap.md`](roadmap.md) | The phased plan (Phase 0–3), what's done, and how it maps to Project #4. |
| [`mods/`](mods/) | One feature breakdown per source mod: BuildCraft, Logistics Pipes, Thermal Expansion, Forestry, Railcraft. |
| [`features/`](features/) | **Feature briefs** — settled roadmap rows expanded into start-ready specs (problem, requirements, design sketch grounded in the code, open questions). One file per feature. |
| [`rfcs/`](rfcs/) | **RFCs** — the contested, unsettled design calls (TBD rows): the question, options with trade-offs, a leaning, and how we'll decide. Authored here, then taken to a Discussion. |

## How to read a breakdown

Every `mods/*.md` file uses the same decision table. See [`principles.md`](principles.md) for the full template and legend. In short: each row is one feature, tagged with a **Decision** (Port / Modernize / Skip), a **Status** (✅ Done / 🚧 Planned / — Not started / ❌ Won't port), and what it **maps to** in the codebase or roadmap.

## How to contribute to the direction

- **Propose or debate a decision:** open a GitHub Discussion under **Ideas** (or **Polls** for a vote). Link the relevant `design/` section.
- **Change a recorded decision:** open a PR editing the relevant `design/` file. The diff *is* the record of the change.
- **Pick up work:** find the issue on Project #4; the `Maps to` column connects the doc to the board.

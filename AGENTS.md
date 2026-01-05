# Agent Instructions

## Project
- Name (temporary): Logistics
- Target: Minecraft 1.21, Fabric
- Core goals: authentic in-pipe item motion, mod interoperability, Logistics Pipes-style request/autocrafting via a Request Table block.

## How to Work
- Follow `PLAN.md` for scope, phases, and architecture.
- Prefer incremental changes that preserve the ability to test frequently.
- Keep visuals BuildCraft-like: thin pipe geometry with visible connections.
- Default to Fabric Transfer API for inventory/fluids integration.

## MVP Priorities
1) Pipe block + connectivity + thin models.
2) Traveling item simulation + smooth client render.
3) Extraction/insertion with ItemStorage.
4) Request Table + provider pipes + vanilla Crafter integration.

## Naming / IDs
- Use mod id `logistics` unless told otherwise.
- Avoid references to "BuildCraft" or "Logistics Pipes" in naming.


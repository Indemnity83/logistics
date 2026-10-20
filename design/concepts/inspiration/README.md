# Inspiration

Builds by other players that shaped the farm designs. These are references, not assets: none of
this ships in the mod. Each image is credited to the creator where a watermark or link names
them, and to the pin it came from.

| Image | Source | What we take from it |
|---|---|---|
| [garden-pergola-vine-curtain.jpg](garden-pergola-vine-curtain.jpg) | [Pinterest pin](https://www.pinterest.com/pin/4644405859425757/) (uploaded by a user, original creator not named) | A wooden pergola with a full curtain of hanging vines, over raised planters with dark trapdoor rims holding pumpkins, with bamboo and crops behind. Ideas for the **Glow Berry Farm** and **Cocoa Plantation** bodies, and for planter-style beds. |
| [pergola-rows-glow-berry-field.jpg](pergola-rows-glow-berry-field.jpg) | _URL pending_ | Rows of pergolas: two posts on mossy stone footings and a double slab beam, with glow-berry vines hanging the full length. Each row sits on its own grass plot, with dirt paths between rows. A model for the **Glow Berry Farm's in-world layout**. |
| [foxel-garden-crop-plots.jpg](foxel-garden-crop-plots.jpg) | [Pinterest pin](https://www.pinterest.com/pin/125467539613969033/); watermark **Foxel** (Instagram) | A large estate garden in fenced plots of one crop each, among them pergolas of hanging crops, bamboo and cactus beds, gourd-like rows and a stone-rimmed water bed, all along wide paths. Shows a whole set of farm types living side by side as one designed garden. |
| [raised-bed-garden-and-orchard.jpg](raised-bed-garden-and-orchard.jpg) | [Pinterest pin](https://www.pinterest.com/pin/861735709989023211/), linking to a [Patreon post](https://www.patreon.com/posts/large-garden-and-124519775) | Many small raised beds, mostly 2×3 to 3×4, each with one crop or flower, separated by paths, bushes, lamp posts and small trees, with arches over the entrances. Farms here are small and irregular, not one large square. |
| [tinycraft-crop-farm-idea.jpg](tinycraft-crop-farm-idea.jpg) | [Pinterest pin](https://www.pinterest.com/pin/1137088605925337956/); watermark **TinyCraft** | Four raised beds of about 7×7 inside quartz-like rims, each with a lantern in the middle, around a potted tree, with hedges, paths, arches and composters. The lantern in each bed's centre is exactly where a ring farm's block would sit. |

## What this means for farm sizing

Three of these builds (Foxel, the raised-bed garden, TinyCraft) are gardens of small, decorated
plots. A farm that clears a fixed square and lays its own pattern can't live in a garden like that.
Ideas raised on 2026-10-20, for when the farm framework is revisited:

- **Tend mode: replant only what was harvested.** A per-farm option where the farm never clears,
  lays soil, tills new ground or changes the layout. It only harvests mature plants in its area and
  replants the same plant in the same spot from the drops. Fences, rims, lanterns and paths are
  never touched because the farm only ever touches plants.
- **Point the farm at its area.** Let the player set the work area: in front of the block, or a
  rectangle marked out, perhaps with the quarry's markers. The tier would limit the number of plots,
  not the shape.
- **Keep the current mode** for players who want an industrial field and don't mind it being cleared.

## Glow Berry Farm layout

What the field reference suggests for the ring the farm builds, when that farm is designed:

- **The farm builds the supports.** Each pergola is two posts and a beam, and the vines hang from
  the beam. This is the positional-ownership model the plan already uses for supports (§3.5): a
  post or beam at a layout position is the farm's own.
- **Rows, not a ceiling.** One pergola row per lane, with a walkable lane between rows. The berries
  can then be seen and reached from the paths, where a full ceiling would bury them.
- **Beam height.** The beam sits at Y+5 over the soil, the same ceiling height the plan already uses
  for cave vines (a maximum of 3 vine blocks).
- **The body matches.** The pergola body in `../farm-bodies/README.md` ("Layout ideas for later")
  is the same structure at block scale. The block and the field it builds would look related.

# Free map asset sources and map catalog

## Included in this commit

`assets/maps/map-catalog.json` contains 100 original, deterministic map-layout recipes:
- 60 RTS layouts
- 40 MOBA layouts

These are **layout specifications**, not 100 imported finished maps. The runtime still needs a map loader/generator that reads the recipes and creates terrain, paths, resource nodes, neutral camps, lanes, towers, spawn points, and objectives. Until that integration and gameplay testing are complete, do not advertise the 100 maps as playable.

## Free asset sources reviewed

All selected sources below identify the relevant pack as CC0. Re-check the source page and package contents before bundling, and preserve any optional credit notes.

| Pack | Intended use | License / source |
|---|---|---|
| Kenney Medieval RTS (120+ files) | RTS terrain, structures, units, environmental sprites | CC0 — https://kenney.nl/assets/medieval-rts |
| Kenney Map Pack (180 assets) | Terrain, roads, overworld/map tiles | CC0 — https://opengameart.org/content/map-pack-180-assets |
| Kenney Tiny Battle | Compact top-down strategy terrain, roads, rivers, bridges, unit markers | CC0 — https://kenney.nl/assets/tiny-battle |
| Kenney Hexagon Kit | Optional low-poly 3D terrain/building prototypes; verify conversion/import first | CC0 — https://kenney.nl/assets/hexagon-kit |
| Kenney Isometric Landscape | Isometric terrain, roads, water and landscape sprites | CC0 — https://kenney.nl/assets/isometric-landscape |
| Kenney Cartography Pack | Map-selection artwork and fantasy map decoration (not runtime terrain) | CC0 — https://opengameart.org/content/cartography-pack |

## Compatibility and copyright safeguards

- Do not bundle Blizzard/WARCRAFT III retail maps, campaign files, models, textures, sounds, or DotA assets.
- Prefer original procedural layouts plus CC0 art.
- Convert/import only the art formats the current LibGDX renderer supports; 3D FBX/GLB/OBJ packs are not automatically compatible with this engine.
- Keep textures small and use atlases where practical for low-memory ARMv7 devices.
- Add an asset credits/license file for every pack actually downloaded and shipped.

## Next engineering step

Implement a Java map-catalog loader and deterministic layout generator in the offline RTS/MOBA demo runtime, then test all 100 seeds for valid spawn points, connected paths, reachable objectives, and memory use. The current catalog file alone does not add playable map support to the Warsmash engine.

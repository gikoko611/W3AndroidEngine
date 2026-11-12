# G.I KO KO Dual Demo Plan

## Goal

Keep the existing Warsmash engine intact and add two independent, offline-playable original demo modes to the Android app:

1. **Fantasy RTS Demo** — original low-poly kingdom, resource gathering, base construction, unit training, and a small skirmish scenario.
2. **Fantasy MOBA Demo** — original hero roster, lane creeps, towers, abilities, and a small 5v5-style offline match (player plus allied/enemy bots).

The existing **Warsmash / Warcraft III data launcher** remains a separate mode and continues to require game data supplied by the user. No Blizzard or DotA copyrighted game data is bundled or downloaded.

## Device target

- Redmi 9A / MediaTek Helio G25
- Android 10
- `armeabi-v7a` only
- Offline after installation
- Low-memory rendering, conservative texture sizes, limited particles, and capped unit counts
- Prefer OpenGL ES-compatible assets and avoid ARM64-only native dependencies

## Architecture

- Preserve the current Warsmash engine and launcher.
- Add a separate Demo Hub entry point rather than modifying Warcraft III simulation rules.
- Implement RTS and MOBA as independent modules/scenes with shared input, audio, settings, and asset-loading utilities where practical.
- Use original or permissively licensed assets only. Record asset source, author, and licence in `docs/ASSET_CREDITS.md`.
- Bundle only optimized demo assets. Keep the retail Warcraft III data out of the APK.
- Keep demo content small enough for the target phone; measure final APK size and memory use in CI.

## Delivery phases

### Phase 0 — Baseline
- Confirm existing ARMv7 debug build still passes.
- Preserve the existing application ID and Warsmash entry path.
- Add a regression check that the APK contains `lib/armeabi-v7a/` and no other ABI.

### Phase 1 — Demo Hub
- Add a clear start screen with three choices: Warsmash Engine, Fantasy RTS Demo, Fantasy MOBA Demo.
- Add an offline indicator and device-friendly graphics settings.
- Do not imply the two demos are original Warcraft III or DotA content.

### Phase 2 — Fantasy RTS Demo
- Build one small playable map with a base, resource nodes, workers, basic units, simple build/train commands, and one enemy AI.
- Add touch selection and command controls.
- Optimize for low unit counts and low-resolution textures.

### Phase 3 — Fantasy MOBA Demo
- Build one compact three-lane map, towers, lane creeps, original heroes, basic abilities, and bot-controlled teams.
- Start with a constrained single-player match; expand only after stable performance.
- Implement touch movement, target selection, and skill buttons.

### Phase 4 — QA and release
- Run Gradle unit tests and ARMv7 APK build in GitHub Actions.
- Inspect APK ABI contents and size.
- Fix all reproducible CI failures before publishing.
- Publish a versioned GitHub Release with APK and checksums only after CI passes.
- Redmi 9A install, launch, gameplay, and memory checks remain required for a verified device-compatibility claim.

## Acceptance criteria

- Existing Warsmash build path remains available.
- Both demo modes launch without external game data and work offline.
- No copyrighted Warcraft III or DotA assets are redistributed.
- APK contains `armeabi-v7a` native libraries only.
- CI build and tests pass.
- Release notes clearly distinguish CI build success from physical Redmi 9A runtime verification.

## Current status

Planning only. The existing v0.3.6 APK is the Warsmash engine/launcher and does not yet contain either new playable demo. No dual-demo APK should be described as released until the phases above are implemented and verified.

# Offline RTS + MOBA Demo Plan

## Goal

Add two original, offline-playable demo modes to the Android app while preserving the existing Warsmash engine and its user-supplied Warcraft III data workflow.

Target device: Redmi 9A (Android 10, ARMv7 / `armeabi-v7a`, Helio G25, 3 GB RAM class).

## Important scope boundary

The existing Warsmash engine is a Warcraft III engine/launcher. Its README explicitly says that the repository and APK do not include Warcraft III game content and that users supply data from their own legally owned copy. This work must not bundle Blizzard models, textures, audio, maps, campaign data, or other proprietary content.

The two new modes will be original demos with newly authored gameplay and assets under compatible licenses. They are Warcraft-III-inspired RTS/MOBA mechanics, not copies of Warcraft III or DotA intellectual property.

## User-facing menu

1. **Original Warsmash** — keep the current launcher and own-game-data flow unchanged.
2. **RTS Demo** — original offline base-building and skirmish scenario.
3. **MOBA Demo** — original offline lane battle with a hero, allied/enemy creeps, towers, and a core objective.

## Implementation phases

### Phase 0 — Baseline and build safety
- Preserve the current ARMv7-only ABI configuration.
- Preserve the existing Warsmash launch/data flow.
- Build the current debug APK in GitHub Actions before feature integration.
- Record the APK architecture verification result.

### Phase 1 — Demo shell and navigation
- Add a lightweight Android-native demo hub with three entries: Warsmash, RTS Demo, MOBA Demo.
- Keep the demo hub separate from the Warsmash data-required launcher.
- Provide clear offline status and help text.

### Phase 2 — RTS Demo vertical slice
- Original small map, camera pan/zoom, touch selection.
- Worker unit, resource nodes, one base, one barracks, one defensive structure.
- Basic gather/build/train/attack loop and a simple AI opponent.
- Local save/settings only if stable on low-memory devices.

### Phase 3 — MOBA Demo vertical slice
- One original hero with movement and three abilities.
- Three-lane-inspired compact map or one-lane first release to control memory and scope.
- Allied/enemy creep waves, towers, enemy core, simple bot behavior.
- Offline match with clear win/lose state.

### Phase 4 — Free asset pipeline
- Use only original low-poly assets or assets with explicit redistribution rights compatible with the app.
- Keep asset attribution and license files in the repository.
- Prefer small textures, compressed audio, and shared materials.
- Do not download proprietary game data from unofficial mirrors.

### Phase 5 — Redmi 9A optimization and QA
- ARMv7 APK verification.
- Target 480p/low graphics preset, capped frame rate, low texture sizes.
- Test cold start, menu navigation, both demo modes, pause/resume, and memory pressure.
- Fix all reproducible build errors and test failures before release.

### Phase 6 — Release
- Produce a signed release APK only when a release signing key is configured securely.
- Until then, publish an installable debug APK as a GitHub Actions artifact and label it clearly as a test build.
- Publish release notes, SHA-256 checksum, and known limitations.

## Acceptance criteria

- Existing Warsmash launcher remains available and its game-data requirement remains explicit.
- RTS Demo and MOBA Demo launch without Warcraft III data and work offline.
- APK contains `lib/armeabi-v7a/` native libraries and no unintended 64-bit-only dependency.
- No proprietary Warcraft III or DotA assets are bundled.
- Build and automated checks pass; device runtime testing is reported separately and is not claimed unless performed on the Redmi 9A.

## Current status

Planning only. The existing v0.3.6 APK is the Warsmash engine/launcher build and does not yet contain the two demo modes. The phases above must be implemented and tested before claiming the combined APK is complete.

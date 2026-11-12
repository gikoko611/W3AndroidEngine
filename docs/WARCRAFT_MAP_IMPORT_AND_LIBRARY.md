# Warcraft III map import and community library

## Scope

The Android app will support two separate sources of maps:

1. **My Maps** — files selected by the player from their own storage, normally `.w3x` or `.w3m`.
2. **Community Library** — a searchable catalog of community map pages. The app opens the original source page and lets the player download a map there; it does not mirror or bundle map files by default.

## Import validation

`MapFileValidator` performs a small, read-only MPQ signature check for a selected `.w3x` or `.w3m` file. It recognizes the common MPQ header at offset 0 and the common user-data-header layout with the MPQ header at offset 512.

This is only a container check. It does not prove that the map is safe, that its scripts are benign, or that its Warcraft III version is supported by the current Warsmash build. The UI must show these as separate checks and must not report a successful game launch based on the signature alone.

## Community source policy

Potential discovery sources:
- WC3Map: https://wc3map.com/
- Classic Battle.net map resources: https://classic.battle.net/mod/help.shtml

The library should store metadata (map title, author, source page, version, file size when known, and license/permission notes), not copy the map file into this repository. A map may be downloaded for the user's own use from its original source when that source permits it.

A free-to-download map is not automatically licensed for redistribution inside this APK. Before any map is mirrored or bundled, record explicit permission or a license that allows redistribution, and preserve required attribution. Do not include Blizzard's official Maps of the Week, retail campaign data, extracted game assets, or files without redistribution permission.

Blizzard's Legal FAQ says creators may distribute their own Warcraft maps for personal, non-commercial purposes, and says its Maps of the Week are exclusive to classic.battle.net. This is not blanket permission for a third party to repackage arbitrary community maps. See:
- https://www.blizzard.com/en-sg/legal/c1ae32ac-7ff9-4ac3-a03b-fc04b8697010/blizzard-legal-faq

## Next integration tasks

- Add an Android Storage Access Framework picker for `.w3x` and `.w3m`.
- Copy selected files into an app-managed Maps directory using streaming I/O and a free-space check.
- Show file name, size, MPQ signature status, and a separate runtime compatibility result.
- Connect the selected file to the existing engine launch contract's `mapfile` field only after confirming the runtime accepts that path and can resolve the map's required game data.
- Add a community catalog UI that opens original source pages and records user-downloaded files; do not scrape or mirror files without permission.
- Test map versions and large files on ARMv7 / Android 10, including cancellation, low storage, and malformed archives.

## Current implementation status

The first foundation step is present: `MapFileValidator` checks the extension, readability, minimum size, and MPQ signature. The Android picker, file manager UI, community catalog UI, and end-to-end map launch wiring are not implemented yet.

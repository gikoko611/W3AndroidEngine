<div align="center">

<img src="docs/images/icon.png" alt="Warsmash Android icon" width="128">

# Warsmash Android

A hobby Android port of the [Warsmash](https://github.com/Retera/WarsmashModEngine) engine for W3.

[![License: AGPL v3](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android%20%7C%20Desktop-3DDC84?logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![libGDX](https://img.shields.io/badge/libGDX-1.12.1-E74C3C)
![Status](https://img.shields.io/badge/status-early%20development-yellow)

</div>

## About

This fork continues Retera's work on Warsmash and adapts it for Android. The aim is to play W3 on phones and tablets with classic graphics at a steady 60 FPS.

> [!IMPORTANT]
> No game files are included or distributed. The engine reads them from your own legally obtained copy of W3.

Supported game versions: **1.31 and newer, including Reforged**, played with Classic graphics. The engine reads the CASC install, or game files extracted from it (for example with the prep tool). Tested with 1.31.1, 1.32.10 (extracted files) and 3.0.1. Older releases that ship MPQ archives (1.29 and earlier) are not supported.

## Status

Current version: **v0.1.085**

- Custom Games work end-to-end; computer players take the computer slots of skirmish maps
- Reign of Chaos and Frozen Throne campaign missions start and run with their own data, with heroes carried between missions
- Cinematics use the original volume levels for dialogue and music
- Full touch screen control support + S-pen support
- Save and load, with load from the main menu; saves stay loadable after app updates
- Game menu and Options (sound, gameplay and video), kept between runs
- Allies window, player chat and the message log
- Weather effects, ubersplats and terrain deformations

Known issues in this version:

- A save cannot be loaded when a script function it was paused in has changed
- Computer players do not buy zeppelins or items yet
- Some unit movement and responses to nearby fights still differ from the original
- Reign of Chaos balance values and interface skin still come from the Frozen Throne data

## Screenshots

![A melee game: Town Hall, Gold Mine and Peasants](docs/images/melee.jpg)

## Running on desktop

Requires JDK 17.

1. Copy `core/assets/warsmash.ini` to `core/assets/warsmash.local.ini`.
2. Point `[DataSources]` at your game install:

   ```ini
   [DataSources]
   Count=3
   Type00=CASC
   Path00="C:\Path\To\Game"
   Prefixes00=war3.w3mod,war3.w3mod\_deprecated.w3mod,war3.w3mod\_locales\enus.w3mod
   Type01=Folder
   Path01="..\..\resources"
   Type02=Folder
   Path02="."
   ```

3. Run:

   ```bash
   ./gradlew desktop:runGame -Pargs="-window -ini warsmash.local.ini"
   ```

## Running on Android

Requires Android 8.0 or newer and your own copy of the game data on the device.

1. Build and install the app:

   ```bash
   ./gradlew android:installDebug
   ```

2. Copy the game data to the device: an installed game folder (CASC) or extracted files. The [W3-Android-prep-tool](https://github.com/nikoltu/W3-Android-prep-tool) prepares the data from your own copy for this engine. Files copied into the app's own folder (`Android/data/com.etheller.warsmash.android/files/data`) are found by themselves.
3. On first start, choose that folder in the **Game data folder** screen with **Choose folder** and tap **Continue**. A folder outside the app's own folder needs the **All files access** permission, which the screen asks for.
4. Later starts go straight to the game. To pick another folder, hold the app icon and choose **Game data**.

## Credits

- [Retera](https://github.com/Retera) for Warsmash and everyone who contributed to it
- [libGDX](https://libgdx.com/)

## License

[GNU AGPL v3.0](LICENSE), same as upstream.

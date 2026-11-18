# Quietly – Forge 1.20.1 port (UNTESTED, uncompiled)

Port of "Quietly" by Kafei (NeoForge 1.21.1–1.21.10, CC0-1.0) to Forge 1.20.1 (47.x), Java 17.

## Build
GitHub: push this repo and the **Actions** tab builds the jar (artifact `quietly-forge-1.20.1`).

Locally:
1. Download the official Forge 1.20.1 MDK and copy `gradlew`, `gradlew.bat` and the `gradle/` folder into this directory
   (or copy this project's `src/` + `build.gradle` over the MDK).
2. `./gradlew build`  ->  `build/libs/quietly-1.0.0+forge1.20.1.jar`
3. Put the jar in the server's `mods/` folder (install on clients too to see the square).

## Behaviour
Sneak + right-click a chest / trapped chest / barrel / ender chest / shulker box with empty hands,
keep sneaking while the square progress indicator (replaces the crosshair, no text) fills, and it opens silently.
Timing: `BASE_OPEN_TICKS = 24` + `TICKS_PER_ITEM_KIND = 8` in `SilentOpenManager` (2x the original's 12 + 4).
The square needs the mod on the **client** as well as the server; it is drawn by a server->client packet.
To also trigger while holding items, set `REQUIRE_EMPTY_HANDS = false` in `QuietlyCommon`.

## Differences from the original
- Rewritten from the compiled classes, not copied line-for-line.
- Only two mixins (ChestMenu, ShulkerBoxMenu): the vanilla Container#startOpen/stopOpen calls are skipped for
  silent menus, so no sound, no sculk vibration and no trapped-chest redstone signal. The original's extra
  sound / game-event muting mixins aren't needed for that approach.
- Uses Forge's RightClickBlock event only (no ServerPlayerGameMode mixin) and no MixinExtras.
- Vanilla open stats and piglin anger are not triggered.

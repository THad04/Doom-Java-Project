# Phantom Maze

First-person Pac-Man with Doom-style enemies, HUD, and weapons. Java 17 Swing project.

## Run
Open the folder in IntelliJ IDEA as a Maven project and run `edu.scu.phantommaze.App`.

## Controls
- `W` `A` `S` `D`: move
- Mouse or left/right arrows: turn
- Left click or `Space`: shoot
- `R`: reload pistol
- `M`: minimap
- `Esc`: pause; click to resume or press `Q` for the main menu

While playing, the mouse pointer is hidden and held in the middle of the window. On macOS this uses CoreGraphics through the JNA library, so no Accessibility permission is needed; IntelliJ downloads JNA automatically from `pom.xml`.

## Gameplay
- Four enemies: Cacodemon (direct chaser), Demon (ambusher), Lost Soul (flanker), and Imp (shy chaser).
- You can take three hits: health goes 100% → 67% → 33% → 0%.
- The pistol holds 3 bullets, reloads without running out, and needs 3 hits to send an enemy back to its spawn.
- Power-ups give a shotgun with 50 shells, armor, and a 50% speed boost. Armor counts down the power-up time and makes enemy contact harmless; enemies flee and one shotgun shell eliminates one.
- The minimap shows enemies only while a power-up is active.
- Sixth sense: when a dangerous enemy is within 5 tiles' walk but you can't see it, the screen edge pulses red on the side it will come from (top = ahead, bottom = behind you), stronger as it gets closer. For an enemy hidden around a corner, the glow points at that corner, not through the wall.

## Assets
Sprites, HUD graphics, font, and sounds are from [Freedoom](https://freedoom.github.io/). Their license and credits are in `src/main/resources/freedoom/COPYING.adoc` and `src/main/resources/freedoom/CREDITS`.

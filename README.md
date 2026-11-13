# Phantom Maze

First-person Pac-Man with Doom-style enemies, HUD, and weapons. Java 17 Swing project.

## Team
- Teagan Haddawy
- Julian Jackson

## What makes it a fusion game
- **Raycaster from scratch:** the first-person 3D view is built with a hand-written raycaster (walls, floor, ceiling, sprites, fog) drawn with plain Java 2D, with no game engine or 3D library.
- **Pac-Man ghost AI on Doom enemies:** the four ghosts' targeting rules, scatter/chase cycles, frightened mode and "Cruise Elroy" speed-up are mapped onto the Cacodemon, Demon, Lost Soul and Imp.
- **Guns replace the power pellet:** a power-up gives a shotgun and armor, so frightened enemies are hunted instead of eaten.
- **Combo and rampage:** killing all four enemies in one streak unlocks the BFG rampage, and the sixth-sense warning pulses the screen edge when an unseen enemy is close.

## Build, run and test
```
mvn compile exec:java   # build and launch the game
mvn test                # run the unit tests
```
You can also open the folder in IntelliJ IDEA as a Maven project and run `edu.scu.phantommaze.App`.

## Architecture (MVC)
| Package | Role | Main classes |
|---|---|---|
| `controller` | Input and screen flow | `ScreenController`, `GameLoop`, `InputController`, `CursorWarp` |
| `model` | Game rules and state, no Swing | `Game`, `Player`, `Enemy`, `EnemyAI`, `Pathfinder`, `GameMap`, `Weapons`, `Combo` |
| `view` | Swing windows and rendering | `GameFrame`, `MenuPanel`, `GamePanel`, `WorldRenderer`, `SpriteRenderer`, `HudRenderer`, `MinimapRenderer`, `ThreatIndicator` |
| `engine` | Raycasting maths | `Raycaster`, `RayHit`, `TextureFactory` |
| `audio` | Sound effects | `SoundPlayer` |

## UML diagrams
- [Class diagram](docs/uml/class-diagram.png)
- [Sequence diagram](docs/uml/sequence-diagram.png) (one game frame)
- [State diagram](docs/uml/state-diagram.png) (game screens and enemy states)

## Tests
JUnit tests in `src/test/java/edu/scu/phantommaze/model/`:
- `GameCombatTest`: shooting and line of sight, pistol hits, power-up and armor, getting caught, sixth sense, combo and BFG rampage.
- `DotsAndScoreTest`: dots, scoring, level completion, lives, and the Cacodemon speed-up.
- `EnemyNavigationTest`: pen exit, reversing direction, and `Pathfinder` routing.
- `WeaponsTest`: pistol clip and reload, shotgun ammo, and switching guns.
- `ModelTestMaps`: small test maps shared by the tests.

## Controls
- `W` `A` `S` `D`: move
- Mouse or left/right arrows: turn
- Left click or `Space`: shoot
- `R`: reload pistol
- `M`: minimap
- `Esc`: pause; click to resume or press `Q` for the main menu

While playing, the mouse pointer is hidden and held in the middle of the window. On macOS this uses CoreGraphics through the JNA library, so no Accessibility permission is needed; IntelliJ downloads JNA automatically from `pom.xml`.

## Gameplay
- Goal: eat every glowing dot (and the 4 power-ups) to complete the level. Eaten dots stay eaten when you lose a life. The status bar shows the dots left; the win screen shows score, time, and kills.
- Score: dot 10, power-up 50, frightened enemy 200 → 400 → 800 → 1600 (doubling within one power-up), pistol kill 100, BFG kill 200, and 1000 per life left when the level is complete.
- With 20 dots left the Cacodemon gets 10% faster and stops taking breaks; with 10 left it gets another 10% faster (Pac-Man's "Cruise Elroy").
- Four enemies: Cacodemon (direct chaser), Demon (ambusher), Lost Soul (flanker), and Imp (shy chaser).
- You can take three hits: health goes 100% → 67% → 33% → 0%.
- The pistol holds 3 bullets, reloads without running out, and needs 3 hits to send an enemy back to its spawn.
- Power-ups give a shotgun with 50 shells, armor, and a 50% speed boost. Armor counts down the power-up time and makes enemy contact harmless; enemies flee and one shotgun shell eliminates one.
- Combo: during a power-up, kill all four enemies with no more than 4 seconds between kills (each enemy counts once). A `COMBO n/4` message and a shrinking bar show the streak.
- Rampage: a full combo burns the shotgun away in a wall of fire and hands you the BFG. The power-up gets 10 extra seconds with armor refilled to 100%, enemies waiting in the pen come straight back out, and each BFG shot charges briefly, then kills every enemy in a 90° cone in front of you that isn't behind a wall. Picking up another power-up during rampage refills its time.
- The minimap shows enemies only while a power-up is active.
- Sixth sense: when a dangerous enemy is within 5 tiles' walk but you can't see it, the screen edge pulses red on the side it will come from (top = ahead, bottom = behind you), stronger as it gets closer. For an enemy hidden around a corner, the glow points at that corner, not through the wall.

## Assets
Sprites, HUD graphics, font, and sounds are from [Freedoom](https://freedoom.github.io/). Their license and credits are in `src/main/resources/freedoom/COPYING.adoc` and `src/main/resources/freedoom/CREDITS`.

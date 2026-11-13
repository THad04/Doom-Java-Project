package edu.scu.phantommaze.model;

import edu.scu.phantommaze.Config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Root of the game model. Owns the map, the player, the enemies and the rules
 * that connect
 * them: shooting, power-ups, getting caught, and running out of health.
 */
public class Game {

    /** Angle the player faces when (re)spawning: 0 = east. */
    private static final double START_ANGLE = 0.0;
    /** Seconds before the first enemy leaves the pen at the start of a round. */
    private static final double FIRST_RELEASE_DELAY = 1.0;
    /** Seconds a pickup message stays on screen. */
    private static final double MESSAGE_TIME = 3.0;
    /** Seconds the screen flashes gold after picking up a power-up. */
    private static final double PICKUP_FLASH_TIME = 0.3;
    /** Dots are eaten every fraction of a second, so their sound is kept quiet. */
    private static final double DOT_VOLUME = 0.35;

    private final GameMap map; // the level
    private final Player player; // the user
    private final Difficulty difficulty;
    private final Weapons weapons = new Weapons();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<int[]> powerUps = new ArrayList<>(); // {x, y} of power pellets not yet taken
    private final List<Enemy> enemyView = Collections.unmodifiableList(enemies);
    private final List<int[]> powerUpView = Collections.unmodifiableList(powerUps);
    private final EnemyAI ai;
    private final Random random = new Random();
    private SoundListener sounds = (sound, volume, pan) -> {
    }; // silent until the audio is connected
    private GameState state = GameState.PLAYING; // what mode the game is in

    private int lives = Config.PLAYER_LIVES;
    private int kills;
    private int score;
    private int dotsLeft; // dots and power-ups still on the map; 0 = level complete
    private int frightenedKills; // frightened enemies killed during this power-up (doubles their points)
    private double time; // seconds of play so far
    private double hitFreeze; // > 0 while the world is frozen after the player was caught
    private double graceTimer; // > 0 while the player just respawned and cannot be hurt
    private double powerUpTimer; // > 0 while the shotgun power-up (armor) lasts
    private double powerUpTotal; // full length of the current power-up, for the armor percentage
    private final Combo combo = new Combo();
    private boolean rampage; // the BFG rampage earned by a full combo; lasts until the power-up ends
    private double rampageTransition; // > 0 while flames swap the shotgun for the BFG
    private double bfgCharge; // > 0 while the BFG charges; it blasts when this reaches 0
    private final List<Blast> blasts = new ArrayList<>();
    private final List<Blast> blastView = Collections.unmodifiableList(blasts);
    private boolean scatterMode = true; // Pac-Man's breaks: enemies roam to their corners
    private double modeTimer;
    private boolean triggerHeld;
    private String message;
    private double messageTimer;
    private double pickupFlash;

    /**
     * Creates a game on a map.
     *
     * @param map        the level map; it must contain a ghost pen ({@code G}) with
     *                   a door ({@code -})
     * @param difficulty enemy speed, power-up length and other tuning
     * @throws IllegalArgumentException if the map has no pen or no pen door
     */
    public Game(GameMap map, Difficulty difficulty) {
        this.map = map;
        this.difficulty = difficulty;
        // asks the map where P is, then spawns player facing east
        this.player = new Player(map.getPlayerStart(), START_ANGLE);

        List<int[]> pen = map.getPenTiles();
        int[] penExit = map.findPenExit();
        if (pen.isEmpty() || penExit == null) {
            throw new IllegalArgumentException("Map needs a ghost pen (G tiles) with a door (-)");
        }
        // Spread the four enemies over the pen tiles.
        EnemyType[] types = EnemyType.values();
        for (int i = 0; i < types.length; i++) {
            int[] tile = pen.get(i * pen.size() / types.length);
            enemies.add(new Enemy(types[i], tile[0], tile[1]));
        }
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (map.getCell(x, y) == GameMap.POWER_UP) {
                    powerUps.add(new int[] { x, y });
                }
            }
        }
        this.ai = new EnemyAI(map, player, enemies, penExit);
        this.dotsLeft = map.getPelletCount();
        startRound();
    }

    /**
     * Connects the game to something that can play sounds.
     *
     * @param sounds receives every sound the game wants played
     */
    public void setSoundListener(SoundListener sounds) {
        this.sounds = sounds;
    }

    /**
     * Advances the simulation.
     *
     * @param dt seconds elapsed
     */
    public void update(double dt) {
        if (state != GameState.PLAYING) {
            return;
        }
        time += dt;
        messageTimer = Math.max(0, messageTimer - dt);
        pickupFlash = Math.max(0, pickupFlash - dt);

        if (hitFreeze > 0) { // the world is frozen while the player recovers from a hit
            hitFreeze -= dt;
            if (hitFreeze <= 0) {
                if (lives == 0) {
                    state = GameState.GAME_OVER;
                } else {
                    graceTimer = Config.RESPAWN_GRACE_TIME;
                    startRound();
                }
            }
            return;
        }

        graceTimer = Math.max(0, graceTimer - dt);
        double speedMultiplier = powerUpTimer > 0 ? Config.POWER_UP_SPEED_MULTIPLIER : 1.0;
        player.update(dt, map, speedMultiplier); // passes time step to player
        weapons.update(dt);
        updatePowerUp(dt);
        pickUpPowerUp();
        eatDot();
        if (state != GameState.PLAYING) {
            return; // that was the last dot
        }
        updateRampage(dt);
        if (triggerHeld) {
            fire();
        }
        updateScatterChase(dt);

        ai.updatePlayer();
        for (int i = 0; i < enemies.size(); i++) {
            updateEnemy(enemies.get(i), dt);
        }
        checkCaught();
        updateGrowls(dt);
    }

    /**
     * Puts the player and enemies at their starting spots, enemies leaving the pen
     * one by one.
     */
    private void startRound() {
        player.respawn(map.getPlayerStart(), START_ANGLE);
        weapons.reset();
        powerUpTimer = 0;
        endRampage();
        triggerHeld = false;
        scatterMode = true;
        modeTimer = difficulty.scatterTime;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            e.sendToPen(FIRST_RELEASE_DELAY + i * difficulty.releaseInterval);
            e.readyToGrowl(0, randomGrowlInterval()); // stagger first growls
        }
    }

    private void updateEnemy(Enemy e, double dt) {
        e.tick(dt);
        switch (e.getState()) {
            case IN_PEN:
                if (e.isReadyToLeave()) {
                    e.startLeaving();
                }
                break;
            case DYING:
                if (e.isDeathFinished()) {
                    e.sendToPen(rampage ? 0 : difficulty.respawnDelay); // rampage needs targets
                    e.setFrightened(isPowerUpActive());
                }
                break;
            default: // LEAVING_PEN or ACTIVE
                if (!e.isInPain()) {
                    boolean fleeing = e.getState() == Enemy.State.ACTIVE && e.isFrightened();
                    double speed = fleeing ? difficulty.fleeSpeed : difficulty.enemySpeed;
                    boolean elroy = !fleeing && e.getType() == EnemyType.CACODEMON && elroyLevel() > 0;
                    if (elroy) {
                        speed *= 1.0 + Config.ELROY_SPEED_STEP * elroyLevel();
                    }
                    ai.move(e, speed * dt, scatterMode && !elroy); // Elroy keeps chasing during breaks
                }
                break;
        }
    }

    /** Fires the held gun if it is ready, and works out what the shot hit. */
    private void fire() {
        if (!weapons.tryFire()) {
            if (weapons.getCurrent() == Weapon.PISTOL && weapons.getAmmo() == 0) {
                requestReload();
            }
            return;
        }
        Weapon weapon = weapons.getCurrent();
        if (weapon == Weapon.BFG) {
            sounds.play(Sound.BFG, 1.0, 0.0);
            bfgCharge = Config.BFG_CHARGE_TIME; // the blast goes off in updateRampage
            return;
        }
        boolean shotgun = weapon == Weapon.SHOTGUN;
        sounds.play(shotgun ? Sound.SHOTGUN : Sound.PISTOL, 1.0, 0.0);

        Enemy target = findShotTarget(shotgun ? Config.SHOTGUN_HIT_RADIUS : Config.PISTOL_HIT_RADIUS);
        if (target != null) {
            if (shotgun || target.hurt()) { // shotgun kills at once; pistol needs 3 hits
                eliminate(target);
            } else {
                sounds.play(target.getType().painSound, volumeFor(target), panFor(target));
            }
        }

        // Check the gun again: the kill above may have completed a combo and handed
        // over the BFG.
        if (weapons.getCurrent() == Weapon.SHOTGUN && weapons.getAmmo() == 0) {
            returnToPistol(); // out of shells: back to the pistol, armor stays
        } else if (weapons.getCurrent() == Weapon.PISTOL && weapons.getAmmo() == 0) {
            requestReload(); // empty clip reloads on its own
        }
    }

    /**
     * The BFG's blast: like Doom's BFG spray, it kills every enemy in a wide cone
     * in front of
     * the player that is not hidden behind a wall.
     */
    private void bfgBlast() {
        double halfCone = Math.toRadians(Config.BFG_CONE_DEGREES / 2.0);
        boolean hit = false;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.isShootable()
                    || Math.abs(relativeAngle(e)) > halfCone
                    || Math.hypot(e.getX() - player.getX(), e.getY() - player.getY()) > Config.HITSCAN_RANGE
                    || !hasClearPath(player.getX(), player.getY(), e.getX(), e.getY())) {
                continue;
            }
            blasts.add(new Blast(e.getX(), e.getY()));
            eliminate(e);
            hit = true;
        }
        if (hit) {
            sounds.play(Sound.BFG_EXPLOSION, 1.0, 0.0);
        }
    }

    /**
     * Hits the closest enemy near the aim line, provided both that line and the
     * line to
     * the enemy's center are clear. Aim assistance must not reach around a wall.
     */
    private Enemy findShotTarget(double hitRadius) {
        double px = player.getX();
        double py = player.getY();
        double dx = player.getDirX();
        double dy = player.getDirY();
        Enemy best = null;
        double bestDistance = Config.HITSCAN_RANGE;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.isShootable()) {
                continue;
            }
            double ex = e.getX() - px;
            double ey = e.getY() - py;
            double along = ex * dx + ey * dy;
            double sideways = Math.abs(ex * dy - ey * dx);
            if (along > 0 && along < bestDistance && sideways <= hitRadius
                    && hasClearPath(px, py, px + dx * along, py + dy * along)
                    && hasClearPath(px, py, e.getX(), e.getY())) {
                best = e;
                bestDistance = along;
            }
        }
        return best;
    }

    /**
     * Traverses every tile touched by a segment, including both sides of grid
     * corners.
     */
    private boolean hasClearPath(double fromX, double fromY, double toX, double toY) {
        int tileX = (int) Math.floor(fromX);
        int tileY = (int) Math.floor(fromY);
        double dx = toX - fromX;
        double dy = toY - fromY;
        boolean onVerticalEdge = dx == 0 && fromX == tileX;
        boolean onHorizontalEdge = dy == 0 && fromY == tileY;
        if (map.isWall(tileX, tileY)
                || (onVerticalEdge && map.isWall(tileX - 1, tileY))
                || (onHorizontalEdge && map.isWall(tileX, tileY - 1))) {
            return false;
        }
        int stepX = dx < 0 ? -1 : 1;
        int stepY = dy < 0 ? -1 : 1;
        double deltaX = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
        double deltaY = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
        double sideX = dx == 0 ? Double.POSITIVE_INFINITY
                : (dx < 0 ? fromX - tileX : tileX + 1.0 - fromX) * deltaX;
        double sideY = dy == 0 ? Double.POSITIVE_INFINITY
                : (dy < 0 ? fromY - tileY : tileY + 1.0 - fromY) * deltaY;
        while (Math.min(sideX, sideY) <= 1.0) {
            if (Math.abs(sideX - sideY) <= 1e-10) {
                if (map.isWall(tileX + stepX, tileY) || map.isWall(tileX, tileY + stepY)) {
                    return false;
                }
                tileX += stepX;
                tileY += stepY;
                sideX += deltaX;
                sideY += deltaY;
            } else if (sideX < sideY) {
                tileX += stepX;
                sideX += deltaX;
            } else {
                tileY += stepY;
                sideY += deltaY;
            }
            if (map.isWall(tileX, tileY)
                    || (onVerticalEdge && map.isWall(tileX - 1, tileY))
                    || (onHorizontalEdge && map.isWall(tileX, tileY - 1))) {
                return false;
            }
        }
        return true;
    }

    private void eliminate(Enemy e) {
        sounds.play(e.getType().deathSound, volumeFor(e), panFor(e));
        score += killPoints(e); // before kill(), which clears the frightened flag
        e.kill();
        kills++;
        if (powerUpTimer > 0 && !rampage) {
            if (combo.recordKill(e, enemies.size())) {
                startRampage();
            } else {
                showMessage("COMBO " + combo.getCount() + "/" + enemies.size());
            }
        }
    }

    /**
     * Points for a kill: BFG kills during rampage are flat; frightened enemies
     * double in value
     * with each kill in the same power-up, like Pac-Man's ghosts; anything else is
     * a pistol kill.
     */
    private int killPoints(Enemy e) {
        if (rampage) {
            return Config.BFG_KILL_POINTS;
        }
        if (e.isFrightened()) {
            int points = Math.min(Config.MAX_FRIGHTENED_KILL_POINTS, Config.FRIGHTENED_KILL_POINTS << frightenedKills);
            frightenedKills++;
            return points;
        }
        return Config.PISTOL_KILL_POINTS;
    }

    /**
     * Every enemy was killed in one quick streak: the shotgun burns away for the
     * BFG, the power-up
     * gets bonus time with full armor, and enemies waiting in the pen come straight
     * back out
     * (still frightened) so there is something to shoot.
     */
    private void startRampage() {
        rampage = true;
        rampageTransition = Config.RAMPAGE_TRANSITION_TIME;
        powerUpTimer += Config.RAMPAGE_BONUS_TIME;
        powerUpTotal = powerUpTimer;
        weapons.equipBfg(Config.RAMPAGE_TRANSITION_TIME);
        sounds.play(Sound.RAMPAGE, 1.0, 0.0);
        showMessage("YOU GOT THE BFG!"); // the big RAMPAGE title is drawn by the HUD
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.getState() == Enemy.State.IN_PEN) {
                e.sendToPen(0);
                e.setFrightened(true);
            }
        }
    }

    /**
     * Runs the combo clock, the flame transition, the BFG charge, and the fading
     * explosions.
     */
    private void updateRampage(double dt) {
        combo.update(dt);
        rampageTransition = Math.max(0, rampageTransition - dt);
        if (bfgCharge > 0) {
            bfgCharge -= dt;
            if (bfgCharge <= 0) {
                bfgCharge = 0;
                bfgBlast();
            }
        }
        for (int i = blasts.size() - 1; i >= 0; i--) {
            Blast b = blasts.get(i);
            b.tick(dt);
            if (b.getAge() >= Config.BLAST_TIME) {
                blasts.remove(i);
            }
        }
    }

    /**
     * Clears the combo and rampage, e.g. when the power-up ends or a new round
     * starts.
     */
    private void endRampage() {
        rampage = false;
        rampageTransition = 0;
        bfgCharge = 0;
        combo.reset();
        blasts.clear();
    }

    /**
     * Picking up a power pellet gives the shotgun, a fresh box of shells, and full
     * armor.
     */
    private void pickUpPowerUp() {
        int tx = (int) Math.floor(player.getX());
        int ty = (int) Math.floor(player.getY());
        if (map.getCell(tx, ty) != GameMap.POWER_UP) {
            return;
        }
        map.setCell(tx, ty, GameMap.EMPTY);
        for (int i = 0; i < powerUps.size(); i++) {
            int[] tile = powerUps.get(i);
            if (tile[0] == tx && tile[1] == ty) {
                powerUps.remove(i);
                break;
            }
        }

        if (rampage) { // keep the BFG; just top the time back up
            powerUpTimer = Math.max(powerUpTimer, difficulty.powerUpDuration);
            showMessage("RAMPAGE REFUELED!");
        } else {
            powerUpTimer = difficulty.powerUpDuration;
            weapons.equipShotgun();
            combo.reset(); // every power-up gets its own chance at a combo
            frightenedKills = 0; // and its own 200-400-800-1600 chain
            showMessage("YOU GOT THE SHOTGUN!");
        }
        powerUpTotal = powerUpTimer;
        sounds.play(Sound.POWER_UP, 1.0, 0.0);
        pickupFlash = PICKUP_FLASH_TIME;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.getState() == Enemy.State.DYING) {
                continue;
            }
            if (e.getState() == Enemy.State.ACTIVE && !e.isFrightened()) {
                e.reverse(); // like Pac-Man: ghosts turn around the moment they get scared
            }
            e.setFrightened(true);
        }
        collect(Config.POWER_UP_POINTS);
    }

    /** Eats the dot on the player's tile, if there is one. */
    private void eatDot() {
        int tx = (int) Math.floor(player.getX());
        int ty = (int) Math.floor(player.getY());
        if (map.getCell(tx, ty) != GameMap.DOT) {
            return;
        }
        map.setCell(tx, ty, GameMap.EMPTY);
        sounds.play(Sound.DOT, DOT_VOLUME, 0.0);
        collect(Config.DOT_POINTS);
    }

    /** Scores a dot or power-up; eating the last one completes the level. */
    private void collect(int points) {
        score += points;
        dotsLeft--;
        if (dotsLeft == 0) {
            score += lives * Config.LIFE_BONUS_POINTS;
            state = GameState.LEVEL_COMPLETE;
            triggerHeld = false;
            sounds.play(Sound.POWER_UP, 1.0, 0.0);
        }
    }

    /**
     * Pac-Man's "Cruise Elroy" for the Cacodemon (Blinky).
     *
     * @return 0 normally, 1 with {@link Config#ELROY_DOTS} dots left, 2 with
     *         {@link Config#ELROY_2_DOTS} left
     */
    private int elroyLevel() {
        if (dotsLeft <= Config.ELROY_2_DOTS) {
            return 2;
        }
        return dotsLeft <= Config.ELROY_DOTS ? 1 : 0;
    }

    private void updatePowerUp(double dt) {
        if (powerUpTimer <= 0) {
            return;
        }
        powerUpTimer -= dt;
        if (powerUpTimer > 0) {
            return;
        }
        powerUpTimer = 0;
        if (weapons.getCurrent() != Weapon.PISTOL) {
            returnToPistol();
        }
        endRampage();
        frightenedKills = 0;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            e.setFrightened(false);
        }
    }

    private void returnToPistol() {
        weapons.equipPistol();
        sounds.play(Sound.WEAPON_UP, 1.0, 0.0);
        if (weapons.getAmmo() == 0) {
            requestReload();
        }
    }

    /**
     * Pac-Man's rhythm: enemies alternate between hunting and taking a break in
     * their corners.
     */
    private void updateScatterChase(double dt) {
        if (powerUpTimer > 0) {
            return; // the clock pauses while enemies are frightened
        }
        modeTimer -= dt;
        if (modeTimer > 0) {
            return;
        }
        scatterMode = !scatterMode;
        modeTimer = scatterMode ? difficulty.scatterTime : difficulty.chaseTime;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.getState() == Enemy.State.ACTIVE) {
                e.reverse();
            }
        }
    }

    /**
     * Touching an enemy costs a third of the player's health, unless armor or
     * respawn grace protects them.
     */
    private void checkCaught() {
        if (isPlayerInvulnerable()) {
            return;
        }
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.isShootable()) { // in the pen or dying: harmless
                continue;
            }
            if (Math.hypot(e.getX() - player.getX(), e.getY() - player.getY()) < Config.CATCH_DISTANCE) {
                lives--;
                hitFreeze = Config.HIT_FREEZE_TIME;
                triggerHeld = false;
                sounds.play(lives == 0 ? Sound.PLAYER_DEATH : Sound.PLAYER_PAIN, 1.0, 0.0);
                return;
            }
        }
    }

    /**
     * Nearby enemies growl now and then, louder when closer, from the side they are
     * on.
     */
    private void updateGrowls(double dt) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.isShootable() || !e.readyToGrowl(dt, randomGrowlInterval())) {
                continue;
            }
            int steps = ai.walkingDistanceToPlayer(e);
            if (steps <= Config.GROWL_RADIUS) {
                double volume = 1.0 - steps / (Config.GROWL_RADIUS + 1.0);
                sounds.play(e.getType().sightSound, volume, panFor(e));
            }
        }
    }

    private double randomGrowlInterval() {
        return Config.GROWL_MIN_INTERVAL + random.nextDouble() * Config.GROWL_RANDOM_INTERVAL;
    }

    /** Louder when the enemy is closer (straight-line distance). */
    private double volumeFor(Enemy e) {
        double distance = Math.hypot(e.getX() - player.getX(), e.getY() - player.getY());
        return Math.max(0.25, 1.0 - distance / 16.0);
    }

    /**
     * Which speaker a sound from this enemy should come out of.
     *
     * @param e the enemy
     * @return -1 (fully left) to 1 (fully right)
     */
    private double panFor(Enemy e) {
        return Math.sin(relativeAngle(e));
    }

    /**
     * Angle from the player's view direction to an enemy.
     *
     * @param e the enemy
     * @return radians in (-PI, PI]: 0 = straight ahead, positive = to the right
     */
    public double relativeAngle(Enemy e) {
        double toEnemy = Math.atan2(e.getY() - player.getY(), e.getX() - player.getX());
        return Math.IEEEremainder(toEnemy - player.getAngle(), 2 * Math.PI);
    }

    /**
     * How strongly the "sixth sense" should warn about an enemy. Only enemies that
     * can hurt
     * the player count (not frightened ones, not ones in the pen), measured by
     * walking distance
     * so an enemy on the other side of a wall does not set it off.
     *
     * @param e the enemy
     * @return 0 (no warning) up to almost 1 (right next to the player)
     */
    public double threatLevel(Enemy e) {
        if (!e.isShootable() || e.isFrightened()) {
            return 0;
        }
        int steps = ai.walkingDistanceToPlayer(e);
        if (steps > Config.SENSE_RADIUS) {
            return 0;
        }
        return 1.0 - steps / (Config.SENSE_RADIUS + 1.0);
    }

    /**
     * Which way the "sixth sense" should point for an enemy. If nothing blocks the
     * view, that is
     * straight at the enemy. Otherwise it follows the enemy's walk toward the
     * player and points at
     * the first spot the player can see - usually the corner the enemy will come
     * around.
     *
     * @param e the enemy
     * @return radians like {@link #relativeAngle}: 0 = straight ahead, positive =
     *         to the right
     */
    public double threatAngle(Enemy e) {
        double px = player.getX();
        double py = player.getY();
        double tx = e.getX();
        double ty = e.getY();
        if (!hasClearPath(px, py, tx, ty)) {
            int width = map.getWidth();
            int x = e.getTileX();
            int y = e.getTileY();
            for (int i = 0; i < Config.SENSE_RADIUS; i++) { // warnings only reach this far anyway
                int next = ai.stepTowardPlayer(x, y);
                if (next == y * width + x) {
                    break; // reached the player's tile
                }
                x = next % width;
                y = next / width;
                tx = x + 0.5;
                ty = y + 0.5;
                if (hasClearPath(px, py, tx, ty)) {
                    break; // first spot on its route the player can see
                }
            }
        }
        double toTarget = Math.atan2(ty - py, tx - px);
        return Math.IEEEremainder(toTarget - player.getAngle(), 2 * Math.PI);
    }

    /**
     * @param e the enemy
     * @return true if the enemy is on screen and no wall hides it, so no warning is
     *         needed
     */
    public boolean isInPlainSight(Enemy e) {
        return Math.abs(relativeAngle(e)) <= Math.toRadians(Config.FOV_DEGREES / 2.0)
                && hasClearPath(player.getX(), player.getY(), e.getX(), e.getY());
    }

    private void showMessage(String text) {
        message = text;
        messageTimer = MESSAGE_TIME;
    }

    /**
     * Holds or releases the trigger. While held, the gun fires as fast as it is
     * allowed to.
     *
     * @param held true while the fire button is down
     */
    public void setTriggerHeld(boolean held) {
        triggerHeld = held;
    }

    /** Starts a pistol reload (does nothing if not possible right now). */
    public void requestReload() {
        if (state == GameState.PLAYING && weapons.startReload()) {
            sounds.play(Sound.RELOAD, 1.0, 0.0);
        }
    }

    /** Freezes the game. */
    public void pause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
            triggerHeld = false;
        }
    }

    /** Unfreezes a paused game. */
    public void resume() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    /**
     * Returns the map.
     *
     * @return the map
     */
    public GameMap getMap() {
        return map;
    }

    /**
     * Returns the player.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the current state.
     *
     * @return the state
     */
    public GameState getState() {
        return state;
    }

    /** @return the difficulty this game was started with */
    public Difficulty getDifficulty() {
        return difficulty;
    }

    /** @return the player's guns */
    public Weapons getWeapons() {
        return weapons;
    }

    /** @return the four enemies (read-only) */
    public List<Enemy> getEnemies() {
        return enemyView;
    }

    /** @return {x, y} of every power pellet still on the map (read-only) */
    public List<int[]> getPowerUps() {
        return powerUpView;
    }

    /** @return hits the player can still take before game over */
    public int getLives() {
        return lives;
    }

    /** @return health shown on the status bar: 100, 67, 33 or 0 */
    public int getHealthPercent() {
        return (int) Math.round(lives * 100.0 / Config.PLAYER_LIVES);
    }

    /**
     * @return armor shown on the status bar: counts down from 100 to 0 while the
     *         power-up lasts
     */
    public int getArmorPercent() {
        if (powerUpTimer <= 0) {
            return 0;
        }
        return (int) Math.ceil(powerUpTimer / powerUpTotal * 100.0);
    }

    /** @return true while the shotgun power-up (armor) is active */
    public boolean isPowerUpActive() {
        return powerUpTimer > 0;
    }

    /** @return seconds left on the power-up, 0 if none */
    public double getPowerUpRemaining() {
        return powerUpTimer;
    }

    /** @return the kill streak of the current power-up */
    public Combo getCombo() {
        return combo;
    }

    /** @return true during BFG rampage mode */
    public boolean isRampageActive() {
        return rampage;
    }

    /**
     * @return how far the flame transition still has to go: 1 when rampage starts,
     *         0 when done
     */
    public double getRampageTransition() {
        return rampageTransition / Config.RAMPAGE_TRANSITION_TIME;
    }

    /** @return BFG explosions currently visible (read-only) */
    public List<Blast> getBlasts() {
        return blastView;
    }

    /** @return true if touching an enemy does no harm right now */
    public boolean isPlayerInvulnerable() {
        return powerUpTimer > 0 || graceTimer > 0;
    }

    /** @return true while the player just respawned and is protected */
    public boolean isRespawnGraceActive() {
        return graceTimer > 0;
    }

    /** @return true while the world is frozen after the player was caught */
    public boolean isPlayerHurt() {
        return hitFreeze > 0;
    }

    /** @return 1 right after being caught, fading to 0 as the freeze ends */
    public double getHurtAmount() {
        return Math.max(0, hitFreeze / Config.HIT_FREEZE_TIME);
    }

    /** @return 1 right after a pickup, fading to 0 */
    public double getPickupFlash() {
        return pickupFlash / PICKUP_FLASH_TIME;
    }

    /** @return enemies eliminated so far */
    public int getKills() {
        return kills;
    }

    /** @return points scored so far */
    public int getScore() {
        return score;
    }

    /** @return dots and power-ups still to eat; the level is complete at 0 */
    public int getDotsLeft() {
        return dotsLeft;
    }

    /** @return seconds of play so far */
    public double getTime() {
        return time;
    }

    /** @return the current pickup message, or null if none is showing */
    public String getMessage() {
        return messageTimer > 0 ? message : null;
    }
}

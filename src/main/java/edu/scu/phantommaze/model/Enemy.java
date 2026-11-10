package edu.scu.phantommaze.model;

import edu.scu.phantommaze.Config;

/**
 * One Doom enemy (a Pac-Man "ghost"). It walks from tile center to tile center along the
 * grid; {@link EnemyAI} decides which way to turn each time it reaches a center.
 */
public class Enemy {

    /** What the enemy is doing right now. */
    public enum State {
        /** Waiting inside the pen for its release timer. */
        IN_PEN,
        /** Walking out of the pen through the door. */
        LEAVING_PEN,
        /** Out in the maze: chasing, roaming to its corner, or fleeing. */
        ACTIVE,
        /** Playing its death animation; harmless and cannot be shot. */
        DYING
    }

    private final EnemyType type;
    private final int spawnX; // pen tile it returns to
    private final int spawnY;

    private double x; // position in tiles
    private double y;
    private int tileX; // the tile center it last passed through
    private int tileY;
    private int dirX; // grid direction it is walking: -1, 0 or 1 on each axis
    private int dirY;
    private double facing = -Math.PI / 2; // angle it faces (radians), starts facing north

    private State state = State.IN_PEN;
    private double stateTimer; // IN_PEN: seconds until release. DYING: seconds of animation left.
    private double painTimer; // > 0 while stunned by a pistol hit
    private int pistolHits;
    private boolean frightened; // running away from a shotgun-carrying player
    private double animTime; // seconds since creation, drives walking animation
    private double growlTimer; // seconds until it may growl again

    /**
     * Creates an enemy waiting in the pen.
     *
     * @param type   which Doom enemy
     * @param spawnX pen tile column
     * @param spawnY pen tile row
     */
    public Enemy(EnemyType type, int spawnX, int spawnY) {
        this.type = type;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        sendToPen(0);
    }

    /**
     * Teleports the enemy back to its pen tile and resets its hits.
     *
     * @param releaseDelay seconds before it starts leaving the pen
     */
    public void sendToPen(double releaseDelay) {
        state = State.IN_PEN;
        stateTimer = releaseDelay;
        x = spawnX + 0.5;
        y = spawnY + 0.5;
        tileX = spawnX;
        tileY = spawnY;
        dirX = 0;
        dirY = 0;
        facing = -Math.PI / 2;
        painTimer = 0;
        pistolHits = 0;
        frightened = false;
    }

    /**
     * Counts down the enemy's timers.
     *
     * @param dt seconds elapsed
     */
    public void tick(double dt) {
        animTime += dt;
        painTimer = Math.max(0, painTimer - dt);
        if (state == State.IN_PEN || state == State.DYING) {
            stateTimer -= dt;
        }
    }

    /**
     * Tells whether the pen timer has run out.
     *
     * @return true if it is in the pen and should start leaving
     */
    public boolean isReadyToLeave() {
        return state == State.IN_PEN && stateTimer <= 0;
    }

    /**
     * Tells whether the death animation is over.
     *
     * @return true if it is dying and the animation finished
     */
    public boolean isDeathFinished() {
        return state == State.DYING && stateTimer <= 0;
    }

    /** Starts walking out of the pen. */
    public void startLeaving() {
        state = State.LEAVING_PEN;
    }

    /** Has stepped out of the pen door into the maze. */
    public void becomeActive() {
        state = State.ACTIVE;
    }

    /** Eliminates the enemy: it plays its death animation, then goes back to the pen. */
    public void kill() {
        state = State.DYING;
        stateTimer = Config.ENEMY_DEATH_TIME;
        frightened = false;
        dirX = 0;
        dirY = 0;
    }

    /**
     * Registers one pistol hit and stuns the enemy for a moment.
     *
     * @return true if that was enough hits to eliminate it
     */
    public boolean hurt() {
        pistolHits++;
        painTimer = Config.ENEMY_PAIN_TIME;
        return pistolHits >= Config.PISTOL_HITS_TO_ELIMINATE;
    }

    /**
     * Tells whether bullets can hit it (it is out of the pen and alive).
     *
     * @return true if shootable
     */
    public boolean isShootable() {
        return state == State.ACTIVE || state == State.LEAVING_PEN;
    }

    /**
     * Turns around on the spot (used when Pac-Man ghosts switch modes). Only works while
     * walking; the enemy heads back to the tile it just left.
     */
    public void reverse() {
        if (dirX == 0 && dirY == 0) {
            return;
        }
        if (x != tileX + 0.5 || y != tileY + 0.5) {
            tileX += dirX;
            tileY += dirY;
        }
        setDirection(-dirX, -dirY);
    }

    /**
     * Sets the grid direction it walks in.
     *
     * @param dx -1, 0 or 1
     * @param dy -1, 0 or 1
     */
    public void setDirection(int dx, int dy) {
        dirX = dx;
        dirY = dy;
        if (dx != 0 || dy != 0) {
            facing = Math.atan2(dy, dx);
        }
    }

    /**
     * Places the enemy exactly on a tile center and records it as the current tile.
     *
     * @param tx tile column
     * @param ty tile row
     */
    public void arriveAt(int tx, int ty) {
        tileX = tx;
        tileY = ty;
        x = tx + 0.5;
        y = ty + 0.5;
    }

    /**
     * Moves part of the way toward the next tile center.
     *
     * @param distance tiles to move along the current direction
     */
    public void advance(double distance) {
        x += dirX * distance;
        y += dirY * distance;
    }

    /**
     * Counts down the growl timer.
     *
     * @param dt           seconds elapsed
     * @param nextInterval seconds to wait after this growl, if one is due
     * @return true if it is time to growl
     */
    public boolean readyToGrowl(double dt, double nextInterval) {
        growlTimer -= dt;
        if (growlTimer > 0) {
            return false;
        }
        growlTimer = nextInterval;
        return true;
    }

    /**
     * Sets whether it is running away from the player.
     *
     * @param frightened true to flee
     */
    public void setFrightened(boolean frightened) {
        this.frightened = frightened;
    }

    /** @return the Doom enemy type */
    public EnemyType getType() {
        return type;
    }

    /** @return x position in tiles */
    public double getX() {
        return x;
    }

    /** @return y position in tiles */
    public double getY() {
        return y;
    }

    /** @return column of the tile center it last passed */
    public int getTileX() {
        return tileX;
    }

    /** @return row of the tile center it last passed */
    public int getTileY() {
        return tileY;
    }

    /** @return grid x direction (-1, 0 or 1) */
    public int getDirX() {
        return dirX;
    }

    /** @return grid y direction (-1, 0 or 1) */
    public int getDirY() {
        return dirY;
    }

    /** @return the angle it faces in radians (same convention as the player) */
    public double getFacing() {
        return facing;
    }

    /** @return its current state */
    public State getState() {
        return state;
    }

    /** @return true while it runs away from the player */
    public boolean isFrightened() {
        return frightened;
    }

    /** @return true while stunned by a pistol hit */
    public boolean isInPain() {
        return painTimer > 0;
    }

    /** @return pistol hits taken since it last left the pen */
    public int getPistolHits() {
        return pistolHits;
    }

    /** @return seconds it has existed, for the walking animation */
    public double getAnimTime() {
        return animTime;
    }

    /** @return 0 (just died) to 1 (animation over); 0 when not dying */
    public double getDeathProgress() {
        if (state != State.DYING) {
            return 0;
        }
        return Math.min(1.0, 1.0 - stateTimer / Config.ENEMY_DEATH_TIME);
    }
}

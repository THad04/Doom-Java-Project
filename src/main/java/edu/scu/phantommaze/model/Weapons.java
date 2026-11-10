package edu.scu.phantommaze.model;

import edu.scu.phantommaze.Config;

/**
 * The player's guns: which one is held, how much ammo is left, and the timers that stop
 * the player from shooting too fast or while reloading.
 */
public class Weapons {

    private Weapon current = Weapon.PISTOL;
    private int pistolClip = Config.PISTOL_CLIP_SIZE;
    private int shells;
    private double cooldown; // seconds until the next shot, retained across weapon switches
    private double reloadTimer; // seconds left on the pistol reload, 0 = not reloading
    private double timeSinceShot = Double.MAX_VALUE; // drives the firing animation

    /**
     * Counts down the timers.
     *
     * @param dt seconds elapsed
     */
    public void update(double dt) {
        cooldown = Math.max(0, cooldown - dt);
        timeSinceShot += dt;
        if (reloadTimer > 0) {
            reloadTimer -= dt;
            if (reloadTimer <= 0) {
                reloadTimer = 0;
                pistolClip = Config.PISTOL_CLIP_SIZE;
            }
        }
    }

    /**
     * Tries to fire the held gun. Fails if the gun is still cooling down, reloading, or empty.
     *
     * @return true if a shot was fired
     */
    public boolean tryFire() {
        if (cooldown > 0) {
            return false;
        }
        if (current == Weapon.BFG) {
            cooldown = Config.BFG_FIRE_DELAY; // unlimited ammo
        } else if (current == Weapon.SHOTGUN) {
            if (shells <= 0) {
                return false;
            }
            shells--;
            cooldown = Config.SHOTGUN_FIRE_DELAY;
        } else {
            if (isReloading() || pistolClip <= 0) {
                return false;
            }
            pistolClip--;
            cooldown = Config.PISTOL_FIRE_DELAY;
        }
        timeSinceShot = 0;
        return true;
    }

    /**
     * Starts a pistol reload if the pistol is held, not already reloading, and not full.
     *
     * @return true if a reload started
     */
    public boolean startReload() {
        if (current != Weapon.PISTOL || isReloading() || pistolClip >= Config.PISTOL_CLIP_SIZE) {
            return false;
        }
        reloadTimer = Config.PISTOL_RELOAD_TIME;
        return true;
    }

    /** Switches to the shotgun with fresh shells, cancelling any pistol reload. */
    public void equipShotgun() {
        current = Weapon.SHOTGUN;
        shells = Config.SHOTGUN_SHELLS;
        reloadTimer = 0;
        timeSinceShot = Double.MAX_VALUE;
    }

    /**
     * Switches to the BFG for rampage mode. It cannot fire until the flame transition is over.
     *
     * @param readyIn seconds until the first shot is allowed
     */
    public void equipBfg(double readyIn) {
        current = Weapon.BFG;
        shells = 0;
        reloadTimer = 0;
        cooldown = readyIn;
        timeSinceShot = Double.MAX_VALUE;
    }

    /** Puts the shotgun or BFG away (any shells left are lost) and goes back to the pistol. */
    public void equipPistol() {
        current = Weapon.PISTOL;
        shells = 0;
        timeSinceShot = Double.MAX_VALUE;
    }

    /** Back to the starting loadout: pistol with a full clip. */
    public void reset() {
        equipPistol();
        pistolClip = Config.PISTOL_CLIP_SIZE;
        reloadTimer = 0;
        cooldown = 0;
    }

    /**
     * Returns the gun in the player's hands.
     *
     * @return the held weapon
     */
    public Weapon getCurrent() {
        return current;
    }

    /**
     * Ammo for the held gun: bullets in the pistol clip, or shotgun shells. The BFG has unlimited
     * ammo, so the status bar leaves the number blank for it (like Doom's fist and chainsaw).
     *
     * @return the ammo count shown on the status bar (0 for the BFG)
     */
    public int getAmmo() {
        if (current == Weapon.PISTOL) {
            return pistolClip;
        }
        return current == Weapon.SHOTGUN ? shells : 0;
    }

    /**
     * Tells whether the pistol is being reloaded.
     *
     * @return true while reloading
     */
    public boolean isReloading() {
        return reloadTimer > 0;
    }

    /**
     * How far along the reload is.
     *
     * @return 0 (just started) to 1 (done); 0 when not reloading
     */
    public double getReloadProgress() {
        return isReloading() ? 1.0 - reloadTimer / Config.PISTOL_RELOAD_TIME : 0.0;
    }

    /**
     * Seconds since the last shot, used to pick the firing animation frame.
     *
     * @return seconds since the last shot
     */
    public double getTimeSinceShot() {
        return timeSinceShot;
    }
}

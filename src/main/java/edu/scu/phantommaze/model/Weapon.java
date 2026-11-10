package edu.scu.phantommaze.model;

/** The guns the player can hold. */
public enum Weapon {
    /** Starting gun: 3-round clip, unlimited reloads, 3 hits sends an enemy back to the pen. */
    PISTOL,
    /** Power-up gun: 50 shells, one shot kills. */
    SHOTGUN,
    /** Rampage gun: unlimited, charges up, then kills every enemy in a wide cone in front. */
    BFG
}

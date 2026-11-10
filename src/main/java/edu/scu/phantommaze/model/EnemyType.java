package edu.scu.phantommaze.model;

/**
 * The four Doom enemies that replace Pac-Man's ghosts. Each copies one ghost's way of
 * hunting (see {@link Game} for the targeting rules).
 */
public enum EnemyType {

    /** Blinky (red): heads straight for the player. Home corner: top right. */
    CACODEMON("Cacodemon", true, false, Sound.CACODEMON_SIGHT, Sound.DEMON_PAIN, Sound.CACODEMON_DEATH),
    /** Pinky (pink): aims 4 tiles ahead of where the player is facing. Home corner: top left. */
    DEMON("Demon", false, false, Sound.DEMON_SIGHT, Sound.DEMON_PAIN, Sound.DEMON_DEATH),
    /**
     * Inky (cyan): flanks, using both the player and the cacodemon's position. Home corner:
     * bottom right.
     */
    LOST_SOUL("Lost Soul", true, true, Sound.LOST_SOUL_SIGHT, Sound.DEMON_PAIN, Sound.LOST_SOUL_DEATH),
    /**
     * Clyde (orange): chases when far away, wanders back to its corner when close. Home
     * corner: bottom left.
     */
    IMP("Imp", false, true, Sound.IMP_SIGHT, Sound.IMP_PAIN, Sound.IMP_DEATH);

    /** Display name. */
    public final String label;
    /** True if its home corner is on the right side of the map. */
    public final boolean homeRight;
    /** True if its home corner is on the bottom of the map. */
    public final boolean homeBottom;
    /** Growl played when it is near the player. */
    public final Sound sightSound;
    /** Grunt played when a pistol bullet hits it. */
    public final Sound painSound;
    /** Played when it is eliminated. */
    public final Sound deathSound;

    EnemyType(String label, boolean homeRight, boolean homeBottom, Sound sightSound, Sound painSound,
            Sound deathSound) {
        this.label = label;
        this.homeRight = homeRight;
        this.homeBottom = homeBottom;
        this.sightSound = sightSound;
        this.painSound = painSound;
        this.deathSound = deathSound;
    }
}

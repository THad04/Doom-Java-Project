package edu.scu.phantommaze.model;

/**
 * The difficulty levels picked on the main menu. Each level is a bundle of numbers that
 * {@link Game} reads while it runs.
 */
public enum Difficulty {

    /** Slow enemies, long power-ups, lots of breathing room. */
    EASY("Easy", 2.0, 1.4, 7.0, 12.0, 7.0, 9.0, 15.0),
    /** The default balance. */
    NORMAL("Normal", 2.4, 1.6, 5.0, 10.0, 5.0, 7.0, 20.0),
    /** Fast enemies that leave the pen quickly and chase for longer. */
    HARD("Hard", 2.8, 1.8, 3.0, 7.0, 3.0, 5.0, 25.0);

    /** Name shown on the menu. */
    public final String label;
    /** Enemy speed while chasing or roaming, in tiles per second (the player does 3.2). */
    public final double enemySpeed;
    /** Enemy speed while running away from a shotgun-carrying player. */
    public final double fleeSpeed;
    /** Seconds between enemies leaving the pen at the start of a round. */
    public final double releaseInterval;
    /** Seconds a shotgun power-up (and its armor) lasts. */
    public final double powerUpDuration;
    /** Seconds an eliminated enemy waits in the pen before coming back out. */
    public final double respawnDelay;
    /** Seconds enemies spend roaming to their home corners before chasing again. */
    public final double scatterTime;
    /** Seconds enemies spend chasing before taking another break. */
    public final double chaseTime;

    Difficulty(String label, double enemySpeed, double fleeSpeed, double releaseInterval,
            double powerUpDuration, double respawnDelay, double scatterTime, double chaseTime) {
        this.label = label;
        this.enemySpeed = enemySpeed;
        this.fleeSpeed = fleeSpeed;
        this.releaseInterval = releaseInterval;
        this.powerUpDuration = powerUpDuration;
        this.respawnDelay = respawnDelay;
        this.scatterTime = scatterTime;
        this.chaseTime = chaseTime;
    }

    /**
     * A one-line summary of this level's numbers for the menu.
     *
     * @return the description
     */
    public String describe() {
        return String.format("Enemy speed %.1f  |  power-up %.0fs  |  respawn %.0fs  |  release every %.0fs",
                enemySpeed, powerUpDuration, respawnDelay, releaseInterval);
    }
}

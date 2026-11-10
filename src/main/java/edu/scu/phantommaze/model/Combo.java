package edu.scu.phantommaze.model;

import edu.scu.phantommaze.Config;

/**
 * Counts a kill streak during a power-up. Each kill must come within {@link Config#COMBO_WINDOW}
 * seconds of the one before, and only different enemies count, so the same enemy killed twice
 * after respawning does not help. When every enemy has been killed in one streak, the combo is
 * complete and rampage mode starts.
 */
public class Combo {

    private int killedMask; // bit i set = enemy type with ordinal i is part of this streak
    private int count;
    private double timer; // seconds left to make the next kill

    /**
     * Counts a kill toward the streak.
     *
     * @param e      the enemy just killed
     * @param needed how many different enemies complete the combo
     * @return true if this kill completed the combo
     */
    boolean recordKill(Enemy e, int needed) {
        int bit = 1 << e.getType().ordinal();
        if ((killedMask & bit) != 0) {
            return false; // already part of this streak: does not count, does not extend the timer
        }
        killedMask |= bit;
        count++;
        timer = Config.COMBO_WINDOW;
        if (count >= needed) {
            reset();
            return true;
        }
        return false;
    }

    /**
     * Runs the streak's clock; the streak breaks if it runs out.
     *
     * @param dt seconds elapsed
     */
    void update(double dt) {
        if (count == 0) {
            return;
        }
        timer -= dt;
        if (timer <= 0) {
            reset();
        }
    }

    /** Forgets the current streak. */
    void reset() {
        killedMask = 0;
        count = 0;
        timer = 0;
    }

    /** @return kills in the current streak, 0 if none */
    public int getCount() {
        return count;
    }

    /** @return time left for the next kill, from 1 (just killed) down to 0 */
    public double getTimeLeftFraction() {
        return count == 0 ? 0 : timer / Config.COMBO_WINDOW;
    }
}

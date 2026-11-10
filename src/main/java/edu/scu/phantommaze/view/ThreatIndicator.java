package edu.scu.phantommaze.view;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.Enemy;
import edu.scu.phantommaze.model.Game;

/**
 * The "sixth sense": when a dangerous enemy is close but the player cannot see it, the edge of the
 * screen glows red on the side it will come from - left, right, bottom ("behind you"), or top
 * ("around a corner ahead"). For an enemy hidden around a corner, that is the direction of the
 * corner, not straight through the wall. The glow gets stronger as the enemy gets closer and
 * pulses gently so it catches the eye.
 */
final class ThreatIndicator {

    /** How far the side glows reach into the picture, in render pixels. */
    private static final int SIDE_WIDTH = Config.RENDER_WIDTH / 6;
    /** How far the top and bottom glows reach into the 3D view. */
    private static final int END_HEIGHT = Config.VIEW_HEIGHT / 4;
    /** Strongest tint, used when an enemy is right next to the player (1 = solid red). */
    private static final double MAX_OPACITY = 0.75;
    /** Speed of the pulse in radians per second (about one pulse per second). */
    private static final double PULSE_SPEED = 6.0;
    /** The red the edges fade toward. */
    private static final int RED = 220;

    /** Tint strength for each distance from the edge, worked out once: strong at the edge, fading inward. */
    private final double[] sideFade = fade(SIDE_WIDTH);
    private final double[] endFade = fade(END_HEIGHT);

    /**
     * Tints the edges of the 3D view (never the status bar).
     *
     * @param pixels the frame's pixels, {@link Config#RENDER_WIDTH} wide
     * @param game   the game being shown
     */
    void render(int[] pixels, Game game) {
        double left = 0;
        double right = 0;
        double ahead = 0;
        double behind = 0;
        for (Enemy e : game.getEnemies()) {
            double level = game.threatLevel(e);
            if (level <= 0 || game.isInPlainSight(e)) {
                continue; // harmless, far away, or the player can already see it
            }
            double angle = game.threatAngle(e); // 0 = ahead, positive = to the right
            double side = Math.sin(angle);
            double front = Math.cos(angle);
            right = Math.max(right, level * Math.max(0, side));
            left = Math.max(left, level * Math.max(0, -side));
            ahead = Math.max(ahead, level * Math.max(0, front));
            behind = Math.max(behind, level * Math.max(0, -front));
        }
        if (left == 0 && right == 0 && ahead == 0 && behind == 0) {
            return;
        }
        double pulse = 0.75 + 0.25 * Math.sin(game.getTime() * PULSE_SPEED);
        double scale = MAX_OPACITY * pulse;
        int w = Config.RENDER_WIDTH;

        for (int y = 0; y < Config.VIEW_HEIGHT; y++) {
            int row = y * w;
            for (int d = 0; d < SIDE_WIDTH; d++) {
                if (left > 0) {
                    tint(pixels, row + d, left * scale * sideFade[d]);
                }
                if (right > 0) {
                    tint(pixels, row + w - 1 - d, right * scale * sideFade[d]);
                }
            }
        }
        tintRows(pixels, ahead * scale, 0, 1);
        tintRows(pixels, behind * scale, Config.VIEW_HEIGHT - 1, -1);
    }

    /**
     * Tints a band of full-width rows, strongest at {@code edgeRow} and fading toward the middle.
     *
     * @param strength how red the edge row gets (0 = skip)
     * @param edgeRow  the first row of the band
     * @param step     +1 to go down the screen, -1 to go up
     */
    private void tintRows(int[] pixels, double strength, int edgeRow, int step) {
        if (strength <= 0) {
            return;
        }
        int w = Config.RENDER_WIDTH;
        for (int d = 0; d < END_HEIGHT; d++) {
            int row = (edgeRow + d * step) * w;
            double amount = strength * endFade[d];
            for (int x = 0; x < w; x++) {
                tint(pixels, row + x, amount);
            }
        }
    }

    /** Mixes one pixel toward red by {@code amount} (0 = unchanged, 1 = fully red). */
    private static void tint(int[] pixels, int i, double amount) {
        int c = pixels[i];
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        r += (int) ((RED - r) * amount);
        g -= (int) (g * amount);
        b -= (int) (b * amount);
        pixels[i] = (r << 16) | (g << 8) | b;
    }

    /** Fade curve: 1 at the edge, dropping smoothly to 0 at {@code size} pixels in. */
    private static double[] fade(int size) {
        double[] values = new double[size];
        for (int d = 0; d < size; d++) {
            double t = 1.0 - (double) d / size;
            values[d] = t * t;
        }
        return values;
    }
}

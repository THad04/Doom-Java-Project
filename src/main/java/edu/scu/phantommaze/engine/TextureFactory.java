package edu.scu.phantommaze.engine;

import edu.scu.phantommaze.Config;
import java.util.Random;

/** Generates the wall textures procedurally as packed RGB int arrays (no image files). */
public final class TextureFactory {

    /** Number of textures returned by {@link #createWallTextures()}. */
    public static final int COUNT = 3;
    /** Index of the brick texture. */
    public static final int BRICK = 0;
    /** Index of the stone texture. */
    public static final int STONE = 1;
    /** Index of the metal texture. */
    public static final int METAL = 2;

    private static final int S = Config.TEXTURE_SIZE;

    private TextureFactory() {
        // static factory
    }

    /**
     * Creates all wall textures.
     *
     * @return array indexed by BRICK, STONE, METAL; each is a 64x64 row-major RGB array
     */
    public static int[][] createWallTextures() {
        return new int[][] {brick(new Random(1)), stone(new Random(2)), metal(new Random(3))};
    }

    private static int rgb(int r, int g, int b) {
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** Red-brown bricks with staggered rows and mortar lines. */
    private static int[] brick(Random rnd) {
        int[] px = new int[S * S];
        int brickH = 16;
        int brickW = 32;
        int[] tint = new int[16];
        for (int i = 0; i < tint.length; i++) {
            tint[i] = rnd.nextInt(40) - 20;
        }
        for (int y = 0; y < S; y++) {
            int row = y / brickH;
            int offset = (row % 2) * (brickW / 2);
            for (int x = 0; x < S; x++) {
                int bx = (x + offset) % S;
                int col = bx / brickW;
                boolean mortar = (y % brickH) < 2 || (bx % brickW) < 2;
                int n = rnd.nextInt(24) - 12;
                if (mortar) {
                    px[y * S + x] = rgb(70 + n / 2, 66 + n / 2, 62 + n / 2);
                } else {
                    int t = tint[(row * 2 + col) % tint.length];
                    px[y * S + x] = rgb(140 + t + n, 55 + t / 2 + n / 2, 45 + t / 3 + n / 2);
                }
            }
        }
        return px;
    }

    /** Grey cut stone blocks with cracks and speckle. */
    private static int[] stone(Random rnd) {
        int[] px = new int[S * S];
        int block = 32;
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                int bi = (y / block) * 2 + (x / block);
                int base = 105 + (bi * 17) % 30;
                int n = rnd.nextInt(30) - 15;
                // Slightly smoother large-scale variation so it looks like rock grain.
                int grain = (int) (10 * Math.sin(x * 0.35 + y * 0.2) * Math.cos(y * 0.3));
                int v = base + n + grain;
                if (x % block < 2 || y % block < 2) {
                    v -= 45;
                }
                px[y * S + x] = rgb(v, v + 2, v + 8);
            }
        }
        // A couple of random cracks.
        for (int c = 0; c < 4; c++) {
            int cx = rnd.nextInt(S);
            int cy = rnd.nextInt(S);
            for (int i = 0; i < 18; i++) {
                cx = (cx + rnd.nextInt(3) - 1 + S) % S;
                cy = (cy + 1) % S;
                px[cy * S + cx] = rgb(35, 35, 45);
            }
        }
        return px;
    }

    /** Blue-grey metal panels with rivets and a hazard stripe. */
    private static int[] metal(Random rnd) {
        int[] px = new int[S * S];
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                int n = rnd.nextInt(10) - 5;
                int v = 100 + n + (y % 32 < 16 ? 6 : 0);
                px[y * S + x] = rgb(v - 15, v, v + 18);
                if (x % 32 < 2 || y % 32 < 2) { // panel seams
                    px[y * S + x] = rgb(40, 48, 62);
                }
            }
        }
        // Rivets in each panel corner.
        int[] rivets = {6, 26, 38, 58};
        for (int ry : rivets) {
            for (int rx : rivets) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int shade = (dx == -1 && dy == -1) ? 190 : (dx + dy > 0 ? 60 : 130);
                        px[(ry + dy) * S + (rx + dx)] = rgb(shade - 20, shade, shade + 15);
                    }
                }
            }
        }
        // Hazard stripe (yellow/black diagonal) along the bottom.
        for (int y = S - 8; y < S; y++) {
            for (int x = 0; x < S; x++) {
                boolean yellow = ((x + y) / 6) % 2 == 0;
                px[y * S + x] = yellow ? rgb(200, 170, 30) : rgb(25, 25, 25);
            }
        }
        return px;
    }
}

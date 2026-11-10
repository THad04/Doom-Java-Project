package edu.scu.phantommaze.view;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.Player;

/**
 * Draws flat pictures that always face the camera ("billboards"): enemies and pickups.
 *
 * <p>For each sprite we work out how far in front of the camera it is and where on the
 * screen it lands, scale the picture by distance, and draw it one screen column at a time.
 * A column is skipped when the {@link WorldRenderer}'s z-buffer says a wall in that column is
 * closer, which is what makes enemies disappear behind corners. Sprites are drawn far to
 * near so closer ones cover further ones.
 *
 * <p>Usage each frame: {@link #clear()}, {@link #add} every sprite, then {@link #render}.
 */
public class SpriteRenderer {

    /** Normal colors. */
    public static final int TINT_NONE = 0;
    /** Pac-Man "frightened" blue. */
    public static final int TINT_FRIGHTENED = 1;
    /** Flashing white: the power-up is about to run out. */
    public static final int TINT_FLASH = 2;

    private static final int W = Config.RENDER_WIDTH;
    private static final int H = Config.VIEW_HEIGHT;
    private static final double NEAR_CLIP = 0.15; // closer than this (tiles) is not drawn
    /** Screen pixels per tile, horizontally, for something 1 tile in front of the camera. */
    private static final double HORIZONTAL_SCALE = (W / 2.0) / Config.CAMERA_PLANE_LENGTH;

    // One slot per sprite added this frame; reused every frame so drawing allocates nothing.
    private final int capacity;
    private final Patch[] patches;
    private final double[] worldX;
    private final double[] worldY;
    private final double[] hover;
    private final int[] tints;
    private final boolean[] fullBright;
    private final double[] depth;
    private final int[] order;
    private final int[] texRows = new int[H]; // sprite row to use for each screen row
    private int count;

    /**
     * Creates a renderer with room for a fixed number of sprites per frame.
     *
     * @param capacity the most sprites that will be {@link #add added} between two {@link #clear} calls
     */
    public SpriteRenderer(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative: " + capacity);
        }
        this.capacity = capacity;
        patches = new Patch[capacity];
        worldX = new double[capacity];
        worldY = new double[capacity];
        hover = new double[capacity];
        tints = new int[capacity];
        fullBright = new boolean[capacity];
        depth = new double[capacity];
        order = new int[capacity];
    }

    /** Forgets last frame's sprites. */
    public void clear() {
        count = 0;
    }

    /**
     * Queues a sprite for drawing.
     *
     * @param patch      the picture
     * @param x          world x in tiles (the sprite's anchor, usually its feet)
     * @param y          world y in tiles
     * @param hoverPx    how far above the floor, in sprite pixels
     * @param tint       one of the TINT_ constants
     * @param bright     true to ignore fog and distance darkening
     * @throws IllegalStateException if more sprites are queued than the capacity
     */
    public void add(Patch patch, double x, double y, double hoverPx, int tint, boolean bright) {
        if (count == capacity) {
            throw new IllegalStateException("SpriteRenderer capacity exceeded: " + capacity);
        }
        patches[count] = patch;
        worldX[count] = x;
        worldY[count] = y;
        hover[count] = hoverPx;
        tints[count] = tint;
        fullBright[count] = bright;
        count++;
    }

    /**
     * Draws every queued sprite into the pixel buffer.
     *
     * @param pixels  the render buffer
     * @param zBuffer wall distance per screen column, filled by {@link WorldRenderer}
     * @param player  the camera
     */
    public void render(int[] pixels, double[] zBuffer, Player player) {
        double dirX = player.getDirX();
        double dirY = player.getDirY();
        // Depth = distance straight ahead of the camera (same measure as the z-buffer).
        for (int i = 0; i < count; i++) {
            depth[i] = (worldX[i] - player.getX()) * dirX + (worldY[i] - player.getY()) * dirY;
            order[i] = i;
        }
        // Sort far to near (insertion sort: there are only a handful of sprites).
        for (int i = 1; i < count; i++) {
            int item = order[i];
            int j = i - 1;
            while (j >= 0 && depth[order[j]] < depth[item]) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = item;
        }
        for (int i = 0; i < count; i++) {
            drawOne(pixels, zBuffer, player, order[i]);
        }
    }

    private void drawOne(int[] pixels, double[] zBuffer, Player player, int i) {
        double d = depth[i];
        if (d < NEAR_CLIP) {
            return; // behind the camera or too close
        }
        Patch p = patches[i];
        double relX = worldX[i] - player.getX();
        double relY = worldY[i] - player.getY();
        // cameraX is -1 at the left screen edge and +1 at the right (same as WorldRenderer).
        double planeLengthSq = Config.CAMERA_PLANE_LENGTH * Config.CAMERA_PLANE_LENGTH;
        double cameraX = (relX * player.getPlaneX() + relY * player.getPlaneY()) / planeLengthSq / d;
        double screenX = (1.0 + cameraX) * W / 2.0;

        // Size of one sprite pixel on screen.
        double pixelW = Config.SPRITE_PIXEL_SIZE * HORIZONTAL_SCALE / d;
        double pixelH = pixelW * Config.SPRITE_ASPECT;
        // Screen row of the floor at this distance (bottom of a wall that far away).
        double floorY = H / 2.0 + Config.PROJECTION_SCALE / 2.0 / d;
        double left = screenX - p.leftOffset * pixelW;
        double top = floorY - (p.topOffset + hover[i]) * pixelH;

        int x0 = Math.max(0, (int) Math.ceil(left));
        int x1 = Math.min(W, (int) Math.ceil(left + p.width * pixelW));
        int y0 = Math.max(0, (int) Math.ceil(top));
        int y1 = Math.min(H, (int) Math.ceil(top + p.height * pixelH));

        int light = 256;
        int fogR = 0;
        int fogG = 0;
        int fogB = 0;
        if (!fullBright[i]) { // same fog formula as the walls
            double fog = Math.max(0.0, 1.0 - d / Config.FOG_DISTANCE);
            light = (int) (256 * fog * fog);
            fogR = Config.FOG_R * (256 - light) / 256;
            fogG = Config.FOG_G * (256 - light) / 256;
            fogB = Config.FOG_B * (256 - light) / 256;
        }
        int tint = tints[i];

        for (int y = y0; y < y1; y++) {
            texRows[y] = Math.min(p.height - 1, (int) ((y + 0.5 - top) / pixelH)) * p.width;
        }
        for (int x = x0; x < x1; x++) {
            if (d >= zBuffer[x]) {
                continue; // a wall is in front of this column
            }
            int texX = Math.min(p.width - 1, (int) ((x + 0.5 - left) / pixelW));
            for (int y = y0; y < y1; y++) {
                int color = p.argb[texRows[y] + texX];
                if ((color >>> 24) < 128) {
                    continue; // transparent pixel
                }
                int r = (color >> 16) & 255;
                int g = (color >> 8) & 255;
                int b = color & 255;
                if (tint != TINT_NONE) {
                    int brightness = (r * 3 + g * 6 + b) / 10;
                    if (tint == TINT_FRIGHTENED) {
                        r = brightness / 4;
                        g = brightness / 3;
                        b = Math.min(255, 70 + brightness);
                    } else {
                        r = Math.min(255, 90 + brightness);
                        g = r;
                        b = r;
                    }
                }
                r = r * light / 256 + fogR;
                g = g * light / 256 + fogG;
                b = b * light / 256 + fogB;
                pixels[y * W + x] = (r << 16) | (g << 8) | b;
            }
        }
    }
}

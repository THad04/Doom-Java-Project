package edu.scu.phantommaze.view;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.engine.RayHit;
import edu.scu.phantommaze.engine.Raycaster;
import edu.scu.phantommaze.engine.TextureFactory;
import edu.scu.phantommaze.model.GameMap;
import edu.scu.phantommaze.model.Player;

/**
 * Draws the textured, fogged 3D walls plus the ceiling/floor gradient into an int[] pixel buffer.
 * Allocates nothing while rendering; fills the z-buffer used later to occlude sprites.
 */
public class WorldRenderer {

    private static final int W = Config.RENDER_WIDTH;
    private static final int H = Config.VIEW_HEIGHT; // rows below this belong to the status bar
    private static final int TEX = Config.TEXTURE_SIZE;

    private final GameMap map;
    private final Raycaster raycaster;
    private final RayHit hit = new RayHit(); // reused for every column
    private final int[][] textures = TextureFactory.createWallTextures();
    private final int[] ceilingRows = new int[H / 2];
    private final int[] floorRows = new int[H - H / 2];

    /**
     * Creates a world renderer.
     *
     * @param map the map to draw
     */
    public WorldRenderer(GameMap map) {
        this.map = map;
        this.raycaster = new Raycaster(map);
        // Precompute one gradient color per screen row.
        for (int y = 0; y < ceilingRows.length; y++) {
            ceilingRows[y] = lerp(Config.CEILING_TOP, Config.CEILING_HORIZON, y / (double) ceilingRows.length);
        }
        for (int y = 0; y < floorRows.length; y++) {
            floorRows[y] = lerp(Config.FLOOR_HORIZON, Config.FLOOR_BOTTOM, y / (double) floorRows.length);
        }
    }

    private static int lerp(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /**
     * Renders one frame of walls.
     *
     * <p>The screen is drawn one vertical column at a time. For each column we:
     * (1) shoot a ray into the map, (2) work out how tall the wall looks,
     * (3) pick a texture, (4) work out the fog, (5) paint ceiling, wall and floor.
     *
     * @param pixels target RGB pixel buffer of size RENDER_WIDTH * RENDER_HEIGHT; only the top
     *               VIEW_HEIGHT rows are drawn
     * @param player the camera
     * @param zBuffer receives the perpendicular wall distance per screen column
     */
    public void render(int[] pixels, Player player, double[] zBuffer) {
        int half = H / 2; // the horizon: ceiling above this row, floor below

        for (int x = 0; x < W; x++) {
            // 1. Shoot a ray through this column and find the wall it hits.
            //    cameraX goes from -1 (left edge of the screen) to +1 (right edge); the ray goes
            //    through the middle of the column so sprites (which sample pixel centers) line up.
            double cameraX = (2.0 * x + 1.0) / W - 1.0;
            double rayX = player.getDirX() + player.getPlaneX() * cameraX;
            double rayY = player.getDirY() + player.getPlaneY() * cameraX;
            raycaster.cast(player.getX(), player.getY(), rayX, rayY, hit);
            zBuffer[x] = hit.distance;

            // 2. Wall height on screen: close walls are tall, far walls are short.
            int lineHeight = (int) (Config.PROJECTION_SCALE / Math.max(hit.distance, 1e-4));
            int start = half - lineHeight / 2; // may be negative (wall taller than screen)
            int end = half + lineHeight / 2;
            int drawStart = Math.max(0, start); // clip to the screen
            int drawEnd = Math.min(H, end);

            // 3. Pick the texture, and which column of it to use.
            int[] tex = textures[chooseTexture()];
            int texX = Math.min(TEX - 1, (int) (hit.textureX * TEX));

            // 4. Fog. "brightness" is 256 when the wall is close (fully visible)
            //    and 0 when far away (fully fogged). Walls hit on their y-side are darker.
            double fog = Math.max(0.0, 1.0 - hit.distance / Config.FOG_DISTANCE);
            fog = fog * fog;
            int brightness = (int) (256 * fog);
            double sideShade = hit.side == 1 ? Config.Y_SIDE_SHADE : 1.0;
            int wallLight = (int) (brightness * sideShade);
            // How much of the fog color to blend in (more fog the further away).
            int fogR = Config.FOG_R * (256 - brightness) / 256;
            int fogG = Config.FOG_G * (256 - brightness) / 256;
            int fogB = Config.FOG_B * (256 - brightness) / 256;

            // 5. Paint the column top to bottom. idx is the pixel position in the
            //    1D array: moving down one row means adding W (the screen width).
            int idx = x;

            for (int y = 0; y < drawStart; y++, idx += W) {
                pixels[idx] = backgroundColor(y, half);
            }

            // How far to move through the 64-pixel texture for each screen pixel.
            double texStep = (double) TEX / lineHeight;
            double texY = (drawStart - start) * texStep; // skips the clipped part
            for (int y = drawStart; y < drawEnd; y++, idx += W) {
                int color = tex[((int) texY % TEX) * TEX + texX];
                texY += texStep;
                // Split the color into red/green/blue (each 0-255), darken, add fog.
                int red = ((color >> 16) & 255) * wallLight / 256 + fogR;
                int green = ((color >> 8) & 255) * wallLight / 256 + fogG;
                int blue = (color & 255) * wallLight / 256 + fogB;
                pixels[idx] = (red << 16) | (green << 8) | blue; // pack back into one int
            }

            for (int y = drawEnd; y < H; y++, idx += W) {
                pixels[idx] = backgroundColor(y, half);
            }
        }
    }

    /** Doors are metal; other walls alternate between brick and stone by tile position. */
    private int chooseTexture() {
        if (map.getCell(hit.mapX, hit.mapY) == GameMap.DOOR) {
            return TextureFactory.METAL;
        }
        return (hit.mapX * 7 + hit.mapY * 13) % 2; // 0 = brick, 1 = stone
    }

    /** Ceiling color above the horizon row, floor color below it. */
    private int backgroundColor(int y, int half) {
        if (y < half) {
            return ceilingRows[y];
        }
        return floorRows[y - half];
    }
}

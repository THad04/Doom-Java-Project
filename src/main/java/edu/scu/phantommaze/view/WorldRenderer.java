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
public final class WorldRenderer {

    private static final int W = Config.RENDER_WIDTH;
    private static final int H = Config.RENDER_HEIGHT;
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
     * @param pixels target RGB pixel buffer of size RENDER_WIDTH * RENDER_HEIGHT
     * @param player the camera
     * @param zBuffer receives the perpendicular wall distance per screen column
     */
    public void render(int[] pixels, Player player, double[] zBuffer) {
        double px = player.getX();
        double py = player.getY();
        double dirX = player.getDirX();
        double dirY = player.getDirY();
        double planeX = player.getPlaneX();
        double planeY = player.getPlaneY();
        int half = H / 2;

        for (int x = 0; x < W; x++) {
            // cameraX sweeps -1 (left edge) .. +1 (right edge) across the screen.
            double cameraX = 2.0 * x / W - 1.0;
            double rayX = dirX + planeX * cameraX;
            double rayY = dirY + planeY * cameraX;
            raycaster.cast(px, py, rayX, rayY, hit);
            zBuffer[x] = hit.distance;

            // Wall height is inversely proportional to the (perpendicular) distance.
            int lineHeight = (int) (H / Math.max(hit.distance, 1e-4));
            int start = -lineHeight / 2 + half;
            int end = lineHeight / 2 + half;
            int drawStart = Math.max(0, start);
            int drawEnd = Math.min(H, end);

            // Pick a texture: doors are metal, other walls vary by tile position.
            int texId = map.getCell(hit.mapX, hit.mapY) == GameMap.DOOR
                    ? TextureFactory.METAL
                    : (hit.mapX * 7 + hit.mapY * 13) % 2;
            int[] tex = textures[texId];
            int texX = Math.min(TEX - 1, (int) (hit.textureX * TEX));

            // Fog: fade to the fog color with distance; y-sides are darker for depth cue.
            double fog = Math.max(0.0, 1.0 - hit.distance / Config.FOG_DISTANCE);
            fog *= fog;
            int fogI = (int) (256 * fog);
            double sideMul = hit.side == 1 ? Config.Y_SIDE_SHADE : 1.0;
            int lit = (int) (fogI * sideMul);
            int addR = (Config.FOG_R * (256 - fogI)) >> 8;
            int addG = (Config.FOG_G * (256 - fogI)) >> 8;
            int addB = (Config.FOG_B * (256 - fogI)) >> 8;

            // Step through the texture once per screen pixel; begin offset handles clipped columns.
            double step = (double) TEX / lineHeight;
            double texPos = (drawStart - start) * step;

            int idx = x;
            for (int y = 0; y < drawStart; y++, idx += W) {
                pixels[idx] = y < half ? ceilingRows[y] : floorRows[y - half];
            }
            for (int y = drawStart; y < drawEnd; y++, idx += W) {
                int c = tex[(((int) texPos) & (TEX - 1)) * TEX + texX];
                texPos += step;
                int r = (((c >> 16) & 255) * lit >> 8) + addR;
                int g = (((c >> 8) & 255) * lit >> 8) + addG;
                int b = ((c & 255) * lit >> 8) + addB;
                pixels[idx] = (r << 16) | (g << 8) | b;
            }
            for (int y = drawEnd; y < H; y++, idx += W) {
                pixels[idx] = y < half ? ceilingRows[y] : floorRows[y - half];
            }
        }
    }
}

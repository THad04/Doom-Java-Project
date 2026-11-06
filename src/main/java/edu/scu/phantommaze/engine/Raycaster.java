package edu.scu.phantommaze.engine;

import edu.scu.phantommaze.model.GameMap;

/** Pure-math DDA ray caster against a {@link GameMap}. Contains no Swing code. */
public final class Raycaster {

    private final GameMap map;

    /**
     * Creates a ray caster.
     *
     * @param map the map to cast against
     */
    public Raycaster(GameMap map) {
        this.map = map;
    }

    /**
     * Casts a ray and fills {@code out} with the first wall it hits.
     * The distance is measured perpendicular to the camera plane, which removes fisheye
     * distortion provided the ray is {@code dir + plane * cameraX}.
     *
     * @param posX ray origin x (tile units)
     * @param posY ray origin y
     * @param rayDirX ray direction x
     * @param rayDirY ray direction y
     * @param out result holder to fill and return
     * @return {@code out}
     */
    public RayHit cast(double posX, double posY, double rayDirX, double rayDirY, RayHit out) {
        // HOW THIS WORKS (big picture):
        // The map is a grid of square tiles. Starting inside one tile, we walk the ray
        // forward one tile at a time -- stepping across either a vertical grid line (x)
        // or a horizontal one (y), whichever the ray reaches first -- until we step into
        // a wall tile. How far we walked is the distance to that wall.

        // The tile the ray starts in (e.g. position 13.5 is inside tile 13).
        int mapX = (int) Math.floor(posX);
        int mapY = (int) Math.floor(posY);

        // Distance the ray travels to cross one full tile in x / y.
        double deltaDistX = rayDirX == 0 ? Double.MAX_VALUE : Math.abs(1.0 / rayDirX);
        double deltaDistY = rayDirY == 0 ? Double.MAX_VALUE : Math.abs(1.0 / rayDirY);

        int stepX;
        int stepY;
        double sideDistX; // distance from origin to the first x grid line
        double sideDistY;
        if (rayDirX < 0) {
            stepX = -1;
            sideDistX = (posX - mapX) * deltaDistX;
        } else {
            stepX = 1;
            sideDistX = (mapX + 1.0 - posX) * deltaDistX;
        }
        if (rayDirY < 0) {
            stepY = -1;
            sideDistY = (posY - mapY) * deltaDistY;
        } else {
            stepY = 1;
            sideDistY = (mapY + 1.0 - posY) * deltaDistY;
        }

        // The walking loop (called DDA). sideDistX / sideDistY are "how far along the ray
        // is the next vertical / horizontal grid line". Always cross the nearer one.
        // side remembers which kind of line we crossed last: 0 = vertical, 1 = horizontal.
        // DDA: always step across whichever grid line is closer, until a wall is entered.
        // Out-of-bounds counts as wall, so this always terminates.
        int side;
        do {
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX;
                mapX += stepX;
                side = 0;
            } else {
                sideDistY += deltaDistY;
                mapY += stepY;
                side = 1;
            }
        } while (!map.isWall(mapX, mapY));

        // We overshot by one delta, so subtract it to get the distance to the wall face.
        // This equals the distance projected onto the camera direction (no fisheye).
        double dist = side == 0 ? sideDistX - deltaDistX : sideDistY - deltaDistY;

        // Exact point where the ray struck the wall, as a fraction along the tile edge.
        double wallX = side == 0 ? posY + dist * rayDirY : posX + dist * rayDirX;
        wallX -= Math.floor(wallX);
        // Mirror on the faces seen from the "back" so the texture reads left-to-right.
        if ((side == 0 && rayDirX > 0) || (side == 1 && rayDirY < 0)) {
            wallX = 1.0 - wallX;
        }

        out.distance = dist;
        out.mapX = mapX;
        out.mapY = mapY;
        out.side = side;
        out.textureX = wallX;
        return out;
    }
}

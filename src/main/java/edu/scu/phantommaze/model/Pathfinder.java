package edu.scu.phantommaze.model;

import java.util.Arrays;

/**
 * Breadth-first search (BFS) over the tile grid.
 *
 * <p>{@link #distancesFrom} starts at one tile and spreads outward one step at a time,
 * writing into every reachable tile how many steps away from the start it is. An enemy
 * standing anywhere can then find the shortest way to the start tile by always stepping to
 * the neighbor with the smallest number.
 */
public class Pathfinder {

    /** Marks tiles that cannot be reached at all. */
    public static final int UNREACHABLE = Integer.MAX_VALUE;

    /** Column offsets of the four directions, in Pac-Man's tie-break order: up, left, down, right. */
    public static final int[] DX = { 0, -1, 0, 1 };
    /** Row offsets of the four directions (same order as {@link #DX}). */
    public static final int[] DY = { -1, 0, 1, 0 };

    private final GameMap map;
    private final int width;
    private final int height;
    private final int[] queue; // reused by every search, so searching allocates nothing

    /**
     * Creates a pathfinder for a map.
     *
     * @param map the map to search
     */
    public Pathfinder(GameMap map) {
        this.map = map;
        this.width = map.getWidth();
        this.height = map.getHeight();
        this.queue = new int[width * height];
    }

    /**
     * Creates an empty distance array the right size for this map.
     *
     * @return a new array with one slot per tile
     */
    public int[] newField() {
        int[] field = new int[width * height];
        Arrays.fill(field, UNREACHABLE);
        return field;
    }

    /**
     * Turns a tile position into its slot in a distance array.
     *
     * @param x tile column
     * @param y tile row
     * @return the array index
     */
    public int index(int x, int y) {
        return y * width + x;
    }

    /**
     * Tells whether an enemy may stand on a tile.
     *
     * @param x           tile column
     * @param y           tile row
     * @param throughDoor true if the pen door counts as open (only while leaving the pen)
     * @return true if walkable
     */
    public boolean isWalkable(int x, int y, boolean throughDoor) {
        char c = map.getCell(x, y); // out of bounds reads as a wall
        if (c == GameMap.WALL) {
            return false;
        }
        return throughDoor || (c != GameMap.DOOR && c != GameMap.PEN);
    }

    /**
     * Fills {@code field} with the walking distance (in steps) from every tile to the start tile.
     *
     * @param startX      start tile column
     * @param startY      start tile row
     * @param throughDoor true if paths may go through the pen door
     * @param field       array from {@link #newField()} to fill
     */
    public void distancesFrom(int startX, int startY, boolean throughDoor, int[] field) {
        Arrays.fill(field, UNREACHABLE);
        if (!isWalkable(startX, startY, throughDoor)) {
            return;
        }
        int head = 0; // next queue slot to read
        int tail = 0; // next queue slot to write
        int start = index(startX, startY);
        field[start] = 0;
        queue[tail++] = start;
        while (head < tail) {
            int current = queue[head++];
            int cx = current % width;
            int cy = current / width;
            for (int d = 0; d < 4; d++) {
                int nx = cx + DX[d];
                int ny = cy + DY[d];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height || !isWalkable(nx, ny, throughDoor)) {
                    continue;
                }
                int next = index(nx, ny);
                if (field[next] == UNREACHABLE) { // first visit is always the shortest
                    field[next] = field[current] + 1;
                    queue[tail++] = next;
                }
            }
        }
    }

    /**
     * Finds the maze tile closest to a point. Targets such as "4 tiles ahead of the player"
     * often land inside a wall or off the map; this snaps them back onto the maze floor
     * (never inside the pen).
     *
     * @param x desired tile column (may be outside the map)
     * @param y desired tile row (may be outside the map)
     * @return the nearest maze tile's {@link #index}, or {@link #UNREACHABLE} if there is none
     */
    public int nearestMazeTile(int x, int y) {
        if (isWalkable(x, y, false)) {
            return index(x, y);
        }
        int best = UNREACHABLE;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int ty = 0; ty < height; ty++) {
            for (int tx = 0; tx < width; tx++) {
                if (!isWalkable(tx, ty, false)) {
                    continue;
                }
                double dx = tx - (double) x;
                double dy = ty - (double) y;
                double distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    best = index(tx, ty);
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

}

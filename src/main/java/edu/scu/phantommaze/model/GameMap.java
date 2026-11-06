package edu.scu.phantommaze.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A rectangular tile grid parsed from text. Legend: {@code #} wall, {@code .} pellet,
 * {@code o} power pellet, {@code P} player start, {@code G} ghost pen tile,
 * {@code -} pen door, space = empty.
 */
public class GameMap {

    /** Character for a wall tile. */
    public static final char WALL = '#';
    /** Character for the pen door. */
    public static final char DOOR = '-';

    private final char[][] cells; // indexed [y][x]
    private final int width;
    private final int height;
    private Vector2 playerStart = new Vector2(1.5, 1.5);
    private final List<int[]> penTiles = new ArrayList<>();
    private int pelletCount;

    /**
     * Builds a map from lines of text.
     *
     * @param lines one string per row; shorter rows are padded with empty tiles
     */
    public GameMap(List<String> lines) {
        height = lines.size();
        int w = 0;
        for (String line : lines) {
            w = Math.max(w, line.length());
        }
        width = w;
        cells = new char[height][width];
        for (int y = 0; y < height; y++) {
            String line = lines.get(y);
            for (int x = 0; x < width; x++) {
                char c = x < line.length() ? line.charAt(x) : ' ';
                if (c == 'P') {
                    playerStart = new Vector2(x + 0.5, y + 0.5);
                    c = ' ';
                } else if (c == 'G') {
                    penTiles.add(new int[] {x, y});
                } else if (c == '.' || c == 'o') {
                    pelletCount++;
                }
                cells[y][x] = c;
            }
        }
    }

    /**
     * Loads a map from a classpath resource.
     *
     * @param resourcePath absolute resource path, e.g. {@code /maps/level1.txt}
     * @return the parsed map
     * @throws IllegalArgumentException if the resource is missing or unreadable
     */
    public static GameMap fromResource(String resourcePath) {
        try (InputStream in = GameMap.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Map not found: " + resourcePath);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
            return new GameMap(lines);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read map: " + resourcePath, e);
        }
    }

    /**
     * Returns the map width.
     *
     * @return width in tiles
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the map height.
     *
     * @return height in tiles
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the raw character at a tile; anything out of bounds is a wall.
     *
     * @param x tile column
     * @param y tile row
     * @return the tile character
     */
    public char getCell(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return WALL;
        }
        return cells[y][x];
    }

    /**
     * Tells whether a tile blocks movement and rays (walls, the pen door, and out-of-bounds).
     *
     * @param x tile column
     * @param y tile row
     * @return true if solid
     */
    public boolean isWall(int x, int y) {
        char c = getCell(x, y);
        return c == WALL || c == DOOR;
    }

    /**
     * Returns the player start position (center of the start tile).
     *
     * @return the start position
     */
    public Vector2 getPlayerStart() {
        return playerStart;
    }

    /**
     * Returns the ghost pen tiles as {x, y} pairs.
     *
     * @return the pen tiles
     */
    public List<int[]> getPenTiles() {
        return penTiles;
    }

    /**
     * Returns the number of pellets (normal and power) the map started with.
     *
     * @return the pellet count
     */
    public int getPelletCount() {
        return pelletCount;
    }
}

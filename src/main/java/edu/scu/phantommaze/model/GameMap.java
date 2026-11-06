package edu.scu.phantommaze.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A rectangular tile grid parsed from text. Legend: {@code #} wall, {@code .}
 * pellet,
 * {@code o} power pellet, {@code P} player start, {@code G} ghost pen tile,
 * {@code -} pen door, space = empty.
 */
public class GameMap {

    /** Character for a wall tile. */
    public static final char WALL = '#';
    /** Character for the pen door. */
    public static final char DOOR = '-';

    private final char[][] cells; // indexed [y][x] row then column
    private final int width; // columns
    private final int height; // rows
    private Vector2 playerStart = new Vector2(1.5, 1.5); // default start if theres no P
    private final List<int[]> penTiles = new ArrayList<>(); // positions of G tiles
    private int pelletCount; // # of . and o

    /**
     * Builds a map from lines of text.
     *
     * @param lines one string per row; shorter rows are padded with empty tiles
     */
    public GameMap(List<String> lines) { // goes through every char of every line
        height = lines.size(); // get height of map (map1: 31)
        int w = 0;
        for (String line : lines) {
            w = Math.max(w, line.length()); // keep longest line seen
        }
        width = w; // set width as longest (map 1: 28)
        cells = new char[height][width]; // width is length of longest line

        // visit every tile
        for (int y = 0; y < height; y++) { // visit rows
            String line = lines.get(y);
            for (int x = 0; x < width; x++) { // visit columns
                char c = x < line.length() ? line.charAt(x) : ' '; // set char as char at location until you pass
                                                                   // length, then ' '
                if (c == 'P') {
                    playerStart = new Vector2(x + 0.5, y + 0.5); // set player start
                    c = ' '; // make location blank
                } else if (c == 'G') { // remembers positions for ghosts
                    penTiles.add(new int[] { x, y });
                } else if (c == '.' || c == 'o') { // iterates pellet count
                    pelletCount++;
                }
                cells[y][x] = c; // mark locations of each char in 2d array
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
        try (InputStream in = GameMap.class.getResourceAsStream(resourcePath)) { // look for map in filepath
            if (in == null) { // if theres no map given, err
                throw new IllegalArgumentException("Map not found: " + resourcePath);
            }
            // reads the file
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) { // add if there is something on line
                    lines.add(line);
                }
            }
            return new GameMap(lines); // creates the gamemap and runs constructor above
        } catch (IOException e) { // catch-all style exception
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
        if (x < 0 || y < 0 || x >= width || y >= height) { // anything out of bounds is a wall
            return WALL;
        }
        return cells[y][x]; // return the char at the specified position
    }

    /**
     * Tells whether a tile blocks movement and rays (walls, the pen door, and
     * out-of-bounds).
     *
     * @param x tile column
     * @param y tile row
     * @return true if solid
     */
    public boolean isWall(int x, int y) {
        char c = getCell(x, y);
        return c == WALL || c == DOOR; // checks if its a wall or door
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

package edu.scu.phantommaze.view;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Loads the Freedoom pictures from {@code src/main/resources/freedoom/} and remembers them,
 * so each file is read from disk only once.
 */
public final class Assets {

    private static final String ROOT = "/freedoom/";
    private static final Map<String, int[]> OFFSETS = loadOffsets();
    private static final Map<String, Patch> CACHE = new HashMap<>();

    private Assets() {
    }

    /**
     * Loads a sprite (monsters, weapons, pickups) by its Doom name, e.g. {@code "trooa1"}.
     *
     * @param name lower-case Doom name without extension
     * @return the patch
     */
    public static Patch sprite(String name) {
        return load("sprites/" + name);
    }

    /**
     * Loads a status bar or font graphic by its Doom name, e.g. {@code "stbar"}.
     *
     * @param name lower-case Doom name without extension
     * @return the patch
     */
    public static Patch graphic(String name) {
        return load("graphics/" + name);
    }

    /**
     * Tells whether a sprite file exists (used to discover which angles a monster has).
     *
     * @param name lower-case Doom name without extension
     * @return true if the file is in the resources
     */
    public static boolean spriteExists(String name) {
        return Assets.class.getResource(ROOT + "sprites/" + name + ".png") != null;
    }

    private static Patch load(String path) {
        return CACHE.computeIfAbsent(path, Assets::read);
    }

    private static Patch read(String path) {
        URL url = Assets.class.getResource(ROOT + path + ".png");
        if (url == null) {
            throw new IllegalArgumentException("Missing image: " + ROOT + path + ".png");
        }
        try {
            BufferedImage source = ImageIO.read(url);
            if (source == null) {
                throw new IOException("not a readable image");
            }
            // Copy into a plain ARGB image so every picture has the same pixel format.
            BufferedImage argb = new BufferedImage(source.getWidth(), source.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = argb.createGraphics();
            g.drawImage(source, 0, 0, null);
            g.dispose();
            String name = path.substring(path.lastIndexOf('/') + 1);
            int[] offsets = OFFSETS.getOrDefault(name, new int[] { 0, 0 });
            return new Patch(argb, offsets[0], offsets[1]);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read image " + path, e);
        }
    }

    /** Reads offsets.txt: one "name leftOffset topOffset" line per picture. */
    private static Map<String, int[]> loadOffsets() {
        Map<String, int[]> offsets = new HashMap<>();
        try (InputStream in = Assets.class.getResourceAsStream(ROOT + "offsets.txt")) {
            if (in == null) {
                throw new IllegalStateException("Missing " + ROOT + "offsets.txt");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 3 && !line.startsWith("#")) {
                    offsets.put(parts[0], new int[] { Integer.parseInt(parts[1]), Integer.parseInt(parts[2]) });
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read offsets.txt", e);
        }
        return offsets;
    }
}

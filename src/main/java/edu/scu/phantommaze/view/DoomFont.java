package edu.scu.phantommaze.view;

import java.awt.Graphics2D;

/**
 * Doom's small red message font (the "STCFN" pictures). It only has capital letters,
 * digits and punctuation, so lower-case text is drawn in capitals.
 */
public final class DoomFont {

    private static final char FIRST = '!';
    private static final char LAST = '_';
    private static final int SPACE_WIDTH = 4;
    private static final int LINE_HEIGHT = 9;
    private static final Patch[] GLYPHS = new Patch[LAST - FIRST + 1];

    static {
        for (char c = FIRST; c <= LAST; c++) {
            GLYPHS[c - FIRST] = Assets.graphic(String.format("stcfn%03d", (int) c));
        }
    }

    private DoomFont() {
    }

    /**
     * Width of a piece of text in font pixels (multiply by the scale for screen pixels).
     *
     * @param text the text
     * @return width at scale 1
     */
    public static int width(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Patch glyph = glyph(c);
            w += glyph == null ? SPACE_WIDTH : glyph.width;
        }
        return w;
    }

    /**
     * Height of one line in font pixels.
     *
     * @return line height at scale 1
     */
    public static int lineHeight() {
        return LINE_HEIGHT;
    }

    /**
     * Draws text with its top-left corner at (x, y).
     *
     * @param g     where to draw
     * @param text  the text
     * @param x     left edge
     * @param y     top edge
     * @param scale size multiplier (each font pixel becomes scale x scale pixels)
     */
    public static void draw(Graphics2D g, String text, int x, int y, int scale) {
        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Patch glyph = glyph(c);
            if (glyph == null) {
                cursor += SPACE_WIDTH * scale;
                continue;
            }
            g.drawImage(glyph.image, cursor, y, glyph.width * scale, glyph.height * scale, null);
            cursor += glyph.width * scale;
        }
    }

    /**
     * Draws text horizontally centered on a point.
     *
     * @param g       where to draw
     * @param text    the text
     * @param centerX horizontal center
     * @param y       top edge
     * @param scale   size multiplier
     */
    public static void drawCentered(Graphics2D g, String text, int centerX, int y, int scale) {
        draw(g, text, centerX - width(text) * scale / 2, y, scale);
    }

    /** Looks up a glyph; lower-case letters use the capitals. Null for a space or an unknown character. */
    private static Patch glyph(char c) {
        char upper = Character.toUpperCase(c);
        if (upper < FIRST || upper > LAST) {
            return null;
        }
        return GLYPHS[upper - FIRST];
    }
}

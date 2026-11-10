package edu.scu.phantommaze.view;

import java.awt.image.BufferedImage;

/**
 * One Doom picture ("patch" in Doom terms): a sprite frame, a status bar digit, a face...
 *
 * <p>Doom pictures carry two offsets that say where the picture's anchor point is. For a
 * monster, {@code leftOffset} is how many pixels from the left edge its middle is, and
 * {@code topOffset} is how many pixels above the floor its top is. Using them keeps every
 * animation frame lined up even though the frames have different sizes.
 */
public final class Patch {

    /** Width in pixels. */
    public final int width;
    /** Height in pixels. */
    public final int height;
    /** Pixels from the left edge to the anchor point. */
    public final int leftOffset;
    /** Pixels from the top edge to the anchor point. */
    public final int topOffset;
    /** Pixels row by row, packed as 0xAARRGGBB (alpha 0 = transparent). */
    public final int[] argb;
    /** The same pixels as an image, for drawing with Graphics2D. */
    public final BufferedImage image;

    /**
     * Wraps an image.
     *
     * @param image      an ARGB image
     * @param leftOffset pixels from the left edge to the anchor
     * @param topOffset  pixels from the top edge to the anchor
     */
    public Patch(BufferedImage image, int leftOffset, int topOffset) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.leftOffset = leftOffset;
        this.topOffset = topOffset;
        this.argb = image.getRGB(0, 0, width, height, null, 0, width);
    }

    /**
     * Makes a left-right flipped copy. Doom stores only one side of a monster for some
     * angles and flips it for the other side. The offsets stay the same, as in Doom.
     *
     * @return the flipped patch
     */
    public Patch mirrored() {
        BufferedImage flipped = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                flipped.setRGB(width - 1 - x, y, argb[y * width + x]);
            }
        }
        return new Patch(flipped, leftOffset, topOffset);
    }
}

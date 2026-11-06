package edu.scu.phantommaze.model;

/** A 2D point or direction with an x and a y value. */
public class Vector2 {

    /** The x component. */
    public final double x;
    /** The y component. */
    public final double y;

    /**
     * Creates a vector.
     *
     * @param x the x component
     * @param y the y component
     */
    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

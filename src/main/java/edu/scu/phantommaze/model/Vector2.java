package edu.scu.phantommaze.model;

/** Immutable 2D vector of doubles. */
public final class Vector2 {

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

    /**
     * Returns the sum of this vector and another.
     *
     * @param o the other vector
     * @return this + o
     */
    public Vector2 add(Vector2 o) {
        return new Vector2(x + o.x, y + o.y);
    }

    /**
     * Returns the difference of this vector and another.
     *
     * @param o the other vector
     * @return this - o
     */
    public Vector2 subtract(Vector2 o) {
        return new Vector2(x - o.x, y - o.y);
    }

    /**
     * Returns this vector multiplied by a scalar.
     *
     * @param s the scalar
     * @return this * s
     */
    public Vector2 scale(double s) {
        return new Vector2(x * s, y * s);
    }

    /**
     * Returns the dot product with another vector.
     *
     * @param o the other vector
     * @return the dot product
     */
    public double dot(Vector2 o) {
        return x * o.x + y * o.y;
    }

    /**
     * Returns the Euclidean length.
     *
     * @return the length
     */
    public double length() {
        return Math.hypot(x, y);
    }

    /**
     * Returns the distance to another point.
     *
     * @param o the other point
     * @return the distance
     */
    public double distanceTo(Vector2 o) {
        return Math.hypot(x - o.x, y - o.y);
    }

    /**
     * Returns this vector rotated counter-clockwise (in math convention) by an angle.
     *
     * @param radians the angle in radians
     * @return the rotated vector
     */
    public Vector2 rotate(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        return new Vector2(x * c - y * s, x * s + y * c);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Vector2)) {
            return false;
        }
        Vector2 o = (Vector2) obj;
        return Double.compare(x, o.x) == 0 && Double.compare(y, o.y) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(x) + Double.hashCode(y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}

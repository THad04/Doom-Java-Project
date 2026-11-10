package edu.scu.phantommaze.model;

/** A BFG explosion left where an enemy was blasted. It only exists to be drawn. */
public class Blast {

    private final double x;
    private final double y;
    private double age;

    Blast(double x, double y) {
        this.x = x;
        this.y = y;
    }

    void tick(double dt) {
        age += dt;
    }

    /** @return x position in tiles */
    public double getX() {
        return x;
    }

    /** @return y position in tiles */
    public double getY() {
        return y;
    }

    /** @return seconds since the explosion started */
    public double getAge() {
        return age;
    }
}

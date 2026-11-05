package edu.scu.phantommaze.engine;

/** Mutable result of a ray cast; reused between casts to avoid per-frame allocation. */
public final class RayHit {

    /** Perpendicular (fisheye-corrected) distance to the wall. */
    public double distance;
    /** Column of the wall tile that was hit. */
    public int mapX;
    /** Row of the wall tile that was hit. */
    public int mapY;
    /** 0 if a vertical (x-facing) wall side was hit, 1 if a horizontal (y-facing) side. */
    public int side;
    /** Horizontal texture coordinate in [0, 1), already mirrored so textures never appear flipped. */
    public double textureX;
}

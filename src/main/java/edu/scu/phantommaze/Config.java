package edu.scu.phantommaze;

/**
 * Central place for every tunable constant in the game (resolution, speeds,
 * colors, ...), so no magic numbers are scattered through the code.
 */
public final class Config {

    /** Width of the off-screen render buffer in pixels. */
    public static final int RENDER_WIDTH = 640;
    /** Height of the off-screen render buffer in pixels. */
    public static final int RENDER_HEIGHT = 400;
    /** Width of the game window in pixels. */
    public static final int WINDOW_WIDTH = 1280;
    /** Height of the game window in pixels. */
    public static final int WINDOW_HEIGHT = 800;

    /** Horizontal field of view in degrees. */
    public static final double FOV_DEGREES = 66.0;
    /** Length of the camera plane vector (tan of half the FOV, with a unit direction vector). */
    public static final double CAMERA_PLANE_LENGTH = Math.tan(Math.toRadians(FOV_DEGREES / 2.0));

    /** Edge length of the square wall textures in pixels. */
    public static final int TEXTURE_SIZE = 64;

    /** Player movement speed in tiles per second. */
    public static final double PLAYER_MOVE_SPEED = 3.2;
    /** Player turning speed in radians per second. */
    public static final double PLAYER_TURN_SPEED = 2.4;
    /** Player collision radius in tiles. */
    public static final double PLAYER_RADIUS = 0.25;

    /** Target frames (and fixed updates) per second. */
    public static final int TARGET_FPS = 60;
    /** Largest amount of simulated time processed in one frame, in seconds. */
    public static final double MAX_FRAME_TIME = 0.25;

    /** Distance (in tiles) at which walls fade completely into the fog. */
    public static final double FOG_DISTANCE = 12.0;
    /** Brightness multiplier (0-1) applied to walls hit on their y-side. */
    public static final double Y_SIDE_SHADE = 0.7;
    /** Fog color, red channel. */
    public static final int FOG_R = 8;
    /** Fog color, green channel. */
    public static final int FOG_G = 4;
    /** Fog color, blue channel. */
    public static final int FOG_B = 18;

    /** Ceiling color at the top of the screen (RGB). */
    public static final int CEILING_TOP = 0x050310;
    /** Ceiling color at the horizon (RGB). */
    public static final int CEILING_HORIZON = 0x241a3a;
    /** Floor color at the horizon (RGB). */
    public static final int FLOOR_HORIZON = 0x15101e;
    /** Floor color at the bottom of the screen (RGB). */
    public static final int FLOOR_BOTTOM = 0x3a3040;

    /** Size of one minimap tile in window pixels. */
    public static final int MINIMAP_TILE = 7;
    /** Margin between the minimap and the window edge in pixels. */
    public static final int MINIMAP_MARGIN = 12;

    private Config() {
        // constants holder, not instantiable
    }
}

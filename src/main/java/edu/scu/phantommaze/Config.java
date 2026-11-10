package edu.scu.phantommaze;

/**
 * Central place for every tunable constant in the game (resolution, speeds,
 * colors, ...), so no magic numbers are scattered through the code.
 */
public class Config {

    /** Width of the off-screen render buffer in pixels. */
    public static final int RENDER_WIDTH = 640;
    /** Height of the off-screen render buffer in pixels (3D view + status bar). */
    public static final int RENDER_HEIGHT = 400;
    /** Width of the game window in pixels. */
    public static final int WINDOW_WIDTH = 1280;
    /** Height of the game window in pixels. */
    public static final int WINDOW_HEIGHT = 800;

    /**
     * Size multiplier for the Doom HUD graphics. Doom drew them on a 320x200 screen;
     * our render buffer is 640x400, so everything is drawn twice as big.
     */
    public static final int HUD_SCALE = 2;
    /** Height of the Doom status bar in render-buffer pixels (32 Doom pixels tall). */
    public static final int STATUS_BAR_HEIGHT = 32 * HUD_SCALE;
    /** Height of the 3D view: everything above the status bar. */
    public static final int VIEW_HEIGHT = RENDER_HEIGHT - STATUS_BAR_HEIGHT;
    /**
     * On-screen height in pixels of a wall that is exactly one tile away. Walls and
     * sprites both use this, so they stay the same size relative to each other.
     */
    public static final double PROJECTION_SCALE = RENDER_HEIGHT;

    /** Horizontal field of view in degrees. */
    public static final double FOV_DEGREES = 66.0;
    /**
     * Length of the camera plane vector (tan of half the FOV, with a unit direction
     * vector).
     */
    public static final double CAMERA_PLANE_LENGTH = Math.tan(Math.toRadians(FOV_DEGREES / 2.0));

    /** Edge length of the square wall textures in pixels. */
    public static final int TEXTURE_SIZE = 64;

    /** Player movement speed in tiles per second. */
    public static final double PLAYER_MOVE_SPEED = 3.2;
    /** Movement speed is multiplied by this while the shotgun power-up lasts, to chase fleeing enemies. */
    public static final double POWER_UP_SPEED_MULTIPLIER = 1.5;
    /** Player turning speed (arrow keys) in radians per second. */
    public static final double PLAYER_TURN_SPEED = 2.4;
    /** Player collision radius in tiles. */
    public static final double PLAYER_RADIUS = 0.25;
    /** Number of hits the player can take: health goes 100% -> 67% -> 33% -> 0%. */
    public static final int PLAYER_LIVES = 3;
    /** Seconds the game freezes (red screen) after the player is hit. */
    public static final double HIT_FREEZE_TIME = 1.5;
    /** Seconds the player cannot be hurt after respawning. */
    public static final double RESPAWN_GRACE_TIME = 2.0;

    /** Mouse turning in radians per pixel moved, at sensitivity 1 (the menu slider goes 1-10). */
    public static final double MOUSE_RADIANS_PER_PIXEL = 0.0006;
    /** Default mouse sensitivity on the menu slider. */
    public static final int DEFAULT_MOUSE_SENSITIVITY = 5;

    /** Bullets in one pistol clip. */
    public static final int PISTOL_CLIP_SIZE = 3;
    /** Minimum seconds between pistol shots. */
    public static final double PISTOL_FIRE_DELAY = 0.45;
    /** Seconds a pistol reload takes. */
    public static final double PISTOL_RELOAD_TIME = 1.5;
    /** How far (in tiles) from the aim line a pistol bullet still counts as a hit. */
    public static final double PISTOL_HIT_RADIUS = 0.35;
    /** Pistol hits needed to send an enemy back to the pen. */
    public static final int PISTOL_HITS_TO_ELIMINATE = 3;
    /** Shotgun shells given by each power-up. */
    public static final int SHOTGUN_SHELLS = 50;
    /** Minimum seconds between shotgun shots (time for the pump animation). */
    public static final double SHOTGUN_FIRE_DELAY = 0.9;
    /** How far (in tiles) from the aim line a shotgun blast still counts as a hit. */
    public static final double SHOTGUN_HIT_RADIUS = 0.5;
    /**
     * Maximum shooting distance in tiles. Doom's hitscan range is 2048 units = 32 tiles,
     * which is longer than our maps, so in practice there is no limit.
     */
    public static final double HITSCAN_RANGE = 32.0;

    /** Distance in tiles between the player and an enemy that counts as being caught. */
    public static final double CATCH_DISTANCE = 0.55;
    /** Seconds an enemy's death animation plays before it reappears in the pen. */
    public static final double ENEMY_DEATH_TIME = 0.9;
    /** Seconds an enemy stops moving after a non-lethal pistol hit. */
    public static final double ENEMY_PAIN_TIME = 0.35;
    /** Enemies further than this (walking distance, in tiles) are silent. */
    public static final int GROWL_RADIUS = 8;
    /** Shortest wait in seconds between two growls from the same enemy. */
    public static final double GROWL_MIN_INTERVAL = 2.5;
    /** Extra random wait (0 to this many seconds) added between growls. */
    public static final double GROWL_RANDOM_INTERVAL = 2.0;
    /** Doomguy's face glances toward enemies closer than this (in tiles). */
    public static final double FACE_ALERT_RADIUS = 6.0;
    /**
     * "Sixth sense": enemies this close (walking distance in tiles) that the player cannot see make
     * the screen edge glow red on the side they will come from.
     */
    public static final int SENSE_RADIUS = 5;

    /** Size of one sprite pixel in tiles (an imp, 56 pixels tall, ends up 83% of a wall). */
    public static final double SPRITE_PIXEL_SIZE = 1.0 / 100.0;
    /**
     * Doom's pixels were 1.2 times taller than wide on a 4:3 monitor; sprites are
     * stretched by this much vertically so they keep their original look.
     */
    public static final double SPRITE_ASPECT = 1.2;

    /** Target frames (and updates) per second. */
    public static final int TARGET_FPS = 60;
    /** Longest time step the simulation will take at once, so a hiccup cannot teleport things. */
    public static final double MAX_TIME_STEP = 0.1;

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
}

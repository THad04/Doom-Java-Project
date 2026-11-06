package edu.scu.phantommaze.model;

import edu.scu.phantommaze.Config;

/**
 * The player: position, facing, and camera plane, with wall-sliding movement.
 * Input is expressed as intents (-1..1) set by the controller before each
 * update.
 */
public class Player {

    private double x; // x coordinate position of player
    private double y; // y coordinate position of player
    private double angle; // which way you face in radians

    // What the keys are asking for right now
    private double forwardIntent;
    private double strafeIntent;
    private double turnIntent;

    /**
     * Creates a player.
     *
     * @param start the starting position in tile coordinates
     * @param angle the initial facing angle in radians (0 = east, positive turns
     *              clockwise on screen)
     */
    public Player(Vector2 start, double angle) {
        this.x = start.x; // set starting x coordinate
        this.y = start.y; // set starting y coordinate
        this.angle = angle; // set starting angle
    }

    /**
     * Sets the current movement intents.
     *
     * @param forward -1 (back) to 1 (forward)
     * @param strafe  -1 (left) to 1 (right)
     * @param turn    -1 (turn left) to 1 (turn right)
     */
    public void setIntents(double forward, double strafe, double turn) { // updates every tick()
        this.forwardIntent = forward;
        this.strafeIntent = strafe;
        this.turnIntent = turn;
    }

    /**
     * Advances the player by one time step, turning and moving with collision.
     *
     * @param dt  seconds elapsed
     * @param map the map to collide with
     */
    public void update(double dt, GameMap map) {
        // uses turn intent along with turn speed and time elapsed to change angle
        angle += turnIntent * Config.PLAYER_TURN_SPEED * dt;

        // X increases to the East. Y increases to the South
        // Angle 0 = Facing East
        // East: 0 degrees = (1,0)
        // South: 90 degrees = (0, 1)
        // West: 180 degrees = (-1, 0)
        // North: 240 degrees = (0, -1)
        double dirX = getDirX(); // cos(0) = 1
        double dirY = getDirY(); // sin(0) = 0
        // Right-hand vector in screen coordinates (y points down).
        double rightX = -dirY; // -(0) = 0
        double rightY = dirX; // 1

        // Combined forward + strafe direction
        double vx = dirX * forwardIntent + rightX * strafeIntent; // velocity in x-direction
        double vy = dirY * forwardIntent + rightY * strafeIntent; // velcocity in y-direction
        double len = Math.hypot(vx, vy); // how far you would you move in total / length of hypontenus
        if (len > 1.0) { // keep diagonal movement from being faster
            // ex: sqrt(1^2 + 1^2) = 1.41
            vx /= len; // ensures movement diagonal is the same speed as movement straight
            vy /= len;
        }
        // mutliply vx and vy by movement speed and time since last frame
        move(vx * Config.PLAYER_MOVE_SPEED * dt, vy * Config.PLAYER_MOVE_SPEED * dt, map);
    }

    /**
     * Moves by a displacement, resolving each axis separately so the player
     * slides along walls instead of sticking to them.
     *
     * @param dx  displacement along x
     * @param dy  displacement along y
     * @param map the map to collide with
     */
    public void move(double dx, double dy, GameMap map) {
        if (!collides(x + dx, y, map)) { // checks that x spot isnt inside a wall before movement
            x += dx;
        }
        if (!collides(x, y + dy, map)) { // checks that y spot isnt inside a wall before movement
            y += dy;
        }
    }

    /**
     * Tests the four corners of the player's bounding square against solid tiles.
     */
    private boolean collides(double px, double py, GameMap map) {
        double r = Config.PLAYER_RADIUS; // how close player can get to walls
        // uses floor() to reduce from decimal to whole number for calculation
        return map.isWall((int) Math.floor(px - r), (int) Math.floor(py - r)) // top left
                || map.isWall((int) Math.floor(px + r), (int) Math.floor(py - r)) // top right
                || map.isWall((int) Math.floor(px - r), (int) Math.floor(py + r)) // bottom left
                || map.isWall((int) Math.floor(px + r), (int) Math.floor(py + r)); // bottom right
    }

    /**
     * Returns the x position.
     *
     * @return x in tiles
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the y position.
     *
     * @return y in tiles
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the facing angle.
     *
     * @return angle in radians
     */
    public double getAngle() {
        return angle;
    }

    /**
     * Returns the x component of the unit facing vector.
     *
     * @return dir x
     */
    public double getDirX() {
        return Math.cos(angle);
    }

    /**
     * Returns the y component of the unit facing vector.
     *
     * @return dir y
     */
    public double getDirY() {
        return Math.sin(angle);
    }

    /**
     * Returns the x component of the camera plane (perpendicular to facing, scaled
     * by FOV).
     *
     * @return plane x
     */
    public double getPlaneX() {
        return -getDirY() * Config.CAMERA_PLANE_LENGTH;
    }

    /**
     * Returns the y component of the camera plane.
     *
     * @return plane y
     */
    public double getPlaneY() {
        return getDirX() * Config.CAMERA_PLANE_LENGTH;
    }
}

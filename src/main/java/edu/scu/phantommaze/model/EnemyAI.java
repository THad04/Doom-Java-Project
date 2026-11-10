package edu.scu.phantommaze.model;

import java.util.List;

/**
 * Moves enemies through the maze using the original Pac-Man ghost rules:
 * <ul>
 * <li>Enemies walk from tile center to tile center and only choose a new direction when
 * they reach a center.</li>
 * <li>They never turn straight around unless they hit a dead end (or the game tells them
 * to with {@link Enemy#reverse()}).</li>
 * <li>Each enemy picks a <em>target tile</em> and takes the step that leads there in the
 * fewest steps. The four enemies pick different targets, which is what makes them hunt
 * as a team instead of following each other in a line.</li>
 * </ul>
 */
class EnemyAI {

    /** The imp (Clyde) only chases when it is further than this many tiles from the player. */
    private static final double IMP_SHY_DISTANCE = 8.0;
    /** The demon (Pinky) aims this many tiles in front of the player. */
    private static final double DEMON_LOOK_AHEAD = 4.0;
    /** The lost soul (Inky) uses the point this many tiles in front of the player. */
    private static final double LOST_SOUL_LOOK_AHEAD = 2.0;

    private final Pathfinder pathfinder;
    private final int width;
    private final Player player;
    private final List<Enemy> enemies;
    private final int exitX; // tile just outside the pen door
    private final int exitY;
    private final int[] exitField; // steps to the pen exit (door counted as open)
    private final int[] playerField; // steps to the player's tile (door closed)
    private final int[] scratchField; // reused for every other target
    private final int[] homeCorners = new int[EnemyType.values().length];
    private final int[][] homeFields = new int[homeCorners.length][];
    private int scratchTarget = Pathfinder.UNREACHABLE;
    private int playerTileX = -1;
    private int playerTileY = -1;

    /**
     * Creates the AI.
     *
     * @param map     the maze
     * @param player  the player being hunted
     * @param enemies all enemies (the lost soul needs to know where the cacodemon is)
     * @param penExit {x, y} of the tile just outside the pen door
     */
    EnemyAI(GameMap map, Player player, List<Enemy> enemies, int[] penExit) {
        this.pathfinder = new Pathfinder(map);
        this.width = map.getWidth();
        this.player = player;
        this.enemies = enemies;
        this.exitX = penExit[0];
        this.exitY = penExit[1];
        this.exitField = pathfinder.newField();
        this.playerField = pathfinder.newField();
        this.scratchField = pathfinder.newField();
        pathfinder.distancesFrom(exitX, exitY, true, exitField);
        for (EnemyType type : EnemyType.values()) {
            int cornerX = type.homeRight ? map.getWidth() - 1 : 0;
            int cornerY = type.homeBottom ? map.getHeight() - 1 : 0;
            homeCorners[type.ordinal()] = pathfinder.nearestMazeTile(cornerX, cornerY);
            int corner = homeCorners[type.ordinal()];
            int[] field = pathfinder.newField();
            pathfinder.distancesFrom(corner % width, corner / width, false, field);
            homeFields[type.ordinal()] = field;
        }
        updatePlayer();
    }

    /** Recomputes the distances to the player, but only when the player entered a new tile. */
    void updatePlayer() {
        int tx = (int) Math.floor(player.getX());
        int ty = (int) Math.floor(player.getY());
        if (tx != playerTileX || ty != playerTileY) {
            playerTileX = tx;
            playerTileY = ty;
            pathfinder.distancesFrom(tx, ty, false, playerField);
        }
    }

    /**
     * Walking distance from an enemy to the player.
     *
     * @param e the enemy
     * @return steps along the maze, or {@link Pathfinder#UNREACHABLE} (e.g. inside the pen)
     */
    int walkingDistanceToPlayer(Enemy e) {
        return playerField[pathfinder.index(e.getTileX(), e.getTileY())];
    }

    /**
     * One step along the shortest walk from a tile toward the player.
     *
     * @param x tile column
     * @param y tile row
     * @return index of the neighbouring tile one step closer, or the tile's own index if it is
     *         the player's tile or cannot reach the player
     */
    int stepTowardPlayer(int x, int y) {
        int tile = pathfinder.index(x, y);
        int steps = playerField[tile];
        if (steps == 0 || steps == Pathfinder.UNREACHABLE) {
            return tile;
        }
        for (int d = 0; d < Pathfinder.DX.length; d++) {
            int nx = x + Pathfinder.DX[d];
            int ny = y + Pathfinder.DY[d];
            if (pathfinder.isWalkable(nx, ny, false) && playerField[pathfinder.index(nx, ny)] == steps - 1) {
                return pathfinder.index(nx, ny);
            }
        }
        return tile;
    }

    /**
     * Moves an enemy along the maze, turning at every tile center it reaches.
     *
     * @param e           the enemy (leaving the pen or active)
     * @param distance    how many tiles to move this tick
     * @param scatterMode true if non-frightened enemies should head to their home corners
     */
    void move(Enemy e, double distance, boolean scatterMode) {
        if (e.getState() == Enemy.State.LEAVING_PEN && e.getTileX() == exitX && e.getTileY() == exitY
                && e.getX() == exitX + 0.5 && e.getY() == exitY + 0.5) {
            e.becomeActive();
        }
        while (distance > 1e-9) {
            boolean leaving = e.getState() == Enemy.State.LEAVING_PEN;
            boolean atCenter = e.getX() == e.getTileX() + 0.5 && e.getY() == e.getTileY() + 0.5;
            if ((e.getDirX() == 0 && e.getDirY() == 0)
                    || (atCenter && !pathfinder.isWalkable(e.getTileX() + e.getDirX(),
                            e.getTileY() + e.getDirY(), leaving))) {
                chooseDirection(e, scatterMode);
                if (e.getDirX() == 0 && e.getDirY() == 0) {
                    return; // boxed in, nowhere to go
                }
            }
            int nextX = e.getTileX() + e.getDirX();
            int nextY = e.getTileY() + e.getDirY();
            double toCenter = Math.abs(nextX + 0.5 - e.getX()) + Math.abs(nextY + 0.5 - e.getY());
            if (toCenter > distance) {
                e.advance(distance); // will not reach the next center this tick
                return;
            }
            e.arriveAt(nextX, nextY);
            distance -= toCenter;
            if (e.getState() == Enemy.State.LEAVING_PEN && nextX == exitX && nextY == exitY) {
                e.becomeActive();
            }
            chooseDirection(e, scatterMode);
            if (e.getDirX() == 0 && e.getDirY() == 0) {
                return; // no next step: do not repeatedly arrive at the same center
            }
        }
    }

    /** Picks the direction to walk from the tile center the enemy is standing on. */
    private void chooseDirection(Enemy e, boolean scatterMode) {
        boolean leaving = e.getState() == Enemy.State.LEAVING_PEN;
        boolean flee = !leaving && e.isFrightened();
        int[] field;
        if (leaving) {
            field = exitField;
        } else if (flee) {
            field = playerField; // run toward whatever is furthest from the player
        } else {
            int target = targetTile(e, scatterMode);
            if (target == Pathfinder.UNREACHABLE || target == pathfinder.index(playerTileX, playerTileY)) {
                field = playerField;
            } else if (target == homeCorners[e.getType().ordinal()]) {
                field = homeFields[e.getType().ordinal()];
            } else {
                if (target != scratchTarget) {
                    pathfinder.distancesFrom(target % width, target / width, false, scratchField);
                    scratchTarget = target;
                }
                field = scratchField;
            }
        }

        int best = -1;
        int bestScore = 0;
        for (int d = 0; d < 4; d++) {
            boolean isReverse = Pathfinder.DX[d] == -e.getDirX() && Pathfinder.DY[d] == -e.getDirY()
                    && (e.getDirX() != 0 || e.getDirY() != 0);
            int nx = e.getTileX() + Pathfinder.DX[d];
            int ny = e.getTileY() + Pathfinder.DY[d];
            if ((!leaving && isReverse) || !pathfinder.isWalkable(nx, ny, leaving)) {
                continue;
            }
            int score = field[pathfinder.index(nx, ny)];
            if (leaving && score == Pathfinder.UNREACHABLE) {
                continue;
            }
            if (flee && score == Pathfinder.UNREACHABLE) {
                score = -1;
            }
            boolean better = flee ? score > bestScore : score < bestScore;
            if (best == -1 || better) {
                best = d;
                bestScore = score;
            }
        }

        if (best >= 0) {
            e.setDirection(Pathfinder.DX[best], Pathfinder.DY[best]);
        } else if (pathfinder.isWalkable(e.getTileX() - e.getDirX(), e.getTileY() - e.getDirY(), leaving)
                && (e.getDirX() != 0 || e.getDirY() != 0)) {
            e.setDirection(-e.getDirX(), -e.getDirY()); // dead end: turn around
        } else {
            e.setDirection(0, 0);
        }
    }

    /**
     * Where an enemy is trying to go. In scatter mode every enemy heads to its home corner;
     * in chase mode each uses its Pac-Man ghost's rule.
     */
    private int targetTile(Enemy e, boolean scatterMode) {
        if (scatterMode) {
            return homeCorners[e.getType().ordinal()];
        }
        double px = player.getX();
        double py = player.getY();
        double dirX = player.getDirX();
        double dirY = player.getDirY();
        switch (e.getType()) {
            case DEMON: // Pinky: ambush in front of the player
                return pathfinder.nearestMazeTile((int) Math.floor(px + dirX * DEMON_LOOK_AHEAD),
                        (int) Math.floor(py + dirY * DEMON_LOOK_AHEAD));
            case LOST_SOUL: { // Inky: mirror the cacodemon around a point in front of the player
                double aheadX = px + dirX * LOST_SOUL_LOOK_AHEAD;
                double aheadY = py + dirY * LOST_SOUL_LOOK_AHEAD;
                Enemy leader = find(EnemyType.CACODEMON);
                double leaderX = leader != null ? leader.getX() : px;
                double leaderY = leader != null ? leader.getY() : py;
                return pathfinder.nearestMazeTile((int) Math.floor(2 * aheadX - leaderX),
                        (int) Math.floor(2 * aheadY - leaderY));
            }
            case IMP: // Clyde: chase from afar, lose interest up close
                if (Math.hypot(e.getX() - px, e.getY() - py) > IMP_SHY_DISTANCE) {
                    return pathfinder.index(playerTileX, playerTileY);
                }
                return homeCorners[e.getType().ordinal()];
            case CACODEMON: // Blinky: straight at the player
            default:
                return pathfinder.index(playerTileX, playerTileY);
        }
    }

    private Enemy find(EnemyType type) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.getType() == type) {
                return e;
            }
        }
        return null;
    }
}

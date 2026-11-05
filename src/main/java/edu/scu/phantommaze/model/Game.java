package edu.scu.phantommaze.model;

import java.util.Random;

/**
 * Root of the game model. Owns the map, the player and the current state.
 * (Ghosts, scoring and lives arrive in later milestones.)
 */
public final class Game {

    private final GameMap map;
    private final Player player;
    private final Random random;
    private GameState state = GameState.PLAYING;

    /**
     * Creates a game on a map.
     *
     * @param map the level map
     * @param random source of all randomness (injectable for tests)
     */
    public Game(GameMap map, Random random) {
        this.map = map;
        this.random = random;
        this.player = new Player(map.getPlayerStart(), 0.0);
    }

    /**
     * Advances the simulation.
     *
     * @param dt seconds elapsed
     */
    public void update(double dt) {
        if (state == GameState.PLAYING) {
            player.update(dt, map);
        }
    }

    /**
     * Returns the map.
     *
     * @return the map
     */
    public GameMap getMap() {
        return map;
    }

    /**
     * Returns the player.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the current state.
     *
     * @return the state
     */
    public GameState getState() {
        return state;
    }

    /**
     * Returns the injected random source.
     *
     * @return the random
     */
    public Random getRandom() {
        return random;
    }
}

package edu.scu.phantommaze.model;

/**
 * Root of the game model. Owns the map, the player and the current state.
 * (Ghosts, scoring and lives arrive in later milestones.)
 */
public class Game {

    private final GameMap map;
    private final Player player;
    private GameState state = GameState.PLAYING;

    /**
     * Creates a game on a map.
     *
     * @param map the level map
     */
    public Game(GameMap map) {
        this.map = map;
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
}

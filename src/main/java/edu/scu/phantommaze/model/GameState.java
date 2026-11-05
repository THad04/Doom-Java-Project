package edu.scu.phantommaze.model;

/** The top-level states of the game. */
public enum GameState {
    /** Title screen. */
    MENU,
    /** Normal gameplay. */
    PLAYING,
    /** Gameplay frozen by the player. */
    PAUSED,
    /** All pellets collected. */
    LEVEL_COMPLETE,
    /** Out of lives. */
    GAME_OVER
}

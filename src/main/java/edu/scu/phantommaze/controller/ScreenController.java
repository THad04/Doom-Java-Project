package edu.scu.phantommaze.controller;

import edu.scu.phantommaze.audio.SoundPlayer;
import edu.scu.phantommaze.model.Difficulty;
import edu.scu.phantommaze.model.Game;
import edu.scu.phantommaze.model.GameMap;
import edu.scu.phantommaze.model.GameState;
import edu.scu.phantommaze.view.GameFrame;
import edu.scu.phantommaze.view.GamePanel;
import edu.scu.phantommaze.view.MenuPanel;

/** Switches between the menu and gameplay, and turns input actions into screen changes. */
public class ScreenController implements InputController.Actions {

    private static final String MAP_PATH = "/maps/level1.txt";

    private final GamePanel gamePanel = new GamePanel();
    private final InputController input = new InputController(gamePanel, this);
    private final GameLoop loop = new GameLoop(input, gamePanel);
    private final SoundPlayer sounds = new SoundPlayer();
    private final GameFrame frame;
    private Game game;

    /** Builds the window and starts at the main menu. */
    public ScreenController() {
        MenuPanel menu = new MenuPanel(this::startGame, () -> System.exit(0));
        frame = new GameFrame(menu, gamePanel);
    }

    /** Shows the application. */
    public void show() {
        frame.setVisible(true);
        frame.showMenu();
    }

    /**
     * Starts a new run: builds a fresh {@link Game} on the level map with the chosen
     * difficulty, connects sound and the view to it, then switches to the game screen,
     * captures the mouse and starts the game loop.
     *
     * @param difficulty       enemy speed, power-up length and other tuning
     * @param mouseSensitivity how fast the mouse turns the player
     */
    private void startGame(Difficulty difficulty, int mouseSensitivity) {
        game = new Game(GameMap.fromResource(MAP_PATH), difficulty);
        game.setSoundListener(sounds);
        gamePanel.setGame(game);
        loop.setGame(game);
        input.setMouseSensitivity(mouseSensitivity);
        frame.showGame();
        input.captureMouse();
        loop.start();
    }

    /**
     * Returns to the main menu: stops the game loop, releases the mouse and discards
     * the current {@link Game}.
     */
    private void showMenu() {
        loop.stop();
        input.releaseMouse();
        game = null;
        loop.setGame(null);
        frame.showMenu();
    }

    /**
     * Toggles pause. While {@code PLAYING} the game is paused and the mouse is released;
     * while {@code PAUSED} the game resumes and the mouse is captured again.
     * Does nothing in other states or when no game is running.
     */
    @Override
    public void escape() {
        if (game == null) {
            return;
        }
        if (game.getState() == GameState.PLAYING) {
            game.pause();
            input.releaseMouse();
        } else if (game.getState() == GameState.PAUSED) {
            game.resume();
            input.captureMouse();
        }
    }

    /**
     * Handles Enter: when the game is over or the level is complete, returns to the
     * main menu. Ignored in every other state.
     */
    @Override
    public void confirm() {
        if (game != null && (game.getState() == GameState.GAME_OVER || game.getState() == GameState.LEVEL_COMPLETE)) {
            showMenu();
        }
    }

    /**
     * Handles Q: returns to the main menu if the game is paused, over, or complete.
     * Ignored while the game is actively {@code PLAYING}.
     */
    @Override
    public void quitToMenu() {
        if (game != null && game.getState() != GameState.PLAYING) { // paused, game over, or level complete
            showMenu();
        }
    }

    /**
     * Handles M: shows or hides the minimap. This only changes the view; the game
     * state is not touched.
     */
    @Override
    public void toggleMinimap() {
        gamePanel.toggleMinimap();
    }

    /**
     * Handles a click while the mouse is released: resumes a {@code PAUSED} game, and if
     * the game is then {@code PLAYING}, captures the mouse again.
     */
    @Override
    public void clickWhileReleased() {
        if (game == null) {
            return;
        }
        if (game.getState() == GameState.PAUSED) {
            game.resume();
        }
        if (game.getState() == GameState.PLAYING) {
            input.captureMouse();
        }
    }

    /**
     * Handles the window losing focus: pauses a {@code PLAYING} game so the player is
     * not attacked while away, and always releases the mouse.
     */
    @Override
    public void focusLost() {
        if (game != null && game.getState() == GameState.PLAYING) {
            game.pause();
        }
        input.releaseMouse();
    }
}

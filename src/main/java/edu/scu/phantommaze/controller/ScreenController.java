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

    private void showMenu() {
        loop.stop();
        input.releaseMouse();
        game = null;
        loop.setGame(null);
        frame.showMenu();
    }

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

    @Override
    public void confirm() {
        if (game != null && (game.getState() == GameState.GAME_OVER || game.getState() == GameState.LEVEL_COMPLETE)) {
            showMenu();
        }
    }

    @Override
    public void quitToMenu() {
        if (game != null && game.getState() != GameState.PLAYING) { // paused, game over, or level complete
            showMenu();
        }
    }

    @Override
    public void toggleMinimap() {
        gamePanel.toggleMinimap();
    }

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

    @Override
    public void focusLost() {
        if (game != null && game.getState() == GameState.PLAYING) {
            game.pause();
        }
        input.releaseMouse();
    }
}

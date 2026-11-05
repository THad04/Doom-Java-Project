package edu.scu.phantommaze;

import edu.scu.phantommaze.controller.GameLoop;
import edu.scu.phantommaze.controller.InputController;
import edu.scu.phantommaze.model.Game;
import edu.scu.phantommaze.model.GameMap;
import edu.scu.phantommaze.view.GameFrame;
import edu.scu.phantommaze.view.GamePanel;
import java.util.Random;
import javax.swing.SwingUtilities;

/** Entry point of Phantom Maze. */
public final class App {

    private App() {
        // not instantiable
    }

    /**
     * Launches the game.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Game game = new Game(GameMap.fromResource("/maps/level1.txt"), new Random());
            GamePanel panel = new GamePanel(game);
            InputController input = new InputController(panel, panel::toggleMinimap, () -> System.exit(0));
            GameFrame frame = new GameFrame(panel);
            frame.setVisible(true);
            panel.requestFocusInWindow();
            new GameLoop(game, input, panel).start();
        });
    }
}

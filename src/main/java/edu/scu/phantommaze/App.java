package edu.scu.phantommaze;

import edu.scu.phantommaze.controller.GameLoop;
import edu.scu.phantommaze.controller.InputController;
import edu.scu.phantommaze.model.Game;
import edu.scu.phantommaze.model.GameMap;
import edu.scu.phantommaze.view.GameFrame;
import edu.scu.phantommaze.view.GamePanel;
import javax.swing.SwingUtilities;

/** Entry point of Phantom Maze. */
public class App {

    /**
     * Launches the game.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> { // Run the following code on the swing thread

            // Reads the map text file and bundles with a Player standing at p
            Game game = new Game(GameMap.fromResource("/maps/level1.txt"));
            // build the panel, a view of the game
            GamePanel panel = new GamePanel(game);
            // listens for keypresses on the panel, toggles the minimap, and provides an
            // exit
            InputController input = new InputController(panel, panel::toggleMinimap, () -> System.exit(0));
            // panel goes inside of a a window
            GameFrame frame = new GameFrame(panel);

            // Show the window and focus on the panel
            frame.setVisible(true);
            panel.requestFocusInWindow();
            // start the clock and make the game run
            new GameLoop(game, input, panel).start();
        });
    }
}

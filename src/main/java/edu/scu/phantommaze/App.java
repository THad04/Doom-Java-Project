package edu.scu.phantommaze;

import edu.scu.phantommaze.controller.ScreenController;
import javax.swing.SwingUtilities;

/** Entry point of Phantom Maze. */
public class App {

    /**
     * Launches the game.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ScreenController().show());
    }
}

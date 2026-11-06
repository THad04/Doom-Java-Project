package edu.scu.phantommaze.view;

import javax.swing.JFrame;

/** The main application window. */
public class GameFrame extends JFrame {

    /**
     * Creates the window around a game panel.
     *
     * @param panel the panel to show
     */
    public GameFrame(GamePanel panel) {
        super("Phantom Maze");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        add(panel);
        pack();
        setLocationRelativeTo(null);
    }
}

package edu.scu.phantommaze.view;

import java.awt.CardLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * The main application window. It holds the menu and the game as two "cards" (like a
 * stack of cards where only the top one is visible) and switches between them.
 */
public class GameFrame extends JFrame {

    private static final String MENU = "menu";
    private static final String GAME = "game";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final GamePanel gamePanel;

    /**
     * Creates the window around the menu and the game panel.
     *
     * @param menu      the main menu
     * @param gamePanel the panel the game is drawn on
     */
    public GameFrame(MenuPanel menu, GamePanel gamePanel) {
        super("Phantom Maze"); // creates new frame named "Phantom Maze"
        this.gamePanel = gamePanel;
        setDefaultCloseOperation(EXIT_ON_CLOSE); // when app closed, exit
        content.add(menu, MENU);
        content.add(gamePanel, GAME);
        add(content); // add the card stack to frame
        pack(); // resize window
        setLocationRelativeTo(null); // center it
    }

    /** Shows the main menu. */
    public void showMenu() {
        cards.show(content, MENU);
    }

    /** Shows the game and gives it keyboard focus. */
    public void showGame() {
        cards.show(content, GAME);
        gamePanel.requestFocusInWindow();
    }
}

package edu.scu.phantommaze.controller;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.Game;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The game loop. About 60 times a second it reads the keys, updates the game,
 * and repaints the screen. Runs on the Swing event thread.
 */
public class GameLoop {

    private static final double STEP = 1.0 / Config.TARGET_FPS;

    private final Game game;
    private final InputController input;
    private final JComponent view;
    private final Timer timer;

    /**
     * Creates the loop (not yet running).
     *
     * @param game  the model to update
     * @param input the input source
     * @param view  the component to repaint after each tick
     */
    public GameLoop(Game game, InputController input, JComponent view) {
        this.game = game;
        this.input = input;
        this.view = view;
        // calls code at a regular interval of every 16 ms (60 fps)
        // whenever the timer fires, call tick()
        this.timer = new Timer(1000 / Config.TARGET_FPS, e -> tick());
        this.timer.setCoalesce(true); // if computer falls behind merge them into one
    }

    /** Starts the loop. */
    public void start() {
        timer.start(); // called at the end of app.java
    }

    /** One step of the game: read the keys, update the game, redraw. */
    private void tick() {
        input.applyTo(game.getPlayer()); // copy held keys into player intent
        game.update(STEP); // advance simulation by 1/60 s
        view.repaint(); // swing redraws the screen
    }
}

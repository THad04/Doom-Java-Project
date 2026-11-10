package edu.scu.phantommaze.controller;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.Game;
import javax.swing.JComponent;
import javax.swing.Timer;

/** Swing timer that reads input, updates the game, and repaints about 60 times a second. */
public class GameLoop {

    private final InputController input;
    private final JComponent view;
    private final Timer timer;
    private Game game;
    private long lastTick;

    /**
     * Creates a stopped loop.
     *
     * @param input input source
     * @param view  component to repaint
     */
    public GameLoop(InputController input, JComponent view) {
        this.input = input;
        this.view = view;
        this.timer = new Timer(1000 / Config.TARGET_FPS, e -> tick());
        this.timer.setCoalesce(true);
    }

    /**
     * Selects the game this loop advances.
     *
     * @param game current game
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /** Starts or restarts the loop. */
    public void start() {
        lastTick = System.nanoTime();
        timer.start();
    }

    /** Stops the loop. */
    public void stop() {
        timer.stop();
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min(Config.MAX_TIME_STEP, (now - lastTick) / 1_000_000_000.0);
        lastTick = now;
        if (game != null) {
            input.applyTo(game);
            game.update(dt);
        }
        view.repaint();
    }
}

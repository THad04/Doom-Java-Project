package edu.scu.phantommaze.controller;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.Game;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Fixed-timestep game loop (about 60 updates per second). Runs on the Swing event
 * thread, so the model and the renderer never touch each other concurrently.
 */
public final class GameLoop {

    private static final double STEP = 1.0 / Config.TARGET_FPS;

    private final Game game;
    private final InputController input;
    private final JComponent view;
    private final Timer timer;
    private long lastNanos;
    private double accumulator;

    /**
     * Creates the loop (not yet running).
     *
     * @param game the model to update
     * @param input the input source
     * @param view the component to repaint after each tick
     */
    public GameLoop(Game game, InputController input, JComponent view) {
        this.game = game;
        this.input = input;
        this.view = view;
        this.timer = new Timer(1000 / Config.TARGET_FPS, e -> tick());
        this.timer.setCoalesce(true);
    }

    /** Starts the loop. */
    public void start() {
        lastNanos = System.nanoTime();
        timer.start();
    }

    private void tick() {
        long now = System.nanoTime();
        accumulator += Math.min((now - lastNanos) / 1e9, Config.MAX_FRAME_TIME);
        lastNanos = now;
        while (accumulator >= STEP) {
            input.applyTo(game.getPlayer());
            game.update(STEP);
            accumulator -= STEP;
        }
        view.repaint();
    }
}

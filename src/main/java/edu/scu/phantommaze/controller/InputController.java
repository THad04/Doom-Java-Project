package edu.scu.phantommaze.controller;

import edu.scu.phantommaze.model.Player;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;

/**
 * Listens to the keyboard and remembers which movement keys are currently held down.
 * Each game tick, {@link #applyTo(Player)} copies that information into the player.
 */
public class InputController {

    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean turnLeft;
    private boolean turnRight;

    private final Runnable onToggleMinimap;
    private final Runnable onQuit;

    /**
     * Starts listening for keys on a component.
     *
     * @param c the component that receives keyboard input (it must have keyboard focus)
     * @param onToggleMinimap called when M is pressed
     * @param onQuit called when ESC is pressed
     */
    public InputController(JComponent c, Runnable onToggleMinimap, Runnable onQuit) {
        this.onToggleMinimap = onToggleMinimap;
        this.onQuit = onQuit;
        c.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleKey(e.getKeyCode(), false);
            }
        });
    }

    /**
     * Reacts to one key event.
     *
     * @param keyCode which key (a {@code KeyEvent.VK_...} constant)
     * @param isDown true if the key was just pressed, false if it was just released
     */
    private void handleKey(int keyCode, boolean isDown) {
        if (keyCode == KeyEvent.VK_W) {
            forward = isDown;
        } else if (keyCode == KeyEvent.VK_S) {
            back = isDown;
        } else if (keyCode == KeyEvent.VK_A) {
            left = isDown;
        } else if (keyCode == KeyEvent.VK_D) {
            right = isDown;
        } else if (keyCode == KeyEvent.VK_LEFT) {
            turnLeft = isDown;
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            turnRight = isDown;
        } else if (keyCode == KeyEvent.VK_M && isDown) {
            onToggleMinimap.run();
        } else if (keyCode == KeyEvent.VK_ESCAPE && isDown) {
            onQuit.run();
        }
    }

    /**
     * Pushes the currently held keys into the player as intents.
     *
     * @param player the player to steer
     */
    public void applyTo(Player player) {
        player.setIntents(axis(forward, back), axis(right, left), axis(turnRight, turnLeft));
    }

    /** Turns two opposite keys into one number: +1, -1, or 0 (neither or both held). */
    private static double axis(boolean positive, boolean negative) {
        double result = 0.0;
        if (positive) {
            result += 1.0;
        }
        if (negative) {
            result -= 1.0;
        }
        return result;
    }
}

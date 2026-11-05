package edu.scu.phantommaze.controller;

import edu.scu.phantommaze.model.Player;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Binds keys (Swing Key Bindings) to player intents by tracking which keys are held.
 */
public final class InputController {

    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean turnLeft;
    private boolean turnRight;

    /**
     * Registers all key bindings on a component.
     *
     * @param c the component that receives keyboard input
     * @param onToggleMinimap called when M is pressed
     * @param onQuit called when ESC is pressed
     */
    public InputController(JComponent c, Runnable onToggleMinimap, Runnable onQuit) {
        hold(c, "W", v -> forward = v);
        hold(c, "S", v -> back = v);
        hold(c, "A", v -> left = v);
        hold(c, "D", v -> right = v);
        hold(c, "LEFT", v -> turnLeft = v);
        hold(c, "RIGHT", v -> turnRight = v);
        press(c, "M", onToggleMinimap);
        press(c, "ESCAPE", onQuit);
    }

    /** Callback receiving whether a key is now held. */
    private interface HeldSetter {
        void set(boolean held);
    }

    private static void hold(JComponent c, String key, HeldSetter setter) {
        InputMap in = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        in.put(KeyStroke.getKeyStroke("pressed " + key), "down-" + key);
        in.put(KeyStroke.getKeyStroke("released " + key), "up-" + key);
        c.getActionMap().put("down-" + key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setter.set(true);
            }
        });
        c.getActionMap().put("up-" + key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setter.set(false);
            }
        });
    }

    private static void press(JComponent c, String key, Runnable action) {
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + key), "do-" + key);
        c.getActionMap().put("do-" + key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /**
     * Pushes the currently held keys into the player as intents.
     *
     * @param player the player to steer
     */
    public void applyTo(Player player) {
        player.setIntents(axis(forward, back), axis(right, left), axis(turnRight, turnLeft));
    }

    private static double axis(boolean positive, boolean negative) {
        return (positive ? 1.0 : 0.0) - (negative ? 1.0 : 0.0);
    }
}

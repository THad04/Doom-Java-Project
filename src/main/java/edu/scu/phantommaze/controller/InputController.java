package edu.scu.phantommaze.controller;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.Game;
import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/** Keyboard and mouse input for the game panel. */
public class InputController {

    /** Actions that are decided by the screen controller. */
    public interface Actions {
        /** ESC was pressed. */
        void escape();

        /** Enter was pressed. */
        void confirm();

        /** Q was pressed. */
        void quitToMenu();

        /** M was pressed. */
        void toggleMinimap();

        /** The panel was clicked while the mouse was not captured. */
        void clickWhileReleased();

        /** The game panel lost focus. */
        void focusLost();
    }

    private final JComponent component;
    private final Actions actions;
    private final Cursor hiddenCursor;
    private final CursorWarp cursorWarp = new CursorWarp();
    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean turnLeft;
    private boolean turnRight;
    private boolean keyFire;
    private boolean mouseFire;
    private boolean reloadRequested;
    private boolean captured;
    private int lastScreenX;
    private double mouseSensitivity = Config.DEFAULT_MOUSE_SENSITIVITY;

    /**
     * Starts listening on a component.
     *
     * @param component component that receives input
     * @param actions   screen-level actions
     */
    public InputController(JComponent component, Actions actions) {
        this.component = component;
        this.actions = actions;
        this.hiddenCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(), "hidden");
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleKey(e.getKeyCode(), false);
            }
        });
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                if (!captured) {
                    actions.clickWhileReleased();
                    return;
                }
                mouseFire = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    mouseFire = false;
                }
            }
        };
        component.addMouseListener(mouse);
        component.addMouseMotionListener(mouse);
        component.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                clearHeldInput();
                actions.focusLost();
            }
        });
    }

    /**
     * Sets mouse sensitivity from the menu.
     *
     * @param sensitivity 1 to 10
     */
    public void setMouseSensitivity(int sensitivity) {
        mouseSensitivity = Math.max(1, Math.min(10, sensitivity));
    }

    /** Hides the cursor and keeps it centered over the game, when macOS permits it. */
    public void captureMouse() {
        if (!component.isShowing()) {
            return;
        }
        captured = true;
        component.setCursor(hiddenCursor);
        cursorWarp.hideCursor();
        lastScreenX = recenter();
        component.requestFocusInWindow();
    }

    /** Shows the cursor and stops turning from mouse movement. */
    public void releaseMouse() {
        captured = false;
        clearHeldInput();
        component.setCursor(Cursor.getDefaultCursor());
        cursorWarp.showCursor();
    }

    /**
     * Copies current input into the game.
     *
     * @param game the game to control
     */
    public void applyTo(Game game) {
        game.getPlayer().setIntents(axis(forward, back), axis(right, left), axis(turnRight, turnLeft));
        if (captured) {
            game.getPlayer().rotate(readMouseTurn() * mouseSensitivity * Config.MOUSE_RADIANS_PER_PIXEL);
        }
        game.setTriggerHeld(keyFire || mouseFire);
        if (reloadRequested) {
            game.requestReload();
            reloadRequested = false;
        }
    }

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
        } else if (keyCode == KeyEvent.VK_SPACE) {
            keyFire = isDown;
        } else if (keyCode == KeyEvent.VK_R && isDown) {
            reloadRequested = true;
        } else if (keyCode == KeyEvent.VK_M && isDown) {
            actions.toggleMinimap();
        } else if (keyCode == KeyEvent.VK_ESCAPE && isDown) {
            actions.escape();
        } else if (keyCode == KeyEvent.VK_ENTER && isDown) {
            actions.confirm();
        } else if (keyCode == KeyEvent.VK_Q && isDown) {
            actions.quitToMenu();
        }
    }

    /**
     * Measures how far the mouse moved sideways since the last frame, then puts the pointer back
     * in the middle of the game. Reading the pointer's screen position every frame (instead of
     * waiting for mouse events) works even if a fast swipe carried it outside the window.
     *
     * @return horizontal movement in screen pixels
     */
    private int readMouseTurn() {
        PointerInfo pointer = MouseInfo.getPointerInfo();
        if (pointer == null) {
            return 0;
        }
        int dx = pointer.getLocation().x - lastScreenX;
        lastScreenX = recenter();
        return dx;
    }

    /**
     * Moves the pointer to the center of the game panel.
     *
     * @return the pointer's x position afterwards
     */
    private int recenter() {
        if (component.isShowing()) {
            Point center = new Point(component.getWidth() / 2, component.getHeight() / 2);
            SwingUtilities.convertPointToScreen(center, component); // follows the window if it moved
            cursorWarp.moveTo(center.x, center.y);
            PointerInfo after = MouseInfo.getPointerInfo();
            if (after != null) {
                return after.getLocation().x;
            }
        }
        return lastScreenX;
    }

    private void clearHeldInput() {
        forward = false;
        back = false;
        left = false;
        right = false;
        turnLeft = false;
        turnRight = false;
        keyFire = false;
        mouseFire = false;
        reloadRequested = false;
    }

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

package edu.scu.phantommaze.controller;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import java.awt.AWTException;
import java.awt.Robot;

/**
 * Moves the mouse pointer to a screen position, used to keep it in the middle of the game.
 *
 * <p>Java has no built-in "lock the mouse" feature. Its only tool is {@link Robot}, which
 * works by faking mouse input; macOS treats that like remote control of the computer and
 * blocks it unless the app is allowed under Accessibility. On macOS we therefore call
 * CoreGraphics' {@code CGWarpMouseCursorPosition} directly (through the JNA library), the
 * same call native Mac games use. It only moves the pointer, so it needs no permission.
 * Other systems use {@link Robot}, which works there without special permission.
 */
final class CursorWarp {

    /** The CoreGraphics functions we need, loaded from macOS's own library. */
    private interface CoreGraphics extends Library {
        int CGWarpMouseCursorPosition(CGPoint.ByValue point);

        int CGAssociateMouseAndMouseCursorPosition(boolean connected);

        int CGDisplayHideCursor(int display);

        int CGDisplayShowCursor(int display);
    }

    /** "Whichever display" for the hide/show calls (kCGNullDirectDisplay). */
    private static final int ANY_DISPLAY = 0;

    /** CoreGraphics' point type: two doubles. */
    @Structure.FieldOrder({ "x", "y" })
    public static class CGPoint extends Structure {
        public double x;
        public double y;

        /** Passed by value, as CGWarpMouseCursorPosition expects. */
        public static class ByValue extends CGPoint implements Structure.ByValue {
        }
    }

    private final CoreGraphics coreGraphics;
    private final CGPoint.ByValue point;
    private final Robot robot;
    private boolean cursorHidden;

    /** Picks CoreGraphics on macOS, otherwise Robot. */
    CursorWarp() {
        CoreGraphics cg = null;
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            try {
                cg = Native.load("CoreGraphics", CoreGraphics.class);
            } catch (UnsatisfiedLinkError e) {
                cg = null;
            }
        }
        coreGraphics = cg;
        point = cg != null ? new CGPoint.ByValue() : null;
        Robot r = null;
        if (cg == null) {
            try {
                r = new Robot();
            } catch (AWTException | SecurityException e) {
                r = null;
            }
        }
        robot = r;
    }

    /**
     * Moves the pointer.
     *
     * @param x screen x
     * @param y screen y
     */
    void moveTo(int x, int y) {
        if (coreGraphics != null) {
            point.x = x;
            point.y = y;
            coreGraphics.CGWarpMouseCursorPosition(point);
            // Without this, macOS ignores mouse movement for a moment after every warp.
            coreGraphics.CGAssociateMouseAndMouseCursorPosition(true);
        } else if (robot != null) {
            robot.mouseMove(x, y);
        }
    }

    /**
     * Hides the pointer on the whole screen, not just over the game window, so it stays
     * invisible even if a fast swipe briefly carries it past the window edge. Only has an
     * effect on macOS; elsewhere the game panel's blank cursor is used.
     */
    void hideCursor() {
        if (coreGraphics != null && !cursorHidden) {
            coreGraphics.CGDisplayHideCursor(ANY_DISPLAY);
            cursorHidden = true;
        }
    }

    /** Shows the pointer again. macOS counts hide/show calls, so each hide gets exactly one show. */
    void showCursor() {
        if (coreGraphics != null && cursorHidden) {
            coreGraphics.CGDisplayShowCursor(ANY_DISPLAY);
            cursorHidden = false;
        }
    }
}

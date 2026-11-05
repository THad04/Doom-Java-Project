package edu.scu.phantommaze.view;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.Game;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.JPanel;

/**
 * Panel that renders the game into a low-resolution off-screen image and scales it to the window.
 */
public final class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Game game;
    private final transient BufferedImage frame =
            new BufferedImage(Config.RENDER_WIDTH, Config.RENDER_HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final transient int[] pixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
    private final transient double[] zBuffer = new double[Config.RENDER_WIDTH];
    private final transient WorldRenderer worldRenderer;
    private final transient MinimapRenderer minimapRenderer = new MinimapRenderer();
    private boolean showMinimap = true;

    /**
     * Creates the panel.
     *
     * @param game the model to display
     */
    public GamePanel(Game game) {
        this.game = game;
        this.worldRenderer = new WorldRenderer(game.getMap());
        setPreferredSize(new Dimension(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT));
        setFocusable(true);
        setBackground(java.awt.Color.BLACK);
    }

    /** Shows or hides the minimap. */
    public void toggleMinimap() {
        showMinimap = !showMinimap;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        worldRenderer.render(pixels, game.getPlayer(), zBuffer);

        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(frame, 0, 0, getWidth(), getHeight(), null);

        if (showMinimap) {
            minimapRenderer.render(g, getWidth(), game.getMap(), game.getPlayer());
        }
    }
}

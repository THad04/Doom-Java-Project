package edu.scu.phantommaze.view;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.Enemy;
import edu.scu.phantommaze.model.Game;
import edu.scu.phantommaze.model.GameState;
import edu.scu.phantommaze.model.Player;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.JPanel;

/**
 * Panel that renders the game into a low-resolution off-screen image and scales it to the window.
 *
 * <p>Each frame is built in layers: walls ({@link WorldRenderer}), then enemies and pickups
 * ({@link SpriteRenderer}), then the gun and status bar ({@link HudRenderer}). The finished
 * image is stretched to the window, and the minimap is drawn on top at full resolution.
 */
public class GamePanel extends JPanel {

    /** Seconds before the power-up ends when frightened enemies start flashing white. */
    private static final double FLASH_WARNING_TIME = 2.0;

    private final BufferedImage frame =
            new BufferedImage(Config.RENDER_WIDTH, Config.RENDER_HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
    private final double[] zBuffer = new double[Config.RENDER_WIDTH];
    private final Graphics2D frameGraphics = frame.createGraphics();
    private final EnemySprites enemySprites = new EnemySprites();
    private final Patch powerUpSprite = Assets.sprite("shota0");
    private final HudRenderer hudRenderer = new HudRenderer();
    private final MinimapRenderer minimapRenderer = new MinimapRenderer();
    private final ThreatIndicator threatIndicator = new ThreatIndicator();
    private SpriteRenderer spriteRenderer; // sized for the game's enemies and pickups in setGame
    private Game game;
    private WorldRenderer worldRenderer;
    private boolean showMinimap = true;

    /** Creates the panel; call {@link #setGame} before it is shown. */
    public GamePanel() {
        setPreferredSize(new Dimension(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT));
        setFocusable(true);
        setBackground(java.awt.Color.BLACK);
    }

    /**
     * Switches to showing a (new) game.
     *
     * @param game the model to display
     */
    public void setGame(Game game) {
        this.game = game;
        this.worldRenderer = new WorldRenderer(game.getMap());
        this.spriteRenderer = new SpriteRenderer(game.getPowerUps().size() + game.getEnemies().size());
    }

    /** Shows or hides the minimap. */
    public void toggleMinimap() {
        showMinimap = !showMinimap;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (game == null) {
            return;
        }
        Player player = game.getPlayer();
        worldRenderer.render(pixels, player, zBuffer);
        queueSprites(player);
        spriteRenderer.render(pixels, zBuffer, player);

        hudRenderer.render(frameGraphics, game);
        if (game.getState() == GameState.PLAYING) {
            threatIndicator.render(pixels, game); // on top of the gun so it is never hidden
        }

        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(frame, 0, 0, getWidth(), getHeight(), null);

        if (showMinimap && game.getState() == GameState.PLAYING) {
            minimapRenderer.render(g, getWidth(), game);
        }
    }

    /** Hands every visible enemy and power-up to the sprite renderer. */
    private void queueSprites(Player player) {
        spriteRenderer.clear();
        for (int[] p : game.getPowerUps()) {
            spriteRenderer.add(powerUpSprite, p[0] + 0.5, p[1] + 0.5, 0, SpriteRenderer.TINT_NONE, true);
        }
        int frightenedTint = SpriteRenderer.TINT_FRIGHTENED;
        if (game.getPowerUpRemaining() < FLASH_WARNING_TIME && (int) (game.getTime() * 5) % 2 == 0) {
            frightenedTint = SpriteRenderer.TINT_FLASH; // blink like Pac-Man's ghosts
        }
        for (Enemy e : game.getEnemies()) {
            if (e.getState() == Enemy.State.IN_PEN) {
                continue; // hidden behind the pen walls anyway
            }
            double viewToEnemy = Math.atan2(e.getY() - player.getY(), e.getX() - player.getX());
            Patch patch = enemySprites.frameFor(e, viewToEnemy);
            int tint = e.isFrightened() ? frightenedTint : SpriteRenderer.TINT_NONE;
            spriteRenderer.add(patch, e.getX(), e.getY(), enemySprites.hoverFor(e), tint,
                    enemySprites.isFullBright(e));
        }
    }
}

package edu.scu.phantommaze.view;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.Blast;
import edu.scu.phantommaze.model.Enemy;
import edu.scu.phantommaze.model.Game;
import edu.scu.phantommaze.model.GameMap;
import edu.scu.phantommaze.model.GameState;
import edu.scu.phantommaze.model.Player;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
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
    /** BFG explosions float this high (sprite pixels), around an enemy's middle. */
    private static final double BLAST_HOVER = 16;
    /** Dots float this high (sprite pixels), a little under the gun's line of sight. */
    private static final double DOT_HOVER = 20;
    /** Size of a dot in sprite pixels (one sprite pixel is 1/100 of a tile). */
    private static final int DOT_SIZE = 4;
    private static final Color DOT_CENTER = new Color(255, 255, 220);
    private static final Color DOT_EDGE = new Color(255, 190, 60);

    private final BufferedImage frame =
            new BufferedImage(Config.RENDER_WIDTH, Config.RENDER_HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
    private final double[] zBuffer = new double[Config.RENDER_WIDTH];
    private final Graphics2D frameGraphics = frame.createGraphics();
    private final EnemySprites enemySprites = new EnemySprites();
    private final Patch powerUpSprite = Assets.sprite("shota0");
    private final Patch dotSprite = glowingDot();
    private final Patch[] blastFrames = {
        Assets.sprite("bfe2a0"), Assets.sprite("bfe2b0"), Assets.sprite("bfe2c0"), Assets.sprite("bfe2d0") };
    private final HudRenderer hudRenderer = new HudRenderer();
    private final MinimapRenderer minimapRenderer = new MinimapRenderer();
    private final ThreatIndicator threatIndicator = new ThreatIndicator();
    private SpriteRenderer spriteRenderer; // sized for the game's dots, enemies, pickups and explosions in setGame
    private Game game;
    private WorldRenderer worldRenderer;
    private boolean showMinimap = true;

    /** Creates the panel; call {@link #setGame} before it is shown. */
    public GamePanel() {
        setPreferredSize(new Dimension(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT));
        setFocusable(true);
        setBackground(Color.BLACK);
    }

    /**
     * Switches to showing a (new) game.
     *
     * @param game the model to display
     */
    public void setGame(Game game) {
        this.game = game;
        this.worldRenderer = new WorldRenderer(game.getMap());
        // Dots and power-ups (the pellet count), each enemy, and at most one explosion per enemy:
        // explosions last less than the BFG's fire delay.
        this.spriteRenderer = new SpriteRenderer(game.getMap().getPelletCount() + 2 * game.getEnemies().size());
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

    /** Hands every visible dot, enemy, power-up and BFG explosion to the sprite renderer. */
    private void queueSprites(Player player) {
        spriteRenderer.clear();
        queueDots(player);
        for (int[] p : game.getPowerUps()) {
            spriteRenderer.add(powerUpSprite, p[0] + 0.5, p[1] + 0.5, 0, SpriteRenderer.TINT_NONE, true);
        }
        for (Blast b : game.getBlasts()) {
            int frame = Math.min(blastFrames.length - 1, (int) (b.getAge() / Config.BLAST_TIME * blastFrames.length));
            spriteRenderer.add(blastFrames[frame], b.getX(), b.getY(), BLAST_HOVER, SpriteRenderer.TINT_NONE, true);
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

    /** Dots in front of the player and closer than the fog (which hides walls further away). */
    private void queueDots(Player player) {
        GameMap map = game.getMap();
        double maxDistanceSq = Config.FOG_DISTANCE * Config.FOG_DISTANCE;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (map.getCell(x, y) != GameMap.DOT) {
                    continue;
                }
                double dx = x + 0.5 - player.getX();
                double dy = y + 0.5 - player.getY();
                if (dx * player.getDirX() + dy * player.getDirY() <= 0 || dx * dx + dy * dy > maxDistanceSq) {
                    continue;
                }
                spriteRenderer.add(dotSprite, x + 0.5, y + 0.5, DOT_HOVER, SpriteRenderer.TINT_NONE, true);
            }
        }
    }

    /** Pac-Man's dot as a small glowing ball: pale yellow in the middle, orange at the edge. */
    private static Patch glowingDot() {
        BufferedImage image = new BufferedImage(DOT_SIZE, DOT_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float radius = DOT_SIZE / 2f;
        g.setPaint(new RadialGradientPaint(radius * 0.8f, radius * 0.8f, radius,
                new float[] { 0f, 1f }, new Color[] { DOT_CENTER, DOT_EDGE }));
        g.fillOval(0, 0, DOT_SIZE, DOT_SIZE);
        g.dispose();
        return new Patch(image, DOT_SIZE / 2, DOT_SIZE); // anchored at its bottom center, like Doom sprites
    }
}

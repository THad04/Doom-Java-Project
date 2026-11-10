package edu.scu.phantommaze.view;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.Enemy;
import edu.scu.phantommaze.model.EnemyType;
import edu.scu.phantommaze.model.Game;
import edu.scu.phantommaze.model.GameMap;
import edu.scu.phantommaze.model.Player;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Draws a top-down minimap with the player's position and view cone, the remaining dots and
 * power-ups, and the enemies (only while the player has the shotgun power-up).
 */
public class MinimapRenderer {

    private static final Color BACKGROUND = new Color(0, 0, 0, 170);
    private static final Color WALL = new Color(90, 70, 160);
    private static final Color DOOR = new Color(200, 120, 200);
    private static final Color CONE = new Color(255, 255, 120, 90);
    private static final Color PLAYER = new Color(255, 230, 40);
    private static final Color POWER_UP = new Color(80, 230, 80);
    private static final Color DOT = new Color(255, 220, 120);
    private static final Color FRIGHTENED = new Color(60, 90, 255);
    private static final double CONE_LENGTH = 5.0;

    private final Map<EnemyType, Color> enemyColors = new EnumMap<>(EnemyType.class);
    private final Path2D.Double cone = new Path2D.Double(); // reused every frame
    private GameMap layerMap; // the map the cached wall image was drawn for
    private BufferedImage layer;

    /** Creates the renderer; each enemy gets its Pac-Man ghost's color. */
    public MinimapRenderer() {
        enemyColors.put(EnemyType.CACODEMON, new Color(255, 40, 40)); // Blinky red
        enemyColors.put(EnemyType.DEMON, new Color(255, 150, 200)); // Pinky pink
        enemyColors.put(EnemyType.LOST_SOUL, new Color(60, 230, 255)); // Inky cyan
        enemyColors.put(EnemyType.IMP, new Color(255, 160, 40)); // Clyde orange
    }

    /**
     * Walls and doors never change during a game, so they are painted once into an image
     * (one pixel block per tile) and that image is reused until the map changes.
     */
    private BufferedImage wallLayer(GameMap map) {
        if (layerMap != map) {
            int t = Config.MINIMAP_TILE;
            layer = new BufferedImage(map.getWidth() * t, map.getHeight() * t, BufferedImage.TYPE_INT_ARGB);
            Graphics2D lg = layer.createGraphics();
            for (int y = 0; y < map.getHeight(); y++) {
                for (int x = 0; x < map.getWidth(); x++) {
                    char c = map.getCell(x, y);
                    if (c == GameMap.WALL) {
                        lg.setColor(WALL);
                        lg.fillRect(x * t, y * t, t, t);
                    } else if (c == GameMap.DOOR) {
                        lg.setColor(DOOR);
                        lg.fillRect(x * t, y * t, t, t);
                    }
                }
            }
            lg.dispose();
            layerMap = map;
        }
        return layer;
    }

    /**
     * Draws the minimap in the top-right corner.
     *
     * @param g           graphics in window coordinates
     * @param windowWidth current panel width
     * @param game        the game to show
     */
    public void render(Graphics2D g, int windowWidth, Game game) {
        GameMap map = game.getMap();
        Player player = game.getPlayer();
        int t = Config.MINIMAP_TILE;
        int w = map.getWidth() * t;
        int h = map.getHeight() * t;
        int ox = windowWidth - w - Config.MINIMAP_MARGIN;
        // Leave room above for the HUD's score line (drawn at 2 Doom pixels from the top).
        int oy = Config.MINIMAP_MARGIN + (int) ((2 + DoomFont.lineHeight() + 1) * Config.HUD_SCALE
                * windowWidth / (double) Config.RENDER_WIDTH);

        g.setColor(BACKGROUND);
        g.fillRect(ox - 2, oy - 2, w + 4, h + 4);
        g.drawImage(wallLayer(map), ox, oy, null);

        g.setColor(DOT);
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (map.getCell(x, y) == GameMap.DOT) {
                    g.fillRect(ox + x * t + t / 2 - 1, oy + y * t + t / 2 - 1, 2, 2);
                }
            }
        }

        g.setColor(POWER_UP);
        for (int[] p : game.getPowerUps()) {
            g.fillOval(ox + p[0] * t + 1, oy + p[1] * t + 1, t - 2, t - 2);
        }

        // Enemies are hidden unless the shotgun power-up is active.
        if (game.isPowerUpActive()) {
            for (Enemy e : game.getEnemies()) {
                if (!e.isShootable()) {
                    continue;
                }
                g.setColor(e.isFrightened() ? FRIGHTENED : enemyColors.get(e.getType()));
                int ex = (int) (ox + e.getX() * t);
                int ey = (int) (oy + e.getY() * t);
                g.fillOval(ex - 3, ey - 3, 6, 6);
            }
        }

        // View cone: a triangle from the player to the edges of the field of view.
        double cx = ox + player.getX() * t;
        double cy = oy + player.getY() * t;
        double leftX = player.getDirX() - player.getPlaneX();
        double leftY = player.getDirY() - player.getPlaneY();
        double rightX = player.getDirX() + player.getPlaneX();
        double rightY = player.getDirY() + player.getPlaneY();
        cone.reset();
        cone.moveTo(cx, cy);
        cone.lineTo(cx + leftX * CONE_LENGTH * t, cy + leftY * CONE_LENGTH * t);
        cone.lineTo(cx + rightX * CONE_LENGTH * t, cy + rightY * CONE_LENGTH * t);
        cone.closePath();
        g.setColor(CONE);
        g.fill(cone);

        g.setColor(PLAYER);
        g.fillOval((int) cx - 3, (int) cy - 3, 6, 6);
    }
}

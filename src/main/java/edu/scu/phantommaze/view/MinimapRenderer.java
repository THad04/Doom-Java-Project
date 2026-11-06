package edu.scu.phantommaze.view;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.GameMap;
import edu.scu.phantommaze.model.Player;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/** Draws a top-down minimap with the player's position and view cone. */
public class MinimapRenderer {

    private static final Color BACKGROUND = new Color(0, 0, 0, 170);
    private static final Color WALL = new Color(90, 70, 160);
    private static final Color DOOR = new Color(200, 120, 200);
    private static final Color CONE = new Color(255, 255, 120, 90);
    private static final Color PLAYER = new Color(255, 230, 40);
    private static final double CONE_LENGTH = 5.0;

    /**
     * Draws the minimap in the top-right corner.
     *
     * @param g graphics in window coordinates
     * @param windowWidth current panel width
     * @param map the map
     * @param player the player
     */
    public void render(Graphics2D g, int windowWidth, GameMap map, Player player) {
        int t = Config.MINIMAP_TILE;
        int w = map.getWidth() * t;
        int h = map.getHeight() * t;
        int ox = windowWidth - w - Config.MINIMAP_MARGIN;
        int oy = Config.MINIMAP_MARGIN;

        g.setColor(BACKGROUND);
        g.fillRect(ox - 2, oy - 2, w + 4, h + 4);
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                char c = map.getCell(x, y);
                if (c == GameMap.WALL) {
                    g.setColor(WALL);
                    g.fillRect(ox + x * t, oy + y * t, t, t);
                } else if (c == GameMap.DOOR) {
                    g.setColor(DOOR);
                    g.fillRect(ox + x * t, oy + y * t, t, t);
                }
            }
        }

        // View cone: a triangle from the player to the edges of the field of view.
        double cx = ox + player.getX() * t;
        double cy = oy + player.getY() * t;
        double leftX = player.getDirX() - player.getPlaneX();
        double leftY = player.getDirY() - player.getPlaneY();
        double rightX = player.getDirX() + player.getPlaneX();
        double rightY = player.getDirY() + player.getPlaneY();
        Path2D.Double cone = new Path2D.Double();
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

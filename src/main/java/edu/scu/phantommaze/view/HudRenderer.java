package edu.scu.phantommaze.view;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.Enemy;
import edu.scu.phantommaze.model.Game;
import edu.scu.phantommaze.model.GameState;
import edu.scu.phantommaze.model.Weapon;
import edu.scu.phantommaze.model.Weapons;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;

/**
 * Draws everything on top of the 3D view: the gun in the player's hands, the crosshair,
 * screen flashes, the Doom status bar (ammo of the held weapon, health, Doomguy's face, armor),
 * pickup messages and the pause / game over screens.
 *
 * <p>Positions are written in Doom's original 320x200 screen coordinates and multiplied by
 * {@link Config#HUD_SCALE}, so the layout matches the real Doom status bar.
 */
public class HudRenderer {

    private static final int S = Config.HUD_SCALE;
    private static final int BAR_Y = 168; // top of the status bar in Doom coordinates
    private static final int NUMBER_Y = 171;
    private static final int AMMO_RIGHT = 44; // right edge of the ammo number
    private static final int HEALTH_RIGHT = 90;
    private static final int ARMOR_RIGHT = 221;
    private static final int FACE_X = 143;
    /** Doom draws the gun 16 pixels higher when the status bar is showing. */
    private static final int WEAPON_TOP = 32 - 16;
    /** How far (Doom pixels) the pistol drops out of view while reloading. */
    private static final int RELOAD_DROP = 70;
    /** How far (Doom pixels) the gun sways while walking. */
    private static final double BOB_AMOUNT = 10;
    /** Seconds Doomguy keeps looking one way before glancing again. */
    private static final double GLANCE_TIME = 0.5;

    /** Alpha levels the red / gold screen flashes are rounded to, so none are created per frame. */
    private static final int TINT_STEPS = 64;
    /**
     * Columns (first, last) of Freedoom's stbar.png to paint over: the FRAGS box and the
     * ammo-type table with its icons. The status bar is only 320 pixels wide.
     */
    private static final int[][] BLANKED_SPANS = { { 107, 141 }, { 262, 314 } };
    /** Plain dark panel texture next to Doomguy's face (columns 148-171 of stbar.png) used to cover them. */
    private static final int PANEL_TEXTURE_X = 148;
    private static final int PANEL_TEXTURE_WIDTH = 24;

    private static final Color HURT_RED = new Color(255, 0, 0);
    private static final Color PICKUP_GOLD = new Color(215, 186, 69);
    private static final Color CROSSHAIR = new Color(255, 255, 255, 160);
    private static final Color DIM = new Color(0, 0, 0, 150);

    private final Patch statusBar = blankUnusedPanels(Assets.graphic("stbar"));
    private final AlphaComposite[] tintComposites = new AlphaComposite[TINT_STEPS]; // alpha 0..1, reused
    private final Patch percent = Assets.graphic("sttprcnt");
    private final Patch[] bigDigits = new Patch[10];
    private final Patch[][] straightFaces = new Patch[5][3]; // [pain level][glance]
    private final Patch[] lookLeftFaces = new Patch[5];
    private final Patch[] lookRightFaces = new Patch[5];
    private final Patch[] ouchFaces = new Patch[5];
    private final Patch deadFace = Assets.graphic("stfdead0");
    private final Patch godFace = Assets.graphic("stfgod0");
    private final Patch pistolIdle = Assets.sprite("pisga0");
    private final Patch pistolFire1 = Assets.sprite("pisgb0");
    private final Patch pistolFire2 = Assets.sprite("pisgc0");
    private final Patch pistolFlash = Assets.sprite("pisfa0");
    private final Patch shotgunIdle = Assets.sprite("shtga0");
    private final Patch shotgunPump1 = Assets.sprite("shtgb0");
    private final Patch shotgunPump2 = Assets.sprite("shtgc0");
    private final Patch shotgunPump3 = Assets.sprite("shtgd0");
    private final Patch shotgunFlash1 = Assets.sprite("shtfa0");
    private final Patch shotgunFlash2 = Assets.sprite("shtfb0");

    /** Loads the HUD pictures. */
    public HudRenderer() {
        for (int i = 0; i < TINT_STEPS; i++) {
            tintComposites[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i / (float) (TINT_STEPS - 1));
        }
        for (int i = 0; i < 10; i++) {
            bigDigits[i] = Assets.graphic("sttnum" + i);
        }
        for (int pain = 0; pain < 5; pain++) {
            for (int glance = 0; glance < 3; glance++) {
                straightFaces[pain][glance] = Assets.graphic("stfst" + pain + glance);
            }
            lookLeftFaces[pain] = Assets.graphic("stftl" + pain + "0");
            lookRightFaces[pain] = Assets.graphic("stftr" + pain + "0");
            ouchFaces[pain] = Assets.graphic("stfouch" + pain);
        }
    }

    /**
     * Draws the HUD into the render buffer (after the world and sprites).
     *
     * @param g    graphics of the render buffer image
     * @param game the game to show
     */
    public void render(Graphics2D g, Game game) {
        Shape oldClip = g.getClip();
        g.setClip(0, 0, Config.RENDER_WIDTH, Config.VIEW_HEIGHT); // keep the gun out of the status bar
        drawWeapon(g, game);
        drawCrosshair(g);
        drawScreenTint(g, game);
        g.setClip(oldClip);

        drawStatusBar(g, game);

        String message = game.getMessage();
        if (message != null) {
            DoomFont.draw(g, message, 2 * S, 2 * S, S);
        }
        drawOverlay(g, game);
    }

    private void drawWeapon(Graphics2D g, Game game) {
        Weapons weapons = game.getWeapons();
        double t = weapons.getTimeSinceShot();
        Patch gun;
        Patch flash = null;
        if (weapons.getCurrent() == Weapon.PISTOL) {
            if (t < 0.07) {
                gun = pistolFire1;
                flash = pistolFlash;
            } else if (t < 0.15) {
                gun = pistolFire2;
            } else if (t < 0.25) {
                gun = pistolFire1;
            } else {
                gun = pistolIdle;
            }
        } else {
            gun = shotgunIdle;
            if (t < 0.07) {
                flash = shotgunFlash1;
            } else if (t < 0.14) {
                flash = shotgunFlash2;
            } else if (t >= 0.25 && t < 0.35) {
                gun = shotgunPump1;
            } else if (t >= 0.35 && t < 0.47) {
                gun = shotgunPump2;
            } else if (t >= 0.47 && t < 0.57) {
                gun = shotgunPump3;
            } else if (t >= 0.57 && t < 0.68) {
                gun = shotgunPump2;
            } else if (t >= 0.68 && t < 0.78) {
                gun = shotgunPump1;
            }
        }

        // Sway while walking, like Doom's weapon bob.
        double bob = BOB_AMOUNT * game.getPlayer().getMoveIntensity();
        double phase = game.getTime() * 2 * Math.PI / 1.8;
        double sx = 1 + bob * Math.cos(phase);
        double sy = WEAPON_TOP + bob * Math.abs(Math.sin(phase));
        if (weapons.isReloading()) { // drop the pistol out of view and bring it back up
            sy += RELOAD_DROP * Math.sin(weapons.getReloadProgress() * Math.PI);
        }
        if (flash != null) {
            drawPatch(g, flash, sx, sy);
        }
        drawPatch(g, gun, sx, sy);
    }

    /** Draws a picture the way Doom does: position minus the picture's offsets. */
    private void drawPatch(Graphics2D g, Patch p, double x, double y) {
        int px = (int) Math.round((x - p.leftOffset) * S);
        int py = (int) Math.round((y - p.topOffset) * S);
        g.drawImage(p.image, px, py, p.width * S, p.height * S, null);
    }

    private void drawCrosshair(Graphics2D g) {
        int cx = Config.RENDER_WIDTH / 2;
        int cy = Config.VIEW_HEIGHT / 2;
        g.setColor(CROSSHAIR);
        g.fillRect(cx - 5, cy, 4, 1);
        g.fillRect(cx + 2, cy, 4, 1);
        g.fillRect(cx, cy - 5, 1, 4);
        g.fillRect(cx, cy + 2, 1, 4);
    }

    /** Red after being hit, gold after a pickup. */
    private void drawScreenTint(Graphics2D g, Game game) {
        double hurt = game.getHurtAmount();
        double pickup = game.getPickupFlash();
        if (hurt > 0) {
            fillTinted(g, HURT_RED, (float) (0.15 + 0.4 * hurt));
        } else if (pickup > 0) {
            fillTinted(g, PICKUP_GOLD, (float) (0.35 * pickup));
        }
    }

    private void fillTinted(Graphics2D g, Color color, float alpha) {
        Composite old = g.getComposite();
        int step = Math.round(Math.min(1f, alpha) * (TINT_STEPS - 1));
        g.setComposite(tintComposites[step]);
        g.setColor(color);
        g.fillRect(0, 0, Config.RENDER_WIDTH, Config.VIEW_HEIGHT);
        g.setComposite(old);
    }

    private void drawStatusBar(Graphics2D g, Game game) {
        drawPatch(g, statusBar, 0, BAR_Y);
        drawNumber(g, game.getWeapons().getAmmo(), AMMO_RIGHT);
        drawNumber(g, game.getHealthPercent(), HEALTH_RIGHT);
        drawPatch(g, percent, HEALTH_RIGHT, NUMBER_Y);
        drawNumber(g, game.getArmorPercent(), ARMOR_RIGHT);
        drawPatch(g, percent, ARMOR_RIGHT, NUMBER_Y);
        drawPatch(g, chooseFace(game), FACE_X, BAR_Y);
    }

    /**
     * Copies Freedoom's status bar and paints over the parts this game never uses: the FRAGS
     * box (where Doom's ARMS panel also sits) and the right-hand table of ammo types. Both are
     * covered with the plain panel texture from beside Doomguy's face, row for row, so the
     * metal frame around them still lines up.
     */
    private static Patch blankUnusedPanels(Patch bar) {
        BufferedImage image = new BufferedImage(bar.width, bar.height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, bar.width, bar.height, bar.argb, 0, bar.width);
        for (int[] span : BLANKED_SPANS) {
            for (int y = 0; y < bar.height; y++) {
                for (int x = span[0]; x <= span[1]; x++) {
                    int sourceX = PANEL_TEXTURE_X + (x - span[0]) % PANEL_TEXTURE_WIDTH;
                    image.setRGB(x, y, bar.argb[y * bar.width + sourceX]);
                }
            }
        }
        return new Patch(image, bar.leftOffset, bar.topOffset);
    }

    /** Draws a number with its right edge at {@code right}, like Doom's status bar. */
    private void drawNumber(Graphics2D g, int value, int right) {
        int x = right;
        int remaining = Math.max(0, value);
        do {
            Patch digit = bigDigits[remaining % 10];
            x -= digit.width;
            drawPatch(g, digit, x, NUMBER_Y);
            remaining /= 10;
        } while (remaining > 0);
    }

    /**
     * Doomguy's face: bloodier as health drops, grinning while invincible, looking toward
     * a nearby enemy, wincing when hit, and dead at the end.
     */
    private Patch chooseFace(Game game) {
        int health = game.getHealthPercent();
        int pain = Math.min(4, (100 - health) * 5 / 101); // Doom's formula: 0 (healthy) to 4
        if (game.getState() == GameState.GAME_OVER || (game.isPlayerHurt() && game.getLives() == 0)) {
            return deadFace;
        }
        if (game.isPlayerHurt()) {
            return ouchFaces[pain];
        }
        if (game.isPowerUpActive()) {
            return godFace;
        }
        Enemy threat = nearestThreat(game);
        if (threat != null) {
            double angle = game.relativeAngle(threat);
            if (angle > Math.toRadians(30)) {
                return lookRightFaces[pain];
            }
            if (angle < -Math.toRadians(30)) {
                return lookLeftFaces[pain];
            }
        }
        // Idle: glance around every half second, picking a "random" direction from the clock.
        long slot = (long) (game.getTime() / GLANCE_TIME);
        int glance = (int) ((slot * 2654435761L >>> 16) % 3);
        return straightFaces[pain][glance];
    }

    private Enemy nearestThreat(Game game) {
        Enemy nearest = null;
        double best = Config.FACE_ALERT_RADIUS;
        for (Enemy e : game.getEnemies()) {
            if (!e.isShootable()) {
                continue;
            }
            double d = Math.hypot(e.getX() - game.getPlayer().getX(), e.getY() - game.getPlayer().getY());
            if (d < best) {
                best = d;
                nearest = e;
            }
        }
        return nearest;
    }

    private void drawOverlay(Graphics2D g, Game game) {
        String title;
        String hint;
        if (game.getState() == GameState.PAUSED) {
            title = "PAUSED";
            hint = "CLICK TO RESUME   Q - MAIN MENU";
        } else if (game.getState() == GameState.GAME_OVER) {
            title = "GAME OVER";
            hint = "PRESS ENTER FOR THE MAIN MENU";
        } else {
            return;
        }
        g.setColor(DIM);
        g.fillRect(0, 0, Config.RENDER_WIDTH, Config.VIEW_HEIGHT);
        int centerX = Config.RENDER_WIDTH / 2;
        int y = Config.VIEW_HEIGHT / 2 - DoomFont.lineHeight() * 4;
        DoomFont.drawCentered(g, title, centerX, y, 4);
        DoomFont.drawCentered(g, hint, centerX, y + DoomFont.lineHeight() * 6, S);
    }
}

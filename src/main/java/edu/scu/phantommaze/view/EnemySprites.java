package edu.scu.phantommaze.view;

import edu.scu.phantommaze.model.Enemy;
import edu.scu.phantommaze.model.EnemyType;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * Picks the right Freedoom picture for an enemy: which animation frame (walking, hurt,
 * dying) and which of the 8 viewing angles.
 *
 * <p>Doom sprite files are named {@code NAME + frame letter + angle}, e.g. {@code trooa1}
 * is the imp, frame A, seen from the front. Angle 0 means "same from every side" (used for
 * death frames). A name like {@code trooa2a8} is one picture used for angle 2, and flipped
 * for angle 8.
 */
public final class EnemySprites {

    /** Seconds each walking frame is shown. */
    private static final double WALK_FRAME_TIME = 0.15;

    /** How one enemy type is drawn. */
    private static final class Look {
        final Patch[][] walk; // [frame][angle 0-7]
        final Patch[] pain; // [angle 0-7]
        final Patch[][] death; // [frame][angle 0-7]
        final double hover; // sprite pixels above the floor (flying monsters)
        final boolean fullBright; // ignores fog (the lost soul is on fire)

        Look(String name, String walkFrames, char painFrame, String deathFrames, double hover,
                boolean fullBright) {
            this.walk = frames(name, walkFrames);
            this.pain = angles(name, painFrame);
            this.death = frames(name, deathFrames);
            this.hover = hover;
            this.fullBright = fullBright;
        }
    }

    private final Map<EnemyType, Look> looks = new EnumMap<>(EnemyType.class);

    /** Loads every enemy picture. */
    public EnemySprites() {
        looks.put(EnemyType.CACODEMON, new Look("head", "A", 'E', "GHIJKL", 18, false));
        looks.put(EnemyType.DEMON, new Look("sarg", "ABCD", 'H', "IJKLMN", 0, false));
        looks.put(EnemyType.LOST_SOUL, new Look("skul", "AB", 'E', "FGHIJK", 22, true));
        looks.put(EnemyType.IMP, new Look("troo", "ABCD", 'H', "IJKLM", 0, false));
    }

    /**
     * Chooses the picture for an enemy as seen from a viewer.
     *
     * @param e            the enemy
     * @param viewToEnemy  angle (radians) of the line from the viewer to the enemy
     * @return the picture to draw
     */
    public Patch frameFor(Enemy e, double viewToEnemy) {
        Look look = looks.get(e.getType());
        int angle = angleIndex(e.getFacing(), viewToEnemy);
        if (e.getState() == Enemy.State.DYING) {
            int frame = Math.min(look.death.length - 1, (int) (e.getDeathProgress() * look.death.length));
            return look.death[frame][angle];
        }
        if (e.isInPain()) {
            return look.pain[angle];
        }
        int frame = (int) (e.getAnimTime() / WALK_FRAME_TIME) % look.walk.length;
        return look.walk[frame][angle];
    }

    /**
     * How high above the floor the enemy floats, in sprite pixels (includes a gentle bob).
     *
     * @param e the enemy
     * @return the height
     */
    public double hoverFor(Enemy e) {
        Look look = looks.get(e.getType());
        if (look.hover == 0) {
            return 0;
        }
        return look.hover + 4 * Math.sin(e.getAnimTime() * 3.0);
    }

    /**
     * Tells whether this enemy glows instead of fading into the fog.
     *
     * @param e the enemy
     * @return true if it should be drawn at full brightness
     */
    public boolean isFullBright(Enemy e) {
        return looks.get(e.getType()).fullBright;
    }

    /**
     * Doom's formula for which side of a monster the viewer sees. 0 = its front,
     * then the angles go round in 45 degree steps.
     */
    private static int angleIndex(double facing, double viewToEnemy) {
        double turn = facing - viewToEnemy + Math.toRadians(202.5);
        double wrapped = turn - 2 * Math.PI * Math.floor(turn / (2 * Math.PI)); // into 0..2PI
        return (int) (wrapped / (Math.PI / 4)) & 7;
    }

    private static Patch[][] frames(String name, String letters) {
        Patch[][] result = new Patch[letters.length()][];
        for (int i = 0; i < letters.length(); i++) {
            result[i] = angles(name, letters.charAt(i));
        }
        return result;
    }

    /** Loads all 8 angles of one frame, using flipped pictures where Doom shares them. */
    private static Patch[] angles(String name, char frameLetter) {
        String f = String.valueOf(Character.toLowerCase(frameLetter));
        Patch[] result = new Patch[8];
        if (Assets.spriteExists(name + f + "0")) { // one picture for every angle
            Patch all = Assets.sprite(name + f + "0");
            Arrays.fill(result, all);
            return result;
        }
        for (int angle = 1; angle <= 8; angle++) {
            result[angle - 1] = findAngle(name, f, angle);
        }
        return result;
    }

    private static Patch findAngle(String name, String f, int angle) {
        if (Assets.spriteExists(name + f + angle)) {
            return Assets.sprite(name + f + angle);
        }
        for (int other = 1; other <= 8; other++) {
            if (Assets.spriteExists(name + f + angle + f + other)) {
                return Assets.sprite(name + f + angle + f + other); // stored as-is
            }
            if (Assets.spriteExists(name + f + other + f + angle)) {
                return Assets.sprite(name + f + other + f + angle).mirrored(); // stored flipped
            }
        }
        throw new IllegalArgumentException("No sprite for " + name + f + angle);
    }
}

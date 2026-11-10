package edu.scu.phantommaze.model;

import edu.scu.phantommaze.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameCombatTest {
    @Test
    void pistolCannotHitThroughAWall() {
        GameMap map = ModelTestMaps.arena();
        map.setCell(3, 1, GameMap.WALL);
        Game game = new Game(map, Difficulty.NORMAL);
        Enemy target = activate(game, 0, 5, 1);

        shoot(game);

        assertEquals(0, target.getPistolHits());
        assertEquals(2, game.getWeapons().getAmmo());
        assertEquals(0, game.getKills());
    }

    @Test
    void aimAssistanceCannotHitAnEnemyHiddenBesideTheCenterRay() {
        GameMap map = ModelTestMaps.arena();
        map.setCell(3, 3, GameMap.WALL);
        Game game = new Game(map, Difficulty.NORMAL);
        game.getPlayer().respawn(new Vector2(1.5, 2.8), 0);
        Enemy target = activate(game, 0, 4, 3);
        target.setDirection(0, -1);
        target.advance(0.4);
        target.setDirection(0, 0);

        shoot(game);

        assertEquals(0, target.getPistolHits());
        assertEquals(2, game.getWeapons().getAmmo());
    }

    @Test
    void diagonalShotCannotPassThroughATouchingWallCorner() {
        GameMap map = ModelTestMaps.arena();
        map.setCell(1, 2, GameMap.WALL);
        Game game = new Game(map, Difficulty.NORMAL);
        game.getPlayer().rotate(Math.PI / 4);
        Enemy target = activate(game, 0, 3, 3);

        shoot(game);

        assertEquals(0, target.getPistolHits());
        assertEquals(0, game.getKills());
    }

    @Test
    void shotAlongGridBoundaryCannotGrazeThroughWallEdge() {
        GameMap map = ModelTestMaps.arena();
        map.setCell(3, 2, GameMap.WALL);
        Game game = new Game(map, Difficulty.NORMAL);
        game.getPlayer().respawn(new Vector2(1.5, 3.0), 0);
        Enemy target = activate(game, 0, 4, 3);
        target.setDirection(0, -1);
        target.advance(0.5);
        target.setDirection(0, 0);

        shoot(game);

        assertEquals(0, target.getPistolHits());
    }

    @Test
    void aimAssistanceHitsVisibleEnemyBesideCenterRay() {
        Game game = new Game(ModelTestMaps.arena(), Difficulty.NORMAL);
        game.getPlayer().respawn(new Vector2(1.5, 1.8), 0);
        Enemy target = activate(game, 0, 4, 1);

        shoot(game);

        assertEquals(1, target.getPistolHits());
        assertEquals(0, game.getKills());
    }

    @Test
    void nearestVisibleEnemyTakesThreePistolHitsAndResetsInPen() {
        Game game = new Game(ModelTestMaps.arena(), Difficulty.NORMAL);
        Enemy nearest = activate(game, 0, 4, 1);
        Enemy behind = activate(game, 1, 7, 1);

        for (int hit = 1; hit <= Config.PISTOL_HITS_TO_ELIMINATE; hit++) {
            shoot(game);
            assertEquals(hit, nearest.getPistolHits());
            assertEquals(0, behind.getPistolHits());
            if (hit < Config.PISTOL_HITS_TO_ELIMINATE) {
                assertEquals(Enemy.State.ACTIVE, nearest.getState());
                game.getWeapons().update(Config.PISTOL_FIRE_DELAY);
            }
        }
        assertEquals(Enemy.State.DYING, nearest.getState());
        assertEquals(1, game.getKills());
        assertTrue(game.getWeapons().isReloading());
        assertEquals(0, game.getWeapons().getAmmo());

        game.update(Config.ENEMY_DEATH_TIME);

        assertEquals(Enemy.State.IN_PEN, nearest.getState());
        assertEquals(0, nearest.getPistolHits());
        assertFalse(nearest.isShootable());
    }

    @Test
    void powerUpProtectsContactButOnlyShotgunFireEliminates() {
        Game game = collectPowerUp();
        Enemy target = activate(game, 0, 2, 1);

        game.update(0);

        assertEquals(Config.PLAYER_LIVES, game.getLives());
        assertEquals(Enemy.State.ACTIVE, target.getState());
        assertEquals(0, game.getKills());
        assertEquals(Config.SHOTGUN_SHELLS, game.getWeapons().getAmmo());
        target.arriveAt(4, 1);
        shoot(game);
        assertEquals(Enemy.State.DYING, target.getState());
        assertEquals(1, game.getKills());
        assertEquals(Config.SHOTGUN_SHELLS - 1, game.getWeapons().getAmmo());
    }

    @Test
    void respawnedEnemyStaysFrightenedUntilPowerExpires() {
        Game game = collectPowerUp();
        Enemy target = activate(game, 0, 4, 1);
        shoot(game);

        game.update(Config.ENEMY_DEATH_TIME);
        assertEquals(Enemy.State.IN_PEN, target.getState());
        assertTrue(target.isFrightened());
        game.update(Difficulty.EASY.respawnDelay);
        assertEquals(Enemy.State.LEAVING_PEN, target.getState());
        game.update(1.6);
        assertEquals(Enemy.State.ACTIVE, target.getState());
        assertTrue(game.isPowerUpActive());
        assertTrue(target.isFrightened());

        game.update(game.getPowerUpRemaining() + 0.1);

        assertFalse(game.isPowerUpActive());
        assertFalse(target.isFrightened());
        assertEquals(Weapon.PISTOL, game.getWeapons().getCurrent());
    }

    @Test
    void pausePreservesReloadAndResumesItsRemainingTime() {
        Game game = new Game(ModelTestMaps.arena(), Difficulty.NORMAL);
        shoot(game);
        game.requestReload();
        game.update(0.25);
        double progress = game.getWeapons().getReloadProgress();
        double time = game.getTime();

        game.pause();
        game.update(100);

        assertEquals(progress, game.getWeapons().getReloadProgress());
        assertEquals(time, game.getTime());
        assertEquals(2, game.getWeapons().getAmmo());
        game.resume();
        game.update(Config.PISTOL_RELOAD_TIME - 0.25);
        assertFalse(game.getWeapons().isReloading());
        assertEquals(Config.PISTOL_CLIP_SIZE, game.getWeapons().getAmmo());
    }

    @Test
    void pickupStartsFullPowerDurationAndPausePreservesIt() {
        Game game = collectPowerUp();
        assertEquals(Difficulty.EASY.powerUpDuration, game.getPowerUpRemaining());
        assertEquals(100, game.getArmorPercent());
        assertTrue(game.getPowerUps().isEmpty());

        game.pause();
        game.update(100);
        assertEquals(Difficulty.EASY.powerUpDuration, game.getPowerUpRemaining());
        game.resume();
        game.update(0.5);
        assertEquals(Difficulty.EASY.powerUpDuration - 0.5, game.getPowerUpRemaining());
    }

    @Test
    void powerExpiryReturnsToEmptyPistolAndStartsReload() {
        Game game = new Game(ModelTestMaps.powerArena(), Difficulty.EASY);
        for (int shot = 0; shot < Config.PISTOL_CLIP_SIZE; shot++) {
            shoot(game);
            game.getWeapons().update(Config.PISTOL_FIRE_DELAY);
        }
        assertTrue(game.getWeapons().isReloading());
        game.getPlayer().move(1, 0, game.getMap());
        game.update(0);
        assertEquals(Weapon.SHOTGUN, game.getWeapons().getCurrent());
        assertFalse(game.getWeapons().isReloading());

        game.update(Difficulty.EASY.powerUpDuration);

        assertEquals(Weapon.PISTOL, game.getWeapons().getCurrent());
        assertEquals(0, game.getWeapons().getAmmo());
        assertTrue(game.getWeapons().isReloading());
        game.update(Config.PISTOL_RELOAD_TIME);
        assertEquals(Config.PISTOL_CLIP_SIZE, game.getWeapons().getAmmo());
    }

    @Test
    void expiredPowerCannotFireOneLastShotgunBlast() {
        Game game = collectPowerUp();
        Enemy target = activate(game, 0, 5, 1);
        game.setTriggerHeld(true);

        game.update(game.getPowerUpRemaining());

        assertEquals(Weapon.PISTOL, game.getWeapons().getCurrent());
        assertEquals(1, target.getPistolHits());
        assertEquals(0, game.getKills());
    }

    @Test
    void thirdContactEndsGameAfterHitFreeze() {
        Game game = new Game(ModelTestMaps.arena(), Difficulty.NORMAL);
        for (int hit = 1; hit <= Config.PLAYER_LIVES; hit++) {
            Enemy target = activate(game, 0, 1, 1);
            game.update(0);
            assertEquals(Config.PLAYER_LIVES - hit, game.getLives());
            assertEquals((int) Math.round((Config.PLAYER_LIVES - hit) * 100.0 / Config.PLAYER_LIVES),
                    game.getHealthPercent());
            assertTrue(game.isPlayerHurt());
            assertEquals(GameState.PLAYING, game.getState());
            game.update(Config.HIT_FREEZE_TIME);
            if (hit < Config.PLAYER_LIVES) {
                assertEquals(Enemy.State.IN_PEN, target.getState());
                assertTrue(game.isRespawnGraceActive());
                game.update(Config.RESPAWN_GRACE_TIME);
            }
        }
        assertEquals(GameState.GAME_OVER, game.getState());
    }

    @Test
    void powerUpSpeedsUpThePlayerUntilItEnds() {
        Game normal = new Game(ModelTestMaps.arena(), Difficulty.EASY);
        normal.getPlayer().move(1, 0, normal.getMap());
        Game powered = collectPowerUp();
        assertEquals(normal.getPlayer().getX(), powered.getPlayer().getX(), 1e-9);

        assertEquals(Config.PLAYER_MOVE_SPEED * 0.5, walkForward(normal, 0.5), 1e-9);
        assertEquals(Config.PLAYER_MOVE_SPEED * Config.POWER_UP_SPEED_MULTIPLIER * 0.5,
                walkForward(powered, 0.5), 1e-9);

        powered.getPlayer().respawn(new Vector2(1.5, 2.5), 0);
        powered.update(Difficulty.EASY.powerUpDuration);
        assertFalse(powered.isPowerUpActive());
        assertEquals(Config.PLAYER_MOVE_SPEED * 0.5, walkForward(powered, 0.5), 1e-9);
    }

    @Test
    void sixthSenseUsesWalkingDistanceAndIgnoresFrightenedEnemies() {
        Game open = new Game(ModelTestMaps.powerArena(), Difficulty.EASY);
        Enemy near = activate(open, 0, 1, 3); // two steps from the player through the open room
        open.update(0);
        assertTrue(open.threatLevel(near) > 0);

        GameMap walled = ModelTestMaps.powerArena();
        for (int x = 1; x <= 10; x++) {
            walled.setCell(x, 2, GameMap.WALL); // same spot, but only reachable the long way round
        }
        Game blocked = new Game(walled, Difficulty.EASY);
        Enemy behindWall = activate(blocked, 0, 1, 3);
        blocked.update(0);
        assertEquals(0, blocked.threatLevel(behindWall));

        open.getPlayer().move(1, 0, open.getMap());
        open.update(0); // picks up the power-up, which frightens the enemy
        assertTrue(near.isFrightened());
        assertEquals(0, open.threatLevel(near));
    }

    @Test
    void sixthSensePointsAtTheCornerAHiddenEnemyWillComeAround() {
        GameMap map = ModelTestMaps.arena(); // player at (1, 1) looking east along row 1
        for (int x = 1; x <= 11; x++) {
            if (x != 4) {
                map.setCell(x, 2, GameMap.WALL); // row 2 is a wall with one gap at column 4
            }
        }
        Game game = new Game(map, Difficulty.NORMAL);
        Enemy enemy = activate(game, 0, 4, 3); // below the wall, down-right of the player
        game.update(0);

        assertTrue(game.threatLevel(enemy) > 0);
        assertFalse(game.isInPlainSight(enemy));
        assertTrue(game.relativeAngle(enemy) > 0.5, "straight line goes through the wall");
        assertEquals(0.0, game.threatAngle(enemy), 1e-9, "it will appear at (4, 1), straight ahead");
    }

    @Test
    void killingAllFourInOneStreakStartsRampageWithBonusTimeAndReleasesThePen() {
        Game game = collectPowerUp();
        double before = game.getPowerUpRemaining();

        earnRampage(game);

        assertTrue(game.isRampageActive());
        assertEquals(Weapon.BFG, game.getWeapons().getCurrent());
        assertEquals(100, game.getArmorPercent());
        double streakTime = 3 * Config.SHOTGUN_FIRE_DELAY;
        assertEquals(before - streakTime + Config.RAMPAGE_BONUS_TIME, game.getPowerUpRemaining(), 1e-9);

        game.update(Config.ENEMY_DEATH_TIME); // the last kill finishes dying
        game.update(0.01);
        for (Enemy e : game.getEnemies()) {
            assertTrue(e.isShootable(), e.getType() + " should be on its way out of the pen");
            assertTrue(e.isFrightened());
        }
    }

    @Test
    void comboBreaksWhenTooSlowAndIgnoresTheSameEnemyTwice() {
        Game game = collectPowerUp();
        killInFront(game, 0);
        game.update(Config.COMBO_WINDOW + 0.1);
        assertEquals(0, game.getCombo().getCount(), "the streak timed out");

        killInFront(game, 1);
        game.update(Config.SHOTGUN_FIRE_DELAY);
        killInFront(game, 1); // same enemy again: does not count
        for (int i = 2; i < 4; i++) {
            game.update(Config.SHOTGUN_FIRE_DELAY);
            killInFront(game, i);
        }

        assertEquals(3, game.getCombo().getCount());
        assertFalse(game.isRampageActive());
        assertEquals(Weapon.SHOTGUN, game.getWeapons().getCurrent());
    }

    @Test
    void bfgBlastKillsEveryVisibleEnemyInTheConeOnly() {
        Game game = collectPowerUp(); // player at (2.5, 1.5) looking east
        earnRampage(game);
        game.update(Config.RAMPAGE_TRANSITION_TIME);
        game.getMap().setCell(8, 1, GameMap.WALL);

        shoot(game);
        game.update(Config.BFG_CHARGE_TIME - 0.05); // still charging
        Enemy ahead = activate(game, 0, 6, 1);
        Enemy aside = activate(game, 1, 2, 4); // about 100 degrees to the right: outside the cone
        Enemy behind = activate(game, 2, 1, 1);
        Enemy hidden = activate(game, 3, 9, 1); // straight ahead, but behind the wall at (8, 1)
        assertTrue(ahead.isShootable(), "nothing fires before the charge is done");
        game.update(0.1);

        assertFalse(ahead.isShootable());
        assertTrue(aside.isShootable());
        assertTrue(behind.isShootable());
        assertTrue(hidden.isShootable());
        assertEquals(1, game.getBlasts().size());
    }

    /** Walks forward for {@code seconds} and returns how far the player went. */
    private static double walkForward(Game game, double seconds) {
        Player player = game.getPlayer();
        double startX = player.getX();
        player.setIntents(1, 0, 0);
        game.update(seconds);
        player.setIntents(0, 0, 0);
        return player.getX() - startX;
    }

    /** Kills all four enemies one shotgun shell apart, completing the combo. */
    private static void earnRampage(Game game) {
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                game.update(Config.SHOTGUN_FIRE_DELAY);
            }
            killInFront(game, i);
        }
    }

    /** Puts an enemy right in front of the player and shoots it with the shotgun. */
    private static void killInFront(Game game, int index) {
        Enemy enemy = activate(game, index, 3, 1);
        shoot(game);
        assertEquals(Enemy.State.DYING, enemy.getState());
    }

    private static Game collectPowerUp() {
        Game game = new Game(ModelTestMaps.powerArena(), Difficulty.EASY);
        game.getPlayer().move(1, 0, game.getMap());
        game.update(0.1);
        return game;
    }

    private static Enemy activate(Game game, int index, int x, int y) {
        Enemy enemy = game.getEnemies().get(index);
        enemy.becomeActive();
        enemy.arriveAt(x, y);
        enemy.setDirection(0, 0);
        return enemy;
    }

    private static void shoot(Game game) {
        game.setTriggerHeld(true);
        game.update(0);
        game.setTriggerHeld(false);
    }
}

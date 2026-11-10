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

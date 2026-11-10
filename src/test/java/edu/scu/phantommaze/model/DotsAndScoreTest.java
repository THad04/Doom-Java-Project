package edu.scu.phantommaze.model;

import edu.scu.phantommaze.Config;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DotsAndScoreTest {

    @Test
    void eatingTheLastDotCompletesTheLevelWithALifeBonus() {
        Game game = new Game(ModelTestMaps.arena(), Difficulty.NORMAL); // its only dot is at (11, 4)
        assertEquals(1, game.getDotsLeft());

        game.getPlayer().respawn(new Vector2(11.5, 4.5), 0);
        game.update(0.01);

        assertEquals(0, game.getDotsLeft());
        assertEquals(GameState.LEVEL_COMPLETE, game.getState());
        assertEquals(Config.DOT_POINTS + Config.PLAYER_LIVES * Config.LIFE_BONUS_POINTS, game.getScore());
        assertEquals(GameMap.EMPTY, game.getMap().getCell(11, 4));
    }

    @Test
    void eatenDotsStayEatenAfterLosingALife() {
        Game game = new Game(twoDotArena(), Difficulty.NORMAL);
        game.getPlayer().respawn(new Vector2(10.5, 4.5), 0);
        game.update(0.01);
        assertEquals(1, game.getDotsLeft());

        Enemy enemy = game.getEnemies().get(0);
        enemy.becomeActive();
        enemy.arriveAt(10, 4); // catches the player
        game.update(0.01);
        game.update(Config.HIT_FREEZE_TIME);

        assertEquals(Config.PLAYER_LIVES - 1, game.getLives());
        assertEquals(1, game.getDotsLeft());
        assertEquals(GameMap.EMPTY, game.getMap().getCell(10, 4));
    }

    @Test
    void frightenedKillsDoubleUpTo1600ThenRampageKillsAreFlat() {
        Game game = collectPowerUp();
        int score = Config.POWER_UP_POINTS;
        assertEquals(score, game.getScore());

        int[] expected = { 200, 400, 800, 1600 };
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                game.update(Config.SHOTGUN_FIRE_DELAY);
            }
            Enemy e = activate(game, i, 3, 1);
            e.setFrightened(true);
            shoot(game);
            score += expected[i];
            assertEquals(score, game.getScore(), "kill " + (i + 1));
        }
        assertTrue(game.isRampageActive(), "four quick kills also complete the combo");

        game.update(Config.RAMPAGE_TRANSITION_TIME);
        shoot(game);
        game.update(Config.BFG_CHARGE_TIME - 0.05);
        activate(game, 0, 5, 1).setFrightened(true);
        for (int i = 1; i < 4; i++) {
            activate(game, i, 1, 1); // behind the player: out of the cone
        }
        game.update(0.1);
        assertEquals(score + Config.BFG_KILL_POINTS, game.getScore());
    }

    @Test
    void pistolKillWithoutPowerUpScores100() {
        Game game = new Game(ModelTestMaps.arena(), Difficulty.NORMAL);
        for (int shot = 0; shot < Config.PISTOL_HITS_TO_ELIMINATE; shot++) {
            if (shot > 0) {
                game.update(Config.PISTOL_FIRE_DELAY);
            }
            activate(game, 0, 4, 1);
            shoot(game);
        }
        assertEquals(Enemy.State.DYING, game.getEnemies().get(0).getState());
        assertEquals(Config.PISTOL_KILL_POINTS, game.getScore());
    }

    @Test
    void cacodemonSpeedsUpWhenFewDotsAreLeft() {
        double dt = 0.2; // short enough that it cannot reach the next tile center and turn
        double normal = cacodemonStep(new Game(manyDotArena(), Difficulty.NORMAL), dt);
        double elroy = cacodemonStep(new Game(ModelTestMaps.arena(), Difficulty.NORMAL), dt); // 1 dot left

        assertEquals(Difficulty.NORMAL.enemySpeed * dt, normal, 1e-9);
        assertEquals(Difficulty.NORMAL.enemySpeed * (1 + 2 * Config.ELROY_SPEED_STEP) * dt, elroy, 1e-9);
    }

    /** How far the cacodemon moves in one step from the middle of the arena. */
    private static double cacodemonStep(Game game, double dt) {
        Enemy caco = activate(game, 0, 6, 3);
        assertEquals(EnemyType.CACODEMON, caco.getType());
        double x = caco.getX();
        double y = caco.getY();
        game.update(dt);
        return Math.hypot(caco.getX() - x, caco.getY() - y);
    }

    /** The test arena with more dots than {@link Config#ELROY_DOTS}, so the cacodemon stays at normal speed. */
    private static GameMap manyDotArena() {
        return new GameMap(List.of(
                "#############",
                "#P          #",
                "#...........#",
                "#           #",
                "#...........#",
                "#####-#######",
                "####GGG######",
                "#############"));
    }

    private static GameMap twoDotArena() {
        return new GameMap(List.of(
                "#############",
                "#P          #",
                "#           #",
                "#           #",
                "#         ..#",
                "#####-#######",
                "####GGG######",
                "#############"));
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

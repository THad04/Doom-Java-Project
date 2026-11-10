package edu.scu.phantommaze.model;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

class EnemyNavigationTest {
    @Test
    void leavingEnemyMayReverseToTakeTheShortestPenExit() {
        GameMap map = new GameMap(List.of(
                "#######",
                "#  P  #",
                "###-###",
                "# GGG #",
                "#######"));
        Enemy enemy = new Enemy(EnemyType.CACODEMON, 3, 3);
        enemy.startLeaving();
        enemy.setDirection(0, 1);
        EnemyAI ai = ai(map, enemy);

        ai.move(enemy, 2, false);

        assertEquals(Enemy.State.ACTIVE, enemy.getState());
        assertEquals(3.5, enemy.getX());
        assertEquals(1.5, enemy.getY());
    }

    @Test
    void enemyAlreadyAtPenExitBecomesActiveWithoutMovement() {
        GameMap map = ModelTestMaps.arena();
        Enemy enemy = new Enemy(EnemyType.CACODEMON, 5, 6);
        enemy.startLeaving();
        enemy.arriveAt(5, 4);

        ai(map, enemy).move(enemy, 0, false);

        assertEquals(Enemy.State.ACTIVE, enemy.getState());
        assertEquals(5.5, enemy.getX());
        assertEquals(4.5, enemy.getY());
    }

    @Test
    @Timeout(1)
    void boxedEnemyReturnsWithoutMovingOrLooping() {
        GameMap map = new GameMap(List.of("#####", "#P###", "### #", "#####"));
        Enemy enemy = new Enemy(EnemyType.CACODEMON, 3, 2);
        enemy.becomeActive();
        EnemyAI ai = new EnemyAI(map, new Player(map.getPlayerStart(), 0), List.of(enemy), new int[] {1, 1});

        ai.move(enemy, 10, false);
        ai.move(enemy, 0, false);

        assertEquals(3.5, enemy.getX());
        assertEquals(2.5, enemy.getY());
        assertEquals(0, enemy.getDirX());
        assertEquals(0, enemy.getDirY());
    }

    @Test
    void reversalAtCenterKeepsItsTileAndActuallyMovesBackward() {
        GameMap map = ModelTestMaps.arena();
        Enemy enemy = new Enemy(EnemyType.CACODEMON, 5, 3);
        enemy.becomeActive();
        enemy.setDirection(1, 0);
        enemy.reverse();
        assertEquals(5, enemy.getTileX());
        assertEquals(3, enemy.getTileY());

        ai(map, enemy).move(enemy, 0.25, false);

        assertEquals(5.25, enemy.getX());
        assertEquals(3.5, enemy.getY());
    }

    @Test
    void reversalBetweenCentersHeadsBackWithoutTeleporting() {
        GameMap map = ModelTestMaps.arena();
        Enemy enemy = new Enemy(EnemyType.CACODEMON, 5, 3);
        enemy.becomeActive();
        enemy.setDirection(1, 0);
        enemy.advance(0.25);
        enemy.reverse();
        assertEquals(5.75, enemy.getX());

        ai(map, enemy).move(enemy, 0.125, false);

        assertEquals(5.625, enemy.getX());
        assertEquals(3.5, enemy.getY());
    }

    @Test
    void pathfinderRoutesAroundWallsAndClosesThePenToActiveEnemies() {
        GameMap map = new GameMap(List.of(
                "#######",
                "#P#   #",
                "# # # #",
                "#   # #",
                "###-###",
                "##GGG##",
                "#######"));
        Pathfinder pathfinder = new Pathfinder(map);
        int[] field = pathfinder.newField();

        pathfinder.distancesFrom(1, 1, false, field);

        assertEquals(6, field[pathfinder.index(3, 1)]);
        assertEquals(Pathfinder.UNREACHABLE, field[pathfinder.index(3, 4)]);
        assertEquals(Pathfinder.UNREACHABLE, field[pathfinder.index(3, 5)]);
        assertFalse(pathfinder.isWalkable(3, 5, false));
        pathfinder.distancesFrom(1, 1, true, field);
        assertEquals(5, field[pathfinder.index(3, 4)]);
        assertEquals(6, field[pathfinder.index(3, 5)]);
    }

    @Test
    void nearestMazeTileUsesEuclideanDistanceRatherThanFirstSquareRing() {
        GameMap map = new GameMap(List.of(
                "#### ####",
                "# #######",
                "#########",
                "#########",
                "#########",
                "#########",
                "#########",
                "#########",
                "#########"));
        Pathfinder pathfinder = new Pathfinder(map);

        assertEquals(pathfinder.index(4, 0), pathfinder.nearestMazeTile(4, 4));
    }

    private static EnemyAI ai(GameMap map, Enemy enemy) {
        return new EnemyAI(map, new Player(map.getPlayerStart(), 0), List.of(enemy), map.findPenExit());
    }
}

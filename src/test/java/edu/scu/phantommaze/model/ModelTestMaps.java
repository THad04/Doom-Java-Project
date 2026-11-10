package edu.scu.phantommaze.model;

import java.util.List;

final class ModelTestMaps {
    private ModelTestMaps() { }

    static GameMap arena() {
        return arena("#P          #");
    }

    static GameMap powerArena() {
        return arena("#Po         #");
    }

    private static GameMap arena(String firstRow) {
        return new GameMap(List.of(
                "#############",
                firstRow,
                "#           #",
                "#           #",
                "#          .#", // one dot, so taking the power-up never finishes the level
                "#####-#######",
                "####GGG######",
                "#############"));
    }
}

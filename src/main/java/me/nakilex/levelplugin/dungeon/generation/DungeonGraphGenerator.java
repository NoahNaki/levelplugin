package me.nakilex.levelplugin.dungeon.generation;

import java.util.List;
import java.util.Random;

/** Strategy interface for generating dungeon graph topology. */
public interface DungeonGraphGenerator {
    List<GridNode> generate(int size, Random random);
}

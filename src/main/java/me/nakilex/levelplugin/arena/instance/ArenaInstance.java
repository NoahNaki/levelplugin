package me.nakilex.levelplugin.arena.instance;

import org.bukkit.Location;
import org.bukkit.World;

/** Simple data holder describing an active arena instance world. */
public final class ArenaInstance {
    private final World world;
    private final Location spawnOne;
    private final Location spawnTwo;

    ArenaInstance(World world, Location spawnOne, Location spawnTwo) {
        this.world = world;
        this.spawnOne = spawnOne.clone();
        this.spawnTwo = spawnTwo.clone();
    }

    public World getWorld() {
        return world;
    }

    public Location getFirstSpawn() {
        return spawnOne.clone();
    }

    public Location getSecondSpawn() {
        return spawnTwo.clone();
    }
}

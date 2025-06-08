package me.nakilex.levelplugin.world;

import me.nakilex.levelplugin.Main;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Periodically spawns falling leaf particles on random leaf blocks in loaded chunks.
 * Uses a small sample per chunk to avoid overcrowding and reduce performance impact.
 */
public class LeafParticleTask extends BukkitRunnable {
    private final Main plugin;
    private final Random random = new Random();

    public LeafParticleTask(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                // Increase attempts so the effect is clearly visible while testing
                for (int i = 0; i < 5; i++) {
                    int x = random.nextInt(16);
                    int z = random.nextInt(16);
                    int worldX = (chunk.getX() << 4) + x;
                    int worldZ = (chunk.getZ() << 4) + z;
                    int highest = world.getHighestBlockYAt(worldX, worldZ);
                    for (int y = highest; y >= world.getMinHeight(); y--) {
                        Block block = world.getBlockAt(worldX, y, worldZ);
                        if (isLeaf(block.getType())) {
                            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                            spawnLeafParticle(world, loc, block.getType());
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean isLeaf(Material mat) {
        return mat.toString().endsWith("_LEAVES");
    }

    private void spawnLeafParticle(World world, Location loc, Material leafType) {
        Particle particle;
        // Match particle to leaf type if possible
        if (leafType == Material.CHERRY_LEAVES) {
            particle = Particle.CHERRY_LEAVES;
        } else if (leafType == Material.PALE_OAK_LEAVES) {
            particle = Particle.PALE_OAK_LEAVES;
        } else {
            // Generic leaf particle
            particle = Particle.CHERRY_LEAVES;
        }
        // Spawn a few particles to exaggerate the effect for testing
        world.spawnParticle(particle, loc, 3, 0.1, 0.0, 0.1, 0);
    }
}

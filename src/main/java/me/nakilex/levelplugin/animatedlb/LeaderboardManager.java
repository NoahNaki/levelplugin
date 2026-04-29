package me.nakilex.levelplugin.animatedlb;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LeaderboardManager {
    private final JavaPlugin plugin;
    private AnimatedLeaderboard board;
    private LeaderboardDataProvider provider = new MockLeaderboardDataProvider();

    public LeaderboardManager(JavaPlugin plugin) { this.plugin = plugin; }

    public void reload() { plugin.reloadConfig(); }

    public boolean spawn() {
        remove();
        FileConfiguration cfg = plugin.getConfig();
        String worldName = cfg.getString("animatedlb.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return false;
        Location origin = new Location(world, cfg.getDouble("animatedlb.x"), cfg.getDouble("animatedlb.y"), cfg.getDouble("animatedlb.z"), (float) cfg.getDouble("animatedlb.yaw"), 0f);
        board = new AnimatedLeaderboard(plugin, provider, origin,
                (float) cfg.getDouble("animatedlb.scale", 0.85),
                cfg.getInt("animatedlb.cycle-duration", 200),
                cfg.getInt("animatedlb.row-count", 10),
                cfg.getDouble("animatedlb.animation-speed", 1.0));
        board.spawn();
        return true;
    }

    public void next() { if (board != null) board.next(); }
    public void remove() { if (board != null) { board.remove(); board = null; } }
}

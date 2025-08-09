package me.nakilex.levelplugin.customscreenmenu.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SchedulerAdapter {
    private final JavaPlugin plugin;
    private final boolean folia;

    public SchedulerAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folia = Bukkit.getServer().getVersion().toLowerCase().contains("folia");
    }

    public void runTask(Runnable task) {
        if (folia) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runTaskTimer(Runnable task, long delay, long period) {
        if (folia) {
            plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), delay, period);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    public void runEntityTask(Player player, Runnable task) {
        if (folia) {
            player.getScheduler().run(plugin, t -> task.run(), null, 0L);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}

package me.nakilex.levelplugin.cursormenu;

import me.nakilex.levelplugin.cursormenu.scheduler.SchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages spawning and animating item showcase displays.
 */
public class ItemShowcaseManager {
    private final Plugin plugin;
    private final SchedulerAdapter scheduler;
    private final Map<UUID, ItemDisplay> displays = new HashMap<>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public ItemShowcaseManager(Plugin plugin, SchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    public void startShowcase(Player player, ItemStack item) {
        stopShowcase(player);
        Location loc = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(2)).add(0, 1.0, 0);
        ItemDisplay display = player.getWorld().spawn(loc, ItemDisplay.class, d -> d.setItemStack(item));
        displays.put(player.getUniqueId(), display);
        BukkitTask task = scheduler.runRepeating(plugin, () -> {
            display.setRotation(display.getYaw() + 5f, 0f);
            display.teleport(display.getLocation().add(0, Math.sin(System.currentTimeMillis() / 200.0) * 0.01, 0));
        }, 1L);
        tasks.put(player.getUniqueId(), task);
    }

    public void stopShowcase(Player player) {
        UUID id = player.getUniqueId();
        ItemDisplay display = displays.remove(id);
        if (display != null) display.remove();
        BukkitTask task = tasks.remove(id);
        if (task != null) task.cancel();
    }

    public void stopAll() {
        displays.values().forEach(ItemDisplay::remove);
        displays.clear();
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
    }
}

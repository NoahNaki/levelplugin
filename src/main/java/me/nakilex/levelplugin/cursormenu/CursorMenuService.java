package me.nakilex.levelplugin.cursormenu;

import me.nakilex.levelplugin.cursormenu.scheduler.SchedulerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.Duration;
import java.util.*;

/**
 * Loads menu configurations, tracks open sessions and delegates to the
 * BetterHud API for rendering.
 */
public class CursorMenuService implements Listener {
    private final JavaPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final Map<String, MenuDefinition> menus = new HashMap<>();
    private final Map<UUID, MenuSession> sessions = new HashMap<>();
    private final ItemShowcaseManager showcaseManager;

    public CursorMenuService(JavaPlugin plugin, SchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.showcaseManager = new ItemShowcaseManager(plugin, scheduler);
    }

    public void reloadMenus() {
        closeAllMenus();
        showcaseManager.stopAll();
        menus.clear();
        File dir = new File(plugin.getDataFolder(), "menus");
        if (!dir.exists()) {
            dir.mkdirs();
            plugin.saveResource("menus/example.yml", false);
        } else {
            File example = new File(dir, "example.yml");
            if (!example.exists()) {
                plugin.saveResource("menus/example.yml", false);
            }
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                String id = cfg.getString("id");
                String title = cfg.getString("title", id);
                if (id == null || id.isBlank()) {
                    plugin.getLogger().warning("Menu file " + f.getName() + " is missing 'id'");
                    continue;
                }
                menus.put(id.toLowerCase(), new MenuDefinition(id, title, Collections.emptyList()));
            } catch (Exception ex) {
                plugin.getLogger().severe("Failed to load menu " + f.getName() + ": " + ex.getMessage());
            }
        }
    }

    public Set<String> getMenuIds() {
        return new HashSet<>(menus.keySet());
    }

    public void openMenu(Player player, String id) {
        MenuDefinition def = menus.get(id.toLowerCase());
        if (def == null) {
            player.sendMessage("Unknown menu: " + id);
            return;
        }
        // Render a simple title using Adventure as a placeholder for a full BetterHud overlay
        Title title = Title.title(
                Component.text(def.getTitle()),
                Component.text("Use /cursormenu stop to close"),
                Title.Times.times(Duration.ZERO, Duration.ofMinutes(5), Duration.ZERO)
        );
        player.showTitle(title);
        sessions.put(player.getUniqueId(), new MenuSession(player.getUniqueId(), def));
        player.sendMessage("Opened menu: " + def.getId());
    }

    public void closeMenu(Player player) {
        sessions.remove(player.getUniqueId());
        player.clearTitle();
        player.sendMessage("Closed menu.");
    }

    public void closeAllMenus() {
        for (UUID id : new ArrayList<>(sessions.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) closeMenu(p);
        }
    }

    public ItemShowcaseManager getShowcaseManager() {
        return showcaseManager;
    }

    public void shutdown() {
        closeAllMenus();
        showcaseManager.stopAll();
    }

    private void cleanup(Player player) {
        closeMenu(player);
        showcaseManager.stopShowcase(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cleanup(e.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        cleanup(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        cleanup(e.getEntity());
    }
}

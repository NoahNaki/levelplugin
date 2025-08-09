package me.nakilex.levelplugin.customscreenmenu.menu;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class MenuManager {
    private final JavaPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final Map<String, MenuDefinition> menus = new HashMap<>();
    private final Map<UUID, MenuSession> sessions = new HashMap<>();
    private final File menuFolder;

    public MenuManager(JavaPlugin plugin, SchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.menuFolder = new File(plugin.getDataFolder(), "menus");
    }

    public void loadMenus() {
        menus.clear();
        if (!menuFolder.exists()) {
            menuFolder.mkdirs();
        }
        File[] files = menuFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id");
            String title = config.getString("title", id);
            if (id == null) {
                plugin.getLogger().warning("Menu file " + file.getName() + " missing id");
                continue;
            }
            menus.put(id.toLowerCase(Locale.ROOT), new MenuDefinition(id, title));
        }
    }

    public void reload() {
        closeAll();
        loadMenus();
    }

    public void openMenu(Player player, String id) {
        MenuDefinition def = menus.get(id.toLowerCase(Locale.ROOT));
        if (def == null) {
            player.sendMessage("Unknown menu: " + id);
            return;
        }
        MenuSession session = sessions.computeIfAbsent(player.getUniqueId(), u -> new MenuSession());
        session.setMenu(def);
        player.sendMessage("Opened menu " + def.title());
    }

    public void closeMenu(Player player) {
        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        session.setMenu(null);
        session.getShowcase().ifPresent(e -> e.remove());
    }

    public void startShowcase(Player player, ItemStack stack) {
        MenuSession session = sessions.computeIfAbsent(player.getUniqueId(), u -> new MenuSession());
        stopShowcase(player);
        ItemDisplay display = player.getWorld().spawn(player.getLocation().add(player.getLocation().getDirection().normalize().multiply(2)).add(0,1,0), ItemDisplay.class, e -> {
            e.setItemStack(stack);
        });
        session.setShowcase(display);
        scheduler.runTaskTimer(() -> {
            if (display.isValid()) {
                display.setRotation(display.getLocation().getYaw() + 5, display.getLocation().getPitch());
            }
        }, 0L, 1L);
    }

    public void stopShowcase(Player player) {
        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        session.getShowcase().ifPresent(ItemDisplay::remove);
        session.setShowcase(null);
    }

    public void closeAll() {
        for (UUID id : new ArrayList<>(sessions.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                closeMenu(p);
            }
        }
        sessions.clear();
    }

    public Collection<String> getMenuIds() {
        return Collections.unmodifiableCollection(menus.keySet());
    }

    public void handleQuit(Player player) {
        closeMenu(player);
        stopShowcase(player);
    }
}

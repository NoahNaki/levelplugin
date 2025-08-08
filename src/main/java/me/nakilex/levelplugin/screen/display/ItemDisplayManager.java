package me.nakilex.levelplugin.screen.display;

import me.nakilex.levelplugin.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows rotating item displays in front of players. The implementation focuses
 * on being thread-safe and easily reusable.
 */
public class ItemDisplayManager {
    private final Main plugin;
    private final Map<String, ItemStack> templates = new ConcurrentHashMap<>();
    private final Map<UUID, ItemDisplay> active = new ConcurrentHashMap<>();

    public ItemDisplayManager(Main plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        templates.clear();
        File file = new File(plugin.getDataFolder(), "cursor-items.yml");
        if (!file.exists()) {
            plugin.saveResource("cursor-items.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            Material mat = Material.matchMaterial(cfg.getString(key + ".material", "STONE"));
            if (mat != null) {
                ItemStack stack = new ItemStack(mat);
                stack.editMeta(meta -> meta.setCustomModelData(cfg.getInt(key + ".custom-model-data", 0)));
                templates.put(key.toLowerCase(), stack);
            }
        }
    }

    public void showItem(Player player, String id) {
        ItemStack stack = templates.get(id.toLowerCase());
        if (stack == null) return;
        Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        ItemDisplay display = player.getWorld().spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(stack);
            d.setBillboard(Display.Billboard.CENTER);
        });
        active.put(player.getUniqueId(), display);
        startRotation(display);
    }

    private void startRotation(ItemDisplay display) {
        new BukkitRunnable() {
            double angle = 0;
            @Override public void run() {
                if (display.isDead()) { cancel(); return; }
                angle += Math.toRadians(3);
                Transformation t = display.getTransformation();
                t.getLeftRotation().setY(angle);
                display.setTransformation(t);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void hideItem(Player player) {
        ItemDisplay disp = active.remove(player.getUniqueId());
        if (disp != null) disp.remove();
    }

    public String getPlayerActiveItemId(Player player) {
        ItemDisplay disp = active.get(player.getUniqueId());
        if (disp == null) return null;
        for (Map.Entry<String, ItemStack> e : templates.entrySet()) {
            if (e.getValue().isSimilar(disp.getItemStack())) {
                return e.getKey();
            }
        }
        return null;
    }

    public Iterable<String> getAllItemIds() {
        return templates.keySet();
    }
}

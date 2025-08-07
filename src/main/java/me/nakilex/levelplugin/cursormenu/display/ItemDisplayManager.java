package me.nakilex.levelplugin.cursormenu.display;

import me.nakilex.levelplugin.cursormenu.util.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for simple item displays. Displays are spawned in front of a player
 * and can execute configured commands when interacted with.
 */
public class ItemDisplayManager implements DisplayManager<String>, Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, ItemDisplay> active = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeIds = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> commandMap = new ConcurrentHashMap<>();
    private FileConfiguration config;

    public ItemDisplayManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = ConfigUtils.loadConfig(plugin, "cursormenu/items.yml");
    }

    @Override
    public void show(Player player, String id) {
        hide(player);

        ConfigurationSection section = config.getConfigurationSection(id);
        if (section == null) return;

        String matName = section.getString("material", "STONE");
        Material mat = Material.matchMaterial(matName);
        final Material material = mat != null ? mat : Material.STONE;

        double forward = section.getDouble("forward", 2.0);
        double x = section.getDouble("offset.x", 0.0);
        double y = section.getDouble("offset.y", 0.0);
        double z = section.getDouble("offset.z", 0.0);

        Location loc = player.getEyeLocation().clone();
        loc.add(player.getLocation().getDirection().normalize().multiply(forward));
        loc.add(x, y, z);

        ItemDisplay display = player.getWorld().spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(material));
            d.setBillboard(Billboard.CENTER);
        });

        UUID uuid = player.getUniqueId();
        active.put(uuid, display);
        activeIds.put(uuid, id);
        List<String> commands = section.getStringList("commands");
        if (!commands.isEmpty()) {
            commandMap.put(display.getUniqueId(), commands);
        }
    }

    @Override
    public void hide(Player player) {
        UUID uuid = player.getUniqueId();
        ItemDisplay display = active.remove(uuid);
        if (display != null) {
            commandMap.remove(display.getUniqueId());
            display.remove();
        }
        activeIds.remove(uuid);
    }

    @Override
    public void reload() {
        this.config = ConfigUtils.loadConfig(plugin, "cursormenu/items.yml");
    }

    @Override
    public Set<String> getAllIds() {
        return config.getKeys(false);
    }

    @Override
    public void cleanup(Player player) {
        hide(player);
    }

    public String getPlayerActiveItemId(Player player) {
        return activeIds.get(player.getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        List<String> commands = commandMap.get(event.getRightClicked().getUniqueId());
        if (commands == null) return;

        Player player = event.getPlayer();
        for (String cmd : commands) {
            Bukkit.dispatchCommand(player, cmd.replace("%player%", player.getName()));
        }
        event.setCancelled(true);
    }
}

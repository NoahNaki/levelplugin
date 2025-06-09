package me.nakilex.levelplugin.fakeblock;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages ModelGates loaded from a configuration file.
 */
public class ModelGateManager implements Listener {

    private final Main plugin;
    private final Map<String, ModelGate> gates = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public ModelGateManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadFromConfig();
    }

    private void loadFromConfig() {
        file = new File(plugin.getDataFolder(), "modelgates.yml");
        if (!file.exists()) {
            plugin.saveResource("modelgates.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection("gates")) return;
        for (String key : config.getConfigurationSection("gates").getKeys(false)) {
            String base = "gates." + key + ".";
            World world = Bukkit.getWorld(config.getString(base + "world"));
            if (world == null) continue;
            double x = config.getDouble(base + "x");
            double y = config.getDouble(base + "y");
            double z = config.getDouble(base + "z");
            String open = config.getString(base + "open_model", "");
            String closed = config.getString(base + "closed_model", "");
            boolean state = config.getBoolean(base + "closed", true);
            ModelGate gate = new ModelGate(key, new Location(world, x, y, z), open, closed, state);
            gate.spawnEntities();
            gates.put(key.toLowerCase(), gate);
        }
        updateAll();
    }

    private void saveConfig() {
        config.set("gates", null);
        for (ModelGate gate : gates.values()) {
            String base = "gates." + gate.getId() + ".";
            Location loc = gate.getLocation();
            config.set(base + "world", loc.getWorld().getName());
            config.set(base + "x", loc.getX());
            config.set(base + "y", loc.getY());
            config.set(base + "z", loc.getZ());
            config.set(base + "open_model", gate.getOpenModel());
            config.set(base + "closed_model", gate.getClosedModel());
            config.set(base + "closed", gate.isDefaultClosed());
        }
        try { config.save(file); } catch (Exception e) { e.printStackTrace(); }
    }

    public void createGate(ModelGate gate) {
        gates.put(gate.getId(), gate);
        gate.spawnEntities();
        saveConfig();
        updateAll();
    }

    public boolean toggleGate(Player player, String id) {
        ModelGate gate = gates.get(id.toLowerCase());
        if (gate == null) return false;
        gate.toggle(player.getUniqueId());
        gate.apply(player, plugin);
        return true;
    }

    public boolean removeGate(String id) {
        ModelGate gate = gates.remove(id.toLowerCase());
        if (gate == null) return false;
        gate.removeAll();
        saveConfig();
        updateAll();
        return true;
    }

    /** Remove all gates and despawn their entities. */
    public void removeAllGates() {
        for (ModelGate gate : gates.values()) {
            gate.removeAll();
        }
    }

    /** Returns the ids of all defined gates. */
    public java.util.Set<String> getGateIds() {
        return new java.util.HashSet<>(gates.keySet());
    }

    public void updatePlayer(Player player) {
        for (ModelGate gate : gates.values()) {
            gate.apply(player, plugin);
        }
    }

    public void updateAll() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            updatePlayer(p);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updatePlayer(event.getPlayer());
    }
}

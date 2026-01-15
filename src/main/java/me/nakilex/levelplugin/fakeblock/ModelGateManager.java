package me.nakilex.levelplugin.fakeblock;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.npc.system.NpcTagUtil;
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
    private final Map<java.util.UUID, ModelGate> entityMap = new HashMap<>();
    /** Map of block locations to their gate for quick lookup from interact events. */
    private final Map<org.bukkit.Location, ModelGate> locationMap = new HashMap<>();
    /** Gate ids that are currently hidden from the world. */
    private final java.util.Set<String> hidden = new java.util.HashSet<>();
    private final FastTravelManager fastTravelManager;
    private File file;
    private FileConfiguration config;

    public ModelGateManager(Main plugin) {
        this.plugin = plugin;
        this.fastTravelManager = plugin.getFastTravelManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadFromConfig();
    }

    private static org.bukkit.Location blockLoc(org.bukkit.Location loc) {
        return new org.bukkit.Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private void registerEntities(ModelGate gate) {
        if (gate.getOpenEntity() != null) {
            entityMap.put(gate.getOpenEntity().getUniqueId(), gate);
        }
        if (gate.getClosedEntity() != null) {
            entityMap.put(gate.getClosedEntity().getUniqueId(), gate);
        }
        locationMap.put(blockLoc(gate.getLocation()), gate);
    }

    private void unregisterEntities(ModelGate gate) {
        if (gate.getOpenEntity() != null) {
            entityMap.remove(gate.getOpenEntity().getUniqueId());
        }
        if (gate.getClosedEntity() != null) {
            entityMap.remove(gate.getClosedEntity().getUniqueId());
        }
        locationMap.remove(blockLoc(gate.getLocation()));
    }

    private void loadFromConfig() {
        file = new File(plugin.getDataFolder(), "modelgates.yml");
        if (!file.exists()) {
            try {
                // Try to copy a default file from the jar if it exists
                plugin.saveResource("modelgates.yml", false);
            } catch (IllegalArgumentException ignored) {
                // If the resource isn't packaged, create an empty file so the plugin can still run
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
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
            // ensure no lingering furniture is at this location before spawning
            gate.removeAll();
            gate.spawnEntities(plugin);
            registerEntities(gate);
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
        gate.spawnEntities(plugin);
        registerEntities(gate);
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
        unregisterEntities(gate);
        gate.removeAll();
        locationMap.remove(blockLoc(gate.getLocation()));
        saveConfig();
        updateAll();
        return true;
    }

    /** Remove all gates and despawn their entities. */
    public void removeAllGates() {
        for (ModelGate gate : gates.values()) {
            unregisterEntities(gate);
            gate.removeAll();
        }
        locationMap.clear();
    }

    /** Temporarily hide or show a gate without removing it from configuration. */
    public void setGateHidden(String id, boolean hide) {
        ModelGate gate = gates.get(id.toLowerCase());
        if (gate == null) return;
        if (hide) {
            hidden.add(gate.getId());
            unregisterEntities(gate);
            gate.removeAll();
        } else {
            hidden.remove(gate.getId());
            gate.spawnEntities(plugin);
            registerEntities(gate);
            updateAll();
        }
    }

    /** Returns the ids of all defined gates. */
    public java.util.Set<String> getGateIds() {
        return new java.util.HashSet<>(gates.keySet());
    }

    /** Returns all defined gates. */
    public java.util.Collection<ModelGate> getGates() {
        return new java.util.ArrayList<>(gates.values());
    }

    /** Get gate by id or null if it doesn't exist. */
    public ModelGate getGate(String id) {
        if (id == null) return null;
        return gates.get(id.toLowerCase());
    }

    /** Returns the gate associated with this entity or null. */
    public ModelGate getGateByEntity(org.bukkit.entity.Entity entity) {
        return entityMap.get(entity.getUniqueId());
    }

    /** Get gate located at the given block location. */
    public ModelGate getGateAt(org.bukkit.Location location) {
        return locationMap.get(blockLoc(location));
    }

    public void updatePlayer(Player player) {
        for (ModelGate gate : gates.values()) {
            if (hidden.contains(gate.getId())) continue;
            boolean unlocked = fastTravelManager.isUnlocked(player, gate.getId());
            gate.setClosed(player.getUniqueId(), !unlocked);
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
        // delay the update a few ticks so the player's client has time to
        // track the spawned furniture entities. Otherwise hideEntity may not
        // take effect and they would still see the open model.
        Player player = event.getPlayer();
        if (NpcTagUtil.isNpc(player)) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> updatePlayer(player), 5L);
    }

    public Main getPlugin() {
        return plugin;
    }
}

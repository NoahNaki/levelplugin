package me.nakilex.levelplugin.cooking.persistence;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import me.nakilex.levelplugin.cooking.registry.CookingWorkstationRegistry;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstation;
import me.nakilex.levelplugin.cooking.runtime.PlacedCookingWorkstationRegistry;
import me.nakilex.levelplugin.cooking.util.CookingLocationKey;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

/** Persists placed cooking workstations so workstation registrations survive server restarts. */
public class CookingWorkstationPersistenceService {
    private static final String FILE_NAME = "cooking-workstations.yml";
    private static final String ROOT = "workstations";

    private final Main plugin;
    private final File file;

    public CookingWorkstationPersistenceService(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
    }

    public void load(PlacedCookingWorkstationRegistry placedWorkstations, CookingWorkstationRegistry workstationTypes) {
        placedWorkstations.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection(ROOT);
        if (root == null) {
            return;
        }
        int loaded = 0;
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                warn("Skipping invalid persisted workstation entry '" + key + "'.");
                continue;
            }
            if (loadEntry(key, section, placedWorkstations, workstationTypes)) {
                loaded++;
            }
        }
        plugin.getLogger().info("[Cooking] Loaded " + loaded + " placed cooking workstations.");
    }

    public void save(Collection<PlacedCookingWorkstation> placedWorkstations) {
        YamlConfiguration config = new YamlConfiguration();
        int index = 0;
        for (PlacedCookingWorkstation placed : placedWorkstations) {
            if (placed == null || !placed.persistent() || placed.type() == null || placed.locationKey() == null) {
                continue;
            }
            String path = ROOT + "." + index++;
            CookingLocationKey key = placed.locationKey();
            config.set(path + ".world", key.worldId());
            config.set(path + ".x", key.x());
            config.set(path + ".y", key.y());
            config.set(path + ".z", key.z());
            config.set(path + ".type", placed.type().id());
        }
        try {
            ensureParentDirectory();
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("[Cooking] Failed to save placed workstations: " + ex.getMessage());
        }
    }

    private boolean loadEntry(String entryKey,
                              ConfigurationSection section,
                              PlacedCookingWorkstationRegistry placedWorkstations,
                              CookingWorkstationRegistry workstationTypes) {
        String worldId = section.getString("world");
        String typeId = section.getString("type", section.getString("workstation-type"));
        if (worldId == null || worldId.isBlank()) {
            warn("Skipping persisted workstation '" + entryKey + "' because world is missing.");
            return false;
        }
        if (typeId == null || typeId.isBlank()) {
            warn("Skipping persisted workstation '" + entryKey + "' because workstation type is missing.");
            return false;
        }
        CookingWorkstationType type = workstationTypes.get(typeId).orElse(null);
        if (type == null) {
            warn("Skipping persisted workstation '" + entryKey + "' because workstation type '" + typeId + "' is not registered.");
            return false;
        }
        World world = resolveWorld(worldId);
        if (world == null) {
            warn("Skipping persisted workstation '" + entryKey + "' because world '" + worldId + "' is not loaded.");
            return false;
        }
        int x = section.getInt("x");
        int y = section.getInt("y");
        int z = section.getInt("z");
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != type.blockMaterial()) {
            warn("Skipping persisted workstation '" + entryKey + "' at " + worldId + ":" + x + ":" + y + ":" + z
                    + " because block is " + block.getType() + " but expected " + type.blockMaterial() + ".");
            return false;
        }
        placedWorkstations.register(CookingLocationKey.of(block), type, null);
        return true;
    }

    private World resolveWorld(String worldId) {
        try {
            World byUuid = Bukkit.getWorld(UUID.fromString(worldId));
            if (byUuid != null) {
                return byUuid;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to a world name for compatibility with hand-authored data.
        }
        return Bukkit.getWorld(worldId);
    }

    private void ensureParentDirectory() throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent.getAbsolutePath());
        }
    }

    private void warn(String message) {
        Logger logger = plugin.getLogger();
        logger.warning("[Cooking] " + message);
    }
}

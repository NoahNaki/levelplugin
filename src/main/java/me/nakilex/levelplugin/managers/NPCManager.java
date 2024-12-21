package me.nakilex.levelplugin.managers;

import me.nakilex.levelplugin.npc.CustomNPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCManager {
    private final JavaPlugin plugin;
    private final Map<String, CustomNPC> npcRegistry = new HashMap<>();
    private final Map<String, UUID> activeNPCs = new HashMap<>();
    private final File npcFile;
    private FileConfiguration npcConfig;

    public NPCManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.npcFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!npcFile.exists()) {
            try {
                npcFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.npcConfig = YamlConfiguration.loadConfiguration(npcFile);
    }

    public void loadNPCs() {
        npcRegistry.clear();
        if (npcConfig.getConfigurationSection("npcs") != null) {
            for (String id : npcConfig.getConfigurationSection("npcs").getKeys(false)) {
                EntityType mobType = EntityType.valueOf(npcConfig.getString("npcs." + id + ".mobType"));
                String name = npcConfig.getString("npcs." + id + ".name");
                boolean alwaysFacePlayer = npcConfig.getBoolean("npcs." + id + ".alwaysFacePlayer");
                Location location = parseLocation(npcConfig.getString("npcs." + id + ".location"));
                String onRightClickCommand = npcConfig.getString("npcs." + id + ".onRightClickCommand");

                CustomNPC npc = new CustomNPC(mobType, name, null, alwaysFacePlayer, null, onRightClickCommand,
                    location, null, false, 0, null, id, true, true, null, null);

                npcRegistry.put(id, npc);
            }
        }
    }

    public void saveNPCs() {
        try {
            for (Map.Entry<String, CustomNPC> entry : npcRegistry.entrySet()) {
                CustomNPC npc = entry.getValue();
                npcConfig.set("npcs." + entry.getKey() + ".mobType", npc.getMobType().name());
                npcConfig.set("npcs." + entry.getKey() + ".name", npc.getName());
                npcConfig.set("npcs." + entry.getKey() + ".alwaysFacePlayer", npc.isAlwaysFacePlayer());
                npcConfig.set("npcs." + entry.getKey() + ".location", formatLocation(npc.getPosition()));
                npcConfig.set("npcs." + entry.getKey() + ".onRightClickCommand", npc.getOnRightClickCommand());
            }
            npcConfig.save(npcFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void spawnAllNPCs() {
        for (CustomNPC npc : npcRegistry.values()) {
            spawnNPC(npc);
        }
    }

    public void despawnAllNPCs() {
        for (UUID uuid : activeNPCs.values()) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
        activeNPCs.clear();
    }

    public void addNPC(String id, CustomNPC npc) {
        // Add NPC to the registry
        npcRegistry.put(id, npc);

        // Save NPC to configuration
        npcConfig.set("npcs." + id + ".mobType", npc.getMobType().name());
        npcConfig.set("npcs." + id + ".name", npc.getName());
        npcConfig.set("npcs." + id + ".alwaysFacePlayer", npc.isAlwaysFacePlayer());
        npcConfig.set("npcs." + id + ".location", formatLocation(npc.getPosition()));
        npcConfig.set("npcs." + id + ".onRightClickCommand", npc.getOnRightClickCommand());

        try {
            npcConfig.save(npcFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void spawnNPC(CustomNPC npc) {
        Entity entity = npc.getPosition().getWorld().spawnEntity(npc.getPosition(), npc.getMobType());
        entity.setCustomName(npc.getName());
        entity.setCustomNameVisible(npc.getName() != null);
        activeNPCs.put(npc.getNpcID(), entity.getUniqueId());
    }

    private Location parseLocation(String locationString) {
        String[] parts = locationString.split(",");
        return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3]));
    }

    private String formatLocation(Location location) {
        return location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ();
    }

    public CustomNPC getNPCById(String id) {
        return npcRegistry.get(id);
    }

    public UUID getNearestNPC(Location location, String npcID) {
        double nearestDistance = Double.MAX_VALUE;
        UUID nearestNPC = null;

        for (Map.Entry<String, UUID> entry : activeNPCs.entrySet()) {
            if (!entry.getKey().equals(npcID)) continue; // Skip NPCs with a different ID

            Entity entity = Bukkit.getEntity(entry.getValue());
            if (entity == null) continue;

            double distance = entity.getLocation().distance(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestNPC = entry.getValue();
            }
        }

        return nearestNPC;
    }

    public void despawnNPC(UUID npcUUID) {
        Entity entity = Bukkit.getEntity(npcUUID);
        if (entity != null) {
            entity.remove();
        }
        activeNPCs.values().remove(npcUUID); // Remove from active NPCs map
    }

    public CustomNPC getNPCByUUID(UUID uuid) {
        for (Map.Entry<String, UUID> entry : activeNPCs.entrySet()) {
            if (entry.getValue().equals(uuid)) {
                return npcRegistry.get(entry.getKey());
            }
        }
        return null;
    }

}

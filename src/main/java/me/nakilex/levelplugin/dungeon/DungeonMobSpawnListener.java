package me.nakilex.levelplugin.dungeon;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.utils.MythicMobModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.Set;

public class DungeonMobSpawnListener implements Listener {
    private final DungeonManager manager;
    private final Set<Dungeon.RoomInstance> triggered = new HashSet<>();
    private final FileConfiguration config;

    public DungeonMobSpawnListener(DungeonManager manager, Main plugin) {
        this.manager = manager;
        File file = new File(plugin.getDataFolder(), "dungeon_mobs.yml");
        if (!file.exists()) {
            plugin.saveResource("dungeon_mobs.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || (to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ())) return;

        for (Dungeon dungeon : manager.getActiveDungeons()) {
            if (dungeon.getRooms().isEmpty() ||
                    !player.getWorld().equals(dungeon.getRooms().get(0).center.getWorld())) continue;
            for (Dungeon.RoomInstance room : dungeon.getRooms()) {
                if (triggered.contains(room)) continue;
                if (room.bossSpawn != null && room.mob != null && room.contains(to)) {
                    spawnBoss(room);
                    triggered.add(room);
                } else if (room.mob != null && room.contains(to)) {
                    spawnConfiguredMobs(room);
                    triggered.add(room);
                }
            }
        }
    }

    private void spawnConfiguredMobs(Dungeon.RoomInstance room) {
        String key = room.mob;
        int count = 5;
        Double hp = null;
        Double dmg = null;
        Double move = null;
        Double atk = null;
        if (config.isConfigurationSection("rooms." + key)) {
            ConfigurationSection sec = config.getConfigurationSection("rooms." + key);
            count = sec.getInt("count", 5);
            hp = sec.getDouble("hp", Double.NaN);
            if (Double.isNaN(hp)) hp = null;
            dmg = sec.contains("damage") ? sec.getDouble("damage") : null;
            move = sec.contains("move-speed") ? sec.getDouble("move-speed") : null;
            atk = sec.contains("attack-speed") ? sec.getDouble("attack-speed") : null;
            key = sec.getString("mob", key);
        }
        for (int i = 0; i < count; i++) {
            double x = room.minX + 1 + Math.random() * (room.maxX - room.minX - 1);
            double z = room.minZ + 1 + Math.random() * (room.maxZ - room.minZ - 1);
            Location spawn = new Location(room.center.getWorld(), x + 0.5, room.center.getY(), z + 0.5);
            MythicMobModifier.spawnModifiedMob(key, spawn, hp, dmg, move, atk);
        }
    }

    private void spawnBoss(Dungeon.RoomInstance room) {
        room.bossSpawn.getChunk().load();
        // clear the black wool marker beneath the spawn point
        room.bossSpawn.clone().add(0, -1, 0).getBlock().setType(Material.AIR, false);

        String mobId = room.mob;
        Main.getInstance().getLogger().info("[DungeonBoss] Attempting to spawn '" + mobId + "'");
        var mob = MythicMobModifier.spawnModifiedMob(mobId, room.bossSpawn, null, null, null, null);
        if (mob != null) {
            mob.getEntity().getBukkitEntity().addScoreboardTag("dungeon_boss");
        } else {
            Main.getInstance().getLogger().warning("[DungeonBoss] MythicMob '" + mobId + "' could not be spawned");
        }
    }

    @EventHandler
    public void onBossDeath(MythicMobDeathEvent event) {
        Entity entity = MythicMobModifier.toBukkitEntity(event.getEntity());
        if (entity == null || !entity.getScoreboardTags().contains("dungeon_boss")) return;
        Location loc = entity.getLocation();
        for (Dungeon dungeon : manager.getActiveDungeons()) {
            if (dungeon.getRoomContaining(loc) != null && !dungeon.isBossDefeated()) {
                dungeon.setBossDefeated(true);
                break;
            }
        }
    }
}

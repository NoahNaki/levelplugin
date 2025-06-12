package me.nakilex.levelplugin.mining.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mining.config.MiningConfig;
import me.nakilex.levelplugin.mining.managers.MiningManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent;

import java.util.*;

/**
 * Handles mining interactions with Mythic ore mobs and their holograms.
 */
public class OreListener implements Listener {
    private final BukkitAPIHelper mythicHelper = MythicBukkit.inst().getAPIHelper();
    private final MiningConfig miningConfig;
    private final MiningManager miningManager;
    private final Main plugin;

    private final Map<UUID, List<ArmorStand>> holograms = new HashMap<>();
    private final Set<UUID> rewarded = new HashSet<>();

    public OreListener(MiningConfig cfg, MiningManager manager) {
        this.plugin = Main.getInstance();
        this.miningConfig = cfg;
        this.miningManager = manager;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onOreInteract(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        ActiveMob mob = mythicHelper.getMythicMobInstance(entity);
        if (mob == null) return;

        String type = mob.getMobType();
        ConfigurationSection sec = miningConfig.getConfig().getConfigurationSection("ores." + type);
        if (sec == null) return; // not an ore

        event.setCancelled(true);
        Player player = event.getPlayer();
        int required = sec.getInt("level", 1);
        int xp = sec.getInt("xp", 0);

        boolean meetsReq = miningManager.getLevel(player) >= required;
        updateRequirementDisplay(entity.getUniqueId(), meetsReq, required);

        if (!meetsReq) {
            player.sendMessage(ChatColor.RED + "Mining level " + required + " required.");
            return;
        }

        if (!rewarded.contains(entity.getUniqueId())) {
            rewarded.add(entity.getUniqueId());
            miningManager.addXP(player, xp);
            player.sendMessage(ChatColor.GREEN + "+" + xp + " Mining XP");
        }
    }

    @EventHandler
    public void onOreDeath(EntityDeathEvent event) {
        ActiveMob mob = mythicHelper.getMythicMobInstance(event.getEntity());
        if (mob == null) return;
        String type = mob.getMobType();
        if (!miningConfig.getConfig().isConfigurationSection("ores." + type)) return;

        rewarded.remove(event.getEntity().getUniqueId());
        removeHologram(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onMythicDeath(MythicMobDeathEvent event) {
        ActiveMob mob = event.getMob();
        String type = mob.getMobType();
        if (!miningConfig.getConfig().isConfigurationSection("ores." + type)) return;
        rewarded.remove(event.getEntity().getUniqueId());
        removeHologram(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onMythicDespawn(MythicMobDespawnEvent event) {
        ActiveMob mob = event.getMob();
        String type = mob.getMobType();
        if (!miningConfig.getConfig().isConfigurationSection("ores." + type)) return;
        rewarded.remove(event.getEntity().getUniqueId());
        removeHologram(event.getEntity().getUniqueId());
    }

    private void removeHologram(UUID uuid) {
        List<ArmorStand> list = holograms.remove(uuid);
        if (list != null) {
            for (ArmorStand as : list) if (!as.isDead()) as.remove();
        }
    }

    private void updateRequirementDisplay(UUID uuid, boolean meets, int level) {
        List<ArmorStand> list = holograms.get(uuid);
        if (list == null || list.size() < 2) return;
        ArmorStand line2 = list.get(1);
        String prefix = meets ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ";
        line2.setCustomName(prefix + ChatColor.WHITE + "Mining Lv. Min: " + ChatColor.YELLOW + level);
    }

    // Called externally when ores spawn
    public void spawnHologram(Entity entity, String type) {
        ConfigurationSection sec = miningConfig.getConfig().getConfigurationSection("ores." + type);
        if (sec == null) return;
        Location loc = entity.getLocation().add(0, 1.2, 0);
        List<ArmorStand> list = new ArrayList<>();

        ArmorStand line1 = loc.getWorld().spawn(loc, ArmorStand.class);
        line1.setVisible(false);
        line1.setMarker(true);
        line1.setGravity(false);
        String nameCol = ChatColor.translateAlternateColorCodes('&', sec.getString("name", type));
        line1.setCustomName(nameCol);
        line1.setCustomNameVisible(true);
        line1.setSilent(true);
        line1.setSmall(true);
        list.add(line1);

        Location l2 = loc.clone().add(0, -0.25, 0);
        ArmorStand line2 = loc.getWorld().spawn(l2, ArmorStand.class);
        line2.setVisible(false);
        line2.setMarker(true);
        line2.setGravity(false);
        line2.setCustomName(ChatColor.WHITE + "Mining Lv. Min: " + ChatColor.YELLOW + sec.getInt("level",1));
        line2.setCustomNameVisible(true);
        line2.setSilent(true);
        line2.setSmall(true);
        list.add(line2);

        Location l3 = loc.clone().add(0, -0.5, 0);
        ArmorStand line3 = loc.getWorld().spawn(l3, ArmorStand.class);
        line3.setVisible(false);
        line3.setMarker(true);
        line3.setGravity(false);
        line3.setCustomName(ChatColor.GRAY + "Right-Click to start mining");
        line3.setCustomNameVisible(true);
        line3.setSilent(true);
        line3.setSmall(true);
        list.add(line3);

        holograms.put(entity.getUniqueId(), list);

        // Cleanup after 5 minutes in case
        new BukkitRunnable() {
            @Override
            public void run() {
                removeHologram(entity.getUniqueId());
            }
        }.runTaskLater(plugin, 20L * 60 * 5);
    }
}

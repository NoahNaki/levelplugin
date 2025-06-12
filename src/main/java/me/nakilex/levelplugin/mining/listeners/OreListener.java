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
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
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
    private final Map<UUID, Integer> healthMap = new HashMap<>();

    public OreListener(MiningConfig cfg, MiningManager manager) {
        this.plugin = Main.getInstance();
        this.miningConfig = cfg;
        this.miningManager = manager;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onOreInteract(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        ActiveMob mob = mythicHelper.getMythicMobInstance(entity);
        if (mob == null) return;

        String type = mob.getMobType();
        ConfigurationSection sec = miningConfig.getConfig().getConfigurationSection("ores." + type);
        if (sec == null) return; // not an ore

        event.setCancelled(true); // prevent MythicMobs skill
        Player player = event.getPlayer();
        int required = sec.getInt("level", 1);
        int xp = sec.getInt("xp", 0);

        boolean meetsReq = miningManager.getLevel(player) >= required;
        updateRequirementDisplay(entity.getUniqueId(), meetsReq, required);

        if (!meetsReq) {
            player.sendMessage(ChatColor.RED + "Mining level " + required + " required.");
            return;
        }

        Material tool = player.getInventory().getItemInMainHand().getType();
        ConfigurationSection dmgSec = sec.getConfigurationSection("damage");
        int dmg = 0;
        if (dmgSec != null && tool != null) {
            dmg = dmgSec.getInt(tool.name(), 0);
        }

        if (dmg <= 0) {
            player.sendMessage(ChatColor.RED + "✘ You need a better pickaxe to mine this!");
            return;
        }

        int eff = player.getInventory().getItemInMainHand().getEnchantmentLevel(org.bukkit.enchantments.Enchantment.DIG_SPEED);
        int cd = Math.max(5, 20 - eff * 3);
        player.setCooldown(tool, cd);

        int hp = healthMap.getOrDefault(entity.getUniqueId(), sec.getInt("health", 1));
        hp -= dmg;
        if (hp <= 0) {
            mineOreDeath(entity, sec, player, xp);
        } else {
            healthMap.put(entity.getUniqueId(), hp);
            entity.getWorld().playSound(entity.getLocation(), org.bukkit.Sound.BLOCK_STONE_HIT, 1f, 1f);
        }
    }

    @EventHandler
    public void onOreDeath(EntityDeathEvent event) {
        ActiveMob mob = mythicHelper.getMythicMobInstance(event.getEntity());
        if (mob == null) return;
        String type = mob.getMobType();
        if (!miningConfig.getConfig().isConfigurationSection("ores." + type)) return;

        rewarded.remove(event.getEntity().getUniqueId());
        healthMap.remove(event.getEntity().getUniqueId());
        removeHologram(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onMythicDeath(MythicMobDeathEvent event) {
        ActiveMob mob = event.getMob();
        String type = mob.getMobType();
        if (!miningConfig.getConfig().isConfigurationSection("ores." + type)) return;
        rewarded.remove(event.getEntity().getUniqueId());
        healthMap.remove(event.getEntity().getUniqueId());
        removeHologram(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onMythicDespawn(MythicMobDespawnEvent event) {
        ActiveMob mob = event.getMob();
        String type = mob.getMobType();
        if (!miningConfig.getConfig().isConfigurationSection("ores." + type)) return;
        rewarded.remove(event.getEntity().getUniqueId());
        healthMap.remove(event.getEntity().getUniqueId());
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

    // Called when a Mythic ore spawns
    public void initOre(Entity entity, String type, int health) {
        healthMap.put(entity.getUniqueId(), health);
        spawnHologram(entity, type);
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

    private void mineOreDeath(Entity entity, ConfigurationSection sec, Player player, int xp) {
        rewarded.remove(entity.getUniqueId());
        healthMap.remove(entity.getUniqueId());
        removeHologram(entity.getUniqueId());

        String drop = sec.getString("drop", "COBBLESTONE");
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.valueOf(drop));
        entity.getWorld().dropItemNaturally(entity.getLocation(), item);

        miningManager.addXP(player, xp);
        player.sendMessage(ChatColor.GREEN + "+" + xp + " Mining XP");

        entity.remove();
    }
}

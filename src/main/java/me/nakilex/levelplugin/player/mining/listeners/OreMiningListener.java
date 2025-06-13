package me.nakilex.levelplugin.player.mining.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Handles awarding mining XP from Mythic ore mobs and pickaxe level restrictions.
 */
public class OreMiningListener implements Listener {

    private static OreMiningListener instance;

    private final BukkitAPIHelper mythicHelper = MythicBukkit.inst().getAPIHelper();
    private final Main plugin;
    private final MiningRewardsConfig rewardsConfig;
    private final MiningManager miningManager;
    private final Map<UUID, Player> damageTracker = new HashMap<>();
    private final Map<UUID, List<ArmorStand>> oreHolograms = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> hologramTasks = new HashMap<>();

    // Pickaxe level requirements
    private final Map<Material, Integer> pickaxeReqs = Map.of(
            Material.WOODEN_PICKAXE, 1,
            Material.STONE_PICKAXE, 10,
            Material.GOLDEN_PICKAXE, 15,
            Material.IRON_PICKAXE, 25,
            Material.DIAMOND_PICKAXE, 40,
            Material.NETHERITE_PICKAXE, 60
    );

    public OreMiningListener(Main plugin, MiningRewardsConfig cfg, MiningManager mgr) {
        this.plugin = plugin;
        this.rewardsConfig = cfg;
        this.miningManager = mgr;
        instance = this;
    }

    public static OreMiningListener getInstance() {
        return instance;
    }

    private ArmorStand spawnStand(Location loc, String text) {
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setCustomName(text);
        stand.setCustomNameVisible(true);
        stand.setSilent(true);
        stand.setSmall(true);
        return stand;
    }

    @EventHandler
    public void onSpawn(MythicMobSpawnEvent event) {
        ActiveMob mob = event.getMob();
        String type = mob.getMobType().toLowerCase();
        if (!rewardsConfig.getConfig().contains("ores." + type)) return;

        Location base = mob.getEntity().getBukkitEntity().getLocation();
        String pretty = type.replace('_', ' ');
        pretty = pretty.substring(0,1).toUpperCase() + pretty.substring(1);
        oreHolograms.put(mob.getEntity().getUniqueId(), new ArrayList<>());

        hologramTasks.put(mob.getEntity().getUniqueId(), plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (mob.getEntity().isDead() || !mob.getEntity().isValid()) {
                List<ArmorStand> st = oreHolograms.remove(mob.getEntity().getUniqueId());
                if (st != null) st.forEach(ArmorStand::remove);
                org.bukkit.scheduler.BukkitTask t = hologramTasks.remove(mob.getEntity().getUniqueId());
                if (t != null) t.cancel();
                return;
            }

            Location loc = mob.getEntity().getBukkitEntity().getLocation();
            boolean playerNear = loc.getWorld().getPlayers().stream().anyMatch(p -> p.getLocation().distanceSquared(loc) <= 20 * 20);

            List<ArmorStand> st = oreHolograms.computeIfAbsent(mob.getEntity().getUniqueId(), k -> new ArrayList<>());
            if (playerNear) {
                if (st.isEmpty()) {
                    String prettyName = type.replace('_', ' ');
                    prettyName = prettyName.substring(0,1).toUpperCase() + prettyName.substring(1);
                    st.add(spawnStand(loc.clone().add(0, 2.2, 0), "§f" + prettyName));
                    st.add(spawnStand(loc.clone().add(0, 1.95, 0), "§7Right-Click to start mining"));
                }
            } else {
                if (!st.isEmpty()) {
                    st.forEach(ArmorStand::remove);
                    st.clear();
                }
            }
        }, 0L, 20L));
    }

    private boolean isPickaxe(Material mat) {
        return pickaxeReqs.containsKey(mat);
    }

    private boolean checkPickaxeLevel(Player player, Material mat) {
        int req = pickaxeReqs.getOrDefault(mat, 0);
        if (req > 0 && miningManager.getLevel(player) < req) {
            player.sendMessage("§cYou need Mining level " + req + " to use this pickaxe!");
            return false;
        }
        return true;
    }

    // Prevent use of high-tier pickaxes on generic interaction
    // Use LOWEST so our cancellation happens before MythicMobs handles the interaction
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_BLOCK && action != Action.LEFT_CLICK_AIR) return;

        ItemStack item = event.getItem();
        if (item == null) return;
        if (!isPickaxe(item.getType())) return;

        if (!checkPickaxeLevel(event.getPlayer(), item.getType())) {
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setCancelled(true);
        }
    }

    // Interacting with entities (e.g., ore mobs)
    // Process entity interactions before MythicMobs reacts
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityInteract(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        ItemStack item = event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND
                ? event.getPlayer().getInventory().getItemInMainHand()
                : event.getPlayer().getInventory().getItemInOffHand();
        if (item == null || !isPickaxe(item.getType())) return;
        if (!checkPickaxeLevel(event.getPlayer(), item.getType())) {
            event.setCancelled(true);
        }
    }

    // Same for interacting at a specific entity location
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST, ignoreCancelled = false)
    public void onAtEntityInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        ItemStack item = event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND
                ? event.getPlayer().getInventory().getItemInMainHand()
                : event.getPlayer().getInventory().getItemInOffHand();
        if (item == null || !isPickaxe(item.getType())) return;
        if (!checkPickaxeLevel(event.getPlayer(), item.getType())) {
            event.setCancelled(true);
        }
    }

    // Track player who damages an ore mob
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity le)) return;
        ActiveMob mob = mythicHelper.getMythicMobInstance(le);
        if (mob == null) return;
        String type = mob.getMobType().toLowerCase();
        if (!rewardsConfig.getConfig().contains("ores." + type)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held != null && isPickaxe(held.getType())) {
            if (!checkPickaxeLevel(player, held.getType())) {
                event.setCancelled(true);
                return;
            }
        }
        damageTracker.put(le.getUniqueId(), player);
    }

    // Award XP on ore death
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        ActiveMob mob = mythicHelper.getMythicMobInstance(entity);
        if (mob == null) return;
        String type = mob.getMobType().toLowerCase();
        if (!rewardsConfig.getConfig().contains("ores." + type)) return;

        List<ArmorStand> stands = oreHolograms.remove(entity.getUniqueId());
        if (stands != null) {
            stands.forEach(ArmorStand::remove);
        }

        Player p = damageTracker.remove(entity.getUniqueId());
        if (p == null) return;

        int xp = rewardsConfig.getConfig().getInt("ores." + type, 0);
        if (xp > 0) {
            miningManager.addXP(p, xp);
        }
    }

    /** Remove all active holograms and cancel tasks */
    public void removeAllHolograms() {
        for (org.bukkit.scheduler.BukkitTask task : hologramTasks.values()) {
            task.cancel();
        }
        hologramTasks.clear();
        for (List<ArmorStand> stands : oreHolograms.values()) {
            for (ArmorStand st : stands) {
                if (!st.isDead()) st.remove();
            }
        }
        oreHolograms.clear();
    }
}

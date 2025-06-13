package me.nakilex.levelplugin.player.mining.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import org.bukkit.scheduler.BukkitTask;

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
    private final Map<UUID, BukkitTask> hpHideTasks = new HashMap<>();
    // Track custom ore health when we handle mining ourselves
    private final Map<UUID, Integer> oreHealth = new HashMap<>();
    private final Map<UUID, Integer> oreMaxHealth = new HashMap<>();
    private final Map<String, String> oreColors = Map.of(
            "coal_ore", "§x§d§5§d§5§d§5",
            "copper_ore", "§x§f§c§9§a§8§2",
            "iron_ore", "§x§f§9§d§a§c§4",
            "gold_ore", "§x§f§b§e§a§3§0",
            "quartz_ore", "§x§e§5§e§0§d§a",
            "amethyst_ore", "§x§a§7§7§9§f§1",
            "redstone_ore", "§x§f§f§0§0§0§0",
            "lapis_ore", "§x§4§d§7§5§f§1",
            "diamond_ore", "§x§2§7§e§9§d§3",
            "emerald_ore", "§x§0§0§d§a§3§9",
            "netherite_ore", "§x§9§5§8§6§7§e"
    );

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

    private String buildBar(String type, int hp, int max) {
        int length = 12;
        int filled = (int) Math.round((double) hp / max * length);
        if (filled < 0) filled = 0;
        if (filled > length) filled = length;
        String color = oreColors.getOrDefault(type, "§a");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i < filled) sb.append(color).append("▉");
            else sb.append("§8▉");
        }
        return sb.toString();
    }

    @EventHandler
    public void onSpawn(MythicMobSpawnEvent event) {
        ActiveMob mob = event.getMob();
        String type = mob.getMobType().toLowerCase();
        if (!rewardsConfig.getConfig().contains("ores." + type)) return;

        // Store base health so we can handle mining damage ourselves
        int hp = (int) ((LivingEntity) mob.getEntity().getBukkitEntity()).getHealth();
        oreHealth.put(mob.getEntity().getUniqueId(), hp);
        oreMaxHealth.put(mob.getEntity().getUniqueId(), hp);
        final int maxHp = hp;

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
                    st.add(spawnStand(loc.clone().add(0, 2.4, 0), "")); // hp bar placeholder
                    st.add(spawnStand(loc.clone().add(0, 2.2, 0), "§f" + prettyName));
                    st.add(spawnStand(loc.clone().add(0, 1.95, 0), "§7Right-Click to start mining"));
                }
                if (st.size() >= 1) {
                    int current = oreHealth.getOrDefault(mob.getEntity().getUniqueId(), maxHp);
                    st.get(0).setCustomName(buildBar(type, current, maxHp));
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

    private final Map<Material, Integer> pickaxeDamage = Map.of(
            Material.WOODEN_PICKAXE, 2,
            Material.GOLDEN_PICKAXE, 2,
            Material.STONE_PICKAXE, 3,
            Material.IRON_PICKAXE, 4,
            Material.DIAMOND_PICKAXE, 5,
            Material.NETHERITE_PICKAXE, 6
    );

    private void handleOreHit(Player player, ActiveMob mob, Material pick) {
        UUID id = mob.getEntity().getUniqueId();
        int hp = oreHealth.getOrDefault(id, (int) ((LivingEntity) mob.getEntity().getBukkitEntity()).getHealth());
        int dmg = pickaxeDamage.getOrDefault(pick, 1);
        hp -= dmg;
        Location loc = mob.getEntity().getBukkitEntity().getLocation();
        loc.getWorld().spawnParticle(Particle.CRIT, loc.add(0, 1.0, 0), 10, 0.3, 0.3, 0.3);
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_HIT, 1f, 1f);
        ((LivingEntity) mob.getEntity().getBukkitEntity()).playEffect(org.bukkit.EntityEffect.HURT);
        damageTracker.put(id, player);

        if (hp <= 0) {
            oreHealth.remove(id);
            oreMaxHealth.remove(id);
            ((LivingEntity) mob.getEntity().getBukkitEntity()).setHealth(0); // triggers death event
        } else {
            oreHealth.put(id, hp);
        }

        List<ArmorStand> st = oreHolograms.get(id);
        if (st != null && !st.isEmpty()) {
            int max = oreMaxHealth.getOrDefault(id, hp);
            st.get(0).setCustomName(buildBar(mob.getMobType().toLowerCase(), Math.max(hp,0), max));
            BukkitTask old = hpHideTasks.remove(id);
            if (old != null) old.cancel();
            hpHideTasks.put(id, plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                List<ArmorStand> stands = oreHolograms.get(id);
                if (stands != null && !stands.isEmpty()) {
                    stands.get(0).setCustomName("");
                }
                hpHideTasks.remove(id);
            }, 120L));
        }
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
            return;
        }

        ActiveMob mob = mythicHelper.getMythicMobInstance(event.getRightClicked());
        if (mob == null) return;
        String type = mob.getMobType().toLowerCase();
        if (!rewardsConfig.getConfig().contains("ores." + type)) return;

        event.setCancelled(true);
        handleOreHit(event.getPlayer(), mob, item.getType());
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
            return;
        }

        ActiveMob mob = mythicHelper.getMythicMobInstance(event.getRightClicked());
        if (mob == null) return;
        String type = mob.getMobType().toLowerCase();
        if (!rewardsConfig.getConfig().contains("ores." + type)) return;

        event.setCancelled(true);
        handleOreHit(event.getPlayer(), mob, item.getType());
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
        if (held == null || !isPickaxe(held.getType())) return;
        if (!checkPickaxeLevel(player, held.getType())) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        handleOreHit(player, mob, held.getType());
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
        oreHealth.remove(entity.getUniqueId());
        oreMaxHealth.remove(entity.getUniqueId());
        BukkitTask hide = hpHideTasks.remove(entity.getUniqueId());
        if (hide != null) hide.cancel();

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
        for (BukkitTask t : hpHideTasks.values()) {
            t.cancel();
        }
        hpHideTasks.clear();
        for (List<ArmorStand> stands : oreHolograms.values()) {
            for (ArmorStand st : stands) {
                if (!st.isDead()) st.remove();
            }
        }
        oreHolograms.clear();
    }
}

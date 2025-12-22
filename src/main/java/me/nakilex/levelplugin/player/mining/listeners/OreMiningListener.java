package me.nakilex.levelplugin.player.mining.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.BukkitAdapter;
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
    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    // Track custom ore health when we handle mining ourselves
    private final Map<UUID, Integer> oreHealth = new HashMap<>();
    private final Map<UUID, Integer> oreMaxHealth = new HashMap<>();
    private final Map<String, String> oreColors = Map.ofEntries(
            Map.entry("coal_ore", "§x§d§5§d§5§d§5"),
            Map.entry("copper_ore", "§x§f§c§9§a§8§2"),
            Map.entry("iron_ore", "§x§f§9§d§a§c§4"),
            Map.entry("gold_ore", "§x§f§b§e§a§3§0"),
            Map.entry("quartz_ore", "§x§e§5§e§0§d§a"),
            Map.entry("amethyst_ore", "§x§a§7§7§9§f§1"),
            Map.entry("redstone_ore", "§x§f§f§0§0§0§0"),
            Map.entry("lapis_ore", "§x§4§d§7§5§f§1"),
            Map.entry("diamond_ore", "§x§2§7§e§9§d§3"),
            Map.entry("emerald_ore", "§x§0§0§d§a§3§9"),
            Map.entry("netherite_ore", "§x§9§5§8§6§7§e")
    );

    private final Map<String, Material> oreParticles = Map.ofEntries(
            Map.entry("coal_ore", Material.COAL_BLOCK),
            Map.entry("copper_ore", Material.RAW_COPPER_BLOCK),
            Map.entry("iron_ore", Material.RAW_IRON_BLOCK),
            Map.entry("gold_ore", Material.RAW_GOLD_BLOCK),
            Map.entry("quartz_ore", Material.QUARTZ_BLOCK),
            Map.entry("amethyst_ore", Material.AMETHYST_BLOCK),
            Map.entry("redstone_ore", Material.REDSTONE_BLOCK),
            Map.entry("lapis_ore", Material.LAPIS_BLOCK),
            Map.entry("diamond_ore", Material.DIAMOND_BLOCK),
            Map.entry("emerald_ore", Material.EMERALD_BLOCK),
            Map.entry("netherite_ore", Material.ANCIENT_DEBRIS)
    );

    private final Map<String, Sound> oreSounds = Map.ofEntries(
            Map.entry("coal_ore", Sound.BLOCK_STONE_HIT),
            Map.entry("copper_ore", Sound.BLOCK_STONE_HIT),
            Map.entry("iron_ore", Sound.BLOCK_STONE_HIT),
            Map.entry("gold_ore", Sound.BLOCK_STONE_HIT),
            Map.entry("quartz_ore", Sound.BLOCK_AMETHYST_BLOCK_HIT),
            Map.entry("amethyst_ore", Sound.BLOCK_AMETHYST_BLOCK_HIT),
            Map.entry("redstone_ore", Sound.BLOCK_AMETHYST_BLOCK_HIT),
            Map.entry("lapis_ore", Sound.BLOCK_AMETHYST_BLOCK_HIT),
            Map.entry("diamond_ore", Sound.BLOCK_STONE_HIT),
            Map.entry("emerald_ore", Sound.BLOCK_STONE_HIT),
            Map.entry("netherite_ore", Sound.BLOCK_STONE_HIT)
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
        int reqLevel = rewardsConfig.getLevelRequirement(type);

        hologramTasks.put(mob.getEntity().getUniqueId(), plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (mob.getEntity().isDead() || !mob.getEntity().isValid()) {
                List<ArmorStand> st = oreHolograms.remove(mob.getEntity().getUniqueId());
                if (st != null) st.forEach(ArmorStand::remove);
                org.bukkit.scheduler.BukkitTask t = hologramTasks.remove(mob.getEntity().getUniqueId());
                if (t != null) t.cancel();
                return;
            }

            Location loc = mob.getEntity().getBukkitEntity().getLocation();
            UUID id = mob.getEntity().getUniqueId();
            int currentHp = oreHealth.getOrDefault(id, maxHp);
            long last = lastHitTime.getOrDefault(id, System.currentTimeMillis());
            if (currentHp < maxHp && System.currentTimeMillis() - last > 10000) {
                currentHp++;
                oreHealth.put(id, currentHp);
            }
            Player nearest = loc.getWorld().getPlayers().stream()
                    .filter(p -> p.getLocation().distanceSquared(loc) <= 20 * 20)
                    .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(loc)))
                    .orElse(null);
            boolean playerNear = nearest != null;

            List<ArmorStand> st = oreHolograms.computeIfAbsent(mob.getEntity().getUniqueId(), k -> new ArrayList<>());
            if (playerNear) {
                if (st.isEmpty()) {
                    String prettyName = type.replace('_', ' ');
                    prettyName = prettyName.substring(0,1).toUpperCase() + prettyName.substring(1);
                    st.add(spawnStand(loc.clone().add(0, 2.9, 0), oreColors.getOrDefault(type, "§f") + prettyName)); // name/hp
                    st.add(spawnStand(loc.clone().add(0, 2.6, 0), "")); // requirement
                    // invisible divider line
                    st.add(spawnStand(loc.clone().add(0, 2.4, 0), "§r"));
                    st.add(spawnStand(loc.clone().add(0, 2.2, 0), "§7Right-Click to start mining"));
                }
                if (st.size() >= 4) {
                    int current = currentHp;
                    // if hp bar visible, update
                    if (hpHideTasks.containsKey(mob.getEntity().getUniqueId())) {
                        st.get(0).setCustomName(buildBar(type, current, maxHp));
                    }
                    if (nearest != null) {
                        int lvl = miningManager.getLevel(nearest);
                        String symbol = lvl >= reqLevel ? "§a✔" : "§c✘";
                        st.get(1).setCustomName(symbol + " §fMining Lv. Min: §e" + reqLevel);
                    }
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
        me.nakilex.levelplugin.items.tools.CustomTool tool = me.nakilex.levelplugin.items.tools.ToolManager.getInstance().getTool(mat);
        return tool != null && tool.getDiscipline() == me.nakilex.levelplugin.items.tools.ToolDiscipline.MINING;
    }

    private boolean checkPickaxeLevel(Player player, Material mat) {
        me.nakilex.levelplugin.items.tools.CustomTool tool = me.nakilex.levelplugin.items.tools.ToolManager.getInstance().getTool(mat);
        if (tool == null || tool.getDiscipline() != me.nakilex.levelplugin.items.tools.ToolDiscipline.MINING) return true;
        me.nakilex.levelplugin.items.tools.ToolTier tier = tool.getTier();
        int req = tier.getLevelRequirement();
        if (miningManager.getLevel(player) < req) {
            player.sendMessage("§cYou need Mining level " + req + " to use this pickaxe!");
            return false;
        }
        return true;
    }

    private boolean checkOreLevel(Player player, String oreType) {
        int req = rewardsConfig.getLevelRequirement(oreType);
        if (req > 0 && miningManager.getLevel(player) < req) {
            player.sendMessage("§cYou need Mining level " + req + " to mine this ore!");
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
        Material partMat = oreParticles.getOrDefault(mob.getMobType().toLowerCase(), Material.STONE);
        loc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE,
                loc.clone().add(0, 1.0, 0), 15, 0.6, 0.6, 0.6, partMat.createBlockData());
        Sound hitSound = oreSounds.getOrDefault(mob.getMobType().toLowerCase(), Sound.BLOCK_STONE_HIT);
        loc.getWorld().playSound(loc, hitSound, 1f, 0.5f);
        loc.getWorld().playSound(loc, hitSound, 1f, 1f);
        loc.getWorld().playSound(loc, hitSound, 1f, 2f);
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT, 1f, 1f);
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT, 1f, 0.5f);
        ((LivingEntity) mob.getEntity().getBukkitEntity()).playEffect(org.bukkit.EntityEffect.HURT);
        damageTracker.put(id, player);

        if (hp <= 0) {
            oreHealth.remove(id);
            oreMaxHealth.remove(id);
            ((LivingEntity) mob.getEntity().getBukkitEntity()).setHealth(0); // triggers death event
        } else {
            oreHealth.put(id, hp);
        }

        lastHitTime.put(id, System.currentTimeMillis());
        List<ArmorStand> st = oreHolograms.get(id);
        if (st != null && !st.isEmpty()) {
            int max = oreMaxHealth.getOrDefault(id, hp);
            st.get(0).setCustomName(buildBar(mob.getMobType().toLowerCase(), Math.max(hp,0), max));
            BukkitTask old = hpHideTasks.remove(id);
            if (old != null) old.cancel();
            hpHideTasks.put(id, plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                List<ArmorStand> stands = oreHolograms.get(id);
                if (stands != null && !stands.isEmpty()) {
                    String prettyName = mob.getMobType().replace('_', ' ');
                    prettyName = prettyName.substring(0,1).toUpperCase()+prettyName.substring(1);
                    stands.get(0).setCustomName(oreColors.getOrDefault(mob.getMobType().toLowerCase(), "§f") + prettyName);
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
        if (!checkOreLevel(event.getPlayer(), type)) {
            event.setCancelled(true);
            return;
        }

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
        if (!checkOreLevel(event.getPlayer(), type)) {
            event.setCancelled(true);
            return;
        }

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
        if (!checkOreLevel(player, type)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        handleOreHit(player, mob, held.getType());
    }

    // Cancel any other damage to ore mobs
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onAnyDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        ActiveMob mob = mythicHelper.getMythicMobInstance(event.getEntity());
        if (mob == null) return;
        String type = mob.getMobType().toLowerCase();
        if (!rewardsConfig.getConfig().contains("ores." + type)) return;

        if (event instanceof EntityDamageByEntityEvent byEntity) {
            if (byEntity.getDamager() instanceof Player p) {
                ItemStack held = p.getInventory().getItemInMainHand();
                if (held != null && isPickaxe(held.getType()) && checkPickaxeLevel(p, held.getType())
                        && checkOreLevel(p, type)) {
                    return; // handled in other listener
                }
            }
        }
        event.setCancelled(true);
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

        int xp = rewardsConfig.getXP(type);
        if (xp > 0) {
            miningManager.addXP(p, xp);
        }

        // Give material drops directly to the contributing player
        me.nakilex.levelplugin.player.mining.items.MiningMaterial mat =
                me.nakilex.levelplugin.player.mining.items.MiningMaterial.fromOre(type);
        if (mat != null) {
            int amt = rewardsConfig.getDropMin(type);
            int max = rewardsConfig.getDropMax(type);
            if (max > amt) {
                amt += java.util.concurrent.ThreadLocalRandom.current().nextInt(max - amt + 1);
            }
            p.getInventory().addItem(mat.createItem(amt));
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

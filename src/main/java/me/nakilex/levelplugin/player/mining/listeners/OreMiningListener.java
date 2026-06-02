package me.nakilex.levelplugin.player.mining.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.player.mining.items.MiningNodeVariant;
import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.MiningToolEnchant;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.DropPickupUtil;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Handles awarding mining XP from custom ore mobs and pickaxe level restrictions.
 */
public class OreMiningListener implements Listener {

    private static OreMiningListener instance;

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
    private final Map<UUID, MiningNodeVariant> oreVariants = new HashMap<>();
    private final Map<UUID, Long> weakPointExpiresAt = new HashMap<>();
    private final Map<UUID, Long> lastHandledHitAt = new HashMap<>();
    private final Set<UUID> announcedVariants = new HashSet<>();
    private static final long WEAK_POINT_WINDOW_MS = 1_800L;
    private static final long DUPLICATE_INTERACTION_GUARD_MS = 60L;
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
    public void onSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        String type = resolveOreType(entity);
        if (type == null) {
            return;
        }
        if (!rewardsConfig.getConfig().contains("ores." + type)) return;

        // Store custom health and a node modifier while reusing the same ore entity pipeline.
        MiningNodeVariant variant = rewardsConfig.rollNodeVariant();
        int hp = Math.max(1, (int) Math.round(entity.getHealth() * rewardsConfig.getHealthMultiplier(variant)));
        oreVariants.put(entity.getUniqueId(), variant);
        oreHealth.put(entity.getUniqueId(), hp);
        oreMaxHealth.put(entity.getUniqueId(), hp);
        final int maxHp = hp;

        oreHolograms.put(entity.getUniqueId(), new ArrayList<>());
        int reqLevel = rewardsConfig.getLevelRequirement(type);

        hologramTasks.put(entity.getUniqueId(), plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (entity.isDead() || !entity.isValid()) {
                List<ArmorStand> st = oreHolograms.remove(entity.getUniqueId());
                if (st != null) st.forEach(ArmorStand::remove);
                org.bukkit.scheduler.BukkitTask t = hologramTasks.remove(entity.getUniqueId());
                if (t != null) t.cancel();
                cleanupOreState(entity.getUniqueId());
                return;
            }

            Location loc = entity.getLocation();
            UUID id = entity.getUniqueId();
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

            List<ArmorStand> st = oreHolograms.computeIfAbsent(entity.getUniqueId(), k -> new ArrayList<>());
            if (playerNear) {
                if (st.isEmpty()) {
                    st.add(spawnStand(loc.clone().add(0, 2.9, 0), buildNodeName(type, variant))); // name/hp
                    st.add(spawnStand(loc.clone().add(0, 2.6, 0), "")); // requirement
                    // invisible divider line
                    st.add(spawnStand(loc.clone().add(0, 2.4, 0), "§r"));
                    st.add(spawnStand(loc.clone().add(0, 2.2, 0), buildMiningPrompt(entity.getUniqueId(), nearest)));
                }
                if (st.size() >= 4) {
                    int current = currentHp;
                    // if hp bar visible, update
                    if (hpHideTasks.containsKey(entity.getUniqueId())) {
                        st.get(0).setCustomName(buildBar(type, current, maxHp));
                    }
                    if (nearest != null) {
                        int lvl = miningManager.getLevel(nearest);
                        String symbol = lvl >= reqLevel ? "§a✔" : "§c✘";
                        st.get(1).setCustomName(symbol + " §fMining Lv. Min: §e" + reqLevel);
                        st.get(3).setCustomName(buildMiningPrompt(entity.getUniqueId(), nearest));
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
        if (!me.nakilex.levelplugin.items.tools.ToolManager.getInstance().meetsLevelRequirement(player, tool)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You need Mining level " + req + " to use this pickaxe.");
            return false;
        }
        return true;
    }

    private boolean checkOreLevel(Player player, String oreType) {
        int req = rewardsConfig.getLevelRequirement(oreType);
        if (req > 0 && miningManager.getLevel(player) < req) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You need Mining level " + req + " to mine this ore.");
            return false;
        }
        return true;
    }

    private void handleOreHit(Player player, LivingEntity entity, String type, ItemStack pickaxe) {
        UUID id = entity.getUniqueId();
        long now = System.currentTimeMillis();
        Long previousHit = lastHandledHitAt.put(id, now);
        if (previousHit != null && now - previousHit < DUPLICATE_INTERACTION_GUARD_MS) return;

        int hp = oreHealth.getOrDefault(id, (int) entity.getHealth());
        MiningNodeVariant variant = oreVariants.getOrDefault(id, MiningNodeVariant.NORMAL);
        if (variant.isSpecial() && announcedVariants.add(id)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "You discovered a " + variant.getColor() + variant.getDisplayName() + ChatColor.GRAY + " ore node!");
        }
        int damage = calculatePickaxeDamage(player, pickaxe);
        boolean weakPoint = consumeWeakPoint(id, now);
        if (weakPoint) {
            damage = Math.max(damage + 1, (int) Math.round(damage * rewardsConfig.getWeakPointDamageMultiplier(variant)));
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Weak point struck for " + ChatColor.YELLOW + damage + ChatColor.GREEN + " mining damage!");
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.6f);
            entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().clone().add(0, 1.0, 0), 16, 0.4, 0.4, 0.4, 0.1);
        }
        hp -= damage;
        Location loc = entity.getLocation();
        Material partMat = oreParticles.getOrDefault(type, Material.STONE);
        loc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE,
                loc.clone().add(0, 1.0, 0), 15, 0.6, 0.6, 0.6, partMat.createBlockData());
        Sound hitSound = oreSounds.getOrDefault(type, Sound.BLOCK_STONE_HIT);
        loc.getWorld().playSound(loc, hitSound, 1f, 0.5f);
        loc.getWorld().playSound(loc, hitSound, 1f, 1f);
        loc.getWorld().playSound(loc, hitSound, 1f, 2f);
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT, 1f, 1f);
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT, 1f, 0.5f);
        entity.playEffect(org.bukkit.EntityEffect.HURT);
        damageTracker.put(id, player);

        if (hp <= 0) {
            oreHealth.remove(id);
            oreMaxHealth.remove(id);
            entity.setHealth(0); // triggers death event
        } else {
            oreHealth.put(id, hp);
            tryOpenWeakPoint(id, variant, now);
        }

        lastHitTime.put(id, now);
        List<ArmorStand> st = oreHolograms.get(id);
        if (st != null && !st.isEmpty()) {
            int max = oreMaxHealth.getOrDefault(id, hp);
            st.get(0).setCustomName(buildBar(type, Math.max(hp,0), max));
            if (st.size() >= 4) st.get(3).setCustomName(buildMiningPrompt(id, player));
            BukkitTask old = hpHideTasks.remove(id);
            if (old != null) old.cancel();
            hpHideTasks.put(id, plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                List<ArmorStand> stands = oreHolograms.get(id);
                if (stands != null && !stands.isEmpty()) stands.get(0).setCustomName(buildNodeName(type, variant));
                hpHideTasks.remove(id);
            }, 120L));
        }
    }

    private int calculatePickaxeDamage(Player player, ItemStack pickaxe) {
        CustomTool tool = ToolManager.getInstance().getTool(pickaxe);
        ToolTier tier = tool != null && tool.getDiscipline() == ToolDiscipline.MINING ? tool.getTier() : null;
        int tierDamage = tier == null ? 1 : tier.getMiningDamage();
        return Math.max(1, (int) Math.round(tierDamage * miningManager.getMomentumDamageMultiplier(player)));
    }

    private boolean consumeWeakPoint(UUID oreId, long now) {
        Long expiresAt = weakPointExpiresAt.remove(oreId);
        return expiresAt != null && now <= expiresAt;
    }

    private void tryOpenWeakPoint(UUID oreId, MiningNodeVariant variant, long now) {
        if (weakPointExpiresAt.containsKey(oreId)) return;
        if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < rewardsConfig.getWeakPointChance(variant)) {
            weakPointExpiresAt.put(oreId, now + WEAK_POINT_WINDOW_MS);
        }
    }

    private String buildMiningPrompt(UUID oreId, Player player) {
        Long expiresAt = weakPointExpiresAt.get(oreId);
        if (expiresAt != null && System.currentTimeMillis() <= expiresAt) return "§e§l✦ WEAK POINT! §fStrike now!";
        if (expiresAt != null) weakPointExpiresAt.remove(oreId);
        String momentum = miningManager.getMomentumIndicator(player);
        return momentum == null ? "§7Left-Click to start mining" : "§7Left-Click to mine  §8•  " + momentum;
    }

    private String buildNodeName(String type, MiningNodeVariant variant) {
        String prettyName = MobNameUtil.toPrettyName(type);
        if (variant == null || !variant.isSpecial()) return oreColors.getOrDefault(type, "§f") + prettyName;
        return variant.getColor() + variant.getDisplayName() + " " + oreColors.getOrDefault(type, "§f") + prettyName;
    }

    // Prevent use of high-tier pickaxes on generic interaction
    // Use LOWEST so our cancellation happens before other handlers react
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

    // Right-clicking an ore should never mine it. Survival-mode mining uses the normal left-click damage event.
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityInteract(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        cancelRightClickOreInteraction(event.getPlayer(), event.getRightClicked(), event);
    }

    // Some entity models emit the more specific at-entity interaction event as well.
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST, ignoreCancelled = false)
    public void onAtEntityInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        cancelRightClickOreInteraction(event.getPlayer(), event.getRightClicked(), event);
    }

    private void cancelRightClickOreInteraction(Player player, org.bukkit.entity.Entity clicked, Cancellable event) {
        if (!(clicked instanceof LivingEntity entity)) return;
        String type = resolveOreType(entity);
        if (type == null || !rewardsConfig.getConfig().contains("ores." + type)) return;
        event.setCancelled(true);
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held != null && isPickaxe(held.getType())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Left-click this ore with your pickaxe to mine it.");
        }
    }

    // Survival-mode left-click mining arrives through the standard damage event.
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity le)) return;
        String type = resolveOreType(le);
        if (type == null) return;
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
        handleOreHit(player, le, type, held);
    }

    // Cancel any other damage to ore mobs
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onAnyDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        String type = resolveOreType(entity);
        if (type == null) return;
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
        String type = resolveOreType(entity);
        if (type == null) return;
        if (!rewardsConfig.getConfig().contains("ores." + type)) return;
        event.getDrops().clear();
        event.setDroppedExp(0);

        BukkitTask hologramTask = hologramTasks.remove(entity.getUniqueId());
        if (hologramTask != null) hologramTask.cancel();
        List<ArmorStand> stands = oreHolograms.remove(entity.getUniqueId());
        if (stands != null) {
            stands.forEach(ArmorStand::remove);
        }
        UUID oreId = entity.getUniqueId();
        MiningNodeVariant variant = oreVariants.getOrDefault(oreId, MiningNodeVariant.NORMAL);
        Player p = damageTracker.get(oreId);
        cleanupOreState(oreId);
        if (p == null) return;

        int xp = (int) Math.round(rewardsConfig.getXP(type) * rewardsConfig.getXpMultiplier(variant));
        ItemStack held = p.getInventory().getItemInMainHand();
        if (ToolManager.getInstance().getMiningEnchant(held) == MiningToolEnchant.INSIGHT
                && java.util.concurrent.ThreadLocalRandom.current().nextDouble() <= 0.30D) {
            xp = (int) Math.round(xp * 1.60D);
            ChatMessageUtil.send(p, ChatMessageUtil.MessageType.REWARD, "Insight granted bonus Mining XP!");
        }
        if (xp > 0) miningManager.addXP(p, xp);
        miningManager.recordMomentumOre(p);
        if (variant.isSpecial()) {
            ChatMessageUtil.send(p, ChatMessageUtil.MessageType.REWARD,
                    variant.getColor() + variant.getDisplayName() + ChatColor.GOLD + " node rewards claimed!");
        }
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().handleMineOre(p, type);
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
            amt = Math.max(1, (int) Math.round(amt * rewardsConfig.getDropMultiplier(variant)));
            DropPickupUtil.dropForPlayerWithDelayedAutoPickup(p, mat.createItem(amt), 20L);
        }
    }


    private void cleanupOreState(UUID oreId) {
        oreHealth.remove(oreId);
        oreMaxHealth.remove(oreId);
        oreVariants.remove(oreId);
        weakPointExpiresAt.remove(oreId);
        lastHandledHitAt.remove(oreId);
        announcedVariants.remove(oreId);
        damageTracker.remove(oreId);
        lastHitTime.remove(oreId);
        BukkitTask hide = hpHideTasks.remove(oreId);
        if (hide != null) hide.cancel();
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
        oreHealth.clear();
        oreMaxHealth.clear();
        oreVariants.clear();
        weakPointExpiresAt.clear();
        lastHandledHitAt.clear();
        announcedVariants.clear();
        damageTracker.clear();
        lastHitTime.clear();
    }

    private String resolveOreType(LivingEntity entity) {
        return MobNameUtil.resolveCustomMobId(entity)
                .map(id -> id.toLowerCase(java.util.Locale.ROOT))
                .orElse(null);
    }
}

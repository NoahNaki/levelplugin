package me.nakilex.levelplugin.boss;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.RewardBombUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class FieldBossListener implements Listener {
    private final Main plugin;
    private final FileConfiguration cfg;
    private final ItemManager itemManager;
    private final GemsManager gemsManager;

    private final Map<String, String> bossKeyMap = new HashMap<>();
    private final Map<UUID, Map<UUID, Double>> damageMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> bossStartTime = new ConcurrentHashMap<>();
    public FieldBossListener(Main plugin,
                             FileConfiguration bossConfig,
                             ItemManager itemManager,
                             GemsManager gemsManager) {
        this.plugin      = plugin;
        this.cfg         = bossConfig;
        this.itemManager = itemManager;
        this.gemsManager = gemsManager;

        if (cfg.isConfigurationSection("mobs")) {
            for (String key : cfg.getConfigurationSection("mobs").getKeys(false)) {
                bossKeyMap.put(key.toUpperCase(Locale.ROOT), key);
            }
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent ev) {
        Entity ent = ev.getEntity();
        if (!(ent instanceof LivingEntity)) return;

        ActiveMob mob = MythicBukkit.inst()
            .getAPIHelper()
            .getMythicMobInstance((LivingEntity) ent);
        if (mob == null) return;

        String mobId = mob.getMobType().toUpperCase(Locale.ROOT);
        String cfgKey = bossKeyMap.get(mobId);
        if (cfgKey == null) return;

        UUID bossId = ent.getUniqueId();
        damageMap.computeIfAbsent(bossId, id -> {
            //announceBossEngage(cfgKey);
            bossStartTime.put(id, System.currentTimeMillis());
            return new ConcurrentHashMap<>();
        });

        Player damager = null;
        if (ev.getDamager() instanceof Player p) damager = p;
        else if (ev.getDamager() instanceof Projectile proj
            && proj.getShooter() instanceof Player shooter) {
            damager = shooter;
        }
        if (damager == null) return;

        damageMap.get(bossId)
            .merge(damager.getUniqueId(), ev.getFinalDamage(), Double::sum);
    }

    @EventHandler
    public void onBossDeath(MythicMobDeathEvent ev) {
        // 1) Identify boss and fetch record
        String mobId = ev.getMob().getMobType().toUpperCase(Locale.ROOT);
        String cfgKey = bossKeyMap.get(mobId);
        if (cfgKey == null) return;

        String bossDisplayName = MobNameUtil.getDisplayName(mobId);
        if (bossDisplayName == null || bossDisplayName.isBlank()) {
            bossDisplayName = MobNameUtil.toPrettyName(mobId);
        }
        bossDisplayName = ChatColor.stripColor(bossDisplayName);
        final String bossNameUpper = bossDisplayName.toUpperCase(Locale.ROOT);

        UUID bossId = BukkitAdapter.adapt(ev.getEntity()).getUniqueId();
        Map<UUID, Double> record = damageMap.remove(bossId);
        Long startTs = bossStartTime.remove(bossId);
        if (record == null || record.isEmpty() || startTs == null) return;

        // 2) Compute elapsed time & config values
        long elapsedMs = System.currentTimeMillis() - startTs;
        String elapsed = String.format("%02d:%02d",
            elapsedMs / 60_000,
            (elapsedMs / 1000) % 60
        );

        int totalExp    = cfg.getInt("mobs." + cfgKey + ".exp", 0);
        String coinRange  = cfg.getString("mobs." + cfgKey + ".coins", "0-0");
        String[] cr       = coinRange.split("-");
        int minCoins      = Integer.parseInt(cr[0]);
        int maxCoins      = Integer.parseInt(cr[1]);

        @SuppressWarnings("unchecked")
        List<Map<String,Object>> items = (List<Map<String,Object>>)
            cfg.getList("mobs." + cfgKey + ".items", Collections.emptyList());

        String gemStr      = cfg.getString("mobs." + cfgKey + ".gems", "0");
        int totalGems      = Integer.parseInt(gemStr);

        double totalDmg = record.values().stream().mapToDouble(d -> d).sum();

        // 3) Determine top‐3 entries
        List<Map.Entry<UUID, Double>> top3 = record.entrySet().stream()
            .sorted(Map.Entry.<UUID,Double>comparingByValue().reversed())
            .limit(3)
            .toList();

        // 4) Award XP, coins & gems to everyone
        for (var entry : record.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;

            double share    = entry.getValue() / totalDmg;
            int xpAward     = (int)Math.round(totalExp * share);
            int coinsAward  = ThreadLocalRandom.current().nextInt(minCoins, maxCoins + 1);
            int gemsAward   = (int)Math.round(totalGems * share);

            plugin.getLevelManager().addXP(p, xpAward);
            plugin.getEconomyManager().addCoins(p, coinsAward);
            gemsManager.addUnits(p, gemsAward);
        }

        // 5) Award items & extra rewards to top‐3 immediately
        for (var entry : top3) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;

            double share   = entry.getValue() / totalDmg;
            int xpAward    = (int)Math.round(totalExp * share);
            int coinsAward = ThreadLocalRandom.current()
                .nextInt(minCoins, maxCoins + 1);
            plugin.getLevelManager().addXP(p, xpAward);
            plugin.getEconomyManager().addCoins(p, coinsAward);

            if (!items.isEmpty()) {
                for (Map<String,Object> m : items) {
                    ItemStack drop = rollConfiguredDrop(m, p);
                    if (drop != null) {
                        p.getWorld().dropItemNaturally(p.getLocation(), drop);
                    }
                }
            } else {
                ItemStack fallback = rollFallbackBossGear(mobId);
                if (fallback != null) {
                    p.getWorld().dropItemNaturally(p.getLocation(), fallback);
                }
            }
        }

        for (var entry : record.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;
            RewardBombUtil.startRewardBomb(plugin, ev.getEntity().getLocation(),
                    createBossRewardBomb(items, mobId), 60, p);
        }

        // 6) Delay only the chat output by 5 ticks
        final String fElapsed = elapsed;
        final List<Map.Entry<UUID, Double>> fTop3 = top3;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    // Centered headers
                    ChatFormatter.constructDivider(pl, " ", 45);
                    ChatFormatter.sendCenteredMessage(pl,
                        ChatColor.GOLD + "" + ChatColor.BOLD
                            + " " + bossNameUpper + " SLAIN!");
                    ChatFormatter.sendCenteredMessage(pl,
                        ChatColor.GRAY + "Time Elapsed: " + ChatColor.WHITE + fElapsed);
                    ChatFormatter.constructDivider(pl, " ", 45);

                    // Leaderboard header
                    pl.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "        Leaderboard");

                    // Top‐3 entries
                    int rank = 1;
                    for (var entry : fTop3) {
                        Player p = Bukkit.getPlayer(entry.getKey());
                        if (p == null) continue;

                        double dmg      = Math.round(entry.getValue() * 10) / 10.0;
                        double pctValue = entry.getValue() / totalDmg * 100;
                        String pctStr   = String.format("%.1f%%", pctValue);

                        String pos = switch(rank) {
                            case 1 -> "          ";
                            case 2 -> "          ";
                            case 3 -> "          ";
                            default -> "#" + rank + " ";
                        };
                        pl.sendMessage(pos
                            + ChatColor.YELLOW + p.getName()
                            + ChatColor.WHITE  + " " + dmg
                            + ChatColor.RED    + " ❤"
                            + ChatColor.GRAY   + " (" + pctStr + ")");
                        rank++;
                    }
                }
            }
        }.runTaskLater(plugin, 5L);
    }



//    private void announceBossEngage(String name) {
//        new BukkitRunnable() {
//            @Override public void run() {
//                Bukkit.broadcastMessage(ChatColor.DARK_PURPLE
//                    + "[Boss Engaged] " + ChatColor.RED + name
//                    + ChatColor.DARK_PURPLE + " has entered combat!");
//            }
//        }.runTask(plugin);
//    }

    private ItemStack rollConfiguredDrop(Map<String, Object> config, Player owner) {
        return createDropFromConfig(config, owner, true);
    }

    private ItemStack createDropFromConfig(Map<String, Object> config, Player owner, boolean applyDropRate) {
        if (config == null) return null;

        if (applyDropRate) {
            Object chance = config.get("drop_rate");
            double dropPct = chance == null ? 0 : Double.parseDouble(chance.toString());
            if (ThreadLocalRandom.current().nextDouble(0, 100) > dropPct) {
                return null;
            }
        }

        String qtyRange = String.valueOf(config.getOrDefault("quantity", "1-1"));
        String[] qr = qtyRange.split("-");
        int minQ = Integer.parseInt(qr[0]);
        int maxQ = qr.length > 1 ? Integer.parseInt(qr[1]) : minQ;
        int qty = ThreadLocalRandom.current().nextInt(minQ, maxQ + 1);

        String itemId = String.valueOf(config.get("itemid"));
        ItemStack drop = null;
        if (itemId != null && itemId.matches("\\d+")) {
            int cid = Integer.parseInt(itemId);
            CustomItem inst = itemManager.rollNewInstance(cid);
            if (inst != null) {
                drop = ItemUtil.createItemStackFromCustomItem(inst, qty, owner);
            }
        }
        if (drop == null && itemId != null) {
            Material mat = Material.matchMaterial(itemId.toUpperCase(Locale.ROOT));
            if (mat != null) {
                drop = new ItemStack(mat, qty);
            }
        }
        return drop;
    }

    private ItemStack createAwakenedEssenceDrop() {
        ItemRarity rarity = rollFieldBossEssenceRarity();
        me.nakilex.levelplugin.player.classes.data.PlayerClass awakened = getRandomAwakenedEssenceClass();
        return ClassEssence.generateEssence(awakened, rarity, 0);
    }

    private ItemStack createFieldBossEssenceDrop() {
        ItemRarity rarity = rollFieldBossEssenceRarity();
        me.nakilex.levelplugin.player.classes.data.PlayerClass clazz = ThreadLocalRandom.current().nextDouble() < 0.10
                ? getRandomAwakenedEssenceClass()
                : getRandomBaseEssenceClass();
        return ClassEssence.generateEssence(clazz, rarity, 0);
    }

    private me.nakilex.levelplugin.player.classes.data.PlayerClass getRandomAwakenedEssenceClass() {
        me.nakilex.levelplugin.player.classes.data.PlayerClass[] awakened = {
                me.nakilex.levelplugin.player.classes.data.PlayerClass.AWAKARCHER,
                me.nakilex.levelplugin.player.classes.data.PlayerClass.AWAKWARRIOR,
                me.nakilex.levelplugin.player.classes.data.PlayerClass.AWAKROGUE,
                me.nakilex.levelplugin.player.classes.data.PlayerClass.AWAKMAGE
        };
        return awakened[ThreadLocalRandom.current().nextInt(awakened.length)];
    }

    private me.nakilex.levelplugin.player.classes.data.PlayerClass getRandomBaseEssenceClass() {
        me.nakilex.levelplugin.player.classes.data.PlayerClass[] base = {
                me.nakilex.levelplugin.player.classes.data.PlayerClass.ARCHER,
                me.nakilex.levelplugin.player.classes.data.PlayerClass.WARRIOR,
                me.nakilex.levelplugin.player.classes.data.PlayerClass.ROGUE,
                me.nakilex.levelplugin.player.classes.data.PlayerClass.MAGE
        };
        return base[ThreadLocalRandom.current().nextInt(base.length)];
    }

    private ItemRarity rollFieldBossEssenceRarity() {
        return ThreadLocalRandom.current().nextDouble() < 0.01 ? ItemRarity.UNCOMMON : ItemRarity.COMMON;
    }

    private java.util.function.Supplier<ItemStack> createBossRewardBomb(List<Map<String, Object>> items, String mobId) {
        return () -> {
            ItemStack gearDrop = rollRandomConfiguredDrop(items, false);
            if (gearDrop == null) {
                gearDrop = rollFallbackBossGear(mobId);
            }
            boolean chooseGear = ThreadLocalRandom.current().nextDouble() < 0.50;

            if (chooseGear && gearDrop != null) {
                return gearDrop.clone();
            }
            ItemStack rolledEssence = createFieldBossEssenceDrop();
            if (rolledEssence != null) return rolledEssence;

            return gearDrop != null ? gearDrop.clone() : createAwakenedEssenceDrop();
        };
    }

    private ItemStack rollFallbackBossGear(String mobId) {
        LootChestManager lootChestManager = plugin.getLootChestManager();
        if (lootChestManager == null) {
            return null;
        }
        return lootChestManager.getRandomLootForCombatPower(80, mobId, null);
    }

    private ItemStack rollRandomConfiguredDrop(List<Map<String, Object>> items, boolean applyDropRate) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> shuffled = new ArrayList<>(items);
        Collections.shuffle(shuffled);

        for (Map<String, Object> entry : shuffled) {
            ItemStack drop = createDropFromConfig(entry, null, applyDropRate);
            if (drop != null) {
                return drop;
            }
        }

        return null;
    }
}

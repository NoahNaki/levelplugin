package me.nakilex.levelplugin.boss;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
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

    // lowercase→YAML key
    private final Map<String, String> bossKeyMap = new HashMap<>();
    // boss UUID → (player UUID → damage)
    private final Map<UUID, Map<UUID, Double>> damageMap = new ConcurrentHashMap<>();
    // boss UUID → start timestamp (ms)
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
                bossKeyMap.put(key.toLowerCase(Locale.ROOT), key);
            }
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private String stripTags(String s) {
        return s.replaceAll("<[^>]+>", "").trim();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent ev) {
        Entity ent = ev.getEntity();
        if (!(ent instanceof LivingEntity)) return;

        ActiveMob mob = MythicBukkit.inst()
            .getAPIHelper()
            .getMythicMobInstance((LivingEntity) ent);
        if (mob == null) return;

        String raw = mob.getType().getDisplayName().get();
        String name = stripTags(raw).toLowerCase(Locale.ROOT);
        String cfgKey = bossKeyMap.get(name);
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
        String raw    = ev.getMob().getType().getDisplayName().get();
        String name   = stripTags(raw).toLowerCase(Locale.ROOT);
        String cfgKey = bossKeyMap.get(name);
        if (cfgKey == null) return;

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

        int    totalExp   = cfg.getInt("mobs." + cfgKey + ".exp", 0);
        String coinRange  = cfg.getString("mobs." + cfgKey + ".coins", "0-0");
        String[] cr       = coinRange.split("-");
        int    minCoins   = Integer.parseInt(cr[0]);
        int    maxCoins   = Integer.parseInt(cr[1]);

        @SuppressWarnings("unchecked")
        List<Map<String,Object>> items = (List<Map<String,Object>>)
            cfg.getList("mobs." + cfgKey + ".items", Collections.emptyList());

        double totalDmg = record.values().stream().mapToDouble(d -> d).sum();

        // 3) Determine top‐3 entries
        List<Map.Entry<UUID, Double>> top3 = record.entrySet().stream()
            .sorted(Map.Entry.<UUID,Double>comparingByValue().reversed())
            .limit(3)
            .toList();

        String gemStr = cfg.getString("mobs." + cfgKey + ".gems", "0");
        int totalGems = Integer.parseInt(gemStr);

        for (var entry : record.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;

            double share    = entry.getValue() / totalDmg;
            int xpAward     = (int)Math.round(totalExp * share);
            int coinsAward  = ThreadLocalRandom.current().nextInt(minCoins, maxCoins + 1);
            int gemsAward   = (int)Math.round(totalGems * share);   // ← new!

            plugin.getLevelManager().addXP(p, xpAward);
            plugin.getEconomyManager().addCoins(p, coinsAward);
            gemsManager.addUnits(p, gemsAward);                     // ← new!
        }

        // 4) Award XP/coins & drop items *immediately* so level‐up fires right away
        for (var entry : top3) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;

            // XP & coins
            double share   = entry.getValue() / totalDmg;
            int xpAward    = (int)Math.round(totalExp * share);
            int coinsAward = ThreadLocalRandom.current()
                .nextInt(minCoins, maxCoins + 1);
            plugin.getLevelManager().addXP(p, xpAward);
            plugin.getEconomyManager().addCoins(p, coinsAward);

            for (Map<String,Object> m : items) {
                String itemId   = m.get("itemid").toString();
                double dropPct  = (double)m.get("drop_rate");
                String qtyRange = m.get("quantity").toString();

                if (ThreadLocalRandom.current().nextDouble(0, 100) > dropPct)
                    continue;

                String[] qr = qtyRange.split("-");
                int minQ = Integer.parseInt(qr[0]), maxQ = Integer.parseInt(qr[1]);
                int qty = ThreadLocalRandom.current().nextInt(minQ, maxQ + 1);

                ItemStack drop = null;

                if (itemId.matches("\\d+")) {
                    int cid = Integer.parseInt(itemId);
                    // >>> use your new manager helper <<<
                    CustomItem inst = itemManager.rollNewInstance(cid);
                    if (inst != null)
                        drop = ItemUtil.createItemStackFromCustomItem(inst, qty, p);
                }

                if (drop == null) {
                    Material mat = Material.matchMaterial(itemId.toUpperCase(Locale.ROOT));
                    if (mat != null)
                        drop = new ItemStack(mat, qty);
                }

                if (drop != null)
                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
            }
        }

        // 5) Delay only the chat output by 5 ticks
        final String fElapsed = elapsed;
        final List<Map.Entry<UUID, Double>> fTop3 = top3;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    // Centered headers
                    ChatFormatter.constructDivider(pl, " ", 45);
                    ChatFormatter.sendCenteredMessage(pl,
                        ChatColor.GOLD + "" + ChatColor.BOLD + " FIELD BOSS SLAIN!");
                    ChatFormatter.sendCenteredMessage(pl,
                        ChatColor.GRAY + "Time Elapsed: " + ChatColor.WHITE + fElapsed);
                    ChatFormatter.constructDivider(pl, " ", 45);

                    // Left‐aligned “Leaderboard”
                    pl.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "        Leaderboard");

                    // Now the top‐3 lines with damage+percent
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
}

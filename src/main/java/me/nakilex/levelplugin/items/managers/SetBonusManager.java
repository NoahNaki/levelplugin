package me.nakilex.levelplugin.items.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * Handles armor prefix/suffix set bonuses. If a player equips multiple armor
 * pieces sharing a prefix or suffix, small percentage-based stat bonuses are
 * granted. Bonuses are removed when the set is broken.
 */
public class SetBonusManager {

    private static final SetBonusManager instance = new SetBonusManager();
    public static SetBonusManager getInstance() { return instance; }

    private final Map<UUID, BonusStats> activeBonuses = new HashMap<>();

    private final Map<String, StatType> prefixStat = new HashMap<>();
    private final Map<String, StatType> suffixStat = new HashMap<>();
    private final Set<String> prefixStrings = new HashSet<>();
    private final Set<String> suffixStrings = new HashSet<>();

    private SetBonusManager() {
        Main plugin = Main.getInstance();
        File preFile = new File(plugin.getDataFolder(), "prefixes.yml");
        File sufFile = new File(plugin.getDataFolder(), "suffixes.yml");
        FileConfiguration preCfg = YamlConfiguration.loadConfiguration(preFile);
        FileConfiguration sufCfg = YamlConfiguration.loadConfiguration(sufFile);

        // Map prefix categories to stats (rough heuristic)
        Map<String, StatType> catMap = new HashMap<>();
        catMap.put("skeleton", StatType.DEX);
        catMap.put("zombie", StatType.STR);
        catMap.put("slime", StatType.INT);
        catMap.put("default", StatType.DEF);

        for (String cat : preCfg.getKeys(false)) {
            List<String> list = preCfg.getStringList(cat);
            prefixStrings.addAll(list);
            StatType st = catMap.getOrDefault(cat.toLowerCase(), StatType.DEF);
            for (String p : list) {
                prefixStat.put(p, st);
            }
        }

        for (String key : sufCfg.getKeys(false)) {
            StatType st = mapSuffixKey(key);
            for (String s : sufCfg.getStringList(key)) {
                suffixStrings.add(s);
                suffixStat.put(s, st);
            }
        }
    }

    private StatType mapSuffixKey(String key) {
        switch (key.toLowerCase()) {
            case "strength": return StatType.STR;
            case "agility": return StatType.AGI;
            case "dexterity": return StatType.DEX;
            case "intelligence": return StatType.INT;
            case "defense": return StatType.DEF;
            default: return StatType.DEF;
        }
    }

    /** Re-scan a player's equipped armor and update any set bonuses. */
    public void updatePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        BonusStats old = activeBonuses.remove(uuid);
        if (old != null) removeBonus(player, old);

        ItemStack[] armor = player.getInventory().getArmorContents();
        Map<String,Integer> prefixCount = new HashMap<>();
        Map<String,Integer> suffixCount = new HashMap<>();
        Map<String,Integer> pairCount   = new HashMap<>();
        Map<String,StatType> suffixType = new HashMap<>();
        Map<String,StatType> prefixType = new HashMap<>();
        Map<String,StatType> pairType   = new HashMap<>();

        for (ItemStack it : armor) {
            if (it == null || it.getType().isAir()) continue;
            CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(it);
            if (ci == null) continue;
            String name = ci.getBaseName();
            String pre = parsePrefix(name);
            String suf = parseSuffix(name);
            if (pre != null) {
                prefixCount.merge(pre, 1, Integer::sum);
                prefixType.put(pre, prefixStat.getOrDefault(pre, StatType.DEF));
            }
            if (suf != null) {
                suffixCount.merge(suf, 1, Integer::sum);
                suffixType.put(suf, suffixStat.getOrDefault(suf, StatType.DEF));
            }
            if (pre != null && suf != null) {
                String key = pre + "|" + suf;
                pairCount.merge(key, 1, Integer::sum);
                pairType.put(key, suffixStat.getOrDefault(suf, StatType.DEF));
            }
        }

        BonusStats total = new BonusStats();

        Map.Entry<String,Integer> preBest = maxEntry(prefixCount);
        if (preBest != null && preBest.getValue() >= 2) {
            int pct = calcPct(preBest.getValue(), false);
            StatType st = prefixType.get(preBest.getKey());
            addPercentBonus(player, total, st, pct);
        }

        Map.Entry<String,Integer> sufBest = maxEntry(suffixCount);
        if (sufBest != null && sufBest.getValue() >= 2) {
            int pct = calcPct(sufBest.getValue(), false);
            StatType st = suffixType.get(sufBest.getKey());
            addPercentBonus(player, total, st, pct);
        }

        Map.Entry<String,Integer> pairBest = maxEntry(pairCount);
        if (pairBest != null && pairBest.getValue() >= 2) {
            int pct = calcPct(pairBest.getValue(), true);
            StatType st = pairType.get(pairBest.getKey());
            addPercentBonus(player, total, st, pct);
        }

        if (!total.isZero()) {
            applyBonus(player, total);
            activeBonuses.put(uuid, total);
        }

        StatsManager.getInstance().recalcDerivedStats(player);
    }

    private Map.Entry<String,Integer> maxEntry(Map<String,Integer> map) {
        return map.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).orElse(null);
    }

    private int calcPct(int count, boolean pair) {
        if (pair) {
            if (count >= 4) return 30; else if (count == 3) return 20; else return 10;
        } else {
            if (count >= 4) return 15; else if (count == 3) return 10; else return 5;
        }
    }

    private void addPercentBonus(Player player, BonusStats total, StatType stat, int percent) {
        int current = StatsManager.getInstance().getStatValue(player, stat);
        int bonus = (int)Math.round(current * (percent / 100.0));
        switch (stat) {
            case STR: total.str += bonus; break;
            case AGI: total.agi += bonus; break;
            case INT: total.intel += bonus; break;
            case DEX: total.dex += bonus; break;
            case DEF: total.def += bonus; break;
            case HP:  total.hp  += bonus; break;
        }
        total.percents.put(stat, percent);
    }

    private void applyBonus(Player player, BonusStats b) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        ps.bonusStrength     += b.str;
        ps.bonusAgility      += b.agi;
        ps.bonusIntelligence += b.intel;
        ps.bonusDexterity    += b.dex;
        ps.bonusDefenceStat  += b.def;
        ps.bonusHealthStat   += b.hp;

        if (!b.percents.isEmpty()) {
            StringBuilder msg = new StringBuilder("§aSet bonus applied:");
            for (Map.Entry<StatType,Integer> e : b.percents.entrySet()) {
                msg.append(" +").append(e.getValue()).append("% ")
                   .append(e.getKey().name().toLowerCase());
            }
            player.sendMessage(msg.toString());
        } else {
            player.sendMessage("§aSet bonus applied!");
        }
    }

    private void removeBonus(Player player, BonusStats b) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        ps.bonusStrength     -= b.str;
        ps.bonusAgility      -= b.agi;
        ps.bonusIntelligence -= b.intel;
        ps.bonusDexterity    -= b.dex;
        ps.bonusDefenceStat  -= b.def;
        ps.bonusHealthStat   -= b.hp;
        if (!b.percents.isEmpty()) {
            StringBuilder msg = new StringBuilder("§cSet bonus lost:");
            for (Map.Entry<StatType,Integer> e : b.percents.entrySet()) {
                msg.append(" -").append(e.getValue()).append("% ")
                   .append(e.getKey().name().toLowerCase());
            }
            player.sendMessage(msg.toString());
        } else {
            player.sendMessage("§cSet bonus lost.");
        }
    }

    private String parsePrefix(String name) {
        for (String p : prefixStrings) {
            if (name.startsWith(p + " ")) return p;
            if (name.equals(p)) return p;
        }
        return null;
    }

    private String parseSuffix(String name) {
        for (String s : suffixStrings) {
            if (name.endsWith(" " + s)) return s;
            if (name.contains(" of " + s)) return s;
            if (name.endsWith(" " + s.replace("of ", ""))) return s;
        }
        return null;
    }

    private static class BonusStats {
        int str, agi, intel, dex, def, hp;
        final Map<StatType,Integer> percents = new LinkedHashMap<>();

        boolean isZero() {
            return str==0 && agi==0 && intel==0 && dex==0 && def==0 && hp==0;
        }
    }
}

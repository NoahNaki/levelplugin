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

        for (String key : preCfg.getKeys(false)) {
            String prefix = preCfg.getString(key);
            if (prefix == null) continue;
            StatType st = mapSuffixKey(key); // reuse mapper for stats
            prefixStrings.add(prefix);
            prefixStat.put(prefix, st);
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
            case "hp": return StatType.HP;
            case "defense": return StatType.DEF;
            default: return StatType.DEF;
        }
    }

    /**
     * Re-scan a player's equipped armor and update any set bonuses.
     * Messages are only sent when the bonus actually changes.
     */
    public void updatePlayer(Player player) {
        UUID uuid = player.getUniqueId();

        ItemStack[] armor = player.getInventory().getArmorContents();
        BonusStats oldBonus = activeBonuses.get(uuid);
        BonusStats newBonus = calculateBonus(player, armor);

        if (bonusEquals(oldBonus, newBonus)) {
            return; // nothing changed
        }

        if (oldBonus != null) {
            // Only show removal message when the bonus actually disappears
            boolean showMsg = newBonus.isZero();
            removeBonus(player, oldBonus, showMsg);
        }

        if (!newBonus.isZero()) {
            applyBonus(player, newBonus);
            activeBonuses.put(uuid, newBonus);
        } else {
            activeBonuses.remove(uuid);
        }
        StatsManager.getInstance().recalcDerivedStats(player);
    }

    private BonusStats calculateBonus(Player player, ItemStack[] armor) {
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
            addPercentBonus(player, total, st, pct, preBest.getValue());
        }

        Map.Entry<String,Integer> sufBest = maxEntry(suffixCount);
        if (sufBest != null && sufBest.getValue() >= 2) {
            int pct = calcPct(sufBest.getValue(), false);
            StatType st = suffixType.get(sufBest.getKey());
            addPercentBonus(player, total, st, pct, sufBest.getValue());
        }

        Map.Entry<String,Integer> pairBest = maxEntry(pairCount);
        if (pairBest != null && pairBest.getValue() >= 2) {
            int pct = calcPct(pairBest.getValue(), true);
            StatType st = pairType.get(pairBest.getKey());
            addPercentBonus(player, total, st, pct, pairBest.getValue());
        }

        return total;
    }

    private boolean bonusEquals(BonusStats a, BonusStats b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.str == b.str && a.agi == b.agi && a.intel == b.intel &&
               a.dex == b.dex && a.def == b.def && a.hp == b.hp &&
               a.percents.equals(b.percents) && a.counts.equals(b.counts);
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

    private void addPercentBonus(Player player, BonusStats total, StatType stat, int percent, int pieces) {
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
        total.percents.merge(stat, percent, Integer::sum);
        total.counts.merge(stat, pieces, Math::max);
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
            for (Map.Entry<StatType,Integer> e : b.percents.entrySet()) {
                int pieces = b.counts.getOrDefault(e.getKey(), 0);
                String statName = getDisplayName(e.getKey());
                player.sendMessage("§7[" + pieces + "/4] §6§lSET BONUS APPLIED §a+" + e.getValue() + "% " + statName);
            }
        }
    }

    private void removeBonus(Player player, BonusStats b, boolean showMsg) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        ps.bonusStrength     -= b.str;
        ps.bonusAgility      -= b.agi;
        ps.bonusIntelligence -= b.intel;
        ps.bonusDexterity    -= b.dex;
        ps.bonusDefenceStat  -= b.def;
        ps.bonusHealthStat   -= b.hp;
        if (showMsg && !b.percents.isEmpty()) {
            for (Map.Entry<StatType,Integer> e : b.percents.entrySet()) {
                String statName = getDisplayName(e.getKey());
                player.sendMessage("§6§lSET BONUS REMOVED §c-" + e.getValue() + "% " + statName);
            }
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

    private String getDisplayName(StatType type) {
        switch (type) {
            case STR:  return "Strength";
            case AGI:  return "Agility";
            case INT:  return "Intelligence";
            case DEX:  return "Dexterity";
            case DEF:  return "Defense";
            case HP:   return "Health";
            default:   return type.name();
        }
    }

    private static class BonusStats {
        int str, agi, intel, dex, def, hp;
        final Map<StatType,Integer> percents = new LinkedHashMap<>();
        final Map<StatType,Integer> counts   = new HashMap<>();

        boolean isZero() {
            return str==0 && agi==0 && intel==0 && dex==0 && def==0 && hp==0;
        }
    }
}

package me.nakilex.levelplugin.items.generator;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.data.StatRange;
import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.mob.utils.CombatRewardCalculator;
import me.nakilex.levelplugin.mob.utils.CombatRewardCalculator.GearTarget;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;

/**
 * Simple procedural item generator used for testing. It builds item names
 * from prefix/suffix lists and rolls basic stats based on mob level and rarity.
 */
public class ProceduralItemGenerator {

    private final FileConfiguration namesConfig;
    private final FileConfiguration prefixesConfig;
    private final FileConfiguration suffixesConfig;
    private final Random random = new Random();
    /** Percentage range (±) applied when rolling stat min/max values. */
    private static final double ROLL_VARIANCE = 0.05;
    /** Extra bonus applied to one random armor stat for variety. */
    private static final double DOMINANT_BONUS = 0.10;
    /** Base coefficient used when scaling health for armor pieces. */
    private static final double HP_COEFF = 1.5;
    /** Rarity growth factor ensuring higher rarities always outrank lower ones. */
    private static final double RARITY_STEP = 1.4;

    public ProceduralItemGenerator(Main plugin) {
        File namesFile = new File(plugin.getDataFolder(), "item_names.yml");
        if (!namesFile.exists()) {
            plugin.saveResource("item_names.yml", true);
        }
        namesConfig = YamlConfiguration.loadConfiguration(namesFile);

        File prefixesFile = new File(plugin.getDataFolder(), "prefixes.yml");
        if (!prefixesFile.exists()) {
            plugin.saveResource("prefixes.yml", true);
        }
        prefixesConfig = YamlConfiguration.loadConfiguration(prefixesFile);

        File suffixesFile = new File(plugin.getDataFolder(), "suffixes.yml");
        if (!suffixesFile.exists()) {
            plugin.saveResource("suffixes.yml", true);
        }
        suffixesConfig = YamlConfiguration.loadConfiguration(suffixesFile);
    }

    /**
     * Generate a new CustomItem based on a mob type and level.
     */
    public CustomItem generate(String mobType, int level) {
        return generateInternal(mobType, level, null, null);
    }

    public CustomItem generateWithMaxRarity(String mobType, int level, ItemRarity maxRarity) {
        return generateInternal(mobType, level, null, maxRarity);
    }

    public CustomItem generateWithRarity(String mobType, int level, ItemRarity rarity) {
        return generateInternal(mobType, level, rarity, null);
    }

    public CustomItem generateForGearScore(String mobType, GearTarget target) {
        return generateForGearScore(mobType, target, null);
    }

    public CustomItem generateForGearScore(String mobType, GearTarget target, Integer forcedLevel) {
        if (target == null) {
            return generateWithRarity(mobType, 1, ItemRarity.COMMON);
        }
        int desired = Math.max(1, target.targetGearScore());
        ItemRarity rarity = target.rarity();
        int level = forcedLevel != null
                ? Math.max(1, forcedLevel)
                : Math.max(1, (int) Math.round(desired / rarityMultiplier(rarity)));
        level = Math.min(level, 100);

        CustomItem best = null;
        double bestDiff = Double.MAX_VALUE;
        for (int i = 0; i < 6; i++) {
            CustomItem candidate = generateInternal(mobType, level, rarity, rarity);
            double gearScore = SalvageManager.getInstance().getTotalStats(candidate);
            double diff = Math.abs(gearScore - desired);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = candidate;
            }
            if (diff <= desired * 0.10) {
                break; // close enough
            }
            if (forcedLevel == null) {
                double ratio = desired / Math.max(1.0, gearScore);
                level = Math.max(1, (int) Math.round(level * ratio));
            }
        }
        return best != null ? best : generateInternal(mobType, level, rarity, rarity);
    }

    public CustomItem generateForCombatPower(int combatPower, String mobType) {
        GearTarget target = CombatRewardCalculator.rollGearTarget(combatPower);
        return generateForGearScore(mobType, target);
    }

    private CustomItem generateInternal(String mobType, int level, ItemRarity forcedRarity, ItemRarity maxRarity) {
        level = Math.min(level, 100);
        ItemRarity rarity = forcedRarity != null ? forcedRarity : rollRarity(level, maxRarity);
        String safeMobType = normalizeMobType(mobType);
        String clazz = pickClassForMob(safeMobType);

        // Randomly decide whether to create armor or a weapon
        boolean createArmor = random.nextBoolean();
        ArmorType armorSlot = null;
        String baseDisplay;
        if (createArmor) {
            Map.Entry<ArmorType, String> base = pickArmorName();
            armorSlot = base.getKey();
            baseDisplay = base.getValue();
        } else {
            Map.Entry<String, String> base = pickWeaponName(clazz);
            baseDisplay = base.getValue();
        }
        int hp = 0, def, str, agi, intel, dex, wil, tec;

        if (createArmor) {
            hp  = scaleStat(level, rarity, HP_COEFF);
            def = scaleStat(level, rarity, 1.0);
            str   = scaleStat(level, rarity, 1.0);
            agi   = scaleStat(level, rarity, 1.0);
            intel = scaleStat(level, rarity, 1.0);
            dex   = scaleStat(level, rarity, 1.0);
            wil   = scaleStat(level, rarity, 1.0);
            tec   = scaleStat(level, rarity, 1.0);

            // Give the armor a random dominant stat for variety
            int choice = random.nextInt(5); // str, agi, intel, dex, def
            switch (choice) {
                case 0 -> str  = (int) Math.round(str  * (1 + DOMINANT_BONUS));
                case 1 -> agi  = (int) Math.round(agi  * (1 + DOMINANT_BONUS));
                case 2 -> intel= (int) Math.round(intel* (1 + DOMINANT_BONUS));
                case 3 -> dex  = (int) Math.round(dex  * (1 + DOMINANT_BONUS));
                case 4 -> def  = (int) Math.round(def  * (1 + DOMINANT_BONUS));
            }
        } else {
            def = 0;
            switch (clazz) {
                case "WARRIOR" -> {
                    str = scaleStat(level, rarity, 2.0);
                    agi = scaleStat(level, rarity, 1.0);
                    dex = scaleStat(level, rarity, 1.0);
                    intel = scaleStat(level, rarity, 1.0);
                    wil = scaleStat(level, rarity, 1.0);
                    tec = scaleStat(level, rarity, 1.0);
                }
                case "ROGUE", "ARCHER" -> {
                    dex = scaleStat(level, rarity, 2.0);
                    agi = scaleStat(level, rarity, 1.0);
                    str = scaleStat(level, rarity, 1.0);
                    intel = scaleStat(level, rarity, 1.0);
                    wil = scaleStat(level, rarity, 1.0);
                    tec = scaleStat(level, rarity, 1.0);
                }
                case "MAGE" -> {
                    intel = scaleStat(level, rarity, 2.0);
                    agi = scaleStat(level, rarity, 1.0);
                    dex = scaleStat(level, rarity, 1.0);
                    str = scaleStat(level, rarity, 1.0);
                    wil = scaleStat(level, rarity, 1.0);
                    tec = scaleStat(level, rarity, 1.0);
                }
                default -> {
                    str = scaleStat(level, rarity, 1.0);
                    agi = scaleStat(level, rarity, 1.0);
                    dex = scaleStat(level, rarity, 1.0);
                    intel = scaleStat(level, rarity, 1.0);
                    wil = scaleStat(level, rarity, 1.0);
                    tec = scaleStat(level, rarity, 1.0);
                }
            }
        }

        GearStat dominantStat = getDominantStatType(hp, def, str, agi, intel, dex, wil, tec);
        EnumSet<GearStat> chosen = selectStatsForRarity(rarity, dominantStat, hp, def, str, agi, intel, dex, wil, tec);
        hp = chosen.contains(GearStat.HP) ? hp : 0;
        def = chosen.contains(GearStat.DEF) ? def : 0;
        str = chosen.contains(GearStat.STR) ? str : 0;
        agi = chosen.contains(GearStat.AGI) ? agi : 0;
        intel = chosen.contains(GearStat.INT) ? intel : 0;
        dex = chosen.contains(GearStat.DEX) ? dex : 0;
        wil = chosen.contains(GearStat.WIL) ? wil : 0;
        tec = chosen.contains(GearStat.TEC) ? tec : 0;

        String dominant = getDominantStatKey(hp, def, str, agi, intel, dex, wil, tec);
        String name = buildName(safeMobType, baseDisplay, rarity, dominant);
        Material material = createArmor ? resolveArmorMaterial(level, armorSlot) : pickWeaponMaterial(clazz, level);

        String classReq = createArmor ? "ANY" : clazz;

        // Assign a unique negative ID so each generated item can be tracked
        int genId = ItemManager.getInstance().getNextGeneratedId();

        CustomItem item = new CustomItem(
            genId,
            name,
            rarity,
            level,
            classReq,
            material,
            createRange(hp),
            createRange(def),
            createRange(str),
            createRange(agi),
            createRange(intel),
            createRange(dex),
            createRange(wil),
            createRange(tec)
        );

        ItemManager.getInstance().addInstance(item);
        return item;
    }

    private String buildName(String mobType, String base, ItemRarity rarity, String statKey) {
        String name = base;

        if (rarity.ordinal() >= ItemRarity.RARE.ordinal()) {
            List<String> suffixes = suffixesConfig.getStringList(statKey);
            if (suffixes.isEmpty()) {
                suffixes = suffixesConfig.getStringList("default");
            }
            if (!suffixes.isEmpty()) {
                String suffix = suffixes.get(random.nextInt(suffixes.size()));
                if (suffix.startsWith("of")) {
                    name += " " + suffix;
                } else {
                    name += " of " + suffix;
                }
            }
        }
        return name;
    }

    /**
     * Build a rollable range around a target value so generated items
     * can be rerolled later on. The range is roughly ±5% of the value.
     */
    private StatRange createRange(int value) {
        if (value <= 0) {
            return new StatRange(0, 0);
        }
        int min = Math.max(0, (int) Math.round(value * (1 - ROLL_VARIANCE)));
        int max = Math.max(min + 1, (int) Math.round(value * (1 + ROLL_VARIANCE)));
        return new StatRange(min, max);
    }

    private String getDominantStatKey(int hp, int def, int str, int agi, int intel, int dex, int wil, int tec) {
        GearStat dominant = getDominantStatType(hp, def, str, agi, intel, dex, wil, tec);
        if (dominant == null) {
            return "default";
        }
        return switch (dominant) {
            case HP -> "vitality";
            case DEF -> "defense";
            case STR -> "strength";
            case AGI -> "agility";
            case INT -> "intelligence";
            case DEX -> "dexterity";
            case WIL -> "will";
            case TEC -> "technique";
        };
    }

    private GearStat getDominantStatType(int hp, int def, int str, int agi, int intel, int dex, int wil, int tec) {
        GearStat dominant = null;
        int max = 0;
        int[] values = {hp, def, str, agi, intel, dex, wil, tec};
        GearStat[] stats = GearStat.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
                dominant = stats[i];
            }
        }
        return dominant;
    }

    private EnumSet<GearStat> selectStatsForRarity(ItemRarity rarity,
                                                   GearStat dominant,
                                                   int hp, int def, int str, int agi, int intel, int dex, int wil, int tec) {
        if (rarity == ItemRarity.COMMON && hp > 0) {
            return EnumSet.of(GearStat.HP);
        }
        List<GearStat> available = new ArrayList<>();
        if (hp > 0) available.add(GearStat.HP);
        if (def > 0) available.add(GearStat.DEF);
        if (str > 0) available.add(GearStat.STR);
        if (agi > 0) available.add(GearStat.AGI);
        if (intel > 0) available.add(GearStat.INT);
        if (dex > 0) available.add(GearStat.DEX);
        if (wil > 0) available.add(GearStat.WIL);
        if (tec > 0) available.add(GearStat.TEC);
        if (available.isEmpty()) {
            return EnumSet.noneOf(GearStat.class);
        }

        int slots = Math.min(getStatSlotsForRarity(rarity), available.size());
        EnumSet<GearStat> chosen = EnumSet.noneOf(GearStat.class);
        if (dominant != null && available.remove(dominant)) {
            chosen.add(dominant);
        }
        Collections.shuffle(available, random);
        for (GearStat stat : available) {
            if (chosen.size() >= slots) break;
            chosen.add(stat);
        }
        return chosen;
    }

    private int getStatSlotsForRarity(ItemRarity rarity) {
        if (rarity == null) return 1;
        return switch (rarity) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC -> 4;
            case LEGENDARY -> 5;
            case MYTHIC, FABLED -> 6;
        };
    }

    private String normalizeMobType(String mobType) {
        if (mobType == null) {
            return "";
        }

        String trimmed = mobType.trim();
        if (trimmed.equalsIgnoreCase("null")) {
            return "";
        }

        return trimmed;
    }

    private String pickClassForMob(String mobType) {
        if (mobType == null || mobType.isBlank()) {
            return pickRandomClass();
        }

        String type = mobType.toLowerCase(Locale.ROOT);
        if (type.contains("skeleton")) return random.nextBoolean() ? "ARCHER" : "ROGUE";
        if (type.contains("zombie")) return "WARRIOR";
        if (type.contains("slime")) return "MAGE";
        // default random class
        return pickRandomClass();
    }

    private String pickRandomClass() {
        String[] classes = {"ROGUE", "MAGE", "ARCHER", "WARRIOR"};
        return classes[random.nextInt(classes.length)];
    }

    private Map.Entry<ArmorType, String> pickArmorName() {
        ConfigurationSection sec = namesConfig.getConfigurationSection("armor_types");
        if (sec == null || sec.getKeys(false).isEmpty()) {
            return new AbstractMap.SimpleEntry<>(ArmorType.BOOTS, "Boots");
        }
        ArmorType[] types = ArmorType.values();
        ArmorType slot = types[random.nextInt(types.length)];
        List<String> names = sec.getStringList(slot.name().toLowerCase());
        if (names.isEmpty()) {
            names = Collections.singletonList(slot.name().substring(0,1) + slot.name().substring(1).toLowerCase());
        }
        String display = names.get(random.nextInt(names.size()));
        return new AbstractMap.SimpleEntry<>(slot, display);
    }

    private Map.Entry<String, String> pickWeaponName(String clazz) {
        List<String> list = namesConfig.getStringList("weapon_types." + clazz);
        if (list.isEmpty()) {
            list.add("Item");
        }
        String display = list.get(random.nextInt(list.size()));
        return new AbstractMap.SimpleEntry<>(display, display);
    }

    private Material pickWeaponMaterial(String clazz, int level) {
        if (level >= 76) {
            switch (clazz) {
                case "ROGUE": return Material.NETHERITE_SWORD;
                case "MAGE":  return Material.STICK;
                case "ARCHER":return Material.BOW;
                case "WARRIOR":default: return Material.NETHERITE_SHOVEL;
            }
        } else if (level >= 61) {
            switch (clazz) {
                case "ROGUE": return Material.DIAMOND_SWORD;
                case "MAGE":  return Material.STICK;
                case "ARCHER":return Material.BOW;
                case "WARRIOR":default: return Material.DIAMOND_SHOVEL;
            }
        } else if (level >= 41) {
            switch (clazz) {
                case "ROGUE": return Material.IRON_SWORD;
                case "MAGE":  return Material.STICK;
                case "ARCHER":return Material.BOW;
                case "WARRIOR":default: return Material.IRON_SHOVEL;
            }
        } else if (level >= 21) {
            switch (clazz) {
                case "ROGUE": return Material.GOLDEN_SWORD;
                case "MAGE":  return Material.STICK;
                case "ARCHER":return Material.BOW;
                case "WARRIOR":default: return Material.GOLDEN_SHOVEL;
            }
        } else if (level >= 11) {
            switch (clazz) {
                case "ROGUE": return Material.STONE_SWORD;
                case "MAGE":  return Material.STICK;
                case "ARCHER":return Material.BOW;
                case "WARRIOR":default: return Material.STONE_SHOVEL;
            }
        }
        switch (clazz) {
            case "ROGUE": return Material.WOODEN_SWORD;
            case "MAGE":  return Material.STICK;
            case "ARCHER":return Material.BOW;
            case "WARRIOR":default: return Material.WOODEN_SHOVEL;
        }
    }

    public static Material resolveArmorMaterial(int level, ArmorType slot) {
        String suffix;
        if (level >= 76)      suffix = "NETHERITE_";
        else if (level >= 61) suffix = "DIAMOND_";
        else if (level >= 46) suffix = "IRON_";
        else if (level >= 31) suffix = "CHAINMAIL_";
        else if (level >= 16) suffix = "GOLDEN_";
        else                  suffix = "LEATHER_";

        String matName = suffix + slot.name();
        try {
            return Material.valueOf(matName);
        } catch (IllegalArgumentException ex) {
            return Material.LEATHER_BOOTS;
        }
    }

    private ItemRarity rollRarity(int level, ItemRarity maxRarity) {
        ItemRarity rarity;
        double r = random.nextDouble() * 100.0;
        if (r < 30.0) rarity = ItemRarity.COMMON;
        else if (r < 70.0) rarity = ItemRarity.UNCOMMON;
        else if (r < 90.0) rarity = ItemRarity.RARE;
        else if (r < 99.0) rarity = ItemRarity.EPIC;
        else if (level >= 7 && r < 99.9) rarity = ItemRarity.LEGENDARY;
        else if (level >= 10) rarity = ItemRarity.MYTHIC;
        else rarity = ItemRarity.EPIC;

        if (maxRarity != null && rarity.ordinal() > maxRarity.ordinal()) {
            return maxRarity;
        }
        return rarity;
    }

    /**
     * Scaling factor for stats based on rarity. Each step multiplies stats by a
     * fixed growth factor so higher rarities always beat lower ones.
     */
    private double rarityMultiplier(ItemRarity rarity) {
        return Math.pow(RARITY_STEP, rarity.ordinal());
    }

    /**
     * Convenience to scale a stat by level, rarity and a slot-specific coefficient.
     * We ceil the result to guarantee that rarities never overlap after rounding.
     */
    private int scaleStat(int level, ItemRarity rarity, double coeff) {
        double value = coeff * level * rarityMultiplier(rarity);
        return (int) Math.ceil(value);
    }

    private enum GearStat {
        HP,
        DEF,
        STR,
        AGI,
        INT,
        DEX,
        WIL,
        TEC
    }
}

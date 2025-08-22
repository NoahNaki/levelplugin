package me.nakilex.levelplugin.items.generator;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.data.StatRange;
import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.managers.ItemManager;
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

/**
 * Simple procedural item generator used for testing. It builds item names
 * from prefix/suffix lists and rolls basic stats based on mob level and rarity.
 */
public class ProceduralItemGenerator {

    private final FileConfiguration namesConfig;
    private final FileConfiguration prefixesConfig;
    private final FileConfiguration suffixesConfig;
    private final Random random = new Random();

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
        ItemRarity rarity = rollRarity(level);
        String clazz = pickClassForMob(mobType);

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
            hp  = scaleStat(level, rarity, 2.0);
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
                case 0 -> str  = (int) Math.round(str  * 1.2);
                case 1 -> agi  = (int) Math.round(agi  * 1.2);
                case 2 -> intel= (int) Math.round(intel* 1.2);
                case 3 -> dex  = (int) Math.round(dex  * 1.2);
                case 4 -> def  = (int) Math.round(def  * 1.2);
            }
        } else {
            def = scaleStat(level, rarity, 1.0);
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

        String dominant = getDominantStat(str, agi, intel, dex, def);
        String name = buildName(mobType, baseDisplay, rarity, dominant);
        Material material = createArmor ? pickArmorMaterial(level, armorSlot) : pickWeaponMaterial(clazz, level);

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
     * can be rerolled later on. The range is roughly ±10% of the value.
     */
    private StatRange createRange(int value) {
        if (value <= 0) {
            return new StatRange(0, 0);
        }
        int min = Math.max(0, (int) Math.round(value * 0.9));
        int max = Math.max(min + 1, (int) Math.round(value * 1.1));
        return new StatRange(min, max);
    }

    private String getDominantStat(int str, int agi, int intel, int dex, int def) {
        int max = str;
        String key = "strength";
        if (agi > max) { max = agi; key = "agility"; }
        if (intel > max) { max = intel; key = "intelligence"; }
        if (dex > max) { max = dex; key = "dexterity"; }
        if (def > max) { key = "defense"; }
        return key;
    }

    private String pickClassForMob(String mobType) {
        String type = mobType.toLowerCase();
        if (type.contains("skeleton")) return random.nextBoolean() ? "ARCHER" : "ROGUE";
        if (type.contains("zombie")) return "WARRIOR";
        if (type.contains("slime")) return "MAGE";
        // default random class
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

    private Material pickArmorMaterial(int level, ArmorType slot) {
        String suffix;
        if (level >= 76)      suffix = "NETHERITE_";
        else if (level >= 61) suffix = "DIAMOND_";
        else if (level >= 41) suffix = "IRON_";
        else if (level >= 21) suffix = "GOLDEN_";
        else if (level >= 11) suffix = "CHAINMAIL_";
        else                  suffix = "LEATHER_";

        String matName = suffix + slot.name();
        try {
            return Material.valueOf(matName);
        } catch (IllegalArgumentException ex) {
            return Material.LEATHER_BOOTS;
        }
    }

    private ItemRarity rollRarity(int level) {
        double r = random.nextDouble() * 100.0;
        if (r < 30.0) return ItemRarity.COMMON;
        if (r < 70.0) return ItemRarity.UNCOMMON;
        if (r < 90.0) return ItemRarity.RARE;
        if (r < 99.0) return ItemRarity.EPIC;
        if (level >= 7 && r < 99.9) return ItemRarity.LEGENDARY;
        if (level >= 10) return ItemRarity.MYTHIC;
        return ItemRarity.EPIC;
    }

    /**
     * Scaling factor for stats based on rarity. Each step multiplies stats by 1.3,
     * ensuring higher rarity items always have higher baselines than lower ones.
     */
    private double rarityMultiplier(ItemRarity rarity) {
        return Math.pow(1.3, rarity.ordinal());
    }

    /**
     * Convenience to scale a stat by level, rarity and a slot-specific coefficient.
     */
    private int scaleStat(int level, ItemRarity rarity, double coeff) {
        double value = coeff * level * rarityMultiplier(rarity);
        return (int) Math.round(value);
    }
}

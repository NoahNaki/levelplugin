package me.nakilex.levelplugin.items.generator;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.data.StatRange;
import me.nakilex.levelplugin.items.managers.ItemManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Random;

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

        boolean createArmor = random.nextBoolean();
        String base = pickBaseName(clazz, createArmor);
        int baseVal = Math.max(1, level);

        int hp = 0, def = 0, str = 0, agi = 0, intel = 0, dex = 0;

        if (createArmor) {
            hp  = baseVal * 2;
            def = baseVal;
            str   = random.nextInt(baseVal + 1);
            agi   = random.nextInt(baseVal + 1);
            intel = random.nextInt(baseVal + 1);
            dex   = random.nextInt(baseVal + 1);
        } else {
            def = random.nextInt(baseVal + 1);
            switch (clazz) {
                case "WARRIOR":
                    str = baseVal * 2;
                    agi = random.nextInt(baseVal + 1);
                    dex = random.nextInt(baseVal + 1);
                    intel = random.nextInt(baseVal + 1);
                    break;
                case "ROGUE":
                    dex = baseVal * 2;
                    agi = baseVal;
                    str = random.nextInt(baseVal + 1);
                    intel = random.nextInt(baseVal + 1);
                    break;
                case "ARCHER":
                    dex = baseVal * 2;
                    agi = baseVal;
                    str = random.nextInt(baseVal + 1);
                    intel = random.nextInt(baseVal + 1);
                    break;
                case "MAGE":
                    intel = baseVal * 2;
                    agi = random.nextInt(baseVal + 1);
                    dex = random.nextInt(baseVal + 1);
                    str = random.nextInt(baseVal + 1);
                    break;
            }
        }

        double mult = 1.0 + rarityBonus(rarity);
        hp    = (int) (hp * mult);
        def   = (int) (def * mult);
        str   = (int) (str * mult);
        agi   = (int) (agi * mult);
        dex   = (int) (dex * mult);
        intel = (int) (intel * mult);

        String dominant = getDominantStat(str, agi, intel, dex, def);
        String name = buildName(mobType, base, rarity, dominant);
        Material material = createArmor ? pickArmorMaterial(level, base) : pickWeaponMaterial(clazz, level);

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
            createRange(dex)
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
     * can be rerolled later on. The range is roughly ±20% of the value.
     */
    private StatRange createRange(int value) {
        if (value <= 0) {
            return new StatRange(0, 0);
        }
        int min = Math.max(0, (int) Math.round(value * 0.8));
        int max = Math.max(min + 1, (int) Math.round(value * 1.2));
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

    private String pickBaseName(String clazz, boolean armor) {
        List<String> list = armor ?
                namesConfig.getStringList("armor_types.general") :
                namesConfig.getStringList("weapon_types." + clazz);
        if (list.isEmpty()) {
            list.add(armor ? "Boots" : "Item");
        }
        return list.get(random.nextInt(list.size()));
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

    private Material pickArmorMaterial(int level, String partName) {
        partName = partName.toUpperCase();
        String suffix;
        if (level >= 76)      suffix = "NETHERITE_";
        else if (level >= 61) suffix = "DIAMOND_";
        else if (level >= 41) suffix = "IRON_";
        else if (level >= 21) suffix = "GOLDEN_";
        else if (level >= 11) suffix = "CHAINMAIL_";
        else                  suffix = "LEATHER_";

        String matName = suffix + partName.toUpperCase().replace(' ', '_');
        try {
            return Material.valueOf(matName);
        } catch (IllegalArgumentException ex) {
            // Fallback to LEATHER_BOOTS if invalid
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

    private double rarityBonus(ItemRarity rarity) {
        switch (rarity) {
            case UNCOMMON:
                return 0.05;
            case RARE:
                return 0.1;
            case EPIC:
                return 0.2;
            case LEGENDARY:
                return 0.3;
            case MYTHIC:
                return 0.5;
            default:
                return 0.0;
        }
    }
}

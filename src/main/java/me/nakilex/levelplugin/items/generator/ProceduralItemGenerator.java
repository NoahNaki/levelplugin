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
 * from prefix lists and rolls basic stats based on mob level and rarity.
 */
public class ProceduralItemGenerator {

    private final FileConfiguration config;
    private final Random random = new Random();

    public ProceduralItemGenerator(Main plugin) {
        File file = new File(plugin.getDataFolder(), "item_names.yml");
        if (!file.exists()) {
            plugin.saveResource("item_names.yml", true);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Generate a new CustomItem based on a mob type and level.
     */
    public CustomItem generate(String mobType, int level) {
        ItemRarity rarity = rollRarity(level);
        String clazz = pickClassForMob(mobType);
        String name = buildName(mobType, clazz);
        Material material = pickMaterial(clazz);

        int base = Math.max(1, level);
        int hp = (int) (base * 1.2);
        int str = 0, intel = 0, dex = 0;
        switch (clazz) {
            case "WARRIOR":
                str = base * 2;
                break;
            case "ROGUE":
                str = base;
                dex = base;
                break;
            case "ARCHER":
                dex = base * 2;
                break;
            case "MAGE":
                intel = base * 2;
                break;
        }
        double mult = 1.0 + rarityBonus(rarity);
        hp = (int) (hp * mult);
        str = (int) (str * mult);
        dex = (int) (dex * mult);
        intel = (int) (intel * mult);

        CustomItem item = new CustomItem(
                -1,
                name,
                rarity,
                level,
                clazz,
                material,
                new StatRange(hp, hp),
                new StatRange(0, 0),
                new StatRange(str, str),
                new StatRange(0, 0),
                new StatRange(intel, intel),
                new StatRange(dex, dex)
        );

        ItemManager.getInstance().addInstance(item);
        return item;
    }

    private String buildName(String mobType, String clazz) {
        List<String> prefixes = config.getStringList("prefixes." + mobType.toLowerCase());
        if (prefixes.isEmpty()) {
            prefixes = config.getStringList("prefixes.default");
        }
        List<String> bases = config.getStringList("weapon_types." + clazz);
        if (bases.isEmpty()) {
            bases.add("Item");
        }
        String prefix = prefixes.get(random.nextInt(prefixes.size()));
        String base = bases.get(random.nextInt(bases.size()));
        return prefix + " " + base;
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

    private Material pickMaterial(String clazz) {
        switch (clazz) {
            case "ROGUE":
                return Material.WOODEN_SWORD;
            case "MAGE":
                return Material.STICK;
            case "ARCHER":
                return Material.BOW;
            case "WARRIOR":
            default:
                return Material.WOODEN_SHOVEL;
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

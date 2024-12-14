package me.nakilex.levelplugin.items;

import org.bukkit.Material;

public class CustomItem {

    private int id;
    private String name;
    private ItemRarity rarity;
    private int levelRequirement;
    private String classRequirement;  // WARRIOR, ROGUE, etc. or ANY
    private Material material;

    // Stats
    private int hp;
    private int def;
    private int str;
    private int agi;
    private int intel;
    private int dex;

    public CustomItem(int id, String name, ItemRarity rarity, int levelRequirement, String classRequirement,
                      Material material, int hp, int def, int str, int agi, int intel, int dex) {
        this.id = id;
        this.name = name;
        this.rarity = rarity;
        this.levelRequirement = levelRequirement;
        this.classRequirement = classRequirement;
        this.material = material;
        this.hp = hp;
        this.def = def;
        this.str = str;
        this.agi = agi;
        this.intel = intel;
        this.dex = dex;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public ItemRarity getRarity() { return rarity; }
    public int getLevelRequirement() { return levelRequirement; }
    public String getClassRequirement() { return classRequirement; }
    public Material getMaterial() { return material; }

    public int getHp() { return hp; }
    public int getDef() { return def; }
    public int getStr() { return str; }
    public int getAgi() { return agi; }
    public int getIntel() { return intel; }
    public int getDex() { return dex; }
}

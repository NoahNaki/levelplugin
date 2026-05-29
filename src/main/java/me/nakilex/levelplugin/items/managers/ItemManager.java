package me.nakilex.levelplugin.items.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.data.StatRange;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ArmorBiasUtil;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.mob.utils.CombatRewardCalculator.GearTarget;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import me.nakilex.levelplugin.items.generator.ProceduralItemGenerator;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Map;

public class ItemManager {

    private static ItemManager instance;

    public static ItemManager getInstance() {
        return instance;
    }

    private final Map<Integer, CustomItem> templatesMap = new HashMap<>(); // Templates by ID
    private final Map<UUID, CustomItem> itemsMap     = new HashMap<>(); // Instances by UUID
    private final Map<Integer, UUID> holderMap = new HashMap<>();

    /**
     * Procedurally generated items all receive unique negative IDs. We keep a
     * counter that starts at -1 and decrements for each generated item so the
     * IDs never collide with the positive template IDs from items.yml.
     */
    private int nextGeneratedId = -1;

    private final ProceduralItemGenerator generator;

    private FileConfiguration itemsConfig;

    public ItemManager(Plugin plugin) {
        instance = this;
        loadItemsConfig(plugin);
        loadItems();
        generator = new ProceduralItemGenerator(Main.getInstance());
    }

    private void loadItemsConfig(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.exists()) {
            plugin.saveResource("items.yml", false);
        }
        itemsConfig = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = plugin.getResource("items.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defStream, StandardCharsets.UTF_8));
            itemsConfig.setDefaults(defConfig);
        }
    }

    private void loadItems() {
        templatesMap.clear();
        itemsMap.clear();

        if (!itemsConfig.contains("items")) {
            Main.getInstance().getLogger().warning("No items found in items.yml!");
            return;
        }

        int schema = itemsConfig.getInt("schema", 1);
        if (schema == 2) {
            loadItemsV2(itemsConfig.getConfigurationSection("items"));
        } else {
            loadItemsV1(itemsConfig.getConfigurationSection("items"));
        }

        Main.getInstance().getLogger()
            .info("Loaded " + templatesMap.size() + " custom item templates from items.yml.");
    }

    private void loadItemsV1(org.bukkit.configuration.ConfigurationSection itemsSection) {
        if (itemsSection == null) {
            return;
        }
        for (String key : itemsSection.getKeys(false)) {
            try {
                int numericId = Integer.parseInt(key);
                String path = "items." + key + ".";

                String name       = itemsConfig.getString(path + "name", "Unknown Item");
                ItemRarity rarity = ItemRarity.valueOf(
                    itemsConfig.getString(path + "rarity", "COMMON").toUpperCase());
                int levelReq      = itemsConfig.getInt(path + "level_requirement", 1);
                String classReq   = "ANY";
                Material material = Material.valueOf(
                    itemsConfig.getString(path + "material", "STONE").toUpperCase());

                StatRange hpRange    = StatRange.fromString(
                    itemsConfig.getString(path + "hp", "0-0"));
                StatRange defRange   = StatRange.fromString(
                    itemsConfig.getString(path + "def", "0-0"));
                StatRange strRange   = StatRange.fromString(
                    itemsConfig.getString(path + "str", "0-0"));
                StatRange agiRange   = StatRange.fromString(
                    itemsConfig.getString(path + "agi", "0-0"));
                StatRange intelRange = StatRange.fromString(
                    itemsConfig.getString(path + "intel", "0-0"));
                StatRange dexRange   = StatRange.fromString(
                    itemsConfig.getString(path + "dex", "0-0"));
                StatRange wilRange   = StatRange.fromString(
                    itemsConfig.getString(path + "wil", "0-0"));
                StatRange tecRange   = StatRange.fromString(
                    itemsConfig.getString(path + "tec", "0-0"));

                addTemplate(numericId, name, rarity, levelReq, classReq, material,
                        hpRange, defRange, strRange, agiRange, intelRange, dexRange, wilRange, tecRange);
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Failed to load item with key: " + key);
                e.printStackTrace();
            }
        }
    }

    private void loadItemsV2(org.bukkit.configuration.ConfigurationSection itemsSection) {
        if (itemsSection == null) {
            return;
        }
        for (String key : itemsSection.getKeys(false)) {
            try {
                org.bukkit.configuration.ConfigurationSection section = itemsSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                int numericId = Integer.parseInt(key);
                String name = section.getString("name", "Unknown Item");
                ItemRarity rarity = ItemRarity.valueOf(section.getString("rarity", "COMMON").toUpperCase());

                org.bukkit.configuration.ConfigurationSection reqSection = section.getConfigurationSection("requirements");
                int levelReq = reqSection != null ? reqSection.getInt("level", 1) : 1;
                String classReq = "ANY";

                org.bukkit.configuration.ConfigurationSection visuals = section.getConfigurationSection("visuals");
                Material material = visuals != null
                        ? Material.valueOf(visuals.getString("baseMaterial", "STONE").toUpperCase())
                        : Material.STONE;
                org.bukkit.configuration.ConfigurationSection statsSection = section.getConfigurationSection("stats");
                StatRange hpRange = parseStatRange(statsSection, "hp");
                StatRange defRange = parseStatRange(statsSection, "def");
                StatRange strRange = parseStatRange(statsSection, "str");
                StatRange agiRange = parseStatRange(statsSection, "agi");
                StatRange intelRange = parseStatRange(statsSection, "intel");
                StatRange dexRange = parseStatRange(statsSection, "dex");
                StatRange wilRange = parseStatRange(statsSection, "wil");
                StatRange tecRange = parseStatRange(statsSection, "tec");

                addTemplate(numericId, name, rarity, levelReq, classReq, material,
                        hpRange, defRange, strRange, agiRange, intelRange, dexRange, wilRange, tecRange);
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Failed to load item with key: " + key);
                e.printStackTrace();
            }
        }
    }

    private void addTemplate(int numericId,
                             String name,
                             ItemRarity rarity,
                             int levelReq,
                             String classReq,
                             Material material,
                             StatRange hpRange,
                             StatRange defRange,
                             StatRange strRange,
                             StatRange agiRange,
                             StatRange intelRange,
                             StatRange dexRange,
                             StatRange wilRange,
                             StatRange tecRange) {
        TemplateRanges normalized = normalizeTemplateRanges(
                levelReq,
                rarity,
                material,
                hpRange,
                defRange,
                strRange,
                agiRange,
                intelRange,
                dexRange,
                wilRange,
                tecRange);

        CustomItem template = new CustomItem(
                numericId,
                name,
                rarity,
                levelReq,
                classReq,
                material,
                normalized.hpRange(),
                normalized.defRange(),
                normalized.strRange(),
                normalized.agiRange(),
                normalized.intelRange(),
                normalized.dexRange(),
                normalized.wilRange(),
                normalized.tecRange()
        );

        templatesMap.put(numericId, template);
    }

    private StatRange parseStatRange(org.bukkit.configuration.ConfigurationSection statsSection, String key) {
        if (statsSection == null || key == null) {
            return new StatRange(0, 0);
        }
        Object raw = statsSection.get(key);
        if (raw instanceof Number number) {
            int value = (int) Math.round(number.doubleValue());
            return new StatRange(value, value);
        }
        if (raw instanceof String str) {
            try {
                return StatRange.fromString(str);
            } catch (IllegalArgumentException ignored) {
                return new StatRange(0, 0);
            }
        }
        if (raw instanceof org.bukkit.configuration.ConfigurationSection section) {
            if (section.contains("fixed")) {
                int value = (int) Math.round(section.getDouble("fixed"));
                return new StatRange(value, value);
            }
            org.bukkit.configuration.ConfigurationSection rangeSection = section.getConfigurationSection("range");
            if (rangeSection != null) {
                int min = (int) Math.round(rangeSection.getDouble("min"));
                int max = (int) Math.round(rangeSection.getDouble("max"));
                return new StatRange(min, max);
            }
        }
        return new StatRange(0, 0);
    }

    private String resolveClassRequirement(java.util.List<String> classes) {
        if (classes == null || classes.isEmpty()) {
            return "ANY";
        }
        for (String entry : classes) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String normalized = entry.trim().toUpperCase();
            if (normalized.equals("ROGUE")
                    || normalized.equals("ARCHER")
                    || normalized.equals("MAGE")
                    || normalized.equals("WARRIOR")) {
                return normalized;
            }
        }
        return "ANY";
    }

    private TemplateRanges normalizeTemplateRanges(int levelRequirement,
                                                   ItemRarity rarity,
                                                   Material material,
                                                   StatRange hpRange,
                                                   StatRange defRange,
                                                   StatRange strRange,
                                                   StatRange agiRange,
                                                   StatRange intelRange,
                                                   StatRange dexRange,
                                                   StatRange wilRange,
                                                   StatRange tecRange) {
        boolean isArmor = ArmorType.fromMaterial(material) != null;
        boolean isGear = isArmor || isWeaponMaterial(material);
        StatRange safeHpRange = isArmor ? hpRange : new StatRange(0, 0);
        int desired = me.nakilex.levelplugin.items.generator.ProceduralItemGenerator.getStatSlotsForRarity(rarity);
        List<StatSlot> missing = new ArrayList<>();

        int count = 0;
        boolean hasVitality = isRangeNonZero(safeHpRange) || isRangeNonZero(defRange);
        if (hasVitality) {
            count++;
        } else if (isArmor) {
            missing.add(StatSlot.HP);
        }

        EnumMap<StatSlot, StatRange> ranges = new EnumMap<>(StatSlot.class);
        ranges.put(StatSlot.STR, strRange);
        ranges.put(StatSlot.AGI, agiRange);
        ranges.put(StatSlot.INT, intelRange);
        ranges.put(StatSlot.DEX, dexRange);
        ranges.put(StatSlot.WIL, wilRange);
        ranges.put(StatSlot.TEC, tecRange);

        for (StatSlot slot : NON_VITALITY_STAT_ORDER) {
            if (isRangeNonZero(ranges.get(slot))) {
                count++;
            } else {
                missing.add(slot);
            }
        }

        if (isArmor && rarity == ItemRarity.COMMON && !hasVitality) {
            safeHpRange = me.nakilex.levelplugin.items.generator.ProceduralItemGenerator
                    .buildTemplateRange(levelRequirement, rarity,
                            me.nakilex.levelplugin.items.generator.ProceduralItemGenerator.ARMOR_HP_COEFF);
            count++;
            missing.remove(StatSlot.HP);
        }

        if (isGear && count < desired && !missing.isEmpty()) {
            java.util.Collections.shuffle(missing, new Random());
            int toAdd = Math.min(desired - count, missing.size());
            for (int i = 0; i < toAdd; i++) {
                StatSlot slot = missing.get(i);
                StatRange generated = me.nakilex.levelplugin.items.generator.ProceduralItemGenerator
                        .buildTemplateRange(levelRequirement, rarity,
                                slot == StatSlot.HP
                                        ? me.nakilex.levelplugin.items.generator.ProceduralItemGenerator.ARMOR_HP_COEFF
                                        : 1.0);
                if (slot == StatSlot.HP) {
                    safeHpRange = generated;
                } else {
                    ranges.put(slot, generated);
                }
            }
            count += toAdd;
        }

        if (isGear && count > desired) {
            int toTrim = count - desired;
            for (StatSlot slot : TRIM_PRIORITY) {
                if (toTrim <= 0) {
                    break;
                }
                if (slot == StatSlot.HP) {
                    if (!isRangeNonZero(safeHpRange) || isRangeNonZero(defRange)) {
                        continue;
                    }
                    safeHpRange = new StatRange(0, 0);
                } else {
                    if (!isRangeNonZero(ranges.get(slot))) {
                        continue;
                    }
                    ranges.put(slot, new StatRange(0, 0));
                }
                toTrim--;
            }
        }

        return new TemplateRanges(
                safeHpRange,
                defRange,
                ranges.get(StatSlot.STR),
                ranges.get(StatSlot.AGI),
                ranges.get(StatSlot.INT),
                ranges.get(StatSlot.DEX),
                ranges.get(StatSlot.WIL),
                ranges.get(StatSlot.TEC)
        );
    }

    private boolean isWeaponMaterial(Material material) {
        if (material == null) {
            return false;
        }
        for (me.nakilex.levelplugin.items.data.WeaponType weaponType : me.nakilex.levelplugin.items.data.WeaponType.values()) {
            if (weaponType.getMaterials().contains(material)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRangeNonZero(StatRange range) {
        return range != null && (range.getMin() > 0 || range.getMax() > 0);
    }

    private static final List<StatSlot> NON_VITALITY_STAT_ORDER = List.of(
            StatSlot.STR,
            StatSlot.AGI,
            StatSlot.INT,
            StatSlot.DEX,
            StatSlot.WIL,
            StatSlot.TEC
    );

    private static final List<StatSlot> TRIM_PRIORITY = List.of(
            StatSlot.DEX,
            StatSlot.INT,
            StatSlot.AGI,
            StatSlot.STR,
            StatSlot.WIL,
            StatSlot.TEC,
            StatSlot.HP
    );

    private enum StatSlot {
        HP,
        STR,
        AGI,
        INT,
        DEX,
        WIL,
        TEC
    }

    private record TemplateRanges(StatRange hpRange,
                                  StatRange defRange,
                                  StatRange strRange,
                                  StatRange agiRange,
                                  StatRange intelRange,
                                  StatRange dexRange,
                                  StatRange wilRange,
                                  StatRange tecRange) {}

    /** Returns a new map of templates, keyed by numeric ID */
    public Map<Integer, CustomItem> getAllTemplates() {
        return new HashMap<>(templatesMap);
    }

    /** Fetch the template (with ranges) for a given numeric ID */
    public CustomItem getTemplateById(int id) {
        return templatesMap.get(id);
    }

    /** Register a freshly‐rolled instance */
    public void addInstance(CustomItem instance) {
        itemsMap.put(instance.getUuid(), instance);
    }

    /** Lookup a live instance by its UUID */
    public CustomItem getItemByUUID(UUID uuid) {
        return itemsMap.get(uuid);
    }

    /**
     * Given an ItemStack with our PDC UUID tag, pull out the matching
     * CustomItem instance.
     */
    public CustomItem getCustomItemFromItemStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;

        ItemMeta meta = itemStack.getItemMeta();
        UUID uuid = ItemUtil.getItemUUID(itemStack);
        if (uuid == null) return null;

        CustomItem ci = itemsMap.get(uuid);
        if (ci == null) {
            Main.getInstance().getLogger()
                .info("No custom item found for UUID: " + uuid);
        }
        return ci;
    }

    public CustomItem rollNewInstance(int templateId) {
        CustomItem tpl = templatesMap.get(templateId);
        if (tpl == null) return null;

        if (ArmorType.fromMaterial(tpl.getMaterial()) != null) {
            double total = averageRange(tpl.getHpRange())
                    + averageRange(tpl.getDefRange())
                    + averageRange(tpl.getStrRange())
                    + averageRange(tpl.getAgiRange())
                    + averageRange(tpl.getIntelRange())
                    + averageRange(tpl.getDexRange())
                    + averageRange(tpl.getWilRange())
                    + averageRange(tpl.getTecRange());
            ArmorBiasUtil.ArmorBias bias = ArmorBiasUtil.ArmorBias.fromPrefix(tpl.getBaseName());
            if (bias == null) {
                bias = ArmorBiasUtil.randomBias(new Random());
            }
            var allocation = ArmorBiasUtil.allocate((int) Math.round(total), tpl.getLevelRequirement(), bias);
            CustomItem inst = new CustomItem(
                    tpl.getId(),
                    ArmorBiasUtil.applyPrefix(tpl.getBaseName(), bias),
                    tpl.getRarity(),
                    tpl.getLevelRequirement(),
                    "ANY",
                    tpl.getMaterial(),
                    ArmorBiasUtil.toRange(allocation.get(ArmorBiasUtil.ArmorStat.HP)),
                    ArmorBiasUtil.toRange(allocation.get(ArmorBiasUtil.ArmorStat.DEF)),
                    ArmorBiasUtil.toRange(allocation.get(ArmorBiasUtil.ArmorStat.STR)),
                    ArmorBiasUtil.toRange(allocation.get(ArmorBiasUtil.ArmorStat.AGI)),
                    ArmorBiasUtil.toRange(allocation.get(ArmorBiasUtil.ArmorStat.INT)),
                    ArmorBiasUtil.toRange(allocation.get(ArmorBiasUtil.ArmorStat.DEX)),
                    ArmorBiasUtil.toRange(allocation.get(ArmorBiasUtil.ArmorStat.WIL)),
                    ArmorBiasUtil.toRange(allocation.get(ArmorBiasUtil.ArmorStat.TEC))
            );
            addInstance(inst);
            return inst;
        }

        CustomItem inst = new CustomItem(
                tpl.getId(),
                tpl.getBaseName(),
                tpl.getRarity(),
                tpl.getLevelRequirement(),
                tpl.getClassRequirement(),
                tpl.getMaterial(),
                tpl.getHpRange(),
                tpl.getDefRange(),
                tpl.getStrRange(),
                tpl.getAgiRange(),
                tpl.getIntelRange(),
                tpl.getDexRange(),
                tpl.getWilRange(),
                tpl.getTecRange()
        );
        addInstance(inst);
        return inst;
    }

    private double averageRange(StatRange range) {
        if (range == null) {
            return 0;
        }
        return (range.getMin() + range.getMax()) / 2.0;
    }

    /**
     * Obtain the next unique ID for a procedurally generated item. IDs start at
     * -1 and move downward to avoid colliding with positive template IDs.
     */
    public synchronized int getNextGeneratedId() {
        return nextGeneratedId--;
    }

    /**
     * Generate a brand new procedural item using the generator utility.
     */
    public CustomItem generateItem(String mobType, int level) {
        return generator.generate(mobType, level);
    }

    public CustomItem generateItemWithMaxRarity(String mobType, int level, ItemRarity maxRarity) {
        return generator.generateWithMaxRarity(mobType, level, maxRarity);
    }

    public CustomItem generateItemForGearScore(String mobType, int targetGearScore, ItemRarity rarity) {
        return generator.generateForGearScore(mobType, new GearTarget(targetGearScore, rarity));
    }

    public CustomItem generateItemForGearScore(String mobType, int targetGearScore, ItemRarity rarity, int levelRequirement) {
        return generator.generateForGearScore(mobType, new GearTarget(targetGearScore, rarity), levelRequirement);
    }

    public CustomItem getCustomItem(int id) {
        return getTemplateById(id);
    }

    public Map<UUID, CustomItem> getAllItems() {
        return new HashMap<>(itemsMap);
    }

    public void registerHolder(int itemID, UUID puuid) {
        holderMap.put(itemID, puuid);
    }

    public void unregisterHolder(int itemID) {
        holderMap.remove(itemID);
    }

    public Player getHolderOf(int itemID) {
        UUID puuid = holderMap.get(itemID);
        if (puuid == null) return null;
        return Bukkit.getPlayer(puuid);
    }
}

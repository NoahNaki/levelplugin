package me.nakilex.levelplugin.player.classes.essence;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility methods for creating and identifying class essence items.
 * These items represent playable classes as tangible items which can
 * later be slotted into a GUI to grant bonuses.
 */
public final class ClassEssence {

    private static final NamespacedKey ESSENCE_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ClassEssence.class), "class_essence");
    private static final NamespacedKey CLASS_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ClassEssence.class), "essence_class");
    private static final NamespacedKey RARITY_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ClassEssence.class), "essence_rarity");
    private static final NamespacedKey STAR_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ClassEssence.class), "essence_star");
    private static final NamespacedKey ATTR_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ClassEssence.class), "essence_attrs");
    private static final NamespacedKey EQUIPPED_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ClassEssence.class), "essence_equipped");
    private static final NamespacedKey EXP_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ClassEssence.class), "essence_exp");
    private static final NamespacedKey NEXT_EXP_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ClassEssence.class), "essence_next_exp");
    private static final NamespacedKey SOULBOUND_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(ClassEssence.class), "essence_soulbound");

    private record AttrData(int value, boolean percent) {}

    private ClassEssence() {}

    /**
     * Generate a class essence item with random class, rarity and attributes.
     * Star level defaults to 0.
     */
    public static ItemStack generateRandomEssence() {
        Random rand = new Random();
        PlayerClass[] classes = PlayerClass.values();
        PlayerClass clazz = classes[rand.nextInt(classes.length)];
        ItemRarity[] rarities = ItemRarity.values();
        ItemRarity rarity = rarities[rand.nextInt(rarities.length)];
        int slots = getAttributeSlots(rarity);
        Map<StatType, AttrData> attrs = rollAttributes(slots, rarity, 0, rand);
        return create(clazz, rarity, 0, attrs, true);
    }

    /**
     * Create an essence item with the provided parameters.
     */
    public static ItemStack create(PlayerClass clazz, ItemRarity rarity, int starLevel,
                                   Map<StatType, AttrData> attributes, boolean soulbound) {
        ItemStack stack = new ItemStack(Material.BOOK);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.setDisplayName(rarity.getColor() + clazz.name() + " Class Essence");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        int exp = 0;
        int next = getRarityThreshold(rarity);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Rarity: " + rarity.name());
        lore.add(ChatColor.GRAY + "Stars: " + starLevel);
        lore.add(ChatColor.GRAY + "EXP: " + GuiUtil.createProgressBar(0, 10) + ChatColor.YELLOW + exp + ChatColor.GRAY + "/" + next);
        lore.add(" ");
        for (Map.Entry<StatType, AttrData> entry : attributes.entrySet()) {
            String suffix = entry.getValue().percent ? "%" : "";
            lore.add(ChatColor.GREEN + entry.getKey().name() + ": +" + entry.getValue().value + suffix);
        }
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ESSENCE_KEY, PersistentDataType.BYTE, (byte)1);
        pdc.set(CLASS_KEY, PersistentDataType.STRING, clazz.name());
        pdc.set(RARITY_KEY, PersistentDataType.STRING, rarity.name());
        pdc.set(STAR_KEY, PersistentDataType.INTEGER, starLevel);
        pdc.set(EXP_KEY, PersistentDataType.INTEGER, exp);
        pdc.set(NEXT_EXP_KEY, PersistentDataType.INTEGER, next);
        pdc.set(SOULBOUND_KEY, PersistentDataType.BYTE, soulbound ? (byte)1 : (byte)0);
        String attrString = attributes.entrySet().stream()
                .map(e -> e.getKey().name() + ":" + e.getValue().value + ":" + (e.getValue().percent ? 1 : 0))
                .collect(Collectors.joining(";"));
        pdc.set(ATTR_KEY, PersistentDataType.STRING, attrString);

        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Determine how many attribute slots an essence of the given rarity should have.
     */
    public static int getAttributeSlots(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC -> 4;
            case LEGENDARY -> 5;
            case MYTHIC -> 6;
            case FABLED -> 7;
        };
    }

    private static double getRarityMult(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0.0;
            case UNCOMMON -> 0.05;
            case RARE -> 0.10;
            case EPIC -> 0.20;
            case LEGENDARY -> 0.30;
            case MYTHIC -> 0.40;
            case FABLED -> 0.50;
        };
    }

    private static int getRarityThreshold(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 100;
            case UNCOMMON -> 200;
            case RARE -> 300;
            case EPIC -> 400;
            case LEGENDARY -> 500;
            case MYTHIC -> 600;
            case FABLED -> 0; // max rarity
        };
    }

    private static Map<StatType, AttrData> rollAttributes(int slots, ItemRarity rarity, int starLevel, Random rand) {
        List<StatType> stats = new ArrayList<>(Arrays.asList(StatType.values()));
        Collections.shuffle(stats, rand);
        Map<StatType, AttrData> map = new LinkedHashMap<>();
        int percentSlots = Math.min(starLevel, slots);
        for (int i = 0; i < slots && i < stats.size(); i++) {
            StatType st = stats.get(i);
            int base = 10 + rand.nextInt(91);
            double rarityMult = 1.0 + getRarityMult(rarity);
            double starMult = 1.0 + (starLevel * 0.05);
            int value = (int)Math.round(base * rarityMult * starMult);
            boolean percent = i < percentSlots;
            map.put(st, new AttrData(value, percent));
        }
        return map;
    }

    /**
     * Checks whether the given item is a class essence.
     */
    public static boolean isEssence(ItemStack stack) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte flag = pdc.get(ESSENCE_KEY, PersistentDataType.BYTE);
        return flag != null && flag == (byte)1;
    }

    public static PlayerClass getClass(ItemStack stack) {
        if (!isEssence(stack)) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String clazz = meta.getPersistentDataContainer().get(CLASS_KEY, PersistentDataType.STRING);
        try {
            return PlayerClass.valueOf(clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static ItemRarity getRarity(ItemStack stack) {
        if (!isEssence(stack)) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String r = meta.getPersistentDataContainer().get(RARITY_KEY, PersistentDataType.STRING);
        try {
            return ItemRarity.valueOf(r);
        } catch (Exception e) {
            return null;
        }
    }

    /** Mark an essence as equipped or unequipped. */
    public static void setEquipped(ItemStack stack, boolean equipped) {
        if (!isEssence(stack)) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(EQUIPPED_KEY, PersistentDataType.BYTE, equipped ? (byte)1 : (byte)0);
        stack.setItemMeta(meta);
        stack.setType(equipped ? Material.ENCHANTED_BOOK : Material.BOOK);
    }

    public static boolean isEquipped(ItemStack stack) {
        if (!isEssence(stack)) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        Byte flag = meta.getPersistentDataContainer().get(EQUIPPED_KEY, PersistentDataType.BYTE);
        return flag != null && flag == (byte)1;
    }

    /** Extract attribute map from an essence item. */
    public static Map<StatType, AttrData> getAttributes(ItemStack stack) {
        Map<StatType, AttrData> map = new LinkedHashMap<>();
        if (!isEssence(stack)) return map;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return map;
        String attrString = meta.getPersistentDataContainer().get(ATTR_KEY, PersistentDataType.STRING);
        if (attrString == null || attrString.isEmpty()) return map;
        for (String part : attrString.split(";")) {
            String[] kv = part.split(":");
            if (kv.length < 2) continue;
            try {
                StatType st = StatType.valueOf(kv[0]);
                int val = Integer.parseInt(kv[1]);
                boolean percent = kv.length > 2 && "1".equals(kv[2]);
                map.put(st, new AttrData(val, percent));
            } catch (IllegalArgumentException ignored) {}
        }
        return map;
    }

    /** Apply attribute bonuses of an essence to a player. */
    public static void applyAttributes(org.bukkit.entity.Player player, ItemStack stack) {
        Map<StatType, AttrData> attrs = getAttributes(stack);
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        for (Map.Entry<StatType, AttrData> e : attrs.entrySet()) {
            int bonus;
            if (e.getValue().percent) {
                int current = StatsManager.getInstance().getStatValue(player, e.getKey());
                bonus = (int)Math.round(current * e.getValue().value / 100.0);
            } else {
                bonus = e.getValue().value;
            }
            switch (e.getKey()) {
                case STR -> ps.bonusStrength += bonus;
                case AGI -> ps.bonusAgility += bonus;
                case INT -> ps.bonusIntelligence += bonus;
                case DEX -> ps.bonusDexterity += bonus;
                case VIT -> ps.bonusVitality += bonus;
                case WIL -> ps.bonusWill += bonus;
                case TEC -> ps.bonusTechnique += bonus;
            }
        }
        StatsManager.getInstance().recalcDerivedStats(player);
    }

    public static void removeAttributes(org.bukkit.entity.Player player, ItemStack stack) {
        Map<StatType, AttrData> attrs = getAttributes(stack);
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        for (Map.Entry<StatType, AttrData> e : attrs.entrySet()) {
            int bonus;
            if (e.getValue().percent) {
                int total = StatsManager.getInstance().getStatValue(player, e.getKey());
                double percent = e.getValue().value / 100.0;
                bonus = (int)Math.round(total * percent / (1 + percent));
            } else {
                bonus = e.getValue().value;
            }
            switch (e.getKey()) {
                case STR -> ps.bonusStrength = Math.max(0, ps.bonusStrength - bonus);
                case AGI -> ps.bonusAgility = Math.max(0, ps.bonusAgility - bonus);
                case INT -> ps.bonusIntelligence = Math.max(0, ps.bonusIntelligence - bonus);
                case DEX -> ps.bonusDexterity = Math.max(0, ps.bonusDexterity - bonus);
                case VIT -> ps.bonusVitality = Math.max(0, ps.bonusVitality - bonus);
                case WIL -> ps.bonusWill = Math.max(0, ps.bonusWill - bonus);
                case TEC -> ps.bonusTechnique = Math.max(0, ps.bonusTechnique - bonus);
            }
        }
        StatsManager.getInstance().recalcDerivedStats(player);
    }

    public static boolean isSoulbound(ItemStack stack) {
        if (!isEssence(stack)) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        Byte flag = meta.getPersistentDataContainer().get(SOULBOUND_KEY, PersistentDataType.BYTE);
        return flag != null && flag == (byte)1;
    }

    public static int getExp(ItemStack stack) {
        if (!isEssence(stack)) return 0;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 0;
        Integer val = meta.getPersistentDataContainer().get(EXP_KEY, PersistentDataType.INTEGER);
        return val == null ? 0 : val;
    }

    public static void addExp(ItemStack stack, int amount) {
        if (!isEssence(stack) || amount <= 0) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int exp = pdc.has(EXP_KEY, PersistentDataType.INTEGER) ? pdc.get(EXP_KEY, PersistentDataType.INTEGER) : 0;
        exp += amount;
        int next = pdc.has(NEXT_EXP_KEY, PersistentDataType.INTEGER) ? pdc.get(NEXT_EXP_KEY, PersistentDataType.INTEGER) : 0;
        ItemRarity rarity = ItemRarity.valueOf(pdc.get(RARITY_KEY, PersistentDataType.STRING));
        int star = pdc.has(STAR_KEY, PersistentDataType.INTEGER) ? pdc.get(STAR_KEY, PersistentDataType.INTEGER) : 0;
        if (next > 0 && exp >= next) {
            exp -= next;
            rarity = nextRarity(rarity);
            int slots = getAttributeSlots(rarity);
            Map<StatType, AttrData> newAttrs = rollAttributes(slots, rarity, star, new Random());
            String attrString = newAttrs.entrySet().stream()
                    .map(e -> e.getKey().name() + ":" + e.getValue().value + ":" + (e.getValue().percent ? 1 : 0))
                    .collect(Collectors.joining(";"));
            pdc.set(ATTR_KEY, PersistentDataType.STRING, attrString);
            next = getRarityThreshold(rarity);
            pdc.set(RARITY_KEY, PersistentDataType.STRING, rarity.name());
        }
        pdc.set(EXP_KEY, PersistentDataType.INTEGER, exp);
        pdc.set(NEXT_EXP_KEY, PersistentDataType.INTEGER, next);
        stack.setItemMeta(meta);
        updateLore(stack);
    }

    private static ItemRarity nextRarity(ItemRarity r) {
        return switch (r) {
            case COMMON -> ItemRarity.UNCOMMON;
            case UNCOMMON -> ItemRarity.RARE;
            case RARE -> ItemRarity.EPIC;
            case EPIC -> ItemRarity.LEGENDARY;
            case LEGENDARY -> ItemRarity.MYTHIC;
            case MYTHIC -> ItemRarity.FABLED;
            case FABLED -> ItemRarity.FABLED;
        };
    }

    public static void updateLore(ItemStack stack) {
        if (!isEssence(stack)) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        ItemRarity rarity = ItemRarity.valueOf(pdc.get(RARITY_KEY, PersistentDataType.STRING));
        int star = pdc.has(STAR_KEY, PersistentDataType.INTEGER) ? pdc.get(STAR_KEY, PersistentDataType.INTEGER) : 0;
        int exp = pdc.has(EXP_KEY, PersistentDataType.INTEGER) ? pdc.get(EXP_KEY, PersistentDataType.INTEGER) : 0;
        int next = pdc.has(NEXT_EXP_KEY, PersistentDataType.INTEGER) ? pdc.get(NEXT_EXP_KEY, PersistentDataType.INTEGER) : 0;
        Map<StatType, AttrData> attrs = getAttributes(stack);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Rarity: " + rarity.name());
        lore.add(ChatColor.GRAY + "Stars: " + star);
        double progress = next > 0 ? (double)exp / next : 1.0;
        lore.add(ChatColor.GRAY + "EXP: " + GuiUtil.createProgressBar(progress, 10) + ChatColor.YELLOW + exp + ChatColor.GRAY + "/" + next);
        lore.add(" ");
        for (Map.Entry<StatType, AttrData> e : attrs.entrySet()) {
            String suffix = e.getValue().percent ? "%" : "";
            lore.add(ChatColor.GREEN + e.getKey().name() + ": +" + e.getValue().value + suffix);
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }
}

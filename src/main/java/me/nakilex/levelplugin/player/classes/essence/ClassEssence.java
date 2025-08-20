package me.nakilex.levelplugin.player.classes.essence;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
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
        Map<StatType, Integer> attrs = rollAttributes(slots, rand);
        return create(clazz, rarity, 0, attrs);
    }

    /**
     * Create an essence item with the provided parameters.
     */
    public static ItemStack create(PlayerClass clazz, ItemRarity rarity, int starLevel, Map<StatType, Integer> attributes) {
        ItemStack stack = new ItemStack(Material.BOOK);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.setDisplayName(rarity.getColor() + clazz.name() + " Class Essence");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Rarity: " + rarity.name());
        lore.add(ChatColor.GRAY + "Stars: " + starLevel);
        lore.add(" ");
        for (Map.Entry<StatType, Integer> entry : attributes.entrySet()) {
            lore.add(ChatColor.GREEN + entry.getKey().name() + ": +" + entry.getValue());
        }
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ESSENCE_KEY, PersistentDataType.BYTE, (byte)1);
        pdc.set(CLASS_KEY, PersistentDataType.STRING, clazz.name());
        pdc.set(RARITY_KEY, PersistentDataType.STRING, rarity.name());
        pdc.set(STAR_KEY, PersistentDataType.INTEGER, starLevel);
        String attrString = attributes.entrySet().stream()
                .map(e -> e.getKey().name() + ":" + e.getValue())
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

    private static Map<StatType, Integer> rollAttributes(int slots, Random rand) {
        List<StatType> stats = new ArrayList<>(Arrays.asList(StatType.values()));
        Collections.shuffle(stats, rand);
        Map<StatType, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < slots && i < stats.size(); i++) {
            StatType st = stats.get(i);
            int value = 10 + rand.nextInt(91); // random 10-100
            map.put(st, value);
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
    public static Map<StatType,Integer> getAttributes(ItemStack stack) {
        Map<StatType,Integer> map = new LinkedHashMap<>();
        if (!isEssence(stack)) return map;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return map;
        String attrString = meta.getPersistentDataContainer().get(ATTR_KEY, PersistentDataType.STRING);
        if (attrString == null || attrString.isEmpty()) return map;
        for (String part : attrString.split(";")) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            try {
                StatType st = StatType.valueOf(kv[0]);
                int val = Integer.parseInt(kv[1]);
                map.put(st, val);
            } catch (IllegalArgumentException ignored) {}
        }
        return map;
    }

    /** Apply attribute bonuses of an essence to a player. */
    public static void applyAttributes(org.bukkit.entity.Player player, ItemStack stack) {
        Map<StatType,Integer> attrs = getAttributes(stack);
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        for (Map.Entry<StatType,Integer> e : attrs.entrySet()) {
            switch (e.getKey()) {
                case STR -> ps.bonusStrength += e.getValue();
                case AGI -> ps.bonusAgility += e.getValue();
                case INT -> ps.bonusIntelligence += e.getValue();
                case DEX -> ps.bonusDexterity += e.getValue();
                case VIT -> ps.bonusVitality += e.getValue();
                case WIL -> ps.bonusWill += e.getValue();
                case TEC -> ps.bonusTechnique += e.getValue();
            }
        }
        StatsManager.getInstance().recalcDerivedStats(player);
    }

    public static void removeAttributes(org.bukkit.entity.Player player, ItemStack stack) {
        Map<StatType,Integer> attrs = getAttributes(stack);
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        for (Map.Entry<StatType,Integer> e : attrs.entrySet()) {
            switch (e.getKey()) {
                case STR -> ps.bonusStrength = Math.max(0, ps.bonusStrength - e.getValue());
                case AGI -> ps.bonusAgility = Math.max(0, ps.bonusAgility - e.getValue());
                case INT -> ps.bonusIntelligence = Math.max(0, ps.bonusIntelligence - e.getValue());
                case DEX -> ps.bonusDexterity = Math.max(0, ps.bonusDexterity - e.getValue());
                case VIT -> ps.bonusVitality = Math.max(0, ps.bonusVitality - e.getValue());
                case WIL -> ps.bonusWill = Math.max(0, ps.bonusWill - e.getValue());
                case TEC -> ps.bonusTechnique = Math.max(0, ps.bonusTechnique - e.getValue());
            }
        }
        StatsManager.getInstance().recalcDerivedStats(player);
    }
}

package me.nakilex.levelplugin.player.classes.essence;

import me.nakilex.levelplugin.items.data.ItemRarity;
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
}

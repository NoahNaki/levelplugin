package me.nakilex.levelplugin.player.classes.essence;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.TextUtil;
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
     * Generate a random essence of any rarity.
     */
    public static ItemStack generateRandomEssence() {
        ItemRarity[] rarities = ItemRarity.values();
        return generateRandomEssence(rarities[new Random().nextInt(rarities.length)]);
    }

    /**
     * Generate a random essence with the specified rarity.
     */
    public static ItemStack generateRandomEssence(ItemRarity rarity) {
        Random rand = new Random();
        PlayerClass[] classes = PlayerClass.values();
        PlayerClass clazz = classes[rand.nextInt(classes.length)];
        int slots = getAttributeSlots(rarity);
        Map<StatType, AttrData> attrs = rollAttributes(slots, rarity, 0, rand, java.util.Collections.emptySet());
        return create(clazz, rarity, 0, attrs, true);
    }

    /**
     * Generate an essence for a specific class, rarity and star level.
     */
    public static ItemStack generateEssence(PlayerClass clazz, ItemRarity rarity, int starLevel) {
        int slots = getAttributeSlots(rarity);
        Map<StatType, AttrData> attrs = rollAttributes(slots, rarity, starLevel, new Random(), java.util.Collections.emptySet());
        return create(clazz, rarity, starLevel, attrs, true);
    }

    /**
     * Create an essence item with the provided parameters.
     */
    public static ItemStack create(PlayerClass clazz, ItemRarity rarity, int starLevel,
                                   Map<StatType, AttrData> attributes, boolean soulbound) {
        ItemStack stack = new ItemStack(Material.BOOK);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        int exp = 0;
        int next = getRarityThreshold(rarity);

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
        updateLore(stack);
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

    private static Map<StatType, AttrData> rollAttributes(int slots, ItemRarity rarity, int starLevel, Random rand, Collection<StatType> exclude) {
        List<StatType> stats = new ArrayList<>(Arrays.asList(StatType.values()));
        if (exclude != null) stats.removeAll(exclude);
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

    /** Add equip/unequip instructions to an essence's lore. */
    public static void addSlotTips(ItemStack stack) {
        if (!isEssence(stack)) return;
        updateLore(stack);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        if (lore.isEmpty() || !lore.get(lore.size() - 1).isEmpty()) {
            lore.add("");
        }
        GuiUtil.addClickInstructions(lore, "to equip", "to unequip");
        meta.setLore(lore);
        stack.setItemMeta(meta);
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

    /** Retrieve the stat types present on an essence. */
    public static java.util.Set<StatType> getStatTypes(ItemStack stack) {
        return getAttributes(stack).keySet();
    }

    /** Calculate the gear score for an essence. */
    public static int getGearScore(ItemStack stack) {
        return getAttributes(stack).values().stream().mapToInt(a -> a.value).sum();
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

    /** Retrieve the current star level of an essence. */
    public static int getStar(ItemStack stack) {
        if (!isEssence(stack)) return 0;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 0;
        Integer val = meta.getPersistentDataContainer().get(STAR_KEY, PersistentDataType.INTEGER);
        return val == null ? 0 : val;
    }

    /** Increase star level of an essence, rerolling attributes accordingly. */
    public static void upgradeStar(ItemStack stack) {
        if (!isEssence(stack)) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int star = pdc.has(STAR_KEY, PersistentDataType.INTEGER) ? pdc.get(STAR_KEY, PersistentDataType.INTEGER) : 0;
        if (star >= 5) return;
        star++;
        ItemRarity rarity = ItemRarity.valueOf(pdc.get(RARITY_KEY, PersistentDataType.STRING));
        int slots = getAttributeSlots(rarity);
        Map<StatType, AttrData> newAttrs = rollAttributes(slots, rarity, star, new Random(), java.util.Collections.emptySet());
        String attrString = newAttrs.entrySet().stream()
                .map(e -> e.getKey().name() + ":" + e.getValue().value + ":" + (e.getValue().percent ? 1 : 0))
                .collect(Collectors.joining(";"));
        pdc.set(ATTR_KEY, PersistentDataType.STRING, attrString);
        pdc.set(STAR_KEY, PersistentDataType.INTEGER, star);
        stack.setItemMeta(meta);
        updateLore(stack);
    }

    public static ItemRarity addExp(ItemStack stack, int amount) {
        if (!isEssence(stack) || amount <= 0) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int exp = pdc.has(EXP_KEY, PersistentDataType.INTEGER) ? pdc.get(EXP_KEY, PersistentDataType.INTEGER) : 0;
        exp += amount;
        int next = pdc.has(NEXT_EXP_KEY, PersistentDataType.INTEGER) ? pdc.get(NEXT_EXP_KEY, PersistentDataType.INTEGER) : 0;
        ItemRarity rarity = ItemRarity.valueOf(pdc.get(RARITY_KEY, PersistentDataType.STRING));
        int star = pdc.has(STAR_KEY, PersistentDataType.INTEGER) ? pdc.get(STAR_KEY, PersistentDataType.INTEGER) : 0;
        ItemRarity upgradedTo = null;
        Map<StatType, AttrData> attrs = getAttributes(stack);

        while (next > 0 && exp >= next && rarity != ItemRarity.FABLED) {
            exp -= next;
            rarity = nextRarity(rarity);
            upgradedTo = rarity;
            int slots = getAttributeSlots(rarity);
            if (attrs.size() < slots) {
                Map<StatType, AttrData> extra = rollAttributes(slots - attrs.size(), rarity, star, new Random(), attrs.keySet());
                attrs.putAll(extra);
            }
            next = getRarityThreshold(rarity);
        }

        if (next == 0) {
            exp = 0;
        }

        String attrString = attrs.entrySet().stream()
                .map(e -> e.getKey().name() + ":" + e.getValue().value + ":" + (e.getValue().percent ? 1 : 0))
                .collect(Collectors.joining(";"));
        pdc.set(ATTR_KEY, PersistentDataType.STRING, attrString);
        pdc.set(RARITY_KEY, PersistentDataType.STRING, rarity.name());
        pdc.set(EXP_KEY, PersistentDataType.INTEGER, exp);
        pdc.set(NEXT_EXP_KEY, PersistentDataType.INTEGER, next);
        stack.setItemMeta(meta);
        updateLore(stack);
        return upgradedTo;
    }

    public static int getInvestExp(ItemRarity rarity) {
        return 50 * (rarity.ordinal() + 1);
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

        String className = TextUtil.beautifyWords(pdc.get(CLASS_KEY, PersistentDataType.STRING));
        String stars = GuiUtil.glyphStars(star);
        meta.setDisplayName(rarity.getColor() + className + " Essence " + stars);

        List<String> lore = new ArrayList<>();
        String rarityGlyph = "<glyph:" + rarity.name().toLowerCase() + ">";
        lore.add(rarityGlyph + "<glyph:essence>");
        lore.add("");
        int gearScore = getGearScore(stack);
        lore.add("<glyph:sword_icon> " + ChatColor.GRAY + "Gear Score: "
                + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + gearScore);
        lore.add("");
        for (Map.Entry<StatType, AttrData> e : attrs.entrySet()) {
            lore.add(GuiUtil.formatStatLine(e.getKey(), e.getValue().value, e.getValue().percent));
        }
        lore.add("");
        String bar = TooltipUtil.progressBar(exp, next, 15);
        String expColor = ChatFormatter.experienceColor();
        String expLabel = ChatFormatter.experienceLabel();
        lore.add(bar + " " + expColor + exp + ChatColor.GOLD + "/" + expColor + next + " <glyph:experience_orb_icon> " + expLabel);
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }
}

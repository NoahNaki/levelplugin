package me.nakilex.levelplugin.player.classes.essence;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.TooltipUtil;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
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

    private static final List<PlayerClass> CORE_ESSENCE_CLASSES = java.util.List.of(
            PlayerClass.ARCHER,
            PlayerClass.AWAKARCHER,
            PlayerClass.MAGE,
            PlayerClass.AWAKMAGE,
            PlayerClass.WARRIOR,
            PlayerClass.AWAKWARRIOR,
            PlayerClass.ROGUE,
            PlayerClass.AWAKROGUE
    );

    private record AttrData(int value, boolean percent) {}

    private static final java.util.Map<PlayerClass, String> CLASS_NEXO_IDS = java.util.Map.of(
            PlayerClass.MAGE, "riptide",
            PlayerClass.WARRIOR, "sharpness",
            //PlayerClass.CLERIC, "smite",
            PlayerClass.ROGUE, "protection",
            PlayerClass.ARCHER, "projectile_protection",
            PlayerClass.AWAKMAGE, "channeling",
            PlayerClass.AWAKROGUE, "unbreaking",
            PlayerClass.AWAKWARRIOR, "sweeping_edge",
            PlayerClass.AWAKARCHER, "piercing"
            //PlayerClass.AWAKCLERIC, "mending"
    );

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
        return create(clazz, rarity, 0, attrs, false);
    }

    /**
     * Generate an essence biased toward common rarity.
     * 89% chance for common, 10% for uncommon and 1% for rare.
     */
    public static ItemStack generateEssence() {
        Random rand = new Random();
        ItemRarity rarity = rollWeightedRarity(rand);
        PlayerClass[] classes = PlayerClass.values();
        PlayerClass clazz = classes[rand.nextInt(classes.length)];
        return generateEssence(clazz, rarity, 0);
    }

    /** Generate an essence for a specific class with weighted rarity. */
    public static ItemStack generateEssence(PlayerClass clazz) {
        Random rand = new Random();
        ItemRarity rarity = rollWeightedRarity(rand);
        return generateEssence(clazz, rarity, 0);
    }

    /** Generate an essence from the shared combat pool using weighted rarity. */
    public static ItemStack generateStandardPoolEssence() {
        return generateStandardPoolEssence(rollWeightedRarity(new Random()), 0);
    }

    /** Generate an essence from the shared combat pool using explicit rarity and star level. */
    public static ItemStack generateStandardPoolEssence(ItemRarity rarity, int starLevel) {
        return generateEssenceFromPool(CORE_ESSENCE_CLASSES, rarity, starLevel);
    }

    /** Generate an essence from a provided pool using weighted rarity. */
    public static ItemStack generateEssenceFromPool(Collection<PlayerClass> pool) {
        return generateEssenceFromPool(pool, rollWeightedRarity(new Random()), 0);
    }

    /** Generate an essence from a provided pool with explicit rarity and star level. */
    public static ItemStack generateEssenceFromPool(Collection<PlayerClass> pool, ItemRarity rarity, int starLevel) {
        if (pool == null || pool.isEmpty()) {
            return generateEssence();
        }
        Random rand = new Random();
        PlayerClass clazz = pool.stream()
                .skip(rand.nextInt(pool.size()))
                .findFirst()
                .orElse(PlayerClass.MAGE);
        return generateEssence(clazz, rarity, starLevel);
    }

    /** Pool of essences used by combat rewards such as bosses and dungeons. */
    public static List<PlayerClass> getCoreEssencePool() {
        return CORE_ESSENCE_CLASSES;
    }

    /**
     * Generate an essence for a specific class, rarity and star level.
     */
    public static ItemStack generateEssence(PlayerClass clazz, ItemRarity rarity, int starLevel) {
        int slots = getAttributeSlots(rarity);
        Map<StatType, AttrData> attrs = rollAttributes(slots, rarity, starLevel, new Random(), java.util.Collections.emptySet());
        return create(clazz, rarity, starLevel, attrs, false);
    }

    /** Roll a rarity with 89% common, 10% uncommon and 1% rare. */
    private static ItemRarity rollWeightedRarity(Random rand) {
        double roll = rand.nextDouble();
        if (roll < 0.01) {
            return ItemRarity.RARE;
        }
        if (roll < 0.11) {
            return ItemRarity.UNCOMMON;
        }
        return ItemRarity.COMMON;
    }

    /**
     * Create an essence item with the provided parameters.
     */
    public static ItemStack create(PlayerClass clazz, ItemRarity rarity, int starLevel,
                                   Map<StatType, AttrData> attributes, boolean soulbound) {
        ItemStack stack = buildBaseItem(clazz);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        // Remove any existing enchants so new essences are not glinting by default
        for (Enchantment ench : new java.util.ArrayList<>(meta.getEnchants().keySet())) {
            meta.removeEnchant(ench);
        }
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
     * Build the base ItemStack for an essence using class-specific Nexo items
     * when available. Falls back to a standard book if no mapping exists.
     */
    private static ItemStack buildBaseItem(PlayerClass clazz) {
        String id = CLASS_NEXO_IDS.get(clazz);
        if (id != null) {
            ItemBuilder builder = NexoItems.itemFromId(id);
            if (builder != null) {
                return builder.build();
            }
        }
        return new ItemStack(Material.BOOK);
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

    private static final int[] RARITY_THRESHOLDS = {
            50,   // COMMON
            100,  // UNCOMMON
            200,  // RARE
            400,  // EPIC
            1000, // LEGENDARY
            0,    // MYTHIC (cannot upgrade further via exp)
            0     // FABLED (max)
    };

    private static final double[] RARITY_ATTRIBUTE_SCALE = {
            1.00, // COMMON
            1.20, // UNCOMMON
            1.45, // RARE
            1.85, // EPIC
            2.40, // LEGENDARY
            3.10, // MYTHIC
            3.90  // FABLED
    };

    private static int getRarityThreshold(ItemRarity rarity) {
        return RARITY_THRESHOLDS[rarity.ordinal()];
    }

    private static double getRarityScale(ItemRarity rarity) {
        return RARITY_ATTRIBUTE_SCALE[rarity.ordinal()];
    }

    private static Map<StatType, AttrData> rollAttributes(int slots, ItemRarity rarity, int starLevel, Random rand, Collection<StatType> exclude) {
        List<StatType> stats = new ArrayList<>(Arrays.asList(StatType.values()));
        if (exclude != null) stats.removeAll(exclude);
        Collections.shuffle(stats, rand);
        Map<StatType, AttrData> map = new LinkedHashMap<>();
        int percentSlots = Math.min(starLevel, slots);
        for (int i = 0; i < slots && i < stats.size(); i++) {
            StatType st = stats.get(i);
            int value = rollStatValue(rarity, starLevel, rand);
            boolean percent = i < percentSlots;
            map.put(st, new AttrData(value, percent));
        }
        return map;
    }

    /** Roll a balanced stat value based on rarity and star level. */
    private static int rollStatValue(ItemRarity rarity, int starLevel, Random rand) {
        int ord = rarity.ordinal();
        int min = 1 + ord;
        int max = min + 2 + ord; // growing range per rarity
        int base = rand.nextInt(max - min + 1) + min;
        double starMult = 1.0 + (starLevel * 0.05);
        return (int) Math.round(base * starMult);
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
        return PlayerClass.fromString(clazz);
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
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        if (equipped) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        } else {
            meta.removeEnchant(Enchantment.UNBREAKING);
        }
        stack.setItemMeta(meta);
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
        lore.addAll(TooltipUtil.clickInstructions("to equip", "to unequip"));
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

    /** Mark or unmark an essence as soulbound. */
    public static void setSoulbound(ItemStack stack, boolean soulbound) {
        if (!isEssence(stack)) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(SOULBOUND_KEY, PersistentDataType.BYTE, soulbound ? (byte)1 : (byte)0);
        stack.setItemMeta(meta);
        updateLore(stack);
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
        Map<StatType, AttrData> attrs = new LinkedHashMap<>(getAttributes(stack));

        while (next > 0 && exp >= next && rarity != ItemRarity.FABLED) {
            exp -= next;
            ItemRarity previous = rarity;
            rarity = nextRarity(rarity);
            upgradedTo = rarity;
            double scale = getRarityScale(rarity) / Math.max(1e-9, getRarityScale(previous));
            if (scale > 1.0) {
                Map<StatType, AttrData> scaled = new LinkedHashMap<>();
                for (Map.Entry<StatType, AttrData> entry : attrs.entrySet()) {
                    AttrData data = entry.getValue();
                    int scaledValue = (int) Math.max(1, Math.round(data.value * scale));
                    scaled.put(entry.getKey(), new AttrData(scaledValue, data.percent));
                }
                attrs = scaled;
            }
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

    /**
     * Refresh the tooltip border on an essence based on its stored rarity.
     * This can be invoked after external rarity changes to keep the border
     * in sync without rebuilding the entire lore.
     */
    public static void updateTooltipStyle(ItemStack stack) {
        if (!isEssence(stack)) return;
        ItemRarity rarity = getRarity(stack);
        if (rarity != null) {
            ItemUtil.applyRarityTooltipStyle(stack, rarity);
        }
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

        PlayerClass pc = PlayerClass.valueOf(pdc.get(CLASS_KEY, PersistentDataType.STRING));
        String className = pc.getDisplayName();
        String stars = GuiUtil.glyphStars(star);
        meta.setDisplayName(rarity.getColor() + className + " Essence " + stars);

        List<String> lore = new ArrayList<>();
        String rarityGlyph = "<glyph:" + rarity.name().toLowerCase() + ">";
        lore.add(rarityGlyph + "<glyph:essence>");
        lore.add("");
        int gearScore = getGearScore(stack);
        lore.add(ChatColor.GRAY + "Gear Score: "
                + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + gearScore);
        lore.add("");
        for (StatType type : StatType.DISPLAY_ORDER) {
            AttrData data = attrs.get(type);
            if (data != null) {
                lore.add(GuiUtil.formatStatLine(type, data.value, data.percent));
            }
        }
        lore.add("");
        String bar = TooltipUtil.progressBar(exp, next, 15);
        String expColor = ChatFormatter.experienceColor();
        String expLabel = ChatFormatter.experienceLabel();
        lore.add(bar + " " + expColor + exp + ChatColor.GOLD + "/" + expColor + next + " <glyph:experience_orb_icon> " + expLabel);
        if (isSoulbound(stack)) {
            lore.add(ChatColor.RED + "Soulbound");
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        updateTooltipStyle(stack);
    }
}

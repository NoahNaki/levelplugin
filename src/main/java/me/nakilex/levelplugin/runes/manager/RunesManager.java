package me.nakilex.levelplugin.runes.manager;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.runes.loader.RuneLoader;
import me.nakilex.levelplugin.runes.model.Rune;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.ChatColor;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Manages players' equipped runes and provides rune-based modifiers for spells.
 */
public class RunesManager {
    private final RuneLoader runeLoader;
    // Map of player -> (spellId -> list of runes targeting that spell)
    private final Map<UUID, Map<String, List<Rune>>> equippedRunes = new ConcurrentHashMap<>();

    private final NamespacedKey runeKey;
    private final NamespacedKey uncarvedKey;

    public RunesManager(Plugin plugin) {
        this.runeLoader = new RuneLoader(plugin);
        this.runeLoader.loadRunes();

        this.runeKey       = new NamespacedKey(plugin, "rune_id");
        this.uncarvedKey   = new NamespacedKey(plugin, "rune_uncarved");
    }

    public void savePlayerData(Player player) {
        List<String> ids = getEquippedRuneIds(player);
        Main.getInstance()
            .getPlayerConfig()
            .setEquippedRunes(player.getUniqueId(), ids);
    }

    /**
     * Initialize a player's equipped runes from stored rune IDs (e.g., from PlayerData).
     */
    public void loadPlayerRunes(UUID playerId, List<String> storedRuneIds) {
        Map<String, List<Rune>> map = new HashMap<>();
        for (String id : storedRuneIds) {
            Rune rune = runeLoader.getRune(id);
            if (rune == null) continue;
            map.computeIfAbsent(rune.getTargetSpell(), k -> new ArrayList<>())
                .add(rune);
        }
        equippedRunes.put(playerId, map);
    }

    // in RunesManager.java
    public ItemStack createUncarvedRuneItem(Rune rune) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Unidentified Rune");
        meta.setLore(List.of(
            ChatColor.DARK_GRAY + "Take this to a Runecarver",
            ChatColor.DARK_GRAY + "to reveal its hidden powers!"
        ));
        // stash the real rune id under the same key you're using in IdentifyRunesGUI
        meta.getPersistentDataContainer()
            .set(new NamespacedKey(Main.getInstance(), "rune_id"),
                PersistentDataType.STRING,
                rune.getId());
        paper.setItemMeta(meta);
        return paper;
    }

    private static final Material[] RUNE_TRIM_MATERIALS = {
        Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE
    };

    private Material chooseRuneMaterial(Rune rune) {
        int idx = Math.abs(rune.getId().hashCode()) % RUNE_TRIM_MATERIALS.length;
        return RUNE_TRIM_MATERIALS[idx];
    }

    /** Create an identified rune item with name, spell, description and rarity. */
    public ItemStack createIdentifiedRuneItem(Rune rune) {
        ItemStack item = new ItemStack(chooseRuneMaterial(rune));
        ItemMeta meta = item.getItemMeta();

        meta.addItemFlags(
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_UNBREAKABLE,
            ItemFlag.HIDE_ARMOR_TRIM,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP
        );

        ChatColor color = rune.getRarity().getColor();
        meta.setDisplayName(color + rune.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Spell: " + rune.getTargetSpell());
        for (String line : rune.getDescription()) {
            lore.add(ChatColor.GRAY + line);
        }
        lore.add("");
        lore.add(color.toString() + ChatColor.BOLD + rune.getRarity().name());

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(runeKey, PersistentDataType.STRING, rune.getId());
        item.setItemMeta(meta);
        return item;
    }


    /**
     * Returns the list of all runes currently equipped by the player.
     */
    public List<Rune> getEquippedRunes(Player player) {
        Map<String, List<Rune>> map = equippedRunes.get(player.getUniqueId());
        if (map == null) return Collections.emptyList();
        List<Rune> all = new ArrayList<>();
        for (List<Rune> list : map.values()) all.addAll(list);
        return Collections.unmodifiableList(all);
    }

    public Collection<Rune> getAllRunes() {
        return Collections.unmodifiableCollection(runeLoader.getAllRunes());
    }

    public NamespacedKey getRuneKey() {
        return runeKey;
    }

    /**
     * Pick a random rune that matches the given rarity.
     * Returns {@code null} if no runes of that rarity exist.
     */
    public Rune getRandomRuneByRarity(Rune.Rarity rarity) {
        List<Rune> filtered = runeLoader.getAllRunes().stream()
            .filter(r -> r.getRarity() == rarity)
            .toList();
        if (filtered.isEmpty()) return null;
        int idx = ThreadLocalRandom.current().nextInt(filtered.size());
        return filtered.get(idx);
    }

    /**
     * Returns the subset of equipped runes that target the given spell ID.
     */
    public List<Rune> getRunesForSpell(Player player, String spellId) {
        Map<String, List<Rune>> map = equippedRunes.get(player.getUniqueId());
        if (map == null) return Collections.emptyList();
        return map.getOrDefault(spellId, Collections.emptyList());
    }

    /**
     * Equip a rune for the player (adds to their equipped list).
     * Returns true if added, false if it was rejected (e.g., duplicate unique rune).
     */
    public boolean equipRune(Player player, Rune rune) {
        Main.getPlugin().getLogger().info("[RunesManager] equipRune: player="
            + player.getName() + " trying to equip rune=" + rune.getId());
        UUID uid = player.getUniqueId();
        equippedRunes.putIfAbsent(uid, new HashMap<>());
        Map<String, List<Rune>> map = equippedRunes.get(uid);
        List<Rune> runes = map.computeIfAbsent(rune.getTargetSpell(), k -> new ArrayList<>());

        Main.getPlugin().getLogger().info("currently equipped: "
            + runes.stream().map(Rune::getId).toList());

        // Prevent stacking of unique runes of the same ID
        if (rune.isUnique() && runes.stream().anyMatch(r -> r.getId().equals(rune.getId()))) {
            Main.getPlugin().getLogger().warning("❌ reject duplicate unique rune");
            return false;
        }

        runes.add(rune);
        savePlayerData(player);
        Main.getPlugin().getLogger().info("✅ equipped! now: "
            + getEquippedRuneIds(player));
        return true;
    }

    public boolean equipRune(Player player, ItemStack stack) {
        if (!isIdentified(stack)) return false;
        String id = stack.getItemMeta()
            .getPersistentDataContainer()
            .get(runeKey, PersistentDataType.STRING);
        Rune rune = runeLoader.getRune(id);
        if (rune == null) return false;

        boolean success = equipRune(player, rune);
        if (!success) return false;

        ItemStack one = stack.clone();
        one.setAmount(1);
        player.getInventory().removeItem(one);
        return true;
    }

    public boolean isIdentified(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        var pdc = stack.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(runeKey, PersistentDataType.STRING)) return false;
        String id = pdc.get(runeKey, PersistentDataType.STRING);
        return runeLoader.getRune(id) != null;
    }

    public boolean unequipRune(Player player, Rune target) {
        UUID uid = player.getUniqueId();
        Map<String, List<Rune>> map = equippedRunes.get(uid);
        if (map == null) return false;

        List<Rune> list = map.get(target.getTargetSpell());
        if (list == null) return false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(target.getId())) {
                list.remove(i);
                savePlayerData(player);
                return true;
            }
        }
        return false;
    }

    public List<String> getEquippedRuneIds(Player player) {
        return getEquippedRunes(player).stream()
            .map(Rune::getId)
            .collect(Collectors.toList());
    }

    public Rune getRuneById(String id) {
        return runeLoader.getRune(id);
    }
}
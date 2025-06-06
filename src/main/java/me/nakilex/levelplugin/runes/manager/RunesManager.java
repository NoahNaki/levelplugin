package me.nakilex.levelplugin.runes.manager;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.runes.loader.RuneLoader;
import me.nakilex.levelplugin.runes.model.Rune;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
    private final Map<UUID, List<Rune>> equippedRunes = new ConcurrentHashMap<>();

    private final NamespacedKey runeKey;
    private final NamespacedKey uncarvedKey;

    public RunesManager(Plugin plugin) {
        this.runeLoader = new RuneLoader(plugin);
        this.runeLoader.loadRunes();

        this.runeKey       = new NamespacedKey(plugin, "rune_id");
        this.uncarvedKey   = new NamespacedKey(plugin, "rune_uncarved");
    }

    public void savePlayerData(Player player) {
        // example: write to config under players.<uuid>.runes = List<String>
        List<String> ids = getEquippedRuneIds(player);
        Main.getPlugin().getConfig()
            .set("players." + player.getUniqueId() + ".runes", ids);
        Main.getPlugin().saveConfig();
    }

    /**
     * Initialize a player's equipped runes from stored rune IDs (e.g., from PlayerData).
     */
    public void loadPlayerRunes(UUID playerId, List<String> storedRuneIds) {
        List<Rune> runes = new ArrayList<>();
        for (String id : storedRuneIds) {
            Rune rune = runeLoader.getRune(id);
            if (rune != null) {
                runes.add(rune);
            }
        }
        equippedRunes.put(playerId, runes);
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


    /**
     * Returns the list of all runes currently equipped by the player.
     */
    public List<Rune> getEquippedRunes(Player player) {
        return Collections.unmodifiableList(
            equippedRunes.getOrDefault(player.getUniqueId(), Collections.emptyList())
        );
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
        return getEquippedRunes(player).stream()
            .filter(r -> r.getTargetSpell().equalsIgnoreCase(spellId))
            .collect(Collectors.toList());
    }

    /**
     * Equip a rune for the player (adds to their equipped list).
     * Returns true if added, false if it was rejected (e.g., duplicate unique rune).
     */
    public boolean equipRune(Player player, Rune rune) {
        Main.getPlugin().getLogger().info("[RunesManager] equipRune: player="
            + player.getName() + " trying to equip rune=" + rune.getId());
        UUID uid = player.getUniqueId();
        equippedRunes.putIfAbsent(uid, new ArrayList<>());
        List<Rune> runes = equippedRunes.get(uid);

        Main.getPlugin().getLogger().info("currently equipped: "
            + runes.stream().map(Rune::getId).toList());

        // Prevent stacking of unique runes of the same ID
        if (rune.isUnique() && runes.stream().anyMatch(r -> r.getId().equals(rune.getId()))) {
            Main.getPlugin().getLogger().warning("❌ reject duplicate unique rune");
            return false;
        }

        runes.add(rune);
        Main.getPlugin().getLogger().info("✅ equipped! now: "
            + runes.stream().map(Rune::getId).toList());
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
        List<Rune> runes = equippedRunes.get(uid);
        if (runes == null) return false;

        // find & remove first match
        for (int i = 0; i < runes.size(); i++) {
            if (runes.get(i).getId().equals(target.getId())) {
                runes.remove(i);
                savePlayerData(player);    // persist the change!
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
package me.nakilex.levelplugin.runes.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.runes.loader.RuneLoader;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.runes.model.Rune;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for identifying Unidentified Runes via a chest inventory.
 * Players insert unidentified rune items into slots 0-6 and click the Identify button at slot 8.
 */
public class IdentifyRunesGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_PURPLE + "Runecarver - Identify Runes";
    private static final int SIZE = 9;
    private static final int IDENTIFY_SLOT = 8;

    private final Main plugin;
    private final RuneLoader runeLoader;
    private final NamespacedKey runeKey;

    public IdentifyRunesGUI(Main plugin, RunesManager runesManager) {
        this.plugin    = plugin;
        this.runeLoader = new RuneLoader(plugin);
        this.runeKey    = new NamespacedKey(plugin, "rune_id");

        this.runeLoader.loadRunes();

        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** Opens the Identify GUI for the player. */
    public void openInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        // Place Identify button
        ItemStack button = new ItemStack(org.bukkit.Material.ANVIL);
        ItemMeta bm = button.getItemMeta();
        bm.setDisplayName(ChatColor.GREEN + "Identify Runes");
        button.setItemMeta(bm);
        inv.setItem(IDENTIFY_SLOT, button);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;

        Player player     = (Player)e.getWhoClicked();
        Inventory top     = e.getView().getTopInventory();
        int rawSlot       = e.getRawSlot();
        Inventory clicked = e.getClickedInventory();
        InventoryAction action = e.getAction();

        // 1) BLOCK non-runes from going into slots 0..IDENTIFY_SLOT-1
        if (clicked == top && rawSlot < IDENTIFY_SLOT) {
            // allow only the identify-button pickup itself
            if (!(rawSlot == IDENTIFY_SLOT && action == InventoryAction.PICKUP_ALL)) {
                ItemStack toPlace = e.getCursor();             // what you're trying to put in
                if (toPlace == null
                    || !toPlace.hasItemMeta()
                    || !toPlace.getItemMeta()
                    .getPersistentDataContainer()
                    .has(runeKey, PersistentDataType.STRING)
                ) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        // 2) Identify button click
        if (rawSlot == IDENTIFY_SLOT && clicked == top && action == InventoryAction.PICKUP_ALL) {
            e.setCancelled(true); // don’t let them pick up the anvil button
            List<String> identified = new ArrayList<>();

            // --- debug loop omitted for brevity ---

            // now actually identify
            for (int i = 0; i < IDENTIFY_SLOT; i++) {
                ItemStack in = top.getItem(i);
                if (in == null || !in.hasItemMeta()) continue;

                var pdc = in.getItemMeta().getPersistentDataContainer();
                if (!pdc.has(runeKey, PersistentDataType.STRING)) continue;

                String id = pdc.get(runeKey, PersistentDataType.STRING);
                Rune rune = runeLoader.getRune(id);
                if (rune == null) continue;

                top.setItem(i, null);
                player.getInventory().addItem(createIdentifiedRuneItem(rune));
                identified.add(id);
            }

            if (identified.isEmpty()) {
                player.sendMessage("§cYou have no unidentified runes to identify.");
            } else {
                player.sendMessage("§aIdentified " + identified.size() + " rune(s): " + identified);
                player.closeInventory();
            }
        }
        // everything else (including placing/removing runes in 0..6) is allowed
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        Inventory top = e.getView().getTopInventory();

        // check any of the dragged‐into slots
        for (int raw : e.getRawSlots()) {
            if (raw < IDENTIFY_SLOT && raw >= 0) {
                ItemStack cursor = e.getOldCursor();  // what’s being dragged
                if (cursor == null
                    || !cursor.hasItemMeta()
                    || !cursor.getItemMeta()
                    .getPersistentDataContainer()
                    .has(runeKey, PersistentDataType.STRING)
                ) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * Builds the identified rune item from a Rune.
     * Adds the rune-id to PDC for later use.
     */
    private ItemStack createIdentifiedRuneItem(Rune rune) {
        ItemStack item = new ItemStack(org.bukkit.Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(rune.getRarity().name() + " Rune: " + rune.getDisplayName());
        List<String> lore = new ArrayList<>();
        lore.add("Affects: " + rune.getTargetClass() + " - " + rune.getTargetSpell());
        for (var e : rune.getEffects()) {
            switch (e.getType()) {
                case MODIFIER:
                    lore.add(ChatColor.GRAY + "+" + e.getBonusDamagePercent() + "% Damage");
                    break;
                case TRANSFORM:
                    lore.add(ChatColor.GRAY + "Transforms to: " + e.getNewEffectKey());
                    break;
            }
        }
        meta.setLore(lore);
        // Store rune ID in PDC
        meta.getPersistentDataContainer().set(runeKey, PersistentDataType.STRING, rune.getId());
        item.setItemMeta(meta);
        return item;
    }
}
package me.nakilex.levelplugin.runes.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.runes.loader.RuneLoader;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.runes.model.Rune;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages the chest‐style GUI for equipping/unequipping identified runes, with debug logs.
 */
public class RuneInventoryGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_PURPLE + "Rune Inventory";
    private static final int SIZE = 27;

    private final RunesManager runesManager;
    private final RuneLoader runeLoader;

    public RuneInventoryGUI(Main plugin, RunesManager runesManager) {
        this.runesManager = runesManager;
        this.runeLoader   = new RuneLoader(plugin);
        this.runeLoader.loadRunes();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        List<Rune> equipped = runesManager.getEquippedRunes(player);
        for (int i = 0; i < equipped.size() && i < SIZE; i++) {
            inv.setItem(i, createRuneItem(equipped.get(i)));
        }
        player.openInventory(inv);
    }

    private ItemStack createRuneItem(Rune rune) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(rune.getRarity().name() + " Rune: " + rune.getDisplayName());
        List<String> lore = rune.getEffects().stream()
            .map(e -> {
                switch (e.getType()) {
                    case MODIFIER:
                        return ChatColor.GRAY + "+" + e.getBonusDamagePercent() + "% Damage";
                    case TRANSFORM:
                        return ChatColor.GRAY + "Transforms to: " + e.getNewEffectKey();
                    default:
                        return "";
                }
            })
            .collect(Collectors.toList());
        lore.add(0, "Affects: " + rune.getTargetClass() + " - " + rune.getTargetSpell());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Attempts to map an ItemStack back to a Rune by matching its displayName
     */
    private Rune itemToRune(ItemStack item) {
        Main.getPlugin().getLogger().info("[RuneGUI] itemToRune raw item=" + (item==null?"null":item.getType()));
        if (item == null || !item.hasItemMeta()) return null;

        String rawName = item.getItemMeta().getDisplayName();
        Main.getPlugin().getLogger().info("[RuneGUI]   displayName='" + rawName + "'");
        String stripped = ChatColor.stripColor(rawName);
        Main.getPlugin().getLogger().info("[RuneGUI]   stripped='" + stripped + "'");

        if (!stripped.contains(" Rune: ")) {
            Main.getPlugin().getLogger().warning("[RuneGUI]    ❌ name doesn’t contain ‘ Rune: ’ → not a rune");
            return null;
        }

        String displayName = stripped.substring(stripped.indexOf(" Rune: ") + 7);
        Main.getPlugin().getLogger().info("[RuneGUI]   extracted displayName='" + displayName + "'");

        // reverse-lookup by lore of loaded runes
        Optional<Rune> found = runeLoader.getAllRunes().stream()
            .peek(r -> Main.getPlugin().getLogger().info("[RuneGUI]     comparing to rune.displayName=" + r.getDisplayName()))
            .filter(r -> r.getDisplayName().equals(displayName))
            .findFirst();

        Rune r = found.orElse(null);
        Main.getPlugin().getLogger().info("[RuneGUI]   reverse lookup returned " + r);
        return r;
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;

        Player player = (Player) e.getWhoClicked();
        int slot      = e.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        Inventory clickedInv = e.getClickedInventory();
        Inventory topInv     = e.getView().getTopInventory();
        boolean inTop        = clickedInv == topInv;
        InventoryAction action = e.getAction();
        ItemStack cursor   = e.getCursor();
        ItemStack clicked  = e.getCurrentItem();

        Main.getPlugin().getLogger().info("[RuneGUI] click slot=" + slot + ", action=" + action + ", inTop=" + inTop);
        Main.getPlugin().getLogger().info("[RuneGUI] cursor=" + (cursor == null ? "null" : cursor.getType()) + ", clicked=" + (clicked == null ? "null" : clicked.getType()));

        // EQUIP via shift-click from player inv
        if (e.isShiftClick() && action == InventoryAction.MOVE_TO_OTHER_INVENTORY && !inTop) {
            Rune rune = itemToRune(clicked);
            if (rune == null) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "That is not an identified rune!");
                return;
            }
            if (!runesManager.equipRune(player, rune)) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Cannot equip duplicate unique rune.");
                return;
            }
            e.setCancelled(true);
            topInv.setItem(slot, createRuneItem(rune));
            clicked.setAmount(clicked.getAmount() - 1);
            return;
        }

        // PLACE via cursor
        if (inTop && action.name().startsWith("PLACE")) {
            Rune rune = itemToRune(cursor);
            if (rune == null) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "That is not an identified rune!");
                return;
            }
            if (!runesManager.equipRune(player, rune)) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Cannot equip duplicate unique rune.");
                return;
            }
            e.setCancelled(true);
            topInv.setItem(slot, createRuneItem(rune));
            cursor.setAmount(cursor.getAmount() - 1);
            return;
        }

        // PICKUP from GUI
        if (inTop && action.name().startsWith("PICKUP")) {
            Rune rune = itemToRune(clicked);
            Main.getPlugin().getLogger().info("[RuneGUI] unequip clicked item to rune=" + rune);
            if (rune != null) {
                runesManager.unequipRune(player, rune);
                topInv.setItem(slot, null);
            }
            return;
        }

        // all other clicks
        if (inTop) e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        // nothing special
    }
}


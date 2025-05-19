package me.nakilex.levelplugin.runes.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.runes.model.Rune;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class EquipRunesGUI implements Listener {
    private final Main plugin;
    private final RunesManager runesManager;
    private final IdentifyRunesGUI identifyGui;

    public static final String TITLE = ChatColor.DARK_GRAY + "Equip Runes";
    private static final int SIZE = 9;

    public EquipRunesGUI(Main plugin, RunesManager runesManager, IdentifyRunesGUI identifyGui) {
        this.plugin = plugin;
        this.runesManager = runesManager;
        if (identifyGui == null) {
            this.identifyGui = new IdentifyRunesGUI(plugin, runesManager);
        } else {
            this.identifyGui = identifyGui;
        }
    }

    public Inventory createInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        var equipped = runesManager.getEquippedRunes(player);
        for (int i = 0; i < equipped.size() && i < SIZE; i++) {
            Rune rune = equipped.get(i);
            inv.setItem(i, identifyGui.createIdentifiedRuneItem(rune));
        }
        return inv;
    }

    public void open(Player player) {
        plugin.getLogger().info("Opening EquipRunesGUI for " + player.getName());
        player.openInventory(createInventory(player));
    }

    private boolean isIdentifiedRune(ItemStack stack) {
        return stack != null
            && stack.getType() == Material.ENCHANTED_BOOK
            && runesManager.isIdentified(stack);
    }

    private boolean isEquipGUI(InventoryView view) {
        return TITLE.equals(view.getTitle());
    }

    private String getSafeName(ItemStack item) {
        if (item == null) return "<none>";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        return item.getType().toString();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        InventoryView view = e.getView();
        if (!isEquipGUI(view)) return;

        Player p = (Player) e.getWhoClicked();
        InventoryAction action = e.getAction();
        ItemStack cursor = e.getCursor();
        ItemStack clicked = e.getCurrentItem();

        // 1) Handle pickups from GUI: unequip or cancel
        if (e.getClickedInventory() == view.getTopInventory() &&
            (action == InventoryAction.PICKUP_ONE || action == InventoryAction.PICKUP_ALL)) {
            if (clicked != null && isIdentifiedRune(clicked)) {
                e.setCancelled(true);
                String id = clicked.getItemMeta()
                    .getPersistentDataContainer()
                    .get(runesManager.getRuneKey(), PersistentDataType.STRING);
                Rune rune = runesManager.getRuneById(id);
                if (rune != null) {
                    runesManager.unequipRune(p, rune);
                    p.getInventory().addItem(clicked.clone().asOne());
                    p.sendMessage(ChatColor.YELLOW + "Unequipped rune: " + getSafeName(clicked));
                }
                open(p);
                return;
            }
            e.setCancelled(true);
            return;
        }

        // 2) Right-click on equipped rune to unequip
        if (action == InventoryAction.PICKUP_HALF
            && clicked != null
            && isIdentifiedRune(clicked)
            && e.getClickedInventory() == view.getTopInventory()) {
            e.setCancelled(true);
            String id = clicked.getItemMeta()
                .getPersistentDataContainer()
                .get(runesManager.getRuneKey(), PersistentDataType.STRING);
            Rune rune = runesManager.getRuneById(id);
            if (rune != null) {
                runesManager.unequipRune(p, rune);
                p.getInventory().addItem(clicked.clone().asOne());
                p.sendMessage(ChatColor.YELLOW + "Unequipped rune: " + getSafeName(clicked));
            }
            open(p);
            return;
        }

        // 3) Shift-click to equip
        if (e.isShiftClick()) {
            ItemStack toShift = e.getClickedInventory() == view.getBottomInventory() ? clicked : null;
            e.setCancelled(true);
            if (toShift != null && isIdentifiedRune(toShift)) {
                boolean success = runesManager.equipRune(p, toShift);
                if (success) {
                    // decrement one from the clicked stack
                    int newAmt = toShift.getAmount() - 1;
                    if (newAmt > 0) {
                        toShift.setAmount(newAmt);
                        e.getClickedInventory().setItem(e.getSlot(), toShift);
                    } else {
                        e.getClickedInventory().setItem(e.getSlot(), null);
                    }
                }
                p.sendMessage(success
                    ? ChatColor.GREEN + "Equipped rune: " + getSafeName(toShift)
                    : ChatColor.RED +   "Failed to equip rune: "  + getSafeName(toShift));
            }
            open(p);
            return;
        }

        // 4) Direct-click placement into GUI to equip
        if (e.getClickedInventory() == view.getTopInventory()
            && cursor != null
            && isIdentifiedRune(cursor)
            && (action == InventoryAction.PLACE_ONE
            || action == InventoryAction.PLACE_ALL
            || action == InventoryAction.PLACE_SOME)) {
            e.setCancelled(true);
            boolean success = runesManager.equipRune(p, cursor);
            if (success) {
                // decrement one from the cursor stack
                int newAmt = cursor.getAmount() - 1;
                if (newAmt > 0) {
                    cursor.setAmount(newAmt);
                    e.setCursor(cursor);
                } else {
                    e.setCursor(null);
                }
            }
            p.sendMessage(success
                ? ChatColor.GREEN + "Equipped rune: " + getSafeName(cursor)
                : ChatColor.RED +   "Failed to equip rune: "  + getSafeName(cursor));
            open(p);
            return;
        }

        // 5) Prevent invalid placements into GUI
        if (e.getClickedInventory() == view.getTopInventory()
            && cursor != null
            && !isIdentifiedRune(cursor)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!isEquipGUI(e.getView())) return;
        e.getNewItems().forEach((slot, stack) -> {
            if (slot < e.getView().getTopInventory().getSize()
                && !isIdentifiedRune(stack)) {
                e.setCancelled(true);
            }
        });
    }
}

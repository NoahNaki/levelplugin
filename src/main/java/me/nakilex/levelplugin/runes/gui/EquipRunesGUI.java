package me.nakilex.levelplugin.runes.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
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
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class EquipRunesGUI implements Listener {
    private final Main plugin;
    private final RunesManager runesManager;
    private final IdentifyRunesGUI identifyGui;

    public static final String TITLE = ChatColor.DARK_GRAY + "Equip Runes";
    private static final int SIZE = 54;

    // Materials considered valid rune items
    private static final Set<Material> VALID_RUNE_MATERIALS = Set.of(
        Material.ENCHANTED_BOOK,
        Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE
    );

    private static final Set<Integer> UNIQUE_SLOTS = Set.of(0, 4, 8, 47, 51);
    private static final Set<Integer> NORMAL_SLOTS = Set.of(
        1,2,5,6,9,11,13,15,17,18,20,22,24,26,27,29,31,33,35,36,38,40,42,44,45,48,49,52,53
    );

    private static final Map<Integer, Integer> UNLOCK_LEVELS = new HashMap<>();
    static {
        List<Integer> snake = new ArrayList<>();
        for (int col = 0; col < 9; col++) {
            if (col % 2 == 0) {
                for (int row = 5; row >= 0; row--) snake.add(row * 9 + col);
            } else {
                for (int row = 0; row < 6; row++) snake.add(row * 9 + col);
            }
        }
        int step = 0;
        for (int slot : snake) {
            if (UNIQUE_SLOTS.contains(slot) || NORMAL_SLOTS.contains(slot)) {
                UNLOCK_LEVELS.put(slot, step * 3);
                step++;
            }
        }
    }

    public EquipRunesGUI(Main plugin, RunesManager runesManager, IdentifyRunesGUI identifyGui) {
        this.plugin = plugin;
        this.runesManager = runesManager;
        this.identifyGui = (identifyGui != null) ? identifyGui : new IdentifyRunesGUI(plugin, runesManager);
    }

    public Inventory createInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        int level = LevelManager.getInstance().getLevel(player);

        // placeholders and filler
        for (int slot = 0; slot < SIZE; slot++) {
            if (UNIQUE_SLOTS.contains(slot)) {
                int req = UNLOCK_LEVELS.get(slot);
                if (level < req) inv.setItem(slot, createPlaceholder(Material.WHITE_STAINED_GLASS_PANE, req));
            } else if (NORMAL_SLOTS.contains(slot)) {
                int req = UNLOCK_LEVELS.get(slot);
                if (level < req) inv.setItem(slot, createPlaceholder(Material.RED_STAINED_GLASS_PANE, req));
            } else {
                inv.setItem(slot, createFiller(Material.GRAY_STAINED_GLASS_PANE));
            }
        }

        // place equipped runes
        for (Rune rune : runesManager.getEquippedRunes(player)) {
            boolean unique = rune.isUnique();
            Set<Integer> target = unique ? UNIQUE_SLOTS : NORMAL_SLOTS;
            for (int slot : target) {
                if (level >= UNLOCK_LEVELS.get(slot) && inv.getItem(slot) == null) {
                    inv.setItem(slot, identifyGui.createIdentifiedRuneItem(rune));
                    break;
                }
            }
        }
        return inv;
    }

    public void open(Player player) {
        player.openInventory(createInventory(player));
    }

    /**
     * True if stack has a valid rune material and a valid rune ID in PDC.
     */
    private boolean isIdentifiedRune(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        if (!VALID_RUNE_MATERIALS.contains(stack.getType())) return false;
        String id = stack.getItemMeta()
            .getPersistentDataContainer()
            .get(runesManager.getRuneKey(), PersistentDataType.STRING);
        return id != null && runesManager.getRuneById(id) != null;
    }

    private boolean isSlotUnlocked(int slot, int level) {
        Integer req = UNLOCK_LEVELS.get(slot);
        return req != null && level >= req;
    }

    private boolean isEquipGUI(InventoryView view) {
        return TITLE.equals(view.getTitle());
    }

    private String getSafeName(ItemStack item) {
        if (item == null) return "<none>";
        ItemMeta meta = item.getItemMeta();
        return (meta != null && meta.hasDisplayName()) ? meta.getDisplayName() : item.getType().toString();
    }

    private ItemStack createPlaceholder(Material mat, int levelReq) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Locked Slot");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "🔒 Unlocks at level " + ChatColor.WHITE + levelReq,
                ChatColor.DARK_GRAY + "Equip runes here once unlocked"
            ));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack createFiller(Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        // Only handle clicks in our Equip Runes GUI
        if (!isEquipGUI(e.getView())) return;

        Inventory clickedInv = e.getClickedInventory();
        boolean top = clickedInv == e.getView().getTopInventory();

        // Cancel only clicks in the GUI itself; allow bottom‐inventory clicks so players can pick up runes
        if (top) {
            e.setCancelled(true);
        }

        Player p = (Player) e.getWhoClicked();
        int level = LevelManager.getInstance().getLevel(p);
        int slot  = e.getRawSlot();
        InventoryAction action = e.getAction();
        ItemStack clicked = e.getCurrentItem();
        ItemStack cursor  = e.getCursor();

        // 1) Unequip via normal click
        if (top
            && (action == InventoryAction.PICKUP_ONE || action == InventoryAction.PICKUP_ALL)
            && isIdentifiedRune(clicked)
            && isSlotUnlocked(slot, level)) {

            String id = clicked.getItemMeta()
                .getPersistentDataContainer()
                .get(runesManager.getRuneKey(), PersistentDataType.STRING);
            Rune rune = runesManager.getRuneById(id);
            if (rune != null) {
                runesManager.unequipRune(p, rune);
                ItemStack giveBack = clicked.clone().asOne();
                int empty = p.getInventory().firstEmpty();
                if (empty == -1) p.getWorld().dropItemNaturally(p.getLocation(), giveBack);
                else p.getInventory().setItem(empty, giveBack);
                p.sendMessage(ChatColor.YELLOW + "Unequipped rune: " + getSafeName(clicked));
                open(p);
            }
            return;
        }

        // 2) Unequip via shift-click
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY
            && top
            && isIdentifiedRune(clicked)
            && isSlotUnlocked(slot, level)) {

            String id = clicked.getItemMeta()
                .getPersistentDataContainer()
                .get(runesManager.getRuneKey(), PersistentDataType.STRING);
            Rune rune = runesManager.getRuneById(id);
            if (rune != null) {
                runesManager.unequipRune(p, rune);
                ItemStack giveBack = clicked.clone().asOne();
                int empty = p.getInventory().firstEmpty();
                if (empty == -1) p.getWorld().dropItemNaturally(p.getLocation(), giveBack);
                else p.getInventory().setItem(empty, giveBack);
                p.sendMessage(ChatColor.YELLOW + "Unequipped rune: " + getSafeName(clicked));
                open(p);
            }
            return;
        }

        // 3) Shift-click equip from bottom into GUI
        // 3) Shift-click equip from bottom into GUI
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY
            && clickedInv == e.getView().getBottomInventory()) {

            // 3a) Always cancel the default move
            e.setCancelled(true);

            // 3b) Now only proceed if it's an identified rune
            if (!isIdentifiedRune(clicked)) {
                // not a rune → just drop it (we already cancelled the move)
                return;
            }

            // 3c) Your existing rune‐equip logic follows here...
            String id = clicked.getItemMeta()
                .getPersistentDataContainer()
                .get(runesManager.getRuneKey(), PersistentDataType.STRING);
            Rune rune = runesManager.getRuneById(id);
            if (rune == null) return;

            boolean unique = rune.isUnique();
            Inventory topInv = e.getView().getTopInventory();

            // Type + availability check
            if (unique) {
                boolean hasSlot = UNIQUE_SLOTS.stream()
                    .anyMatch(s -> isSlotUnlocked(s, level) && topInv.getItem(s) == null);
                if (!hasSlot) {
                    p.sendMessage(ChatColor.RED + "This rune can only go in a unique rune slot.");
                    return;
                }
            } else {
                boolean hasSlot = NORMAL_SLOTS.stream()
                    .anyMatch(s -> isSlotUnlocked(s, level) && topInv.getItem(s) == null);
                if (!hasSlot) {
                    p.sendMessage(ChatColor.RED + "This rune can only go in a normal rune slot.");
                    return;
                }
            }

            // Duplicate‐unique check
            if (unique && runesManager.getEquippedRunes(p).stream()
                .anyMatch(r -> r.getId().equals(id))) {
                p.sendMessage(ChatColor.RED + "You already have that unique rune equipped.");
                return;
            }

            // Actually equip
            Set<Integer> target = unique ? UNIQUE_SLOTS : NORMAL_SLOTS;
            for (int s : target) {
                if (isSlotUnlocked(s, level) && topInv.getItem(s) == null) {
                    boolean success = runesManager.equipRune(p, clicked);
                    if (success) {
                        clicked.setAmount(clicked.getAmount() - 1);
                        if (clicked.getAmount() <= 0)
                            e.getClickedInventory().setItem(e.getSlot(), null);
                        p.sendMessage(ChatColor.GREEN + "Equipped rune: " + getSafeName(clicked));
                        open(p);
                    } else {
                        p.sendMessage(ChatColor.RED + "Failed to equip rune.");
                    }
                    return;
                }
            }
        }


        // 4) Direct placement equip (click GUI slot while holding rune)
        if (top
            && (action == InventoryAction.PLACE_ONE
            || action == InventoryAction.PLACE_ALL
            || action == InventoryAction.PLACE_SOME)
            && isIdentifiedRune(cursor)
            && isSlotUnlocked(slot, level)) {

            String id = cursor.getItemMeta()
                .getPersistentDataContainer()
                .get(runesManager.getRuneKey(), PersistentDataType.STRING);
            Rune rune = runesManager.getRuneById(id);
            if (rune == null) return;

            boolean unique = rune.isUnique();
            // slot type validation
            if (unique && !UNIQUE_SLOTS.contains(slot)) {
                p.sendMessage(ChatColor.RED + "This rune can only go in a unique rune slot.");
                return;
            } else if (!unique && !NORMAL_SLOTS.contains(slot)) {
                p.sendMessage(ChatColor.RED + "This rune can only go in a normal rune slot.");
                return;
            }
            // unique dup check
            if (unique && runesManager.getEquippedRunes(p).stream()
                .anyMatch(r -> r.getId().equals(id))) {
                p.sendMessage(ChatColor.RED + "You already have that unique rune equipped.");
                return;
            }

            boolean success = runesManager.equipRune(p, cursor);
            if (success) {
                cursor.setAmount(cursor.getAmount() - 1);
                e.setCursor(cursor.getAmount() > 0 ? cursor : null);
                p.sendMessage(ChatColor.GREEN + "Equipped rune: " + getSafeName(cursor));
                open(p);
            } else {
                p.sendMessage(ChatColor.RED + "Failed to equip rune.");
            }
            return;
        }

        // 5) Catch-all: cancel any other leftover clicks in top
        if (top) {
            e.setCancelled(true);
        }
    }



    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!isEquipGUI(e.getView())) return;
        Player p = (Player) e.getWhoClicked();
        int level = LevelManager.getInstance().getLevel(p);

        // For every slot the player is trying to drop into:
        for (Map.Entry<Integer, ItemStack> entry : e.getNewItems().entrySet()) {
            int slot = entry.getKey();
            ItemStack stack = entry.getValue();
            if (!isIdentifiedRune(stack) || !isSlotUnlocked(slot, level)) {
                e.setCancelled(true);
                return;
            }
        }

        // Let the drag go through, *then* schedule a tick to equip whatever landed
        Bukkit.getScheduler().runTask(plugin, () -> {
            Inventory top = e.getView().getTopInventory();
            for (Map.Entry<Integer, ItemStack> entry : e.getNewItems().entrySet()) {
                int slot = entry.getKey();
                ItemStack item = top.getItem(slot);
                if (isIdentifiedRune(item)) {
                    runesManager.equipRune(p, item);
                    // remove one from cursor manually if needed...
                }
            }
        });
    }
}

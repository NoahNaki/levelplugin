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

    public static final String TITLE = ChatColor.DARK_GRAY + "Runes";
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
            int req = UNLOCK_LEVELS.getOrDefault(slot, 0);
            if (UNIQUE_SLOTS.contains(slot)) {
                if (level < req) inv.setItem(slot, createPlaceholder(Material.WHITE_STAINED_GLASS_PANE, req, true));
            } else if (NORMAL_SLOTS.contains(slot)) {
                if (level < req) inv.setItem(slot, createPlaceholder(Material.RED_STAINED_GLASS_PANE, req, false));
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

    private ItemStack createPlaceholder(Material mat, int levelReq, boolean unique) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            String title = unique
                ? ChatColor.RED + "Locked Unique Slot 🔒"
                : ChatColor.RED + "Locked Slot 🔒";

            meta.setDisplayName(title);
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Unlocks at level " + ChatColor.WHITE + levelReq,
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
        // Only our Equip GUI
        if (!isEquipGUI(e.getView())) return;

        InventoryView view     = e.getView();
        Inventory    topInv    = view.getTopInventory();
        Inventory    bottomInv = view.getBottomInventory();
        Inventory    clickedInv= e.getClickedInventory();

        Player       p         = (Player) e.getWhoClicked();
        int          level     = LevelManager.getInstance().getLevel(p);
        int          rawSlot   = e.getRawSlot();
        InventoryAction action  = e.getAction();
        ItemStack    current   = e.getCurrentItem();
        ItemStack    cursor    = e.getCursor();
        boolean      top       = clickedInv == topInv;
        boolean      bottom    = clickedInv == bottomInv;

        // --- 0) Block taking placeholders out ---
        if (top && current != null && current.hasItemMeta()) {
            String name = current.getItemMeta().getDisplayName();
            if (name != null && name.contains("Locked")) {
                e.setCancelled(true);
                return;
            }
        }

        // --- 1) UNEQUIP (any removal action on a rune in the GUI) ---
        if (top && current != null && isIdentifiedRune(current) && isSlotUnlocked(rawSlot, level)) {
            boolean isRemoval = switch (action) {
                case PICKUP_ONE, PICKUP_ALL, MOVE_TO_OTHER_INVENTORY, COLLECT_TO_CURSOR -> true;
                default -> false;
            };
            if (isRemoval) {
                // Debug log
                plugin.getLogger().info("[DBG] Unequip fired for rune: " + getSafeName(current));

                // Perform data unequip
                String id = current.getItemMeta()
                    .getPersistentDataContainer()
                    .get(runesManager.getRuneKey(), PersistentDataType.STRING);
                Rune rune = runesManager.getRuneById(id);
                if (rune != null) {
                    runesManager.unequipRune(p, rune);
                }
                // let vanilla remove the item and put it on cursor/inv
                return;
            }
        }

        // --- 2) SHIFT-CLICK EQUIP from player INV into GUI ---
        if (bottom && action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // Must be an identified rune
            if (!isIdentifiedRune(current)) {
                e.setCancelled(true);
                return;
            }

            String id        = current.getItemMeta()
                .getPersistentDataContainer()
                .get(runesManager.getRuneKey(), PersistentDataType.STRING);
            Rune   rune      = runesManager.getRuneById(id);
            boolean unique   = rune.isUnique();

            // Find first free slot of correct type
            Set<Integer> pool    = unique ? UNIQUE_SLOTS : NORMAL_SLOTS;
            Optional<Integer> free = pool.stream()
                .filter(s -> isSlotUnlocked(s, level) && topInv.getItem(s) == null)
                .findFirst();

            if (free.isEmpty()) {
                p.sendMessage(ChatColor.RED + (unique
                    ? "No free unique rune slots."
                    : "No free normal rune slots."));
                e.setCancelled(true);
                return;
            }
            // Prevent duplicate unique
            if (unique && runesManager.getEquippedRunes(p).stream()
                .anyMatch(r -> r.getId().equals(id))) {
                p.sendMessage(ChatColor.RED + "You already have that unique rune equipped.");
                e.setCancelled(true);
                return;
            }

            // Debug log
            plugin.getLogger().info("[DBG] Equip fired (shift-click) for rune: " + getSafeName(current));

            // Let vanilla move one; then update data & GUI slot
            e.setCancelled(true);
            if (runesManager.equipRune(p, rune)) {
                // consume one from bottomInv
                current.setAmount(current.getAmount() - 1);
                if (current.getAmount() == 0) bottomInv.setItem(e.getSlot(), null);
                else bottomInv.setItem(e.getSlot(), current);

                // place into the free GUI slot
                ItemStack placed = identifyGui.createIdentifiedRuneItem(rune);
                placed.setAmount(1);
                topInv.setItem(free.get(), placed);
            }
            return;
        }

        // --- 3) CLICK-PLACE EQUIP (direct place) ---
        if (top
            && (action == InventoryAction.PLACE_ONE
            || action == InventoryAction.PLACE_ALL
            || action == InventoryAction.PLACE_SOME)
            && cursor != null
            && isIdentifiedRune(cursor)
            && isSlotUnlocked(rawSlot, level)) {

            String id     = cursor.getItemMeta()
                .getPersistentDataContainer()
                .get(runesManager.getRuneKey(), PersistentDataType.STRING);
            Rune   rune   = runesManager.getRuneById(id);
            boolean unique= rune.isUnique();

            // Reject wrong slot or duplicate unique
            if ((unique && !UNIQUE_SLOTS.contains(rawSlot))
                || (!unique && !NORMAL_SLOTS.contains(rawSlot))
                || (unique && runesManager.getEquippedRunes(p).stream()
                .anyMatch(r -> r.getId().equals(id)))) {
                e.setCancelled(true);
                return;
            }

            // Debug log
            plugin.getLogger().info("[DBG] Equip fired (place) for rune: " + getSafeName(cursor));

            // Perform equip
            e.setCancelled(true);
            if (runesManager.equipRune(p, cursor)) {
                cursor.setAmount(cursor.getAmount() - 1);
                e.setCursor(cursor.getAmount() > 0 ? cursor : null);

                ItemStack placed = identifyGui.createIdentifiedRuneItem(rune);
                placed.setAmount(1);
                topInv.setItem(rawSlot, placed);
            }
            return;
        }

        // Everything else (including picking up filler panes) is handled by vanilla
    }



    // —— Drag support ——
    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!isEquipGUI(e.getView())) return;
        Player p = (Player) e.getWhoClicked();
        int level = LevelManager.getInstance().getLevel(p);

        // if any of the target slots in the drag are illegal, cancel all of it
        for (Map.Entry<Integer, ItemStack> entry : e.getNewItems().entrySet()) {
            int slot = entry.getKey();
            ItemStack stack = entry.getValue();

            // only validate drags *into* our GUI
            if (entry.getKey() < e.getView().getTopInventory().getSize()) {
                // must be identified rune, correct slot‐type, and unlocked
                if (!isIdentifiedRune(stack)
                    || !isSlotUnlocked(slot, level)
                    || (runesManager.getRuneById(
                    stack.getItemMeta()
                        .getPersistentDataContainer()
                        .get(runesManager.getRuneKey(), PersistentDataType.STRING)
                ).isUnique() && !UNIQUE_SLOTS.contains(slot))
                    || (!runesManager.getRuneById(
                    stack.getItemMeta()
                        .getPersistentDataContainer()
                        .get(runesManager.getRuneKey(), PersistentDataType.STRING)
                ).isUnique() && !NORMAL_SLOTS.contains(slot)))
                {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
}

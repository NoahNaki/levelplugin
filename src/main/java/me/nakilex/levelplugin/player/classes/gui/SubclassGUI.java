package me.nakilex.levelplugin.player.classes.gui;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for selecting unlocked subclasses. Supports sorting and pagination
 * similar to the storage GUI style.
 */
public class SubclassGUI implements Listener {

    public static final String TITLE = ChatColor.DARK_AQUA + "Select Subclass";

    private static final int GUI_SIZE = 54; // full chest
    private static final int[] CLASS_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int ITEMS_PER_PAGE = CLASS_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int FILTER_SLOT = 48;
    private static final int SORT_SLOT = 50;
    private static final int INFO_SLOT = 8;

    private static final ItemStack LOCK_ITEM_BASE;
    private static final ItemStack FILLER;

    static {
        ItemBuilder b = NexoItems.itemFromId("lock");
        LOCK_ITEM_BASE = b == null ? new ItemStack(Material.BARRIER) : b.build();
        ItemMeta meta = LOCK_ITEM_BASE.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Locked");
            LOCK_ITEM_BASE.setItemMeta(meta);
        }
        FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = FILLER.getItemMeta();
        if (fm != null) { fm.setDisplayName(" "); FILLER.setItemMeta(fm); }
    }

    private static final Map<UUID, Inventory> OPEN = new HashMap<>();
    static final Map<UUID, Integer> PAGE = new HashMap<>();
    static final Map<UUID, Integer> FILTER = new HashMap<>();
    static final Map<UUID, Integer> SORT = new HashMap<>();
    private static final Map<UUID, List<PlayerClass>> PAGE_CONTENT = new HashMap<>();

    public static void open(Player player) {
        int page = PAGE.getOrDefault(player.getUniqueId(), 0);
        open(player, page);
    }

    static void open(Player player, int page) {
        PAGE.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, TITLE);

        for (int i = 0; i < GUI_SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, FILLER);
            }
        }

        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        List<PlayerClass> list = new ArrayList<>();
        for (PlayerClass pc : PlayerClass.values()) {
            if (pc != PlayerClass.VILLAGER) list.add(pc);
        }

        int filter = FILTER.getOrDefault(player.getUniqueId(), 0);
        int sort = SORT.getOrDefault(player.getUniqueId(), 0);

        // sort
        if (sort == 1) {
            list.sort(Comparator
                    .comparing((PlayerClass pc) -> !ps.unlockedClasses.contains(pc))
                    .thenComparing(pc -> pc.name().toLowerCase()));
        } else {
            list.sort(Comparator.comparing(pc -> pc.name().toLowerCase()));
        }

        // filter
        if (filter == 1) {
            list.removeIf(pc -> !ps.unlockedClasses.contains(pc));
        }

        int start = page * ITEMS_PER_PAGE;
        List<PlayerClass> pageList = new ArrayList<>();
        for (int i = start, slot = 0; i < list.size() && slot < ITEMS_PER_PAGE; i++) {
            PlayerClass pc = list.get(i);
            boolean unlocked = ps.unlockedClasses.contains(pc);
            ItemStack item = unlocked ? createItem(pc) : createLockedItem(pc);
            inv.setItem(CLASS_SLOTS[slot++], item);
            pageList.add(pc);
        }
        PAGE_CONTENT.put(player.getUniqueId(), pageList);

        if (page > 0) inv.setItem(PREV_SLOT, getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        if (list.size() > (page + 1) * ITEMS_PER_PAGE) inv.setItem(NEXT_SLOT, getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        inv.setItem(FILTER_SLOT, createFilterButton(filter));
        inv.setItem(SORT_SLOT, createSortButton(sort));
        inv.setItem(INFO_SLOT, getNexoItem("info", ChatColor.YELLOW + "Information"));

        player.openInventory(inv);
        OPEN.put(player.getUniqueId(), inv);
    }

    private static ItemStack createItem(PlayerClass pc) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + pc.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createLockedItem(PlayerClass pc) {
        ItemStack item = LOCK_ITEM_BASE.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + pc.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack getNexoItem(String id, String name) {
        ItemBuilder b = NexoItems.itemFromId(id);
        ItemStack it = b == null ? new ItemStack(Material.BARRIER) : b.build();
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); it.setItemMeta(meta); }
        return it;
    }

    private static ItemStack createFilterButton(int mode) {
        ItemStack it = new ItemStack(Material.HOPPER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Filter");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_GRAY + "Filter the classes");
            lore.add(" ");
            String[] opts = {"Show All", "Unlocked"};
            for (int i = 0; i < opts.length; i++) {
                lore.add(rangeLine(i, mode, opts[i]));
            }
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Click to cycle");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static ItemStack createSortButton(int mode) {
        ItemStack it = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Sorting");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_GRAY + "Sort the classes");
            lore.add(" ");
            String[] opts = {"A-Z", "Unlocked First"};
            for (int i = 0; i < opts.length; i++) {
                lore.add(rangeLine(i, mode, opts[i]));
            }
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Click to cycle");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static String rangeLine(int index, int current, String label) {
        ChatColor color = index == current ? ChatColor.WHITE : ChatColor.GRAY;
        ChatColor bullet = index == current ? ChatColor.GREEN : ChatColor.DARK_GRAY;
        return bullet + "- " + color + label;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory open = OPEN.get(player.getUniqueId());
        if (open == null || !e.getView().getTopInventory().equals(open)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == PREV_SLOT) {
            int p = PAGE.getOrDefault(player.getUniqueId(), 0);
            open(player, Math.max(0, p - 1));
            return;
        }
        if (slot == NEXT_SLOT) {
            int p = PAGE.getOrDefault(player.getUniqueId(), 0);
            open(player, p + 1);
            return;
        }
        if (slot == FILTER_SLOT) {
            int f = FILTER.getOrDefault(player.getUniqueId(), 0);
            f = (f + 1) % 2;
            FILTER.put(player.getUniqueId(), f);
            open(player, PAGE.getOrDefault(player.getUniqueId(), 0));
            return;
        }
        if (slot == SORT_SLOT) {
            int s = SORT.getOrDefault(player.getUniqueId(), 0);
            s = (s + 1) % 2;
            SORT.put(player.getUniqueId(), s);
            open(player, PAGE.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        List<PlayerClass> current = PAGE_CONTENT.getOrDefault(player.getUniqueId(), Collections.emptyList());
        for (int i = 0; i < current.size() && i < CLASS_SLOTS.length; i++) {
            if (slot == CLASS_SLOTS[i]) {
                PlayerClass pc = current.get(i);
                StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
                if (ps.unlockedClasses.contains(pc)) {
                    ps.playerClass = pc;
                    player.sendMessage(ChatColor.GREEN + "Subclass changed to " + pc.name());
                }
                player.closeInventory();
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory open = OPEN.get(e.getPlayer().getUniqueId());
        if (open != null && e.getInventory().equals(open)) {
            OPEN.remove(e.getPlayer().getUniqueId());
            PAGE_CONTENT.remove(e.getPlayer().getUniqueId());
        }
    }
}


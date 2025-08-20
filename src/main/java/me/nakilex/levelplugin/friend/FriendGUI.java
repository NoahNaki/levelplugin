package me.nakilex.levelplugin.friend;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

/** GUI to display a player's friends with sorting options. */
public class FriendGUI implements Listener {
    private final FriendManager manager;

    private static final int SIZE = 54;
    private static final String TITLE = ChatColor.BLACK + "Friends";
    private static final int[] FRIEND_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int ITEMS_PER_PAGE = FRIEND_SLOTS.length;

    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int SORT_SLOT = 51;

    private final Map<UUID, Integer> pageMap = new HashMap<>();
    private final Map<UUID, Integer> sortModes = new HashMap<>();

    public FriendGUI(FriendManager manager) {
        this.manager = manager;
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    public void open(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        open(player, page);
    }

    private void open(Player player, int page) {
        List<UUID> list = new ArrayList<>(manager.getFriends(player.getUniqueId()));
        int sort = sortModes.getOrDefault(player.getUniqueId(), 0);

        Comparator<UUID> comp;
        switch (sort) {
            case 1 -> comp = Comparator.comparing((UUID id) -> Bukkit.getPlayer(id) == null)
                    .thenComparing(id -> Optional.ofNullable(Bukkit.getOfflinePlayer(id).getName()).orElse(""), String.CASE_INSENSITIVE_ORDER);
            case 2 -> comp = Comparator.comparingLong((UUID id) -> -manager.getFriendAddedTime(player.getUniqueId(), id));
            case 3 -> comp = Comparator.comparingInt((UUID id) -> -LevelManager.getInstance().getLevel(id));
            default -> comp = Comparator.comparing(id -> {
                OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                String n = op.getName();
                return n == null ? "" : n.toLowerCase();
            });
        }
        list.sort(comp);

        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        GuiUtil.fillBorder(inv, filler);

        int start = page * ITEMS_PER_PAGE;
        for (int i = start, slot = 0; i < list.size() && slot < ITEMS_PER_PAGE; i++) {
            UUID id = list.get(i);
            OfflinePlayer off = Bukkit.getOfflinePlayer(id);
            List<String> lore = new ArrayList<>();
            int lvl = LevelManager.getInstance().getLevel(id);
            lore.add(ChatColor.GRAY + "Level: " + lvl);
            PlayerClass pc = StatsManager.getInstance().getPlayerStats(id).playerClass;
            String className = pc.name().substring(0,1) + pc.name().substring(1).toLowerCase();
            lore.add(ChatColor.GRAY + "Class: " + className);
            boolean online = Bukkit.getPlayer(id) != null;
            lore.add(online ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline");
            long added = manager.getFriendAddedTime(player.getUniqueId(), id);
            if (added > 0) {
                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(added));
                lore.add(ChatColor.GRAY + "Added: " + ChatColor.WHITE + date);
            }
            String name = off.getName() != null ? off.getName() : id.toString();
            ItemStack head = HeadUtil.createPlayerHead(off, ChatColor.YELLOW + name, lore);
            inv.setItem(FRIEND_SLOTS[slot++], head);
        }

        if (page > 0) inv.setItem(PREV_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        if (list.size() > (page + 1) * ITEMS_PER_PAGE) {
            inv.setItem(NEXT_SLOT, GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        }
        inv.setItem(SORT_SLOT, createSortButton(sort));

        player.openInventory(inv);
        pageMap.put(player.getUniqueId(), page);
    }

    private ItemStack createSortButton(int mode) {
        ItemStack it = GuiUtil.getNexoItem("server_icon", ChatColor.AQUA + "Sort");
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            String[] opts = {"Alphabetical", "Online", "Date Added", "Level"};
            for (int i = 0; i < opts.length; i++) {
                ChatColor c = i == mode ? ChatColor.WHITE : ChatColor.GRAY;
                ChatColor b = i == mode ? ChatColor.GREEN : ChatColor.DARK_GRAY;
                lore.add(b + "- " + c + opts[i]);
            }
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Left-Click " + ChatColor.GRAY + "to go forward");
            lore.add(ChatColor.WHITE + "Right-Click " + ChatColor.GRAY + "to go backward");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!ChatColor.stripColor(e.getView().getTitle()).equals(ChatColor.stripColor(TITLE))) return;
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        if (slot == PREV_SLOT) {
            int p = pageMap.getOrDefault(player.getUniqueId(), 0);
            open(player, Math.max(0, p - 1));
            return;
        }
        if (slot == NEXT_SLOT) {
            int p = pageMap.getOrDefault(player.getUniqueId(), 0);
            open(player, p + 1);
            return;
        }
        if (slot == SORT_SLOT) {
            int m = sortModes.getOrDefault(player.getUniqueId(), 0);
            if (e.getClick() == ClickType.RIGHT) {
                m = (m + 3) % 4;
            } else {
                m = (m + 1) % 4;
            }
            sortModes.put(player.getUniqueId(), m);
            open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
        }
    }
}


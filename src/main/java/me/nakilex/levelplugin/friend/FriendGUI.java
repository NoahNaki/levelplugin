package me.nakilex.levelplugin.friend;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
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
    private static final String TITLE = "Friends";
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
    private final List<GuiWidget> widgets;

    public FriendGUI(FriendManager manager) {
        this.manager = manager;
        this.widgets = buildWidgets();
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

        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();

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

        renderWidgets(inv, player);

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
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!GuiUtil.titleMatches(e.getView().getTitle(), TITLE)) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (handleWidgetClick(e, player)) {
            return;
        }
        e.setCancelled(true);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new ActionWidget(PREV_SLOT,
                context -> createPrevItem(context.player()),
                (click, context) -> handlePrev(context.player())));
        widgetList.add(new ActionWidget(NEXT_SLOT,
                context -> createNextItem(context.player()),
                (click, context) -> handleNext(context.player())));
        widgetList.add(new ActionWidget(SORT_SLOT,
                context -> createSortButton(sortModes.getOrDefault(context.player().getUniqueId(), 0)),
                (click, context) -> handleSortClick(context.player(), click)));
        return widgetList;
    }

    private void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private ItemStack createPrevItem(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        return page > 0 ? GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous") : null;
    }

    private ItemStack createNextItem(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        int size = manager.getFriends(player.getUniqueId()).size();
        return size > (page + 1) * ITEMS_PER_PAGE
                ? GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next")
                : null;
    }

    private void handlePrev(Player player) {
        int p = pageMap.getOrDefault(player.getUniqueId(), 0);
        open(player, Math.max(0, p - 1));
    }

    private void handleNext(Player player) {
        int p = pageMap.getOrDefault(player.getUniqueId(), 0);
        open(player, p + 1);
    }

    private void handleSortClick(Player player, ClickType click) {
        int m = sortModes.getOrDefault(player.getUniqueId(), 0);
        if (click == ClickType.RIGHT) {
            m = (m + 3) % 4;
        } else {
            m = (m + 1) % 4;
        }
        sortModes.put(player.getUniqueId(), m);
        open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
    }
}

package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * GUI for guild leaders to manage pending applications.
 */
public class GuildApplicantsGUI implements Listener {
    private final GuildManager manager;
    private GuildMemberGUI memberGUI;

    private static final int SIZE = 54;
    private static final String TITLE = ChatColor.BLACK + "Guild Applicants";

    private static final int[] APPLICANT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int ITEMS_PER_PAGE = APPLICANT_SLOTS.length;

    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int SEARCH_SLOT = 48;
    private static final int BACK_SLOT = 49;
    private static final int SORT_SLOT = 51;
    private static final int REFRESH_SLOT = 0;

    private static final String CONFIRM_TITLE = ChatColor.BLACK + "Confirm";
    private static final int CONFIRM_SIZE = 27;
    private static final int CONFIRM_YES_SLOT = 11;
    private static final int CONFIRM_NO_SLOT = 15;

    private final Map<UUID, Integer> pageMap = new HashMap<>();
    private final Map<UUID, String> searchTerms = new HashMap<>();
    private final Map<UUID, Integer> sortModes = new HashMap<>();
    private final Set<UUID> awaitingSearch = new HashSet<>();

    private static class PendingDecision {
        UUID applicant;
        boolean accept;
    }
    private final Map<UUID, PendingDecision> pending = new HashMap<>();

    public GuildApplicantsGUI(GuildManager manager) {
        this.manager = manager;
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    public void setMemberGUI(GuildMemberGUI memberGUI) {
        this.memberGUI = memberGUI;
    }

    public void open(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        open(player, page);
    }

    private void open(Player player, int page) {
        Guild g = manager.getGuild(player.getUniqueId());
        if (g == null) {
            player.sendMessage(ChatColor.RED + "You are not in a guild.");
            return;
        }
        pageMap.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, filler);
            }
        }

        String term = searchTerms.getOrDefault(player.getUniqueId(), "").toLowerCase();
        int sort = sortModes.getOrDefault(player.getUniqueId(), 0);

        List<Map.Entry<UUID, Long>> apps = new ArrayList<>(g.getApplicants().entrySet());
        // filter
        if (!term.isEmpty()) {
            apps.removeIf(e -> {
                OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
                String n = op.getName();
                return n == null || !n.toLowerCase().contains(term);
            });
        }

        // sort
        Comparator<Map.Entry<UUID, Long>> comp;
        switch (sort) {
            case 1 -> comp = Comparator.comparingLong(Map.Entry::getValue); // date
            case 2 -> comp = Comparator.comparingInt((Map.Entry<UUID, Long> e) -> -LevelManager.getInstance().getLevel(e.getKey()));
            default -> comp = Comparator.comparing(e -> {
                OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
                String n = op.getName();
                return n == null ? "" : n.toLowerCase();
            });
        }
        apps.sort(comp);

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        int start = page * ITEMS_PER_PAGE;
        for (int i = start, slot = 0; i < apps.size() && slot < ITEMS_PER_PAGE; i++) {
            UUID id = apps.get(i).getKey();
            long ts = apps.get(i).getValue();
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            List<String> lore = new ArrayList<>();
            int level = LevelManager.getInstance().getLevel(id);
            lore.add(ChatColor.GRAY + "Level: " + level);
            lore.add(ChatColor.GRAY + "Applied: " + fmt.format(new Date(ts)));
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Left-click to accept");
            lore.add(ChatColor.WHITE + "Right-click to deny");
            ItemStack head = HeadUtil.createPlayerHead(op, ChatColor.YELLOW + op.getName(), lore);
            inv.setItem(APPLICANT_SLOTS[slot++], head);
        }

        if (page > 0) inv.setItem(PREV_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        if (apps.size() > (page + 1) * ITEMS_PER_PAGE) {
            inv.setItem(NEXT_SLOT, GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        }
        inv.setItem(BACK_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));
        inv.setItem(SEARCH_SLOT, createSearchButton(term));
        inv.setItem(SORT_SLOT, createSortButton(sort));
        inv.setItem(REFRESH_SLOT, GuiUtil.getNexoItem("refresh", ChatColor.RED + "Refresh"));

        player.openInventory(inv);
    }

    private ItemStack createSearchButton(String term) {
        ItemStack it = GuiUtil.getNexoItem("search", ChatColor.AQUA + "Search");
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (!term.isEmpty()) {
                lore.add(ChatColor.GRAY + "Current: " + ChatColor.YELLOW + term);
                lore.add(ChatColor.GRAY + "Right-click to clear");
            } else {
                lore.add(ChatColor.GRAY + "Left-click to search");
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createSortButton(int mode) {
        ItemStack it = GuiUtil.getNexoItem("server_icon", ChatColor.AQUA + "Sort");
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            String[] opts = {"Alphabetical", "Date", "Level"};
            for (int i = 0; i < opts.length; i++) {
                ChatColor c = i == mode ? ChatColor.WHITE : ChatColor.GRAY;
                ChatColor b = i == mode ? ChatColor.GREEN : ChatColor.DARK_GRAY;
                lore.add(b + "- " + c + opts[i]);
            }
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Click to cycle");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private void openConfirm(Player player, UUID applicant, boolean accept) {
        Inventory inv = Bukkit.createInventory(null, CONFIRM_SIZE, CONFIRM_TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < CONFIRM_SIZE; i++) inv.setItem(i, filler);
        OfflinePlayer op = Bukkit.getOfflinePlayer(applicant);
        inv.setItem(CONFIRM_YES_SLOT, GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm"));
        inv.setItem(13, HeadUtil.createPlayerHead(op, ChatColor.YELLOW + op.getName(), null));
        inv.setItem(CONFIRM_NO_SLOT, GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel"));
        PendingDecision pd = new PendingDecision();
        pd.applicant = applicant;
        pd.accept = accept;
        pending.put(player.getUniqueId(), pd);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        String title = ChatColor.stripColor(e.getView().getTitle());
        if (title.equals(ChatColor.stripColor(TITLE))) {
            e.setCancelled(true);
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
            if (slot == BACK_SLOT) {
                memberGUI.open(player);
                return;
            }
            if (slot == SEARCH_SLOT) {
                if (e.getClick() == ClickType.RIGHT) {
                    searchTerms.remove(player.getUniqueId());
                    open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
                } else {
                    awaitingSearch.add(player.getUniqueId());
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "Enter search term or 'cancel'.");
                }
                return;
            }
            if (slot == SORT_SLOT) {
                int m = sortModes.getOrDefault(player.getUniqueId(), 0);
                m = (m + 1) % 3;
                sortModes.put(player.getUniqueId(), m);
                open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
                return;
            }
            if (slot == REFRESH_SLOT) {
                open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
                return;
            }
            ItemStack clicked = e.getCurrentItem();
            if (clicked != null && clicked.hasItemMeta() && clicked.getType() == Material.PLAYER_HEAD) {
                String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                OfflinePlayer op = Bukkit.getOfflinePlayer(name);
                if (op != null) {
                    UUID target = op.getUniqueId();
                    if (e.isLeftClick()) {
                        openConfirm(player, target, true);
                    } else if (e.isRightClick()) {
                        openConfirm(player, target, false);
                    }
                }
            }
            return;
        }
        if (title.equals(ChatColor.stripColor(CONFIRM_TITLE))) {
            e.setCancelled(true);
            PendingDecision pd = pending.get(player.getUniqueId());
            if (pd == null) return;
            if (e.getRawSlot() == CONFIRM_YES_SLOT) {
                Guild g = manager.getGuild(player.getUniqueId());
                if (g != null) {
                    if (pd.accept) {
                        if (manager.acceptApplicant(g.getName(), pd.applicant)) {
                            OfflinePlayer ap = Bukkit.getOfflinePlayer(pd.applicant);
                            if (ap.isOnline()) {
                                ((Player) ap.getPlayer()).sendMessage(ChatColor.GREEN + "Your guild application to " + g.getName() + " was accepted!");
                            }
                        }
                    } else {
                        if (manager.denyApplicant(g.getName(), pd.applicant)) {
                            OfflinePlayer ap = Bukkit.getOfflinePlayer(pd.applicant);
                            if (ap.isOnline()) {
                                ((Player) ap.getPlayer()).sendMessage(ChatColor.RED + "Your guild application to " + g.getName() + " was denied.");
                            }
                        }
                    }
                }
                pending.remove(player.getUniqueId());
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player, pageMap.getOrDefault(player.getUniqueId(), 0)));
            } else if (e.getRawSlot() == CONFIRM_NO_SLOT) {
                pending.remove(player.getUniqueId());
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player, pageMap.getOrDefault(player.getUniqueId(), 0)));
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (awaitingSearch.remove(id)) {
            e.setCancelled(true);
            String msg = e.getMessage();
            if (msg.equalsIgnoreCase("cancel")) {
                searchTerms.remove(id);
            } else {
                searchTerms.put(id, msg.trim());
            }
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(e.getPlayer(), pageMap.getOrDefault(id, 0)));
        }
    }
}


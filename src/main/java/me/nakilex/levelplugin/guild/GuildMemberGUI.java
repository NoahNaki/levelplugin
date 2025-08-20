package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import me.nakilex.levelplugin.guild.GuildPermission;
import me.nakilex.levelplugin.guild.GuildRole;
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

import java.util.*;

/**
 * Enhanced guild menu showing members with pagination, search and MOTD editing.
 */
public class GuildMemberGUI implements Listener {
    private final GuildManager manager;
    private final GuildGUI guildGUI;
    private final GuildApplicantsGUI applicantsGUI;
    private final GuildSettingsGUI settingsGUI;
    private final me.nakilex.levelplugin.guild.quests.GuildQuestGUI questGUI;

    private static final int SIZE = 54;
    private static final String TITLE = ChatColor.BLACK + "Guild Menu";

    private static final int[] MEMBER_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int ITEMS_PER_PAGE = MEMBER_SLOTS.length;

    private static final int PREV_SLOT   = 45;
    private static final int NEXT_SLOT   = 53;
    private static final int MOTD_SLOT   = 47;
    private static final int SEARCH_SLOT = 48;
    private static final int HOME_SLOT   = 49;
    private static final int CAMERA_SLOT = 50;
    private static final int SORT_SLOT   = 51;
    private static final int VAULT_SLOT  = 46;
    private static final int SETTINGS_SLOT = 52;
    private static final int QUESTS_SLOT = 44;
    private static final int INFO_SLOT   = 8;
    private static final int REFRESH_SLOT = 0;

    private final Map<UUID, Integer> pageMap = new HashMap<>();
    private final Map<UUID, String> searchTerms = new HashMap<>();
    private final Map<UUID, Integer> sortModes = new HashMap<>();
    private final Set<UUID> awaitingSearch = new HashSet<>();
    private final Set<UUID> awaitingMotd = new HashSet<>();

    public GuildMemberGUI(GuildManager manager, GuildGUI guildGUI, GuildApplicantsGUI applicantsGUI, GuildSettingsGUI settingsGUI,
                          me.nakilex.levelplugin.guild.quests.GuildQuestGUI questGUI) {
        this.manager = manager;
        this.guildGUI = guildGUI;
        this.applicantsGUI = applicantsGUI;
        this.settingsGUI = settingsGUI;
        this.questGUI = questGUI;
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
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

        List<UUID> members = new ArrayList<>(g.getMembers());
        // filter by search
        if (!term.isEmpty()) {
            members.removeIf(id -> {
                OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                String n = op.getName();
                return n == null || !n.toLowerCase().contains(term);
            });
        }

        // sort
        Comparator<UUID> comp;
        switch (sort) {
            case 1 -> comp = Comparator.comparing((UUID id) -> Bukkit.getPlayer(id) == null)
                    .thenComparing(id -> {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                        String n = op.getName();
                        return n == null ? "" : n.toLowerCase();
                    }); // online first
            case 2 -> comp = Comparator.comparingInt((UUID id) ->
                    -LevelManager.getInstance().getLevel(id)); // level desc
            default -> comp = Comparator.comparing(id -> {
                OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                String n = op.getName();
                return n == null ? "" : n.toLowerCase();
            });
        }
        members.sort(comp);

        int start = page * ITEMS_PER_PAGE;
        for (int i = start, slot = 0; i < members.size() && slot < ITEMS_PER_PAGE; i++) {
            UUID id = members.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            List<String> lore = new ArrayList<>();
            if (id.equals(g.getLeader())) lore.add(ChatColor.GOLD + "Leader");
            int level = LevelManager.getInstance().getLevel(id);
            lore.add(ChatColor.GRAY + "Level: " + level);
            boolean online = Bukkit.getPlayer(id) != null;
            lore.add(online ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline");
            ItemStack head = HeadUtil.createPlayerHead(op, ChatColor.YELLOW + op.getName(), lore);
            inv.setItem(MEMBER_SLOTS[slot++], head);
        }

        if (page > 0) inv.setItem(PREV_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        if (members.size() > (page + 1) * ITEMS_PER_PAGE) {
            inv.setItem(NEXT_SLOT, GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        }
        ItemStack infoItem = GuiUtil.getNexoItem("home", ChatColor.YELLOW + "Guild Info");
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Leader: " + ChatColor.WHITE + g.getLeaderName());
            lore.add(ChatColor.GRAY + "Members: " + ChatColor.WHITE + g.getMembers().size() + ChatColor.GRAY + "/" + ChatColor.WHITE + g.getMaxMembers());
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + g.getLevel());
            int need = g.getExpNeeded();
            if (need > 0) {
                int cur = g.getExp();
                double progress = cur / (double) need;
                double percent = Math.round(progress * 1000.0) / 10.0;
                lore.add(ChatColor.GRAY + "Progress: " + ChatColor.YELLOW + percent + "%");
                String bar = GuiUtil.createProgressBar(progress, 15);
                String expColor = ChatFormatter.experienceColor();
                lore.add(bar + " " + expColor + cur + ChatColor.GOLD + "/" + expColor + need + " " + ChatFormatter.experienceLabel());
            }
            infoMeta.setLore(lore);
            infoItem.setItemMeta(infoMeta);
        }
        inv.setItem(HOME_SLOT, infoItem);
        inv.setItem(SEARCH_SLOT, createSearchButton(term));
        inv.setItem(MOTD_SLOT, createMotdButton(g, player));
        ItemStack camItem;
        if (g.getLeader().equals(player.getUniqueId())) {
            camItem = GuiUtil.getNexoItem("camera", ChatColor.YELLOW + "Applicants");
            ItemMeta meta = camItem.getItemMeta();
            if (meta != null) {
                int count = g.getApplicants().size();
                List<String> lore = new ArrayList<>();
                if (count > 0) {
                    lore.add(ChatColor.GRAY + "Pending: " + ChatColor.WHITE + count);
                } else {
                    lore.add(ChatColor.GRAY + "No pending applications");
                }
                meta.setLore(lore);
                camItem.setItemMeta(meta);
            }
        } else {
            camItem = GuiUtil.getNexoItem("camera", ChatColor.YELLOW + "Coming Soon");
        }
        inv.setItem(CAMERA_SLOT, camItem);
        inv.setItem(SORT_SLOT, createSortButton(sort));
        inv.setItem(INFO_SLOT, createInfoButton(g));
        inv.setItem(REFRESH_SLOT, GuiUtil.getNexoItem("refresh", ChatColor.RED + "Refresh"));
        ItemStack vaultItem = new ItemStack(Material.CHEST);
        ItemMeta vMeta = vaultItem.getItemMeta();
        if (vMeta != null) {
            vMeta.setDisplayName(ChatColor.GOLD + "Guild Storage");
            vaultItem.setItemMeta(vMeta);
        }
        inv.setItem(VAULT_SLOT, vaultItem);
        inv.setItem(SETTINGS_SLOT, GuiUtil.getNexoItem("settings", ChatColor.AQUA + "Settings"));
        inv.setItem(QUESTS_SLOT, GuiUtil.getNexoItem("pack1_scroll2", ChatColor.GOLD + "Guild Quests"));

        player.openInventory(inv);
    }

    private ItemStack createSearchButton(String term) {
        ItemStack it = GuiUtil.getNexoItem("search", ChatColor.GOLD + "Search");
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (!term.isEmpty()) {
                lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + term);
                lore.add(ChatColor.GRAY + "Right-click to clear");
            } else {
                lore.add(ChatColor.GRAY + "Click to enter a term");
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createMotdButton(Guild g, Player viewer) {
        ItemStack it = GuiUtil.getNexoItem("speech", ChatColor.YELLOW + "Guild MOTD");
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            String motd = g.getMotd();
            if (motd == null || motd.isEmpty()) motd = ChatColor.GRAY + "None";
            lore.add(motd);
            if (manager.hasPermission(viewer.getUniqueId(), GuildPermission.CHANGE_MOTD)) {
                lore.add(" ");
                lore.add(ChatColor.WHITE + "Left-click to edit");
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
            String[] opts = {"Alphabetical", "Active", "Level"};
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

    private ItemStack createInfoButton(Guild g) {
        ItemStack it = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Information");
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Benefits of Level " + ChatColor.YELLOW + g.getLevel());
            lore.add(ChatColor.GRAY + "Members: " + ChatColor.WHITE + g.getMaxMembers());
            lore.add(ChatColor.GRAY + "Coin Capacity: " + ChatColor.GOLD + g.getCoinCapacity() + ChatColor.YELLOW + " <glyph:coins_icon>");
            lore.add(ChatColor.GRAY + "Storage Pages: " + ChatColor.WHITE + g.getMaxPages());
            int disc = (int) Math.round(g.getUpgradeDiscount() * 100);
            lore.add(ChatColor.GRAY + "Upgrade Discount: " + ChatColor.GREEN + disc + "%");
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
        if (slot == CAMERA_SLOT) {
            if (manager.hasPermission(player.getUniqueId(), GuildPermission.ACCEPT_APPLICANTS)) {
                applicantsGUI.open(player);
            }
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
        if (slot == MOTD_SLOT && manager.hasPermission(player.getUniqueId(), GuildPermission.CHANGE_MOTD)) {
            awaitingMotd.add(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Enter new MOTD or 'cancel'.");
            return;
        }
        if (slot == SORT_SLOT) {
            int m = sortModes.getOrDefault(player.getUniqueId(), 0);
            if (e.getClick() == ClickType.RIGHT) {
                m = (m + 3 - 1) % 3;
            } else {
                m = (m + 1) % 3;
            }
            sortModes.put(player.getUniqueId(), m);
            open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
            return;
        }
        if (slot == VAULT_SLOT) {
            if (manager.hasPermission(player.getUniqueId(), GuildPermission.VAULT_ACCESS)) {
                Guild g = manager.getGuild(player.getUniqueId());
                if (g != null) {
                    me.nakilex.levelplugin.Main.getInstance().getGuildVaultManager().getVault(g.getName()).open(player);
                }
            }
            return;
        }
        if (slot == SETTINGS_SLOT) {
            Guild g = manager.getGuild(player.getUniqueId());
            if (g != null && g.getRole(player.getUniqueId()) == GuildRole.LEADER) {
                settingsGUI.open(player);
            }
            return;
        }
        if (slot == QUESTS_SLOT) {
            questGUI.open(player);
            return;
        }
        if (slot == REFRESH_SLOT) {
            open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
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
            return;
        }
        if (awaitingMotd.remove(id)) {
            e.setCancelled(true);
            String msg = e.getMessage();
            if (!msg.equalsIgnoreCase("cancel")) {
                Guild g = manager.getGuild(id);
                if (g != null && manager.hasPermission(id, GuildPermission.CHANGE_MOTD)) {
                    g.setMotd(msg);
                }
            }
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(e.getPlayer(), pageMap.getOrDefault(id, 0)));
        }
    }
}

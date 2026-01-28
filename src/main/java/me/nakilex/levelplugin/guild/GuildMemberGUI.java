package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.guild.quests.GuildQuestGUI;
import me.nakilex.levelplugin.guild.quests.GuildQuestManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
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
import me.nakilex.levelplugin.utils.TextUtil;

import java.util.*;

/**
 * Enhanced guild menu showing members with pagination, search and MOTD editing.
 */
public class GuildMemberGUI implements Listener {
    private final GuildManager manager;
    private final GuildGUI guildGUI;
    private final GuildApplicantsGUI applicantsGUI;
    private final GuildSettingsGUI settingsGUI;

    private static final int SIZE = 54;
    private static final String TITLE = "Guild Menu";

    private static final int[] MEMBER_SLOTS = GuiUtil.PAGED_SLOTS;
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
    private static final int INFO_SLOT   = 8;
    private static final int REFRESH_SLOT = 0;
    private static final int QUESTS_SLOT = 44;

    private final Map<UUID, Integer> pageMap = new HashMap<>();
    private final Map<UUID, String> searchTerms = new HashMap<>();
    private final Map<UUID, Integer> sortModes = new HashMap<>();
    private final Set<UUID> awaitingSearch = new HashSet<>();
    private final Set<UUID> awaitingMotd = new HashSet<>();
    private final Map<UUID, UUID> managingTargets = new HashMap<>();
    private final Map<UUID, PendingRoleChange> pendingRoleChanges = new HashMap<>();
    private final List<GuiWidget> widgets;
    private final ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);

    private static final String MANAGE_TITLE = "Manage Member";
    private static final int MANAGE_SIZE = 36;
    private static final int MANAGE_PROMOTE_SLOT = 11;
    private static final int MANAGE_HEAD_SLOT = 13;
    private static final int MANAGE_KICK_SLOT = 15;
    private static final int MANAGE_DEMOTE_SLOT = 20;
    private static final int MANAGE_BACK_SLOT = 35;

    private static final String CONFIRM_ROLE_TITLE = "Confirm Role Change";
    private static final int CONFIRM_ROLE_SIZE = 27;
    private static final int CONFIRM_ROLE_YES_SLOT = 11;
    private static final int CONFIRM_ROLE_NO_SLOT = 15;

    private static class PendingRoleChange {
        UUID target;
        GuildRole desired;
    }

    public GuildMemberGUI(GuildManager manager, GuildGUI guildGUI, GuildApplicantsGUI applicantsGUI, GuildSettingsGUI settingsGUI) {
        this.manager = manager;
        this.guildGUI = guildGUI;
        this.applicantsGUI = applicantsGUI;
        this.settingsGUI = settingsGUI;
        this.widgets = buildWidgets();
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
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        renderWidgets(inv, player);
        player.openInventory(inv);
    }

    private List<UUID> getFilteredMembers(Player player, Guild g) {
        String term = searchTerms.getOrDefault(player.getUniqueId(), "").toLowerCase();
        int sort = sortModes.getOrDefault(player.getUniqueId(), 0);
        List<UUID> members = new ArrayList<>(g.getMembers());
        if (!term.isEmpty()) {
            members.removeIf(id -> {
                OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                String n = op.getName();
                return n == null || !n.toLowerCase().contains(term);
            });
        }

        Comparator<UUID> comp;
        switch (sort) {
            case 1 -> comp = Comparator.comparing((UUID id) -> Bukkit.getPlayer(id) == null)
                    .thenComparing(id -> {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                        String n = op.getName();
                        return n == null ? "" : n.toLowerCase();
                    });
            case 2 -> comp = Comparator.comparingInt((UUID id) ->
                    -LevelManager.getInstance().getLevel(id));
            default -> comp = Comparator.comparing(id -> {
                OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                String n = op.getName();
                return n == null ? "" : n.toLowerCase();
            });
        }
        members.sort(comp);
        return members;
    }

    private ItemStack createMemberItem(Guild guild, Player viewer, UUID memberId) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
        List<String> lore = new ArrayList<>();
        GuildRole role = guild.getRole(memberId);
        lore.add(ChatColor.GRAY + "Role: " + ChatColor.WHITE + TextUtil.beautifyWords(role.name()));
        int level = LevelManager.getInstance().getLevel(memberId);
        lore.add(ChatColor.GRAY + "Level: " + level);
        boolean online = Bukkit.getPlayer(memberId) != null;
        lore.add(online ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline");
        boolean canManage = manager.canManageMemberRole(viewer.getUniqueId(), memberId)
                || manager.hasPermission(viewer.getUniqueId(), GuildPermission.KICK);
        if (canManage) {
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to manage member", null));
        }
        return HeadUtil.createPlayerHead(op, ChatColor.YELLOW + op.getName(), lore);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        for (int i = 0; i < MEMBER_SLOTS.length; i++) {
            final int index = i;
            widgetList.add(new ActionWidget(MEMBER_SLOTS[i],
                    context -> createMemberItemForSlot(context, index),
                    (click, context) -> {
                        Guild guild = manager.getGuild(context.player().getUniqueId());
                        if (guild != null) {
                            handleMemberRoleClick(context.player(), guild, index);
                        }
                    }));
        }
        widgetList.add(new ActionWidget(PREV_SLOT, this::createPrevItem,
                (click, context) -> {
                    int page = pageMap.getOrDefault(context.player().getUniqueId(), 0);
                    open(context.player(), Math.max(0, page - 1));
                }));
        widgetList.add(new ActionWidget(NEXT_SLOT, this::createNextItem,
                (click, context) -> {
                    int page = pageMap.getOrDefault(context.player().getUniqueId(), 0);
                    open(context.player(), page + 1);
                }));
        widgetList.add(new ActionWidget(HOME_SLOT, this::createHomeInfoItem, null));
        widgetList.add(new ActionWidget(SEARCH_SLOT,
                context -> createSearchButton(searchTerms.getOrDefault(context.player().getUniqueId(), "")),
                (click, context) -> {
                    if (click == ClickType.RIGHT) {
                        searchTerms.remove(context.player().getUniqueId());
                        open(context.player(), pageMap.getOrDefault(context.player().getUniqueId(), 0));
                    } else {
                        awaitingSearch.add(context.player().getUniqueId());
                        context.player().closeInventory();
                        context.player().sendMessage(ChatColor.YELLOW + "Enter search term or 'cancel'.");
                    }
                }));
        widgetList.add(new ActionWidget(MOTD_SLOT,
                context -> {
                    Guild guild = manager.getGuild(context.player().getUniqueId());
                    return guild != null ? createMotdButton(guild, context.player()) : filler.clone();
                },
                (click, context) -> {
                    if (manager.hasPermission(context.player().getUniqueId(), GuildPermission.CHANGE_MOTD)) {
                        awaitingMotd.add(context.player().getUniqueId());
                        context.player().closeInventory();
                        context.player().sendMessage(ChatColor.YELLOW + "Enter new MOTD or 'cancel'.");
                    }
                }));
        widgetList.add(new ActionWidget(CAMERA_SLOT, this::createApplicantsButton,
                (click, context) -> {
                    if (manager.hasPermission(context.player().getUniqueId(), GuildPermission.ACCEPT_APPLICANTS)) {
                        applicantsGUI.open(context.player());
                    }
                }));
        widgetList.add(new ActionWidget(SORT_SLOT,
                context -> createSortButton(sortModes.getOrDefault(context.player().getUniqueId(), 0)),
                (click, context) -> {
                    int mode = sortModes.getOrDefault(context.player().getUniqueId(), 0);
                    mode = click == ClickType.RIGHT ? (mode + 2) % 3 : (mode + 1) % 3;
                    sortModes.put(context.player().getUniqueId(), mode);
                    open(context.player(), pageMap.getOrDefault(context.player().getUniqueId(), 0));
                }));
        widgetList.add(new ActionWidget(INFO_SLOT, context -> {
            Guild guild = manager.getGuild(context.player().getUniqueId());
            return guild != null ? createInfoButton(guild) : filler.clone();
        }, null));
        widgetList.add(new ActionWidget(REFRESH_SLOT,
                context -> GuiUtil.getNexoItem("refresh", ChatColor.RED + "Refresh"),
                (click, context) -> open(context.player(), pageMap.getOrDefault(context.player().getUniqueId(), 0))));
        widgetList.add(new ActionWidget(VAULT_SLOT,
                context -> GuiUtil.createGuiItem(Material.CHEST, ChatColor.GOLD + "Guild Storage", List.of()),
                (click, context) -> {
                    Guild guild = manager.getGuild(context.player().getUniqueId());
                    if (manager.hasPermission(context.player().getUniqueId(), GuildPermission.VAULT_ACCESS) && guild != null) {
                        Main.getInstance().getGuildVaultManager().getVault(guild.getName()).open(context.player());
                    }
                }));
        widgetList.add(new ActionWidget(SETTINGS_SLOT,
                context -> GuiUtil.getNexoItem("settings", ChatColor.AQUA + "Settings"),
                (click, context) -> {
                    Guild guild = manager.getGuild(context.player().getUniqueId());
                    if (guild != null && guild.getRole(context.player().getUniqueId()) == GuildRole.LEADER) {
                        settingsGUI.open(context.player());
                    }
                }));
        widgetList.add(new ActionWidget(QUESTS_SLOT,
                context -> GuiUtil.getNexoItem("pack1_scroll2", ChatColor.LIGHT_PURPLE + "Guild Quests"),
                (click, context) -> {
                    Guild guild = manager.getGuild(context.player().getUniqueId());
                    if (guild != null) {
                        GuildQuestManager.getInstance().ensureQuests(guild);
                        context.player().openInventory(GuildQuestGUI.create(context.player(), guild.getQuests()));
                    }
                }));
        return widgetList;
    }

    private void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private void handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return;
        }
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
    }

    private ItemStack createMemberItemForSlot(GuiContext context, int slotIndex) {
        Guild guild = manager.getGuild(context.player().getUniqueId());
        if (guild == null) {
            return filler.clone();
        }
        List<UUID> members = getFilteredMembers(context.player(), guild);
        int page = pageMap.getOrDefault(context.player().getUniqueId(), 0);
        int idx = page * ITEMS_PER_PAGE + slotIndex;
        if (idx >= members.size()) {
            return filler.clone();
        }
        return createMemberItem(guild, context.player(), members.get(idx));
    }

    private ItemStack createPrevItem(GuiContext context) {
        int page = pageMap.getOrDefault(context.player().getUniqueId(), 0);
        if (page > 0) {
            return GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous");
        }
        return filler.clone();
    }

    private ItemStack createNextItem(GuiContext context) {
        Guild guild = manager.getGuild(context.player().getUniqueId());
        if (guild == null) {
            return filler.clone();
        }
        List<UUID> members = getFilteredMembers(context.player(), guild);
        int page = pageMap.getOrDefault(context.player().getUniqueId(), 0);
        if (members.size() > (page + 1) * ITEMS_PER_PAGE) {
            return GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next");
        }
        return filler.clone();
    }

    private ItemStack createHomeInfoItem(GuiContext context) {
        Guild guild = manager.getGuild(context.player().getUniqueId());
        if (guild == null) {
            return filler.clone();
        }
        ItemStack infoItem = GuiUtil.getNexoItem("home", ChatColor.YELLOW + "Guild Info");
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Leader: " + ChatColor.WHITE + guild.getLeaderName());
            lore.add(ChatColor.GRAY + "Members: " + ChatColor.WHITE + guild.getMembers().size() + ChatColor.GRAY + "/" + ChatColor.WHITE + guild.getMaxMembers());
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + guild.getLevel());
            int need = guild.getExpNeeded();
            if (need > 0) {
                int cur = guild.getExp();
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
        return infoItem;
    }

    private ItemStack createApplicantsButton(GuiContext context) {
        Guild guild = manager.getGuild(context.player().getUniqueId());
        if (guild == null) {
            return filler.clone();
        }
        if (guild.getLeader().equals(context.player().getUniqueId())) {
            ItemStack camItem = GuiUtil.getNexoItem("camera", ChatColor.YELLOW + "Applicants");
            ItemMeta meta = camItem.getItemMeta();
            if (meta != null) {
                int count = guild.getApplicants().size();
                List<String> lore = new ArrayList<>();
                if (count > 0) {
                    lore.add(ChatColor.GRAY + "Pending: " + ChatColor.WHITE + count);
                } else {
                    lore.add(ChatColor.GRAY + "No pending applications");
                }
                meta.setLore(lore);
                camItem.setItemMeta(meta);
            }
            return camItem;
        }
        return GuiUtil.getNexoItem("camera", ChatColor.YELLOW + "Coming Soon");
    }

    private void handleMemberRoleClick(Player viewer, Guild g, int memberSlotIndex) {
        List<UUID> members = getFilteredMembers(viewer, g);
        int page = pageMap.getOrDefault(viewer.getUniqueId(), 0);
        int idx = page * ITEMS_PER_PAGE + memberSlotIndex;
        if (idx >= members.size()) return;
        UUID target = members.get(idx);
        boolean canManageRoles = manager.canManageMemberRole(viewer.getUniqueId(), target);
        boolean canKick = manager.hasPermission(viewer.getUniqueId(), GuildPermission.KICK) && !viewer.getUniqueId().equals(target);
        if (!canManageRoles && !canKick) return;
        managingTargets.put(viewer.getUniqueId(), target);
        openManage(viewer, g, target);
    }

    private GuildRole promoteRole(GuildRole role) {
        return switch (role) {
            case MEMBER -> GuildRole.VETERAN;
            case VETERAN -> GuildRole.ADVISOR;
            case ADVISOR, LEADER -> role;
        };
    }

    private GuildRole demoteRole(GuildRole role) {
        return switch (role) {
            case LEADER -> GuildRole.ADVISOR;
            case ADVISOR -> GuildRole.VETERAN;
            case VETERAN -> GuildRole.MEMBER;
            case MEMBER -> role;
        };
    }

    private ItemStack createRoleButton(String label, GuildRole targetRole, boolean allowed) {
        ChatColor color = allowed ? ChatColor.GREEN : ChatColor.RED;
        String name = allowed && targetRole != null
                ? ChatColor.WHITE + TextUtil.beautifyWords(targetRole.name())
                : ChatColor.DARK_GRAY + "Unavailable";
        ItemStack button = GuiUtil.getNexoItem(allowed ? "plus" : "cross", color + label);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (targetRole != null) {
                lore.add(ChatColor.GRAY + "Change to " + ChatColor.WHITE + TextUtil.beautifyWords(targetRole.name()));
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to confirm", null));
            meta.setLore(lore);
            meta.setLocalizedName(name);
            button.setItemMeta(meta);
        }
        return button;
    }

    private void openManage(Player viewer, Guild guild, UUID target) {
        Inventory inv = GuiBuilder.create(MANAGE_SIZE, MANAGE_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        GuildRole role = guild.getRole(target);
        List<String> headLore = new ArrayList<>();
        headLore.add(ChatColor.GRAY + "Role: " + ChatColor.WHITE + TextUtil.beautifyWords(role.name()));
        headLore.add(ChatColor.GRAY + "Level: " + LevelManager.getInstance().getLevel(target));
        inv.setItem(MANAGE_HEAD_SLOT, HeadUtil.createPlayerHead(op, ChatColor.YELLOW + op.getName(), headLore));

        boolean canManageRole = manager.canManageMemberRole(viewer.getUniqueId(), target);
        boolean canKick = manager.hasPermission(viewer.getUniqueId(), GuildPermission.KICK)
                && !viewer.getUniqueId().equals(target);
        GuildRole promote = promoteRole(role);
        GuildRole demote = demoteRole(role);

        inv.setItem(MANAGE_PROMOTE_SLOT, createRoleButton("Promote", promote, canManageRole && promote != role));
        inv.setItem(MANAGE_DEMOTE_SLOT, createRoleButton("Demote", demote, canManageRole && demote != role));

        ItemStack kick = GuiUtil.getNexoItem(canKick ? "cross" : "info", ChatColor.RED + "Kick from Guild");
        ItemMeta kickMeta = kick.getItemMeta();
        if (kickMeta != null) {
            List<String> lore = new ArrayList<>();
            if (canKick) {
                lore.add(ChatColor.GRAY + "Remove this member from the guild.");
                lore.add(" ");
                lore.addAll(TooltipUtil.clickInstructions("to confirm", null));
            } else {
                lore.add(ChatColor.DARK_GRAY + "You cannot kick this member.");
            }
            kickMeta.setLore(lore);
            kick.setItemMeta(kickMeta);
        }
        inv.setItem(MANAGE_KICK_SLOT, kick);
        inv.setItem(MANAGE_BACK_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));
        viewer.openInventory(inv);
    }

    private void openRoleConfirm(Player viewer, GuildRole desired) {
        UUID viewerId = viewer.getUniqueId();
        UUID target = managingTargets.get(viewerId);
        if (desired == null || target == null) return;
        Inventory inv = GuiBuilder.create(CONFIRM_ROLE_SIZE, CONFIRM_ROLE_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        inv.setItem(CONFIRM_ROLE_YES_SLOT, GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm"));
        inv.setItem(13, HeadUtil.createPlayerHead(op, ChatColor.YELLOW + op.getName(),
                TooltipUtil.bulletList("Change to " + TextUtil.beautifyWords(desired.name()))));
        inv.setItem(CONFIRM_ROLE_NO_SLOT, GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel"));
        PendingRoleChange change = new PendingRoleChange();
        change.target = target;
        change.desired = desired;
        pendingRoleChanges.put(viewerId, change);
        viewer.openInventory(inv);
    }

    private ItemStack createSearchButton(String term) {
        ItemStack it = GuiUtil.getNexoItem("search", ChatColor.GOLD + "Search");
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (!term.isEmpty()) {
                lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + term);
                lore.addAll(TooltipUtil.clickInstructions(null, "to clear"));
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
                lore.addAll(TooltipUtil.clickInstructions("to edit", null));
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
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
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
        String title = ChatColor.stripColor(e.getView().getTitle());
        Player player = (Player) e.getWhoClicked();
        if (title.equals(ChatColor.stripColor(TITLE))) {
            e.setCancelled(true);
            handleWidgetClick(e, player);
            return;
        }

        if (title.equals(ChatColor.stripColor(MANAGE_TITLE))) {
            e.setCancelled(true);
            UUID target = managingTargets.get(player.getUniqueId());
            Guild guild = manager.getGuild(player.getUniqueId());
            if (target == null || guild == null || !guild.getMembers().contains(target)) {
                player.closeInventory();
                return;
            }
            int slot = e.getRawSlot();
            GuildRole current = guild.getRole(target);
            if (slot == MANAGE_BACK_SLOT) {
                open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
                return;
            }
            if (slot == MANAGE_PROMOTE_SLOT) {
                GuildRole next = promoteRole(current);
                if (next == current || next == null) return;
                if (!manager.canManageMemberRole(player.getUniqueId(), target)) {
                    player.sendMessage(ChatColor.RED + "You cannot promote this member.");
                    return;
                }
                openRoleConfirm(player, next);
                return;
            }
            if (slot == MANAGE_DEMOTE_SLOT) {
                GuildRole next = demoteRole(current);
                if (next == current || next == null) return;
                if (!manager.canManageMemberRole(player.getUniqueId(), target)) {
                    player.sendMessage(ChatColor.RED + "You cannot demote this member.");
                    return;
                }
                openRoleConfirm(player, next);
                return;
            }
            if (slot == MANAGE_KICK_SLOT) {
                if (manager.hasPermission(player.getUniqueId(), GuildPermission.KICK)
                        && !player.getUniqueId().equals(target)) {
                    if (manager.removeMember(player.getUniqueId(), target)) {
                        String name = Bukkit.getOfflinePlayer(target).getName();
                        player.sendMessage(ChatColor.RED + "Kicked " + ChatColor.YELLOW + name + ChatColor.RED + " from the guild.");
                        managingTargets.remove(player.getUniqueId());
                        open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
                    }
                }
            }
            return;
        }

        if (title.equals(ChatColor.stripColor(CONFIRM_ROLE_TITLE))) {
            e.setCancelled(true);
            PendingRoleChange change = pendingRoleChanges.get(player.getUniqueId());
            if (change == null) return;
            if (e.getRawSlot() == CONFIRM_ROLE_YES_SLOT) {
                if (manager.changeRole(player.getUniqueId(), change.target, change.desired)) {
                    String targetName = Bukkit.getOfflinePlayer(change.target).getName();
                    player.sendMessage(ChatColor.GREEN + "Updated role for " + ChatColor.YELLOW + targetName
                            + ChatColor.GREEN + " to " + ChatColor.WHITE + TextUtil.beautifyWords(change.desired.name()) + ChatColor.GREEN + ".");
                } else {
                    player.sendMessage(ChatColor.RED + "You cannot change that member's role.");
                }
                pendingRoleChanges.remove(player.getUniqueId());
                Guild g = manager.getGuild(player.getUniqueId());
                if (g != null && managingTargets.containsKey(player.getUniqueId())) {
                    openManage(player, g, managingTargets.get(player.getUniqueId()));
                } else {
                    open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
                }
            } else if (e.getRawSlot() == CONFIRM_ROLE_NO_SLOT) {
                pendingRoleChanges.remove(player.getUniqueId());
                Guild g = manager.getGuild(player.getUniqueId());
                if (g != null && managingTargets.containsKey(player.getUniqueId())) {
                    openManage(player, g, managingTargets.get(player.getUniqueId()));
                } else {
                    open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
                }
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

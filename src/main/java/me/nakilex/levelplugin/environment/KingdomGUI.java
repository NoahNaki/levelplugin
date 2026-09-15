package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Central dashboard for creating, entering, inspecting, sharing, visiting, and resetting kingdoms. */
public final class KingdomGUI implements Listener {
    private static final String MAIN_TITLE = "Kingdom Management";
    private static final String BUILDINGS_TITLE = "Kingdom Buildings";
    private static final String MEMBERS_TITLE = "Kingdom Members";
    private static final String INVITE_TITLE = "Invite Kingdom Member";
    private static final String VISIT_TITLE = "Visit a Kingdom";
    private static final String DELETE_TITLE = "Delete Kingdom?";
    private static final String REBUILD_TITLE = "Rebuild Kingdom?";
    private static final String KICK_TITLE = "Remove Kingdom Member?";

    private static final int INFO_SLOT = 4;
    private static final int PRIMARY_SLOT = 20;
    private static final int BUILDINGS_SLOT = 22;
    private static final int MEMBERS_SLOT = 24;
    private static final int VISIT_SLOT = 30;
    private static final int REBUILD_SLOT = 32;
    private static final int DELETE_SLOT = 40;
    private static final int BACK_SLOT = 45;
    private static final int FOOTER_INFO_SLOT = 49;
    private static final int REFRESH_SLOT = 53;
    private static final int[] BUILDING_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private final EnvironmentAreaInstanceManager manager;
    private final NamespacedKey ownerKey;
    private final Map<UUID, UUID> pendingKick = new HashMap<>();

    public KingdomGUI(Main plugin, EnvironmentAreaInstanceManager manager) {
        this.manager = manager;
        this.ownerKey = new NamespacedKey(plugin, "kingdom_gui_owner");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        boolean active = manager.hasAccessibleKingdom(player.getUniqueId());
        boolean preparing = manager.isInitializing(player.getUniqueId());
        boolean owner = manager.hasSession(player.getUniqueId());
        Inventory inventory = menu(45, MAIN_TITLE);

        UUID ownerId = active ? manager.resolveAreaOwner(player.getUniqueId()) : player.getUniqueId();
        OfflinePlayer kingdomOwner = Bukkit.getOfflinePlayer(ownerId);
        List<EnvironmentAreaInstanceManager.KingdomBuildingSnapshot> buildings = manager.getKingdomBuildings(player);
        long built = buildings.stream().filter(EnvironmentAreaInstanceManager.KingdomBuildingSnapshot::built).count();
        String partner = active ? manager.getDebugCoopPartnerName(ownerId) : null;
        List<String> summary = new ArrayList<>();
        summary.add(ChatColor.GRAY + "Owner: " + ChatColor.WHITE + safeName(kingdomOwner));
        summary.add(ChatColor.GRAY + "Status: " + (active ? ChatColor.GREEN + "Active"
                : preparing ? ChatColor.YELLOW + "Preparing" : ChatColor.RED + "Not active"));
        summary.add(ChatColor.GRAY + "Buildings: " + ChatColor.WHITE + built + ChatColor.DARK_GRAY + "/" + buildings.size());
        summary.add(ChatColor.GRAY + "Members: " + ChatColor.WHITE + (partner == null ? 1 : 2));
        if (!owner && active) summary.add(ChatColor.AQUA + "You are a co-op member.");
        inventory.setItem(INFO_SLOT, HeadUtil.createPlayerHead(kingdomOwner,
                ChatColor.GOLD + safeName(kingdomOwner) + "'s Kingdom", summary));

        if (active) {
            inventory.setItem(PRIMARY_SLOT, GuiUtil.getNexoItem("home", ChatColor.GREEN + "Enter Kingdom",
                    TooltipUtil.clickInstructions("to teleport home", null)));
        } else if (preparing) {
            inventory.setItem(PRIMARY_SLOT, GuiUtil.getNexoItem("refresh", ChatColor.YELLOW + "Preparing Kingdom",
                    List.of(ChatColor.GRAY + "Your kingdom is currently being generated.", "",
                            TooltipUtil.leftClickLine("to refresh status"))));
        } else {
            List<String> createLore = new ArrayList<>();
            createLore.add(ChatColor.GRAY + "Generate your personal kingdom.");
            createLore.add("");
            createLore.addAll(TooltipUtil.clickInstructions("to create", null));
            inventory.setItem(PRIMARY_SLOT, GuiUtil.getNexoItem("plus", ChatColor.GREEN + "Create Kingdom", createLore));
        }
        inventory.setItem(BUILDINGS_SLOT, active
                ? GuiUtil.getNexoItem("settings", ChatColor.GOLD + "Buildings",
                List.of(ChatColor.GRAY + "Inspect every building and its level.", "",
                        TooltipUtil.leftClickLine("to browse")))
                : locked("Buildings", "Create your kingdom first."));
        inventory.setItem(MEMBERS_SLOT, active
                ? GuiUtil.getNexoItem("server_icon", ChatColor.AQUA + "Members",
                List.of(ChatColor.GRAY + "View, invite, or remove members.", "",
                        TooltipUtil.leftClickLine("to manage")))
                : locked("Members", "Create your kingdom first."));
        inventory.setItem(VISIT_SLOT, GuiUtil.getNexoItem("search", ChatColor.YELLOW + "Visit Kingdoms",
                List.of(ChatColor.GRAY + "Browse kingdoms that are online now.", "",
                        TooltipUtil.leftClickLine("to browse"))));
        inventory.setItem(REBUILD_SLOT, active && owner
                ? GuiUtil.getNexoItem("refresh", ChatColor.YELLOW + "Rebuild Instance",
                List.of(ChatColor.GRAY + "Reload the kingdom world while keeping", ChatColor.GRAY + "all saved building progress.", "",
                        TooltipUtil.leftClickLine("to continue")))
                : locked("Rebuild Instance", active ? "Only the owner can rebuild." : "Create your kingdom first."));
        inventory.setItem(DELETE_SLOT, active && owner
                ? GuiUtil.getNexoItem("cross", ChatColor.RED + "Delete Kingdom",
                List.of(ChatColor.GRAY + "Permanently reset all building progress", ChatColor.GRAY + "for your active profile.", "",
                        ChatColor.RED + "This cannot be undone.", TooltipUtil.leftClickLine("to continue")))
                : locked("Delete Kingdom", active ? "Only the owner can delete it." : "No active kingdom to delete."));
        player.openInventory(inventory);
    }

    public void openBuildings(Player player) {
        if (!requireKingdom(player)) return;
        Inventory inventory = menu(54, BUILDINGS_TITLE);
        List<EnvironmentAreaInstanceManager.KingdomBuildingSnapshot> buildings = manager.getKingdomBuildings(player);
        for (int index = 0; index < buildings.size() && index < BUILDING_SLOTS.length; index++) {
            EnvironmentAreaInstanceManager.KingdomBuildingSnapshot building = buildings.get(index);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Status: " + (building.built() ? ChatColor.GREEN + "Built" : ChatColor.RED + "Not built"));
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.WHITE + building.level() + ChatColor.DARK_GRAY + "/" + building.maxLevel());
            lore.add(ChatColor.GRAY + GuiUtil.createProgressBar(
                    building.maxLevel() <= 0 ? 0.0 : building.level() / (double) building.maxLevel(), 18));
            if (building.finishAtMs() > System.currentTimeMillis()) {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Construction: " + ChatColor.WHITE
                        + formatDuration((building.finishAtMs() - System.currentTimeMillis()) / 1000L));
            }
            lore.add("");
            lore.add(TooltipUtil.leftClickLine("to visit its build marker"));
            inventory.setItem(BUILDING_SLOTS[index], GuiUtil.createGuiItem(building.icon(),
                    (building.built() ? ChatColor.GREEN : ChatColor.RED) + building.displayName(), lore));
        }
        long built = buildings.stream().filter(EnvironmentAreaInstanceManager.KingdomBuildingSnapshot::built).count();
        inventory.setItem(BACK_SLOT, backButton());
        inventory.setItem(FOOTER_INFO_SLOT, GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Building Overview",
                List.of(ChatColor.GRAY + "Built: " + ChatColor.WHITE + built + ChatColor.DARK_GRAY + "/" + buildings.size(),
                        ChatColor.GRAY + "Upgrades are started at the building", ChatColor.GRAY + "hologram inside your kingdom.")));
        inventory.setItem(REFRESH_SLOT, refreshButton());
        player.openInventory(inventory);
    }

    public void openMembers(Player player) {
        if (!requireKingdom(player)) return;
        UUID ownerId = manager.resolveAreaOwner(player.getUniqueId());
        boolean isOwner = ownerId.equals(player.getUniqueId());
        UUID partnerId = manager.getKingdomPartner(ownerId);
        Inventory inventory = menu(45, MEMBERS_TITLE);
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
        inventory.setItem(20, HeadUtil.createPlayerHead(owner, ChatColor.GOLD + safeName(owner),
                List.of(ChatColor.GRAY + "Role: " + ChatColor.GOLD + "Kingdom Owner",
                        ChatColor.GRAY + "Status: " + onlineStatus(owner))));
        if (partnerId != null) {
            OfflinePlayer partner = Bukkit.getOfflinePlayer(partnerId);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Role: " + ChatColor.AQUA + "Co-op Member");
            lore.add(ChatColor.GRAY + "Status: " + onlineStatus(partner));
            if (isOwner) {
                lore.add("");
                lore.add(TooltipUtil.leftClickLine("to remove member"));
            }
            inventory.setItem(24, playerHead(partner, ChatColor.AQUA + safeName(partner), lore, ownerKey));
        } else {
            inventory.setItem(24, GuiUtil.getNexoItem("plus", ChatColor.GREEN + "Invite a Member",
                    isOwner ? TooltipUtil.clickInstructions("to choose a player", null)
                            : List.of(ChatColor.GRAY + "Only the owner can invite members.")));
        }
        inventory.setItem(31, isOwner && partnerId == null
                ? GuiUtil.getNexoItem("plus", ChatColor.GREEN + "Invite Player",
                TooltipUtil.clickInstructions("to browse online players", null))
                : GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Membership",
                List.of(ChatColor.GRAY + "A kingdom currently supports one owner", ChatColor.GRAY + "and one co-op member.")));
        inventory.setItem(36, backButton());
        inventory.setItem(44, refreshButton());
        player.openInventory(inventory);
    }

    public void openVisitBrowser(Player player) {
        Inventory inventory = menu(54, VISIT_TITLE);
        List<UUID> owners = new ArrayList<>(manager.getActiveKingdomOwners());
        owners.remove(manager.resolveAreaOwner(player.getUniqueId()));
        owners.sort(Comparator.comparing(id -> safeName(Bukkit.getOfflinePlayer(id)), String.CASE_INSENSITIVE_ORDER));
        int index = 0;
        for (UUID ownerId : owners) {
            if (index >= GuiUtil.PAGED_SLOTS.length) break;
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
            UUID partner = manager.getKingdomPartner(ownerId);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Owner: " + ChatColor.WHITE + safeName(owner));
            lore.add(ChatColor.GRAY + "Members: " + ChatColor.WHITE + (partner == null ? 1 : 2));
            lore.add("");
            lore.add(TooltipUtil.leftClickLine("to visit"));
            inventory.setItem(GuiUtil.PAGED_SLOTS[index++],
                    playerHead(owner, ChatColor.GREEN + safeName(owner) + "'s Kingdom", lore, ownerKey));
        }
        if (owners.isEmpty()) {
            inventory.setItem(22, GuiUtil.getNexoItem("info", ChatColor.YELLOW + "No Kingdoms Online",
                    List.of(ChatColor.GRAY + "There are no other active kingdoms", ChatColor.GRAY + "available to visit right now.")));
        }
        inventory.setItem(BACK_SLOT, backButton());
        inventory.setItem(FOOTER_INFO_SLOT, GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Kingdom Browser",
                List.of(ChatColor.GRAY + "Only currently active kingdoms appear here.")));
        inventory.setItem(REFRESH_SLOT, refreshButton());
        player.openInventory(inventory);
    }

    public void openDeleteConfirmation(Player player) {
        if (!manager.hasSession(player.getUniqueId())
                || !manager.hasAccessibleKingdom(player.getUniqueId())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "You do not own an active kingdom.");
            return;
        }
        Inventory inventory = menu(27, DELETE_TITLE);
        inventory.setItem(11, GuiUtil.getNexoItem("check", ChatColor.RED + "Permanently Delete",
                List.of(ChatColor.RED + "All buildings and building levels on", ChatColor.RED + "this profile will be reset.", "",
                        TooltipUtil.leftClickLine("to confirm"))));
        inventory.setItem(15, GuiUtil.getNexoItem("cross", ChatColor.GREEN + "Cancel",
                TooltipUtil.clickInstructions("to return", null)));
        player.openInventory(inventory);
    }

    private void openRebuildConfirmation(Player player) {
        if (!manager.hasSession(player.getUniqueId())
                || !manager.hasAccessibleKingdom(player.getUniqueId())) return;
        Inventory inventory = menu(27, REBUILD_TITLE);
        inventory.setItem(11, GuiUtil.getNexoItem("check", ChatColor.YELLOW + "Rebuild Instance",
                List.of(ChatColor.GRAY + "The world will be reloaded, but all", ChatColor.GRAY + "saved buildings and levels are kept.", "",
                        TooltipUtil.leftClickLine("to confirm"))));
        inventory.setItem(15, GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel",
                TooltipUtil.clickInstructions("to return", null)));
        player.openInventory(inventory);
    }

    private void openInviteBrowser(Player player) {
        if (!manager.hasSession(player.getUniqueId()) || manager.hasCoopPartner(player.getUniqueId())) {
            openMembers(player);
            return;
        }
        Inventory inventory = menu(54, INVITE_TITLE);
        List<? extends Player> candidates = Bukkit.getOnlinePlayers().stream()
                .filter(candidate -> !candidate.getUniqueId().equals(player.getUniqueId()))
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int index = 0;
        for (Player candidate : candidates) {
            if (index >= GuiUtil.PAGED_SLOTS.length) break;
            inventory.setItem(GuiUtil.PAGED_SLOTS[index++], playerHead(candidate,
                    ChatColor.GREEN + candidate.getName(),
                    List.of(ChatColor.GRAY + "Invite this player to your kingdom.", "",
                            TooltipUtil.leftClickLine("to invite")), ownerKey));
        }
        if (candidates.isEmpty()) {
            inventory.setItem(22, GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Nobody Available",
                    List.of(ChatColor.GRAY + "No other players are currently online.")));
        }
        inventory.setItem(BACK_SLOT, GuiUtil.getNexoItem("arrow_left2", ChatColor.YELLOW + "Back",
                TooltipUtil.clickInstructions("to return to members", null)));
        inventory.setItem(REFRESH_SLOT, refreshButton());
        player.openInventory(inventory);
    }

    private void openKickConfirmation(Player owner, UUID memberId) {
        OfflinePlayer member = Bukkit.getOfflinePlayer(memberId);
        pendingKick.put(owner.getUniqueId(), memberId);
        Inventory inventory = menu(27, KICK_TITLE);
        inventory.setItem(13, HeadUtil.createPlayerHead(member, ChatColor.RED + safeName(member),
                List.of(ChatColor.GRAY + "Remove this player from your kingdom?")));
        inventory.setItem(11, GuiUtil.getNexoItem("check", ChatColor.RED + "Remove Member",
                TooltipUtil.clickInstructions("to confirm", null)));
        inventory.setItem(15, GuiUtil.getNexoItem("cross", ChatColor.GREEN + "Cancel",
                TooltipUtil.clickInstructions("to return", null)));
        owner.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!isKingdomTitle(title)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;

        if (GuiUtil.titleMatches(title, MAIN_TITLE)) handleMainClick(player, slot);
        else if (GuiUtil.titleMatches(title, BUILDINGS_TITLE)) handleBuildingsClick(player, slot);
        else if (GuiUtil.titleMatches(title, MEMBERS_TITLE)) handleMembersClick(player, slot, event.getCurrentItem());
        else if (GuiUtil.titleMatches(title, INVITE_TITLE)) handleInviteClick(player, slot, event.getCurrentItem());
        else if (GuiUtil.titleMatches(title, VISIT_TITLE)) handleVisitClick(player, slot, event.getCurrentItem());
        else if (GuiUtil.titleMatches(title, DELETE_TITLE)) handleDeleteClick(player, slot);
        else if (GuiUtil.titleMatches(title, REBUILD_TITLE)) handleRebuildClick(player, slot);
        else if (GuiUtil.titleMatches(title, KICK_TITLE)) handleKickClick(player, slot);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (isKingdomTitle(event.getView().getTitle())) event.setCancelled(true);
    }

    private void handleMainClick(Player player, int slot) {
        if (slot == PRIMARY_SLOT) {
            if (manager.isInitializing(player.getUniqueId())) {
                open(player);
                return;
            }
            player.closeInventory();
            if (manager.hasAccessibleKingdom(player.getUniqueId())) {
                if (!manager.teleportToKingdom(player)) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Could not teleport to your kingdom.");
                }
            } else if (!manager.isInitializing(player.getUniqueId())) {
                manager.initialize(player);
            }
        } else if (slot == BUILDINGS_SLOT && manager.hasAccessibleKingdom(player.getUniqueId())) {
            openBuildings(player);
        } else if (slot == MEMBERS_SLOT && manager.hasAccessibleKingdom(player.getUniqueId())) {
            openMembers(player);
        } else if (slot == VISIT_SLOT) {
            openVisitBrowser(player);
        } else if (slot == REBUILD_SLOT && manager.hasSession(player.getUniqueId())) {
            openRebuildConfirmation(player);
        } else if (slot == DELETE_SLOT && manager.hasSession(player.getUniqueId())) {
            openDeleteConfirmation(player);
        }
    }

    private void handleBuildingsClick(Player player, int slot) {
        if (slot == BACK_SLOT) {
            open(player);
            return;
        }
        if (slot == REFRESH_SLOT) {
            openBuildings(player);
            return;
        }
        for (int index = 0; index < BUILDING_SLOTS.length; index++) {
            if (slot != BUILDING_SLOTS[index]) continue;
            List<EnvironmentAreaInstanceManager.KingdomBuildingSnapshot> buildings = manager.getKingdomBuildings(player);
            if (index >= buildings.size()) return;
            player.closeInventory();
            if (manager.teleportToBuilding(player, buildings.get(index).slot())) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        "Right-click the building hologram to build or upgrade it.");
            } else {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "That building is not available right now.");
            }
            return;
        }
    }

    private void handleMembersClick(Player player, int slot, ItemStack clicked) {
        UUID ownerId = manager.resolveAreaOwner(player.getUniqueId());
        if (slot == 36) open(player);
        else if (slot == 44) openMembers(player);
        else if ((slot == 24 || slot == 31) && manager.getKingdomPartner(ownerId) == null
                && ownerId.equals(player.getUniqueId())) {
            openInviteBrowser(player);
        } else if (slot == 24 && ownerId.equals(player.getUniqueId())) {
            UUID memberId = readOwner(clicked);
            if (memberId != null && memberId.equals(manager.getKingdomPartner(ownerId))) {
                openKickConfirmation(player, memberId);
            }
        }
    }

    private void handleInviteClick(Player player, int slot, ItemStack clicked) {
        if (slot == BACK_SLOT) {
            openMembers(player);
        } else if (slot == REFRESH_SLOT) {
            openInviteBrowser(player);
        } else {
            UUID targetId = readOwner(clicked);
            Player target = targetId == null ? null : Bukkit.getPlayer(targetId);
            if (target == null) return;
            player.closeInventory();
            manager.invite(player, target);
        }
    }

    private void handleVisitClick(Player player, int slot, ItemStack clicked) {
        if (slot == BACK_SLOT) {
            open(player);
        } else if (slot == REFRESH_SLOT) {
            openVisitBrowser(player);
        } else {
            UUID ownerId = readOwner(clicked);
            Player owner = ownerId == null ? null : Bukkit.getPlayer(ownerId);
            if (owner == null) return;
            player.closeInventory();
            manager.visit(player, owner);
        }
    }

    private void handleDeleteClick(Player player, int slot) {
        if (slot == 11) {
            player.closeInventory();
            manager.deleteKingdom(player);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Your kingdom and its saved building progress were deleted.");
        } else if (slot == 15) {
            open(player);
        }
    }

    private void handleRebuildClick(Player player, int slot) {
        if (slot == 11) {
            player.closeInventory();
            manager.removeKingdom(player.getUniqueId());
            manager.initialize(player);
        } else if (slot == 15) {
            open(player);
        }
    }

    private void handleKickClick(Player player, int slot) {
        if (slot == 15) {
            pendingKick.remove(player.getUniqueId());
            openMembers(player);
            return;
        }
        if (slot != 11) return;
        UUID memberId = pendingKick.remove(player.getUniqueId());
        Player member = memberId == null ? null : Bukkit.getPlayer(memberId);
        if (member == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "That member is no longer online.");
            openMembers(player);
            return;
        }
        manager.kick(player, member);
        openMembers(player);
    }

    private boolean requireKingdom(Player player) {
        if (manager.hasAccessibleKingdom(player.getUniqueId())) return true;
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "You do not have an active kingdom.");
        open(player);
        return false;
    }

    private Inventory menu(int size, String title) {
        return GuiBuilder.create(size, title)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
    }

    private ItemStack locked(String name, String reason) {
        return GuiUtil.getNexoItem("lock", ChatColor.DARK_GRAY + name,
                List.of(ChatColor.GRAY + reason));
    }

    private ItemStack backButton() {
        return GuiUtil.getNexoItem("arrow_left2", ChatColor.YELLOW + "Back",
                TooltipUtil.clickInstructions("to return", null));
    }

    private ItemStack refreshButton() {
        return GuiUtil.getNexoItem("refresh", ChatColor.GREEN + "Refresh",
                TooltipUtil.clickInstructions("to refresh", null));
    }

    private ItemStack playerHead(OfflinePlayer player, String name, List<String> lore, NamespacedKey key) {
        ItemStack head = HeadUtil.createPlayerHead(player, name, lore);
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, player.getUniqueId().toString());
            head.setItemMeta(meta);
        }
        return head;
    }

    private UUID readOwner(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String value = item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean isKingdomTitle(String title) {
        return GuiUtil.titleMatches(title, MAIN_TITLE)
                || GuiUtil.titleMatches(title, BUILDINGS_TITLE)
                || GuiUtil.titleMatches(title, MEMBERS_TITLE)
                || GuiUtil.titleMatches(title, INVITE_TITLE)
                || GuiUtil.titleMatches(title, VISIT_TITLE)
                || GuiUtil.titleMatches(title, DELETE_TITLE)
                || GuiUtil.titleMatches(title, REBUILD_TITLE)
                || GuiUtil.titleMatches(title, KICK_TITLE);
    }

    private String safeName(OfflinePlayer player) {
        String name = player == null ? null : player.getName();
        return name == null || name.isBlank() ? "Unknown Player" : name;
    }

    private String onlineStatus(OfflinePlayer player) {
        return player != null && player.isOnline() ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline";
    }

    private String formatDuration(long totalSeconds) {
        long seconds = Math.max(0L, totalSeconds);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainder = seconds % 60L;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m " + remainder + "s";
    }
}

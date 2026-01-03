package me.nakilex.levelplugin.player.profile;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.utils.BetterHudUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Simple GUI for selecting or creating character profiles.
 */
public class ProfileSelectionGUI implements Listener {
    private static final String TITLE = "Select Profile";
    private static final int SIZE = 27;
    private static final int[] PROFILE_SLOTS = {10, 12, 14, 16};
    private static final int LOGOUT_SLOT = 22;
    private static final ItemStack LOGOUT_ITEM;
    private static final String EDIT_TITLE = "Edit Profile";
    private static final int DELETE_SLOT = 11;
    private static final int BACK_SLOT = 15;
    private static final ItemStack DELETE_ITEM;

    private static final String CONFIRM_TITLE = "Confirm Delete";
    private static final int CONFIRM_YES_SLOT = 11;
    private static final int CONFIRM_NO_SLOT = 15;


    static {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Leave Server");
            barrier.setItemMeta(meta);
        }
        LOGOUT_ITEM = barrier;

        ItemStack cauldron = new ItemStack(Material.CAULDRON);
        ItemMeta cm = cauldron.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(ChatColor.RED.toString() + ChatColor.BOLD + "Delete Profile");
            cm.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "Mark this profile for permanent deletion.",
                    "",
                    ChatColor.RED + "Click to delete"
            ));
            cauldron.setItemMeta(cm);
        }
        DELETE_ITEM = cauldron;
    }

    private static final Map<UUID, Inventory> OPEN = new HashMap<>();
    private static final Map<UUID, Inventory> EDIT_OPEN = new HashMap<>();
    private static final Map<UUID, Inventory> CONFIRM_OPEN = new HashMap<>();
    private static final Set<UUID> SELECTING = new HashSet<>();
    private static final Set<UUID> NAMING = new HashSet<>();
    private static final Map<UUID, Integer> PENDING_SLOT = new HashMap<>();
    // When the very first profile is created, store the slot so the
    // introductory quest can start once that profile is selected.
    private static final Map<UUID, Integer> FIRST_PROFILE_SLOT = new HashMap<>();

    private static void hideOthers(Player player) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) player.hidePlayer(Main.getInstance(), p);
        }
    }

    private static void showOthers(Player player) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) player.showPlayer(Main.getInstance(), p);
        }
    }

    public static boolean isSelecting(Player p) {
        return SELECTING.contains(p.getUniqueId());
    }

    /**
     * Begin the profile selection process for a player. The GUI will
     * automatically reopen if closed until a profile is selected.
     */
    public static void startSelection(Player player) {
        SELECTING.add(player.getUniqueId());
        hideOthers(player);
        BetterHudUtil.removeHud(player);
        var sbManager = Main.getInstance().getScoreboardManager();
        if (sbManager != null) sbManager.removeBoard(player);

        // Save state from the currently active profile, if any
        ProfileManager pm = ProfileManager.getInstance();
        Integer slot = pm.getActiveSlot(player.getUniqueId());
        if (slot != null) {
            pm.saveActiveProfile(player);
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }

        me.nakilex.levelplugin.items.listeners.StaticItemListener.giveStaticItems(player);
        open(player);
    }

    private static void stopSelection(Player player) {
        SELECTING.remove(player.getUniqueId());
        showOthers(player);
        Main.getInstance().getPlayerVisibilityManager().apply(player);
        var sbManager = Main.getInstance().getScoreboardManager();
        if (sbManager != null) sbManager.createBoard(player);
    }

    /** Called when a player quits to clear any temporary state. */
    public static void handleQuit(Player player) {
        UUID id = player.getUniqueId();
        SELECTING.remove(id);
        NAMING.remove(id);
        OPEN.remove(id);
        EDIT_OPEN.remove(id);
        CONFIRM_OPEN.remove(id);
        PENDING_SLOT.remove(id);
        FIRST_PROFILE_SLOT.remove(id);
    }

    public static void open(Player player) {
        ProfileManager pm = ProfileManager.getInstance();

        // When switching profiles via the command, persist the current profile
        // state before showing the selection menu. startSelection() already
        // handles this, so only run here if the player isn't in selection mode.
        if (!SELECTING.contains(player.getUniqueId())) {
            Integer slot = pm.getActiveSlot(player.getUniqueId());
            if (slot != null) {
                pm.saveActiveProfile(player);
            }
        }

        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();

        int unlocked = pm.getUnlockedSlots(player.getUniqueId());
        List<PlayerProfile> list = pm.getProfiles(player.getUniqueId());

        for (int i = 0; i < PROFILE_SLOTS.length; i++) {
            int slot = PROFILE_SLOTS[i];
            if (i >= unlocked) {
                inv.setItem(slot, GuiUtil.getNexoItem("lock", ChatColor.RED + "Locked"));
                continue;
            }
            PlayerProfile prof = list.get(i);
            if (prof == null) {
                inv.setItem(slot,
                        GuiUtil.getNexoItem("plus", ChatColor.GREEN + "[+] Create character"));
            } else {
                inv.setItem(slot, createProfileItem(player, prof));
            }
        }

        // add logout button
        inv.setItem(LOGOUT_SLOT, LOGOUT_ITEM);

        OPEN.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    private static void openEdit(Player player, int slotIndex) {
        Inventory inv = GuiBuilder.create(SIZE, EDIT_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        inv.setItem(DELETE_SLOT, DELETE_ITEM);
        inv.setItem(BACK_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.GRAY + "Back"));
        EDIT_OPEN.put(player.getUniqueId(), inv);
        PENDING_SLOT.put(player.getUniqueId(), slotIndex);
        player.openInventory(inv);
    }

    private static void openConfirmDelete(Player player, int slotIndex) {
        Inventory inv = GuiBuilder.create(SIZE, CONFIRM_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        // Build the confirm items dynamically so Nexo is already initialized
        ItemStack confirm = GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm");
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            List<String> lore = new ArrayList<>();
            me.nakilex.levelplugin.guild.Guild guild =
                    me.nakilex.levelplugin.guild.GuildManager.getInstance().getGuild(player.getUniqueId());
            if (guild != null && player.getUniqueId().equals(guild.getLeader())) {
                lore.add(ChatColor.YELLOW + "Warning:");
                lore.addAll(TooltipUtil.bulletList(
                        "Transfer guild leadership or disband your guild before deleting this profile."));
            }
            confirmMeta.setLore(lore);
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(CONFIRM_YES_SLOT, confirm);
        inv.setItem(CONFIRM_NO_SLOT,
                GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel"));
        // The edit menu closes when this opens; remove the reference so the
        // close handler doesn't reopen the main menu before the confirm GUI
        // appears.
        EDIT_OPEN.remove(player.getUniqueId());
        CONFIRM_OPEN.put(player.getUniqueId(), inv);
        PENDING_SLOT.put(player.getUniqueId(), slotIndex);
        player.openInventory(inv);
    }

    private static ItemStack createProfileItem(Player player, PlayerProfile profile) {
        ItemStack item = GuiUtil.getNexoItem("save", ChatColor.YELLOW + profile.getName());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            // blank divider so the name is visually separated from stats
            lore.add("");

            me.nakilex.levelplugin.player.level.managers.LevelManager lm =
                    me.nakilex.levelplugin.player.level.managers.LevelManager.getInstance();
            me.nakilex.levelplugin.player.attributes.managers.StatsManager sm =
                    me.nakilex.levelplugin.player.attributes.managers.StatsManager.getInstance();
            me.nakilex.levelplugin.quests.managers.QuestManager qm =
                    me.nakilex.levelplugin.Main.getInstance().getQuestManager();

            int level = lm.getLevel(player);
            int xp = lm.getXP(player);
            int needed = lm.getXpNeededForNextLevel(player);
            int pct = needed > 0 ? (int) Math.round((xp * 100.0) / needed) : 100;

            me.nakilex.levelplugin.player.classes.data.PlayerClass pc =
                    sm.getPlayerStats(player.getUniqueId()).playerClass;

            int completed = qm.getCompletedQuestCount(player.getUniqueId());
            int total = qm.getTotalQuestCount();

            int playMinutes = profile.getPlayMinutes();

            String className = pc.getDisplayName();

            lore.add(ChatColor.GRAY + "Level: " + ChatColor.WHITE + level);
            lore.add(ChatColor.GRAY + "XP: " + ChatColor.WHITE + pct + "%");
            lore.add(ChatColor.GRAY + "Class: " + ChatColor.WHITE + className);
            lore.add(ChatColor.GRAY + "Finished Quests: " + ChatColor.WHITE + completed + "/" + total);
            lore.add(ChatColor.GRAY + "Playtime: " + ChatColor.WHITE + playMinutes + "m");
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to select this profile", "to edit this profile"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory open = OPEN.get(player.getUniqueId());
        if (open == null || !e.getView().getTopInventory().equals(open)) return;
        e.setCancelled(true);
        for (int i = 0; i < PROFILE_SLOTS.length; i++) {
            if (e.getRawSlot() == PROFILE_SLOTS[i]) {
                if (e.isRightClick()) {
                    handleEdit(player, i);
                } else {
                    selectProfile(player, i);
                }
                return;
            }
        }
        if (e.getRawSlot() == LOGOUT_SLOT) {
            handleLogout(player);
        }
    }

    @EventHandler
    public void onEditClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory inv = EDIT_OPEN.get(player.getUniqueId());
        if (inv == null || !e.getView().getTopInventory().equals(inv)) return;
        e.setCancelled(true);
        int slotIndex = PENDING_SLOT.getOrDefault(player.getUniqueId(), -1);
        if (e.getRawSlot() == DELETE_SLOT) {
            openConfirmDelete(player, slotIndex);
            return;
        } else if (e.getRawSlot() == BACK_SLOT) {
            EDIT_OPEN.remove(player.getUniqueId());
            PENDING_SLOT.remove(player.getUniqueId());
            open(player);
            return;
        }
    }

    @EventHandler
    public void onConfirmClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory inv = CONFIRM_OPEN.get(player.getUniqueId());
        if (inv == null || !e.getView().getTopInventory().equals(inv)) return;
        e.setCancelled(true);
        int slotIndex = PENDING_SLOT.getOrDefault(player.getUniqueId(), -1);
        if (e.getRawSlot() == CONFIRM_YES_SLOT) {
            if (slotIndex >= 0) {
                ProfileManager pm = ProfileManager.getInstance();
                pm.deleteProfile(player, slotIndex);
                player.sendMessage(ChatColor.RED + "Profile deleted.");

                Integer active = pm.getActiveSlot(player.getUniqueId());
                if (active != null && active == slotIndex) {
                    pm.clearActiveSlot(player.getUniqueId());

                    // Teleport back to the lobby world before forcing profile
                    // selection again. Delay reopening slightly so the player
                    // lands on the ground first.
                    org.bukkit.World lobbyWorld = Bukkit.getWorld("world");
                    if (lobbyWorld != null) {
                        player.teleport(new org.bukkit.Location(lobbyWorld, 217, 6, 80));
                    }

                    Bukkit.getScheduler().runTaskLater(
                            Main.getInstance(),
                            () -> startSelection(player),
                            30L); // ~1.5 seconds
                }
            }
            player.closeInventory();
        } else if (e.getRawSlot() == CONFIRM_NO_SLOT) {
            openEdit(player, slotIndex);
        }
    }

    public static void selectProfile(Player player, int index) {
        ProfileManager pm = ProfileManager.getInstance();
        if (index >= pm.getUnlockedSlots(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "This profile is locked.");
            return;
        }
        List<PlayerProfile> existing = pm.getProfiles(player.getUniqueId());
        boolean firstCreation = existing.stream().allMatch(Objects::isNull);

        PlayerProfile prof = pm.getProfile(player.getUniqueId(), index);
        if (prof == null) {
            promptForName(player, index, firstCreation);
            return;
        }

        Integer active = pm.getActiveSlot(player.getUniqueId());
        if (active != null && active == index) {
            player.sendMessage(ChatColor.YELLOW + "Selected character " + prof.getName());
            stopSelection(player);
            player.closeInventory();
            BetterHudUtil.addHud(player);
            return;
        }

        // If this is the first profile the player ever created and
        // they are selecting it for the first time, start the intro quest.
        Integer pending = FIRST_PROFILE_SLOT.get(player.getUniqueId());
        if (pending != null && pending == index) {
            Main.getInstance().getQuestManager().startQuest(player, "officeerrands");
            FIRST_PROFILE_SLOT.remove(player.getUniqueId());
        }

        player.sendMessage(ChatColor.YELLOW + "Selected character " + prof.getName());
        pm.setActiveSlot(player.getUniqueId(), index);
        me.nakilex.levelplugin.player.config.PlayerConfig cfg = Main.getInstance().getPlayerConfig();
        org.bukkit.Location loc = cfg.getProfileLocation(player.getUniqueId(), index);
        if (loc != null) player.teleport(loc);

        // Start the introductory quest for brand new characters
        QuestManager qm = Main.getInstance().getQuestManager();
        if (!qm.hasCompleted(player.getUniqueId(), "officeerrands") &&
                qm.getProgress(player.getUniqueId(), "officeerrands") == null) {
            qm.startQuest(player, "officeerrands");
        }

        // Load inventory and armor for this profile
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        org.bukkit.inventory.ItemStack[] contents = cfg.getProfileInventory(player.getUniqueId(), index);
        org.bukkit.inventory.ItemStack[] armor = cfg.getProfileArmor(player.getUniqueId(), index);
        if (contents.length > 0) {
            player.getInventory().setContents(contents);
        } else {
            me.nakilex.levelplugin.items.listeners.StaticItemListener.giveStaticItems(player);
        }
        if (armor.length > 0) player.getInventory().setArmorContents(armor);
        me.nakilex.levelplugin.player.attributes.managers.StatsManager statsManager =
                me.nakilex.levelplugin.player.attributes.managers.StatsManager.getInstance();
        statsManager.recalcDerivedStats(player);
        me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats ps =
                statsManager.getPlayerStats(player.getUniqueId());
        player.setHealth(player.getMaxHealth());
        ps.currentMana = ps.maxMana;
        stopSelection(player);
        player.closeInventory();
        BetterHudUtil.addHud(player);

    }

    private void handleEdit(Player player, int index) {
        ProfileManager pm = ProfileManager.getInstance();
        PlayerProfile prof = pm.getProfile(player.getUniqueId(), index);
        if (prof == null) {
            player.sendMessage(ChatColor.RED + "No profile in this slot.");
            return;
        }
        openEdit(player, index);
    }

    private static void promptForName(Player player, int index, boolean firstCreation) {
        NAMING.add(player.getUniqueId());
        PENDING_SLOT.put(player.getUniqueId(), index);
        player.closeInventory();

        ConversationFactory factory = new ConversationFactory(Main.getInstance())
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        return ChatColor.GOLD + "Enter a name for this profile:";
                    }

                    @Override
                    public Prompt acceptInput(ConversationContext context, String input) {
                        if (input == null || input.trim().isEmpty()) {
                            player.sendMessage(ChatColor.RED + "Invalid name.");
                            return this;
                        }
                        ProfileManager pm = ProfileManager.getInstance();
                        pm.createProfile(player.getUniqueId(), index, input.trim());
                        player.getInventory().clear();
                        me.nakilex.levelplugin.items.listeners.StaticItemListener.giveStaticItems(player);
                        if (firstCreation) {
                            FIRST_PROFILE_SLOT.put(player.getUniqueId(), index);
                        }
                        return Prompt.END_OF_CONVERSATION;
                    }
                })
                .withLocalEcho(false)
                .addConversationAbandonedListener(event -> {
                    NAMING.remove(player.getUniqueId());
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player));
                });
        factory.buildConversation(player).begin();
    }

    private void handleLogout(Player player) {
        stopSelection(player);
        player.kickPlayer(ChatColor.YELLOW + "Disconnected");
    }

    private static boolean anyGuiOpen(UUID id) {
        return OPEN.containsKey(id) || EDIT_OPEN.containsKey(id) || CONFIRM_OPEN.containsKey(id);
    }

    private static void handlePostClose(Player player) {
        UUID id = player.getUniqueId();
        if (!SELECTING.contains(id) || NAMING.contains(id) || anyGuiOpen(id)) {
            return;
        }
        ProfileManager pm = ProfileManager.getInstance();
        if (pm.getActiveSlot(id) == null) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                if (player.isOnline() && SELECTING.contains(id)
                        && pm.getActiveSlot(id) == null && !anyGuiOpen(id)) {
                    open(player);
                }
            }, 40L);
        } else {
            stopSelection(player);
            BetterHudUtil.addHud(player);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        UUID id = player.getUniqueId();
        Inventory inv = e.getInventory();

        boolean handled = false;
        Inventory open = OPEN.get(id);
        if (open != null && inv.equals(open)) {
            OPEN.remove(id);
            handled = true;
        }

        Inventory edit = EDIT_OPEN.get(id);
        if (edit != null && inv.equals(edit)) {
            EDIT_OPEN.remove(id);
            PENDING_SLOT.remove(id);
            handled = true;
        }

        Inventory confirm = CONFIRM_OPEN.get(id);
        if (confirm != null && inv.equals(confirm)) {
            CONFIRM_OPEN.remove(id);
            PENDING_SLOT.remove(id);
            handled = true;
        }

        if (handled) {
            handlePostClose(player);
        }
    }

    @EventHandler
    public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (SELECTING.contains(id) && !NAMING.contains(id)) {
            e.setCancelled(true);
        }
    }
}

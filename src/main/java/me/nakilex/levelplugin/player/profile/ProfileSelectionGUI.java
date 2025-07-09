package me.nakilex.levelplugin.player.profile;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GuiUtil;
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
    private static final String TITLE = ChatColor.DARK_GREEN + "Select Profile";
    private static final int SIZE = 27;
    private static final int[] PROFILE_SLOTS = {10, 12, 14, 16};
    private static final ItemStack FILLER = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
    private static final int LOGOUT_SLOT = 22;
    private static final ItemStack LOGOUT_ITEM;
    private static final String EDIT_TITLE = ChatColor.DARK_GREEN + "Edit Profile";
    private static final int DELETE_SLOT = 11;
    private static final int BACK_SLOT = 15;
    private static final ItemStack DELETE_ITEM;

    private static final String CONFIRM_TITLE = ChatColor.RED + "Confirm Delete";
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
        // Allow flight temporarily so the anti-cheat doesn't kick the player
        // while they are frozen in midair waiting for the GUI to open.
        player.setAllowFlight(true);
        open(player);
    }

    private static void stopSelection(Player player) {
        SELECTING.remove(player.getUniqueId());
        showOthers(player);
        Main.getInstance().getPlayerVisibilityManager().apply(player);
        // Restore flight state to prevent unintended flying after the menu
        // closes.
        player.setAllowFlight(false);
    }

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, FILLER);

        ProfileManager pm = ProfileManager.getInstance();
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
                inv.setItem(slot, createProfileItem(prof));
            }
        }

        // add logout button
        inv.setItem(LOGOUT_SLOT, LOGOUT_ITEM);

        OPEN.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    private static void openEdit(Player player, int slotIndex) {
        Inventory inv = Bukkit.createInventory(null, SIZE, EDIT_TITLE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, FILLER);
        inv.setItem(DELETE_SLOT, DELETE_ITEM);
        inv.setItem(BACK_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.GRAY + "Back"));
        EDIT_OPEN.put(player.getUniqueId(), inv);
        PENDING_SLOT.put(player.getUniqueId(), slotIndex);
        player.openInventory(inv);
    }

    private static void openConfirmDelete(Player player, int slotIndex) {
        Inventory inv = Bukkit.createInventory(null, SIZE, CONFIRM_TITLE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, FILLER);
        // Build the confirm items dynamically so Nexo is already initialized
        inv.setItem(CONFIRM_YES_SLOT,
                GuiUtil.getNexoItem("check", ChatColor.GREEN + "Delete"));
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

    private static ItemStack createProfileItem(PlayerProfile profile) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + profile.getName());
            List<String> lore = new ArrayList<>();
            // blank divider so the name is visually separated from stats
            lore.add("");
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.WHITE + "1");
            lore.add(ChatColor.GRAY + "XP: " + ChatColor.WHITE + "0%");
            lore.add(ChatColor.GRAY + "Class: " + ChatColor.WHITE + "None");
            lore.add(ChatColor.GRAY + "Finished Quests: " + ChatColor.WHITE + "0/0");
            lore.add(ChatColor.GRAY + "Playtime: " + ChatColor.WHITE + "0m");
            lore.add("");
            lore.add(ChatColor.WHITE + "Left-click " + ChatColor.GRAY + "to select this profile");
            lore.add(ChatColor.WHITE + "Right-click " + ChatColor.GRAY + "to edit this profile");
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
                    handleSelect(player, i);
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
            player.closeInventory();
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
                pm.deleteProfile(player.getUniqueId(), slotIndex);
                player.sendMessage(ChatColor.RED + "Profile deleted.");

                Integer active = pm.getActiveSlot(player.getUniqueId());
                if (active != null && active == slotIndex) {
                    pm.clearActiveSlot(player.getUniqueId());
                    // Do not teleport back to the lobby. Instead, reopen the
                    // selection menu after a short delay so gravity can settle
                    // the player before movement is locked again.
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

    private void handleSelect(Player player, int index) {
        ProfileManager pm = ProfileManager.getInstance();
        if (index >= pm.getUnlockedSlots(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "This profile is locked.");
            return;
        }
        List<PlayerProfile> existing = pm.getProfiles(player.getUniqueId());
        boolean firstCreation = existing.stream().allMatch(Objects::isNull);

        Integer active = pm.getActiveSlot(player.getUniqueId());
        if (active != null && active == index) {
            player.sendMessage(ChatColor.RED + "You already have this profile selected!");
            return;
        }

        PlayerProfile prof = pm.getProfile(player.getUniqueId(), index);
        if (prof == null) {
            promptForName(player, index, firstCreation);
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
        org.bukkit.Location loc = Main.getInstance().getPlayerConfig()
                .getProfileLocation(player.getUniqueId(), index);
        if (loc != null) player.teleport(loc);
        stopSelection(player);
        player.closeInventory();
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

    private void promptForName(Player player, int index, boolean firstCreation) {
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

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        Inventory open = OPEN.get(id);
        if (open != null && e.getInventory().equals(open)) {
            OPEN.remove(id);
            Player p = (Player) e.getPlayer();
            if (SELECTING.contains(id) && !NAMING.contains(id)
                    && ProfileManager.getInstance().getActiveSlot(id) == null
                    && !EDIT_OPEN.containsKey(id)) {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    if (p.isOnline() && SELECTING.contains(id)
                            && ProfileManager.getInstance().getActiveSlot(id) == null
                            && !EDIT_OPEN.containsKey(id)) {
                        open(p);
                    }
                }, 40L);
            } else if (SELECTING.contains(id) && ProfileManager.getInstance().getActiveSlot(id) != null) {
                // Player closed the menu while a profile was already selected;
                // stop enforcing selection so they can continue playing.
                stopSelection(p);
            }
        }

        Inventory edit = EDIT_OPEN.get(id);
        if (edit != null && e.getInventory().equals(edit)) {
            EDIT_OPEN.remove(id);
            PENDING_SLOT.remove(id);
            if (SELECTING.contains(id) && !NAMING.contains(id)
                    && ProfileManager.getInstance().getActiveSlot(id) == null
                    && !CONFIRM_OPEN.containsKey(id)) {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> open((Player) e.getPlayer()), 1L);
            } else if (SELECTING.contains(id) && ProfileManager.getInstance().getActiveSlot(id) != null) {
                stopSelection((Player) e.getPlayer());
            }
            return;
        }

        Inventory confirm = CONFIRM_OPEN.get(id);
        if (confirm != null && e.getInventory().equals(confirm)) {
            CONFIRM_OPEN.remove(id);
            PENDING_SLOT.remove(id);
            if (SELECTING.contains(id) && !NAMING.contains(id)
                    && ProfileManager.getInstance().getActiveSlot(id) == null
                    && !EDIT_OPEN.containsKey(id)) {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> open((Player) e.getPlayer()), 1L);
            } else if (SELECTING.contains(id) && ProfileManager.getInstance().getActiveSlot(id) != null) {
                stopSelection((Player) e.getPlayer());
            }
        }
    }

    @EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (SELECTING.contains(id) && !NAMING.contains(id) && e.getFrom().distanceSquared(e.getTo()) > 0) {
            e.setTo(e.getFrom());
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

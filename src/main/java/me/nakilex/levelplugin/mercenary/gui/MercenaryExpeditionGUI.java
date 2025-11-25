package me.nakilex.levelplugin.mercenary.gui;

import me.nakilex.levelplugin.mercenary.ExpeditionDefinition;
import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager;
import me.nakilex.levelplugin.mercenary.MercenaryRole;
import me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionRewardsGUI.RewardView;
import me.nakilex.levelplugin.utils.ChatUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Combined expedition hub with navigation for party selection, dungeon dispatch, and reward viewing.
 */
public class MercenaryExpeditionGUI implements Listener {
    private static final int SIZE = 54;
    private static final String TITLE = ChatColor.DARK_GREEN + "Mercenary Expeditions";
    private static final int PARTY_TAB_SLOT = 45;
    private static final int DUNGEON_TAB_SLOT = 53;
    private static final int REWARD_SLOT = 49;
    private static final int GIFT_SLOT = 48;
    private static final int AFFINITY_SLOT = 50;

    private enum Tab { PARTY, DUNGEONS }

    private final Plugin plugin;
    private final MercenaryAffinityManager affinityManager;
    private final MercenaryExpeditionManager expeditionManager;
    private final MercenaryGiftBrowserGUI giftBrowserGUI;
    private final MercenaryFriendshipGUI friendshipGUI;
    private final MercenaryExpeditionRewardsGUI rewardsGUI;

    private final Map<UUID, Tab> tabs = new HashMap<>();
    private final Map<UUID, List<Integer>> party = new HashMap<>();

    public MercenaryExpeditionGUI(Plugin plugin,
                                  MercenaryAffinityManager affinityManager,
                                  MercenaryExpeditionManager expeditionManager,
                                  MercenaryGiftBrowserGUI giftBrowserGUI,
                                  MercenaryFriendshipGUI friendshipGUI,
                                  MercenaryExpeditionRewardsGUI rewardsGUI) {
        this.plugin = plugin;
        this.affinityManager = affinityManager;
        this.expeditionManager = expeditionManager;
        this.giftBrowserGUI = giftBrowserGUI;
        this.friendshipGUI = friendshipGUI;
        this.rewardsGUI = rewardsGUI;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        open(player, tabs.getOrDefault(player.getUniqueId(), Tab.PARTY));
    }

    private void open(Player player, Tab tab) {
        tabs.put(player.getUniqueId(), tab);
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        switch (tab) {
            case PARTY -> renderParty(inv, player);
            case DUNGEONS -> renderDungeons(inv, player);
        }
        renderNavigation(inv, tab, player);
        player.openInventory(inv);
    }

    private void renderNavigation(Inventory inv, Tab tab, Player player) {
        inv.setItem(PARTY_TAB_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Party"));
        inv.setItem(DUNGEON_TAB_SLOT, GuiUtil.getNexoItem("arrow_right", ChatColor.YELLOW + "Dungeons"));
        inv.setItem(REWARD_SLOT, GuiUtil.getNexoItem("star", ChatColor.AQUA + "Rewards"));
        inv.setItem(GIFT_SLOT, GuiUtil.getNexoItem("gift", ChatColor.LIGHT_PURPLE + "Gift Browser"));
        inv.setItem(AFFINITY_SLOT, GuiUtil.getNexoItem("book", ChatColor.GREEN + "Affinity"));

        ItemStack tabItem = GuiUtil.getNexoItem("target", ChatColor.GOLD + "Current: " + tab.name());
        inv.setItem(4, tabItem);

        List<Integer> selected = party.getOrDefault(player.getUniqueId(), Collections.emptyList());
        ItemStack partyStatus = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = partyStatus.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Selected Mercenaries" + ChatColor.GRAY + " (" + selected.size() + "/3)");
            List<String> lore = new ArrayList<>();
            for (int id : selected) {
                lore.add(ChatColor.GRAY + "#" + id + ChatColor.WHITE + " • " + ChatColor.YELLOW + affinityManager.getRole(id));
            }
            if (lore.isEmpty()) {
                lore.add(ChatColor.DARK_GRAY + "No mercenaries selected.");
            }
            meta.setLore(lore);
            partyStatus.setItemMeta(meta);
        }
        inv.setItem(46, partyStatus);
    }

    private void renderParty(Inventory inv, Player player) {
        int slotIndex = 10;
        List<Integer> selected = party.computeIfAbsent(player.getUniqueId(), id -> new ArrayList<>());
        for (int npcId : affinityManager.getMercenaryIds()) {
            ItemStack icon = new ItemStack(Material.IRON_SWORD);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Mercenary " + npcId);
                List<String> lore = new ArrayList<>();
                int gs = affinityManager.getGearScore(npcId);
                int level = affinityManager.getFriendship(player.getUniqueId(), npcId).getLevel();
                MercenaryRole role = affinityManager.getRole(npcId);
                lore.add(ChatColor.GRAY + "Role: " + ChatColor.YELLOW + role.name());
                lore.add(ChatColor.GRAY + "Gear Score: " + ChatColor.GREEN + gs);
                lore.add(ChatColor.GRAY + "Friendship: " + ChatColor.AQUA + level);
                lore.add(ChatColor.WHITE + TooltipUtil.progressBar(level, 5, 10));
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Left-click to toggle selection");
                lore.add(ChatColor.GRAY + "Right-click to open affinity");
                if (level < 3) {
                    lore.add(ChatColor.RED + "Requires level 3+ to deploy");
                }
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(slotIndex, icon);
            slotIndex++;
            if ((slotIndex + 1) % 9 == 0) {
                slotIndex += 2;
            }
        }
    }

    private void renderDungeons(Inventory inv, Player player) {
        List<Integer> selected = party.getOrDefault(player.getUniqueId(), Collections.emptyList());
        int slot = 10;
        for (ExpeditionDefinition definition : expeditionManager.getExpeditions()) {
            ItemStack item = new ItemStack(Material.MAP);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + definition.displayName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Threat: " + ChatColor.RED + definition.threat());
                lore.add(ChatColor.GRAY + "Recommended GS: " + ChatColor.GREEN + definition.recommendedGearScore());
                int combined = selected.stream().mapToInt(affinityManager::getGearScore).sum();
                lore.add(ChatColor.GRAY + "Party GS: " + ChatColor.AQUA + combined);
                double success = expeditionManager.successChance(selected, definition.threat(), definition.recommendedGearScore());
                int friendship = expeditionManager.averageFriendship(player.getUniqueId(), selected);
                int seconds = expeditionManager.adjustedDuration(combined, definition.threat(), definition.baseDurationSeconds(), friendship);
                lore.add(ChatColor.WHITE + TooltipUtil.progressBar(Math.min(combined, definition.recommendedGearScore()),
                        Math.max(definition.recommendedGearScore(), 1), 12));
                lore.add(ChatColor.GRAY + "Success: " + ChatColor.GREEN + String.format(Locale.ENGLISH, "%.1f%%", success));
                lore.add(ChatColor.GRAY + "Est. Duration: " + ChatColor.AQUA + seconds / 60 + "m");
                lore.add(ChatColor.GRAY + "Roles grant bonus when Tank/DPS/Support are present.");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Click to start with current party");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            if ((slot + 1) % 9 == 8) {
                slot += 3;
            } else {
                slot++;
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }
        UUID id = player.getUniqueId();
        Tab tab = tabs.getOrDefault(id, Tab.PARTY);

        if (event.getSlot() == PARTY_TAB_SLOT) {
            open(player, Tab.PARTY);
            return;
        }
        if (event.getSlot() == DUNGEON_TAB_SLOT) {
            open(player, Tab.DUNGEONS);
            return;
        }
        if (event.getSlot() == GIFT_SLOT) {
            giftBrowserGUI.open(player);
            return;
        }
        if (event.getSlot() == AFFINITY_SLOT) {
            if (!party.getOrDefault(id, Collections.emptyList()).isEmpty()) {
                int npcId = party.get(id).get(0);
                friendshipGUI.open(player, npcId, "Mercenary " + npcId);
            } else {
                player.sendMessage(ChatColor.RED + "Select a mercenary first to view affinity.");
            }
            return;
        }
        if (event.getSlot() == REWARD_SLOT) {
            rewardsGUI.open(player, RewardView.EXPEDITIONS);
            return;
        }

        switch (tab) {
            case PARTY -> handlePartyClick(player, clicked, event.getSlot(), event.getClick());
            case DUNGEONS -> handleDungeonClick(player, clicked);
        }
    }

    private void handlePartyClick(Player player, ItemStack clicked, int slot, ClickType clickType) {
        if (clicked.getType() != Material.IRON_SWORD) {
            return;
        }
        String display = clicked.getItemMeta() != null ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : "";
        if (!display.startsWith("Mercenary")) {
            return;
        }
        String[] parts = display.split(" ");
        if (parts.length < 2) {
            return;
        }
        try {
            int npcId = Integer.parseInt(parts[1]);
            int level = affinityManager.getFriendship(player.getUniqueId(), npcId).getLevel();
            if (clickType.isRightClick()) {
                friendshipGUI.open(player, npcId, "Mercenary " + npcId);
                return;
            }
            if (level < 3) {
                player.sendMessage(ChatColor.RED + "Reach friendship level 3 with this mercenary first.");
                return;
            }
            List<Integer> selection = party.computeIfAbsent(player.getUniqueId(), id -> new ArrayList<>());
            if (selection.contains(npcId)) {
                selection.remove((Integer) npcId);
                ChatUtil.sendCenteredMessage(player, ChatColor.YELLOW + "Removed mercenary #" + npcId + " from party.");
            } else {
                if (selection.size() >= 3) {
                    player.sendMessage(ChatColor.RED + "You can only send up to 3 mercenaries per expedition.");
                    return;
                }
                selection.add(npcId);
                ChatUtil.sendCenteredMessage(player, ChatColor.GREEN + "Added mercenary #" + npcId + " to party.");
            }
            open(player, Tab.PARTY);
        } catch (NumberFormatException ignored) {
        }
    }

    private void handleDungeonClick(Player player, ItemStack clicked) {
        if (clicked.getType() != Material.MAP) {
            return;
        }
        String display = clicked.getItemMeta() != null ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : "";
        ExpeditionDefinition definition = expeditionManager.getExpeditions().stream()
                .filter(def -> ChatColor.stripColor(def.displayName()).equals(display))
                .findFirst()
                .orElse(null);
        if (definition == null) {
            return;
        }
        List<Integer> selected = party.getOrDefault(player.getUniqueId(), Collections.emptyList());
        if (selected.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Select at least one mercenary before starting an expedition.");
            return;
        }
        if (expeditionManager.isOnExpedition(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You already have an active expedition.");
            return;
        }
        expeditionManager.startExpedition(player, selected, definition);
        player.closeInventory();
    }
}

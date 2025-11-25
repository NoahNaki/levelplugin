package me.nakilex.levelplugin.mercenary.gui;

import me.nakilex.levelplugin.mercenary.ActiveExpedition;
import me.nakilex.levelplugin.mercenary.ExpeditionDefinition;
import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager;
import me.nakilex.levelplugin.mercenary.MercenaryFriendship;
import me.nakilex.levelplugin.mercenary.MercenaryRole;
import me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionRewardsGUI.RewardView;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.NumberUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final int SEARCH_SLOT = 47;
    private static final int FILTER_SLOT = 51;
    private static final int SORT_SLOT = 52;

    private static final String[] SORT_OPTIONS = {
            "Threat: High to Low",
            "Threat: Low to High",
            "Recommended GS",
            "Duration",
            "Alphabetical"
    };

    private enum Tab { PARTY, DUNGEONS }

    private final Plugin plugin;
    private final MercenaryAffinityManager affinityManager;
    private final MercenaryExpeditionManager expeditionManager;
    private final MercenaryGiftBrowserGUI giftBrowserGUI;
    private final MercenaryFriendshipGUI friendshipGUI;
    private final MercenaryExpeditionRewardsGUI rewardsGUI;

    private final Map<UUID, Tab> tabs = new HashMap<>();
    private final Map<UUID, List<Integer>> party = new HashMap<>();
    private final Map<UUID, String> searchTerms = new HashMap<>();
    private final Map<UUID, Integer> threatFilters = new HashMap<>();
    private final Map<UUID, Integer> sortModes = new HashMap<>();
    private final Set<UUID> awaitingSearch = new HashSet<>();

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
        threatFilters.putIfAbsent(player.getUniqueId(), 3); // default to show all
        sortModes.putIfAbsent(player.getUniqueId(), 0);
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

        if (tab == Tab.DUNGEONS) {
            inv.setItem(SEARCH_SLOT, createSearchItem(player));
            inv.setItem(FILTER_SLOT, createThreatFilterItem(player));
            inv.setItem(SORT_SLOT, createSortItem(player));
        }
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
                MercenaryFriendship friendship = affinityManager.getFriendship(player.getUniqueId(), npcId);
                int level = friendship.getLevel();
                MercenaryRole role = affinityManager.getRole(npcId);
                int currentThreshold = affinityManager.thresholdForLevel(level);
                int nextThreshold = affinityManager.thresholdForLevel(Math.min(5, level + 1));
                int progressCurrent = Math.max(0, friendship.getPoints() - currentThreshold);
                int progressMax = level >= 5 ? 1 : Math.max(1, nextThreshold - currentThreshold);
                if (level >= 5) {
                    progressCurrent = 1;
                } else {
                    progressCurrent = Math.min(progressCurrent, progressMax);
                }

                lore.addAll(TooltipUtil.bulletList(
                        "Role: " + ChatColor.YELLOW + role.name(),
                        "Gear Score: " + ChatColor.GREEN + NumberUtil.formatCommas(gs),
                        "Friendship: " + ChatColor.AQUA + level + ChatColor.DARK_GRAY + "/5"
                ));
                lore.add(ChatColor.GRAY + "Progress: " + ChatColor.WHITE
                        + TooltipUtil.progressBar(progressCurrent, progressMax, 12));
                if (level < 5) {
                    lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + progressCurrent + ChatColor.DARK_GRAY + "/"
                            + ChatColor.GRAY + progressMax);
                }
                lore.add(" ");
                lore.addAll(TooltipUtil.clickInstructions("to toggle selection", "to view affinity & perks"));
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
        String search = searchTerms.getOrDefault(player.getUniqueId(), "").toLowerCase(Locale.ENGLISH);
        int filter = threatFilters.getOrDefault(player.getUniqueId(), 3);
        int sort = sortModes.getOrDefault(player.getUniqueId(), 0);

        List<ExpeditionDefinition> definitions = new ArrayList<>();
        for (ExpeditionDefinition definition : expeditionManager.getExpeditions()) {
            if (!search.isEmpty()) {
                String name = ChatColor.stripColor(definition.displayName()).toLowerCase(Locale.ENGLISH);
                if (!name.contains(search) && !definition.id().toLowerCase(Locale.ENGLISH).contains(search)) {
                    continue;
                }
            }
            if (!matchesThreat(definition.threat(), filter)) {
                continue;
            }
            definitions.add(definition);
        }
        sortDefinitions(definitions, sort);

        ActiveExpedition active = expeditionManager.getActive(player.getUniqueId());
        String activeId = active != null ? active.getDefinition().id() : null;
        int slot = 10;
        for (ExpeditionDefinition definition : definitions) {
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
                if (activeId != null) {
                    lore.add(" ");
                    lore.addAll(activeLore(active, definition.id().equals(activeId)));
                }
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

        if (tab == Tab.DUNGEONS) {
            if (event.getSlot() == SEARCH_SLOT) {
                if (event.getClick() == ClickType.RIGHT) {
                    searchTerms.remove(id);
                    open(player, Tab.DUNGEONS);
                } else {
                    awaitingSearch.add(id);
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "Enter dungeon search term or 'cancel'.");
                }
                return;
            }
            if (event.getSlot() == FILTER_SLOT) {
                int filter = threatFilters.getOrDefault(id, 3);
                switch (event.getClick()) {
                    case RIGHT -> filter = (filter + 3) % 4;
                    default -> filter = (filter + 1) % 4;
                }
                threatFilters.put(id, filter);
                open(player, Tab.DUNGEONS);
                return;
            }
            if (event.getSlot() == SORT_SLOT) {
                int mode = sortModes.getOrDefault(id, 0);
                int total = SORT_OPTIONS.length;
                switch (event.getClick()) {
                    case RIGHT -> mode = (mode + total - 1) % total;
                    default -> mode = (mode + 1) % total;
                }
                sortModes.put(id, mode);
                open(player, Tab.DUNGEONS);
                return;
            }
        }

        switch (tab) {
            case PARTY -> handlePartyClick(player, clicked, event.getSlot(), event.getClick());
            case DUNGEONS -> handleDungeonClick(player, clicked);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (awaitingSearch.remove(id)) {
            event.setCancelled(true);
            String message = event.getMessage();
            if (message.equalsIgnoreCase("cancel")) {
                searchTerms.remove(id);
            } else {
                searchTerms.put(id, message.trim());
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> open(event.getPlayer(), Tab.DUNGEONS));
        }
    }

    private ItemStack createSearchItem(Player player) {
        ItemStack item = GuiUtil.getNexoItem("search", ChatColor.GOLD + "Search");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String term = searchTerms.getOrDefault(player.getUniqueId(), "");
            List<String> lore = new ArrayList<>();
            if (term.isEmpty()) {
                lore.add(ChatColor.GRAY + "Left-Click to enter a term.");
            } else {
                lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + term);
            }
            lore.add(ChatColor.DARK_GRAY + "Right-Click to clear search.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createThreatFilterItem(Player player) {
        ItemStack item = GuiUtil.getNexoItem("filter", ChatColor.AQUA + "Threat Filter");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int filter = threatFilters.getOrDefault(player.getUniqueId(), 3);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "Filter dungeons by threat.");
            lore.add(" ");
            lore.add(optionLine(0, filter, "Low (\u2264 5)"));
            lore.add(optionLine(1, filter, "Medium (6-10)"));
            lore.add(optionLine(2, filter, "High (11+)"));
            lore.add(optionLine(3, filter, "Show All"));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSortItem(Player player) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int mode = sortModes.getOrDefault(player.getUniqueId(), 0);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "Sort dungeons.");
            lore.add(" ");
            for (int i = 0; i < SORT_OPTIONS.length; i++) {
                lore.add(optionLine(i, mode, SORT_OPTIONS[i]));
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String optionLine(int idx, int selected, String label) {
        ChatColor color = idx == selected ? ChatColor.GOLD : ChatColor.GRAY;
        String marker = idx == selected ? ChatColor.YELLOW + "➤ " : ChatColor.DARK_GRAY + "• ";
        return marker + color + label;
    }

    private boolean matchesThreat(int threat, int filter) {
        return switch (filter) {
            case 0 -> threat <= 5;
            case 1 -> threat > 5 && threat <= 10;
            case 2 -> threat > 10;
            default -> true;
        };
    }

    private void sortDefinitions(List<ExpeditionDefinition> definitions, int mode) {
        Comparator<ExpeditionDefinition> comparator = switch (mode) {
            case 1 -> Comparator.comparingInt(ExpeditionDefinition::threat);
            case 2 -> Comparator.comparingInt(ExpeditionDefinition::recommendedGearScore).reversed();
            case 3 -> Comparator.comparingInt(ExpeditionDefinition::baseDurationSeconds);
            case 4 -> Comparator.comparing(def -> ChatColor.stripColor(def.displayName()), String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparingInt(ExpeditionDefinition::threat).reversed();
        };
        definitions.sort(comparator);
    }

    private List<String> activeLore(ActiveExpedition active, boolean currentDungeon) {
        List<String> lore = new ArrayList<>();
        long remaining = Math.max(0, Duration.between(Instant.now(), active.getEndTime()).getSeconds());
        String duration = formatDuration(remaining);
        if (currentDungeon) {
            lore.add(ChatColor.GOLD + "Status: " + ChatColor.GREEN + "Underway");
            lore.add(ChatColor.GRAY + "Time left: " + ChatColor.AQUA + duration);
            lore.add(ChatColor.GRAY + "Party size: " + ChatColor.WHITE + active.getNpcIds().size());
        } else {
            lore.add(ChatColor.RED + "Another expedition is active.");
            lore.add(ChatColor.GRAY + "Time left: " + ChatColor.AQUA + duration);
            lore.add(ChatColor.DARK_GRAY + "Complete it before starting another.");
        }
        return lore;
    }

    private String formatDuration(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        if (mins <= 0) {
            return secs + "s";
        }
        return mins + "m " + secs + "s";
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
                ChatFormatter.sendCenteredMessage(player, ChatColor.YELLOW + "Removed mercenary #" + npcId + " from party.");
            } else {
                if (selection.size() >= 3) {
                    player.sendMessage(ChatColor.RED + "You can only send up to 3 mercenaries per expedition.");
                    return;
                }
                selection.add(npcId);
                ChatFormatter.sendCenteredMessage(player, ChatColor.GREEN + "Added mercenary #" + npcId + " to party.");
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

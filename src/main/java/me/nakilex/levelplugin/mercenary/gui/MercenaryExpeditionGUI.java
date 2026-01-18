package me.nakilex.levelplugin.mercenary.gui;

import me.nakilex.levelplugin.mercenary.ActiveExpedition;
import me.nakilex.levelplugin.mercenary.ExpeditionDefinition;
import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager;
import me.nakilex.levelplugin.mercenary.MercenaryFriendship;
import me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionRewardsGUI.RewardView;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import me.nakilex.levelplugin.utils.NumberUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

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
    private static final String TITLE = "Mercenary Expeditions";
    private static final int PARTY_TAB_SLOT = 45;
    private static final int DUNGEON_TAB_SLOT = 53;
    private static final int REWARD_SLOT = 49;
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
    private final MercenaryFriendshipGUI friendshipGUI;
    private final MercenaryExpeditionRewardsGUI rewardsGUI;
    private final NamespacedKey mercenaryKey;

    private final Map<UUID, Tab> tabs = new HashMap<>();
    private final Map<UUID, List<Integer>> party = new HashMap<>();
    private final Map<UUID, String> searchTerms = new HashMap<>();
    private final Map<UUID, Integer> threatFilters = new HashMap<>();
    private final Map<UUID, Integer> sortModes = new HashMap<>();
    private final Set<UUID> awaitingSearch = new HashSet<>();

    public MercenaryExpeditionGUI(Plugin plugin,
                                  MercenaryAffinityManager affinityManager,
                                  MercenaryExpeditionManager expeditionManager,
                                  MercenaryFriendshipGUI friendshipGUI,
                                  MercenaryExpeditionRewardsGUI rewardsGUI) {
        this.plugin = plugin;
        this.affinityManager = affinityManager;
        this.expeditionManager = expeditionManager;
        this.friendshipGUI = friendshipGUI;
        this.rewardsGUI = rewardsGUI;
        this.mercenaryKey = new NamespacedKey(plugin, "mercenary_id");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        pruneParty(player);
        open(player, tabs.getOrDefault(player.getUniqueId(), Tab.PARTY));
    }

    private void open(Player player, Tab tab) {
        tabs.put(player.getUniqueId(), tab);
        threatFilters.putIfAbsent(player.getUniqueId(), 0); // default to show all
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
        inv.setItem(4, createInfoItem(tab, player));

        ItemStack partyTab = tab == Tab.DUNGEONS
                ? GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Party")
                : GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack dungeonTab = tab == Tab.PARTY
                ? GuiUtil.getNexoItem("arrow_right", ChatColor.YELLOW + "Dungeons")
                : GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);

        inv.setItem(PARTY_TAB_SLOT, partyTab);
        inv.setItem(DUNGEON_TAB_SLOT, dungeonTab);
        inv.setItem(REWARD_SLOT, createRewardsButton());

        List<Integer> selected = party.getOrDefault(player.getUniqueId(), Collections.emptyList());
        ItemStack partyStatus = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = partyStatus.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Selected Mercenaries" + ChatColor.GRAY + " (" + selected.size() + "/3)");
            List<String> lore = new ArrayList<>();
            for (int id : selected) {
                lore.add(ChatColor.GRAY + getNpcName(id) + ChatColor.WHITE + " • " + ChatColor.YELLOW + affinityManager.getRoleLabel(id));
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

    private ItemStack createInfoItem(Tab tab, Player player) {
        ItemStack info = GuiUtil.getNexoItem("home", ChatColor.YELLOW + "Current: " + (tab == Tab.PARTY ? "Party" : "Dungeons"));
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (tab == Tab.PARTY) {
                List<Integer> selected = party.getOrDefault(player.getUniqueId(), Collections.emptyList());
                int totalGs = selected.stream().mapToInt(affinityManager::getGearScore).sum();
                lore.add(ChatColor.GRAY + "Selected: " + ChatColor.WHITE + selected.size() + ChatColor.GRAY + "/3");
                lore.add(ChatColor.GRAY + "Slots: " + TooltipUtil.progressBar(selected.size(), 3, 12));
                lore.add(ChatColor.GRAY + "Party GS: " + ChatColor.AQUA + NumberUtil.formatCommas(totalGs));
                lore.add(ChatColor.GRAY + "Friendship Avg: "
                        + ChatColor.AQUA + expeditionManager.averageFriendship(player.getUniqueId(), selected));
            } else {
                lore.add(ChatColor.GRAY + "Available: " + ChatColor.WHITE + expeditionManager.getExpeditions().size());
                ActiveExpedition active = expeditionManager.getActive(player.getUniqueId());
                if (active != null) {
                    lore.add(" ");
                    lore.add(ChatColor.GRAY + "Active: " + ChatColor.GREEN + active.getDefinition().displayName());
                    lore.add(ChatColor.GRAY + "Party size: " + ChatColor.WHITE + active.getNpcIds().size());
                }
            }
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        return info;
    }

    private ItemStack createRewardsButton() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Rewards");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Review loot and coin earnings.");
            lore.add(ChatColor.DARK_GRAY + "Coins are granted automatically.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMercenaryIcon(Player player, int npcId, boolean selected) {
        NPC npc = NpcApi.getRegistry().getById(npcId);
        String skin = null;
        if (npc != null) {
            SkinTrait trait = npc.getOrAddTrait(SkinTrait.class);
            skin = trait.getTexture();
        }
        String baseName = getNpcName(npcId);
        String display = ChatColor.GOLD + baseName;
        ItemStack icon;
        if (skin != null && !skin.isEmpty()) {
            icon = HeadUtil.createCustomHead(skin, display, null);
        } else {
            icon = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta baseMeta = icon.getItemMeta();
            if (baseMeta != null) {
                baseMeta.setDisplayName(display);
                icon.setItemMeta(baseMeta);
            }
        }

        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            int gs = affinityManager.getGearScore(npcId);
            MercenaryFriendship friendship = affinityManager.getFriendship(player.getUniqueId(), npcId);
            int level = friendship.getLevel();
            int currentThreshold = affinityManager.thresholdForLevel(level);
            int nextThreshold = affinityManager.thresholdForLevel(Math.min(5, level + 1));
            int progressCurrent = Math.max(0, friendship.getPoints() - currentThreshold);
            int progressMax = level >= 5 ? 1 : Math.max(1, nextThreshold - currentThreshold);
            if (level >= 5) {
                progressCurrent = 1;
            } else {
                progressCurrent = Math.min(progressCurrent, progressMax);
            }

            List<String> lore = new ArrayList<>();
            lore.addAll(TooltipUtil.bulletList(
                    "Role: " + ChatColor.YELLOW + affinityManager.getRoleLabel(npcId),
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
            lore.add(selected ? ChatColor.GREEN + "Selected for party" : ChatColor.DARK_GRAY + "Not selected");
            lore.addAll(TooltipUtil.clickInstructions("to toggle selection", "to view affinity & perks"));
            if (level < 3) {
                lore.add(ChatColor.RED + "Requires level 3+ to deploy");
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(mercenaryKey, PersistentDataType.INTEGER, npcId);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private void renderParty(Inventory inv, Player player) {
        int slotIndex = 10;
        List<Integer> selected = party.computeIfAbsent(player.getUniqueId(), id -> new ArrayList<>());
        Set<Integer> unlocked = new HashSet<>(affinityManager.getUnlockedMercenaryIds(player.getUniqueId()));
        selected.removeIf(id -> !unlocked.contains(id));
        for (int npcId : affinityManager.getUnlockedMercenaryIds(player.getUniqueId())) {
            boolean isSelected = selected.contains(npcId);
            inv.setItem(slotIndex, createMercenaryIcon(player, npcId, isSelected));
            slotIndex++;
            if ((slotIndex + 1) % 9 == 0) {
                slotIndex += 2;
            }
        }
    }

    private void renderDungeons(Inventory inv, Player player) {
        List<Integer> selected = party.getOrDefault(player.getUniqueId(), Collections.emptyList());
        String search = searchTerms.getOrDefault(player.getUniqueId(), "").toLowerCase(Locale.ENGLISH);
        int filter = threatFilters.getOrDefault(player.getUniqueId(), 0);
        int sort = sortModes.getOrDefault(player.getUniqueId(), 0);
        Set<String> clearedDungeons = Collections.emptySet();
        if (plugin instanceof Main main && main.getPlayerConfig() != null) {
            clearedDungeons = main.getPlayerConfig().getClearedDungeons(player.getUniqueId());
        }

        List<ExpeditionDefinition> definitions = new ArrayList<>();
        for (ExpeditionDefinition definition : expeditionManager.getExpeditions()) {
            if (!clearedDungeons.isEmpty() && !clearedDungeons.contains(definition.id().toLowerCase(Locale.ENGLISH))) {
                continue;
            }
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
                String durationLabel = expeditionManager.isInstantExpeditions()
                        ? ChatColor.GREEN + "Instant"
                        : ChatColor.AQUA.toString() + seconds / 60 + "m";
                lore.add(ChatColor.GRAY + "Est. Duration: " + durationLabel);
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
            if (tab != Tab.PARTY) {
                open(player, Tab.PARTY);
            }
            return;
        }
        if (event.getSlot() == DUNGEON_TAB_SLOT) {
            if (tab != Tab.DUNGEONS) {
                open(player, Tab.DUNGEONS);
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
                int filter = threatFilters.getOrDefault(id, 0);
                int max = 5;
                switch (event.getClick()) {
                    case RIGHT -> filter = filter <= 0 ? max : filter - 1;
                    default -> filter = filter >= max ? 0 : filter + 1;
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
            lore.add(ChatColor.DARK_GRAY + "Search by dungeon name or ID.");
            lore.add(" ");
            if (term.isEmpty()) {
                lore.addAll(TooltipUtil.bulletList("No search set."));
            } else {
                lore.addAll(TooltipUtil.bulletList("Current: " + ChatColor.WHITE + term));
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to enter a term", "to clear search"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createThreatFilterItem(Player player) {
        ItemStack item = GuiUtil.getNexoItem("filter", ChatColor.AQUA + "Threat Filter");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int filter = threatFilters.getOrDefault(player.getUniqueId(), 0);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "Filter dungeons by threat level.");
            lore.add(" ");
            for (int tier = 1; tier <= 5; tier++) {
                lore.add(optionLine(tier, filter, "Threat " + tier));
            }
            lore.add(optionLine(0, filter, "All Threats"));
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
            meta.setDisplayName(ChatColor.GOLD + "Sort Dungeons");
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
        if (filter <= 0) {
            return true;
        }
        return threat == filter;
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

    private String getNpcName(int npcId) {
        NPC npc = NpcApi.getRegistry().getById(npcId);
        if (npc != null) {
            return ChatColor.stripColor(npc.getName());
        }
        return "Mercenary";
    }

    private void pruneParty(Player player) {
        if (player == null) {
            return;
        }
        List<Integer> selection = party.get(player.getUniqueId());
        if (selection == null) {
            return;
        }
        Set<Integer> unlocked = new HashSet<>(affinityManager.getUnlockedMercenaryIds(player.getUniqueId()));
        selection.removeIf(id -> !unlocked.contains(id));
    }

    private void handlePartyClick(Player player, ItemStack clicked, int slot, ClickType clickType) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Integer npcId = container.get(mercenaryKey, PersistentDataType.INTEGER);
        if (npcId == null) {
            return;
        }
        int level = affinityManager.getFriendship(player.getUniqueId(), npcId).getLevel();
        if (clickType.isRightClick()) {
            friendshipGUI.open(player, npcId, getNpcName(npcId));
            return;
        }
        if (!affinityManager.isUnlocked(player.getUniqueId(), npcId)) {
            player.sendMessage(ChatColor.RED + "Meet this mercenary in the Codex before deploying them.");
            return;
        }
        if (level < 3) {
            player.sendMessage(ChatColor.RED + "Reach friendship level 3 with this mercenary first.");
            return;
        }
        List<Integer> selection = party.computeIfAbsent(player.getUniqueId(), id -> new ArrayList<>());
        if (selection.contains(npcId)) {
            selection.remove((Integer) npcId);
            ChatFormatter.sendCenteredMessage(player, ChatColor.YELLOW + "Removed " + getNpcName(npcId) + " from party.");
        } else {
            if (selection.size() >= 3) {
                player.sendMessage(ChatColor.RED + "You can only send up to 3 mercenaries per expedition.");
                return;
            }
            selection.add(npcId);
            ChatFormatter.sendCenteredMessage(player, ChatColor.GREEN + "Added " + getNpcName(npcId) + " to party.");
        }
        open(player, Tab.PARTY);
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

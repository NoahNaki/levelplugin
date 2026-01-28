package me.nakilex.levelplugin.quests.gui;

import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class QuestGUI {

    public static final String GUI_TITLE = "Quests";
    private static final int GUI_SIZE = 54;
    private static final int[] QUEST_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int ITEMS_PER_PAGE = QUEST_SLOTS.length;
    private static final int PREV_PAGE = 45;
    private static final int NEXT_PAGE = 53;
    private static final int FILTER_SLOT = 48;
    private static final int REPEAT_FILTER_SLOT = 49;
    private static final int SORT_SLOT = 50;

    static final Map<java.util.UUID, Integer> pageMap = new java.util.HashMap<>();
    static final Map<java.util.UUID, Integer> filterMap = new java.util.HashMap<>();
    static final Map<java.util.UUID, Integer> repeatFilterMap = new java.util.HashMap<>();
    static final Map<java.util.UUID, Integer> sortMap = new java.util.HashMap<>();

    public static final NamespacedKey QUEST_ID_KEY = new NamespacedKey(Main.getInstance(), "quest_id");

    private static final List<String> STORY_ORDER = List.of(
            "officeerrands",
            "newbeginning",
            "serashelp",
            "stablekeeper",
            "serashelp_part2",
            "salvagerslesson",
            "hawiehermitcrabs",
            "essenceweaverslesson",
            "abandonedcastle",
            "forgefundamentals",
            "fieldworkfavor"
    );
    private static final Map<String, Integer> STORY_ORDER_INDEX = new HashMap<>();

    // Confirmation menu constants
    public static final String CONFIRM_TITLE = "Confirm Abandon";
    private static final int CONFIRM_SIZE = 27;
    public static final int CONFIRM_YES_SLOT = 11;
    public static final int CONFIRM_NO_SLOT = 15;
    private static final Map<java.util.UUID, Inventory> CONFIRM_OPEN = new java.util.HashMap<>();
    private static final Map<java.util.UUID, List<GuiWidget>> CONFIRM_WIDGETS = new java.util.HashMap<>();
    private static final Map<java.util.UUID, String> PENDING_QUEST = new java.util.HashMap<>();

    public static void openQuestGUI(Player player, QuestManager questManager) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        openQuestGUI(player, questManager, page);
    }

    public static void openConfirmAbandon(Player player, Quest quest) {
        Inventory inv = GuiBuilder.create(CONFIRM_SIZE, CONFIRM_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        List<GuiWidget> widgets = buildConfirmWidgets(player, Main.getInstance().getQuestManager());
        renderWidgets(inv, player, widgets);
        CONFIRM_WIDGETS.put(player.getUniqueId(), widgets);
        CONFIRM_OPEN.put(player.getUniqueId(), inv);
        PENDING_QUEST.put(player.getUniqueId(), quest.getId());
        player.openInventory(inv);
    }

    static void openQuestGUI(Player player, QuestManager questManager, int page) {
        ensureStoryOrderIndex();
        pageMap.put(player.getUniqueId(), page);
        Inventory gui = GuiBuilder.create(GUI_SIZE, GUI_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();

        List<Quest> list = getFilteredSortedQuests(player, questManager);
        int filter = filterMap.getOrDefault(player.getUniqueId(), 0);
        int repeatFilter = repeatFilterMap.getOrDefault(player.getUniqueId(), 0);
        int sort = sortMap.getOrDefault(player.getUniqueId(), 3);
        int maxPage = Math.max(0, (list.size() - 1) / ITEMS_PER_PAGE);
        List<GuiWidget> widgets = buildWidgets(player, questManager, list, page, maxPage, filter, repeatFilter, sort);
        renderWidgets(gui, player, widgets);
        player.openInventory(gui);
    }

    private static ItemStack createQuestItem(Player player, Quest quest, QuestState state,
                                             PlayerQuestProgress progress, QuestManager qm) {
        String name;
        if (state == QuestState.LOCKED) {
            name = ChatColor.DARK_GRAY + "???";
        } else {
            String baseName = state.getColor() + quest.getName();
            if (quest.getRepeatType() != me.nakilex.levelplugin.quests.data.QuestRepeatType.ONE_TIME) {
                baseName += ChatColor.GRAY + " (" + ChatColor.DARK_GRAY + quest.getRepeatType().getDisplayName() + ChatColor.GRAY + ")";
            }
            name = baseName;
        }

        // Use scroll2 for all active quests unless this one is tracked
        String icon = state.getIconId();
        if (state == QuestState.ACCEPTED || state == QuestState.IN_PROGRESS || state == QuestState.TURN_IN_READY) {
            icon = "pack1_scroll2";
        }
        String tracked = qm.getTrackedQuest(player.getUniqueId());
        if (tracked != null && tracked.equals(quest.getId())) {
            icon = "pack1_scroll4";
        }
        if (quest.getRepeatType() == me.nakilex.levelplugin.quests.data.QuestRepeatType.DAILY) {
            if (state == QuestState.COMPLETED) {
                icon = "bluecheck";
            } else if (tracked == null || !tracked.equals(quest.getId())) {
                if (state == QuestState.AVAILABLE || state == QuestState.ACCEPTED
                        || state == QuestState.IN_PROGRESS || state == QuestState.TURN_IN_READY) {
                    icon = "pack1_scroll5";
                }
            }
        }

        ItemStack item = GuiUtil.getNexoItem(icon, name);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            String levelLine = formatLevelRequirement(player, quest);
            String locationLine = quest.isLocationVisible() ? formatLocationLine(quest) : null;

            if (state != QuestState.LOCKED) {
                lore.add(levelLine);
                lore.add(" ");
                for (String line : wrapText(quest.getDescription(), 28)) {
                    lore.add(ChatColor.GRAY + line);
                }
                lore.add(" ");
                if (locationLine != null) {
                    lore.add(locationLine);
                    lore.add(" ");
                }

                if (state == QuestState.ACCEPTED || state == QuestState.IN_PROGRESS || state == QuestState.TURN_IN_READY) {
                    int objIndex = 0;
                    int objProgress = 0;
                    if (progress != null && progress.getQuest().getId().equals(quest.getId())) {
                        for (int i = 0; i < quest.getObjectives().size(); i++) {
                            if (progress.getProgress(i) < quest.getObjectives().get(i).getAmount()) {
                                objIndex = i;
                                objProgress = progress.getProgress(i);
                                break;
                            }
                        }
                    }
                    QuestObjective obj = quest.getObjectives().get(objIndex);
                    String desc = qm.describeObjective(obj);
                    List<String> wrapped = wrapText(desc, 28);
                    for (int i = 0; i < wrapped.size(); i++) {
                        String prefix = i == 0 ? ChatColor.WHITE.toString() : ChatColor.WHITE + "  ";
                        lore.add(prefix + wrapped.get(i));
                    }
                    lore.add(ChatColor.GRAY + "(" + objProgress + "/" + obj.getAmount() + ")");
                }

                lore.add(" ");
                lore.add(ChatColor.GREEN + "Rewards:");
                if (quest.getReward() != null) {
                    QuestReward r = quest.getReward();
                    if (r.getXp() > 0) {
                        String expLabel = me.nakilex.levelplugin.utils.ChatFormatter.experienceLabel();
                        String expColor = me.nakilex.levelplugin.utils.ChatFormatter.experienceColor();
                        lore.add(ChatColor.GREEN + "- " + expColor + r.getXp() + ChatColor.RESET + " <glyph:experience_orb_icon> " + expLabel);
                    }
                    if (r.getCoins() > 0) lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + r.getCoins() + " <glyph:coins_icon>");
                    if (r.getGems() > 0) lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + r.getGems() + " " + ChatColor.LIGHT_PURPLE + "<glyph:purple_orb_icon>");
                    for (int id : r.getItemIds()) {
                        me.nakilex.levelplugin.items.data.CustomItem tpl = me.nakilex.levelplugin.Main.getInstance().getItemManager().getTemplateById(id);
                        String in = tpl != null ? tpl.getBaseName() : ("Item " + id);
                        lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + in);
                    }
                    for (var cls : r.getUnlockClasses()) {
                        String pretty = cls.name().substring(0,1) + cls.name().substring(1).toLowerCase();
                        lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + pretty + " Class");
                    }
                    for (String text : r.getCustomLines()) {
                        lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + text);
                    }
                } else {
                    lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + "None");
                }

                if (state == QuestState.ACCEPTED || state == QuestState.IN_PROGRESS || state == QuestState.TURN_IN_READY) {
                    lore.add(" ");
                    lore.addAll(TooltipUtil.clickInstructions("to track", quest.isMainQuest() ? null : "to abandon"));
                }
            } else {
                lore.add(levelLine);
                if (locationLine != null) {
                    lore.add(" ");
                    lore.add(locationLine);
                }
            }

            meta.setLore(lore);
            meta.setLocalizedName(quest.getId());
            meta.getPersistentDataContainer().set(QUEST_ID_KEY, PersistentDataType.STRING, quest.getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static List<String> wrapText(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return List.of("");
        }
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (current.length() == 0) {
                current.append(word);
                continue;
            }
            if (current.length() + 1 + word.length() > maxLength) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current.append(' ').append(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static String formatLevelRequirement(Player player, Quest quest) {
        LevelManager levelManager = Main.getInstance().getLevelManager();
        int playerLevel = levelManager != null ? levelManager.getLevel(player) : 0;
        boolean meets = playerLevel >= quest.getLevelRequirement();
        ChatColor color = meets ? ChatColor.GREEN : ChatColor.RED;
        String symbol = meets ? "✔ " : "✘ ";
        return color + symbol + ChatColor.GRAY + "Requires Level: " + ChatColor.WHITE + quest.getLevelRequirement();
    }

    private static String formatLocationLine(Quest quest) {
        return ChatColor.GRAY + "Quest Location: " + ChatColor.WHITE + resolveNpcLocation(quest);
    }

    private static String resolveNpcLocation(Quest quest) {
        if (quest == null || quest.getNpcGiverId() == null) {
            return "Unknown";
        }

        NPC npc = NpcApi.getRegistry().getById(quest.getNpcGiverId());
        if (npc == null) {
            return "Unknown";
        }

        Location loc = npc.getStoredLocation();
        if (loc == null && npc.isSpawned()) {
            loc = npc.getEntity().getLocation();
        }

        if (loc == null) {
            return "Unknown";
        }

        String coords = loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
        return coords;
    }

    private static ItemStack createFilterButton(int mode) {
        ItemStack it = new ItemStack(Material.HOPPER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Filter");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "Filter the quests");
            lore.add(" ");
            String[] opts = {"Show All", "Available", "In Progress", "Completed"};
            for (int i = 0; i < opts.length; i++) {
                lore.add(TooltipUtil.selectionLine(i == mode, opts[i]));
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static ItemStack createSortButton(int mode) {
        ItemStack it = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Sorting");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "Sort the quests");
            lore.add(" ");
            String[] opts = {"A-Z", "By State", "By Level", "Story Order"};
            for (int i = 0; i < opts.length; i++) {
                lore.add(TooltipUtil.selectionLine(i == mode, opts[i]));
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static ItemStack createRepeatFilterButton(int mode) {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Repeat Filter");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "Filter by repeat type");
            lore.add(" ");
            String[] opts = {"Show All", "One-Time", "Daily"};
            for (int i = 0; i < opts.length; i++) {
                lore.add(TooltipUtil.selectionLine(i == mode, opts[i]));
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    public static boolean handleWidgetClick(InventoryClickEvent event, Player player, QuestManager questManager) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return false;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        List<Quest> list = getFilteredSortedQuests(player, questManager);
        int maxPage = Math.max(0, (list.size() - 1) / ITEMS_PER_PAGE);
        int filter = filterMap.getOrDefault(player.getUniqueId(), 0);
        int repeatFilter = repeatFilterMap.getOrDefault(player.getUniqueId(), 0);
        int sort = sortMap.getOrDefault(player.getUniqueId(), 3);
        GuiWidget widget = buildWidgets(player, questManager, list, page, maxPage, filter, repeatFilter, sort).stream()
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

    public static boolean handleConfirmWidgetClick(InventoryClickEvent event, Player player) {
        if (!event.getView().getTitle().equals(CONFIRM_TITLE)) {
            return false;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        List<GuiWidget> widgets = CONFIRM_WIDGETS.get(player.getUniqueId());
        if (widgets == null) {
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

    private static List<Quest> getFilteredSortedQuests(Player player, QuestManager questManager) {
        ensureStoryOrderIndex();
        List<Quest> list = new ArrayList<>(questManager.getQuests());
        int filter = filterMap.getOrDefault(player.getUniqueId(), 0);
        int repeatFilter = repeatFilterMap.getOrDefault(player.getUniqueId(), 0);
        int sort = sortMap.getOrDefault(player.getUniqueId(), 3);

        list.removeIf(q -> {
            QuestState state = questManager.getQuestState(player, q);
            return switch (filter) {
                case 1 -> state != QuestState.AVAILABLE;
                case 2 -> state != QuestState.IN_PROGRESS && state != QuestState.ACCEPTED;
                case 3 -> state != QuestState.COMPLETED;
                default -> false;
            };
        });
        list.removeIf(q -> {
            if (repeatFilter == 1) {
                return q.getRepeatType() != me.nakilex.levelplugin.quests.data.QuestRepeatType.ONE_TIME;
            }
            if (repeatFilter == 2) {
                return q.getRepeatType() != me.nakilex.levelplugin.quests.data.QuestRepeatType.DAILY;
            }
            return false;
        });

        Comparator<Quest> comp;
        switch (sort) {
            case 1 -> comp = Comparator.comparingInt((Quest q) -> questManager.getQuestState(player, q).ordinal());
            case 2 -> comp = Comparator.comparingInt(Quest::getLevelRequirement)
                    .thenComparing(Quest::getName, String.CASE_INSENSITIVE_ORDER);
            case 3 -> comp = Comparator.comparingInt(
                            (Quest q) -> STORY_ORDER_INDEX.getOrDefault(q.getId(), Integer.MAX_VALUE))
                    .thenComparing(Quest::getName, String.CASE_INSENSITIVE_ORDER);
            default -> comp = Comparator.comparing(Quest::getName, String.CASE_INSENSITIVE_ORDER);
        }
        list.sort(comp);
        return list;
    }

    private static void ensureStoryOrderIndex() {
        if (!STORY_ORDER_INDEX.isEmpty()) {
            return;
        }
        for (int i = 0; i < STORY_ORDER.size(); i++) {
            STORY_ORDER_INDEX.put(STORY_ORDER.get(i), i);
        }
    }

    private static List<GuiWidget> buildWidgets(Player player, QuestManager questManager, List<Quest> list,
                                                int page, int maxPage, int filter, int repeatFilter, int sort) {
        List<GuiWidget> widgets = new ArrayList<>();
        int start = page * ITEMS_PER_PAGE;
        int slotIndex = 0;
        for (int i = start; i < list.size() && slotIndex < ITEMS_PER_PAGE; i++) {
            Quest quest = list.get(i);
            int slot = QUEST_SLOTS[slotIndex++];
            widgets.add(new ActionWidget(slot,
                    context -> {
                        QuestState state = questManager.getQuestState(context.player(), quest);
                        return createQuestItem(context.player(), quest, state,
                                questManager.getProgress(context.player().getUniqueId(), quest.getId()), questManager);
                    },
                    (click, context) -> handleQuestClick(context.player(), quest, questManager, click)));
        }
        if (page > 0) {
            widgets.add(new ActionWidget(PREV_PAGE,
                    context -> GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous"),
                    (click, context) -> openQuestGUI(context.player(), questManager, Math.max(0, page - 1))));
        }
        if (list.size() > (page + 1) * ITEMS_PER_PAGE) {
            widgets.add(new ActionWidget(NEXT_PAGE,
                    context -> GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next"),
                    (click, context) -> openQuestGUI(context.player(), questManager, page + 1)));
        }
        widgets.add(new ActionWidget(FILTER_SLOT,
                context -> createFilterButton(filter),
                (click, context) -> {
                    int mode = filterMap.getOrDefault(context.player().getUniqueId(), 0);
                    mode = click.isRightClick() ? (mode + 3) % 4 : (mode + 1) % 4;
                    filterMap.put(context.player().getUniqueId(), mode);
                    openQuestGUI(context.player(), questManager, pageMap.getOrDefault(context.player().getUniqueId(), 0));
                }));
        widgets.add(new ActionWidget(REPEAT_FILTER_SLOT,
                context -> createRepeatFilterButton(repeatFilter),
                (click, context) -> {
                    int mode = repeatFilterMap.getOrDefault(context.player().getUniqueId(), 0);
                    mode = click.isRightClick() ? (mode + 2) % 3 : (mode + 1) % 3;
                    repeatFilterMap.put(context.player().getUniqueId(), mode);
                    openQuestGUI(context.player(), questManager, pageMap.getOrDefault(context.player().getUniqueId(), 0));
                }));
        widgets.add(new ActionWidget(SORT_SLOT,
                context -> createSortButton(sort),
                (click, context) -> {
                    int mode = sortMap.getOrDefault(context.player().getUniqueId(), 0);
                    mode = click.isRightClick() ? (mode + 3) % 4 : (mode + 1) % 4;
                    sortMap.put(context.player().getUniqueId(), mode);
                    openQuestGUI(context.player(), questManager, pageMap.getOrDefault(context.player().getUniqueId(), 0));
                }));
        return widgets;
    }

    private static void renderWidgets(Inventory inventory, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private static List<GuiWidget> buildConfirmWidgets(Player player, QuestManager questManager) {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(CONFIRM_YES_SLOT,
                context -> GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm"),
                (click, context) -> {
                    String qId = getPendingQuest(context.player().getUniqueId());
                    if (qId != null) {
                        var quest = questManager.getQuest(qId);
                        questManager.resetQuest(context.player().getUniqueId(), qId);
                        context.player().sendMessage(ChatColor.RED + "Abandoned quest: " + ChatColor.WHITE + quest.getName());
                    }
                    clearPending(context.player().getUniqueId());
                    openQuestGUI(context.player(), questManager, pageMap.getOrDefault(context.player().getUniqueId(), 0));
                }));
        widgets.add(new ActionWidget(CONFIRM_NO_SLOT,
                context -> GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel"),
                (click, context) -> {
                    clearPending(context.player().getUniqueId());
                    openQuestGUI(context.player(), questManager, pageMap.getOrDefault(context.player().getUniqueId(), 0));
                }));
        return widgets;
    }

    private static void handleQuestClick(Player player, Quest quest, QuestManager questManager,
                                         org.bukkit.event.inventory.ClickType click) {
        QuestState state = questManager.getQuestState(player, quest);
        if (click.isRightClick()) {
            if (state == QuestState.AVAILABLE) {
                questManager.startQuest(player, quest.getId());
                player.closeInventory();
            } else if (state == QuestState.ACCEPTED || state == QuestState.IN_PROGRESS || state == QuestState.TURN_IN_READY) {
                if (!quest.isMainQuest()) {
                    Bukkit.getScheduler().runTask(Main.getInstance(),
                            () -> openConfirmAbandon(player, quest));
                }
            }
        } else if (click.isLeftClick()) {
            if (state == QuestState.ACCEPTED || state == QuestState.IN_PROGRESS || state == QuestState.TURN_IN_READY
                    || state == QuestState.AVAILABLE) {
                questManager.setTrackedQuest(player, quest.getId());
                player.sendMessage(ChatColor.GREEN + "Tracking quest: " + ChatColor.WHITE + quest.getName());
                openQuestGUI(player, questManager, pageMap.getOrDefault(player.getUniqueId(), 0));
            }
        }
    }

    static Inventory getConfirmInventory(java.util.UUID id) {
        return CONFIRM_OPEN.get(id);
    }

    static String getPendingQuest(java.util.UUID id) {
        return PENDING_QUEST.get(id);
    }

    static boolean hasPending(java.util.UUID id) {
        return PENDING_QUEST.containsKey(id);
    }

    static void clearPending(java.util.UUID id) {
        CONFIRM_OPEN.remove(id);
        CONFIRM_WIDGETS.remove(id);
        PENDING_QUEST.remove(id);
    }
}

package me.nakilex.levelplugin.quests.gui;

import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class QuestGUI {

    public static final String GUI_TITLE = ChatColor.DARK_GREEN + "Quests";
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
    private static final int SORT_SLOT = 50;

    static final Map<java.util.UUID, Integer> pageMap = new java.util.HashMap<>();
    static final Map<java.util.UUID, Integer> filterMap = new java.util.HashMap<>();
    static final Map<java.util.UUID, Integer> sortMap = new java.util.HashMap<>();

    // Confirmation menu constants
    public static final String CONFIRM_TITLE = ChatColor.RED + "Confirm Abandon";
    private static final int CONFIRM_SIZE = 27;
    public static final int CONFIRM_YES_SLOT = 11;
    public static final int CONFIRM_NO_SLOT = 15;
    private static final Map<java.util.UUID, Inventory> CONFIRM_OPEN = new java.util.HashMap<>();
    private static final Map<java.util.UUID, String> PENDING_QUEST = new java.util.HashMap<>();

    public static void openQuestGUI(Player player, QuestManager questManager) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        openQuestGUI(player, questManager, page);
    }

    public static void openConfirmAbandon(Player player, Quest quest) {
        Inventory inv = Bukkit.createInventory(null, CONFIRM_SIZE, CONFIRM_TITLE);
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) { fm.setDisplayName(" "); filler.setItemMeta(fm); }
        for (int i = 0; i < CONFIRM_SIZE; i++) inv.setItem(i, filler);
        inv.setItem(CONFIRM_YES_SLOT, getNexoItem("check", ChatColor.GREEN + "Confirm"));
        inv.setItem(CONFIRM_NO_SLOT, getNexoItem("cross", ChatColor.RED + "Cancel"));
        CONFIRM_OPEN.put(player.getUniqueId(), inv);
        PENDING_QUEST.put(player.getUniqueId(), quest.getId());
        player.openInventory(inv);
    }

    static void openQuestGUI(Player player, QuestManager questManager, int page) {
        pageMap.put(player.getUniqueId(), page);
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < GUI_SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, filler);
            }
        }

        List<Quest> list = new ArrayList<>(questManager.getQuests());
        int filter = filterMap.getOrDefault(player.getUniqueId(), 0);
        int sort = sortMap.getOrDefault(player.getUniqueId(), 0);

        list.removeIf(q -> {
            QuestState state = questManager.getQuestState(player, q);
            return switch (filter) {
                case 1 -> state != QuestState.AVAILABLE;
                case 2 -> state != QuestState.IN_PROGRESS && state != QuestState.ACCEPTED;
                case 3 -> state != QuestState.COMPLETED;
                default -> false;
            };
        });

        Comparator<Quest> comp = sort == 0
                ? Comparator.comparing(Quest::getName, String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparing(q -> questManager.getQuestState(player, q).ordinal());
        list.sort(comp);

        int start = page * ITEMS_PER_PAGE;
        int slot = 0;
        for (int i = start; i < list.size() && slot < ITEMS_PER_PAGE; i++) {
            Quest quest = list.get(i);
            QuestState state = questManager.getQuestState(player, quest);
            ItemStack item = createQuestItem(player, quest, state, questManager.getProgress(player.getUniqueId()), questManager);
            gui.setItem(QUEST_SLOTS[slot++], item);
        }

        if (page > 0) gui.setItem(PREV_PAGE, getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        if (list.size() > (page + 1) * ITEMS_PER_PAGE) gui.setItem(NEXT_PAGE, getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        gui.setItem(FILTER_SLOT, createFilterButton(filter));
        gui.setItem(SORT_SLOT, createSortButton(sort));

        player.openInventory(gui);
    }

    private static ItemStack createQuestItem(Player player, Quest quest, QuestState state,
                                             PlayerQuestProgress progress, QuestManager qm) {
        String name = state == QuestState.LOCKED ? ChatColor.DARK_GRAY + "???" : state.getColor() + quest.getName();

        String icon = state.getIconId();
        String tracked = qm.getTrackedQuest(player.getUniqueId());
        if (tracked != null && tracked.equals(quest.getId())) {
            icon = "pack1_scroll4";
        }

        ItemStack item = getNexoItem(icon, name);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();

            if (state != QuestState.LOCKED) {
                lore.add(ChatColor.GRAY + quest.getDescription());

                if (state == QuestState.ACCEPTED || state == QuestState.IN_PROGRESS || state == QuestState.TURN_IN_READY) {
                    lore.add(" ");
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
                    lore.add(ChatColor.WHITE + desc + ChatColor.GRAY + " (" + objProgress + "/" + obj.getAmount() + ")");
                }

                lore.add(" ");
                lore.add(ChatColor.GREEN + "Rewards:");
                if (quest.getReward() != null) {
                    QuestReward r = quest.getReward();
                    if (r.getXp() > 0) lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + r.getXp() + " " + ChatColor.GREEN + "XP");
                    if (r.getCoins() > 0) lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + r.getCoins() + " " + ChatColor.YELLOW + "⛃");
                    if (r.getGems() > 0) lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + r.getGems() + " " + ChatColor.LIGHT_PURPLE + "✦");
                    for (int id : r.getItemIds()) {
                        me.nakilex.levelplugin.items.data.CustomItem tpl = me.nakilex.levelplugin.Main.getInstance().getItemManager().getTemplateById(id);
                        String in = tpl != null ? tpl.getBaseName() : ("Item " + id);
                        lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + in);
                    }
                    for (var cls : r.getUnlockClasses()) {
                        String pretty = cls.name().substring(0,1) + cls.name().substring(1).toLowerCase();
                        lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + pretty + " Class");
                    }
                } else {
                    lore.add(ChatColor.GREEN + "- " + ChatColor.GRAY + "None");
                }

                if (state == QuestState.ACCEPTED || state == QuestState.IN_PROGRESS || state == QuestState.TURN_IN_READY) {
                    lore.add(" ");
                    lore.add(ChatColor.WHITE + "Left-click " + ChatColor.GRAY + "to track");
                    if (!quest.isMainQuest()) {
                        lore.add(ChatColor.WHITE + "Right-click " + ChatColor.GRAY + "to abandon");
                    }
                }
            }

            meta.setLore(lore);
            meta.setLocalizedName(quest.getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack getNexoItem(String id, String name) {
        ItemBuilder b = NexoItems.itemFromId(id);
        ItemStack it = b == null ? new ItemStack(Material.BARRIER) : b.build();
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); it.setItemMeta(meta); }
        return it;
    }

    private static ItemStack createFilterButton(int mode) {
        ItemStack it = new ItemStack(Material.HOPPER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Filter");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_GRAY + "Filter the quests");
            lore.add(" ");
            String[] opts = {"Show All", "Available", "In Progress", "Completed"};
            for (int i = 0; i < opts.length; i++) {
                lore.add(rangeLine(i, mode, opts[i]));
            }
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Left-Click " + ChatColor.GRAY + "to go forward");
            lore.add(ChatColor.WHITE + "Right-Click " + ChatColor.GRAY + "to go backward");
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
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_GRAY + "Sort the quests");
            lore.add(" ");
            String[] opts = {"A-Z", "By State"};
            for (int i = 0; i < opts.length; i++) {
                lore.add(rangeLine(i, mode, opts[i]));
            }
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Left-Click " + ChatColor.GRAY + "to go forward");
            lore.add(ChatColor.WHITE + "Right-Click " + ChatColor.GRAY + "to go backward");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static String rangeLine(int index, int current, String label) {
        ChatColor color = index == current ? ChatColor.WHITE : ChatColor.GRAY;
        ChatColor bullet = index == current ? ChatColor.GREEN : ChatColor.DARK_GRAY;
        return bullet + "- " + color + label;
    }

    static Inventory getConfirmInventory(java.util.UUID id) {
        return CONFIRM_OPEN.get(id);
    }

    static String getPendingQuest(java.util.UUID id) {
        return PENDING_QUEST.get(id);
    }

    static void clearPending(java.util.UUID id) {
        CONFIRM_OPEN.remove(id);
        PENDING_QUEST.remove(id);
    }
}

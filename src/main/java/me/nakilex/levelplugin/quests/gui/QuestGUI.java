package me.nakilex.levelplugin.quests.gui;

import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
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
    private static final int INFO_SLOT = 8;

    static final Map<java.util.UUID, Integer> pageMap = new java.util.HashMap<>();
    static final Map<java.util.UUID, Integer> filterMap = new java.util.HashMap<>();
    static final Map<java.util.UUID, Integer> sortMap = new java.util.HashMap<>();

    public static void openQuestGUI(Player player, QuestManager questManager) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        openQuestGUI(player, questManager, page);
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
            ItemStack item = createQuestItem(player, quest, state, questManager.getProgress(player.getUniqueId()));
            gui.setItem(QUEST_SLOTS[slot++], item);
        }

        if (page > 0) gui.setItem(PREV_PAGE, getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        if (list.size() > (page + 1) * ITEMS_PER_PAGE) gui.setItem(NEXT_PAGE, getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        gui.setItem(FILTER_SLOT, createFilterButton(filter));
        gui.setItem(SORT_SLOT, createSortButton(sort));
        gui.setItem(INFO_SLOT, getNexoItem("info", ChatColor.YELLOW + "Information"));

        player.openInventory(gui);
    }

    private static ItemStack createQuestItem(Player player, Quest quest, QuestState state, PlayerQuestProgress progress) {
        ItemStack item = new ItemStack(state.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(state.getColor() + quest.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + quest.getDescription());
            if (progress != null && progress.getQuest().getId().equals(quest.getId())) {
                for (int i = 0; i < quest.getObjectives().size(); i++) {
                    lore.add(ChatColor.YELLOW + "Objective " + (i + 1) + ": " + progress.getProgress(i) + "/" + quest.getObjectives().get(i).getAmount());
                }
            }
            meta.setLore(lore);
            meta.setLocalizedName(quest.getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack getNexoItem(String id, String name) {
        ItemBuilder b = NexoItems.getItemById(id);
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
            String[] opts = {"Show All", "Available", "In Progress", "Completed"};
            for (int i = 0; i < opts.length; i++) {
                String pre = i == mode ? ChatColor.GREEN + "➤ " : ChatColor.GRAY + "  ";
                lore.add(pre + opts[i]);
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static ItemStack createSortButton(int mode) {
        ItemStack it = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Sort");
            List<String> lore = new ArrayList<>();
            String[] opts = {"A-Z", "By State"};
            for (int i = 0; i < opts.length; i++) {
                String pre = i == mode ? ChatColor.GREEN + "➤ " : ChatColor.GRAY + "  ";
                lore.add(pre + opts[i]);
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }
}

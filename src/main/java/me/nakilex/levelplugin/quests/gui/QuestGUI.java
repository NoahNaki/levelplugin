package me.nakilex.levelplugin.quests.gui;

import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class QuestGUI {

    public static final String GUI_TITLE = ChatColor.DARK_GREEN + "Quests";
    private static final int GUI_SIZE = 54;

    public static void openQuestGUI(Player player, QuestManager questManager) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        int slot = 0;
        for (Quest quest : questManager.getQuests()) {
            QuestState state = questManager.getQuestState(player, quest);
            ItemStack item = createQuestItem(player, quest, state, questManager.getProgress(player.getUniqueId()));
            gui.setItem(slot++, item);
            if (slot >= GUI_SIZE) break;
        }

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
}

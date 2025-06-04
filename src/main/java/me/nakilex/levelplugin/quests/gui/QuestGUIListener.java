package me.nakilex.levelplugin.quests.gui;

import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class QuestGUIListener implements Listener {

    private final QuestManager questManager;

    public QuestGUIListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equals(QuestGUI.GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        String id = meta.getLocalizedName();
        if (id == null || id.isEmpty()) return;

        QuestState state = questManager.getQuestState(player, questManager.getQuest(id));

        if (event.getClick() == ClickType.RIGHT) {
            if (state == QuestState.AVAILABLE) {
                questManager.startQuest(player, id);
                player.closeInventory();
            }
        }

        if (event.getClick() == ClickType.LEFT) {
            player.sendMessage(ChatColor.AQUA + "Quest: " + id);
        }
    }
}

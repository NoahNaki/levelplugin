package me.nakilex.levelplugin.player.classes.essence.listener;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceGUI;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ClassEssenceMenuListener implements Listener {

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (ClassEssenceGUI.TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!ClassEssenceGUI.TITLE.equals(event.getView().getTitle())) return;

        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() >= topSize) {
            // allow normal interaction in player inventory
            return;
        }
        event.setCancelled(true);

        int idx = ClassEssenceGUI.indexFromSlot(event.getRawSlot());
        if (idx == -1) return;

        Player player = (Player) event.getWhoClicked();
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        ClickType click = event.getClick();

        if (click.isLeftClick()) {
            if (current == null && cursor != null && ClassEssence.isEssence(cursor)) {
                ItemStack placed = cursor.clone();
                ClassEssence.addSlotTips(placed);
                ps.essenceSlots[idx] = placed;
                event.getView().setItem(event.getRawSlot(), placed);
                event.getWhoClicked().setItemOnCursor(null);
            } else if (current != null && ClassEssence.isEssence(current) && !ps.equippedEssences[idx]) {
                PlayerClass essenceClass = ClassEssence.getClass(current);
                for (int i = 0; i < ps.equippedEssences.length; i++) {
                    if (ps.equippedEssences[i] && i != idx) {
                        ItemStack other = event.getView().getItem(ClassEssenceGUI.slotFromIndex(i));
                        if (other != null && ClassEssence.isEssence(other)) {
                            ClassEssence.removeAttributes(player, other);
                            ClassEssence.setEquipped(other, false);
                            ClassEssence.addSlotTips(other);
                        }
                        ps.equippedEssences[i] = false;
                    }
                }
                Map<StatType, Integer> before = new HashMap<>();
                for (StatType st : ClassEssence.getStatTypes(current)) {
                    before.put(st, StatsManager.getInstance().getStatValue(player, st));
                }

                ClassEssence.setEquipped(current, true);
                ClassEssence.applyAttributes(player, current);
                ps.equippedEssences[idx] = true;

                // Change the player's class to match the essence
                ps.playerClass = essenceClass;
                ps.unlockedClasses.add(essenceClass);
                me.nakilex.levelplugin.player.classes.managers.PlayerClassManager.getInstance()
                        .setPlayerClass(player, essenceClass);
                me.nakilex.levelplugin.items.utils.ItemUtil.refreshTooltips(player);

                me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§6§l-", 45);
                me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§6§lESSENCE EQUIPPED!");
                me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "");
                me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player,
                        ChatColor.GRAY + "You are now the §e§l" + TextUtil.beautifyWords(essenceClass.name()) + " §7class!");
                me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "");
                for (StatType st : before.keySet()) {
                    int after = StatsManager.getInstance().getStatValue(player, st);
                    ChatColor col = after >= before.get(st) ? ChatColor.GREEN : ChatColor.RED;
                    String name = GuiUtil.formatStatName(st);
                    me.nakilex.levelplugin.utils.ChatMessageUtil.send(player,
                            me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.INFO,
                            name + ": " + ChatColor.WHITE + before.get(st) + ChatColor.GRAY + " -> " + col + after);
                }
                me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "");
                me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§6§l-", 45);
            }
        } else if (click.isRightClick()) {
            if (current != null && ClassEssence.isEssence(current)) {
                if (ps.equippedEssences[idx]) {
                    ClassEssence.removeAttributes(player, current);
                    ps.equippedEssences[idx] = false;
                    ClassEssence.setEquipped(current, false);
                }
                ClassEssence.updateLore(current);
                player.getInventory().addItem(current);
                ps.essenceSlots[idx] = null;
                event.getView().setItem(event.getRawSlot(), null);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!ClassEssenceGUI.TITLE.equals(event.getView().getTitle())) return;
        Player player = (Player) event.getPlayer();
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        for (int i = 0; i < 3; i++) {
            ItemStack item = event.getInventory().getItem(ClassEssenceGUI.slotFromIndex(i));
            if (item != null && ClassEssence.isEssence(item)) {
                ClassEssence.updateLore(item);
            }
            ps.essenceSlots[i] = item;
            ps.equippedEssences[i] = item != null && ClassEssence.isEssence(item) && ClassEssence.isEquipped(item);
        }
    }
}

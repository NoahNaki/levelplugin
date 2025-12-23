package me.nakilex.levelplugin.player.classes.essence.listener;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Allows players to cycle their essences with the swap-hand (F) key without opening the GUI.
 */
public class ClassEssenceSwapListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

        int totalEssences = 0;
        for (ItemStack essence : ps.essenceSlots) {
            if (essence != null && ClassEssence.isEssence(essence)) {
                totalEssences++;
            }
        }

        if (totalEssences == 0) {
            return;
        }

        event.setCancelled(true);

        int currentIdx = -1;
        for (int i = 0; i < ps.equippedEssences.length; i++) {
            if (ps.equippedEssences[i]) {
                currentIdx = i;
                break;
            }
        }

        int nextIdx = findNext(ps, currentIdx);
        if (nextIdx < 0 || nextIdx == currentIdx) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No other essences to swap to.");
            return;
        }

        ItemStack nextEssence = ps.essenceSlots[nextIdx];
        if (nextEssence == null || !ClassEssence.isEssence(nextEssence)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Your next essence slot is empty.");
            return;
        }

        if (currentIdx >= 0) {
            ItemStack equipped = ps.essenceSlots[currentIdx];
            if (equipped != null && ClassEssence.isEssence(equipped)) {
                ClassEssenceEquipHelper.unequip(player, ps, currentIdx, equipped);
            }
        }

        ClassEssenceEquipHelper.equip(player, ps, nextIdx, nextEssence, null);
    }

    private int findNext(StatsManager.PlayerStats ps, int currentIdx) {
        int count = ps.essenceSlots.length;
        for (int offset = 1; offset <= count; offset++) {
            int check = (currentIdx + offset + count) % count;
            ItemStack stack = ps.essenceSlots[check];
            if (stack != null && ClassEssence.isEssence(stack)) {
                return check;
            }
        }
        return -1;
    }
}


package me.nakilex.levelplugin.player.classes.essence.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.server.ServerSelectionManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.WorldExclusionUtil;
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
        ServerSelectionManager serverSelectionManager = Main.getInstance().getServerSelectionManager();
        if (serverSelectionManager != null && serverSelectionManager.isHubWorld(player.getWorld())) {
            return;
        }
        if (WorldExclusionUtil.isExcluded(player)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You cannot swap essences in this area.");
            event.setCancelled(true);
            return;
        }
        StatsManager statsManager = StatsManager.getInstance();
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(player.getUniqueId());
        int unlockedSlots = statsManager.getUnlockedEssenceSlots(player);

        int totalEssences = 0;
        for (int i = 0; i < unlockedSlots; i++) {
            ItemStack essence = ps.essenceSlots[i];
            if (essence != null && ClassEssence.isEssence(essence)) {
                totalEssences++;
            }
        }

        if (totalEssences == 0) {
            return;
        }

        event.setCancelled(true);

        int currentIdx = -1;
        for (int i = 0; i < unlockedSlots; i++) {
            if (ps.equippedEssences[i]) {
                currentIdx = i;
                break;
            }
        }

        int nextIdx = findNext(ps, currentIdx, unlockedSlots);
        if (nextIdx < 0 || nextIdx == currentIdx) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No other essences to swap to.");
            return;
        }

        ItemStack nextEssence = ps.essenceSlots[nextIdx];
        if (nextEssence == null || !ClassEssence.isEssence(nextEssence)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Your next essence slot is empty.");
            return;
        }

        PlayerClass previousClass = null;
        if (currentIdx >= 0) {
            ItemStack equipped = ps.essenceSlots[currentIdx];
            if (equipped != null && ClassEssence.isEssence(equipped)) {
                previousClass = ClassEssence.getClass(equipped);
                ClassEssenceEquipHelper.unequip(player, ps, currentIdx, equipped);
            }
        }

        ClassEssenceEquipHelper.equip(player, ps, nextIdx, nextEssence, null);
        PlayerClass nextClass = ClassEssence.getClass(nextEssence);
        if (previousClass != null && nextClass != null && previousClass != nextClass) {
            Main.getInstance().getQuestManager().handleEssenceSwap(player);
        }
    }

    private int findNext(StatsManager.PlayerStats ps, int currentIdx, int unlockedSlots) {
        int count = Math.max(0, unlockedSlots);
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

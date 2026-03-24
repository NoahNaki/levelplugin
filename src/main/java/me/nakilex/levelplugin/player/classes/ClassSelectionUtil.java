package me.nakilex.levelplugin.player.classes;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class ClassSelectionUtil {
    private ClassSelectionUtil() {
    }

    public static boolean isSelectableBaseClass(PlayerClass playerClass) {
        return playerClass == PlayerClass.MAGE
                || playerClass == PlayerClass.ARCHER
                || playerClass == PlayerClass.ROGUE
                || playerClass == PlayerClass.WARRIOR;
    }

    public static boolean applyClassSelection(Player player, PlayerClass chosen, boolean announce) {
        if (player == null || chosen == null || !isSelectableBaseClass(chosen)) {
            return false;
        }
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        stats.unlockedClasses.add(chosen);
        PlayerClassManager.getInstance().setPlayerClass(player, chosen);
        if (announce) {
            sendSelectionFeedback(player, chosen);
        }
        Main.getInstance().getQuestManager().handleClassSelect(player);
        me.nakilex.levelplugin.items.utils.ItemUtil.refreshTooltips(player);
        return true;
    }

    private static void sendSelectionFeedback(Player player, PlayerClass chosen) {
        ChatFormatter.constructDivider(player, "§6§l-", 45);
        ChatFormatter.sendCenteredMessage(player, "§6§lCLASS SELECTED!");
        ChatFormatter.sendCenteredMessage(player, "");
        ChatFormatter.sendCenteredMessage(player,
                "§7You are now the §e§l" + chosen.name() + " §7class!");
        ChatFormatter.sendCenteredMessage(player, "");
        ChatFormatter.constructDivider(player, "§6§l-", 45);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.closeInventory();
    }
}

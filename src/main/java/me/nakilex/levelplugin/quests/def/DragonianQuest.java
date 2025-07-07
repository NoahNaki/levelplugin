package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.quests.data.*;
import org.bukkit.entity.Player;

/** Simple quest that unlocks the Dragonian class. */
public class DragonianQuest extends Quest implements QuestCompletionScript {
    private static java.util.List<QuestObjective> createObjectives() {
        return java.util.List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc533", 1)
        );
    }

    public DragonianQuest() {
        super(
                "dragonianquest",
                "Dragon's Challenge",
                "Speak with the dragon to earn a new power.",
                createObjectives(),
                1,
                java.util.List.of(),
                null,
                QuestRewardCompat.create(200, 100, 0, java.util.List.of()),
                533,
                java.util.List.of("Greetings, mortal.", "Return to me and claim your reward.")
        );
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        StatsManager.getInstance().unlockClass(player.getUniqueId(), PlayerClass.DRAGONIAN);
        player.sendMessage(org.bukkit.ChatColor.GREEN + "You have unlocked the DRAGONIAN class!");
    }
}

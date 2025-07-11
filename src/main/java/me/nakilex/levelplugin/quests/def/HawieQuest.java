package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.quests.data.*;
import me.nakilex.levelplugin.Main;
import org.bukkit.entity.Player;
import java.util.List;

public class HawieQuest extends Quest implements QuestScript, QuestCompletionScript {
    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.COLLECT, "stone", 10),
                new QuestObjective(QuestObjectiveType.COLLECT, "coal", 10),
                new QuestObjective(QuestObjectiveType.COLLECT, "lumber", 20),
                new QuestObjective(QuestObjectiveType.TALK, "npc540", 1)
        );
    }

    public HawieQuest() {
        super(
                "hawieshop",
                "Rebuild Hawie's Shop",
                "Gather materials to rebuild the blacksmith shop.",
                createObjectives(),
                1,
                List.of("serashelp"),
                null,
                QuestRewardCompat.create(300, 150, 0, List.of()),
                540,
                List.of(
                        "Greetings adventurer, how can I help you?",
                        "Ah you're looking for upgrades, I understand but sadly my shop got destroyed the other day due a dragon flying over this village, everything got destroyed...",
                        "If only I was young again, I could build it from the ground up like I did before, although you seem like a capable adventurer, if you help me rebuild my shop, I'll make sure to reward you handsomely.",
                        "Go collect 10 stone, 10 coal, 20 lumber at <location>",
                        "Wow that was fast, with this we can rebuild the shop."
                ),
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // nothing
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        player.sendMessage("Blacksmith Unlocked- Upgrade Unlocked- Repair Unlocked- Stat Reroll Unlocked");
    }
}

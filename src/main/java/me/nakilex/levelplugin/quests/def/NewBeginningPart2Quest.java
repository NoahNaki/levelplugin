package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.classes.gui.ClassMenu;
import me.nakilex.levelplugin.quests.data.*;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Second part of the introduction questline.
 */
public class NewBeginningPart2Quest extends Quest implements QuestScript {
    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.SELECT_CLASS, "ANY", 1),
                new QuestObjective(QuestObjectiveType.BUY, "class_weapon", 1),
                new QuestObjective(QuestObjectiveType.TALK, "npc537", 1)
        );
    }

    public NewBeginningPart2Quest() {
        super(
                "newbeginning2",
                "A New Beginning II",
                "Choose a class and gear up for adventure.",
                createObjectives(),
                1,
                List.of("newbeginning1"),
                null,
                QuestRewardCompat.create(150, 30, 0, List.of()),
                537,
                List.of(
                        "First things first—gear. That outfit of yours could sell for a fortune.",
                        "The fabric’s nobility-tier, but you might need it later, so I’ll cover you for now.",
                        "Take these coins, go see the merchant, and grab some armor and a weapon.",
                        "But before that, we need to know your /class."
                )
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        player.openInventory(ClassMenu.getClassSelectionMenu(player));
    }
}

package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.data.QuestCompletionScript;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Hawie's follow-up request that has players thin out the hermit crabs gnawing
 * on his newly rebuilt docks.
 */
public class HawieHermitCrabQuest extends Quest implements QuestScript, QuestCompletionScript {
    public static final String ID = "hawiehermitcrabs";
    public static final String INTRO_TARGET = "npc1089_intro";
    public static final String RETURN_TARGET = "npc1089_return";
    public static final String FISH_RETURN_TARGET = "npc1089_fish";
    public static final String CAPTURE_TARGET = "ANY";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(1089)),
                new QuestObjective(QuestObjectiveType.KILL, "vp1_hermit_crab", 10),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(1089)),
                new QuestObjective(QuestObjectiveType.CAPTURE_FISH, CAPTURE_TARGET, 1, BeaconTargets.npc(1089)),
                new QuestObjective(QuestObjectiveType.TALK, FISH_RETURN_TARGET, 1, BeaconTargets.npc(1089))
        );
    }

    public HawieHermitCrabQuest() {
        super(
                ID,
                "Clattering Cleanup",
                "Help Hawie stop the hermit crabs from tearing up his docks.",
                createObjectives(),
                10,
                List.of(StableKeeperQuest.ID),
                null,
                QuestRewardCompat.create(1000, 250, 0, List.of()),
                1089,
                List.of(
                        "Hawie|This little pond used to be peaceful, but now it's crawling with hermit crabs squatting in every tidepool.",
                        "Hawie|They rip shingles off my dock to make new shells and pinch the deckhands any time they reach for a mooring rope.",
                        "Hawie|Head down the shoreline, smash ten of the Hermit Crabs stirring up the muck, and I'll pay you better than those pests deserve.",
                        "Hawie|Come back alive with good news and maybe we can hear the waves again instead of all that clattering."
                ),
                false,
                true,
                true
        );
    }

    public static void registerTalkTargets(QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, "Hawie", "Hawie");
        questManager.registerTalkTarget(RETURN_TARGET, "Hawie", "Hawie");
        questManager.registerTalkTarget(FISH_RETURN_TARGET, "Hawie", "Hawie");
    }

    public static List<String> getReturnDialog() {
        return List.of(
                "Hawie|That's the last of the clattering nuisances? Music to my ears!",
                "<player>|The docks should stay in one piece now.",
                "Hawie|Nice, now that the lake is cleared of the hermit crabs the fish should be returning soon.",
                "Hawie|How about you try it out? Bring me back your first catch."
        );
    }

    public static List<String> getFishingReturnDialog() {
        return List.of(
                "Hawie|That's a catch worth bragging about.",
                "<player>|Thanks, say Hawie, do you by any chance know about an Essence Weaver somewhere in these woods?",
                "Hawie|Essence Weaver you say?",
                "Hawie|Yeah there's one..."
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // no special start logic
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null) {
            return;
        }
        boolean hasEssenceWeaver = questManager.hasCompleted(player.getUniqueId(), EssenceWeaversLessonQuest.ID)
                || questManager.getProgress(player.getUniqueId(), EssenceWeaversLessonQuest.ID) != null;
        if (!hasEssenceWeaver) {
            questManager.startQuest(player, EssenceWeaversLessonQuest.ID);
            questManager.setTrackedQuest(player, EssenceWeaversLessonQuest.ID);
        }
    }
}

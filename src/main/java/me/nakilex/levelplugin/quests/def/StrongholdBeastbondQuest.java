package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestCompletionScript;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class StrongholdBeastbondQuest extends Quest implements QuestCompletionScript {
    public static final String ID = "strongholdbeastbond";
    public static final int NPC_ID = 4709;

    public StrongholdBeastbondQuest() {
        super(
                ID,
                "Beastbond in the Bastion",
                "Help a stable warden prove pets can endure Stronghold pressure.",
                List.of(
                        new QuestObjective(QuestObjectiveType.TALK, "npc" + NPC_ID, 1, BeaconTargets.npc(NPC_ID)),
                        new QuestObjective(QuestObjectiveType.STRONGHOLD_ENTER, "ANY", 1),
                        new QuestObjective(QuestObjectiveType.STRONGHOLD_WAVE_CLEAR, "ANY", 8),
                        new QuestObjective(QuestObjectiveType.TALK, "npc" + NPC_ID + "_return", 1, BeaconTargets.npc(NPC_ID))
                ),
                12,
                List.of(StrongholdInitiationQuest.ID),
                null,
                QuestRewardCompat.create(650, 350, 20, List.of()),
                null,
                List.of(
                        "Beast Warden|Stronghold pressure breaks weak bonds. Strong bonds endure.",
                        "<player>|What do you need from me?",
                        "Beast Warden|Take your companion into the Stronghold, survive several waves, then report back.",
                        "Beast Warden|Prove that bond, and I'll authorize one more active pet slot."
                ),
                true,
                false
        );
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        if (player == null || plugin == null || plugin.getPetManager() == null) {
            return;
        }
        int maxSlots = plugin.getPetManager().grantExtraPetSlots(player.getUniqueId(), 1);
        player.sendMessage(ChatColor.GREEN + "Pet capacity increased: " + ChatColor.WHITE + maxSlots + ChatColor.GREEN + " active slot(s).");
    }
}

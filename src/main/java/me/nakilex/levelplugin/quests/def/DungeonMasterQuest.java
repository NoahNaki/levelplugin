package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.quests.data.*;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Simple dialog quest for the Dungeon Master NPC used for testing dialog flows.
 */
public class DungeonMasterQuest extends Quest implements QuestScript {
    public DungeonMasterQuest() {
        super(
                "dungeonmaster",
                "Dungeon Master's Proposal",
                "Hear out the Dungeon Master's proposal.",
                List.of(),
                1,
                List.of(),
                null,
                QuestRewardCompat.create(0, 0, 0, List.of()),
                900,
                null,
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        NPC npc = CitizensAPI.getNPCRegistry().getById(900);
        if (npc == null) return;
        PlayerClass cls = PlayerClassManager.getInstance().getPlayerClass(player);
        String action = ClassUtil.getAttackPhrase(cls);
        List<String> lines = List.of(
                "Ah, so you're the infamous " + player.getName() + ". Word of your exploits has reached even me - dungeons cleared, monsters felled, treasures claimed. And now... you've set your sights on mine.",
                "But before you go " + action + ", let me offer you a different perspective.",
                "A dungeon is no different from a casino. Sometimes the house wins, sometimes the adventurers do. Some come chasing treasure, others glory - but one truth remains: a dungeon is never short of challengers. The game always goes on.",
                "And in that, our goals are the same. Adventurers want riches, experience, and fame. Dungeon masters want a steady flow of contenders to keep the halls alive, to feed the walls with mana, essence, and fear. Without adventurers, a dungeon withers. Without dungeons, adventurers have nowhere to prove themselves.",
                "So you see, when you clear a dungeon, you're not conquering a fortress - you're shutting down the very game both sides rely on.",
                "That's why I propose this: instead of ending the game, let's raise the stakes. You will design a dungeon of your own - stocked with the monsters you've slain, the traps you devise, the challenges only you could imagine. And I will manage it for you.",
                "You gain more than glory from conquest - you gain legacy. A dungeon bearing your mark, your name etched in the Leaderboards, coin flowing into your coffers as others gamble their lives against your creation. And I? I climb in rank, feeding off the difficulty and popularity we build together.",
                "So, " + player.getName() + "... what will it be? Do you end the game here? Or will you sit at the table with me, and make the world play by our rules?"
        );
        plugin.getDialogManager().startDialog(player, lines, npc, () ->
                plugin.getDialogManager().startChoiceDialog(player, npc, List.of("Yes", "No"), null, null, choice -> {
                    if (choice == 0) {
                        plugin.getDialogManager().startDialog(player,
                                List.of("Dungeon Master|Great, whenever you want to begin just come find me, I'll be here."), npc, null);
                    } else {
                        plugin.getDialogManager().startDialog(player,
                                List.of("Dungeon Master|Alright, then prepare to have the fight of your life."), npc, null);
                    }
                })
        );
    }
}


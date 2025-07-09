package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.quests.data.*;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Intro quest that plays a short conversation with Piwan.
 */
public class NewBeginningPart1Quest extends Quest implements QuestScript {
    private static List<QuestObjective> createObjectives() {
        return List.of(new QuestObjective(QuestObjectiveType.TALK, "npc600", 1));
    }

    public NewBeginningPart1Quest() {
        super(
                "newbeginning1",
                "A New Beginning I",
                "Meet Piwan after arriving in the new world.",
                createObjectives(),
                1,
                List.of("officeerrands"),
                null,
                QuestRewardCompat.create(100, 0, 0, List.of(),
                        List.of(PlayerClass.ARCHER, PlayerClass.WARRIOR,
                                PlayerClass.MAGE, PlayerClass.ROGUE)),
                null,
                null
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                NPC npc = CitizensAPI.getNPCRegistry().getById(600);
                if (npc == null || !npc.isSpawned()) return;
                if (!player.isOnline()) { cancel(); return; }
                if (!npc.getEntity().getWorld().equals(player.getWorld())) return;
                if (player.getLocation().distanceSquared(npc.getEntity().getLocation()) <= 100) {
                    playDialog(player, plugin, npc);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void playDialog(Player player, Main plugin, NPC npc) {
        InetSocketAddress addr = player.getAddress();
        String ip = addr != null && addr.getAddress() != null ? addr.getAddress().getHostAddress() : "unknown";
        String[] lines = new String[] {
                "Adventurer please help me!",
                "Phew thank you that was a close one, one second later and I would've been toast, how did you suddenly appear, I could've sworn there wasn't anyone there a second ago, and your clothes, your certainly dont look from around here, are you perhaps a noble from another country?",
                "What? You came from another world, I guess that somewhat makes sense, my mom used to tell me that she once met an adventurer who claimed the same thing, said they're from a place called " + ip + ".",
                "My name is Piwan, since you're not from around here, I could show you around my village."
        };
        new BukkitRunnable() {
            int idx = 0;
            @Override
            public void run() {
                if (idx >= lines.length) {
                    Main.getInstance().getQuestManager().handleTalk(player, "npc600");
                    cancel();
                    return;
                }
                player.sendMessage(ChatColor.YELLOW + npc.getName() + ChatColor.WHITE + ": " + lines[idx]);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                idx++;
            }
        }.runTaskTimer(plugin, 0L, 60L);
    }
}

package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.*;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInteractEntityEvent;

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
        // Invoke the external class selection plugin
        player.performCommand("class");

        Listener[] handler = new Listener[1];
        handler[0] = new Listener() {
            int idx = 0;

            @EventHandler(priority = EventPriority.LOWEST)
            public void onInteract(PlayerInteractEntityEvent event) {
                if (!event.getPlayer().equals(player)) return;
                if (event.getHand() == EquipmentSlot.OFF_HAND) return;
                if (!CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) return;
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                if (npc.getId() != 537) return;

                PlayerQuestProgress prog = plugin.getQuestManager().getProgress(player.getUniqueId());
                if (prog == null || !prog.getQuest().getId().equals("newbeginning2")) return;
                if (prog.getProgress(0) < 1 || prog.getProgress(1) < 1 || prog.getProgress(2) >= 1) return;

                event.setCancelled(true);

                PlayerClass pc = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
                String className = pc.name().substring(0, 1) + pc.name().substring(1).toLowerCase();
                String[] lines = new String[]{
                        "Ah I see you went with the " + className + ", a wise choice.",
                        "Now all that's left is for you to venture forth into the vast world of Eldrin and become stronger!",
                        "Good luck adventurer! Maybe some day our paths will cross again."
                };

                if (idx >= lines.length) return;
                player.sendMessage(ChatColor.GRAY + "[" + (idx + 1) + "/" + lines.length + "] " +
                        ChatColor.YELLOW + npc.getName() + ChatColor.WHITE + ": " + lines[idx]);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                idx++;

                if (idx >= lines.length) {
                    HandlerList.unregisterAll(handler[0]);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getQuestManager().handleTalk(player, "npc537");
                        }
                    }, 20L);
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(handler[0], plugin);
    }
}

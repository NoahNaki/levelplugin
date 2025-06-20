package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fakeblock.QuestGate;
import me.nakilex.levelplugin.fakeblock.QuestGateManager;
import me.nakilex.levelplugin.quests.data.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class OfficeErrandsQuest extends Quest implements QuestScript {

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld("redrocks");
        Location beacon = world != null ? new Location(world, 29.5, 142, -92.5) : null;
        return java.util.List.of(
                new QuestObjective(QuestObjectiveType.TALK, "ANY", 1, beacon)
        );
    }

    public OfficeErrandsQuest() {
        super(
                "officeerrands",
                "Office Errands",
                "Help around the office.",
                createObjectives(),
                1,
                java.util.List.of(),
                null,
                null,
                null,
                java.util.List.of()
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        World world = Bukkit.getWorld("redrocks");
        if (world != null) {
            player.teleport(new Location(world, 29.5, 142.0, -92.5));
        }

        QuestGateManager gates = plugin.getQuestGateManager();
        String gateId = "office_elevator";
        gates.closeGate(player, gateId);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            gates.openGate(player, gateId);
            player.sendTitle(ChatColor.RED.toString() + ChatColor.BOLD + "CENTRAL EXECUTIVE",
                    "", 10, 40, 10);
        }, 80L);
    }
}

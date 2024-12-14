package me.nakilex.levelplugin.tasks;

import me.nakilex.levelplugin.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ActionBarTask extends BukkitRunnable {
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);

            double hp = player.getHealth();
            double maxHp = player.getMaxHealth();
            int currentMana = (int) ps.currentMana;
            int maxMana = ps.maxMana;

            String barMessage = "§c" + (int)hp + "/" + (int)maxHp + " §7| §b" + currentMana + "/" + maxMana;
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(barMessage));
        }
    }
}

package me.nakilex.levelplugin.player.attributes.managers;

import me.nakilex.levelplugin.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarTask extends BukkitRunnable {
    private final Main plugin;

    public ActionBarTask(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getCutsceneManager().isInCutscene(player)) continue;
            CooldownIndicatorManager.Info info = CooldownIndicatorManager.getInstance().get(player);
            if (info != null) {
                boolean showCd = now < info.expireAt && now < info.costExpireAt;
                boolean showCost = info.cost > 0 && now < info.costExpireAt;
                if (showCd || showCost) {
                    StringBuilder msg = new StringBuilder();
                    if (showCd) {
                        long remaining = info.expireAt - now;
                        int seconds = (int) Math.ceil(remaining / 1000.0);
                        msg.append(ChatColor.YELLOW).append(info.name)
                           .append(ChatColor.GRAY).append(" cooldown ")
                           .append(ChatColor.YELLOW).append(seconds).append("s");
                    }
                    if (showCost) {
                        if (!showCd) {
                            msg.append(ChatColor.YELLOW).append(info.name);
                        }
                        msg.append(" ")
                           .append(ChatColor.DARK_GRAY).append("[")
                           .append(ChatColor.GRAY).append("-")
                           .append(ChatColor.GRAY).append(info.cost)
                           .append(ChatColor.DARK_GRAY).append("]");
                    }
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg.toString()));
                    continue;
                }
            }
            if (StatsManager.getInstance().isInCombat(player.getUniqueId())) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.RED + "In Combat"));
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
            }
        }
    }
}

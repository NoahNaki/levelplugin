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
                long remaining = info.expireAt - now;
                int seconds = (int) Math.ceil(remaining / 1000.0);
                String msg = ChatColor.YELLOW + info.name + ChatColor.GRAY + " cooldown " + ChatColor.YELLOW + seconds + "s";
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
            }
        }
    }
}

package me.nakilex.levelplugin.player.attributes.managers;

import me.nakilex.levelplugin.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
            String baseMessage = "";
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
                    baseMessage = msg.toString();
                }
            }
            if (baseMessage.isEmpty()) {
                if (StatsManager.getInstance().isInCombat(player.getUniqueId())) {
                    baseMessage = ChatColor.RED + "In Combat";
                } else {
                    String consistency = me.nakilex.levelplugin.player.farming.managers.FarmingManager.getInstance()
                            .getConsistencyIndicator(player);
                    if (consistency != null) {
                        baseMessage = consistency;
                    }
                }
            }
            Component hudComponent = plugin.getHudManager() != null
                    ? plugin.getHudManager().getHudActionBarComponent(player)
                    : Component.empty();
            Component baseComponent = baseMessage.isBlank()
                    ? Component.empty()
                    : LegacyComponentSerializer.legacySection().deserialize(baseMessage);
            Component combined = hudComponent;
            if (!baseMessage.isBlank()) {
                if (combined.equals(Component.empty())) {
                    combined = baseComponent;
                } else {
                    combined = combined.append(Component.space()).append(baseComponent);
                }
            }
            player.sendActionBar(combined);
        }
    }
}

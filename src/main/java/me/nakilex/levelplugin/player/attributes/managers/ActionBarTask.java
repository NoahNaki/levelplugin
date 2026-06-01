package me.nakilex.levelplugin.player.attributes.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.stronghold.run.StrongholdRunManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
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
            if (plugin.getQuestDialogueManager() != null && plugin.getQuestDialogueManager().hasSession(player)) continue;
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
                    String strongholdHp = strongholdHpSegment(player);
                    if (!strongholdHp.isEmpty()) {
                        msg.append(ChatColor.DARK_GRAY).append(" | ").append(strongholdHp);
                    }
                    player.sendActionBar(Component.text(msg.toString()));
                    continue;
                }
            }
            String strongholdHp = strongholdHpSegment(player);
            if (StatsManager.getInstance().isInCombat(player.getUniqueId())) {
                String msg = ChatColor.RED + "In Combat"
                        + (strongholdHp.isEmpty() ? "" : ChatColor.DARK_GRAY + " | " + strongholdHp);
                player.sendActionBar(Component.text(msg));
            } else {
                String consistency = me.nakilex.levelplugin.player.farming.managers.FarmingManager.getInstance()
                        .getConsistencyIndicator(player);
                if (consistency != null) {
                    String msg = consistency
                            + (strongholdHp.isEmpty() ? "" : ChatColor.DARK_GRAY + " | " + strongholdHp);
                    player.sendActionBar(Component.text(msg));
                } else {
                    player.sendActionBar(Component.text(strongholdHp));
                }
            }
        }
    }

    private String strongholdHpSegment(Player player) {
        if (player == null) {
            return "";
        }
        StrongholdRunManager runManager = plugin.getStrongholdRunManager();
        if (runManager == null || runManager.getStageStatus(player.getUniqueId()) == null) {
            return "";
        }
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) == null
                ? player.getMaxHealth()
                : player.getAttribute(Attribute.MAX_HEALTH).getValue();
        return ChatColor.RED + "HP " + ChatColor.WHITE + (int) Math.ceil(player.getHealth())
                + ChatColor.GRAY + "/" + ChatColor.WHITE + (int) Math.ceil(maxHealth);
    }
}
